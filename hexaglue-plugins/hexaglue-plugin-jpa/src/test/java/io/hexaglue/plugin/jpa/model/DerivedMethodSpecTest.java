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

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;
import io.hexaglue.arch.ClassificationTrace;
import io.hexaglue.arch.ElementKind;
import io.hexaglue.arch.model.Identifier;
import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.TypeNature;
import io.hexaglue.arch.model.TypeRegistry;
import io.hexaglue.arch.model.TypeStructure;
import io.hexaglue.arch.model.index.DomainIndex;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DerivedMethodSpec}.
 *
 * @since 5.0.0
 */
class DerivedMethodSpecTest {

    private static final TypeName ENTITY_TYPE = ClassName.get("com.example.infrastructure.jpa", "OrderEntity");

    @Nested
    @DisplayName("C4 fix: Identifier type resolution")
    class IdentifierTypeResolution {

        @Test
        @DisplayName("C4 BUG: findByCustomerId(CustomerId) should use UUID instead of CustomerId")
        void findByCustomerIdShouldUseUuidInsteadOfIdentifier() {
            // Given: Port method findByCustomerId(CustomerId customerId) -> List<Order>
            io.hexaglue.arch.model.Parameter customerIdParam = io.hexaglue.arch.model.Parameter.of(
                    "customerId", TypeRef.of("com.ecommerce.domain.customer.CustomerId"));
            io.hexaglue.arch.model.Method method = new io.hexaglue.arch.model.Method(
                    "findByCustomerId",
                    TypeRef.of("java.util.List"),
                    List.of(customerIdParam),
                    Set.of(),
                    List.of(),
                    Optional.empty(),
                    List.of(),
                    Set.of(),
                    java.util.OptionalInt.empty());

            // Given: DomainIndex with CustomerId identifier that wraps UUID
            Identifier customerIdIdentifier = createIdentifier(
                    "com.ecommerce.domain.customer.CustomerId", "java.util.UUID");
            TypeRegistry registry = TypeRegistry.builder().add(customerIdIdentifier).build();
            DomainIndex domainIndex = DomainIndex.from(registry);

            // When
            DerivedMethodSpec spec = DerivedMethodSpec.fromV5(method, ENTITY_TYPE, Optional.of(domainIndex));

            // Then: Parameter type should be UUID, not CustomerId
            assertThat(spec).isNotNull();
            assertThat(spec.parameters()).hasSize(1);
            assertThat(spec.parameters().get(0).type().toString())
                    .as("C4 fix: Identifier type should be unwrapped to UUID")
                    .isEqualTo("java.util.UUID");
        }

        @Test
        @DisplayName("Non-identifier types should remain unchanged")
        void nonIdentifierTypesShouldRemainUnchanged() {
            // Given: Port method findByStatus(String status) -> List<Order>
            io.hexaglue.arch.model.Parameter statusParam =
                    io.hexaglue.arch.model.Parameter.of("status", TypeRef.of("java.lang.String"));
            io.hexaglue.arch.model.Method method = new io.hexaglue.arch.model.Method(
                    "findByStatus",
                    TypeRef.of("java.util.List"),
                    List.of(statusParam),
                    Set.of(),
                    List.of(),
                    Optional.empty(),
                    List.of(),
                    Set.of(),
                    java.util.OptionalInt.empty());

            // Given: Empty DomainIndex (no identifiers)
            TypeRegistry registry = TypeRegistry.builder().build();
            DomainIndex domainIndex = DomainIndex.from(registry);

            // When
            DerivedMethodSpec spec = DerivedMethodSpec.fromV5(method, ENTITY_TYPE, Optional.of(domainIndex));

            // Then: String should remain String
            assertThat(spec).isNotNull();
            assertThat(spec.parameters()).hasSize(1);
            assertThat(spec.parameters().get(0).type().toString()).isEqualTo("java.lang.String");
        }

        @Test
        @DisplayName("Without DomainIndex, types should remain unchanged")
        void withoutDomainIndexTypesShouldRemainUnchanged() {
            // Given: Port method findByCustomerId(CustomerId customerId) -> List<Order>
            io.hexaglue.arch.model.Parameter customerIdParam = io.hexaglue.arch.model.Parameter.of(
                    "customerId", TypeRef.of("com.ecommerce.domain.customer.CustomerId"));
            io.hexaglue.arch.model.Method method = new io.hexaglue.arch.model.Method(
                    "findByCustomerId",
                    TypeRef.of("java.util.List"),
                    List.of(customerIdParam),
                    Set.of(),
                    List.of(),
                    Optional.empty(),
                    List.of(),
                    Set.of(),
                    java.util.OptionalInt.empty());

            // When: No DomainIndex provided
            DerivedMethodSpec spec = DerivedMethodSpec.fromV5(method, ENTITY_TYPE, Optional.empty());

            // Then: CustomerId should remain as is (fallback behavior)
            assertThat(spec).isNotNull();
            assertThat(spec.parameters()).hasSize(1);
            assertThat(spec.parameters().get(0).type().toString())
                    .isEqualTo("com.ecommerce.domain.customer.CustomerId");
        }

        private Identifier createIdentifier(String qualifiedName, String wrappedTypeName) {
            TypeId typeId = TypeId.of(qualifiedName);
            TypeStructure structure = TypeStructure.builder(TypeNature.RECORD).build();
            ClassificationTrace trace = ClassificationTrace.highConfidence(
                    ElementKind.IDENTIFIER, "identifier-pattern", "Wraps UUID");
            TypeRef wrappedType = TypeRef.of(wrappedTypeName);
            return Identifier.of(typeId, structure, trace, wrappedType);
        }
    }
}
