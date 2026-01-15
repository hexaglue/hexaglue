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

package io.hexaglue.spi.ir.testing;

import io.hexaglue.spi.ir.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Fluent builder for creating {@link Port} instances in tests.
 *
 * <p>Example usage:
 * <pre>{@code
 * Port repo = PortBuilder.repository("com.example.OrderRepository")
 *     .managing("com.example.Order")
 *     .withMethod("save", "com.example.Order", "com.example.Order")
 *     .withMethod("findById", "java.util.Optional<com.example.Order>", "com.example.OrderId")
 *     .build();
 * }</pre>
 */
public final class PortBuilder {

    private String qualifiedName;
    private String simpleName;
    private String packageName;
    private PortKind kind = PortKind.REPOSITORY;
    private PortDirection direction = PortDirection.DRIVEN;
    private ConfidenceLevel confidence = ConfidenceLevel.HIGH;
    private final List<String> managedTypes = new ArrayList<>();
    private String primaryManagedType;
    private final List<PortMethod> methods = new ArrayList<>();
    private final List<String> annotations = new ArrayList<>();
    private SourceRef sourceRef;

    private PortBuilder() {}

    // =========================================================================
    // Factory methods
    // =========================================================================

    /**
     * Creates a builder for a repository port.
     */
    public static PortBuilder repository(String qualifiedName) {
        return new PortBuilder()
                .qualifiedName(qualifiedName)
                .kind(PortKind.REPOSITORY)
                .direction(PortDirection.DRIVEN);
    }

    /**
     * Creates a builder for a use case (driving) port.
     */
    public static PortBuilder useCase(String qualifiedName) {
        return new PortBuilder()
                .qualifiedName(qualifiedName)
                .kind(PortKind.USE_CASE)
                .direction(PortDirection.DRIVING);
    }

    /**
     * Creates a builder for a gateway (external service) port.
     */
    public static PortBuilder gateway(String qualifiedName) {
        return new PortBuilder()
                .qualifiedName(qualifiedName)
                .kind(PortKind.GATEWAY)
                .direction(PortDirection.DRIVEN);
    }

    /**
     * Creates a builder for a query port.
     */
    public static PortBuilder query(String qualifiedName) {
        return new PortBuilder()
                .qualifiedName(qualifiedName)
                .kind(PortKind.QUERY)
                .direction(PortDirection.DRIVING);
    }

    // =========================================================================
    // Configuration
    // =========================================================================

    /**
     * Sets the qualified name and derives simple name and package.
     */
    public PortBuilder qualifiedName(String qualifiedName) {
        this.qualifiedName = qualifiedName;
        int lastDot = qualifiedName.lastIndexOf('.');
        this.simpleName = lastDot > 0 ? qualifiedName.substring(lastDot + 1) : qualifiedName;
        this.packageName = lastDot > 0 ? qualifiedName.substring(0, lastDot) : "";
        return this;
    }

    /**
     * Sets the port kind.
     */
    public PortBuilder kind(PortKind kind) {
        this.kind = kind;
        return this;
    }

    /**
     * Sets the port direction.
     */
    public PortBuilder direction(PortDirection direction) {
        this.direction = direction;
        return this;
    }

    /**
     * Sets the confidence level.
     */
    public PortBuilder confidence(ConfidenceLevel confidence) {
        this.confidence = confidence;
        return this;
    }

    // =========================================================================
    // Managed types
    // =========================================================================

    /**
     * Adds a managed type to this port.
     */
    public PortBuilder managing(String typeFqn) {
        managedTypes.add(typeFqn);
        return this;
    }

    /**
     * Adds multiple managed types to this port.
     */
    public PortBuilder managing(String... types) {
        managedTypes.addAll(List.of(types));
        return this;
    }

    /**
     * Sets the primary managed type (the main aggregate managed by this port).
     */
    public PortBuilder primaryManaging(String typeFqn) {
        this.primaryManagedType = typeFqn;
        if (!managedTypes.contains(typeFqn)) {
            managedTypes.add(typeFqn);
        }
        return this;
    }

    // =========================================================================
    // Methods
    // =========================================================================

    /**
     * Adds a method to this port.
     *
     * @param name the method name
     * @param returnType the return type
     * @param parameterTypes the parameter types
     */
    public PortBuilder withMethod(String name, String returnType, String... parameterTypes) {
        methods.add(PortMethod.legacy(name, returnType, List.of(parameterTypes)));
        return this;
    }

    /**
     * Adds a void method to this port.
     */
    public PortBuilder withVoidMethod(String name, String... parameterTypes) {
        return withMethod(name, "void", parameterTypes);
    }

    /**
     * Adds a finder method (returns Optional).
     */
    public PortBuilder withFindByIdMethod(String entityType, String idType) {
        String returnType = "java.util.Optional<" + entityType + ">";
        methods.add(PortMethod.legacy("findById", returnType, List.of(idType)));
        return this;
    }

    /**
     * Adds a findAll method.
     */
    public PortBuilder withFindAllMethod(String entityType) {
        String returnType = "java.util.List<" + entityType + ">";
        methods.add(PortMethod.legacy("findAll", returnType, List.of()));
        return this;
    }

    /**
     * Adds a save method.
     */
    public PortBuilder withSaveMethod(String entityType) {
        methods.add(PortMethod.legacy("save", entityType, List.of(entityType)));
        return this;
    }

    /**
     * Adds a delete method.
     */
    public PortBuilder withDeleteMethod(String idType) {
        methods.add(PortMethod.legacy("delete", "void", List.of(idType)));
        return this;
    }

    // =========================================================================
    // Annotations
    // =========================================================================

    /**
     * Adds an annotation.
     */
    public PortBuilder withAnnotation(String annotationFqn) {
        annotations.add(annotationFqn);
        return this;
    }

    /**
     * Adds a jMolecules @Repository annotation.
     */
    public PortBuilder withJMoleculesRepository() {
        return withAnnotation("org.jmolecules.ddd.annotation.Repository");
    }

    // =========================================================================
    // Source Reference
    // =========================================================================

    /**
     * Sets the source reference.
     */
    public PortBuilder withSourceRef(String filePath, int line) {
        this.sourceRef = SourceRef.ofLine(filePath, line);
        return this;
    }

    // =========================================================================
    // Build
    // =========================================================================

    /**
     * Builds the Port.
     */
    public Port build() {
        if (qualifiedName == null) {
            throw new IllegalStateException("qualifiedName is required");
        }

        SourceRef ref = sourceRef != null ? sourceRef : SourceRef.ofLine(simpleName + ".java", 1);

        // Derive primaryManagedType from first managed type if not explicitly set
        String primary = primaryManagedType;
        if (primary == null && !managedTypes.isEmpty()) {
            primary = managedTypes.get(0);
        }

        return new Port(
                qualifiedName,
                simpleName,
                packageName,
                kind,
                direction,
                confidence,
                List.copyOf(managedTypes),
                primary,
                List.copyOf(methods),
                List.copyOf(annotations),
                ref);
    }
}
