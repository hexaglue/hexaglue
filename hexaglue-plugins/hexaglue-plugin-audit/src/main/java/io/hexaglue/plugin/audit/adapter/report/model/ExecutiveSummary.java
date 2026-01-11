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

package io.hexaglue.plugin.audit.adapter.report.model;

import java.util.List;
import java.util.Objects;

/**
 * Executive summary of the audit report for quick high-level review.
 *
 * <p>Provides a structured overview including verdict, key strengths,
 * concerns requiring attention, and KPI metrics.
 *
 * @param verdict          overall assessment text (e.g., "Architecture requires attention")
 * @param strengths        list of positive findings
 * @param concerns         list of concerns organized by severity
 * @param kpis             key performance indicators
 * @param immediateActions recommended immediate actions
 * @since 1.0.0
 */
public record ExecutiveSummary(
        String verdict,
        List<String> strengths,
        List<ConcernEntry> concerns,
        List<KpiEntry> kpis,
        List<String> immediateActions) {

    public ExecutiveSummary {
        Objects.requireNonNull(verdict, "verdict required");
        strengths = strengths != null ? List.copyOf(strengths) : List.of();
        concerns = concerns != null ? List.copyOf(concerns) : List.of();
        kpis = kpis != null ? List.copyOf(kpis) : List.of();
        immediateActions = immediateActions != null ? List.copyOf(immediateActions) : List.of();
    }

    /**
     * Returns an empty executive summary.
     *
     * @return an ExecutiveSummary with no content
     */
    public static ExecutiveSummary empty() {
        return new ExecutiveSummary("No issues detected", List.of(), List.of(), List.of(), List.of());
    }

    /**
     * Creates a builder for constructing an ExecutiveSummary.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * A concern entry representing an area requiring attention.
     *
     * @param severity    the severity level (BLOCKER, CRITICAL, MAJOR, MINOR)
     * @param description brief description of the concern
     * @param count       number of occurrences
     */
    public record ConcernEntry(String severity, String description, int count) {
        public ConcernEntry {
            Objects.requireNonNull(severity, "severity required");
            Objects.requireNonNull(description, "description required");
            if (count < 0) {
                throw new IllegalArgumentException("count cannot be negative: " + count);
            }
        }
    }

    /**
     * A key performance indicator entry.
     *
     * @param name      the KPI name
     * @param value     the current value
     * @param threshold the threshold value
     * @param status    OK, WARNING, or CRITICAL
     */
    public record KpiEntry(String name, String value, String threshold, String status) {
        public KpiEntry {
            Objects.requireNonNull(name, "name required");
            Objects.requireNonNull(value, "value required");
            Objects.requireNonNull(threshold, "threshold required");
            Objects.requireNonNull(status, "status required");
        }

        public boolean isOk() {
            return "OK".equalsIgnoreCase(status);
        }

        public boolean isWarning() {
            return "WARNING".equalsIgnoreCase(status);
        }

        public boolean isCritical() {
            return "CRITICAL".equalsIgnoreCase(status);
        }
    }

    /**
     * Builder for ExecutiveSummary.
     */
    public static final class Builder {
        private String verdict = "";
        private List<String> strengths = List.of();
        private List<ConcernEntry> concerns = List.of();
        private List<KpiEntry> kpis = List.of();
        private List<String> immediateActions = List.of();

        public Builder verdict(String verdict) {
            this.verdict = Objects.requireNonNull(verdict, "verdict required");
            return this;
        }

        public Builder strengths(List<String> strengths) {
            this.strengths = strengths;
            return this;
        }

        public Builder concerns(List<ConcernEntry> concerns) {
            this.concerns = concerns;
            return this;
        }

        public Builder kpis(List<KpiEntry> kpis) {
            this.kpis = kpis;
            return this;
        }

        public Builder immediateActions(List<String> actions) {
            this.immediateActions = actions;
            return this;
        }

        public ExecutiveSummary build() {
            return new ExecutiveSummary(verdict, strengths, concerns, kpis, immediateActions);
        }
    }
}
