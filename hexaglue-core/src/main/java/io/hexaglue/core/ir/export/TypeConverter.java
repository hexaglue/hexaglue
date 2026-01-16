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

package io.hexaglue.core.ir.export;

import io.hexaglue.arch.ElementKind;
import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.classification.port.PortDirection;
import io.hexaglue.core.frontend.JavaForm;
import io.hexaglue.core.frontend.TypeRef;
import io.hexaglue.spi.ir.Cardinality;
import io.hexaglue.spi.ir.JavaConstruct;
import io.hexaglue.spi.ir.PortKind;
import java.util.List;

/**
 * Converts core model types to SPI types.
 *
 * <p>This class centralizes all type conversion logic between the core
 * classification/graph model and the public SPI IR types.
 */
final class TypeConverter {

    /**
     * Converts a classification kind string to an ElementKind.
     *
     * @param kind the kind string from classification
     * @return the corresponding ElementKind
     */
    ElementKind toElementKind(String kind) {
        return ElementKind.valueOf(kind);
    }

    /**
     * Converts a port kind string to a PortKind.
     *
     * @param kind the kind string from classification
     * @return the corresponding PortKind
     */
    PortKind toPortKind(String kind) {
        return switch (kind) {
            case "REPOSITORY" -> PortKind.REPOSITORY;
            case "USE_CASE" -> PortKind.USE_CASE;
            case "GATEWAY" -> PortKind.GATEWAY;
            case "QUERY" -> PortKind.QUERY;
            case "COMMAND" -> PortKind.COMMAND;
            case "EVENT_PUBLISHER" -> PortKind.EVENT_PUBLISHER;
            default -> PortKind.GENERIC;
        };
    }

    /**
     * Converts a core ConfidenceLevel to SPI ConfidenceLevel.
     *
     * @param confidence the core confidence level
     * @return the SPI confidence level
     */
    io.hexaglue.spi.ir.ConfidenceLevel toSpiConfidence(ConfidenceLevel confidence) {
        if (confidence == null) {
            return io.hexaglue.spi.ir.ConfidenceLevel.LOW;
        }
        return switch (confidence) {
            case EXPLICIT -> io.hexaglue.spi.ir.ConfidenceLevel.EXPLICIT;
            case HIGH -> io.hexaglue.spi.ir.ConfidenceLevel.HIGH;
            case MEDIUM -> io.hexaglue.spi.ir.ConfidenceLevel.MEDIUM;
            case LOW -> io.hexaglue.spi.ir.ConfidenceLevel.LOW;
        };
    }

    /**
     * Converts a core PortDirection to SPI PortDirection.
     *
     * @param direction the core port direction
     * @return the SPI port direction
     */
    io.hexaglue.spi.ir.PortDirection toSpiPortDirection(PortDirection direction) {
        if (direction == null) {
            return io.hexaglue.spi.ir.PortDirection.DRIVEN;
        }
        return switch (direction) {
            case DRIVING -> io.hexaglue.spi.ir.PortDirection.DRIVING;
            case DRIVEN -> io.hexaglue.spi.ir.PortDirection.DRIVEN;
        };
    }

    /**
     * Converts a JavaForm to a JavaConstruct.
     *
     * @param form the Java form from the semantic model
     * @return the corresponding JavaConstruct
     */
    JavaConstruct toJavaConstruct(JavaForm form) {
        return switch (form) {
            case CLASS -> JavaConstruct.CLASS;
            case RECORD -> JavaConstruct.RECORD;
            case INTERFACE -> JavaConstruct.INTERFACE;
            case ENUM -> JavaConstruct.ENUM;
            case ANNOTATION -> JavaConstruct.INTERFACE; // Annotations treated as interfaces in IR
        };
    }

    /**
     * Converts a core TypeRef to SPI TypeRef.
     *
     * @param coreTypeRef the core type reference
     * @return the SPI type reference
     */
    io.hexaglue.spi.ir.TypeRef toSpiTypeRef(TypeRef coreTypeRef) {
        List<io.hexaglue.spi.ir.TypeRef> spiArguments =
                coreTypeRef.arguments().stream().map(this::toSpiTypeRef).toList();

        return io.hexaglue.spi.ir.TypeRef.parameterized(
                coreTypeRef.rawQualifiedName(), extractCardinality(coreTypeRef), spiArguments);
    }

    /**
     * Extracts cardinality from a type reference.
     *
     * @param typeRef the type reference
     * @return the cardinality (SINGLE, OPTIONAL, or COLLECTION)
     */
    Cardinality extractCardinality(TypeRef typeRef) {
        if (typeRef.isCollectionLike() || typeRef.isArray()) {
            return Cardinality.COLLECTION;
        }
        if (typeRef.isOptionalLike()) {
            return Cardinality.OPTIONAL;
        }
        return Cardinality.SINGLE;
    }

    /**
     * Determines if an element kind should have an identity field.
     *
     * @param kind the element kind
     * @return true if the type should have identity
     */
    boolean shouldHaveIdentity(ElementKind kind) {
        return switch (kind) {
            case AGGREGATE, AGGREGATE_ROOT, ENTITY -> true;
            case VALUE_OBJECT,
                    DOMAIN_EVENT,
                    EXTERNALIZED_EVENT,
                    IDENTIFIER,
                    DOMAIN_SERVICE,
                    APPLICATION_SERVICE,
                    INBOUND_ONLY,
                    OUTBOUND_ONLY,
                    SAGA,
                    DRIVING_PORT,
                    DRIVEN_PORT,
                    DRIVING_ADAPTER,
                    DRIVEN_ADAPTER,
                    UNCLASSIFIED -> false;
        };
    }

    /**
     * Converts a core ConfidenceLevel to SPI CertaintyLevel.
     *
     * <p>Maps core confidence levels to SPI certainty levels for enrichment plugins:
     * <ul>
     *   <li>EXPLICIT → EXPLICIT (annotation-based)</li>
     *   <li>HIGH → CERTAIN_BY_STRUCTURE (strong structural signals)</li>
     *   <li>MEDIUM → INFERRED (medium strength inference)</li>
     *   <li>LOW → UNCERTAIN (weak signals)</li>
     * </ul>
     *
     * @param confidence the core confidence level
     * @return the corresponding SPI certainty level
     */
    io.hexaglue.spi.classification.CertaintyLevel toSpiCertainty(ConfidenceLevel confidence) {
        if (confidence == null) {
            return io.hexaglue.spi.classification.CertaintyLevel.NONE;
        }
        return switch (confidence) {
            case EXPLICIT -> io.hexaglue.spi.classification.CertaintyLevel.EXPLICIT;
            case HIGH -> io.hexaglue.spi.classification.CertaintyLevel.CERTAIN_BY_STRUCTURE;
            case MEDIUM -> io.hexaglue.spi.classification.CertaintyLevel.INFERRED;
            case LOW -> io.hexaglue.spi.classification.CertaintyLevel.UNCERTAIN;
        };
    }
}
