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

import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.model.DrivingPort;
import io.hexaglue.arch.model.Field;
import io.hexaglue.arch.model.Identifier;
import io.hexaglue.arch.model.Parameter;
import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.TypeStructure;
import io.hexaglue.arch.model.UseCase;
import io.hexaglue.arch.model.UseCase.UseCaseType;
import io.hexaglue.arch.model.ValueObject;
import io.hexaglue.arch.model.index.DomainIndex;
import io.hexaglue.arch.model.index.ModuleIndex;
import io.hexaglue.arch.model.index.ModuleRole;
import io.hexaglue.arch.model.index.ModuleRouting;
import io.hexaglue.arch.model.index.PortIndex;
import io.hexaglue.plugin.rest.builder.ControllerSpecBuilder;
import io.hexaglue.plugin.rest.builder.DtoFieldMapper;
import io.hexaglue.plugin.rest.builder.ExceptionHandlerSpecBuilder;
import io.hexaglue.plugin.rest.builder.RestConfigurationSpecBuilder;
import io.hexaglue.plugin.rest.codegen.ExceptionHandlerCodegen;
import io.hexaglue.plugin.rest.codegen.RequestDtoCodegen;
import io.hexaglue.plugin.rest.codegen.ResponseDtoCodegen;
import io.hexaglue.plugin.rest.codegen.RestConfigurationCodegen;
import io.hexaglue.plugin.rest.codegen.RestControllerCodegen;
import io.hexaglue.plugin.rest.model.BindingKind;
import io.hexaglue.plugin.rest.model.ControllerSpec;
import io.hexaglue.plugin.rest.model.EndpointSpec;
import io.hexaglue.plugin.rest.model.ExceptionHandlerSpec;
import io.hexaglue.plugin.rest.model.ExceptionMappingSpec;
import io.hexaglue.plugin.rest.model.ParameterBindingSpec;
import io.hexaglue.plugin.rest.model.QueryParamSpec;
import io.hexaglue.plugin.rest.model.RequestDtoSpec;
import io.hexaglue.plugin.rest.model.ResponseDtoSpec;
import io.hexaglue.plugin.rest.model.RestConfigurationSpec;
import io.hexaglue.plugin.rest.util.NamingConventions;
import io.hexaglue.spi.generation.ArtifactWriter;
import io.hexaglue.spi.generation.GeneratorContext;
import io.hexaglue.spi.generation.GeneratorPlugin;
import io.hexaglue.spi.plugin.DiagnosticReporter;
import io.hexaglue.spi.plugin.PluginConfig;
import io.hexaglue.syntax.Modifier;
import io.hexaglue.syntax.TypeRef;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * REST plugin for HexaGlue.
 *
 * <p>Generates Spring MVC REST controllers from driving ports using JavaPoet
 * for type-safe code generation. Each driving port with use cases produces
 * a {@code @RestController} class.
 *
 * <p>Phase 1 (MVP) generates controllers with:
 * <ul>
 *   <li>{@code @RestController}, {@code @RequestMapping}, {@code @Tag}</li>
 *   <li>Constructor injection of the driving port</li>
 *   <li>Endpoints using FallbackStrategy (QUERY to GET, COMMAND to POST)</li>
 * </ul>
 *
 * @since 3.1.0
 */
public final class RestPlugin implements GeneratorPlugin {

    /** Plugin identifier. */
    public static final String PLUGIN_ID = "io.hexaglue.plugin.rest";

    /** Indentation for generated code (4 spaces). */
    private static final String INDENT = "    ";

    @Override
    public String id() {
        return PLUGIN_ID;
    }

