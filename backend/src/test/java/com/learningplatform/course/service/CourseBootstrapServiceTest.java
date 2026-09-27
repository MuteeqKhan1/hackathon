package com.learningplatform.course.service;

import com.learningplatform.operation.domain.AsyncOperation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseBootstrapServiceTest {

    @Mock private CourseStructureService courseStructureService;
    @InjectMocks private CourseBootstrapService bootstrapService;

    @Test
    void generateDraftsFalse_noOp() {
        assertThat(bootstrapService.maybeStartStructure(UUID.randomUUID(), false)).isNull();
        verify(courseStructureService, never()).generateStructureAsync(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void generateDraftsTrue_enqueuesStructure() {
        UUID courseId = UUID.randomUUID();
        AsyncOperation op = mock(AsyncOperation.class);
        when(courseStructureService.generateStructureAsync(courseId)).thenReturn(op);
        assertThat(bootstrapService.maybeStartStructure(courseId, true)).isEqualTo(op);
    }
}
