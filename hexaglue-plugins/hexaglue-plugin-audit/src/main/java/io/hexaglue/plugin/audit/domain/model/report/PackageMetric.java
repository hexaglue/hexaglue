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

package io.hexaglue.plugin.audit.domain.model.report;

import java.util.Objects;

/**
 * Package-level metrics for stability analysis.
 *
 * @param packageName fully qualified package name
 * @param ca afferent coupling (incoming dependencies)
 * @param ce efferent coupling (outgoing dependencies)
 * @param instability instability metric I = Ce / (Ca + Ce)
 * @param abstractness abstractness metric A = abstract classes / total classes
 * @param distance distance from main sequence D = |A + I - 1|
 * @param zone stability zone classification
 * @since 5.0.0
 */
public record PackageMetric(
        String packageName, int ca, int ce, double instability, double abstractness, double distance, ZoneType zone) {

    /**
     * Creates a package metric with validation.
     */
    public PackageMetric {
        Objects.requireNonNull(packageName, "packageName is required");
        Objects.requireNonNull(zone, "zone is required");
    }

    /**
     * Stability zone classification.
     */
    public enum ZoneType {
        /** Stable and concrete - core domain */
        STABLE_CORE("Stable Core"),
        /** On the main sequence - balanced */
        MAIN_SEQUENCE("Main Sequence"),
        /** Zone of pain - too stable but concrete */
        ZONE_OF_PAIN("Zone of Pain"),
        /** Zone of uselessness - too abstract */
        ZONE_OF_USELESSNESS("Zone of Uselessness");

        private final String label;

        ZoneType(String label) {
            this.label = label;
        }

        /**
         * Returns a human-readable label for the zone.
         *
         * @return the label
         */
        public String label() {
            return label;
        }
    }

    /**
     * Calculates the zone from instability and abstractness.
     *
     * @param instability instability value
     * @param abstractness abstractness value
     * @return the zone type
     */
    public static ZoneType calculateZone(double instability, double abstractness) {
        double distance = Math.abs(abstractness + instability - 1);
        if (distance <= 0.2) {
            return ZoneType.MAIN_SEQUENCE;
        }
        if (abstractness > 0.7 && instability < 0.3) {
            return ZoneType.ZONE_OF_USELESSNESS;
        }
        if (abstractness < 0.3 && instability < 0.3) {
            return ZoneType.ZONE_OF_PAIN;
        }
        if (abstractness < 0.3 && instability > 0.5) {
            return ZoneType.STABLE_CORE;
        }
        return ZoneType.MAIN_SEQUENCE;
    }

    /**
     * Builder for PackageMetric.
     */
    public static class Builder {
        private String packageName;
        private int ca;
        private int ce;

        public Builder packageName(String packageName) {
            this.packageName = packageName;
            return this;
        }

        public Builder ca(int ca) {
            this.ca = ca;
            return this;
        }

        public Builder ce(int ce) {
            this.ce = ce;
            return this;
        }

        public PackageMetric build(double abstractness) {
            double instability = (ca + ce) == 0 ? 0 : (double) ce / (ca + ce);
            double distance = Math.abs(abstractness + instability - 1);
            ZoneType zone = calculateZone(instability, abstractness);
            return new PackageMetric(packageName, ca, ce, instability, abstractness, distance, zone);
        }
    }

    /**
     * Creates a new builder.
     *
     * @return new builder
     */
    public static Builder builder() {
        return new Builder();
    }
}
