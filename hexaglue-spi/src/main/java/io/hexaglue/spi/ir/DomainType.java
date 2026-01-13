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

import java.util.List;
import java.util.Optional;

/**
 * A domain type extracted from the application source code.
 *
 * @param qualifiedName the fully qualified class name
 * @param simpleName the simple class name
 * @param packageName the package name
 * @param kind the DDD classification
 * @param confidence how confident the classification is
 * @param construct the Java construct (CLASS, RECORD, ENUM)
 * @param identity the identity information, if applicable
 * @param properties the domain properties
 * @param relations the relationships to other domain types
 * @param annotations the annotation qualified names present on this type
 * @param sourceRef source location for diagnostics
 * @param unclassifiedReason reason for UNCLASSIFIED status, present only when kind is UNCLASSIFIED
 */
public record DomainType(
        String qualifiedName,
        String simpleName,
        String packageName,
        DomainKind kind,
        ConfidenceLevel confidence,
        JavaConstruct construct,
        Optional<Identity> identity,
        List<DomainProperty> properties,
        List<DomainRelation> relations,
        List<String> annotations,
        SourceRef sourceRef,
        Optional<UnclassifiedReason> unclassifiedReason) {

    /**
     * Backward-compatible constructor without relations and unclassifiedReason.
     *
     * @since 2.0.0
     */
    public DomainType(
            String qualifiedName,
            String simpleName,
            String packageName,
            DomainKind kind,
            ConfidenceLevel confidence,
            JavaConstruct construct,
            Optional<Identity> identity,
            List<DomainProperty> properties,
            List<String> annotations,
            SourceRef sourceRef) {
        this(
                qualifiedName,
                simpleName,
                packageName,
                kind,
                confidence,
                construct,
                identity,
                properties,
                List.of(),
                annotations,
                sourceRef,
                Optional.empty());
    }

    /**
     * Backward-compatible constructor without unclassifiedReason.
     *
     * @since 3.0.0
     */
    public DomainType(
            String qualifiedName,
            String simpleName,
            String packageName,
            DomainKind kind,
            ConfidenceLevel confidence,
            JavaConstruct construct,
            Optional<Identity> identity,
            List<DomainProperty> properties,
            List<DomainRelation> relations,
            List<String> annotations,
            SourceRef sourceRef) {
        this(
                qualifiedName,
                simpleName,
                packageName,
                kind,
                confidence,
                construct,
                identity,
                properties,
                relations,
                annotations,
                sourceRef,
                Optional.empty());
    }

    /**
     * Returns true if this type has an identity field.
     */
    public boolean hasIdentity() {
        return identity.isPresent();
    }

    /**
     * Returns true if this type is an aggregate root.
     */
    public boolean isAggregateRoot() {
        return kind == DomainKind.AGGREGATE_ROOT;
    }

    /**
     * Returns true if this type is an entity (including aggregate roots).
     */
    public boolean isEntity() {
        return kind == DomainKind.ENTITY || kind == DomainKind.AGGREGATE_ROOT;
    }

    /**
     * Returns true if this type is a value object.
     */
    public boolean isValueObject() {
        return kind == DomainKind.VALUE_OBJECT;
    }

    /**
     * Returns true if this type could not be classified.
     *
     * @since 3.0.0
     */
    public boolean isUnclassified() {
        return kind == DomainKind.UNCLASSIFIED;
    }

    /**
     * Returns true if this type is a record.
     */
    public boolean isRecord() {
        return construct == JavaConstruct.RECORD;
    }

    /**
     * Returns true if this type has any relationships.
     *
     * @since 2.0.0
     */
    public boolean hasRelations() {
        return !relations.isEmpty();
    }

    /**
     * Returns relations of a specific kind.
     *
     * @param kind the relation kind to filter by
     * @return list of relations matching the specified kind
     * @since 2.0.0
     */
    public List<DomainRelation> relationsOfKind(RelationKind kind) {
        return relations.stream().filter(r -> r.kind() == kind).toList();
    }

    /**
     * Returns the embedded relations (value objects).
     *
     * @return list of embedded and element collection relations
     * @since 2.0.0
     */
    public List<DomainRelation> embeddedRelations() {
        return relations.stream()
                .filter(r -> r.kind() == RelationKind.EMBEDDED || r.kind() == RelationKind.ELEMENT_COLLECTION)
                .toList();
    }

    /**
     * Returns the entity relations (one-to-many, many-to-one, etc.).
     *
     * @return list of relations targeting entities
     * @since 2.0.0
     */
    public List<DomainRelation> entityRelations() {
        return relations.stream().filter(DomainRelation::targetsEntity).toList();
    }
}
