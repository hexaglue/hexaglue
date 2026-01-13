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

package io.hexaglue.core.engine;

import static org.assertj.core.api.Assertions.*;

import io.hexaglue.spi.ir.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for the HexaGlueEngine facade.
 */
class HexaGlueEngineTest {

    @TempDir
    Path tempDir;

    private HexaGlueEngine engine;

    @BeforeEach
    void setUp() {
        engine = HexaGlueEngine.create();
    }

    // =========================================================================
    // Basic Analysis Tests
    // =========================================================================

    @Nested
    @DisplayName("Basic Analysis")
    class BasicAnalysisTest {

        @Test
        @DisplayName("should analyze empty project successfully")
        void analyzeEmptyProject() {
            EngineConfig config = EngineConfig.minimal(tempDir, "com.example");

            EngineResult result = engine.analyze(config);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.ir().isEmpty()).isTrue();
            assertThat(result.metrics().totalTypes()).isEqualTo(0);
            assertThat(result.warnings()).hasSize(1);
            assertThat(result.warnings().get(0).code()).isEqualTo("HG-ENGINE-001");
        }

        @Test
        @DisplayName("should analyze simple domain with repository")
        void analyzeSimpleDomain() throws IOException {
            writeSource("com/example/Order.java", """
                    package com.example;
                    public class Order {
                        private String id;
                        private String customerName;
                    }
                    """);
            writeSource("com/example/OrderRepository.java", """
                    package com.example;
                    public interface OrderRepository {
                        Order findById(String id);
                        void save(Order order);
                    }
                    """);

            EngineConfig config = EngineConfig.minimal(tempDir, "com.example");
            EngineResult result = engine.analyze(config);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.errors()).isEmpty();

            // Verify domain types
            assertThat(result.ir().domain().types()).hasSize(1);
            DomainType order = result.ir().domain().types().get(0);
            assertThat(order.qualifiedName()).isEqualTo("com.example.Order");
            assertThat(order.kind()).isEqualTo(DomainKind.AGGREGATE_ROOT);
            assertThat(order.confidence()).isEqualTo(ConfidenceLevel.HIGH);

            // Verify ports
            assertThat(result.ir().ports().ports()).hasSize(1);
            Port repo = result.ir().ports().ports().get(0);
            assertThat(repo.qualifiedName()).isEqualTo("com.example.OrderRepository");
            assertThat(repo.kind()).isEqualTo(PortKind.REPOSITORY);
            assertThat(repo.direction()).isEqualTo(PortDirection.DRIVEN);

