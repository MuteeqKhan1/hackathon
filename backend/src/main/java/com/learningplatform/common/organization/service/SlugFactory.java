package com.learningplatform.common.organization.service;

import java.util.Locale;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Deterministic slug helper — pure, unit-testable.
 */
public final class SlugFactory {

    private SlugFactory() {
    }

    public static String fromName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        String slug = name.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (slug.isBlank()) {
            slug = "org";
        }
        if (slug.length() > 80) {
            slug = slug.substring(0, 80).replaceAll("-$", "");
        }
        return slug;
    }

    public static String unique(String base, Predicate<String> exists) {
        if (!exists.test(base)) {
            return base;
        }
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String candidate = base + "-" + suffix;
        if (candidate.length() > 100) {
            candidate = base.substring(0, Math.max(1, 100 - 9)) + "-" + suffix;
        }
        return candidate;
    }
}
