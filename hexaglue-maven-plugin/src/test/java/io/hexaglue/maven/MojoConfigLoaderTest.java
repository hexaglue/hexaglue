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

package io.hexaglue.maven;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.arch.model.index.ModuleRole;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link MojoConfigLoader} module configuration and hierarchical resolution.
 *
 * @since 5.0.0
 */
class MojoConfigLoaderTest {

    @TempDir
    Path tempDir;

    private final Log log = new SystemStreamLog();

    @Nested
    @DisplayName("loadModuleConfigs")
    class LoadModuleConfigs {

        @Test
        @DisplayName("should parse module section with roles")
        void shouldParseModuleSectionWithRoles() throws IOException {
            // Given
            String yaml = """
                    classification:
                      exclude:
                        - "*.shared.*"
                    modules:
                      banking-core:
                        role: DOMAIN
                      banking-persistence:
                        role: INFRASTRUCTURE
                      banking-service:
                        role: APPLICATION
                      banking-api:
                        role: API
                      banking-app:
                        role: ASSEMBLY
                    """;
            Files.writeString(tempDir.resolve("hexaglue.yaml"), yaml);

            // When
            Map<String, ModuleRole> modules = MojoConfigLoader.loadModuleConfigs(tempDir, log);

            // Then
            assertThat(modules).hasSize(5);
            assertThat(modules.get("banking-core")).isEqualTo(ModuleRole.DOMAIN);
            assertThat(modules.get("banking-persistence")).isEqualTo(ModuleRole.INFRASTRUCTURE);
            assertThat(modules.get("banking-service")).isEqualTo(ModuleRole.APPLICATION);
            assertThat(modules.get("banking-api")).isEqualTo(ModuleRole.API);
            assertThat(modules.get("banking-app")).isEqualTo(ModuleRole.ASSEMBLY);
        }

        @Test
        @DisplayName("should return empty map when no modules section")
        void shouldReturnEmptyMapWhenNoModulesSection() throws IOException {
            // Given
            String yaml = """
                    classification:
                      exclude:
                        - "*.shared.*"
                    plugins:
                      jpa:
                        targetModule: persistence
                    """;
            Files.writeString(tempDir.resolve("hexaglue.yaml"), yaml);

            // When
            Map<String, ModuleRole> modules = MojoConfigLoader.loadModuleConfigs(tempDir, log);

            // Then
            assertThat(modules).isEmpty();
        }

        @Test
        @DisplayName("should default to SHARED for invalid role")
        void shouldDefaultToSharedForInvalidRole() throws IOException {
            // Given
            String yaml = """
                    modules:
                      my-module:
                        role: INVALID_ROLE
                      good-module:
                        role: DOMAIN
                    """;
            Files.writeString(tempDir.resolve("hexaglue.yaml"), yaml);

            // When
            Map<String, ModuleRole> modules = MojoConfigLoader.loadModuleConfigs(tempDir, log);

            // Then
            assertThat(modules).hasSize(2);
            assertThat(modules.get("my-module")).isEqualTo(ModuleRole.SHARED);
            assertThat(modules.get("good-module")).isEqualTo(ModuleRole.DOMAIN);
        }

        @Test
        @DisplayName("should return empty map when no config file exists")
        void shouldReturnEmptyMapWhenNoConfigFile() {
            // When
            Map<String, ModuleRole> modules = MojoConfigLoader.loadModuleConfigs(tempDir, log);

            // Then
            assertThat(modules).isEmpty();
        }

        @Test
        @DisplayName("should handle module without role property")
        void shouldHandleModuleWithoutRoleProperty() throws IOException {
            // Given
            String yaml = """
                    modules:
                      some-module:
                        description: "A module without explicit role"
                    """;
            Files.writeString(tempDir.resolve("hexaglue.yaml"), yaml);

            // When
            Map<String, ModuleRole> modules = MojoConfigLoader.loadModuleConfigs(tempDir, log);

            // Then
            assertThat(modules).hasSize(1);
            assertThat(modules.get("some-module")).isEqualTo(ModuleRole.SHARED);
        }

        @Test
        @DisplayName("should handle case-insensitive role values")
        void shouldHandleCaseInsensitiveRoleValues() throws IOException {
            // Given
            String yaml = """
                    modules:
                      mod1:
                        role: domain
                      mod2:
                        role: Infrastructure
                    """;
            Files.writeString(tempDir.resolve("hexaglue.yaml"), yaml);

            // When
            Map<String, ModuleRole> modules = MojoConfigLoader.loadModuleConfigs(tempDir, log);

            // Then
            assertThat(modules.get("mod1")).isEqualTo(ModuleRole.DOMAIN);
            assertThat(modules.get("mod2")).isEqualTo(ModuleRole.INFRASTRUCTURE);
        }
    }

    @Nested
    @DisplayName("resolveConfigPathHierarchical")
    class ResolveConfigPathHierarchical {

        @Test
        @DisplayName("should resolve local config first")
        void shouldResolveLocalConfigFirst() throws IOException {
            // Given
            Path localDir = tempDir.resolve("child");
            Path parentDir = tempDir.resolve("parent");
            Files.createDirectories(localDir);
            Files.createDirectories(parentDir);
            Files.writeString(localDir.resolve("hexaglue.yaml"), "# local");
            Files.writeString(parentDir.resolve("hexaglue.yaml"), "# parent");

            // When
            Path result = MojoConfigLoader.resolveConfigPathHierarchical(localDir, parentDir);

            // Then
            assertThat(result).isEqualTo(localDir.resolve("hexaglue.yaml"));
        }

        @Test
        @DisplayName("should fall back to parent config when local absent")
        void shouldFallBackToParentConfig() throws IOException {
            // Given
            Path localDir = tempDir.resolve("child");
            Path parentDir = tempDir.resolve("parent");
            Files.createDirectories(localDir);
            Files.createDirectories(parentDir);
            Files.writeString(parentDir.resolve("hexaglue.yml"), "# parent");

            // When
            Path result = MojoConfigLoader.resolveConfigPathHierarchical(localDir, parentDir);

            // Then
            assertThat(result).isEqualTo(parentDir.resolve("hexaglue.yml"));
        }

        @Test
        @DisplayName("should return null when neither location has config")
        void shouldReturnNullWhenNeitherHasConfig() throws IOException {
            // Given
            Path localDir = tempDir.resolve("child");
            Path parentDir = tempDir.resolve("parent");
            Files.createDirectories(localDir);
            Files.createDirectories(parentDir);

            // When
            Path result = MojoConfigLoader.resolveConfigPathHierarchical(localDir, parentDir);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should prefer yaml over yml in same directory")
        void shouldPreferYamlOverYml() throws IOException {
            // Given
            Path dir = tempDir.resolve("both");
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("hexaglue.yaml"), "# yaml");
            Files.writeString(dir.resolve("hexaglue.yml"), "# yml");

            // When
            Path result = MojoConfigLoader.resolveConfigPathHierarchical(dir, tempDir);

            // Then
            assertThat(result).isEqualTo(dir.resolve("hexaglue.yaml"));
        }
    }
}
