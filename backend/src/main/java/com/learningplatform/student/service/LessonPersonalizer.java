package com.learningplatform.student.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.learningplatform.content.domain.LearningPace;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Applies student language + pace preferences to approved lesson JSON so differences are visible.
 */
@Component
public class LessonPersonalizer {

    private final ObjectMapper objectMapper;

    public LessonPersonalizer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String personalizeExplanation(String contentJson, String language, LearningPace pace) {
        if (contentJson == null || contentJson.isBlank()) {
            return contentJson;
        }
        try {
            ObjectNode root = (ObjectNode) objectMapper.readTree(contentJson);
            String title = text(root, "title");
            String body = text(root, "body");
            List<String> points = readStringArray(root.get("keyPoints"));

            title = localizeHeading(title, language);
            body = applyPaceToBody(body, pace);
            body = localizeBody(body, language, pace);
            points = applyPaceToPoints(points, pace);
            points = points.stream().map(p -> localizeLine(p, language)).toList();

            root.put("title", title);
            root.put("body", body);
            root.put("pace", pace.name());
            root.put("language", normalizeLang(language));
            ArrayNode kp = root.putArray("keyPoints");
            points.forEach(kp::add);
            return objectMapper.writeValueAsString(root);
        } catch (Exception ex) {
            return contentJson;
        }
    }

    public String personalizeVideo(String contentJson, String language, LearningPace pace) {
        if (contentJson == null || contentJson.isBlank()) {
            return contentJson;
        }
        try {
            ObjectNode root = (ObjectNode) objectMapper.readTree(contentJson);
            String title = localizeHeading(text(root, "title"), language);
            root.put("title", title);
            root.put("pace", pace.name());
            root.put("language", normalizeLang(language));

            ArrayNode scenesIn = root.withArray("scenes");
            List<JsonNode> scenes = new ArrayList<>();
            scenesIn.forEach(scenes::add);
            int keep = switch (pace) {
                case EASY -> Math.min(2, scenes.size());
                case HARD -> scenes.size();
                default -> Math.min(3, Math.max(2, scenes.size()));
            };
            ArrayNode scenesOut = root.putArray("scenes");
            for (int i = 0; i < keep; i++) {
                ObjectNode scene = scenes.get(i).deepCopy();
                String onScreen = localizeHeading(text(scene, "onScreenText"), language);
                String narration = localizeBody(text(scene, "narration"), language, pace);
                if (pace == LearningPace.EASY) {
                    narration = truncate(narration, 140);
                    scene.put("durationSeconds", Math.min(scene.path("durationSeconds").asInt(10), 8));
                } else if (pace == LearningPace.HARD) {
                    narration = narration + " " + hardExtra(language);
                    scene.put("durationSeconds", Math.max(scene.path("durationSeconds").asInt(10), 12));
                }
                scene.put("onScreenText", onScreen);
                scene.put("narration", narration);
                scene.put("sequence", i + 1);
                scenesOut.add(scene);
            }
            return objectMapper.writeValueAsString(root);
        } catch (Exception ex) {
            return contentJson;
        }
    }

    public PersonalizedQuiz personalizeQuiz(
            String quizTitle,
            List<QuizQuestion> questions,
            String language,
            LearningPace pace
    ) {
        String title = localizeHeading(quizTitle == null ? "Quiz" : quizTitle, language);
        if (pace == LearningPace.EASY) {
            title = prefix(language, "Warm-up", "वार्म-अप", "Calentamiento") + ": " + title;
        } else if (pace == LearningPace.HARD) {
            title = prefix(language, "Challenge", "चुनौती", "Desafío") + ": " + title;
        }
        List<QuizQuestion> out = new ArrayList<>();
        int limit = pace == LearningPace.EASY ? Math.min(1, questions.size()) : questions.size();
        for (int i = 0; i < limit; i++) {
            QuizQuestion q = questions.get(i);
            String prompt = localizeLine(q.prompt(), language);
            if (pace == LearningPace.HARD) {
                prompt = prefix(language, "Think carefully", "ध्यान से सोचें", "Piensa con cuidado") + ": " + prompt;
            }
            List<String> options = q.options().stream().map(o -> localizeLine(o, language)).toList();
            out.add(new QuizQuestion(q.id(), i + 1, prompt, options));
        }
        return new PersonalizedQuiz(title, out);
    }

