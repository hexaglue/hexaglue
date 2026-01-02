package io.hexaglue.spi.ir;

import java.time.Instant;

/**
 * Metadata about the IR analysis.
 *
 * @param basePackage the base package that was analyzed
 * @param timestamp when the analysis was performed
 * @param engineVersion the HexaGlue engine version
 * @param typeCount total number of types analyzed
 * @param portCount total number of ports detected
 */
public record IrMetadata(String basePackage, Instant timestamp, String engineVersion, int typeCount, int portCount) {}
