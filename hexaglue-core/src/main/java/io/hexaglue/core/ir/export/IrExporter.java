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

package io.hexaglue.core.ir.export;

import io.hexaglue.core.analysis.RelationAnalyzer;
import io.hexaglue.core.classification.ClassificationContext;
import io.hexaglue.core.classification.ClassificationResult;
import io.hexaglue.core.classification.ClassificationStatus;
import io.hexaglue.core.classification.ClassificationTarget;
import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.frontend.JavaForm;
import io.hexaglue.core.frontend.SourceRef;
import io.hexaglue.core.frontend.TypeRef;
import io.hexaglue.core.graph.ApplicationGraph;
import io.hexaglue.core.graph.model.*;
import io.hexaglue.core.graph.query.GraphQuery;
import io.hexaglue.spi.ir.*;
import io.hexaglue.spi.ir.IdentityWrapperKind;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Exports a classified application graph to the IR (Intermediate Representation).
 *
 * <p>The IR is the stable API for plugins. This exporter converts the internal
 * graph representation and classification results into the public SPI types.
 */
public final class IrExporter {

    private static final String ENGINE_VERSION = "2.0.0-SNAPSHOT";

    private static final Set<String> IDENTITY_TYPES =
            Set.of("java.util.UUID", "java.lang.Long", "java.lang.String", "java.lang.Integer", "long", "int");

    private final RelationAnalyzer relationAnalyzer;

    public IrExporter() {
        this(new RelationAnalyzer());
    }

    public IrExporter(RelationAnalyzer relationAnalyzer) {
        this.relationAnalyzer = relationAnalyzer;
    }

    /**
     * Exports the classified graph to an IR snapshot.
     *
     * @param graph the application graph
     * @param classifications the classification results for all types
     * @return the IR snapshot for plugins
     */
    public IrSnapshot export(ApplicationGraph graph, List<ClassificationResult> classifications) {
        Objects.requireNonNull(graph, "graph cannot be null");
        Objects.requireNonNull(classifications, "classifications cannot be null");

        // Build classification context from domain classifications for relation analysis
        Map<NodeId, ClassificationResult> domainClassificationsMap = classifications.stream()
                .filter(c -> c.target() == ClassificationTarget.DOMAIN)
                .filter(c -> c.status() == ClassificationStatus.CLASSIFIED)
                .collect(Collectors.toMap(ClassificationResult::subjectId, c -> c));

        ClassificationContext context = new ClassificationContext(domainClassificationsMap);
        GraphQuery query = graph.query();

        List<DomainType> domainTypes = classifications.stream()
                .filter(c -> c.target() == ClassificationTarget.DOMAIN)
                .filter(c -> c.status() == ClassificationStatus.CLASSIFIED)
                .map(c -> toDomainType(graph, c, query, context))
                .sorted(Comparator.comparing(DomainType::qualifiedName))
                .toList();

        List<Port> ports = classifications.stream()
                .filter(c -> c.target() == ClassificationTarget.PORT)
                .filter(c -> c.status() == ClassificationStatus.CLASSIFIED)
                .map(c -> toPort(graph, c))
                .sorted(Comparator.comparing(Port::qualifiedName))
                .toList();

        return new IrSnapshot(
                new DomainModel(domainTypes), new PortModel(ports), createMetadata(graph, domainTypes, ports));
    }

    private DomainType toDomainType(
            ApplicationGraph graph,
            ClassificationResult classification,
            GraphQuery query,
            ClassificationContext context) {
        TypeNode node = graph.typeNode(classification.subjectId())
                .orElseThrow(() -> new IllegalStateException(
                        "Classification refers to unknown node: " + classification.subjectId()));

        // Extract relations using the RelationAnalyzer
        List<DomainRelation> relations = relationAnalyzer.analyzeRelations(node, query, context);

        DomainKind domainKind = toDomainKind(classification.kind());

        // Extract identity first to know the actual identity field name
        // Domain events and value objects do NOT have identity
        Optional<Identity> identity = shouldHaveIdentity(domainKind) ? extractIdentity(graph, node) : Optional.empty();
        String identityFieldName = identity.map(Identity::fieldName).orElse(null);

        return new DomainType(
                node.qualifiedName(),
                node.simpleName(),
                node.packageName(),
                domainKind,
                toSpiConfidence(classification.confidence()),
                toJavaConstruct(node.form()),
                identity,
                extractProperties(graph, node, identityFieldName),
                relations,
                extractAnnotationNames(node),
                SourceRef.toSpi(node.sourceRef()));
    }

