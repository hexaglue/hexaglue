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

import io.hexaglue.core.frontend.CachedSpoonAnalyzer;
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
 * Benchmark for ApplicationGraph construction.
 *
 * <p>Measures the time to build the ApplicationGraph from a parsed semantic model.
 * This isolates graph building from parsing and classification.
 *
 * <p>The semantic models are pre-parsed in the setup phase to isolate graph building performance.
 *
 * <p>Usage:
 * <pre>{@code
 * mvn clean package -DskipTests
 * java -jar target/benchmarks.jar GraphBuildingBenchmark
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
public class GraphBuildingBenchmark {

    private GraphBuilder graphBuilder;
    private JavaSemanticModel smallModel;
    private JavaSemanticModel mediumModel;
    private JavaSemanticModel largeModel;
    private GraphMetadata smallMetadata;
    private GraphMetadata mediumMetadata;
    private GraphMetadata largeMetadata;

    @Setup(Level.Trial)
    public void setup() {
        // Create graph builder with derived edges enabled
        CachedSpoonAnalyzer analyzer = new CachedSpoonAnalyzer();
        graphBuilder = new GraphBuilder(true, analyzer);

        // Parse semantic models once during setup
        JavaFrontend frontend = new SpoonFrontend();
        Path benchmarksDir = Paths.get(System.getProperty("user.dir"));

        // Load small corpus
        Path smallSourceRoot = benchmarksDir.resolve("test-corpus/small/src/main/java");
        JavaAnalysisInput smallInput =
                new JavaAnalysisInput(List.of(smallSourceRoot), List.of(), 17, "com.example.ecommerce");
        smallModel = frontend.build(smallInput);
        smallMetadata =
                GraphMetadata.of("com.example.ecommerce", 17, smallModel.types().size());

        // Load medium corpus
        Path mediumSourceRoot = benchmarksDir.resolve("test-corpus/medium/src/main/java");
        JavaAnalysisInput mediumInput =
                new JavaAnalysisInput(List.of(mediumSourceRoot), List.of(), 17, "com.example.ecommerce");
        mediumModel = frontend.build(mediumInput);
        mediumMetadata = GraphMetadata.of(
                "com.example.ecommerce", 17, mediumModel.types().size());

        // Load large corpus
        Path largeSourceRoot = benchmarksDir.resolve("test-corpus/large/src/main/java");
        JavaAnalysisInput largeInput =
                new JavaAnalysisInput(List.of(largeSourceRoot), List.of(), 17, "com.example.enterprise");
        largeModel = frontend.build(largeInput);
        largeMetadata = GraphMetadata.of(
                "com.example.enterprise", 17, largeModel.types().size());
    }

    @Benchmark
    public void buildGraphSmall(Blackhole blackhole) {
        ApplicationGraph graph = graphBuilder.build(smallModel, smallMetadata);
        blackhole.consume(graph);
    }

    @Benchmark
    public void buildGraphMedium(Blackhole blackhole) {
        ApplicationGraph graph = graphBuilder.build(mediumModel, mediumMetadata);
        blackhole.consume(graph);
    }

    @Benchmark
    public void buildGraphLarge(Blackhole blackhole) {
        ApplicationGraph graph = graphBuilder.build(largeModel, largeMetadata);
        blackhole.consume(graph);
    }
}
