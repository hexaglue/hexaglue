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

package io.hexaglue.plugin.jpa;

import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.TypeSpec;
import io.hexaglue.plugin.jpa.builder.AdapterSpecBuilder;
import io.hexaglue.plugin.jpa.builder.EmbeddableSpecBuilder;
import io.hexaglue.plugin.jpa.builder.EntitySpecBuilder;
import io.hexaglue.plugin.jpa.builder.MapperSpecBuilder;
import io.hexaglue.plugin.jpa.builder.RepositorySpecBuilder;
import io.hexaglue.plugin.jpa.codegen.JpaAdapterCodegen;
import io.hexaglue.plugin.jpa.codegen.JpaEmbeddableCodegen;
import io.hexaglue.plugin.jpa.codegen.JpaEntityCodegen;
import io.hexaglue.plugin.jpa.codegen.JpaMapperCodegen;
import io.hexaglue.plugin.jpa.codegen.JpaRepositoryCodegen;
import io.hexaglue.plugin.jpa.model.AdapterSpec;
import io.hexaglue.plugin.jpa.model.EmbeddableSpec;
import io.hexaglue.plugin.jpa.model.EntitySpec;
import io.hexaglue.plugin.jpa.model.MapperSpec;
import io.hexaglue.plugin.jpa.model.RepositorySpec;
import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.spi.arch.PluginContexts;
import io.hexaglue.spi.generation.ArtifactWriter;
import io.hexaglue.spi.generation.GeneratorContext;
import io.hexaglue.spi.generation.GeneratorPlugin;
import io.hexaglue.spi.ir.DomainType;
import io.hexaglue.spi.ir.IrSnapshot;
import io.hexaglue.spi.ir.Port;
import io.hexaglue.spi.ir.PortKind;
import io.hexaglue.spi.plugin.DiagnosticReporter;
import io.hexaglue.spi.plugin.PluginConfig;
import io.hexaglue.spi.plugin.PluginContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * JPA plugin for HexaGlue.
 *
 * <p>Generates complete JPA infrastructure from domain types using JavaPoet for
 * type-safe code generation:
 * <ul>
 *   <li>Entities and Aggregate Roots → {@code @Entity} classes</li>
 *   <li>Value Objects → {@code @Embeddable} classes</li>
 *   <li>Repository interfaces → Spring Data JPA repositories</li>
 *   <li>Mappers → MapStruct mapper interfaces</li>
 *   <li>Adapters → Port implementations with transactions</li>
 * </ul>
 *
 * <p>This plugin uses a two-stage architecture:
 * <ol>
 *   <li><b>Specification building</b>: Builders transform SPI types to intermediate specs</li>
 *   <li><b>Code generation</b>: Codegen classes generate JavaPoet TypeSpecs from specs</li>
 * </ol>
 *
 * <p>Configuration options in hexaglue.yaml:
 * <pre>
 * hexaglue:
 *   plugins:
 *     io.hexaglue.plugin.jpa:
 *       infrastructurePackage: com.example.infrastructure.persistence
 *       entitySuffix: Entity
 *       repositorySuffix: JpaRepository
 *       adapterSuffix: Adapter
 *       mapperSuffix: Mapper
 *       tablePrefix: ""
 *       enableAuditing: false
 *       enableOptimisticLocking: false
 *       generateRepositories: true
 *       generateMappers: true
 *       generateAdapters: true
 * </pre>
 *
 * @since 2.0.0
 */
public final class JpaPlugin implements GeneratorPlugin {

    /** Plugin identifier. */
    public static final String PLUGIN_ID = "io.hexaglue.plugin.jpa";

    /** Indentation for generated code (4 spaces). */
    private static final String INDENT = "    ";

    /** V4 ArchitecturalModel (set during execute if available). */
    private ArchitecturalModel archModel;

    @Override
    public String id() {
        return PLUGIN_ID;
    }

    /**
     * Overrides default execute to capture v4 ArchitecturalModel if available.
     *
     * @param context the plugin context
     * @since 4.0.0
     */
    @Override
    public void execute(PluginContext context) {
        // Capture v4 model if available before delegating to generate()
        this.archModel = PluginContexts.getModel(context).orElse(null);
        GeneratorPlugin.super.execute(context);
    }

