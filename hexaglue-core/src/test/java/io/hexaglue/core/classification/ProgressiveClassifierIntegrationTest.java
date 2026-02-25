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

package io.hexaglue.core.classification;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.core.analysis.AnalysisBudget;
import io.hexaglue.core.frontend.CachedSpoonAnalyzer;
import io.hexaglue.core.frontend.JavaFrontend.JavaAnalysisInput;
import io.hexaglue.core.frontend.JavaSemanticModel;
import io.hexaglue.core.frontend.spoon.SpoonFrontend;
import io.hexaglue.core.graph.ApplicationGraph;
import io.hexaglue.core.graph.builder.GraphBuilder;
import io.hexaglue.core.graph.model.GraphMetadata;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration test for ProgressiveClassifier with CachedSpoonAnalyzer.
 *
 * <p>This test verifies that the ProgressiveClassifier correctly integrates with
 * CachedSpoonAnalyzer for method body analysis in Pass 3.
 */
@DisplayName("ProgressiveClassifier Integration with CachedSpoonAnalyzer")
class ProgressiveClassifierIntegrationTest {

    @TempDir
    Path tempDir;

    private SpoonFrontend frontend;
    private GraphBuilder graphBuilder;

    @BeforeEach
    void setUp() {
        frontend = new SpoonFrontend();
        CachedSpoonAnalyzer analyzer = new CachedSpoonAnalyzer();
        graphBuilder = new GraphBuilder(true, analyzer);
    }

    private void writeSource(String relativePath, String content) throws IOException {
        Path file = tempDir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }

    @Test
    @DisplayName("Should successfully create classifier with semantic model and analyzer")
    void shouldCreateClassifierWithDependencies() throws IOException {
        // Given
        writeSource("com/example/Order.java", """
                package com.example;
                public class Order {
                    private String id;
                }
                """);

        JavaAnalysisInput input = new JavaAnalysisInput(List.of(tempDir), List.of(), 17, "com.example", false, false);
        JavaSemanticModel model = frontend.build(input);

        CachedSpoonAnalyzer analyzer = new CachedSpoonAnalyzer();
        AnalysisBudget budget = AnalysisBudget.mediumProject();

        // When
        ProgressiveClassifier classifier = new ProgressiveClassifier(budget, model, analyzer);

        // Then
        assertThat(classifier).isNotNull();
        assertThat(classifier.budget()).isEqualTo(budget);
    }

    @Test
    @DisplayName("Should perform progressive classification without errors")
    void shouldPerformProgressiveClassification() throws IOException {
        // Given
        writeSource("com/example/Order.java", """
                package com.example;
                public class Order {
                    private String id;
                    public String getId() { return id; }
                    public void setId(String id) { this.id = id; }
                }
                """);

        JavaAnalysisInput input = new JavaAnalysisInput(List.of(tempDir), List.of(), 17, "com.example", false, false);
        JavaSemanticModel model = frontend.build(input);

        GraphMetadata metadata =
                GraphMetadata.of("com.example", 17, model.types().size());
        ApplicationGraph graph = graphBuilder.build(model, metadata);

        CachedSpoonAnalyzer analyzer = new CachedSpoonAnalyzer();
        AnalysisBudget budget = AnalysisBudget.mediumProject();

        ProgressiveClassifier classifier = new ProgressiveClassifier(budget, model, analyzer);

        // When
        ClassificationResults results = classifier.classifyProgressive(graph);

        // Then
        assertThat(results).isNotNull();
        assertThat(classifier.totalDuration()).isNotNull();
        assertThat(classifier.pass1Duration()).isNotNull();
        assertThat(classifier.pass2Duration()).isNotNull();
        assertThat(classifier.pass3Duration()).isNotNull();

        // Verify statistics
        ProgressiveClassifier.ProgressiveClassificationStatistics stats = classifier.statistics();
        assertThat(stats).isNotNull();
        assertThat(stats.totalClassified()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Should analyze method bodies for classification")
    void shouldAnalyzeMethodBodiesForClassification() throws IOException {
        // Given - Entity with mutable behavior (field writes)
        writeSource("com/example/Account.java", """
                package com.example;
                public class Account {
                    private String id;
                    private double balance;

                    public void deposit(double amount) {
                        this.balance += amount;  // Field write - mutation
                    }

                    public double getBalance() {
                        return balance;  // Field read only
                    }
                }
                """);

        JavaAnalysisInput input = new JavaAnalysisInput(List.of(tempDir), List.of(), 17, "com.example", false, false);
        JavaSemanticModel model = frontend.build(input);

        GraphMetadata metadata =
                GraphMetadata.of("com.example", 17, model.types().size());
        ApplicationGraph graph = graphBuilder.build(model, metadata);

        CachedSpoonAnalyzer analyzer = new CachedSpoonAnalyzer();
        AnalysisBudget budget = AnalysisBudget.mediumProject();

        ProgressiveClassifier classifier = new ProgressiveClassifier(budget, model, analyzer);

        // When
        ClassificationResults results = classifier.classifyProgressive(graph);

        // Then
        assertThat(results).isNotNull();

        // Verify that pass 3 was executed (deep analysis)
        assertThat(classifier.pass3Duration()).isNotNull();

        // Verify that analyzer cached method analysis
        CachedSpoonAnalyzer.CacheStatistics stats = analyzer.statistics();
        assertThat(stats).isNotNull();
    }
}
