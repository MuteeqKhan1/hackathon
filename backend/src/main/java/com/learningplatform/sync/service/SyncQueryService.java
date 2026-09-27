package com.learningplatform.sync.service;

import com.learningplatform.common.domain.Permission;
import com.learningplatform.common.exception.NotFoundException;
import com.learningplatform.common.security.AuthenticatedUser;
import com.learningplatform.common.security.PermissionGuard;
import com.learningplatform.sync.domain.ImpactAnalysis;
import com.learningplatform.sync.domain.ImpactStatus;
import com.learningplatform.sync.domain.SyncEvent;
import com.learningplatform.sync.repository.ImpactAnalysisRepository;
import com.learningplatform.sync.repository.SyncEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SyncQueryService {

    private final SyncEventRepository syncEventRepository;
    private final ImpactAnalysisRepository impactAnalysisRepository;

    public SyncQueryService(
            SyncEventRepository syncEventRepository,
            ImpactAnalysisRepository impactAnalysisRepository
    ) {
        this.syncEventRepository = syncEventRepository;
        this.impactAnalysisRepository = impactAnalysisRepository;
    }

    @Transactional(readOnly = true)
    public List<SyncEvent> listEvents() {
        AuthenticatedUser user = PermissionGuard.require(Permission.IMPACT_VIEW);
        return syncEventRepository.findByOrganizationIdOrderByCreatedAtDesc(user.organizationId());
    }

    @Transactional(readOnly = true)
    public SyncEvent getEvent(UUID id) {
        AuthenticatedUser user = PermissionGuard.require(Permission.IMPACT_VIEW);
        SyncEvent event = syncEventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("SYNC_EVENT_NOT_FOUND", "Sync event was not found."));
        if (!event.getOrganizationId().equals(user.organizationId())) {
            throw new NotFoundException("SYNC_EVENT_NOT_FOUND", "Sync event was not found.");
        }
        return event;
    }

    /**
     * One row per asset for the instructor UI (collapses legacy per-section duplicate rows).
     */
    @Transactional(readOnly = true)
    public List<ImpactAnalysis> listImpacts(UUID syncEventId) {
        getEvent(syncEventId);
        List<ImpactAnalysis> all = impactAnalysisRepository.findBySyncEventId(syncEventId);
        Map<UUID, ImpactAnalysis> byAsset = new LinkedHashMap<>();
        List<ImpactAnalysis> ordered = new ArrayList<>(all);
        ordered.sort(Comparator
                .comparing((ImpactAnalysis i) -> i.getStatus().name())
                .thenComparing(i -> -i.getImpactLevel().ordinal())
                .thenComparing(ImpactAnalysis::getCreatedAt));
        for (ImpactAnalysis impact : ordered) {
            ImpactAnalysis existing = byAsset.get(impact.getContentAssetId());
            if (existing == null) {
                byAsset.put(impact.getContentAssetId(), impact);
                continue;
            }
            // Prefer OPEN over CLOSED when collapsing duplicates for display.
            if (existing.getStatus() != ImpactStatus.OPEN && impact.getStatus() == ImpactStatus.OPEN) {
                byAsset.put(impact.getContentAssetId(), impact);
            }
        }
        return new ArrayList<>(byAsset.values());
    }
}
