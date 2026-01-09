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
import io.hexaglue.plugin.jpa.builder.EntitySpecBuilder;
import io.hexaglue.plugin.jpa.builder.MapperSpecBuilder;
import io.hexaglue.plugin.jpa.builder.RepositorySpecBuilder;
import io.hexaglue.plugin.jpa.codegen.JpaAdapterCodegen;
import io.hexaglue.plugin.jpa.codegen.JpaEntityCodegen;
import io.hexaglue.plugin.jpa.codegen.JpaMapperCodegen;
import io.hexaglue.plugin.jpa.codegen.JpaRepositoryCodegen;
import io.hexaglue.plugin.jpa.model.AdapterSpec;
import io.hexaglue.plugin.jpa.model.EntitySpec;
import io.hexaglue.plugin.jpa.model.MapperSpec;
import io.hexaglue.plugin.jpa.model.RepositorySpec;
import io.hexaglue.spi.ir.DomainType;
import io.hexaglue.spi.ir.IrSnapshot;
import io.hexaglue.spi.ir.Port;
import io.hexaglue.spi.ir.PortKind;
import io.hexaglue.spi.plugin.CodeWriter;
import io.hexaglue.spi.plugin.DiagnosticReporter;
import io.hexaglue.spi.plugin.HexaGluePlugin;
import io.hexaglue.spi.plugin.PluginConfig;
import io.hexaglue.spi.plugin.PluginContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
public final class JpaPlugin implements HexaGluePlugin {

    /** Plugin identifier. */
    public static final String PLUGIN_ID = "io.hexaglue.plugin.jpa";

    /** Indentation for generated code (4 spaces). */
    private static final String INDENT = "    ";

    @Override
    public String id() {
        return PLUGIN_ID;
    }

    @Override
    public void execute(PluginContext context) {
        IrSnapshot ir = context.ir();
        PluginConfig pluginConfig = context.config();
        CodeWriter writer = context.writer();
        DiagnosticReporter diagnostics = context.diagnostics();

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

        // Counters for summary
        int entityCount = 0;
        int embeddableCount = 0;
        int repositoryCount = 0;
        int mapperCount = 0;
        int adapterCount = 0;

        // Generate entities and embeddables
        for (DomainType type : ir.domain().types()) {
            try {
                if (type.isEntity() && type.hasIdentity()) {
                    // Build entity specification
                    EntitySpec entitySpec = EntitySpecBuilder.builder()
                            .domainType(type)
                            .config(config)
                            .infrastructurePackage(infraPackage)
                            .allTypes(allTypes)
                            .build();

                    // Generate and write JPA entity
                    TypeSpec entityTypeSpec = JpaEntityCodegen.generate(entitySpec);
                    String entitySource = toJavaSource(infraPackage, entityTypeSpec);
                    writer.writeJavaSource(infraPackage, entitySpec.className(), entitySource);
                    entityCount++;
                    diagnostics.info("Generated entity: " + entitySpec.className());

                    // Generate repository if enabled
                    if (config.generateRepositories()) {
                        RepositorySpec repoSpec = RepositorySpecBuilder.builder()
                                .domainType(type)
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
                                .domainType(type)
                                .config(config)
                                .infrastructurePackage(infraPackage)
                                .allTypes(allTypes)
                                .build();

                        TypeSpec mapperTypeSpec = JpaMapperCodegen.generate(mapperSpec);
                        String mapperSource = toJavaSource(infraPackage, mapperTypeSpec);
                        writer.writeJavaSource(infraPackage, mapperSpec.interfaceName(), mapperSource);
                        mapperCount++;
                        diagnostics.info("Generated mapper: " + mapperSpec.interfaceName());
                    }

                } else if (type.isValueObject()) {
                    // Value objects (embeddables) are handled implicitly through entity relations
                    // Skip explicit generation for now - they are embedded
                    embeddableCount++;
                    diagnostics.info("Detected embeddable value object: " + type.simpleName());
                }

            } catch (IOException e) {
                diagnostics.error("Failed to generate JPA class for " + type.simpleName(), e);
            } catch (IllegalArgumentException | IllegalStateException e) {
                diagnostics.warn("Skipping " + type.simpleName() + ": " + e.getMessage());
            }
        }

        // Generate adapters for repository ports
        // Group ports by managed type to merge multiple ports for the same type into one adapter
        if (config.generateAdapters()) {
            // Group ports by managed domain type
            Map<DomainType, List<Port>> portsByManagedType = new HashMap<>();

            for (Port port : ir.ports().ports()) {
                if (port.kind() == PortKind.REPOSITORY && port.isDriven()) {
                    Optional<DomainType> managedType =
                            findManagedType(port, ir.domain().types());
                    if (managedType.isPresent() && managedType.get().isEntity()) {
                        portsByManagedType
                                .computeIfAbsent(managedType.get(), k -> new ArrayList<>())
                                .add(port);
                    } else if (managedType.isEmpty()) {
                        diagnostics.warn("Could not determine managed type for port: " + port.simpleName());
                    }
                }
            }

            // Generate one adapter per managed type, implementing ALL ports for that type
            for (Map.Entry<DomainType, List<Port>> entry : portsByManagedType.entrySet()) {
                DomainType managedType = entry.getKey();
                List<Port> ports = entry.getValue();

                try {
                    AdapterSpec adapterSpec = AdapterSpecBuilder.builder()
                            .ports(ports)
                            .domainType(managedType)
                            .config(config)
                            .infrastructurePackage(infraPackage)
                            .build();

                    TypeSpec adapterTypeSpec = JpaAdapterCodegen.generate(adapterSpec);
                    String adapterSource = toJavaSource(infraPackage, adapterTypeSpec);
                    writer.writeJavaSource(infraPackage, adapterSpec.className(), adapterSource);
                    adapterCount++;

                    String portNames = ports.stream()
                            .map(Port::simpleName)
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("");
                    diagnostics.info("Generated adapter: " + adapterSpec.className() + " implementing " + portNames);
                } catch (IOException e) {
                    diagnostics.error("Failed to generate adapter for " + managedType.simpleName(), e);
                } catch (IllegalArgumentException | IllegalStateException e) {
                    diagnostics.warn("Skipping adapter for " + managedType.simpleName() + ": " + e.getMessage());
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
}
