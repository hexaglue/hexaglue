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

package io.hexaglue.spi.core;

import java.util.Objects;

/**
 * Basic information about a Java type.
 *
 * <p>This record provides the fundamental identification and classification of a type,
 * independent of its domain or architectural role. It serves as a foundation for
 * more specialized type representations in the SPI.
 *
 * <p>Immutability guarantee: All fields are final and non-null.
 *
 * @param qualifiedName the fully qualified class name (e.g., "com.example.Order")
 * @param simpleName    the simple class name (e.g., "Order")
 * @param packageName   the package name (e.g., "com.example")
 * @param kind          the Java construct kind (CLASS, INTERFACE, ENUM, RECORD, ANNOTATION)
 * @since 3.0.0
 */
public record TypeInfo(String qualifiedName, String simpleName, String packageName, TypeKind kind) {

    /**
     * Compact constructor with validation.
     *
     * @throws NullPointerException if any parameter is null
     */
    public TypeInfo {
        Objects.requireNonNull(qualifiedName, "qualifiedName required");
        Objects.requireNonNull(simpleName, "simpleName required");
        Objects.requireNonNull(packageName, "packageName required");
        Objects.requireNonNull(kind, "kind required");
    }

    /**
     * Returns true if this type is a class.
     *
     * @return true if kind is CLASS
     */
    public boolean isClass() {
        return kind == TypeKind.CLASS;
    }

    /**
     * Returns true if this type is an interface.
     *
     * @return true if kind is INTERFACE
     */
    public boolean isInterface() {
        return kind == TypeKind.INTERFACE;
    }

    /**
     * Returns true if this type is a record.
     *
     * @return true if kind is RECORD
     */
    public boolean isRecord() {
        return kind == TypeKind.RECORD;
    }

    /**
     * Returns true if this type is an enum.
     *
     * @return true if kind is ENUM
     */
    public boolean isEnum() {
        return kind == TypeKind.ENUM;
    }

    /**
     * Returns true if this type is an annotation.
     *
     * @return true if kind is ANNOTATION
     */
    public boolean isAnnotation() {
        return kind == TypeKind.ANNOTATION;
    }
}
