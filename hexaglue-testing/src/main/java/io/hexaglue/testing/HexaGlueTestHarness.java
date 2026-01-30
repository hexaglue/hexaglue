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

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.ConfidenceLevel;
import io.hexaglue.arch.ElementKind;
import io.hexaglue.arch.model.ArchKind;
import io.hexaglue.arch.model.ArchType;
import io.hexaglue.arch.model.DrivenPort;
import io.hexaglue.arch.model.DrivingPort;
import io.hexaglue.arch.model.TypeId;
import io.hexaglue.core.engine.EngineConfig;
import io.hexaglue.core.engine.EngineResult;
import io.hexaglue.core.engine.HexaGlueEngine;
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
 *     .assertDomainType("com.example.Order", ElementKind.AGGREGATE_ROOT)
 *     .assertPort("com.example.Orders", ElementKind.DRIVEN_PORT);
 * }</pre>
 *
 * @since 4.0.0 Updated to use ArchitecturalModel instead of IrSnapshot
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
     * Returns the architectural model.
     *
     * @return the model from the analysis result
     * @since 4.0.0
     */
    public ArchitecturalModel model() {
        requireAnalyzed();
        return result.model();
    }

    /**
     * Asserts that a domain type exists with the given kind.
     *
     * @param qualifiedName the fully qualified class name
     * @param expectedKind the expected element kind
     * @return this harness for chaining
     * @since 4.0.0 Changed parameter from ElementKind to ElementKind
     * @since 5.0.0 Updated to use typeRegistry() API
     */
    public HexaGlueTestHarness assertDomainType(String qualifiedName, ElementKind expectedKind) {
        requireAnalyzed();
        Optional<ArchType> archType = result.model().typeRegistry().flatMap(reg -> reg.get(TypeId.of(qualifiedName)));
        assertThat(archType).as("Domain type '%s' should exist", qualifiedName).isPresent();
        ArchKind expectedArchKind = ArchKind.fromElementKind(expectedKind);
        assertThat(archType.get().kind())
                .as("Domain type '%s' should be %s", qualifiedName, expectedKind)
                .isEqualTo(expectedArchKind);
        return this;
    }

    /**
     * Asserts that an element has at least the given confidence level.
     *
     * @param qualifiedName the fully qualified class name
     * @param minConfidence the minimum required confidence level
     * @return this harness for chaining
     * @since 4.0.0 Changed parameter from spi.ir.ConfidenceLevel to arch.ConfidenceLevel
     * @since 5.0.0 Updated to use typeRegistry() API
     */
    public HexaGlueTestHarness assertConfidenceAtLeast(String qualifiedName, ConfidenceLevel minConfidence) {
        requireAnalyzed();
        Optional<ArchType> archType = result.model().typeRegistry().flatMap(reg -> reg.get(TypeId.of(qualifiedName)));
        assertThat(archType).isPresent();
        ConfidenceLevel actual = archType.get().classification().confidence();
        // Compare by ordinal (HIGH=0, MEDIUM=1, LOW=2, so lower is better)
        assertThat(actual.ordinal())
                .as("'%s' confidence should be at least %s but was %s", qualifiedName, minConfidence, actual)
                .isLessThanOrEqualTo(minConfidence.ordinal());
        return this;
    }

    /**
     * Asserts that a port exists with the given kind.
     *
     * @param qualifiedName the fully qualified interface name
     * @param expectedKind the expected element kind (DRIVING_PORT or DRIVEN_PORT)
     * @return this harness for chaining
     * @since 4.0.0 Simplified to use ElementKind instead of PortKind and PortDirection
     * @since 5.0.0 Updated to use typeRegistry() API
     */
    public HexaGlueTestHarness assertPort(String qualifiedName, ElementKind expectedKind) {
        requireAnalyzed();
        Optional<ArchType> archType = result.model().typeRegistry().flatMap(reg -> reg.get(TypeId.of(qualifiedName)));
        assertThat(archType).as("Port '%s' should exist", qualifiedName).isPresent();
        ArchKind expectedArchKind = ArchKind.fromElementKind(expectedKind);
        assertThat(archType.get().kind())
                .as("Port '%s' should be %s", qualifiedName, expectedKind)
                .isEqualTo(expectedArchKind);
        return this;
    }

    /**
     * Asserts that a driving port exists.
     *
     * @param qualifiedName the fully qualified interface name
     * @return this harness for chaining
     * @since 4.0.0
     * @since 5.0.0 updated to use portIndex() API
     */
    public HexaGlueTestHarness assertDrivingPort(String qualifiedName) {
        requireAnalyzed();
        Optional<DrivingPort> port = result.model().portIndex().flatMap(pi -> pi.drivingPorts()
                .filter(p -> p.id().qualifiedName().equals(qualifiedName))
                .findFirst());
        assertThat(port).as("Driving port '%s' should exist", qualifiedName).isPresent();
        return this;
    }

    /**
     * Asserts that a driven port exists.
     *
     * @param qualifiedName the fully qualified interface name
     * @return this harness for chaining
     * @since 4.0.0
     * @since 5.0.0 updated to use portIndex() API
     */
    public HexaGlueTestHarness assertDrivenPort(String qualifiedName) {
        requireAnalyzed();
        Optional<DrivenPort> port = result.model().portIndex().flatMap(pi -> pi.drivenPorts()
                .filter(p -> p.id().qualifiedName().equals(qualifiedName))
                .findFirst());
        assertThat(port).as("Driven port '%s' should exist", qualifiedName).isPresent();
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
            Optional<String> packageName = extractPackage(content);
            Path packageDir = tempDir;
            if (packageName.isPresent() && !packageName.get().isEmpty()) {
                packageDir = tempDir.resolve(packageName.get().replace('.', '/'));
            }
            Files.createDirectories(packageDir);

            Path sourceFile = packageDir.resolve(filename);
            Files.writeString(sourceFile, content);
        }
    }

    /**
     * Extracts the package declaration from Java source content.
     *
     * @param content the Java source code
     * @return the package name if present, empty otherwise
     */
    private Optional<String> extractPackage(String content) {
        for (String line : content.split("\n")) {
            line = line.trim();
            if (line.startsWith("package ") && line.endsWith(";")) {
                return Optional.of(line.substring(8, line.length() - 1).trim());
            }
        }
        return Optional.empty();
    }

    private void requireAnalyzed() {
        if (result == null) {
            throw new IllegalStateException("Call analyze() first");
        }
    }
}
