package com.greentraffic.common.util;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class TimezoneUtilsTest {

    @Test
    void returnsCurrentSystemDefaultZone() {
        assertThat(TimezoneUtils.getSystemZone()).isEqualTo(ZoneId.systemDefault());
    }

    @Test
    void formatsNullInstantAsFluxNowExpression() {
        assertThat(TimezoneUtils.formatForFlux(null)).isEqualTo("now()");
    }

    @Test
    void formatsInstantWithSystemZoneOffset() {
        Instant instant = Instant.parse("2026-08-22T10:00:00Z");
        String expected = instant.atZone(ZoneId.systemDefault()).toOffsetDateTime().toString();

        assertThat(TimezoneUtils.formatForFlux(instant)).isEqualTo(expected);
    }

    @Test
    void convertsLocalDateTimeUsingSystemZone() {
        LocalDateTime localDateTime = LocalDateTime.of(2026, 8, 22, 18, 30, 15);
        Instant expected = localDateTime.atZone(ZoneId.systemDefault()).toInstant();

        assertThat(TimezoneUtils.toInstant(localDateTime)).isEqualTo(expected);
    }

    @Test
    void returnsNullForNullLocalDateTime() {
        assertThat(TimezoneUtils.toInstant(null)).isNull();
    }

    @Test
    void preservesInstantDuringNormalization() {
        Instant instant = Instant.parse("2026-08-22T10:00:00Z");

        assertThat(TimezoneUtils.normalizeInstant(instant)).isSameAs(instant);
        assertThat(TimezoneUtils.normalizeInstant(null)).isNull();
    }
}