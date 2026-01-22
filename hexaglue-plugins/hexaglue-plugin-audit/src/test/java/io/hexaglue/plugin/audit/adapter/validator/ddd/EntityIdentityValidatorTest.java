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

package io.hexaglue.plugin.audit.adapter.validator.ddd;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.ClassificationTrace;
import io.hexaglue.arch.ElementKind;
import io.hexaglue.arch.ProjectContext;
import io.hexaglue.arch.model.Entity;
import io.hexaglue.arch.model.Field;
import io.hexaglue.arch.model.FieldRole;
import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.TypeNature;
import io.hexaglue.arch.model.TypeRegistry;
import io.hexaglue.arch.model.TypeStructure;
import io.hexaglue.arch.model.index.DomainIndex;
import io.hexaglue.arch.model.index.PortIndex;
import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.plugin.audit.util.TestCodebaseBuilder;
import io.hexaglue.plugin.audit.util.TestModelBuilder;
import io.hexaglue.spi.audit.Codebase;
import io.hexaglue.syntax.Modifier;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link EntityIdentityValidator}.
 *
 * <p>Validates that entities and aggregate roots are correctly checked for identity fields
 * using the v5 ArchType API.
 *
 * @since 5.0.0 Migrated to v5 ArchType API
 */
class EntityIdentityValidatorTest {

    private EntityIdentityValidator validator;

    @BeforeEach
    void setUp() {
        validator = new EntityIdentityValidator();
    }

    @Test
    @DisplayName("Should pass when entity has identity field")
    void shouldPass_whenEntityHasIdentity() {
        // Given - entity with identity field (TestModelBuilder adds identity by default)
        ArchitecturalModel model =
                new TestModelBuilder().addEntity("com.example.domain.Order").build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail when entity is missing identity field")
    void shouldFail_whenEntityMissingIdentity() {
        // Given - entity without identity field
        ArchitecturalModel model = createModelWithEntityWithoutIdentity("com.example.domain.Order");
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).constraintId().value()).isEqualTo("ddd:entity-identity");
        assertThat(violations.get(0).severity()).isEqualTo(Severity.CRITICAL);
        assertThat(violations.get(0).message()).contains("Order").contains("no identity field");
        assertThat(violations.get(0).affectedTypes()).contains("com.example.domain.Order");
    }

    @Test
    @DisplayName("Should pass when aggregate has identity field")
    void shouldPass_whenAggregateHasIdentity() {
        // Given - aggregate root always has identity (by design in v5 API)
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.Customer")
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then - aggregate roots always have identity by design
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should check entities for identity")
    void shouldCheckEntitiesForIdentity() {
        // Given: Two entities - one valid (with identity), one invalid (without)
        ArchitecturalModel model = createModelWithMixedEntities();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then: Should find 1 violation for entity without identity
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).message()).contains("LineItem");
    }

    @Test
    @DisplayName("Should pass when codebase has no entities")
    void shouldPass_whenNoEntities() {
        // Given: Empty model
        ArchitecturalModel model = TestModelBuilder.emptyModel();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should provide structural evidence")
    void shouldProvideStructuralEvidence() {
        // Given
        ArchitecturalModel model = createModelWithEntityWithoutIdentity("com.example.domain.Order");
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).evidence()).isNotEmpty();
        assertThat(violations.get(0).evidence().get(0).description())
                .contains("identity")
                .contains("distinguish instances");
    }

    @Test
    @DisplayName("Should return correct default severity")
    void shouldReturnCorrectDefaultSeverity() {
        // When/Then
        assertThat(validator.defaultSeverity()).isEqualTo(Severity.CRITICAL);
    }

    @Test
    @DisplayName("Should return correct constraint ID")
    void shouldReturnCorrectConstraintId() {
        // When/Then
        assertThat(validator.constraintId().value()).isEqualTo("ddd:entity-identity");
    }

    // === Helper methods to create entities without identity ===

    private ArchitecturalModel createModelWithEntityWithoutIdentity(String qualifiedName) {
        TypeId id = TypeId.of(qualifiedName);
        TypeStructure structure = TypeStructure.builder(TypeNature.CLASS)
                .modifiers(Set.of(Modifier.PUBLIC))
                .build();
        ClassificationTrace trace = ClassificationTrace.highConfidence(ElementKind.ENTITY, "test", "Test entity");

        // Entity WITHOUT identity field
        Entity entity = Entity.of(id, structure, trace, Optional.empty(), Optional.empty());

        TypeRegistry.Builder registryBuilder = TypeRegistry.builder();
        registryBuilder.add(entity);
        TypeRegistry typeRegistry = registryBuilder.build();

        DomainIndex domainIndex = DomainIndex.from(typeRegistry);
        PortIndex portIndex = PortIndex.from(typeRegistry);

        return ArchitecturalModel.builder(ProjectContext.of("test", "com.example", java.nio.file.Path.of(".")))
                .typeRegistry(typeRegistry)
                .domainIndex(domainIndex)
                .portIndex(portIndex)
                .build();
    }

    private ArchitecturalModel createModelWithMixedEntities() {
        // Entity WITH identity
        TypeId orderId = TypeId.of("com.example.domain.Order");
        TypeStructure orderStructure = TypeStructure.builder(TypeNature.CLASS)
                .modifiers(Set.of(Modifier.PUBLIC))
                .build();
        ClassificationTrace orderTrace = ClassificationTrace.highConfidence(ElementKind.ENTITY, "test", "Test entity");
        Field orderIdField = Field.builder("id", TypeRef.of("java.lang.Long"))
                .roles(Set.of(FieldRole.IDENTITY))
                .build();
        Entity order = Entity.of(orderId, orderStructure, orderTrace, Optional.of(orderIdField), Optional.empty());

        // Entity WITHOUT identity
        TypeId lineItemId = TypeId.of("com.example.domain.LineItem");
        TypeStructure lineItemStructure = TypeStructure.builder(TypeNature.CLASS)
                .modifiers(Set.of(Modifier.PUBLIC))
                .build();
        ClassificationTrace lineItemTrace =
                ClassificationTrace.highConfidence(ElementKind.ENTITY, "test", "Test entity");
        Entity lineItem = Entity.of(lineItemId, lineItemStructure, lineItemTrace, Optional.empty(), Optional.empty());

        TypeRegistry.Builder registryBuilder = TypeRegistry.builder();
        registryBuilder.add(order);
        registryBuilder.add(lineItem);
        TypeRegistry typeRegistry = registryBuilder.build();

        DomainIndex domainIndex = DomainIndex.from(typeRegistry);
        PortIndex portIndex = PortIndex.from(typeRegistry);

        return ArchitecturalModel.builder(ProjectContext.of("test", "com.example", java.nio.file.Path.of(".")))
                .typeRegistry(typeRegistry)
                .domainIndex(domainIndex)
                .portIndex(portIndex)
                .build();
    }
}