    @Override
    public void generate(GeneratorContext context) throws Exception {
        PluginConfig pluginConfig = context.config();
        ArtifactWriter writer = context.writer();
        DiagnosticReporter diagnostics = context.diagnostics();
        ArchitecturalModel model = context.model().orElse(null);

        if (model == null) {
            diagnostics.error("ArchitecturalModel is required for REST code generation.");
            return;
        }

        PortIndex portIndex = model.portIndex().orElse(null);
        if (portIndex == null) {
            diagnostics.error("PortIndex is required for REST code generation.");
            return;
        }

        RestConfig config = RestConfig.from(pluginConfig);

        String effectiveTargetModule = resolveEffectiveTargetModule(config, model, writer, diagnostics);
        if (effectiveTargetModule != null) {
            diagnostics.info("REST effectiveTargetModule: " + effectiveTargetModule);
        }

        String basePackage = model.project().basePackage();
        String apiPackage = config.apiPackage() != null ? config.apiPackage() : deriveApiPackage(basePackage);

        DomainIndex domainIndex = model.domainIndex().orElse(null);

        List<DrivingPort> drivingPorts =
                portIndex.drivingPorts().filter(DrivingPort::hasUseCases).toList();

        if (drivingPorts.isEmpty()) {
            diagnostics.info("No driving ports with use cases found. Skipping REST generation.");
            return;
        }

        int controllerCount = 0;
        int dtoCount = 0;
        List<ExceptionMappingSpec> allExceptionMappings = new ArrayList<>();

        for (DrivingPort port : drivingPorts) {
            if (hasTypeVariables(port)) {
                diagnostics.warn("Skipping generic driving port: " + port.id().simpleName()
                        + " (type parameters not supported)");
                continue;
            }

            ControllerSpec spec = ControllerSpecBuilder.builder()
                    .drivingPort(port)
                    .config(config)
                    .apiPackage(apiPackage)
                    .domainIndex(domainIndex)
                    .build();

            emitPortDiagnostics(port, spec, domainIndex, diagnostics);
            if (domainIndex != null) {
                spec = mergeCollectionEndpoints(port, spec, domainIndex, diagnostics);
            }
            spec = resolveUrlCollisions(port, spec, diagnostics);

            TypeSpec typeSpec = RestControllerCodegen.generate(spec, config);
            String source = toJavaSource(spec.packageName(), typeSpec);
            writeJavaSource(writer, effectiveTargetModule, spec.packageName(), spec.className(), source, diagnostics);
            controllerCount++;

            // Generate request DTOs
            for (RequestDtoSpec requestDto : spec.requestDtos()) {
                TypeSpec dtoTypeSpec = RequestDtoCodegen.generate(requestDto);
                String dtoSource = toJavaSource(requestDto.packageName(), dtoTypeSpec);
                writeJavaSource(
                        writer,
                        effectiveTargetModule,
                        requestDto.packageName(),
                        requestDto.className(),
                        dtoSource,
                        diagnostics);
                dtoCount++;
            }

            // Generate response DTOs
            for (ResponseDtoSpec responseDto : spec.responseDtos()) {
                TypeSpec dtoTypeSpec = ResponseDtoCodegen.generate(responseDto);
                String dtoSource = toJavaSource(responseDto.packageName(), dtoTypeSpec);
                writeJavaSource(
                        writer,
                        effectiveTargetModule,
                        responseDto.packageName(),
                        responseDto.className(),
                        dtoSource,
                        diagnostics);
                dtoCount++;
            }

            // Collect exception mappings for global handler
            allExceptionMappings.addAll(spec.exceptionMappings());
        }

        // Generate global exception handler (once, aggregated across all controllers)
        if (config.generateExceptionHandler()) {
            ExceptionHandlerSpec handlerSpec = ExceptionHandlerSpecBuilder.builder()
                    .exceptionMappings(allExceptionMappings)
                    .config(config)
                    .apiPackage(apiPackage)
                    .build();

            TypeSpec handlerTypeSpec = ExceptionHandlerCodegen.generate(handlerSpec);
            String handlerSource = toJavaSource(handlerSpec.packageName(), handlerTypeSpec);
            writeJavaSource(
                    writer,
                    effectiveTargetModule,
                    handlerSpec.packageName(),
                    handlerSpec.className(),
                    handlerSource,
                    diagnostics);
        }

        // Generate @Configuration class with @Bean methods for application services
        if (config.generateConfiguration()) {
            RestConfigurationSpec configSpec = RestConfigurationSpecBuilder.builder()
                    .drivingPorts(drivingPorts)
                    .model(model)
                    .apiPackage(apiPackage)
                    .build();

            if (!configSpec.beans().isEmpty()) {
                TypeSpec configTypeSpec = RestConfigurationCodegen.generate(configSpec);
                String configSource = toJavaSource(configSpec.packageName(), configTypeSpec);
                writeJavaSource(
                        writer,
                        effectiveTargetModule,
                        configSpec.packageName(),
                        configSpec.className(),
                        configSource,
                        diagnostics);
                diagnostics.info("REST plugin generated @Configuration class with "
                        + configSpec.beans().size() + " @Bean method(s).");
            }
        }

        diagnostics.info("REST plugin generated " + controllerCount + " controller(s) and " + dtoCount + " DTO(s).");
    }

