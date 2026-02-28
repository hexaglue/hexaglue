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

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.ClassificationTrace;
import io.hexaglue.arch.ElementKind;
import io.hexaglue.arch.ProjectContext;
import io.hexaglue.arch.model.ApplicationService;
import io.hexaglue.arch.model.ArchType;
import io.hexaglue.arch.model.Constructor;
import io.hexaglue.arch.model.DrivingPort;
import io.hexaglue.arch.model.Parameter;
import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.TypeNature;
import io.hexaglue.arch.model.TypeRegistry;
import io.hexaglue.arch.model.TypeStructure;
import io.hexaglue.plugin.rest.TestUseCaseFactory;
import io.hexaglue.plugin.rest.model.ApplicationServiceBeanSpec;
import io.hexaglue.plugin.rest.model.RestConfigurationSpec;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link RestConfigurationSpecBuilder}.
 */
@DisplayName("RestConfigurationSpecBuilder")
class RestConfigurationSpecBuilderTest {

    private static final String API_PACKAGE = "com.acme.api";

    @Nested
    @DisplayName("Bean discovery")
    class BeanDiscovery {

        @Test
        @DisplayName("should create bean spec when ApplicationService implements DrivingPort")
        void shouldCreateBeanSpecWhenServiceImplementsPort() {
            DrivingPort port = TestUseCaseFactory.drivingPort(
                    "com.acme.core.port.in.TaskUseCases", List.of(TestUseCaseFactory.command("createTask")));

            ApplicationService service = applicationService(
                    "com.acme.core.service.TaskService",
                    List.of(TypeRef.of("com.acme.core.port.in.TaskUseCases")),
                    List.of(Parameter.of("taskRepository", TypeRef.of("com.acme.core.port.out.TaskRepository"))));

            ArchitecturalModel model = modelWith(port, service);

            RestConfigurationSpec spec = RestConfigurationSpecBuilder.builder()
                    .drivingPorts(List.of(port))
                    .model(model)
                    .apiPackage(API_PACKAGE)
                    .build();

            assertThat(spec.beans()).hasSize(1);
            ApplicationServiceBeanSpec bean = spec.beans().get(0);
            assertThat(bean.portType().simpleName()).isEqualTo("TaskUseCases");
            assertThat(bean.implementationType().simpleName()).isEqualTo("TaskService");
            assertThat(bean.beanMethodName()).isEqualTo("taskUseCases");
            assertThat(bean.dependencies()).hasSize(1);
            assertThat(bean.dependencies().get(0).type().simpleName()).isEqualTo("TaskRepository");
            assertThat(bean.dependencies().get(0).paramName()).isEqualTo("taskRepository");
        }

        @Test
        @DisplayName("should return empty beans when no ApplicationService implements port")
        void shouldReturnEmptyBeansWhenNoServiceImplementsPort() {
            DrivingPort port = TestUseCaseFactory.drivingPort(
                    "com.acme.core.port.in.TaskUseCases", List.of(TestUseCaseFactory.command("createTask")));

            ArchitecturalModel model = modelWith(port);

            RestConfigurationSpec spec = RestConfigurationSpecBuilder.builder()
                    .drivingPorts(List.of(port))
                    .model(model)
                    .apiPackage(API_PACKAGE)
                    .build();

            assertThat(spec.beans()).isEmpty();
        }

