package io.hexaglue.core.graph.model;

import io.hexaglue.core.frontend.SourceRef;
import java.util.List;
import java.util.Optional;

/**
 * Base class for all nodes in the application graph.
 *
 * <p>Nodes represent Java elements (types, fields, methods, constructors).
 * Each node has a stable {@link NodeId} for identification.
 */
public abstract sealed class Node permits TypeNode, MemberNode {

    /**
     * Returns the unique identifier of this node.
     */
    public abstract NodeId id();

    /**
     * Returns the simple name of this element.
     */
    public abstract String simpleName();

    /**
     * Returns the fully qualified name of this element.
     */
    public abstract String qualifiedName();

    /**
     * Returns the package name.
     */
    public abstract String packageName();

    /**
     * Returns the source location, if available.
     */
    public abstract Optional<SourceRef> sourceRef();

    /**
     * Returns the annotations on this element.
     */
    public abstract List<AnnotationRef> annotations();

    /**
     * Returns true if this node has an annotation with the given qualified name.
     */
    public boolean hasAnnotation(String qualifiedName) {
        return annotations().stream().anyMatch(a -> a.qualifiedName().equals(qualifiedName));
    }

    /**
     * Returns the annotation with the given qualified name, if present.
     */
    public Optional<AnnotationRef> annotation(String qualifiedName) {
        return annotations().stream()
                .filter(a -> a.qualifiedName().equals(qualifiedName))
                .findFirst();
    }

    /**
     * Returns true if this node has any jMolecules annotation.
     */
    public boolean hasJMoleculesAnnotation() {
        return annotations().stream().anyMatch(AnnotationRef::isJMolecules);
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Node other = (Node) obj;
        return id().equals(other.id());
    }

    @Override
    public final int hashCode() {
        return id().hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + id() + "]";
    }
}
