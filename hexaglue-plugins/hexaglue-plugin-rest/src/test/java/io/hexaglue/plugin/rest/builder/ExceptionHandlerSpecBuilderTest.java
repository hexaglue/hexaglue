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

package io.hexaglue.plugin.rest.builder;

import static org.assertj.core.api.Assertions.assertThat;

import com.palantir.javapoet.ClassName;
import io.hexaglue.plugin.rest.RestConfig;
import io.hexaglue.plugin.rest.model.ExceptionHandlerSpec;
import io.hexaglue.plugin.rest.model.ExceptionMappingSpec;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ExceptionHandlerSpecBuilder}.
 */
@DisplayName("ExceptionHandlerSpecBuilder")
class ExceptionHandlerSpecBuilderTest {

    private static final String API_PACKAGE = "com.acme.api";
    private final RestConfig config = RestConfig.defaults();

    @Nested
    @DisplayName("Heuristic mapping")
    class HeuristicMapping {

        @Test
        @DisplayName("should map NotFoundException to 404")
        void shouldMapNotFoundExceptionTo404() {
            ClassName exception = ClassName.get("com.acme.core.exception", "AccountNotFoundException");

            ExceptionMappingSpec mapping = ExceptionHandlerSpecBuilder.deriveMapping(exception);

            assertThat(mapping.httpStatus()).isEqualTo(404);
            assertThat(mapping.errorCode()).isEqualTo("NOT_FOUND");
            assertThat(mapping.handlerMethod()).isEqualTo("handleAccountNotFound");
        }

        @Test
        @DisplayName("should map AlreadyExistsException to 409")
        void shouldMapAlreadyExistsTo409() {
            ClassName exception = ClassName.get("com.acme.core.exception", "AccountAlreadyExistsException");

            ExceptionMappingSpec mapping = ExceptionHandlerSpecBuilder.deriveMapping(exception);

            assertThat(mapping.httpStatus()).isEqualTo(409);
            assertThat(mapping.errorCode()).isEqualTo("ALREADY_EXISTS");
            assertThat(mapping.handlerMethod()).isEqualTo("handleAccountAlreadyExists");
        }

        @Test
        @DisplayName("should map InsufficientFundsException to 400")
        void shouldMapInsufficientTo400() {
            ClassName exception = ClassName.get("com.acme.core.exception", "InsufficientFundsException");

            ExceptionMappingSpec mapping = ExceptionHandlerSpecBuilder.deriveMapping(exception);

            assertThat(mapping.httpStatus()).isEqualTo(400);
            assertThat(mapping.errorCode()).isEqualTo("INSUFFICIENT_RESOURCE");
            assertThat(mapping.handlerMethod()).isEqualTo("handleInsufficientFunds");
        }

        @Test
        @DisplayName("should map RejectedException to 422")
        void shouldMapRejectedTo422() {
            ClassName exception = ClassName.get("com.acme.core.exception", "TransferRejectedException");

            ExceptionMappingSpec mapping = ExceptionHandlerSpecBuilder.deriveMapping(exception);

            assertThat(mapping.httpStatus()).isEqualTo(422);
            assertThat(mapping.errorCode()).isEqualTo("REJECTED");
            assertThat(mapping.handlerMethod()).isEqualTo("handleTransferRejected");
        }

        @Test
        @DisplayName("should map unknown exception to 500")
        void shouldMapUnknownTo500() {
            ClassName exception = ClassName.get("com.acme.core.exception", "SomeWeirdProblem");

            ExceptionMappingSpec mapping = ExceptionHandlerSpecBuilder.deriveMapping(exception);

            assertThat(mapping.httpStatus()).isEqualTo(500);
            assertThat(mapping.errorCode()).isEqualTo("INTERNAL_ERROR");
            assertThat(mapping.handlerMethod()).isEqualTo("handleSomeWeirdProblem");
        }
    }

    @Nested
    @DisplayName("Custom mappings")
    class CustomMappings {

