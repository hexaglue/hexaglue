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
import io.hexaglue.core.classification.Evidence;
import io.hexaglue.core.frontend.SourceRef;
import io.hexaglue.core.graph.ApplicationGraph;
import io.hexaglue.core.graph.model.AnnotationRef;
import io.hexaglue.core.graph.model.NodeId;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import io.hexaglue.spi.classification.ClassificationEvidence;
import io.hexaglue.spi.classification.ClassificationStrategy;
import io.hexaglue.spi.classification.PrimaryClassificationResult;
import io.hexaglue.spi.ir.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Exports a classified application graph to the IR (Intermediate Representation).
 *
 * <p>The IR is the stable API for plugins. This exporter converts the internal
 * graph representation and classification results into the public SPI types.
 *
 * <p>This class orchestrates the export process using specialized extractors:
 * <ul>
 *   <li>{@link TypeConverter} - Type conversions between core and SPI</li>
 *   <li>{@link IdentityExtractor} - Identity field extraction</li>
 *   <li>{@link PropertyExtractor} - Domain property extraction</li>
 *   <li>{@link PortExtractor} - Port method and managed types extraction</li>
 * </ul>
 */
public final class IrExporter {

    private static final String ENGINE_VERSION = "2.0.0-SNAPSHOT";

    private final RelationAnalyzer relationAnalyzer;
    private final TypeConverter typeConverter;
    private final IdentityExtractor identityExtractor;
    private final PropertyExtractor propertyExtractor;
    private final PortExtractor portExtractor;

    public IrExporter() {
        this(new RelationAnalyzer());
    }

    public IrExporter(RelationAnalyzer relationAnalyzer) {
        this.relationAnalyzer = relationAnalyzer;
        this.typeConverter = new TypeConverter();
        this.identityExtractor = new IdentityExtractor(typeConverter);
        this.propertyExtractor = new PropertyExtractor(typeConverter);
        this.portExtractor = new PortExtractor();
    }

    /**
     * Exports the classified graph to an IR snapshot.
     *
     * @param graph the application graph
     * @param classifications the classification results for all types
     * @return the IR snapshot for plugins
     */
    public IrSnapshot export(ApplicationGraph graph, List<ClassificationResult> classifications) {
        return export(graph, classifications, null, null);
    }

    /**
     * Exports the classified graph to an IR snapshot with project information.
     *
     * @param graph the application graph
     * @param classifications the classification results for all types
     * @param projectName the project name (e.g., from Maven pom.xml), can be null
     * @param projectVersion the project version (e.g., "1.0.0-SNAPSHOT"), can be null
     * @return the IR snapshot for plugins
     * @since 3.0.0
     */
    public IrSnapshot export(
            ApplicationGraph graph,
            List<ClassificationResult> classifications,
            String projectName,
            String projectVersion) {
        Objects.requireNonNull(graph, "graph cannot be null");
        Objects.requireNonNull(classifications, "classifications cannot be null");

        // Build classification context from domain classifications for relation analysis
        Map<NodeId, ClassificationResult> domainClassificationsMap = classifications.stream()
                .filter(c -> c.target() == ClassificationTarget.DOMAIN)
                .filter(c -> c.status() == ClassificationStatus.CLASSIFIED)
                .collect(Collectors.toMap(ClassificationResult::subjectId, c -> c));

        ClassificationContext context = new ClassificationContext(domainClassificationsMap);
        GraphQuery query = graph.query();

        List<DomainType> domainTypes = exportDomainTypes(graph, classifications, query, context);
        List<Port> ports = exportPorts(graph, classifications, domainTypes);

        return new IrSnapshot(
                new DomainModel(domainTypes),
                new PortModel(ports),
                createMetadata(graph, domainTypes, ports, projectName, projectVersion));
    }

    private List<DomainType> exportDomainTypes(
            ApplicationGraph graph,
            List<ClassificationResult> classifications,
            GraphQuery query,
            ClassificationContext context) {

        // Export classified domain types
        List<DomainType> classifiedTypes = classifications.stream()
                .filter(c -> c.target() == ClassificationTarget.DOMAIN)
                .filter(c -> c.status() == ClassificationStatus.CLASSIFIED)
                .map(c -> toDomainType(graph, c, query, context))
                .toList();

        // Find and export unclassified VALUE_OBJECT types (records and enums in the domain)
        // that are referenced by classified types - needed by plugins for embedded/enum fields
        List<DomainType> unclassifiedValueObjects = exportUnclassifiedValueObjects(graph, query, classifiedTypes);

        // Merge and sort all domain types
        return Stream.concat(classifiedTypes.stream(), unclassifiedValueObjects.stream())
                .sorted(Comparator.comparing(DomainType::qualifiedName))
                .toList();
    }

