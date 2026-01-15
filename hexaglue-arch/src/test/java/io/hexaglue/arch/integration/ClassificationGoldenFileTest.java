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

package io.hexaglue.arch.integration;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.arch.ArchElement;
import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.ElementKind;
import io.hexaglue.arch.UnclassifiedType;
import io.hexaglue.arch.builder.ArchitecturalModelBuilder;
import io.hexaglue.syntax.SyntaxProvider;
import io.hexaglue.syntax.spoon.SpoonSyntaxProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Golden file tests for classification regression detection.
 *
 * <p>These tests compare the current classification results against a baseline
 * "golden file" to detect unintended changes in classification behavior.</p>
 *
 * <p>To update the golden file when classification behavior intentionally changes:</p>
 * <pre>
 * mvn test -Dtest=ClassificationGoldenFileTest -Dhexaglue.updateGolden=true
 * </pre>
 */
@DisplayName("Classification Golden Files")
class ClassificationGoldenFileTest {

    private static final String BASE_PACKAGE = "io.hexaglue.arch.integration.fixtures";
    private static final Path GOLDEN_FILE = Path.of("src/test/resources/golden/classification-results.txt");

    private static ArchitecturalModel model;
    private static String actualResults;

    @BeforeAll
    static void setUp() {
        // Parse the fixtures using Spoon
        SyntaxProvider syntaxProvider = SpoonSyntaxProvider.builder()
                .basePackage(BASE_PACKAGE)
                .sourceDirectory(Path.of("src/test/java/io/hexaglue/arch/integration/fixtures"))
                .build();

        // Build the architectural model
        model = ArchitecturalModelBuilder.builder(syntaxProvider)
                .projectName("Golden File Test")
                .basePackage(BASE_PACKAGE)
                .build();

        // Generate the actual results
        actualResults = generateClassificationReport(model);

        // If update flag is set, write the golden file
        if (Boolean.getBoolean("hexaglue.updateGolden")) {
            try {
                Files.createDirectories(GOLDEN_FILE.getParent());
                Files.writeString(GOLDEN_FILE, actualResults);
                System.out.println("Golden file updated: " + GOLDEN_FILE);
            } catch (IOException e) {
                throw new RuntimeException("Failed to update golden file", e);
            }
        }
    }

    @Test
    @DisplayName("should match golden file classifications")
    void shouldMatchGoldenFileClassifications() throws IOException {
        // Skip if golden file doesn't exist (first run)
        if (!Files.exists(GOLDEN_FILE)) {
            System.out.println("Golden file not found. Run with -Dhexaglue.updateGolden=true to create it.");
            System.out.println("Actual results:\n" + actualResults);
            return;
        }

        String expected = Files.readString(GOLDEN_FILE);

        assertThat(actualResults)
                .as("Classification results should match golden file. "
                        + "If changes are intentional, run with -Dhexaglue.updateGolden=true")
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("should have expected number of classified types")
    void shouldHaveExpectedClassifiedTypes() {
        // Domain types: Order, OrderId, CustomerId, Money, Address, OrderPlaced
        // Port types: PlaceOrderUseCase, OrderRepository, PaymentGateway
        long classifiedCount = countClassifiedTypes(model);

        assertThat(classifiedCount)
                .as("Number of classified types")
                .isGreaterThanOrEqualTo(9);
    }

    @Test
    @DisplayName("should have stable classification kinds")
    void shouldHaveStableClassificationKinds() {
        // Verify specific types are classified as expected
        assertTypeClassifiedAs("io.hexaglue.arch.integration.fixtures.domain.Order", ElementKind.AGGREGATE_ROOT);
        assertTypeClassifiedAs("io.hexaglue.arch.integration.fixtures.domain.OrderId", ElementKind.IDENTIFIER);
        assertTypeClassifiedAs("io.hexaglue.arch.integration.fixtures.domain.Money", ElementKind.VALUE_OBJECT);
        assertTypeClassifiedAs("io.hexaglue.arch.integration.fixtures.domain.OrderPlaced", ElementKind.DOMAIN_EVENT);
        assertTypeClassifiedAs("io.hexaglue.arch.integration.fixtures.ports.OrderRepository", ElementKind.DRIVEN_PORT);
    }

    // ===== Helper methods =====

    private static String generateClassificationReport(ArchitecturalModel model) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Classification Results\n");
        sb.append("# Generated by HexaGlue Classification Pipeline\n\n");

        // Aggregate roots
        appendSection(sb, "AGGREGATE_ROOT", model.domainEntities()
                .filter(e -> e.isAggregateRoot())
                .map(e -> e.id().qualifiedName()));

        // Entities
        appendSection(sb, "ENTITY", model.domainEntities()
                .filter(e -> !e.isAggregateRoot())
                .map(e -> e.id().qualifiedName()));

        // Value Objects
        appendSection(sb, "VALUE_OBJECT", model.valueObjects()
                .map(e -> e.id().qualifiedName()));

        // Identifiers
        appendSection(sb, "IDENTIFIER", model.identifiers()
                .map(e -> e.id().qualifiedName()));

        // Domain Events
        appendSection(sb, "DOMAIN_EVENT", model.domainEvents()
                .map(e -> e.id().qualifiedName()));

        // Driving Ports
        appendSection(sb, "DRIVING_PORT", model.drivingPorts()
                .map(e -> e.id().qualifiedName()));

        // Driven Ports
        appendSection(sb, "DRIVEN_PORT", model.drivenPorts()
                .map(e -> e.id().qualifiedName()));

        // Unclassified
        appendSection(sb, "UNCLASSIFIED", model.unclassifiedTypes()
                .map(e -> e.id().qualifiedName()));

        return sb.toString();
    }

    private static void appendSection(StringBuilder sb, String kind, Stream<String> types) {
        List<String> sorted = types.sorted().collect(Collectors.toList());
        sb.append("## ").append(kind).append(" (").append(sorted.size()).append(")\n");
        for (String type : sorted) {
            sb.append("- ").append(type).append("\n");
        }
        sb.append("\n");
    }

    private static long countClassifiedTypes(ArchitecturalModel model) {
        return model.size() - model.unclassifiedCount();
    }

    private void assertTypeClassifiedAs(String qualifiedName, ElementKind expectedKind) {
        // Check in classified types
        var allElements = Stream.of(
                model.domainEntities().map(e -> (ArchElement) e),
                model.valueObjects().map(e -> (ArchElement) e),
                model.identifiers().map(e -> (ArchElement) e),
                model.domainEvents().map(e -> (ArchElement) e),
                model.drivingPorts().map(e -> (ArchElement) e),
                model.drivenPorts().map(e -> (ArchElement) e)
        ).flatMap(s -> s);

        var element = allElements
                .filter(e -> e.id().qualifiedName().equals(qualifiedName))
                .findFirst();

        assertThat(element)
                .as("Type %s should be classified", qualifiedName)
                .isPresent();
        assertThat(element.get().kind())
                .as("Type %s should be classified as %s", qualifiedName, expectedKind)
                .isEqualTo(expectedKind);
    }
}
