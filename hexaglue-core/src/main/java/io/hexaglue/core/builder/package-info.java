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
 * Builders for transforming TypeNode + Classification into enriched ArchType instances.
 *
 * <p>This package contains the builders that form the transformation pipeline
 * from the classification results to the final architectural model. The pipeline
 * follows a layered architecture:</p>
 *
 * <h2>Architecture</h2>
 * <pre>
 * TypeNode + ClassificationResult
 *          │
 *          ▼
 * ┌─────────────────────────────┐
 * │      BuilderContext         │  ← Shared context
 * ├─────────────────────────────┤
 * │  - GraphQuery               │
 * │  - ClassificationResults    │
 * │  - Map&lt;String, ArchType&gt;    │  (already built types)
 * └─────────────────────────────┘
 *          │
 *          ▼
 * ┌─────────────────────────────┐
 * │       DETECTORS             │
 * ├─────────────────────────────┤
 * │  - FieldRoleDetector        │  → Set&lt;FieldRole&gt;
 * │  - UnclassifiedCatDetector  │  → UnclassifiedCategory
 * └─────────────────────────────┘
 *          │
 *          ▼
 * ┌─────────────────────────────┐
 * │       BUILDERS              │
 * ├─────────────────────────────┤
 * │  - TypeStructureBuilder     │  → TypeStructure
 * │  - AggregateRootBuilder     │  → AggregateRoot
 * │  - EntityBuilder            │  → Entity
 * │  - ValueObjectBuilder       │  → ValueObject
 * │  - IdentifierBuilder        │  → Identifier
 * │  - DomainEventBuilder       │  → DomainEvent
 * │  - DomainServiceBuilder     │  → DomainService
 * │  - DrivingPortBuilder       │  → DrivingPort
 * │  - DrivenPortBuilder        │  → DrivenPort
 * │  - ApplicationTypeBuilder   │  → ApplicationType
 * │  - UnclassifiedTypeBuilder  │  → UnclassifiedType
 * └─────────────────────────────┘
 *          │
 *          ▼
 *       ArchType
 * </pre>
 *
 * <h2>Key Components</h2>
 * <ul>
 *   <li>{@link io.hexaglue.core.builder.BuilderContext} - Shared context for all builders</li>
 *   <li>{@link io.hexaglue.core.builder.FieldRoleDetector} - Detects semantic roles of fields</li>
 *   <li>{@link io.hexaglue.core.builder.UnclassifiedCategoryDetector} - Categorizes unclassified types</li>
 *   <li>{@link io.hexaglue.core.builder.ClassificationTraceConverter} - Converts Classification to ClassificationTrace</li>
 *   <li>{@link io.hexaglue.core.builder.TypeStructureBuilder} - Builds TypeStructure from TypeNode</li>
 * </ul>
 *
 * @since 4.1.0
 */
package io.hexaglue.core.builder;
