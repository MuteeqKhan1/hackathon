package com.learningplatform.content.service;

import com.learningplatform.common.domain.Permission;
import com.learningplatform.common.exception.NotFoundException;
import com.learningplatform.common.security.AuthenticatedUser;
import com.learningplatform.common.security.PermissionGuard;
import com.learningplatform.content.domain.AssetType;
import com.learningplatform.content.domain.ContentAsset;
import com.learningplatform.content.domain.ContentAssetStatus;
import com.learningplatform.content.domain.ContentAssetVersion;
import com.learningplatform.content.domain.ContentSourceMapping;
import com.learningplatform.content.repository.ContentAssetRepository;
import com.learningplatform.content.repository.ContentAssetVersionRepository;
import com.learningplatform.content.repository.ContentSourceMappingRepository;
import com.learningplatform.course.domain.Course;
import com.learningplatform.course.domain.CourseChapter;
import com.learningplatform.course.domain.CourseTopic;
import com.learningplatform.course.repository.CourseChapterRepository;
import com.learningplatform.course.repository.CourseTopicRepository;
import com.learningplatform.course.service.CourseService;
import com.learningplatform.operation.domain.AsyncOperation;
import com.learningplatform.operation.service.OperationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.UUID;

@Service
public class ContentGenerationFacade {

    private final CourseService courseService;
    private final CourseChapterRepository chapterRepository;
    private final CourseTopicRepository topicRepository;
    private final ContentAssetRepository assetRepository;
    private final ContentAssetVersionRepository versionRepository;
    private final ContentSourceMappingRepository mappingRepository;
    private final OperationService operationService;
    private final ContentGenerateWorker contentGenerateWorker;

    public ContentGenerationFacade(
            CourseService courseService,
            CourseChapterRepository chapterRepository,
            CourseTopicRepository topicRepository,
            ContentAssetRepository assetRepository,
            ContentAssetVersionRepository versionRepository,
            ContentSourceMappingRepository mappingRepository,
            OperationService operationService,
            ContentGenerateWorker contentGenerateWorker
    ) {
        this.courseService = courseService;
        this.chapterRepository = chapterRepository;
        this.topicRepository = topicRepository;
        this.assetRepository = assetRepository;
        this.versionRepository = versionRepository;
        this.mappingRepository = mappingRepository;
        this.operationService = operationService;
        this.contentGenerateWorker = contentGenerateWorker;
    }

    @Transactional
    public AsyncOperation startGenerate(UUID courseId, UUID topicId, List<AssetType> assetTypes) {
        AuthenticatedUser user = PermissionGuard.require(Permission.CONTENT_GENERATE);
        Course course = courseService.get(courseId);
        CourseTopic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new NotFoundException("TOPIC_NOT_FOUND", "Topic was not found."));
        CourseChapter chapter = chapterRepository.findById(topic.getChapterId())
                .orElseThrow(() -> new NotFoundException("CHAPTER_NOT_FOUND", "Chapter was not found."));
        if (!chapter.getCourseId().equals(course.getId())) {
            throw new NotFoundException("TOPIC_NOT_FOUND", "Topic does not belong to course.");
        }
        if (assetTypes == null || assetTypes.isEmpty()) {
            throw new IllegalArgumentException("assetTypes must not be empty");
        }

        AsyncOperation operation = operationService.create("CONTENT_GENERATE");
        UUID operationId = operation.getId();
        UUID actorId = user.userId();
        List<AssetType> types = List.copyOf(assetTypes);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    contentGenerateWorker.processTopic(operationId, courseId, topicId, types, actorId);
                }
            });
        } else {
            contentGenerateWorker.processTopic(operationId, courseId, topicId, types, actorId);
        }
        return operation;
    }

    @Transactional
    public AsyncOperation startRegenerate(UUID assetId) {
        AuthenticatedUser user = PermissionGuard.require(Permission.CONTENT_GENERATE);
        ContentAsset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new NotFoundException("CONTENT_ASSET_NOT_FOUND", "Content asset was not found."));
        PermissionGuard.requireSameOrganization(asset.getOrganizationId());

        AsyncOperation operation = operationService.create("CONTENT_REGENERATE");
        UUID operationId = operation.getId();
        UUID actorId = user.userId();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    contentGenerateWorker.processRegenerate(operationId, assetId, actorId);
                }
            });
        } else {
            contentGenerateWorker.processRegenerate(operationId, assetId, actorId);
        }
        return operation;
    }

    @Transactional(readOnly = true)
    public ContentAsset getAsset(UUID assetId) {
        PermissionGuard.require(Permission.CONTENT_EDIT);
        ContentAsset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new NotFoundException("CONTENT_ASSET_NOT_FOUND", "Content asset was not found."));
        PermissionGuard.requireSameOrganization(asset.getOrganizationId());
        return asset;
    }

    @Transactional(readOnly = true)
    public ContentAssetVersion getCurrentVersion(ContentAsset asset) {
        if (asset.getCurrentVersionId() == null) {
            throw new NotFoundException("CONTENT_VERSION_NOT_FOUND", "Asset has no current version.");
        }
        return versionRepository.findById(asset.getCurrentVersionId())
                .orElseThrow(() -> new NotFoundException("CONTENT_VERSION_NOT_FOUND", "Asset version was not found."));
    }

    @Transactional(readOnly = true)
    public List<ContentSourceMapping> mappingsForVersion(UUID versionId) {
        return mappingRepository.findByContentAssetVersionId(versionId);
    }

    @Transactional(readOnly = true)
    public List<ContentAsset> listForTopic(UUID courseId, UUID topicId) {
        courseService.get(courseId);
        return assetRepository.findByTopicIdOrderByCreatedAtAsc(topicId);
    }

    @Transactional(readOnly = true)
    public List<PendingReviewItem> listPendingReview(UUID courseId) {
        courseService.get(courseId);
        return assetRepository.findByCourseIdAndStatusOrderByUpdatedAtDesc(courseId, ContentAssetStatus.PENDING_REVIEW)
                .stream()
                .map(asset -> {
                    String topicTitle = topicRepository.findById(asset.getTopicId())
                            .map(CourseTopic::getTitle)
                            .orElse("Topic");
                    return new PendingReviewItem(asset, topicTitle);
                })
                .toList();
    }

    public record PendingReviewItem(ContentAsset asset, String topicTitle) {
    }
}
