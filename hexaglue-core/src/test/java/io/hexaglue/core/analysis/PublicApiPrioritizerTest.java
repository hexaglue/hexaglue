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

package io.hexaglue.core.analysis;

import static org.assertj.core.api.Assertions.*;

import io.hexaglue.core.analysis.PublicApiPrioritizer.Priority;
import io.hexaglue.core.analysis.PublicApiPrioritizer.PriorityCounts;
import io.hexaglue.core.frontend.JavaModifier;
import io.hexaglue.core.frontend.TypeRef;
import io.hexaglue.core.graph.model.MethodNode;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PublicApiPrioritizer}.
 */
class PublicApiPrioritizerTest {

    @Test
    void priorityOf_shouldReturnCriticalForPublicMethods() {
        MethodNode method = createMethod("publicMethod", Set.of(JavaModifier.PUBLIC));

        assertThat(PublicApiPrioritizer.priorityOf(method)).isEqualTo(Priority.CRITICAL);
    }

    @Test
    void priorityOf_shouldReturnImportantForProtectedMethods() {
        MethodNode method = createMethod("protectedMethod", Set.of(JavaModifier.PROTECTED));

        assertThat(PublicApiPrioritizer.priorityOf(method)).isEqualTo(Priority.IMPORTANT);
    }

    @Test
    void priorityOf_shouldReturnSkipForPrivateMethods() {
        MethodNode method = createMethod("privateMethod", Set.of(JavaModifier.PRIVATE));

        assertThat(PublicApiPrioritizer.priorityOf(method)).isEqualTo(Priority.SKIP);
    }

    @Test
    void priorityOf_shouldReturnOptionalForPackagePrivateMethods() {
        MethodNode method = createMethod("packagePrivateMethod", Set.of());

        assertThat(PublicApiPrioritizer.priorityOf(method)).isEqualTo(Priority.OPTIONAL);
    }

    @Test
    void prioritize_shouldSortMethodsByPriority() {
        List<MethodNode> methods = List.of(
                createMethod("privateMethod", Set.of(JavaModifier.PRIVATE)),
                createMethod("publicMethod", Set.of(JavaModifier.PUBLIC)),
                createMethod("packageMethod", Set.of()),
                createMethod("protectedMethod", Set.of(JavaModifier.PROTECTED)));

        List<MethodNode> prioritized = PublicApiPrioritizer.prioritize(methods);

        assertThat(prioritized)
                .extracting(MethodNode::simpleName)
                .containsExactly("publicMethod", "protectedMethod", "packageMethod");
    }

    @Test
    void prioritize_shouldExcludePrivateMethodsByDefault() {
        List<MethodNode> methods = List.of(
                createMethod("publicMethod", Set.of(JavaModifier.PUBLIC)),
                createMethod("privateMethod1", Set.of(JavaModifier.PRIVATE)),
                createMethod("privateMethod2", Set.of(JavaModifier.PRIVATE)));

        List<MethodNode> prioritized = PublicApiPrioritizer.prioritize(methods);

        assertThat(prioritized).hasSize(1).extracting(MethodNode::simpleName).containsOnly("publicMethod");
    }

    @Test
    void prioritize_shouldRespectBudget() {
        List<MethodNode> methods = List.of(
                createMethod("public1", Set.of(JavaModifier.PUBLIC)),
                createMethod("public2", Set.of(JavaModifier.PUBLIC)),
                createMethod("public3", Set.of(JavaModifier.PUBLIC)),
                createMethod("protected1", Set.of(JavaModifier.PROTECTED)));

        AnalysisBudget budget = new AnalysisBudget(2, 10000, Duration.ofSeconds(30));

        List<MethodNode> prioritized = PublicApiPrioritizer.prioritize(methods, budget);

        assertThat(prioritized).hasSize(2);
    }

    @Test
    void prioritize_shouldIncludePrivateMethodsWithUnlimitedBudget() {
        List<MethodNode> methods = List.of(
                createMethod("publicMethod", Set.of(JavaModifier.PUBLIC)),
                createMethod("privateMethod", Set.of(JavaModifier.PRIVATE)));

        AnalysisBudget budget = AnalysisBudget.unlimited();

        List<MethodNode> prioritized = PublicApiPrioritizer.prioritize(methods, budget);

        assertThat(prioritized)
                .hasSize(2)
                .extracting(MethodNode::simpleName)
                .containsExactlyInAnyOrder("publicMethod", "privateMethod");
    }

