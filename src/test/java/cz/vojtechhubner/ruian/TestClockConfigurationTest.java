package cz.vojtechhubner.ruian;

import static org.assertj.core.api.Assertions.assertThat;

import cz.vojtechhubner.ruian.config.ClockConfig;
import java.time.Clock;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class TestClockConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(ClockConfig.class, TestClockConfiguration.class);

    @Test
    void coexistsWithTheApplicationClockAndIsTheClockThatGetsInjected() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).getBeans(Clock.class).hasSize(2);
            assertThat(context.getBean(Clock.class)).isSameAs(context.getBean(MutableClock.class));
            assertThat(context.getBean(MutableClock.class).instant()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
        });
    }
}
