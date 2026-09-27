package com.learningplatform.sync.service;

import com.learningplatform.audit.service.AuditService;
import com.learningplatform.content.domain.AssetType;
import com.learningplatform.content.domain.ContentAsset;
import com.learningplatform.content.domain.ContentAssetStatus;
import com.learningplatform.content.domain.ContentAssetVersion;
import com.learningplatform.content.domain.ContentSourceMapping;
import com.learningplatform.content.domain.GenerationMethod;
import com.learningplatform.content.domain.MappingRelationshipType;
import com.learningplatform.content.domain.SyncPolicy;
import com.learningplatform.content.lineage.RecommendationFilter;
import com.learningplatform.content.repository.ContentAssetRepository;
import com.learningplatform.content.repository.ContentAssetVersionRepository;
import com.learningplatform.content.repository.ContentSourceMappingRepository;
import com.learningplatform.course.domain.Course;
import com.learningplatform.course.domain.CourseStatus;
import com.learningplatform.course.repository.CourseRepository;
import com.learningplatform.source.parsing.SectionChangeType;
import com.learningplatform.sync.domain.ImpactAnalysis;
import com.learningplatform.sync.domain.ImpactLevel;
import com.learningplatform.sync.domain.ImpactStatus;
import com.learningplatform.sync.domain.SyncEvent;
import com.learningplatform.sync.domain.SyncEventStatus;
import com.learningplatform.sync.repository.ImpactAnalysisRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImpactAnalyzerTest {

    @Mock private ContentSourceMappingRepository mappingRepository;
    @Mock private ContentAssetRepository assetRepository;
    @Mock private ContentAssetVersionRepository versionRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private ImpactAnalysisRepository impactAnalysisRepository;
    @Mock private AuditService auditService;
    @Spy private RecommendationFilter recommendationFilter = new RecommendationFilter();

    @InjectMocks
    private ImpactAnalyzer analyzer;

    @Test
    void sectionWithTwoAssets_createsTwoImpacts() {
        UUID org = UUID.randomUUID();
        UUID material = UUID.randomUUID();
        UUID section = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID asset1 = UUID.randomUUID();
        UUID asset2 = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        SyncEvent event = new SyncEvent(
                eventId, org, material, UUID.randomUUID(), UUID.randomUUID(),
                SyncEventStatus.ANALYZING, "key", Instant.now()
        );
        Course course = course(courseId, org, material);
        ContentAsset a1 = asset(asset1, org, courseId, AssetType.EXPLANATION);
        ContentAsset a2 = asset(asset2, org, courseId, AssetType.QUIZ);

        when(mappingRepository.findBySourceSectionId(section)).thenReturn(List.of(
                mapping(asset1, section),
                mapping(asset2, section)
        ));
        when(assetRepository.findById(asset1)).thenReturn(Optional.of(a1));
        when(assetRepository.findById(asset2)).thenReturn(Optional.of(a2));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(impactAnalysisRepository.findByContentAssetIdAndStatusOrderByCreatedAtDesc(any(), any()))
                .thenReturn(List.of());
        when(impactAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ChangeDetectionService.DetectedChange change = new ChangeDetectionService.DetectedChange(
                section, SectionChangeType.MODIFIED, "old", "new", ImpactLevel.HIGH, "HIGH", "sig-1"
        );

        List<ImpactAnalysis> impacts = analyzer.analyze(event, List.of(change));

        assertThat(impacts).hasSize(2);
    }

    @Test
    void sameAssetManyRemovedSections_createsOneImpact() {
        UUID org = UUID.randomUUID();
        UUID material = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        UUID section1 = UUID.randomUUID();
        UUID section2 = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        SyncEvent event = new SyncEvent(
                eventId, org, material, UUID.randomUUID(), UUID.randomUUID(),
                SyncEventStatus.ANALYZING, "key", Instant.now()
        );
        Course course = course(courseId, org, material);
        ContentAsset asset = asset(assetId, org, courseId, AssetType.EXPLANATION);

        when(mappingRepository.findBySourceSectionId(section1)).thenReturn(List.of(mapping(assetId, section1)));
        when(mappingRepository.findBySourceSectionId(section2)).thenReturn(List.of(mapping(assetId, section2)));
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(impactAnalysisRepository.findByContentAssetIdAndStatusOrderByCreatedAtDesc(any(), any()))
                .thenReturn(List.of());
        when(impactAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<ImpactAnalysis> impacts = analyzer.analyze(event, List.of(
                new ChangeDetectionService.DetectedChange(
                        section1, SectionChangeType.REMOVED, "old", null, ImpactLevel.HIGH, "REMOVED scored HIGH", "sig-a"
                ),
                new ChangeDetectionService.DetectedChange(
                        section2, SectionChangeType.REMOVED, "old2", null, ImpactLevel.HIGH, "REMOVED scored HIGH", "sig-b"
                )
        ));

        assertThat(impacts).hasSize(1);
        assertThat(impacts.get(0).getContentAssetId()).isEqualTo(assetId);
        assertThat(impacts.get(0).getImpactLevel()).isEqualTo(ImpactLevel.HIGH);
    }

    @Test
    void noLineage_zeroImpacts() {
        UUID org = UUID.randomUUID();
        SyncEvent event = new SyncEvent(
                UUID.randomUUID(), org, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                SyncEventStatus.ANALYZING, "key", Instant.now()
        );
        UUID section = UUID.randomUUID();
        when(mappingRepository.findBySourceSectionId(section)).thenReturn(List.of());

        List<ImpactAnalysis> impacts = analyzer.analyze(event, List.of(
                new ChangeDetectionService.DetectedChange(
                        section, SectionChangeType.MODIFIED, "a", "b", ImpactLevel.MEDIUM, "m", "sig"
                )
        ));

        assertThat(impacts).isEmpty();
    }

    private static Course course(UUID id, UUID org, UUID material) {
        return new Course(
                id, org, "C", null, material, UUID.randomUUID(), "en", null, null, BigDecimal.ONE,
                CourseStatus.PUBLISHED, UUID.randomUUID(), Instant.now(), Instant.now()
        );
    }

    private static ContentAsset asset(UUID id, UUID org, UUID courseId, AssetType type) {
        ContentAsset a = new ContentAsset(
                id, org, courseId, UUID.randomUUID(), UUID.randomUUID(),
                type, ContentAssetStatus.APPROVED, UUID.randomUUID(), Instant.now(), Instant.now()
        );
        a.setCurrentVersion(UUID.randomUUID(), ContentAssetStatus.APPROVED);
        return a;
    }

    private static ContentSourceMapping mapping(UUID assetId, UUID sectionId) {
        return new ContentSourceMapping(
                UUID.randomUUID(), assetId, UUID.randomUUID(), sectionId, UUID.randomUUID(),
                MappingRelationshipType.PRIMARY
        );
    }
}
