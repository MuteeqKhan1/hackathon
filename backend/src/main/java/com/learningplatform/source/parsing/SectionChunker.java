package com.learningplatform.source.parsing;

import java.util.ArrayList;
import java.util.List;

public final class SectionChunker {

    public static final int DEFAULT_MAX_CHARS = 1200;

    private SectionChunker() {
    }

    public static List<String> chunk(String content) {
        return chunk(content, DEFAULT_MAX_CHARS);
    }

    public static List<String> chunk(String content, int maxChars) {
        String normalized = TextNormalizer.normalize(content);
        if (normalized.isBlank()) {
            return List.of();
        }
        if (normalized.length() <= maxChars) {
            return List.of(normalized);
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(start + maxChars, normalized.length());
            if (end < normalized.length()) {
                int breakAt = normalized.lastIndexOf('\n', end);
                if (breakAt > start + maxChars / 3) {
                    end = breakAt;
                }
            }
            String piece = normalized.substring(start, end).trim();
            if (!piece.isBlank()) {
                chunks.add(piece);
            }
            start = end;
            while (start < normalized.length() && Character.isWhitespace(normalized.charAt(start))) {
                start++;
            }
        }
        return chunks;
    }
}
