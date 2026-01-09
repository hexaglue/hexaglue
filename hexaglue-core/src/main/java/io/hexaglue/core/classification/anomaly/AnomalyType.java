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

package io.hexaglue.core.classification.anomaly;

/**
 * Types of architectural anomalies detected in domain models.
 *
 * <p>Anomalies represent violations of DDD best practices or potential design
 * issues that should be reviewed by developers. Each anomaly type has an
 * associated severity and remediation guidance.
 *
 * @since 3.0.0
 */
public enum AnomalyType {

    /**
     * Direct reference from one aggregate root to another.
     *
     * <p>In DDD, aggregate roots should reference each other via identifiers only,
     * not direct object references. Direct references violate aggregate isolation
     * and can cause issues with:
     * <ul>
     *   <li>Transaction boundaries</li>
     *   <li>Distributed systems (aggregates in different services)</li>
     *   <li>Lazy loading and performance</li>
     * </ul>
     *
     * <p><b>Severity:</b> WARNING
     * <p><b>Remediation:</b> Replace direct reference with ID wrapper reference
     */
    DIRECT_AGGREGATE_REFERENCE,

    /**
     * Cycle detected in the composition graph.
     *
     * <p>Circular dependencies between domain types indicate a modeling issue.
     * Cycles can cause:
     * <ul>
     *   <li>Infinite recursion during serialization</li>
     *   <li>Complex initialization logic</li>
     *   <li>Difficult-to-understand code</li>
     * </ul>
     *
     * <p><b>Severity:</b> ERROR
     * <p><b>Remediation:</b> Break cycle by introducing indirection or refactoring model
     */
    COMPOSITION_CYCLE,

    /**
     * Entity is composed by multiple aggregates.
     *
     * <p>An entity should belong to exactly one aggregate. Being shared across
     * aggregates violates the single ownership principle and can cause:
     * <ul>
     *   <li>Concurrent modification conflicts</li>
     *   <li>Unclear lifecycle management</li>
     *   <li>Transaction complexity</li>
     * </ul>
     *
     * <p><b>Severity:</b> ERROR
     * <p><b>Remediation:</b> Move entity to one aggregate, or make it an aggregate root itself
     */
    SHARED_ENTITY,

    /**
     * Aggregate root inferred but no repository found.
     *
     * <p>Aggregate roots should have a corresponding repository for persistence.
     * Missing repository might indicate:
     * <ul>
     *   <li>Incomplete implementation</li>
     *   <li>Misclassification as aggregate root</li>
     *   <li>Read-only aggregate (query model)</li>
     * </ul>
     *
     * <p><b>Severity:</b> WARNING
     * <p><b>Remediation:</b> Create repository or reconsider aggregate root classification
     */
    AGGREGATE_WITHOUT_REPOSITORY,

    /**
     * Value object has an identity field.
     *
     * <p>Value objects are defined by their attributes, not by identity.
     * Having an ID field suggests:
     * <ul>
     *   <li>Should be classified as entity instead</li>
     *   <li>ID field is not actually used for identity</li>
     *   <li>Misunderstanding of value object concept</li>
     * </ul>
     *
     * <p><b>Severity:</b> WARNING
     * <p><b>Remediation:</b> Remove ID field or reclassify as entity
     */
    VALUE_OBJECT_WITH_IDENTITY;

    /**
     * Returns true if this anomaly type indicates a design smell.
     *
     * @return true for DIRECT_AGGREGATE_REFERENCE
     */
    public boolean isSmell() {
        return this == DIRECT_AGGREGATE_REFERENCE;
    }

    /**
     * Returns true if this anomaly type indicates a structural issue.
     *
     * @return true for COMPOSITION_CYCLE or SHARED_ENTITY
     */
    public boolean isStructuralIssue() {
        return this == COMPOSITION_CYCLE || this == SHARED_ENTITY;
    }

    /**
     * Returns true if this anomaly type indicates incomplete implementation.
     *
     * @return true for AGGREGATE_WITHOUT_REPOSITORY
     */
    public boolean isIncomplete() {
        return this == AGGREGATE_WITHOUT_REPOSITORY;
    }
}
