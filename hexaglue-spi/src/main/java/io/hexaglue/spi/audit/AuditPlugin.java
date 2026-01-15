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

package io.hexaglue.spi.audit;

import io.hexaglue.spi.generation.PluginCategory;
import io.hexaglue.spi.plugin.HexaGluePlugin;
import io.hexaglue.spi.plugin.PluginContext;
import java.util.List;

/**
 * Service Provider Interface for code audit plugins.
 *
 * <p>Audit plugins analyze the codebase for quality, compliance, and
 * architectural issues. They apply a set of rules to code units and produce
 * an audit snapshot with violations and metrics.
 *
 * <p>Audit plugins are discovered via {@link java.util.ServiceLoader} and
 * must be registered in {@code META-INF/services/io.hexaglue.spi.plugin.HexaGluePlugin}.
 *
 * <h2>Example Implementation</h2>
 * <pre>{@code
 * public class HexagonalArchitectureAuditPlugin implements AuditPlugin {
 *
 *     @Override
 *     public String id() {
 *         return "io.hexaglue.audit.hexagonal";
 *     }
 *
 *     @Override
 *     public AuditSnapshot audit(AuditContext context) {
 *         Codebase codebase = context.codebase();
 *         List<RuleViolation> violations = new ArrayList<>();
 *
 *         // Apply all rules to all code units
 *         for (CodeUnit unit : codebase.units()) {
 *             for (AuditRule rule : context.rules()) {
 *                 violations.addAll(rule.check(unit));
 *             }
 *         }
 *
 *         // Compute metrics
 *         QualityMetrics qualityMetrics = computeQualityMetrics(codebase);
 *         ArchitectureMetrics archMetrics = computeArchitectureMetrics(codebase);
 *
 *         return new AuditSnapshot(
 *             codebase,
 *             DetectedArchitectureStyle.HEXAGONAL,
 *             violations,
 *             qualityMetrics,
 *             archMetrics,
 *             new AuditMetadata(Instant.now(), "3.0.0", Duration.ofSeconds(5))
 *         );
 *     }
 * }
 * }</pre>
 *
 * @since 3.0.0
 */
public interface AuditPlugin extends HexaGluePlugin {

    /**
     * Performs the audit and returns the complete snapshot.
     *
     * <p>This method is called by the HexaGlue engine after the analysis phase
     * completes. Implementations should apply all configured rules to the
     * codebase and collect violations and metrics.
     *
     * @param context the audit context (never null)
     * @return the complete audit snapshot
     * @throws Exception if audit execution fails
     */
    AuditSnapshot audit(AuditContext context) throws Exception;

    /**
     * Returns the plugin category (always AUDIT for audit plugins).
     *
     * @return {@link PluginCategory#AUDIT}
     */
    @Override
    default PluginCategory category() {
        return PluginCategory.AUDIT;
    }

    /**
     * Executes the plugin (delegates to audit with context adaptation).
     *
     * <p>This method adapts the generic {@link PluginContext} to the specialized
     * {@link AuditContext} required by audit plugins. The implementation builds
     * a codebase representation from the classification snapshot.
     *
     * <p>The resulting {@link AuditSnapshot} is stored in the plugin output store
     * under the key {@code "audit-snapshot"} for retrieval by the engine/mojos.
     *
     * @param context the generic plugin context
     */
    @Override
    default void execute(PluginContext context) {
        try {
            // Build Codebase from IrSnapshot
            Codebase codebase = buildCodebaseFromIr(context.ir());

            // Create AuditContext
            // Rules list is empty - plugins should define their own rules
            AuditContext auditContext = new AuditContext(
                    codebase,
                    List.of(),
                    context.diagnostics(),
                    context.config(),
                    context.architectureQuery().orElse(null));

            // Execute the audit and capture the snapshot
            AuditSnapshot snapshot = audit(auditContext);

            // Store the snapshot for retrieval by the engine/mojos
            if (snapshot != null) {
                context.setOutput("audit-snapshot", snapshot);
            }

        } catch (Exception e) {
            context.diagnostics().error("Audit plugin execution failed: " + id(), e);
        }
    }

    /**
     * Builds a Codebase from an IrSnapshot.
     *
     * <p>Converts domain types and ports into CodeUnits suitable for audit analysis.
     * This is a best-effort conversion - detailed metrics, method bodies, and
     * dependencies are not available from the IR and are filled with placeholder values.
     *
     * @param ir the IR snapshot
     * @return the constructed codebase
     */
    private static Codebase buildCodebaseFromIr(io.hexaglue.spi.ir.IrSnapshot ir) {
        java.util.List<CodeUnit> units = new java.util.ArrayList<>();

        // Convert domain types to code units
        for (io.hexaglue.spi.ir.DomainType type : ir.domain().types()) {
            units.add(convertDomainTypeToCodeUnit(type));
        }

        // Convert ports to code units
        for (io.hexaglue.spi.ir.Port port : ir.ports().ports()) {
            units.add(convertPortToCodeUnit(port));
        }

        // Extract dependency information (simplified - no detailed call graph available from IR)
        java.util.Map<String, java.util.Set<String>> dependencies = extractDependenciesFromIr(ir);

        // Infer base package from units
        String basePackage = inferBasePackageFromUnits(units, ir.metadata().basePackage());

        return new Codebase("audit-target", basePackage, units, dependencies);
    }

