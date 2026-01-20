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

package io.hexaglue.core.builder;

import io.hexaglue.arch.ClassificationTrace;
import io.hexaglue.arch.model.AggregateRoot;
import io.hexaglue.arch.model.Field;
import io.hexaglue.arch.model.FieldRole;
import io.hexaglue.arch.model.Invariant;
import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.TypeStructure;
import io.hexaglue.core.classification.ClassificationResult;
import io.hexaglue.core.graph.model.FieldNode;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Builds {@link AggregateRoot} instances from classification results.
 *
 * <p>This is the most complex builder as it must detect and analyze:</p>
 * <ul>
 *   <li>Identity field (REQUIRED - throws if not found)</li>
 *   <li>Effective identity type (may differ for wrapped IDs)</li>
 *   <li>Internal entities that are part of the aggregate boundary</li>
 *   <li>Embedded value objects</li>
 *   <li>Domain events that this aggregate can emit</li>
 *   <li>Associated repository/driven port</li>
 *   <li>Business invariants from method names</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * AggregateRootBuilder builder = new AggregateRootBuilder(
 *     structureBuilder, traceConverter, fieldRoleDetector);
 * AggregateRoot aggregate = builder.build(typeNode, classification, context);
 * }</pre>
 *
 * @since 4.1.0
 */
public final class AggregateRootBuilder {

    private static final Pattern INVARIANT_METHOD_PATTERN =
            Pattern.compile("^(validate|check|ensure|verify).+", Pattern.CASE_INSENSITIVE);

    private final TypeStructureBuilder structureBuilder;
    private final ClassificationTraceConverter traceConverter;
    private final FieldRoleDetector fieldRoleDetector;

    /**
     * Creates a new AggregateRootBuilder.
     *
     * @param structureBuilder the builder for type structures
     * @param traceConverter the converter for classification traces
     * @param fieldRoleDetector the detector for field roles
     * @throws NullPointerException if any argument is null
     */
    public AggregateRootBuilder(
            TypeStructureBuilder structureBuilder,
            ClassificationTraceConverter traceConverter,
            FieldRoleDetector fieldRoleDetector) {
        this.structureBuilder = Objects.requireNonNull(structureBuilder, "structureBuilder must not be null");
        this.traceConverter = Objects.requireNonNull(traceConverter, "traceConverter must not be null");
        this.fieldRoleDetector = Objects.requireNonNull(fieldRoleDetector, "fieldRoleDetector must not be null");
    }

    /**
     * Builds an AggregateRoot from a classified type node.
     *
     * @param typeNode the type node to build from
     * @param classification the classification result
     * @param context the builder context
     * @return the built AggregateRoot
     * @throws NullPointerException if any argument is null
     * @throws IllegalStateException if no identity field is found
     */
    public AggregateRoot build(TypeNode typeNode, ClassificationResult classification, BuilderContext context) {
        Objects.requireNonNull(typeNode, "typeNode must not be null");
        Objects.requireNonNull(classification, "classification must not be null");
        Objects.requireNonNull(context, "context must not be null");

        TypeId id = TypeId.of(typeNode.qualifiedName());
        TypeStructure structure = structureBuilder.build(typeNode, context);
        ClassificationTrace trace = traceConverter.convert(classification);

        // REQUIRED: Find identity field - throws if not found
        Field identityField = findIdentityField(typeNode, structure, context)
                .orElseThrow(() -> new IllegalStateException(
                        "AggregateRoot '" + typeNode.qualifiedName() + "' must have an identity field"));

        // Compute effective identity type
        TypeRef effectiveIdType = computeEffectiveIdentityType(identityField);

        // Collect boundary components
        List<TypeRef> entities = collectBoundaryEntities(typeNode, context);
        List<TypeRef> valueObjects = collectEmbeddedValueObjects(structure);
        List<TypeRef> domainEvents = collectDomainEvents(typeNode, context);
        Optional<TypeRef> drivenPort = findAssociatedRepository(typeNode, context);
        List<Invariant> invariants = detectInvariants(typeNode, context);

        return AggregateRoot.builder(id, structure, trace, identityField)
                .effectiveIdentityType(effectiveIdType)
                .entities(entities)
                .valueObjects(valueObjects)
                .domainEvents(domainEvents)
                .drivenPort(drivenPort.orElse(null))
                .invariants(invariants)
                .build();
    }