    private Port toPort(ApplicationGraph graph, ClassificationResult classification) {
        TypeNode node = graph.typeNode(classification.subjectId())
                .orElseThrow(() -> new IllegalStateException(
                        "Classification refers to unknown node: " + classification.subjectId()));

        return new Port(
                node.qualifiedName(),
                node.simpleName(),
                node.packageName(),
                toPortKind(classification.kind()),
                toSpiPortDirection(classification.portDirection()),
                toSpiConfidence(classification.confidence()),
                extractManagedTypes(graph, node),
                extractPortMethods(graph, node),
                extractAnnotationNames(node),
                SourceRef.toSpi(node.sourceRef()));
    }

    // === Type conversions ===

    private DomainKind toDomainKind(String kind) {
        return DomainKind.valueOf(kind);
    }

    private boolean shouldHaveIdentity(DomainKind kind) {
        // Only aggregate roots and entities have identity
        // Value objects, domain events, identifiers, services, and actors do NOT
        return switch (kind) {
            case AGGREGATE_ROOT, ENTITY -> true;
            case VALUE_OBJECT,
                    DOMAIN_EVENT,
                    IDENTIFIER,
                    DOMAIN_SERVICE,
                    APPLICATION_SERVICE,
                    INBOUND_ONLY,
                    OUTBOUND_ONLY,
                    SAGA -> false;
        };
    }

    private PortKind toPortKind(String kind) {
        // Map core PortKind names to SPI PortKind
        return switch (kind) {
            case "REPOSITORY" -> PortKind.REPOSITORY;
            case "USE_CASE" -> PortKind.USE_CASE;
            case "GATEWAY" -> PortKind.GATEWAY;
            case "QUERY" -> PortKind.QUERY;
            case "COMMAND" -> PortKind.COMMAND;
            case "EVENT_PUBLISHER" -> PortKind.EVENT_PUBLISHER;
            default -> PortKind.GENERIC;
        };
    }

    private io.hexaglue.spi.ir.ConfidenceLevel toSpiConfidence(ConfidenceLevel confidence) {
        if (confidence == null) {
            return io.hexaglue.spi.ir.ConfidenceLevel.LOW;
        }
        return switch (confidence) {
            case EXPLICIT -> io.hexaglue.spi.ir.ConfidenceLevel.EXPLICIT;
            case HIGH -> io.hexaglue.spi.ir.ConfidenceLevel.HIGH;
            case MEDIUM -> io.hexaglue.spi.ir.ConfidenceLevel.MEDIUM;
            case LOW -> io.hexaglue.spi.ir.ConfidenceLevel.LOW;
        };
    }

    private io.hexaglue.spi.ir.PortDirection toSpiPortDirection(
            io.hexaglue.core.classification.port.PortDirection direction) {
        if (direction == null) {
            return io.hexaglue.spi.ir.PortDirection.DRIVEN;
        }
        return switch (direction) {
            case DRIVING -> io.hexaglue.spi.ir.PortDirection.DRIVING;
            case DRIVEN -> io.hexaglue.spi.ir.PortDirection.DRIVEN;
        };
    }

    private JavaConstruct toJavaConstruct(JavaForm form) {
        return switch (form) {
            case CLASS -> JavaConstruct.CLASS;
            case RECORD -> JavaConstruct.RECORD;
            case INTERFACE -> JavaConstruct.INTERFACE;
            case ENUM -> JavaConstruct.ENUM;
            case ANNOTATION -> JavaConstruct.INTERFACE; // Annotations treated as interfaces in IR
        };
    }

    // === Identity extraction ===

    private Optional<Identity> extractIdentity(ApplicationGraph graph, TypeNode node) {
        List<FieldNode> fields = graph.fieldsOf(node);

        // Priority 1: Look for explicit @Identity annotation
        Optional<FieldNode> annotatedId =
                fields.stream().filter(this::hasIdentityAnnotation).findFirst();
        if (annotatedId.isPresent()) {
            return Optional.of(createIdentity(annotatedId.get(), graph));
        }

        // Priority 2: Look for field named exactly "id"
        Optional<FieldNode> exactIdField =
                fields.stream().filter(f -> f.simpleName().equals("id")).findFirst();
        if (exactIdField.isPresent()) {
            return Optional.of(createIdentity(exactIdField.get(), graph));
        }

        // Priority 3: Look for field ending with "Id" (e.g., orderId, customerId)
        Optional<FieldNode> suffixIdField =
                fields.stream().filter(f -> f.simpleName().endsWith("Id")).findFirst();

        return suffixIdField.map(field -> createIdentity(field, graph));
    }

