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

package io.hexaglue.plugin.audit.util;

import io.hexaglue.spi.audit.CodeMetrics;
import io.hexaglue.spi.audit.CodeUnit;
import io.hexaglue.spi.audit.CodeUnitKind;
import io.hexaglue.spi.audit.Codebase;
import io.hexaglue.spi.audit.DocumentationInfo;
import io.hexaglue.spi.audit.FieldDeclaration;
import io.hexaglue.spi.audit.LayerClassification;
import io.hexaglue.spi.audit.MethodDeclaration;
import io.hexaglue.spi.audit.RoleClassification;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builder utility for creating test Codebase instances.
 *
 * <p>This utility provides fluent methods for creating mock codebase structures
 * for testing validators. It simplifies test setup by providing sensible defaults
 * and convenience methods for common scenarios.
 *
 * @since 1.0.0
 */
public class TestCodebaseBuilder {

    private final List<CodeUnit> units = new ArrayList<>();
    private final Map<String, Set<String>> dependencies = new HashMap<>();
    private String name = "test-project";
    private String basePackage = "com.example.domain";

    /**
     * Sets the codebase name.
     *
     * @param name the codebase name
     * @return this builder
     */
    public TestCodebaseBuilder name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Sets the base package.
     *
     * @param basePackage the base package
     * @return this builder
     */
    public TestCodebaseBuilder basePackage(String basePackage) {
        this.basePackage = basePackage;
        return this;
    }

    /**
     * Adds a code unit to the codebase.
     *
     * @param unit the code unit
     * @return this builder
     */
    public TestCodebaseBuilder addUnit(CodeUnit unit) {
        this.units.add(unit);
        return this;
    }

    /**
     * Adds a dependency between two units.
     *
     * @param from the source unit qualified name
     * @param to   the target unit qualified name
     * @return this builder
     */
    public TestCodebaseBuilder addDependency(String from, String to) {
        dependencies.computeIfAbsent(from, k -> new HashSet<>()).add(to);
        return this;
    }

    /**
     * Builds the codebase.
     *
     * @return a new Codebase instance
     */
    public Codebase build() {
        return new Codebase(name, basePackage, units, dependencies);
    }

    // === Static Factory Methods ===

    /**
     * Creates a codebase with the given units.
     *
     * @param units the code units
     * @return a new Codebase instance
     */
    public static Codebase withUnits(CodeUnit... units) {
        TestCodebaseBuilder builder = new TestCodebaseBuilder();
        for (CodeUnit unit : units) {
            builder.addUnit(unit);
        }
        return builder.build();
    }

    /**
     * Creates an entity code unit.
     *
     * @param simpleName the simple name
     * @param hasId      whether the entity has an identity field
     * @return a new CodeUnit representing an entity
     */
    public static CodeUnit entity(String simpleName, boolean hasId) {
        String qualifiedName = "com.example.domain." + simpleName;
        List<FieldDeclaration> fields = new ArrayList<>();

        if (hasId) {
            fields.add(new FieldDeclaration("id", "java.lang.Long", Set.of("private"), Set.of("javax.persistence.Id")));
        }

        return new CodeUnit(
                qualifiedName,
                CodeUnitKind.CLASS,
                LayerClassification.DOMAIN,
                RoleClassification.ENTITY,
                List.of(),
                fields,
                defaultMetrics(),
                defaultDocumentation());
    }

    /**
     * Creates an aggregate root code unit.
     *
     * @param simpleName the simple name
     * @return a new CodeUnit representing an aggregate root
     */
    public static CodeUnit aggregate(String simpleName) {
        return aggregate(simpleName, true);
    }

    /**
     * Creates an aggregate root code unit.
     *
     * @param simpleName the simple name
     * @param hasId      whether the aggregate has an identity field
     * @return a new CodeUnit representing an aggregate root
     */
    public static CodeUnit aggregate(String simpleName, boolean hasId) {
        String qualifiedName = "com.example.domain." + simpleName;
        List<FieldDeclaration> fields = new ArrayList<>();

        if (hasId) {
            fields.add(new FieldDeclaration("id", "java.lang.Long", Set.of("private"), Set.of("javax.persistence.Id")));
        }

        return new CodeUnit(
                qualifiedName,
                CodeUnitKind.CLASS,
                LayerClassification.DOMAIN,
                RoleClassification.AGGREGATE_ROOT,
                List.of(),
                fields,
                defaultMetrics(),
                defaultDocumentation());
    }

