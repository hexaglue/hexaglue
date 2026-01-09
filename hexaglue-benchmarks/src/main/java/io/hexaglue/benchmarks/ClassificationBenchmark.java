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

package io.hexaglue.benchmarks;

import io.hexaglue.core.classification.ClassificationResults;
import io.hexaglue.core.classification.SinglePassClassifier;
import io.hexaglue.core.frontend.JavaFrontend;
import io.hexaglue.core.frontend.JavaFrontend.JavaAnalysisInput;
import io.hexaglue.core.frontend.JavaSemanticModel;
import io.hexaglue.core.frontend.spoon.SpoonFrontend;
import io.hexaglue.core.graph.ApplicationGraph;
import io.hexaglue.core.graph.builder.GraphBuilder;
import io.hexaglue.core.graph.model.GraphMetadata;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Benchmark for classification engine performance.
 *
 * <p>Measures the time to classify all types in a pre-built ApplicationGraph.
 * This isolates classification logic from parsing and graph building overhead.
 *
 * <p>The graphs are pre-built in the setup phase to isolate classification performance.
 *
 * <p>Usage:
 * <pre>{@code
 * mvn clean package -DskipTests
 * java -jar target/benchmarks.jar ClassificationBenchmark
 * }</pre>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(
        value = 2,
        jvmArgs = {"-Xms2G", "-Xmx2G"})
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class ClassificationBenchmark {

    private ApplicationGraph smallGraph;
    private ApplicationGraph mediumGraph;
    private ApplicationGraph largeGraph;
    private SinglePassClassifier classifier;

    @Setup(Level.Trial)
    public void setup() {
        // Create classifier with default profile
        classifier = new SinglePassClassifier();

        // Build graphs once during setup
        JavaFrontend frontend = new SpoonFrontend();
        GraphBuilder graphBuilder = new GraphBuilder(true);
        Path benchmarksDir = Paths.get(System.getProperty("user.dir"));

        // Load and build small corpus graph
        Path smallSourceRoot = benchmarksDir.resolve("test-corpus/small/src/main/java");
        JavaAnalysisInput smallInput =
                new JavaAnalysisInput(List.of(smallSourceRoot), List.of(), 17, "com.example.ecommerce");
        JavaSemanticModel smallModel = frontend.build(smallInput);
        GraphMetadata smallMetadata =
                GraphMetadata.of("com.example.ecommerce", 17, smallModel.types().size());
        smallGraph = graphBuilder.build(smallModel, smallMetadata);

        // Load and build medium corpus graph
        Path mediumSourceRoot = benchmarksDir.resolve("test-corpus/medium/src/main/java");
        JavaAnalysisInput mediumInput =
                new JavaAnalysisInput(List.of(mediumSourceRoot), List.of(), 17, "com.example.ecommerce");
        JavaSemanticModel mediumModel = frontend.build(mediumInput);
        GraphMetadata mediumMetadata = GraphMetadata.of(
                "com.example.ecommerce", 17, mediumModel.types().size());
        mediumGraph = graphBuilder.build(mediumModel, mediumMetadata);

        // Load and build large corpus graph
        Path largeSourceRoot = benchmarksDir.resolve("test-corpus/large/src/main/java");
        JavaAnalysisInput largeInput =
                new JavaAnalysisInput(List.of(largeSourceRoot), List.of(), 17, "com.example.enterprise");
        JavaSemanticModel largeModel = frontend.build(largeInput);
        GraphMetadata largeMetadata = GraphMetadata.of(
                "com.example.enterprise", 17, largeModel.types().size());
        largeGraph = graphBuilder.build(largeModel, largeMetadata);
    }

    @Benchmark
    public void classifySmall(Blackhole blackhole) {
        ClassificationResults results = classifier.classify(smallGraph);
        blackhole.consume(results);
    }

    @Benchmark
    public void classifyMedium(Blackhole blackhole) {
        ClassificationResults results = classifier.classify(mediumGraph);
        blackhole.consume(results);
    }

    @Benchmark
    public void classifyLarge(Blackhole blackhole) {
        ClassificationResults results = classifier.classify(largeGraph);
        blackhole.consume(results);
    }
}
