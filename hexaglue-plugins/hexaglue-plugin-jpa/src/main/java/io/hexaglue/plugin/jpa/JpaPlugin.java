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
import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.domain.DomainEntity;
import io.hexaglue.arch.domain.ValueObject;
import io.hexaglue.arch.ports.DrivenPort;
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
import io.hexaglue.spi.generation.ArtifactWriter;
import io.hexaglue.spi.generation.GeneratorContext;
import io.hexaglue.spi.generation.GeneratorPlugin;
import io.hexaglue.spi.plugin.DiagnosticReporter;
import io.hexaglue.spi.plugin.PluginConfig;
import io.hexaglue.spi.plugin.PluginContext;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
 *   <li><b>Specification building</b>: Builders transform model types to intermediate specs</li>
 *   <li><b>Code generation</b>: Codegen classes generate JavaPoet TypeSpecs from specs</li>
 * </ol>
 *
 * <p>This plugin requires a v4 {@code ArchitecturalModel}. The generated code uses
 * SPI classification types for consistency with the hexaglue ecosystem.
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
 * @since 4.0.0
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
     * Overrides default execute to capture v4 ArchitecturalModel.
     *
     * @param context the plugin context
     * @since 4.0.0
     */
    @Override
    public void execute(PluginContext context) {
        // Capture v4 model before delegating to generate()
        this.archModel = context.model();
        GeneratorPlugin.super.execute(context);
    }

    @Override
    public void generate(GeneratorContext context) throws Exception {
        PluginConfig pluginConfig = context.config();
        ArtifactWriter writer = context.writer();
        DiagnosticReporter diagnostics = context.diagnostics();

        // archModel is set by execute() - should never be null at this point
        if (archModel == null) {
            diagnostics.error("v4 ArchitecturalModel is required for JPA code generation. "
                    + "Please ensure the model is available.");
            return;
        }

        // Load configuration
        JpaConfig config = JpaConfig.from(pluginConfig);

        // Determine packages from ArchitecturalModel
        String basePackage = archModel.project().basePackage();
        String infraPackage =
                pluginConfig.getString("infrastructurePackage").orElse(basePackage + ".infrastructure.persistence");

        diagnostics.info("JPA Plugin starting with package: " + infraPackage);
        logConfig(diagnostics, config);
        logClassificationSummary(archModel, diagnostics);

        generateFromModel(archModel, config, infraPackage, writer, diagnostics);
    }

    /**
     * Generates JPA infrastructure using v4 ArchitecturalModel.
     *
     * @param model the architectural model
     * @param config the JPA configuration
     * @param infraPackage the infrastructure package name
     * @param writer the artifact writer
     * @param diagnostics the diagnostic reporter
     * @since 4.0.0
     */
    private void generateFromModel(
            ArchitecturalModel model,
            JpaConfig config,
            String infraPackage,
            ArtifactWriter writer,
            DiagnosticReporter diagnostics) {

        // Counters for summary
        int entityCount = 0;
        int embeddableCount = 0;
        int repositoryCount = 0;
        int mapperCount = 0;
        int adapterCount = 0;

        // Build embeddable mapping: domain VALUE_OBJECT FQN -> generated embeddable FQN
        Map<String, String> embeddableMapping = new HashMap<>();

        // Generate embeddables from ValueObjects
        if (config.generateEmbeddables()) {
            // First pass: compute all embeddable mappings
            model.valueObjects()
                    .filter(vo -> vo.syntax() != null)
                    .filter(vo -> !isEnumType(vo))
                    .forEach(vo -> {
                        String embeddableClassName = vo.id().simpleName() + config.embeddableSuffix();
                        String embeddableFqn = infraPackage + "." + embeddableClassName;
                        embeddableMapping.put(vo.id().qualifiedName(), embeddableFqn);
                    });

            // Second pass: generate embeddables
            for (ValueObject vo :
                    model.valueObjects().filter(v -> v.syntax() != null).toList()) {
                if (isEnumType(vo)) {
                    continue;
                }

                try {
                    EmbeddableSpec embeddableSpec = EmbeddableSpecBuilder.builder()
                            .valueObject(vo)
                            .model(model)
                            .config(config)
                            .infrastructurePackage(infraPackage)
                            .embeddableMapping(embeddableMapping)
                            .build();

                    TypeSpec embeddableTypeSpec = JpaEmbeddableCodegen.generate(embeddableSpec);
                    String embeddableSource = toJavaSource(infraPackage, embeddableTypeSpec);
                    writer.writeJavaSource(infraPackage, embeddableSpec.className(), embeddableSource);
                    embeddableCount++;
                    diagnostics.info("Generated embeddable: " + embeddableSpec.className());

                } catch (IOException e) {
                    diagnostics.error(
                            "Failed to generate embeddable for " + vo.id().simpleName(), e);
                } catch (IllegalArgumentException | IllegalStateException e) {
                    diagnostics.warn("Skipping embeddable " + vo.id().simpleName() + ": " + e.getMessage());
                }
            }
        }

        // Group driven ports by primary managed type
        Map<String, List<DrivenPort>> portsByManagedType = model.drivenPorts()
                .filter(port -> port.primaryManagedType().isPresent())
                .collect(Collectors.groupingBy(
                        port -> port.primaryManagedType().get().id().qualifiedName(), Collectors.toList()));

        // Generate entities from DomainEntities
        for (DomainEntity entity :
                model.domainEntities().filter(DomainEntity::hasIdentity).toList()) {
            try {
                EntitySpec entitySpec = EntitySpecBuilder.builder()
                        .domainEntity(entity)
                        .model(model)
                        .config(config)
                        .infrastructurePackage(infraPackage)
                        .embeddableMapping(embeddableMapping)
                        .build();

                TypeSpec entityTypeSpec = JpaEntityCodegen.generate(entitySpec);
                String entitySource = toJavaSource(infraPackage, entityTypeSpec);
                writer.writeJavaSource(infraPackage, entitySpec.className(), entitySource);
                entityCount++;
                diagnostics.info("Generated entity: " + entitySpec.className());

                // Get driven ports for this entity
                List<DrivenPort> portsForEntity =
                        portsByManagedType.getOrDefault(entity.id().qualifiedName(), List.of());

                // Generate repository if enabled
                if (config.generateRepositories()) {
                    RepositorySpec repoSpec = RepositorySpecBuilder.builder()
                            .domainEntity(entity)
                            .drivenPorts(portsForEntity)
                            .model(model)
                            .config(config)
                            .infrastructurePackage(infraPackage)
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
                            .domainEntity(entity)
                            .model(model)
                            .config(config)
                            .infrastructurePackage(infraPackage)
                            .embeddableMapping(embeddableMapping)
                            .build();

                    TypeSpec mapperTypeSpec = JpaMapperCodegen.generate(mapperSpec);
                    String mapperSource = toJavaSource(infraPackage, mapperTypeSpec);
                    writer.writeJavaSource(infraPackage, mapperSpec.interfaceName(), mapperSource);
                    mapperCount++;
                    diagnostics.info("Generated mapper: " + mapperSpec.interfaceName());
                }

            } catch (IOException e) {
                diagnostics.error(
                        "Failed to generate JPA class for " + entity.id().simpleName(), e);
            } catch (IllegalArgumentException | IllegalStateException e) {
                diagnostics.warn("Skipping " + entity.id().simpleName() + ": " + e.getMessage());
            }
        }

        // Generate adapters for driven ports
        if (config.generateAdapters()) {
            for (DrivenPort port : model.drivenPorts().toList()) {
                if (port.primaryManagedType().isEmpty()) {
                    diagnostics.warn("Skipping adapter for port " + port.id().simpleName() + ": no managed type");
                    continue;
                }

                // Find the domain entity for this port
                String managedTypeFqn = port.primaryManagedType().get().id().qualifiedName();
                Optional<DomainEntity> entityOpt = model.domainEntities()
                        .filter(e -> e.id().qualifiedName().equals(managedTypeFqn))
                        .findFirst();

                if (entityOpt.isEmpty()) {
                    diagnostics.warn("Skipping adapter for port " + port.id().simpleName() + ": managed type "
                            + managedTypeFqn + " not found as entity");
                    continue;
                }

                DomainEntity entity = entityOpt.get();

                try {
                    AdapterSpec adapterSpec = AdapterSpecBuilder.builder()
                            .drivenPorts(List.of(port))
                            .domainEntity(entity)
                            .model(model)
                            .config(config)
                            .infrastructurePackage(infraPackage)
                            .build();

                    TypeSpec adapterTypeSpec = JpaAdapterCodegen.generate(adapterSpec);
                    String adapterSource = toJavaSource(infraPackage, adapterTypeSpec);
                    writer.writeJavaSource(infraPackage, adapterSpec.className(), adapterSource);
                    adapterCount++;

                    diagnostics.info("Generated adapter: " + adapterSpec.className() + " implementing "
                            + port.id().simpleName());
                } catch (IOException e) {
                    diagnostics.error(
                            "Failed to generate adapter for port " + port.id().simpleName(), e);
                } catch (IllegalArgumentException | IllegalStateException e) {
                    diagnostics.warn("Skipping adapter for port " + port.id().simpleName() + ": " + e.getMessage());
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
     * Determines if a v4 ValueObject is an enum type.
     *
     * <p>Enums don't need embeddable classes - they are handled via @Enumerated.
     *
     * @param vo the value object
     * @return true if the type is an enum
     * @since 4.0.0
     */
    private boolean isEnumType(ValueObject vo) {
        return vo.syntax() != null && vo.syntax().isEnum();
    }

    /**
     * Logs a summary of the v4 ArchitecturalModel classification.
     *
     * <p>This provides visibility into the new classification system when available,
     * including classification traces for debugging and warnings for unclassified types.
     *
     * @param model the architectural model
     * @param diagnostics the diagnostic reporter
     * @since 4.0.0
     */
    private void logClassificationSummary(ArchitecturalModel model, DiagnosticReporter diagnostics) {
        // Note: Use domainEntities().filter(isAggregateRoot) instead of aggregates()
        // because ArchitecturalModelBuilder creates DomainEntity with kind=AGGREGATE_ROOT,
        // but does not create Aggregate objects (which are higher-level constructs).
        long aggregateCount =
                model.domainEntities().filter(DomainEntity::isAggregateRoot).count();
        long entityCount =
                model.domainEntities().filter(e -> !e.isAggregateRoot()).count();
        long valueObjectCount = model.valueObjects().count();
        long drivenPortCount = model.drivenPorts().count();
        long unclassifiedCount = model.unclassifiedCount();

        diagnostics.info(String.format(
                "v4 Model: %d aggregates, %d entities, %d value objects, %d driven ports",
                aggregateCount, entityCount, valueObjectCount, drivenPortCount));

        // Log classification traces for aggregate roots (useful for debugging)
        model.domainEntities().filter(DomainEntity::isAggregateRoot).limit(3).forEach(agg -> {
            diagnostics.info("  - " + agg.id().simpleName() + ": "
                    + agg.classificationTrace().explain());
        });

        // Warn about unclassified types with remediation hints
        if (unclassifiedCount > 0) {
            diagnostics.warn(String.format(
                    "Found %d unclassified types (no JPA code will be generated for these):", unclassifiedCount));
            model.unclassifiedTypes().limit(5).forEach(unclassified -> {
                String hint = unclassified.classificationTrace().remediationHints().stream()
                        .findFirst()
                        .map(h -> " - Hint: " + h.description())
                        .orElse("");
                diagnostics.warn(String.format(
                        "  - %s: %s%s",
                        unclassified.id().simpleName(),
                        unclassified.classificationTrace().explain(),
                        hint));
            });
            if (unclassifiedCount > 5) {
                diagnostics.warn(String.format("  ... and %d more unclassified types", unclassifiedCount - 5));
            }
        }
    }
}
