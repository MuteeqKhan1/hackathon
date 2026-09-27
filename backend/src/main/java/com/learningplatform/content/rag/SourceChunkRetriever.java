package com.learningplatform.content.rag;

import com.learningplatform.source.domain.SourceChunk;
import com.learningplatform.source.domain.SourceSection;
import com.learningplatform.source.domain.SourceSectionVersion;
import com.learningplatform.source.repository.SourceChunkRepository;
import com.learningplatform.source.repository.SourceSectionRepository;
import com.learningplatform.source.repository.SourceSectionVersionRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * v1 RAG: retrieve chunks for the course's current source version (top-k by order, no embeddings yet).
 */
@Component
public class SourceChunkRetriever {

    private static final int DEFAULT_TOP_K = 8;

    private final SourceSectionVersionRepository sectionVersionRepository;
    private final SourceSectionRepository sectionRepository;
    private final SourceChunkRepository chunkRepository;

    public SourceChunkRetriever(
            SourceSectionVersionRepository sectionVersionRepository,
            SourceSectionRepository sectionRepository,
            SourceChunkRepository chunkRepository
    ) {
        this.sectionVersionRepository = sectionVersionRepository;
        this.sectionRepository = sectionRepository;
        this.chunkRepository = chunkRepository;
    }

    public RetrievalResult retrieve(UUID sourceVersionId, int topK) {
        int limit = topK > 0 ? topK : DEFAULT_TOP_K;
        List<SourceSectionVersion> sectionVersions = sectionVersionRepository.findBySourceVersionId(sourceVersionId);
        Map<UUID, SourceSection> sections = sectionRepository.findAllById(
                sectionVersions.stream().map(SourceSectionVersion::getSourceSectionId).toList()
        ).stream().collect(Collectors.toMap(SourceSection::getId, Function.identity()));

        List<SourceSectionVersion> ordered = sectionVersions.stream()
                .sorted(Comparator.comparing(sv -> {
                    SourceSection s = sections.get(sv.getSourceSectionId());
                    return s != null ? s.getExternalReference() : sv.getId().toString();
                }))
                .toList();

        Map<UUID, UUID> sectionToVersion = new HashMap<>();
        Set<UUID> retrievedSectionIds = new LinkedHashSet<>();
        List<RetrievedChunk> chunks = new ArrayList<>();

        for (SourceSectionVersion sv : ordered) {
            sectionToVersion.put(sv.getSourceSectionId(), sv.getId());
            retrievedSectionIds.add(sv.getSourceSectionId());
            SourceSection section = sections.get(sv.getSourceSectionId());
            String title = section != null ? section.getTitle() : "Section";
            List<SourceChunk> sectionChunks = chunkRepository.findBySourceSectionVersionIdOrderByChunkIndexAsc(sv.getId());
            if (sectionChunks.isEmpty()) {
                chunks.add(new RetrievedChunk(sv.getSourceSectionId(), sv.getId(), title, sv.getContent()));
            } else {
                for (SourceChunk chunk : sectionChunks) {
                    chunks.add(new RetrievedChunk(sv.getSourceSectionId(), sv.getId(), title, chunk.getContent()));
                }
            }
            if (chunks.size() >= limit) {
                break;
            }
        }

        if (chunks.size() > limit) {
            chunks = chunks.subList(0, limit);
        }
        return new RetrievalResult(List.copyOf(chunks), Set.copyOf(retrievedSectionIds), Map.copyOf(sectionToVersion));
    }

    public record RetrievedChunk(UUID sectionId, UUID sectionVersionId, String sectionTitle, String text) {
    }

    public record RetrievalResult(
            List<RetrievedChunk> chunks,
            Set<UUID> retrievedSectionIds,
            Map<UUID, UUID> sectionIdToSectionVersionId
    ) {
    }
}
