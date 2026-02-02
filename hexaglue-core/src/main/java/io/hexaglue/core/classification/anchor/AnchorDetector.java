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

import io.hexaglue.core.classification.Evidence;
import io.hexaglue.core.classification.EvidenceType;
import io.hexaglue.core.frontend.JavaForm;
import io.hexaglue.core.graph.ApplicationGraph;
import io.hexaglue.core.graph.model.AnnotationRef;
import io.hexaglue.core.graph.model.FieldNode;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import java.util.List;
import java.util.Set;

/**
 * Detects the anchor classification for types based on infrastructure dependencies.
 *
 * <p>Anchors are computed BEFORE domain/port classification to identify
 * which classes belong to infrastructure vs application core.
 *
 * <p>Detection priority:
 * <ol>
 *   <li>Driving annotations (RestController, etc.) → DRIVING_ANCHOR</li>
 *   <li>Infrastructure annotations (Repository, Entity, etc.) → INFRA_ANCHOR</li>
 *   <li>Field dependencies on infrastructure types → INFRA_ANCHOR</li>
 *   <li>Default → DOMAIN_ANCHOR</li>
 * </ol>
 */
public final class AnchorDetector {

    // === Driving annotations (framework entry points) ===
    private static final Set<String> DRIVING_ANNOTATIONS = Set.of(
            // Spring Web
            "org.springframework.web.bind.annotation.RestController",
            "org.springframework.stereotype.Controller",
            "org.springframework.web.bind.annotation.ControllerAdvice",
            // Spring Messaging
            "org.springframework.kafka.annotation.KafkaListener",
            "org.springframework.jms.annotation.JmsListener",
            "org.springframework.amqp.rabbit.annotation.RabbitListener",
            "org.springframework.cloud.stream.annotation.StreamListener",
            // Spring Scheduling
            "org.springframework.scheduling.annotation.Scheduled",
            // Spring GraphQL
            "org.springframework.graphql.data.method.annotation.QueryMapping",
            "org.springframework.graphql.data.method.annotation.MutationMapping",
            // Jakarta/Javax
            "jakarta.ws.rs.Path",
            "javax.ws.rs.Path");

    // === Infrastructure annotations (persistence, messaging, etc.) ===
    // Note: @Service and @Component are intentionally NOT included here.
    // These are Spring stereotype annotations commonly used on application services
    // (hexagonal core). Including them would prevent CoreAppClassDetector from
    // analyzing application services, breaking semantic port detection.
    // Infrastructure services with @Service/@Component are still detected via
    // field dependencies on infrastructure types (JdbcTemplate, EntityManager, etc.).
    private static final Set<String> INFRA_ANNOTATIONS = Set.of(
            // Spring Data
            "org.springframework.stereotype.Repository",
            "org.springframework.data.repository.Repository",
            // JPA/Jakarta Persistence
            "jakarta.persistence.Entity",
            "jakarta.persistence.Table",
            "jakarta.persistence.Embeddable",
            "javax.persistence.Entity",
            "javax.persistence.Table",
            // MongoDB
            "org.springframework.data.mongodb.core.mapping.Document",
            // Cassandra
            "org.springframework.data.cassandra.core.mapping.Table");

    // === Infrastructure types (field dependencies) ===
    private static final Set<String> INFRA_TYPE_PREFIXES = Set.of(
            // Spring Data
            "org.springframework.jdbc.core.JdbcTemplate",
            "org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate",
            "org.springframework.data.jpa.repository.",
            "org.springframework.data.mongodb.core.MongoTemplate",
            "org.springframework.data.mongodb.repository.",
            "org.springframework.data.redis.core.RedisTemplate",
            // JPA/Jakarta
            "jakarta.persistence.EntityManager",
            "javax.persistence.EntityManager",
            // HTTP clients
            "org.springframework.web.client.RestTemplate",
            "org.springframework.web.reactive.function.client.WebClient",
            "feign.",
            // Messaging
            "org.springframework.kafka.core.KafkaTemplate",
            "org.springframework.amqp.rabbit.core.RabbitTemplate",
            "org.springframework.jms.core.JmsTemplate");

    private AnchorDetector() {
        // Utility class
    }

