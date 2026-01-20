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

import io.hexaglue.arch.ClassificationTrace;
import io.hexaglue.arch.ElementKind;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ArchType} sealed interface hierarchy.
 *
 * @since 4.1.0
 */
@DisplayName("ArchType")
class ArchTypeTest {

    @Nested
    @DisplayName("Sealed Interface Structure")
    class SealedStructure {

        @Test
        @DisplayName("should be a sealed interface")
        void shouldBeSealedInterface() {
            assertThat(ArchType.class.isSealed()).isTrue();
        }

        @Test
        @DisplayName("should permit only DomainType, PortType, ApplicationType, and UnclassifiedType")
        void shouldPermitOnlyDefinedTypes() {
            Class<?>[] permitted = ArchType.class.getPermittedSubclasses();
            assertThat(permitted).hasSize(4);

            Set<String> names =
                    Arrays.stream(permitted).map(Class::getSimpleName).collect(Collectors.toSet());
            assertThat(names)
                    .containsExactlyInAnyOrder("DomainType", "PortType", "ApplicationType", "UnclassifiedType");
        }
    }

    @Nested
    @DisplayName("DomainType Interface")
    class DomainTypeInterface {

        @Test
        @DisplayName("should be a sealed interface extending ArchType")
        void shouldBeSealedExtendingArchType() {
            assertThat(DomainType.class.isSealed()).isTrue();
            assertThat(ArchType.class.isAssignableFrom(DomainType.class)).isTrue();
        }
    }

    @Nested
    @DisplayName("PortType Interface")
    class PortTypeInterface {

        @Test
        @DisplayName("should be a sealed interface extending ArchType")
        void shouldBeSealedExtendingArchType() {
            assertThat(PortType.class.isSealed()).isTrue();
            assertThat(ArchType.class.isAssignableFrom(PortType.class)).isTrue();
        }
    }

    @Nested
    @DisplayName("ApplicationType Interface")
    class ApplicationTypeInterface {

        @Test
        @DisplayName("should be a sealed interface extending ArchType")
        void shouldBeSealedExtendingArchType() {
            assertThat(ApplicationType.class.isSealed()).isTrue();
            assertThat(ArchType.class.isAssignableFrom(ApplicationType.class)).isTrue();
        }
    }

    @Nested
    @DisplayName("Default Methods")
    class DefaultMethods {

        @Test
        @DisplayName("qualifiedName should return id's qualified name")
        void qualifiedNameShouldReturnIdQualifiedName() {
            // given
            ArchType type = createTestArchType("com.example.Order");

            // then
            assertThat(type.qualifiedName()).isEqualTo("com.example.Order");
        }

        @Test
        @DisplayName("simpleName should return id's simple name")
        void simpleNameShouldReturnIdSimpleName() {
            // given
            ArchType type = createTestArchType("com.example.Order");

            // then
            assertThat(type.simpleName()).isEqualTo("Order");
        }

        @Test
        @DisplayName("packageName should return id's package name")
        void packageNameShouldReturnIdPackageName() {
            // given
            ArchType type = createTestArchType("com.example.Order");

            // then
            assertThat(type.packageName()).isEqualTo("com.example");
        }
    }

    /**
     * Creates a test implementation of ArchType for testing default methods.
     *
     * <p>Uses UnclassifiedType as a concrete implementation since ArchType is sealed.</p>
     */
    private ArchType createTestArchType(String qualifiedName) {
        TypeId id = TypeId.of(qualifiedName);
        TypeStructure structure = TypeStructure.builder(TypeNature.CLASS).build();
        ClassificationTrace trace =
                ClassificationTrace.highConfidence(ElementKind.AGGREGATE_ROOT, "test", "Test classification");

        // Use UnclassifiedType as a concrete implementation for testing default methods
        return UnclassifiedType.of(id, structure, trace, UnclassifiedType.UnclassifiedCategory.UNKNOWN);
    }
}
