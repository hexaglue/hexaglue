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

import io.hexaglue.arch.model.audit.AggregateInfo;
import io.hexaglue.arch.model.audit.BoundedContextInfo;
import io.hexaglue.arch.model.audit.CouplingMetrics;
import io.hexaglue.arch.model.audit.DependencyCycle;
import io.hexaglue.arch.model.audit.LakosMetrics;
import io.hexaglue.arch.model.audit.LayerViolation;
import io.hexaglue.arch.model.audit.StabilityViolation;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Query interface for architecture analysis and auditing.
 *
 * <p>This interface provides methods for analyzing the application architecture,
 * detecting violations, and computing metrics. It is designed for use by
 * enrichment plugins and audit tools.
 *
 * <p>Key capabilities:
 * <ul>
 *   <li><b>Cycle detection</b>: Find dependency cycles at various levels</li>
 *   <li><b>Metrics</b>: Calculate Lakos metrics, coupling, cohesion</li>
 *   <li><b>Aggregate analysis</b>: Identify aggregates and their boundaries</li>
 *   <li><b>Violation detection</b>: Find layer and stability violations</li>
 * </ul>
 *
 * @since 3.0.0
 */
public interface ArchitectureQuery {

    // === Cycle detection ===

    /**
     * Finds all type-level dependency cycles.
     *
     * <p>A type-level cycle exists when a type transitively depends on itself
     * through other types (e.g., A → B → C → A).
     *
     * @return list of detected cycles
     */
    List<DependencyCycle> findDependencyCycles();

    /**
     * Finds all package-level dependency cycles.
     *
     * <p>A package-level cycle exists when packages form circular dependencies.
     * This is generally more serious than type-level cycles as it indicates
     * poor module boundaries.
     *
     * @return list of package cycles
     */
    List<DependencyCycle> findPackageCycles();

    /**
     * Finds bounded context level cycles (in modular architectures).
     *
     * <p>This method analyzes cycles between major architectural modules or
     * bounded contexts. Only applicable if the codebase follows a modular structure.
     *
     * @return list of bounded context cycles
     */
    List<DependencyCycle> findBoundedContextCycles();

    /**
     * Finds all bounded contexts in the analyzed codebase.
     *
     * <p>A bounded context is identified by analyzing the package structure.
     * The detection algorithm extracts the context name from the third segment
     * of package names (e.g., "order" from "com.example.order.domain").
     *
     * <p><strong>Detection rules:</strong>
     * <ul>
     *   <li>Packages with fewer than 3 segments are not assigned to a bounded context</li>
     *   <li>Each unique third segment defines a separate bounded context</li>
     *   <li>All types within a context's package hierarchy belong to that context</li>
     * </ul>
     *
     * <p><strong>Example:</strong>
     * <pre>{@code
     * // Given packages:
     * // - com.example.order.domain
     * // - com.example.order.application
     * // - com.example.inventory.domain
     *
     * // Returns:
     * // - BoundedContextInfo("order", "com.example.order", [...])
     * // - BoundedContextInfo("inventory", "com.example.inventory", [...])
     * }</pre>
     *
     * @return list of detected bounded contexts, or empty list if none found
     * @since 3.0.0
     */
    List<BoundedContextInfo> findBoundedContexts();

    // === Lakos metrics ===

    /**
     * Calculates the "Depends On" score for a type.
     *
     * <p>The Depends On score is the transitive closure of dependencies.
     * Higher scores indicate types that depend on many others, making them
     * harder to reuse and test.
     *
     * @param qualifiedName the type's fully-qualified name
     * @return the number of types this type transitively depends on
     */
    int calculateDependsOnScore(String qualifiedName);

    /**
     * Calculates Cumulative Component Dependency (CCD) for a package.
     *
     * <p>CCD is the sum of all Depends On scores for types in the package.
     * It measures the total coupling of the package.
     *
     * @param packageName the package name
     * @return cumulative component dependency
     */
    int calculateCCD(String packageName);

