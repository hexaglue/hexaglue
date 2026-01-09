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

import io.hexaglue.core.engine.EngineConfig;
import io.hexaglue.core.engine.EngineResult;
import io.hexaglue.core.engine.HexaGlueEngine;
import java.nio.file.Path;
import java.nio.file.Paths;
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
 * End-to-end benchmark for the complete HexaGlue analysis pipeline.
 *
 * <p>Measures the total time for the full pipeline:
 * <ol>
 *   <li>Parsing Java source files (SpoonFrontend)</li>
 *   <li>Building the ApplicationGraph (GraphBuilder)</li>
 *   <li>Computing derived edges (DerivedEdgeComputer)</li>
 *   <li>Classifying types (SinglePassClassifier)</li>
 *   <li>Exporting to IR (IrExporter)</li>
 * </ol>
 *
 * <p>This benchmark simulates real-world usage of the HexaGlueEngine and provides
 * an overall performance metric. For isolated component performance, use the
 * specific benchmarks (ParsingBenchmark, GraphBuildingBenchmark, ClassificationBenchmark).
 *
 * <p>Usage:
 * <pre>{@code
 * mvn clean package -DskipTests
 * java -jar target/benchmarks.jar EndToEndBenchmark
 * }</pre>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(
        value = 2,
        jvmArgs = {"-Xms2G", "-Xmx2G"})
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
public class EndToEndBenchmark {

    private HexaGlueEngine engine;
    private EngineConfig smallConfig;
    private EngineConfig mediumConfig;
    private EngineConfig largeConfig;

    @Setup(Level.Trial)
    public void setup() {
        engine = HexaGlueEngine.create();
        Path benchmarksDir = Paths.get(System.getProperty("user.dir"));

        // Small corpus configuration
        Path smallSourceRoot = benchmarksDir.resolve("test-corpus/small/src/main/java");
        smallConfig = EngineConfig.minimal(smallSourceRoot, "com.example.ecommerce");

        // Medium corpus configuration
        Path mediumSourceRoot = benchmarksDir.resolve("test-corpus/medium/src/main/java");
        mediumConfig = EngineConfig.minimal(mediumSourceRoot, "com.example.ecommerce");

        // Large corpus configuration
        Path largeSourceRoot = benchmarksDir.resolve("test-corpus/large/src/main/java");
        largeConfig = EngineConfig.minimal(largeSourceRoot, "com.example.enterprise");
    }

    @Benchmark
    public void analyzeSmall(Blackhole blackhole) {
        EngineResult result = engine.analyze(smallConfig);
        blackhole.consume(result);
    }

    @Benchmark
    public void analyzeMedium(Blackhole blackhole) {
        EngineResult result = engine.analyze(mediumConfig);
        blackhole.consume(result);
    }

    @Benchmark
    public void analyzeLarge(Blackhole blackhole) {
        EngineResult result = engine.analyze(largeConfig);
        blackhole.consume(result);
    }
}
