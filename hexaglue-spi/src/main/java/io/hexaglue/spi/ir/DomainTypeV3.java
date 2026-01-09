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

package io.hexaglue.spi.ir;

import io.hexaglue.spi.classification.CertaintyLevel;
import io.hexaglue.spi.classification.ClassificationEvidence;
import io.hexaglue.spi.classification.ClassificationStrategy;
import io.hexaglue.spi.core.SourceLocation;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A domain type extracted from the application source code (Version 3.0 redesign).
 *
 * <p>This record represents a complete, analyzed domain type with full classification
 * metadata, evidence tracking, and structural information. It is the primary data
 * structure used by plugins for code generation and analysis.
 *
 * <p><b>Breaking changes from 2.x:</b>
 * <ul>
 *   <li>Replaced {@code confidence: ConfidenceLevel} with {@code certainty: CertaintyLevel}</li>
 *   <li>Added {@code strategy: ClassificationStrategy} to track how classification was determined</li>
 *   <li>Added {@code reasoning: String} for human-readable classification explanation</li>
 *   <li>Added {@code evidences: List<ClassificationEvidence>} for detailed signal tracking</li>
 *   <li>Replaced {@code sourceRef: SourceRef} with {@code sourceLocation: SourceLocation}</li>
 *   <li>Added {@code fields: List<FieldInfo>} for field metadata</li>
 *   <li>Added {@code methods: List<MethodInfo>} for method metadata</li>
 *   <li>Changed {@code annotations: List<String>} to {@code annotations: Set<String>}</li>
 *   <li>Removed {@code construct: JavaConstruct} (use FieldInfo/MethodInfo instead)</li>
 *   <li>Removed {@code identity: Optional<Identity>} (use FieldInfo with analysis)</li>
 *   <li>Removed {@code properties: List<DomainProperty>} (superseded by fields)</li>
 *   <li>Removed {@code relations: List<DomainRelation>} (use graph analysis)</li>
 * </ul>
 *
 * @param qualifiedName  the fully qualified class name (e.g., "com.example.Order")
 * @param simpleName     the simple class name (e.g., "Order")
 * @param packageName    the package name (e.g., "com.example")
 * @param kind           the DDD tactical pattern (AGGREGATE_ROOT, ENTITY, VALUE_OBJECT, etc.)
 * @param certainty      how certain the classification is (EXPLICIT, CERTAIN_BY_STRUCTURE, etc.)
 * @param strategy       the strategy used for classification (ANNOTATION, REPOSITORY, etc.)
 * @param reasoning      human-readable explanation of why this classification was chosen
 * @param evidences      the classification signals that led to this decision
 * @param sourceLocation the source code location of this type
 * @param fields         the fields declared in this type
 * @param methods        the methods declared in this type
 * @param annotations    the fully qualified annotation names on this type
 * @since 3.0.0
 */
