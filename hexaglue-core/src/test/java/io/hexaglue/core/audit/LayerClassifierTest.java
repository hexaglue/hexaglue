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

package io.hexaglue.core.audit;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.spi.audit.LayerClassification;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link LayerClassifier}.
 */
class LayerClassifierTest {

    private LayerClassifier classifier;

    @BeforeEach
    void setUp() {
        classifier = new LayerClassifier();
    }

    @Nested
    @DisplayName("Presentation Layer Classification")
    class PresentationLayerTests {

        @Test
        @DisplayName("should classify type with @RestController annotation as PRESENTATION")
        void shouldClassifyRestController() {
            LayerClassification result = classifier.classify(
                    "com.example.OrderController",
                    "OrderController",
                    List.of("org.springframework.web.bind.annotation.RestController"));

            assertThat(result).isEqualTo(LayerClassification.PRESENTATION);
        }

        @Test
        @DisplayName("should classify type with @Controller annotation as PRESENTATION")
        void shouldClassifyController() {
            LayerClassification result = classifier.classify(
                    "com.example.ProductController",
                    "ProductController",
                    List.of("org.springframework.stereotype.Controller"));

            assertThat(result).isEqualTo(LayerClassification.PRESENTATION);
        }

        @Test
        @DisplayName("should classify type in presentation package as PRESENTATION")
        void shouldClassifyPresentationPackage() {
            LayerClassification result =
                    classifier.classify("com.example.presentation.OrderHandler", "OrderHandler", List.of());

            assertThat(result).isEqualTo(LayerClassification.PRESENTATION);
        }

        @Test
        @DisplayName("should classify type in ui package as PRESENTATION")
        void shouldClassifyUiPackage() {
            LayerClassification result = classifier.classify("com.example.ui.CustomerView", "CustomerView", List.of());

            assertThat(result).isEqualTo(LayerClassification.PRESENTATION);
        }

        @Test
        @DisplayName("should classify type with Controller suffix as PRESENTATION")
        void shouldClassifyControllerSuffix() {
            LayerClassification result =
                    classifier.classify("com.example.api.OrderController", "OrderController", List.of());

            assertThat(result).isEqualTo(LayerClassification.PRESENTATION);
        }

        @Test
        @DisplayName("should classify type with Resolver suffix as PRESENTATION")
        void shouldClassifyResolverSuffix() {
            LayerClassification result =
                    classifier.classify("com.example.graphql.OrderResolver", "OrderResolver", List.of());

            assertThat(result).isEqualTo(LayerClassification.PRESENTATION);
        }
    }

    @Nested
    @DisplayName("Application Layer Classification")
    class ApplicationLayerTests {

        @Test
        @DisplayName("should classify type with @Service annotation as APPLICATION")
        void shouldClassifyService() {
            LayerClassification result = classifier.classify(
                    "com.example.OrderService", "OrderService", List.of("org.springframework.stereotype.Service"));

            assertThat(result).isEqualTo(LayerClassification.APPLICATION);
        }

        @Test
        @DisplayName("should classify type in application package as APPLICATION")
        void shouldClassifyApplicationPackage() {
            LayerClassification result =
                    classifier.classify("com.example.application.CreateOrderUseCase", "CreateOrderUseCase", List.of());

            assertThat(result).isEqualTo(LayerClassification.APPLICATION);
        }

        @Test
        @DisplayName("should classify type with Service suffix as APPLICATION")
        void shouldClassifyServiceSuffix() {
            LayerClassification result = classifier.classify("com.example.OrderService", "OrderService", List.of());

            assertThat(result).isEqualTo(LayerClassification.APPLICATION);
        }

        @Test
        @DisplayName("should classify type with UseCase suffix as APPLICATION")
        void shouldClassifyUseCaseSuffix() {
            LayerClassification result =
                    classifier.classify("com.example.PlaceOrderUseCase", "PlaceOrderUseCase", List.of());

            assertThat(result).isEqualTo(LayerClassification.APPLICATION);
        }
    }

    @Nested
    @DisplayName("Domain Layer Classification")
    class DomainLayerTests {

        @Test
        @DisplayName("should classify type in domain package as DOMAIN")
        void shouldClassifyDomainPackage() {
            LayerClassification result = classifier.classify("com.example.domain.Order", "Order", List.of());

            assertThat(result).isEqualTo(LayerClassification.DOMAIN);
        }

