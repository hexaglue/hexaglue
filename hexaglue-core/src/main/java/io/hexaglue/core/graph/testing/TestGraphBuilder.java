package io.hexaglue.core.graph.testing;

import io.hexaglue.core.frontend.JavaForm;
import io.hexaglue.core.frontend.JavaModifier;
import io.hexaglue.core.frontend.TypeRef;
import io.hexaglue.core.graph.ApplicationGraph;
import io.hexaglue.core.graph.model.*;
import java.util.*;

/**
 * Fluent builder for creating {@link ApplicationGraph} instances in tests.
 *
 * <p>Example usage:
 * <pre>{@code
 * ApplicationGraph graph = TestGraphBuilder.create()
 *     .withClass("com.example.Order")
 *         .withField("id", "java.util.UUID")
 *         .withField("status", "com.example.OrderStatus")
 *     .withRecord("com.example.OrderId")
 *         .withField("value", "java.util.UUID")
 *     .withInterface("com.example.OrderRepository")
 *         .withMethod("save", "com.example.Order", "com.example.Order")
 *         .withMethod("findById", "java.util.Optional<com.example.Order>", "com.example.OrderId")
 *     .build();
 * }</pre>
 */
public final class TestGraphBuilder {

    private final String basePackage;
    private final List<TypeDefinition> typeDefinitions = new ArrayList<>();
    private TypeDefinition currentType;

    private TestGraphBuilder(String basePackage) {
        this.basePackage = basePackage;
    }

    /**
     * Creates a new builder with default base package "com.example".
     */
    public static TestGraphBuilder create() {
        return new TestGraphBuilder("com.example");
    }

    /**
     * Creates a new builder with the specified base package.
     */
    public static TestGraphBuilder create(String basePackage) {
        return new TestGraphBuilder(basePackage);
    }

    // =========================================================================
    // Type creation
    // =========================================================================

    /**
     * Adds a class with the given fully qualified name.
     */
    public TestGraphBuilder withClass(String fqn) {
        return addType(fqn, JavaForm.CLASS);
    }

    /**
     * Adds a record with the given fully qualified name.
     */
    public TestGraphBuilder withRecord(String fqn) {
        return addType(fqn, JavaForm.RECORD);
    }

    /**
     * Adds an interface with the given fully qualified name.
     */
    public TestGraphBuilder withInterface(String fqn) {
        return addType(fqn, JavaForm.INTERFACE);
    }

    /**
     * Adds an enum with the given fully qualified name.
     */
    public TestGraphBuilder withEnum(String fqn) {
        return addType(fqn, JavaForm.ENUM);
    }

    private TestGraphBuilder addType(String fqn, JavaForm form) {
        if (currentType != null) {
            typeDefinitions.add(currentType);
        }
        currentType = new TypeDefinition(fqn, form);
        return this;
    }

    // =========================================================================
    // Type modifiers
    // =========================================================================

    /**
     * Adds an annotation to the current type.
     */
    public TestGraphBuilder annotatedWith(String annotationFqn) {
        requireCurrentType();
        currentType.annotations.add(annotationFqn);
        return this;
    }

    /**
     * Sets the current type as public.
     */
    public TestGraphBuilder asPublic() {
        requireCurrentType();
        currentType.modifiers.add(JavaModifier.PUBLIC);
        return this;
    }

    /**
     * Sets the current type as abstract.
     */
    public TestGraphBuilder asAbstract() {
        requireCurrentType();
        currentType.modifiers.add(JavaModifier.ABSTRACT);
        return this;
    }

    /**
     * Sets the supertype for the current type.
     */
    public TestGraphBuilder extending(String superTypeFqn) {
        requireCurrentType();
        currentType.superType = superTypeFqn;
        return this;
    }

    /**
     * Adds an interface that the current type implements.
     */
    public TestGraphBuilder implementing(String interfaceFqn) {
        requireCurrentType();
        currentType.interfaces.add(interfaceFqn);
        return this;
    }

    // =========================================================================
    // Field creation
    // =========================================================================

    /**
     * Adds a field to the current type.
     *
     * @param name the field name
     * @param typeFqn the field type (fully qualified name)
     */
    public TestGraphBuilder withField(String name, String typeFqn) {
        requireCurrentType();
        currentType.fields.add(new FieldDefinition(name, typeFqn, Set.of()));
        return this;
    }

