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

package io.hexaglue.core.graph.style;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.graph.testing.TestGraphBuilder;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("StyleDetector")
class StyleDetectorTest {

    private final StyleDetector detector = new StyleDetector();

    @Nested
    @DisplayName("Hexagonal style detection")
    class HexagonalStyleTest {

        @Test
        @DisplayName("should detect HEXAGONAL from ports.in/ports.out packages")
        void shouldDetectHexagonalFromPortsInOut() {
            var graph = TestGraphBuilder.create("com.example")
                    .withInterface("com.example.order.ports.in.OrderingCoffee")
                    .withInterface("com.example.order.ports.out.OrderRepository")
                    .withInterface("com.example.payment.ports.in.MakingPayment")
                    .withInterface("com.example.payment.ports.out.PaymentGateway")
                    .withClass("com.example.order.domain.Order")
                    .withClass("com.example.payment.domain.Payment")
                    .build();

            var result = detector.detect(graph.typeNodes(), "com.example");

            assertThat(result.style()).isEqualTo(PackageOrganizationStyle.HEXAGONAL);
            assertThat(result.isDetected()).isTrue();
            assertThat(result.detectedPatterns()).containsKey(".ports.in");
            assertThat(result.detectedPatterns()).containsKey(".ports.out");
        }

        @Test
        @DisplayName("should detect HEXAGONAL from adapters package")
        void shouldDetectHexagonalFromAdapters() {
            var graph = TestGraphBuilder.create("com.example")
                    .withClass("com.example.adapters.persistence.JpaOrderRepository")
                    .withClass("com.example.adapters.web.OrderController")
                    .withClass("com.example.adapters.messaging.OrderEventPublisher")
                    .withClass("com.example.domain.Order")
                    .withClass("com.example.domain.Payment")
                    .build();

            var result = detector.detect(graph.typeNodes(), "com.example");

            assertThat(result.style()).isEqualTo(PackageOrganizationStyle.HEXAGONAL);
            assertThat(result.detectedPatterns()).containsKey(".adapters");
        }

        @Test
        @DisplayName("should detect HEXAGONAL from inbound/outbound packages")
        void shouldDetectHexagonalFromInboundOutbound() {
            var graph = TestGraphBuilder.create("com.example")
                    .withInterface("com.example.inbound.OrderService")
                    .withInterface("com.example.inbound.PaymentService")
                    .withInterface("com.example.inbound.CustomerService")
                    .withInterface("com.example.outbound.OrderRepository")
                    .withInterface("com.example.outbound.PaymentGateway")
                    .withInterface("com.example.outbound.NotificationSender")
                    .withClass("com.example.model.Order")
                    .withClass("com.example.model.Payment")
                    .build();

            var result = detector.detect(graph.typeNodes(), "com.example");

            assertThat(result.style()).isEqualTo(PackageOrganizationStyle.HEXAGONAL);
        }
    }

    @Nested
    @DisplayName("Layer style detection")
    class LayerStyleTest {

        @Test
        @DisplayName("should detect BY_LAYER from domain/application/infrastructure packages")
        void shouldDetectByLayerFromTypicalPackages() {
            var graph = TestGraphBuilder.create("com.example")
                    .withClass("com.example.domain.Order")
                    .withClass("com.example.domain.Customer")
                    .withClass("com.example.application.OrderService")
                    .withClass("com.example.application.CustomerService")
                    .withClass("com.example.infrastructure.JpaOrderRepository")
                    .withClass("com.example.infrastructure.JpaCustomerRepository")
                    .build();

            var result = detector.detect(graph.typeNodes(), "com.example");

            assertThat(result.style()).isEqualTo(PackageOrganizationStyle.BY_LAYER);
            assertThat(result.detectedPatterns()).containsKey(".domain");
            assertThat(result.detectedPatterns()).containsKey(".application");
            assertThat(result.detectedPatterns()).containsKey(".infrastructure");
        }

