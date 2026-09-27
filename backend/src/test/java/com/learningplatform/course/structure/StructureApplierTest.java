package com.learningplatform.course.structure;

import com.learningplatform.course.domain.Course;
import com.learningplatform.course.domain.CourseChapter;
import com.learningplatform.course.domain.CourseStatus;
import com.learningplatform.course.domain.CourseTopic;
import com.learningplatform.course.repository.CourseChapterRepository;
import com.learningplatform.course.repository.CourseTopicRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StructureApplierTest {

    @Mock
    private CourseChapterRepository chapterRepository;
    @Mock
    private CourseTopicRepository topicRepository;

    @InjectMocks
    private StructureApplier structureApplier;

    @Test
    void apply_aiStructureDto_chaptersAndTopicsSequencedFrom1() {
        Course course = new Course(
                UUID.randomUUID(), UUID.randomUUID(), "Physics", null,
                UUID.randomUUID(), UUID.randomUUID(), "en", null, null, null,
                CourseStatus.DRAFT, UUID.randomUUID(), Instant.now(), Instant.now()
        );
        when(chapterRepository.findByCourseIdOrderBySequenceAsc(course.getId())).thenReturn(List.of());
        when(chapterRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(topicRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        StructureProposal proposal = new StructureProposal(List.of(
                new StructureProposal.ProposedChapter("Mechanics", List.of(
                        new StructureProposal.ProposedTopic("Newton 1"),
                        new StructureProposal.ProposedTopic("Newton 2")
                )),
                new StructureProposal.ProposedChapter("Energy", List.of(
                        new StructureProposal.ProposedTopic("Kinetic")
                ))
        ));

        StructureApplier.AppliedStructure applied = structureApplier.apply(course, proposal);

        assertThat(applied.chapters()).extracting(CourseChapter::getSequence).containsExactly(1, 2);
        assertThat(applied.topics()).extracting(CourseTopic::getSequence).containsExactly(1, 2, 1);

        ArgumentCaptor<CourseChapter> chapterCaptor = ArgumentCaptor.forClass(CourseChapter.class);
        verify(chapterRepository, times(2)).save(chapterCaptor.capture());
        assertThat(chapterCaptor.getAllValues()).extracting(CourseChapter::getTitle)
                .containsExactly("Mechanics", "Energy");
    }
}
