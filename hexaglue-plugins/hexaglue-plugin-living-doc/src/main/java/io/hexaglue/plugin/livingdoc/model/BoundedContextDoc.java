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

package io.hexaglue.plugin.livingdoc.model;

import java.util.List;
import java.util.Objects;

/**
 * Documentation model for a bounded context detected from package analysis.
 *
 * <p>A bounded context groups domain types that share a common package prefix,
 * typically the third segment of the package name (e.g., {@code com.example.order}).
 *
 * @param name the bounded context name (e.g., "order")
 * @param rootPackage the root package for this context (e.g., "com.example.order")
 * @param aggregateCount number of aggregate roots in this context
 * @param entityCount number of entities in this context
 * @param valueObjectCount number of value objects in this context
 * @param applicationServiceCount number of application services in this context
 * @param portCount number of ports (driving + driven) in this context
 * @param totalTypeCount total number of architectural types in this context
 * @param typeNames simple names of all types in this context
 * @since 5.0.0
 * @since 5.0.0 - Added applicationServiceCount parameter
 */
public record BoundedContextDoc(
        String name,
        String rootPackage,
        int aggregateCount,
        int entityCount,
        int valueObjectCount,
        int applicationServiceCount,
        int portCount,
        int totalTypeCount,
        List<String> typeNames) {

    /**
     * Compact constructor enforcing non-null constraints and defensive copy.
     */
    public BoundedContextDoc {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(rootPackage, "rootPackage must not be null");
        Objects.requireNonNull(typeNames, "typeNames must not be null");
        typeNames = List.copyOf(typeNames);
    }
}