    /**
     * Calculates Normalized Cumulative Component Dependency (NCCD).
     *
     * <p>NCCD = CCD / n² where n is the number of types. This normalizes
     * the metric for comparison across packages of different sizes.
     *
     * @param packageName the package name
     * @return normalized CCD (0.0 to 1.0)
     */
    double calculateNCCD(String packageName);

    /**
     * Calculates complete Lakos metrics for a specific package.
     *
     * <p>Lakos metrics measure architectural quality based on cumulative
     * component dependencies:
     * <ul>
     *   <li>CCD: Sum of transitive dependencies</li>
     *   <li>ACD: Average component dependency</li>
     *   <li>NCCD: Normalized compared to balanced tree</li>
     *   <li>RACD: Relative to theoretical minimum</li>
     * </ul>
     *
     * @param packageName the package to analyze
     * @return Lakos metrics for the package
     * @since 3.0.0
     */
    LakosMetrics calculateLakosMetrics(String packageName);

    /**
     * Calculates Lakos metrics for a set of types.
     *
     * <p>This method allows calculating metrics for an arbitrary set of types,
     * not necessarily in the same package. Useful for analyzing aggregates
     * or bounded contexts.
     *
     * @param qualifiedNames the fully-qualified names of types to include
     * @return Lakos metrics for the specified types
     * @since 3.0.0
     */
    LakosMetrics calculateLakosMetrics(Set<String> qualifiedNames);

    /**
     * Calculates Lakos metrics for the entire codebase.
     *
     * <p>This provides a global view of the system's architectural quality
     * and coupling characteristics.
     *
     * @return global Lakos metrics
     * @since 3.0.0
     */
    LakosMetrics calculateGlobalLakosMetrics();

    // === Aggregate analysis ===

    /**
     * Finds all aggregates in the domain model.
     *
     * <p>Aggregates are identified by aggregate roots and their associated
     * entities and value objects.
     *
     * @return list of detected aggregates
     */
    List<AggregateInfo> findAggregates();

    /**
     * Finds entities contained within a specific aggregate.
     *
     * @param aggregateRootType the aggregate root's fully-qualified name
     * @return list of entity types in the aggregate
     */
    List<String> findEntitiesInAggregate(String aggregateRootType);

    /**
     * Finds the aggregate containing the given entity.
     *
     * @param entityType the entity's fully-qualified name
     * @return the containing aggregate, or empty if not part of an aggregate
     */
    Optional<AggregateInfo> findContainingAggregate(String entityType);

    // === Dependency analysis ===

    /**
     * Finds layer dependency violations.
     *
     * <p>Layer violations occur when dependencies flow in the wrong direction
     * (e.g., domain depending on infrastructure).
     *
     * @return list of layer violations
     */
    List<LayerViolation> findLayerViolations();

    /**
     * Finds stability principle violations.
     *
     * <p>Stability violations occur when unstable components depend on
     * more stable components (violating the Stable Dependencies Principle).
     *
     * @return list of stability violations
     */
    List<StabilityViolation> findStabilityViolations();

    /**
     * Analyzes coupling metrics for a package.
     *
     * <p>Returns detailed coupling metrics including afferent coupling,
     * efferent coupling, instability, abstractness, and distance from
     * the main sequence.
     *
     * @param packageName the package name
     * @return coupling metrics, or empty if package not found
     */
    Optional<CouplingMetrics> analyzePackageCoupling(String packageName);

    /**
     * Analyzes coupling metrics for all packages.
     *
     * @return list of coupling metrics for all packages
     */
    List<CouplingMetrics> analyzeAllPackageCoupling();

    // === Port analysis ===

