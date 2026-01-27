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

package io.hexaglue.arch.model.audit;

/**
 * Represents a stability principle violation.
 *
 * <p>The Stable Dependencies Principle states that dependencies should flow
 * in the direction of stability. A component should only depend on components
 * more stable than itself.
 *
 * <p>Stability is measured as I = Ce / (Ca + Ce) where:
 * <ul>
 *   <li>Ca = Afferent couplings (incoming dependencies)</li>
 *   <li>Ce = Efferent couplings (outgoing dependencies)</li>
 *   <li>I = 0 (maximally stable) to 1 (maximally unstable)</li>
 * </ul>
 *
 * @param fromType the type causing the violation
 * @param toType the type being depended upon
 * @param fromStability stability of the source type (0.0 to 1.0)
 * @param toStability stability of the target type (0.0 to 1.0)
 * @since 3.0.0
 * @since 5.0.0 - Migrated from io.hexaglue.spi.audit
 */
public record StabilityViolation(String fromType, String toType, double fromStability, double toStability) {

    /**
     * Returns the stability difference (positive = violation).
     */
    public double stabilityDelta() {
        return fromStability - toStability;
    }

    /**
     * Returns true if this is a severe violation (delta > 0.5).
     */
    public boolean isSevere() {
        return stabilityDelta() > 0.5;
    }
}
