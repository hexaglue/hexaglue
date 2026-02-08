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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link OverwritePolicy}.
 *
 * @since 5.0.0
 */
@DisplayName("OverwritePolicy")
class OverwritePolicyTest {

    @Test
    @DisplayName("should have three values")
    void shouldHaveThreeValues() {
        assertThat(OverwritePolicy.values())
                .containsExactly(OverwritePolicy.ALWAYS, OverwritePolicy.IF_UNCHANGED, OverwritePolicy.NEVER);
    }

    @Test
    @DisplayName("should parse from string")
    void shouldParseFromString() {
        assertThat(OverwritePolicy.valueOf("ALWAYS")).isEqualTo(OverwritePolicy.ALWAYS);
        assertThat(OverwritePolicy.valueOf("IF_UNCHANGED")).isEqualTo(OverwritePolicy.IF_UNCHANGED);
        assertThat(OverwritePolicy.valueOf("NEVER")).isEqualTo(OverwritePolicy.NEVER);
    }
}
