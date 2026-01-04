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

package io.hexaglue.testing;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.core.engine.EngineConfig;
import io.hexaglue.core.engine.EngineResult;
import io.hexaglue.core.engine.HexaGlueEngine;
import io.hexaglue.spi.ir.ConfidenceLevel;
import io.hexaglue.spi.ir.DomainKind;
import io.hexaglue.spi.ir.DomainType;
import io.hexaglue.spi.ir.IrSnapshot;
import io.hexaglue.spi.ir.Port;
import io.hexaglue.spi.ir.PortDirection;
import io.hexaglue.spi.ir.PortKind;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Fluent test harness for HexaGlue analysis.
 *
 * <p>Usage:
 * <pre>{@code
 * HexaGlueTestHarness.create()
 *     .withSource("Order.java", """
 *         package com.example;
 *         public class Order {
 *             private UUID id;
 *         }
 *         """)
 *     .withSource("Orders.java", """
 *         package com.example;
 *         public interface Orders {
 *             Order save(Order order);
 *         }
 *         """)
 *     .analyze()
 *     .assertDomainType("com.example.Order", DomainKind.AGGREGATE_ROOT)
 *     .assertPort("com.example.Orders", PortKind.REPOSITORY, PortDirection.DRIVEN);
 * }</pre>
 */
public final class HexaGlueTestHarness {

    private final Map<String, String> sources = new HashMap<>();
    private String basePackage = "com.example";
    private Path tempDir;
    private EngineResult result;

    private HexaGlueTestHarness() {}

    public static HexaGlueTestHarness create() {
        return new HexaGlueTestHarness();
    }

    /**
     * Adds a source file to analyze.
     *
     * @param filename the filename (e.g., "Order.java")
     * @param content the Java source code
     */
    public HexaGlueTestHarness withSource(String filename, String content) {
        sources.put(filename, content);
        return this;
    }

    /**
     * Sets the base package to analyze.
     */
    public HexaGlueTestHarness withBasePackage(String basePackage) {
        this.basePackage = basePackage;
        return this;
    }

    /**
     * Runs the analysis.
     */
    public HexaGlueTestHarness analyze() {
        try {
            tempDir = Files.createTempDirectory("hexaglue-test-");
            writeSources();

            EngineConfig config = EngineConfig.minimal(tempDir, basePackage);

            HexaGlueEngine engine = HexaGlueEngine.create();
            result = engine.analyze(config);

            return this;
        } catch (IOException e) {
            throw new RuntimeException("Failed to setup test", e);
        }
    }

    /**
     * Returns the analysis result.
     */
    public EngineResult result() {
        requireAnalyzed();
        return result;
    }

    /**
     * Returns the IR snapshot.
     */
    public IrSnapshot ir() {
        requireAnalyzed();
        return result.ir();
    }

    /**
     * Asserts that a domain type exists with the given kind.
     */
    public HexaGlueTestHarness assertDomainType(String qualifiedName, DomainKind expectedKind) {
        requireAnalyzed();
        Optional<DomainType> type = result.ir().domain().findByQualifiedName(qualifiedName);
        assertThat(type).as("Domain type '%s' should exist", qualifiedName).isPresent();
        assertThat(type.get().kind())
                .as("Domain type '%s' should be %s", qualifiedName, expectedKind)
                .isEqualTo(expectedKind);
        return this;
    }

    /**
     * Asserts that a domain type has at least the given confidence level.
     */
    public HexaGlueTestHarness assertConfidenceAtLeast(String qualifiedName, ConfidenceLevel minConfidence) {
        requireAnalyzed();
        Optional<DomainType> type = result.ir().domain().findByQualifiedName(qualifiedName);
        assertThat(type).isPresent();
        assertThat(type.get().confidence().isAtLeast(minConfidence))
                .as(
                        "'%s' confidence should be at least %s but was %s",
                        qualifiedName, minConfidence, type.get().confidence())
                .isTrue();
        return this;
    }

    /**
     * Asserts that a port exists with the given kind and direction.
     */
    public HexaGlueTestHarness assertPort(
            String qualifiedName, PortKind expectedKind, PortDirection expectedDirection) {
        requireAnalyzed();
        Optional<Port> port = result.ir().ports().findByQualifiedName(qualifiedName);
        assertThat(port).as("Port '%s' should exist", qualifiedName).isPresent();
        assertThat(port.get().kind())
                .as("Port '%s' should be %s", qualifiedName, expectedKind)
                .isEqualTo(expectedKind);
        assertThat(port.get().direction())
                .as("Port '%s' should be %s", qualifiedName, expectedDirection)
                .isEqualTo(expectedDirection);
        return this;
    }

    /**
     * Asserts that the analysis succeeded without errors.
     */
    public HexaGlueTestHarness assertSuccess() {
        requireAnalyzed();
        assertThat(result.isSuccess())
                .as("Analysis should succeed without errors")
                .isTrue();
        return this;
    }

    /**
     * Cleans up temporary files.
     */
    public void cleanup() {
        if (tempDir != null) {
            try {
                Files.walk(tempDir).sorted((a, b) -> -a.compareTo(b)).forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        // ignore
                    }
                });
            } catch (IOException e) {
                // ignore
            }
        }
    }

    private void writeSources() throws IOException {
        for (Map.Entry<String, String> entry : sources.entrySet()) {
            String filename = entry.getKey();
            String content = entry.getValue();

            // Extract package from content
            String packageName = extractPackage(content);
            Path packageDir = tempDir;
            if (packageName != null && !packageName.isEmpty()) {
                packageDir = tempDir.resolve(packageName.replace('.', '/'));
            }
            Files.createDirectories(packageDir);

            Path sourceFile = packageDir.resolve(filename);
            Files.writeString(sourceFile, content);
        }
    }

    private String extractPackage(String content) {
        for (String line : content.split("\n")) {
            line = line.trim();
            if (line.startsWith("package ") && line.endsWith(";")) {
                return line.substring(8, line.length() - 1).trim();
            }
        }
        return null;
    }

    private void requireAnalyzed() {
        if (result == null) {
            throw new IllegalStateException("Call analyze() first");
        }
    }
}
