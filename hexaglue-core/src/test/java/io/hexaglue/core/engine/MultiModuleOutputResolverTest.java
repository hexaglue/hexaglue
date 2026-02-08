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
    @DisplayName("consistency")
    class Consistency {

        @Test
        @DisplayName("two resolvers with same reactorBaseDir should produce identical paths")
        void twoResolversWithSameBaseDirShouldProduceIdenticalPaths() {
            // Simulates the scenario where Builder and Participant both create a resolver
            MultiModuleOutputResolver resolver1 = new MultiModuleOutputResolver(tempDir);
            MultiModuleOutputResolver resolver2 = new MultiModuleOutputResolver(tempDir);

            assertThat(resolver1.resolveSourcesDirectory("banking-core"))
                    .isEqualTo(resolver2.resolveSourcesDirectory("banking-core"));
            assertThat(resolver1.resolveResourcesDirectory("banking-core"))
                    .isEqualTo(resolver2.resolveResourcesDirectory("banking-core"));
            assertThat(resolver1.resolveReportsDirectory()).isEqualTo(resolver2.resolveReportsDirectory());
            assertThat(resolver1.resolveDefaultOutputDirectory()).isEqualTo(resolver2.resolveDefaultOutputDirectory());
        }

        @Test
        @DisplayName("custom resolver should produce consistent sources/resources/reports for same module")
        void customResolverShouldProduceConsistentPaths() {
            MultiModuleOutputResolver resolver = new MultiModuleOutputResolver(
                    tempDir, "target/custom-src", "target/custom-res", "target/custom-reports");

            Path sources = resolver.resolveSourcesDirectory("infra");
            Path resources = resolver.resolveResourcesDirectory("infra");
            Path reports = resolver.resolveReportsDirectory();

            // Sources and resources should have parallel structure with /modules/infra
            assertThat(sources).isEqualTo(tempDir.resolve("target/custom-src/modules/infra"));
            assertThat(resources).isEqualTo(tempDir.resolve("target/custom-res/modules/infra"));
            // Reports are shared (no module subdirectory)
            assertThat(reports).isEqualTo(tempDir.resolve("target/custom-reports"));
        }

        @Test
        @DisplayName("sources and resources should differ only in base segment")
        void sourcesAndResourcesShouldDifferOnlyInBaseSegment() {
            MultiModuleOutputResolver resolver = new MultiModuleOutputResolver(tempDir);

            Path sources = resolver.resolveSourcesDirectory("my-module");
            Path resources = resolver.resolveResourcesDirectory("my-module");

            // Both should end with /modules/my-module
            assertThat(sources.getFileName()).isEqualTo(resources.getFileName());
            assertThat(sources.getParent().getFileName())
                    .isEqualTo(resources.getParent().getFileName());
            // But the base segment should differ
            assertThat(sources.toString()).contains("generated-sources");
            assertThat(resources.toString()).contains("generated-resources");
        }

        @Test
        @DisplayName("multiple modules should have distinct output directories")
        void multipleModulesShouldHaveDistinctOutputDirectories() {
            MultiModuleOutputResolver resolver = new MultiModuleOutputResolver(tempDir);

            Path coreOutput = resolver.resolveSourcesDirectory("banking-core");
            Path infraOutput = resolver.resolveSourcesDirectory("banking-infrastructure");
            Path apiOutput = resolver.resolveSourcesDirectory("banking-api");

            assertThat(coreOutput).isNotEqualTo(infraOutput);
            assertThat(infraOutput).isNotEqualTo(apiOutput);
            assertThat(coreOutput).isNotEqualTo(apiOutput);

            // All should share the same prefix
            Path commonPrefix = tempDir.resolve("target/generated-sources/hexaglue/modules");
            assertThat(coreOutput.startsWith(commonPrefix)).isTrue();
            assertThat(infraOutput.startsWith(commonPrefix)).isTrue();
            assertThat(apiOutput.startsWith(commonPrefix)).isTrue();
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
