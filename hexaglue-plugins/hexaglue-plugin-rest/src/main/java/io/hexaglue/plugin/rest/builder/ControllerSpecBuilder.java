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
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import io.hexaglue.arch.model.DrivingPort;
import io.hexaglue.arch.model.Field;
import io.hexaglue.arch.model.Identifier;
import io.hexaglue.arch.model.Parameter;
import io.hexaglue.arch.model.UseCase;
import io.hexaglue.arch.model.ValueObject;
import io.hexaglue.arch.model.index.DomainIndex;
import io.hexaglue.plugin.rest.RestConfig;
import io.hexaglue.plugin.rest.model.BindingKind;
import io.hexaglue.plugin.rest.model.ControllerSpec;
import io.hexaglue.plugin.rest.model.EndpointSpec;
import io.hexaglue.plugin.rest.model.HttpMapping;
import io.hexaglue.plugin.rest.model.ParameterBindingSpec;
import io.hexaglue.plugin.rest.model.RequestDtoSpec;
import io.hexaglue.plugin.rest.strategy.HttpVerbStrategyFactory;
import io.hexaglue.plugin.rest.util.NamingConventions;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Builds a {@link ControllerSpec} from a {@link DrivingPort}.
 *
 * <p>Orchestrates HTTP verb derivation, endpoint construction, and
 * request DTO generation when a {@link DomainIndex} is provided.
 *
 * @since 3.1.0
 */
public final class ControllerSpecBuilder {

    private static final ClassName RESPONSE_ENTITY = ClassName.get("org.springframework.http", "ResponseEntity");
    private static final ClassName WILDCARD_TYPE = ClassName.get("java.lang", "Object");

