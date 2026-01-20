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

package io.hexaglue.arch.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DrivenPortType}.
 *
 * @since 4.1.0
 */
@DisplayName("DrivenPortType")
class DrivenPortTypeTest {

    @Nested
    @DisplayName("Enum Values")
    class EnumValues {

        @Test
        @DisplayName("should have all expected values")
        void shouldHaveAllExpectedValues() {
            assertThat(DrivenPortType.values())
                    .containsExactlyInAnyOrder(
                            DrivenPortType.REPOSITORY,
                            DrivenPortType.GATEWAY,
                            DrivenPortType.EVENT_PUBLISHER,
                            DrivenPortType.NOTIFICATION,
                            DrivenPortType.OTHER);
        }

        @Test
        @DisplayName("should be retrievable by name")
        void shouldBeRetrievableByName() {
            assertThat(DrivenPortType.valueOf("REPOSITORY")).isEqualTo(DrivenPortType.REPOSITORY);
            assertThat(DrivenPortType.valueOf("GATEWAY")).isEqualTo(DrivenPortType.GATEWAY);
            assertThat(DrivenPortType.valueOf("EVENT_PUBLISHER")).isEqualTo(DrivenPortType.EVENT_PUBLISHER);
            assertThat(DrivenPortType.valueOf("NOTIFICATION")).isEqualTo(DrivenPortType.NOTIFICATION);
            assertThat(DrivenPortType.valueOf("OTHER")).isEqualTo(DrivenPortType.OTHER);
        }
    }

    @Nested
    @DisplayName("Description")
    class Description {

        @Test
        @DisplayName("REPOSITORY should have meaningful description")
        void repositoryShouldHaveDescription() {
            assertThat(DrivenPortType.REPOSITORY.description()).containsIgnoringCase("persistence");
        }

        @Test
        @DisplayName("GATEWAY should have meaningful description")
        void gatewayShouldHaveDescription() {
            assertThat(DrivenPortType.GATEWAY.description()).containsIgnoringCase("external");
        }

        @Test
        @DisplayName("EVENT_PUBLISHER should have meaningful description")
        void eventPublisherShouldHaveDescription() {
            assertThat(DrivenPortType.EVENT_PUBLISHER.description()).containsIgnoringCase("event");
        }

        @Test
        @DisplayName("NOTIFICATION should have meaningful description")
        void notificationShouldHaveDescription() {
            assertThat(DrivenPortType.NOTIFICATION.description()).containsIgnoringCase("notification");
        }

        @Test
        @DisplayName("OTHER should have meaningful description")
        void otherShouldHaveDescription() {
            assertThat(DrivenPortType.OTHER.description()).isNotBlank();
        }

        @Test
        @DisplayName("all values should have non-blank descriptions")
        void allValuesShouldHaveDescriptions() {
            for (DrivenPortType type : DrivenPortType.values()) {
                assertThat(type.description()).as("Description for %s", type).isNotBlank();
            }
        }
    }
}
