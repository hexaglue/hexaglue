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
 * Code generation plugin infrastructure.
 *
 * <p>This package provides the specialized API for building code generation
 * plugins. Generator plugins analyze the classified domain model and produce
 * code artifacts such as JPA entities, REST controllers, GraphQL schemas,
 * and database migration scripts.
 *
 * <h2>Key Types</h2>
 * <ul>
 *   <li>{@link io.hexaglue.spi.generation.GeneratorPlugin} - Plugin interface for generators</li>
 *   <li>{@link io.hexaglue.spi.generation.GeneratorContext} - Context with classification data and writers</li>
 *   <li>{@link io.hexaglue.spi.generation.ClassificationSnapshot} - Analyzed domain model view</li>
 *   <li>{@link io.hexaglue.spi.generation.ArtifactWriter} - File writer for generated artifacts</li>
 *   <li>{@link io.hexaglue.spi.generation.PluginCategory} - Plugin categorization</li>
 * </ul>
 *
 * @since 3.0.0
 */
package io.hexaglue.spi.generation;
