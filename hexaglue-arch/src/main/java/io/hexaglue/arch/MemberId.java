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

/**
 * Identifier for class members (methods, fields, constructors).
 *
 * <h2>Why a separate ID?</h2>
 * <p>Multiple methods can have the same name (overloading). The signature differentiates them.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Field
 * MemberId statusField = MemberId.field(orderId, "status");
 *
 * // Method with parameters
 * MemberId findMethod = MemberId.method(orderId, "findById", "(String)");
 *
 * // Constructor
 * MemberId ctor = MemberId.constructor(orderId, "(String,List)");
 * }</pre>
 *
 * @param owner the owning type
 * @param memberName the member name (method name, field name, or "&lt;init&gt;" for constructors)
 * @param signature the signature (e.g., "(String,int)" for methods, empty for fields)
 * @since 4.0.0
 */
public record MemberId(ElementId owner, String memberName, String signature) implements Comparable<MemberId> {

    /**
     * Creates a new MemberId.
     *
     * @param owner the owning type, must not be null
     * @param memberName the member name, must not be null
     * @param signature the signature, must not be null (empty for fields)
     * @throws NullPointerException if any argument is null
     */
    public MemberId {
        Objects.requireNonNull(owner, "owner must not be null");
        Objects.requireNonNull(memberName, "memberName must not be null");
        Objects.requireNonNull(signature, "signature must not be null");
    }

    /**
     * Creates a MemberId for a field.
     *
     * @param owner the owning type
     * @param fieldName the field name
     * @return a new MemberId with empty signature
     * @throws NullPointerException if owner or fieldName is null
     */
    public static MemberId field(ElementId owner, String fieldName) {
        return new MemberId(owner, fieldName, "");
    }

    /**
     * Creates a MemberId for a method.
     *
     * @param owner the owning type
     * @param methodName the method name
     * @param signature the method signature (e.g., "(String,int)")
     * @return a new MemberId
     * @throws NullPointerException if any argument is null
     */
    public static MemberId method(ElementId owner, String methodName, String signature) {
        return new MemberId(owner, methodName, signature);
    }

    /**
     * Creates a MemberId for a constructor.
     *
     * <p>Constructors are identified by the special name {@code <init>}.</p>
     *
     * @param owner the owning type
     * @param signature the constructor signature (e.g., "(String,List)")
     * @return a new MemberId
     * @throws NullPointerException if owner or signature is null
     */
    public static MemberId constructor(ElementId owner, String signature) {
        return new MemberId(owner, "<init>", signature);
    }

    @Override
    public int compareTo(MemberId other) {
        int ownerCmp = owner.compareTo(other.owner);
        if (ownerCmp != 0) {
            return ownerCmp;
        }
        int nameCmp = memberName.compareTo(other.memberName);
        return nameCmp != 0 ? nameCmp : signature.compareTo(other.signature);
    }

    @Override
    public String toString() {
        return owner + "#" + memberName + signature;
    }
}
