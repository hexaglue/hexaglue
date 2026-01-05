/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/**
 * Single-pass classification system for domain types and ports.
 *
 * <p>The {@link io.hexaglue.core.classification.SinglePassClassifier} orchestrates
 * the classification pipeline, which runs in three phases:
 *
 * <h2>Classification Pipeline</h2>
 * <ol>
 *   <li><b>Semantic Index Construction</b>
 *     <ul>
 *       <li>{@link io.hexaglue.core.classification.anchor.AnchorDetector} → AnchorContext</li>
 *       <li>{@link io.hexaglue.core.classification.semantic.CoreAppClassDetector} → CoreAppClassIndex</li>
 *       <li>{@link io.hexaglue.core.classification.semantic.InterfaceFactsIndex} → InterfaceFacts</li>
 *     </ul>
 *   </li>
 *   <li><b>Port Classification</b> (ports first, provides context for domain)</li>
 *   <li><b>Domain Classification</b> (with full port context available)</li>
 * </ol>
 *
 * <h2>Subpackages</h2>
 * <ul>
 *   <li>{@link io.hexaglue.core.classification.domain} - Domain type classification</li>
 *   <li>{@link io.hexaglue.core.classification.port} - Port interface classification</li>
 *   <li>{@link io.hexaglue.core.classification.anchor} - Infrastructure anchor detection</li>
 *   <li>{@link io.hexaglue.core.classification.semantic} - Semantic index computation</li>
 * </ul>
 *
 * @see io.hexaglue.core.classification.SinglePassClassifier Main orchestrator
 * @see io.hexaglue.core.classification.ClassificationCriteria Criteria interface
 */
package io.hexaglue.core.classification;
