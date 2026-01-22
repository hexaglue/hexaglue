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

package io.hexaglue.arch.ports;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for PortClassification enum.
 *
 * @since 4.0.0
 * @since 5.0.0 Simplified to only test PortClassification (legacy port types removed)
 */
@DisplayName("Port Types")
class PortTypesTest {

    @Nested
    @DisplayName("PortClassification")
    class PortClassificationTest {

        @Test
        @DisplayName("should identify driving classifications")
        void shouldIdentifyDriving() {
            assertThat(PortClassification.USE_CASE.isDriving()).isTrue();
            assertThat(PortClassification.COMMAND_HANDLER.isDriving()).isTrue();
            assertThat(PortClassification.QUERY_HANDLER.isDriving()).isTrue();
            assertThat(PortClassification.REPOSITORY.isDriving()).isFalse();
        }

        @Test
        @DisplayName("should identify driven classifications")
        void shouldIdentifyDriven() {
            assertThat(PortClassification.REPOSITORY.isDriven()).isTrue();
            assertThat(PortClassification.GATEWAY.isDriven()).isTrue();
            assertThat(PortClassification.EVENT_PUBLISHER.isDriven()).isTrue();
            assertThat(PortClassification.USE_CASE.isDriven()).isFalse();
        }
    }
}
