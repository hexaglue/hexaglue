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

package io.hexaglue.core.classification.engine;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.core.classification.ClassificationCriteria;
import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.classification.MatchResult;
import io.hexaglue.core.classification.domain.DomainKind;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CriteriaProfile}.
 */
@DisplayName("CriteriaProfile")
class CriteriaProfileTest {

    // =========================================================================
    // legacy()
    // =========================================================================

    @Nested
    @DisplayName("legacy()")
    class LegacyProfileTest {

        @Test
        @DisplayName("should always return empty priority")
        void shouldAlwaysReturnEmpty() {
            CriteriaProfile profile = CriteriaProfile.legacy();

            assertThat(profile.priorityFor("any-criteria")).isEmpty();
            assertThat(profile.priorityFor("explicit-aggregate-root")).isEmpty();
            assertThat(profile.priorityFor("repository-dominant")).isEmpty();
        }

        @Test
        @DisplayName("resolvePriority should return criteria default")
        void resolvePriorityShouldReturnDefault() {
            CriteriaProfile profile = CriteriaProfile.legacy();
            ClassificationCriteria<DomainKind> criteria = createCriteria("test-criteria", 80);

            assertThat(profile.resolvePriority(criteria)).isEqualTo(80);
        }

        @Test
        @DisplayName("resolvePriority should work with any criteria priority")
        void resolvePriorityShouldWorkWithAnyPriority() {
            CriteriaProfile profile = CriteriaProfile.legacy();

            assertThat(profile.resolvePriority(createCriteria("p100", 100))).isEqualTo(100);
            assertThat(profile.resolvePriority(createCriteria("p50", 50))).isEqualTo(50);
            assertThat(profile.resolvePriority(createCriteria("p0", 0))).isEqualTo(0);
        }
    }

    // =========================================================================
    // withOverrides()
    // =========================================================================

    @Nested
    @DisplayName("withOverrides()")
    class WithOverridesTest {

        @Test
        @DisplayName("should return empty for non-overridden criteria")
        void shouldReturnEmptyForNonOverridden() {
            CriteriaProfile profile = CriteriaProfile.withOverrides(Map.of("other-criteria", 100));

            assertThat(profile.priorityFor("test-criteria")).isEmpty();
        }

        @Test
        @DisplayName("should return override for configured criteria")
        void shouldReturnOverride() {
            CriteriaProfile profile = CriteriaProfile.withOverrides(Map.of("test-criteria", 120));

            assertThat(profile.priorityFor("test-criteria")).hasValue(120);
        }

        @Test
        @DisplayName("resolvePriority should use override when present")
        void resolvePriorityShouldUseOverride() {
            CriteriaProfile profile = CriteriaProfile.withOverrides(Map.of("test-criteria", 120));
            ClassificationCriteria<DomainKind> criteria = createCriteria("test-criteria", 80);

            assertThat(profile.resolvePriority(criteria)).isEqualTo(120);
        }

        @Test
        @DisplayName("resolvePriority should use default when no override")
        void resolvePriorityShouldUseDefaultWhenNoOverride() {
            CriteriaProfile profile = CriteriaProfile.withOverrides(Map.of("other-criteria", 120));
            ClassificationCriteria<DomainKind> criteria = createCriteria("test-criteria", 80);

            assertThat(profile.resolvePriority(criteria)).isEqualTo(80);
        }

        @Test
        @DisplayName("should support multiple overrides")
        void shouldSupportMultipleOverrides() {
            CriteriaProfile profile = CriteriaProfile.withOverrides(Map.of(
                    "criteria-a", 100,
                    "criteria-b", 90,
                    "criteria-c", 80));

            assertThat(profile.priorityFor("criteria-a")).hasValue(100);
            assertThat(profile.priorityFor("criteria-b")).hasValue(90);
            assertThat(profile.priorityFor("criteria-c")).hasValue(80);
        }

        @Test
        @DisplayName("should work with empty overrides map")
        void shouldWorkWithEmptyMap() {
            CriteriaProfile profile = CriteriaProfile.withOverrides(Map.of());

            assertThat(profile.priorityFor("any-criteria")).isEmpty();
        }

        @Test
        @DisplayName("override can be zero")
        void overrideCanBeZero() {
            CriteriaProfile profile = CriteriaProfile.withOverrides(Map.of("test-criteria", 0));

            assertThat(profile.priorityFor("test-criteria")).hasValue(0);
        }

