package io.hexaglue.core.frontend;

import java.util.List;
import java.util.Optional;

/**
 * An annotated Java element.
 */
public interface JavaAnnotated {

    /**
     * Returns all annotations on this element.
     */
    List<JavaAnnotation> annotations();

    /**
     * Returns true if this element has an annotation with the given qualified name.
     */
    default boolean hasAnnotation(String annotationFqn) {
        return annotations().stream().anyMatch(a -> a.qualifiedName().equals(annotationFqn));
    }

    /**
     * Returns the annotation with the given qualified name, if present.
     */
    default Optional<JavaAnnotation> getAnnotation(String annotationFqn) {
        return annotations().stream()
                .filter(a -> a.qualifiedName().equals(annotationFqn))
                .findFirst();
    }

    /**
     * Returns true if this element has any jMolecules annotation.
     */
    default boolean hasJMoleculesAnnotation() {
        return annotations().stream().anyMatch(a -> a.qualifiedName().startsWith("org.jmolecules."));
    }
}
