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

package io.hexaglue.spi.core;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ResolutionConfig}.
 */
class ResolutionConfigTest {

    @Test
    void defaults_shouldReturnExpectedConfiguration() {
        ResolutionConfig config = ResolutionConfig.defaults();

        assertThat(config.maxDepthForMemberTypes()).isEqualTo(1);
        assertThat(config.maxDepthForSupertypes()).isEqualTo(-1);
        assertThat(config.maxDepthForAnnotations()).isEqualTo(-1);
        assertThat(config.maxDepthForGenericTypes()).isEqualTo(1);
        assertThat(config.maxDepthForMethodBodies()).isEqualTo(1);
    }

    @Test
    void fast_shouldReturnOptimizedConfiguration() {
        ResolutionConfig config = ResolutionConfig.fast();

        assertThat(config.maxDepthForMemberTypes()).isEqualTo(1);
        assertThat(config.maxDepthForSupertypes()).isEqualTo(2);
        assertThat(config.maxDepthForAnnotations()).isEqualTo(1);
        assertThat(config.maxDepthForGenericTypes()).isEqualTo(1);
        assertThat(config.maxDepthForMethodBodies()).isEqualTo(0);
    }

    @Test
    void complete_shouldReturnUnlimitedConfiguration() {
        ResolutionConfig config = ResolutionConfig.complete();

        assertThat(config.maxDepthForMemberTypes()).isEqualTo(-1);
        assertThat(config.maxDepthForSupertypes()).isEqualTo(-1);
        assertThat(config.maxDepthForAnnotations()).isEqualTo(-1);
        assertThat(config.maxDepthForGenericTypes()).isEqualTo(-1);
        assertThat(config.maxDepthForMethodBodies()).isEqualTo(-1);
    }

    @Test
    void builder_shouldCreateCustomConfiguration() {
        ResolutionConfig config = ResolutionConfig.builder()
                .maxDepthForMemberTypes(2)
                .maxDepthForSupertypes(3)
                .maxDepthForAnnotations(1)
                .maxDepthForGenericTypes(2)
                .maxDepthForMethodBodies(0)
                .build();

        assertThat(config.maxDepthForMemberTypes()).isEqualTo(2);
        assertThat(config.maxDepthForSupertypes()).isEqualTo(3);
        assertThat(config.maxDepthForAnnotations()).isEqualTo(1);
        assertThat(config.maxDepthForGenericTypes()).isEqualTo(2);
        assertThat(config.maxDepthForMethodBodies()).isEqualTo(0);
    }

    @Test
    void builder_shouldUseDefaultValues() {
        ResolutionConfig config = ResolutionConfig.builder().build();

        ResolutionConfig defaults = ResolutionConfig.defaults();
        assertThat(config).isEqualTo(defaults);
    }

