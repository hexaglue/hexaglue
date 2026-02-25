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

package io.hexaglue.core.frontend;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link TypeNameResolver}.
 *
 * @since 6.0.0
 */
class TypeNameResolverTest {

    @Nested
    @DisplayName("resolve")
    class Resolve {

        @Test
        @DisplayName("should return qualified name unchanged")
        void shouldReturnQualifiedNameUnchanged() {
            TypeNameResolver resolver =
                    new TypeNameResolver("com.example", List.of("com.example.domain.User", "com.example.domain.Order"));

            assertThat(resolver.resolve("com.example.domain.User")).isEqualTo("com.example.domain.User");
        }

        @Test
        @DisplayName("should resolve unique simple name to qualified name")
        void shouldResolveUniqueSimpleName() {
            TypeNameResolver resolver =
                    new TypeNameResolver("com.example", List.of("com.example.domain.User", "com.example.domain.Order"));

            assertThat(resolver.resolve("User")).isEqualTo("com.example.domain.User");
        }

        @Test
        @DisplayName("should prefer candidate in base package when ambiguous")
        void shouldPreferBasePackageCandidate() {
            TypeNameResolver resolver =
                    new TypeNameResolver("com.example", List.of("com.example.domain.User", "org.external.model.User"));

            assertThat(resolver.resolve("User")).isEqualTo("com.example.domain.User");
        }

        @Test
        @DisplayName("should return simple name when unknown")
        void shouldReturnSimpleNameWhenUnknown() {
            TypeNameResolver resolver = new TypeNameResolver("com.example", List.of("com.example.domain.Order"));

            assertThat(resolver.resolve("UnknownType")).isEqualTo("UnknownType");
        }

        @Test
        @DisplayName("should return simple name when ambiguous without base package match")
        void shouldReturnSimpleNameWhenAmbiguousWithoutBasePackage() {
            TypeNameResolver resolver =
                    new TypeNameResolver("com.example", List.of("org.external.model.User", "io.another.User"));

            assertThat(resolver.resolve("User")).isEqualTo("User");
        }

        @Test
        @DisplayName("should handle null input")
        void shouldHandleNullInput() {
            TypeNameResolver resolver = new TypeNameResolver("com.example", List.of());

            assertThat(resolver.resolve(null)).isNull();
        }

        @Test
        @DisplayName("should handle empty input")
        void shouldHandleEmptyInput() {
            TypeNameResolver resolver = new TypeNameResolver("com.example", List.of());

            assertThat(resolver.resolve("")).isEmpty();
        }
    }

    @Nested
    @DisplayName("identity")
    class Identity {

        @Test
        @DisplayName("should return input unchanged")
        void shouldReturnInputUnchanged() {
            TypeNameResolver resolver = TypeNameResolver.identity();

            assertThat(resolver.resolve("User")).isEqualTo("User");
            assertThat(resolver.resolve("com.example.User")).isEqualTo("com.example.User");
        }
    }
}
