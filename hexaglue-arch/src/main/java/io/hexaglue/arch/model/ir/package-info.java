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

/**
 * Intermediate representation types for domain model analysis and code generation.
 *
 * <p>This package contains types that represent analyzed domain constructs
 * in a normalized form suitable for code generation plugins. These types
 * capture relationships, identities, type references, and port methods
 * with their semantic information.
 *
 * <h2>Key Types</h2>
 * <ul>
 *   <li>{@link io.hexaglue.arch.model.ir.TypeRef} - Type reference with generics support</li>
 *   <li>{@link io.hexaglue.arch.model.ir.Identity} - Identity information for entities/aggregates</li>
 *   <li>{@link io.hexaglue.arch.model.ir.DomainRelation} - Relationship between domain types</li>
 *   <li>{@link io.hexaglue.arch.model.ir.PortMethod} - Port method with semantic classification</li>
 * </ul>
 *
 * <h2>Enumerations</h2>
 * <ul>
 *   <li>{@link io.hexaglue.arch.model.ir.Cardinality} - Single, Optional, Collection, Stream</li>
 *   <li>{@link io.hexaglue.arch.model.ir.RelationKind} - JPA relationship types</li>
 *   <li>{@link io.hexaglue.arch.model.ir.MethodKind} - Spring Data method classifications</li>
 *   <li>{@link io.hexaglue.arch.model.ir.IdentityStrategy} - ID generation strategies</li>
 * </ul>
 *
 * <p>These types were migrated from {@code io.hexaglue.spi.ir} to properly
 * place data models in the architectural model module.
 *
 * @since 5.0.0
 */
package io.hexaglue.arch.model.ir;
