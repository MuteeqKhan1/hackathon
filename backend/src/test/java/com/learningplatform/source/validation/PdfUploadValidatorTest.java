package com.learningplatform.source.validation;

import com.learningplatform.common.config.AppProperties;
import com.learningplatform.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdfUploadValidatorTest {

    private PdfUploadValidator validator;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties(
                null,
                new AppProperties.Storage(
                        "http://localhost", "k", "s", "b", "us-east-1", "./data", 1024
                ),
                null
        );
        validator = new PdfUploadValidator(props);
    }

    @Test
    void validate_pdf_allows() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "physics.pdf", "application/pdf", "%PDF-1.4 content".getBytes()
        );
        assertThatCode(() -> validator.validate(file)).doesNotThrowAnyException();
    }

    @Test
    void validate_msword_rejects() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.doc", "application/msword", "not-a-pdf".getBytes()
        );
        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("FILE_TYPE_NOT_ALLOWED");
    }

    @Test
    void validate_tooLarge_rejects() {
        byte[] big = new byte[2048];
        System.arraycopy("%PDF".getBytes(), 0, big, 0, 4);
        MockMultipartFile file = new MockMultipartFile("file", "big.pdf", "application/pdf", big);
        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("FILE_TOO_LARGE");
    }
}
