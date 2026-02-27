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

package io.hexaglue.plugin.rest;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.ClassificationTrace;
import io.hexaglue.arch.ElementKind;
import io.hexaglue.arch.ProjectContext;
import io.hexaglue.arch.model.AggregateRoot;
import io.hexaglue.arch.model.DrivingPort;
import io.hexaglue.arch.model.Field;
import io.hexaglue.arch.model.FieldRole;
import io.hexaglue.arch.model.Method;
import io.hexaglue.arch.model.Parameter;
import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.TypeNature;
import io.hexaglue.arch.model.TypeRegistry;
import io.hexaglue.arch.model.TypeStructure;
import io.hexaglue.arch.model.UseCase;
import io.hexaglue.arch.model.UseCase.UseCaseType;
import io.hexaglue.arch.model.index.DomainIndex;
import io.hexaglue.arch.model.index.PortIndex;
import io.hexaglue.spi.generation.ArtifactWriter;
import io.hexaglue.spi.generation.GeneratorContext;
import io.hexaglue.spi.plugin.DiagnosticReporter;
import io.hexaglue.spi.plugin.PluginConfig;
import io.hexaglue.syntax.Modifier;
import io.hexaglue.syntax.TypeRef;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * End-to-end integration tests for {@link RestPlugin}.
 *
 * <p>Invokes {@link RestPlugin#generate(GeneratorContext)} with the complete
 * banking domain model and verifies that all expected artifacts are generated.
 *
 * @since 3.1.0
 */
@DisplayName("RestPlugin integration")
class RestPluginIntegrationTest {

    @Nested
    @DisplayName("Banking case study")
    class BankingCaseStudy {

        @Test
        @DisplayName("should generate controllers for all 3 driving ports")
        void should_generate_controllers_for_all_ports() throws Exception {
            RecordingWriter writer = new RecordingWriter();
            GeneratorContext context = buildBankingContext(writer);

            new RestPlugin().generate(context);

            List<String> classNames =
                    writer.writes.stream().map(w -> w.className).toList();

            assertThat(classNames).contains("AccountController", "CustomerController", "TransferController");
        }

        @Test
        @DisplayName("should generate request DTOs for COMMAND/COMMAND_QUERY use cases")
        void should_generate_request_dtos() throws Exception {
            RecordingWriter writer = new RecordingWriter();
            GeneratorContext context = buildBankingContext(writer);

            new RestPlugin().generate(context);

            List<String> classNames =
                    writer.writes.stream().map(w -> w.className).toList();

            assertThat(classNames)
                    .contains(
                            "OpenAccountRequest",
                            "DepositRequest",
                            "WithdrawRequest",
                            "CreateCustomerRequest",
                            "UpdateCustomerRequest",
                            "InitiateTransferRequest");
        }

        @Test
        @DisplayName("should generate response DTOs for aggregate-returning use cases")
        void should_generate_response_dtos() throws Exception {
            RecordingWriter writer = new RecordingWriter();
            GeneratorContext context = buildBankingContext(writer);

            new RestPlugin().generate(context);

            List<String> classNames =
                    writer.writes.stream().map(w -> w.className).toList();

            assertThat(classNames).contains("AccountResponse", "CustomerResponse", "TransferResponse");
        }

        @Test
        @DisplayName("should generate global exception handler")
        void should_generate_exception_handler() throws Exception {
            RecordingWriter writer = new RecordingWriter();
            GeneratorContext context = buildBankingContext(writer);

            new RestPlugin().generate(context);

            List<String> classNames =
                    writer.writes.stream().map(w -> w.className).toList();

            assertThat(classNames).contains("GlobalExceptionHandler");
        }

        @Test
        @DisplayName("should generate all artifacts in correct packages")
        void should_generate_in_correct_packages() throws Exception {
            RecordingWriter writer = new RecordingWriter();
            GeneratorContext context = buildBankingContext(writer);

            new RestPlugin().generate(context);

            // Controllers go to .controller package
            assertThat(writer.writes.stream()
                            .filter(w -> w.className.endsWith("Controller"))
                            .map(w -> w.packageName))
                    .allMatch(pkg -> pkg.endsWith(".controller"));

            // DTOs go to .dto package
            assertThat(writer.writes.stream()
                            .filter(w -> w.className.endsWith("Request") || w.className.endsWith("Response"))
                            .map(w -> w.packageName))
                    .allMatch(pkg -> pkg.endsWith(".dto"));

            // Exception handler goes to .exception package
            assertThat(writer.writes.stream()
                            .filter(w -> w.className.equals("GlobalExceptionHandler"))
                            .map(w -> w.packageName))
                    .allMatch(pkg -> pkg.endsWith(".exception"));
        }

        @Test
        @DisplayName("should generate non-empty source code for all artifacts")
        void should_generate_non_empty_source() throws Exception {
            RecordingWriter writer = new RecordingWriter();
            GeneratorContext context = buildBankingContext(writer);

            new RestPlugin().generate(context);

            assertThat(writer.writes).isNotEmpty();
            assertThat(writer.writes).allSatisfy(w -> assertThat(w.content).isNotBlank());
        }

        @Test
        @DisplayName("should not generate exception handler when disabled")
        void should_not_generate_exception_handler_when_disabled() throws Exception {
            RecordingWriter writer = new RecordingWriter();
            GeneratorContext context = buildBankingContext(writer, noExceptionHandlerConfig());

            new RestPlugin().generate(context);

            List<String> classNames =
                    writer.writes.stream().map(w -> w.className).toList();

            assertThat(classNames).doesNotContain("GlobalExceptionHandler");
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("should handle empty model gracefully")
        void should_handle_empty_port_index() throws Exception {
            RecordingWriter writer = new RecordingWriter();

            TypeRegistry registry = TypeRegistry.builder().build();
            PortIndex portIndex = PortIndex.from(registry);

            ArchitecturalModel model = ArchitecturalModel.builder(ProjectContext.forTesting("test", "com.acme"))
                    .typeRegistry(registry)
                    .portIndex(portIndex)
                    .build();

            GeneratorContext context = buildContext(writer, model, defaultConfig());

            new RestPlugin().generate(context);

            assertThat(writer.writes).isEmpty();
        }
    }

    @Nested
    @DisplayName("Diagnostics")
    class Diagnostics {

        @Test
        @DisplayName("should warn when no aggregate found for port")
        void should_warn_when_no_aggregate_found() throws Exception {
            RecordingWriter writer = new RecordingWriter();
            RecordingDiagnostics diagnostics = new RecordingDiagnostics();

            // Port named "NotificationUseCases" with no matching aggregate in the domain
            UseCase sendNotification = TestUseCaseFactory.commandWithParams(
                    "sendNotification", List.of(Parameter.of("userId", TypeRef.of("java.lang.String"))));
            DrivingPort port =
                    TestUseCaseFactory.drivingPort("com.acme.port.in.NotificationUseCases", List.of(sendNotification));

            // Domain index with only banking aggregates (no "Notification" aggregate)
            DomainIndex domainIndex = BankingTestModel.domainIndex();

            TypeRegistry registry = TypeRegistry.builder()
                    .add(port)
                    .add(BankingTestModel.account())
                    .add(BankingTestModel.accountId())
                    .build();
            PortIndex portIndex = PortIndex.from(registry);

            ArchitecturalModel model = ArchitecturalModel.builder(ProjectContext.forTesting("test", "com.acme"))
                    .typeRegistry(registry)
                    .portIndex(portIndex)
                    .domainIndex(domainIndex)
                    .build();

            GeneratorContext context = buildContext(writer, model, defaultConfig(), diagnostics);

            new RestPlugin().generate(context);

            assertThat(diagnostics.warns)
                    .anyMatch(msg -> msg.contains("No aggregate found for port NotificationUseCases"));
        }

        @Test
        @DisplayName("should warn and skip generic driving port")
        void should_warn_and_skip_generic_port() throws Exception {
            RecordingWriter writer = new RecordingWriter();
            RecordingDiagnostics diagnostics = new RecordingDiagnostics();

            // Port with a method returning a type variable "T"
            Method genericMethod = new Method(
                    "process",
                    TypeRef.of("T"),
                    List.of(Parameter.of("input", TypeRef.of("java.lang.String"))),
                    Set.of(),
                    List.of(),
                    Optional.empty(),
                    List.of(),
                    Set.of(),
                    OptionalInt.empty(),
                    Optional.empty());
            UseCase genericUseCase = UseCase.of(genericMethod, UseCaseType.COMMAND_QUERY);
            DrivingPort genericPort =
                    TestUseCaseFactory.drivingPort("com.acme.port.in.GenericUseCases", List.of(genericUseCase));

            TypeRegistry registry = TypeRegistry.builder().add(genericPort).build();
            PortIndex portIndex = PortIndex.from(registry);

            ArchitecturalModel model = ArchitecturalModel.builder(ProjectContext.forTesting("test", "com.acme"))
                    .typeRegistry(registry)
                    .portIndex(portIndex)
                    .build();

            GeneratorContext context = buildContext(writer, model, defaultConfig(), diagnostics);

            new RestPlugin().generate(context);

            assertThat(diagnostics.warns)
                    .anyMatch(msg -> msg.contains("Skipping generic driving port: GenericUseCases"));

            // No controller should be generated for the generic port
            List<String> classNames =
                    writer.writes.stream().map(w -> w.className).toList();
            assertThat(classNames).noneMatch(name -> name.contains("Generic"));
        }

        @Test
        @DisplayName("should warn and skip generic port with type variable in parameterized return type")
        void should_warn_and_skip_generic_port_with_parameterized_return() throws Exception {
            RecordingWriter writer = new RecordingWriter();
            RecordingDiagnostics diagnostics = new RecordingDiagnostics();

            // Port like SearchUseCases<T> with method: List<T> search(String query)
            // T only appears inside List<T>, not as a top-level return type
            Method searchMethod = new Method(
                    "search",
                    TypeRef.parameterized("java.util.List", List.of(TypeRef.of("T"))),
                    List.of(Parameter.of("query", TypeRef.of("java.lang.String"))),
                    Set.of(),
                    List.of(),
                    Optional.empty(),
                    List.of(),
                    Set.of(),
                    OptionalInt.empty(),
                    Optional.empty());
            UseCase searchUseCase = UseCase.of(searchMethod, UseCaseType.QUERY);
            DrivingPort genericPort =
                    TestUseCaseFactory.drivingPort("com.acme.port.in.SearchUseCases", List.of(searchUseCase));

            TypeRegistry registry = TypeRegistry.builder().add(genericPort).build();
            PortIndex portIndex = PortIndex.from(registry);

            ArchitecturalModel model = ArchitecturalModel.builder(ProjectContext.forTesting("test", "com.acme"))
                    .typeRegistry(registry)
                    .portIndex(portIndex)
                    .build();

            GeneratorContext context = buildContext(writer, model, defaultConfig(), diagnostics);

            new RestPlugin().generate(context);

            assertThat(diagnostics.warns)
                    .anyMatch(msg -> msg.contains("Skipping generic driving port: SearchUseCases"));

            // No controller should be generated for the generic port
            List<String> classNames =
                    writer.writes.stream().map(w -> w.className).toList();
            assertThat(classNames).noneMatch(name -> name.contains("Search"));
        }

        @Test
        @DisplayName("should emit INFO and generate response DTO for interface aggregate")
        void should_info_polymorphic_response() throws Exception {
            RecordingWriter writer = new RecordingWriter();
            RecordingDiagnostics diagnostics = new RecordingDiagnostics();

            // Create an aggregate root with INTERFACE nature and multiple fields
            Field idField = Field.builder("id", TypeRef.of("com.acme.model.PaymentId"))
                    .wrappedType(TypeRef.of("java.lang.Long"))
                    .roles(Set.of(FieldRole.IDENTITY))
                    .build();
            Field statusField =
                    Field.builder("status", TypeRef.of("java.lang.String")).build();
            TypeStructure interfaceStructure = TypeStructure.builder(TypeNature.INTERFACE)
                    .fields(List.of(idField, statusField))
                    .build();
            AggregateRoot interfaceAggregate = AggregateRoot.builder(
                            TypeId.of("com.acme.model.Payment"),
                            interfaceStructure,
                            ClassificationTrace.highConfidence(ElementKind.AGGREGATE_ROOT, "test", "test"),
                            idField)
                    .effectiveIdentityType(TypeRef.of("java.lang.Long"))
                    .build();

            // Port with a query returning the interface aggregate
            UseCase getPayment = TestUseCaseFactory.queryWithParams(
                    "getPayment",
                    TypeRef.of("com.acme.model.Payment"),
                    List.of(Parameter.of("paymentId", TypeRef.of("com.acme.model.PaymentId"))));
            DrivingPort port = TestUseCaseFactory.drivingPort("com.acme.port.in.PaymentUseCases", List.of(getPayment));

            TypeRegistry registry = TypeRegistry.builder()
                    .add(port)
                    .add(interfaceAggregate)
                    .add(TestUseCaseFactory.identifier("com.acme.model.PaymentId", "java.lang.Long"))
                    .build();
            PortIndex portIndex = PortIndex.from(registry);
            DomainIndex domainIndex = DomainIndex.from(registry);

            ArchitecturalModel model = ArchitecturalModel.builder(ProjectContext.forTesting("test", "com.acme"))
                    .typeRegistry(registry)
                    .portIndex(portIndex)
                    .domainIndex(domainIndex)
                    .build();

            GeneratorContext context = buildContext(writer, model, defaultConfig(), diagnostics);

            new RestPlugin().generate(context);

            // Diagnostic emitted
            assertThat(diagnostics.infos)
                    .anyMatch(msg -> msg.contains("Using polymorphic response for interface Payment"));

            // Response DTO generated from interface fields only
            List<String> classNames =
                    writer.writes.stream().map(w -> w.className).toList();
            assertThat(classNames).contains("PaymentResponse");

            // Generated DTO projects declared interface fields
            WrittenFile responseDto = writer.writes.stream()
                    .filter(w -> w.className.equals("PaymentResponse"))
                    .findFirst()
                    .orElseThrow();
            assertThat(responseDto.content).contains("Long id");
            assertThat(responseDto.content).contains("String status");
            assertThat(responseDto.content).contains("from(Payment source)");
        }

        @Test
        @DisplayName("should emit INFO and generate response DTO for abstract class aggregate")
        void should_info_polymorphic_response_for_abstract_class() throws Exception {
            RecordingWriter writer = new RecordingWriter();
            RecordingDiagnostics diagnostics = new RecordingDiagnostics();

            // Create an aggregate root with abstract CLASS nature
            Field idField = Field.builder("id", TypeRef.of("com.acme.model.PaymentId"))
                    .wrappedType(TypeRef.of("java.lang.Long"))
                    .roles(Set.of(FieldRole.IDENTITY))
                    .build();
            Field amountField =
                    Field.builder("amount", TypeRef.of("java.math.BigDecimal")).build();
            TypeStructure abstractStructure = TypeStructure.builder(TypeNature.CLASS)
                    .modifiers(Set.of(Modifier.PUBLIC, Modifier.ABSTRACT))
                    .fields(List.of(idField, amountField))
                    .build();
            AggregateRoot abstractAggregate = AggregateRoot.builder(
                            TypeId.of("com.acme.model.Payment"),
                            abstractStructure,
                            ClassificationTrace.highConfidence(ElementKind.AGGREGATE_ROOT, "test", "test"),
                            idField)
                    .effectiveIdentityType(TypeRef.of("java.lang.Long"))
                    .build();

            UseCase getPayment = TestUseCaseFactory.queryWithParams(
                    "getPayment",
                    TypeRef.of("com.acme.model.Payment"),
                    List.of(Parameter.of("paymentId", TypeRef.of("com.acme.model.PaymentId"))));
            DrivingPort port = TestUseCaseFactory.drivingPort("com.acme.port.in.PaymentUseCases", List.of(getPayment));

            TypeRegistry registry = TypeRegistry.builder()
                    .add(port)
                    .add(abstractAggregate)
                    .add(TestUseCaseFactory.identifier("com.acme.model.PaymentId", "java.lang.Long"))
                    .build();
            PortIndex portIndex = PortIndex.from(registry);
            DomainIndex domainIndex = DomainIndex.from(registry);

            ArchitecturalModel model = ArchitecturalModel.builder(ProjectContext.forTesting("test", "com.acme"))
                    .typeRegistry(registry)
                    .portIndex(portIndex)
                    .domainIndex(domainIndex)
                    .build();

            GeneratorContext context = buildContext(writer, model, defaultConfig(), diagnostics);

            new RestPlugin().generate(context);

            // Diagnostic emitted for abstract class
            assertThat(diagnostics.infos)
                    .anyMatch(msg -> msg.contains("Using polymorphic response for interface Payment"));

            // Response DTO generated from declared fields only (no subtypes projected)
            WrittenFile responseDto = writer.writes.stream()
                    .filter(w -> w.className.equals("PaymentResponse"))
                    .findFirst()
                    .orElseThrow();
            assertThat(responseDto.content).contains("Long id");
            assertThat(responseDto.content).contains("BigDecimal amount");
            assertThat(responseDto.content).contains("from(Payment source)");
        }

        @Test
        @DisplayName("should warn on URL collision for same HTTP method and path")
        void should_warn_url_collision() throws Exception {
            RecordingWriter writer = new RecordingWriter();
            RecordingDiagnostics diagnostics = new RecordingDiagnostics();

            // Two queries with identity parameters that will produce the same GET /{id} pattern
            TypeRef accountRef = TypeRef.of("com.acme.model.Account");
            TypeRef accountIdRef = TypeRef.of("com.acme.model.AccountId");
            UseCase getAccount = TestUseCaseFactory.queryWithParams(
                    "getAccount", accountRef, List.of(Parameter.of("accountId", accountIdRef)));
            UseCase findAccount = TestUseCaseFactory.queryWithParams(
                    "findAccount", accountRef, List.of(Parameter.of("accountId", accountIdRef)));

            DrivingPort port = TestUseCaseFactory.drivingPort(
                    "com.acme.port.in.AccountUseCases", List.of(getAccount, findAccount));

            Field idField = Field.builder("id", accountIdRef)
                    .wrappedType(TypeRef.of("java.lang.Long"))
                    .roles(Set.of(FieldRole.IDENTITY))
                    .build();
            AggregateRoot account =
                    TestUseCaseFactory.aggregateRoot("com.acme.model.Account", idField, List.of(idField));

            TypeRegistry registry = TypeRegistry.builder()
                    .add(port)
                    .add(account)
                    .add(TestUseCaseFactory.identifier("com.acme.model.AccountId", "java.lang.Long"))
                    .build();
            PortIndex portIndex = PortIndex.from(registry);
            DomainIndex domainIndex = DomainIndex.from(registry);

            ArchitecturalModel model = ArchitecturalModel.builder(ProjectContext.forTesting("test", "com.acme"))
                    .typeRegistry(registry)
                    .portIndex(portIndex)
                    .domainIndex(domainIndex)
                    .build();

            GeneratorContext context = buildContext(writer, model, defaultConfig(), diagnostics);

            new RestPlugin().generate(context);

            assertThat(diagnostics.warns)
                    .anyMatch(msg -> msg.contains("URL collision detected for port AccountUseCases"));

            // Verify disambiguation happened in generated code
            WrittenFile controllerFile = writer.writes.stream()
                    .filter(w -> w.className.equals("AccountController"))
                    .findFirst()
                    .orElseThrow();
            assertThat(controllerFile.content).contains("/find-account/");
        }

        @Test
        @DisplayName("should disambiguate colliding endpoints with method prefix")
        void should_disambiguate_colliding_endpoints_with_method_prefix() throws Exception {
            RecordingWriter writer = new RecordingWriter();
            RecordingDiagnostics diagnostics = new RecordingDiagnostics();

            // Two queries with identity parameters that will produce the same GET /{id} pattern
            TypeRef accountRef = TypeRef.of("com.acme.model.Account");
            TypeRef accountIdRef = TypeRef.of("com.acme.model.AccountId");
            UseCase getAccount = TestUseCaseFactory.queryWithParams(
                    "getAccount", accountRef, List.of(Parameter.of("accountId", accountIdRef)));
            UseCase findAccount = TestUseCaseFactory.queryWithParams(
                    "findAccount", accountRef, List.of(Parameter.of("accountId", accountIdRef)));

            DrivingPort port = TestUseCaseFactory.drivingPort(
                    "com.acme.port.in.AccountUseCases", List.of(getAccount, findAccount));

            Field idField = Field.builder("id", accountIdRef)
                    .wrappedType(TypeRef.of("java.lang.Long"))
                    .roles(Set.of(FieldRole.IDENTITY))
                    .build();
            AggregateRoot account =
                    TestUseCaseFactory.aggregateRoot("com.acme.model.Account", idField, List.of(idField));

            TypeRegistry registry = TypeRegistry.builder()
                    .add(port)
                    .add(account)
                    .add(TestUseCaseFactory.identifier("com.acme.model.AccountId", "java.lang.Long"))
                    .build();
            PortIndex portIndex = PortIndex.from(registry);
            DomainIndex domainIndex = DomainIndex.from(registry);

            ArchitecturalModel model = ArchitecturalModel.builder(ProjectContext.forTesting("test", "com.acme"))
                    .typeRegistry(registry)
                    .portIndex(portIndex)
                    .domainIndex(domainIndex)
                    .build();

            GeneratorContext context = buildContext(writer, model, defaultConfig(), diagnostics);

            new RestPlugin().generate(context);

            WrittenFile controllerFile = writer.writes.stream()
                    .filter(w -> w.className.equals("AccountController"))
                    .findFirst()
                    .orElseThrow();

            // First endpoint keeps its original path /{id}
            assertThat(controllerFile.content).contains("/{id}");
            // Second endpoint gets disambiguated with method name prefix
            assertThat(controllerFile.content).contains("/find-account/{id}");

            // Exactly one WARN for the collision
            long collisionWarns = diagnostics.warns.stream()
                    .filter(msg -> msg.contains("URL collision detected for port AccountUseCases"))
                    .count();
            assertThat(collisionWarns).isEqualTo(1);
        }

        @Test
        @DisplayName("should merge collection endpoints with optional filter")
        void should_merge_collection_endpoints_with_optional_filter() throws Exception {
            RecordingWriter writer = new RecordingWriter();
            RecordingDiagnostics diagnostics = new RecordingDiagnostics();

            // Both methods match GetCollectionStrategy: getAll*/findAll*/list* with 0 or 1 param
            // getAllAccounts() → GET "" (default), findAllAccounts(CustomerId) → GET "" (filtered)
            TypeRef accountListRef = TypeRef.parameterized(
                    "java.util.List", List.of(TypeRef.of(BankingTestModel.MODEL_PACKAGE + ".Account")));
            TypeRef customerIdRef = TypeRef.of(BankingTestModel.MODEL_PACKAGE + ".CustomerId");

            UseCase getAllAccounts = TestUseCaseFactory.queryWithParams("getAllAccounts", accountListRef, List.of());
            UseCase findAllAccounts = TestUseCaseFactory.queryWithParams(
                    "findAllAccounts", accountListRef, List.of(Parameter.of("customerId", customerIdRef)));

            DrivingPort port = TestUseCaseFactory.drivingPort(
                    BankingTestModel.PORT_PACKAGE + ".AccountUseCases", List.of(getAllAccounts, findAllAccounts));

            TypeRegistry registry = TypeRegistry.builder()
                    .add(port)
                    .add(BankingTestModel.account())
                    .add(BankingTestModel.accountId())
                    .add(BankingTestModel.customerId())
                    .build();
            PortIndex portIndex = PortIndex.from(registry);
            DomainIndex domainIndex = DomainIndex.from(registry);

            ArchitecturalModel model = ArchitecturalModel.builder(
                            ProjectContext.forTesting("test", BankingTestModel.BASE_PACKAGE))
                    .typeRegistry(registry)
                    .portIndex(portIndex)
                    .domainIndex(domainIndex)
                    .build();

            GeneratorContext context = buildContext(writer, model, defaultConfig(), diagnostics);

            new RestPlugin().generate(context);

            WrittenFile controllerFile = writer.writes.stream()
                    .filter(w -> w.className.equals("AccountController"))
                    .findFirst()
                    .orElseThrow();

            // No collision disambiguation path
            assertThat(controllerFile.content).doesNotContain("/find-all-accounts/");

            // Contains @RequestParam(required = false)
            assertThat(controllerFile.content).contains("required = false");

            // Contains null check for conditional dispatch
            assertThat(controllerFile.content).contains("if (customerId != null)");

            // Contains filtered call with wrapped identifier
            assertThat(controllerFile.content).contains("findAllAccounts(new CustomerId(customerId))");

            // Contains default no-arg call
            assertThat(controllerFile.content).contains("getAllAccounts()");

            // INFO diagnostic emitted
            assertThat(diagnostics.infos)
                    .anyMatch(msg -> msg.contains("Merged collection endpoints: getAllAccounts + findAllAccounts"));

            // No collision WARN
            assertThat(diagnostics.warns).noneMatch(msg -> msg.contains("URL collision detected"));
        }

        @Test
        @DisplayName("should not merge collection endpoints with different element types")
        void should_not_merge_collection_endpoints_with_different_element_types() throws Exception {
            RecordingWriter writer = new RecordingWriter();
            RecordingDiagnostics diagnostics = new RecordingDiagnostics();

            // Port: getAllAccounts() → List<Account>, listTransactions() → List<Transaction>
            TypeRef accountListRef = TypeRef.parameterized(
                    "java.util.List", List.of(TypeRef.of(BankingTestModel.MODEL_PACKAGE + ".Account")));
            TypeRef transferListRef = TypeRef.parameterized(
                    "java.util.List", List.of(TypeRef.of(BankingTestModel.MODEL_PACKAGE + ".Transfer")));

            UseCase getAllAccounts = TestUseCaseFactory.queryWithParams("getAllAccounts", accountListRef, List.of());
            UseCase listTransfers = TestUseCaseFactory.queryWithParams("listTransfers", transferListRef, List.of());

            DrivingPort port = TestUseCaseFactory.drivingPort(
                    BankingTestModel.PORT_PACKAGE + ".AccountUseCases", List.of(getAllAccounts, listTransfers));

            TypeRegistry registry = TypeRegistry.builder()
                    .add(port)
                    .add(BankingTestModel.account())
                    .add(BankingTestModel.transfer())
                    .add(BankingTestModel.accountId())
                    .add(BankingTestModel.transferId())
                    .build();
            PortIndex portIndex = PortIndex.from(registry);
            DomainIndex domainIndex = DomainIndex.from(registry);

            ArchitecturalModel model = ArchitecturalModel.builder(
                            ProjectContext.forTesting("test", BankingTestModel.BASE_PACKAGE))
                    .typeRegistry(registry)
                    .portIndex(portIndex)
                    .domainIndex(domainIndex)
                    .build();

            GeneratorContext context = buildContext(writer, model, defaultConfig(), diagnostics);

            new RestPlugin().generate(context);

            // No merge INFO
            assertThat(diagnostics.infos).noneMatch(msg -> msg.contains("Merged collection endpoints"));

            // Both endpoints kept as separate (no merge happened)
            WrittenFile controllerFile = writer.writes.stream()
                    .filter(w -> w.className.equals("AccountController"))
                    .findFirst()
                    .orElseThrow();
            assertThat(controllerFile.content).contains("getAllAccounts");
            assertThat(controllerFile.content).contains("listTransfers");
        }
    }

    // === Test infrastructure ===

    private static GeneratorContext buildBankingContext(RecordingWriter writer) {
        return buildBankingContext(writer, defaultConfig());
    }

    private static GeneratorContext buildBankingContext(RecordingWriter writer, PluginConfig pluginConfig) {
        DrivingPort accountPort = BankingTestModel.accountUseCases();
        DrivingPort customerPort = BankingTestModel.customerUseCases();
        DrivingPort transferPort = BankingTestModel.transferUseCases();

        DomainIndex domainIndex = BankingTestModel.domainIndex();

        TypeRegistry registry = TypeRegistry.builder()
                .add(accountPort)
                .add(customerPort)
                .add(transferPort)
                .add(BankingTestModel.account())
                .add(BankingTestModel.customer())
                .add(BankingTestModel.transfer())
                .add(BankingTestModel.accountId())
                .add(BankingTestModel.customerId())
                .add(BankingTestModel.transferId())
                .add(BankingTestModel.money())
                .add(BankingTestModel.email())
                .build();
        PortIndex portIndex = PortIndex.from(registry);

        ArchitecturalModel model = ArchitecturalModel.builder(
                        ProjectContext.forTesting("banking", BankingTestModel.BASE_PACKAGE))
                .typeRegistry(registry)
                .portIndex(portIndex)
                .domainIndex(domainIndex)
                .build();

        return buildContext(writer, model, pluginConfig);
    }

    private static GeneratorContext buildContext(
            RecordingWriter writer, ArchitecturalModel model, PluginConfig config) {
        return buildContext(writer, model, config, new NoOpDiagnostics());
    }

    private static GeneratorContext buildContext(
            RecordingWriter writer, ArchitecturalModel model, PluginConfig config, DiagnosticReporter diagnostics) {
        return GeneratorContext.of(ArtifactWriter.of(writer), diagnostics, config, new StubPluginContext(model));
    }

    private static PluginConfig defaultConfig() {
        return new StubPluginConfig(null);
    }

    private static PluginConfig noExceptionHandlerConfig() {
        return new PluginConfig() {
            @Override
            public Optional<String> getString(String key) {
                return Optional.empty();
            }

            @Override
            public Optional<Boolean> getBoolean(String key) {
                if ("generateExceptionHandler".equals(key)) {
                    return Optional.of(false);
                }
                return Optional.empty();
            }

            @Override
            public Optional<Integer> getInteger(String key) {
                return Optional.empty();
            }
        };
    }

    /** Records all writeJavaSource calls with their content. */
    private static final class RecordingWriter implements io.hexaglue.spi.plugin.CodeWriter {
        final List<WrittenFile> writes = new ArrayList<>();

        @Override
        public void writeJavaSource(String packageName, String className, String content) {
            writes.add(new WrittenFile(packageName, className, content));
        }

        @Override
        public void writeJavaSource(String moduleId, String packageName, String className, String content) {
            writes.add(new WrittenFile(packageName, className, content));
        }

        @Override
        public boolean isMultiModule() {
            return false;
        }

        @Override
        public boolean exists(String packageName, String className) {
            return false;
        }

        @Override
        public void delete(String packageName, String className) {}

        @Override
        public Path getOutputDirectory() {
            return Path.of("target/generated-sources");
        }

        @Override
        public void writeResource(String path, String content) {}

        @Override
        public boolean resourceExists(String path) {
            return false;
        }

        @Override
        public void deleteResource(String path) {}

        @Override
        public void writeDoc(String path, String content) {}

        @Override
        public boolean docExists(String path) {
            return false;
        }

        @Override
        public void deleteDoc(String path) {}

        @Override
        public Path getDocsOutputDirectory() {
            return Path.of("target/generated-docs");
        }
    }

    record WrittenFile(String packageName, String className, String content) {}

    /** Minimal PluginConfig for tests. */
    private static final class StubPluginConfig implements PluginConfig {
        private final String targetModule;

        StubPluginConfig(String targetModule) {
            this.targetModule = targetModule;
        }

        @Override
        public Optional<String> getString(String key) {
            if ("targetModule".equals(key) && targetModule != null) {
                return Optional.of(targetModule);
            }
            return Optional.empty();
        }

        @Override
        public Optional<Boolean> getBoolean(String key) {
            return Optional.empty();
        }

        @Override
        public Optional<Integer> getInteger(String key) {
            return Optional.empty();
        }
    }

    /** Minimal PluginContext that exposes the model. */
    private static final class StubPluginContext implements io.hexaglue.spi.plugin.PluginContext {
        private final ArchitecturalModel model;

        StubPluginContext(ArchitecturalModel model) {
            this.model = model;
        }

        @Override
        public ArchitecturalModel model() {
            return model;
        }

        @Override
        public PluginConfig config() {
            return new StubPluginConfig(null);
        }

        @Override
        public io.hexaglue.spi.plugin.CodeWriter writer() {
            throw new UnsupportedOperationException();
        }

        @Override
        public DiagnosticReporter diagnostics() {
            return new NoOpDiagnostics();
        }

        @Override
        public <T> void setOutput(String key, T value) {}

        @Override
        public <T> Optional<T> getOutput(String pluginId, String key, Class<T> type) {
            return Optional.empty();
        }

        @Override
        public String currentPluginId() {
            return RestPlugin.PLUGIN_ID;
        }

        @Override
        public io.hexaglue.spi.plugin.TemplateEngine templates() {
            throw new UnsupportedOperationException();
        }
    }

    /** No-op diagnostics for tests that don't need to inspect messages. */
    private static final class NoOpDiagnostics implements DiagnosticReporter {
        @Override
        public void info(String message) {}

        @Override
        public void warn(String message) {}

        @Override
        public void error(String message) {}

        @Override
        public void error(String message, Throwable cause) {}
    }

    /** Recording diagnostics that captures messages for verification. */
    private static final class RecordingDiagnostics implements DiagnosticReporter {
        final List<String> infos = new ArrayList<>();
        final List<String> warns = new ArrayList<>();
        final List<String> errors = new ArrayList<>();

        @Override
        public void info(String message) {
            infos.add(message);
        }

        @Override
        public void warn(String message) {
            warns.add(message);
        }

        @Override
        public void error(String message) {
            errors.add(message);
        }

        @Override
        public void error(String message, Throwable cause) {
            errors.add(message);
        }
    }
}
