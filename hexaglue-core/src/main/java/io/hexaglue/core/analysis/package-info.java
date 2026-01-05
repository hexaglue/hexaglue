/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/**
 * Post-classification relation analysis.
 *
 * <p>This package analyzes relationships between classified types to enrich
 * the IR with relation information (ONE_TO_MANY, EMBEDDED, etc.).
 *
 * <p><b>Note:</b> This analysis runs AFTER classification, using the classified
 * types to determine relation kinds and cascade behavior.
 *
 * <h2>Components</h2>
 * <ul>
 *   <li>{@link io.hexaglue.core.analysis.RelationAnalyzer} - Extracts domain relations</li>
 *   <li>{@link io.hexaglue.core.analysis.MappedByDetector} - Detects bidirectional relations</li>
 *   <li>{@link io.hexaglue.core.analysis.CascadeInference} - Infers JPA cascade behavior</li>
 * </ul>
 *
 * <h2>Relation Kinds</h2>
 * <ul>
 *   <li><b>ONE_TO_ONE</b> - Single reference to entity</li>
 *   <li><b>ONE_TO_MANY</b> - Collection of entities</li>
 *   <li><b>MANY_TO_ONE</b> - Reference from child to parent</li>
 *   <li><b>EMBEDDED</b> - Value object embedded in entity</li>
 * </ul>
 *
 * @see io.hexaglue.core.analysis.RelationAnalyzer Main analyzer
 */
package io.hexaglue.core.analysis;