    /**
     * Creates a value object code unit.
     *
     * @param simpleName the simple name
     * @param hasSetter  whether the value object has setter methods
     * @return a new CodeUnit representing a value object
     */
    public static CodeUnit valueObject(String simpleName, boolean hasSetter) {
        String qualifiedName = "com.example.domain." + simpleName;
        List<MethodDeclaration> methods = new ArrayList<>();

        if (hasSetter) {
            methods.add(new MethodDeclaration(
                    "setValue", "void", List.of("java.lang.String"), Set.of("public"), Set.of(), 1));
        }

        return new CodeUnit(
                qualifiedName,
                CodeUnitKind.CLASS,
                LayerClassification.DOMAIN,
                RoleClassification.VALUE_OBJECT,
                methods,
                List.of(),
                defaultMetrics(),
                defaultDocumentation());
    }

    /**
     * Creates a port code unit.
     *
     * @param simpleName the simple name
     * @param kind       the code unit kind (INTERFACE or CLASS)
     * @return a new CodeUnit representing a port
     */
    public static CodeUnit port(String simpleName, CodeUnitKind kind) {
        String qualifiedName = "com.example.domain.port." + simpleName;
        return new CodeUnit(
                qualifiedName,
                kind,
                LayerClassification.DOMAIN,
                RoleClassification.PORT,
                List.of(),
                List.of(),
                defaultMetrics(),
                defaultDocumentation());
    }

    /**
     * Creates a repository code unit for an aggregate.
     *
     * @param aggregateSimpleName the aggregate simple name
     * @return a new CodeUnit representing a repository
     */
    public static CodeUnit repository(String aggregateSimpleName) {
        String qualifiedName = "com.example.domain.port." + aggregateSimpleName + "Repository";
        return new CodeUnit(
                qualifiedName,
                CodeUnitKind.INTERFACE,
                LayerClassification.DOMAIN,
                RoleClassification.REPOSITORY,
                List.of(),
                List.of(),
                defaultMetrics(),
                defaultDocumentation());
    }

    /**
     * Creates a domain code unit (generic domain layer).
     *
     * @param simpleName the simple name
     * @return a new CodeUnit in the domain layer
     */
    public static CodeUnit domainClass(String simpleName) {
        String qualifiedName = "com.example.domain." + simpleName;
        return new CodeUnit(
                qualifiedName,
                CodeUnitKind.CLASS,
                LayerClassification.DOMAIN,
                RoleClassification.SERVICE,
                List.of(),
                List.of(),
                defaultMetrics(),
                defaultDocumentation());
    }

    /**
     * Creates an infrastructure code unit.
     *
     * @param simpleName the simple name
     * @return a new CodeUnit in the infrastructure layer
     */
    public static CodeUnit infraClass(String simpleName) {
        String qualifiedName = "com.example.infrastructure." + simpleName;
        return new CodeUnit(
                qualifiedName,
                CodeUnitKind.CLASS,
                LayerClassification.INFRASTRUCTURE,
                RoleClassification.ADAPTER,
                List.of(),
                List.of(),
                defaultMetrics(),
                defaultDocumentation());
    }

    /**
     * Creates an application code unit.
     *
     * @param simpleName the simple name
     * @return a new CodeUnit in the application layer
     */
    public static CodeUnit applicationClass(String simpleName) {
        String qualifiedName = "com.example.application." + simpleName;
        return new CodeUnit(
                qualifiedName,
                CodeUnitKind.CLASS,
                LayerClassification.APPLICATION,
                RoleClassification.USE_CASE,
                List.of(),
                List.of(),
                defaultMetrics(),
                defaultDocumentation());
    }

    /**
     * Adds a simple code unit with the given qualified name.
     *
     * <p>Creates a domain-layer class unit for the given qualified name.
     * This is useful for tests that need to match model types with codebase units.
     *
     * @param qualifiedName the fully qualified class name
     * @return this builder
     */
    public TestCodebaseBuilder addType(String qualifiedName) {
        return addType(qualifiedName, LayerClassification.DOMAIN);
    }

    /**
     * Adds a simple code unit with the given qualified name and layer.
     *
     * @param qualifiedName the fully qualified class name
     * @param layer         the layer classification
     * @return this builder
     */
    public TestCodebaseBuilder addType(String qualifiedName, LayerClassification layer) {
        CodeUnit unit = new CodeUnit(
                qualifiedName,
                CodeUnitKind.CLASS,
                layer,
                layer == LayerClassification.DOMAIN ? RoleClassification.ENTITY : RoleClassification.SERVICE,
                List.of(),
                List.of(),
                defaultMetrics(),
                defaultDocumentation());
        return addUnit(unit);
    }

    // === Helper Methods ===

    /**
     * Creates default metrics for test units.
     *
     * @return default CodeMetrics
     */
    private static CodeMetrics defaultMetrics() {
        return new CodeMetrics(50, 5, 3, 2, 80.0);
    }

    /**
     * Creates default documentation info for test units.
     *
     * @return default DocumentationInfo
     */
    private static DocumentationInfo defaultDocumentation() {
        return new DocumentationInfo(true, 100, List.of());
    }
}
