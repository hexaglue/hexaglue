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

package io.hexaglue.core.classification;

/**
 * Type of evidence supporting a classification.
 *
 * <p>Used to categorize and explain why a type was classified
 * in a particular way.
 */
public enum EvidenceType {

    /**
     * Evidence from an annotation (e.g., @AggregateRoot, @Entity).
     */
    ANNOTATION,

    /**
     * Evidence from naming patterns (e.g., suffix "Repository", "UseCase").
     */
    NAMING,

    /**
     * Evidence from type structure (e.g., has identity field, is immutable).
     */
    STRUCTURE,

    /**
     * Evidence from relationships (e.g., used in Repository signature).
     */
    RELATIONSHIP,

    /**
     * Evidence from package location (e.g., in ".ports.in" package).
     */
    PACKAGE
}
