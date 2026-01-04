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

package io.hexaglue.core.classification.port.criteria;

import io.hexaglue.core.classification.port.PortDirection;
import io.hexaglue.core.classification.port.PortKind;

/**
 * Matches interfaces annotated with @Repository (jMolecules).
 *
 * <p>Priority: 100 (explicit annotation)
 * <p>Confidence: EXPLICIT
 * <p>Direction: DRIVEN
 */
public final class ExplicitRepositoryCriteria extends AbstractExplicitPortAnnotationCriteria {

    public static final String ANNOTATION_SIMPLE_NAME = "Repository";
    public static final String ANNOTATION_QUALIFIED_NAME = "org.jmolecules.ddd.annotation.Repository";

    public ExplicitRepositoryCriteria() {
        super(ANNOTATION_SIMPLE_NAME, ANNOTATION_QUALIFIED_NAME, PortKind.REPOSITORY, PortDirection.DRIVEN);
    }
}
