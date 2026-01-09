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

import io.hexaglue.core.frontend.JavaFrontend;
import io.hexaglue.core.frontend.JavaFrontend.JavaAnalysisInput;
import io.hexaglue.core.frontend.JavaSemanticModel;
import io.hexaglue.core.frontend.spoon.SpoonFrontend;
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
 * Benchmark for Spoon parsing and semantic model building.
 *
 * <p>Measures the time to parse Java source files and build the JavaSemanticModel.
 * This isolates parsing overhead from graph building and classification logic.
 *
 * <p>Usage:
 * <pre>{@code
 * mvn clean package -DskipTests
 * java -jar target/benchmarks.jar ParsingBenchmark
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
public class ParsingBenchmark {

    private JavaFrontend frontend;
    private JavaAnalysisInput smallInput;
    private JavaAnalysisInput mediumInput;
    private JavaAnalysisInput largeInput;

    @Setup(Level.Trial)
    public void setup() {
        frontend = new SpoonFrontend();

        // Resolve test-corpus paths relative to the module directory
        Path benchmarksDir = Paths.get(System.getProperty("user.dir"));
        Path smallSourceRoot = benchmarksDir.resolve("test-corpus/small/src/main/java");
        Path mediumSourceRoot = benchmarksDir.resolve("test-corpus/medium/src/main/java");
        Path largeSourceRoot = benchmarksDir.resolve("test-corpus/large/src/main/java");

        // Create analysis inputs for each corpus
        smallInput = new JavaAnalysisInput(
                List.of(smallSourceRoot),
                List.of(), // No classpath needed for this simple corpus
                17, // Java 17
                "com.example.ecommerce");

        mediumInput = new JavaAnalysisInput(List.of(mediumSourceRoot), List.of(), 17, "com.example.ecommerce");

        largeInput = new JavaAnalysisInput(List.of(largeSourceRoot), List.of(), 17, "com.example.enterprise");
    }

    @Benchmark
    public void parseSmall(Blackhole blackhole) {
        JavaSemanticModel model = frontend.build(smallInput);
        blackhole.consume(model);
    }

    @Benchmark
    public void parseMedium(Blackhole blackhole) {
        JavaSemanticModel model = frontend.build(mediumInput);
        blackhole.consume(model);
    }

    @Benchmark
    public void parseLarge(Blackhole blackhole) {
        JavaSemanticModel model = frontend.build(largeInput);
        blackhole.consume(model);
    }
}