    private static String toJavaSource(String packageName, TypeSpec typeSpec) {
        return JavaFile.builder(packageName, typeSpec).indent(INDENT).build().toString();
    }

    private static void writeJavaSource(
            ArtifactWriter writer,
            String effectiveTargetModule,
            String packageName,
            String className,
            String source,
            DiagnosticReporter diagnostics) {
        try {
            if (effectiveTargetModule != null && writer.isMultiModule()) {
                writer.writeJavaSource(effectiveTargetModule, packageName, className, source);
            } else {
                writer.writeJavaSource(packageName, className, source);
            }
        } catch (IOException e) {
            diagnostics.error("Failed to write " + packageName + "." + className, e);
        }
    }

    /**
     * Resolves the effective target module for REST code routing.
     *
     * <p>Priority: explicit config &gt; auto-routing by {@link ModuleRole#API} &gt; null.
     *
     * @param config the REST configuration
     * @param model the architectural model
     * @param writer the artifact writer
     * @param diagnostics the diagnostic reporter
     * @return the target module ID, or null if not multi-module or not resolvable
     * @since 3.1.0
     */
    private String resolveEffectiveTargetModule(
            RestConfig config, ArchitecturalModel model, ArtifactWriter writer, DiagnosticReporter diagnostics) {
        if (!writer.isMultiModule()) {
            return null;
        }

        // Priority 1: explicit configuration
        if (config.targetModule() != null) {
            return config.targetModule();
        }

        // Priority 2: auto-routing by ModuleRole.API
        Optional<ModuleIndex> moduleIndexOpt = model.moduleIndex();
        if (moduleIndexOpt.isEmpty()) {
            return null;
        }

        ModuleIndex moduleIndex = moduleIndexOpt.get();
        Optional<String> autoRouted = ModuleRouting.resolveUniqueModuleByRole(moduleIndex, ModuleRole.API);

        if (autoRouted.isPresent()) {
            diagnostics.info("REST auto-routing: detected unique API module '" + autoRouted.get() + "'");
            return autoRouted.get();
        }

        long apiCount = moduleIndex.modulesByRole(ModuleRole.API).count();
        if (apiCount > 1) {
            diagnostics.warn("REST auto-routing: " + apiCount + " API modules found. "
                    + "Please configure 'targetModule' explicitly in hexaglue.yaml under plugins.rest.");
        }

        return null;
    }

    private static String deriveApiPackage(String basePackage) {
        return basePackage + ".api";
    }

    /**
     * Checks whether a driving port uses type variables (generics) in any use case.
     *
     * <p>Detects type variables both at top level ({@code T process()}) and nested
     * inside parameterized types ({@code List<T> search(String query)}).
     *
     * @param port the driving port to check
     * @return true if any use case has a type variable in return type or parameters
     */
    private static boolean hasTypeVariables(DrivingPort port) {
        return port.useCases().stream().anyMatch(uc -> {
            if (containsTypeVariable(uc.method().returnType())) {
                return true;
            }
            return uc.method().parameters().stream().anyMatch(p -> containsTypeVariable(p.type()));
        });
    }

    /**
     * Checks whether a TypeRef contains a type variable, either directly or in its type arguments.
     *
     * <p>A type variable is detected when the qualified name has no package (no dot),
     * is not primitive, and is not void. This method recursively checks type arguments
     * to detect variables inside parameterized types like {@code List<T>} or {@code Map<String, T>}.
     *
     * @param type the type reference to check
     * @return true if the type or any of its type arguments is a type variable
     */
    private static boolean containsTypeVariable(TypeRef type) {
        String qn = type.qualifiedName();
        if (!qn.contains(".") && !type.isPrimitive() && !"void".equals(qn)) {
            return true;
        }
        return type.typeArguments().stream().anyMatch(RestPlugin::containsTypeVariable);
    }

