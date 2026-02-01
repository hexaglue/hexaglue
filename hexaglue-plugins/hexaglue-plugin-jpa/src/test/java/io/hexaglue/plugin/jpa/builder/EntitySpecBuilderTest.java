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

package io.hexaglue.plugin.jpa.builder;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.ClassificationTrace;
import io.hexaglue.arch.ElementKind;
import io.hexaglue.arch.ProjectContext;
import io.hexaglue.arch.model.AggregateRoot;
import io.hexaglue.arch.model.Entity;
import io.hexaglue.arch.model.Field;
import io.hexaglue.arch.model.FieldRole;
import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.TypeNature;
import io.hexaglue.arch.model.TypeStructure;
import io.hexaglue.arch.model.ir.IdentityStrategy;
import io.hexaglue.arch.model.ir.IdentityWrapperKind;
import io.hexaglue.plugin.jpa.JpaConfig;
import io.hexaglue.plugin.jpa.model.EntitySpec;
import io.hexaglue.plugin.jpa.model.PropertyFieldSpec;
import io.hexaglue.syntax.Modifier;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link EntitySpecBuilder}.
 *
 * <p>These tests validate that domain audit fields (createdAt, updatedAt) are correctly
 * handled when JPA auditing is enabled or disabled. When auditing is enabled,
 * {@code JpaEntityCodegen.addAuditingFields()} generates these fields with Spring Data
 * annotations, so the builder must exclude them from regular properties to avoid
 * duplicate field compilation errors.
 *
 * @since 5.0.0
 */
@DisplayName("EntitySpecBuilder")
class EntitySpecBuilderTest {

    private static final String TEST_PKG = "com.example.domain";
    private static final String INFRA_PKG = "com.example.infrastructure.jpa";

    private static ClassificationTrace highConfidence(ElementKind kind) {
        return ClassificationTrace.highConfidence(kind, "test", "Test classification");
    }

    private static ArchitecturalModel minimalModel() {
        ProjectContext project = ProjectContext.forTesting("test-project", "com.example");
        return ArchitecturalModel.builder(project).build();
    }

    private static JpaConfig configWithAuditing(boolean enableAuditing) {
        return new JpaConfig(
                "Entity",
                "Embeddable",
                "JpaRepository",
                "Adapter",
                "Mapper",
                "", // tablePrefix
                enableAuditing,
                false, // enableOptimisticLocking
                true, // generateAdapters
                true, // generateMappers
                true, // generateRepositories
                true // generateEmbeddables
                );
    }

    /**
     * Creates an AggregateRoot with the given fields (plus the identity field).
     */
    private static AggregateRoot createAggregate(List<Field> additionalFields) {
        Field identityField = Field.builder("id", TypeRef.of("java.util.UUID"))
                .roles(Set.of(FieldRole.IDENTITY))
                .build();

        List<Field> allFields = new java.util.ArrayList<>();
        allFields.add(identityField);
        allFields.addAll(additionalFields);

        TypeStructure structure = TypeStructure.builder(TypeNature.CLASS)
                .modifiers(Set.of(Modifier.PUBLIC))
                .fields(allFields)
                .build();

        return AggregateRoot.builder(
                        TypeId.of(TEST_PKG + ".Order"),
                        structure,
                        highConfidence(ElementKind.AGGREGATE_ROOT),
                        identityField)
                .effectiveIdentityType(TypeRef.of("java.util.UUID"))
                .build();
    }

    @Nested
    @DisplayName("When auditing is enabled")
    class WhenAuditingIsEnabled {

        @Test
        @DisplayName("should exclude createdAt and updatedAt from properties")
        void shouldExcludeAuditFieldsFromProperties() {
            // Given: An aggregate with domain audit fields createdAt and updatedAt
            Field createdAt = Field.builder("createdAt", TypeRef.of("java.time.LocalDateTime"))
                    .roles(Set.of(FieldRole.AUDIT))
                    .build();
            Field updatedAt = Field.builder("updatedAt", TypeRef.of("java.time.LocalDateTime"))
                    .roles(Set.of(FieldRole.AUDIT))
                    .build();
            Field name = Field.builder("name", TypeRef.of("java.lang.String")).build();

            AggregateRoot aggregate = createAggregate(List.of(name, createdAt, updatedAt));

            // When: Building EntitySpec with auditing enabled
            EntitySpec spec = EntitySpecBuilder.builder()
                    .aggregateRoot(aggregate)
                    .model(minimalModel())
                    .config(configWithAuditing(true))
                    .infrastructurePackage(INFRA_PKG)
                    .build();

            // Then: createdAt and updatedAt should NOT appear in properties
            List<String> propertyNames =
                    spec.properties().stream().map(PropertyFieldSpec::fieldName).toList();
            assertThat(propertyNames).doesNotContain("createdAt", "updatedAt");
        }

