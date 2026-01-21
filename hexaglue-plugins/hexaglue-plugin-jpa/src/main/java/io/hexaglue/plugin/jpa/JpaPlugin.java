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
import io.hexaglue.arch.model.AggregateRoot;
import io.hexaglue.arch.model.Entity;
import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.TypeNature;
import io.hexaglue.arch.model.index.DomainIndex;
import io.hexaglue.arch.model.index.PortIndex;
import io.hexaglue.arch.model.report.ClassificationReport;
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
 * <p>This plugin requires a v5 {@code ArchitecturalModel}. The generated code uses
 * SPI classification types for consistency with the hexaglue ecosystem.
 *
 * <h2>v5.0.0 Architecture</h2>
 * <p>Since v5.0.0, this plugin uses the new {@link DomainIndex}, {@link PortIndex},
 * and {@link ClassificationReport} APIs for improved type access and classification insights.</p>
 *
 * <p>v5.0.0 features used:
 * <ul>
 *   <li>{@link DomainIndex#aggregateRoots()} for accessing enriched aggregate types</li>
 *   <li>{@link PortIndex#repositories()} for accessing repository ports with managed aggregates</li>
 *   <li>{@link ClassificationReport#actionRequired()} for identifying types needing attention</li>
 * </ul>
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
 * @since 4.1.0 - Added support for new DomainIndex, PortIndex, and ClassificationReport APIs
 */
public final class JpaPlugin implements GeneratorPlugin {

    /** Plugin identifier. */
    public static final String PLUGIN_ID = "io.hexaglue.plugin.jpa";

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
            diagnostics.error("ArchitecturalModel is required for JPA code generation. "
                    + "Please ensure the model is available.");
            return;
        }

        // Verify v5 indices are available
        if (model.domainIndex().isEmpty() || model.portIndex().isEmpty()) {
            diagnostics.error("v5 ArchitecturalModel with DomainIndex and PortIndex is required. "
                    + "Please ensure the model is built with v5 model types.");
            return;
        }

        // Load configuration
        JpaConfig config = JpaConfig.from(pluginConfig);

        // Determine packages from ArchitecturalModel
        String basePackage = model.project().basePackage();
        String infraPackage =
                pluginConfig.getString("infrastructurePackage").orElse(basePackage + ".infrastructure.persistence");

        diagnostics.info("JPA Plugin starting with package: " + infraPackage);
        logConfig(diagnostics, config);
        logClassificationSummary(model, diagnostics);

        generateFromV5Model(
                model, model.domainIndex().get(), model.portIndex().get(), config, infraPackage, writer, diagnostics);
    }

    /**
     * Generates JPA infrastructure using v5 model types.
     *
     * @param model the architectural model
     * @param domainIndex the v5 domain index
     * @param portIndex the v5 port index
     * @param config the JPA configuration
     * @param infraPackage the infrastructure package name
     * @param writer the artifact writer
     * @param diagnostics the diagnostic reporter
     * @since 5.0.0
     */
    private void generateFromV5Model(
            ArchitecturalModel model,
            DomainIndex domainIndex,
            PortIndex portIndex,
            JpaConfig config,
            String infraPackage,
            ArtifactWriter writer,
            DiagnosticReporter diagnostics) {

        diagnostics.info("Using v5 model types for JPA generation");

        // Counters for summary
        int entityCount = 0;
        int embeddableCount = 0;
        int repositoryCount = 0;
        int mapperCount = 0;
        int adapterCount = 0;

        // Build embeddable mapping: domain VALUE_OBJECT FQN -> generated embeddable FQN
        Map<String, String> embeddableMapping = new HashMap<>();

        // Generate embeddables from v5 ValueObjects
        if (config.generateEmbeddables()) {
            List<io.hexaglue.arch.model.ValueObject> valueObjects = domainIndex
                    .valueObjects()
                    .filter(vo -> vo.structure() != null)
                    .filter(vo -> vo.structure().nature() != TypeNature.ENUM)
                    .toList();

            // First pass: compute all embeddable mappings
            for (io.hexaglue.arch.model.ValueObject vo : valueObjects) {
                String embeddableClassName = vo.id().simpleName() + config.embeddableSuffix();
                String embeddableFqn = infraPackage + "." + embeddableClassName;
                embeddableMapping.put(vo.id().qualifiedName(), embeddableFqn);
            }

            // Second pass: generate embeddables
            for (io.hexaglue.arch.model.ValueObject vo : valueObjects) {
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

        // Group v5 driven ports by managed aggregate
        Map<String, List<io.hexaglue.arch.model.DrivenPort>> portsByManagedType = portIndex
                .drivenPorts()
                .filter(port -> port.managedAggregate().isPresent())
                .collect(Collectors.groupingBy(
                        port -> port.managedAggregate().get().qualifiedName(), Collectors.toList()));

        // Generate entities from v5 AggregateRoots
        List<AggregateRoot> allAggregates = domainIndex.aggregateRoots().toList();

        for (AggregateRoot aggregate : allAggregates) {
            try {
                EntitySpec entitySpec = EntitySpecBuilder.builder()
                        .aggregateRoot(aggregate)
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

                // Get v5 driven ports for this aggregate
                List<io.hexaglue.arch.model.DrivenPort> portsForAggregate =
                        portsByManagedType.getOrDefault(aggregate.id().qualifiedName(), List.of());

                // Generate repository if enabled
                if (config.generateRepositories()) {
                    RepositorySpec repoSpec = RepositorySpecBuilder.builder()
                            .aggregateRoot(aggregate)
                            .drivenPorts(portsForAggregate)
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
                            .aggregateRoot(aggregate)
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
                        "Failed to generate JPA class for " + aggregate.id().simpleName(), e);
            } catch (IllegalArgumentException | IllegalStateException e) {
                diagnostics.warn("Skipping " + aggregate.id().simpleName() + ": " + e.getMessage());
            }
        }

        // Generate entities from v5 Entities (non-aggregate-root entities)
        List<Entity> allEntities = domainIndex
                .entities()
                .filter(e -> e.identityField().isPresent())
                .toList();

        for (Entity entity : allEntities) {
            try {
                EntitySpec entitySpec = EntitySpecBuilder.builder()
                        .entity(entity)
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

                // Get v5 driven ports for this entity
                List<io.hexaglue.arch.model.DrivenPort> portsForEntity =
                        portsByManagedType.getOrDefault(entity.id().qualifiedName(), List.of());

                // Generate repository if enabled
                if (config.generateRepositories()) {
                    RepositorySpec repoSpec = RepositorySpecBuilder.builder()
                            .entity(entity)
                            .drivenPorts(portsForEntity)
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
                            .entity(entity)
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

        // Generate adapters for v5 driven ports
        if (config.generateAdapters()) {
            for (io.hexaglue.arch.model.DrivenPort port :
                    portIndex.drivenPorts().toList()) {
                if (port.managedAggregate().isEmpty()) {
                    diagnostics.warn("Skipping adapter for port " + port.id().simpleName() + ": no managed type");
                    continue;
                }

                String managedTypeFqn = port.managedAggregate().get().qualifiedName();

                // Try to find the managed type as AggregateRoot first, then as Entity
                Optional<AggregateRoot> aggregateOpt = domainIndex.aggregateRoot(TypeId.of(managedTypeFqn));

                if (aggregateOpt.isPresent()) {
                    try {
                        AdapterSpec adapterSpec = AdapterSpecBuilder.builder()
                                .drivenPorts(List.of(port))
                                .aggregateRoot(aggregateOpt.get())
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
                                "Failed to generate adapter for port "
                                        + port.id().simpleName(),
                                e);
                    } catch (IllegalArgumentException | IllegalStateException e) {
                        diagnostics.warn(
                                "Skipping adapter for port " + port.id().simpleName() + ": " + e.getMessage());
                    }
                } else {
                    // Try as Entity
                    Optional<Entity> entityOpt = allEntities.stream()
                            .filter(e -> e.id().qualifiedName().equals(managedTypeFqn))
                            .findFirst();

                    if (entityOpt.isPresent()) {
                        try {
                            AdapterSpec adapterSpec = AdapterSpecBuilder.builder()
                                    .drivenPorts(List.of(port))
                                    .entity(entityOpt.get())
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
                                    "Failed to generate adapter for port "
                                            + port.id().simpleName(),
                                    e);
                        } catch (IllegalArgumentException | IllegalStateException e) {
                            diagnostics.warn(
                                    "Skipping adapter for port " + port.id().simpleName() + ": " + e.getMessage());
                        }
                    } else {
                        diagnostics.warn(
                                "Skipping adapter for port " + port.id().simpleName() + ": managed type "
                                        + managedTypeFqn + " not found as aggregate or entity");
                    }
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
     * Logs a summary of the v5.0 ArchitecturalModel classification.
     *
     * <p>This provides visibility into the classification system,
     * including classification traces for debugging and warnings for unclassified types.
     *
     * <p>Uses the new {@link DomainIndex}, {@link PortIndex}, and
     * {@link ClassificationReport} API for accurate and enriched information.
     *
     * @param model the architectural model
     * @param diagnostics the diagnostic reporter
     * @since 5.0.0
     */
    private void logClassificationSummary(ArchitecturalModel model, DiagnosticReporter diagnostics) {
        // v5.0.0: Use new DomainIndex and PortIndex
        Optional<DomainIndex> domainIndexOpt = model.domainIndex();
        Optional<PortIndex> portIndexOpt = model.portIndex();
        Optional<ClassificationReport> reportOpt = model.classificationReport();

        if (domainIndexOpt.isEmpty() || portIndexOpt.isEmpty()) {
            diagnostics.warn("v5.0.0 indices not available for classification summary");
            return;
        }

        DomainIndex domain = domainIndexOpt.get();
        PortIndex ports = portIndexOpt.get();

        long aggregateCount = domain.aggregateRoots().count();
        long entityCount = domain.entities().count();
        long valueObjectCount = domain.valueObjects().count();
        long drivenPortCount = ports.drivenPorts().count();

        diagnostics.info(String.format(
                "v5.0 Model: %d aggregates, %d entities, %d value objects, %d driven ports",
                aggregateCount, entityCount, valueObjectCount, drivenPortCount));

        // Log classification traces for aggregate roots (useful for debugging)
        domain.aggregateRoots()
                .limit(3)
                .forEach(agg -> diagnostics.info(
                        "  - " + agg.simpleName() + ": " + agg.classification().explain()));

        // v5.0.0: Use ClassificationReport for unclassified types
        reportOpt.ifPresent(report -> {
            if (report.hasIssues()) {
                diagnostics.info(String.format(
                        "Classification rate: %.1f%% (%d types need attention)",
                        report.stats().classificationRate(),
                        report.actionRequired().size()));

                report.actionRequired().stream().limit(5).forEach(unclassified -> {
                    String hint = unclassified.classification().remediationHints().stream()
                            .findFirst()
                            .map(h -> " - Hint: " + h.description())
                            .orElse("");
                    diagnostics.warn(String.format(
                            "  - %s [%s]: %s%s",
                            unclassified.simpleName(),
                            unclassified.category(),
                            unclassified.classification().explain(),
                            hint));
                });

                if (report.actionRequired().size() > 5) {
                    diagnostics.warn(String.format(
                            "  ... and %d more types need attention",
                            report.actionRequired().size() - 5));
                }
            }
        });
    }
}
