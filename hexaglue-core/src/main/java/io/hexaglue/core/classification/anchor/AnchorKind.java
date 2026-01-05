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

package io.hexaglue.core.classification.anchor;

/**
 * Classification of a class based on its infrastructure dependencies.
 *
 * <p>Anchors are computed BEFORE domain/port classification to identify
 * which classes belong to infrastructure vs application core.
 *
 * <p>This enables semantic classification based on structural relationships
 * rather than naming conventions.
 */
public enum AnchorKind {

    /**
     * Class depends on infrastructure types or annotations.
     *
     * <p>Examples:
     * <ul>
     *   <li>Has @Repository (Spring Data)</li>
     *   <li>Has @Entity (JPA)</li>
     *   <li>Depends on JdbcTemplate, EntityManager, MongoTemplate</li>
     *   <li>Depends on RestTemplate, WebClient</li>
     * </ul>
     */
    INFRA_ANCHOR,

    /**
     * Class is a framework entry point (driving adapter).
     *
     * <p>Examples:
     * <ul>
     *   <li>Has @RestController, @Controller</li>
     *   <li>Has @MessageListener, @KafkaListener</li>
     *   <li>Has @Scheduled</li>
     *   <li>Has @RabbitListener</li>
     * </ul>
     */
    DRIVING_ANCHOR,

    /**
     * User-code class without infrastructure or driving dependencies.
     *
     * <p>This is the "pure" domain/application code that forms the core
     * of the hexagonal architecture.
     */
    DOMAIN_ANCHOR
}
