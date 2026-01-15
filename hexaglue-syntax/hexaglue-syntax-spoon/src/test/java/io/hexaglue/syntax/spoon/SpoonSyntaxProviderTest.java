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

package io.hexaglue.syntax.spoon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.hexaglue.syntax.SyntaxCapabilities;
import io.hexaglue.syntax.SyntaxProvider;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SpoonSyntaxProvider")
class SpoonSyntaxProviderTest {

    private static SyntaxProvider provider;

    @BeforeAll
    static void setUp() {
        provider = SpoonSyntaxProvider.builder()
                .basePackage("io.hexaglue.syntax.spoon.fixtures")
                .sourceDirectory(Path.of("src/test/java/io/hexaglue/syntax/spoon/fixtures"))
                .build();
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTest {

        @Test
        @DisplayName("should throw when no source directory specified")
        void shouldThrowWhenNoSourceDir() {
            // then
            assertThatThrownBy(() -> SpoonSyntaxProvider.builder()
                            .basePackage("com.example")
                            .build())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("source directory");
        }

        @Test
        @DisplayName("should build with valid configuration")
        void shouldBuildWithValidConfig() {
            // when
            SyntaxProvider result = SpoonSyntaxProvider.builder()
                    .basePackage("io.hexaglue.syntax.spoon.fixtures")
                    .sourceDirectory(Path.of("src/test/java/io/hexaglue/syntax/spoon/fixtures"))
                    .javaVersion(17)
                    .noClasspath(true)
                    .build();

            // then
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("Types")
    class TypesTest {

        @Test
        @DisplayName("should return all types in scope")
        void shouldReturnAllTypes() {
            // when
            long count = provider.types().count();

            // then
            assertThat(count).isGreaterThanOrEqualTo(10); // We have at least 10 fixture classes
        }

        @Test
        @DisplayName("should return types with correct qualified names")
        void shouldReturnTypesWithCorrectNames() {
            // then
            assertThat(provider.types())
                    .anyMatch(t -> t.qualifiedName().equals("io.hexaglue.syntax.spoon.fixtures.SimpleClass"));
        }

        @Test
        @DisplayName("should filter types by base package")
        void shouldFilterByBasePackage() {
            // then
            assertThat(provider.types())
                    .allMatch(t -> t.qualifiedName().startsWith("io.hexaglue.syntax.spoon.fixtures"));
        }
    }

    @Nested
    @DisplayName("Type lookup")
    class TypeLookupTest {

        @Test
        @DisplayName("should find type by qualified name")
        void shouldFindTypeByName() {
            // when
            var result = provider.type("io.hexaglue.syntax.spoon.fixtures.SimpleClass");

            // then
            assertThat(result).isPresent();
            assertThat(result.get().simpleName()).isEqualTo("SimpleClass");
        }

        @Test
        @DisplayName("should return empty for unknown type")
        void shouldReturnEmptyForUnknown() {
            // when
            var result = provider.type("com.example.NonExistent");

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should cache type lookups")
        void shouldCacheLookups() {
            // given
            String qualifiedName = "io.hexaglue.syntax.spoon.fixtures.SimpleInterface";

            // when
            var first = provider.type(qualifiedName);
            var second = provider.type(qualifiedName);

            // then
            assertThat(first).isPresent();
            assertThat(second).isPresent();
            assertThat(first.get()).isSameAs(second.get());
        }
    }

    @Nested
    @DisplayName("Metadata")
    class MetadataTest {

        @Test
        @DisplayName("should return metadata with base package")
        void shouldReturnMetadataWithBasePackage() {
            // when
            var metadata = provider.metadata();

            // then
            assertThat(metadata.basePackage()).isEqualTo("io.hexaglue.syntax.spoon.fixtures");
        }

        @Test
        @DisplayName("should return metadata with parser name")
        void shouldReturnMetadataWithParserName() {
            // when
            var metadata = provider.metadata();

            // then
            assertThat(metadata.parserName()).isEqualTo("Spoon");
        }

        @Test
        @DisplayName("should return metadata with type count")
        void shouldReturnMetadataWithTypeCount() {
            // when
            var metadata = provider.metadata();

            // then
            assertThat(metadata.typeCount()).isGreaterThanOrEqualTo(10);
        }

        @Test
        @DisplayName("should return metadata with source paths")
        void shouldReturnMetadataWithSourcePaths() {
            // when
            var metadata = provider.metadata();

            // then
            assertThat(metadata.sourcePaths()).hasSize(1);
        }

        @Test
        @DisplayName("should return metadata with analysis time")
        void shouldReturnMetadataWithAnalysisTime() {
            // when
            var metadata = provider.metadata();

            // then
            assertThat(metadata.analysisTime()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Capabilities")
    class CapabilitiesTest {

        @Test
        @DisplayName("should return Spoon capabilities")
        void shouldReturnSpoonCapabilities() {
            // when
            var capabilities = provider.capabilities();

            // then
            assertThat(capabilities.supports(SyntaxCapabilities.Capability.METHOD_BODY_SOURCE))
                    .isTrue();
            assertThat(capabilities.supports(SyntaxCapabilities.Capability.INVOCATION_GRAPH))
                    .isTrue();
            assertThat(capabilities.supports(SyntaxCapabilities.Capability.TYPE_RESOLUTION_FULL))
                    .isTrue();
            assertThat(capabilities.supports(SyntaxCapabilities.Capability.ANNOTATION_VALUES_FULL))
                    .isTrue();
        }
    }
}
