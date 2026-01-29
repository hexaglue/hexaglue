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

package io.hexaglue.plugin.livingdoc.util;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.arch.model.DrivenPortType;
import io.hexaglue.arch.model.ir.PortKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PortKindMapper}.
 *
 * @since 5.0.0
 */
@DisplayName("PortKindMapper")
class PortKindMapperTest {

    @Test
    @DisplayName("should map REPOSITORY to REPOSITORY")
    void shouldMapRepository() {
        assertThat(PortKindMapper.from(DrivenPortType.REPOSITORY)).isEqualTo(PortKind.REPOSITORY);
    }

    @Test
    @DisplayName("should map GATEWAY to GATEWAY")
    void shouldMapGateway() {
        assertThat(PortKindMapper.from(DrivenPortType.GATEWAY)).isEqualTo(PortKind.GATEWAY);
    }

    @Test
    @DisplayName("should map EVENT_PUBLISHER to EVENT_PUBLISHER")
    void shouldMapEventPublisher() {
        assertThat(PortKindMapper.from(DrivenPortType.EVENT_PUBLISHER)).isEqualTo(PortKind.EVENT_PUBLISHER);
    }

    @Test
    @DisplayName("should map NOTIFICATION to GENERIC")
    void shouldMapNotification() {
        assertThat(PortKindMapper.from(DrivenPortType.NOTIFICATION)).isEqualTo(PortKind.GENERIC);
    }

    @Test
    @DisplayName("should map OTHER to GENERIC")
    void shouldMapOther() {
        assertThat(PortKindMapper.from(DrivenPortType.OTHER)).isEqualTo(PortKind.GENERIC);
    }
}
