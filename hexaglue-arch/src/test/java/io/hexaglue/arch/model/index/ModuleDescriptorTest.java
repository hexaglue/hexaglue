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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ModuleDescriptor}.
 *
 * @since 5.0.0
 */
@DisplayName("ModuleDescriptor")
class ModuleDescriptorTest {

    private static final Path BASE_DIR = Path.of("/projects/banking-core");

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("should create with all fields")
        void shouldCreateWithAllFields() {
            // given
            List<Path> sourceRoots = List.of(BASE_DIR.resolve("src/main/java"));

            // when
            ModuleDescriptor descriptor = new ModuleDescriptor(
                    "banking-core", ModuleRole.DOMAIN, BASE_DIR, sourceRoots, "com.example.banking");

            // then
            assertThat(descriptor.moduleId()).isEqualTo("banking-core");
            assertThat(descriptor.role()).isEqualTo(ModuleRole.DOMAIN);
            assertThat(descriptor.baseDir()).isEqualTo(BASE_DIR);
            assertThat(descriptor.sourceRoots()).containsExactly(BASE_DIR.resolve("src/main/java"));
            assertThat(descriptor.basePackage()).isEqualTo("com.example.banking");
        }

        @Test
        @DisplayName("should allow null basePackage")
        void shouldAllowNullBasePackage() {
            ModuleDescriptor descriptor =
                    new ModuleDescriptor("banking-core", ModuleRole.DOMAIN, BASE_DIR, List.of(), null);

            assertThat(descriptor.basePackage()).isNull();
        }

        @Test
        @DisplayName("should reject null moduleId")
        void shouldRejectNullModuleId() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new ModuleDescriptor(null, ModuleRole.DOMAIN, BASE_DIR, List.of(), null))
                    .withMessageContaining("moduleId");
        }

        @Test
        @DisplayName("should reject blank moduleId")
        void shouldRejectBlankModuleId() {
            assertThatThrownBy(() -> new ModuleDescriptor("  ", ModuleRole.DOMAIN, BASE_DIR, List.of(), null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("moduleId must not be blank");
        }

        @Test
        @DisplayName("should reject null role")
        void shouldRejectNullRole() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new ModuleDescriptor("banking-core", null, BASE_DIR, List.of(), null))
                    .withMessageContaining("role");
        }

        @Test
        @DisplayName("should reject null baseDir")
        void shouldRejectNullBaseDir() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new ModuleDescriptor("banking-core", ModuleRole.DOMAIN, null, List.of(), null))
                    .withMessageContaining("baseDir");
        }

        @Test
        @DisplayName("should reject null sourceRoots")
        void shouldRejectNullSourceRoots() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new ModuleDescriptor("banking-core", ModuleRole.DOMAIN, BASE_DIR, null, null))
                    .withMessageContaining("sourceRoots");
        }

        @Test
        @DisplayName("should make defensive copy of sourceRoots")
        void shouldMakeDefensiveCopyOfSourceRoots() {
            // given
            List<Path> mutableRoots = new ArrayList<>();
            mutableRoots.add(BASE_DIR.resolve("src/main/java"));

            // when
            ModuleDescriptor descriptor =
                    new ModuleDescriptor("banking-core", ModuleRole.DOMAIN, BASE_DIR, mutableRoots, null);
            mutableRoots.add(BASE_DIR.resolve("src/test/java"));

            // then
            assertThat(descriptor.sourceRoots()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("of(String, ModuleRole, Path)")
    class OfFactory {

        @Test
        @DisplayName("should create with default source roots")
        void shouldCreateWithDefaultSourceRoots() {
            // when
            ModuleDescriptor descriptor = ModuleDescriptor.of("banking-core", ModuleRole.DOMAIN, BASE_DIR);

            // then
            assertThat(descriptor.moduleId()).isEqualTo("banking-core");
            assertThat(descriptor.role()).isEqualTo(ModuleRole.DOMAIN);
            assertThat(descriptor.baseDir()).isEqualTo(BASE_DIR);
            assertThat(descriptor.sourceRoots()).containsExactly(BASE_DIR.resolve("src/main/java"));
            assertThat(descriptor.basePackage()).isNull();
        }

        @Test
        @DisplayName("should reject null baseDir")
        void shouldRejectNullBaseDir() {
            assertThatNullPointerException()
                    .isThrownBy(() -> ModuleDescriptor.of("banking-core", ModuleRole.DOMAIN, null))
                    .withMessageContaining("baseDir");
        }
    }
}
