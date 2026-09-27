package com.learningplatform.sync.service;

import com.learningplatform.source.domain.SourceSectionVersion;
import com.learningplatform.source.parsing.SectionChangeType;
import com.learningplatform.source.repository.SourceSectionVersionRepository;
import com.learningplatform.sync.domain.ImpactLevel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Section-level change detection + deterministic impact scoring (PRD-09).
 */
@Service
public class ChangeDetectionService {

    private final SourceSectionVersionRepository sectionVersionRepository;

    public ChangeDetectionService(SourceSectionVersionRepository sectionVersionRepository) {
        this.sectionVersionRepository = sectionVersionRepository;
    }

    public List<DetectedChange> detect(UUID oldVersionId, UUID newVersionId) {
        Map<UUID, SourceSectionVersion> oldBySection = indexBySection(
                sectionVersionRepository.findBySourceVersionId(oldVersionId)
        );
        Map<UUID, SourceSectionVersion> newBySection = indexBySection(
                sectionVersionRepository.findBySourceVersionId(newVersionId)
        );

        Set<UUID> allSections = new HashSet<>();
        allSections.addAll(oldBySection.keySet());
        allSections.addAll(newBySection.keySet());

        List<DetectedChange> changes = new ArrayList<>();
        for (UUID sectionId : allSections) {
            SourceSectionVersion oldSv = oldBySection.get(sectionId);
            SourceSectionVersion newSv = newBySection.get(sectionId);

            if (oldSv != null && newSv != null) {
                if (oldSv.getContentHash().equals(newSv.getContentHash())
                        || newSv.getChangeType() == SectionChangeType.UNCHANGED) {
                    continue;
                }
                SectionChangeType type = newSv.getChangeType() != null
                        ? newSv.getChangeType()
                        : SectionChangeType.MODIFIED;
                ImpactLevel level = score(type, oldSv.getContent(), newSv.getContent());
                String signature = type.name() + ":" + oldSv.getContentHash() + "->" + newSv.getContentHash();
                changes.add(new DetectedChange(
                        sectionId, type, oldSv.getContent(), newSv.getContent(), level, reason(type, level), signature
                ));
            } else if (oldSv != null) {
                String signature = "REMOVED:" + oldSv.getContentHash();
                changes.add(new DetectedChange(
                        sectionId,
                        SectionChangeType.REMOVED,
                        oldSv.getContent(),
                        "",
                        ImpactLevel.HIGH,
                        "Section removed from source",
                        signature
                ));
            } else if (newSv != null && newSv.getChangeType() != SectionChangeType.UNCHANGED) {
                String signature = "ADDED:" + newSv.getContentHash();
                changes.add(new DetectedChange(
                        sectionId,
                        SectionChangeType.ADDED,
                        "",
                        newSv.getContent(),
                        ImpactLevel.HIGH,
                        "Section added in source",
                        signature
                ));
            }
        }
        return changes;
    }

    /** Scores a single change for unit tests / rule matrix. */
    public ImpactLevel score(SectionChangeType type, String oldContent, String newContent) {
        if (type == SectionChangeType.UNCHANGED) {
            return ImpactLevel.LOW;
        }
        if (type == SectionChangeType.REMOVED || type == SectionChangeType.ADDED) {
            return ImpactLevel.HIGH;
        }
        if (type == SectionChangeType.METADATA_CHANGED
                || type == SectionChangeType.RENAMED
                || type == SectionChangeType.MOVED) {
            return ImpactLevel.LOW;
        }
        // MODIFIED
        if (isFormulaChange(oldContent, newContent)) {
            return ImpactLevel.HIGH;
        }
        if (isSmallTypoDelta(oldContent, newContent)) {
            return ImpactLevel.LOW;
        }
        if (isExampleOriented(oldContent, newContent)) {
            return ImpactLevel.MEDIUM;
        }
        return ImpactLevel.MEDIUM;
    }

    private static boolean isFormulaChange(String oldContent, String newContent) {
        String o = nullToEmpty(oldContent);
        String n = nullToEmpty(newContent);
        boolean involvesFormula = looksLikeFormula(o) || looksLikeFormula(n);
        if (!involvesFormula) {
            return false;
        }
        return !extractFormulaTokens(o).equals(extractFormulaTokens(n));
    }

    private static boolean looksLikeFormula(String text) {
        String t = text.toLowerCase(Locale.ROOT);
        return t.contains("=") || t.contains("∑") || t.contains("∫") || t.contains("Δ")
                || t.contains("f =") || t.contains("f=") || t.matches("(?s).*\\b[a-z]\\s*=\\s*.*");
    }

    private static String extractFormulaTokens(String text) {
        StringBuilder sb = new StringBuilder();
        for (String line : nullToEmpty(text).split("\\R")) {
            String trimmed = line.trim().toLowerCase(Locale.ROOT);
            if (trimmed.contains("=") || looksLikeFormula(trimmed)) {
                sb.append(trimmed.replaceAll("\\s+", "")).append('|');
            }
        }
        return sb.toString();
    }

    private static boolean isSmallTypoDelta(String oldContent, String newContent) {
        String o = nullToEmpty(oldContent);
        String n = nullToEmpty(newContent);
        int delta = Math.abs(o.length() - n.length());
        if (delta > 20) {
            return false;
        }
        if (looksLikeFormula(o) || looksLikeFormula(n)) {
            return false;
        }
        int maxLen = Math.max(o.length(), n.length());
        return maxLen == 0 || ((double) delta / maxLen) <= 0.05 || delta <= 8;
    }

    private static boolean isExampleOriented(String oldContent, String newContent) {
        String combined = (nullToEmpty(oldContent) + " " + nullToEmpty(newContent)).toLowerCase(Locale.ROOT);
        return combined.contains("example") || combined.contains("for instance") || combined.contains("e.g.");
    }

    private static String reason(SectionChangeType type, ImpactLevel level) {
        return type.name() + " scored " + level.name();
    }

    private static Map<UUID, SourceSectionVersion> indexBySection(List<SourceSectionVersion> versions) {
        Map<UUID, SourceSectionVersion> map = new HashMap<>();
        for (SourceSectionVersion v : versions) {
            map.put(v.getSourceSectionId(), v);
        }
        return map;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    public record DetectedChange(
            UUID sourceSectionId,
            SectionChangeType changeType,
            String oldContent,
            String newContent,
            ImpactLevel impactLevel,
            String reason,
            String changeSignature
    ) {
    }
}
