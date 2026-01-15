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

import java.util.Objects;
import java.util.Optional;

/**
 * Type-safe reference to an architectural element.
 *
 * <h2>Design Rationale (v2.1)</h2>
 * <p>This reference includes {@code Class<T>} for verified resolution at runtime.
 * No unchecked casts - errors are deterministic and clear.</p>
 *
 * <h2>Benefits</h2>
 * <ul>
 *   <li><strong>Type-safe</strong>: {@code ElementRef<Aggregate>} cannot be confused with
 *       {@code ElementRef<DrivenPort>}</li>
 *   <li><strong>Verified</strong>: Resolution validates the expected type</li>
 *   <li><strong>Resolvable</strong>: {@code resolve(registry)} returns {@code ResolutionResult<T>}</li>
 *   <li><strong>Serializable</strong>: Serializes as (qualifiedName, expectedType)</li>
 *   <li><strong>Explicit</strong>: Code clearly shows this is a reference</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Create a reference
 * ElementRef<Aggregate> ref = ElementRef.of(elementId, Aggregate.class);
 *
 * // Resolve with explicit result handling
 * ResolutionResult<Aggregate> result = ref.resolve(registry);
 * if (result.isResolved()) {
 *     Aggregate agg = result.value().get();
 * }
 *
 * // Or resolve with Optional
 * Optional<Aggregate> agg = ref.resolveOpt(registry);
 *
 * // Or throw if not found
 * Aggregate agg = ref.resolveOrThrow(registry);
 * }</pre>
 *
 * @param id the element identifier
 * @param expectedType the expected element type
 * @param <T> the type of the referenced element
 * @since 4.0.0
 */
public record ElementRef<T extends ArchElement>(ElementId id, Class<T> expectedType) {

    /**
     * Creates a new ElementRef.
     *
     * @param id the element identifier, must not be null
     * @param expectedType the expected element type, must not be null
     * @throws NullPointerException if id or expectedType is null
     */
    public ElementRef {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(expectedType, "expectedType must not be null");
    }

    /**
     * Creates a type-safe reference to an element.
     *
     * @param id the element identifier
     * @param type the expected element type
     * @param <T> the element type
     * @return a new ElementRef
     * @throws NullPointerException if id or type is null
     */
    public static <T extends ArchElement> ElementRef<T> of(ElementId id, Class<T> type) {
        return new ElementRef<>(id, type);
    }

    /**
     * Creates a type-safe reference from a qualified name.
     *
     * @param qualifiedName the fully qualified name of the element
     * @param type the expected element type
     * @param <T> the element type
     * @return a new ElementRef
     * @throws NullPointerException if qualifiedName or type is null
     */
    public static <T extends ArchElement> ElementRef<T> of(String qualifiedName, Class<T> type) {
        return new ElementRef<>(ElementId.of(qualifiedName), type);
    }

    // ===== Resolution methods (v2.1 - verified) =====

    /**
     * Resolves this reference against a registry with explicit result.
     *
     * @param registry the registry to resolve against
     * @return a ResolutionResult containing the element or an error
     */
    public ResolutionResult<T> resolve(ElementRegistry registry) {
        Optional<ArchElement> found = registry.get(id);

        if (found.isEmpty()) {
            return ResolutionResult.notFound(id);
        }

        ArchElement element = found.get();
        if (!expectedType.isInstance(element)) {
            return ResolutionResult.typeMismatch(id, expectedType, element.getClass());
        }

        return ResolutionResult.resolved(expectedType.cast(element));
    }

    /**
     * Resolves this reference returning an Optional.
     *
     * @param registry the registry to resolve against
     * @return an Optional containing the element, or empty if resolution failed
     */
    public Optional<T> resolveOpt(ElementRegistry registry) {
        return resolve(registry).value();
    }

    /**
     * Resolves this reference or throws an exception.
     *
     * @param registry the registry to resolve against
     * @return the resolved element
     * @throws UnresolvedReferenceException if the reference cannot be resolved
     */
    public T resolveOrThrow(ElementRegistry registry) {
        ResolutionResult<T> result = resolve(registry);
        if (result.isResolved()) {
            return result.value().orElseThrow();
        }
        throw new UnresolvedReferenceException(this, result.error().orElseThrow());
    }

    // ===== Convenience methods =====

    /**
     * Returns the qualified name of the referenced element.
     *
     * <p>Shortcut for {@code id().qualifiedName()}.</p>
     *
     * @return the qualified name
     */
    public String qualifiedName() {
        return id.qualifiedName();
    }

    /**
     * Returns the simple name of the referenced element.
     *
     * <p>Shortcut for {@code id().simpleName()}.</p>
     *
     * @return the simple name
     */
    public String simpleName() {
        return id.simpleName();
    }
}