    @Test
    void prioritize_shouldAccountForMethodsAlreadyAnalyzed() {
        List<MethodNode> methods = List.of(
                createMethod("public1", Set.of(JavaModifier.PUBLIC)),
                createMethod("public2", Set.of(JavaModifier.PUBLIC)),
                createMethod("public3", Set.of(JavaModifier.PUBLIC)));

        AnalysisBudget budget = new AnalysisBudget(5, 10000, Duration.ofSeconds(30));
        budget.recordMethodsAnalyzed(3); // Already analyzed 3 methods

        List<MethodNode> prioritized = PublicApiPrioritizer.prioritize(methods, budget);

        assertThat(prioritized).hasSize(2); // Only 2 remaining from budget of 5
    }

    @Test
    void prioritize_shouldReturnEmptyListForEmptyInput() {
        List<MethodNode> methods = List.of();

        List<MethodNode> prioritized = PublicApiPrioritizer.prioritize(methods);

        assertThat(prioritized).isEmpty();
    }

    @Test
    void prioritizeStream_shouldFilterAndSortMethods() {
        List<MethodNode> methods = List.of(
                createMethod("privateMethod", Set.of(JavaModifier.PRIVATE)),
                createMethod("publicMethod", Set.of(JavaModifier.PUBLIC)),
                createMethod("protectedMethod", Set.of(JavaModifier.PROTECTED)));

        List<MethodNode> result = PublicApiPrioritizer.prioritizeStream(methods).toList();

        assertThat(result).extracting(MethodNode::simpleName).containsExactly("publicMethod", "protectedMethod");
    }

    @Test
    void shouldAnalyze_shouldReturnTrueForMethodsAtOrAboveThreshold() {
        MethodNode publicMethod = createMethod("publicMethod", Set.of(JavaModifier.PUBLIC));
        MethodNode protectedMethod = createMethod("protectedMethod", Set.of(JavaModifier.PROTECTED));
        MethodNode packageMethod = createMethod("packageMethod", Set.of());
        MethodNode privateMethod = createMethod("privateMethod", Set.of(JavaModifier.PRIVATE));

        // Threshold: IMPORTANT (protected and above)
        assertThat(PublicApiPrioritizer.shouldAnalyze(publicMethod, Priority.IMPORTANT))
                .isTrue();
        assertThat(PublicApiPrioritizer.shouldAnalyze(protectedMethod, Priority.IMPORTANT))
                .isTrue();
        assertThat(PublicApiPrioritizer.shouldAnalyze(packageMethod, Priority.IMPORTANT))
                .isFalse();
        assertThat(PublicApiPrioritizer.shouldAnalyze(privateMethod, Priority.IMPORTANT))
                .isFalse();

        // Threshold: OPTIONAL (package-private and above)
        assertThat(PublicApiPrioritizer.shouldAnalyze(publicMethod, Priority.OPTIONAL))
                .isTrue();
        assertThat(PublicApiPrioritizer.shouldAnalyze(protectedMethod, Priority.OPTIONAL))
                .isTrue();
        assertThat(PublicApiPrioritizer.shouldAnalyze(packageMethod, Priority.OPTIONAL))
                .isTrue();
        assertThat(PublicApiPrioritizer.shouldAnalyze(privateMethod, Priority.OPTIONAL))
                .isFalse();
    }

    @Test
    void filterByPriority_shouldFilterCorrectly() {
        List<MethodNode> methods = List.of(
                createMethod("publicMethod", Set.of(JavaModifier.PUBLIC)),
                createMethod("protectedMethod", Set.of(JavaModifier.PROTECTED)),
                createMethod("packageMethod", Set.of()),
                createMethod("privateMethod", Set.of(JavaModifier.PRIVATE)));

        List<MethodNode> important = PublicApiPrioritizer.filterByPriority(methods, Priority.IMPORTANT);

        assertThat(important)
                .extracting(MethodNode::simpleName)
                .containsExactlyInAnyOrder("publicMethod", "protectedMethod");

        List<MethodNode> optional = PublicApiPrioritizer.filterByPriority(methods, Priority.OPTIONAL);

        assertThat(optional)
                .extracting(MethodNode::simpleName)
                .containsExactlyInAnyOrder("publicMethod", "protectedMethod", "packageMethod");
    }

