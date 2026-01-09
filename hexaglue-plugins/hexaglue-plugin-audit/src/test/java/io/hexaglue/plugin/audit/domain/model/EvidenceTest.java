/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.hexaglue.plugin.audit.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Evidence} sealed interface and its implementations.
 *
 * <p>Validates the creation, behavior, and sealed interface properties of all evidence types.
 */
class EvidenceTest {

    @Nested
    @DisplayName("StructuralEvidence tests")
    class StructuralEvidenceTests {

        @Test
        @DisplayName("Should create structural evidence with multiple types")
        void shouldCreateStructuralEvidenceWithMultipleTypes() {
            // When
            StructuralEvidence evidence = StructuralEvidence.of(
                    "Missing identity field", List.of("com.example.Order", "com.example.Customer"));

            // Then
            assertThat(evidence.description()).isEqualTo("Missing identity field");
            assertThat(evidence.involvedTypes()).containsExactly("com.example.Order", "com.example.Customer");
        }

        @Test
        @DisplayName("Should create structural evidence with single type")
        void shouldCreateStructuralEvidenceWithSingleType() {
            // When
            StructuralEvidence evidence = StructuralEvidence.of("Missing identity field", "com.example.Order");

            // Then
            assertThat(evidence.description()).isEqualTo("Missing identity field");
            assertThat(evidence.involvedTypes()).containsExactly("com.example.Order");
        }

        @Test
        @DisplayName("Should reject null description")
        void shouldRejectNullDescription() {
            // When/Then
            assertThatThrownBy(() -> StructuralEvidence.of(null, List.of("Type")))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("description required");
        }

        @Test
        @DisplayName("Should handle null involved types list")
        void shouldHandleNullInvolvedTypesList() {
            // When
            StructuralEvidence evidence = StructuralEvidence.of("Test description", (List<String>) null);

            // Then
            assertThat(evidence.involvedTypes()).isEmpty();
        }

