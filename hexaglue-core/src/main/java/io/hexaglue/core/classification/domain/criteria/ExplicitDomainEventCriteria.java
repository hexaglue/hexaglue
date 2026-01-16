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

import io.hexaglue.arch.ElementKind;

/**
 * Matches types annotated with @DomainEvent (jMolecules).
 *
 * <p>Priority: 100 (explicit annotation)
 * <p>Confidence: EXPLICIT
 */
public final class ExplicitDomainEventCriteria extends AbstractExplicitAnnotationCriteria {

    public static final String ANNOTATION_SIMPLE_NAME = "DomainEvent";
    public static final String ANNOTATION_QUALIFIED_NAME = "org.jmolecules.event.annotation.DomainEvent";

    public ExplicitDomainEventCriteria() {
        super(ANNOTATION_SIMPLE_NAME, ANNOTATION_QUALIFIED_NAME, ElementKind.DOMAIN_EVENT);
    }
}
