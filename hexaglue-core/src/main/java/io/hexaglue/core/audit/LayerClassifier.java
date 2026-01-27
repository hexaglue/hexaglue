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

package io.hexaglue.core.audit;

import io.hexaglue.arch.model.audit.LayerClassification;
import java.util.List;
import java.util.Set;

/**
 * Classifies types into architectural layers based on naming conventions,
 * annotations, and structural patterns.
 *
 * <p>Classification uses a multi-pass strategy:
 * <ol>
 *   <li>Package naming conventions (.presentation., .ui., .domain., etc.)</li>
 *   <li>Type name suffixes (Controller, Service, Repository, etc.)</li>
 *   <li>Annotations (@Controller, @Service, @Repository, etc.)</li>
 * </ol>
 *
 * <p>Example usage:
 * <pre>{@code
 * LayerClassifier classifier = new LayerClassifier();
 * LayerClassification layer = classifier.classify(
 *     "com.example.presentation.OrderController",
 *     "OrderController",
 *     List.of("org.springframework.stereotype.Controller")
 * );
 * // Returns LayerClassification.PRESENTATION
 * }</pre>
 *
 * @since 3.0.0
 */
public final class LayerClassifier {

    // Package name segments that indicate presentation layer
    private static final Set<String> PRESENTATION_PACKAGE_SEGMENTS =
            Set.of("presentation", "ui", "web", "rest", "api", "controller", "graphql", "grpc");

    // Package name segments that indicate application layer
    private static final Set<String> APPLICATION_PACKAGE_SEGMENTS =
            Set.of("application", "usecase", "service", "command", "query", "handler");

    // Package name segments that indicate domain layer
    private static final Set<String> DOMAIN_PACKAGE_SEGMENTS =
            Set.of("domain", "model", "entity", "valueobject", "aggregate");

    // Package name segments that indicate infrastructure layer
    private static final Set<String> INFRASTRUCTURE_PACKAGE_SEGMENTS =
            Set.of("infrastructure", "adapter", "persistence", "repository", "messaging", "external");

    // Type name suffixes that indicate presentation layer
    private static final Set<String> PRESENTATION_SUFFIXES =
            Set.of("Controller", "Resolver", "Endpoint", "Resource", "View", "ViewModel");

    // Type name suffixes that indicate application layer
    private static final Set<String> APPLICATION_SUFFIXES =
            Set.of("Service", "UseCase", "CommandHandler", "QueryHandler", "Handler", "Facade");

    // Type name suffixes that indicate domain layer
    private static final Set<String> DOMAIN_SUFFIXES =
            Set.of("Entity", "ValueObject", "AggregateRoot", "DomainService", "DomainEvent");

    // Type name suffixes that indicate infrastructure layer
    private static final Set<String> INFRASTRUCTURE_SUFFIXES =
            Set.of("Repository", "RepositoryImpl", "Adapter", "Gateway", "Client", "Publisher", "Consumer");

    // Annotations that indicate presentation layer
    private static final Set<String> PRESENTATION_ANNOTATIONS = Set.of(
            "org.springframework.stereotype.Controller",
            "org.springframework.web.bind.annotation.RestController",
            "org.springframework.web.bind.annotation.ControllerAdvice",
            "jakarta.ws.rs.Path",
            "javax.ws.rs.Path");

    // Annotations that indicate application layer
    private static final Set<String> APPLICATION_ANNOTATIONS =
            Set.of("org.springframework.stereotype.Service", "jakarta.ejb.Stateless", "javax.ejb.Stateless");

    // Annotations that indicate infrastructure layer
    private static final Set<String> INFRASTRUCTURE_ANNOTATIONS = Set.of(
            "org.springframework.stereotype.Repository",
            "org.springframework.data.repository.Repository",
            "jakarta.persistence.Entity",
            "javax.persistence.Entity");

