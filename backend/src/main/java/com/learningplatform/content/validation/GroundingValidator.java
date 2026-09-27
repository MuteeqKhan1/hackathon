package com.learningplatform.content.validation;

import com.learningplatform.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class GroundingValidator {

    public void validateCitedSubset(List<UUID> citedSectionIds, Set<UUID> retrievedSectionIds) {
        Set<UUID> allowed = retrievedSectionIds == null ? Set.of() : retrievedSectionIds;
        for (UUID cited : citedSectionIds) {
            if (!allowed.contains(cited)) {
                throw new BusinessException(
                        "GROUNDING_VIOLATION",
                        "Cited section " + cited + " was not in the retrieved source set."
                );
            }
        }
    }

    public Set<UUID> asSet(List<UUID> ids) {
        return new HashSet<>(ids);
    }
}