    /**
     * Exports unclassified VALUE_OBJECT types that are referenced by classified types.
     *
     * <p>These types are needed by plugins to:
     * <ul>
     *   <li>Detect enum fields for {@code @Enumerated} annotation</li>
     *   <li>Detect VALUE_OBJECT collections for {@code @ElementCollection}</li>
     *   <li>Detect embedded VALUE_OBJECT fields for {@code @Embedded}</li>
     * </ul>
     *
     * <p>Types are detected as VALUE_OBJECT if they are:
     * <ul>
     *   <li>Records (immutable data carriers)</li>
     *   <li>Enums (fixed set of values)</li>
     *   <li>Classes without identity fields (data structures without own lifecycle)</li>
     * </ul>
     *
     * <p>Only includes types that are:
     * <ul>
     *   <li>Potential VALUE_OBJECTs (records, enums, or classes without identity)</li>
     *   <li>Not already classified</li>
     *   <li>Referenced by at least one classified domain type (via fields)</li>
     * </ul>
     *
     * @param graph the application graph
     * @param query the graph query interface
     * @param classifiedTypes the list of classified domain types
     * @return list of unclassified VALUE_OBJECT types referenced by classified types
     */
    private List<DomainType> exportUnclassifiedValueObjects(
            ApplicationGraph graph, GraphQuery query, List<DomainType> classifiedTypes) {
        // Collect qualified names of classified types for exclusion
        Set<String> classifiedNames =
                classifiedTypes.stream().map(DomainType::qualifiedName).collect(Collectors.toSet());

        // Collect type names referenced by classified types (via properties)
        Set<String> referencedTypes = classifiedTypes.stream()
                .flatMap(dt -> dt.properties().stream())
                .map(prop -> prop.type().qualifiedName())
                .collect(Collectors.toSet());

        // Also include types from collection generics
        classifiedTypes.stream()
                .flatMap(dt -> dt.properties().stream())
                .filter(prop -> prop.type().isCollectionLike())
                .map(prop -> prop.type().unwrapElement())
                .filter(t -> t != null)
                .map(t -> t.qualifiedName())
                .forEach(referencedTypes::add);

        return graph.typeNodes().stream()
                .filter(node -> !classifiedNames.contains(node.qualifiedName()))
                .filter(node -> referencedTypes.contains(node.qualifiedName()))
                .filter(node -> looksLikeValueObject(node, query))
                .filter(node -> !node.isInterface()) // Safety check
                .map(node -> toMinimalDomainType(node, query))
                .toList();
    }

    /**
     * Determines if a type looks like a VALUE_OBJECT based on heuristics.
     *
     * <p>A type is considered a VALUE_OBJECT if it is:
     * <ul>
     *   <li>A record (immutable data carrier)</li>
     *   <li>An enum (fixed set of values)</li>
     *   <li>A class without identity fields (data structure without own lifecycle)</li>
     * </ul>
     *
     * @param node the type node to check
     * @param query the graph query for field access
     * @return true if the type looks like a VALUE_OBJECT
     */
    private boolean looksLikeValueObject(TypeNode node, GraphQuery query) {
        // Records and enums are clearly VALUE_OBJECTs
        if (node.isRecord() || node.isEnum()) {
            return true;
        }

        // Classes without identity fields are likely VALUE_OBJECTs
        // This heuristic helps detect embedded collection elements like OrderLine
        if (node.isClass() && !hasIdentityField(node, query)) {
            return true;
        }

        return false;
    }

    /**
     * Checks if a type has its own identity field.
     *
     * <p>A field is considered an identity of the type if:
     * <ul>
     *   <li>It is named exactly "id"</li>
     *   <li>Or it matches the pattern "{typeName}Id" (e.g., "orderId" in Order)</li>
     * </ul>
     *
     * <p>This is more precise than just checking if any field ends with "Id",
     * which would incorrectly flag foreign key references (like "productId" in OrderLine)
     * as identity fields.
     *
     * @param node the type node to check
     * @param query the graph query for field access
     * @return true if the type has its own identity field
     */
    private boolean hasIdentityField(TypeNode node, GraphQuery query) {
        String typeName = node.simpleName();
        String expectedIdFieldName = Character.toLowerCase(typeName.charAt(0)) + typeName.substring(1) + "Id";

        return query.fieldsOf(node).stream().anyMatch(field -> {
            String fieldName = field.simpleName();
            // Field is "id" or matches "{typeName}Id" pattern (e.g., "orderId" for Order)
            return fieldName.equals("id") || fieldName.equals(expectedIdFieldName);
        });
    }

