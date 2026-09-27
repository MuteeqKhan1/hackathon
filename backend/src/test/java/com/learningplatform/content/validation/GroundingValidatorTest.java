package com.learningplatform.content.validation;

import com.learningplatform.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GroundingValidatorTest {

    private final GroundingValidator validator = new GroundingValidator();

    @Test
    void citedSectionNotInRetrievedSet_fails() {
        UUID cited = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        assertThatThrownBy(() -> validator.validateCitedSubset(List.of(cited), Set.of(other)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo("GROUNDING_VIOLATION");
    }

    @Test
    void citedInRetrieved_passes() {
        UUID id = UUID.randomUUID();
        assertThatCode(() -> validator.validateCitedSubset(List.of(id), Set.of(id)))
                .doesNotThrowAnyException();
    }
}
