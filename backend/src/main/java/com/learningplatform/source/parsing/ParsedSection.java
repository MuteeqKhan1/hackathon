package com.learningplatform.source.parsing;

public record ParsedSection(
        String externalReference,
        String title,
        String sectionType,
        String content
) {
}
