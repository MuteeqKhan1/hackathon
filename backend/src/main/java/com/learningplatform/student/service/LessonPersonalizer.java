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
 * Applies student language + pace preferences to approved lesson JSON.
 * Same published concept → different learner experience (EASY scaffold / HARD advanced).
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
            String topic = stripExistingPrefix(title.isBlank() ? "this topic" : title);

            title = localizeHeading(title, language);
            body = buildExplanationBody(body, topic, language, pace);
            points = buildExplanationPoints(points, topic, language, pace);

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
                String narration = text(scene, "narration");
                narration = buildVideoNarration(narration, i + 1, keep, language, pace);
                if (pace == LearningPace.EASY) {
                    scene.put("durationSeconds", Math.min(scene.path("durationSeconds").asInt(10), 8));
                } else if (pace == LearningPace.HARD) {
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
        String baseTitle = quizTitle == null || quizTitle.isBlank() ? "Quiz" : quizTitle;
        String title = switch (pace) {
            case EASY -> prefix(language, "Practice", "अभ्यास", "Práctica") + ": " + localizeHeading(baseTitle, language);
            case HARD -> prefix(language, "Challenge", "चुनौती", "Desafío") + ": " + localizeHeading(baseTitle, language);
            default -> localizeHeading(baseTitle, language);
        };

        List<QuizQuestion> out = new ArrayList<>();
        int limit = pace == LearningPace.EASY ? Math.min(1, questions.size()) : questions.size();
        for (int i = 0; i < limit; i++) {
            QuizQuestion q = questions.get(i);
            String prompt = localizeLine(q.prompt(), language);
            prompt = switch (pace) {
                case EASY -> prefix(language,
                        "Practice — take it step by step",
                        "अभ्यास — चरणबद्ध हल करें",
                        "Práctica — paso a paso") + ": " + prompt;
                case HARD -> prefix(language,
                        "Challenge — apply a deeper concept",
                        "चुनौती — गहराई से लागू करें",
                        "Desafío — aplica un concepto más profundo") + ": " + prompt;
                default -> prompt;
            };
            List<String> options = q.options().stream().map(o -> localizeLine(o, language)).toList();
            out.add(new QuizQuestion(q.id(), i + 1, prompt, options));
        }
        return new PersonalizedQuiz(title, out);
    }

    private static String buildExplanationBody(String body, String topic, String language, LearningPace pace) {
        String source = body == null ? "" : body.trim();
        String lang = normalizeLang(language);
        return switch (pace) {
            case EASY -> {
                String simplified = truncate(source, 160);
                String step1 = label(lang, "Step 1 — Core idea", "चरण 1 — मुख्य विचार", "Paso 1 — Idea central");
                String step2 = label(lang, "Step 2 — In simple words", "चरण 2 — सरल भाषा में", "Paso 2 — En palabras simples");
                String step3 = label(lang, "Step 3 — Try a practice example", "चरण 3 — अभ्यास उदाहरण", "Paso 3 — Ejemplo de práctica");
                String practice = label(lang,
                        "Practice example: Restate \"" + topic + "\" using one everyday situation.",
                        "अभ्यास उदाहरण: \"" + topic + "\" को एक रोज़मर्रा की स्थिति से समझाएँ।",
                        "Ejemplo de práctica: Explica \"" + topic + "\" con una situación cotidiana.");
                yield intro(lang, pace)
                        + step1 + ": " + simplified + " "
                        + step2 + ": " + label(lang,
                        "Focus only on the main idea before details.",
                        "विवरण से पहले केवल मुख्य विचार पर ध्यान दें।",
                        "Concéntrate solo en la idea principal antes de los detalles.") + " "
                        + step3 + ": " + practice;
            }
            case HARD -> {
                String deeper = label(lang,
                        "Deeper concept: Connect \"" + topic + "\" to assumptions, edge cases, and related formulas or definitions from the source.",
                        "गहराई: \"" + topic + "\" को मान्यताओं, सीमा मामलों और स्रोत की परिभाषाओं से जोड़ें।",
                        "Concepto más profundo: Relaciona \"" + topic + "\" con supuestos, casos límite y definiciones del material.");
                String extraExample = label(lang,
                        "Additional example: Compare a standard case with a non-obvious variant of \"" + topic + "\".",
                        "अतिरिक्त उदाहरण: \"" + topic + "\" के सामान्य और कम स्पष्ट रूप की तुलना करें।",
                        "Ejemplo adicional: Compara un caso estándar con una variante no obvia de \"" + topic + "\".");
                yield intro(lang, pace)
                        + label(lang, "Advanced explanation", "उन्नत व्याख्या", "Explicación avanzada") + ": "
                        + source + " "
                        + deeper + " "
                        + extraExample;
            }
            default -> intro(lang, pace) + source;
        };
    }

    private static List<String> buildExplanationPoints(List<String> points, String topic, String language, LearningPace pace) {
        List<String> base = points == null ? List.of() : points;
        String lang = normalizeLang(language);
        return switch (pace) {
            case EASY -> {
                List<String> easy = new ArrayList<>();
                int i = 1;
                for (String p : base.stream().limit(2).toList()) {
                    easy.add(label(lang, "Step " + i, "चरण " + i, "Paso " + i) + ": " + localizeLine(p, language));
                    i++;
                }
                if (easy.isEmpty()) {
                    easy.add(label(lang, "Step 1: Learn the core idea of", "चरण 1: मुख्य विचार सीखें —", "Paso 1: Aprende la idea central de")
                            + " " + topic);
                }
                easy.add(label(lang,
                        "Practice example: Apply \"" + topic + "\" to a simple everyday case.",
                        "अभ्यास उदाहरण: \"" + topic + "\" को एक सरल रोज़मर्रा के मामले में लागू करें।",
                        "Ejemplo de práctica: Aplica \"" + topic + "\" a un caso cotidiano simple."));
                yield easy;
            }
            case HARD -> {
                List<String> hard = new ArrayList<>();
                for (String p : base) {
                    hard.add(localizeLine(p, language));
                }
                hard.add(label(lang,
                        "Additional example: Invent a second scenario that stresses an edge case of \"" + topic + "\".",
                        "अतिरिक्त उदाहरण: \"" + topic + "\" के सीमा मामले वाला दूसरा परिदृश्य बनाएँ।",
                        "Ejemplo adicional: Inventa un segundo escenario que tensiona un caso límite de \"" + topic + "\"."));
                hard.add(label(lang,
                        "Challenge: Explain a deeper concept linking \"" + topic + "\" to a related idea in your own words.",
                        "चुनौती: \"" + topic + "\" को एक संबंधित विचार से जोड़कर अपने शब्दों में समझाएँ।",
                        "Desafío: Explica un concepto más profundo relacionando \"" + topic + "\" con una idea afín."));
                yield hard;
            }
            default -> base.stream().map(p -> localizeLine(p, language)).toList();
        };
    }

    private static String buildVideoNarration(String narration, int step, int total, String language, LearningPace pace) {
        String base = narration == null ? "" : narration.trim();
        String lang = normalizeLang(language);
        return switch (pace) {
            case EASY -> {
                String stepLabel = label(lang,
                        "Step " + step + " of " + total,
                        "चरण " + step + " / " + total,
                        "Paso " + step + " de " + total);
                yield stepLabel + ": " + truncate(base, 140) + " "
                        + label(lang, "Go slowly and check each step.", "धीरे चलें और प्रत्येक चरण जाँचें।", "Ve despacio y revisa cada paso.");
            }
            case HARD -> base + " " + hardExtra(language);
            default -> localizeBody(base, language, pace);
        };
    }

    private static String intro(String lang, LearningPace pace) {
        String paceNote = switch (pace) {
            case EASY -> switch (lang) {
                case "hi" -> "आसान गति। ";
                case "es" -> "Ritmo fácil. ";
                default -> "Easy pace. ";
            };
            case HARD -> switch (lang) {
                case "hi" -> "कठिन गति। ";
                case "es" -> "Ritmo difícil. ";
                default -> "Hard pace. ";
            };
            default -> switch (lang) {
                case "hi" -> "मध्यम गति। ";
                case "es" -> "Ritmo medio. ";
                default -> "Medium pace. ";
            };
        };
        String grounded = switch (lang) {
            case "hi" -> "यह पाठ आपके स्रोत से है। ";
            case "es" -> "Esta lección proviene de tu material fuente. ";
            default -> "This lesson is grounded in your source material. ";
        };
        return grounded + paceNote;
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
        return intro(normalizeLang(language), pace) + (text == null ? "" : text);
    }

    private static String localizeLine(String text, String language) {
        String lang = normalizeLang(language);
        String base = text == null ? "" : text;
        return switch (lang) {
            case "hi", "es" -> "• " + base;
            default -> base;
        };
    }

    private static String hardExtra(String language) {
        return switch (normalizeLang(language)) {
            case "hi" -> "अतिरिक्त चुनौती: सूत्र, सीमाएँ और गहरे संबंधों पर ध्यान दें।";
            case "es" -> "Reto extra: presta atención a fórmulas, límites y conexiones más profundas.";
            default -> "Extra challenge: watch formulas, edge cases, and deeper connections.";
        };
    }

    private static String prefix(String language, String en, String hi, String es) {
        return switch (normalizeLang(language)) {
            case "hi" -> hi;
            case "es" -> es;
            default -> en;
        };
    }

    private static String label(String lang, String en, String hi, String es) {
        return switch (lang) {
            case "hi" -> hi;
            case "es" -> es;
            default -> en;
        };
    }

    private static String stripExistingPrefix(String text) {
        String t = text.trim();
        for (String p : List.of("Lesson:", "पाठ:", "Lección:", "Video:", "Quiz:", "Practice:", "Challenge:", "Warm-up:")) {
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
