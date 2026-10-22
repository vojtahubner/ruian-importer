package cz.vojtechhubner.ruian;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * A UTC {@link Clock} for tests that stands still at a chosen instant until a test moves it, so audit timestamps and
 * import run times are predictable. The instant is volatile so that a test may move the clock while the application
 * reads it on another thread.
 */
public final class MutableClock extends Clock {

    private volatile Instant instant;

    public MutableClock(Instant start) {
        this.instant = start;
    }

    public void advance(Duration duration) {
        instant = instant.plus(duration);
    }

    public void setInstant(Instant instant) {
        this.instant = instant;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    /** Returns a fixed clock at this clock's current instant in the given zone. */
    @Override
    public Clock withZone(ZoneId zone) {
        return Clock.fixed(instant, zone);
    }

    @Override
    public Instant instant() {
        return instant;
    }
}