    private boolean hasIdentityAnnotation(FieldNode field) {
        return field.annotations().stream()
                .anyMatch(a -> a.qualifiedName().equals("org.jmolecules.ddd.annotation.Identity")
                        || a.simpleName().equals("Identity")
                        || a.simpleName().equals("Id"));
    }

    private Identity createIdentity(FieldNode field, ApplicationGraph graph) {
        TypeRef coreTypeRef = field.type();
        io.hexaglue.spi.ir.TypeRef spiType = toSpiTypeRef(coreTypeRef);

        UnwrapResult unwrapResult = unwrapIdentityType(coreTypeRef, graph);
        io.hexaglue.spi.ir.TypeRef spiUnwrappedType = toSpiTypeRef(unwrapResult.unwrappedType());

        IdentityStrategy strategy = determineStrategy(
                coreTypeRef.rawQualifiedName(), unwrapResult.unwrappedType().rawQualifiedName());

        return new Identity(field.simpleName(), spiType, spiUnwrappedType, strategy, unwrapResult.wrapperKind());
    }

    private record UnwrapResult(TypeRef unwrappedType, IdentityWrapperKind wrapperKind) {}

    private UnwrapResult unwrapIdentityType(TypeRef typeRef, ApplicationGraph graph) {
        String typeName = typeRef.rawQualifiedName();

        // Check if this is a known primitive/wrapper ID type
        if (IDENTITY_TYPES.contains(typeName)) {
            return new UnwrapResult(typeRef, IdentityWrapperKind.NONE);
        }

        // Check if the type is a record with a single component (likely an ID wrapper)
        Optional<TypeNode> idType = graph.typeNode(typeName);
        if (idType.isPresent()) {
            TypeNode typeNode = idType.get();
            List<FieldNode> fields = graph.fieldsOf(typeNode);

            if (fields.size() == 1) {
                TypeRef innerType = fields.get(0).type();
                if (IDENTITY_TYPES.contains(innerType.rawQualifiedName())) {
                    IdentityWrapperKind wrapperKind =
                            typeNode.isRecord() ? IdentityWrapperKind.RECORD : IdentityWrapperKind.CLASS;
                    return new UnwrapResult(innerType, wrapperKind);
                }
            }
        }

        return new UnwrapResult(typeRef, IdentityWrapperKind.NONE);
    }

    private IdentityStrategy determineStrategy(String typeName, String unwrappedTypeName) {
        // UUID is typically UUID strategy (application-generated)
        if (unwrappedTypeName.equals("java.util.UUID")) {
            return IdentityStrategy.UUID;
        }
        // Long/Integer often use database sequences or identity columns
        if (unwrappedTypeName.equals("java.lang.Long")
                || unwrappedTypeName.equals("long")
                || unwrappedTypeName.equals("java.lang.Integer")
                || unwrappedTypeName.equals("int")) {
            // Default to AUTO - let JPA provider decide
            return IdentityStrategy.AUTO;
        }
        // String is typically assigned (natural key or application-assigned)
        if (unwrappedTypeName.equals("java.lang.String")) {
            return IdentityStrategy.ASSIGNED;
        }
        return IdentityStrategy.UNKNOWN;
    }

    // === Property extraction ===

    private List<DomainProperty> extractProperties(ApplicationGraph graph, TypeNode node, String identityFieldName) {
        List<FieldNode> fields = graph.fieldsOf(node);

        return fields.stream()
                .filter(this::shouldIncludeField)
                .map(field -> toDomainProperty(field, identityFieldName))
                .toList();
    }

    /**
     * Determines if a field should be included in the domain properties.
     * Static and transient fields are excluded as they don't represent domain state.
     */
    private boolean shouldIncludeField(FieldNode field) {
        return !field.isStatic() && !field.isTransient();
    }

