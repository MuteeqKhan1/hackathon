package com.learningplatform.sync.service;

import com.learningplatform.audit.service.AuditService;
import com.learningplatform.sync.domain.SyncEvent;
import com.learningplatform.sync.domain.SyncEventStatus;
import com.learningplatform.sync.repository.SyncEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SyncEventFactoryTest {

    @Mock private SyncEventRepository syncEventRepository;
    @Mock private AuditService auditService;

    @InjectMocks
    private SyncEventFactory factory;

    @Test
    void firstEvent_detectedWithIdempotencyKey() {
        UUID org = UUID.randomUUID();
        UUID material = UUID.randomUUID();
        UUID oldV = UUID.randomUUID();
        UUID newV = UUID.randomUUID();
        String key = SyncEventFactory.idempotencyKey(material, oldV, newV);

        when(syncEventRepository.findByIdempotencyKey(key)).thenReturn(Optional.empty());
        when(syncEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SyncEvent event = factory.createOrGet(org, material, oldV, newV);

        assertThat(event.getStatus()).isEqualTo(SyncEventStatus.DETECTED);
        assertThat(event.getIdempotencyKey()).isEqualTo(key);
        verify(auditService).record(any(), any(), any(), any());
    }

    @Test
    void duplicateKey_returnsExisting_noSecondRow() {
        UUID org = UUID.randomUUID();
        UUID material = UUID.randomUUID();
        UUID oldV = UUID.randomUUID();
        UUID newV = UUID.randomUUID();
        String key = SyncEventFactory.idempotencyKey(material, oldV, newV);
        SyncEvent existing = new SyncEvent(
                UUID.randomUUID(), org, material, oldV, newV, SyncEventStatus.NOTIFIED, key, java.time.Instant.now()
        );

        when(syncEventRepository.findByIdempotencyKey(key)).thenReturn(Optional.of(existing));

        SyncEvent first = factory.createOrGet(org, material, oldV, newV);
        SyncEvent second = factory.createOrGet(org, material, oldV, newV);

        assertThat(first.getId()).isEqualTo(existing.getId());
        assertThat(second.getId()).isEqualTo(existing.getId());
        verify(syncEventRepository, never()).save(any());
    }
}
