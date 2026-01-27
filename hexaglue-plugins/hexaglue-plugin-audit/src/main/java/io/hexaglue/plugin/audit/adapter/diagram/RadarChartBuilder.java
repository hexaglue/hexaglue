/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Commercial licensing options are available for organizations wishing
 * to use HexaGlue under terms different from the MPL 2.0.
 * Contact: info@hexaglue.io
 */

package io.hexaglue.plugin.audit.adapter.diagram;

import io.hexaglue.plugin.audit.domain.model.report.ScoreBreakdown;

/**
 * Builds Mermaid radar-beta charts for score visualization.
 *
 * <p>Generates a radar chart showing compliance scores across multiple
 * dimensions with threshold lines for easy comparison.
 *
 * @since 5.0.0
 */
public class RadarChartBuilder {

    private static final int DEFAULT_THRESHOLD_DDD = 90;
    private static final int DEFAULT_THRESHOLD_HEX = 90;
    private static final int DEFAULT_THRESHOLD_DEP = 80;
    private static final int DEFAULT_THRESHOLD_CPL = 70;
    private static final int DEFAULT_THRESHOLD_COH = 80;

    /**
     * Builds a radar chart showing scores by dimension.
     *
     * @param breakdown score breakdown by dimension
     * @return Mermaid radar-beta diagram code (without code fence)
     */
    public String build(ScoreBreakdown breakdown) {
        return """
---
config:
  radar:
    curveTension: 0.1
  themeVariables:
    cScale0: "#81C784"
    cScale1: "#3F51B5"
---
radar-beta
    title Architecture Compliance by Dimension
    max 100
    axis ddd["DDD (%d%%)"], hex["Hexagonal (%d%%)"], dep["Dependencies (%d%%)"], cpl["Coupling (%d%%)"], coh["Cohesion (%d%%)"]
    curve target["Threshold"]{%d, %d, %d, %d, %d}
    curve score["Current Score"]{%d, %d, %d, %d, %d}"""
                .formatted(
                        breakdown.dddCompliance().weight(),
                        breakdown.hexagonalCompliance().weight(),
                        breakdown.dependencyQuality().weight(),
                        breakdown.couplingMetrics().weight(),
                        breakdown.cohesionQuality().weight(),
                        DEFAULT_THRESHOLD_DDD,
                        DEFAULT_THRESHOLD_HEX,
                        DEFAULT_THRESHOLD_DEP,
                        DEFAULT_THRESHOLD_CPL,
                        DEFAULT_THRESHOLD_COH,
                        breakdown.dddCompliance().score(),
                        breakdown.hexagonalCompliance().score(),
                        breakdown.dependencyQuality().score(),
                        breakdown.couplingMetrics().score(),
                        breakdown.cohesionQuality().score());
    }

    /**
     * Builds a radar chart with custom thresholds.
     *
     * @param breakdown score breakdown by dimension
     * @param thresholds custom threshold values [ddd, hex, dep, cpl, coh]
     * @return Mermaid radar-beta diagram code (without code fence)
     */
    public String build(ScoreBreakdown breakdown, int[] thresholds) {
        if (thresholds == null || thresholds.length != 5) {
            return build(breakdown);
        }
        return """
---
config:
  radar:
    curveTension: 0.1
  themeVariables:
    cScale0: "#81C784"
    cScale1: "#3F51B5"
---
radar-beta
    title Architecture Compliance by Dimension
    max 100
    axis ddd["DDD (%d%%)"], hex["Hexagonal (%d%%)"], dep["Dependencies (%d%%)"], cpl["Coupling (%d%%)"], coh["Cohesion (%d%%)"]
    curve target["Threshold"]{%d, %d, %d, %d, %d}
    curve score["Current Score"]{%d, %d, %d, %d, %d}"""
                .formatted(
                        breakdown.dddCompliance().weight(),
                        breakdown.hexagonalCompliance().weight(),
                        breakdown.dependencyQuality().weight(),
                        breakdown.couplingMetrics().weight(),
                        breakdown.cohesionQuality().weight(),
                        thresholds[0],
                        thresholds[1],
                        thresholds[2],
                        thresholds[3],
                        thresholds[4],
                        breakdown.dddCompliance().score(),
                        breakdown.hexagonalCompliance().score(),
                        breakdown.dependencyQuality().score(),
                        breakdown.couplingMetrics().score(),
                        breakdown.cohesionQuality().score());
    }
}
