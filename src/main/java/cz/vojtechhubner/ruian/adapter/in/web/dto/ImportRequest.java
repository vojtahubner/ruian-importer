package cz.vojtechhubner.ruian.adapter.in.web.dto;

import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

/**
 * Body of a request to run an import; {@code sourceUrl} is optional and defaults to the configured source.
 */
public record ImportRequest(@Nullable @Size(max = 2048) String sourceUrl) {
}
