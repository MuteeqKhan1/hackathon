package com.learningplatform.content.lineage;

import com.learningplatform.content.domain.MappingRelationshipType;
import com.learningplatform.content.domain.SyncPolicy;
import org.springframework.stereotype.Component;

/**
 * Filters which lineage-backed assets should be recommended for auto REGENERATE (PRD-08 / used by PRD-09).
 */
@Component
public class RecommendationFilter {

    public boolean includeForAutoRegenerate(
            MappingRelationshipType relationshipType,
            SyncPolicy syncPolicy,
            String previousChangeSignature,
            String newChangeSignature
    ) {
        if (relationshipType == MappingRelationshipType.CUSTOM) {
            return false;
        }
        if (syncPolicy == SyncPolicy.MANUAL
                && previousChangeSignature != null
                && previousChangeSignature.equals(newChangeSignature)) {
            return false;
        }
        return true;
    }
}
