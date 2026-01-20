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

/**
 * Roles that a field can play within a domain type.
 *
 * <p>Field roles help understand the semantic meaning of fields during code generation.
 * For example, an IDENTITY field is the primary identifier of an entity, while a COLLECTION
 * field contains multiple related entities or value objects.</p>
 *
 * <h2>Categories</h2>
 * <ul>
 *   <li><strong>Business-relevant</strong>: IDENTITY, COLLECTION, AGGREGATE_REFERENCE, EMBEDDED</li>
 *   <li><strong>Technical</strong>: AUDIT, TECHNICAL</li>
 * </ul>
 *
 * @since 4.1.0
 */
public enum FieldRole {

    /**
     * The field is the identity of the entity or aggregate root.
     *
     * <p>Typically a field named {@code id} or annotated with identity markers.</p>
     */
    IDENTITY,

    /**
     * The field is a collection (List, Set, etc.) of other entities or value objects.
     */
    COLLECTION,

    /**
     * The field is a reference to another aggregate root.
     *
     * <p>This represents a cross-aggregate reference, typically stored as an ID.</p>
     */
    AGGREGATE_REFERENCE,

    /**
     * The field is an embedded value object or entity.
     *
     * <p>The embedded type is part of the same aggregate and doesn't have independent identity.</p>
     */
    EMBEDDED,

    /**
     * The field is used for auditing purposes.
     *
     * <p>Examples: createdAt, updatedAt, createdBy, version.</p>
     */
    AUDIT,

    /**
     * The field is a technical/infrastructure concern.
     *
     * <p>Examples: serialVersionUID, transient fields, framework-specific fields.</p>
     */
    TECHNICAL;

    /**
     * Returns whether this role represents a business-relevant field.
     *
     * <p>Business-relevant fields are part of the domain model's semantics.
     * Non-business fields (AUDIT, TECHNICAL) are infrastructure concerns.</p>
     *
     * @return true if this is a business-relevant role
     */
    public boolean isBusinessRelevant() {
        return switch (this) {
            case IDENTITY, COLLECTION, AGGREGATE_REFERENCE, EMBEDDED -> true;
            case AUDIT, TECHNICAL -> false;
        };
    }
}