        @Test
        @DisplayName("should override heuristic status with custom mapping")
        void shouldOverrideHeuristicStatusWithCustomMapping() {
            // NotFoundException is 404 by heuristic — override to 410
            ClassName notFound = ClassName.get("com.acme.core.exception", "AccountNotFoundException");
            ExceptionMappingSpec mapping = ExceptionHandlerSpecBuilder.deriveMapping(notFound);

            RestConfig customConfig = new RestConfig(
                    null,
                    "Controller",
                    "Request",
                    "Response",
                    "/api",
                    true,
                    true,
                    true,
                    "GlobalExceptionHandler",
                    null,
                    Map.of("com.acme.core.exception.AccountNotFoundException", 410));

            ExceptionHandlerSpec spec = ExceptionHandlerSpecBuilder.builder()
                    .exceptionMappings(List.of(mapping))
                    .config(customConfig)
                    .apiPackage(API_PACKAGE)
                    .build();

            ExceptionMappingSpec overridden = spec.mappings().stream()
                    .filter(m -> m.exceptionType().simpleName().equals("AccountNotFoundException"))
                    .findFirst()
                    .orElseThrow();

            assertThat(overridden.httpStatus()).isEqualTo(410);
            // errorCode and handlerMethod preserved from heuristic
            assertThat(overridden.errorCode()).isEqualTo("NOT_FOUND");
            assertThat(overridden.handlerMethod()).isEqualTo("handleAccountNotFound");
        }

        @Test
        @DisplayName("should add custom mapping for unknown exception")
        void shouldAddCustomMappingForUnknownException() {
            RestConfig customConfig = new RestConfig(
                    null,
                    "Controller",
                    "Request",
                    "Response",
                    "/api",
                    true,
                    true,
                    true,
                    "GlobalExceptionHandler",
                    null,
                    Map.of("com.acme.infra.PaymentGatewayException", 502));

            ExceptionHandlerSpec spec = ExceptionHandlerSpecBuilder.builder()
                    .exceptionMappings(List.of())
                    .config(customConfig)
                    .apiPackage(API_PACKAGE)
                    .build();

            ExceptionMappingSpec added = spec.mappings().stream()
                    .filter(m -> m.exceptionType().simpleName().equals("PaymentGatewayException"))
                    .findFirst()
                    .orElseThrow();

            assertThat(added.httpStatus()).isEqualTo(502);
            assertThat(added.errorCode()).isEqualTo("INTERNAL_ERROR");
            assertThat(added.handlerMethod()).isEqualTo("handlePaymentGateway");
        }
    }

    @Nested
    @DisplayName("Builder behavior")
    class BuilderBehavior {

        @Test
        @DisplayName("should auto-include IllegalArgumentException and Exception catch-all")
        void shouldAutoIncludeFallbacks() {
            ClassName notFound = ClassName.get("com.acme.core.exception", "AccountNotFoundException");
            ExceptionMappingSpec mapping = ExceptionHandlerSpecBuilder.deriveMapping(notFound);

            ExceptionHandlerSpec spec = ExceptionHandlerSpecBuilder.builder()
                    .exceptionMappings(List.of(mapping))
                    .config(config)
                    .apiPackage(API_PACKAGE)
                    .build();

            assertThat(spec.mappings()).hasSize(3);
            assertThat(spec.mappings().stream().map(m -> m.exceptionType().simpleName()))
                    .containsExactly("AccountNotFoundException", "IllegalArgumentException", "Exception");
        }

        @Test
        @DisplayName("should deduplicate exceptions by qualified name")
        void shouldDeduplicateByQualifiedName() {
            ClassName notFound1 = ClassName.get("com.acme.core.exception", "AccountNotFoundException");
            ClassName notFound2 = ClassName.get("com.acme.core.exception", "AccountNotFoundException");
            ExceptionMappingSpec m1 = ExceptionHandlerSpecBuilder.deriveMapping(notFound1);
            ExceptionMappingSpec m2 = ExceptionHandlerSpecBuilder.deriveMapping(notFound2);

            ExceptionHandlerSpec spec = ExceptionHandlerSpecBuilder.builder()
                    .exceptionMappings(List.of(m1, m2))
                    .config(config)
                    .apiPackage(API_PACKAGE)
                    .build();

            // 1 domain exception + 2 auto-includes
            assertThat(spec.mappings()).hasSize(3);
        }

        @Test
        @DisplayName("should use config className and exception package")
        void shouldUseConfigClassNameAndPackage() {
            ExceptionHandlerSpec spec = ExceptionHandlerSpecBuilder.builder()
                    .exceptionMappings(List.of())
                    .config(config)
                    .apiPackage(API_PACKAGE)
                    .build();

            assertThat(spec.className()).isEqualTo("GlobalExceptionHandler");
            assertThat(spec.packageName()).isEqualTo("com.acme.api.exception");
        }
    }
}
