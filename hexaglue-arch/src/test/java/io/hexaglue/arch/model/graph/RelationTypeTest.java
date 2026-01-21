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

package io.hexaglue.arch.model.graph;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tests for {@link RelationType}.
 *
 * @since 5.0.0
 */
@DisplayName("RelationType")
class RelationTypeTest {

    @Nested
    @DisplayName("Compositional Relationships")
    class CompositionalRelationships {

        @ParameterizedTest
        @EnumSource(
                value = RelationType.class,
                names = {"CONTAINS", "OWNS"})
        @DisplayName("should identify compositional relationships")
        void shouldIdentifyCompositionalRelationships(RelationType type) {
            assertThat(type.isCompositional()).isTrue();
        }

        @ParameterizedTest
        @EnumSource(
                value = RelationType.class,
                names = {
                    "REFERENCES",
                    "DEPENDS_ON",
                    "EXTENDS",
                    "IMPLEMENTS",
                    "EXPOSES",
                    "ADAPTS",
                    "PERSISTS",
                    "EMITS",
                    "HANDLES"
                })
        @DisplayName("should identify non-compositional relationships")
        void shouldIdentifyNonCompositionalRelationships(RelationType type) {
            assertThat(type.isCompositional()).isFalse();
        }
    }

    @Nested
    @DisplayName("Reference Relationships")
    class ReferenceRelationships {

        @ParameterizedTest
        @EnumSource(
                value = RelationType.class,
                names = {"REFERENCES", "DEPENDS_ON"})
        @DisplayName("should identify reference relationships")
        void shouldIdentifyReferenceRelationships(RelationType type) {
            assertThat(type.isReference()).isTrue();
        }

        @ParameterizedTest
        @EnumSource(
                value = RelationType.class,
                names = {
                    "CONTAINS",
                    "OWNS",
                    "EXTENDS",
                    "IMPLEMENTS",
                    "EXPOSES",
                    "ADAPTS",
                    "PERSISTS",
                    "EMITS",
                    "HANDLES"
                })
        @DisplayName("should identify non-reference relationships")
        void shouldIdentifyNonReferenceRelationships(RelationType type) {
            assertThat(type.isReference()).isFalse();
        }
    }

    @Nested
    @DisplayName("Inheritance Relationships")
    class InheritanceRelationships {

        @ParameterizedTest
        @EnumSource(
                value = RelationType.class,
                names = {"EXTENDS", "IMPLEMENTS"})
        @DisplayName("should identify inheritance relationships")
        void shouldIdentifyInheritanceRelationships(RelationType type) {
            assertThat(type.isInheritance()).isTrue();
        }

        @ParameterizedTest
        @EnumSource(
                value = RelationType.class,
                names = {
                    "CONTAINS",
                    "OWNS",
                    "REFERENCES",
                    "DEPENDS_ON",
                    "EXPOSES",
                    "ADAPTS",
                    "PERSISTS",
                    "EMITS",
                    "HANDLES"
                })
        @DisplayName("should identify non-inheritance relationships")
        void shouldIdentifyNonInheritanceRelationships(RelationType type) {
            assertThat(type.isInheritance()).isFalse();
        }
    }

    @Nested
    @DisplayName("Port Related Relationships")
    class PortRelatedRelationships {

        @ParameterizedTest
        @EnumSource(
                value = RelationType.class,
                names = {"EXPOSES", "ADAPTS", "PERSISTS"})
        @DisplayName("should identify port-related relationships")
        void shouldIdentifyPortRelatedRelationships(RelationType type) {
            assertThat(type.isPortRelated()).isTrue();
        }

        @ParameterizedTest
        @EnumSource(
                value = RelationType.class,
                names = {"CONTAINS", "OWNS", "REFERENCES", "DEPENDS_ON", "EXTENDS", "IMPLEMENTS", "EMITS", "HANDLES"})
        @DisplayName("should identify non-port-related relationships")
        void shouldIdentifyNonPortRelatedRelationships(RelationType type) {
            assertThat(type.isPortRelated()).isFalse();
        }
    }

    @Nested
    @DisplayName("Event Related Relationships")
    class EventRelatedRelationships {

        @ParameterizedTest
        @EnumSource(
                value = RelationType.class,
                names = {"EMITS", "HANDLES"})
        @DisplayName("should identify event-related relationships")
        void shouldIdentifyEventRelatedRelationships(RelationType type) {
            assertThat(type.isEventRelated()).isTrue();
        }

        @ParameterizedTest
        @EnumSource(
                value = RelationType.class,
                names = {
                    "CONTAINS",
                    "OWNS",
                    "REFERENCES",
                    "DEPENDS_ON",
                    "EXTENDS",
                    "IMPLEMENTS",
                    "EXPOSES",
                    "ADAPTS",
                    "PERSISTS"
                })
        @DisplayName("should identify non-event-related relationships")
        void shouldIdentifyNonEventRelatedRelationships(RelationType type) {
            assertThat(type.isEventRelated()).isFalse();
        }
    }

    @Nested
    @DisplayName("Ownership Semantics")
    class OwnershipSemantics {

        @ParameterizedTest
        @EnumSource(
                value = RelationType.class,
                names = {"CONTAINS", "OWNS", "PERSISTS"})
        @DisplayName("should identify ownership relationships")
        void shouldIdentifyOwnershipRelationships(RelationType type) {
            assertThat(type.impliesOwnership()).isTrue();
        }

        @ParameterizedTest
        @EnumSource(
                value = RelationType.class,
                names = {"REFERENCES", "DEPENDS_ON", "EXTENDS", "IMPLEMENTS", "EXPOSES", "ADAPTS", "EMITS", "HANDLES"})
        @DisplayName("should identify non-ownership relationships")
        void shouldIdentifyNonOwnershipRelationships(RelationType type) {
            assertThat(type.impliesOwnership()).isFalse();
        }
    }

    @Nested
    @DisplayName("Completeness")
    class Completeness {

        @Test
        @DisplayName("should have exactly 11 values")
        void shouldHaveExactNumberOfValues() {
            // CONTAINS, OWNS, REFERENCES, DEPENDS_ON, EXTENDS, IMPLEMENTS,
            // EXPOSES, ADAPTS, PERSISTS, EMITS, HANDLES
            assertThat(RelationType.values()).hasSize(11);
        }
    }
}