        @Test
        @DisplayName("should handle multiple driving ports with their services")
        void shouldHandleMultipleDrivingPorts() {
            DrivingPort taskPort = TestUseCaseFactory.drivingPort(
                    "com.acme.core.port.in.TaskUseCases", List.of(TestUseCaseFactory.command("createTask")));
            DrivingPort orderPort = TestUseCaseFactory.drivingPort(
                    "com.acme.core.port.in.OrderUseCases", List.of(TestUseCaseFactory.command("placeOrder")));

            ApplicationService taskService = applicationService(
                    "com.acme.core.service.TaskService",
                    List.of(TypeRef.of("com.acme.core.port.in.TaskUseCases")),
                    List.of(Parameter.of("taskRepository", TypeRef.of("com.acme.core.port.out.TaskRepository"))));
            ApplicationService orderService = applicationService(
                    "com.acme.core.service.OrderService",
                    List.of(TypeRef.of("com.acme.core.port.in.OrderUseCases")),
                    List.of(
                            Parameter.of("orderRepository", TypeRef.of("com.acme.core.port.out.OrderRepository")),
                            Parameter.of("paymentGateway", TypeRef.of("com.acme.core.port.out.PaymentGateway"))));

            ArchitecturalModel model = modelWith(taskPort, orderPort, taskService, orderService);

            RestConfigurationSpec spec = RestConfigurationSpecBuilder.builder()
                    .drivingPorts(List.of(taskPort, orderPort))
                    .model(model)
                    .apiPackage(API_PACKAGE)
                    .build();

            assertThat(spec.beans()).hasSize(2);
            assertThat(spec.beans().get(0).beanMethodName()).isEqualTo("taskUseCases");
            assertThat(spec.beans().get(0).dependencies()).hasSize(1);
            assertThat(spec.beans().get(1).beanMethodName()).isEqualTo("orderUseCases");
            assertThat(spec.beans().get(1).dependencies()).hasSize(2);
        }

        @Test
        @DisplayName("should handle service with no-arg constructor")
        void shouldHandleServiceWithNoArgConstructor() {
            DrivingPort port = TestUseCaseFactory.drivingPort(
                    "com.acme.core.port.in.TaskUseCases", List.of(TestUseCaseFactory.command("createTask")));

            ApplicationService service = applicationService(
                    "com.acme.core.service.TaskService",
                    List.of(TypeRef.of("com.acme.core.port.in.TaskUseCases")),
                    List.of());

            ArchitecturalModel model = modelWith(port, service);

            RestConfigurationSpec spec = RestConfigurationSpecBuilder.builder()
                    .drivingPorts(List.of(port))
                    .model(model)
                    .apiPackage(API_PACKAGE)
                    .build();

            assertThat(spec.beans()).hasSize(1);
            assertThat(spec.beans().get(0).dependencies()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Spec metadata")
    class SpecMetadata {

        @Test
        @DisplayName("should use RestConfiguration as class name")
        void shouldUseRestConfigurationAsClassName() {
            DrivingPort port = TestUseCaseFactory.drivingPort(
                    "com.acme.core.port.in.TaskUseCases", List.of(TestUseCaseFactory.command("createTask")));

            ArchitecturalModel model = modelWith(port);

            RestConfigurationSpec spec = RestConfigurationSpecBuilder.builder()
                    .drivingPorts(List.of(port))
                    .model(model)
                    .apiPackage(API_PACKAGE)
                    .build();

            assertThat(spec.className()).isEqualTo("RestConfiguration");
        }

        @Test
        @DisplayName("should use apiPackage + .config as package name")
        void shouldUseConfigPackage() {
            DrivingPort port = TestUseCaseFactory.drivingPort(
                    "com.acme.core.port.in.TaskUseCases", List.of(TestUseCaseFactory.command("createTask")));

            ArchitecturalModel model = modelWith(port);

            RestConfigurationSpec spec = RestConfigurationSpecBuilder.builder()
                    .drivingPorts(List.of(port))
                    .model(model)
                    .apiPackage(API_PACKAGE)
                    .build();

            assertThat(spec.packageName()).isEqualTo("com.acme.api.config");
        }
    }

    // --- Test helpers ---

    private static ApplicationService applicationService(
            String qualifiedName, List<TypeRef> interfaces, List<Parameter> constructorParams) {
        TypeStructure.Builder structureBuilder =
                TypeStructure.builder(TypeNature.CLASS).interfaces(interfaces);
        if (!constructorParams.isEmpty()) {
            structureBuilder.constructors(List.of(Constructor.of(constructorParams)));
        }
        return ApplicationService.of(
                TypeId.of(qualifiedName),
                structureBuilder.build(),
                ClassificationTrace.highConfidence(
                        ElementKind.APPLICATION_SERVICE, "test", "test application service"));
    }

    private static ArchitecturalModel modelWith(ArchType... types) {
        TypeRegistry.Builder registryBuilder = TypeRegistry.builder();
        for (ArchType type : types) {
            registryBuilder.add(type);
        }
        ProjectContext project = ProjectContext.forTesting("test", "com.acme");
        return ArchitecturalModel.builder(project)
                .typeRegistry(registryBuilder.build())
                .build();
    }
}