    /**
     * Merges pairs of collection endpoints (same HTTP verb + path) into a single endpoint
     * with optional query parameters and conditional dispatch.
     *
     * <p>When two QUERY endpoints on the same path return the same collection element type,
     * and one has no parameters (default) while the other has filter parameters, they are
     * fused into a single endpoint with {@code @RequestParam(required = false)} and an
     * if/else dispatch in the controller body.
     *
     * @param port        the driving port (for use case lookup)
     * @param spec        the controller spec potentially containing mergeable pairs
     * @param domainIndex the domain index for identifier/VO resolution
     * @param diagnostics the diagnostic reporter
     * @return a new ControllerSpec with merged endpoints, or the original if nothing to merge
     * @since 3.1.0
     */
    private static ControllerSpec mergeCollectionEndpoints(
            DrivingPort port, ControllerSpec spec, DomainIndex domainIndex, DiagnosticReporter diagnostics) {
        List<EndpointSpec> endpoints = spec.endpoints();

        // Group endpoint indices by verb+path
        Map<String, List<Integer>> urlToIndices = new LinkedHashMap<>();
        for (int i = 0; i < endpoints.size(); i++) {
            EndpointSpec ep = endpoints.get(i);
            String key = ep.httpMethod() + " " + ep.path();
            urlToIndices.computeIfAbsent(key, k -> new ArrayList<>()).add(i);
        }

        // Collect merge operations
        Set<Integer> removedIndices = new HashSet<>();
        Map<Integer, EndpointSpec> replacements = new LinkedHashMap<>();

        for (List<Integer> indices : urlToIndices.values()) {
            if (indices.size() != 2) {
                continue;
            }

            EndpointSpec ep1 = endpoints.get(indices.get(0));
            EndpointSpec ep2 = endpoints.get(indices.get(1));

            // Both must be QUERY
            if (ep1.useCaseType() != UseCaseType.QUERY || ep2.useCaseType() != UseCaseType.QUERY) {
                continue;
            }

            // Identify default (0 queryParams) and filtered (1+ queryParams)
            EndpointSpec defaultEp;
            EndpointSpec filteredEp;
            int defaultIdx;
            int filteredIdx;
            if (ep1.queryParams().isEmpty() && !ep2.queryParams().isEmpty()) {
                defaultEp = ep1;
                filteredEp = ep2;
                defaultIdx = indices.get(0);
                filteredIdx = indices.get(1);
            } else if (ep2.queryParams().isEmpty() && !ep1.queryParams().isEmpty()) {
                defaultEp = ep2;
                filteredEp = ep1;
                defaultIdx = indices.get(1);
                filteredIdx = indices.get(0);
            } else {
                continue;
            }

            // Verify same collection element type
            Optional<UseCase> defaultUc = findUseCase(port, defaultEp.methodName());
            Optional<UseCase> filteredUc = findUseCase(port, filteredEp.methodName());
            if (defaultUc.isEmpty() || filteredUc.isEmpty()) {
                continue;
            }

            List<io.hexaglue.syntax.TypeRef> defaultTypeArgs =
                    defaultUc.get().method().returnType().typeArguments();
            List<io.hexaglue.syntax.TypeRef> filteredTypeArgs =
                    filteredUc.get().method().returnType().typeArguments();
            if (defaultTypeArgs.isEmpty()
                    || filteredTypeArgs.isEmpty()
                    || !defaultTypeArgs
                            .get(0)
                            .qualifiedName()
                            .equals(filteredTypeArgs.get(0).qualifiedName())) {
                continue;
            }

            // Build merged query params (required=false, unwrapped) and parameter bindings
            List<QueryParamSpec> mergedQueryParams = new ArrayList<>();
            List<ParameterBindingSpec> mergedBindings = new ArrayList<>();

            for (Parameter param : filteredUc.get().method().parameters()) {
                buildMergedQueryParam(param, domainIndex, mergedQueryParams, mergedBindings);
            }

            // Create merged endpoint
            EndpointSpec merged = new EndpointSpec(
                    defaultEp.methodName(),
                    defaultEp.httpMethod(),
                    defaultEp.path(),
                    defaultEp.operationSummary(),
                    defaultEp.returnType(),
                    defaultEp.responseStatus(),
                    defaultEp.requestDtoRef(),
                    defaultEp.responseDtoRef(),
                    defaultEp.pathVariables(),
                    mergedQueryParams,
                    defaultEp.thrownExceptions(),
                    defaultEp.useCaseType(),
                    mergedBindings,
                    Optional.of(filteredEp.methodName()));

            int keepIdx = Math.min(defaultIdx, filteredIdx);
            int removeIdx = Math.max(defaultIdx, filteredIdx);
            replacements.put(keepIdx, merged);
            removedIndices.add(removeIdx);

            diagnostics.info("Merged collection endpoints: " + defaultEp.methodName() + " + " + filteredEp.methodName()
                    + " into single GET with optional filter");
        }

        if (replacements.isEmpty()) {
            return spec;
        }

        List<EndpointSpec> result = new ArrayList<>();
        for (int i = 0; i < endpoints.size(); i++) {
            if (removedIndices.contains(i)) {
                continue;
            }
            if (replacements.containsKey(i)) {
                result.add(replacements.get(i));
            } else {
                result.add(endpoints.get(i));
            }
        }

        return new ControllerSpec(
                spec.className(),
                spec.packageName(),
                spec.basePath(),
                spec.drivingPortType(),
                spec.aggregateType(),
                spec.tagName(),
                spec.tagDescription(),
                result,
                spec.requestDtos(),
                spec.responseDtos(),
                spec.exceptionMappings());
    }

