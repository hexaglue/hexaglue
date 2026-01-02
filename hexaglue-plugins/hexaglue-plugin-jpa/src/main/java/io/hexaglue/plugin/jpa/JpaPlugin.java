package io.hexaglue.plugin.jpa;

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
import java.util.List;
import java.util.Optional;

/**
 * JPA plugin for HexaGlue.
 *
 * <p>Generates complete JPA infrastructure from domain types:
 * <ul>
 *   <li>Entities and Aggregate Roots → {@code @Entity} classes</li>
 *   <li>Value Objects → {@code @Embeddable} classes</li>
 *   <li>Repository interfaces → Spring Data JPA repositories</li>
 *   <li>Mappers → MapStruct mapper interfaces</li>
 *   <li>Adapters → Port implementations with transactions</li>
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
 */
public final class JpaPlugin implements HexaGluePlugin {

    /** Plugin identifier. */
    public static final String PLUGIN_ID = "io.hexaglue.plugin.jpa";

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

        // Create generators
        JpaEntityGenerator entityGenerator = new JpaEntityGenerator(infraPackage, config);
        JpaRepositoryGenerator repositoryGenerator = new JpaRepositoryGenerator(infraPackage, config);
        JpaMapperGenerator mapperGenerator = new JpaMapperGenerator(infraPackage, basePackage, config);
        JpaAdapterGenerator adapterGenerator = new JpaAdapterGenerator(infraPackage, config);

        // Collect all domain types for mapper references
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
                if (type.isEntity()) {
                    // Generate JPA entity
                    String entityName = type.simpleName() + config.entitySuffix();
                    String source = entityGenerator.generateEntity(type);
                    writer.writeJavaSource(infraPackage, entityName, source);
                    entityCount++;
                    diagnostics.info("Generated entity: " + entityName);

                    // Generate repository if enabled
                    if (config.generateRepositories()) {
                        String repoName = type.simpleName() + config.repositorySuffix();
                        String repoSource = repositoryGenerator.generateRepository(type);
                        writer.writeJavaSource(infraPackage, repoName, repoSource);
                        repositoryCount++;
                        diagnostics.info("Generated repository: " + repoName);
                    }

                    // Generate mapper if enabled
                    if (config.generateMappers()) {
                        String mapperName = type.simpleName() + config.mapperSuffix();
                        String mapperSource = mapperGenerator.generateMapper(type, allTypes);
                        writer.writeJavaSource(infraPackage, mapperName, mapperSource);
                        mapperCount++;
                        diagnostics.info("Generated mapper: " + mapperName);
                    }

                } else if (type.isValueObject()) {
                    // Generate JPA embeddable
                    String embeddableName = type.simpleName() + "Embeddable";
                    String source = entityGenerator.generateEmbeddable(type);
                    writer.writeJavaSource(infraPackage, embeddableName, source);
                    embeddableCount++;
                    diagnostics.info("Generated embeddable: " + embeddableName);

                    // Generate value object mapper if enabled
                    if (config.generateMappers()) {
                        String mapperName = type.simpleName() + config.mapperSuffix();
                        String mapperSource = mapperGenerator.generateValueObjectMapper(type);
                        writer.writeJavaSource(infraPackage, mapperName, mapperSource);
                        mapperCount++;
                        diagnostics.info("Generated VO mapper: " + mapperName);
                    }
                }

            } catch (IOException e) {
                diagnostics.error("Failed to generate JPA class for " + type.simpleName(), e);
            }
        }

        // Generate adapters for repository ports
        if (config.generateAdapters()) {
            for (Port port : ir.ports().ports()) {
                if (port.kind() == PortKind.REPOSITORY && port.isDriven()) {
                    try {
                        Optional<DomainType> managedType =
                                findManagedType(port, ir.domain().types());
                        // Only generate adapter if managed type is an entity (not identifier/value object)
                        if (managedType.isPresent() && managedType.get().isEntity()) {
                            String adapterName = managedType.get().simpleName() + config.adapterSuffix();
                            String adapterSource = adapterGenerator.generateAdapter(port, managedType.get());
                            writer.writeJavaSource(infraPackage, adapterName, adapterSource);
                            adapterCount++;
                            diagnostics.info("Generated adapter: " + adapterName);
                        } else if (managedType.isEmpty()) {
                            diagnostics.warn("Could not determine managed type for port: " + port.simpleName());
                        }
                        // Skip silently if managed type is not an entity (e.g., identifier, value object)
                    } catch (IOException e) {
                        diagnostics.error("Failed to generate adapter for " + port.simpleName(), e);
                    }
                }
            }
        }

        // Summary
        diagnostics.info(String.format(
                "JPA generation complete: %d entities, %d embeddables, %d repositories, %d mappers, %d adapters",
                entityCount, embeddableCount, repositoryCount, mapperCount, adapterCount));
    }

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
