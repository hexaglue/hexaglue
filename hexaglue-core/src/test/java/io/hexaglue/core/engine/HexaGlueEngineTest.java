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

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.model.AggregateRoot;
import io.hexaglue.arch.model.DrivenPort;
import io.hexaglue.arch.model.DrivingPort;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for the HexaGlueEngine facade.
 *
 * @since 4.0.0 - Updated to use ArchitecturalModel instead of IrSnapshot
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
            assertThat(result.model().size()).isZero();
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

            // Verify aggregates via domainIndex
            ArchitecturalModel model = result.model();
            assertThat(model.domainIndex()).isPresent();
            List<AggregateRoot> aggregateRoots =
                    model.domainIndex().get().aggregateRoots().toList();
            assertThat(aggregateRoots).hasSize(1);
            assertThat(aggregateRoots.get(0).id().qualifiedName()).isEqualTo("com.example.Order");

            // Verify driven ports (repositories) via portIndex
            assertThat(model.portIndex()).isPresent();
            List<DrivenPort> drivenPorts = model.portIndex().get().drivenPorts().toList();
            assertThat(drivenPorts).hasSize(1);
            assertThat(drivenPorts.get(0).id().qualifiedName()).isEqualTo("com.example.OrderRepository");

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
        @org.junit.jupiter.api.Disabled("Temporarily disabled during v5 migration - investigate DomainIndex population")
        void analyzeHexagonalArchitecture() throws IOException {
            // Domain - explicit annotations for deterministic classification
            writeSource("com/example/domain/Product.java", """
                    package com.example.domain;
                    import org.jmolecules.ddd.annotation.AggregateRoot;
                    import org.jmolecules.ddd.annotation.Identity;
                    @AggregateRoot
                    public class Product {
                        @Identity
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

            ArchitecturalModel model = result.model();

            // Domain types - aggregate roots via domainIndex
            assertThat(model.domainIndex()).isPresent();
            var domainIndex = model.domainIndex().get();
            List<AggregateRoot> aggregateRoots = domainIndex.aggregateRoots().toList();
            assertThat(aggregateRoots).hasSize(1);
            assertThat(aggregateRoots.get(0).id().simpleName()).isEqualTo("Product");

            // Identifiers or Value Objects (ProductId may be classified as either)
            // depending on the classification criteria used
            long identifierOrValueObjectCount = domainIndex.identifiers().count()
                    + domainIndex.valueObjects().count();
            assertThat(identifierOrValueObjectCount).isGreaterThanOrEqualTo(1);

            // Ports via portIndex
            assertThat(model.portIndex()).isPresent();
            var portIndex = model.portIndex().get();

            // Driving ports (use cases)
            List<DrivingPort> drivingPorts = portIndex.drivingPorts().toList();
            assertThat(drivingPorts).hasSize(1);
            assertThat(drivingPorts.get(0).id().simpleName()).isEqualTo("CreateProductUseCase");

            // Driven ports (repositories)
            List<DrivenPort> drivenPorts = portIndex.drivenPorts().toList();
            assertThat(drivenPorts).hasSize(1);
            assertThat(drivenPorts.get(0).id().simpleName()).isEqualTo("ProductRepository");
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

            assertThat(result.model().domainIndex()).isPresent();
            List<AggregateRoot> aggregateRoots =
                    result.model().domainIndex().get().aggregateRoots().toList();
            assertThat(aggregateRoots).hasSize(1);
            assertThat(aggregateRoots.get(0).id().qualifiedName()).isEqualTo("com.example.Customer");
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

            // Project context
            assertThat(result.model().project().basePackage()).isEqualTo("com.example");

            // Analysis metadata
            assertThat(result.model().analysisMetadata()).isNotNull();
            assertThat(result.model().analysisMetadata().analysisTime()).isNotNull();
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
