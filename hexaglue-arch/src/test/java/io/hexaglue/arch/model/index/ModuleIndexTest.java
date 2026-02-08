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

package io.hexaglue.arch.model.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import io.hexaglue.arch.model.TypeId;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ModuleIndex}.
 *
 * @since 5.0.0
 */
@DisplayName("ModuleIndex")
class ModuleIndexTest {

    private static final ModuleDescriptor CORE_MODULE =
            ModuleDescriptor.of("banking-core", ModuleRole.DOMAIN, Path.of("/projects/banking-core"));
    private static final ModuleDescriptor INFRA_MODULE = ModuleDescriptor.of(
            "banking-persistence", ModuleRole.INFRASTRUCTURE, Path.of("/projects/banking-persistence"));
    private static final ModuleDescriptor APP_MODULE =
            ModuleDescriptor.of("banking-service", ModuleRole.APPLICATION, Path.of("/projects/banking-service"));

    private static final TypeId ORDER_ID = TypeId.of("com.example.Order");
    private static final TypeId ORDER_ENTITY_ID = TypeId.of("com.example.OrderJpaEntity");
    private static final TypeId CUSTOMER_ID = TypeId.of("com.example.Customer");

    @Nested
    @DisplayName("builder()")
    class BuilderTests {

        @Test
        @DisplayName("should build empty index")
        void shouldBuildEmptyIndex() {
            ModuleIndex index = ModuleIndex.builder().build();

            assertThat(index.size()).isZero();
            assertThat(index.modules().count()).isZero();
        }

        @Test
        @DisplayName("should register modules")
        void shouldRegisterModules() {
            ModuleIndex index = ModuleIndex.builder()
                    .addModule(CORE_MODULE)
                    .addModule(INFRA_MODULE)
                    .build();

            assertThat(index.size()).isEqualTo(2);
        }

        @Test
        @DisplayName("should reject null module")
        void shouldRejectNullModule() {
            assertThatNullPointerException()
                    .isThrownBy(() -> ModuleIndex.builder().addModule(null))
                    .withMessageContaining("module");
        }

        @Test
        @DisplayName("should reject null typeId in assignType")
        void shouldRejectNullTypeId() {
            assertThatNullPointerException()
                    .isThrownBy(() -> ModuleIndex.builder().assignType(null, CORE_MODULE))
                    .withMessageContaining("typeId");
        }

        @Test
        @DisplayName("should reject null module in assignType")
        void shouldRejectNullModuleInAssignType() {
            assertThatNullPointerException()
                    .isThrownBy(() -> ModuleIndex.builder().assignType(ORDER_ID, null))
                    .withMessageContaining("module");
        }
    }

    @Nested
    @DisplayName("moduleOf(TypeId)")
    class ModuleOfTests {

        @Test
        @DisplayName("should return module for assigned type")
        void shouldReturnModuleForAssignedType() {
            ModuleIndex index = ModuleIndex.builder()
                    .addModule(CORE_MODULE)
                    .assignType(ORDER_ID, CORE_MODULE)
                    .build();

            Optional<ModuleDescriptor> result = index.moduleOf(ORDER_ID);

            assertThat(result).contains(CORE_MODULE);
        }

        @Test
        @DisplayName("should return empty for unassigned type")
        void shouldReturnEmptyForUnassignedType() {
            ModuleIndex index = ModuleIndex.builder().addModule(CORE_MODULE).build();

            Optional<ModuleDescriptor> result = index.moduleOf(ORDER_ID);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should reject null typeId")
        void shouldRejectNullTypeId() {
            ModuleIndex index = ModuleIndex.builder().build();

            assertThatNullPointerException()
                    .isThrownBy(() -> index.moduleOf(null))
                    .withMessageContaining("typeId");
        }
    }

    @Nested
    @DisplayName("module(String)")
    class ModuleTests {

        @Test
        @DisplayName("should return module for known moduleId")
        void shouldReturnModuleForKnownModuleId() {
            ModuleIndex index = ModuleIndex.builder()
                    .addModule(CORE_MODULE)
                    .addModule(INFRA_MODULE)
                    .build();

            Optional<ModuleDescriptor> result = index.module("banking-core");

            assertThat(result).contains(CORE_MODULE);
        }

        @Test
        @DisplayName("should return empty for unknown moduleId")
        void shouldReturnEmptyForUnknownModuleId() {
            ModuleIndex index = ModuleIndex.builder().addModule(CORE_MODULE).build();

            Optional<ModuleDescriptor> result = index.module("unknown-module");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should reject null moduleId")
        void shouldRejectNullModuleId() {
            ModuleIndex index = ModuleIndex.builder().build();

            assertThatNullPointerException()
                    .isThrownBy(() -> index.module(null))
                    .withMessageContaining("moduleId");
        }
    }

