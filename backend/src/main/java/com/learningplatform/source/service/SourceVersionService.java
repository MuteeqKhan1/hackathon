package com.learningplatform.source.service;

import com.learningplatform.audit.service.AuditService;
import com.learningplatform.common.domain.Permission;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.common.exception.NotFoundException;
import com.learningplatform.common.security.AuthenticatedUser;
import com.learningplatform.common.security.PermissionGuard;
import com.learningplatform.operation.domain.AsyncOperation;
import com.learningplatform.operation.service.OperationService;
import com.learningplatform.source.domain.SourceMaterial;
import com.learningplatform.source.domain.SourceMaterialVersion;
import com.learningplatform.source.domain.SourceVersionStatus;
import com.learningplatform.source.repository.SourceMaterialVersionRepository;
import com.learningplatform.source.storage.ObjectStorage;
import com.learningplatform.source.validation.PdfUploadValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class SourceVersionService {

    private final SourceMaterialService sourceMaterialService;
    private final SourceMaterialVersionRepository versionRepository;
    private final ObjectStorage objectStorage;
    private final PdfUploadValidator pdfUploadValidator;
    private final AuditService auditService;
    private final OperationService operationService;
    private final SourcePublishWorker sourcePublishWorker;

    public SourceVersionService(
            SourceMaterialService sourceMaterialService,
            SourceMaterialVersionRepository versionRepository,
            ObjectStorage objectStorage,
            PdfUploadValidator pdfUploadValidator,
            AuditService auditService,
            OperationService operationService,
            SourcePublishWorker sourcePublishWorker
    ) {
        this.sourceMaterialService = sourceMaterialService;
        this.versionRepository = versionRepository;
        this.objectStorage = objectStorage;
        this.pdfUploadValidator = pdfUploadValidator;
        this.auditService = auditService;
        this.operationService = operationService;
        this.sourcePublishWorker = sourcePublishWorker;
    }

    @Transactional
    public SourceMaterialVersion upload(UUID materialId, MultipartFile file) {
        AuthenticatedUser user = PermissionGuard.require(Permission.SOURCE_CREATE);
        SourceMaterial material = sourceMaterialService.get(materialId);
        pdfUploadValidator.validate(file);

        int nextVersion = versionRepository.findMaxVersionNumber(materialId) + 1;
        String hash;
        try {
            hash = sha256(file.getBytes());
        } catch (IOException e) {
            throw new BusinessException("FILE_READ_FAILED", "Unable to read uploaded file.");
        }

        String storageKey = material.getOrganizationId() + "/" + materialId + "/v" + nextVersion + "-" + UUID.randomUUID() + ".pdf";
        try (InputStream in = file.getInputStream()) {
            objectStorage.put(storageKey, in, file.getSize(), "application/pdf");
        } catch (IOException e) {
            throw new BusinessException("FILE_STORE_FAILED", "Unable to store uploaded file.");
        }

        SourceMaterialVersion version = new SourceMaterialVersion(
                UUID.randomUUID(),
                materialId,
                nextVersion,
                storageKey,
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload.pdf",
                "application/pdf",
                file.getSize(),
                hash,
                SourceVersionStatus.DRAFT,
                user.userId(),
                Instant.now(),
                null
        );
        versionRepository.save(version);
        auditService.record("SOURCE_UPLOADED", "SourceMaterialVersion", version.getId().toString(), storageKey);
        auditService.record("SOURCE_VERSION_CREATED", "SourceMaterialVersion", version.getId().toString(), "v" + nextVersion);
        return version;
    }

    @Transactional(readOnly = true)
    public List<SourceMaterialVersion> listVersions(UUID materialId) {
        PermissionGuard.require(Permission.SOURCE_VERSION_VIEW);
        sourceMaterialService.get(materialId);
        return versionRepository.findBySourceMaterialIdOrderByVersionNumberAsc(materialId);
    }

    @Transactional
    public AsyncOperation publish(UUID materialId, UUID versionId) {
        PermissionGuard.require(Permission.SOURCE_PUBLISH);
        sourceMaterialService.get(materialId);
        SourceMaterialVersion version = versionRepository.findByIdAndSourceMaterialId(versionId, materialId)
                .orElseThrow(() -> new NotFoundException("SOURCE_VERSION_NOT_FOUND", "Source version was not found."));

        if (version.getStatus() == SourceVersionStatus.PUBLISHED) {
            throw new BusinessException("ILLEGAL_STATE", "Version is already published.");
        }
        if (version.getStatus() == SourceVersionStatus.PROCESSING) {
            throw new BusinessException("ILLEGAL_STATE", "Version publish is already in progress.");
        }
        if (version.getStatus() == SourceVersionStatus.FAILED) {
            throw new BusinessException("ILLEGAL_STATE", "Failed versions cannot be published; upload a new version.");
        }

        version.markProcessing();
        versionRepository.save(version);

        AsyncOperation operation = operationService.create("SOURCE_VERSION_PUBLISH");
        UUID operationId = operation.getId();
        // Start async work only after this transaction commits, otherwise worker sees INITIATED forever.
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sourcePublishWorker.processPublish(operationId, materialId, versionId);
                }
            });
        } else {
            sourcePublishWorker.processPublish(operationId, materialId, versionId);
        }
        return operation;
    }

    public static String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
