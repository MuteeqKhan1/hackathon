package com.learningplatform.ai;

/**
 * LLM provider abstraction. Business services must depend on this interface only.
 */
public interface AiProvider {

    GenerationResponse generate(GenerationRequest request);

    EmbeddingResponse embed(EmbeddingRequest request);

    record GenerationRequest(
            String promptKey,
            String promptVersion,
            String systemPrompt,
            String userPrompt,
            String responseSchemaName
    ) {
    }

    record GenerationResponse(
            String contentJson,
            String modelProvider,
            String modelName,
            String modelVersion,
            String promptVersion
    ) {
    }

    record EmbeddingRequest(String text) {
    }

    record EmbeddingResponse(float[] vector, String modelName, String modelVersion) {
    }
}
