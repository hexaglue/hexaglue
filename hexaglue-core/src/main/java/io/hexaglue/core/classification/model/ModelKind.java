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

package io.hexaglue.core.classification.model;

/**
 * Classification kinds for DTOs and data models used in port signatures.
 *
 * <p>Models are types that appear in port method signatures but are not
 * domain objects themselves. They serve as data transfer objects between
 * layers.
 *
 * <p>Classification is based on which type of port uses the model:
 * <ul>
 *   <li>{@link #INBOUND_MODEL} - Used in DRIVING port signatures (input from external)</li>
 *   <li>{@link #OUTBOUND_MODEL} - Used in DRIVEN port signatures (output to external)</li>
 * </ul>
 */
public enum ModelKind {

    /**
     * Inbound model - a DTO used in DRIVING port method signatures.
     *
     * <p>Inbound models carry data from external actors (REST controllers,
     * message listeners) into the application core. They typically represent:
     * <ul>
     *   <li>Command objects (e.g., CreateOrderCommand)</li>
     *   <li>Query parameters (e.g., OrderSearchCriteria)</li>
     *   <li>Request DTOs (e.g., OrderRequest)</li>
     * </ul>
     *
     * <p>Detection: Type appears in parameter position of DRIVING port methods
     * and is not a domain type.
     */
    INBOUND_MODEL,

    /**
     * Outbound model - a DTO used in DRIVEN port method signatures.
     *
     * <p>Outbound models carry data from the application core to external systems
     * (databases, external APIs). They typically represent:
     * <ul>
     *   <li>Persistence DTOs (e.g., OrderEntity)</li>
     *   <li>External API payloads (e.g., PaymentRequest)</li>
     *   <li>Event payloads for message brokers</li>
     * </ul>
     *
     * <p>Detection: Type appears in DRIVEN port method signatures (parameters or return)
     * and is not a domain type.
     */
    OUTBOUND_MODEL
}
