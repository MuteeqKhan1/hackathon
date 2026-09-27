package com.learningplatform.course.service;

import com.learningplatform.operation.domain.AsyncOperation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Optional post-create bootstrap: enqueue structure generation (PRD-14).
 * Content-per-topic generation remains instructor-triggered or follow-up ops.
 */
@Service
public class CourseBootstrapService {

    private final CourseStructureService courseStructureService;

    public CourseBootstrapService(CourseStructureService courseStructureService) {
        this.courseStructureService = courseStructureService;
    }

    @Transactional
    public AsyncOperation maybeStartStructure(UUID courseId, boolean generateDrafts) {
        if (!generateDrafts) {
            return null;
        }
        return courseStructureService.generateStructureAsync(courseId);
    }
}
