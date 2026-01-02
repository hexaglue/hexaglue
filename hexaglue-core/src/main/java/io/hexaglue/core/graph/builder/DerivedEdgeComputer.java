package io.hexaglue.core.graph.builder;

import io.hexaglue.core.frontend.JavaForm;
import io.hexaglue.core.graph.ApplicationGraph;
import io.hexaglue.core.graph.model.*;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Computes DERIVED edges from the RAW edges in the graph.
 *
 * <p>Derived edges include:
 * <ul>
 *   <li>{@link EdgeKind#USES_IN_SIGNATURE} - types used in interface method signatures</li>
 *   <li>{@link EdgeKind#USES_AS_COLLECTION_ELEMENT} - types used as collection elements</li>
 * </ul>
 */
public final class DerivedEdgeComputer {

    private static final Logger log = LoggerFactory.getLogger(DerivedEdgeComputer.class);

    /**
     * Computes all derived edges and adds them to the graph.
     */
    public void compute(ApplicationGraph graph) {
        int initialCount = graph.edgeCount();

        computeUsesInSignature(graph);
        computeUsesAsCollectionElement(graph);

        int derivedCount = graph.edgeCount() - initialCount;
        log.debug("Computed {} derived edges", derivedCount);
    }

    /**
     * Computes USES_IN_SIGNATURE edges.
     *
     * <p>For each interface, finds all types used in method signatures
     * (return types and parameter types) and creates edges.
     */
    private void computeUsesInSignature(ApplicationGraph graph) {
        // Track already added edges to avoid duplicates
        Set<String> addedEdges = new HashSet<>();

        for (TypeNode type : graph.typeNodes()) {
            if (type.form() != JavaForm.INTERFACE) {
                continue;
            }

            // Find all methods of this interface
            for (MethodNode method : graph.methodsOf(type)) {
                // Process return type
                processTypeForSignature(graph, type, method, method.returnType(), "return", addedEdges);

                // Process parameter types
                int paramIndex = 0;
                for (ParameterInfo param : method.parameters()) {
                    processTypeForSignature(graph, type, method, param.type(), "param:" + paramIndex, addedEdges);
                    paramIndex++;
                }
            }
        }
    }

    private void processTypeForSignature(
            ApplicationGraph graph,
            TypeNode interfaceType,
            MethodNode method,
            io.hexaglue.core.frontend.TypeRef typeRef,
            String via,
            Set<String> addedEdges) {

        String typeName = typeRef.rawQualifiedName();
        if (typeName.equals("void")) {
            return;
        }

        NodeId typeId = NodeId.type(typeName);

        // Create edge only if the type is in the graph (application type)
        if (graph.containsNode(typeId)) {
            // Check if edge already exists (for idempotency)
            String edgeKey = interfaceType.id() + "->" + typeId + ":USES_IN_SIGNATURE";
            if (!addedEdges.contains(edgeKey)
                    && !graph.containsEdge(interfaceType.id(), typeId, EdgeKind.USES_IN_SIGNATURE)) {
                // Create derived edge with proof
                EdgeProof proof = EdgeProof.signatureUsage(method.id(), via);
                graph.addEdge(Edge.usesInSignature(interfaceType.id(), typeId, proof));
                addedEdges.add(edgeKey);
            }
        }

        // Always process type arguments (e.g., List<Order> -> Order, Optional<Order> -> Order)
        // even if the container type is external
        int argIndex = 0;
        for (io.hexaglue.core.frontend.TypeRef arg : typeRef.arguments()) {
            processTypeForSignature(graph, interfaceType, method, arg, via + ":typeArg:" + argIndex, addedEdges);
            argIndex++;
        }
    }

    /**
     * Computes USES_AS_COLLECTION_ELEMENT edges.
     *
     * <p>For collection/optional types, unwraps the element type
     * and creates edges if the element is an application type.
     */
    private void computeUsesAsCollectionElement(ApplicationGraph graph) {
        Set<String> addedEdges = new HashSet<>();

        // Process fields with collection types
        for (Node node : graph.nodes()) {
            if (!(node instanceof FieldNode field)) {
                continue;
            }

            io.hexaglue.core.frontend.TypeRef fieldType = field.type();
            if (!fieldType.isCollectionLike() && !fieldType.isOptionalLike()) {
                continue;
            }

            // Get the element type
            io.hexaglue.core.frontend.TypeRef elementType = fieldType.unwrapElement();
            if (elementType == fieldType) {
                continue; // No unwrapping happened
            }

            String elementTypeName = elementType.rawQualifiedName();
            NodeId elementTypeId = NodeId.type(elementTypeName);
            if (!graph.containsNode(elementTypeId)) {
                continue;
            }

            // Find the declaring type
            graph.indexes().declaringTypeOf(field.id()).ifPresent(declaringTypeId -> {
                String edgeKey = declaringTypeId + "->" + elementTypeId + ":USES_AS_COLLECTION_ELEMENT";
                if (!addedEdges.contains(edgeKey)
                        && !graph.containsEdge(declaringTypeId, elementTypeId, EdgeKind.USES_AS_COLLECTION_ELEMENT)) {
                    String rule = fieldType.isCollectionLike()
                            ? EdgeProof.RULE_COLLECTION_UNWRAP
                            : EdgeProof.RULE_OPTIONAL_UNWRAP;
                    EdgeProof proof = EdgeProof.viaField(field.id(), rule);
                    graph.addEdge(Edge.usesAsCollectionElement(declaringTypeId, elementTypeId, proof));
                    addedEdges.add(edgeKey);
                }
            });
        }
    }
}
