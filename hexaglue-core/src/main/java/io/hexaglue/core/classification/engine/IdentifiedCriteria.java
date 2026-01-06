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

package io.hexaglue.core.classification.engine;

/**
 * Interface for criteria with stable identifiers.
 *
 * <p>Criteria implementing this interface provide a stable ID that can be
 * used for configuration purposes (e.g., in YAML profiles) independently
 * of the class name.
 *
 * <p>Convention for IDs: {@code {target}.{category}.{name}}
 * <ul>
 *   <li>target: "domain" or "port"</li>
 *   <li>category: "explicit", "semantic", "structural", "naming"</li>
 *   <li>name: specific criteria name (camelCase)</li>
 * </ul>
 *
 * <p>Examples:
 * <ul>
 *   <li>{@code domain.explicit.aggregateRoot}</li>
 *   <li>{@code domain.semantic.saga}</li>
 *   <li>{@code port.naming.repository}</li>
 * </ul>
 */
public interface IdentifiedCriteria {

    /**
     * Returns the stable identifier for this criteria.
     *
     * <p>This ID should remain stable across refactoring and is used
     * for configuration purposes.
     *
     * @return the stable identifier
     */
    String id();
}
