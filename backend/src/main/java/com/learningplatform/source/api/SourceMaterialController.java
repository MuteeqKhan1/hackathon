package com.learningplatform.source.api;

import com.learningplatform.operation.domain.AsyncOperation;
import com.learningplatform.operation.domain.OperationStatus;
import com.learningplatform.source.domain.SourceMaterial;
import com.learningplatform.source.domain.SourceMaterialStatus;
import com.learningplatform.source.domain.SourceMaterialVersion;
import com.learningplatform.source.domain.SourceVersionStatus;
import com.learningplatform.source.parsing.SectionChangeType;
import com.learningplatform.source.service.SourceMaterialService;
import com.learningplatform.source.service.SourceSectionQueryService;
import com.learningplatform.source.service.SourceVersionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/source-materials")
public class SourceMaterialController {

    private final SourceMaterialService sourceMaterialService;
    private final SourceVersionService sourceVersionService;
    private final SourceSectionQueryService sourceSectionQueryService;

    public SourceMaterialController(
            SourceMaterialService sourceMaterialService,
            SourceVersionService sourceVersionService,
            SourceSectionQueryService sourceSectionQueryService
    ) {
        this.sourceMaterialService = sourceMaterialService;
        this.sourceVersionService = sourceVersionService;
        this.sourceSectionQueryService = sourceSectionQueryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SourceMaterialResponse create(@Valid @RequestBody CreateSourceMaterialRequest request) {
        return SourceMaterialResponse.from(
                sourceMaterialService.create(request.title(), request.description(), request.subject())
        );
    }

    @GetMapping
    public List<SourceMaterialResponse> list() {
        return sourceMaterialService.listForCurrentOrg().stream().map(SourceMaterialResponse::from).toList();
    }

    @GetMapping("/{materialId}")
    public SourceMaterialResponse get(@PathVariable UUID materialId) {
        return SourceMaterialResponse.from(sourceMaterialService.get(materialId));
    }

    @PostMapping(path = "/{materialId}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public SourceVersionResponse uploadVersion(
            @PathVariable UUID materialId,
            @RequestPart("file") MultipartFile file
    ) {
        return SourceVersionResponse.from(sourceVersionService.upload(materialId, file));
    }

    @GetMapping("/{materialId}/versions")
    public List<SourceVersionResponse> listVersions(@PathVariable UUID materialId) {
        return sourceVersionService.listVersions(materialId).stream().map(SourceVersionResponse::from).toList();
    }

    @GetMapping("/{materialId}/versions/{versionId}/sections")
    public List<SectionResponse> listSections(
            @PathVariable UUID materialId,
            @PathVariable UUID versionId
    ) {
        return sourceSectionQueryService.listSections(materialId, versionId).stream()
                .map(SectionResponse::from)
                .toList();
    }

    @PostMapping("/{materialId}/versions/{versionId}/publish")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public OperationAcceptedResponse publish(
            @PathVariable UUID materialId,
            @PathVariable UUID versionId
    ) {
        AsyncOperation op = sourceVersionService.publish(materialId, versionId);
        return new OperationAcceptedResponse(op.getId(), op.getStatus(), op.getProgress());
    }

    public record CreateSourceMaterialRequest(
            @NotBlank String title,
            String description,
            String subject
    ) {
    }

    public record SourceMaterialResponse(
            UUID id,
            UUID organizationId,
            String title,
            String description,
            String subject,
            SourceMaterialStatus status,
            UUID createdBy,
            Instant createdAt
    ) {
        static SourceMaterialResponse from(SourceMaterial material) {
            return new SourceMaterialResponse(
                    material.getId(),
                    material.getOrganizationId(),
                    material.getTitle(),
                    material.getDescription(),
                    material.getSubject(),
                    material.getStatus(),
                    material.getCreatedBy(),
                    material.getCreatedAt()
            );
        }
    }

    public record SourceVersionResponse(
            UUID id,
            UUID sourceMaterialId,
            int versionNumber,
            String originalFilename,
            String fileContentHash,
            long fileSizeBytes,
            SourceVersionStatus status,
            Instant createdAt,
            Instant publishedAt
    ) {
        static SourceVersionResponse from(SourceMaterialVersion version) {
            return new SourceVersionResponse(
                    version.getId(),
                    version.getSourceMaterialId(),
                    version.getVersionNumber(),
                    version.getOriginalFilename(),
                    version.getFileContentHash(),
                    version.getFileSizeBytes(),
                    version.getStatus(),
                    version.getCreatedAt(),
                    version.getPublishedAt()
            );
        }
    }

    public record OperationAcceptedResponse(UUID operationId, OperationStatus status, int progress) {
    }

    public record SectionResponse(
            UUID sectionId,
            String externalReference,
            String title,
            String sectionType,
            String contentHash,
            SectionChangeType changeType,
            String contentPreview,
            int chunkCount
    ) {
        static SectionResponse from(SourceSectionQueryService.SectionView view) {
            return new SectionResponse(
                    view.sectionId(),
                    view.externalReference(),
                    view.title(),
                    view.sectionType(),
                    view.contentHash(),
                    view.changeType(),
                    view.contentPreview(),
                    view.chunkCount()
            );
        }
    }
}