    @Override
    public void generate(GeneratorContext context) throws Exception {
        IrSnapshot ir = context.ir();
        PluginConfig pluginConfig = context.config();
        ArtifactWriter writer = context.writer();
        DiagnosticReporter diagnostics = context.diagnostics();

        // Log v4 model info if available
        if (archModel != null) {
            diagnostics.info("Using v4 ArchitecturalModel (classification traces available)");
            logClassificationSummary(archModel, diagnostics);
        }

        if (ir.isEmpty()) {
            diagnostics.info("No domain types to process");
            return;
        }

        // Load configuration
        JpaConfig config = JpaConfig.from(pluginConfig);

        // Determine packages
        String basePackage = ir.metadata().basePackage();
        String infraPackage =
                pluginConfig.getString("infrastructurePackage").orElse(basePackage + ".infrastructure.persistence");

        diagnostics.info("JPA Plugin starting with package: " + infraPackage);
        logConfig(diagnostics, config);

        // Collect all domain types for mapper and entity references
        List<DomainType> allTypes = new ArrayList<>(ir.domain().types());

        // Collect repository ports with their managed types (used for repository and adapter generation)
        // Each port will generate its own adapter to avoid method signature conflicts
        Map<Port, DomainType> portToManagedType = new LinkedHashMap<>();
        for (Port port : ir.ports().ports()) {
            if (port.kind() == PortKind.REPOSITORY && port.isDriven()) {
                Optional<DomainType> managedType =
                        findManagedType(port, ir.domain().types());
                if (managedType.isPresent() && managedType.get().isEntity()) {
                    portToManagedType.put(port, managedType.get());
                } else if (managedType.isEmpty()) {
                    diagnostics.warn("Could not determine managed type for port: " + port.simpleName());
                }
            }
        }

        // Group ports by managed type for repository generation (repositories are shared)
        Map<DomainType, List<Port>> portsByManagedType = new HashMap<>();
        for (Map.Entry<Port, DomainType> entry : portToManagedType.entrySet()) {
            portsByManagedType
                    .computeIfAbsent(entry.getValue(), k -> new ArrayList<>())
                    .add(entry.getKey());
        }

        // Counters for summary
        int entityCount = 0;
        int embeddableCount = 0;
        int repositoryCount = 0;
        int mapperCount = 0;
        int adapterCount = 0;

        // Build embeddable mapping: domain VALUE_OBJECT FQN -> generated embeddable FQN
        // Only non-enum VALUE_OBJECTs need embeddable classes (enums use @Enumerated)
        // FIRST PASS: Build the complete mapping before generating any embeddable
        // This allows nested VALUE_OBJECTs (like Money in OrderLine) to be substituted
        Map<String, String> embeddableMapping = new HashMap<>();

        if (config.generateEmbeddables()) {
            // First pass: compute all embeddable mappings
            for (DomainType type : ir.domain().types()) {
                if (type.isValueObject() && !isEnumType(type)) {
                    String embeddableClassName = type.simpleName() + config.embeddableSuffix();
                    String embeddableFqn = infraPackage + "." + embeddableClassName;
                    embeddableMapping.put(type.qualifiedName(), embeddableFqn);
                }
            }

            // Second pass: generate embeddables with the complete mapping
            for (DomainType type : ir.domain().types()) {
                if (type.isValueObject() && !isEnumType(type)) {
                    try {
                        EmbeddableSpec embeddableSpec = EmbeddableSpecBuilder.builder()
                                .domainType(type)
                                .config(config)
                                .infrastructurePackage(infraPackage)
                                .allTypes(allTypes)
                                .embeddableMapping(embeddableMapping)
                                .build();

                        // Generate and write JPA embeddable
                        TypeSpec embeddableTypeSpec = JpaEmbeddableCodegen.generate(embeddableSpec);
                        String embeddableSource = toJavaSource(infraPackage, embeddableTypeSpec);
                        writer.writeJavaSource(infraPackage, embeddableSpec.className(), embeddableSource);
                        embeddableCount++;
                        diagnostics.info("Generated embeddable: " + embeddableSpec.className());

                    } catch (IOException e) {
                        diagnostics.error("Failed to generate embeddable for " + type.simpleName(), e);
                    } catch (IllegalArgumentException | IllegalStateException e) {
                        diagnostics.warn("Skipping embeddable " + type.simpleName() + ": " + e.getMessage());
                    }
                }
            }
        }

        // Generate entities (using embeddable mapping for VALUE_OBJECT relations)
        for (DomainType type : ir.domain().types()) {
            try {
                if (type.isEntity() && type.hasIdentity()) {
                    // Build entity specification with embeddable mapping
                    EntitySpec entitySpec = EntitySpecBuilder.builder()
                            .domainType(type)
                            .config(config)
                            .infrastructurePackage(infraPackage)
                            .allTypes(allTypes)
                            .embeddableMapping(embeddableMapping)
                            .build();

                    // Generate and write JPA entity
                    TypeSpec entityTypeSpec = JpaEntityCodegen.generate(entitySpec);
                    String entitySource = toJavaSource(infraPackage, entityTypeSpec);
                    writer.writeJavaSource(infraPackage, entitySpec.className(), entitySource);
                    entityCount++;
                    diagnostics.info("Generated entity: " + entitySpec.className());

                    // Generate repository if enabled
                    if (config.generateRepositories()) {
                        // Get ports associated with this domain type for derived method extraction
                        List<Port> portsForType = portsByManagedType.getOrDefault(type, List.of());

                        RepositorySpec repoSpec = RepositorySpecBuilder.builder()
                                .domainType(type)
                                .config(config)
                                .infrastructurePackage(infraPackage)
                                .ports(portsForType)
                                .build();

                        TypeSpec repoTypeSpec = JpaRepositoryCodegen.generate(repoSpec);
                        String repoSource = toJavaSource(infraPackage, repoTypeSpec);
                        writer.writeJavaSource(infraPackage, repoSpec.interfaceName(), repoSource);
                        repositoryCount++;
                        diagnostics.info("Generated repository: " + repoSpec.interfaceName());
                    }

                    // Generate mapper if enabled
                    if (config.generateMappers()) {
                        MapperSpec mapperSpec = MapperSpecBuilder.builder()
                                .domainType(type)
                                .config(config)
                                .infrastructurePackage(infraPackage)
                                .allTypes(allTypes)
                                .embeddableMapping(embeddableMapping)
                                .build();

                        TypeSpec mapperTypeSpec = JpaMapperCodegen.generate(mapperSpec);
                        String mapperSource = toJavaSource(infraPackage, mapperTypeSpec);
                        writer.writeJavaSource(infraPackage, mapperSpec.interfaceName(), mapperSource);
                        mapperCount++;
                        diagnostics.info("Generated mapper: " + mapperSpec.interfaceName());
                    }

                } else if (type.isValueObject() && isEnumType(type)) {
                    // Enums don't need embeddables - they use @Enumerated
                    diagnostics.info("Detected enum value object (no embeddable needed): " + type.simpleName());
                }

            } catch (IOException e) {
                diagnostics.error("Failed to generate JPA class for " + type.simpleName(), e);
            } catch (IllegalArgumentException | IllegalStateException e) {
                diagnostics.warn("Skipping " + type.simpleName() + ": " + e.getMessage());
            }
        }

        // Generate adapters for repository ports
        // Generate ONE adapter PER PORT to avoid method signature conflicts
        if (config.generateAdapters()) {
            for (Map.Entry<Port, DomainType> entry : portToManagedType.entrySet()) {
                Port port = entry.getKey();
                DomainType managedType = entry.getValue();

                try {
                    AdapterSpec adapterSpec = AdapterSpecBuilder.builder()
                            .ports(List.of(port)) // Single port per adapter
                            .domainType(managedType)
                            .config(config)
                            .infrastructurePackage(infraPackage)
                            .build();

                    TypeSpec adapterTypeSpec = JpaAdapterCodegen.generate(adapterSpec);
                    String adapterSource = toJavaSource(infraPackage, adapterTypeSpec);
                    writer.writeJavaSource(infraPackage, adapterSpec.className(), adapterSource);
                    adapterCount++;

                    diagnostics.info(
                            "Generated adapter: " + adapterSpec.className() + " implementing " + port.simpleName());
                } catch (IOException e) {
                    diagnostics.error("Failed to generate adapter for port " + port.simpleName(), e);
                } catch (IllegalArgumentException | IllegalStateException e) {
                    diagnostics.warn("Skipping adapter for port " + port.simpleName() + ": " + e.getMessage());
                }
            }
        }

        // Summary
        diagnostics.info(String.format(
                "JPA generation complete: %d entities, %d embeddables, %d repositories, %d mappers, %d adapters",
                entityCount, embeddableCount, repositoryCount, mapperCount, adapterCount));
    }

