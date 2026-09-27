package com.learningplatform.content.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.content.domain.AssetType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class SchemaValidator {

    private final ObjectMapper objectMapper;

    public SchemaValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ValidatedPayload validate(AssetType assetType, String contentJson) {
        JsonNode root;
        try {
            root = objectMapper.readTree(contentJson);
        } catch (Exception ex) {
            throw new BusinessException("INVALID_LLM_JSON", "LLM output is not valid JSON.");
        }
        return switch (assetType) {
            case EXPLANATION -> validateExplanation(root);
            case QUIZ -> validateQuiz(root);
            case VIDEO -> validateVideo(root);
        };
    }

    private ValidatedPayload validateVideo(JsonNode root) {
        requireText(root, "title");
        if (!root.has("scenes") || !root.get("scenes").isArray() || root.get("scenes").isEmpty()) {
            throw new BusinessException("INVALID_LLM_JSON", "Video missing required field: scenes");
        }
        List<UUID> allCited = new ArrayList<>(readCitedSectionIds(root));
        for (JsonNode scene : root.get("scenes")) {
            requireText(scene, "narration");
            requireText(scene, "onScreenText");
            if (!scene.has("durationSeconds") || !scene.get("durationSeconds").isNumber()) {
                throw new BusinessException("INVALID_LLM_JSON", "Video scene missing durationSeconds");
            }
            allCited.addAll(readCitedSectionIds(scene));
        }
        if (allCited.isEmpty()) {
            throw new BusinessException("INVALID_LLM_JSON", "Video missing required field: citedSectionIds");
        }
        return new ValidatedPayload(root.toString(), allCited.stream().distinct().toList());
    }

    private ValidatedPayload validateExplanation(JsonNode root) {
        requireText(root, "title");
        requireText(root, "body");
        if (!root.has("keyPoints") || !root.get("keyPoints").isArray() || root.get("keyPoints").isEmpty()) {
            throw new BusinessException("INVALID_LLM_JSON", "Explanation missing required field: keyPoints");
        }
        List<UUID> cited = readCitedSectionIds(root);
        if (cited.isEmpty()) {
            throw new BusinessException("INVALID_LLM_JSON", "Explanation missing required field: citedSectionIds");
        }
        return new ValidatedPayload(root.toString(), cited);
    }

    private ValidatedPayload validateQuiz(JsonNode root) {
        requireText(root, "title");
        if (!root.has("questions") || !root.get("questions").isArray() || root.get("questions").isEmpty()) {
            throw new BusinessException("INVALID_LLM_JSON", "Quiz missing required field: questions");
        }
        List<UUID> allCited = new ArrayList<>();
        for (JsonNode q : root.get("questions")) {
            requireText(q, "prompt");
            if (!q.has("options") || !q.get("options").isArray() || q.get("options").size() < 2) {
                throw new BusinessException("INVALID_LLM_JSON", "Quiz question missing required field: options");
            }
            if (!q.has("correctIndex") || !q.get("correctIndex").isInt()) {
                throw new BusinessException("INVALID_LLM_JSON", "Quiz question missing required field: correctIndex");
            }
            allCited.addAll(readCitedSectionIds(q));
        }
        if (allCited.isEmpty()) {
            throw new BusinessException("INVALID_LLM_JSON", "Quiz missing required field: citedSectionIds");
        }
        return new ValidatedPayload(root.toString(), allCited.stream().distinct().toList());
    }

    private static void requireText(JsonNode node, String field) {
        if (!node.has(field) || node.get(field).asText("").isBlank()) {
            throw new BusinessException("INVALID_LLM_JSON", "Missing required field: " + field);
        }
    }

    private static List<UUID> readCitedSectionIds(JsonNode node) {
        List<UUID> ids = new ArrayList<>();
        if (!node.has("citedSectionIds") || !node.get("citedSectionIds").isArray()) {
            return ids;
        }
        for (JsonNode idNode : node.get("citedSectionIds")) {
            try {
                ids.add(UUID.fromString(idNode.asText()));
            } catch (IllegalArgumentException ex) {
                throw new BusinessException("INVALID_LLM_JSON", "citedSectionIds must be UUIDs");
            }
        }
        return ids;
    }

    public record ValidatedPayload(String contentJson, List<UUID> citedSectionIds) {
    }
}
