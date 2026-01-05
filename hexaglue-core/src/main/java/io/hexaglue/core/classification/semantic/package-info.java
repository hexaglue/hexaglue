/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/**
 * Semantic index computation for classification.
 *
 * <p>This package provides semantic indexes that capture structural relationships
 * in the codebase, enabling classification based on relationships rather than
 * just names.
 *
 * <h2>Key Concepts</h2>
 *
 * <h3>CoreAppClass</h3>
 * <p>A DOMAIN_ANCHOR class that implements or depends on user-code interfaces.
 * CoreAppClasses are the "pivots" that link DRIVING and DRIVEN ports:
 * <ul>
 *   <li><b>Pivot</b> - Implements DRIVING + depends on DRIVEN (Application Service)</li>
 *   <li><b>Inbound-only</b> - Implements DRIVING, no DRIVEN dependencies</li>
 *   <li><b>Outbound-only</b> - Depends on DRIVEN, no DRIVING implementation</li>
 * </ul>
 *
 * <h3>InterfaceFacts</h3>
 * <p>Facts about each interface for semantic port classification:
 * <ul>
 *   <li>{@code implementedByCore} → DRIVING port candidate</li>
 *   <li>{@code usedByCore + missingImpl} → DRIVEN port candidate</li>
 * </ul>
 *
 * @see io.hexaglue.core.classification.semantic.CoreAppClass Pivot class model
 * @see io.hexaglue.core.classification.semantic.CoreAppClassDetector Detection logic
 * @see io.hexaglue.core.classification.semantic.InterfaceFacts Interface facts model
 * @see io.hexaglue.core.classification.semantic.InterfaceFactsIndex Facts index
 */
package io.hexaglue.core.classification.semantic;
