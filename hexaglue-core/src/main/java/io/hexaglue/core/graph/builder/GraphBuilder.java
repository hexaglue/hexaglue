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

package io.hexaglue.core.graph.builder;

import io.hexaglue.core.frontend.*;
import io.hexaglue.core.frontend.spoon.adapters.SpoonMethodAdapter;
import io.hexaglue.core.graph.ApplicationGraph;
import io.hexaglue.core.graph.model.*;
import io.hexaglue.core.graph.style.StyleDetectionResult;
import io.hexaglue.core.graph.style.StyleDetector;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.reflect.declaration.CtMethod;

/**
 * Builds an {@link ApplicationGraph} from a {@link JavaSemanticModel}.
 *
 * <p>The build process has three passes:
 * <ol>
 *   <li><b>Pass 1</b>: Create all TypeNodes from source types</li>
 *   <li><b>Pass 2</b>: Create member nodes and RAW edges</li>
 *   <li><b>Pass 3</b>: Compute DERIVED edges (delegated to {@link DerivedEdgeComputer})</li>
 * </ol>
 */
public final class GraphBuilder {

    private static final Logger log = LoggerFactory.getLogger(GraphBuilder.class);

    private final boolean computeDerivedEdges;
    private final StyleDetector styleDetector;
    private final CachedSpoonAnalyzer spoonAnalyzer;

    /**
     * Creates a builder with complexity calculation.
     *
     * @param spoonAnalyzer the analyzer for method complexity
     * @since 5.0.0
     */
    public GraphBuilder(CachedSpoonAnalyzer spoonAnalyzer) {
        this(true, spoonAnalyzer);
    }

    /**
     * Creates a builder with optional derived edges and complexity calculation.
     *
     * @param computeDerivedEdges whether to compute derived edges
     * @param spoonAnalyzer the analyzer for method complexity
     * @since 5.0.0
     */
    public GraphBuilder(boolean computeDerivedEdges, CachedSpoonAnalyzer spoonAnalyzer) {
        this.computeDerivedEdges = computeDerivedEdges;
        this.styleDetector = new StyleDetector();
        this.spoonAnalyzer = Objects.requireNonNull(spoonAnalyzer, "spoonAnalyzer cannot be null");
    }

    /**
     * Builds an application graph from the semantic model.
     *
     * @param model the semantic model to transform
     * @param metadata the graph metadata
     * @return the built graph
     */
    public ApplicationGraph build(JavaSemanticModel model, GraphMetadata metadata) {
        ApplicationGraph graph = new ApplicationGraph(metadata);

        // Get types (already sorted by JavaSemanticModel implementation)
        List<JavaType> types = model.types();

        log.info("Building graph from {} types", types.size());

        // Pass 1: Create all TypeNodes
        Set<String> knownTypes = new HashSet<>();
        for (JavaType type : types) {
            TypeNode node = createTypeNode(type);
            graph.addNode(node);
            knownTypes.add(type.qualifiedName());
        }

        log.debug("Pass 1 complete: {} type nodes created", graph.typeCount());

        // Pass 1.5: Detect package organization style and enrich metadata
        StyleDetectionResult styleResult = styleDetector.detect(graph.typeNodes(), metadata.basePackage());
        GraphMetadata enrichedMetadata = GraphMetadata.builder()
                .basePackage(metadata.basePackage())
                .javaVersion(metadata.javaVersion())
                .buildTimestamp(metadata.buildTimestamp())
                .sourceCount(metadata.sourceCount())
                .style(styleResult.style())
                .styleConfidence(styleResult.confidence())
                .detectedPatterns(styleResult.detectedPatterns())
                .build();
        graph.setMetadata(enrichedMetadata);

        log.debug(
                "Pass 1.5 complete: detected style {} with {} confidence",
                styleResult.style(),
                styleResult.confidence());

        // Pass 2: Create members and RAW edges
        for (JavaType type : types) {
            addMembersAndEdges(graph, type, knownTypes);
        }

        log.debug("Pass 2 complete: {} members, {} edges", graph.memberCount(), graph.edgeCount());

        // Pass 3: Compute DERIVED edges
        if (computeDerivedEdges) {
            new DerivedEdgeComputer().compute(graph);
            log.debug("Pass 3 complete: {} total edges", graph.edgeCount());
        }

        log.info("Graph built: {} nodes, {} edges", graph.nodeCount(), graph.edgeCount());

        return graph;
    }

    // === Pass 1: Create TypeNodes ===

    private TypeNode createTypeNode(JavaType type) {
        return TypeNode.builder()
                .qualifiedName(type.qualifiedName())
                .simpleName(type.simpleName())
                .packageName(type.packageName())
                .form(type.form())
                .modifiers(type.modifiers())
                .superType(type.superType().orElse(null))
                .interfaces(type.interfaces())
                .annotations(toAnnotationRefs(type.annotations()))
                .sourceRef(type.sourceRef().orElse(null))
                .documentation(type.documentation().orElse(null))
                .build();
    }