    /**
     * Creates a minimal DomainType for an unclassified VALUE_OBJECT type.
     *
     * <p>This is used to export records, enums, and classes that weren't classified
     * but are needed by plugins to properly handle embedded/enum fields.
     *
     * <p>Properties are extracted for records and classes to support {@code @Embeddable} generation.
     * Enums don't need properties since they're mapped via {@code @Enumerated}.
     *
     * @param node the type node
     * @param query the graph query interface (for property extraction)
     * @return a minimal DomainType with VALUE_OBJECT classification
     */
    private DomainType toMinimalDomainType(TypeNode node, GraphQuery query) {
        // Extract properties for records and classes (needed for @Embeddable generation)
        // Enums don't need properties since they map via @Enumerated
        List<DomainProperty> properties =
                node.isEnum() ? List.of() : propertyExtractor.extractProperties(query.graph(), node, null);

        return new DomainType(
                node.qualifiedName(),
                node.simpleName(),
                node.packageName(),
                DomainKind.VALUE_OBJECT,
                ConfidenceLevel.LOW, // Low confidence since inferred, not classified
                typeConverter.toJavaConstruct(node.form()),
                Optional.empty(), // No identity for VALUE_OBJECT
                properties,
                List.of(), // No relations for minimal export
                extractAnnotationNames(node),
                SourceRef.toSpi(node.sourceRef()));
    }

    private List<Port> exportPorts(
            ApplicationGraph graph, List<ClassificationResult> classifications, List<DomainType> domainTypes) {
        // Build classification context from domain classifications for primary type detection
        Map<NodeId, ClassificationResult> domainClassificationsMap = classifications.stream()
                .filter(c -> c.target() == ClassificationTarget.DOMAIN)
                .filter(c -> c.status() == ClassificationStatus.CLASSIFIED)
                .collect(Collectors.toMap(ClassificationResult::subjectId, c -> c));

        // Build identity map from domain types for method classification
        Map<String, Identity> identityByTypeName = domainTypes.stream()
                .filter(dt -> dt.identity().isPresent())
                .collect(Collectors.toMap(
                        DomainType::qualifiedName, dt -> dt.identity().get()));

        return classifications.stream()
                .filter(c -> c.target() == ClassificationTarget.PORT)
                .filter(c -> c.status() == ClassificationStatus.CLASSIFIED)
                .map(c -> toPort(graph, c, domainClassificationsMap, identityByTypeName))
                .sorted(Comparator.comparing(Port::qualifiedName))
                .toList();
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

        DomainKind domainKind = typeConverter.toDomainKind(classification.kind());

        // Extract identity first to know the actual identity field name
        // Domain events and value objects do NOT have identity
        Optional<Identity> identity = typeConverter.shouldHaveIdentity(domainKind)
                ? identityExtractor.extractIdentity(graph, node)
                : Optional.empty();
        String identityFieldName = identity.map(Identity::fieldName).orElse(null);

        // Extract properties and enrich them with relation info
        List<DomainProperty> properties = propertyExtractor.extractProperties(graph, node, identityFieldName);
        List<DomainProperty> enrichedProperties = enrichPropertiesWithRelations(properties, relations);

        return new DomainType(
                node.qualifiedName(),
                node.simpleName(),
                node.packageName(),
                domainKind,
                typeConverter.toSpiConfidence(classification.confidence()),
                typeConverter.toJavaConstruct(node.form()),
                identity,
                enrichedProperties,
                relations,
                extractAnnotationNames(node),
                SourceRef.toSpi(node.sourceRef()));
    }

    /**
     * Enriches properties with relation information from DomainRelations.
     *
     * <p>For each property that has a corresponding relation (matched by field name),
     * creates a new DomainProperty with the RelationInfo populated. This enables
     * plugins to detect relationships using {@link DomainProperty#hasRelation()}.
     *
     * @param properties the original properties from PropertyExtractor
     * @param relations the relations from RelationAnalyzer
     * @return properties enriched with relation info
     */
    private List<DomainProperty> enrichPropertiesWithRelations(
            List<DomainProperty> properties, List<DomainRelation> relations) {
        // Build a map of property name -> relation for quick lookup
        Map<String, DomainRelation> relationsByProperty =
                relations.stream().collect(Collectors.toMap(DomainRelation::propertyName, r -> r, (a, b) -> a));

        return properties.stream()
                .map(prop -> {
                    DomainRelation relation = relationsByProperty.get(prop.name());
                    if (relation != null) {
                        // Create RelationInfo from DomainRelation
                        RelationInfo relationInfo = toRelationInfo(relation);
                        // Determine if this should be embedded (EMBEDDED relation kind)
                        boolean isEmbedded = relation.kind() == RelationKind.EMBEDDED;
                        return new DomainProperty(
                                prop.name(),
                                prop.type(),
                                prop.cardinality(),
                                prop.nullability(),
                                prop.isIdentity(),
                                isEmbedded,
                                relationInfo);
                    }
                    return prop;
                })
                .toList();
    }