    /**
     * Adds a private final field to the current type.
     */
    public TestGraphBuilder withFinalField(String name, String typeFqn) {
        requireCurrentType();
        currentType.fields.add(new FieldDefinition(name, typeFqn, Set.of(JavaModifier.PRIVATE, JavaModifier.FINAL)));
        return this;
    }

    /**
     * Adds a collection field to the current type.
     *
     * @param name the field name
     * @param elementTypeFqn the element type (e.g., "com.example.LineItem")
     */
    public TestGraphBuilder withListField(String name, String elementTypeFqn) {
        requireCurrentType();
        currentType.fields.add(new FieldDefinition(name, "java.util.List<" + elementTypeFqn + ">", Set.of()));
        return this;
    }

    /**
     * Adds a Set field to the current type.
     */
    public TestGraphBuilder withSetField(String name, String elementTypeFqn) {
        requireCurrentType();
        currentType.fields.add(new FieldDefinition(name, "java.util.Set<" + elementTypeFqn + ">", Set.of()));
        return this;
    }

    // =========================================================================
    // Method creation
    // =========================================================================

    /**
     * Adds a method to the current type.
     *
     * @param name the method name
     * @param returnTypeFqn the return type (use "void" for void methods)
     * @param parameterTypeFqns the parameter types
     */
    public TestGraphBuilder withMethod(String name, String returnTypeFqn, String... parameterTypeFqns) {
        requireCurrentType();
        currentType.methods.add(new MethodDefinition(name, returnTypeFqn, List.of(parameterTypeFqns), Set.of()));
        return this;
    }

    /**
     * Adds a void method to the current type.
     */
    public TestGraphBuilder withVoidMethod(String name, String... parameterTypeFqns) {
        return withMethod(name, "void", parameterTypeFqns);
    }

    /**
     * Adds an abstract method to the current type.
     */
    public TestGraphBuilder withAbstractMethod(String name, String returnTypeFqn, String... parameterTypeFqns) {
        requireCurrentType();
        currentType.methods.add(
                new MethodDefinition(name, returnTypeFqn, List.of(parameterTypeFqns), Set.of(JavaModifier.ABSTRACT)));
        return this;
    }

    // =========================================================================
    // Build
    // =========================================================================

    /**
     * Builds the application graph.
     */
    public ApplicationGraph build() {
        if (currentType != null) {
            typeDefinitions.add(currentType);
            currentType = null;
        }

        GraphMetadata metadata = GraphMetadata.of(basePackage, 17, typeDefinitions.size());
        ApplicationGraph graph = new ApplicationGraph(metadata);

        // First pass: add all type nodes
        for (TypeDefinition typeDef : typeDefinitions) {
            TypeNode typeNode = buildTypeNode(typeDef);
            graph.addNode(typeNode);
        }

        // Second pass: add members and edges
        for (TypeDefinition typeDef : typeDefinitions) {
            addMembersAndEdges(graph, typeDef);
        }

        return graph;
    }

    private TypeNode buildTypeNode(TypeDefinition typeDef) {
        TypeNode.Builder builder =
                TypeNode.builder().qualifiedName(typeDef.fqn).form(typeDef.form).modifiers(typeDef.modifiers);

        // Add annotations
        List<AnnotationRef> annotationRefs =
                typeDef.annotations.stream().map(AnnotationRef::of).toList();
        builder.annotations(annotationRefs);

        // Add supertype
        if (typeDef.superType != null) {
            builder.superType(TypeRef.of(typeDef.superType));
        }

        // Add interfaces
        if (!typeDef.interfaces.isEmpty()) {
            builder.interfaces(typeDef.interfaces.stream().map(TypeRef::of).toList());
        }

        return builder.build();
    }

