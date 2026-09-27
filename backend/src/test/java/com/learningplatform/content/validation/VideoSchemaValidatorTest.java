package com.learningplatform.content.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.content.domain.AssetType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VideoSchemaValidatorTest {

    private final SchemaValidator validator = new SchemaValidator(new ObjectMapper());

    @Test
    void validVideoJson_passes() {
        String json = """
                {
                  "title":"Video: Newton",
                  "pace":"MEDIUM",
                  "scenes":[{"sequence":1,"narration":"Hello","onScreenText":"F=ma","durationSeconds":10,"citedSectionIds":["11111111-1111-1111-1111-111111111111"]}],
                  "citedSectionIds":["11111111-1111-1111-1111-111111111111"]
                }
                """;
        assertThat(validator.validate(AssetType.VIDEO, json).citedSectionIds()).hasSize(1);
    }

    @Test
    void missingScenes_fails() {
        assertThatThrownBy(() -> validator.validate(AssetType.VIDEO, "{\"title\":\"x\",\"citedSectionIds\":[]}"))
                .isInstanceOf(BusinessException.class);
    }
}
