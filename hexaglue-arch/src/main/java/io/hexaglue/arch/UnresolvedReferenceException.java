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
 * Exception thrown when an element reference cannot be resolved.
 *
 * <p>This exception is thrown by {@link ElementRef#resolveOrThrow(ElementRegistry)}
 * when the referenced element cannot be found or has the wrong type.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * try {
 *     Aggregate agg = ref.resolveOrThrow(registry);
 * } catch (UnresolvedReferenceException e) {
 *     log.error("Reference {} could not be resolved: {}",
 *         e.getRef().id(), e.getError().message());
 * }
 * }</pre>
 *
 * @since 4.0.0
 */
public class UnresolvedReferenceException extends RuntimeException {

    private final ElementRef<?> ref;
    private final ResolutionError error;

    /**
     * Creates a new exception for an unresolved reference.
     *
     * @param ref the reference that could not be resolved
     * @param error the error describing why resolution failed
     */
    public UnresolvedReferenceException(ElementRef<?> ref, ResolutionError error) {
        super(error.message());
        this.ref = ref;
        this.error = error;
    }

    /**
     * Returns the reference that could not be resolved.
     *
     * @return the unresolved reference
     */
    public ElementRef<?> getRef() {
        return ref;
    }

    /**
     * Returns the error describing why resolution failed.
     *
     * @return the resolution error
     */
    public ResolutionError getError() {
        return error;
    }
}
