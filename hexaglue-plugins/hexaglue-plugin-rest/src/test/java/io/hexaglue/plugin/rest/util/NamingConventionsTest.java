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

package io.hexaglue.plugin.rest.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link NamingConventions}.
 */
@DisplayName("NamingConventions")
class NamingConventionsTest {

    @Nested
    @DisplayName("toKebabCase")
    class ToKebabCase {

        @Test
        @DisplayName("should convert PascalCase to kebab-case")
        void shouldConvertPascalCase() {
            assertThat(NamingConventions.toKebabCase("AccountUseCases")).isEqualTo("account-use-cases");
        }

        @Test
        @DisplayName("should convert single word")
        void shouldConvertSingleWord() {
            assertThat(NamingConventions.toKebabCase("Account")).isEqualTo("account");
        }

        @Test
        @DisplayName("should handle consecutive uppercase letters")
        void shouldHandleConsecutiveUppercase() {
            assertThat(NamingConventions.toKebabCase("HTTPRequest")).isEqualTo("http-request");
        }
    }

    @Nested
    @DisplayName("pluralize")
    class Pluralize {

        @Test
        @DisplayName("should add s to regular word")
        void shouldAddS() {
            assertThat(NamingConventions.pluralize("account")).isEqualTo("accounts");
        }

        @Test
        @DisplayName("should add es to word ending in s/sh/ch/x/z")
        void shouldAddEs() {
            assertThat(NamingConventions.pluralize("address")).isEqualTo("addresses");
        }

        @Test
        @DisplayName("should change y to ies for consonant+y")
        void shouldChangeYToIes() {
            assertThat(NamingConventions.pluralize("currency")).isEqualTo("currencies");
        }
    }

    @Nested
    @DisplayName("stripSuffix")
    class StripSuffix {

        @Test
        @DisplayName("should strip UseCases suffix")
        void shouldStripUseCases() {
            assertThat(NamingConventions.stripSuffix("AccountUseCases")).isEqualTo("Account");
        }

        @Test
        @DisplayName("should strip Service suffix")
        void shouldStripService() {
            assertThat(NamingConventions.stripSuffix("AccountService")).isEqualTo("Account");
        }
    }

    @Nested
    @DisplayName("capitalize and decapitalize")
    class CapitalizeDecapitalize {

        @Test
        @DisplayName("should capitalize first letter")
        void shouldCapitalize() {
            assertThat(NamingConventions.capitalize("account")).isEqualTo("Account");
        }

        @Test
        @DisplayName("should decapitalize first letter")
        void shouldDecapitalize() {
            assertThat(NamingConventions.decapitalize("Account")).isEqualTo("account");
        }
    }
}
