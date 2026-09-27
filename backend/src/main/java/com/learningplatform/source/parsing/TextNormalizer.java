package com.learningplatform.source.parsing;

import java.text.Normalizer;

/**
 * Deterministic text normalization for fingerprinting.
 */
public final class TextNormalizer {

    private TextNormalizer() {
    }

    public static String normalize(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String nfkc = Normalizer.normalize(input, Normalizer.Form.NFKC);
        String unified = nfkc.replace("\r\n", "\n").replace('\r', '\n');
        return unified.replaceAll("[ \\t\\x0B\\f]+", " ")
                .replaceAll(" *\\n *", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}