    private void addMembersAndEdges(ApplicationGraph graph, TypeDefinition typeDef) {
        NodeId typeId = NodeId.type(typeDef.fqn);

        // Add fields
        for (FieldDefinition fieldDef : typeDef.fields) {
            FieldNode fieldNode = buildFieldNode(typeDef.fqn, fieldDef);
            graph.addNode(fieldNode);
            graph.addEdge(Edge.declares(typeId, fieldNode.id()));

            // Add FIELD_TYPE edge if target type exists in graph
            TypeRef fieldType = parseTypeRef(fieldDef.typeFqn);
            NodeId targetTypeId = NodeId.type(fieldType.rawQualifiedName());
            if (graph.containsNode(targetTypeId)) {
                graph.addEdge(Edge.fieldType(fieldNode.id(), targetTypeId));
            }
        }

        // Add methods
        for (MethodDefinition methodDef : typeDef.methods) {
            MethodNode methodNode = buildMethodNode(typeDef.fqn, methodDef);
            graph.addNode(methodNode);
            graph.addEdge(Edge.declares(typeId, methodNode.id()));

            // Add RETURN_TYPE edge if target type exists
            TypeRef returnType = parseTypeRef(methodDef.returnTypeFqn);
            NodeId returnTypeId = NodeId.type(returnType.rawQualifiedName());
            if (graph.containsNode(returnTypeId)) {
                graph.addEdge(Edge.returnType(methodNode.id(), returnTypeId));
            }

            // Add PARAMETER_TYPE edges
            for (String paramType : methodDef.parameterTypeFqns) {
                TypeRef paramTypeRef = parseTypeRef(paramType);
                NodeId paramTypeId = NodeId.type(paramTypeRef.rawQualifiedName());
                if (graph.containsNode(paramTypeId)) {
                    graph.addEdge(Edge.parameterType(methodNode.id(), paramTypeId));
                }
            }
        }

        // Add EXTENDS edge
        if (typeDef.superType != null) {
            NodeId superTypeId = NodeId.type(typeDef.superType);
            if (graph.containsNode(superTypeId)) {
                graph.addEdge(Edge.extends_(typeId, superTypeId));
            }
        }

        // Add IMPLEMENTS edges
        for (String iface : typeDef.interfaces) {
            NodeId ifaceId = NodeId.type(iface);
            if (graph.containsNode(ifaceId)) {
                graph.addEdge(Edge.implements_(typeId, ifaceId));
            }
        }

        // Add ANNOTATED_BY edges
        for (String ann : typeDef.annotations) {
            NodeId annId = NodeId.type(ann);
            if (graph.containsNode(annId)) {
                graph.addEdge(Edge.annotatedBy(typeId, annId));
            }
        }
    }

    private FieldNode buildFieldNode(String declaringTypeFqn, FieldDefinition fieldDef) {
        return FieldNode.builder()
                .declaringTypeName(declaringTypeFqn)
                .simpleName(fieldDef.name)
                .type(parseTypeRef(fieldDef.typeFqn))
                .modifiers(fieldDef.modifiers)
                .build();
    }

    private MethodNode buildMethodNode(String declaringTypeFqn, MethodDefinition methodDef) {
        List<ParameterInfo> params = new ArrayList<>();
        for (int i = 0; i < methodDef.parameterTypeFqns.size(); i++) {
            params.add(new ParameterInfo("arg" + i, parseTypeRef(methodDef.parameterTypeFqns.get(i)), List.of()));
        }

        return MethodNode.builder()
                .declaringTypeName(declaringTypeFqn)
                .simpleName(methodDef.name)
                .returnType(parseTypeRef(methodDef.returnTypeFqn))
                .parameters(params)
                .modifiers(methodDef.modifiers)
                .build();
    }

    private TypeRef parseTypeRef(String typeFqn) {
        // Handle generic types like "java.util.List<com.example.Order>"
        int angleBracket = typeFqn.indexOf('<');
        if (angleBracket > 0) {
            String raw = typeFqn.substring(0, angleBracket);
            String argsPart = typeFqn.substring(angleBracket + 1, typeFqn.length() - 1);
            // Simple parsing for single type argument
            TypeRef arg = parseTypeRef(argsPart);
            return TypeRef.parameterized(raw, arg);
        }
        return TypeRef.of(typeFqn);
    }

    private void requireCurrentType() {
        if (currentType == null) {
            throw new IllegalStateException(
                    "No current type. Call withClass(), withRecord(), or withInterface() first.");
        }
    }

    // =========================================================================
    // Internal types
    // =========================================================================

    private static class TypeDefinition {
        final String fqn;
        final JavaForm form;
        final Set<JavaModifier> modifiers = new HashSet<>();
        final List<String> annotations = new ArrayList<>();
        String superType;
        final List<String> interfaces = new ArrayList<>();
        final List<FieldDefinition> fields = new ArrayList<>();
        final List<MethodDefinition> methods = new ArrayList<>();

        TypeDefinition(String fqn, JavaForm form) {
            this.fqn = fqn;
            this.form = form;
        }
    }

    private record FieldDefinition(String name, String typeFqn, Set<JavaModifier> modifiers) {}

    private record MethodDefinition(
            String name, String returnTypeFqn, List<String> parameterTypeFqns, Set<JavaModifier> modifiers) {}
}
