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

package io.hexaglue.syntax;

import java.util.EnumSet;
import java.util.Set;

/**
 * Capabilities declared by each parser implementation.
 *
 * <p>Avoids surprises: if a capability isn't supported, plugins know BEFORE
 * trying to use it.</p>
 *
 * @param supported the set of supported capabilities
 * @since 4.0.0
 */
public record SyntaxCapabilities(Set<Capability> supported) {

    public SyntaxCapabilities {
        supported = supported != null ? EnumSet.copyOf(supported) : EnumSet.noneOf(Capability.class);
    }

    /**
     * Returns whether this parser supports a given capability.
     *
     * @param cap the capability to check
     * @return true if supported
     */
    public boolean supports(Capability cap) {
        return supported.contains(cap);
    }

    /**
     * Throws an exception if the capability is not supported.
     *
     * @param cap the required capability
     * @param context a description of the context requiring this capability
     * @throws UnsupportedCapabilityException if the capability is not supported
     */
    public void requireOrThrow(Capability cap, String context) {
        if (!supports(cap)) {
            throw new UnsupportedCapabilityException(cap, context);
        }
    }

    /**
     * Parser capabilities.
     */
    public enum Capability {

        /**
         * Source code of method bodies is available.
         */
        METHOD_BODY_SOURCE,

        /**
         * Method invocation graph can be extracted from bodies.
         */
        INVOCATION_GRAPH,

        /**
         * Field accesses can be extracted from method bodies.
         */
        FIELD_ACCESSES,

        /**
         * Full type resolution (vs partial resolution without symbol solver).
         */
        TYPE_RESOLUTION_FULL,

        /**
         * Cyclomatic complexity can be computed.
         */
        CYCLOMATIC_COMPLEXITY,

        /**
         * Comments and Javadoc are available.
         */
        COMMENTS_AND_JAVADOC,

        /**
         * Annotation values including complex nested structures.
         */
        ANNOTATION_VALUES_FULL
    }

    // ===== Factories for known implementations =====

    /**
     * Returns the capabilities of Spoon parser.
     *
     * @return Spoon capabilities
     */
    public static SyntaxCapabilities spoon() {
        return new SyntaxCapabilities(EnumSet.of(
                Capability.METHOD_BODY_SOURCE,
                Capability.INVOCATION_GRAPH,
                Capability.FIELD_ACCESSES,
                Capability.TYPE_RESOLUTION_FULL,
                Capability.CYCLOMATIC_COMPLEXITY,
                Capability.COMMENTS_AND_JAVADOC,
                Capability.ANNOTATION_VALUES_FULL));
    }

    /**
     * Returns the capabilities of Eclipse JDT parser.
     *
     * @return JDT capabilities
     */
    public static SyntaxCapabilities jdt() {
        return new SyntaxCapabilities(EnumSet.of(
                Capability.METHOD_BODY_SOURCE,
                Capability.INVOCATION_GRAPH,
                Capability.TYPE_RESOLUTION_FULL,
                Capability.COMMENTS_AND_JAVADOC,
                Capability.ANNOTATION_VALUES_FULL));
    }

    /**
     * Returns the capabilities of JavaParser.
     *
     * <p>Note: Type resolution is partial without symbol solver.</p>
     *
     * @return JavaParser capabilities
     */
    public static SyntaxCapabilities javaparser() {
        return new SyntaxCapabilities(EnumSet.of(
                Capability.METHOD_BODY_SOURCE, Capability.COMMENTS_AND_JAVADOC, Capability.ANNOTATION_VALUES_FULL));
    }
}
