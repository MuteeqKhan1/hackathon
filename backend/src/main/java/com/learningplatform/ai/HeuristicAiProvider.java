package com.learningplatform.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Local demo provider — builds valid grounded JSON from retrieved chunk metadata in the prompt.
 * Activate with app.ai.provider=heuristic (default).
 */
@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "heuristic", matchIfMissing = true)
public class HeuristicAiProvider implements AiProvider {

    private static final Pattern SECTION_BLOCK = Pattern.compile(
            "\\[sectionId=(.*?)\\]\\s*title=(.*?)\\n([\\s\\S]*?)(?=\\n\\[sectionId=|$)"
    );

    private final ObjectMapper objectMapper;

    public HeuristicAiProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public GenerationResponse generate(GenerationRequest request) {
        List<SectionSnippet> sections = parseSections(request.userPrompt());
        if (sections.isEmpty()) {
            sections = List.of(new SectionSnippet(
                    "00000000-0000-0000-0000-000000000001",
                    "Introduction",
                    "No source chunks were available; placeholder content."
            ));
        }
        String topic = extractTopic(request.userPrompt());
        sections = preferTopicSections(sections, topic);
        String json = switch (request.responseSchemaName()) {
            case "EXPLANATION" -> buildExplanation(sections, request.userPrompt());
            case "QUIZ" -> buildQuiz(sections, topic);
            case "VIDEO" -> buildVideo(sections, request.userPrompt());
            default -> throw new IllegalArgumentException("Unknown schema: " + request.responseSchemaName());
        };
        return new GenerationResponse(json, "heuristic", "heuristic-rag", "1.0.0", request.promptVersion());
    }

    @Override
    public EmbeddingResponse embed(EmbeddingRequest request) {
        float[] vector = new float[8];
        int hash = request.text() == null ? 0 : request.text().hashCode();
        for (int i = 0; i < vector.length; i++) {
            vector[i] = ((hash >> i) & 1) == 1 ? 0.1f : -0.1f;
        }
        return new EmbeddingResponse(vector, "heuristic-embed", "1.0.0");
    }

    private String buildVideo(List<SectionSnippet> sections, String userPrompt) {
        String topic = extractTopic(userPrompt);
        String pace = extractPace(userPrompt);
        String displayTopic = topic.isBlank() ? sections.get(0).title() : topic;
        ObjectNode root = objectMapper.createObjectNode();
        root.put("title", "Video: " + displayTopic);
        root.put("pace", pace);
        ArrayNode scenes = root.putArray("scenes");
        int maxScenes = "EASY".equals(pace) ? 2 : "HARD".equals(pace) ? Math.min(sections.size(), 5) : Math.min(sections.size(), 3);
        String[] openers = {
                "In this lesson we explore " + displayTopic + ".",
                "Here is the key idea from the source.",
                "Apply what you learned with this takeaway."
        };
        for (int i = 0; i < maxScenes; i++) {
            SectionSnippet s = sections.get(i % sections.size());
            ObjectNode scene = scenes.addObject();
            scene.put("sequence", i + 1);
            String opener = openers[Math.min(i, openers.length - 1)];
            scene.put("narration", opener + " " + truncate(s.text(), 200));
            scene.put("onScreenText", i == 0 ? displayTopic : s.title());
            scene.put("durationSeconds", "EASY".equals(pace) ? 8 : "HARD".equals(pace) ? 12 : 10);
            ArrayNode cited = scene.putArray("citedSectionIds");
            cited.add(s.sectionId());
        }
        ArrayNode cited = root.putArray("citedSectionIds");
        for (SectionSnippet s : sections) {
            cited.add(s.sectionId());
        }
        return root.toString();
    }

    private static String extractPace(String prompt) {
        if (prompt == null) {
            return "MEDIUM";
        }
        int idx = prompt.indexOf("Pace:");
        if (idx < 0) {
            return "MEDIUM";
        }
        int end = prompt.indexOf('\n', idx);
        String line = end < 0 ? prompt.substring(idx + 5) : prompt.substring(idx + 5, end);
        return line.trim().isEmpty() ? "MEDIUM" : line.trim().toUpperCase();
    }

