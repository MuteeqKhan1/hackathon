package com.learningplatform.source.parsing;

import java.util.Locale;
import java.util.regex.Pattern;

public final class StableKeyFactory {

    private static final Pattern NON_SLUG = Pattern.compile("[^a-z0-9]+");

    private StableKeyFactory() {
    }

    public static String fromOutlinePath(int chapterOrdinal, String title) {
        return "CH" + chapterOrdinal + "/" + slug(title);
    }

    public static String fromSequence(int ordinal, String title) {
        return "SEC-" + ordinal + "-" + slug(title);
    }

    public static String slug(String title) {
        if (title == null || title.isBlank()) {
            return "untitled";
        }
        String slug = NON_SLUG.matcher(title.trim().toLowerCase(Locale.ROOT)).replaceAll("-");
        slug = slug.replaceAll("(^-|-$)", "");
        if (slug.isBlank()) {
            return "untitled";
        }
        return slug.length() > 80 ? slug.substring(0, 80).replaceAll("-$", "") : slug;
    }
}
