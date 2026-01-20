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

package io.hexaglue.arch.model;

import io.hexaglue.arch.ClassificationTrace;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents an entity in the domain model.
 *
 * <p>An entity is an object that has a distinct identity that runs through time
 * and different states. Unlike value objects, entities are distinguished by
 * their identity, not their attributes.</p>
 *
 * <h2>Characteristics</h2>
 * <ul>
 *   <li>Has identity - uniquely identified by an identity field</li>
 *   <li>Has lifecycle - exists through time with state changes</li>
 *   <li>Mutable - attributes can change while identity remains</li>
 * </ul>
 *
 * <h2>Identity Field</h2>
 * <p>The {@link #identityField()} returns the field that uniquely identifies
 * this entity, if detected. Not all entities have an explicit identity field
 * (they might inherit it from a base class).</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Entity with identity field
 * Field idField = Field.builder("id", TypeRef.of("com.example.OrderLineId"))
 *     .roles(Set.of(FieldRole.IDENTITY))
 *     .build();
 * Entity entity = Entity.of(
 *     TypeId.of("com.example.OrderLine"),
 *     structure,
 *     trace,
 *     idField
 * );
 *
 * // Entity without detected identity
 * Entity baseEntity = Entity.of(typeId, structure, trace);
 * }</pre>
 *
 * @param id the unique identifier for this type
 * @param structure the structural description of this type
 * @param classification the classification trace explaining why this type was classified
 * @param identityField the field that identifies this entity (if detected)
 * @since 4.1.0
 */
public record Entity(
        TypeId id, TypeStructure structure, ClassificationTrace classification, Optional<Field> identityField)
        implements DomainType {

    /**
     * Creates a new Entity.
     *
     * @param id the type id, must not be null
     * @param structure the type structure, must not be null
     * @param classification the classification trace, must not be null
     * @param identityField the identity field, must not be null (use Optional.empty() for none)
     * @throws NullPointerException if id, structure, classification, or identityField is null
     */
    public Entity {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(structure, "structure must not be null");
        Objects.requireNonNull(classification, "classification must not be null");
        Objects.requireNonNull(identityField, "identityField must not be null");
    }

    @Override
    public ArchKind kind() {
        return ArchKind.ENTITY;
    }

    /**
     * Returns whether this entity has an identity field.
     *
     * @return true if an identity field is present
     */
    public boolean hasIdentity() {
        return identityField.isPresent();
    }

    /**
     * Creates an Entity with the given parameters and an identity field.
     *
     * @param id the type id
     * @param structure the type structure
     * @param classification the classification trace
     * @param identityField the identity field (may be null)
     * @return a new Entity
     * @throws NullPointerException if id, structure, or classification is null
     */
    public static Entity of(
            TypeId id, TypeStructure structure, ClassificationTrace classification, Field identityField) {
        return new Entity(id, structure, classification, Optional.ofNullable(identityField));
    }

    /**
     * Creates an Entity without an identity field.
     *
     * @param id the type id
     * @param structure the type structure
     * @param classification the classification trace
     * @return a new Entity with no identity field
     * @throws NullPointerException if any argument is null
     */
    public static Entity of(TypeId id, TypeStructure structure, ClassificationTrace classification) {
        return new Entity(id, structure, classification, Optional.empty());
    }
}
