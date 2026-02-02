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

package io.hexaglue.core.builder;

import io.hexaglue.arch.model.UnclassifiedType.UnclassifiedCategory;
import io.hexaglue.core.classification.ClassificationResult;
import io.hexaglue.core.graph.model.AnnotationRef;
import io.hexaglue.core.graph.model.TypeNode;
import java.util.Set;

/**
 * Determines the category of unclassified types.
 *
 * <p>When a type cannot be classified into a known architectural pattern,
 * this detector determines why by analyzing the type's characteristics
 * and the classification result.</p>
 *
 * <h2>Category Priority Order</h2>
 * <ol>
 *   <li>CONFLICTING - Classification had conflicts</li>
 *   <li>OUT_OF_SCOPE - Test/mock/stub package</li>
 *   <li>UTILITY - Utils/Helper class</li>
 *   <li>TECHNICAL - Infrastructure/framework class</li>
 *   <li>AMBIGUOUS - Had evidence but no clear match</li>
 *   <li>UNKNOWN - Default fallback</li>
 * </ol>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * UnclassifiedCategoryDetector detector = new UnclassifiedCategoryDetector();
 * UnclassifiedCategory category = detector.detect(typeNode, classificationResult);
 * }</pre>
 *
 * @since 4.1.0
 */
public final class UnclassifiedCategoryDetector {

    private static final Set<String> UTILITY_SUFFIXES =
            Set.of("Utils", "Util", "Helper", "Helpers", "Tools", "Constants");

    private static final Set<String> OUT_OF_SCOPE_PACKAGE_MARKERS = Set.of(
            ".test.",
            ".tests.",
            ".mock.",
            ".mocks.",
            ".stub.",
            ".stubs.",
            ".fixture.",
            ".fixtures.",
            ".fake.",
            ".fakes.");

    private static final Set<String> GENERATED_ANNOTATIONS = Set.of(
            "javax.annotation.Generated", "javax.annotation.processing.Generated", "jakarta.annotation.Generated");

    private static final Set<String> TECHNICAL_ANNOTATIONS = Set.of(
            "org.springframework.context.annotation.Configuration",
            "org.springframework.stereotype.Component",
            "org.springframework.stereotype.Service",
            "org.springframework.stereotype.Controller",
            "org.springframework.stereotype.Repository",
            "org.springframework.web.bind.annotation.RestController",
            "jakarta.inject.Named",
            "javax.inject.Named",
            "jakarta.enterprise.context.ApplicationScoped",
            "jakarta.enterprise.context.RequestScoped");

    /**
     * Creates a new UnclassifiedCategoryDetector.
     */
    public UnclassifiedCategoryDetector() {
        // Stateless detector
    }

    /**
     * Detects the category of an unclassified type.
     *
     * @param typeNode the type node
     * @param classification the classification result
     * @return the detected category
     */
    public UnclassifiedCategory detect(TypeNode typeNode, ClassificationResult classification) {
        // Priority 1: CONFLICTING - Classification had conflicts
        if (classification.hasConflicts()) {
            return UnclassifiedCategory.CONFLICTING;
        }

        // Priority 2: OUT_OF_SCOPE - Test/mock/stub package
        if (isOutOfScope(typeNode)) {
            return UnclassifiedCategory.OUT_OF_SCOPE;
        }

        // Priority 3: UTILITY - Utils/Helper class
        if (isUtilityClass(typeNode)) {
            return UnclassifiedCategory.UTILITY;
        }

        // Priority 4: TECHNICAL - Infrastructure/framework class
        if (isTechnicalClass(typeNode)) {
            return UnclassifiedCategory.TECHNICAL;
        }

        // Priority 5: AMBIGUOUS - Had evidence but no clear match
        if (!classification.evidence().isEmpty()) {
            return UnclassifiedCategory.AMBIGUOUS;
        }

        // Default: UNKNOWN
        return UnclassifiedCategory.UNKNOWN;
    }

    private boolean isOutOfScope(TypeNode typeNode) {
        String qualifiedName = typeNode.qualifiedName().toLowerCase();
        if (OUT_OF_SCOPE_PACKAGE_MARKERS.stream().anyMatch(qualifiedName::contains)) {
            return true;
        }
        // @Generated types are out of scope (generated adapters visible in audit mode)
        return typeNode.annotations().stream()
                .map(AnnotationRef::qualifiedName)
                .anyMatch(GENERATED_ANNOTATIONS::contains);
    }

    private boolean isUtilityClass(TypeNode typeNode) {
        String simpleName = typeNode.simpleName();
        return UTILITY_SUFFIXES.stream().anyMatch(simpleName::endsWith);
    }

    private boolean isTechnicalClass(TypeNode typeNode) {
        return typeNode.annotations().stream()
                .map(AnnotationRef::qualifiedName)
                .anyMatch(TECHNICAL_ANNOTATIONS::contains);
    }
}
