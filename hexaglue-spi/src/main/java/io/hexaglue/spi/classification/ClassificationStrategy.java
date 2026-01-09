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

package io.hexaglue.spi.classification;

/**
 * The strategy used to classify a domain type.
 *
 * <p>This enum captures <b>how</b> HexaGlue determined the classification of a type,
 * providing transparency and traceability for classification decisions. Understanding
 * the strategy helps developers debug classifications and tune the classifier.
 *
 * <p>Strategies are listed in priority order - earlier strategies take precedence:
 * <ol>
 *   <li>ANNOTATION - explicit user declaration</li>
 *   <li>REPOSITORY - structural pattern matching</li>
 *   <li>RECORD - Java language construct heuristic</li>
 *   <li>COMPOSITION - graph-based inference</li>
 *   <li>WEIGHTED - multi-signal scoring</li>
 *   <li>UNCLASSIFIED - no strategy matched</li>
 * </ol>
 *
 * @since 3.0.0
 */
public enum ClassificationStrategy {

    /**
     * Classification based on explicit HexaGlue annotations.
     *
     * <p>The type was annotated with {@code @AggregateRoot}, {@code @Entity},
     * {@code @ValueObject}, or similar. This is the highest-priority strategy
     * because it represents explicit developer intent.
     *
     * <p>Example:
     * <pre>{@code
     * @AggregateRoot
     * public class Order { ... }
     * }</pre>
     *
     * <p>Associated certainty: {@link CertaintyLevel#EXPLICIT}
     */
    ANNOTATION,

    /**
     * Classification based on Repository pattern matching.
     *
     * <p>The type is the type parameter of a Repository interface (e.g.,
     * {@code Repository<Order>} or {@code OrderRepository}). This structural
     * pattern strongly indicates an aggregate root.
     *
     * <p>Example:
     * <pre>{@code
     * interface OrderRepository extends Repository<Order, OrderId> { ... }
     * }</pre>
     *
     * <p>Associated certainty: {@link CertaintyLevel#CERTAIN_BY_STRUCTURE}
     */
    REPOSITORY,

    /**
     * Classification based on composition and embedding relationships.
     *
     * <p>The type is embedded within or composed by another classified domain type.
     * For example, a type embedded in an aggregate root is likely a value object.
     *
     * <p>Example:
     * <pre>{@code
     * @AggregateRoot
     * class Order {
     *     Money total; // Money classified as VALUE_OBJECT via COMPOSITION
     * }
     * }</pre>
     *
     * <p>Associated certainty: {@link CertaintyLevel#INFERRED}
     */
    COMPOSITION,

    /**
     * Classification based on Java Record heuristic.
     *
     * <p>The type is a Java {@code record}, which are immutable by design and
     * commonly used for value objects in modern DDD.
     *
     * <p>Example:
     * <pre>{@code
     * public record Money(BigDecimal amount, Currency currency) { }
     * }</pre>
     *
     * <p>Associated certainty: {@link CertaintyLevel#CERTAIN_BY_STRUCTURE}
     */
    RECORD,

    /**
     * Classification based on weighted scoring of multiple signals.
     *
     * <p>No single strong signal was present, so the classifier combined multiple
     * weaker signals (naming patterns, field analysis, method signatures) and used
     * a scoring algorithm to determine the most likely classification.
     *
     * <p>Example signals:
     * <ul>
     *   <li>Name ends with "Entity" or "VO"</li>
     *   <li>Has/lacks identity fields</li>
     *   <li>Has/lacks equals/hashCode methods</li>
     *   <li>Mutability characteristics</li>
     * </ul>
     *
     * <p>Associated certainty: {@link CertaintyLevel#INFERRED} or {@link CertaintyLevel#UNCERTAIN}
     */
    WEIGHTED,

    /**
     * No classification strategy matched.
     *
     * <p>The type could not be classified as a domain type. This typically happens
     * for infrastructure classes, DTOs, or types outside the domain model.
     *
     * <p>Associated certainty: {@link CertaintyLevel#NONE}
     */
    UNCLASSIFIED;

    /**
     * Returns true if this strategy is based on explicit user declaration.
     *
     * @return true if strategy is ANNOTATION
     */
    public boolean isExplicit() {
        return this == ANNOTATION;
    }

    /**
     * Returns true if this strategy is based on structural patterns.
     *
     * @return true if strategy is REPOSITORY or RECORD
     */
    public boolean isStructural() {
        return this == REPOSITORY || this == RECORD;
    }

    /**
     * Returns true if this strategy is based on inference or heuristics.
     *
     * @return true if strategy is COMPOSITION or WEIGHTED
     */
    public boolean isInferred() {
        return this == COMPOSITION || this == WEIGHTED;
    }
}
