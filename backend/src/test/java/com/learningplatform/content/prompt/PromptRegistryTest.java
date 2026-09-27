package com.learningplatform.content.prompt;

import com.learningplatform.content.domain.PromptVersion;
import com.learningplatform.content.repository.PromptVersionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromptRegistryTest {

    @Mock
    private PromptVersionRepository promptVersionRepository;

    @InjectMocks
    private PromptRegistry promptRegistry;

    @Test
    void quizGeneration_returnsVersionedTemplate() {
        PromptVersion prompt = new PromptVersion(
                UUID.randomUUID(),
                PromptRegistry.QUIZ_GENERATION,
                "1.0.0",
                "Generate quiz for {{topicTitle}}",
                Instant.now()
        );
        when(promptVersionRepository.findFirstByPromptKeyOrderByCreatedAtDesc(PromptRegistry.QUIZ_GENERATION))
                .thenReturn(Optional.of(prompt));

        PromptVersion found = promptRegistry.requireLatest(PromptRegistry.QUIZ_GENERATION);
        assertThat(found.getVersion()).isEqualTo("1.0.0");
        assertThat(promptRegistry.render(found, "Newton")).contains("Newton");
    }
}