    /**
     * Converts a JavaPoet TypeSpec to a Java source string.
     *
     * @param packageName the package name for the generated class
     * @param typeSpec the JavaPoet type specification
     * @return the complete Java source code as a string
     */
    private String toJavaSource(String packageName, TypeSpec typeSpec) {
        JavaFile javaFile =
                JavaFile.builder(packageName, typeSpec).indent(INDENT).build();
        return javaFile.toString();
    }

    /**
     * Logs the plugin configuration for debugging.
     *
     * @param diagnostics the diagnostic reporter
     * @param config the JPA configuration
     */
    private void logConfig(DiagnosticReporter diagnostics, JpaConfig config) {
        diagnostics.info("  entitySuffix: " + config.entitySuffix());
        diagnostics.info("  repositorySuffix: " + config.repositorySuffix());
        diagnostics.info("  enableAuditing: " + config.enableAuditing());
        diagnostics.info("  enableOptimisticLocking: " + config.enableOptimisticLocking());
        diagnostics.info("  generateRepositories: " + config.generateRepositories());
        diagnostics.info("  generateMappers: " + config.generateMappers());
        diagnostics.info("  generateAdapters: " + config.generateAdapters());
    }

    /**
     * Determines if a VALUE_OBJECT is an enum type.
     *
     * <p>Enums don't need embeddable classes - they are handled via @Enumerated.
     *
     * @param type the domain type
     * @return true if the type is an enum
     */
    private boolean isEnumType(DomainType type) {
        return type.construct() == io.hexaglue.spi.ir.JavaConstruct.ENUM;
    }

