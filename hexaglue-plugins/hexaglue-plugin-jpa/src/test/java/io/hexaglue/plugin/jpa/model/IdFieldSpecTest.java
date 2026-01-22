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

package io.hexaglue.plugin.jpa.model;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.arch.model.Annotation;
import io.hexaglue.arch.model.Field;
import io.hexaglue.arch.model.FieldRole;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link IdFieldSpec} V5 Field mapping validation.
 *
 * <p>These tests validate that IdFieldSpec correctly maps V5 Field
 * to the intermediate representation needed for JPA @Id and @GeneratedValue annotations.
 */
class IdFieldSpecTest {

    /**
     * Helper to create @GeneratedValue annotation with strategy.
     */
    private Annotation generatedValueAnnotation(String strategy) {
        return Annotation.of(
                "jakarta.persistence.GeneratedValue",
                Map.of("strategy", strategy),
                Map.of(
                        "strategy",
                        new io.hexaglue.arch.model.AnnotationValue.EnumVal(
                                "jakarta.persistence.GenerationType", strategy)));
    }

    @Test
    void from_shouldMapSimpleIdentity_whenUnwrappedUuid() {
        // Given: A simple UUID identity without wrapper
        Field field = Field.builder("id", TypeRef.of("java.util.UUID"))
                .annotations(List.of(generatedValueAnnotation("UUID")))
                .roles(Set.of(FieldRole.IDENTITY))
                .build();

        // When: Converting to IdFieldSpec
        IdFieldSpec spec = IdFieldSpec.from(field);

        // Then: Both types should be the same UUID
        assertThat(spec.fieldName()).isEqualTo("id");
        assertThat(spec.javaType().toString()).isEqualTo("java.util.UUID");
        assertThat(spec.unwrappedType().toString()).isEqualTo("java.util.UUID");
        assertThat(spec.strategy()).isEqualTo(io.hexaglue.spi.ir.IdentityStrategy.UUID);
        assertThat(spec.wrapperKind()).isEqualTo(io.hexaglue.spi.ir.IdentityWrapperKind.NONE);
        assertThat(spec.isWrapped()).isFalse();
        assertThat(spec.requiresGeneratedValue()).isTrue();
        assertThat(spec.isUuidGenerated()).isTrue();
    }

    @Test
    void from_shouldMapWrappedIdentity_whenRecordWrapper() {
        // Given: A wrapped identity (e.g., record OrderId(UUID value))
        Field field = Field.builder("orderId", TypeRef.of("com.example.OrderId"))
                .wrappedType(TypeRef.of("java.util.UUID"))
                .roles(Set.of(FieldRole.IDENTITY))
                .build();

        // When: Converting to IdFieldSpec (without TypeStructure, defaults to CLASS wrapper)
        IdFieldSpec spec = IdFieldSpec.from(field);

        // Then: Should distinguish between wrapper and unwrapped type
        assertThat(spec.fieldName()).isEqualTo("orderId");
        assertThat(spec.javaType().toString()).isEqualTo("com.example.OrderId");
        assertThat(spec.unwrappedType().toString()).isEqualTo("java.util.UUID");
        assertThat(spec.isWrapped()).isTrue();
        // NOTE: Without TypeStructure, detection defaults to CLASS. To get RECORD, pass TypeStructure.
        assertThat(spec.wrapperKind()).isEqualTo(io.hexaglue.spi.ir.IdentityWrapperKind.CLASS);
    }

    @Test
    void isWrapped_shouldReturnCorrectValue_basedOnWrapperKind() {
        // Given: Fields with different wrapper kinds
        Field unwrapped = Field.builder("id", TypeRef.of("java.lang.Long"))
                .annotations(List.of(generatedValueAnnotation("IDENTITY")))
                .roles(Set.of(FieldRole.IDENTITY))
                .build();

        Field recordWrapped = Field.builder("customerId", TypeRef.of("com.example.CustomerId"))
                .wrappedType(TypeRef.of("java.lang.Long"))
                .annotations(List.of(generatedValueAnnotation("IDENTITY")))
                .roles(Set.of(FieldRole.IDENTITY))
                .build();

        Field classWrapped = Field.builder("legacyId", TypeRef.of("com.example.LegacyId"))
                .wrappedType(TypeRef.of("java.lang.String"))
                .roles(Set.of(FieldRole.IDENTITY))
                .build();

        // When: Converting to specs
        IdFieldSpec unwrappedSpec = IdFieldSpec.from(unwrapped);
        IdFieldSpec recordSpec = IdFieldSpec.from(recordWrapped);
        IdFieldSpec classSpec = IdFieldSpec.from(classWrapped);

        // Then: isWrapped should reflect wrapper presence
        assertThat(unwrappedSpec.isWrapped()).isFalse();
        assertThat(recordSpec.isWrapped()).isTrue();
        assertThat(classSpec.isWrapped()).isTrue();
    }