    @Test
    void countByPriority_shouldCountCorrectly() {
        List<MethodNode> methods = List.of(
                createMethod("public1", Set.of(JavaModifier.PUBLIC)),
                createMethod("public2", Set.of(JavaModifier.PUBLIC)),
                createMethod("protected1", Set.of(JavaModifier.PROTECTED)),
                createMethod("package1", Set.of()),
                createMethod("private1", Set.of(JavaModifier.PRIVATE)),
                createMethod("private2", Set.of(JavaModifier.PRIVATE)),
                createMethod("private3", Set.of(JavaModifier.PRIVATE)));

        PriorityCounts counts = PublicApiPrioritizer.countByPriority(methods);

        assertThat(counts.critical()).isEqualTo(2);
        assertThat(counts.important()).isEqualTo(1);
        assertThat(counts.optional()).isEqualTo(1);
        assertThat(counts.skip()).isEqualTo(3);
        assertThat(counts.total()).isEqualTo(7);
        assertThat(counts.analyzable()).isEqualTo(4);
    }

    @Test
    void priorityCounts_shouldProvideSummary() {
        PriorityCounts counts = new PriorityCounts(5, 3, 2, 10);

        String summary = counts.summary();

        assertThat(summary)
                .contains("critical=5")
                .contains("important=3")
                .contains("optional=2")
                .contains("skip=10")
                .contains("total=20");
    }

    @Test
    void priority_shouldHaveCorrectLevels() {
        assertThat(Priority.CRITICAL.level()).isEqualTo(1);
        assertThat(Priority.IMPORTANT.level()).isEqualTo(2);
        assertThat(Priority.OPTIONAL.level()).isEqualTo(3);
        assertThat(Priority.SKIP.level()).isEqualTo(4);
    }

    @Test
    void prioritize_shouldHandleAllPublicMethods() {
        List<MethodNode> methods = List.of(
                createMethod("public1", Set.of(JavaModifier.PUBLIC)),
                createMethod("public2", Set.of(JavaModifier.PUBLIC)),
                createMethod("public3", Set.of(JavaModifier.PUBLIC)));

        List<MethodNode> prioritized = PublicApiPrioritizer.prioritize(methods);

        assertThat(prioritized).hasSize(3);
    }

    @Test
    void prioritize_shouldHandleAllPrivateMethods() {
        List<MethodNode> methods = List.of(
                createMethod("private1", Set.of(JavaModifier.PRIVATE)),
                createMethod("private2", Set.of(JavaModifier.PRIVATE)));

        List<MethodNode> prioritized = PublicApiPrioritizer.prioritize(methods);

        assertThat(prioritized).isEmpty();
    }

    @Test
    void prioritize_shouldStopWhenBudgetExhausted() {
        List<MethodNode> methods = List.of(
                createMethod("public1", Set.of(JavaModifier.PUBLIC)),
                createMethod("public2", Set.of(JavaModifier.PUBLIC)),
                createMethod("public3", Set.of(JavaModifier.PUBLIC)),
                createMethod("public4", Set.of(JavaModifier.PUBLIC)),
                createMethod("public5", Set.of(JavaModifier.PUBLIC)));

        AnalysisBudget budget = new AnalysisBudget(3, 10000, Duration.ofSeconds(30));

        List<MethodNode> prioritized = PublicApiPrioritizer.prioritize(methods, budget);

        assertThat(prioritized).hasSize(3);
    }

    // === Helper methods ===

    private MethodNode createMethod(String name, Set<JavaModifier> modifiers) {
        return MethodNode.builder()
                .declaringTypeName("com.example.TestClass")
                .simpleName(name)
                .returnType(TypeRef.of("void"))
                .modifiers(modifiers)
                .build();
    }
}
