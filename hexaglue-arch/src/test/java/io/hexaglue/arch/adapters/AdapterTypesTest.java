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

package io.hexaglue.arch.adapters;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.arch.ClassificationTrace;
import io.hexaglue.arch.ElementId;
import io.hexaglue.arch.ElementKind;
import io.hexaglue.arch.UnclassifiedType;
import io.hexaglue.arch.model.TypeId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Adapter Types")
class AdapterTypesTest {

    private static final String PKG = "com.example.adapters";

    private ClassificationTrace highConfidence(ElementKind kind) {
        return ClassificationTrace.highConfidence(kind, "test", "Test classification");
    }

    @Nested
    @DisplayName("AdapterType")
    class AdapterTypeTest {

        @Test
        @DisplayName("should identify driving adapter types")
        void shouldIdentifyDriving() {
            assertThat(AdapterType.REST_CONTROLLER.isDriving()).isTrue();
            assertThat(AdapterType.GRAPHQL_CONTROLLER.isDriving()).isTrue();
            assertThat(AdapterType.MESSAGE_LISTENER.isDriving()).isTrue();
            assertThat(AdapterType.CLI.isDriving()).isTrue();
            assertThat(AdapterType.SCHEDULED_TASK.isDriving()).isTrue();
            assertThat(AdapterType.JPA_REPOSITORY.isDriving()).isFalse();
        }

        @Test
        @DisplayName("should identify driven adapter types")
        void shouldIdentifyDriven() {
            assertThat(AdapterType.JPA_REPOSITORY.isDriven()).isTrue();
            assertThat(AdapterType.JDBC_REPOSITORY.isDriven()).isTrue();
            assertThat(AdapterType.HTTP_CLIENT.isDriven()).isTrue();
            assertThat(AdapterType.MESSAGE_PRODUCER.isDriven()).isTrue();
            assertThat(AdapterType.FILE_STORAGE.isDriven()).isTrue();
            assertThat(AdapterType.CACHE.isDriven()).isTrue();
            assertThat(AdapterType.REST_CONTROLLER.isDriven()).isFalse();
        }
    }

    @Nested
    @DisplayName("DrivingAdapter")
    class DrivingAdapterTest {

        @Test
        @DisplayName("should create REST controller")
        void shouldCreateRestController() {
            // when
            DrivingAdapter adapter = new DrivingAdapter(
                    ElementId.of(PKG + ".OrderController"),
                    AdapterType.REST_CONTROLLER,
                    List.of(),
                    List.of("/orders", "/orders/{id}"),
                    null,
                    highConfidence(ElementKind.DRIVING_ADAPTER));

            // then
            assertThat(adapter.kind()).isEqualTo(ElementKind.DRIVING_ADAPTER);
            assertThat(adapter.isRestController()).isTrue();
            assertThat(adapter.isGraphQLController()).isFalse();
            assertThat(adapter.hasEndpoints()).isTrue();
            assertThat(adapter.endpoints()).containsExactly("/orders", "/orders/{id}");
        }

        @Test
        @DisplayName("should create message listener")
        void shouldCreateMessageListener() {
            // when
            DrivingAdapter adapter = new DrivingAdapter(
                    ElementId.of(PKG + ".OrderEventListener"),
                    AdapterType.MESSAGE_LISTENER,
                    List.of(),
                    List.of(),
                    null,
                    highConfidence(ElementKind.DRIVING_ADAPTER));

            // then
            assertThat(adapter.isMessageListener()).isTrue();
            assertThat(adapter.hasEndpoints()).isFalse();
        }

        @Test
        @DisplayName("should track called ports")
        void shouldTrackCalledPorts() {
            // given
            TypeId useCaseId = TypeId.of("com.example.PlaceOrderUseCase");

            // when
            DrivingAdapter adapter = new DrivingAdapter(
                    ElementId.of(PKG + ".OrderController"),
                    AdapterType.REST_CONTROLLER,
                    List.of(useCaseId),
                    List.of(),
                    null,
                    highConfidence(ElementKind.DRIVING_ADAPTER));

            // then
            assertThat(adapter.calledPorts()).hasSize(1);
        }