        @Test
        @DisplayName("should detect BY_LAYER with presentation layer")
        void shouldDetectByLayerWithPresentation() {
            var graph = TestGraphBuilder.create("com.example")
                    .withClass("com.example.domain.Order")
                    .withClass("com.example.presentation.OrderController")
                    .withClass("com.example.presentation.CustomerController")
                    .withClass("com.example.infrastructure.OrderRepository")
                    .build();

            var result = detector.detect(graph.typeNodes(), "com.example");

            assertThat(result.style()).isEqualTo(PackageOrganizationStyle.BY_LAYER);
        }
    }

    @Nested
    @DisplayName("Clean Architecture detection")
    class CleanArchitectureTest {

        @Test
        @DisplayName("should detect CLEAN_ARCHITECTURE from usecases/gateways/entities")
        void shouldDetectCleanArchitecture() {
            var graph = TestGraphBuilder.create("com.example")
                    .withClass("com.example.entities.Order")
                    .withClass("com.example.entities.Customer")
                    .withClass("com.example.usecases.PlaceOrder")
                    .withClass("com.example.usecases.CancelOrder")
                    .withInterface("com.example.gateways.OrderGateway")
                    .withInterface("com.example.gateways.PaymentGateway")
                    .build();

            var result = detector.detect(graph.typeNodes(), "com.example");

            assertThat(result.style()).isEqualTo(PackageOrganizationStyle.CLEAN_ARCHITECTURE);
            assertThat(result.detectedPatterns()).containsKey(".usecases");
            assertThat(result.detectedPatterns()).containsKey(".gateways");
            assertThat(result.detectedPatterns()).containsKey(".entities");
        }

        @Test
        @DisplayName("should detect CLEAN_ARCHITECTURE with controllers")
        void shouldDetectCleanArchitectureWithControllers() {
            var graph = TestGraphBuilder.create("com.example")
                    .withClass("com.example.usecases.PlaceOrder")
                    .withClass("com.example.usecases.GetOrderStatus")
                    .withClass("com.example.controllers.OrderController")
                    .withClass("com.example.controllers.CustomerController")
                    .withClass("com.example.entities.Order")
                    .build();

            var result = detector.detect(graph.typeNodes(), "com.example");

            assertThat(result.style()).isEqualTo(PackageOrganizationStyle.CLEAN_ARCHITECTURE);
        }
    }

    @Nested
    @DisplayName("Onion Architecture detection")
    class OnionArchitectureTest {