    /**
     * Converts a DomainType to a CodeUnit for audit purposes.
     */
    private static CodeUnit convertDomainTypeToCodeUnit(io.hexaglue.spi.ir.DomainType type) {
        // Determine layer classification based on domain kind
        LayerClassification layer = determineLayerFromDomainKind(type.kind());

        // Convert domain kind to role classification
        RoleClassification role = convertDomainKindToRole(type.kind());

        // Convert Java construct to CodeUnitKind
        CodeUnitKind kind = convertJavaConstructToCodeUnitKind(type.construct());

        // Extract method information from properties (simplified)
        java.util.List<MethodDeclaration> methods = extractMethodsFromDomainType(type);

        // Extract field information from properties
        java.util.List<FieldDeclaration> fields = extractFieldsFromDomainType(type);

        // Create placeholder metrics (actual values would require AST analysis)
        CodeMetrics metrics = new CodeMetrics(
                0, // linesOfCode - not available from IR
                0, // cyclomaticComplexity - not available from IR
                methods.size(),
                fields.size(),
                100.0 // maintainabilityIndex - assume good by default
                );

        // Create placeholder documentation info
        DocumentationInfo documentation = new DocumentationInfo(
                false, // hasJavadoc - not available from IR
                0, // javadocCoverage - not available from IR
                java.util.List.of());

        return new CodeUnit(type.qualifiedName(), kind, layer, role, methods, fields, metrics, documentation);
    }

    /**
     * Converts a Port to a CodeUnit for audit purposes.
     */
    private static CodeUnit convertPortToCodeUnit(io.hexaglue.spi.ir.Port port) {
        // Ports are always interfaces in the application layer
        LayerClassification layer = LayerClassification.APPLICATION;

        // Convert port kind to role classification
        RoleClassification role = convertPortKindToRole(port.kind());

        // Ports are always interfaces
        CodeUnitKind kind = CodeUnitKind.INTERFACE;

        // Convert port methods
        java.util.List<MethodDeclaration> methods = extractMethodsFromPort(port);

        // Ports don't have fields
        java.util.List<FieldDeclaration> fields = java.util.List.of();

        // Create placeholder metrics
        CodeMetrics metrics = new CodeMetrics(0, 0, methods.size(), 0, 100.0);

        // Create placeholder documentation info
        DocumentationInfo documentation = new DocumentationInfo(false, 0, java.util.List.of());

        return new CodeUnit(port.qualifiedName(), kind, layer, role, methods, fields, metrics, documentation);
    }

    /**
     * Determines the architectural layer from a domain kind.
     */
    private static LayerClassification determineLayerFromDomainKind(io.hexaglue.spi.ir.DomainKind kind) {
        return switch (kind) {
            case AGGREGATE_ROOT, ENTITY, VALUE_OBJECT, IDENTIFIER, DOMAIN_EVENT, DOMAIN_SERVICE ->
                LayerClassification.DOMAIN;
            case APPLICATION_SERVICE, INBOUND_ONLY, OUTBOUND_ONLY, SAGA -> LayerClassification.APPLICATION;
            case UNCLASSIFIED -> LayerClassification.UNKNOWN;
        };
    }

    /**
     * Converts a DomainKind to RoleClassification.
     */
    private static RoleClassification convertDomainKindToRole(io.hexaglue.spi.ir.DomainKind kind) {
        return switch (kind) {
            case AGGREGATE_ROOT -> RoleClassification.AGGREGATE_ROOT;
            case ENTITY -> RoleClassification.ENTITY;
            case VALUE_OBJECT -> RoleClassification.VALUE_OBJECT;
            case IDENTIFIER -> RoleClassification.VALUE_OBJECT; // Map IDENTIFIER to VALUE_OBJECT
            case DOMAIN_EVENT -> RoleClassification.VALUE_OBJECT; // Map DOMAIN_EVENT to VALUE_OBJECT
            case DOMAIN_SERVICE -> RoleClassification.SERVICE;
            case APPLICATION_SERVICE -> RoleClassification.USE_CASE;
            case INBOUND_ONLY, OUTBOUND_ONLY, SAGA -> RoleClassification.SERVICE;
            case UNCLASSIFIED -> RoleClassification.UNKNOWN;
        };
    }

    /**
     * Converts a PortKind to RoleClassification.
     */
    private static RoleClassification convertPortKindToRole(io.hexaglue.spi.ir.PortKind kind) {
        return switch (kind) {
            case REPOSITORY -> RoleClassification.REPOSITORY;
            case USE_CASE, COMMAND, QUERY -> RoleClassification.USE_CASE;
            case GATEWAY, EVENT_PUBLISHER, GENERIC -> RoleClassification.PORT;
        };
    }

