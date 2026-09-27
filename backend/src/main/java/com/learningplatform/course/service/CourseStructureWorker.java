package com.learningplatform.course.service;

import com.learningplatform.audit.service.AuditService;
import com.learningplatform.common.exception.NotFoundException;
import com.learningplatform.course.domain.Course;
import com.learningplatform.course.repository.CourseRepository;
import com.learningplatform.course.structure.SectionBasedStructureProposer;
import com.learningplatform.course.structure.StructureApplier;
import com.learningplatform.course.structure.StructureProposal;
import com.learningplatform.operation.service.OperationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class CourseStructureWorker {

    private static final Logger log = LoggerFactory.getLogger(CourseStructureWorker.class);

    private final CourseRepository courseRepository;
    private final SectionBasedStructureProposer structureProposer;
    private final StructureApplier structureApplier;
    private final OperationService operationService;
    private final AuditService auditService;

    public CourseStructureWorker(
            CourseRepository courseRepository,
            SectionBasedStructureProposer structureProposer,
            StructureApplier structureApplier,
            OperationService operationService,
            AuditService auditService
    ) {
        this.courseRepository = courseRepository;
        this.structureProposer = structureProposer;
        this.structureApplier = structureApplier;
        this.operationService = operationService;
        this.auditService = auditService;
    }

    @Async
    @Transactional
    public void process(UUID operationId, UUID courseId) {
        try {
            operationService.markRunning(operationId, 10);
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new NotFoundException("COURSE_NOT_FOUND", "Course was not found."));
            operationService.markRunning(operationId, 40);
            StructureProposal proposal = structureProposer.propose(course);
            StructureApplier.AppliedStructure applied = structureApplier.apply(course, proposal);
            auditService.record(
                    "COURSE_STRUCTURE_APPLIED",
                    "Course",
                    courseId.toString(),
                    "chapters=" + applied.chapters().size() + ",topics=" + applied.topics().size()
            );
            String result = "{\"courseId\":\"" + courseId + "\",\"chapters\":" + applied.chapters().size()
                    + ",\"topics\":" + applied.topics().size() + "}";
            operationService.markCompleted(operationId, result);
        } catch (Exception ex) {
            log.warn("Course structure generation failed for course {}: {}", courseId, ex.getMessage());
            String message = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            operationService.markFailed(operationId, "COURSE_STRUCTURE_FAILED", message);
        }
    }
}
