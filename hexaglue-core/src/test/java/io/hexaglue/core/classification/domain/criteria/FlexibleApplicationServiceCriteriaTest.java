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

package io.hexaglue.core.classification.domain.criteria;

import static org.assertj.core.api.Assertions.*;

import io.hexaglue.arch.ElementKind;
import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.classification.MatchResult;
import io.hexaglue.core.classification.semantic.CoreAppClass;
import io.hexaglue.core.classification.semantic.CoreAppClassIndex;
import io.hexaglue.core.frontend.CachedSpoonAnalyzer;
import io.hexaglue.core.frontend.JavaFrontend.JavaAnalysisInput;
import io.hexaglue.core.frontend.JavaSemanticModel;
import io.hexaglue.core.frontend.spoon.SpoonFrontend;
import io.hexaglue.core.graph.ApplicationGraph;
import io.hexaglue.core.graph.builder.GraphBuilder;
import io.hexaglue.core.graph.model.GraphMetadata;
import io.hexaglue.core.graph.model.NodeId;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link FlexibleApplicationServiceCriteria}.
 *
 * @since 5.0.0
 */
class FlexibleApplicationServiceCriteriaTest {

    @TempDir
    Path tempDir;

    private SpoonFrontend frontend;
    private GraphBuilder builder;

    @BeforeEach
    void setUp() {
        frontend = new SpoonFrontend();
        CachedSpoonAnalyzer analyzer = new CachedSpoonAnalyzer();
        builder = new GraphBuilder(true, analyzer);
    }

    @Nested
    class PivotClassification {

