package com.learningplatform.content.prompt;

import com.learningplatform.common.exception.NotFoundException;
import com.learningplatform.content.domain.PromptVersion;
import com.learningplatform.content.repository.PromptVersionRepository;
import org.springframework.stereotype.Component;

@Component
public class PromptRegistry {

    public static final String EXPLANATION_GENERATION = "EXPLANATION_GENERATION";
    public static final String QUIZ_GENERATION = "QUIZ_GENERATION";
    public static final String VIDEO_GENERATION = "VIDEO_GENERATION";

    private final PromptVersionRepository promptVersionRepository;

    public PromptRegistry(PromptVersionRepository promptVersionRepository) {
        this.promptVersionRepository = promptVersionRepository;
    }

    public PromptVersion requireLatest(String promptKey) {
        return promptVersionRepository.findFirstByPromptKeyOrderByCreatedAtDesc(promptKey)
                .orElseThrow(() -> new NotFoundException("PROMPT_NOT_FOUND", "No prompt registered for " + promptKey));
    }

    public String render(PromptVersion prompt, String topicTitle) {
        return prompt.getTemplate().replace("{{topicTitle}}", topicTitle == null ? "" : topicTitle);
    }
}