    // === Pass 2: Create members and edges ===

    private void addMembersAndEdges(ApplicationGraph graph, JavaType type, Set<String> knownTypes) {
        NodeId typeId = NodeId.type(type.qualifiedName());

        // Add hierarchy edges
        addHierarchyEdges(graph, type, typeId, knownTypes);

        // Add annotation edges for the type
        addAnnotationEdgesFromFrontend(graph, type, typeId, knownTypes);

        // Add fields
        for (JavaField field : type.fields()) {
            addField(graph, field, typeId, knownTypes);
        }

        // Add methods
        for (JavaMethod method : type.methods()) {
            addMethod(graph, method, typeId, knownTypes);
        }

        // Add constructors
        for (JavaConstructor ctor : type.constructors()) {
            addConstructor(graph, ctor, typeId, knownTypes);
        }
    }

    private void addHierarchyEdges(ApplicationGraph graph, JavaType type, NodeId typeId, Set<String> knownTypes) {
        // EXTENDS edge
        type.superType().ifPresent(superType -> {
            String superName = superType.rawQualifiedName();
            if (knownTypes.contains(superName)) {
                graph.addEdge(Edge.extends_(typeId, NodeId.type(superName)));
            }
        });

        // IMPLEMENTS edges
        for (TypeRef iface : type.interfaces()) {
            String ifaceName = iface.rawQualifiedName();
            if (knownTypes.contains(ifaceName)) {
                graph.addEdge(Edge.implements_(typeId, NodeId.type(ifaceName)));
            }
        }
    }

    private void addAnnotationEdgesFromFrontend(
            ApplicationGraph graph, JavaAnnotated element, NodeId nodeId, Set<String> knownTypes) {
        for (JavaAnnotation anno : element.annotations()) {
            String annoName = anno.annotationType().rawQualifiedName();
            if (knownTypes.contains(annoName)) {
                graph.addEdge(Edge.annotatedBy(nodeId, NodeId.type(annoName)));
            }
        }
    }

    private void addAnnotationEdges(ApplicationGraph graph, Node node, NodeId nodeId, Set<String> knownTypes) {
        for (AnnotationRef anno : node.annotations()) {
            String annoName = anno.qualifiedName();
            if (knownTypes.contains(annoName)) {
                graph.addEdge(Edge.annotatedBy(nodeId, NodeId.type(annoName)));
            }
        }
    }

    private void addField(ApplicationGraph graph, JavaField field, NodeId typeId, Set<String> knownTypes) {
        FieldNode fieldNode = FieldNode.builder()
                .declaringTypeName(extractTypeName(typeId))
                .simpleName(field.simpleName())
                .type(field.type())
                .modifiers(field.modifiers())
                .annotations(toAnnotationRefs(field.annotations()))
                .sourceRef(field.sourceRef().orElse(null))
                .documentation(field.documentation().orElse(null))
                .build();

        addMemberNode(graph, fieldNode, typeId, knownTypes);

        // FIELD_TYPE edge
        String fieldTypeName = field.type().rawQualifiedName();
        if (knownTypes.contains(fieldTypeName)) {
            graph.addEdge(Edge.fieldType(fieldNode.id(), NodeId.type(fieldTypeName)));
        }

        // TYPE_ARGUMENT edges for generic types
        addTypeArgumentEdges(graph, fieldNode.id(), field.type(), knownTypes);
    }

    private void addMethod(ApplicationGraph graph, JavaMethod method, NodeId typeId, Set<String> knownTypes) {
        // Calculate complexity from Spoon AST
        OptionalInt complexity = calculateComplexity(method);

        MethodNode methodNode = MethodNode.builder()
                .declaringTypeName(extractTypeName(typeId))
                .simpleName(method.simpleName())
                .returnType(method.returnType())
                .parameters(toParameterInfos(method.parameters()))
                .modifiers(method.modifiers())
                .annotations(toAnnotationRefs(method.annotations()))
                .sourceRef(method.sourceRef().orElse(null))
                .cyclomaticComplexity(complexity)
                .documentation(method.documentation().orElse(null))
                .build();

        addMemberNode(graph, methodNode, typeId, knownTypes);

        // RETURN_TYPE edge
        String returnTypeName = method.returnType().rawQualifiedName();
        if (knownTypes.contains(returnTypeName) && !returnTypeName.equals("void")) {
            graph.addEdge(Edge.returnType(methodNode.id(), NodeId.type(returnTypeName)));
        }

        // TYPE_ARGUMENT edges for return type
        addTypeArgumentEdges(graph, methodNode.id(), method.returnType(), knownTypes);

        // PARAMETER_TYPE edges
        addParameterTypeEdges(graph, methodNode.id(), method.parameters(), knownTypes);
    }

