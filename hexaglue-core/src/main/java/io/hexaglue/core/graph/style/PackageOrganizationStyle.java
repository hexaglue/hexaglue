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

package io.hexaglue.core.graph.style;

/**
 * Represents the detected package organization style of a codebase.
 *
 * <p>HexaGlue detects the organization style to adapt classification criteria
 * and provide more accurate type classifications based on package conventions.
 *
 * <p>Each style has characteristic package patterns that help identify it:
 * <ul>
 *   <li>{@link #HEXAGONAL}: {@code *.ports.in}, {@code *.ports.out}, {@code *.adapters.*}</li>
 *   <li>{@link #BY_LAYER}: {@code *.domain}, {@code *.application}, {@code *.infrastructure}</li>
 *   <li>{@link #BY_FEATURE}: {@code *.order.domain}, {@code *.payment.ports.*}</li>
 *   <li>{@link #CLEAN_ARCHITECTURE}: {@code *.usecases}, {@code *.gateways}, {@code *.entities}</li>
 *   <li>{@link #ONION}: {@code *.core}, {@code *.domain.services}</li>
 * </ul>
 */
public enum PackageOrganizationStyle {

    /**
     * Hexagonal Architecture / Ports and Adapters.
     *
     * <p>Characteristic patterns:
     * <ul>
     *   <li>{@code *.ports.in.*} - Driving/Primary ports</li>
     *   <li>{@code *.ports.out.*} - Driven/Secondary ports</li>
     *   <li>{@code *.adapters.*} - Infrastructure adapters</li>
     *   <li>{@code *.in.*}, {@code *.out.*} - Simplified variants</li>
     * </ul>
     *
     * <p>Example: {@code com.app.order.ports.in.OrderingCoffee}
     */
    HEXAGONAL,

    /**
     * Layer-based organization (traditional layered architecture).
     *
     * <p>Characteristic patterns:
     * <ul>
     *   <li>{@code *.domain.*} - Domain/Business logic layer</li>
     *   <li>{@code *.application.*} - Application/Service layer</li>
     *   <li>{@code *.infrastructure.*} - Infrastructure layer</li>
     *   <li>{@code *.presentation.*} - Presentation/UI layer</li>
     * </ul>
     *
     * <p>Example: {@code com.app.domain.Order}
     */
    BY_LAYER,

    /**
     * Feature-based organization (vertical slices).
     *
     * <p>Characteristic patterns:
     * <ul>
     *   <li>{@code *.{feature}.domain.*} - Feature's domain types</li>
     *   <li>{@code *.{feature}.ports.*} - Feature's ports</li>
     *   <li>{@code *.{feature}.service.*} - Feature's services</li>
     * </ul>
     *
     * <p>Example: {@code com.app.order.domain.Order}, {@code com.app.payment.service.PaymentService}
     */
    BY_FEATURE,

    /**
     * Clean Architecture style (Uncle Bob).
     *
     * <p>Characteristic patterns:
     * <ul>
     *   <li>{@code *.entities.*} - Enterprise business rules</li>
     *   <li>{@code *.usecases.*} - Application business rules</li>
     *   <li>{@code *.gateways.*} - Interface adapters (outbound)</li>
     *   <li>{@code *.controllers.*} - Interface adapters (inbound)</li>
     *   <li>{@code *.presenters.*} - Interface adapters (presentation)</li>
     * </ul>
     *
     * <p>Example: {@code com.app.usecases.PlaceOrder}
     */
    CLEAN_ARCHITECTURE,

    /**
     * Onion Architecture style.
     *
     * <p>Characteristic patterns:
     * <ul>
     *   <li>{@code *.core.*} - Core domain</li>
     *   <li>{@code *.domain.services.*} - Domain services</li>
     *   <li>{@code *.application.*} - Application services</li>
     *   <li>{@code *.infrastructure.*} - External concerns</li>
     * </ul>
     *
     * <p>Example: {@code com.app.core.Order}
     */
    ONION,

    /**
     * Style not detected or mixed styles.
     *
     * <p>This is the default when:
     * <ul>
     *   <li>No recognizable patterns are found</li>
     *   <li>Multiple conflicting styles are detected</li>
     *   <li>Too few types to make a confident determination</li>
     * </ul>
     */
    UNKNOWN;

    /**
     * Returns true if this style supports inbound/outbound port distinction via packages.
     */
    public boolean supportsPortDirection() {
        return this == HEXAGONAL || this == CLEAN_ARCHITECTURE;
    }

    /**
     * Returns true if this is a known (non-UNKNOWN) style.
     */
    public boolean isKnown() {
        return this != UNKNOWN;
    }
}
