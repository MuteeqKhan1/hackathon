package com.learningplatform.source.service;

import com.learningplatform.common.domain.Permission;
import com.learningplatform.common.security.PermissionGuard;
import com.learningplatform.source.domain.SourceChunk;
import com.learningplatform.source.domain.SourceSection;
import com.learningplatform.source.domain.SourceSectionVersion;
import com.learningplatform.source.parsing.SectionChangeType;
import com.learningplatform.source.repository.SourceChunkRepository;
import com.learningplatform.source.repository.SourceSectionRepository;
import com.learningplatform.source.repository.SourceSectionVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SourceSectionQueryService {

    private final SourceMaterialService sourceMaterialService;
    private final SourceSectionVersionRepository sourceSectionVersionRepository;
    private final SourceSectionRepository sourceSectionRepository;
    private final SourceChunkRepository sourceChunkRepository;

    public SourceSectionQueryService(
            SourceMaterialService sourceMaterialService,
            SourceSectionVersionRepository sourceSectionVersionRepository,
            SourceSectionRepository sourceSectionRepository,
            SourceChunkRepository sourceChunkRepository
    ) {
        this.sourceMaterialService = sourceMaterialService;
        this.sourceSectionVersionRepository = sourceSectionVersionRepository;
        this.sourceSectionRepository = sourceSectionRepository;
        this.sourceChunkRepository = sourceChunkRepository;
    }

    @Transactional(readOnly = true)
    public List<SectionView> listSections(UUID materialId, UUID versionId) {
        PermissionGuard.require(Permission.SOURCE_VERSION_VIEW);
        sourceMaterialService.get(materialId);

        List<SourceSectionVersion> versions = sourceSectionVersionRepository.findBySourceVersionId(versionId);
        Map<UUID, SourceSection> sections = sourceSectionRepository.findAllById(
                versions.stream().map(SourceSectionVersion::getSourceSectionId).toList()
        ).stream().collect(Collectors.toMap(SourceSection::getId, Function.identity()));

        List<SectionView> views = new ArrayList<>();
        for (SourceSectionVersion sv : versions) {
            SourceSection section = sections.get(sv.getSourceSectionId());
            if (section == null) {
                continue;
            }
            List<SourceChunk> chunks = sourceChunkRepository.findBySourceSectionVersionIdOrderByChunkIndexAsc(sv.getId());
            views.add(new SectionView(
                    section.getId(),
                    section.getExternalReference(),
                    section.getTitle(),
                    section.getSectionType(),
                    sv.getContentHash(),
                    sv.getChangeType(),
                    sv.getContent().length() > 280 ? sv.getContent().substring(0, 280) + "…" : sv.getContent(),
                    chunks.size()
            ));
        }
        return views;
    }

    public record SectionView(
            UUID sectionId,
            String externalReference,
            String title,
            String sectionType,
            String contentHash,
            SectionChangeType changeType,
            String contentPreview,
            int chunkCount
    ) {
    }
}