        @Test
        @DisplayName("should keep non-audit domain fields")
        void shouldKeepNonAuditFields() {
            // Given: An aggregate with regular fields and audit fields
            Field name = Field.builder("name", TypeRef.of("java.lang.String")).build();
            Field status =
                    Field.builder("status", TypeRef.of("java.lang.String")).build();
            Field createdAt = Field.builder("createdAt", TypeRef.of("java.time.LocalDateTime"))
                    .roles(Set.of(FieldRole.AUDIT))
                    .build();
            Field updatedAt = Field.builder("updatedAt", TypeRef.of("java.time.LocalDateTime"))
                    .roles(Set.of(FieldRole.AUDIT))
                    .build();

            AggregateRoot aggregate = createAggregate(List.of(name, status, createdAt, updatedAt));

            // When
            EntitySpec spec = EntitySpecBuilder.builder()
                    .aggregateRoot(aggregate)
                    .model(minimalModel())
                    .config(configWithAuditing(true))
                    .infrastructurePackage(INFRA_PKG)
                    .build();

            // Then: name and status should be present
            List<String> propertyNames =
                    spec.properties().stream().map(PropertyFieldSpec::fieldName).toList();
            assertThat(propertyNames).containsExactly("name", "status");
        }

        @Test
        @DisplayName("should keep other AUDIT-role fields like createdBy that are not generated by auditing")
        void shouldKeepNonGeneratedAuditRoleFields() {
            // Given: An aggregate with createdBy (AUDIT role but NOT generated by addAuditingFields)
            Field createdBy = Field.builder("createdBy", TypeRef.of("java.lang.String"))
                    .roles(Set.of(FieldRole.AUDIT))
                    .build();
            Field createdAt = Field.builder("createdAt", TypeRef.of("java.time.LocalDateTime"))
                    .roles(Set.of(FieldRole.AUDIT))
                    .build();

            AggregateRoot aggregate = createAggregate(List.of(createdBy, createdAt));

            // When
            EntitySpec spec = EntitySpecBuilder.builder()
                    .aggregateRoot(aggregate)
                    .model(minimalModel())
                    .config(configWithAuditing(true))
                    .infrastructurePackage(INFRA_PKG)
                    .build();

            // Then: createdBy should be kept (not generated by auditing), createdAt should be excluded
            List<String> propertyNames =
                    spec.properties().stream().map(PropertyFieldSpec::fieldName).toList();
            assertThat(propertyNames).contains("createdBy");
            assertThat(propertyNames).doesNotContain("createdAt");
        }
    }

    @Nested
    @DisplayName("When auditing is disabled")
    class WhenAuditingIsDisabled {

        @Test
        @DisplayName("should keep createdAt and updatedAt as regular properties")
        void shouldKeepAuditFieldsAsRegularProperties() {
            // Given: An aggregate with createdAt and updatedAt fields
            Field createdAt = Field.builder("createdAt", TypeRef.of("java.time.LocalDateTime"))
                    .roles(Set.of(FieldRole.AUDIT))
                    .build();
            Field updatedAt = Field.builder("updatedAt", TypeRef.of("java.time.LocalDateTime"))
                    .roles(Set.of(FieldRole.AUDIT))
                    .build();
            Field name = Field.builder("name", TypeRef.of("java.lang.String")).build();

            AggregateRoot aggregate = createAggregate(List.of(name, createdAt, updatedAt));

            // When: Building EntitySpec with auditing disabled
            EntitySpec spec = EntitySpecBuilder.builder()
                    .aggregateRoot(aggregate)
                    .model(minimalModel())
                    .config(configWithAuditing(false))
                    .infrastructurePackage(INFRA_PKG)
                    .build();

            // Then: All fields should appear as properties (no filtering)
            List<String> propertyNames =
                    spec.properties().stream().map(PropertyFieldSpec::fieldName).toList();
            assertThat(propertyNames).containsExactly("name", "createdAt", "updatedAt");
        }
    }

