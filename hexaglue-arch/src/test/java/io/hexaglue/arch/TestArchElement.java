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
 * Test stub for ArchElement used in unit tests.
 */
record TestArchElement(ElementId id, ElementKind kind, ClassificationTrace classificationTrace)
        implements ArchElement.Marker {

    TestArchElement(ElementId id, ElementKind kind) {
        this(id, kind, ClassificationTrace.highConfidence(kind, "test-criterion", "Test classification"));
    }

    static TestArchElement aggregate(String qualifiedName) {
        return new TestArchElement(ElementId.of(qualifiedName), ElementKind.AGGREGATE);
    }

    static TestArchElement entity(String qualifiedName) {
        return new TestArchElement(ElementId.of(qualifiedName), ElementKind.ENTITY);
    }

    static TestArchElement valueObject(String qualifiedName) {
        return new TestArchElement(ElementId.of(qualifiedName), ElementKind.VALUE_OBJECT);
    }

    static TestArchElement drivenPort(String qualifiedName) {
        return new TestArchElement(ElementId.of(qualifiedName), ElementKind.DRIVEN_PORT);
    }

    static TestArchElement drivingPort(String qualifiedName) {
        return new TestArchElement(ElementId.of(qualifiedName), ElementKind.DRIVING_PORT);
    }
}
