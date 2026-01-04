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

package io.hexaglue.spi.ir;

/**
 * Identity information for an entity or aggregate root.
 *
 * <p>The identity represents the unique identifier of a domain type. It can be:
 * <ul>
 *   <li>A wrapped type: {@code record OrderId(UUID value) {}} - provides type safety</li>
 *   <li>An unwrapped type: {@code private UUID id;} - direct primitive/wrapper usage</li>
 * </ul>
 *
 * @param fieldName the name of the identity field (e.g., "id", "orderId")
 * @param type the declared type reference (e.g., TypeRef("com.example.OrderId"))
 * @param unwrappedType the underlying type if wrapped (e.g., TypeRef("java.util.UUID"))
 * @param strategy the identity generation strategy
 * @param wrapperKind the kind of wrapper (RECORD, CLASS, or NONE)
 */
public record Identity(
        String fieldName,
        TypeRef type,
        TypeRef unwrappedType,
        IdentityStrategy strategy,
        IdentityWrapperKind wrapperKind) {

    /**
     * Returns true if the identity is wrapped in a custom type.
     */
    public boolean isWrapped() {
        return wrapperKind != IdentityWrapperKind.NONE;
    }

    /**
     * Returns the qualified name of the declared type.
     *
     * @deprecated Use {@link #type()} for full type information.
     */
    @Deprecated
    public String typeName() {
        return type.qualifiedName();
    }

    /**
     * Returns the qualified name of the underlying type.
     *
     * @deprecated Use {@link #unwrappedType()} for full type information.
     */
    @Deprecated
    public String unwrappedTypeName() {
        return unwrappedType.qualifiedName();
    }

    /**
     * Creates an Identity with backward-compatible string parameters.
     *
     * @deprecated Use the full constructor with TypeRef parameters.
     */
    @Deprecated
    public static Identity of(String fieldName, String typeName, String unwrappedTypeName, IdentityStrategy strategy) {
        TypeRef type = TypeRef.of(typeName);
        TypeRef unwrapped = TypeRef.of(unwrappedTypeName);
        IdentityWrapperKind wrapperKind =
                typeName.equals(unwrappedTypeName) ? IdentityWrapperKind.NONE : IdentityWrapperKind.RECORD;
        return new Identity(fieldName, type, unwrapped, strategy, wrapperKind);
    }
}