public record DomainTypeV3(
        String qualifiedName,
        String simpleName,
        String packageName,
        DomainKind kind,
        CertaintyLevel certainty,
        ClassificationStrategy strategy,
        String reasoning,
        List<ClassificationEvidence> evidences,
        SourceLocation sourceLocation,
        List<FieldInfo> fields,
        List<MethodInfo> methods,
        Set<String> annotations) {

    /**
     * Compact constructor with validation and defensive copies.
     *
     * @throws NullPointerException if any required parameter is null
     */
    public DomainTypeV3 {
        Objects.requireNonNull(qualifiedName, "qualifiedName required");
        Objects.requireNonNull(simpleName, "simpleName required");
        Objects.requireNonNull(packageName, "packageName required");
        Objects.requireNonNull(kind, "kind required");
        Objects.requireNonNull(certainty, "certainty required");
        Objects.requireNonNull(strategy, "strategy required");

        // Reasoning can be null or empty for simple cases
        reasoning = reasoning != null ? reasoning : "";

        // Make defensive immutable copies
        evidences = evidences != null ? List.copyOf(evidences) : List.of();
        fields = fields != null ? List.copyOf(fields) : List.of();
        methods = methods != null ? List.copyOf(methods) : List.of();
        annotations = annotations != null ? Set.copyOf(annotations) : Set.of();
    }

    /**
     * Returns true if this classification is based on explicit user annotation.
     *
     * @return true if certainty is EXPLICIT
     */
    public boolean isExplicit() {
        return certainty == CertaintyLevel.EXPLICIT;
    }

    /**
     * Returns true if this classification needs manual review.
     *
     * <p>Classifications with UNCERTAIN or NONE certainty should be reviewed
     * by developers before relying on them for code generation.
     *
     * @return true if certainty is UNCERTAIN or NONE
     */
    public boolean needsReview() {
        return certainty == CertaintyLevel.UNCERTAIN || certainty == CertaintyLevel.NONE;
    }

    /**
     * Returns true if this type is an aggregate root.
     *
     * @return true if kind is AGGREGATE_ROOT
     */
    public boolean isAggregateRoot() {
        return kind == DomainKind.AGGREGATE_ROOT;
    }

    /**
     * Returns true if this type is an entity (including aggregate roots).
     *
     * @return true if kind is ENTITY or AGGREGATE_ROOT
     */
    public boolean isEntity() {
        return kind == DomainKind.ENTITY || kind == DomainKind.AGGREGATE_ROOT;
    }

    /**
     * Returns true if this type is a value object.
     *
     * @return true if kind is VALUE_OBJECT
     */
    public boolean isValueObject() {
        return kind == DomainKind.VALUE_OBJECT;
    }

    /**
     * Returns true if this type is a domain service.
     *
     * @return true if kind is DOMAIN_SERVICE
     */
    public boolean isDomainService() {
        return kind == DomainKind.DOMAIN_SERVICE;
    }

    /**
     * Returns true if this type is an application service.
     *
     * @return true if kind is APPLICATION_SERVICE
     */
    public boolean isApplicationService() {
        return kind == DomainKind.APPLICATION_SERVICE;
    }

    /**
     * Returns true if this type has any classification evidence.
     *
     * @return true if evidences is not empty
     */
    public boolean hasEvidences() {
        return !evidences.isEmpty();
    }

    /**
     * Returns true if this type is annotated with the given annotation.
     *
     * @param annotationQualifiedName the fully qualified annotation name
     * @return true if annotation is present
     */
    public boolean hasAnnotation(String annotationQualifiedName) {
        return annotations.contains(annotationQualifiedName);
    }

    /**
     * Returns true if this type has any annotations.
     *
     * @return true if annotations is not empty
     */
    public boolean isAnnotated() {
        return !annotations.isEmpty();
    }

    /**
     * Returns fields matching a given predicate (by name, type, or annotation).
     *
     * @param predicate the predicate to apply
     * @return filtered list of fields
     */
    public List<FieldInfo> fieldsMatching(java.util.function.Predicate<FieldInfo> predicate) {
        return fields.stream().filter(predicate).toList();
    }

    /**
     * Returns methods matching a given predicate.
     *
     * @param predicate the predicate to apply
     * @return filtered list of methods
     */
    public List<MethodInfo> methodsMatching(java.util.function.Predicate<MethodInfo> predicate) {
        return methods.stream().filter(predicate).toList();
    }

    /**
     * Returns positive classification evidences (supporting the classification).
     *
     * @return list of evidences with positive weight
     */
    public List<ClassificationEvidence> positiveEvidences() {
        return evidences.stream().filter(ClassificationEvidence::isPositive).toList();
    }

    /**
     * Returns negative classification evidences (contradicting the classification).
     *
     * @return list of evidences with negative weight
     */
    public List<ClassificationEvidence> negativeEvidences() {
        return evidences.stream().filter(ClassificationEvidence::isNegative).toList();
    }

    /**
     * Returns the total weight of all evidences.
     *
     * @return sum of all evidence weights
     */
    public int totalEvidenceWeight() {
        return evidences.stream().mapToInt(ClassificationEvidence::weight).sum();
    }
}
