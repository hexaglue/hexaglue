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
import java.util.Optional;

/**
 * Details about an entity component in the domain model.
 *
 * <p>An entity is an object with a distinct identity that runs through time
 * and different states. Unlike value objects, entities are distinguished by
 * their identity, not their attributes.
 *
 * @param name simple name of the entity
 * @param packageName fully qualified package name
 * @param fields number of fields
 * @param identityField name of the identity field (if detected)
 * @param aggregateRoot name of the owning aggregate root (if known)
 * @param fieldDetails detailed information about each field for diagram rendering
 * @since 5.0.0
 */
public record EntityComponent(
        String name,
        String packageName,
        int fields,
        String identityField,
        String aggregateRoot,
        List<FieldDetail> fieldDetails) {

    /**
     * Creates an entity component with validation.
     */
    public EntityComponent {
        Objects.requireNonNull(name, "name is required");
        Objects.requireNonNull(packageName, "packageName is required");
        fieldDetails = fieldDetails != null ? List.copyOf(fieldDetails) : List.of();
    }

    /**
     * Returns the identity field as optional.
     *
     * @return optional identity field name
     */
    public Optional<String> identityFieldOpt() {
        return Optional.ofNullable(identityField);
    }

    /**
     * Returns the aggregate root as optional.
     *
     * @return optional aggregate root name
     */
    public Optional<String> aggregateRootOpt() {
        return Optional.ofNullable(aggregateRoot);
    }

    /**
     * Creates an entity component with all fields.
     *
     * @param name simple name
     * @param packageName package name
     * @param fields number of fields
     * @param identityField identity field name (may be null)
     * @param aggregateRoot owning aggregate root name (may be null)
     * @return the entity component
     */
    public static EntityComponent of(
            String name, String packageName, int fields, String identityField, String aggregateRoot) {
        return new EntityComponent(name, packageName, fields, identityField, aggregateRoot, List.of());
    }

    /**
     * Creates an entity component with field details.
     *
     * @param name simple name
     * @param packageName package name
     * @param fields number of fields
     * @param identityField identity field name (may be null)
     * @param aggregateRoot owning aggregate root name (may be null)
     * @param fieldDetails field details for rendering
     * @return the entity component
     */
    public static EntityComponent of(
            String name,
            String packageName,
            int fields,
            String identityField,
            String aggregateRoot,
            List<FieldDetail> fieldDetails) {
        return new EntityComponent(name, packageName, fields, identityField, aggregateRoot, fieldDetails);
    }

    /**
     * Creates an entity component without identity or aggregate info.
     *
     * @param name simple name
     * @param packageName package name
     * @param fields number of fields
     * @return the entity component
     */
    public static EntityComponent of(String name, String packageName, int fields) {
        return new EntityComponent(name, packageName, fields, null, null, List.of());
    }
}
