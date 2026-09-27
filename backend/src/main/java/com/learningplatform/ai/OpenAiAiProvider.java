package com.learningplatform.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Placeholder OpenAI adapter — set app.ai.provider=openai and app.ai.openai.api-key when ready.
 * Until a real HTTP client is wired, generation fails loudly rather than silently inventing content.
 */
@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "openai")
public class OpenAiAiProvider implements AiProvider {

    @Override
    public GenerationResponse generate(GenerationRequest request) {
        throw new UnsupportedOperationException(
                "OpenAI adapter selected but HTTP client not configured. Set app.ai.provider=heuristic for local demos, or implement OpenAI calls."
        );
    }

    @Override
    public EmbeddingResponse embed(EmbeddingRequest request) {
        throw new UnsupportedOperationException("OpenAI embeddings not configured yet.");
    }
}
