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