    private static Optional<UseCase> findUseCase(DrivingPort port, String methodName) {
        return port.useCases().stream()
                .filter(uc -> uc.name().equals(methodName))
                .findFirst();
    }

    /**
     * Builds a query param spec and a parameter binding spec for a single parameter
     * of the filtered use case in a merged collection endpoint.
     *
     * <p>Identifiers and single-field VOs are unwrapped to their primitive/wrapper type
     * (using {@link BindingKind#PATH_VARIABLE_WRAP} for reconstruction). Other types use
     * {@link BindingKind#QUERY_PARAM} for direct pass-through.
     */
    private static void buildMergedQueryParam(
            Parameter param,
            DomainIndex domainIndex,
            List<QueryParamSpec> queryParams,
            List<ParameterBindingSpec> bindings) {
        TypeRef paramType = param.type();
        String paramName = param.name();

        // 1. Identifier: unwrap to wrappedType
        Optional<Identifier> id = DtoFieldMapper.findIdentifier(paramType, domainIndex);
        if (id.isPresent()) {
            TypeName unwrapped =
                    DtoFieldMapper.toTypeName(id.get().wrappedType()).box();
            queryParams.add(new QueryParamSpec(paramName, paramName, unwrapped, false, null));
            bindings.add(new ParameterBindingSpec(
                    paramName,
                    DtoFieldMapper.toTypeName(paramType),
                    BindingKind.PATH_VARIABLE_WRAP,
                    List.of(paramName)));
            return;
        }

        // 2. Single-field ValueObject: unwrap to inner field type
        Optional<ValueObject> vo = DtoFieldMapper.findValueObject(paramType, domainIndex);
        if (vo.isPresent() && vo.get().isSingleValue()) {
            Field wrappedField = vo.get().wrappedField().orElseThrow();
            TypeName unwrapped = DtoFieldMapper.toTypeName(wrappedField.type()).box();
            queryParams.add(new QueryParamSpec(paramName, paramName, unwrapped, false, null));
            bindings.add(new ParameterBindingSpec(
                    paramName,
                    DtoFieldMapper.toTypeName(paramType),
                    BindingKind.PATH_VARIABLE_WRAP,
                    List.of(paramName)));
            return;
        }

        // 3. Primitive or other type: box and use QUERY_PARAM
        TypeName javaType = DtoFieldMapper.toTypeName(paramType).box();
        queryParams.add(new QueryParamSpec(paramName, paramName, javaType, false, null));
        bindings.add(new ParameterBindingSpec(
                paramName, DtoFieldMapper.toTypeName(paramType), BindingKind.QUERY_PARAM, List.of(paramName)));
    }

