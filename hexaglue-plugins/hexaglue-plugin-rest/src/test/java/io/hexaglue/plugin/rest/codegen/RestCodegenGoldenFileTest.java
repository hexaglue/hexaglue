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

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.TypeSpec;
import io.hexaglue.arch.model.DrivingPort;
import io.hexaglue.arch.model.Field;
import io.hexaglue.arch.model.Identifier;
import io.hexaglue.arch.model.Parameter;
import io.hexaglue.arch.model.UseCase;
import io.hexaglue.arch.model.ValueObject;
import io.hexaglue.arch.model.index.DomainIndex;
import io.hexaglue.plugin.rest.RestConfig;
import io.hexaglue.plugin.rest.TestUseCaseFactory;
import io.hexaglue.plugin.rest.builder.ControllerSpecBuilder;
import io.hexaglue.plugin.rest.model.ControllerSpec;
import io.hexaglue.plugin.rest.model.DtoFieldSpec;
import io.hexaglue.plugin.rest.model.ProjectionKind;
import io.hexaglue.plugin.rest.model.RequestDtoSpec;
import io.hexaglue.plugin.rest.model.ValidationKind;
import io.hexaglue.syntax.TypeRef;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Golden file tests for REST code generation.
 *
 * <p>Validates that generated controllers produce stable, expected output.
 *
 * @since 3.1.0
 */
@DisplayName("REST Codegen Golden Files")
class RestCodegenGoldenFileTest {

    private static final Path GOLDEN_DIR = Path.of("src/test/resources/golden");
    private static final String API_PACKAGE = "com.acme.api";
    private static final Pattern GENERATED_DATE_PATTERN = Pattern.compile("date = \"[^\"]+\"");

    @Test
    @DisplayName("Simple controller with 2 endpoints (GET + POST)")
    void simpleController() throws IOException {
        UseCase query = TestUseCaseFactory.query("getAccount");
        UseCase command = TestUseCaseFactory.command("closeAccount");
        DrivingPort port =
                TestUseCaseFactory.drivingPort("com.acme.core.port.in.AccountUseCases", List.of(query, command));

        RestConfig config = RestConfig.defaults();
        ControllerSpec spec = ControllerSpecBuilder.builder()
                .drivingPort(port)
                .config(config)
                .apiPackage(API_PACKAGE)
                .build();

        TypeSpec typeSpec = RestControllerCodegen.generate(spec, config);
        assertGoldenFile(typeSpec, API_PACKAGE + ".controller", "SimpleController.java.txt");
    }

    @Test
    @DisplayName("OpenAccountRequest DTO with 3 fields")
    void openAccountRequestDto() throws IOException {
        RequestDtoSpec spec = new RequestDtoSpec(
                "OpenAccountRequest",
                API_PACKAGE + ".dto",
                List.of(
                        new DtoFieldSpec(
                                "customerId",
                                ClassName.get("java.lang", "Long"),
                                "customerId",
                                null,
                                ValidationKind.NOT_NULL,
                                ProjectionKind.IDENTITY_UNWRAP),
                        new DtoFieldSpec(
                                "type",
                                ClassName.get("com.acme.core.model", "AccountType"),
                                "type",
                                null,
                                ValidationKind.NOT_NULL,
                                ProjectionKind.DIRECT),
                        new DtoFieldSpec(
                                "accountNumber",
                                ClassName.get(String.class),
                                "accountNumber",
                                null,
                                ValidationKind.NOT_BLANK,
                                ProjectionKind.DIRECT)),
                "openAccount");
        TypeSpec typeSpec = RequestDtoCodegen.generate(spec);
        assertGoldenFile(typeSpec, API_PACKAGE + ".dto", "OpenAccountRequest.java.txt");
    }

    @Test
    @DisplayName("DepositRequest DTO with flattened VO")
    void depositRequestDto() throws IOException {
        RequestDtoSpec spec = new RequestDtoSpec(
                "DepositRequest",
                API_PACKAGE + ".dto",
                List.of(
                        new DtoFieldSpec(
                                "amount",
                                ClassName.get("java.math", "BigDecimal"),
                                "amount",
                                null,
                                ValidationKind.NOT_NULL,
                                ProjectionKind.VALUE_OBJECT_FLATTEN),
                        new DtoFieldSpec(
                                "currency",
                                ClassName.get(String.class),
                                "amount",
                                null,
                                ValidationKind.NOT_NULL,
                                ProjectionKind.VALUE_OBJECT_FLATTEN)),
                "deposit");
        TypeSpec typeSpec = RequestDtoCodegen.generate(spec);
        assertGoldenFile(typeSpec, API_PACKAGE + ".dto", "DepositRequest.java.txt");
    }

    @Test
    @DisplayName("Controller with request DTO and parameter bindings")
    void controllerWithRequestDto() throws IOException {
        Identifier customerId = TestUseCaseFactory.identifier("com.acme.core.model.CustomerId", "java.lang.Long");
        ValueObject money = TestUseCaseFactory.multiFieldValueObject(
                "com.acme.core.model.Money",
                List.of(
                        Field.of("amount", TypeRef.of("java.math.BigDecimal")),
                        Field.of("currency", TypeRef.of("java.lang.String"))));
        DomainIndex domainIndex = TestUseCaseFactory.domainIndex(customerId, money);

        UseCase openAccount = TestUseCaseFactory.commandQueryWithParams(
                "openAccount",
                TypeRef.of("java.lang.Object"),
                List.of(
                        Parameter.of("customerId", TypeRef.of("com.acme.core.model.CustomerId")),
                        Parameter.of("type", TypeRef.of("com.acme.core.model.AccountType")),
                        Parameter.of("accountNumber", TypeRef.of("java.lang.String"))));
        UseCase deposit = TestUseCaseFactory.commandWithParams(
                "deposit",
                List.of(
                        Parameter.of("accountId", TypeRef.of("com.acme.core.model.CustomerId")),
                        Parameter.of("amount", TypeRef.of("com.acme.core.model.Money"))));
        DrivingPort port =
                TestUseCaseFactory.drivingPort("com.acme.core.port.in.AccountUseCases", List.of(openAccount, deposit));

        RestConfig config = RestConfig.defaults();
        ControllerSpec spec = ControllerSpecBuilder.builder()
                .drivingPort(port)
                .config(config)
                .apiPackage(API_PACKAGE)
                .domainIndex(domainIndex)
                .build();

        TypeSpec typeSpec = RestControllerCodegen.generate(spec, config);
        assertGoldenFile(typeSpec, API_PACKAGE + ".controller", "ControllerWithDto.java.txt");
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
