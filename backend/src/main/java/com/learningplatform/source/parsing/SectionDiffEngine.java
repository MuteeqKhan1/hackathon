package com.learningplatform.source.parsing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class SectionDiffEngine {

    public record SectionSnapshot(String externalReference, String title, String contentHash) {
    }

    public record SectionDiff(String externalReference, SectionChangeType changeType, String reason) {
    }

    private SectionDiffEngine() {
    }

    public static List<SectionDiff> diff(Map<String, SectionSnapshot> oldSections, Map<String, SectionSnapshot> newSections) {
        Map<String, SectionSnapshot> oldMap = oldSections != null ? oldSections : Map.of();
        Map<String, SectionSnapshot> newMap = newSections != null ? newSections : Map.of();
        List<SectionDiff> diffs = new ArrayList<>();

        for (Map.Entry<String, SectionSnapshot> entry : newMap.entrySet()) {
            String key = entry.getKey();
            SectionSnapshot neu = entry.getValue();
            SectionSnapshot old = oldMap.get(key);
            if (old == null) {
                diffs.add(new SectionDiff(key, SectionChangeType.ADDED, "New section"));
                continue;
            }
            if (!Objects.equals(old.contentHash(), neu.contentHash())) {
                diffs.add(new SectionDiff(key, SectionChangeType.MODIFIED, "Content hash changed"));
            } else if (!Objects.equals(old.title(), neu.title())) {
                diffs.add(new SectionDiff(key, SectionChangeType.METADATA_CHANGED, "Title changed with same content hash"));
            } else {
                diffs.add(new SectionDiff(key, SectionChangeType.UNCHANGED, "No change"));
            }
        }

        for (String key : oldMap.keySet()) {
            if (!newMap.containsKey(key)) {
                diffs.add(new SectionDiff(key, SectionChangeType.REMOVED, "Section missing in new version"));
            }
        }
        return diffs;
    }

    public static Map<String, SectionSnapshot> indexByKey(List<SectionSnapshot> snapshots) {
        Map<String, SectionSnapshot> map = new LinkedHashMap<>();
        if (snapshots == null) {
            return map;
        }
        for (SectionSnapshot snapshot : snapshots) {
            map.put(snapshot.externalReference(), snapshot);
        }
        return map;
    }
}
