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

package io.hexaglue.core.classification.port.criteria;

import static org.assertj.core.api.Assertions.*;

import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.classification.MatchResult;
import io.hexaglue.core.classification.port.PortDirection;
import io.hexaglue.core.classification.port.PortKind;
import io.hexaglue.core.frontend.JavaFrontend.JavaAnalysisInput;
import io.hexaglue.core.frontend.JavaSemanticModel;
import io.hexaglue.core.frontend.spoon.SpoonFrontend;
import io.hexaglue.core.graph.ApplicationGraph;
import io.hexaglue.core.graph.builder.GraphBuilder;
import io.hexaglue.core.graph.model.GraphMetadata;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for port classification criteria.
 */
class PortCriteriaTest {

    @TempDir
    Path tempDir;

    private SpoonFrontend frontend;
    private GraphBuilder builder;

    @BeforeEach
    void setUp() {
        frontend = new SpoonFrontend();
        builder = new GraphBuilder(true);
    }

    // =========================================================================
    // Explicit Annotation Criteria
    // =========================================================================

    @Nested
    class ExplicitRepositoryCriteriaTest {

        @Test
        void shouldMatchInterfaceWithRepositoryAnnotation() throws IOException {
            writeSource("com/example/OrderRepository.java", """
                    package com.example;
                    import org.jmolecules.ddd.annotation.Repository;
                    @Repository
                    public interface OrderRepository {
                        void save(Object order);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode repo = graph.typeNode("com.example.OrderRepository").orElseThrow();
            GraphQuery query = graph.query();

            ExplicitRepositoryCriteria criteria = new ExplicitRepositoryCriteria();
            MatchResult result = criteria.evaluate(repo, query);

            assertThat(result.matched()).isTrue();
            assertThat(result.confidence()).isEqualTo(ConfidenceLevel.EXPLICIT);
            assertThat(result.justification()).contains("@Repository");
            assertThat(criteria.targetKind()).isEqualTo(PortKind.REPOSITORY);
            assertThat(criteria.targetDirection()).isEqualTo(PortDirection.DRIVEN);
            assertThat(criteria.priority()).isEqualTo(100);
        }

        @Test
        void shouldNotMatchInterfaceWithoutAnnotation() throws IOException {
            writeSource("com/example/OrderRepository.java", """
                    package com.example;
                    public interface OrderRepository {
                        void save(Object order);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode repo = graph.typeNode("com.example.OrderRepository").orElseThrow();
            GraphQuery query = graph.query();

            ExplicitRepositoryCriteria criteria = new ExplicitRepositoryCriteria();
            MatchResult result = criteria.evaluate(repo, query);

            assertThat(result.matched()).isFalse();
        }
    }

    @Nested
    class ExplicitPrimaryPortCriteriaTest {

        @Test
        void shouldMatchInterfaceImplementingPrimaryPort() throws IOException {
            writeSource("com/example/OrderingCoffee.java", """
                    package com.example;
                    import org.jmolecules.architecture.hexagonal.PrimaryPort;
                    public interface OrderingCoffee extends PrimaryPort {
                        void placeOrder(Object order);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode useCase = graph.typeNode("com.example.OrderingCoffee").orElseThrow();
            GraphQuery query = graph.query();

            ExplicitPrimaryPortCriteria criteria = new ExplicitPrimaryPortCriteria();
            MatchResult result = criteria.evaluate(useCase, query);

            assertThat(result.matched()).isTrue();
            assertThat(result.confidence()).isEqualTo(ConfidenceLevel.EXPLICIT);
            assertThat(result.justification()).contains("PrimaryPort");
            assertThat(criteria.targetKind()).isEqualTo(PortKind.USE_CASE);
            assertThat(criteria.targetDirection()).isEqualTo(PortDirection.DRIVING);
        }
    }

    @Nested
    class ExplicitSecondaryPortCriteriaTest {

        @Test
        void shouldMatchInterfaceImplementingSecondaryPort() throws IOException {
            writeSource("com/example/PaymentGateway.java", """
                    package com.example;
                    import org.jmolecules.architecture.hexagonal.SecondaryPort;
                    public interface PaymentGateway extends SecondaryPort {
                        void processPayment(Object payment);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode gateway = graph.typeNode("com.example.PaymentGateway").orElseThrow();
            GraphQuery query = graph.query();

            ExplicitSecondaryPortCriteria criteria = new ExplicitSecondaryPortCriteria();
            MatchResult result = criteria.evaluate(gateway, query);

            assertThat(result.matched()).isTrue();
            assertThat(result.confidence()).isEqualTo(ConfidenceLevel.EXPLICIT);
            assertThat(result.justification()).contains("SecondaryPort");
            assertThat(criteria.targetKind()).isEqualTo(PortKind.GATEWAY);
            assertThat(criteria.targetDirection()).isEqualTo(PortDirection.DRIVEN);
        }
    }

    // =========================================================================
    // Heuristic Criteria - Naming
    // =========================================================================

    @Nested
    class NamingRepositoryCriteriaTest {

        @Test
        void shouldMatchInterfaceWithRepositoryNameAndCrudMethods() throws IOException {
            writeSource("com/example/OrderRepository.java", """
                    package com.example;
                    public interface OrderRepository {
                        void save(Object order);
                        Object findById(String id);
                        void delete(Object order);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode repo = graph.typeNode("com.example.OrderRepository").orElseThrow();
            GraphQuery query = graph.query();

            NamingRepositoryCriteria criteria = new NamingRepositoryCriteria();
            MatchResult result = criteria.evaluate(repo, query);

            assertThat(result.matched()).isTrue();
            assertThat(result.confidence()).isEqualTo(ConfidenceLevel.HIGH);
            assertThat(result.justification()).contains("OrderRepository");
            assertThat(result.justification()).contains("CRUD");
            assertThat(criteria.targetKind()).isEqualTo(PortKind.REPOSITORY);
            assertThat(criteria.targetDirection()).isEqualTo(PortDirection.DRIVEN);
            // Priority demoted from 80 to 50 to give precedence to semantic criteria
            assertThat(criteria.priority()).isEqualTo(50);
        }

        @Test
        void shouldMatchInterfaceWithRepositoryNameButNoCrudMethods() throws IOException {
            writeSource("com/example/ConfigRepository.java", """
                    package com.example;
                    public interface ConfigRepository {
                        Object load();
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode repo = graph.typeNode("com.example.ConfigRepository").orElseThrow();
            GraphQuery query = graph.query();

            NamingRepositoryCriteria criteria = new NamingRepositoryCriteria();
            MatchResult result = criteria.evaluate(repo, query);

            assertThat(result.matched()).isTrue();
            assertThat(result.confidence()).isEqualTo(ConfidenceLevel.MEDIUM);
        }

        @Test
        void shouldNotMatchClass() throws IOException {
            writeSource("com/example/OrderRepository.java", """
                    package com.example;
                    public class OrderRepository {
                        public void save(Object order) {}
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode repo = graph.typeNode("com.example.OrderRepository").orElseThrow();
            GraphQuery query = graph.query();

            NamingRepositoryCriteria criteria = new NamingRepositoryCriteria();
            MatchResult result = criteria.evaluate(repo, query);

            assertThat(result.matched()).isFalse();
        }

        @Test
        void shouldNotMatchInterfaceWithoutRepositoryName() throws IOException {
            writeSource("com/example/OrderService.java", """
                    package com.example;
                    public interface OrderService {
                        void save(Object order);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode service = graph.typeNode("com.example.OrderService").orElseThrow();
            GraphQuery query = graph.query();

            NamingRepositoryCriteria criteria = new NamingRepositoryCriteria();
            MatchResult result = criteria.evaluate(service, query);

            assertThat(result.matched()).isFalse();
        }
    }

    @Nested
    class NamingUseCaseCriteriaTest {

        @Test
        void shouldMatchInterfaceWithUseCaseName() throws IOException {
            writeSource("com/example/PlaceOrderUseCase.java", """
                    package com.example;
                    public interface PlaceOrderUseCase {
                        void execute(Object command);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode useCase = graph.typeNode("com.example.PlaceOrderUseCase").orElseThrow();
            GraphQuery query = graph.query();

            NamingUseCaseCriteria criteria = new NamingUseCaseCriteria();
            MatchResult result = criteria.evaluate(useCase, query);

            assertThat(result.matched()).isTrue();
            assertThat(result.confidence()).isEqualTo(ConfidenceLevel.HIGH);
            assertThat(result.justification()).contains("*UseCase");
            assertThat(criteria.targetKind()).isEqualTo(PortKind.USE_CASE);
            assertThat(criteria.targetDirection()).isEqualTo(PortDirection.DRIVING);
        }

        @Test
        void shouldMatchInterfaceWithServiceName() throws IOException {
            writeSource("com/example/OrderService.java", """
                    package com.example;
                    public interface OrderService {
                        void processOrder(Object order);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode service = graph.typeNode("com.example.OrderService").orElseThrow();
            GraphQuery query = graph.query();

            NamingUseCaseCriteria criteria = new NamingUseCaseCriteria();
            MatchResult result = criteria.evaluate(service, query);

            assertThat(result.matched()).isTrue();
            assertThat(result.justification()).contains("*Service");
        }

        @Test
        void shouldMatchInterfaceWithHandlerName() throws IOException {
            writeSource("com/example/PlaceOrderHandler.java", """
                    package com.example;
                    public interface PlaceOrderHandler {
                        void handle(Object command);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode handler = graph.typeNode("com.example.PlaceOrderHandler").orElseThrow();
            GraphQuery query = graph.query();

            NamingUseCaseCriteria criteria = new NamingUseCaseCriteria();
            MatchResult result = criteria.evaluate(handler, query);

            assertThat(result.matched()).isTrue();
            assertThat(result.justification()).contains("*Handler");
        }

        @Test
        void shouldNotMatchClass() throws IOException {
            writeSource("com/example/PlaceOrderUseCase.java", """
                    package com.example;
                    public class PlaceOrderUseCase {
                        public void execute(Object command) {}
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode useCase = graph.typeNode("com.example.PlaceOrderUseCase").orElseThrow();
            GraphQuery query = graph.query();

            NamingUseCaseCriteria criteria = new NamingUseCaseCriteria();
            MatchResult result = criteria.evaluate(useCase, query);

            assertThat(result.matched()).isFalse();
        }
    }

    @Nested
    class PackageInCriteriaTest {

        @Test
        void shouldMatchInterfaceInPackageEndingWithIn() throws IOException {
            writeSource("com/example/in/OrderingCoffee.java", """
                    package com.example.in;
                    public interface OrderingCoffee {
                        void placeOrder(Object order);
                    }
                    """);

            ApplicationGraph graph = buildGraph("com.example.in");
            TypeNode useCase = graph.typeNode("com.example.in.OrderingCoffee").orElseThrow();
            GraphQuery query = graph.query();

            PackageInCriteria criteria = new PackageInCriteria();
            MatchResult result = criteria.evaluate(useCase, query);

            assertThat(result.matched()).isTrue();
            assertThat(result.confidence()).isEqualTo(ConfidenceLevel.MEDIUM);
            assertThat(result.justification()).contains(".in");
            assertThat(criteria.targetKind()).isEqualTo(PortKind.USE_CASE);
            assertThat(criteria.targetDirection()).isEqualTo(PortDirection.DRIVING);
            assertThat(criteria.priority()).isEqualTo(60);
        }

        @Test
        void shouldMatchInterfaceInPackageWithPortsIn() throws IOException {
            writeSource("com/example/ports/in/OrderingCoffee.java", """
                    package com.example.ports.in;
                    public interface OrderingCoffee {
                        void placeOrder(Object order);
                    }
                    """);

            ApplicationGraph graph = buildGraph("com.example.ports.in");
            TypeNode useCase =
                    graph.typeNode("com.example.ports.in.OrderingCoffee").orElseThrow();
            GraphQuery query = graph.query();

            PackageInCriteria criteria = new PackageInCriteria();
            MatchResult result = criteria.evaluate(useCase, query);

            assertThat(result.matched()).isTrue();
        }

        @Test
        void shouldNotMatchInterfaceInOutPackage() throws IOException {
            writeSource("com/example/out/Orders.java", """
                    package com.example.out;
                    public interface Orders {
                        void save(Object order);
                    }
                    """);

            ApplicationGraph graph = buildGraph("com.example.out");
            TypeNode orders = graph.typeNode("com.example.out.Orders").orElseThrow();
            GraphQuery query = graph.query();

            PackageInCriteria criteria = new PackageInCriteria();
            MatchResult result = criteria.evaluate(orders, query);

            assertThat(result.matched()).isFalse();
        }
    }

    @Nested
    class PackageOutCriteriaTest {

        @Test
        void shouldMatchInterfaceInPackageEndingWithOut() throws IOException {
            writeSource("com/example/out/OrderRepository.java", """
                    package com.example.out;
                    public interface OrderRepository {
                        void save(Object order);
                    }
                    """);

            ApplicationGraph graph = buildGraph("com.example.out");
            TypeNode repo = graph.typeNode("com.example.out.OrderRepository").orElseThrow();
            GraphQuery query = graph.query();

            PackageOutCriteria criteria = new PackageOutCriteria();
            MatchResult result = criteria.evaluate(repo, query);

            assertThat(result.matched()).isTrue();
            assertThat(result.confidence()).isEqualTo(ConfidenceLevel.MEDIUM);
            assertThat(result.justification()).contains(".out");
            assertThat(criteria.targetKind()).isEqualTo(PortKind.REPOSITORY);
            assertThat(criteria.targetDirection()).isEqualTo(PortDirection.DRIVEN);
            assertThat(criteria.priority()).isEqualTo(60);
        }

        @Test
        void shouldMatchInterfaceInPackageWithPortsOut() throws IOException {
            writeSource("com/example/ports/out/OrderRepository.java", """
                    package com.example.ports.out;
                    public interface OrderRepository {
                        void save(Object order);
                    }
                    """);

            ApplicationGraph graph = buildGraph("com.example.ports.out");
            TypeNode repo =
                    graph.typeNode("com.example.ports.out.OrderRepository").orElseThrow();
            GraphQuery query = graph.query();

            PackageOutCriteria criteria = new PackageOutCriteria();
            MatchResult result = criteria.evaluate(repo, query);

            assertThat(result.matched()).isTrue();
        }

        @Test
        void shouldNotMatchInterfaceInInPackage() throws IOException {
            writeSource("com/example/in/OrderingCoffee.java", """
                    package com.example.in;
                    public interface OrderingCoffee {
                        void placeOrder(Object order);
                    }
                    """);

            ApplicationGraph graph = buildGraph("com.example.in");
            TypeNode useCase = graph.typeNode("com.example.in.OrderingCoffee").orElseThrow();
            GraphQuery query = graph.query();

            PackageOutCriteria criteria = new PackageOutCriteria();
            MatchResult result = criteria.evaluate(useCase, query);

            assertThat(result.matched()).isFalse();
        }
    }

    @Nested
    class NamingGatewayCriteriaTest {

        @Test
        void shouldMatchInterfaceWithGatewayName() throws IOException {
            writeSource("com/example/PaymentGateway.java", """
                    package com.example;
                    public interface PaymentGateway {
                        void processPayment(Object payment);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode gateway = graph.typeNode("com.example.PaymentGateway").orElseThrow();
            GraphQuery query = graph.query();

            NamingGatewayCriteria criteria = new NamingGatewayCriteria();
            MatchResult result = criteria.evaluate(gateway, query);

            assertThat(result.matched()).isTrue();
            assertThat(result.confidence()).isEqualTo(ConfidenceLevel.HIGH);
            assertThat(result.justification()).contains("*Gateway");
            assertThat(criteria.targetKind()).isEqualTo(PortKind.GATEWAY);
            assertThat(criteria.targetDirection()).isEqualTo(PortDirection.DRIVEN);
            // Priority demoted from 80 to 50 to give precedence to semantic criteria
            assertThat(criteria.priority()).isEqualTo(50);
        }

        @Test
        void shouldMatchInterfaceWithClientName() throws IOException {
            writeSource("com/example/EmailClient.java", """
                    package com.example;
                    public interface EmailClient {
                        void sendEmail(Object email);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode client = graph.typeNode("com.example.EmailClient").orElseThrow();
            GraphQuery query = graph.query();

            NamingGatewayCriteria criteria = new NamingGatewayCriteria();
            MatchResult result = criteria.evaluate(client, query);

            assertThat(result.matched()).isTrue();
            assertThat(result.justification()).contains("*Client");
        }

        @Test
        void shouldMatchInterfaceWithAdapterName() throws IOException {
            writeSource("com/example/NotificationAdapter.java", """
                    package com.example;
                    public interface NotificationAdapter {
                        void notify(Object notification);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode adapter = graph.typeNode("com.example.NotificationAdapter").orElseThrow();
            GraphQuery query = graph.query();

            NamingGatewayCriteria criteria = new NamingGatewayCriteria();
            MatchResult result = criteria.evaluate(adapter, query);

            assertThat(result.matched()).isTrue();
            assertThat(result.justification()).contains("*Adapter");
        }

        @Test
        void shouldNotMatchClass() throws IOException {
            writeSource("com/example/PaymentGateway.java", """
                    package com.example;
                    public class PaymentGateway {
                        public void processPayment(Object payment) {}
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode gateway = graph.typeNode("com.example.PaymentGateway").orElseThrow();
            GraphQuery query = graph.query();

            NamingGatewayCriteria criteria = new NamingGatewayCriteria();
            MatchResult result = criteria.evaluate(gateway, query);

            assertThat(result.matched()).isFalse();
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

    private ApplicationGraph buildGraph() {
        return buildGraph("com.example");
    }

    private ApplicationGraph buildGraph(String basePackage) {
        JavaAnalysisInput input = new JavaAnalysisInput(List.of(tempDir), List.of(), 17, basePackage);

        JavaSemanticModel model = frontend.build(input);
        GraphMetadata metadata =
                GraphMetadata.of(basePackage, 17, (int) model.types().size());

        model = frontend.build(input);
        return builder.build(model, metadata);
    }
}
