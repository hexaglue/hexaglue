/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/**
 * Domain type classification - AGGREGATE_ROOT, ENTITY, VALUE_OBJECT, etc.
 *
 * <p>The {@link io.hexaglue.core.classification.domain.DomainClassifier} evaluates
 * criteria to classify types as domain concepts.
 *
 * <h2>Domain Kinds</h2>
 * <ul>
 *   <li><b>AGGREGATE_ROOT</b> - Root of an aggregate, manages consistency</li>
 *   <li><b>ENTITY</b> - Has identity, mutable lifecycle</li>
 *   <li><b>VALUE_OBJECT</b> - Immutable, defined by attributes</li>
 *   <li><b>IDENTIFIER</b> - Typed identity (e.g., OrderId)</li>
 *   <li><b>DOMAIN_EVENT</b> - Something that happened in the domain</li>
 *   <li><b>DOMAIN_SERVICE</b> - Stateless domain logic</li>
 *   <li><b>APPLICATION_SERVICE</b> - Use case orchestrator</li>
 * </ul>
 *
 * <h2>Criteria Priority</h2>
 * <ul>
 *   <li>100: Explicit jMolecules annotations</li>
 *   <li>80: Strong heuristics (Repository-dominant, etc.)</li>
 *   <li>60-70: Medium heuristics (identity, immutability)</li>
 *   <li>50-55: Weak heuristics (naming patterns)</li>
 * </ul>
 *
 * @see io.hexaglue.core.classification.domain.DomainClassifier Main classifier
 * @see io.hexaglue.core.classification.domain.DomainKind Classification kinds
 * @see io.hexaglue.core.classification.domain.criteria Domain criteria
 */
package io.hexaglue.core.classification.domain;
