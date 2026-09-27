package com.learningplatform.course.service;

import com.learningplatform.audit.service.AuditService;
import com.learningplatform.common.domain.Permission;
import com.learningplatform.common.security.PermissionGuard;
import com.learningplatform.course.domain.Course;
import com.learningplatform.course.structure.SectionBasedStructureProposer;
import com.learningplatform.course.structure.StructureApplier;
import com.learningplatform.course.structure.StructureProposal;
import com.learningplatform.operation.domain.AsyncOperation;
import com.learningplatform.operation.service.OperationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

@Service
public class CourseStructureService {

    private final CourseService courseService;
    private final OperationService operationService;
    private final CourseStructureWorker courseStructureWorker;
    private final StructureApplier structureApplier;
    private final SectionBasedStructureProposer structureProposer;
    private final AuditService auditService;

    public CourseStructureService(
            CourseService courseService,
            OperationService operationService,
            CourseStructureWorker courseStructureWorker,
            StructureApplier structureApplier,
            SectionBasedStructureProposer structureProposer,
            AuditService auditService
    ) {
        this.courseService = courseService;
        this.operationService = operationService;
        this.courseStructureWorker = courseStructureWorker;
        this.structureApplier = structureApplier;
        this.structureProposer = structureProposer;
        this.auditService = auditService;
    }

    @Transactional
    public AsyncOperation generateStructureAsync(UUID courseId) {
        PermissionGuard.require(Permission.COURSE_EDIT);
        Course course = courseService.get(courseId);
        course.assertEditableStructure();

        AsyncOperation operation = operationService.create("COURSE_STRUCTURE_GENERATE");
        UUID operationId = operation.getId();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    courseStructureWorker.process(operationId, courseId);
                }
            });
        } else {
            courseStructureWorker.process(operationId, courseId);
        }
        return operation;
    }

    @Transactional
    public StructureApplier.AppliedStructure applyProposal(Course course, StructureProposal proposal) {
        StructureApplier.AppliedStructure applied = structureApplier.apply(course, proposal);
        auditService.record(
                "COURSE_STRUCTURE_APPLIED",
                "Course",
                course.getId().toString(),
                "chapters=" + applied.chapters().size() + ",topics=" + applied.topics().size()
        );
        return applied;
    }

    @Transactional
    public StructureApplier.AppliedStructure generateFromSourceSections(Course course) {
        return applyProposal(course, structureProposer.propose(course));
    }
}
