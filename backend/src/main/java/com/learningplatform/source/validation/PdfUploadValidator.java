package com.learningplatform.source.validation;

import com.learningplatform.common.config.AppProperties;
import com.learningplatform.common.exception.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;

@Component
public class PdfUploadValidator {

    private final long maxFileBytes;

    public PdfUploadValidator(AppProperties appProperties) {
        AppProperties.Storage storage = appProperties.storage();
        this.maxFileBytes = storage != null ? storage.effectiveMaxFileBytes() : 50L * 1024 * 1024;
    }

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("FILE_REQUIRED", "PDF file is required.");
        }
        if (file.getSize() > maxFileBytes) {
            throw new BusinessException("FILE_TOO_LARGE", "File exceeds maximum allowed size of " + maxFileBytes + " bytes.");
        }

        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
        String lower = filename.toLowerCase(Locale.ROOT);
        if (!lower.endsWith(".pdf")) {
            throw new BusinessException("FILE_TYPE_NOT_ALLOWED", "Only PDF files are allowed.");
        }

        String contentType = file.getContentType();
        if (contentType != null
                && !contentType.isBlank()
                && !contentType.equalsIgnoreCase("application/pdf")
                && !contentType.equalsIgnoreCase("application/octet-stream")) {
            throw new BusinessException("FILE_TYPE_NOT_ALLOWED", "Only PDF content type is allowed.");
        }

        try {
            byte[] header = file.getInputStream().readNBytes(5);
            if (header.length < 4 || header[0] != '%' || header[1] != 'P' || header[2] != 'D' || header[3] != 'F') {
                throw new BusinessException("FILE_TYPE_NOT_ALLOWED", "File content is not a valid PDF.");
            }
        } catch (IOException e) {
            throw new BusinessException("FILE_READ_FAILED", "Unable to read uploaded file.");
        }
    }
}
