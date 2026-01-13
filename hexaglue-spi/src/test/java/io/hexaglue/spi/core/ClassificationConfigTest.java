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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ClassificationConfig}.
 */
class ClassificationConfigTest {

    @Nested
    class Defaults {

        @Test
        void defaults_shouldReturnEmptyExclusionsAndExplicit() {
            ClassificationConfig config = ClassificationConfig.defaults();

            assertThat(config.excludePatterns()).isEmpty();
            assertThat(config.explicitClassifications()).isEmpty();
            assertThat(config.hasExclusions()).isFalse();
            assertThat(config.hasExplicitClassifications()).isFalse();
        }

        @Test
        void defaults_shouldHaveDefaultValidationConfig() {
            ClassificationConfig config = ClassificationConfig.defaults();

            assertThat(config.validationConfig().failOnUnclassified()).isFalse();
            assertThat(config.validationConfig().allowInferred()).isTrue();
        }
    }

    @Nested
    class Builder {

        @Test
        void builder_shouldBuildWithExcludePatterns() {
            ClassificationConfig config = ClassificationConfig.builder()
                    .excludePatterns(List.of("*.shared.*", "**.*Exception"))
                    .build();

            assertThat(config.excludePatterns()).containsExactly("*.shared.*", "**.*Exception");
            assertThat(config.hasExclusions()).isTrue();
        }

        @Test
        void builder_shouldBuildWithExplicitClassifications() {
            ClassificationConfig config = ClassificationConfig.builder()
                    .explicitClassifications(Map.of(
                            "com.example.Order", "AGGREGATE_ROOT", "com.example.OrderId", "VALUE_OBJECT"))
                    .build();

            assertThat(config.explicitClassifications()).hasSize(2);
            assertThat(config.hasExplicitClassifications()).isTrue();
        }

        @Test
        void builder_shouldBuildWithValidationConfig() {
            ClassificationConfig config = ClassificationConfig.builder()
                    .validationConfig(new ClassificationConfig.ValidationConfig(true, false))
                    .build();

            assertThat(config.validationConfig().failOnUnclassified()).isTrue();
            assertThat(config.validationConfig().allowInferred()).isFalse();
        }

        @Test
        void builder_failOnUnclassified_shouldSetValidationConfig() {
            ClassificationConfig config =
                    ClassificationConfig.builder().failOnUnclassified().build();

            assertThat(config.validationConfig().failOnUnclassified()).isTrue();
        }
    }

    @Nested
    class ExcludePatternMatching {

        @Test
        void shouldExclude_singleWildcard_matchesSingleSegment() {
            ClassificationConfig config = ClassificationConfig.builder()
                    .excludePatterns(List.of("com.example.*.DomainEvent"))
                    .build();

            assertThat(config.shouldExclude("com.example.order.DomainEvent")).isTrue();
            assertThat(config.shouldExclude("com.example.payment.DomainEvent")).isTrue();
            assertThat(config.shouldExclude("com.example.DomainEvent")).isFalse();
            assertThat(config.shouldExclude("com.example.order.domain.DomainEvent")).isFalse();
        }

        @Test
        void shouldExclude_doubleWildcard_matchesAnyPath() {
            ClassificationConfig config = ClassificationConfig.builder()
                    .excludePatterns(List.of("**.*Exception"))
                    .build();

            assertThat(config.shouldExclude("com.example.OrderException")).isTrue();
            assertThat(config.shouldExclude("com.example.order.domain.InvalidOrderException")).isTrue();
            // Note: "OrderException" without a dot doesn't match "**.*Exception" pattern
            // because the pattern expects at least one dot before "Exception"
            assertThat(config.shouldExclude("OrderException")).isFalse();
            assertThat(config.shouldExclude("com.example.Order")).isFalse();
        }

