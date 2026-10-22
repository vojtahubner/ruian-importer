/**
 * RÚIAN importer: downloads a municipality's VFR XML, extracts the municipality and its parts and stores them in
 * PostgreSQL. The code is organised hexagonally: {@code domain} (model and ports), {@code application} (use cases),
 * {@code adapter} (inbound web/startup, outbound HTTP/XML/persistence) and {@code config}.
 */
@NullMarked
package cz.vojtechhubner.ruian;

import org.jspecify.annotations.NullMarked;
