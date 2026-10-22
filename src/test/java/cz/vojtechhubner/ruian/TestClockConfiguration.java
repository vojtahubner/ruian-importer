package cz.vojtechhubner.ruian;

import java.time.Instant;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Gives tests a {@link MutableClock} starting at 2026-01-01T00:00:00Z. It is primary so that it also wins over the
 * application clock in full-context tests; it is not named {@code clock} because a second bean definition with
 * that name would be rejected, bean definition overriding being disabled.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestClockConfiguration {

    @Bean
    @Primary
    MutableClock testClock() {
        return new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
    }
}
