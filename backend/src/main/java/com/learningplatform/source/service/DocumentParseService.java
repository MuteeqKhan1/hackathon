package com.learningplatform.source.service;

import com.learningplatform.source.domain.SourceChunk;
import com.learningplatform.source.domain.SourceMaterialVersion;
import com.learningplatform.source.domain.SourceSection;
import com.learningplatform.source.domain.SourceSectionVersion;
import com.learningplatform.source.domain.SourceVersionStatus;
import com.learningplatform.source.parsing.ContentHasher;
import com.learningplatform.source.parsing.ParsedSection;
import com.learningplatform.source.parsing.PdfSectionParser;
import com.learningplatform.source.parsing.SectionChangeType;
import com.learningplatform.source.parsing.SectionChunker;
import com.learningplatform.source.parsing.SectionDiffEngine;
import com.learningplatform.source.parsing.TextNormalizer;
import com.learningplatform.source.repository.SourceChunkRepository;
import com.learningplatform.source.repository.SourceMaterialVersionRepository;
import com.learningplatform.source.repository.SourceSectionRepository;
import com.learningplatform.source.repository.SourceSectionVersionRepository;
import com.learningplatform.source.storage.ObjectStorage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DocumentParseService {

    private final ObjectStorage objectStorage;
    private final PdfSectionParser pdfSectionParser;
    private final SourceSectionRepository sourceSectionRepository;
    private final SourceSectionVersionRepository sourceSectionVersionRepository;
    private final SourceChunkRepository sourceChunkRepository;
    private final SourceMaterialVersionRepository sourceMaterialVersionRepository;

    public DocumentParseService(
            ObjectStorage objectStorage,
            PdfSectionParser pdfSectionParser,
            SourceSectionRepository sourceSectionRepository,
            SourceSectionVersionRepository sourceSectionVersionRepository,
            SourceChunkRepository sourceChunkRepository,
            SourceMaterialVersionRepository sourceMaterialVersionRepository
    ) {
        this.objectStorage = objectStorage;
        this.pdfSectionParser = pdfSectionParser;
        this.sourceSectionRepository = sourceSectionRepository;
        this.sourceSectionVersionRepository = sourceSectionVersionRepository;
        this.sourceChunkRepository = sourceChunkRepository;
        this.sourceMaterialVersionRepository = sourceMaterialVersionRepository;
    }

    @Transactional
    public ParseResult parseAndPersist(SourceMaterialVersion version) throws Exception {
        List<ParsedSection> parsed;
        try (InputStream in = objectStorage.get(version.getStorageKey())) {
            parsed = pdfSectionParser.parse(in);
        }
        if (parsed.isEmpty()) {
            throw new IllegalStateException("Parser produced no sections");
        }

        Map<String, SectionDiffEngine.SectionSnapshot> previous = loadPreviousSnapshots(version);
        Map<String, SectionDiffEngine.SectionSnapshot> current = new LinkedHashMap<>();
        for (ParsedSection section : parsed) {
            String hash = ContentHasher.sha256Normalized(section.content());
            current.put(
                    section.externalReference(),
                    new SectionDiffEngine.SectionSnapshot(section.externalReference(), section.title(), hash)
            );
        }

        Map<String, SectionChangeType> changeByKey = new HashMap<>();
        for (SectionDiffEngine.SectionDiff diff : SectionDiffEngine.diff(previous, current)) {
            changeByKey.put(diff.externalReference(), diff.changeType());
        }

        int sectionCount = 0;
        int chunkCount = 0;
        int modified = 0;
        int added = 0;

        for (ParsedSection parsedSection : parsed) {
            SourceSection section = sourceSectionRepository
                    .findBySourceMaterialIdAndExternalReference(version.getSourceMaterialId(), parsedSection.externalReference())
                    .orElseGet(() -> sourceSectionRepository.save(new SourceSection(
                            UUID.randomUUID(),
                            version.getSourceMaterialId(),
                            null,
                            parsedSection.externalReference(),
                            parsedSection.title(),
                            parsedSection.sectionType()
                    )));
            if (!section.getTitle().equals(parsedSection.title())) {
                section.updateTitle(parsedSection.title());
                sourceSectionRepository.save(section);
            }

            String normalized = TextNormalizer.normalize(parsedSection.content());
            String hash = ContentHasher.sha256(normalized);
            SectionChangeType changeType = changeByKey.getOrDefault(
                    parsedSection.externalReference(),
                    previous.isEmpty() ? SectionChangeType.ADDED : SectionChangeType.UNCHANGED
            );
            if (changeType == SectionChangeType.ADDED) {
                added++;
            }
            if (changeType == SectionChangeType.MODIFIED) {
                modified++;
            }

            SourceSectionVersion sectionVersion = sourceSectionVersionRepository.save(new SourceSectionVersion(
                    UUID.randomUUID(),
                    section.getId(),
                    version.getId(),
                    normalized,
                    hash,
                    changeType,
                    Instant.now()
            ));
            sectionCount++;

            List<String> chunks = SectionChunker.chunk(normalized);
            int index = 0;
            for (String chunk : chunks) {
                String metadata = """
                        {"sourceMaterialId":"%s","sourceVersionId":"%s","sectionId":"%s","language":"en","chunkIndex":%d}
                        """.formatted(
                        version.getSourceMaterialId(),
                        version.getId(),
                        section.getId(),
                        index
                ).trim();
                sourceChunkRepository.save(new SourceChunk(
                        UUID.randomUUID(),
                        sectionVersion.getId(),
                        index,
                        chunk,
                        "pending-embed",
                        metadata
                ));
                index++;
                chunkCount++;
            }
        }

        // Persist REMOVED markers against previous sections missing in new PDF
        for (Map.Entry<String, SectionDiffEngine.SectionSnapshot> entry : previous.entrySet()) {
            if (!current.containsKey(entry.getKey())) {
                sourceSectionRepository
                        .findBySourceMaterialIdAndExternalReference(version.getSourceMaterialId(), entry.getKey())
                        .ifPresent(section -> sourceSectionVersionRepository.save(new SourceSectionVersion(
                                UUID.randomUUID(),
                                section.getId(),
                                version.getId(),
                                "",
                                ContentHasher.sha256(""),
                                SectionChangeType.REMOVED,
                                Instant.now()
                        )));
            }
        }

        return new ParseResult(sectionCount, chunkCount, added, modified, previous.size());
    }

    private Map<String, SectionDiffEngine.SectionSnapshot> loadPreviousSnapshots(SourceMaterialVersion version) {
        return sourceMaterialVersionRepository
                .findFirstBySourceMaterialIdAndStatusAndIdNotOrderByVersionNumberDesc(
                        version.getSourceMaterialId(),
                        SourceVersionStatus.PUBLISHED,
                        version.getId()
                )
                .map(prev -> {
                    Map<String, SectionDiffEngine.SectionSnapshot> map = new LinkedHashMap<>();
                    for (SourceSectionVersion sv : sourceSectionVersionRepository.findBySourceVersionId(prev.getId())) {
                        if (sv.getChangeType() == SectionChangeType.REMOVED) {
                            continue;
                        }
                        SourceSection section = sourceSectionRepository.findById(sv.getSourceSectionId()).orElse(null);
                        if (section == null) {
                            continue;
                        }
                        map.put(
                                section.getExternalReference(),
                                new SectionDiffEngine.SectionSnapshot(
                                        section.getExternalReference(),
                                        section.getTitle(),
                                        sv.getContentHash()
                                )
                        );
                    }
                    return map;
                })
                .orElseGet(Map::of);
    }

    public record ParseResult(int sectionCount, int chunkCount, int added, int modified, int previousSectionCount) {
    }
}
