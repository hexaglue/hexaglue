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

import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link MultiModuleOutputResolver}.
 *
 * @since 5.0.0
 */
@DisplayName("MultiModuleOutputResolver")
class MultiModuleOutputResolverTest {

    @TempDir
    Path tempDir;

    @Nested
    @DisplayName("default paths")
    class DefaultPaths {

        @Test
        @DisplayName("should resolve sources directory for a module")
        void shouldResolveSourcesDirectoryForModule() {
            MultiModuleOutputResolver resolver = new MultiModuleOutputResolver(tempDir);

            Path result = resolver.resolveSourcesDirectory("banking-infrastructure");

            assertThat(result)
                    .isEqualTo(tempDir.resolve("target/generated-sources/hexaglue/modules/banking-infrastructure"));
        }

        @Test
        @DisplayName("should resolve resources directory for a module")
        void shouldResolveResourcesDirectoryForModule() {
            MultiModuleOutputResolver resolver = new MultiModuleOutputResolver(tempDir);

            Path result = resolver.resolveResourcesDirectory("banking-infrastructure");

            assertThat(result)
                    .isEqualTo(tempDir.resolve("target/generated-resources/hexaglue/modules/banking-infrastructure"));
        }

        @Test
        @DisplayName("should resolve shared reports directory")
        void shouldResolveSharedReportsDirectory() {
            MultiModuleOutputResolver resolver = new MultiModuleOutputResolver(tempDir);

            Path result = resolver.resolveReportsDirectory();

            assertThat(result).isEqualTo(tempDir.resolve("target/hexaglue/reports"));
        }

        @Test
        @DisplayName("should resolve default output directory")
        void shouldResolveDefaultOutputDirectory() {
            MultiModuleOutputResolver resolver = new MultiModuleOutputResolver(tempDir);

            Path result = resolver.resolveDefaultOutputDirectory();

            assertThat(result).isEqualTo(tempDir.resolve("target/generated-sources/hexaglue"));
        }

        @Test
        @DisplayName("should return reactor base dir")
        void shouldReturnReactorBaseDir() {
            MultiModuleOutputResolver resolver = new MultiModuleOutputResolver(tempDir);

            assertThat(resolver.reactorBaseDir()).isEqualTo(tempDir);
        }
    }

    @Nested
    @DisplayName("custom paths")
    class CustomPaths {

        @Test
        @DisplayName("should use custom sources base")
        void shouldUseCustomSourcesBase() {
            MultiModuleOutputResolver resolver = new MultiModuleOutputResolver(
                    tempDir, "target/custom-sources", "target/custom-resources", "target/custom-reports");

            Path result = resolver.resolveSourcesDirectory("my-module");

            assertThat(result).isEqualTo(tempDir.resolve("target/custom-sources/modules/my-module"));
        }

        @Test
        @DisplayName("should use custom resources base")
        void shouldUseCustomResourcesBase() {
            MultiModuleOutputResolver resolver = new MultiModuleOutputResolver(
                    tempDir, "target/custom-sources", "target/custom-resources", "target/custom-reports");

            Path result = resolver.resolveResourcesDirectory("my-module");

            assertThat(result).isEqualTo(tempDir.resolve("target/custom-resources/modules/my-module"));
        }

        @Test
        @DisplayName("should use custom reports base")
        void shouldUseCustomReportsBase() {
            MultiModuleOutputResolver resolver = new MultiModuleOutputResolver(
                    tempDir, "target/custom-sources", "target/custom-resources", "target/custom-reports");

            Path result = resolver.resolveReportsDirectory();

            assertThat(result).isEqualTo(tempDir.resolve("target/custom-reports"));
        }
    }

    @Nested
    @DisplayName("validation")
    class Validation {

        @Test
        @DisplayName("should reject null reactorBaseDir")
        void shouldRejectNullReactorBaseDir() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new MultiModuleOutputResolver(null))
                    .withMessageContaining("reactorBaseDir");
        }

        @Test
        @DisplayName("should reject null moduleId in resolveSourcesDirectory")
        void shouldRejectNullModuleIdInSources() {
            MultiModuleOutputResolver resolver = new MultiModuleOutputResolver(tempDir);

            assertThatNullPointerException()
                    .isThrownBy(() -> resolver.resolveSourcesDirectory(null))
                    .withMessageContaining("moduleId");
        }

        @Test
        @DisplayName("should reject null moduleId in resolveResourcesDirectory")
        void shouldRejectNullModuleIdInResources() {
            MultiModuleOutputResolver resolver = new MultiModuleOutputResolver(tempDir);

            assertThatNullPointerException()
                    .isThrownBy(() -> resolver.resolveResourcesDirectory(null))
                    .withMessageContaining("moduleId");
        }
    }
}
