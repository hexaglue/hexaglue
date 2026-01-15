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

/**
 * Error types for element reference resolution.
 *
 * <p>This sealed interface provides diagnostic information when a reference
 * cannot be resolved. Plugins can use this to generate meaningful error
 * messages or documentation about unresolved references.</p>
 *
 * <h2>Error Types</h2>
 * <ul>
 *   <li>{@link NotFound} - The referenced element does not exist in the registry</li>
 *   <li>{@link TypeMismatch} - The element exists but has a different type than expected</li>
 * </ul>
 *
 * @since 4.0.0
 */
public sealed interface ResolutionError {

    /**
     * Returns the identifier of the element that could not be resolved.
     *
     * @return the element identifier
     */
    ElementId id();

    /**
     * Returns a human-readable error message.
     *
     * @return the error message
     */
    String message();

    /**
     * Error when the referenced element does not exist in the registry.
     *
     * @param id the identifier of the missing element
     */
    record NotFound(ElementId id) implements ResolutionError {
        @Override
        public String message() {
            return "Element not found: " + id;
        }
    }

    /**
     * Error when the referenced element exists but has a different type than expected.
     *
     * @param id the identifier of the element
     * @param expectedType the type that was expected
     * @param actualType the actual type of the element
     */
    record TypeMismatch(ElementId id, Class<?> expectedType, Class<?> actualType) implements ResolutionError {

        @Override
        public String message() {
            return "Type mismatch for " + id + ": expected "
                    + expectedType.getSimpleName() + " but found "
                    + actualType.getSimpleName();
        }
    }
}
