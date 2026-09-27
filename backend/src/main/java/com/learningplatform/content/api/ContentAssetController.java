package com.learningplatform.content.api;

import com.learningplatform.content.domain.AssetType;
import com.learningplatform.content.domain.ContentAsset;
import com.learningplatform.content.domain.ContentAssetStatus;
import com.learningplatform.content.domain.ContentAssetVersion;
import com.learningplatform.content.domain.ContentSourceMapping;
import com.learningplatform.content.domain.GenerationMethod;
import com.learningplatform.content.domain.MappingRelationshipType;
import com.learningplatform.content.service.ContentGenerationFacade;
import com.learningplatform.content.service.ContentReviewService;
import com.learningplatform.operation.domain.AsyncOperation;
import com.learningplatform.operation.domain.OperationStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ContentAssetController {

    private final ContentGenerationFacade contentGenerationFacade;
    private final ContentReviewService contentReviewService;

    public ContentAssetController(
            ContentGenerationFacade contentGenerationFacade,
            ContentReviewService contentReviewService
    ) {
        this.contentGenerationFacade = contentGenerationFacade;
        this.contentReviewService = contentReviewService;
    }

    @PostMapping("/courses/{courseId}/topics/{topicId}/generate")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public OperationAcceptedResponse generate(
            @PathVariable UUID courseId,
            @PathVariable UUID topicId,
            @Valid @RequestBody GenerateRequest request
    ) {
        AsyncOperation op = contentGenerationFacade.startGenerate(courseId, topicId, request.assetTypes());
        return new OperationAcceptedResponse(op.getId(), op.getStatus(), op.getProgress());
    }

    @PostMapping("/content-assets/{assetId}/regenerate")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public OperationAcceptedResponse regenerate(@PathVariable UUID assetId) {
        AsyncOperation op = contentGenerationFacade.startRegenerate(assetId);
        return new OperationAcceptedResponse(op.getId(), op.getStatus(), op.getProgress());
    }

    @GetMapping("/content-assets/{assetId}")
    public ContentAssetDetailResponse get(@PathVariable UUID assetId) {
        ContentAsset asset = contentGenerationFacade.getAsset(assetId);
        ContentAssetVersion version = contentGenerationFacade.getCurrentVersion(asset);
        List<MappingResponse> mappings = contentGenerationFacade.mappingsForVersion(version.getId()).stream()
                .map(MappingResponse::from)
                .toList();
        return ContentAssetDetailResponse.from(asset, version, mappings);
    }

    @PatchMapping("/content-assets/{assetId}")
    public ContentAssetDetailResponse edit(
            @PathVariable UUID assetId,
            @Valid @RequestBody EditContentRequest request
    ) {
        contentReviewService.edit(assetId, request.contentJson());
        return get(assetId);
    }

    @PostMapping("/content-assets/{assetId}/approve")
    public ContentAssetDetailResponse approve(@PathVariable UUID assetId) {
        contentReviewService.approve(assetId);
        return get(assetId);
    }

    @PostMapping("/content-assets/{assetId}/reject")
    public ContentAssetDetailResponse reject(@PathVariable UUID assetId) {
        contentReviewService.reject(assetId);
        return get(assetId);
    }

    @GetMapping("/courses/{courseId}/topics/{topicId}/content-assets")
    public List<ContentAssetSummaryResponse> listForTopic(
            @PathVariable UUID courseId,
            @PathVariable UUID topicId
    ) {
        return contentGenerationFacade.listForTopic(courseId, topicId).stream()
                .map(ContentAssetSummaryResponse::from)
                .toList();
    }

    @GetMapping("/courses/{courseId}/content-assets/pending-review")
    public List<PendingReviewResponse> listPendingReview(@PathVariable UUID courseId) {
        return contentGenerationFacade.listPendingReview(courseId).stream()
                .map(PendingReviewResponse::from)
                .toList();
    }

    public record GenerateRequest(@NotEmpty List<AssetType> assetTypes) {
    }

    public record EditContentRequest(@NotBlank String contentJson) {
    }

    public record OperationAcceptedResponse(UUID operationId, OperationStatus status, int progress) {
    }

    public record ContentAssetSummaryResponse(
            UUID id,
            AssetType assetType,
            ContentAssetStatus status,
            UUID currentVersionId
    ) {
        static ContentAssetSummaryResponse from(ContentAsset asset) {
            return new ContentAssetSummaryResponse(
                    asset.getId(),
                    asset.getAssetType(),
                    asset.getStatus(),
                    asset.getCurrentVersionId()
            );
        }
    }

    public record PendingReviewResponse(
            UUID id,
            UUID topicId,
            String topicTitle,
            AssetType assetType,
            ContentAssetStatus status
    ) {
        static PendingReviewResponse from(ContentGenerationFacade.PendingReviewItem item) {
            return new PendingReviewResponse(
                    item.asset().getId(),
                    item.asset().getTopicId(),
                    item.topicTitle(),
                    item.asset().getAssetType(),
                    item.asset().getStatus()
            );
        }
    }

    public record ContentAssetDetailResponse(
            UUID id,
            UUID courseId,
            UUID topicId,
            AssetType assetType,
            ContentAssetStatus status,
            ContentAssetVersionResponse currentVersion,
            List<MappingResponse> mappings
    ) {
        static ContentAssetDetailResponse from(
                ContentAsset asset,
                ContentAssetVersion version,
                List<MappingResponse> mappings
        ) {
            return new ContentAssetDetailResponse(
                    asset.getId(),
                    asset.getCourseId(),
                    asset.getTopicId(),
                    asset.getAssetType(),
                    asset.getStatus(),
                    ContentAssetVersionResponse.from(version),
                    mappings
            );
        }
    }

    public record ContentAssetVersionResponse(
            UUID id,
            int versionNumber,
            String contentJson,
            String language,
            UUID sourceVersionId,
            GenerationMethod generationMethod,
            String promptVersion,
            String modelName,
            String modelVersion,
            ContentAssetStatus status,
            boolean manualModification,
            UUID approvedBy,
            Instant approvedAt,
            Instant createdAt
    ) {
        static ContentAssetVersionResponse from(ContentAssetVersion version) {
            return new ContentAssetVersionResponse(
                    version.getId(),
                    version.getVersionNumber(),
                    version.getContentJson(),
                    version.getLanguage(),
                    version.getSourceVersionId(),
                    version.getGenerationMethod(),
                    version.getPromptVersion(),
                    version.getModelName(),
                    version.getModelVersion(),
                    version.getStatus(),
                    version.isManualModification(),
                    version.getApprovedBy(),
                    version.getApprovedAt(),
                    version.getCreatedAt()
            );
        }
    }

    public record MappingResponse(
            UUID id,
            UUID sourceSectionId,
            UUID sourceSectionVersionId,
            MappingRelationshipType relationshipType
    ) {
        static MappingResponse from(ContentSourceMapping mapping) {
            return new MappingResponse(
                    mapping.getId(),
                    mapping.getSourceSectionId(),
                    mapping.getSourceSectionVersionId(),
                    mapping.getRelationshipType()
            );
        }
    }
}