    private String buildExplanation(List<SectionSnippet> sections, String userPrompt) {
        SectionSnippet primary = sections.get(0);
        ObjectNode root = objectMapper.createObjectNode();
        String topic = extractTopic(userPrompt);
        String title = topic.isBlank() ? primary.title() : topic;
        root.put("title", title);
        root.put(
                "body",
                "This lesson covers " + title + ". "
                        + "From the source: " + truncate(primary.text(), 420)
        );
        ArrayNode keyPoints = root.putArray("keyPoints");
        keyPoints.add("Focus: " + title);
        keyPoints.add(primary.title() + " — " + truncate(primary.text(), 120));
        if (sections.size() > 1) {
            keyPoints.add(sections.get(1).title() + " — " + truncate(sections.get(1).text(), 100));
        } else {
            keyPoints.add("Practice applying the idea with examples from your textbook.");
        }
        ArrayNode cited = root.putArray("citedSectionIds");
        for (SectionSnippet s : sections) {
            cited.add(s.sectionId());
        }
        return root.toString();
    }

    private String buildQuiz(List<SectionSnippet> sections, String topic) {
        SectionSnippet primary = sections.get(0);
        String focus = topic.isBlank() ? primary.title() : topic;
        ObjectNode root = objectMapper.createObjectNode();
        root.put("title", "Check your understanding: " + focus);
        ArrayNode questions = root.putArray("questions");
        ObjectNode q = questions.addObject();
        q.put("prompt", "What is the main focus of the lesson \"" + focus + "\"?");
        ArrayNode options = q.putArray("options");
        options.add(focus);
        options.add("Unrelated historical event");
        options.add("A formula with no source support");
        options.add("None of the above");
        q.put("correctIndex", 0);
        q.put("explanation", "The correct option matches this lesson's topic grounded in the source.");
        ArrayNode cited = q.putArray("citedSectionIds");
        cited.add(primary.sectionId());
        return root.toString();
    }

    /** Prefer chunks whose title overlaps the topic so lessons are not all titled like the course. */
    private static List<SectionSnippet> preferTopicSections(List<SectionSnippet> sections, String topic) {
        if (topic == null || topic.isBlank() || sections.size() < 2) {
            return sections;
        }
        String needle = topic.toLowerCase();
        List<SectionSnippet> matched = new ArrayList<>();
        List<SectionSnippet> rest = new ArrayList<>();
        for (SectionSnippet s : sections) {
            String t = s.title() == null ? "" : s.title().toLowerCase();
            String body = s.text() == null ? "" : s.text().toLowerCase();
            if (t.contains(needle) || needle.contains(t) || body.contains(needle)) {
                matched.add(s);
            } else {
                rest.add(s);
            }
        }
        if (matched.isEmpty()) {
            return sections;
        }
        List<SectionSnippet> ordered = new ArrayList<>(matched);
        ordered.addAll(rest);
        return ordered;
    }

    private static List<SectionSnippet> parseSections(String prompt) {
        List<SectionSnippet> out = new ArrayList<>();
        if (prompt == null) {
            return out;
        }
        Matcher matcher = SECTION_BLOCK.matcher(prompt);
        while (matcher.find()) {
            out.add(new SectionSnippet(
                    matcher.group(1).trim(),
                    matcher.group(2).trim(),
                    matcher.group(3).trim()
            ));
        }
        return out;
    }

    private static String extractTopic(String prompt) {
        if (prompt == null) {
            return "";
        }
        int idx = prompt.indexOf("Topic:");
        if (idx < 0) {
            return "";
        }
        int end = prompt.indexOf('\n', idx);
        String line = end < 0 ? prompt.substring(idx + 6) : prompt.substring(idx + 6, end);
        return line.trim();
    }

    private static String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max) + "…";
    }

    private record SectionSnippet(String sectionId, String title, String text) {
    }
}
