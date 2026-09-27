package com.learningplatform.student.service;

import com.learningplatform.student.domain.StudentMastery;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MasteryServiceTest {

    @Test
    void afterAttempts_masteryUpdated_noSlowFastField() throws Exception {
        StudentMastery mastery = new StudentMastery(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Instant.now()
        );
        mastery.recordAttempt(BigDecimal.valueOf(100));
        mastery.recordAttempt(BigDecimal.valueOf(50));

        assertThat(mastery.getAttempts()).isEqualTo(2);
        assertThat(mastery.getAverageQuizScore()).isEqualByComparingTo("75.00");
        assertThat(mastery.getMasteryScore()).isEqualByComparingTo("75.00");

        boolean hasSlowFast = Arrays.stream(StudentMastery.class.getDeclaredFields())
                .map(Field::getName)
                .anyMatch(n -> n.toLowerCase().contains("slow") || n.toLowerCase().contains("fast"));
        assertThat(hasSlowFast).isFalse();
    }
}
