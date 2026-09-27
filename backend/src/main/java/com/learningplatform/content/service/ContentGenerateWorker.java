package com.learningplatform.content.service;

import com.learningplatform.content.domain.AssetType;
import com.learningplatform.operation.service.OperationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ContentGenerateWorker {

    private static final Logger log = LoggerFactory.getLogger(ContentGenerateWorker.class);

    private final GenerationService generationService;
    private final OperationService operationService;

    public ContentGenerateWorker(GenerationService generationService, OperationService operationService) {
        this.generationService = generationService;
        this.operationService = operationService;
    }

    @Async
    @Transactional
    public void processTopic(UUID operationId, UUID courseId, UUID topicId, List<AssetType> types, UUID actorUserId) {
        try {
            operationService.markRunning(operationId, 15);
            List<GenerationService.GeneratedAssetResult> results =
                    generationService.generateForTopic(courseId, topicId, types, actorUserId);
            operationService.markRunning(operationId, 90);
            String summary = results.stream()
                    .map(r -> r.asset().getAssetType() + ":v" + r.version().getVersionNumber()
                            + ":maps=" + r.mappingCount())
                    .collect(Collectors.joining(","));
            operationService.markCompleted(
                    operationId,
                    "{\"courseId\":\"" + courseId + "\",\"topicId\":\"" + topicId
                            + "\",\"assets\":\"" + summary + "\"}"
            );
        } catch (Exception ex) {
            log.warn("Content generation failed for topic {}: {}", topicId, ex.getMessage());
            String message = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            operationService.markFailed(operationId, "CONTENT_GENERATE_FAILED", message);
        }
    }

    @Async
    @Transactional
    public void processRegenerate(UUID operationId, UUID assetId, UUID actorUserId) {
        try {
            operationService.markRunning(operationId, 20);
            GenerationService.GeneratedAssetResult result = generationService.regenerate(assetId, actorUserId);
            operationService.markCompleted(
                    operationId,
                    "{\"assetId\":\"" + assetId + "\",\"version\":" + result.version().getVersionNumber()
                            + ",\"mappings\":" + result.mappingCount() + "}"
            );
        } catch (Exception ex) {
            log.warn("Content regenerate failed for asset {}: {}", assetId, ex.getMessage());
            String message = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            operationService.markFailed(operationId, "CONTENT_REGENERATE_FAILED", message);
        }
    }
}
