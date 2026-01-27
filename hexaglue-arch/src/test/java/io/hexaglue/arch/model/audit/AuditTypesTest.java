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

package io.hexaglue.arch.model.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for audit model types migrated from spi.audit.
 */
class AuditTypesTest {

    @Nested
    class SeverityTest {
        @Test
        void shouldHaveCorrectOrderFromMostToLeastSevere() {
            assertThat(Severity.values())
                    .containsExactly(Severity.BLOCKER, Severity.CRITICAL, Severity.MAJOR, Severity.MINOR, Severity.INFO);
        }
    }

    @Nested
    class LayerClassificationTest {
        @Test
        void shouldHaveAllLayers() {
            assertThat(LayerClassification.values())
                    .contains(
                            LayerClassification.DOMAIN,
                            LayerClassification.APPLICATION,
                            LayerClassification.INFRASTRUCTURE,
                            LayerClassification.PRESENTATION,
                            LayerClassification.UNKNOWN);
        }
    }

    @Nested
    class QualityLevelTest {
        @Test
        void shouldClassifyNCCDCorrectly() {
            assertThat(QualityLevel.fromNCCD(1.0)).isEqualTo(QualityLevel.EXCELLENT);
            assertThat(QualityLevel.fromNCCD(1.7)).isEqualTo(QualityLevel.GOOD);
            assertThat(QualityLevel.fromNCCD(2.5)).isEqualTo(QualityLevel.ACCEPTABLE);
            assertThat(QualityLevel.fromNCCD(4.0)).isEqualTo(QualityLevel.WARNING);
            assertThat(QualityLevel.fromNCCD(6.0)).isEqualTo(QualityLevel.CRITICAL);
        }

        @Test
        void shouldIndicateAttentionRequired() {
            assertThat(QualityLevel.WARNING.requiresAttention()).isTrue();
            assertThat(QualityLevel.CRITICAL.requiresAttention()).isTrue();
            assertThat(QualityLevel.GOOD.requiresAttention()).isFalse();
        }
    }

    @Nested
    class ZoneClassificationTest {
        @Test
        void shouldClassifyCorrectly() {
            assertThat(ZoneClassification.classify(0.0, 0.5)).isEqualTo(ZoneClassification.IDEAL);
            assertThat(ZoneClassification.classify(0.2, 0.5)).isEqualTo(ZoneClassification.MAIN_SEQUENCE);
            assertThat(ZoneClassification.classify(0.5, 0.3)).isEqualTo(ZoneClassification.ZONE_OF_PAIN);
            assertThat(ZoneClassification.classify(0.5, 0.7)).isEqualTo(ZoneClassification.ZONE_OF_USELESSNESS);
        }
    }

    @Nested
    class SourceLocationTest {
        @Test
        void shouldCreateValidLocation() {
            var loc = new SourceLocation("Test.java", 10, 20, 5, 15);
            assertThat(loc.filePath()).isEqualTo("Test.java");
            assertThat(loc.lineStart()).isEqualTo(10);
            assertThat(loc.lineEnd()).isEqualTo(20);
            assertThat(loc.columnStart()).isEqualTo(5);
            assertThat(loc.columnEnd()).isEqualTo(15);
        }

        @Test
        void shouldCreateSinglePointLocation() {
            var loc = SourceLocation.of("Test.java", 42, 10);
            assertThat(loc.lineStart()).isEqualTo(42);
            assertThat(loc.lineEnd()).isEqualTo(42);
            assertThat(loc.isSinglePoint()).isTrue();
        }

        @Test
        void shouldFormatForIDE() {
            var loc = SourceLocation.of("Order.java", 42, 5);
            assertThat(loc.toIdeFormat()).isEqualTo("Order.java:42:5");
        }