        @Test
        @DisplayName("override can be negative")
        void overrideCanBeNegative() {
            CriteriaProfile profile = CriteriaProfile.withOverrides(Map.of("test-criteria", -10));

            assertThat(profile.priorityFor("test-criteria")).hasValue(-10);
        }
    }

    // =========================================================================
    // Integration with IdentifiedCriteria
    // =========================================================================

    @Nested
    @DisplayName("Integration with IdentifiedCriteria")
    class IdentifiedCriteriaIntegrationTest {

        @Test
        @DisplayName("resolvePriority should use id() for IdentifiedCriteria")
        void resolvePriorityShouldUseId() {
            CriteriaProfile profile = CriteriaProfile.withOverrides(Map.of("domain.explicit.aggregateRoot", 150));

            TestIdentifiedCriteria criteria =
                    new TestIdentifiedCriteria("domain.explicit.aggregateRoot", "legacy-name", 100);

            // Should use the id() from IdentifiedCriteria, not name()
            assertThat(profile.resolvePriority(criteria)).isEqualTo(150);
        }

        @Test
        @DisplayName("resolvePriority should fallback to default for non-identified criteria")
        void resolvePriorityShouldFallbackForNonIdentified() {
            CriteriaProfile profile = CriteriaProfile.withOverrides(Map.of("domain.explicit.aggregateRoot", 150));

            // Non-identified criteria with matching name
            ClassificationCriteria<DomainKind> criteria = createCriteria("domain.explicit.aggregateRoot", 100);

            // Should use the name() fallback via CriteriaKey.of()
            assertThat(profile.resolvePriority(criteria)).isEqualTo(150);
        }
    }

    // =========================================================================
    // Custom Profile Implementation
    // =========================================================================

    @Nested
    @DisplayName("Custom profile implementation")
    class CustomProfileTest {

        @Test
        @DisplayName("should support custom profile logic")
        void shouldSupportCustomLogic() {
            // Custom profile: all criteria with "explicit" in name get +20 priority
            CriteriaProfile profile = key -> {
                if (key.contains("explicit")) {
                    return java.util.OptionalInt.of(120);
                }
                return java.util.OptionalInt.empty();
            };

            assertThat(profile.priorityFor("explicit-aggregate-root")).hasValue(120);
            assertThat(profile.priorityFor("other-criteria")).isEmpty();
        }

        @Test
        @DisplayName("custom profile can combine with defaults")
        void customProfileCanCombineWithDefaults() {
            // Custom profile with fallback chain
            CriteriaProfile base = CriteriaProfile.withOverrides(Map.of("base-criteria", 90));

            CriteriaProfile extended = key -> {
                if (key.startsWith("extended-")) {
                    return java.util.OptionalInt.of(110);
                }
                return base.priorityFor(key);
            };

            assertThat(extended.priorityFor("extended-criteria")).hasValue(110);
            assertThat(extended.priorityFor("base-criteria")).hasValue(90);
            assertThat(extended.priorityFor("unknown")).isEmpty();
        }
    }

    // =========================================================================
    // Helper Classes
    // =========================================================================

    private ClassificationCriteria<DomainKind> createCriteria(String name, int priority) {
        return new ClassificationCriteria<>() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public int priority() {
                return priority;
            }

            @Override
            public DomainKind targetKind() {
                return DomainKind.ENTITY;
            }

            @Override
            public MatchResult evaluate(TypeNode node, GraphQuery query) {
                return MatchResult.match(ConfidenceLevel.HIGH, "Test");
            }
        };
    }

    private static class TestIdentifiedCriteria implements ClassificationCriteria<DomainKind>, IdentifiedCriteria {
        private final String id;
        private final String name;
        private final int priority;

        TestIdentifiedCriteria(String id, String name, int priority) {
            this.id = id;
            this.name = name;
            this.priority = priority;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public int priority() {
            return priority;
        }

        @Override
        public DomainKind targetKind() {
            return DomainKind.AGGREGATE_ROOT;
        }

        @Override
        public MatchResult evaluate(TypeNode node, GraphQuery query) {
            return MatchResult.match(ConfidenceLevel.EXPLICIT, "Explicit annotation");
        }
    }
}
