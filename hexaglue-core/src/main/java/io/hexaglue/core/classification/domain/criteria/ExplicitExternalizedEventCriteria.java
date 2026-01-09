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

package io.hexaglue.core.classification.domain.criteria;

import io.hexaglue.core.classification.domain.DomainKind;

/**
 * Matches types annotated with @Externalized (jMolecules).
 *
 * <p>Externalized events are domain events intended for publication
 * outside the bounded context, typically through a message broker.
 *
 * <p>Priority: 100 (explicit annotation)
 * <p>Confidence: EXPLICIT
 */
public final class ExplicitExternalizedEventCriteria extends AbstractExplicitAnnotationCriteria {

    public static final String ANNOTATION_SIMPLE_NAME = "Externalized";
    public static final String ANNOTATION_QUALIFIED_NAME = "org.jmolecules.event.annotation.Externalized";

    public ExplicitExternalizedEventCriteria() {
        super(ANNOTATION_SIMPLE_NAME, ANNOTATION_QUALIFIED_NAME, DomainKind.EXTERNALIZED_EVENT);
    }
}
