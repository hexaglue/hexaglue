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
import io.hexaglue.core.classification.MatchResult;
import io.hexaglue.core.classification.domain.DomainClassifier;
import io.hexaglue.core.classification.domain.DomainKind;
import io.hexaglue.core.classification.port.PortClassifier;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CriteriaKey")
class CriteriaKeyTest {

    // ID convention: {target}.{category}.{name}
    private static final Pattern STABLE_ID_PATTERN = Pattern.compile(
            "^(domain|port)\\.(explicit|semantic|structural|naming|pattern|relationship|package|signature)\\.[a-zA-Z]+$");

    @Nested
    @DisplayName("of()")
    class OfTest {

        @Test
        @DisplayName("should return id for IdentifiedCriteria")
        void shouldReturnIdForIdentifiedCriteria() {
            var criteria = new TestIdentifiedCriteria("test.category.myId", "my-name");

            String key = CriteriaKey.of(criteria);

            assertThat(key).isEqualTo("test.category.myId");
        }

        @Test
        @DisplayName("should return name for non-IdentifiedCriteria")
        void shouldReturnNameForNonIdentifiedCriteria() {
            var criteria = new TestNonIdentifiedCriteria("my-name");

            String key = CriteriaKey.of(criteria);

            assertThat(key).isEqualTo("my-name");
        }
    }

    @Nested
    @DisplayName("ofAny()")
    class OfAnyTest {

        @Test
        @DisplayName("should return id for IdentifiedCriteria")
        void shouldReturnIdForIdentifiedCriteria() {
            var criteria = new TestIdentifiedCriteria("test.category.myId", "my-name");

            String key = CriteriaKey.ofAny(criteria);

            assertThat(key).isEqualTo("test.category.myId");
        }

        @Test
        @DisplayName("should return name for ClassificationCriteria")
        void shouldReturnNameForClassificationCriteria() {
            var criteria = new TestNonIdentifiedCriteria("my-name");

            String key = CriteriaKey.ofAny(criteria);

            assertThat(key).isEqualTo("my-name");
        }

        @Test
        @DisplayName("should return toString for other objects")
        void shouldReturnToStringForOtherObjects() {
            var obj = new Object() {
                @Override
                public String toString() {
                    return "custom-object";
                }
            };

            String key = CriteriaKey.ofAny(obj);

            assertThat(key).isEqualTo("custom-object");
        }
    }

    @Nested
    @DisplayName("Stable ID Convention")
    class StableIdConventionTest {

        @Test
        @DisplayName("all domain criteria should implement IdentifiedCriteria")
        void allDomainCriteriaShouldImplementIdentifiedCriteria() {
            List<ClassificationCriteria<DomainKind>> criteria = DomainClassifier.defaultCriteria();

            for (var c : criteria) {
                assertThat(c)
                        .as("Domain criteria '%s' should implement IdentifiedCriteria", c.name())
                        .isInstanceOf(IdentifiedCriteria.class);
            }
        }

        @Test
        @DisplayName("all domain criteria should have unique stable IDs")
        void allDomainCriteriaShouldHaveUniqueStableIds() {
            List<ClassificationCriteria<DomainKind>> criteria = DomainClassifier.defaultCriteria();
            Set<String> ids = new HashSet<>();

            for (var c : criteria) {
                String id = CriteriaKey.of(c);
                assertThat(ids.add(id))
                        .as("Duplicate ID found: %s (for criteria %s)", id, c.name())
                        .isTrue();
            }
        }

        @Test
        @DisplayName("all domain criteria IDs should follow naming convention")
        void allDomainCriteriaIdsShouldFollowNamingConvention() {
            List<ClassificationCriteria<DomainKind>> criteria = DomainClassifier.defaultCriteria();

            for (var c : criteria) {
                String id = CriteriaKey.of(c);
                assertThat(id)
                        .as("ID '%s' (for criteria %s) should start with 'domain.'", id, c.name())
                        .startsWith("domain.");
                assertThat(STABLE_ID_PATTERN.matcher(id).matches())
                        .as("ID '%s' (for criteria %s) should match pattern {target}.{category}.{name}", id, c.name())
                        .isTrue();
            }
        }

        @Test
        @DisplayName("all port criteria should implement IdentifiedCriteria")
        void allPortCriteriaShouldImplementIdentifiedCriteria() {
            List<?> criteria = PortClassifier.defaultCriteria();

            for (var c : criteria) {
                assertThat(c)
                        .as("Port criteria '%s' should implement IdentifiedCriteria", c)
                        .isInstanceOf(IdentifiedCriteria.class);
            }
        }

        @Test
        @DisplayName("all port criteria should have unique stable IDs")
        void allPortCriteriaShouldHaveUniqueStableIds() {
            List<?> criteria = PortClassifier.defaultCriteria();
            Set<String> ids = new HashSet<>();

            for (var c : criteria) {
                if (c instanceof ClassificationCriteria<?> cc) {
                    String id = CriteriaKey.of(cc);
                    assertThat(ids.add(id)).as("Duplicate ID found: %s", id).isTrue();
                }
            }
        }

        @Test
        @DisplayName("all port criteria IDs should follow naming convention")
        void allPortCriteriaIdsShouldFollowNamingConvention() {
            List<?> criteria = PortClassifier.defaultCriteria();

            for (var c : criteria) {
                if (c instanceof ClassificationCriteria<?> cc) {
                    String id = CriteriaKey.of(cc);
                    assertThat(id).as("ID '%s' should start with 'port.'", id).startsWith("port.");
                    assertThat(STABLE_ID_PATTERN.matcher(id).matches())
                            .as("ID '%s' should match pattern {target}.{category}.{name}", id)
                            .isTrue();
                }
            }
        }
    }

    // =========================================================================
    // Test fixtures
    // =========================================================================

    private static class TestIdentifiedCriteria implements ClassificationCriteria<DomainKind>, IdentifiedCriteria {
        private final String id;
        private final String name;

        TestIdentifiedCriteria(String id, String name) {
            this.id = id;
            this.name = name;
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
            return 50;
        }

        @Override
        public DomainKind targetKind() {
            return DomainKind.ENTITY;
        }

        @Override
        public MatchResult evaluate(TypeNode node, GraphQuery query) {
            return MatchResult.noMatch();
        }
    }

    private static class TestNonIdentifiedCriteria implements ClassificationCriteria<DomainKind> {
        private final String name;

        TestNonIdentifiedCriteria(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public int priority() {
            return 50;
        }

        @Override
        public DomainKind targetKind() {
            return DomainKind.ENTITY;
        }

        @Override
        public MatchResult evaluate(TypeNode node, GraphQuery query) {
            return MatchResult.noMatch();
        }
    }
}
