package com.learningplatform.content.service;

import com.learningplatform.ai.AiProvider;
import com.learningplatform.audit.service.AuditService;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.content.domain.AssetType;
import com.learningplatform.content.domain.ContentAsset;
import com.learningplatform.content.domain.ContentAssetVersion;
import com.learningplatform.content.domain.PromptVersion;
import com.learningplatform.content.lineage.MappingWriter;
import com.learningplatform.content.prompt.PromptRegistry;
import com.learningplatform.content.rag.SourceChunkRetriever;
import com.learningplatform.content.repository.ContentAssetRepository;
import com.learningplatform.content.repository.ContentAssetVersionRepository;
import com.learningplatform.content.validation.GroundingValidator;
import com.learningplatform.content.validation.SchemaValidator;
import com.learningplatform.course.domain.Course;
import com.learningplatform.course.domain.CourseChapter;
import com.learningplatform.course.domain.CourseStatus;
import com.learningplatform.course.domain.CourseTopic;
import com.learningplatform.course.domain.StructureNodeStatus;
import com.learningplatform.course.repository.CourseChapterRepository;
import com.learningplatform.course.repository.CourseRepository;
import com.learningplatform.course.repository.CourseTopicRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerationServiceTest {

    private static final UUID COURSE_ID = UUID.randomUUID();
    private static final UUID CHAPTER_ID = UUID.randomUUID();
    private static final UUID TOPIC_ID = UUID.randomUUID();
    private static final UUID ORG = UUID.randomUUID();
    private static final UUID USER = UUID.randomUUID();
    private static final UUID SOURCE_VERSION = UUID.randomUUID();
    private static final UUID SECTION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SECTION_VERSION_ID = UUID.randomUUID();

    @Mock private CourseRepository courseRepository;
    @Mock private CourseChapterRepository chapterRepository;
    @Mock private CourseTopicRepository topicRepository;
    @Mock private ContentAssetRepository assetRepository;
    @Mock private ContentAssetVersionRepository versionRepository;
    @Mock private SourceChunkRetriever chunkRetriever;
    @Mock private PromptRegistry promptRegistry;
    @Mock private AiProvider aiProvider;
    @Mock private SchemaValidator schemaValidator;
    @Mock private GroundingValidator groundingValidator;
    @Mock private MappingWriter mappingWriter;
    @Mock private AuditService auditService;
    @Mock private QuizMaterializer quizMaterializer;
    @Mock private com.learningplatform.content.video.VideoMediaService videoMediaService;

    @InjectMocks
    private GenerationService generationService;

    @Test
    void fakeAiProviderSuccess_savesAssetVersionAndMappings() {
        stubCourseGraph();
        stubRetrieval();
        PromptVersion prompt = new PromptVersion(UUID.randomUUID(), "EXPLANATION_GENERATION", "1.0.0", "t", Instant.now());
        when(promptRegistry.requireLatest(PromptRegistry.EXPLANATION_GENERATION)).thenReturn(prompt);
        when(promptRegistry.render(eq(prompt), any())).thenReturn("system");
        when(aiProvider.generate(any())).thenReturn(new AiProvider.GenerationResponse(
                "{}", "heuristic", "heuristic-rag", "1.0.0", "1.0.0"
        ));
        when(schemaValidator.validate(eq(AssetType.EXPLANATION), any()))
                .thenReturn(new SchemaValidator.ValidatedPayload("{}", List.of(SECTION_ID)));
        when(assetRepository.findByTopicIdAndAssetType(TOPIC_ID, AssetType.EXPLANATION)).thenReturn(Optional.empty());
        when(assetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(versionRepository.findMaxVersionNumber(any())).thenReturn(0);
        when(versionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mappingWriter.write(any(), any(), anyList(), anyMap())).thenReturn(List.of());

        List<GenerationService.GeneratedAssetResult> results = generationService.generateForTopic(
                COURSE_ID, TOPIC_ID, List.of(AssetType.EXPLANATION), USER
        );

        assertThat(results).hasSize(1);
        assertThat(results.get(0).version().getVersionNumber()).isEqualTo(1);
        verify(mappingWriter).write(any(), any(), eq(List.of(SECTION_ID)), anyMap());
        verify(versionRepository).save(any(ContentAssetVersion.class));
    }

    @Test
    void aiProviderThrows_noVersionSaved() {
        stubCourseGraph();
        stubRetrieval();
        PromptVersion prompt = new PromptVersion(UUID.randomUUID(), "EXPLANATION_GENERATION", "1.0.0", "t", Instant.now());
        when(promptRegistry.requireLatest(any())).thenReturn(prompt);
        when(promptRegistry.render(any(), any())).thenReturn("system");
        when(aiProvider.generate(any())).thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> generationService.generateForTopic(
                COURSE_ID, TOPIC_ID, List.of(AssetType.EXPLANATION), USER
        )).isInstanceOf(BusinessException.class);

        verify(versionRepository, never()).save(any());
    }

    @Test
    void regenerate_incrementsVersion_keepsV1() {
        stubCourseGraph();
        stubRetrieval();
        ContentAsset existing = new ContentAsset(
                UUID.randomUUID(), ORG, COURSE_ID, CHAPTER_ID, TOPIC_ID,
                AssetType.EXPLANATION, com.learningplatform.content.domain.ContentAssetStatus.PENDING_REVIEW,
                USER, Instant.now(), Instant.now()
        );
        when(assetRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(assetRepository.findByTopicIdAndAssetType(TOPIC_ID, AssetType.EXPLANATION)).thenReturn(Optional.of(existing));
        when(assetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(versionRepository.findMaxVersionNumber(existing.getId())).thenReturn(1);
        when(versionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PromptVersion prompt = new PromptVersion(UUID.randomUUID(), "EXPLANATION_GENERATION", "1.0.0", "t", Instant.now());
        when(promptRegistry.requireLatest(any())).thenReturn(prompt);
        when(promptRegistry.render(any(), any())).thenReturn("system");
        when(aiProvider.generate(any())).thenReturn(new AiProvider.GenerationResponse(
                "{}", "heuristic", "heuristic-rag", "1.0.0", "1.0.0"
        ));
        when(schemaValidator.validate(any(), any()))
                .thenReturn(new SchemaValidator.ValidatedPayload("{}", List.of(SECTION_ID)));
        when(mappingWriter.write(any(), any(), anyList(), anyMap())).thenReturn(List.of());

        GenerationService.GeneratedAssetResult result = generationService.regenerate(existing.getId(), USER);

        assertThat(result.version().getVersionNumber()).isEqualTo(2);
        assertThat(result.version().getContentAssetId()).isEqualTo(existing.getId());
    }

    private void stubCourseGraph() {
        Course course = new Course(
                COURSE_ID, ORG, "Physics", null, UUID.randomUUID(), SOURCE_VERSION,
                "en", null, null, null, CourseStatus.DRAFT, USER, Instant.now(), Instant.now()
        );
        CourseChapter chapter = new CourseChapter(CHAPTER_ID, COURSE_ID, "Ch1", 1, StructureNodeStatus.DRAFT);
        CourseTopic topic = new CourseTopic(TOPIC_ID, CHAPTER_ID, "Newton", 1, StructureNodeStatus.DRAFT);
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
        when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(topic));
        when(chapterRepository.findById(CHAPTER_ID)).thenReturn(Optional.of(chapter));
    }

    private void stubRetrieval() {
        when(chunkRetriever.retrieve(eq(SOURCE_VERSION), anyInt())).thenReturn(
                new SourceChunkRetriever.RetrievalResult(
                        List.of(new SourceChunkRetriever.RetrievedChunk(
                                SECTION_ID, SECTION_VERSION_ID, "Newton", "F = ma"
                        )),
                        Set.of(SECTION_ID),
                        Map.of(SECTION_ID, SECTION_VERSION_ID)
                )
        );
    }
}