        @Test
        @DisplayName("Should create immutable involved types list")
        void shouldCreateImmutableInvolvedTypesList() {
            // Given
            StructuralEvidence evidence = StructuralEvidence.of("Test", List.of("Type1", "Type2"));

            // When/Then
            assertThatThrownBy(() -> evidence.involvedTypes().add("Type3"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("BehavioralEvidence tests")
    class BehavioralEvidenceTests {

        @Test
        @DisplayName("Should create behavioral evidence without method name")
        void shouldCreateBehavioralEvidenceWithoutMethodName() {
            // When
            BehavioralEvidence evidence =
                    BehavioralEvidence.of("Setter detected", List.of("com.example.ValueObject"));

            // Then
            assertThat(evidence.description()).isEqualTo("Setter detected");
            assertThat(evidence.involvedTypes()).containsExactly("com.example.ValueObject");
            assertThat(evidence.methodName()).isNull();
        }

        @Test
        @DisplayName("Should create behavioral evidence with method name")
        void shouldCreateBehavioralEvidenceWithMethodName() {
            // When
            BehavioralEvidence evidence =
                    BehavioralEvidence.of("Setter method detected", "com.example.ValueObject", "setValue");

            // Then
            assertThat(evidence.description()).isEqualTo("Setter method detected");
            assertThat(evidence.involvedTypes()).containsExactly("com.example.ValueObject");
            assertThat(evidence.methodName()).isEqualTo("setValue");
        }

        @Test
        @DisplayName("Should reject null description")
        void shouldRejectNullDescription() {
            // When/Then
            assertThatThrownBy(() -> BehavioralEvidence.of(null, List.of("Type")))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("description required");
        }

        @Test
        @DisplayName("Should handle null involved types list")
        void shouldHandleNullInvolvedTypesList() {
            // When
            BehavioralEvidence evidence = new BehavioralEvidence("Test description", null, "method");

            // Then
            assertThat(evidence.involvedTypes()).isEmpty();
        }

        @Test
        @DisplayName("Should create immutable involved types list")
        void shouldCreateImmutableInvolvedTypesList() {
            // Given
            BehavioralEvidence evidence = BehavioralEvidence.of("Test", List.of("Type1"));

            // When/Then
            assertThatThrownBy(() -> evidence.involvedTypes().add("Type2"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("RelationshipEvidence tests")
    class RelationshipEvidenceTests {

        @Test
        @DisplayName("Should create relationship evidence with relationships")
        void shouldCreateRelationshipEvidenceWithRelationships() {
            // When
            RelationshipEvidence evidence = RelationshipEvidence.of(
                    "Circular dependency",
                    List.of("Order", "Customer"),
                    List.of("Order -> Customer", "Customer -> Order"));

            // Then
            assertThat(evidence.description()).isEqualTo("Circular dependency");
            assertThat(evidence.involvedTypes()).containsExactly("Order", "Customer");
            assertThat(evidence.relationships()).containsExactly("Order -> Customer", "Customer -> Order");
        }

        @Test
        @DisplayName("Should create relationship evidence without explicit relationships")
        void shouldCreateRelationshipEvidenceWithoutExplicitRelationships() {
            // When
            RelationshipEvidence evidence =
                    RelationshipEvidence.of("Illegal dependency", List.of("Domain", "Infrastructure"));

            // Then
            assertThat(evidence.description()).isEqualTo("Illegal dependency");
            assertThat(evidence.involvedTypes()).containsExactly("Domain", "Infrastructure");
            assertThat(evidence.relationships()).isEmpty();
        }

        @Test
        @DisplayName("Should reject null description")
        void shouldRejectNullDescription() {
            // When/Then
            assertThatThrownBy(() -> RelationshipEvidence.of(null, List.of("Type"), List.of("rel")))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("description required");
        }

        @Test
        @DisplayName("Should handle null lists")
        void shouldHandleNullLists() {
            // When
            RelationshipEvidence evidence = new RelationshipEvidence("Test description", null, null);

            // Then
            assertThat(evidence.involvedTypes()).isEmpty();
            assertThat(evidence.relationships()).isEmpty();
        }

        @Test
        @DisplayName("Should create immutable lists")
        void shouldCreateImmutableLists() {
            // Given
            RelationshipEvidence evidence =
                    RelationshipEvidence.of("Test", List.of("Type1"), List.of("Type1 -> Type2"));

            // When/Then
            assertThatThrownBy(() -> evidence.involvedTypes().add("Type3"))
                    .isInstanceOf(UnsupportedOperationException.class);

            assertThatThrownBy(() -> evidence.relationships().add("New relation"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("DependencyEvidence tests")
    class DependencyEvidenceTests {

        @Test
        @DisplayName("Should create dependency evidence with source and target")
        void shouldCreateDependencyEvidenceWithSourceAndTarget() {
            // When
            RelationshipEvidence evidence =
                    DependencyEvidence.of("Illegal dependency", "com.example.Domain", "com.example.Infrastructure");

            // Then
            assertThat(evidence.description()).isEqualTo("Illegal dependency");
            assertThat(evidence.involvedTypes()).containsExactly("com.example.Domain", "com.example.Infrastructure");
            assertThat(evidence.relationships()).containsExactly("com.example.Domain -> com.example.Infrastructure");
        }

        @Test
        @DisplayName("Should create dependency evidence with multiple dependencies")
        void shouldCreateDependencyEvidenceWithMultipleDependencies() {
            // When
            RelationshipEvidence evidence = DependencyEvidence.of(
                    "Multiple violations",
                    List.of("Domain1", "Domain2", "Infrastructure"),
                    List.of("Domain1 -> Infrastructure", "Domain2 -> Infrastructure"));

            // Then
            assertThat(evidence.description()).isEqualTo("Multiple violations");
            assertThat(evidence.involvedTypes()).containsExactly("Domain1", "Domain2", "Infrastructure");
            assertThat(evidence.relationships())
                    .containsExactly("Domain1 -> Infrastructure", "Domain2 -> Infrastructure");
        }
    }

    @Nested
    @DisplayName("Sealed interface behavior")
    class SealedInterfaceTests {

        @Test
        @DisplayName("Should be assignable to Evidence interface")
        void shouldBeAssignableToEvidenceInterface() {
            // Given
            Evidence structural = StructuralEvidence.of("Test", "Type");
            Evidence behavioral = BehavioralEvidence.of("Test", "Type", "method");
            Evidence relationship = RelationshipEvidence.of("Test", List.of("Type"));

            // Then: All should be Evidence instances
            assertThat(structural).isInstanceOf(Evidence.class);
            assertThat(behavioral).isInstanceOf(Evidence.class);
            assertThat(relationship).isInstanceOf(Evidence.class);
        }

        @Test
        @DisplayName("Should support instanceof checks")
        void shouldSupportInstanceofChecks() {
            // Given
            Evidence structural = StructuralEvidence.of("Test", "Type");
            Evidence behavioral = BehavioralEvidence.of("Test", "Type", "method");
            Evidence relationship = RelationshipEvidence.of("Test", List.of("Type"));

            // When/Then
            assertThat(structural).isInstanceOf(StructuralEvidence.class);
            assertThat(behavioral).isInstanceOf(BehavioralEvidence.class);
            assertThat(relationship).isInstanceOf(RelationshipEvidence.class);
        }

        @Test
        @DisplayName("All evidence types should implement common methods")
        void allEvidenceTypesShouldImplementCommonMethods() {
            // Given
            Evidence structural = StructuralEvidence.of("Structural issue", "Type1");
            Evidence behavioral = BehavioralEvidence.of("Behavioral issue", "Type2", "method");
            Evidence relationship = RelationshipEvidence.of("Relationship issue", List.of("Type3"));

            // Then: All should have description and involvedTypes
            assertThat(structural.description()).isEqualTo("Structural issue");
            assertThat(structural.involvedTypes()).containsExactly("Type1");

            assertThat(behavioral.description()).isEqualTo("Behavioral issue");
            assertThat(behavioral.involvedTypes()).containsExactly("Type2");

            assertThat(relationship.description()).isEqualTo("Relationship issue");
            assertThat(relationship.involvedTypes()).containsExactly("Type3");
        }
    }
}
