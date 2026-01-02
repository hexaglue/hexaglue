package io.hexaglue.core.graph.index;

import io.hexaglue.core.frontend.JavaForm;
import io.hexaglue.core.graph.model.*;
import java.util.*;

/**
 * Indexes for fast queries on the application graph.
 *
 * <p>Indexes are built incrementally as nodes and edges are added to the graph.
 * All returned collections are unmodifiable.
 */
public final class GraphIndexes {

    // === Primary indexes (by node attribute) ===

    /** Types by package name */
    private final Map<String, Set<NodeId>> byPackage = new HashMap<>();

    /** Types by JavaForm (CLASS, INTERFACE, RECORD, etc.) */
    private final Map<JavaForm, Set<NodeId>> byJavaForm = new HashMap<>();

    /** Elements by annotation qualified name */
    private final Map<String, Set<NodeId>> byAnnotation = new HashMap<>();

    /** All type node ids */
    private final Set<NodeId> allTypes = new LinkedHashSet<>();

    /** All member node ids */
    private final Set<NodeId> allMembers = new LinkedHashSet<>();

    // === Relationship indexes (from edges) ===

    /** Type → declared members (fields, methods, constructors) */
    private final Map<NodeId, Set<NodeId>> declaredMembers = new HashMap<>();

    /** Member → declaring type */
    private final Map<NodeId, NodeId> declaringType = new HashMap<>();

    /** Type → subtypes (via EXTENDS edge) */
    private final Map<NodeId, Set<NodeId>> subtypes = new HashMap<>();

    /** Type → supertypes (inverse of subtypes) */
    private final Map<NodeId, Set<NodeId>> supertypes = new HashMap<>();

    /** Interface → implementors (via IMPLEMENTS edge) */
    private final Map<NodeId, Set<NodeId>> implementors = new HashMap<>();

    /** Type → implemented interfaces */
    private final Map<NodeId, Set<NodeId>> implementedInterfaces = new HashMap<>();

    /** Type → types using it in signature (USES_IN_SIGNATURE) */
    private final Map<NodeId, Set<NodeId>> usedInSignatureOf = new HashMap<>();

    /** Field type → fields having that type (FIELD_TYPE) */
    private final Map<NodeId, Set<NodeId>> fieldsByType = new HashMap<>();

    /** Return type → methods returning that type (RETURN_TYPE) */
    private final Map<NodeId, Set<NodeId>> methodsByReturnType = new HashMap<>();

    /** Parameter type → methods having that parameter type (PARAMETER_TYPE) */
    private final Map<NodeId, Set<NodeId>> methodsByParameterType = new HashMap<>();

    // === Indexing methods ===

    /**
     * Indexes a node.
     */
    public void indexNode(Node node) {
        if (node instanceof TypeNode typeNode) {
            indexTypeNode(typeNode);
        } else if (node instanceof MemberNode memberNode) {
            indexMemberNode(memberNode);
        }
    }

    private void indexTypeNode(TypeNode node) {
        NodeId id = node.id();
        allTypes.add(id);

        // By package
        byPackage
                .computeIfAbsent(node.packageName(), k -> new LinkedHashSet<>())
                .add(id);

        // By form
        byJavaForm.computeIfAbsent(node.form(), k -> new LinkedHashSet<>()).add(id);

        // By annotation
        for (AnnotationRef anno : node.annotations()) {
            byAnnotation
                    .computeIfAbsent(anno.qualifiedName(), k -> new LinkedHashSet<>())
                    .add(id);
        }
    }

    private void indexMemberNode(MemberNode node) {
        NodeId id = node.id();
        allMembers.add(id);

        // By annotation
        for (AnnotationRef anno : node.annotations()) {
            byAnnotation
                    .computeIfAbsent(anno.qualifiedName(), k -> new LinkedHashSet<>())
                    .add(id);
        }
    }

    /**
     * Indexes an edge.
     */
    public void indexEdge(Edge edge) {
        switch (edge.kind()) {
            case DECLARES -> indexDeclares(edge);
            case EXTENDS -> indexExtends(edge);
            case IMPLEMENTS -> indexImplements(edge);
            case FIELD_TYPE -> indexFieldType(edge);
            case RETURN_TYPE -> indexReturnType(edge);
            case PARAMETER_TYPE -> indexParameterType(edge);
            case USES_IN_SIGNATURE -> indexUsesInSignature(edge);
            default -> {
                // Other edge kinds don't need special indexing
            }
        }
    }

    private void indexDeclares(Edge edge) {
        // type → member
        declaredMembers.computeIfAbsent(edge.from(), k -> new LinkedHashSet<>()).add(edge.to());
        // member → type
        declaringType.put(edge.to(), edge.from());
    }

    private void indexExtends(Edge edge) {
        // subtype → supertype
        supertypes.computeIfAbsent(edge.from(), k -> new LinkedHashSet<>()).add(edge.to());
        // supertype → subtype
        subtypes.computeIfAbsent(edge.to(), k -> new LinkedHashSet<>()).add(edge.from());
    }

    private void indexImplements(Edge edge) {
        // type → interface
        implementedInterfaces
                .computeIfAbsent(edge.from(), k -> new LinkedHashSet<>())
                .add(edge.to());
        // interface → implementor
        implementors.computeIfAbsent(edge.to(), k -> new LinkedHashSet<>()).add(edge.from());
    }

    private void indexFieldType(Edge edge) {
        // type → fields of that type
        fieldsByType.computeIfAbsent(edge.to(), k -> new LinkedHashSet<>()).add(edge.from());
    }

    private void indexReturnType(Edge edge) {
        // type → methods returning it
        methodsByReturnType
                .computeIfAbsent(edge.to(), k -> new LinkedHashSet<>())
                .add(edge.from());
    }

