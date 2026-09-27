package com.learningplatform.course.structure;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SectionBasedStructureProposerTest {

    @Test
    void selectTopics_prefersLecturesAndCaps() {
        List<SectionBasedStructureProposer.Candidate> input = List.of(
                new SectionBasedStructureProposer.Candidate("Contents", SectionBasedStructureProposer.Kind.OTHER),
                new SectionBasedStructureProposer.Candidate("Lecture 1 Mechanics", SectionBasedStructureProposer.Kind.LECTURE),
                new SectionBasedStructureProposer.Candidate("Random heading about friction", SectionBasedStructureProposer.Kind.SUBSTANTIVE),
                new SectionBasedStructureProposer.Candidate("Lecture 2 Forces", SectionBasedStructureProposer.Kind.LECTURE),
                new SectionBasedStructureProposer.Candidate("Lecture 1 Mechanics again", SectionBasedStructureProposer.Kind.LECTURE)
        );

        List<SectionBasedStructureProposer.Candidate> selected = SectionBasedStructureProposer.selectTopics(input);

        assertThat(selected).extracting(SectionBasedStructureProposer.Candidate::title)
                .containsExactly("Lecture 1 Mechanics", "Lecture 2 Forces");
    }

    @Test
    void selectTopics_fallsBackToChaptersThenSubstantive() {
        List<SectionBasedStructureProposer.Candidate> chapters = List.of(
                new SectionBasedStructureProposer.Candidate("Chapter 1 Intro", SectionBasedStructureProposer.Kind.CHAPTER),
                new SectionBasedStructureProposer.Candidate("Chapter 2 Motion", SectionBasedStructureProposer.Kind.CHAPTER)
        );
        assertThat(SectionBasedStructureProposer.selectTopics(chapters))
                .extracting(SectionBasedStructureProposer.Candidate::title)
                .containsExactly("Chapter 1 Intro", "Chapter 2 Motion");

        List<SectionBasedStructureProposer.Candidate> substantive = List.of(
                new SectionBasedStructureProposer.Candidate("Newton's laws of motion", SectionBasedStructureProposer.Kind.SUBSTANTIVE),
                new SectionBasedStructureProposer.Candidate("Work and energy", SectionBasedStructureProposer.Kind.SUBSTANTIVE)
        );
        assertThat(SectionBasedStructureProposer.selectTopics(substantive)).hasSize(2);
    }

    @Test
    void isNoiseTitle_filtersTocAndPageMarkers() {
        assertThat(SectionBasedStructureProposer.isNoiseTitle("Contents")).isTrue();
        assertThat(SectionBasedStructureProposer.isNoiseTitle("12")).isTrue();
        assertThat(SectionBasedStructureProposer.isNoiseTitle("Page 3")).isTrue();
        assertThat(SectionBasedStructureProposer.isNoiseTitle("Lecture 1 Forces")).isFalse();
    }

    @Test
    void classify_detectsLectureChapterUnit() {
        assertThat(SectionBasedStructureProposer.classify("Lecture 3 Optics"))
                .isEqualTo(SectionBasedStructureProposer.Kind.LECTURE);
        assertThat(SectionBasedStructureProposer.classify("Chapter 2"))
                .isEqualTo(SectionBasedStructureProposer.Kind.CHAPTER);
        assertThat(SectionBasedStructureProposer.classify("Module 4 Waves"))
                .isEqualTo(SectionBasedStructureProposer.Kind.UNIT);
    }
}
