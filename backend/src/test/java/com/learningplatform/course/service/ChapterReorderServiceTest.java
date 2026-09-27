package com.learningplatform.course.service;

import com.learningplatform.common.domain.Permission;
import com.learningplatform.common.domain.UserRole;
import com.learningplatform.common.security.AuthenticatedUser;
import com.learningplatform.common.security.SecurityContext;
import com.learningplatform.course.domain.Course;
import com.learningplatform.course.domain.CourseChapter;
import com.learningplatform.course.domain.CourseStatus;
import com.learningplatform.course.domain.CourseTopic;
import com.learningplatform.course.domain.StructureNodeStatus;
import com.learningplatform.course.repository.CourseChapterRepository;
import com.learningplatform.course.repository.CourseTopicRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChapterReorderServiceTest {

    private static final UUID ORG = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID COURSE_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID CHAPTER_ID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");

    @Mock
    private CourseService courseService;
    @Mock
    private CourseChapterRepository chapterRepository;
    @Mock
    private CourseTopicRepository topicRepository;

    @InjectMocks
    private ChapterReorderService chapterReorderService;

    @BeforeEach
    void auth() {
        SecurityContext.set(new AuthenticatedUser(USER, ORG, UserRole.INSTRUCTOR, Permission.forRole(UserRole.INSTRUCTOR)));
    }

    @AfterEach
    void clear() {
        SecurityContext.clear();
    }

    @Test
    void moveTopicToIndex_sequencesCompactUnique() {
        Course course = new Course(
                COURSE_ID, ORG, "Physics", null, UUID.randomUUID(), UUID.randomUUID(),
                "en", null, null, null, CourseStatus.DRAFT, USER, Instant.now(), Instant.now()
        );
        CourseChapter chapter = new CourseChapter(CHAPTER_ID, COURSE_ID, "Ch1", 1, StructureNodeStatus.DRAFT);
        CourseTopic t1 = new CourseTopic(UUID.randomUUID(), CHAPTER_ID, "A", 1, StructureNodeStatus.DRAFT);
        CourseTopic t2 = new CourseTopic(UUID.randomUUID(), CHAPTER_ID, "B", 2, StructureNodeStatus.DRAFT);
        CourseTopic t3 = new CourseTopic(UUID.randomUUID(), CHAPTER_ID, "C", 3, StructureNodeStatus.DRAFT);

        when(courseService.get(COURSE_ID)).thenReturn(course);
        when(chapterRepository.findByIdAndCourseId(CHAPTER_ID, COURSE_ID)).thenReturn(Optional.of(chapter));
        when(topicRepository.findByChapterIdOrderBySequenceAsc(CHAPTER_ID))
                .thenReturn(new ArrayList<>(List.of(t1, t2, t3)))
                .thenReturn(List.of(t3, t1, t2));
        when(topicRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<CourseTopic> reordered = chapterReorderService.moveTopicToIndex(COURSE_ID, CHAPTER_ID, t3.getId(), 0);

        assertThat(reordered).extracting(CourseTopic::getTitle).containsExactly("C", "A", "B");
        assertThat(reordered).extracting(CourseTopic::getSequence).containsExactly(1, 2, 3);
        assertThat(reordered).extracting(CourseTopic::getId).containsExactly(t3.getId(), t1.getId(), t2.getId());
    }
}