        @Test
        void shouldRejectInvalidLineStart() {
            assertThatThrownBy(() -> new SourceLocation("Test.java", 0, 1, 1, 1))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class CodeMetricsTest {
        @Test
        void shouldDetectComplexCode() {
            var complex = new CodeMetrics(100, 15, 10, 5, 60.0);
            var simple = new CodeMetrics(50, 5, 5, 3, 80.0);

            assertThat(complex.isComplex()).isTrue();
            assertThat(simple.isComplex()).isFalse();
        }

        @Test
        void shouldDetectMaintainability() {
            var maintainable = new CodeMetrics(100, 5, 10, 5, 75.0);
            var notMaintainable = new CodeMetrics(100, 5, 10, 5, 60.0);

            assertThat(maintainable.isMaintainable()).isTrue();
            assertThat(notMaintainable.isMaintainable()).isFalse();
        }
    }

    @Nested
    class CouplingMetricsTest {
        @Test
        void shouldCalculateInstability() {
            var stable = new CouplingMetrics("pkg", 10, 2, 0.3);
            var unstable = new CouplingMetrics("pkg", 2, 10, 0.3);

            assertThat(stable.instability()).isLessThan(0.3);
            assertThat(unstable.instability()).isGreaterThan(0.7);
        }

        @Test
        void shouldDetectZoneOfPain() {
            var painfulPackage = new CouplingMetrics("pkg", 10, 2, 0.1);
            assertThat(painfulPackage.isInZoneOfPain()).isTrue();
        }
    }

    @Nested
    class LakosMetricsTest {
        @Test
        void shouldDetermineQualityLevel() {
            var excellent = new LakosMetrics(10, 15, 1.5, 1.2, 1.0);
            var warning = new LakosMetrics(10, 50, 5.0, 4.0, 3.0);

            assertThat(excellent.qualityLevel()).isEqualTo(QualityLevel.EXCELLENT);
            assertThat(warning.qualityLevel()).isEqualTo(QualityLevel.WARNING);
        }

        @Test
        void shouldCreateEmptyMetrics() {
            var empty = LakosMetrics.empty();
            assertThat(empty.isEmpty()).isTrue();
            assertThat(empty.componentCount()).isZero();
        }
    }

    @Nested
    class DependencyCycleTest {
        @Test
        void shouldCalculateCycleLength() {
            var cycle = new DependencyCycle(CycleKind.TYPE_LEVEL, List.of("A", "B", "C", "A"));
            assertThat(cycle.length()).isEqualTo(3);
            assertThat(cycle.isDirect()).isFalse();
        }

        @Test
        void shouldDetectDirectCycle() {
            var direct = new DependencyCycle(CycleKind.TYPE_LEVEL, List.of("A", "B", "A"));
            assertThat(direct.isDirect()).isTrue();
        }

        @Test
        void shouldFormatPath() {
            var cycle = new DependencyCycle(CycleKind.PACKAGE_LEVEL, List.of("a", "b", "a"));
            assertThat(cycle.toPathString()).isEqualTo("a → b → a");
        }
    }

    @Nested
    class RuleViolationTest {
        @Test
        void shouldCreateWithFactoryMethods() {
            var loc = SourceLocation.of("Test.java", 1, 1);

            var info = RuleViolation.info("rule1", "Info message", loc);
            var warning = RuleViolation.warning("rule2", "Warning message", loc);
            var error = RuleViolation.error("rule3", "Error message", loc);

            assertThat(info.isInfo()).isTrue();
            assertThat(warning.isWarning()).isTrue();
            assertThat(error.isError()).isTrue();
        }
    }

    @Nested
    class CodeUnitTest {
        @Test
        void shouldExtractSimpleName() {
            var unit = new CodeUnit(
                    "com.example.Order",
                    CodeUnitKind.CLASS,
                    LayerClassification.DOMAIN,
                    RoleClassification.AGGREGATE_ROOT,
                    List.of(),
                    List.of(),
                    new CodeMetrics(100, 5, 10, 5, 80.0),
                    new DocumentationInfo(true, 90, List.of()));

            assertThat(unit.simpleName()).isEqualTo("Order");
            assertThat(unit.packageName()).isEqualTo("com.example");
        }
    }

    @Nested
    class CodebaseTest {
        @Test
        void shouldFilterByLayer() {
            var domainUnit = new CodeUnit(
                    "Domain",
                    CodeUnitKind.CLASS,
                    LayerClassification.DOMAIN,
                    RoleClassification.ENTITY,
                    List.of(),
                    List.of(),
                    new CodeMetrics(0, 0, 0, 0, 0),
                    new DocumentationInfo(false, 0, List.of()));
            var infraUnit = new CodeUnit(
                    "Infra",
                    CodeUnitKind.CLASS,
                    LayerClassification.INFRASTRUCTURE,
                    RoleClassification.REPOSITORY,
                    List.of(),
                    List.of(),
                    new CodeMetrics(0, 0, 0, 0, 0),
                    new DocumentationInfo(false, 0, List.of()));

            var codebase = new Codebase("test", "com.example", List.of(domainUnit, infraUnit), Map.of());

            assertThat(codebase.unitsInLayer(LayerClassification.DOMAIN)).hasSize(1);
            assertThat(codebase.unitsInLayer(LayerClassification.INFRASTRUCTURE)).hasSize(1);
        }
    }

    @Nested
    class AuditSnapshotTest {
        @Test
        void shouldDetectPassedAudit() {
            var snapshot = new AuditSnapshot(
                    new Codebase("test", "com.example", List.of(), Map.of()),
                    DetectedArchitectureStyle.HEXAGONAL,
                    List.of(),
                    new QualityMetrics(80, 90, 30, 4.5),
                    new ArchitectureMetrics(10, 0.3, 0.8, 0),
                    new AuditMetadata(Instant.now(), "5.0.0", Duration.ofSeconds(5)));

            assertThat(snapshot.passed()).isTrue();
        }

        @Test
        void shouldDetectFailedAudit() {
            var loc = SourceLocation.of("Test.java", 1, 1);
            var snapshot = new AuditSnapshot(
                    new Codebase("test", "com.example", List.of(), Map.of()),
                    DetectedArchitectureStyle.HEXAGONAL,
                    List.of(RuleViolation.error("rule", "Critical error", loc)),
                    new QualityMetrics(80, 90, 30, 4.5),
                    new ArchitectureMetrics(10, 0.3, 0.8, 0),
                    new AuditMetadata(Instant.now(), "5.0.0", Duration.ofSeconds(5)));

            assertThat(snapshot.passed()).isFalse();
            assertThat(snapshot.errorCount()).isEqualTo(1);
        }
    }

    @Nested
    class BoundedContextInfoTest {
        @Test
        void shouldValidateRequiredFields() {
            assertThatThrownBy(() -> new BoundedContextInfo(null, "pkg", List.of()))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void shouldCheckPackageContainment() {
            var context = new BoundedContextInfo("order", "com.example.order", List.of());
            assertThat(context.containsPackage("com.example.order")).isTrue();
            assertThat(context.containsPackage("com.example.order.domain")).isTrue();
            assertThat(context.containsPackage("com.example.other")).isFalse();
        }
    }

    @Nested
    class AggregateInfoTest {
        @Test
        void shouldCalculateSize() {
            var aggregate = new AggregateInfo("Order", List.of("OrderLine"), List.of("Money", "Address"));
            assertThat(aggregate.size()).isEqualTo(4); // root + 1 entity + 2 VOs
        }

        @Test
        void shouldCheckContainment() {
            var aggregate = new AggregateInfo("Order", List.of("OrderLine"), List.of("Money"));
            assertThat(aggregate.contains("Order")).isTrue();
            assertThat(aggregate.contains("OrderLine")).isTrue();
            assertThat(aggregate.contains("Money")).isTrue();
            assertThat(aggregate.contains("Other")).isFalse();
        }
    }
}
