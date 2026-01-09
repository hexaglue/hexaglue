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

/**
 * The kind of Java type construct.
 *
 * <p>This enum represents the fundamental structural classification of a Java type,
 * independent of its domain or architectural role. It answers the question:
 * "What Java language construct is this?"
 *
 * @since 3.0.0
 */
public enum TypeKind {

    /**
     * A regular Java class.
     * Supports inheritance, mutability, and full OOP capabilities.
     */
    CLASS,

    /**
     * A Java interface.
     * Defines contracts without implementation (except default methods).
     */
    INTERFACE,

    /**
     * A Java enum.
     * Represents a fixed set of named constants.
     */
    ENUM,

    /**
     * A Java record (Java 16+).
     * Immutable data carrier with generated equals/hashCode/toString.
     */
    RECORD,

    /**
     * A Java annotation type.
     * Used to provide metadata about code elements.
     */
    ANNOTATION
}