        @Test
        void shouldExclude_sharedPackagePattern() {
            ClassificationConfig config = ClassificationConfig.builder()
                    .excludePatterns(List.of("*.shared.**"))
                    .build();

            assertThat(config.shouldExclude("com.shared.DomainEvent")).isTrue();
            assertThat(config.shouldExclude("com.shared.events.OrderCreated")).isTrue();
            assertThat(config.shouldExclude("com.example.Order")).isFalse();
        }

        @Test
        void shouldExclude_exactMatch() {
            ClassificationConfig config = ClassificationConfig.builder()
                    .excludePatterns(List.of("com.example.internal.Helper"))
                    .build();

            assertThat(config.shouldExclude("com.example.internal.Helper")).isTrue();
            assertThat(config.shouldExclude("com.example.internal.HelperUtils")).isFalse();
        }

        @Test
        void shouldExclude_multiplePatterns() {
            ClassificationConfig config = ClassificationConfig.builder()
                    .excludePatterns(List.of("**.*Exception", "**.*Event", "*.internal.**"))
                    .build();

            assertThat(config.shouldExclude("com.example.OrderException")).isTrue();
            assertThat(config.shouldExclude("com.example.OrderCreatedEvent")).isTrue();
            assertThat(config.shouldExclude("com.internal.Helper")).isTrue();
            assertThat(config.shouldExclude("com.example.Order")).isFalse();
        }

        @Test
        void shouldExclude_emptyPatterns_neverExcludes() {
            ClassificationConfig config = ClassificationConfig.defaults();

            assertThat(config.shouldExclude("com.example.Order")).isFalse();
            assertThat(config.shouldExclude("anything")).isFalse();
        }
    }

    @Nested
    class ExplicitClassificationLookup {

        @Test
        void getExplicitKind_shouldReturnConfiguredKind() {
            ClassificationConfig config = ClassificationConfig.builder()
                    .explicitClassifications(Map.of("com.example.Order", "AGGREGATE_ROOT"))
                    .build();

            Optional<String> kind = config.getExplicitKind("com.example.Order");

            assertThat(kind).isPresent().contains("AGGREGATE_ROOT");
        }

        @Test
        void getExplicitKind_shouldReturnEmptyForUnknown() {
            ClassificationConfig config = ClassificationConfig.builder()
                    .explicitClassifications(Map.of("com.example.Order", "AGGREGATE_ROOT"))
                    .build();

            Optional<String> kind = config.getExplicitKind("com.example.Product");

            assertThat(kind).isEmpty();
        }

        @Test
        void getExplicitKind_emptyConfig_shouldAlwaysReturnEmpty() {
            ClassificationConfig config = ClassificationConfig.defaults();

            assertThat(config.getExplicitKind("com.example.Order")).isEmpty();
        }
    }

    @Nested
    class ValidationConfig {

        @Test
        void defaults_shouldAllowInferredAndNotFailOnUnclassified() {
            ClassificationConfig.ValidationConfig validationConfig =
                    ClassificationConfig.ValidationConfig.defaults();

            assertThat(validationConfig.failOnUnclassified()).isFalse();
            assertThat(validationConfig.allowInferred()).isTrue();
        }

        @Test
        void strict_shouldFailOnUnclassified() {
            ClassificationConfig.ValidationConfig validationConfig = ClassificationConfig.ValidationConfig.strict();

            assertThat(validationConfig.failOnUnclassified()).isTrue();
            assertThat(validationConfig.allowInferred()).isTrue();
        }
    }

    @Nested
    class Immutability {

        @Test
        void excludePatterns_shouldBeImmutable() {
            ClassificationConfig config = ClassificationConfig.builder()
                    .excludePatterns(List.of("*.shared.*"))
                    .build();

            assertThatThrownBy(() -> config.excludePatterns().add("new.pattern"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void explicitClassifications_shouldBeImmutable() {
            ClassificationConfig config = ClassificationConfig.builder()
                    .explicitClassifications(Map.of("com.example.Order", "ENTITY"))
                    .build();

            assertThatThrownBy(() -> config.explicitClassifications().put("new.Type", "VALUE_OBJECT"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