    private DomainProperty toDomainProperty(FieldNode field, String identityFieldName) {
        TypeRef typeRef = field.type();

        // Only mark as identity if this is the actual identity field, not just any field ending with "Id"
        // Fields like "customerId" are inter-aggregate references, not identity fields
        boolean isIdentity = identityFieldName != null && field.simpleName().equals(identityFieldName);

        return new DomainProperty(
                field.simpleName(),
                toSpiTypeRef(typeRef),
                extractCardinality(typeRef),
                extractNullability(field),
                isIdentity);
    }

    private io.hexaglue.spi.ir.TypeRef toSpiTypeRef(TypeRef coreTypeRef) {
        List<io.hexaglue.spi.ir.TypeRef> spiArguments =
                coreTypeRef.arguments().stream().map(this::toSpiTypeRef).toList();

        return io.hexaglue.spi.ir.TypeRef.parameterized(
                coreTypeRef.rawQualifiedName(), extractCardinality(coreTypeRef), spiArguments);
    }

    private Cardinality extractCardinality(TypeRef typeRef) {
        if (typeRef.isCollectionLike() || typeRef.isArray()) {
            return Cardinality.COLLECTION;
        }
        if (typeRef.isOptionalLike()) {
            return Cardinality.OPTIONAL;
        }
        return Cardinality.SINGLE;
    }

    private Nullability extractNullability(FieldNode field) {
        // Check for @Nullable or @NonNull annotations
        boolean hasNullable = field.annotations().stream()
                .anyMatch(
                        a -> a.simpleName().equals("Nullable") || a.simpleName().equals("CheckForNull"));
        if (hasNullable) {
            return Nullability.NULLABLE;
        }

        boolean hasNonNull = field.annotations().stream()
                .anyMatch(a -> a.simpleName().equals("NonNull")
                        || a.simpleName().equals("NotNull")
                        || a.simpleName().equals("Nonnull"));
        if (hasNonNull) {
            return Nullability.NON_NULL;
        }

        // Optionals are nullable by design
        if (field.type().isOptionalLike()) {
            return Nullability.NULLABLE;
        }

        // Primitives are never null
        if (isPrimitiveType(field.type().rawQualifiedName())) {
            return Nullability.NON_NULL;
        }

        return Nullability.UNKNOWN;
    }

    private boolean isPrimitiveType(String typeName) {
        return Set.of("boolean", "byte", "char", "short", "int", "long", "float", "double")
                .contains(typeName);
    }

    // === Port extraction ===

    private List<String> extractManagedTypes(ApplicationGraph graph, TypeNode portNode) {
        // Find all types used in method signatures (return types and parameters)
        Set<String> managedTypes = new LinkedHashSet<>();

        List<MethodNode> methods = graph.methodsOf(portNode);
        for (MethodNode method : methods) {
            // Return type
            addManagedType(method.returnType(), graph, managedTypes);

            // Parameters
            for (ParameterInfo param : method.parameters()) {
                addManagedType(param.type(), graph, managedTypes);
            }
        }

        return new ArrayList<>(managedTypes);
    }

    private void addManagedType(TypeRef typeRef, ApplicationGraph graph, Set<String> managedTypes) {
        // Unwrap collections and optionals
        TypeRef elementType = typeRef.unwrapElement();
        String typeName = elementType.rawQualifiedName();

        // Only include application types (not JDK types)
        if (graph.typeNode(typeName).isPresent()) {
            managedTypes.add(typeName);
        }
    }

    private List<PortMethod> extractPortMethods(ApplicationGraph graph, TypeNode portNode) {
        return graph.methodsOf(portNode).stream().map(this::toPortMethod).toList();
    }

    private PortMethod toPortMethod(MethodNode method) {
        List<String> paramTypes = method.parameters().stream()
                .map(p -> p.type().rawQualifiedName())
                .toList();

        return new PortMethod(method.simpleName(), method.returnType().rawQualifiedName(), paramTypes);
    }

    // === Annotation extraction ===

    private List<String> extractAnnotationNames(TypeNode node) {
        return node.annotations().stream().map(AnnotationRef::qualifiedName).toList();
    }

    // === Metadata ===

    private IrMetadata createMetadata(ApplicationGraph graph, List<DomainType> domainTypes, List<Port> ports) {
        String basePackage = inferBasePackage(graph);

        return new IrMetadata(basePackage, Instant.now(), ENGINE_VERSION, domainTypes.size(), ports.size());
    }

    private String inferBasePackage(ApplicationGraph graph) {
        return graph.typeNodes().stream()
                .map(TypeNode::packageName)
                .min(Comparator.comparingInt(String::length))
                .orElse("");
    }
}
