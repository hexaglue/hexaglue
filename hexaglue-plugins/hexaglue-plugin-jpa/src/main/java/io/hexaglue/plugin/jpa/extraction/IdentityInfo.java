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

package io.hexaglue.plugin.jpa.extraction;

import io.hexaglue.syntax.TypeRef;
import java.util.Objects;
import java.util.Optional;

/**
 * JPA identity information extracted from field annotations.
 *
 * <p>Captures identity metadata including generation strategy and whether
 * the identity is embedded (composite key) or simple.</p>
 *
 * @param fieldName the identity field name
 * @param idType the identity type reference
 * @param strategy the generation strategy (null if not generated)
 * @param embedded whether this is an embedded ID (composite key)
 * @param wrappedType the wrapped primitive type for value object IDs (e.g., Long inside OrderId)
 * @since 4.0.0
 */
public record IdentityInfo(
        String fieldName, TypeRef idType, GenerationStrategy strategy, boolean embedded, TypeRef wrappedType) {

    /**
     * JPA ID generation strategies.
     */
    public enum GenerationStrategy {
        /** No automatic generation - application provides ID */
        NONE,
        /** Database auto-increment */
        IDENTITY,
        /** Database sequence */
        SEQUENCE,
        /** JPA-managed table */
        TABLE,
        /** Provider-specific strategy */
        AUTO,
        /** UUID generation */
        UUID
    }

    /**
     * Compact constructor with validation.
     */
    public IdentityInfo {
        Objects.requireNonNull(fieldName, "fieldName must not be null");
        Objects.requireNonNull(idType, "idType must not be null");
    }

    /**
     * Returns whether this identity uses automatic generation.
     *
     * @return true if a generation strategy is defined
     */
    public boolean isGenerated() {
        return strategy != null && strategy != GenerationStrategy.NONE;
    }

    /**
     * Returns the generation strategy as an Optional.
     *
     * @return the generation strategy, or empty if not generated
     */
    public Optional<GenerationStrategy> strategyOpt() {
        return Optional.ofNullable(strategy);
    }

    /**
     * Returns the wrapped type as an Optional.
     *
     * @return the wrapped type for value object IDs, or empty if primitive
     */
    public Optional<TypeRef> wrappedTypeOpt() {
        return Optional.ofNullable(wrappedType);
    }

    /**
     * Returns whether this identity wraps a primitive type.
     *
     * <p>Value object identities (e.g., OrderId wrapping Long) have a wrapped type.</p>
     *
     * @return true if wrappedType is present
     */
    public boolean isWrapped() {
        return wrappedType != null;
    }

    /**
     * Returns the actual ID type for JPA mapping.
     *
     * <p>For wrapped IDs, returns the wrapped type; otherwise returns the ID type itself.</p>
     *
     * @return the type to use in JPA @Id annotation
     */
    public TypeRef jpaIdType() {
        return wrappedType != null ? wrappedType : idType;
    }

    // ===== Factory methods =====

    /**
     * Creates a simple auto-generated identity.
     *
     * @param fieldName the field name
     * @param idType the identity type (typically Long)
     * @return a new IdentityInfo with IDENTITY strategy
     */
    public static IdentityInfo simpleAutoId(String fieldName, TypeRef idType) {
        return new IdentityInfo(fieldName, idType, GenerationStrategy.IDENTITY, false, null);
    }

    /**
     * Creates a simple identity without generation.
     *
     * @param fieldName the field name
     * @param idType the identity type
     * @return a new IdentityInfo with no generation strategy
     */
    public static IdentityInfo simpleId(String fieldName, TypeRef idType) {
        return new IdentityInfo(fieldName, idType, null, false, null);
    }

    /**
     * Creates an embedded identity (composite key).
     *
     * @param fieldName the field name
     * @param embeddedType the embeddable ID class type
     * @return a new IdentityInfo marked as embedded
     */
    public static IdentityInfo embeddedId(String fieldName, TypeRef embeddedType) {
        return new IdentityInfo(fieldName, embeddedType, null, true, null);
    }

    /**
     * Creates a wrapped value object identity.
     *
     * @param fieldName the field name
     * @param valueObjectType the value object type (e.g., OrderId)
     * @param wrappedType the wrapped primitive type (e.g., Long)
     * @param strategy the generation strategy for the wrapped type
     * @return a new IdentityInfo with wrapped type info
     */
    public static IdentityInfo wrappedId(
            String fieldName, TypeRef valueObjectType, TypeRef wrappedType, GenerationStrategy strategy) {
        return new IdentityInfo(fieldName, valueObjectType, strategy, false, wrappedType);
    }
}