    /**
     * Detects URL collisions (same HTTP verb + path) and disambiguates by prefixing
     * the path of colliding endpoints with the method name in kebab-case.
     *
     * <p>The first endpoint in each collision group keeps its original path.
     * Subsequent endpoints receive a {@code /{method-name}} prefix. A WARN diagnostic
     * is emitted for each collision.
     *
     * @param port        the driving port (for diagnostic messages)
     * @param spec        the controller spec potentially containing collisions
     * @param diagnostics the diagnostic reporter
     * @return a new ControllerSpec with disambiguated paths, or the original if no collision
     * @since 3.1.0
     */
    private static ControllerSpec resolveUrlCollisions(
            DrivingPort port, ControllerSpec spec, DiagnosticReporter diagnostics) {
        List<EndpointSpec> endpoints = spec.endpoints();

        // Group endpoint indices by verb+path
        Map<String, List<Integer>> urlToIndices = new LinkedHashMap<>();
        for (int i = 0; i < endpoints.size(); i++) {
            EndpointSpec ep = endpoints.get(i);
            String key = ep.httpMethod() + " " + ep.path();
            urlToIndices.computeIfAbsent(key, k -> new ArrayList<>()).add(i);
        }

        boolean hasCollision = urlToIndices.values().stream().anyMatch(list -> list.size() > 1);
        if (!hasCollision) {
            return spec;
        }

        // Disambiguate colliding endpoints
        List<EndpointSpec> resolved = new ArrayList<>(endpoints);
        for (List<Integer> indices : urlToIndices.values()) {
            if (indices.size() <= 1) {
                continue;
            }
            // First endpoint keeps its path; subsequent ones get a method-name prefix
            for (int i = 1; i < indices.size(); i++) {
                int idx = indices.get(i);
                EndpointSpec colliding = resolved.get(idx);
                String prefix = "/" + NamingConventions.toKebabCase(colliding.methodName());
                String disambiguated = prefix + colliding.path();
                diagnostics.warn("URL collision detected for port " + port.id().simpleName() + ": "
                        + resolved.get(indices.get(0)).methodName() + " and " + colliding.methodName());
                resolved.set(idx, colliding.withPath(disambiguated));
            }
        }

        return new ControllerSpec(
                spec.className(),
                spec.packageName(),
                spec.basePath(),
                spec.drivingPortType(),
                spec.aggregateType(),
                spec.tagName(),
                spec.tagDescription(),
                resolved,
                spec.requestDtos(),
                spec.responseDtos(),
                spec.exceptionMappings());
    }

    /**
     * Emits diagnostics for a single port after its {@link ControllerSpec} is built.
     *
     * <p>Checks for: missing aggregate association and polymorphic response types.
     *
     * @param port        the driving port
     * @param spec        the built controller spec
     * @param domainIndex the domain index (may be null)
     * @param diagnostics the diagnostic reporter
     */
    private static void emitPortDiagnostics(
            DrivingPort port, ControllerSpec spec, DomainIndex domainIndex, DiagnosticReporter diagnostics) {
        // 1. No aggregate found
        if (!spec.hasAggregate() && domainIndex != null) {
            diagnostics.warn("No aggregate found for port " + port.id().simpleName() + ", using port name for path");
        }

        // 2. Polymorphic response
        if (domainIndex != null) {
            emitPolymorphicResponseDiagnostics(port, domainIndex, diagnostics);
        }
    }

    /**
     * Emits INFO diagnostics when a use case returns an interface or abstract type.
     *
     * @param port        the driving port
     * @param domainIndex the domain index
     * @param diagnostics the diagnostic reporter
     */
    private static void emitPolymorphicResponseDiagnostics(
            DrivingPort port, DomainIndex domainIndex, DiagnosticReporter diagnostics) {
        Set<String> reported = new HashSet<>();
        for (UseCase uc : port.useCases()) {
            TypeRef rt = uc.method().returnType();
            String qn = rt.qualifiedName();
            if ("void".equals(qn) || !reported.add(qn)) {
                continue;
            }
            TypeId typeId = TypeId.of(qn);
            // Check AggregateRoot
            domainIndex
                    .aggregateRoot(typeId)
                    .filter(agg -> isPolymorphicType(agg.structure()))
                    .ifPresent(agg -> diagnostics.info("Using polymorphic response for interface " + rt.simpleName()));
            // Check ValueObject
            domainIndex
                    .valueObjects()
                    .filter(vo -> vo.id().equals(typeId))
                    .filter(vo -> isPolymorphicType(vo.structure()))
                    .findFirst()
                    .ifPresent(vo -> diagnostics.info("Using polymorphic response for interface " + rt.simpleName()));
        }
    }

    /**
     * Checks whether a type structure represents a polymorphic type (interface or abstract class).
     *
     * @param structure the type structure to check
     * @return true if the type is an interface or abstract
     */
    private static boolean isPolymorphicType(TypeStructure structure) {
        return structure.isInterface() || structure.modifiers().contains(Modifier.ABSTRACT);
    }
}
