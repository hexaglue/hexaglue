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

import java.util.Objects;
import java.util.Optional;

/**
 * Details about a domain event component.
 *
 * <p>A domain event represents something that happened in the domain that domain
 * experts care about. Events are named in past tense and are typically immutable
 * records of what occurred.
 *
 * @param name simple name of the domain event
 * @param packageName fully qualified package name
 * @param fields number of fields
 * @param publishedBy name of the aggregate that publishes this event (if known)
 * @since 5.0.0
 */
public record DomainEventComponent(String name, String packageName, int fields, String publishedBy) {

    /**
     * Creates a domain event component with validation.
     */
    public DomainEventComponent {
        Objects.requireNonNull(name, "name is required");
        Objects.requireNonNull(packageName, "packageName is required");
    }

    /**
     * Returns the publisher as optional.
     *
     * @return optional publisher name
     */
    public Optional<String> publishedByOpt() {
        return Optional.ofNullable(publishedBy);
    }

    /**
     * Creates a domain event component with all fields.
     *
     * @param name simple name
     * @param packageName package name
     * @param fields number of fields
     * @param publishedBy publishing aggregate name (may be null)
     * @return the domain event component
     */
    public static DomainEventComponent of(String name, String packageName, int fields, String publishedBy) {
        return new DomainEventComponent(name, packageName, fields, publishedBy);
    }

    /**
     * Creates a domain event component without publisher info.
     *
     * @param name simple name
     * @param packageName package name
     * @param fields number of fields
     * @return the domain event component
     */
    public static DomainEventComponent of(String name, String packageName, int fields) {
        return new DomainEventComponent(name, packageName, fields, null);
    }
}
