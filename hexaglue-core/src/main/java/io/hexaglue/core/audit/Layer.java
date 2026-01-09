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

package io.hexaglue.core.audit;

import io.hexaglue.spi.audit.LayerClassification;

/**
 * Internal enum representing architectural layers with classification rules.
 *
 * <p>This enum extends the SPI {@link LayerClassification} with additional
 * classification logic based on package naming conventions, type name suffixes,
 * and annotations. It is used by {@link LayerClassifier} to determine the
 * architectural layer of a type.
 *
 * @since 3.0.0
 */
enum Layer {

    /**
     * Presentation layer (user interfaces and APIs).
     */
    PRESENTATION(LayerClassification.PRESENTATION),

    /**
     * Application layer (use cases and orchestration).
     */
    APPLICATION(LayerClassification.APPLICATION),

    /**
     * Domain layer (core business logic).
     */
    DOMAIN(LayerClassification.DOMAIN),

    /**
     * Infrastructure layer (technical implementations).
     */
    INFRASTRUCTURE(LayerClassification.INFRASTRUCTURE),

    /**
     * Layer could not be determined.
     */
    UNKNOWN(LayerClassification.UNKNOWN);

    private final LayerClassification spiClassification;

    Layer(LayerClassification spiClassification) {
        this.spiClassification = spiClassification;
    }

    /**
     * Converts this internal layer to the SPI LayerClassification.
     *
     * @return the corresponding SPI layer classification
     */
    public LayerClassification toSpiClassification() {
        return spiClassification;
    }
}
