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

import io.hexaglue.plugin.audit.domain.model.report.PackageMetric;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Builds Mermaid quadrant charts for package stability analysis.
 *
 * <p>Visualizes packages on an Abstractness vs Instability plot,
 * showing the "Main Sequence" and zones of pain/uselessness.
 *
 * @since 5.0.0
 */
public class QuadrantChartBuilder {

    private static final int MAX_PACKAGES = 15;

    /**
     * Builds a quadrant chart for package stability analysis.
     *
     * @param packageMetrics list of package metrics
     * @return Mermaid quadrantChart diagram code (without code fence)
     */
    public String build(List<PackageMetric> packageMetrics) {
        StringBuilder sb = new StringBuilder();
        // Configuration to increase chart size and label padding
        sb.append(
                "%%{init: {\"quadrantChart\": {\"chartWidth\": 600, \"chartHeight\": 600, \"pointTextPadding\": 8}}}%%\n");
        sb.append("quadrantChart\n");
        sb.append("    title Package Stability Analysis\n");
        sb.append("    x-axis Concrete --> Abstract\n");
        sb.append("    y-axis Stable --> Unstable\n");
        sb.append("    quadrant-1 Zone of Uselessness\n");
        sb.append("    quadrant-2 Main Sequence\n");
        sb.append("    quadrant-3 Zone of Pain\n");
        sb.append("    quadrant-4 Main Sequence\n");

        if (packageMetrics == null || packageMetrics.isEmpty()) {
            sb.append("    \"No packages\": [0.5, 0.5]\n");
            return sb.toString().trim();
        }

        // Limit packages to avoid cluttered chart
        List<PackageMetric> displayed =
                packageMetrics.size() > MAX_PACKAGES ? packageMetrics.subList(0, MAX_PACKAGES) : packageMetrics;

        // Group packages by coordinates to combine labels for overlapping points
        Map<String, List<String>> coordinateToNames = new LinkedHashMap<>();
        Map<String, double[]> coordinateToValues = new LinkedHashMap<>();

        for (PackageMetric pm : displayed) {
            String shortName = shortenPackageName(pm.packageName());
            String coordKey = pm.abstractness() + "," + pm.instability();

            coordinateToNames.computeIfAbsent(coordKey, k -> new ArrayList<>()).add(shortName);
            coordinateToValues.putIfAbsent(coordKey, new double[] {pm.abstractness(), pm.instability()});
        }

        // Output one point per unique coordinate with combined labels
        for (Map.Entry<String, List<String>> entry : coordinateToNames.entrySet()) {
            List<String> names = entry.getValue();
            double[] coords = coordinateToValues.get(entry.getKey());

            String label = String.join(" | ", names);

            sb.append("    \"")
                    .append(label)
                    .append("\": [")
                    .append(formatCoordinate(coords[0]))
                    .append(", ")
                    .append(formatCoordinate(coords[1]))
                    .append("]\n");
        }

        if (packageMetrics.size() > MAX_PACKAGES) {
            sb.append("    \"... +")
                    .append(packageMetrics.size() - MAX_PACKAGES)
                    .append(" more\": [0.5, 0.5]\n");
        }

        return sb.toString().trim();
    }

    /**
     * Shortens a package name to the last two segments.
     *
     * @param packageName full package name
     * @return shortened name
     */
    private String shortenPackageName(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return "unknown";
        }
        String[] parts = packageName.split("\\.");
        if (parts.length <= 2) {
            return packageName;
        }
        // Return last two segments
        return parts[parts.length - 2] + "." + parts[parts.length - 1];
    }

    /**
     * Formats a coordinate value for Mermaid quadrantChart.
     *
     * <p>Returns integer format (e.g., "0", "1") for whole numbers,
     * and decimal format (e.g., "0.5") for fractional values.
     * Mermaid doesn't accept "1.00" but accepts "1".
     *
     * @param value the coordinate value (0.0 to 1.0)
     * @return formatted string
     */
    private String formatCoordinate(double value) {
        if (value == Math.floor(value)) {
            // Whole number: format as integer
            return String.format(Locale.ROOT, "%.0f", value);
        }
        // Fractional: format with minimal decimals
        String formatted = String.format(Locale.ROOT, "%.2f", value);
        // Remove trailing zeros after decimal point
        if (formatted.contains(".")) {
            formatted = formatted.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return formatted;
    }
}