    /**
     * Converts JavaConstruct to CodeUnitKind.
     */
    private static CodeUnitKind convertJavaConstructToCodeUnitKind(io.hexaglue.spi.ir.JavaConstruct construct) {
        return switch (construct) {
            case CLASS -> CodeUnitKind.CLASS;
            case INTERFACE -> CodeUnitKind.INTERFACE;
            case RECORD -> CodeUnitKind.RECORD;
            case ENUM -> CodeUnitKind.ENUM;
        };
    }

    /**
     * Extracts method declarations from a domain type (simplified).
     */
    private static java.util.List<MethodDeclaration> extractMethodsFromDomainType(io.hexaglue.spi.ir.DomainType type) {
        // The IR doesn't contain full method signatures, only properties
        // Return getters/setters for properties as a placeholder
        java.util.List<MethodDeclaration> methods = new java.util.ArrayList<>();

        for (io.hexaglue.spi.ir.DomainProperty prop : type.properties()) {
            // Create a getter method
            String getterName = "get" + capitalize(prop.name());
            methods.add(new MethodDeclaration(
                    getterName,
                    prop.type().qualifiedName(),
                    java.util.List.of(),
                    java.util.Set.of("public"),
                    java.util.Set.of(),
                    1 // Simple getter has complexity 1
                    ));
        }

        return methods;
    }

    /**
     * Extracts field declarations from a domain type.
     */
    private static java.util.List<FieldDeclaration> extractFieldsFromDomainType(io.hexaglue.spi.ir.DomainType type) {
        java.util.List<FieldDeclaration> fields = new java.util.ArrayList<>();

        for (io.hexaglue.spi.ir.DomainProperty prop : type.properties()) {
            fields.add(new FieldDeclaration(
                    prop.name(), prop.type().qualifiedName(), java.util.Set.of("private"), java.util.Set.of()));
        }

        return fields;
    }

    /**
     * Extracts method declarations from a port.
     */
    private static java.util.List<MethodDeclaration> extractMethodsFromPort(io.hexaglue.spi.ir.Port port) {
        java.util.List<MethodDeclaration> methods = new java.util.ArrayList<>();

        for (io.hexaglue.spi.ir.PortMethod method : port.methods()) {
            java.util.List<String> parameterTypes = method.parameters().stream()
                    .map(p -> p.type().qualifiedName())
                    .toList();
            methods.add(new MethodDeclaration(
                    method.name(),
                    method.returnType().qualifiedName(),
                    parameterTypes,
                    java.util.Set.of("public", "abstract"),
                    java.util.Set.of(),
                    1 // Interface methods have complexity 1
                    ));
        }

        return methods;
    }

    /**
     * Extracts dependency information from the IR snapshot.
     * This is simplified as the IR doesn't contain full call graph information.
     */
    private static java.util.Map<String, java.util.Set<String>> extractDependenciesFromIr(
            io.hexaglue.spi.ir.IrSnapshot ir) {
        java.util.Map<String, java.util.Set<String>> dependencies = new java.util.HashMap<>();

        // Extract dependencies from domain relations
        for (io.hexaglue.spi.ir.DomainType type : ir.domain().types()) {
            java.util.Set<String> typeDeps = new java.util.HashSet<>();

            // Add dependencies from relations
            for (io.hexaglue.spi.ir.DomainRelation relation : type.relations()) {
                typeDeps.add(relation.targetTypeFqn());
            }

            // Add dependencies from property types
            for (io.hexaglue.spi.ir.DomainProperty prop : type.properties()) {
                typeDeps.add(prop.type().qualifiedName());
            }

            if (!typeDeps.isEmpty()) {
                dependencies.put(type.qualifiedName(), typeDeps);
            }
        }

        // Extract dependencies from port managed types
        for (io.hexaglue.spi.ir.Port port : ir.ports().ports()) {
            java.util.Set<String> portDeps = new java.util.HashSet<>(port.managedTypes());

            if (!portDeps.isEmpty()) {
                dependencies.put(port.qualifiedName(), portDeps);
            }
        }

        return dependencies;
    }

    /**
     * Infers the base package from code units.
     */
    private static String inferBasePackageFromUnits(java.util.List<CodeUnit> units, String fallback) {
        if (units.isEmpty()) {
            return fallback;
        }

        // Find the common prefix of all package names
        String firstPackage = units.get(0).packageName();
        String commonPrefix = firstPackage;

        for (CodeUnit unit : units) {
            String pkg = unit.packageName();
            while (!pkg.startsWith(commonPrefix) && commonPrefix.contains(".")) {
                int lastDot = commonPrefix.lastIndexOf('.');
                commonPrefix = lastDot > 0 ? commonPrefix.substring(0, lastDot) : "";
            }
        }

        return commonPrefix.isEmpty() ? fallback : commonPrefix;
    }

    /**
     * Capitalizes the first letter of a string.
     */
    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
