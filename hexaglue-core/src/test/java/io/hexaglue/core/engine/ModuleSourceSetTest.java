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

package io.hexaglue.core.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.hexaglue.arch.model.index.ModuleRole;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ModuleSourceSet}.
 *
 * @since 5.0.0
 */
@DisplayName("ModuleSourceSet")
class ModuleSourceSetTest {

    private static final Path BASE_DIR = Path.of("/projects/banking-core");
    private static final Path SOURCE_ROOT = BASE_DIR.resolve("src/main/java");
    private static final Path OUTPUT_DIR = BASE_DIR.resolve("target/generated-sources/hexaglue");

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("should create with all fields")
        void shouldCreateWithAllFields() {
            ModuleSourceSet sourceSet = new ModuleSourceSet(
                    "banking-core",
                    ModuleRole.DOMAIN,
                    List.of(SOURCE_ROOT),
                    List.of(Path.of("/lib/dep.jar")),
                    OUTPUT_DIR,
                    BASE_DIR);

            assertThat(sourceSet.moduleId()).isEqualTo("banking-core");
            assertThat(sourceSet.role()).isEqualTo(ModuleRole.DOMAIN);
            assertThat(sourceSet.sourceRoots()).containsExactly(SOURCE_ROOT);
            assertThat(sourceSet.classpathEntries()).containsExactly(Path.of("/lib/dep.jar"));
            assertThat(sourceSet.outputDirectory()).isEqualTo(OUTPUT_DIR);
            assertThat(sourceSet.baseDir()).isEqualTo(BASE_DIR);
        }

        @Test
        @DisplayName("should reject null moduleId")
        void shouldRejectNullModuleId() {
            assertThatNullPointerException()
                    .isThrownBy(() ->
                            new ModuleSourceSet(null, ModuleRole.DOMAIN, List.of(), List.of(), OUTPUT_DIR, BASE_DIR))
                    .withMessageContaining("moduleId");
        }

        @Test
        @DisplayName("should reject blank moduleId")
        void shouldRejectBlankModuleId() {
            assertThatThrownBy(() ->
                            new ModuleSourceSet("  ", ModuleRole.DOMAIN, List.of(), List.of(), OUTPUT_DIR, BASE_DIR))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("moduleId must not be blank");
        }

        @Test
        @DisplayName("should reject null role")
        void shouldRejectNullRole() {
            assertThatNullPointerException()
                    .isThrownBy(
                            () -> new ModuleSourceSet("banking-core", null, List.of(), List.of(), OUTPUT_DIR, BASE_DIR))
                    .withMessageContaining("role");
        }

        @Test
        @DisplayName("should reject null sourceRoots")
        void shouldRejectNullSourceRoots() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new ModuleSourceSet(
                            "banking-core", ModuleRole.DOMAIN, null, List.of(), OUTPUT_DIR, BASE_DIR))
                    .withMessageContaining("sourceRoots");
        }

        @Test
        @DisplayName("should reject null classpathEntries")
        void shouldRejectNullClasspathEntries() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new ModuleSourceSet(
                            "banking-core", ModuleRole.DOMAIN, List.of(), null, OUTPUT_DIR, BASE_DIR))
                    .withMessageContaining("classpathEntries");
        }

        @Test
        @DisplayName("should reject null outputDirectory")
        void shouldRejectNullOutputDirectory() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new ModuleSourceSet(
                            "banking-core", ModuleRole.DOMAIN, List.of(), List.of(), null, BASE_DIR))
                    .withMessageContaining("outputDirectory");
        }

        @Test
        @DisplayName("should reject null baseDir")
        void shouldRejectNullBaseDir() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new ModuleSourceSet(
                            "banking-core", ModuleRole.DOMAIN, List.of(), List.of(), OUTPUT_DIR, null))
                    .withMessageContaining("baseDir");
        }

        @Test
        @DisplayName("should make defensive copy of sourceRoots")
        void shouldMakeDefensiveCopyOfSourceRoots() {
            List<Path> mutableRoots = new ArrayList<>();
            mutableRoots.add(SOURCE_ROOT);

            ModuleSourceSet sourceSet = new ModuleSourceSet(
                    "banking-core", ModuleRole.DOMAIN, mutableRoots, List.of(), OUTPUT_DIR, BASE_DIR);
            mutableRoots.add(Path.of("/extra"));

            assertThat(sourceSet.sourceRoots()).hasSize(1);
        }

        @Test
        @DisplayName("should make defensive copy of classpathEntries")
        void shouldMakeDefensiveCopyOfClasspathEntries() {
            List<Path> mutableCp = new ArrayList<>();
            mutableCp.add(Path.of("/lib/dep.jar"));

            ModuleSourceSet sourceSet =
                    new ModuleSourceSet("banking-core", ModuleRole.DOMAIN, List.of(), mutableCp, OUTPUT_DIR, BASE_DIR);
            mutableCp.add(Path.of("/lib/extra.jar"));

            assertThat(sourceSet.classpathEntries()).hasSize(1);
        }
    }
}