    @Nested
    @DisplayName("typesInModule(String)")
    class TypesInModuleTests {

        @Test
        @DisplayName("should return types assigned to module")
        void shouldReturnTypesAssignedToModule() {
            ModuleIndex index = ModuleIndex.builder()
                    .addModule(CORE_MODULE)
                    .addModule(INFRA_MODULE)
                    .assignType(ORDER_ID, CORE_MODULE)
                    .assignType(CUSTOMER_ID, CORE_MODULE)
                    .assignType(ORDER_ENTITY_ID, INFRA_MODULE)
                    .build();

            List<TypeId> coreTypes = index.typesInModule("banking-core").toList();
            List<TypeId> infraTypes = index.typesInModule("banking-persistence").toList();

            assertThat(coreTypes).containsExactlyInAnyOrder(ORDER_ID, CUSTOMER_ID);
            assertThat(infraTypes).containsExactly(ORDER_ENTITY_ID);
        }

        @Test
        @DisplayName("should return empty stream for module with no types")
        void shouldReturnEmptyStreamForModuleWithNoTypes() {
            ModuleIndex index = ModuleIndex.builder().addModule(CORE_MODULE).build();

            assertThat(index.typesInModule("banking-core").count()).isZero();
        }

        @Test
        @DisplayName("should return empty stream for unknown module")
        void shouldReturnEmptyStreamForUnknownModule() {
            ModuleIndex index = ModuleIndex.builder().build();

            assertThat(index.typesInModule("unknown").count()).isZero();
        }

        @Test
        @DisplayName("should reject null moduleId")
        void shouldRejectNullModuleId() {
            ModuleIndex index = ModuleIndex.builder().build();

            assertThatNullPointerException()
                    .isThrownBy(() -> index.typesInModule(null))
                    .withMessageContaining("moduleId");
        }
    }

    @Nested
    @DisplayName("modules()")
    class ModulesTests {

        @Test
        @DisplayName("should return all registered modules")
        void shouldReturnAllRegisteredModules() {
            ModuleIndex index = ModuleIndex.builder()
                    .addModule(CORE_MODULE)
                    .addModule(INFRA_MODULE)
                    .addModule(APP_MODULE)
                    .build();

            List<ModuleDescriptor> modules = index.modules().toList();

            assertThat(modules).containsExactlyInAnyOrder(CORE_MODULE, INFRA_MODULE, APP_MODULE);
        }
    }

    @Nested
    @DisplayName("modulesByRole(ModuleRole)")
    class ModulesByRoleTests {

        @Test
        @DisplayName("should return modules matching role")
        void shouldReturnModulesMatchingRole() {
            ModuleIndex index = ModuleIndex.builder()
                    .addModule(CORE_MODULE)
                    .addModule(INFRA_MODULE)
                    .addModule(APP_MODULE)
                    .build();

            List<ModuleDescriptor> domainModules =
                    index.modulesByRole(ModuleRole.DOMAIN).toList();
            List<ModuleDescriptor> infraModules =
                    index.modulesByRole(ModuleRole.INFRASTRUCTURE).toList();

            assertThat(domainModules).containsExactly(CORE_MODULE);
            assertThat(infraModules).containsExactly(INFRA_MODULE);
        }

        @Test
        @DisplayName("should return empty for role with no modules")
        void shouldReturnEmptyForRoleWithNoModules() {
            ModuleIndex index = ModuleIndex.builder().addModule(CORE_MODULE).build();

            assertThat(index.modulesByRole(ModuleRole.API).count()).isZero();
        }

        @Test
        @DisplayName("should reject null role")
        void shouldRejectNullRole() {
            ModuleIndex index = ModuleIndex.builder().build();

            assertThatNullPointerException()
                    .isThrownBy(() -> index.modulesByRole(null))
                    .withMessageContaining("role");
        }
    }

    @Nested
    @DisplayName("size()")
    class SizeTests {

        @Test
        @DisplayName("should return zero for empty index")
        void shouldReturnZeroForEmptyIndex() {
            assertThat(ModuleIndex.builder().build().size()).isZero();
        }

        @Test
        @DisplayName("should return number of registered modules")
        void shouldReturnNumberOfRegisteredModules() {
            ModuleIndex index = ModuleIndex.builder()
                    .addModule(CORE_MODULE)
                    .addModule(INFRA_MODULE)
                    .build();

            assertThat(index.size()).isEqualTo(2);
        }
    }
}
