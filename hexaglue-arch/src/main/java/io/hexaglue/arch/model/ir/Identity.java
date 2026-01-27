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

package io.hexaglue.arch.model.ir;

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
 * @param accessorMethodName the method name to access the unwrapped value (e.g., "value" for records,
 *                           "getValue" for classes, null if not wrapped)
 * @since 3.0.0
 * @since 5.0.0 - Migrated from io.hexaglue.spi.ir
 */
public record Identity(
        String fieldName,
        TypeRef type,
        TypeRef unwrappedType,
        IdentityStrategy strategy,
        IdentityWrapperKind wrapperKind,
        String accessorMethodName) {

    /**
     * Returns true if the identity is wrapped in a custom type.
     */
    public boolean isWrapped() {
        return wrapperKind != IdentityWrapperKind.NONE;
    }

    /**
     * Creates an Identity for an unwrapped (primitive/simple) identifier.
     *
     * @param fieldName the field name
     * @param type the type reference
     * @param strategy the generation strategy
     * @return a new Identity with NONE wrapper kind
     * @since 3.0.0
     */
    public static Identity unwrapped(String fieldName, TypeRef type, IdentityStrategy strategy) {
        return new Identity(fieldName, type, type, strategy, IdentityWrapperKind.NONE, null);
    }

    /**
     * Creates an Identity for a wrapped identifier (record or class).
     *
     * @param fieldName the field name
     * @param type the wrapper type reference
     * @param unwrappedType the underlying type reference
     * @param strategy the generation strategy
     * @param wrapperKind the wrapper kind (RECORD or CLASS)
     * @param accessorMethodName the method to access the unwrapped value
     * @return a new Identity
     * @since 3.0.0
     */
    public static Identity wrapped(
            String fieldName,
            TypeRef type,
            TypeRef unwrappedType,
            IdentityStrategy strategy,
            IdentityWrapperKind wrapperKind,
            String accessorMethodName) {
        return new Identity(fieldName, type, unwrappedType, strategy, wrapperKind, accessorMethodName);
    }
}