    @Test
    void requiresGeneratedValue_shouldDelegateToStrategy() {
        // Given: Fields with different generation strategies
        Field uuidGenerated = Field.builder("id", TypeRef.of("java.util.UUID"))
                .annotations(List.of(generatedValueAnnotation("UUID")))
                .roles(Set.of(FieldRole.IDENTITY))
                .build();

        Field autoGenerated = Field.builder("id", TypeRef.of("java.lang.Long"))
                .annotations(List.of(generatedValueAnnotation("AUTO")))
                .roles(Set.of(FieldRole.IDENTITY))
                .build();

        Field assigned = Field.builder("id", TypeRef.of("java.lang.Long"))
                .roles(Set.of(FieldRole.IDENTITY))
                .build();

        Field natural = Field.builder("isbn", TypeRef.of("java.lang.String"))
                .roles(Set.of(FieldRole.IDENTITY))
                .build();

        // When: Converting to specs
        IdFieldSpec uuidSpec = IdFieldSpec.from(uuidGenerated);
        IdFieldSpec autoSpec = IdFieldSpec.from(autoGenerated);
        IdFieldSpec assignedSpec = IdFieldSpec.from(assigned);
        IdFieldSpec naturalSpec = IdFieldSpec.from(natural);

        // Then: Generated strategies require @GeneratedValue
        assertThat(uuidSpec.requiresGeneratedValue()).isTrue();
        assertThat(autoSpec.requiresGeneratedValue()).isTrue();
        assertThat(assignedSpec.requiresGeneratedValue()).isFalse();
        assertThat(naturalSpec.requiresGeneratedValue()).isFalse();
    }

    @Test
    void jpaGenerationType_shouldMapCorrectly_forJpaStrategies() {
        // Given: Fields with JPA generation strategies
        Field auto = Field.builder("id", TypeRef.of("java.lang.Long"))
                .annotations(List.of(generatedValueAnnotation("AUTO")))
                .roles(Set.of(FieldRole.IDENTITY))
                .build();

        Field identity = Field.builder("id", TypeRef.of("java.lang.Long"))
                .annotations(List.of(generatedValueAnnotation("IDENTITY")))
                .roles(Set.of(FieldRole.IDENTITY))
                .build();

        Field sequence = Field.builder("id", TypeRef.of("java.lang.Long"))
                .annotations(List.of(generatedValueAnnotation("SEQUENCE")))
                .roles(Set.of(FieldRole.IDENTITY))
                .build();

        Field table = Field.builder("id", TypeRef.of("java.lang.Long"))
                .annotations(List.of(generatedValueAnnotation("TABLE")))
                .roles(Set.of(FieldRole.IDENTITY))
                .build();

        Field uuid = Field.builder("id", TypeRef.of("java.util.UUID"))
                .annotations(List.of(generatedValueAnnotation("UUID")))
                .roles(Set.of(FieldRole.IDENTITY))
                .build();

        // When: Converting to specs and getting JPA type
        IdFieldSpec autoSpec = IdFieldSpec.from(auto);
        IdFieldSpec identitySpec = IdFieldSpec.from(identity);
        IdFieldSpec sequenceSpec = IdFieldSpec.from(sequence);
        IdFieldSpec tableSpec = IdFieldSpec.from(table);
        IdFieldSpec uuidSpec = IdFieldSpec.from(uuid);

        // Then: JPA generation type should match
        assertThat(autoSpec.jpaGenerationType()).isEqualTo("AUTO");
        assertThat(identitySpec.jpaGenerationType()).isEqualTo("IDENTITY");
        assertThat(sequenceSpec.jpaGenerationType()).isEqualTo("SEQUENCE");
        assertThat(tableSpec.jpaGenerationType()).isEqualTo("TABLE");
        assertThat(uuidSpec.jpaGenerationType()).isEqualTo("UUID");
    }

    @Test
    void isDatabaseGenerated_shouldExcludeUuid() {
        // Given: Database-generated and UUID fields
        Field dbGenerated = Field.builder("id", TypeRef.of("java.lang.Long"))
                .annotations(List.of(generatedValueAnnotation("IDENTITY")))
                .roles(Set.of(FieldRole.IDENTITY))
                .build();

        Field uuidGenerated = Field.builder("id", TypeRef.of("java.util.UUID"))
                .annotations(List.of(generatedValueAnnotation("UUID")))
                .roles(Set.of(FieldRole.IDENTITY))
                .build();

        // When: Converting to specs
        IdFieldSpec dbSpec = IdFieldSpec.from(dbGenerated);
        IdFieldSpec uuidSpec = IdFieldSpec.from(uuidGenerated);

        // Then: UUID is not database-generated (application-level)
        assertThat(dbSpec.isDatabaseGenerated()).isTrue();
        assertThat(uuidSpec.isDatabaseGenerated()).isFalse();
    }

    @Test
    void isUuidGenerated_shouldOnlyBeTrueForUuidStrategy() {
        // Given: Different generation strategies
        Field uuid = Field.builder("id", TypeRef.of("java.util.UUID"))
                .annotations(List.of(generatedValueAnnotation("UUID")))
                .roles(Set.of(FieldRole.IDENTITY))
                .build();

        Field dbIdentity = Field.builder("id", TypeRef.of("java.lang.Long"))
                .annotations(List.of(generatedValueAnnotation("IDENTITY")))
                .roles(Set.of(FieldRole.IDENTITY))
                .build();

        // When: Converting to specs
        IdFieldSpec uuidSpec = IdFieldSpec.from(uuid);
        IdFieldSpec dbSpec = IdFieldSpec.from(dbIdentity);

        // Then: Only UUID strategy returns true
        assertThat(uuidSpec.isUuidGenerated()).isTrue();
        assertThat(dbSpec.isUuidGenerated()).isFalse();
    }
}
