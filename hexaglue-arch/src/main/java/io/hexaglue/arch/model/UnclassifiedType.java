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

package io.hexaglue.arch.model;

import io.hexaglue.arch.ClassificationTrace;
import java.util.Objects;

/**
 * Represents a type that could not be classified into any architectural category.
 *
 * <p>UnclassifiedType is a fallback for types that don't match any known
 * architectural pattern. It includes the category of unclassification and
 * the classification trace with remediation hints.</p>
 *
 * <h2>Categories</h2>
 * <ul>
 *   <li>CONFLICTING - Multiple classification criteria matched with conflicts</li>
 *   <li>UTILITY - Utility/helper class (ends with Utils, Helper, etc.)</li>
 *   <li>OUT_OF_SCOPE - Test, mock, or generated code</li>
 *   <li>TECHNICAL - Infrastructure/framework class (Configuration, Component, etc.)</li>
 *   <li>AMBIGUOUS - Criteria evaluated but no clear match</li>
 *   <li>UNKNOWN - No conditions matched at all</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Check category and get hints
 * UnclassifiedType unclassified = ...;
 * if (unclassified.category() == UnclassifiedCategory.CONFLICTING) {
 *     unclassified.classification().conflicts().forEach(System.out::println);
 * }
 * }</pre>
 *
 * @param id the unique identifier
 * @param structure the structural description of the type
 * @param classification the classification trace with remediation hints
 * @param category the category of unclassification
 * @param reason optional reason for being unclassified
 * @since 4.1.0
 */
public record UnclassifiedType(
        TypeId id,
        TypeStructure structure,
        ClassificationTrace classification,
        UnclassifiedCategory category,
        String reason)
        implements ArchType {

    /**
     * Creates a new UnclassifiedType.
     *
     * @param id the type id, must not be null
     * @param structure the type structure, must not be null
     * @param classification the classification trace, must not be null
     * @param category the unclassification category, must not be null
     * @param reason the reason for being unclassified (can be null)
     * @throws NullPointerException if id, structure, classification, or category is null
     */
    public UnclassifiedType {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(structure, "structure must not be null");
        Objects.requireNonNull(classification, "classification must not be null");
        Objects.requireNonNull(category, "category must not be null");
    }

    @Override
    public ArchKind kind() {
        return ArchKind.UNCLASSIFIED;
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
        return !classification.remediationHints().isEmpty();
    }

    /**
     * Creates an UnclassifiedType with the given parameters.
     *
     * @param id the type id
     * @param structure the type structure
     * @param classification the classification trace
     * @param category the unclassification category
     * @return a new UnclassifiedType
     */
    public static UnclassifiedType of(
            TypeId id, TypeStructure structure, ClassificationTrace classification, UnclassifiedCategory category) {
        return new UnclassifiedType(id, structure, classification, category, null);
    }

    /**
     * Creates an UnclassifiedType with a reason.
     *
     * @param id the type id
     * @param structure the type structure
     * @param classification the classification trace
     * @param category the unclassification category
     * @param reason the reason for being unclassified
     * @return a new UnclassifiedType
     */
    public static UnclassifiedType of(
            TypeId id,
            TypeStructure structure,
            ClassificationTrace classification,
            UnclassifiedCategory category,
            String reason) {
        return new UnclassifiedType(id, structure, classification, category, reason);
    }

    /**
     * Categories of unclassified types.
     *
     * <p>These categories help understand why a type was not classified
     * and provide guidance for remediation.</p>
     *
     * @since 4.1.0
     */
    public enum UnclassifiedCategory {

        /**
         * Multiple classification criteria matched with conflicts.
         *
         * <p>The DecisionPolicy returned a conflict and no clear winner could be determined.</p>
         */
        CONFLICTING,

        /**
         * Utility or helper class.
         *
         * <p>The type name ends with Utils, Helper, Util, or similar patterns.</p>
         */
        UTILITY,

        /**
         * Out of scope for classification.
         *
         * <p>The type is in a test, mock, or generated package.</p>
         */
        OUT_OF_SCOPE,

        /**
         * Technical/infrastructure class.
         *
         * <p>The type is annotated with framework annotations like @Configuration, @Component, etc.</p>
         */
        TECHNICAL,

        /**
         * Criteria evaluated but no clear match.
         *
         * <p>Classification criteria were evaluated but none matched with sufficient confidence.</p>
         */
        AMBIGUOUS,

        /**
         * No conditions matched at all.
         *
         * <p>Default fallback when no other category applies.</p>
         */
        UNKNOWN
    }
}