    /**
     * Finds the direction (DRIVING or DRIVEN) of a port.
     *
     * <p>This method allows plugins to retrieve the port direction as determined
     * by the core's classification analysis, rather than inferring it from naming
     * conventions.
     *
     * <p><strong>Usage example:</strong>
     * <pre>{@code
     * Optional<PortDirection> direction = query.findPortDirection("com.example.OrderRepository");
     * if (direction.isPresent() && direction.get() == PortDirection.DRIVEN) {
     *     // Validate driven port usage
     * }
     * }</pre>
     *
     * @param portQualifiedName the fully-qualified name of the port interface
     * @return the port direction, or empty if the type is not a recognized port
     * @since 3.0.0
     */
    Optional<io.hexaglue.arch.model.ir.PortDirection> findPortDirection(String portQualifiedName);

    // === Aggregate membership ===

    /**
     * Returns the complete aggregate membership map.
     *
     * <p>This method provides the mapping of aggregate roots to their member entities,
     * as determined by the core's classification and relationship analysis. This is
     * more accurate than package-based inference as it uses actual type relationships.
     *
     * <p>An entity belongs to an aggregate if:
     * <ul>
     *   <li>It is directly referenced by the aggregate root (field or method signature)</li>
     *   <li>It is classified as an ENTITY and has a structural relationship to the root</li>
     * </ul>
     *
     * <p><strong>Usage example:</strong>
     * <pre>{@code
     * Map<String, List<String>> membership = query.findAggregateMembership();
     * List<String> orderEntities = membership.get("com.example.domain.Order");
     * // Returns ["com.example.domain.OrderLine", "com.example.domain.OrderItem"]
     * }</pre>
     *
     * @return map of aggregate root qualified names to lists of member entity qualified names;
     *         empty map if no aggregates are found
     * @since 3.0.0
     */
    Map<String, List<String>> findAggregateMembership();

    /**
     * Calculates the cohesion score for an aggregate.
     *
     * <p>Cohesion measures how strongly the members of an aggregate are related to each other.
     * A high cohesion score (close to 1.0) indicates that aggregate members have strong
     * internal relationships, which is desirable in DDD.
     *
     * <p>The calculation is based on:
     * <ul>
     *   <li>Internal dependencies between aggregate members (root, entities, VOs)</li>
     *   <li>Ratio of actual connections to maximum possible connections</li>
     * </ul>
     *
     * <p><strong>Interpretation:</strong>
     * <ul>
     *   <li>0.8 - 1.0: Excellent cohesion</li>
     *   <li>0.6 - 0.8: Good cohesion</li>
     *   <li>0.4 - 0.6: Moderate cohesion (review recommended)</li>
     *   <li>Below 0.4: Low cohesion (aggregate may need restructuring)</li>
     * </ul>
     *
     * @param aggregateRootType the fully-qualified name of the aggregate root
     * @return the cohesion score (0.0 to 1.0), or empty if aggregate not found
     * @since 3.0.0
     */
    Optional<Double> calculateAggregateCohesion(String aggregateRootType);

    /**
     * Finds the repository port associated with an aggregate.
     *
     * <p>In hexagonal architecture, each aggregate typically has a repository port
     * that provides persistence operations. This method finds the repository
     * that manages the specified aggregate.
     *
     * @param aggregateRootType the fully-qualified name of the aggregate root
     * @return the repository port qualified name, or empty if no repository found
     * @since 3.0.0
     */
    Optional<String> findRepositoryForAggregate(String aggregateRootType);

    // === Port implementation analysis ===

    /**
     * Finds types that implement a given interface in the full application graph.
     *
     * <p>Unlike {@code CompositionIndex} or {@code Codebase.dependencies()} which
     * only contain classified types, this method searches the complete application
     * graph including types in excluded packages (infrastructure, application).
     *
     * <p>This is essential for detecting adapter implementations that are in
     * packages excluded from classification but are still valid implementations
     * of port interfaces.
     *
     * @param interfaceQualifiedName the fully-qualified name of the interface
     * @return list of implementor qualified names, or empty list if none found
     * @since 5.0.0
     */
    List<String> findImplementors(String interfaceQualifiedName);
}
