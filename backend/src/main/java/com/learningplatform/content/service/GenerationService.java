package com.learningplatform.content.service;

import com.learningplatform.ai.AiProvider;
import com.learningplatform.audit.service.AuditService;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.common.exception.NotFoundException;
import com.learningplatform.content.domain.AssetType;
import com.learningplatform.content.domain.LearningPace;
import com.learningplatform.content.domain.ContentAsset;
import com.learningplatform.content.domain.ContentAssetStatus;
import com.learningplatform.content.domain.ContentAssetVersion;
import com.learningplatform.content.domain.ContentSourceMapping;
import com.learningplatform.content.domain.GenerationMethod;
import com.learningplatform.content.domain.PromptVersion;
import com.learningplatform.content.domain.SyncPolicy;
import com.learningplatform.content.lineage.MappingWriter;
import com.learningplatform.content.prompt.PromptRegistry;
import com.learningplatform.content.rag.SourceChunkRetriever;
import com.learningplatform.content.repository.ContentAssetRepository;
import com.learningplatform.content.repository.ContentAssetVersionRepository;
import com.learningplatform.content.validation.GroundingValidator;
import com.learningplatform.content.validation.SchemaValidator;
import com.learningplatform.course.domain.Course;
import com.learningplatform.course.domain.CourseChapter;
import com.learningplatform.course.domain.CourseTopic;
import com.learningplatform.course.repository.CourseChapterRepository;
import com.learningplatform.course.repository.CourseRepository;
import com.learningplatform.course.repository.CourseTopicRepository;
import com.learningplatform.content.video.VideoMediaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class GenerationService {

    private final CourseRepository courseRepository;
    private final CourseChapterRepository chapterRepository;
    private final CourseTopicRepository topicRepository;
    private final ContentAssetRepository assetRepository;
    private final ContentAssetVersionRepository versionRepository;
    private final SourceChunkRetriever chunkRetriever;
    private final PromptRegistry promptRegistry;
    private final AiProvider aiProvider;
    private final SchemaValidator schemaValidator;
    private final GroundingValidator groundingValidator;
    private final MappingWriter mappingWriter;
    private final AuditService auditService;
    private final QuizMaterializer quizMaterializer;
    private final VideoMediaService videoMediaService;

    public GenerationService(
            CourseRepository courseRepository,
            CourseChapterRepository chapterRepository,
            CourseTopicRepository topicRepository,
            ContentAssetRepository assetRepository,
            ContentAssetVersionRepository versionRepository,
            SourceChunkRetriever chunkRetriever,
            PromptRegistry promptRegistry,
            AiProvider aiProvider,
            SchemaValidator schemaValidator,
            GroundingValidator groundingValidator,
            MappingWriter mappingWriter,
            AuditService auditService,
            QuizMaterializer quizMaterializer,
            VideoMediaService videoMediaService
    ) {
        this.courseRepository = courseRepository;
        this.chapterRepository = chapterRepository;
        this.topicRepository = topicRepository;
        this.assetRepository = assetRepository;
        this.versionRepository = versionRepository;
        this.chunkRetriever = chunkRetriever;
        this.promptRegistry = promptRegistry;
        this.aiProvider = aiProvider;
        this.schemaValidator = schemaValidator;
        this.groundingValidator = groundingValidator;
        this.mappingWriter = mappingWriter;
        this.auditService = auditService;
        this.quizMaterializer = quizMaterializer;
        this.videoMediaService = videoMediaService;
    }

    @Transactional
    public List<GeneratedAssetResult> generateForTopic(
            UUID courseId,
            UUID topicId,
            List<AssetType> assetTypes,
            UUID actorUserId
    ) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("COURSE_NOT_FOUND", "Course was not found."));
        CourseTopic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new NotFoundException("TOPIC_NOT_FOUND", "Topic was not found."));
        CourseChapter chapter = chapterRepository.findById(topic.getChapterId())
                .orElseThrow(() -> new NotFoundException("CHAPTER_NOT_FOUND", "Chapter was not found."));
        if (!chapter.getCourseId().equals(courseId)) {
            throw new NotFoundException("TOPIC_NOT_FOUND", "Topic does not belong to course.");
        }

        SourceChunkRetriever.RetrievalResult retrieval = chunkRetriever.retrieve(course.getCurrentSourceVersionId(), 8);
        if (retrieval.chunks().isEmpty()) {
            throw new BusinessException(
                    "NO_SOURCE_CHUNKS",
                    "No source chunks for this course's source version. Bind a published version with parsed sections."
            );
        }

        List<GeneratedAssetResult> results = new ArrayList<>();
        for (AssetType type : assetTypes) {
            results.add(generateOne(course, chapter, topic, type, retrieval, actorUserId, course.getCurrentSourceVersionId()));
        }
        return results;
    }

    @Transactional
    public GeneratedAssetResult regenerate(UUID assetId, UUID actorUserId) {
        return regenerate(assetId, actorUserId, null);
    }

    @Transactional
    public GeneratedAssetResult regenerate(UUID assetId, UUID actorUserId, UUID sourceVersionOverride) {
        ContentAsset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new NotFoundException("CONTENT_ASSET_NOT_FOUND", "Content asset was not found."));
        Course course = courseRepository.findById(asset.getCourseId())
                .orElseThrow(() -> new NotFoundException("COURSE_NOT_FOUND", "Course was not found."));
        CourseTopic topic = topicRepository.findById(asset.getTopicId())
                .orElseThrow(() -> new NotFoundException("TOPIC_NOT_FOUND", "Topic was not found."));
        CourseChapter chapter = chapterRepository.findById(asset.getChapterId())
                .orElseThrow(() -> new NotFoundException("CHAPTER_NOT_FOUND", "Chapter was not found."));

        UUID sourceVersionId = sourceVersionOverride != null
                ? sourceVersionOverride
                : course.getCurrentSourceVersionId();
        SourceChunkRetriever.RetrievalResult retrieval = chunkRetriever.retrieve(sourceVersionId, 8);
        if (retrieval.chunks().isEmpty()) {
            throw new BusinessException("NO_SOURCE_CHUNKS", "No source chunks for regeneration.");
        }
        return generateOne(course, chapter, topic, asset.getAssetType(), retrieval, actorUserId, sourceVersionId);
    }

    private GeneratedAssetResult generateOne(
            Course course,
            CourseChapter chapter,
            CourseTopic topic,
            AssetType type,
            SourceChunkRetriever.RetrievalResult retrieval,
            UUID actorUserId,
            UUID sourceVersionId
    ) {
        String promptKey = switch (type) {
            case EXPLANATION -> PromptRegistry.EXPLANATION_GENERATION;
            case QUIZ -> PromptRegistry.QUIZ_GENERATION;
            case VIDEO -> PromptRegistry.VIDEO_GENERATION;
        };
        PromptVersion prompt = promptRegistry.requireLatest(promptKey);
        String system = promptRegistry.render(prompt, topic.getTitle());
        String userPrompt = buildUserPrompt(topic.getTitle(), course.getLanguage(), LearningPace.MEDIUM.name(), retrieval);

        AiProvider.GenerationResponse aiResponse;
        try {
            aiResponse = aiProvider.generate(new AiProvider.GenerationRequest(
                    promptKey,
                    prompt.getVersion(),
                    system,
                    userPrompt,
                    type.name()
            ));
        } catch (RuntimeException ex) {
            throw new BusinessException("AI_PROVIDER_FAILED", ex.getMessage() != null ? ex.getMessage() : "AI provider failed");
        }

        SchemaValidator.ValidatedPayload validated = schemaValidator.validate(type, aiResponse.contentJson());
        groundingValidator.validateCitedSubset(validated.citedSectionIds(), retrieval.retrievedSectionIds());

        Instant now = Instant.now();
        ContentAsset asset = assetRepository.findByTopicIdAndAssetType(topic.getId(), type)
                .orElseGet(() -> assetRepository.save(new ContentAsset(
                        UUID.randomUUID(),
                        course.getOrganizationId(),
                        course.getId(),
                        chapter.getId(),
                        topic.getId(),
                        type,
                        ContentAssetStatus.GENERATED,
                        actorUserId,
                        now,
                        now
                )));

        String contentJson = validated.contentJson();
        if (type == AssetType.VIDEO) {
            String key = VideoMediaService.keyFor(asset.getId().toString(), course.getLanguage(), LearningPace.MEDIUM.name());
            contentJson = videoMediaService.renderAndAttach(contentJson, key);
        }

        int nextVersion = versionRepository.findMaxVersionNumber(asset.getId()) + 1;
        ContentAssetVersion version = new ContentAssetVersion(
                UUID.randomUUID(),
                asset.getId(),
                nextVersion,
                contentJson,
                course.getLanguage(),
                sourceVersionId,
                GenerationMethod.AI,
                aiResponse.promptVersion() != null ? aiResponse.promptVersion() : prompt.getVersion(),
                aiResponse.modelName(),
                aiResponse.modelVersion(),
                ContentAssetStatus.PENDING_REVIEW,
                false,
                SyncPolicy.AUTO.name(),
                actorUserId,
                now
        );
        versionRepository.save(version);

        asset.setCurrentVersion(version.getId(), ContentAssetStatus.PENDING_REVIEW);
        assetRepository.save(asset);

        List<ContentSourceMapping> mappings = mappingWriter.write(
                asset.getId(),
                version.getId(),
                validated.citedSectionIds(),
                retrieval.sectionIdToSectionVersionId()
        );

        if (type == AssetType.QUIZ) {
            quizMaterializer.materialize(asset.getId(), version.getId(), validated.contentJson());
        }

        auditService.record(
                "CONTENT_GENERATED",
                "ContentAsset",
                asset.getId().toString(),
                type.name() + ":v" + nextVersion + ":mappings=" + mappings.size()
                        + (type == AssetType.VIDEO ? ":mp4=true" : "")
        );

        return new GeneratedAssetResult(asset, version, mappings.size());
    }

    private static String buildUserPrompt(
            String topicTitle,
            String language,
            String pace,
            SourceChunkRetriever.RetrievalResult retrieval
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("Topic: ").append(topicTitle).append('\n');
        sb.append("Language: ").append(language == null ? "en" : language).append('\n');
        sb.append("Pace: ").append(pace == null ? "MEDIUM" : pace).append("\n\n");
        sb.append("Source chunks:\n");
        for (SourceChunkRetriever.RetrievedChunk chunk : retrieval.chunks()) {
            sb.append("[sectionId=").append(chunk.sectionId()).append("] title=")
                    .append(chunk.sectionTitle()).append('\n')
                    .append(chunk.text()).append("\n\n");
        }
        return sb.toString();
    }

    public record GeneratedAssetResult(ContentAsset asset, ContentAssetVersion version, int mappingCount) {
    }
}
