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

package io.hexaglue.plugin.rest.codegen;

import static org.assertj.core.api.Assertions.assertThat;

import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.TypeSpec;
import io.hexaglue.arch.model.DrivingPort;
import io.hexaglue.arch.model.index.DomainIndex;
import io.hexaglue.plugin.rest.BankingTestModel;
import io.hexaglue.plugin.rest.RestConfig;
import io.hexaglue.plugin.rest.builder.ControllerSpecBuilder;
import io.hexaglue.plugin.rest.model.ControllerSpec;
import io.hexaglue.plugin.rest.model.RequestDtoSpec;
import io.hexaglue.plugin.rest.model.ResponseDtoSpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Golden file tests for the banking case study.
 *
 * <p>Unlike {@link RestCodegenGoldenFileTest} which tests codegen in isolation
 * with manually constructed specs, these tests exercise the full pipeline:
 * domain types &rarr; builders &rarr; codegen &rarr; golden file comparison.
 *
 * @since 3.1.0
 */
@DisplayName("Banking Case Study Golden Files")
class BankingGoldenFileTest {

    private static final Path GOLDEN_DIR = Path.of("src/test/resources/golden");
    private static final Pattern GENERATED_DATE_PATTERN = Pattern.compile("date = \"[^\"]+\"");

    @Nested
    @DisplayName("Controllers")
    class Controllers {

        @Test
        @DisplayName("AccountController with 6 endpoints (GET/{id}, POST, POST/{id}/action x2, DELETE/{id}, GET)")
        void accountController() throws IOException {
            ControllerSpec spec = buildSpec(BankingTestModel.accountUseCases());
            TypeSpec typeSpec = RestControllerCodegen.generate(spec, RestConfig.defaults());
            assertGoldenFile(typeSpec, spec.packageName(), "BankingAccountController.java.txt");
        }

        @Test
        @DisplayName("CustomerController with 3 endpoints (GET/{id}, POST, PUT/{id})")
        void customerController() throws IOException {
            ControllerSpec spec = buildSpec(BankingTestModel.customerUseCases());
            TypeSpec typeSpec = RestControllerCodegen.generate(spec, RestConfig.defaults());
            assertGoldenFile(typeSpec, spec.packageName(), "BankingCustomerController.java.txt");
        }

        @Test
        @DisplayName("TransferController with 3 endpoints (GET/{id}, POST, GET/by-{prop})")
        void transferController() throws IOException {
            ControllerSpec spec = buildSpec(BankingTestModel.transferUseCases());
            TypeSpec typeSpec = RestControllerCodegen.generate(spec, RestConfig.defaults());
            assertGoldenFile(typeSpec, spec.packageName(), "BankingTransferController.java.txt");
        }

        @Test
        @DisplayName("AccountController without OpenAPI annotations")
        void accountControllerNoOpenapi() throws IOException {
            RestConfig config = new RestConfig(
                    null,
                    "Controller",
                    "Request",
                    "Response",
                    "/api",
                    false,
                    true,
                    true,
                    "GlobalExceptionHandler",
                    null,
                    Map.of());
            ControllerSpec spec = buildSpec(BankingTestModel.accountUseCases(), config);
            TypeSpec typeSpec = RestControllerCodegen.generate(spec, config);
            assertGoldenFile(typeSpec, spec.packageName(), "BankingAccountControllerNoOpenapi.java.txt");
        }
    }

    @Nested
    @DisplayName("Request DTOs")
    class RequestDtos {

        @Test
        @DisplayName("OpenAccountRequest with identity unwrap and direct fields")
        void openAccountRequest() throws IOException {
            ControllerSpec spec = buildSpec(BankingTestModel.accountUseCases());
            RequestDtoSpec dto = findRequestDto(spec, "OpenAccountRequest");
            TypeSpec typeSpec = RequestDtoCodegen.generate(dto);
            assertGoldenFile(typeSpec, dto.packageName(), "BankingOpenAccountRequest.java.txt");
        }

