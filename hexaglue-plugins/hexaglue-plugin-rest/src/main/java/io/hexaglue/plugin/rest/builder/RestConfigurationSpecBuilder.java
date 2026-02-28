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

import com.palantir.javapoet.ClassName;
import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.model.ApplicationService;
import io.hexaglue.arch.model.Constructor;
import io.hexaglue.arch.model.DrivingPort;
import io.hexaglue.arch.model.Parameter;
import io.hexaglue.arch.model.TypeRegistry;
import io.hexaglue.plugin.rest.RestConfig;
import io.hexaglue.plugin.rest.model.ApplicationServiceBeanSpec;
import io.hexaglue.plugin.rest.model.BeanDependencySpec;
import io.hexaglue.plugin.rest.model.RestConfigurationSpec;
import io.hexaglue.plugin.rest.util.NamingConventions;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Builds a {@link RestConfigurationSpec} from driving ports and their implementing application services.
 *
 * <p>For each driving port processed by the REST plugin, this builder searches the
 * {@link TypeRegistry} for an {@link ApplicationService} that implements the port interface.
 * When found, it extracts the constructor parameters to generate {@code @Bean} method
 * specifications.
 *
 * @since 3.1.0
 */
public final class RestConfigurationSpecBuilder {

    private static final String DEFAULT_CLASS_NAME = "RestConfiguration";

    private RestConfigurationSpecBuilder() {
        /* use builder() */
    }

    /**
     * Creates a new builder.
     *
     * @return a fresh builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for RestConfigurationSpec.
     */
    public static final class Builder {

        private List<DrivingPort> drivingPorts = List.of();
        private ArchitecturalModel model;
        private String apiPackage;

        /**
         * Sets the driving ports to generate beans for.
         *
         * @param drivingPorts the driving ports
         * @return this builder
         */
        public Builder drivingPorts(List<DrivingPort> drivingPorts) {
            this.drivingPorts = drivingPorts;
            return this;
        }

        /**
         * Sets the architectural model (used to access the type registry).
         *
         * @param model the model
         * @return this builder
         */
        public Builder model(ArchitecturalModel model) {
            this.model = model;
            return this;
        }

        /**
         * Sets the API package.
         *
         * @param apiPackage the package
         * @return this builder
         */
        public Builder apiPackage(String apiPackage) {
            this.apiPackage = apiPackage;
            return this;
        }

        /**
         * Builds the RestConfigurationSpec.
         *
         * <p>For each driving port, searches the type registry for an application service
         * that implements the port interface. If found, creates a bean spec with the
         * service's constructor parameters as dependencies.
         *
         * @return the built RestConfigurationSpec
         * @throws NullPointerException if required fields are missing
         */
        public RestConfigurationSpec build() {
            Objects.requireNonNull(model, "model is required");
            Objects.requireNonNull(apiPackage, "apiPackage is required");

            String packageName = RestConfig.configPackage(apiPackage);

            Optional<TypeRegistry> registryOpt = model.typeRegistry();
            if (registryOpt.isEmpty()) {
                return new RestConfigurationSpec(DEFAULT_CLASS_NAME, packageName, List.of());
            }

            TypeRegistry registry = registryOpt.get();
            List<ApplicationService> allServices =
                    registry.all(ApplicationService.class).toList();

            List<ApplicationServiceBeanSpec> beans = new ArrayList<>();
            for (DrivingPort port : drivingPorts) {
                findImplementingService(port, allServices).ifPresent(service -> {
                    beans.add(buildBeanSpec(port, service));
                });
            }

            return new RestConfigurationSpec(DEFAULT_CLASS_NAME, packageName, beans);
        }
    }

    /**
     * Finds an application service that implements the given driving port.
     *
     * <p>Searches the service's declared interfaces for a match with the port's qualified name.
     */
    static Optional<ApplicationService> findImplementingService(DrivingPort port, List<ApplicationService> services) {
        String portQualifiedName = port.qualifiedName();
        return services.stream()
                .filter(service -> service.structure().interfaces().stream()
                        .anyMatch(iface -> iface.qualifiedName().equals(portQualifiedName)))
                .findFirst();
    }

    /**
     * Builds a bean spec for a driving port and its implementing application service.
     */
    private static ApplicationServiceBeanSpec buildBeanSpec(DrivingPort port, ApplicationService service) {
        ClassName portType = ClassName.bestGuess(port.qualifiedName());
        ClassName implType = ClassName.bestGuess(service.qualifiedName());
        String beanMethodName = NamingConventions.decapitalize(port.simpleName());

        List<BeanDependencySpec> dependencies = extractDependencies(service);

        return new ApplicationServiceBeanSpec(portType, implType, beanMethodName, dependencies);
    }

    /**
     * Extracts constructor dependencies from an application service.
     *
     * <p>Takes the first constructor's parameters. If the service has no constructors,
     * returns an empty list (no-arg instantiation).
     */
    private static List<BeanDependencySpec> extractDependencies(ApplicationService service) {
        List<Constructor> constructors = service.structure().constructors();
        if (constructors.isEmpty()) {
            return List.of();
        }

        Constructor constructor = constructors.get(0);
        List<BeanDependencySpec> dependencies = new ArrayList<>();
        for (Parameter param : constructor.parameters()) {
            ClassName paramType = ClassName.bestGuess(param.type().qualifiedName());
            String paramName = param.name();
            dependencies.add(new BeanDependencySpec(paramType, paramName));
        }
        return dependencies;
    }
}
