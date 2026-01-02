package io.hexaglue.core.engine;

import java.time.Duration;

/**
 * Metrics from the analysis.
 *
 * @param totalTypes number of types analyzed
 * @param classifiedTypes number of types successfully classified
 * @param portsDetected number of port interfaces detected
 * @param analysisTime total analysis duration
 */
public record EngineMetrics(int totalTypes, int classifiedTypes, int portsDetected, Duration analysisTime) {}
