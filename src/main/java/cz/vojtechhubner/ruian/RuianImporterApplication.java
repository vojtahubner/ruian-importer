package cz.vojtechhubner.ruian;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the RÚIAN importer service.
 */
@SpringBootApplication
public class RuianImporterApplication {

    public static void main(String[] args) {
        SpringApplication.run(RuianImporterApplication.class, args);
    }
}
