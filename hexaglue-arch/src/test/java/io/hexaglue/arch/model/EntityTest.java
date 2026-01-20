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

package io.hexaglue.arch.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.hexaglue.arch.ClassificationTrace;
import io.hexaglue.arch.ElementKind;
import io.hexaglue.syntax.TypeRef;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Entity}.
 *
 * @since 4.1.0
 */
@DisplayName("Entity")
class EntityTest {

    private static final TypeId ID = TypeId.of("com.example.OrderLine");
    private static final TypeStructure STRUCTURE =
            TypeStructure.builder(TypeNature.CLASS).build();
    private static final ClassificationTrace TRACE =
            ClassificationTrace.highConfidence(ElementKind.ENTITY, "explicit-entity", "Has Entity annotation");
    private static final Field IDENTITY_FIELD = Field.builder("id", TypeRef.of("com.example.OrderLineId"))
            .roles(Set.of(FieldRole.IDENTITY))
            .build();

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("should create with all required fields and identity")
        void shouldCreateWithAllRequiredFieldsAndIdentity() {
            // when
            Entity entity = Entity.of(ID, STRUCTURE, TRACE, IDENTITY_FIELD);

            // then
            assertThat(entity.id()).isEqualTo(ID);
            assertThat(entity.structure()).isEqualTo(STRUCTURE);
            assertThat(entity.classification()).isEqualTo(TRACE);
            assertThat(entity.identityField()).isPresent().contains(IDENTITY_FIELD);
        }

        @Test
        @DisplayName("should create without identity field")
        void shouldCreateWithoutIdentityField() {
            // when
            Entity entity = Entity.of(ID, STRUCTURE, TRACE);

            // then
            assertThat(entity.id()).isEqualTo(ID);
            assertThat(entity.structure()).isEqualTo(STRUCTURE);
            assertThat(entity.classification()).isEqualTo(TRACE);
            assertThat(entity.identityField()).isEmpty();
        }

        @Test
        @DisplayName("should return ENTITY kind")
        void shouldReturnEntityKind() {
            // when
            Entity entity = Entity.of(ID, STRUCTURE, TRACE, IDENTITY_FIELD);

            // then
            assertThat(entity.kind()).isEqualTo(ArchKind.ENTITY);
        }

        @Test
        @DisplayName("should reject null id")
        void shouldRejectNullId() {
            assertThatThrownBy(() -> Entity.of(null, STRUCTURE, TRACE))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("id");
        }

        @Test
        @DisplayName("should reject null structure")
        void shouldRejectNullStructure() {
            assertThatThrownBy(() -> Entity.of(ID, null, TRACE))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("structure");
        }

        @Test
        @DisplayName("should reject null classification")
        void shouldRejectNullClassification() {
            assertThatThrownBy(() -> Entity.of(ID, STRUCTURE, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("classification");
        }

        @Test
        @DisplayName("should handle null identity field by creating empty Optional")
        void shouldHandleNullIdentityField() {
            // when
            Entity entity = Entity.of(ID, STRUCTURE, TRACE, null);

            // then
            assertThat(entity.identityField()).isEmpty();
        }
    }

    @Nested
    @DisplayName("ArchType Contract")
    class ArchTypeContract {

        @Test
        @DisplayName("should implement DomainType interface")
        void shouldImplementDomainType() {
            // given
            Entity entity = Entity.of(ID, STRUCTURE, TRACE, IDENTITY_FIELD);

            // then
            assertThat(entity).isInstanceOf(DomainType.class);
            assertThat(entity).isInstanceOf(ArchType.class);
        }

        @Test
        @DisplayName("should provide convenience methods from ArchType")
        void shouldProvideConvenienceMethods() {
            // given
            Entity entity = Entity.of(ID, STRUCTURE, TRACE, IDENTITY_FIELD);

            // then
            assertThat(entity.qualifiedName()).isEqualTo("com.example.OrderLine");
            assertThat(entity.simpleName()).isEqualTo("OrderLine");
            assertThat(entity.packageName()).isEqualTo("com.example");
        }
    }

    @Nested
    @DisplayName("Identity Field")
    class IdentityFieldTests {

        @Test
        @DisplayName("should return identity field when present")
        void shouldReturnIdentityFieldWhenPresent() {
            // given
            Entity entity = Entity.of(ID, STRUCTURE, TRACE, IDENTITY_FIELD);

            // then
            assertThat(entity.identityField()).isPresent();
            assertThat(entity.identityField().get().name()).isEqualTo("id");
            assertThat(entity.identityField().get().isIdentity()).isTrue();
        }

        @Test
        @DisplayName("should return empty when no identity field")
        void shouldReturnEmptyWhenNoIdentityField() {
            // given
            Entity entity = Entity.of(ID, STRUCTURE, TRACE);

            // then
            assertThat(entity.identityField()).isEmpty();
        }

        @Test
        @DisplayName("hasIdentity should return true when identity field present")
        void hasIdentityShouldReturnTrueWhenPresent() {
            // given
            Entity entity = Entity.of(ID, STRUCTURE, TRACE, IDENTITY_FIELD);

            // then
            assertThat(entity.hasIdentity()).isTrue();
        }

        @Test
        @DisplayName("hasIdentity should return false when no identity field")
        void hasIdentityShouldReturnFalseWhenAbsent() {
            // given
            Entity entity = Entity.of(ID, STRUCTURE, TRACE);

            // then
            assertThat(entity.hasIdentity()).isFalse();
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("should be equal when all fields match")
        void shouldBeEqualWhenAllFieldsMatch() {
            // given
            Entity e1 = Entity.of(ID, STRUCTURE, TRACE, IDENTITY_FIELD);
            Entity e2 = Entity.of(ID, STRUCTURE, TRACE, IDENTITY_FIELD);

            // then
            assertThat(e1).isEqualTo(e2);
            assertThat(e1.hashCode()).isEqualTo(e2.hashCode());
        }

        @Test
        @DisplayName("should not be equal when ids differ")
        void shouldNotBeEqualWhenIdsDiffer() {
            // given
            TypeId otherId = TypeId.of("com.example.ProductLine");
            Entity e1 = Entity.of(ID, STRUCTURE, TRACE, IDENTITY_FIELD);
            Entity e2 = Entity.of(otherId, STRUCTURE, TRACE, IDENTITY_FIELD);

            // then
            assertThat(e1).isNotEqualTo(e2);
        }

        @Test
        @DisplayName("should not be equal when identity fields differ")
        void shouldNotBeEqualWhenIdentityFieldsDiffer() {
            // given
            Field otherField = Field.builder("otherId", TypeRef.of("java.lang.Long"))
                    .roles(Set.of(FieldRole.IDENTITY))
                    .build();
            Entity e1 = Entity.of(ID, STRUCTURE, TRACE, IDENTITY_FIELD);
            Entity e2 = Entity.of(ID, STRUCTURE, TRACE, otherField);

            // then
            assertThat(e1).isNotEqualTo(e2);
        }
    }
}