    @Test
    void constructor_shouldRejectNegativeDepthsLessThanMinusOne() {
        assertThatThrownBy(() -> new ResolutionConfig(-2, -1, -1, -1, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxDepthForMemberTypes")
                .hasMessageContaining("must be >= -1");

        assertThatThrownBy(() -> new ResolutionConfig(1, -5, -1, -1, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxDepthForSupertypes");

        assertThatThrownBy(() -> new ResolutionConfig(1, -1, -3, -1, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxDepthForAnnotations");

        assertThatThrownBy(() -> new ResolutionConfig(1, -1, -1, -2, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxDepthForGenericTypes");

        assertThatThrownBy(() -> new ResolutionConfig(1, -1, -1, -1, -10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxDepthForMethodBodies");
    }

    @Test
    void builder_shouldRejectInvalidDepths() {
        assertThatThrownBy(() -> ResolutionConfig.builder().maxDepthForMemberTypes(-2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxDepthForMemberTypes");

        assertThatThrownBy(() -> ResolutionConfig.builder().maxDepthForSupertypes(-5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxDepthForSupertypes");

        assertThatThrownBy(() -> ResolutionConfig.builder().maxDepthForAnnotations(-3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxDepthForAnnotations");

        assertThatThrownBy(() -> ResolutionConfig.builder().maxDepthForGenericTypes(-2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxDepthForGenericTypes");

        assertThatThrownBy(() -> ResolutionConfig.builder().maxDepthForMethodBodies(-10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxDepthForMethodBodies");
    }

    @Test
    void resolveMemberTypes_shouldReturnTrueWhenEnabled() {
        assertThat(ResolutionConfig.defaults().resolveMemberTypes()).isTrue();
        assertThat(ResolutionConfig.fast().resolveMemberTypes()).isTrue();
        assertThat(ResolutionConfig.complete().resolveMemberTypes()).isTrue();

        assertThat(ResolutionConfig.builder().maxDepthForMemberTypes(0).build().resolveMemberTypes())
                .isFalse();
    }

    @Test
    void resolveSupertypes_shouldReturnTrueWhenEnabled() {
        assertThat(ResolutionConfig.defaults().resolveSupertypes()).isTrue();
        assertThat(ResolutionConfig.fast().resolveSupertypes()).isTrue();
        assertThat(ResolutionConfig.complete().resolveSupertypes()).isTrue();

        assertThat(ResolutionConfig.builder().maxDepthForSupertypes(0).build().resolveSupertypes())
                .isFalse();
    }

    @Test
    void resolveAnnotations_shouldReturnTrueWhenEnabled() {
        assertThat(ResolutionConfig.defaults().resolveAnnotations()).isTrue();
        assertThat(ResolutionConfig.fast().resolveAnnotations()).isTrue();
        assertThat(ResolutionConfig.complete().resolveAnnotations()).isTrue();

        assertThat(ResolutionConfig.builder().maxDepthForAnnotations(0).build().resolveAnnotations())
                .isFalse();
    }

    @Test
    void resolveGenericTypes_shouldReturnTrueWhenEnabled() {
        assertThat(ResolutionConfig.defaults().resolveGenericTypes()).isTrue();
        assertThat(ResolutionConfig.fast().resolveGenericTypes()).isTrue();
        assertThat(ResolutionConfig.complete().resolveGenericTypes()).isTrue();

        assertThat(ResolutionConfig.builder().maxDepthForGenericTypes(0).build().resolveGenericTypes())
                .isFalse();
    }

    @Test
    void resolveMethodBodies_shouldReturnTrueWhenEnabled() {
        assertThat(ResolutionConfig.defaults().resolveMethodBodies()).isTrue();
        assertThat(ResolutionConfig.complete().resolveMethodBodies()).isTrue();

        assertThat(ResolutionConfig.fast().resolveMethodBodies()).isFalse();
        assertThat(ResolutionConfig.builder().maxDepthForMethodBodies(0).build().resolveMethodBodies())
                .isFalse();
    }

    @Test
    void isComplete_shouldReturnTrueOnlyForCompleteConfiguration() {
        assertThat(ResolutionConfig.complete().isComplete()).isTrue();
        assertThat(ResolutionConfig.defaults().isComplete()).isFalse();
        assertThat(ResolutionConfig.fast().isComplete()).isFalse();
    }

    @Test
    void isFast_shouldReturnTrueOnlyForFastConfiguration() {
        assertThat(ResolutionConfig.fast().isFast()).isTrue();
        assertThat(ResolutionConfig.defaults().isFast()).isFalse();
        assertThat(ResolutionConfig.complete().isFast()).isFalse();
    }

    @Test
    void equality_shouldWorkCorrectly() {
        ResolutionConfig config1 = ResolutionConfig.builder()
                .maxDepthForMemberTypes(2)
                .maxDepthForSupertypes(3)
                .build();

        ResolutionConfig config2 = ResolutionConfig.builder()
                .maxDepthForMemberTypes(2)
                .maxDepthForSupertypes(3)
                .build();

        ResolutionConfig config3 = ResolutionConfig.builder()
                .maxDepthForMemberTypes(2)
                .maxDepthForSupertypes(4)
                .build();

        assertThat(config1).isEqualTo(config2);
        assertThat(config1).isNotEqualTo(config3);
        assertThat(config1.hashCode()).isEqualTo(config2.hashCode());
    }

    @Test
    void zeroDepth_shouldDisableResolution() {
        ResolutionConfig config = ResolutionConfig.builder()
                .maxDepthForMemberTypes(0)
                .maxDepthForSupertypes(0)
                .maxDepthForAnnotations(0)
                .maxDepthForGenericTypes(0)
                .maxDepthForMethodBodies(0)
                .build();

        assertThat(config.resolveMemberTypes()).isFalse();
        assertThat(config.resolveSupertypes()).isFalse();
        assertThat(config.resolveAnnotations()).isFalse();
        assertThat(config.resolveGenericTypes()).isFalse();
        assertThat(config.resolveMethodBodies()).isFalse();
    }

    @Test
    void unlimitedDepth_shouldEnableFullResolution() {
        ResolutionConfig config = ResolutionConfig.builder()
                .maxDepthForMemberTypes(-1)
                .maxDepthForSupertypes(-1)
                .maxDepthForAnnotations(-1)
                .maxDepthForGenericTypes(-1)
                .maxDepthForMethodBodies(-1)
                .build();

        assertThat(config.resolveMemberTypes()).isTrue();
        assertThat(config.resolveSupertypes()).isTrue();
        assertThat(config.resolveAnnotations()).isTrue();
        assertThat(config.resolveGenericTypes()).isTrue();
        assertThat(config.resolveMethodBodies()).isTrue();
        assertThat(config.isComplete()).isTrue();
    }
}