    /**
     * Finds the domain type managed by a repository port.
     *
     * <p>Looks at the port's managed types and signature to determine
     * which aggregate root or entity it manages.
     *
     * @param port the repository port
     * @param types all domain types
     * @return the managed domain type, if found
     */
    private Optional<DomainType> findManagedType(Port port, List<DomainType> types) {
        // First check managedTypes from IR
        if (!port.managedTypes().isEmpty()) {
            String managedTypeFqn = port.managedTypes().get(0);
            return types.stream()
                    .filter(t -> t.qualifiedName().equals(managedTypeFqn))
                    .findFirst();
        }

        // Fallback: derive from port name (e.g., "Orders" -> "Order", "OrderRepository" -> "Order")
        String portName = port.simpleName();
        String baseName = portName.replace("Repository", "").replace("Repo", "").replace("Store", "");

        // Handle plural (simple heuristic)
        if (baseName.endsWith("s") && !baseName.endsWith("ss")) {
            baseName = baseName.substring(0, baseName.length() - 1);
        }

        final String searchName = baseName;
        return types.stream()
                .filter(t -> t.simpleName().equals(searchName))
                .filter(DomainType::isEntity)
                .findFirst();
    }

    /**
     * Logs a summary of the v4 ArchitecturalModel classification.
     *
     * <p>This provides visibility into the new classification system when available,
     * including classification traces for debugging.
     *
     * @param model the architectural model
     * @param diagnostics the diagnostic reporter
     * @since 4.0.0
     */
    private void logClassificationSummary(ArchitecturalModel model, DiagnosticReporter diagnostics) {
        long aggregateCount = model.aggregates().count();
        long entityCount = model.domainEntities().filter(e -> !e.isAggregateRoot()).count();
        long valueObjectCount = model.valueObjects().count();
        long drivenPortCount = model.drivenPorts().count();

        diagnostics.info(String.format(
                "v4 Model: %d aggregates, %d entities, %d value objects, %d driven ports",
                aggregateCount, entityCount, valueObjectCount, drivenPortCount));

        // Log classification traces for aggregates (useful for debugging)
        model.aggregates().limit(3).forEach(agg -> {
            diagnostics.info("  - " + agg.id().simpleName() + ": " + agg.classificationTrace().explain());
        });
    }
}