    private static String applyPaceToBody(String body, LearningPace pace) {
        if (body == null) {
            return "";
        }
        return switch (pace) {
            case EASY -> truncate(body, 180) + " " + "(Easy pace: short summary.)";
            case HARD -> body + " Extension: compare this idea with a related example and note edge cases.";
            default -> body;
        };
    }

    private static List<String> applyPaceToPoints(List<String> points, LearningPace pace) {
        if (points == null || points.isEmpty()) {
            return List.of();
        }
        return switch (pace) {
            case EASY -> points.stream().limit(2).toList();
            case HARD -> {
                List<String> hard = new ArrayList<>(points);
                hard.add("Challenge: explain this topic in your own words using a new example.");
                yield hard;
            }
            default -> points;
        };
    }

    private static String localizeHeading(String text, String language) {
        String lang = normalizeLang(language);
        String base = text == null ? "" : text;
        return switch (lang) {
            case "hi" -> "पाठ: " + stripExistingPrefix(base);
            case "es" -> "Lección: " + stripExistingPrefix(base);
            default -> base.startsWith("Lesson:") ? base : "Lesson: " + stripExistingPrefix(base);
        };
    }

    private static String localizeBody(String text, String language, LearningPace pace) {
        String lang = normalizeLang(language);
        String base = text == null ? "" : text;
        String paceNote = switch (pace) {
            case EASY -> switch (lang) {
                case "hi" -> " आसान गति।";
                case "es" -> " Ritmo fácil.";
                default -> " Easy pace.";
            };
            case HARD -> switch (lang) {
                case "hi" -> " कठिन गति।";
                case "es" -> " Ritmo difícil.";
                default -> " Hard pace.";
            };
            default -> switch (lang) {
                case "hi" -> " मध्यम गति।";
                case "es" -> " Ritmo medio.";
                default -> " Medium pace.";
            };
        };
        String intro = switch (lang) {
            case "hi" -> "यह पाठ आपके स्रोत से है। ";
            case "es" -> "Esta lección proviene de tu material fuente. ";
            default -> "This lesson is grounded in your source material. ";
        };
        return intro + base + paceNote;
    }

    private static String localizeLine(String text, String language) {
        String lang = normalizeLang(language);
        String base = text == null ? "" : text;
        return switch (lang) {
            case "hi" -> "• " + base;
            case "es" -> "• " + base;
            default -> base;
        };
    }

    private static String hardExtra(String language) {
        return switch (normalizeLang(language)) {
            case "hi" -> "अतिरिक्त चुनौती: सूत्र और सीमाओं पर ध्यान दें।";
            case "es" -> "Reto extra: presta atención a fórmulas y límites.";
            default -> "Extra challenge: watch formulas and edge cases.";
        };
    }

    private static String prefix(String language, String en, String hi, String es) {
        return switch (normalizeLang(language)) {
            case "hi" -> hi;
            case "es" -> es;
            default -> en;
        };
    }

    private static String stripExistingPrefix(String text) {
        String t = text.trim();
        for (String p : List.of("Lesson:", "पाठ:", "Lección:", "Video:", "Quiz:")) {
            if (t.regionMatches(true, 0, p, 0, p.length())) {
                return t.substring(p.length()).trim();
            }
        }
        return t;
    }

    private static String normalizeLang(String language) {
        if (language == null || language.isBlank()) {
            return "en";
        }
        return language.trim().toLowerCase(Locale.ROOT);
    }

    private static String text(JsonNode node, String field) {
        return node.path(field).asText("");
    }

    private static List<String> readStringArray(JsonNode node) {
        List<String> out = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return out;
        }
        node.forEach(n -> out.add(n.asText("")));
        return out;
    }

    private static String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max).trim() + "…";
    }

    public record QuizQuestion(java.util.UUID id, int sequence, String prompt, List<String> options) {
    }

    public record PersonalizedQuiz(String title, List<QuizQuestion> questions) {
    }
}