    /**
     * Calculates cyclomatic complexity for a method.
     *
     * @param method the method to analyze
     * @return the complexity, or empty for abstract methods
     * @since 5.0.0
     */
    private OptionalInt calculateComplexity(JavaMethod method) {
        if (method instanceof SpoonMethodAdapter adapter) {
            CtMethod<?> ctMethod = adapter.getCtMethod();
            if (ctMethod.getBody() != null) {
                return OptionalInt.of(spoonAnalyzer.analyzeMethodBody(ctMethod).cyclomaticComplexity());
            }
        }
        return OptionalInt.empty();
    }

    private void addConstructor(ApplicationGraph graph, JavaConstructor ctor, NodeId typeId, Set<String> knownTypes) {
        ConstructorNode ctorNode = ConstructorNode.builder()
                .declaringTypeName(extractTypeName(typeId))
                .parameters(toParameterInfos(ctor.parameters()))
                .modifiers(ctor.modifiers())
                .annotations(toAnnotationRefs(ctor.annotations()))
                .sourceRef(ctor.sourceRef().orElse(null))
                .documentation(ctor.documentation().orElse(null))
                .build();

        addMemberNode(graph, ctorNode, typeId, knownTypes);

        // PARAMETER_TYPE edges
        addParameterTypeEdges(graph, ctorNode.id(), ctor.parameters(), knownTypes);
    }

    /**
     * Adds a member node to the graph with common setup.
     *
     * <p>This method handles the common pattern for all member types:
     * <ol>
     *   <li>Add the node to the graph</li>
     *   <li>Add DECLARES edge from declaring type to member</li>
     *   <li>Add ANNOTATED_BY edges for annotations</li>
     * </ol>
     *
     * @param graph the application graph
     * @param memberNode the member node to add
     * @param typeId the declaring type's node id
     * @param knownTypes set of known type names for edge creation
     */
    private void addMemberNode(ApplicationGraph graph, MemberNode memberNode, NodeId typeId, Set<String> knownTypes) {
        graph.addNode(memberNode);
        graph.addEdge(Edge.declares(typeId, memberNode.id()));
        addAnnotationEdges(graph, memberNode, memberNode.id(), knownTypes);
    }

    /**
     * Adds PARAMETER_TYPE and TYPE_ARGUMENT edges for method/constructor parameters.
     *
     * @param graph the application graph
     * @param memberId the member node id (method or constructor)
     * @param parameters the list of parameters
     * @param knownTypes set of known type names for edge creation
     */
    private void addParameterTypeEdges(
            ApplicationGraph graph, NodeId memberId, List<? extends JavaParameter> parameters, Set<String> knownTypes) {
        for (JavaParameter param : parameters) {
            String paramTypeName = param.type().rawQualifiedName();
            if (knownTypes.contains(paramTypeName)) {
                graph.addEdge(Edge.parameterType(memberId, NodeId.type(paramTypeName)));
            }
            addTypeArgumentEdges(graph, memberId, param.type(), knownTypes);
        }
    }

    /**
     * Recursively adds TYPE_ARGUMENT edges for generic type parameters.
     *
     * <p>This method handles nested generic types like {@code Map<String, List<Order>>}
     * by recursively traversing the type argument tree.
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code List<Order>} → creates edge to Order</li>
     *   <li>{@code Map<String, Order>} → creates edge to Order (String is not in knownTypes)</li>
     *   <li>{@code Optional<List<Order>>} → creates edge to Order (nested)</li>
     * </ul>
     *
     * @param graph the application graph
     * @param sourceId the source node id (field, method, or constructor)
     * @param typeRef the type reference to process
     * @param knownTypes set of known type names for edge creation
     */
    private void addTypeArgumentEdges(
            ApplicationGraph graph, NodeId sourceId, TypeRef typeRef, Set<String> knownTypes) {
        for (TypeRef arg : typeRef.arguments()) {
            String argName = arg.rawQualifiedName();
            if (knownTypes.contains(argName)) {
                graph.addEdge(Edge.typeArgument(sourceId, NodeId.type(argName)));
            }
            // Recursively handle nested type arguments
            addTypeArgumentEdges(graph, sourceId, arg, knownTypes);
        }
    }

    // === Conversion helpers ===

    private List<AnnotationRef> toAnnotationRefs(List<JavaAnnotation> annotations) {
        return annotations.stream()
                .map(a -> new AnnotationRef(
                        a.annotationType().rawQualifiedName(),
                        a.annotationType().simpleName(),
                        a.values()))
                .toList();
    }

    private List<ParameterInfo> toParameterInfos(List<? extends JavaParameter> parameters) {
        return parameters.stream()
                .map(p -> new ParameterInfo(p.name(), p.type(), toAnnotationRefs(p.annotations())))
                .toList();
    }

    private String extractTypeName(NodeId typeId) {
        // NodeId format: "type:com.example.Order"
        String value = typeId.value();
        return value.startsWith("type:") ? value.substring(5) : value;
    }
}