    private void indexParameterType(Edge edge) {
        // type → methods having it as parameter
        methodsByParameterType
                .computeIfAbsent(edge.to(), k -> new LinkedHashSet<>())
                .add(edge.from());
    }

    private void indexUsesInSignature(Edge edge) {
        // used type → interfaces using it
        usedInSignatureOf.computeIfAbsent(edge.to(), k -> new LinkedHashSet<>()).add(edge.from());
    }

    // === Query methods - Primary ===

    /**
     * Returns all type node ids in the given package.
     */
    public Set<NodeId> typesByPackage(String packageName) {
        return unmodifiable(byPackage.get(packageName));
    }

    /**
     * Returns all type node ids with the given form.
     */
    public Set<NodeId> typesByForm(JavaForm form) {
        return unmodifiable(byJavaForm.get(form));
    }

    /**
     * Returns all node ids annotated with the given annotation.
     */
    public Set<NodeId> byAnnotation(String annotationQualifiedName) {
        return unmodifiable(byAnnotation.get(annotationQualifiedName));
    }

    /**
     * Returns all type node ids.
     */
    public Set<NodeId> allTypes() {
        return Collections.unmodifiableSet(allTypes);
    }

    /**
     * Returns all member node ids.
     */
    public Set<NodeId> allMembers() {
        return Collections.unmodifiableSet(allMembers);
    }

    /**
     * Returns all interface type node ids.
     */
    public Set<NodeId> allInterfaces() {
        return typesByForm(JavaForm.INTERFACE);
    }

    /**
     * Returns all class type node ids.
     */
    public Set<NodeId> allClasses() {
        return typesByForm(JavaForm.CLASS);
    }

    /**
     * Returns all record type node ids.
     */
    public Set<NodeId> allRecords() {
        return typesByForm(JavaForm.RECORD);
    }

    // === Query methods - Relationships ===

    /**
     * Returns the members declared by the given type.
     */
    public Set<NodeId> membersOf(NodeId typeId) {
        return unmodifiable(declaredMembers.get(typeId));
    }

    /**
     * Returns the declaring type of the given member.
     */
    public Optional<NodeId> declaringTypeOf(NodeId memberId) {
        return Optional.ofNullable(declaringType.get(memberId));
    }

    /**
     * Returns the subtypes of the given type (direct only).
     */
    public Set<NodeId> subtypesOf(NodeId typeId) {
        return unmodifiable(subtypes.get(typeId));
    }

    /**
     * Returns the supertypes of the given type (direct only).
     */
    public Set<NodeId> supertypesOf(NodeId typeId) {
        return unmodifiable(supertypes.get(typeId));
    }

    /**
     * Returns the implementors of the given interface.
     */
    public Set<NodeId> implementorsOf(NodeId interfaceId) {
        return unmodifiable(implementors.get(interfaceId));
    }

    /**
     * Returns the interfaces implemented by the given type.
     */
    public Set<NodeId> interfacesOf(NodeId typeId) {
        return unmodifiable(implementedInterfaces.get(typeId));
    }

    /**
     * Returns the interfaces that use the given type in their signature.
     */
    public Set<NodeId> interfacesUsingInSignature(NodeId typeId) {
        return unmodifiable(usedInSignatureOf.get(typeId));
    }

    /**
     * Returns the fields that have the given type.
     */
    public Set<NodeId> fieldsOfType(NodeId typeId) {
        return unmodifiable(fieldsByType.get(typeId));
    }

    /**
     * Returns the methods that return the given type.
     */
    public Set<NodeId> methodsReturning(NodeId typeId) {
        return unmodifiable(methodsByReturnType.get(typeId));
    }

    /**
     * Returns the methods that have the given type as a parameter.
     */
    public Set<NodeId> methodsWithParameter(NodeId typeId) {
        return unmodifiable(methodsByParameterType.get(typeId));
    }

    // === Convenience query methods ===

    /**
     * Returns true if the given type is used in any repository signature.
     */
    public boolean isUsedInRepositorySignature(NodeId typeId) {
        Set<NodeId> users = usedInSignatureOf.get(typeId);
        if (users == null || users.isEmpty()) {
            return false;
        }
        // Check if any of the users has "Repository" suffix
        // Note: This requires access to the actual nodes, which we don't have here
        // This method is a simplified check based on node id patterns
        return users.stream().anyMatch(id -> id.value().contains("Repository"));
    }

    /**
     * Returns true if the given type has any subtypes.
     */
    public boolean hasSubtypes(NodeId typeId) {
        Set<NodeId> subs = subtypes.get(typeId);
        return subs != null && !subs.isEmpty();
    }

    /**
     * Returns true if the given type has any implementors.
     */
    public boolean hasImplementors(NodeId interfaceId) {
        Set<NodeId> impls = implementors.get(interfaceId);
        return impls != null && !impls.isEmpty();
    }

    /**
     * Returns true if the given type is annotated with the given annotation.
     */
    public boolean hasAnnotation(NodeId nodeId, String annotationQualifiedName) {
        Set<NodeId> annotated = byAnnotation.get(annotationQualifiedName);
        return annotated != null && annotated.contains(nodeId);
    }

    /**
     * Returns the count of types in the graph.
     */
    public int typeCount() {
        return allTypes.size();
    }

    /**
     * Returns the count of members in the graph.
     */
    public int memberCount() {
        return allMembers.size();
    }

    // === Helper ===

    private static Set<NodeId> unmodifiable(Set<NodeId> set) {
        return set != null ? Collections.unmodifiableSet(set) : Set.of();
    }
}
