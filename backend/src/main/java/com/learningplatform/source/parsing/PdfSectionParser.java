package com.learningplatform.source.parsing;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PdfSectionParser {

    private static final Pattern HEADING = Pattern.compile(
            "(?m)^(Chapter\\s+\\d+[:.\\s].+|CHAPTER\\s+\\d+.*|[A-Z][A-Za-z0-9 ,\\-]{3,80})$"
    );

    public List<ParsedSection> parse(InputStream pdfStream) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfStream.readAllBytes())) {
            List<ParsedSection> fromOutline = tryOutline(document);
            if (!fromOutline.isEmpty()) {
                return fromOutline;
            }
            return fromPlainText(document);
        }
    }

    private List<ParsedSection> tryOutline(PDDocument document) throws IOException {
        PDDocumentOutline outline = document.getDocumentCatalog().getDocumentOutline();
        if (outline == null || outline.getFirstChild() == null) {
            return List.of();
        }

        PDFTextStripper stripper = new PDFTextStripper();
        String fullText = stripper.getText(document);
        if (fullText == null || fullText.isBlank()) {
            return List.of();
        }

        List<String> titles = new ArrayList<>();
        PDOutlineItem current = outline.getFirstChild();
        while (current != null) {
            if (current.getTitle() != null && !current.getTitle().isBlank()) {
                titles.add(current.getTitle().trim());
            }
            current = current.getNextSibling();
        }
        if (titles.isEmpty()) {
            return List.of();
        }

        List<ParsedSection> sections = new ArrayList<>();
        for (int i = 0; i < titles.size(); i++) {
            String title = titles.get(i);
            String nextTitle = i + 1 < titles.size() ? titles.get(i + 1) : null;
            String body = extractBetween(fullText, title, nextTitle);
            if (body.isBlank()) {
                body = title;
            }
            sections.add(new ParsedSection(
                    StableKeyFactory.fromOutlinePath(i + 1, title),
                    title,
                    "CHAPTER",
                    body
            ));
        }
        return sections;
    }

    private List<ParsedSection> fromPlainText(PDDocument document) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        String fullText = stripper.getText(document);
        if (fullText == null || fullText.isBlank()) {
            throw new IOException("PDF contained no extractable text (scanned PDFs are not supported in v1)");
        }

        List<ParsedSection> sections = splitByHeadings(fullText);
        if (sections.isEmpty()) {
            sections = splitByParagraphs(fullText);
        }
        if (sections.isEmpty()) {
            String title = fullText.lines().findFirst().orElse("Document").trim();
            if (title.length() > 80) {
                title = title.substring(0, 80);
            }
            sections = List.of(new ParsedSection(
                    StableKeyFactory.fromSequence(1, title),
                    title,
                    "BODY",
                    fullText
            ));
        }
        return sections;
    }

    private List<ParsedSection> splitByHeadings(String fullText) {
        Matcher matcher = HEADING.matcher(fullText);
        List<int[]> headingSpans = new ArrayList<>();
        List<String> titles = new ArrayList<>();
        while (matcher.find()) {
            String title = matcher.group(1).trim();
            if (title.length() < 4) {
                continue;
            }
            headingSpans.add(new int[]{matcher.start(), matcher.end()});
            titles.add(title);
        }
        if (headingSpans.size() < 2) {
            return List.of();
        }

        List<ParsedSection> sections = new ArrayList<>();
        for (int i = 0; i < headingSpans.size(); i++) {
            int contentStart = headingSpans.get(i)[1];
            int contentEnd = i + 1 < headingSpans.size() ? headingSpans.get(i + 1)[0] : fullText.length();
            String body = fullText.substring(contentStart, contentEnd).trim();
            if (body.isBlank()) {
                body = titles.get(i);
            }
            sections.add(new ParsedSection(
                    StableKeyFactory.fromSequence(i + 1, titles.get(i)),
                    titles.get(i),
                    "SECTION",
                    body
            ));
        }
        return sections;
    }

    private List<ParsedSection> splitByParagraphs(String fullText) {
        String[] parts = fullText.split("\\n\\s*\\n");
        List<ParsedSection> sections = new ArrayList<>();
        int ordinal = 1;
        for (String part : parts) {
            String content = part.trim();
            if (content.length() < 8) {
                continue;
            }
            String title = content.lines().findFirst().orElse("Section " + ordinal);
            if (title.length() > 80) {
                title = title.substring(0, 80);
            }
            sections.add(new ParsedSection(
                    StableKeyFactory.fromSequence(ordinal, title),
                    title,
                    "PARAGRAPH",
                    content
            ));
            ordinal++;
        }
        return dedupeKeys(sections);
    }

    private static String extractBetween(String fullText, String startTitle, String nextTitle) {
        int start = indexOfIgnoreCase(fullText, startTitle);
        if (start < 0) {
            return "";
        }
        start += startTitle.length();
        int end = nextTitle != null ? indexOfIgnoreCase(fullText, nextTitle, start) : fullText.length();
        if (end < 0) {
            end = fullText.length();
        }
        return fullText.substring(start, end).trim();
    }

    private static int indexOfIgnoreCase(String haystack, String needle) {
        return indexOfIgnoreCase(haystack, needle, 0);
    }

    private static int indexOfIgnoreCase(String haystack, String needle, int from) {
        return haystack.toLowerCase().indexOf(needle.toLowerCase(), from);
    }

    private static List<ParsedSection> dedupeKeys(List<ParsedSection> sections) {
        Map<String, Integer> seen = new LinkedHashMap<>();
        List<ParsedSection> result = new ArrayList<>();
        for (ParsedSection section : sections) {
            String key = section.externalReference();
            int count = seen.getOrDefault(key, 0) + 1;
            seen.put(key, count);
            if (count > 1) {
                key = key + "-" + count;
                result.add(new ParsedSection(key, section.title(), section.sectionType(), section.content()));
            } else {
                result.add(section);
            }
        }
        return result;
    }
}