        @Test
        @DisplayName("should classify type in model package as DOMAIN")
        void shouldClassifyModelPackage() {
            LayerClassification result = classifier.classify("com.example.model.Customer", "Customer", List.of());

            assertThat(result).isEqualTo(LayerClassification.DOMAIN);
        }

        @Test
        @DisplayName("should classify type with Entity suffix as DOMAIN")
        void shouldClassifyEntitySuffix() {
            LayerClassification result = classifier.classify("com.example.OrderEntity", "OrderEntity", List.of());

            assertThat(result).isEqualTo(LayerClassification.DOMAIN);
        }

        @Test
        @DisplayName("should classify type with ValueObject suffix as DOMAIN")
        void shouldClassifyValueObjectSuffix() {
            LayerClassification result =
                    classifier.classify("com.example.MoneyValueObject", "MoneyValueObject", List.of());

            assertThat(result).isEqualTo(LayerClassification.DOMAIN);
        }
    }

    @Nested
    @DisplayName("Infrastructure Layer Classification")
    class InfrastructureLayerTests {

        @Test
        @DisplayName("should classify type with @Repository annotation as INFRASTRUCTURE")
        void shouldClassifyRepository() {
            LayerClassification result = classifier.classify(
                    "com.example.OrderRepository",
                    "OrderRepository",
                    List.of("org.springframework.stereotype.Repository"));

            assertThat(result).isEqualTo(LayerClassification.INFRASTRUCTURE);
        }

        @Test
        @DisplayName("should classify type with @Entity annotation as INFRASTRUCTURE")
        void shouldClassifyEntity() {
            LayerClassification result = classifier.classify(
                    "com.example.OrderEntity", "OrderEntity", List.of("jakarta.persistence.Entity"));

            assertThat(result).isEqualTo(LayerClassification.INFRASTRUCTURE);
        }

        @Test
        @DisplayName("should classify type in infrastructure package as INFRASTRUCTURE")
        void shouldClassifyInfrastructurePackage() {
            LayerClassification result = classifier.classify(
                    "com.example.infrastructure.OrderRepositoryImpl", "OrderRepositoryImpl", List.of());

            assertThat(result).isEqualTo(LayerClassification.INFRASTRUCTURE);
        }

        @Test
        @DisplayName("should classify type with Repository suffix as INFRASTRUCTURE")
        void shouldClassifyRepositorySuffix() {
            LayerClassification result =
                    classifier.classify("com.example.OrderRepository", "OrderRepository", List.of());

            assertThat(result).isEqualTo(LayerClassification.INFRASTRUCTURE);
        }

        @Test
        @DisplayName("should classify type with Adapter suffix as INFRASTRUCTURE")
        void shouldClassifyAdapterSuffix() {
            LayerClassification result = classifier.classify("com.example.EmailAdapter", "EmailAdapter", List.of());

            assertThat(result).isEqualTo(LayerClassification.INFRASTRUCTURE);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("should return UNKNOWN for null qualified name")
        void shouldReturnUnknownForNullQualifiedName() {
            LayerClassification result = classifier.classify(null, "Order", List.of());

            assertThat(result).isEqualTo(LayerClassification.UNKNOWN);
        }

        @Test
        @DisplayName("should return UNKNOWN for null simple name")
        void shouldReturnUnknownForNullSimpleName() {
            LayerClassification result = classifier.classify("com.example.Order", null, List.of());

            assertThat(result).isEqualTo(LayerClassification.UNKNOWN);
        }

        @Test
        @DisplayName("should return UNKNOWN for type with no classification hints")
        void shouldReturnUnknownForNoHints() {
            LayerClassification result = classifier.classify("com.example.SomeClass", "SomeClass", List.of());

            assertThat(result).isEqualTo(LayerClassification.UNKNOWN);
        }

        @Test
        @DisplayName("should prioritize annotation over package name")
        void shouldPrioritizeAnnotationOverPackage() {
            LayerClassification result = classifier.classify(
                    "com.example.domain.OrderRepository", // domain package
                    "OrderRepository",
                    List.of("org.springframework.stereotype.Repository")); // infrastructure annotation

            assertThat(result).isEqualTo(LayerClassification.INFRASTRUCTURE);
        }

        @Test
        @DisplayName("should prioritize package name over suffix")
        void shouldPrioritizePackageOverSuffix() {
            LayerClassification result = classifier.classify(
                    "com.example.presentation.OrderService", // presentation package
                    "OrderService", // application suffix
                    List.of());

            assertThat(result).isEqualTo(LayerClassification.PRESENTATION);
        }
    }
}
