package cz.vojtechhubner.ruian.application;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

/**
 * Settings of the municipality import, bound from the {@code ruian.import.*} properties.
 *
 * @param defaultSourceUrl     the VFR file downloaded when a request names no source
 * @param allowedHosts         hosts a source URL may point to
 * @param onStartup            whether the default source is imported when the application starts
 * @param maxUncompressedBytes the most XML the parser reads from one source, after decompression
 */
@ConfigurationProperties("ruian.import")
@Validated
public record ImportProperties(
        @NotNull URI defaultSourceUrl,
        @NotEmpty List<String> allowedHosts,
        boolean onStartup,
        @NotNull DataSize maxUncompressedBytes) {
}