        @Test
        @DisplayName("should use factory methods")
        void shouldUseFactory() {
            // when
            DrivingAdapter adapter =
                    DrivingAdapter.of(PKG + ".TestController", highConfidence(ElementKind.DRIVING_ADAPTER));

            // then
            assertThat(adapter.adapterType()).isEqualTo(AdapterType.REST_CONTROLLER);

            // when
            DrivingAdapter rest = DrivingAdapter.restController(
                    PKG + ".OrderController", List.of("/orders"), highConfidence(ElementKind.DRIVING_ADAPTER));

            // then
            assertThat(rest.endpoints()).containsExactly("/orders");
        }
    }

    @Nested
    @DisplayName("DrivenAdapter")
    class DrivenAdapterTest {

        @Test
        @DisplayName("should create JPA repository")
        void shouldCreateJpaRepository() {
            // when
            DrivenAdapter adapter = new DrivenAdapter(
                    ElementId.of(PKG + ".JpaOrderRepository"),
                    AdapterType.JPA_REPOSITORY,
                    List.of(),
                    null,
                    highConfidence(ElementKind.DRIVEN_ADAPTER));

            // then
            assertThat(adapter.kind()).isEqualTo(ElementKind.DRIVEN_ADAPTER);
            assertThat(adapter.isJpaRepository()).isTrue();
            assertThat(adapter.isJdbcRepository()).isFalse();
        }

        @Test
        @DisplayName("should create HTTP client")
        void shouldCreateHttpClient() {
            // when
            DrivenAdapter adapter = new DrivenAdapter(
                    ElementId.of(PKG + ".PaymentGatewayClient"),
                    AdapterType.HTTP_CLIENT,
                    List.of(),
                    null,
                    highConfidence(ElementKind.DRIVEN_ADAPTER));

            // then
            assertThat(adapter.isHttpClient()).isTrue();
            assertThat(adapter.isJpaRepository()).isFalse();
        }

        @Test
        @DisplayName("should track implemented ports")
        void shouldTrackImplementedPorts() {
            // given
            TypeId repoId = TypeId.of("com.example.OrderRepository");

            // when
            DrivenAdapter adapter = new DrivenAdapter(
                    ElementId.of(PKG + ".JpaOrderRepository"),
                    AdapterType.JPA_REPOSITORY,
                    List.of(repoId),
                    null,
                    highConfidence(ElementKind.DRIVEN_ADAPTER));

            // then
            assertThat(adapter.implementsDrivenPorts()).isTrue();
            assertThat(adapter.implementedPorts()).hasSize(1);
        }

        @Test
        @DisplayName("should use factory methods")
        void shouldUseFactory() {
            // when
            DrivenAdapter adapter =
                    DrivenAdapter.of(PKG + ".TestRepository", highConfidence(ElementKind.DRIVEN_ADAPTER));

            // then
            assertThat(adapter.adapterType()).isEqualTo(AdapterType.JPA_REPOSITORY);
            assertThat(adapter.implementsDrivenPorts()).isFalse();
        }
    }

    @Nested
    @DisplayName("UnclassifiedType")
    class UnclassifiedTypeTest {

        @Test
        @DisplayName("should create with reason")
        void shouldCreateWithReason() {
            // when
            UnclassifiedType unknown = UnclassifiedType.of(
                    PKG + ".SomeUtility", "Utility class with no clear role", highConfidence(ElementKind.UNCLASSIFIED));

            // then
            assertThat(unknown.kind()).isEqualTo(ElementKind.UNCLASSIFIED);
            assertThat(unknown.hasReason()).isTrue();
            assertThat(unknown.reason()).isEqualTo("Utility class with no clear role");
        }

        @Test
        @DisplayName("should create without reason")
        void shouldCreateWithoutReason() {
            // when
            UnclassifiedType unknown =
                    UnclassifiedType.of(PKG + ".SomeClass", highConfidence(ElementKind.UNCLASSIFIED));

            // then
            assertThat(unknown.hasReason()).isFalse();
        }

        @Test
        @DisplayName("should track remediation hints")
        void shouldTrackRemediationHints() {
            // given
            ClassificationTrace trace = ClassificationTrace.unclassified("No match", List.of());

            // when
            UnclassifiedType unknown = UnclassifiedType.of(PKG + ".SomeClass", trace);

            // then
            assertThat(unknown.hasRemediationHints()).isFalse();
        }
    }
}
