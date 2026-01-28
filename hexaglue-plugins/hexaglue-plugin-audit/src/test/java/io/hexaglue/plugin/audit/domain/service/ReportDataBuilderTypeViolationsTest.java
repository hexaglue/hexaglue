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

package io.hexaglue.plugin.audit.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.plugin.audit.domain.model.ConstraintId;
import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.plugin.audit.domain.model.report.TypeViolation;
import io.hexaglue.plugin.audit.domain.model.report.TypeViolation.ViolationType;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests for type violation mapping in {@link ReportDataBuilder}.
 *
 * <p>Tests that all 11 constraint IDs are correctly mapped to TypeViolation instances.
 *
 * @since 5.0.0
 */
class ReportDataBuilderTypeViolationsTest {

    private Method mapConstraintToTypeViolationMethod;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        // Access the private method for testing
        mapConstraintToTypeViolationMethod =
                ReportDataBuilder.class.getDeclaredMethod("mapConstraintToTypeViolation", String.class, String.class);
        mapConstraintToTypeViolationMethod.setAccessible(true);
    }

    @Nested
    @DisplayName("Constraint ID to ViolationType mapping")
    class ConstraintMappingTests {

        @ParameterizedTest(name = "constraintId={0} -> violationType={1}")
        @CsvSource({
            // Existing DDD constraints
            "ddd:value-object-immutable, MUTABLE_VALUE_OBJECT",
            "ddd:domain-purity, IMPURE_DOMAIN",
            "ddd:aggregate-boundary, BOUNDARY_VIOLATION",
            // New DDD constraints
            "ddd:entity-identity, MISSING_IDENTITY",
            "ddd:aggregate-repository, MISSING_REPOSITORY",
            "ddd:event-naming, EVENT_NAMING",
            // New hexagonal constraints
            "hexagonal:port-coverage, PORT_UNCOVERED",
            "hexagonal:dependency-inversion, DEPENDENCY_INVERSION",
            "hexagonal:layer-isolation, LAYER_VIOLATION",
            "hexagonal:port-interface, PORT_NOT_INTERFACE"
        })
        @DisplayName("should map constraint ID to correct ViolationType")
        void shouldMapConstraintIdToCorrectViolationType(String constraintId, String expectedTypeName)
                throws Exception {
            // Given
            ViolationType expectedType = ViolationType.valueOf(expectedTypeName);

            // When
            TypeViolation result =
                    (TypeViolation) mapConstraintToTypeViolationMethod.invoke(null, constraintId, "TestType");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.typeName()).isEqualTo("TestType");
            assertThat(result.violationType()).isEqualTo(expectedType);
        }

        @Test
        @DisplayName("should return null for unknown constraint ID")
        void shouldReturnNullForUnknownConstraintId() throws Exception {
            // When
            TypeViolation result =
                    (TypeViolation) mapConstraintToTypeViolationMethod.invoke(null, "unknown:constraint", "TestType");

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should return null for aggregate cycle constraint (handled separately)")
        void shouldReturnNullForAggregateCycleConstraint() throws Exception {
            // Aggregate cycle is handled in buildRelationships, not buildTypeViolations
            TypeViolation result =
                    (TypeViolation) mapConstraintToTypeViolationMethod.invoke(null, "ddd:aggregate-cycle", "Order");

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("Violation extraction")
    class ViolationExtractionTests {

        @Test
        @DisplayName("should extract type violations from violations list")
        void shouldExtractTypeViolationsFromViolationsList() throws Exception {
            // Use the private buildTypeViolations method through reflection
            Method buildTypeViolationsMethod =
                    ReportDataBuilder.class.getDeclaredMethod("buildTypeViolations", List.class);
            buildTypeViolationsMethod.setAccessible(true);

            // Given - violations covering various constraint IDs
            var violations = List.of(
                    createViolation("ddd:value-object-immutable", "com.example.Money"),
                    createViolation("ddd:domain-purity", "com.example.OrderService"),
                    createViolation("ddd:entity-identity", "com.example.Customer"),
                    createViolation("hexagonal:port-coverage", "com.example.PaymentGateway"));

            // When
            @SuppressWarnings("unchecked")
            List<TypeViolation> result =
                    (List<TypeViolation>) buildTypeViolationsMethod.invoke(new ReportDataBuilder(), violations);

            // Then
            assertThat(result).hasSize(4);
            assertThat(result)
                    .extracting(TypeViolation::typeName)
                    .containsExactlyInAnyOrder("Money", "OrderService", "Customer", "PaymentGateway");
            assertThat(result)
                    .extracting(TypeViolation::violationType)
                    .containsExactlyInAnyOrder(
                            ViolationType.MUTABLE_VALUE_OBJECT,
                            ViolationType.IMPURE_DOMAIN,
                            ViolationType.MISSING_IDENTITY,
                            ViolationType.PORT_UNCOVERED);
        }

        @Test
        @DisplayName("should not duplicate violations for same type and constraint")
        void shouldNotDuplicateViolationsForSameTypeAndConstraint() throws Exception {
            Method buildTypeViolationsMethod =
                    ReportDataBuilder.class.getDeclaredMethod("buildTypeViolations", List.class);
            buildTypeViolationsMethod.setAccessible(true);

            // Given - same type with same constraint twice
            var violations = List.of(
                    createViolation("ddd:value-object-immutable", "com.example.Money"),
                    createViolation("ddd:value-object-immutable", "com.example.Money"));

            // When
            @SuppressWarnings("unchecked")
            List<TypeViolation> result =
                    (List<TypeViolation>) buildTypeViolationsMethod.invoke(new ReportDataBuilder(), violations);

            // Then - only one violation should be present
            assertThat(result).hasSize(1);
            assertThat(result.get(0).typeName()).isEqualTo("Money");
        }

        @Test
        @DisplayName("should extract simple name from qualified name")
        void shouldExtractSimpleNameFromQualifiedName() throws Exception {
            Method buildTypeViolationsMethod =
                    ReportDataBuilder.class.getDeclaredMethod("buildTypeViolations", List.class);
            buildTypeViolationsMethod.setAccessible(true);

            // Given
            var violations = List.of(createViolation("ddd:entity-identity", "com.example.domain.order.Customer"));

            // When
            @SuppressWarnings("unchecked")
            List<TypeViolation> result =
                    (List<TypeViolation>) buildTypeViolationsMethod.invoke(new ReportDataBuilder(), violations);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).typeName()).isEqualTo("Customer");
        }

        @Test
        @DisplayName("should skip violations with empty type names")
        void shouldSkipViolationsWithEmptyTypeNames() throws Exception {
            Method buildTypeViolationsMethod =
                    ReportDataBuilder.class.getDeclaredMethod("buildTypeViolations", List.class);
            buildTypeViolationsMethod.setAccessible(true);

            // Given - violation with empty affected types
            var violation = Violation.builder(ConstraintId.of("ddd:value-object-immutable"))
                    .message("Test violation")
                    .severity(Severity.MAJOR)
                    .affectedType("")
                    .build();

            var violations = List.of(violation);

            // When
            @SuppressWarnings("unchecked")
            List<TypeViolation> result =
                    (List<TypeViolation>) buildTypeViolationsMethod.invoke(new ReportDataBuilder(), violations);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("All 11 constraint mappings")
    class AllConstraintMappingsTests {

        @Test
        @DisplayName("should map all 11 constraint IDs to TypeViolations")
        void shouldMapAllElevenConstraintIds() throws Exception {
            // All 11 constraint IDs that should be mapped
            var constraintMappings = List.of(
                    // DDD constraints (existing + new)
                    new String[] {"ddd:value-object-immutable", "MUTABLE_VALUE_OBJECT"},
                    new String[] {"ddd:domain-purity", "IMPURE_DOMAIN"},
                    new String[] {"ddd:aggregate-boundary", "BOUNDARY_VIOLATION"},
                    new String[] {"ddd:entity-identity", "MISSING_IDENTITY"},
                    new String[] {"ddd:aggregate-repository", "MISSING_REPOSITORY"},
                    new String[] {"ddd:event-naming", "EVENT_NAMING"},
                    // Hexagonal constraints (new)
                    new String[] {"hexagonal:port-coverage", "PORT_UNCOVERED"},
                    new String[] {"hexagonal:dependency-inversion", "DEPENDENCY_INVERSION"},
                    new String[] {"hexagonal:layer-isolation", "LAYER_VIOLATION"},
                    new String[] {"hexagonal:port-interface", "PORT_NOT_INTERFACE"});

            for (String[] mapping : constraintMappings) {
                String constraintId = mapping[0];
                ViolationType expectedType = ViolationType.valueOf(mapping[1]);

                TypeViolation result =
                        (TypeViolation) mapConstraintToTypeViolationMethod.invoke(null, constraintId, "TestType");

                assertThat(result)
                        .as("Mapping for constraint %s", constraintId)
                        .isNotNull()
                        .extracting(TypeViolation::violationType)
                        .isEqualTo(expectedType);
            }
        }
    }

    // Helper method to create test violations
    private Violation createViolation(String constraintId, String affectedType) {
        return Violation.builder(ConstraintId.of(constraintId))
                .message("Test violation for " + affectedType)
                .severity(Severity.MAJOR)
                .affectedType(affectedType)
                .build();
    }
}
