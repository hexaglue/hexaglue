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
import io.hexaglue.core.frontend.SourceRef;
import io.hexaglue.core.graph.ApplicationGraph;
import io.hexaglue.core.graph.model.AnnotationRef;
import io.hexaglue.core.graph.model.NodeId;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import io.hexaglue.spi.ir.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

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
        List<Port> ports = exportPorts(graph, classifications);

        return new IrSnapshot(
                new DomainModel(domainTypes), new PortModel(ports), createMetadata(graph, domainTypes, ports));
    }

    private List<DomainType> exportDomainTypes(
            ApplicationGraph graph,
            List<ClassificationResult> classifications,
            GraphQuery query,
            ClassificationContext context) {
        return classifications.stream()
                .filter(c -> c.target() == ClassificationTarget.DOMAIN)
                .filter(c -> c.status() == ClassificationStatus.CLASSIFIED)
                .map(c -> toDomainType(graph, c, query, context))
                .sorted(Comparator.comparing(DomainType::qualifiedName))
                .toList();
    }

    private List<Port> exportPorts(ApplicationGraph graph, List<ClassificationResult> classifications) {
        // Build classification context from domain classifications for primary type detection
        Map<NodeId, ClassificationResult> domainClassificationsMap = classifications.stream()
                .filter(c -> c.target() == ClassificationTarget.DOMAIN)
                .filter(c -> c.status() == ClassificationStatus.CLASSIFIED)
                .collect(Collectors.toMap(ClassificationResult::subjectId, c -> c));

        return classifications.stream()
                .filter(c -> c.target() == ClassificationTarget.PORT)
                .filter(c -> c.status() == ClassificationStatus.CLASSIFIED)
                .map(c -> toPort(graph, c, domainClassificationsMap))
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

        return new DomainType(
                node.qualifiedName(),
                node.simpleName(),
                node.packageName(),
                domainKind,
                typeConverter.toSpiConfidence(classification.confidence()),
                typeConverter.toJavaConstruct(node.form()),
                identity,
                propertyExtractor.extractProperties(graph, node, identityFieldName),
                relations,
                extractAnnotationNames(node),
                SourceRef.toSpi(node.sourceRef()));
    }

    private Port toPort(
            ApplicationGraph graph,
            ClassificationResult classification,
            Map<NodeId, ClassificationResult> domainClassifications) {
        TypeNode node = graph.typeNode(classification.subjectId())
                .orElseThrow(() -> new IllegalStateException(
                        "Classification refers to unknown node: " + classification.subjectId()));

        List<String> managedTypes = portExtractor.extractManagedTypes(graph, node);
        String primaryManagedType = portExtractor.extractPrimaryManagedType(managedTypes, domainClassifications);

        return new Port(
                node.qualifiedName(),
                node.simpleName(),
                node.packageName(),
                typeConverter.toPortKind(classification.kind()),
                typeConverter.toSpiPortDirection(classification.portDirection()),
                typeConverter.toSpiConfidence(classification.confidence()),
                managedTypes,
                primaryManagedType,
                portExtractor.extractPortMethods(graph, node),
                extractAnnotationNames(node),
                SourceRef.toSpi(node.sourceRef()));
    }

    private List<String> extractAnnotationNames(TypeNode node) {
        return node.annotations().stream().map(AnnotationRef::qualifiedName).toList();
    }

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
