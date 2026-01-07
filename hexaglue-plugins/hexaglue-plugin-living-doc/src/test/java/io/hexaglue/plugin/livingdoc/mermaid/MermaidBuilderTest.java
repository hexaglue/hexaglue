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

package io.hexaglue.plugin.livingdoc.mermaid;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MermaidBuilderTest {

    @Test
    void shouldSanitizeSimpleIdentifier() {
        String result = MermaidBuilder.sanitizeId("OrderService");

        assertThat(result).isEqualTo("OrderService");
    }

    @Test
    void shouldSanitizeIdentifierWithHyphen() {
        String result = MermaidBuilder.sanitizeId("Order-Service");

        assertThat(result).isEqualTo("Order_Service");
    }

    @Test
    void shouldSanitizeIdentifierWithDot() {
        String result = MermaidBuilder.sanitizeId("User.Profile");

        assertThat(result).isEqualTo("User_Profile");
    }

    @Test
    void shouldSanitizeIdentifierWithSpaces() {
        String result = MermaidBuilder.sanitizeId("My Class Name");

        assertThat(result).isEqualTo("My_Class_Name");
    }

    @Test
    void shouldSanitizeIdentifierWithSpecialCharacters() {
        String result = MermaidBuilder.sanitizeId("Order@Service#123");

        assertThat(result).isEqualTo("Order_Service_123");
    }

    @Test
    void shouldHandleEmptyString() {
        String result = MermaidBuilder.sanitizeId("");

        assertThat(result).isEqualTo("_");
    }

    @Test
    void shouldHandleNull() {
        String result = MermaidBuilder.sanitizeId(null);

        assertThat(result).isEqualTo("_");
    }

    @Test
    void shouldEscapeLabelWithQuotes() {
        String result = MermaidBuilder.escapeLabel("Order \"Premium\"");

        assertThat(result).isEqualTo("Order \\\"Premium\\\"");
    }

    @Test
    void shouldHandleEmptyLabel() {
        String result = MermaidBuilder.escapeLabel("");

        assertThat(result).isEqualTo("");
    }

    @Test
    void shouldHandleNullLabel() {
        String result = MermaidBuilder.escapeLabel(null);

        assertThat(result).isEqualTo("");
    }

    @Test
    void shouldNotModifyLabelWithoutQuotes() {
        String result = MermaidBuilder.escapeLabel("Simple Label");

        assertThat(result).isEqualTo("Simple Label");
    }
}
