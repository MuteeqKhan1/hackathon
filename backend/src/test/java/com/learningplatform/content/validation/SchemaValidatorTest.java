package com.learningplatform.content.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.content.domain.AssetType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SchemaValidatorTest {

    private SchemaValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SchemaValidator(new ObjectMapper());
    }

    @Test
    void validExplanationJson_passes() {
        String json = """
                {"title":"Newton","body":"F=ma","keyPoints":["Force"],"citedSectionIds":["11111111-1111-1111-1111-111111111111"]}
                """;
        SchemaValidator.ValidatedPayload payload = validator.validate(AssetType.EXPLANATION, json);
        assertThat(payload.citedSectionIds()).hasSize(1);
    }

    @Test
    void missingRequiredField_fails() {
        String json = """
                {"title":"Newton","keyPoints":["Force"],"citedSectionIds":["11111111-1111-1111-1111-111111111111"]}
                """;
        assertThatThrownBy(() -> validator.validate(AssetType.EXPLANATION, json))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("body");
    }
}