    /**
     * Converts a DomainRelation to a RelationInfo.
     *
     * @param relation the domain relation
     * @return the relation info for the property
     */
    private RelationInfo toRelationInfo(DomainRelation relation) {
        return new RelationInfo(
                relation.kind(),
                relation.targetTypeFqn(),
                relation.mappedBy(),
                true, // owning side by default
                relation.cascade(),
                relation.fetch(),
                relation.targetKind());
    }

    private Port toPort(
            ApplicationGraph graph,
            ClassificationResult classification,
            Map<NodeId, ClassificationResult> domainClassifications,
            Map<String, Identity> identityByTypeName) {
        TypeNode node = graph.typeNode(classification.subjectId())
                .orElseThrow(() -> new IllegalStateException(
                        "Classification refers to unknown node: " + classification.subjectId()));

        List<String> managedTypes = portExtractor.extractManagedTypes(graph, node);
        String primaryManagedType = portExtractor.extractPrimaryManagedType(managedTypes, domainClassifications);

        // Get the identity of the primary managed type for method classification
        Optional<Identity> aggregateIdentity = primaryManagedType != null
                ? Optional.ofNullable(identityByTypeName.get(primaryManagedType))
                : Optional.empty();

        return new Port(
                node.qualifiedName(),
                node.simpleName(),
                node.packageName(),
                typeConverter.toPortKind(classification.kind()),
                typeConverter.toSpiPortDirection(classification.portDirection()),
                typeConverter.toSpiConfidence(classification.confidence()),
                managedTypes,
                primaryManagedType,
                portExtractor.extractPortMethods(graph, node, aggregateIdentity),
                extractAnnotationNames(node),
                SourceRef.toSpi(node.sourceRef()));
    }

    private List<String> extractAnnotationNames(TypeNode node) {
        return node.annotations().stream().map(AnnotationRef::qualifiedName).toList();
    }

    private IrMetadata createMetadata(
            ApplicationGraph graph,
            List<DomainType> domainTypes,
            List<Port> ports,
            String projectName,
            String projectVersion) {
        String basePackage = inferBasePackage(graph);
        return new IrMetadata(
                basePackage,
                projectName,
                projectVersion,
                Instant.now(),
                ENGINE_VERSION,
                domainTypes.size(),
                ports.size());
    }

    private String inferBasePackage(ApplicationGraph graph) {
        return graph.typeNodes().stream()
                .map(TypeNode::packageName)
                .min(Comparator.comparingInt(String::length))
                .orElse("");
    }

    /**
     * Exports primary classification results for enrichment plugins.
     *
     * <p>Converts core classification results to SPI PrimaryClassificationResult instances
     * that enrichment plugins can analyze and potentially override.
     *
     * @param classifications the core classification results
     * @return list of primary classification results for the SPI
     */
    public List<PrimaryClassificationResult> exportPrimaryClassifications(List<ClassificationResult> classifications) {
        Objects.requireNonNull(classifications, "classifications cannot be null");

        return classifications.stream()
                .filter(c -> c.target() == ClassificationTarget.DOMAIN
                        || (c.target() == null && c.status() == ClassificationStatus.UNCLASSIFIED))
                .map(this::toPrimaryClassificationResult)
                .sorted(Comparator.comparing(PrimaryClassificationResult::typeName))
                .toList();
    }

