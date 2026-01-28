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

package io.hexaglue.plugin.audit.domain.model.report;

import java.util.List;
import java.util.Objects;

/**
 * Details about a domain service component.
 *
 * <p>A domain service is a stateless operation that performs domain logic that
 * doesn't naturally fit within an entity or value object. Domain services
 * encapsulate domain logic that involves multiple entities or requires
 * coordination between domain objects.
 *
 * @param name simple name of the domain service
 * @param packageName fully qualified package name
 * @param methods number of business methods
 * @param usedAggregates aggregates used by this service
 * @since 5.0.0
 */
public record DomainServiceComponent(String name, String packageName, int methods, List<String> usedAggregates) {

    /**
     * Creates a domain service component with validation.
     */
    public DomainServiceComponent {
        Objects.requireNonNull(name, "name is required");
        Objects.requireNonNull(packageName, "packageName is required");
        usedAggregates = usedAggregates != null ? List.copyOf(usedAggregates) : List.of();
    }

    /**
     * Creates a domain service component with all fields.
     *
     * @param name simple name
     * @param packageName package name
     * @param methods number of methods
     * @param usedAggregates aggregates used
     * @return the domain service component
     */
    public static DomainServiceComponent of(String name, String packageName, int methods, List<String> usedAggregates) {
        return new DomainServiceComponent(name, packageName, methods, usedAggregates);
    }

    /**
     * Creates a domain service component without aggregate info.
     *
     * @param name simple name
     * @param packageName package name
     * @param methods number of methods
     * @return the domain service component
     */
    public static DomainServiceComponent of(String name, String packageName, int methods) {
        return new DomainServiceComponent(name, packageName, methods, List.of());
    }
}