    /**
     * Finds the identity field in the type structure.
     */
    private Optional<Field> findIdentityField(TypeNode typeNode, TypeStructure structure, BuilderContext context) {
        // First try to find from the built structure
        Optional<Field> fromStructure = structure.fields().stream()
                .filter(f -> f.hasRole(FieldRole.IDENTITY))
                .findFirst();

        if (fromStructure.isPresent()) {
            return fromStructure;
        }

        // Fallback: check field nodes directly
        List<FieldNode> fieldNodes = context.graphQuery().fieldsOf(typeNode);
        for (FieldNode fieldNode : fieldNodes) {
            Set<FieldRole> roles = fieldRoleDetector.detect(fieldNode, context);
            if (roles.contains(FieldRole.IDENTITY)) {
                return structure.fields().stream()
                        .filter(f -> f.name().equals(fieldNode.simpleName()))
                        .findFirst();
            }
        }

        return Optional.empty();
    }

    /**
     * Computes the effective identity type.
     *
     * <p>For wrapped identity types (e.g., OrderId wrapping UUID), this returns the wrapped type.
     * Otherwise, it returns the field's type.</p>
     */
    private TypeRef computeEffectiveIdentityType(Field identityField) {
        // Check if there's a wrapped type (for Identifier types)
        return identityField
                .wrappedType()
                .map(wrapped -> TypeRef.of(wrapped.qualifiedName()))
                .orElse(TypeRef.of(identityField.type().qualifiedName()));
    }

    /**
     * Collects boundary entities that are part of this aggregate.
     */
    private List<TypeRef> collectBoundaryEntities(TypeNode typeNode, BuilderContext context) {
        return context.graphQuery().fieldsOf(typeNode).stream()
                .filter(f -> !f.isCollectionType())
                .filter(f -> isClassifiedAs(f.type().rawQualifiedName(), "ENTITY", context))
                .map(f -> TypeRef.of(f.type().rawQualifiedName()))
                .distinct()
                .toList();
    }

    /**
     * Collects embedded value objects from the structure.
     */
    private List<TypeRef> collectEmbeddedValueObjects(TypeStructure structure) {
        return structure.fields().stream()
                .filter(f -> f.hasRole(FieldRole.EMBEDDED))
                .map(Field::type)
                .distinct()
                .toList();
    }

    /**
     * Collects domain events that this aggregate can emit.
     */
    private List<TypeRef> collectDomainEvents(TypeNode typeNode, BuilderContext context) {
        return context.graphQuery().methodsOf(typeNode).stream()
                .filter(m -> m.returnType() != null)
                .map(m -> m.returnType().rawQualifiedName())
                .filter(typeName -> isClassifiedAs(typeName, "DOMAIN_EVENT", context))
                .distinct()
                .map(TypeRef::of)
                .toList();
    }

    /**
     * Finds the associated repository for this aggregate.
     */
    private Optional<TypeRef> findAssociatedRepository(TypeNode typeNode, BuilderContext context) {
        String aggregateName = typeNode.qualifiedName();

        return context.classificationResults().stream()
                .filter(c -> "REPOSITORY".equals(c.kind()))
                .filter(c -> referencesAggregate(c, aggregateName, context))
                .findFirst()
                .map(c -> extractQualifiedName(c))
                .map(TypeRef::of);
    }

    /**
     * Detects business invariants from method names.
     */
    private List<Invariant> detectInvariants(TypeNode typeNode, BuilderContext context) {
        return context.graphQuery().methodsOf(typeNode).stream()
                .filter(m -> INVARIANT_METHOD_PATTERN.matcher(m.simpleName()).matches())
                .map(m -> Invariant.of(m.simpleName(), "Invariant from method: " + m.simpleName()))
                .toList();
    }

    /**
     * Checks if a type is classified with the given kind.
     */
    private boolean isClassifiedAs(String qualifiedName, String kind, BuilderContext context) {
        return context.classificationResults().stream()
                .filter(result -> matchesQualifiedName(result, qualifiedName))
                .anyMatch(result -> kind.equals(result.kind()));
    }

    /**
     * Checks if a classification result matches the given qualified name.
     */
    private boolean matchesQualifiedName(ClassificationResult result, String qualifiedName) {
        String nodeIdStr = result.subjectId().toString();
        return nodeIdStr.endsWith(qualifiedName) || nodeIdStr.contains(":" + qualifiedName);
    }

    /**
     * Extracts the qualified name from a classification result.
     */
    private String extractQualifiedName(ClassificationResult result) {
        String nodeIdStr = result.subjectId().toString();
        int colonIndex = nodeIdStr.indexOf(':');
        return colonIndex >= 0 ? nodeIdStr.substring(colonIndex + 1) : nodeIdStr;
    }

    /**
     * Checks if a repository references this aggregate.
     */
    private boolean referencesAggregate(
            ClassificationResult repoClassification, String aggregateName, BuilderContext context) {
        // Simple heuristic: repository name contains aggregate name
        String repoName = extractQualifiedName(repoClassification);
        String simpleAggregateName = aggregateName.substring(aggregateName.lastIndexOf('.') + 1);
        return repoName.contains(simpleAggregateName);
    }
}