    /**
     * Converts a Core ClassificationResult to an SPI PrimaryClassificationResult.
     *
     * <p>This adapter bridges the internal classification model with the stable SPI,
     * enabling enrichment plugins to work with classification results.
     *
     * <p>Mapping details:
     * <ul>
     *   <li>typeName: Extracted from subjectId</li>
     *   <li>kind: Converted via TypeConverter (null for unclassified)</li>
     *   <li>certainty: Derived from both confidence and status</li>
     *   <li>strategy: Inferred from criteria name and evidence</li>
     *   <li>reasoning: Taken from justification</li>
     *   <li>evidences: Transformed from Evidence to ClassificationEvidence</li>
     * </ul>
     *
     * @param coreResult the core classification result
     * @return the SPI primary classification result
     */
    private PrimaryClassificationResult toPrimaryClassificationResult(ClassificationResult coreResult) {
        // Extract the qualified type name from NodeId (format: "type:com.example.Order")
        String nodeIdValue = coreResult.subjectId().value();
        String typeName = nodeIdValue.startsWith("type:") ? nodeIdValue.substring(5) : nodeIdValue;

        // Handle unclassified case
        if (coreResult.status() == ClassificationStatus.UNCLASSIFIED) {
            return PrimaryClassificationResult.unclassified(
                    typeName, coreResult.justification() != null ? coreResult.justification() : "No criteria matched");
        }

        // Handle conflict case - mark as uncertain
        if (coreResult.status() == ClassificationStatus.CONFLICT) {
            return new PrimaryClassificationResult(
                    typeName,
                    null, // kind is null for conflicts
                    io.hexaglue.spi.classification.CertaintyLevel.UNCERTAIN,
                    ClassificationStrategy.WEIGHTED, // conflicts suggest weighted decision
                    coreResult.justification() != null ? coreResult.justification() : "Multiple conflicting criteria",
                    extractEvidences(coreResult));
        }

        // Handle successful classification
        DomainKind domainKind = typeConverter.toDomainKind(coreResult.kind());
        io.hexaglue.spi.classification.CertaintyLevel certainty = typeConverter.toSpiCertainty(coreResult.confidence());
        ClassificationStrategy strategy = deriveStrategy(coreResult);

        return new PrimaryClassificationResult(
                typeName,
                domainKind,
                certainty,
                strategy,
                coreResult.justification() != null ? coreResult.justification() : "",
                extractEvidences(coreResult));
    }

    /**
     * Derives the classification strategy from the core result.
     *
     * <p>Maps criteria names to SPI ClassificationStrategy values:
     * <ul>
     *   <li>Annotation criteria → ANNOTATION</li>
     *   <li>Repository criteria → REPOSITORY</li>
     *   <li>Record criteria → RECORD</li>
     *   <li>Relationship/composition → COMPOSITION</li>
     *   <li>Others → WEIGHTED</li>
     * </ul>
     *
     * @param result the core classification result
     * @return the inferred strategy
     */
    private ClassificationStrategy deriveStrategy(ClassificationResult result) {
        if (result.matchedCriteria() == null) {
            return ClassificationStrategy.UNCLASSIFIED;
        }

        String criteriaName = result.matchedCriteria().toLowerCase();

        // Check for annotation-based classification
        if (criteriaName.contains("annotation") || criteriaName.contains("@")) {
            return ClassificationStrategy.ANNOTATION;
        }

        // Check for repository pattern
        if (criteriaName.contains("repository")) {
            return ClassificationStrategy.REPOSITORY;
        }

        // Check for record heuristic
        if (criteriaName.contains("record")) {
            return ClassificationStrategy.RECORD;
        }

        // Check for composition/relationship based
        if (criteriaName.contains("composition")
                || criteriaName.contains("relationship")
                || criteriaName.contains("embedded")) {
            return ClassificationStrategy.COMPOSITION;
        }

        // Default to weighted for other criteria
        return ClassificationStrategy.WEIGHTED;
    }

    /**
     * Extracts and converts evidence from core model to SPI model.
     *
     * <p>Each core Evidence is transformed to a ClassificationEvidence with:
     * <ul>
     *   <li>signal: Evidence type name (ANNOTATION, NAMING, STRUCTURE, etc.)</li>
     *   <li>weight: Derived from evidence priority (1-100 scale)</li>
     *   <li>description: Original evidence description</li>
     * </ul>
     *
     * @param result the core classification result
     * @return list of SPI classification evidences
     */
    private List<ClassificationEvidence> extractEvidences(ClassificationResult result) {
        if (result.evidence() == null || result.evidence().isEmpty()) {
            return List.of();
        }

        return result.evidence().stream().map(this::toClassificationEvidence).toList();
    }

    /**
     * Converts a single core Evidence to SPI ClassificationEvidence.
     *
     * @param evidence the core evidence
     * @return the SPI classification evidence
     */
    private ClassificationEvidence toClassificationEvidence(Evidence evidence) {
        // Map evidence type to signal name
        String signal = evidence.type().name();

        // Derive weight from evidence type (annotation = 100, structure = 80, etc.)
        int weight =
                switch (evidence.type()) {
                    case ANNOTATION -> 100;
                    case STRUCTURE -> 80;
                    case RELATIONSHIP -> 70;
                    case NAMING -> 50;
                    case PACKAGE -> 40;
                };

        return new ClassificationEvidence(signal, weight, evidence.description());
    }
}
