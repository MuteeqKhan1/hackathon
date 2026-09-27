package com.learningplatform.content.lineage;

import com.learningplatform.content.domain.ContentSourceMapping;
import com.learningplatform.content.domain.MappingRelationshipType;
import com.learningplatform.content.repository.ContentSourceMappingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MappingWriterTest {

    @Mock
    private ContentSourceMappingRepository mappingRepository;

    @InjectMocks
    private MappingWriter mappingWriter;

    @Test
    void twoSections_primaryThenSupporting() {
        when(mappingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        UUID s1 = UUID.randomUUID();
        UUID s2 = UUID.randomUUID();
        UUID sv1 = UUID.randomUUID();
        UUID sv2 = UUID.randomUUID();

        List<ContentSourceMapping> mappings = mappingWriter.write(
                UUID.randomUUID(),
                UUID.randomUUID(),
                List.of(s1, s2),
                Map.of(s1, sv1, s2, sv2)
        );

        assertThat(mappings).hasSize(2);
        assertThat(mappings.get(0).getRelationshipType()).isEqualTo(MappingRelationshipType.PRIMARY);
        assertThat(mappings.get(1).getRelationshipType()).isEqualTo(MappingRelationshipType.SUPPORTING);
        verify(mappingRepository, times(2)).save(any());
    }
}