            // Verify metrics
            assertThat(result.metrics().totalTypes()).isEqualTo(2);
            assertThat(result.metrics().classifiedTypes()).isEqualTo(2);
            assertThat(result.metrics().portsDetected()).isEqualTo(1);
            assertThat(result.metrics().analysisTime()).isNotNull();
        }
    }

    // =========================================================================
    // Complete Scenario Tests
    // =========================================================================

    @Nested
    @DisplayName("Complete Scenarios")
    class CompleteScenariosTest {

        @Test
        @DisplayName("should analyze hexagonal architecture")
        void analyzeHexagonalArchitecture() throws IOException {
            // Domain - explicit annotations for deterministic classification
            writeSource("com/example/domain/Product.java", """
                    package com.example.domain;
                    import org.jmolecules.ddd.annotation.AggregateRoot;
                    @AggregateRoot
                    public class Product {
                        private String id;
                        private String name;
                    }
                    """);
            writeSource("com/example/domain/ProductId.java", """
                    package com.example.domain;
                    public record ProductId(java.util.UUID value) {}
                    """);

            // Ports - explicit annotations (naming criteria removed)
            writeSource("com/example/port/in/CreateProductUseCase.java", """
                    package com.example.port.in;
                    import com.example.domain.Product;
                    import org.jmolecules.architecture.hexagonal.PrimaryPort;
                    @PrimaryPort
                    public interface CreateProductUseCase {
                        Product newProduct(String name);
                    }
                    """);
            writeSource("com/example/port/out/ProductRepository.java", """
                    package com.example.port.out;
                    import com.example.domain.Product;
                    import org.jmolecules.ddd.annotation.Repository;
                    @Repository
                    public interface ProductRepository {
                        void save(Product product);
                    }
                    """);

            EngineConfig config = EngineConfig.minimal(tempDir, "com.example");
            EngineResult result = engine.analyze(config);

            assertThat(result.isSuccess()).isTrue();

            // Domain types
            assertThat(result.ir().domain().types()).hasSize(2);
            assertThat(result.ir().domain().types())
                    .extracting(DomainType::simpleName)
                    .containsExactlyInAnyOrder("Product", "ProductId");

            // Ports
            assertThat(result.ir().ports().ports()).hasSize(2);

            Port useCase = result.ir().ports().ports().stream()
                    .filter(p -> p.simpleName().equals("CreateProductUseCase"))
                    .findFirst()
                    .orElseThrow();
            assertThat(useCase.kind()).isEqualTo(PortKind.USE_CASE);
            assertThat(useCase.direction()).isEqualTo(PortDirection.DRIVING);

            Port repository = result.ir().ports().ports().stream()
                    .filter(p -> p.simpleName().equals("ProductRepository"))
                    .findFirst()
                    .orElseThrow();
            assertThat(repository.kind()).isEqualTo(PortKind.REPOSITORY);
            assertThat(repository.direction()).isEqualTo(PortDirection.DRIVEN);
        }

        @Test
        @DisplayName("should handle jMolecules annotations")
        void handleJMoleculesAnnotations() throws IOException {
            writeSource("com/example/Customer.java", """
                    package com.example;
                    @org.jmolecules.ddd.annotation.AggregateRoot
                    public class Customer {
                        private String id;
                    }
                    """);

            EngineConfig config = EngineConfig.minimal(tempDir, "com.example");
            EngineResult result = engine.analyze(config);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.ir().domain().types()).hasSize(1);

            DomainType customer = result.ir().domain().types().get(0);
            assertThat(customer.kind()).isEqualTo(DomainKind.AGGREGATE_ROOT);
            assertThat(customer.confidence()).isEqualTo(ConfidenceLevel.EXPLICIT);
        }
    }

    // =========================================================================
    // Metadata Tests
    // =========================================================================

    @Nested
    @DisplayName("Metadata")
    class MetadataTest {

        @Test
        @DisplayName("should include correct metadata")
        void includeCorrectMetadata() throws IOException {
            writeSource("com/example/Order.java", """
                    package com.example;
                    public class Order { private String id; }
                    """);
            writeSource("com/example/OrderRepository.java", """
                    package com.example;
                    public interface OrderRepository { Order findById(String id); }
                    """);

            EngineConfig config = EngineConfig.minimal(tempDir, "com.example");
            EngineResult result = engine.analyze(config);

            IrMetadata metadata = result.ir().metadata();
            assertThat(metadata.basePackage()).isEqualTo("com.example");
            assertThat(metadata.typeCount()).isEqualTo(1);
            assertThat(metadata.portCount()).isEqualTo(1);
            assertThat(metadata.timestamp()).isNotNull();
            assertThat(metadata.engineVersion()).isEqualTo("2.0.0-SNAPSHOT");
        }
    }

    // =========================================================================
    // Error Handling Tests
    // =========================================================================

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTest {

        @Test
        @DisplayName("should reject non-existent source root at config creation")
        void handleNonExistentSourceRoot() {
            Path nonExistent = tempDir.resolve("non-existent");

            // Validation happens at EngineConfig construction time
            assertThatThrownBy(() -> EngineConfig.minimal(nonExistent, "com.example"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Source root does not exist");
        }
    }

    // =========================================================================
    // Helper methods
    // =========================================================================

    private void writeSource(String relativePath, String content) throws IOException {
        Path file = tempDir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }
}
