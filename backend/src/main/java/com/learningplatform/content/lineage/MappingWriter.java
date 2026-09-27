package com.learningplatform.content.lineage;

import com.learningplatform.content.domain.ContentSourceMapping;
import com.learningplatform.content.domain.MappingRelationshipType;
import com.learningplatform.content.repository.ContentSourceMappingRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class MappingWriter {

    private final ContentSourceMappingRepository mappingRepository;

    public MappingWriter(ContentSourceMappingRepository mappingRepository) {
        this.mappingRepository = mappingRepository;
    }

    /**
     * Writes one mapping per cited section. First = PRIMARY, rest = SUPPORTING.
     */
    public List<ContentSourceMapping> write(
            UUID contentAssetId,
            UUID contentAssetVersionId,
            List<UUID> citedSectionIds,
            Map<UUID, UUID> sectionIdToSectionVersionId
    ) {
        List<ContentSourceMapping> saved = new ArrayList<>();
        int i = 0;
        for (UUID sectionId : citedSectionIds) {
            UUID sectionVersionId = sectionIdToSectionVersionId.get(sectionId);
            if (sectionVersionId == null) {
                continue;
            }
            MappingRelationshipType type = i == 0 ? MappingRelationshipType.PRIMARY : MappingRelationshipType.SUPPORTING;
            ContentSourceMapping mapping = new ContentSourceMapping(
                    UUID.randomUUID(),
                    contentAssetId,
                    contentAssetVersionId,
                    sectionId,
                    sectionVersionId,
                    type
            );
            mappingRepository.save(mapping);
            saved.add(mapping);
            i++;
        }
        return saved;
    }
}
