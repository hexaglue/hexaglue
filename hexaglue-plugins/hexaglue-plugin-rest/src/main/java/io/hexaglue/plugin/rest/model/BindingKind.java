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

package io.hexaglue.plugin.rest.model;

/**
 * Describes how a request DTO field maps back to a domain type parameter.
 *
 * <p>Used by the controller codegen to reconstruct domain types
 * from flat DTO fields when delegating to the driving port.
 *
 * @since 3.1.0
 */
public enum BindingKind {

    /** Field is passed directly to the port method (e.g., String, int, enum). */
    DIRECT,

    /** Field is wrapped via constructor: {@code new CustomerId(request.customerId())}. */
    CONSTRUCTOR_WRAP,

    /** Fields are wrapped via static factory: {@code Money.of(request.amount(), request.currency())}. */
    FACTORY_WRAP,

    /** Field comes from a path variable: {@code new AccountId(id)}. */
    PATH_VARIABLE_WRAP
}
