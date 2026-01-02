package io.hexaglue.core.graph.builder;

import io.hexaglue.core.frontend.*;
import io.hexaglue.core.graph.ApplicationGraph;
import io.hexaglue.core.graph.model.*;
import io.hexaglue.core.graph.style.StyleDetectionResult;
import io.hexaglue.core.graph.style.StyleDetector;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    /**
     * Creates a builder that computes derived edges.
     */
    public GraphBuilder() {
        this(true);
    }

    /**
     * Creates a builder with optional derived edge computation.
     *
     * @param computeDerivedEdges whether to compute derived edges
     */
    public GraphBuilder(boolean computeDerivedEdges) {
        this.computeDerivedEdges = computeDerivedEdges;
        this.styleDetector = new StyleDetector();
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

        // Collect types sorted for deterministic order
        List<JavaType> types = model.types()
                .sorted(Comparator.comparing(JavaType::qualifiedName))
                .toList();

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
                .build();

        graph.addNode(fieldNode);

        // DECLARES edge
        graph.addEdge(Edge.declares(typeId, fieldNode.id()));

        // FIELD_TYPE edge
        String fieldTypeName = field.type().rawQualifiedName();
        if (knownTypes.contains(fieldTypeName)) {
            graph.addEdge(Edge.fieldType(fieldNode.id(), NodeId.type(fieldTypeName)));
        }

        // TYPE_ARGUMENT edges for generic types
        addTypeArgumentEdges(graph, fieldNode.id(), field.type(), knownTypes);

        // Annotation edges on field
        addAnnotationEdges(graph, fieldNode, fieldNode.id(), knownTypes);
    }

    private void addMethod(ApplicationGraph graph, JavaMethod method, NodeId typeId, Set<String> knownTypes) {
        MethodNode methodNode = MethodNode.builder()
                .declaringTypeName(extractTypeName(typeId))
                .simpleName(method.simpleName())
                .returnType(method.returnType())
                .parameters(toParameterInfos(method.parameters()))
                .modifiers(method.modifiers())
                .annotations(toAnnotationRefs(method.annotations()))
                .sourceRef(method.sourceRef().orElse(null))
                .build();

        graph.addNode(methodNode);

        // DECLARES edge
        graph.addEdge(Edge.declares(typeId, methodNode.id()));

        // RETURN_TYPE edge
        String returnTypeName = method.returnType().rawQualifiedName();
        if (knownTypes.contains(returnTypeName) && !returnTypeName.equals("void")) {
            graph.addEdge(Edge.returnType(methodNode.id(), NodeId.type(returnTypeName)));
        }

        // TYPE_ARGUMENT edges for return type
        addTypeArgumentEdges(graph, methodNode.id(), method.returnType(), knownTypes);

        // PARAMETER_TYPE edges
        for (JavaParameter param : method.parameters()) {
            String paramTypeName = param.type().rawQualifiedName();
            if (knownTypes.contains(paramTypeName)) {
                graph.addEdge(Edge.parameterType(methodNode.id(), NodeId.type(paramTypeName)));
            }
            // TYPE_ARGUMENT edges for parameter types
            addTypeArgumentEdges(graph, methodNode.id(), param.type(), knownTypes);
        }

        // Annotation edges on method
        addAnnotationEdges(graph, methodNode, methodNode.id(), knownTypes);
    }

    private void addConstructor(ApplicationGraph graph, JavaConstructor ctor, NodeId typeId, Set<String> knownTypes) {
        ConstructorNode ctorNode = ConstructorNode.builder()
                .declaringTypeName(extractTypeName(typeId))
                .parameters(toParameterInfos(ctor.parameters()))
                .modifiers(ctor.modifiers())
                .annotations(toAnnotationRefs(ctor.annotations()))
                .sourceRef(ctor.sourceRef().orElse(null))
                .build();

        graph.addNode(ctorNode);

        // DECLARES edge
        graph.addEdge(Edge.declares(typeId, ctorNode.id()));

        // PARAMETER_TYPE edges
        for (JavaParameter param : ctor.parameters()) {
            String paramTypeName = param.type().rawQualifiedName();
            if (knownTypes.contains(paramTypeName)) {
                graph.addEdge(Edge.parameterType(ctorNode.id(), NodeId.type(paramTypeName)));
            }
            // TYPE_ARGUMENT edges for parameter types
            addTypeArgumentEdges(graph, ctorNode.id(), param.type(), knownTypes);
        }

        // Annotation edges on constructor
        addAnnotationEdges(graph, ctorNode, ctorNode.id(), knownTypes);
    }

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