    /**
     * Analyzes all types in the graph and returns an AnchorContext.
     *
     * @param graph the application graph
     * @return the anchor context with classifications for all types
     */
    public static AnchorContext analyze(ApplicationGraph graph) {
        return analyze(graph.query());
    }

    /**
     * Analyzes all types via the query and returns an AnchorContext.
     *
     * @param query the graph query
     * @return the anchor context with classifications for all types
     */
    public static AnchorContext analyze(GraphQuery query) {
        AnchorContext.Builder builder = AnchorContext.builder();

        // Only classify classes and records (not interfaces, enums, annotations)
        query.types(t -> t.form() == JavaForm.CLASS || t.form() == JavaForm.RECORD)
                .forEach(type -> {
                    AnchorResult result = classify(type, query);
                    builder.put(result);
                });

        return builder.build();
    }

    /**
     * Classifies a single type as an anchor.
     *
     * @param type the type to classify
     * @param query the graph query for field lookups
     * @return the anchor result
     */
    public static AnchorResult classify(TypeNode type, GraphQuery query) {
        // Priority 1: Check for driving annotations
        for (AnnotationRef annotation : type.annotations()) {
            if (isDrivingAnnotation(annotation.qualifiedName())) {
                return AnchorResult.drivingAnchor(
                        type.id(), Evidence.fromAnnotation(annotation.simpleName(), type.id()));
            }
        }

        // Priority 2: Check for infrastructure annotations
        for (AnnotationRef annotation : type.annotations()) {
            if (isInfraAnnotation(annotation.qualifiedName())) {
                return AnchorResult.infraAnchor(type.id(), Evidence.fromAnnotation(annotation.simpleName(), type.id()));
            }
        }

        // Priority 3: Check for infrastructure package patterns
        if (isInfraPackage(type.packageName())) {
            return AnchorResult.infraAnchor(
                    type.id(),
                    new Evidence(
                            EvidenceType.RELATIONSHIP,
                            "Package '%s' matches infrastructure pattern".formatted(type.packageName()),
                            List.of()));
        }

        // Priority 4: Check for infrastructure field dependencies
        List<FieldNode> fields = query.fieldsOf(type);
        for (FieldNode field : fields) {
            String fieldTypeName = field.type().rawQualifiedName();
            if (isInfraType(fieldTypeName)) {
                return AnchorResult.infraAnchor(
                        type.id(),
                        new Evidence(
                                EvidenceType.RELATIONSHIP,
                                "Field '%s' has infrastructure type '%s'".formatted(field.simpleName(), fieldTypeName),
                                List.of(field.id())));
            }
        }

        // Default: Domain anchor
        return AnchorResult.domainAnchor(type.id());
    }

    /**
     * Returns true if the annotation is a driving annotation.
     */
    public static boolean isDrivingAnnotation(String annotationQualifiedName) {
        return DRIVING_ANNOTATIONS.contains(annotationQualifiedName);
    }

    /**
     * Returns true if the annotation is an infrastructure annotation.
     */
    public static boolean isInfraAnnotation(String annotationQualifiedName) {
        return INFRA_ANNOTATIONS.contains(annotationQualifiedName);
    }

    /**
     * Returns true if the type is an infrastructure type.
     */
    public static boolean isInfraType(String typeQualifiedName) {
        if (typeQualifiedName == null) {
            return false;
        }
        for (String prefix : INFRA_TYPE_PREFIXES) {
            if (typeQualifiedName.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the package indicates infrastructure code.
     *
     * <p>This is a weak signal and should be used with caution.
     */
    public static boolean isInfraPackage(String packageName) {
        if (packageName == null) {
            return false;
        }
        return packageName.contains(".infrastructure.")
                || packageName.contains(".infra.")
                || packageName.contains(".adapters.")
                || packageName.contains(".adapter.")
                || packageName.endsWith(".infrastructure")
                || packageName.endsWith(".infra");
    }

    /**
     * Returns true if the package indicates a driving adapter.
     *
     * <p>This is a weak signal and should be used with caution.
     */
    public static boolean isDrivingPackage(String packageName) {
        if (packageName == null) {
            return false;
        }
        return packageName.contains(".web.")
                || packageName.contains(".rest.")
                || packageName.contains(".api.")
                || packageName.contains(".controller.")
                || packageName.contains(".controllers.");
    }
}