    /**
     * Tests for entities without a detected identity field.
     *
     * <p>When a child entity like {@code OrderLine} has no field detected as IDENTITY
     * by the FieldRoleDetector, the builder should generate a surrogate {@code Long id}
     * with {@code @GeneratedValue(strategy = IDENTITY)} instead of throwing.
     *
     * @since 5.0.0
     */
    @Nested
    @DisplayName("When entity has no identity field")
    class WhenEntityHasNoIdentityField {

        /**
         * Creates an Entity without an identity field, simulating a child entity
         * like OrderLine where the FieldRoleDetector did not detect an identity.
         */
        private static Entity createEntityWithoutIdentity(List<Field> fields) {
            TypeStructure structure = TypeStructure.builder(TypeNature.CLASS)
                    .modifiers(Set.of(Modifier.PUBLIC))
                    .fields(fields)
                    .build();

            return Entity.of(TypeId.of(TEST_PKG + ".OrderLine"), structure, highConfidence(ElementKind.ENTITY));
        }

        @Test
        @DisplayName("should generate surrogate id when no identity field detected")
        void shouldGenerateSurrogateId_whenNoIdentityFieldDetected() {
            // Given: An entity without any identity field
            Field lineNumber = Field.builder("lineNumber", TypeRef.of("int")).build();
            Field quantity = Field.builder("quantity", TypeRef.of("int")).build();

            Entity entity = createEntityWithoutIdentity(List.of(lineNumber, quantity));

            // When: Building EntitySpec
            EntitySpec spec = EntitySpecBuilder.builder()
                    .entity(entity)
                    .model(minimalModel())
                    .config(configWithAuditing(false))
                    .infrastructurePackage(INFRA_PKG)
                    .build();

            // Then: Should have a surrogate Long id with IDENTITY strategy
            assertThat(spec.idField().fieldName()).isEqualTo("id");
            assertThat(spec.idField().javaType().toString()).isEqualTo("java.lang.Long");
            assertThat(spec.idField().strategy()).isEqualTo(IdentityStrategy.IDENTITY);
            assertThat(spec.idField().wrapperKind()).isEqualTo(IdentityWrapperKind.NONE);
            assertThat(spec.idField().requiresGeneratedValue()).isTrue();
        }

        @Test
        @DisplayName("should keep all domain fields as properties when no identity field")
        void shouldKeepAllDomainFieldsAsProperties_whenNoIdentityField() {
            // Given: An entity with regular fields but no identity
            Field lineNumber = Field.builder("lineNumber", TypeRef.of("int")).build();
            Field quantity = Field.builder("quantity", TypeRef.of("int")).build();
            Field description =
                    Field.builder("description", TypeRef.of("java.lang.String")).build();

            Entity entity = createEntityWithoutIdentity(List.of(lineNumber, quantity, description));

            // When
            EntitySpec spec = EntitySpecBuilder.builder()
                    .entity(entity)
                    .model(minimalModel())
                    .config(configWithAuditing(false))
                    .infrastructurePackage(INFRA_PKG)
                    .build();

            // Then: All domain fields should be in properties (none excluded as identity)
            List<String> propertyNames =
                    spec.properties().stream().map(PropertyFieldSpec::fieldName).toList();
            assertThat(propertyNames).containsExactly("lineNumber", "quantity", "description");
        }

        @Test
        @DisplayName("should produce valid EntitySpec for identity-less entity")
        void shouldProduceValidEntitySpec_forIdentityLessEntity() {
            // Given: An entity without identity
            Field lineNumber = Field.builder("lineNumber", TypeRef.of("int")).build();

            Entity entity = createEntityWithoutIdentity(List.of(lineNumber));

            // When
            EntitySpec spec = EntitySpecBuilder.builder()
                    .entity(entity)
                    .model(minimalModel())
                    .config(configWithAuditing(false))
                    .infrastructurePackage(INFRA_PKG)
                    .build();

            // Then: EntitySpec should be valid with correct naming
            assertThat(spec.className()).isEqualTo("OrderLineEntity");
            assertThat(spec.tableName()).isEqualTo("order_line");
            assertThat(spec.domainQualifiedName()).isEqualTo(TEST_PKG + ".OrderLine");
            assertThat(spec.packageName()).isEqualTo(INFRA_PKG);
            assertThat(spec.idField()).isNotNull();
        }
    }
}