    private ControllerSpecBuilder() {
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
     * Builder for ControllerSpec.
     */
    public static final class Builder {

        private DrivingPort drivingPort;
        private RestConfig config;
        private String apiPackage;
        private DomainIndex domainIndex;

        /**
         * Sets the driving port to generate a controller from.
         *
         * @param port the driving port
         * @return this builder
         */
        public Builder drivingPort(DrivingPort port) {
            this.drivingPort = port;
            return this;
        }

        /**
         * Sets the REST plugin configuration.
         *
         * @param config the config
         * @return this builder
         */
        public Builder config(RestConfig config) {
            this.config = config;
            return this;
        }

        /**
         * Sets the API package for generated code.
         *
         * @param apiPackage the package
         * @return this builder
         */
        public Builder apiPackage(String apiPackage) {
            this.apiPackage = apiPackage;
            return this;
        }

        /**
         * Sets the domain index for identifier/VO resolution.
         *
         * <p>When provided, enables request DTO generation and parameter
         * binding construction for COMMAND/COMMAND_QUERY use cases.
         *
         * @param index the domain index (nullable for Phase 1 backward compat)
         * @return this builder
         */
        public Builder domainIndex(DomainIndex index) {
            this.domainIndex = index;
            return this;
        }

        /**
         * Builds the controller spec.
         *
         * @return the built ControllerSpec
         * @throws NullPointerException if required fields are missing
         */
        public ControllerSpec build() {
            Objects.requireNonNull(drivingPort, "drivingPort is required");
            Objects.requireNonNull(config, "config is required");
            Objects.requireNonNull(apiPackage, "apiPackage is required");

            String portSimpleName = drivingPort.id().simpleName();
            String stripped = NamingConventions.stripSuffix(portSimpleName);
            String className = stripped + config.controllerSuffix();
            String packageName = apiPackage + ".controller";

            String resourceName = NamingConventions.pluralize(NamingConventions.toKebabCase(stripped));
            String basePath = config.basePath() + "/" + resourceName;

            String tagName = NamingConventions.capitalize(NamingConventions.pluralize(stripped));
            String tagDescription = stripped + " management operations";

            ClassName portType = ClassName.get(drivingPort.id().packageName(), portSimpleName);
            String dtoPackage = apiPackage + ".dto";

            HttpVerbStrategyFactory strategyFactory = new HttpVerbStrategyFactory();
            List<EndpointSpec> endpoints = new ArrayList<>();
            List<RequestDtoSpec> requestDtos = new ArrayList<>();

            for (UseCase useCase : drivingPort.useCases()) {
                HttpMapping mapping = strategyFactory.derive(useCase, null, basePath);
                String summary = deriveOperationSummary(useCase.name());
                TypeName returnType = ParameterizedTypeName.get(RESPONSE_ENTITY, WILDCARD_TYPE);

                String requestDtoRef = null;
                List<ParameterBindingSpec> parameterBindings = List.of();

                if (domainIndex != null) {
                    Optional<RequestDtoSpec> dtoSpec =
                            RequestDtoSpecBuilder.build(useCase, mapping, domainIndex, config, dtoPackage);
                    if (dtoSpec.isPresent()) {
                        requestDtos.add(dtoSpec.get());
                        requestDtoRef = dtoSpec.get().className();
                        parameterBindings = buildParameterBindings(useCase, mapping, domainIndex);
                    }
                }

                endpoints.add(new EndpointSpec(
                        useCase.name(),
                        mapping.httpMethod(),
                        mapping.path(),
                        summary,
                        returnType,
                        mapping.responseStatus(),
                        requestDtoRef,
                        null,
                        mapping.pathVariables(),
                        mapping.queryParams(),
                        List.of(),
                        useCase.type(),
                        parameterBindings));
            }

            return new ControllerSpec(
                    className,
                    packageName,
                    basePath,
                    portType,
                    null,
                    tagName,
                    tagDescription,
                    endpoints,
                    requestDtos,
                    List.of(),
                    List.of());
        }

        private List<ParameterBindingSpec> buildParameterBindings(
                UseCase useCase, HttpMapping mapping, DomainIndex domainIndex) {
            List<ParameterBindingSpec> bindings = new ArrayList<>();
            for (Parameter param : useCase.method().parameters()) {
                TypeName domainType = DtoFieldMapper.toTypeName(param.type());

                Optional<Identifier> id = DtoFieldMapper.findIdentifier(param.type(), domainIndex);
                if (id.isPresent()) {
                    bindings.add(new ParameterBindingSpec(
                            param.name(), domainType, BindingKind.CONSTRUCTOR_WRAP, List.of(param.name())));
                    continue;
                }

                Optional<ValueObject> vo = DtoFieldMapper.findValueObject(param.type(), domainIndex);
                if (vo.isPresent() && vo.get().isSingleValue()) {
                    bindings.add(new ParameterBindingSpec(
                            param.name(), domainType, BindingKind.CONSTRUCTOR_WRAP, List.of(param.name())));
                    continue;
                }
                if (vo.isPresent() && !vo.get().isSingleValue() && config.flattenValueObjects()) {
                    List<String> fieldNames = vo.get().structure().fields().stream()
                            .map(Field::name)
                            .toList();
                    bindings.add(
                            new ParameterBindingSpec(param.name(), domainType, BindingKind.FACTORY_WRAP, fieldNames));
                    continue;
                }

                // Direct
                bindings.add(
                        new ParameterBindingSpec(param.name(), domainType, BindingKind.DIRECT, List.of(param.name())));
            }
            return bindings;
        }

        /**
         * Derives an OpenAPI operation summary from a camelCase method name.
         *
         * <p>Splits camelCase into words, capitalizes the first word,
         * and lowercases the rest.
         */
        private static String deriveOperationSummary(String methodName) {
            String kebab = NamingConventions.toKebabCase(methodName);
            String[] words = kebab.split("-");
            if (words.length == 0) {
                return methodName;
            }
            StringBuilder sb = new StringBuilder();
            sb.append(NamingConventions.capitalize(words[0]));
            for (int i = 1; i < words.length; i++) {
                sb.append(' ').append(words[i]);
            }
            return sb.toString();
        }
    }
}
