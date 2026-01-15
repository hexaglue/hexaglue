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

import java.util.Optional;

/**
 * Result of resolving an element reference.
 *
 * <p>This sealed interface provides explicit handling of resolution outcomes,
 * allowing plugins to understand WHY a reference could not be resolved
 * (not found vs type mismatch).</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * ResolutionResult<Aggregate> result = ref.resolve(registry);
 * if (result.isResolved()) {
 *     Aggregate agg = result.value().get();
 * } else {
 *     log.warn("Resolution failed: {}", result.error().get().message());
 * }
 * }</pre>
 *
 * @param <T> the expected type of the resolved element
 * @since 4.0.0
 */
public sealed interface ResolutionResult<T> {

    /**
     * Returns {@code true} if the reference was successfully resolved.
     *
     * @return true if resolved
     */
    boolean isResolved();

    /**
     * Returns the resolved element, if resolution was successful.
     *
     * @return an Optional containing the element, or empty if resolution failed
     */
    Optional<T> value();

    /**
     * Returns the error, if resolution failed.
     *
     * @return an Optional containing the error, or empty if resolution succeeded
     */
    Optional<ResolutionError> error();

    /**
     * Successful resolution containing the resolved element.
     *
     * @param element the resolved element
     * @param <T> the element type
     */
    record Resolved<T>(T element) implements ResolutionResult<T> {
        @Override
        public boolean isResolved() {
            return true;
        }

        @Override
        public Optional<T> value() {
            return Optional.of(element);
        }

        @Override
        public Optional<ResolutionError> error() {
            return Optional.empty();
        }
    }

    /**
     * Failed resolution containing the error information.
     *
     * @param resolutionError the error describing why resolution failed
     * @param <T> the expected element type
     */
    record Failed<T>(ResolutionError resolutionError) implements ResolutionResult<T> {
        @Override
        public boolean isResolved() {
            return false;
        }

        @Override
        public Optional<T> value() {
            return Optional.empty();
        }

        @Override
        public Optional<ResolutionError> error() {
            return Optional.of(resolutionError);
        }
    }

    /**
     * Creates a successful resolution result.
     *
     * @param element the resolved element
     * @param <T> the element type
     * @return a successful resolution result
     */
    static <T> ResolutionResult<T> resolved(T element) {
        return new Resolved<>(element);
    }

    /**
     * Creates a failed resolution result for a missing element.
     *
     * @param id the identifier of the missing element
     * @param <T> the expected element type
     * @return a failed resolution result
     */
    static <T> ResolutionResult<T> notFound(ElementId id) {
        return new Failed<>(new ResolutionError.NotFound(id));
    }

    /**
     * Creates a failed resolution result for a type mismatch.
     *
     * @param id the identifier of the element
     * @param expectedType the type that was expected
     * @param actualType the actual type of the element
     * @param <T> the expected element type
     * @return a failed resolution result
     */
    static <T> ResolutionResult<T> typeMismatch(ElementId id, Class<?> expectedType, Class<?> actualType) {
        return new Failed<>(new ResolutionError.TypeMismatch(id, expectedType, actualType));
    }
}
