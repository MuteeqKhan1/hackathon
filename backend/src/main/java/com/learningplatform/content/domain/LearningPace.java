package com.learningplatform.content.domain;

public enum LearningPace {
    EASY,
    MEDIUM,
    HARD;

    public static LearningPace fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return MEDIUM;
        }
        try {
            return LearningPace.valueOf(raw.trim().toUpperCase());
        } catch (Exception ex) {
            return MEDIUM;
        }
    }
}