    /**
     * Classifies a type into an architectural layer.
     *
     * @param qualifiedName the fully qualified type name (e.g., "com.example.OrderController")
     * @param simpleName    the simple type name (e.g., "OrderController")
     * @param annotations   list of fully qualified annotation names
     * @return the layer classification
     */
    public LayerClassification classify(String qualifiedName, String simpleName, List<String> annotations) {
        if (qualifiedName == null || simpleName == null) {
            return LayerClassification.UNKNOWN;
        }

        // First pass: Check annotations (most reliable)
        LayerClassification fromAnnotation = classifyByAnnotations(annotations != null ? annotations : List.of());
        if (fromAnnotation != LayerClassification.UNKNOWN) {
            return fromAnnotation;
        }

        // Second pass: Check package naming conventions
        LayerClassification fromPackage = classifyByPackageName(qualifiedName);
        if (fromPackage != LayerClassification.UNKNOWN) {
            return fromPackage;
        }

        // Third pass: Check type name suffixes
        LayerClassification fromSuffix = classifyByTypeName(simpleName);
        if (fromSuffix != LayerClassification.UNKNOWN) {
            return fromSuffix;
        }

        return LayerClassification.UNKNOWN;
    }

    /**
     * Classifies based on annotations present on the type.
     */
    private LayerClassification classifyByAnnotations(List<String> annotations) {
        for (String annotation : annotations) {
            if (PRESENTATION_ANNOTATIONS.contains(annotation)) {
                return LayerClassification.PRESENTATION;
            }
            if (APPLICATION_ANNOTATIONS.contains(annotation)) {
                return LayerClassification.APPLICATION;
            }
            if (INFRASTRUCTURE_ANNOTATIONS.contains(annotation)) {
                return LayerClassification.INFRASTRUCTURE;
            }
        }
        return LayerClassification.UNKNOWN;
    }

    /**
     * Classifies based on package naming conventions.
     */
    private LayerClassification classifyByPackageName(String qualifiedName) {
        String lowerQualifiedName = qualifiedName.toLowerCase();

        // Check for presentation layer indicators
        if (containsAnySegment(lowerQualifiedName, PRESENTATION_PACKAGE_SEGMENTS)) {
            return LayerClassification.PRESENTATION;
        }

        // Check for application layer indicators
        if (containsAnySegment(lowerQualifiedName, APPLICATION_PACKAGE_SEGMENTS)) {
            return LayerClassification.APPLICATION;
        }

        // Check for domain layer indicators
        if (containsAnySegment(lowerQualifiedName, DOMAIN_PACKAGE_SEGMENTS)) {
            return LayerClassification.DOMAIN;
        }

        // Check for infrastructure layer indicators
        if (containsAnySegment(lowerQualifiedName, INFRASTRUCTURE_PACKAGE_SEGMENTS)) {
            return LayerClassification.INFRASTRUCTURE;
        }

        return LayerClassification.UNKNOWN;
    }

    /**
     * Classifies based on type name suffixes.
     */
    private LayerClassification classifyByTypeName(String simpleName) {
        if (endsWithAny(simpleName, PRESENTATION_SUFFIXES)) {
            return LayerClassification.PRESENTATION;
        }
        if (endsWithAny(simpleName, APPLICATION_SUFFIXES)) {
            return LayerClassification.APPLICATION;
        }
        if (endsWithAny(simpleName, DOMAIN_SUFFIXES)) {
            return LayerClassification.DOMAIN;
        }
        if (endsWithAny(simpleName, INFRASTRUCTURE_SUFFIXES)) {
            return LayerClassification.INFRASTRUCTURE;
        }
        return LayerClassification.UNKNOWN;
    }

    /**
     * Checks if the qualified name contains any of the package segments.
     */
    private boolean containsAnySegment(String qualifiedName, Set<String> segments) {
        for (String segment : segments) {
            if (qualifiedName.contains("." + segment + ".") || qualifiedName.endsWith("." + segment)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the simple name ends with any of the suffixes.
     */
    private boolean endsWithAny(String simpleName, Set<String> suffixes) {
        for (String suffix : suffixes) {
            if (simpleName.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }
}
