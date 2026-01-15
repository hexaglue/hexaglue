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

package io.hexaglue.arch;

import io.hexaglue.syntax.TypeSyntax;
import java.util.Objects;

/**
 * An element that could not be classified into any architectural category.
 *
 * <p>UnclassifiedType is a fallback for types that don't match any known
 * architectural pattern. The classification trace will contain hints for
 * how to make the classification explicit.</p>
 *
 * <h2>Common Reasons</h2>
 * <ul>
 *   <li>No matching criterion with sufficient confidence</li>
 *   <li>Conflicting classification signals</li>
 *   <li>Utility classes that don't fit DDD/Hexagonal patterns</li>
 *   <li>Test classes or generated code</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * UnclassifiedType unknown = new UnclassifiedType(
 *     ElementId.of("com.example.SomeUtility"),
 *     "Utility class with no clear architectural role",
 *     typeSyntax,
 *     classificationTrace
 * );
 *
 * // Check hints for clarification
 * unknown.classificationTrace().remediationHints().forEach(System.out::println);
 * }</pre>
 *
 * @param id the unique identifier
 * @param reason the reason for being unclassified
 * @param syntax the syntax information (nullable)
 * @param classificationTrace the trace with remediation hints
 * @since 4.0.0
 */
public record UnclassifiedType(ElementId id, String reason, TypeSyntax syntax, ClassificationTrace classificationTrace)
        implements ArchElement.Marker {

    /**
     * Creates a new UnclassifiedType instance.
     *
     * @param id the identifier, must not be null
     * @param reason the reason for being unclassified (can be null)
     * @param syntax the syntax (can be null)
     * @param classificationTrace the trace, must not be null
     * @throws NullPointerException if id or classificationTrace is null
     */
    public UnclassifiedType {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(classificationTrace, "classificationTrace must not be null");
    }

    @Override
    public ElementKind kind() {
        return ElementKind.UNCLASSIFIED;
    }

    /**
     * Returns whether a reason was provided.
     *
     * @return true if a reason is available
     */
    public boolean hasReason() {
        return reason != null && !reason.isBlank();
    }

    /**
     * Returns whether remediation hints are available.
     *
     * @return true if hints are available
     */
    public boolean hasRemediationHints() {
        return !classificationTrace.remediationHints().isEmpty();
    }

    /**
     * Creates an UnclassifiedType with a reason.
     *
     * @param qualifiedName the fully qualified name
     * @param reason the reason for being unclassified
     * @param trace the classification trace
     * @return a new UnclassifiedType
     */
    public static UnclassifiedType of(String qualifiedName, String reason, ClassificationTrace trace) {
        return new UnclassifiedType(ElementId.of(qualifiedName), reason, null, trace);
    }

    /**
     * Creates an UnclassifiedType without a specific reason.
     *
     * @param qualifiedName the fully qualified name
     * @param trace the classification trace
     * @return a new UnclassifiedType
     */
    public static UnclassifiedType of(String qualifiedName, ClassificationTrace trace) {
        return new UnclassifiedType(ElementId.of(qualifiedName), null, null, trace);
    }
}