        @Test
        @DisplayName("InitiateTransferRequest with multiple identity unwraps and VO flatten")
        void initiateTransferRequest() throws IOException {
            ControllerSpec spec = buildSpec(BankingTestModel.transferUseCases());
            RequestDtoSpec dto = findRequestDto(spec, "InitiateTransferRequest");
            TypeSpec typeSpec = RequestDtoCodegen.generate(dto);
            assertGoldenFile(typeSpec, dto.packageName(), "BankingInitiateTransferRequest.java.txt");
        }
    }

    @Nested
    @DisplayName("Response DTOs")
    class ResponseDtos {

        @Test
        @DisplayName("AccountResponse with VO flattening through full pipeline")
        void accountResponse() throws IOException {
            ControllerSpec spec = buildSpec(BankingTestModel.accountUseCases());
            ResponseDtoSpec dto = findResponseDto(spec, "AccountResponse");
            TypeSpec typeSpec = ResponseDtoCodegen.generate(dto);
            assertGoldenFile(typeSpec, dto.packageName(), "BankingAccountResponse.java.txt");
        }

        @Test
        @DisplayName("CustomerResponse with single-field VO unwrap")
        void customerResponse() throws IOException {
            ControllerSpec spec = buildSpec(BankingTestModel.customerUseCases());
            ResponseDtoSpec dto = findResponseDto(spec, "CustomerResponse");
            TypeSpec typeSpec = ResponseDtoCodegen.generate(dto);
            assertGoldenFile(typeSpec, dto.packageName(), "BankingCustomerResponse.java.txt");
        }
    }

    // === Test infrastructure ===

    private static ControllerSpec buildSpec(DrivingPort port) {
        return buildSpec(port, RestConfig.defaults());
    }

    private static ControllerSpec buildSpec(DrivingPort port, RestConfig config) {
        DomainIndex domainIndex = BankingTestModel.domainIndex();
        return ControllerSpecBuilder.builder()
                .drivingPort(port)
                .config(config)
                .apiPackage(BankingTestModel.API_PACKAGE)
                .domainIndex(domainIndex)
                .build();
    }

    private static RequestDtoSpec findRequestDto(ControllerSpec spec, String className) {
        return spec.requestDtos().stream()
                .filter(dto -> dto.className().equals(className))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Expected request DTO '" + className + "' not found in: " + spec.requestDtos()));
    }

    private static ResponseDtoSpec findResponseDto(ControllerSpec spec, String className) {
        return spec.responseDtos().stream()
                .filter(dto -> dto.className().equals(className))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Expected response DTO '" + className + "' not found in: " + spec.responseDtos()));
    }

    private void assertGoldenFile(TypeSpec typeSpec, String packageName, String goldenFileName) throws IOException {
        JavaFile javaFile =
                JavaFile.builder(packageName, typeSpec).indent("    ").build();
        String actualCode = normalizeGeneratedCode(javaFile.toString());

        Path goldenPath =
                Path.of(System.getProperty("user.dir")).resolve(GOLDEN_DIR).resolve(goldenFileName);
        if (Files.exists(goldenPath)) {
            String expectedCode = Files.readString(goldenPath, StandardCharsets.UTF_8);
            assertThat(actualCode)
                    .as("Generated code should match golden file: %s", goldenFileName)
                    .isEqualTo(expectedCode);
        } else {
            // First run: create golden file
            Files.createDirectories(goldenPath.getParent());
            Files.writeString(goldenPath, actualCode, StandardCharsets.UTF_8);
            System.out.println("Golden file created: " + goldenPath);
            System.out.println("Please review and commit the golden file.");
        }
    }

    private String normalizeGeneratedCode(String code) {
        return GENERATED_DATE_PATTERN.matcher(code).replaceAll("date = \"2026-01-01T00:00:00Z\"");
    }
}
