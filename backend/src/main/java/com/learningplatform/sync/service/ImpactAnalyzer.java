package com.learningplatform.sync.service;

import com.learningplatform.audit.service.AuditService;
import com.learningplatform.content.domain.ContentAsset;
import com.learningplatform.content.domain.ContentAssetVersion;
import com.learningplatform.content.domain.ContentSourceMapping;
import com.learningplatform.content.domain.MappingRelationshipType;
import com.learningplatform.content.domain.SyncPolicy;
import com.learningplatform.content.lineage.RecommendationFilter;
import com.learningplatform.content.repository.ContentAssetRepository;
import com.learningplatform.content.repository.ContentAssetVersionRepository;
import com.learningplatform.content.repository.ContentSourceMappingRepository;
import com.learningplatform.course.domain.Course;
import com.learningplatform.course.repository.CourseRepository;
import com.learningplatform.sync.domain.ImpactAnalysis;
import com.learningplatform.sync.domain.ImpactStatus;
import com.learningplatform.sync.domain.RecommendedAction;
import com.learningplatform.sync.domain.SyncEvent;
import com.learningplatform.sync.repository.ImpactAnalysisRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Builds instructor-facing impact rows: one OPEN impact per content asset per sync event
 * (merged across all cited source sections that changed).
 */
@Service
public class ImpactAnalyzer {

    private final ContentSourceMappingRepository mappingRepository;
    private final ContentAssetRepository assetRepository;
    private final ContentAssetVersionRepository versionRepository;
    private final CourseRepository courseRepository;
    private final ImpactAnalysisRepository impactAnalysisRepository;
    private final RecommendationFilter recommendationFilter;
    private final AuditService auditService;

    public ImpactAnalyzer(
            ContentSourceMappingRepository mappingRepository,
            ContentAssetRepository assetRepository,
            ContentAssetVersionRepository versionRepository,
            CourseRepository courseRepository,
            ImpactAnalysisRepository impactAnalysisRepository,
            RecommendationFilter recommendationFilter,
            AuditService auditService
    ) {
        this.mappingRepository = mappingRepository;
        this.assetRepository = assetRepository;
        this.versionRepository = versionRepository;
        this.courseRepository = courseRepository;
        this.impactAnalysisRepository = impactAnalysisRepository;
        this.recommendationFilter = recommendationFilter;
        this.auditService = auditService;
    }

    @Transactional
    public List<ImpactAnalysis> analyze(SyncEvent event, List<ChangeDetectionService.DetectedChange> changes) {
        Map<UUID, ImpactAnalysis> byAsset = new LinkedHashMap<>();
        int sectionHits = 0;

        for (ChangeDetectionService.DetectedChange change : changes) {
            List<ContentSourceMapping> mappings = mappingRepository.findBySourceSectionId(change.sourceSectionId());
            for (ContentSourceMapping mapping : mappings) {
                ContentAsset asset = assetRepository.findById(mapping.getContentAssetId()).orElse(null);
                if (asset == null || !asset.getOrganizationId().equals(event.getOrganizationId())) {
                    continue;
                }
                Course course = courseRepository.findById(asset.getCourseId()).orElse(null);
                if (course == null || !course.getSourceMaterialId().equals(event.getSourceMaterialId())) {
                    continue;
                }

                ContentAssetVersion current = asset.getCurrentVersionId() != null
                        ? versionRepository.findById(asset.getCurrentVersionId()).orElse(null)
                        : null;
                SyncPolicy policy = current != null ? current.getSyncPolicyEnum() : SyncPolicy.AUTO;
                MappingRelationshipType rel = mapping.getRelationshipType();

                String previousSignature = lastSignature(asset.getId());
                if (!recommendationFilter.includeForAutoRegenerate(
                        rel, policy, previousSignature, change.changeSignature()
                )) {
                    continue;
                }

                RecommendedAction recommended = RecommendedAction.REGENERATE;
                String reason = change.reason();
                if (current != null && current.isManualModification()) {
                    recommended = RecommendedAction.REVIEW_CONFLICT;
                    reason = "manual_modification=true; " + reason;
                }

                sectionHits++;
                ImpactAnalysis existing = byAsset.get(asset.getId());
                if (existing == null) {
                    byAsset.put(asset.getId(), new ImpactAnalysis(
                            UUID.randomUUID(),
                            event.getId(),
                            course.getId(),
                            asset.getId(),
                            change.sourceSectionId(),
                            change.impactLevel(),
                            reason,
                            recommended,
                            change.changeSignature(),
                            ImpactStatus.OPEN,
                            Instant.now()
                    ));
                } else {
                    existing.mergeFrom(change.impactLevel(), reason, recommended, change.changeSignature());
                }
            }
        }

        List<ImpactAnalysis> created = new ArrayList<>();
        for (ImpactAnalysis impact : byAsset.values()) {
            created.add(impactAnalysisRepository.save(impact));
        }

        if (!created.isEmpty()) {
            auditService.record(
                    "IMPACT_IDENTIFIED",
                    "SyncEvent",
                    event.getId().toString(),
                    "assets=" + created.size() + ";sectionHits=" + sectionHits
            );
        }
        return created;
    }

    private String lastSignature(UUID assetId) {
        List<ImpactAnalysis> prior = impactAnalysisRepository
                .findByContentAssetIdAndStatusOrderByCreatedAtDesc(assetId, ImpactStatus.CLOSED);
        if (prior.isEmpty()) {
            prior = impactAnalysisRepository.findByContentAssetIdAndStatusOrderByCreatedAtDesc(assetId, ImpactStatus.OPEN);
        }
        return prior.isEmpty() ? null : prior.get(0).getChangeSignature();
    }
}
