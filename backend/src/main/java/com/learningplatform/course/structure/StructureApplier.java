package com.learningplatform.course.structure;

import com.learningplatform.course.domain.Course;
import com.learningplatform.course.domain.CourseChapter;
import com.learningplatform.course.domain.CourseTopic;
import com.learningplatform.course.domain.StructureNodeStatus;
import com.learningplatform.course.repository.CourseChapterRepository;
import com.learningplatform.course.repository.CourseTopicRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Applies a structure proposal to a course, replacing prior chapters/topics.
 * Stable ids are assigned at apply time; later reorders must preserve them.
 */
@Component
public class StructureApplier {

    private final CourseChapterRepository chapterRepository;
    private final CourseTopicRepository topicRepository;

    public StructureApplier(CourseChapterRepository chapterRepository, CourseTopicRepository topicRepository) {
        this.chapterRepository = chapterRepository;
        this.topicRepository = topicRepository;
    }

    @Transactional
    public AppliedStructure apply(Course course, StructureProposal proposal) {
        if (proposal == null || proposal.chapters() == null || proposal.chapters().isEmpty()) {
            throw new IllegalArgumentException("Structure proposal must contain at least one chapter");
        }

        List<CourseChapter> existing = chapterRepository.findByCourseIdOrderBySequenceAsc(course.getId());
        if (!existing.isEmpty()) {
            topicRepository.deleteByChapterIdIn(existing.stream().map(CourseChapter::getId).toList());
            chapterRepository.deleteByCourseId(course.getId());
            chapterRepository.flush();
        }

        List<CourseChapter> chapters = new ArrayList<>();
        List<CourseTopic> topics = new ArrayList<>();
        int chapterSeq = 1;
        for (StructureProposal.ProposedChapter proposedChapter : proposal.chapters()) {
            if (proposedChapter.title() == null || proposedChapter.title().isBlank()) {
                throw new IllegalArgumentException("Chapter title must not be blank");
            }
            CourseChapter chapter = new CourseChapter(
                    UUID.randomUUID(),
                    course.getId(),
                    proposedChapter.title().trim(),
                    chapterSeq++,
                    StructureNodeStatus.DRAFT
            );
            chapterRepository.save(chapter);
            chapters.add(chapter);

            List<StructureProposal.ProposedTopic> proposedTopics = proposedChapter.topics() == null
                    ? List.of()
                    : proposedChapter.topics();
            if (proposedTopics.isEmpty()) {
                throw new IllegalArgumentException("Each chapter must contain at least one topic");
            }
            int topicSeq = 1;
            for (StructureProposal.ProposedTopic proposedTopic : proposedTopics) {
                if (proposedTopic.title() == null || proposedTopic.title().isBlank()) {
                    throw new IllegalArgumentException("Topic title must not be blank");
                }
                CourseTopic topic = new CourseTopic(
                        UUID.randomUUID(),
                        chapter.getId(),
                        proposedTopic.title().trim(),
                        topicSeq++,
                        StructureNodeStatus.DRAFT
                );
                topicRepository.save(topic);
                topics.add(topic);
            }
        }
        return new AppliedStructure(chapters, topics);
    }

    public record AppliedStructure(List<CourseChapter> chapters, List<CourseTopic> topics) {
    }
}
