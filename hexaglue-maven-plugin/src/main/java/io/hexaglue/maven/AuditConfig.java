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

package io.hexaglue.maven;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration for HexaGlue audit execution.
 *
 * <p>This class encapsulates all audit-related configuration including enabled/disabled
 * rules, quality thresholds, and severity settings. It is typically configured in the
 * Maven plugin configuration or loaded from an external YAML/XML file.
 *
 * <p>Example Maven configuration:
 * <pre>{@code
 * <configuration>
 *     <auditConfig>
 *         <enabledRules>
 *             <rule>hexagonal.port-segregation</rule>
 *             <rule>hexagonal.dependency-direction</rule>
 *         </enabledRules>
 *         <disabledRules>
 *             <rule>complexity.cyclomatic</rule>
 *         </disabledRules>
 *         <thresholds>
 *             <maxCyclomaticComplexity>10</maxCyclomaticComplexity>
 *             <maxMethodLength>50</maxMethodLength>
 *             <minTestCoverage>80.0</minTestCoverage>
 *         </thresholds>
 *     </auditConfig>
 * </configuration>
 * }</pre>
 *
 * @since 3.0.0
 */
public class AuditConfig {

    /**
     * List of explicitly enabled audit rules.
     * If empty, all available rules are enabled by default.
     */
    private List<String> enabledRules = new ArrayList<>();

    /**
     * List of explicitly disabled audit rules.
     * These rules will not be executed even if they're in the enabled list.
     */
    private List<String> disabledRules = new ArrayList<>();

    /**
     * Quality thresholds configuration.
     */
    private Thresholds thresholds = new Thresholds();

    /**
     * Rule-specific configurations keyed by rule ID.
     */
    private Map<String, Map<String, Object>> ruleConfigs = new HashMap<>();

    // Getters and setters

    public List<String> getEnabledRules() {
        return enabledRules;
    }

    public void setEnabledRules(List<String> enabledRules) {
        this.enabledRules = enabledRules != null ? enabledRules : new ArrayList<>();
    }

    public List<String> getDisabledRules() {
        return disabledRules;
    }

    public void setDisabledRules(List<String> disabledRules) {
        this.disabledRules = disabledRules != null ? disabledRules : new ArrayList<>();
    }

    public Thresholds getThresholds() {
        return thresholds;
    }

    public void setThresholds(Thresholds thresholds) {
        this.thresholds = thresholds != null ? thresholds : new Thresholds();
    }

    public Map<String, Map<String, Object>> getRuleConfigs() {
        return ruleConfigs;
    }

    public void setRuleConfigs(Map<String, Map<String, Object>> ruleConfigs) {
        this.ruleConfigs = ruleConfigs != null ? ruleConfigs : new HashMap<>();
    }

    /**
     * Converts this configuration to plugin configs format expected by the engine.
     *
     * @return map of plugin configs keyed by plugin ID
     */
    public Map<String, Map<String, Object>> toPluginConfigs() {
        Map<String, Map<String, Object>> pluginConfigs = new HashMap<>();

        // Build audit plugin configuration
        Map<String, Object> auditPluginConfig = new HashMap<>();
        auditPluginConfig.put("enabledRules", enabledRules);
        auditPluginConfig.put("disabledRules", disabledRules);
        auditPluginConfig.put("thresholds", thresholds.toMap());
        auditPluginConfig.putAll(ruleConfigs);

        // Use a standard audit plugin ID (this should match the actual audit plugin)
        pluginConfigs.put("io.hexaglue.audit", auditPluginConfig);

        return pluginConfigs;
    }

    /**
     * Quality thresholds for various metrics.
     */
    public static class Thresholds {

        /**
         * Maximum cyclomatic complexity allowed per method.
         * Default: 10 (common standard from PMD/Checkstyle)
         */
        private int maxCyclomaticComplexity = 10;

        /**
         * Maximum lines of code allowed per method.
         * Default: 50
         */
        private int maxMethodLength = 50;

        /**
         * Maximum lines of code allowed per class.
         * Default: 500
         */
        private int maxClassLength = 500;

        /**
         * Maximum number of parameters allowed per method.
         * Default: 7 (from Clean Code)
         */
        private int maxMethodParameters = 7;

        /**
         * Maximum nesting depth allowed.
         * Default: 4
         */
        private int maxNestingDepth = 4;

        /**
         * Minimum test coverage percentage required.
         * Default: 80.0
         */
        private double minTestCoverage = 80.0;

        /**
         * Minimum documentation coverage percentage required.
         * Default: 70.0
         */
        private double minDocumentationCoverage = 70.0;

        /**
         * Maximum technical debt allowed in minutes.
         * Default: 480 (8 hours)
         */
        private int maxTechnicalDebtMinutes = 480;

        /**
         * Minimum maintainability rating required (0.0-5.0).
         * Default: 3.0
         */
        private double minMaintainabilityRating = 3.0;

        // Getters and setters

        public int getMaxCyclomaticComplexity() {
            return maxCyclomaticComplexity;
        }

        public void setMaxCyclomaticComplexity(int maxCyclomaticComplexity) {
            this.maxCyclomaticComplexity = maxCyclomaticComplexity;
        }

        public int getMaxMethodLength() {
            return maxMethodLength;
        }

        public void setMaxMethodLength(int maxMethodLength) {
            this.maxMethodLength = maxMethodLength;
        }

        public int getMaxClassLength() {
            return maxClassLength;
        }

        public void setMaxClassLength(int maxClassLength) {
            this.maxClassLength = maxClassLength;
        }

        public int getMaxMethodParameters() {
            return maxMethodParameters;
        }

        public void setMaxMethodParameters(int maxMethodParameters) {
            this.maxMethodParameters = maxMethodParameters;
        }

        public int getMaxNestingDepth() {
            return maxNestingDepth;
        }

        public void setMaxNestingDepth(int maxNestingDepth) {
            this.maxNestingDepth = maxNestingDepth;
        }

        public double getMinTestCoverage() {
            return minTestCoverage;
        }

        public void setMinTestCoverage(double minTestCoverage) {
            this.minTestCoverage = minTestCoverage;
        }

        public double getMinDocumentationCoverage() {
            return minDocumentationCoverage;
        }

        public void setMinDocumentationCoverage(double minDocumentationCoverage) {
            this.minDocumentationCoverage = minDocumentationCoverage;
        }

        public int getMaxTechnicalDebtMinutes() {
            return maxTechnicalDebtMinutes;
        }

        public void setMaxTechnicalDebtMinutes(int maxTechnicalDebtMinutes) {
            this.maxTechnicalDebtMinutes = maxTechnicalDebtMinutes;
        }

        public double getMinMaintainabilityRating() {
            return minMaintainabilityRating;
        }

        public void setMinMaintainabilityRating(double minMaintainabilityRating) {
            this.minMaintainabilityRating = minMaintainabilityRating;
        }

        /**
         * Converts thresholds to a map for plugin configuration.
         *
         * @return map representation of thresholds
         */
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("maxCyclomaticComplexity", maxCyclomaticComplexity);
            map.put("maxMethodLength", maxMethodLength);
            map.put("maxClassLength", maxClassLength);
            map.put("maxMethodParameters", maxMethodParameters);
            map.put("maxNestingDepth", maxNestingDepth);
            map.put("minTestCoverage", minTestCoverage);
            map.put("minDocumentationCoverage", minDocumentationCoverage);
            map.put("maxTechnicalDebtMinutes", maxTechnicalDebtMinutes);
            map.put("minMaintainabilityRating", minMaintainabilityRating);
            return map;
        }
    }
}
