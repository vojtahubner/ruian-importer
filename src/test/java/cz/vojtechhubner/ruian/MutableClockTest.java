package cz.vojtechhubner.ruian;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class MutableClockTest {

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void standsStillAtTheStartInstantInUtc() {
        var clock = new MutableClock(START);

        assertThat(clock.instant()).isEqualTo(START);
        assertThat(clock.getZone()).isEqualTo(ZoneOffset.UTC);
    }

    @Test
    void advanceMovesTheInstantForward() {
        var clock = new MutableClock(START);

        clock.advance(Duration.ofHours(1));

        assertThat(clock.instant()).isEqualTo(Instant.parse("2026-01-01T01:00:00Z"));
    }

    @Test
    void setInstantMovesTheClockToTheGivenInstant() {
        var clock = new MutableClock(START);

        clock.setInstant(Instant.parse("2026-06-15T12:00:00Z"));

        assertThat(clock.instant()).isEqualTo(Instant.parse("2026-06-15T12:00:00Z"));
    }

    @Test
    void withZoneKeepsTheInstantAndTakesTheGivenZone() {
        var clock = new MutableClock(START);

        var prague = clock.withZone(ZoneId.of("Europe/Prague"));

        assertThat(prague.instant()).isEqualTo(START);
        assertThat(prague.getZone()).isEqualTo(ZoneId.of("Europe/Prague"));
    }
}
