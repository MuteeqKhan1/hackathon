package com.learningplatform.course.structure;

import com.learningplatform.course.domain.Course;
import com.learningplatform.source.domain.SourceSection;
import com.learningplatform.source.domain.SourceSectionVersion;
import com.learningplatform.source.repository.SourceSectionRepository;
import com.learningplatform.source.repository.SourceSectionVersionRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * v1 structure proposal from published source sections (no live LLM — S5 adds AiProvider path).
 * Prefers lecture-like headings and caps topic count so noisy PDF outlines stay usable.
 */
@Component
public class SectionBasedStructureProposer {

    /** Soft cap so instructors can generate/approve a manageable set before publish. */
    static final int MAX_TOPICS = 40;

    private static final Pattern LECTURE_PATTERN = Pattern.compile(
            "(?i)^\\s*(lecture|lec\\.?)\\s*(\\d+)\\b.*$"
    );
    private static final Pattern CHAPTER_PATTERN = Pattern.compile(
            "(?i)^\\s*(chapter|ch\\.?)\\s*(\\d+)\\b.*$"
    );
    private static final Pattern UNIT_PATTERN = Pattern.compile(
            "(?i)^\\s*(unit|module|lesson)\\s*(\\d+)\\b.*$"
    );

    private final SourceSectionVersionRepository sectionVersionRepository;
    private final SourceSectionRepository sectionRepository;

    public SectionBasedStructureProposer(
            SourceSectionVersionRepository sectionVersionRepository,
            SourceSectionRepository sectionRepository
    ) {
        this.sectionVersionRepository = sectionVersionRepository;
        this.sectionRepository = sectionRepository;
    }

    public StructureProposal propose(Course course) {
        List<SourceSectionVersion> sectionVersions =
                sectionVersionRepository.findBySourceVersionId(course.getCurrentSourceVersionId());
        if (sectionVersions.isEmpty()) {
            return new StructureProposal(List.of(
                    new StructureProposal.ProposedChapter(
                            course.getTitle(),
                            List.of(new StructureProposal.ProposedTopic("Introduction"))
                    )
            ));
        }

        Map<UUID, SourceSection> sectionsById = sectionRepository.findAllById(
                sectionVersions.stream().map(SourceSectionVersion::getSourceSectionId).toList()
        ).stream().collect(Collectors.toMap(SourceSection::getId, Function.identity()));

        List<SourceSectionVersion> ordered = sectionVersions.stream()
                .sorted(Comparator.comparing(sv -> {
                    SourceSection section = sectionsById.get(sv.getSourceSectionId());
                    return section != null ? section.getExternalReference() : sv.getId().toString();
                }))
                .toList();

        List<Candidate> candidates = new ArrayList<>();
        for (SourceSectionVersion sv : ordered) {
            SourceSection section = sectionsById.get(sv.getSourceSectionId());
            String title = section != null ? section.getTitle() : "Topic";
            if (title == null || title.isBlank() || isNoiseTitle(title)) {
                continue;
            }
            candidates.add(new Candidate(title.trim(), classify(title.trim())));
        }

        List<Candidate> selected = selectTopics(candidates);
        if (selected.isEmpty()) {
            return new StructureProposal(List.of(
                    new StructureProposal.ProposedChapter(
                            course.getTitle(),
                            List.of(new StructureProposal.ProposedTopic("Introduction"))
                    )
            ));
        }

        List<StructureProposal.ProposedTopic> topics = selected.stream()
                .map(c -> new StructureProposal.ProposedTopic(c.title()))
                .toList();

        return new StructureProposal(List.of(
                new StructureProposal.ProposedChapter(course.getTitle(), topics)
        ));
    }

    static List<Candidate> selectTopics(List<Candidate> candidates) {
        List<Candidate> lectures = candidates.stream().filter(c -> c.kind() == Kind.LECTURE).toList();
        if (!lectures.isEmpty()) {
            return dedupePreserveOrder(lectures).stream().limit(MAX_TOPICS).toList();
        }
        List<Candidate> chapters = candidates.stream().filter(c -> c.kind() == Kind.CHAPTER).toList();
        if (!chapters.isEmpty()) {
            return dedupePreserveOrder(chapters).stream().limit(MAX_TOPICS).toList();
        }
        List<Candidate> units = candidates.stream().filter(c -> c.kind() == Kind.UNIT).toList();
        if (!units.isEmpty()) {
            return dedupePreserveOrder(units).stream().limit(MAX_TOPICS).toList();
        }
        List<Candidate> substantive = candidates.stream()
                .filter(c -> c.kind() == Kind.SUBSTANTIVE)
                .toList();
        if (!substantive.isEmpty()) {
            return dedupePreserveOrder(substantive).stream().limit(MAX_TOPICS).toList();
        }
        return dedupePreserveOrder(candidates).stream().limit(MAX_TOPICS).toList();
    }

    static boolean isNoiseTitle(String title) {
        String t = title.trim();
        if (t.length() < 3) {
            return true;
        }
        String lower = t.toLowerCase(Locale.ROOT);
        if (lower.matches("^(contents?|table of contents|index|glossary|references|bibliography|appendix|preface|acknowledgements?|copyright|cover|title page)\\.?$")) {
            return true;
        }
        if (t.matches("(?i)^page\\s*\\d+$") || t.matches("^\\d+$") || t.matches("(?i)^fig(ure)?\\.?\\s*\\d+.*")) {
            return true;
        }
        return t.contains("....");
    }

    static Kind classify(String title) {
        if (LECTURE_PATTERN.matcher(title).matches()) {
            return Kind.LECTURE;
        }
        if (CHAPTER_PATTERN.matcher(title).matches()) {
            return Kind.CHAPTER;
        }
        if (UNIT_PATTERN.matcher(title).matches()) {
            return Kind.UNIT;
        }
        if (title.length() >= 8 && !title.matches("(?i)^(section|part)\\s*\\d+\\.?$")) {
            return Kind.SUBSTANTIVE;
        }
        return Kind.OTHER;
    }

    private static List<Candidate> dedupePreserveOrder(List<Candidate> input) {
        Map<String, Candidate> seen = new LinkedHashMap<>();
        for (Candidate c : input) {
            String key = normalizeKey(c.title());
            seen.putIfAbsent(key, c);
        }
        return new ArrayList<>(seen.values());
    }

    private static String normalizeKey(String title) {
        Matcher lecture = LECTURE_PATTERN.matcher(title);
        if (lecture.matches()) {
            return "lecture:" + lecture.group(2);
        }
        Matcher chapter = CHAPTER_PATTERN.matcher(title);
        if (chapter.matches()) {
            return "chapter:" + chapter.group(2);
        }
        Matcher unit = UNIT_PATTERN.matcher(title);
        if (unit.matches()) {
            return "unit:" + unit.group(1).toLowerCase(Locale.ROOT) + ":" + unit.group(2);
        }
        return title.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    enum Kind {
        LECTURE,
        CHAPTER,
        UNIT,
        SUBSTANTIVE,
        OTHER
    }

    record Candidate(String title, Kind kind) {
    }
}