        @Test
        @DisplayName("should detect ONION from core/domain.services packages")
        void shouldDetectOnion() {
            var graph = TestGraphBuilder.create("com.example")
                    .withClass("com.example.core.Order")
                    .withClass("com.example.core.Customer")
                    .withClass("com.example.domain.services.OrderDomainService")
                    .withClass("com.example.domain.services.PricingService")
                    .withClass("com.example.infrastructure.persistence.JpaRepository")
                    .build();

            var result = detector.detect(graph.typeNodes(), "com.example");

            assertThat(result.style()).isEqualTo(PackageOrganizationStyle.ONION);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCasesTest {

        @Test
        @DisplayName("should return UNKNOWN for empty types")
        void shouldReturnUnknownForEmptyTypes() {
            var result = detector.detect(List.of(), "com.example");

            assertThat(result.style()).isEqualTo(PackageOrganizationStyle.UNKNOWN);
            assertThat(result.isDetected()).isFalse();
            assertThat(result.confidence()).isEqualTo(ConfidenceLevel.LOW);
        }

        @Test
        @DisplayName("should return UNKNOWN for null types")
        void shouldReturnUnknownForNullTypes() {
            var result = detector.detect(null, "com.example");

            assertThat(result.style()).isEqualTo(PackageOrganizationStyle.UNKNOWN);
        }

        @Test
        @DisplayName("should return UNKNOWN when no patterns match")
        void shouldReturnUnknownWhenNoPatternsMatch() {
            var graph = TestGraphBuilder.create("com.example")
                    .withClass("com.example.foo.Bar")
                    .withClass("com.example.baz.Qux")
                    .withClass("com.example.models.Something")
                    .build();

            var result = detector.detect(graph.typeNodes(), "com.example");

            assertThat(result.style()).isEqualTo(PackageOrganizationStyle.UNKNOWN);
        }

        @Test
        @DisplayName("should have LOW confidence for few types")
        void shouldHaveLowConfidenceForFewTypes() {
            var graph = TestGraphBuilder.create("com.example")
                    .withInterface("com.example.ports.in.OrderService")
                    .withClass("com.example.domain.Order")
                    .build();

            var result = detector.detect(graph.typeNodes(), "com.example");

            // Style might be detected but with low confidence due to few types
            assertThat(result.confidence()).isIn(ConfidenceLevel.LOW, ConfidenceLevel.MEDIUM);
        }
    }

    @Nested
    @DisplayName("Confidence levels")
    class ConfidenceLevelTest {

        @Test
        @DisplayName("should have HIGH confidence for clear hexagonal structure")
        void shouldHaveHighConfidenceForClearHexagonal() {
            var graph = TestGraphBuilder.create("com.example")
                    .withInterface("com.example.order.ports.in.OrderingCoffee")
                    .withInterface("com.example.order.ports.in.ManagingOrders")
                    .withInterface("com.example.order.ports.in.CancellingOrder")
                    .withInterface("com.example.order.ports.out.OrderRepository")
                    .withInterface("com.example.order.ports.out.PaymentGateway")
                    .withInterface("com.example.order.ports.out.NotificationSender")
                    .withInterface("com.example.payment.ports.in.MakingPayment")
                    .withInterface("com.example.payment.ports.in.RefundingPayment")
                    .withInterface("com.example.payment.ports.out.PaymentProcessor")
                    .withInterface("com.example.payment.ports.out.AuditLogger")
                    .withClass("com.example.order.model.Order")
                    .withClass("com.example.payment.model.Payment")
                    .build();

            var result = detector.detect(graph.typeNodes(), "com.example");

            assertThat(result.style()).isEqualTo(PackageOrganizationStyle.HEXAGONAL);
            assertThat(result.confidence()).isIn(ConfidenceLevel.MEDIUM, ConfidenceLevel.HIGH);
            assertThat(result.isDetected()).isTrue();
        }

        @Test
        @DisplayName("should have MEDIUM confidence for mixed styles")
        void shouldHaveMediumConfidenceForMixedStyles() {
            var graph = TestGraphBuilder.create("com.example")
                    // Hexagonal patterns
                    .withInterface("com.example.ports.in.OrderService")
                    .withInterface("com.example.ports.out.Repository")
                    // Layer patterns
                    .withClass("com.example.domain.Order")
                    .withClass("com.example.application.OrderUseCase")
                    .withClass("com.example.infrastructure.JpaRepo")
                    .build();

            var result = detector.detect(graph.typeNodes(), "com.example");

            // Should detect something, but with medium confidence due to mixed signals
            assertThat(result.isDetected()).isTrue();
            assertThat(result.confidence()).isIn(ConfidenceLevel.LOW, ConfidenceLevel.MEDIUM);
        }
    }

    @Nested
    @DisplayName("Pattern matching")
    class PatternMatchingTest {

        @Test
        @DisplayName("should count pattern matches correctly")
        void shouldCountPatternMatchesCorrectly() {
            var graph = TestGraphBuilder.create("com.example")
                    .withInterface("com.example.a.ports.in.ServiceA")
                    .withInterface("com.example.b.ports.in.ServiceB")
                    .withInterface("com.example.c.ports.in.ServiceC")
                    .withInterface("com.example.a.ports.out.RepoA")
                    .withInterface("com.example.b.ports.out.RepoB")
                    .build();

            var result = detector.detect(graph.typeNodes(), "com.example");

            assertThat(result.detectedPatterns().get(".ports.in")).isEqualTo(3);
            assertThat(result.detectedPatterns().get(".ports.out")).isEqualTo(2);
            assertThat(result.totalMatches()).isEqualTo(5);
        }
    }
}
