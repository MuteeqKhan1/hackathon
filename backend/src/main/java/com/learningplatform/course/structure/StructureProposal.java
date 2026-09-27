package com.learningplatform.course.structure;

import java.util.List;

/**
 * AI (or heuristic) proposed course outline before persistence.
 */
public record StructureProposal(List<ProposedChapter> chapters) {

    public record ProposedChapter(String title, List<ProposedTopic> topics) {
    }

    public record ProposedTopic(String title) {
    }
}