        @Test
        void shouldMatchPivotClass() throws IOException {
            // A pivot class implements a driving port AND depends on a driven port
            writeSource("com/example/ports/in/OrderUseCases.java", """
                    package com.example.ports.in;
                    public interface OrderUseCases {
                        void createOrder(String product);
                    }
                    """);
            writeSource("com/example/ports/out/OrderRepository.java", """
                    package com.example.ports.out;
                    public interface OrderRepository {
                        void save(Object order);
                    }
                    """);
            writeSource("com/example/application/OrderService.java", """
                    package com.example.application;
                    import com.example.ports.in.OrderUseCases;
                    import com.example.ports.out.OrderRepository;
                    public class OrderService implements OrderUseCases {
                        private final OrderRepository repository;
                        public OrderService(OrderRepository repository) {
                            this.repository = repository;
                        }
                        public void createOrder(String product) {
                            repository.save(product);
                        }
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode orderService =
                    graph.typeNode("com.example.application.OrderService").orElseThrow();
            GraphQuery query = graph.query();

            // Build CoreAppClassIndex with OrderService as pivot
            NodeId serviceId = NodeId.type("com.example.application.OrderService");
            NodeId useCasesId = NodeId.type("com.example.ports.in.OrderUseCases");
            NodeId repoId = NodeId.type("com.example.ports.out.OrderRepository");

            CoreAppClassIndex index = CoreAppClassIndex.builder()
                    .put(new CoreAppClass(serviceId, Set.of(useCasesId), Set.of(repoId)))
                    .build();

            FlexibleApplicationServiceCriteria criteria = new FlexibleApplicationServiceCriteria(index);
            MatchResult result = criteria.evaluate(orderService, query);

            assertThat(result.matched()).isTrue();
            assertThat(result.confidence()).isEqualTo(ConfidenceLevel.HIGH);
            assertThat(result.justification()).contains("driving port").contains("driven");
        }
    }

    @Nested
    class NonMatchingCases {

        @Test
        void shouldNotMatchInboundOnlyClass() throws IOException {
            // Inbound-only: implements driving port but no driven dependencies
            writeSource("com/example/ports/in/QueryHandler.java", """
                    package com.example.ports.in;
                    public interface QueryHandler {
                        String query();
                    }
                    """);
            writeSource("com/example/application/SimpleQueryHandler.java", """
                    package com.example.application;
                    import com.example.ports.in.QueryHandler;
                    public class SimpleQueryHandler implements QueryHandler {
                        public String query() { return "result"; }
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode handler =
                    graph.typeNode("com.example.application.SimpleQueryHandler").orElseThrow();
            GraphQuery query = graph.query();

            NodeId handlerId = NodeId.type("com.example.application.SimpleQueryHandler");
            NodeId portId = NodeId.type("com.example.ports.in.QueryHandler");

            CoreAppClassIndex index = CoreAppClassIndex.builder()
                    .put(new CoreAppClass(handlerId, Set.of(portId), Set.of()))
                    .build();

            FlexibleApplicationServiceCriteria criteria = new FlexibleApplicationServiceCriteria(index);
            MatchResult result = criteria.evaluate(handler, query);

            assertThat(result.matched()).isFalse();
        }

        @Test
        void shouldNotMatchOutboundOnlyClass() throws IOException {
            // Outbound-only: depends on driven port but doesn't implement driving port
            writeSource("com/example/ports/out/NotificationGateway.java", """
                    package com.example.ports.out;
                    public interface NotificationGateway {
                        void send(String message);
                    }
                    """);
            writeSource("com/example/application/BackgroundProcessor.java", """
                    package com.example.application;
                    import com.example.ports.out.NotificationGateway;
                    public class BackgroundProcessor {
                        private final NotificationGateway gateway;
                        public BackgroundProcessor(NotificationGateway gateway) {
                            this.gateway = gateway;
                        }
                        public void process() { gateway.send("done"); }
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode processor = graph.typeNode("com.example.application.BackgroundProcessor")
                    .orElseThrow();
            GraphQuery query = graph.query();

            NodeId processorId = NodeId.type("com.example.application.BackgroundProcessor");
            NodeId gatewayId = NodeId.type("com.example.ports.out.NotificationGateway");

            CoreAppClassIndex index = CoreAppClassIndex.builder()
                    .put(new CoreAppClass(processorId, Set.of(), Set.of(gatewayId)))
                    .build();

            FlexibleApplicationServiceCriteria criteria = new FlexibleApplicationServiceCriteria(index);
            MatchResult result = criteria.evaluate(processor, query);

            assertThat(result.matched()).isFalse();
        }

        @Test
        void shouldNotMatchInterface() throws IOException {
            writeSource("com/example/ports/in/OrderUseCases.java", """
                    package com.example.ports.in;
                    public interface OrderUseCases {
                        void createOrder(String product);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode iface =
                    graph.typeNode("com.example.ports.in.OrderUseCases").orElseThrow();
            GraphQuery query = graph.query();

            CoreAppClassIndex index = CoreAppClassIndex.empty();

            FlexibleApplicationServiceCriteria criteria = new FlexibleApplicationServiceCriteria(index);
            MatchResult result = criteria.evaluate(iface, query);

            assertThat(result.matched()).isFalse();
        }

        @Test
        void shouldNotMatchAbstractClass() throws IOException {
            writeSource("com/example/ports/in/OrderUseCases.java", """
                    package com.example.ports.in;
                    public interface OrderUseCases {
                        void createOrder(String product);
                    }
                    """);
            writeSource("com/example/application/AbstractService.java", """
                    package com.example.application;
                    import com.example.ports.in.OrderUseCases;
                    public abstract class AbstractService implements OrderUseCases {}
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode abstractService =
                    graph.typeNode("com.example.application.AbstractService").orElseThrow();
            GraphQuery query = graph.query();

            CoreAppClassIndex index = CoreAppClassIndex.empty();

            FlexibleApplicationServiceCriteria criteria = new FlexibleApplicationServiceCriteria(index);
            MatchResult result = criteria.evaluate(abstractService, query);

            assertThat(result.matched()).isFalse();
        }

        @Test
        void shouldNotMatchNonCoreAppClass() throws IOException {
            writeSource("com/example/application/PlainService.java", """
                    package com.example.application;
                    public class PlainService {
                        public void doSomething() {}
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode plainService =
                    graph.typeNode("com.example.application.PlainService").orElseThrow();
            GraphQuery query = graph.query();

            // Empty index - PlainService is not a CoreAppClass
            CoreAppClassIndex index = CoreAppClassIndex.empty();

            FlexibleApplicationServiceCriteria criteria = new FlexibleApplicationServiceCriteria(index);
            MatchResult result = criteria.evaluate(plainService, query);

            assertThat(result.matched()).isFalse();
        }
    }

    @Nested
    class CriteriaMetadata {

        @Test
        void shouldHaveCorrectMetadata() {
            CoreAppClassIndex index = CoreAppClassIndex.empty();
            FlexibleApplicationServiceCriteria criteria = new FlexibleApplicationServiceCriteria(index);

            assertThat(criteria.id()).isEqualTo("domain.semantic.applicationService");
            assertThat(criteria.name()).isEqualTo("flexible-application-service");
            assertThat(criteria.priority()).isEqualTo(74);
            assertThat(criteria.targetKind()).isEqualTo(ElementKind.APPLICATION_SERVICE);
            assertThat(criteria.description()).contains("APPLICATION_SERVICE");
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
        JavaAnalysisInput input = new JavaAnalysisInput(List.of(tempDir), List.of(), 17, "com.example", false, false);

        JavaSemanticModel model = frontend.build(input);
        GraphMetadata metadata =
                GraphMetadata.of("com.example", 17, (int) model.types().size());

        model = frontend.build(input);
        return builder.build(model, metadata);
    }
}
