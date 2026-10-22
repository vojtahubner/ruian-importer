package cz.vojtechhubner.ruian;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class ApplicationIT {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void startsAgainstPostgres() {
        assertThat(jdbcTemplate.queryForObject("select 1", Integer.class)).isEqualTo(1);
    }
}
