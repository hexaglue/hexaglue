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

package io.hexaglue.plugin.livingdoc.util;

import io.hexaglue.arch.ElementKind;
import io.hexaglue.arch.model.ir.ConfidenceLevel;
import io.hexaglue.arch.model.ir.PortDirection;
import io.hexaglue.arch.model.ir.PortKind;

/**
 * Utility methods for formatting type information for display.
 *
 * <p>Provides shared functionality for:
 * <ul>
 *   <li>Simplifying fully qualified type names</li>
 *   <li>Formatting domain kinds</li>
 *   <li>Formatting port kinds and directions</li>
 *   <li>Displaying confidence badges</li>
 * </ul>
 */
public final class TypeDisplayUtil {

    private TypeDisplayUtil() {
        // Utility class
    }

    /**
     * Simplifies a fully qualified type name to its simple name.
     *
     * <p>Handles generic types like {@code Optional<Order>} by simplifying
     * the outer type while preserving the generic structure.
     *
     * @param qualifiedType the fully qualified type name
     * @return the simple type name, or "?" if null/empty
     */
    public static String simplifyType(String qualifiedType) {
        if (qualifiedType == null || qualifiedType.isEmpty()) {
            return "?";
        }

        // Handle generics like Optional<com.example.Order>
        if (qualifiedType.contains("<")) {
            int genericStart = qualifiedType.indexOf('<');
            String beforeGeneric = qualifiedType.substring(0, genericStart);
            String genericPart = qualifiedType.substring(genericStart);

            int lastDot = beforeGeneric.lastIndexOf('.');
            String simplifiedOuter = lastDot >= 0 ? beforeGeneric.substring(lastDot + 1) : beforeGeneric;

            return simplifiedOuter + genericPart;
        }

        int lastDot = qualifiedType.lastIndexOf('.');
        return lastDot >= 0 ? qualifiedType.substring(lastDot + 1) : qualifiedType;
    }

    /**
     * Formats a ElementKind for human-readable display.
     *
     * @param kind the domain kind to format
     * @return formatted kind name
     */
    public static String formatKind(ElementKind kind) {
        return switch (kind) {
            case AGGREGATE -> "Aggregate";
            case AGGREGATE_ROOT -> "Aggregate Root";
            case ENTITY -> "Entity";
            case VALUE_OBJECT -> "Value Object";
            case IDENTIFIER -> "Identifier";
            case DOMAIN_EVENT -> "Domain Event";
            case EXTERNALIZED_EVENT -> "Externalized Event";
            case DOMAIN_SERVICE -> "Domain Service";
            case APPLICATION_SERVICE -> "Application Service";
            case INBOUND_ONLY -> "Inbound Only";
            case OUTBOUND_ONLY -> "Outbound Only";
            case SAGA -> "Saga";
            case DRIVING_PORT -> "Driving Port";
            case DRIVEN_PORT -> "Driven Port";
            case DRIVING_ADAPTER -> "Driving Adapter";
            case DRIVEN_ADAPTER -> "Driven Adapter";
            case UNCLASSIFIED -> "Unclassified";
        };
    }

    /**
     * Formats a PortKind for human-readable display.
     *
     * @param kind the port kind to format
     * @return formatted kind name
     */
    public static String formatKind(PortKind kind) {
        return switch (kind) {
            case REPOSITORY -> "Repository";
            case GATEWAY -> "Gateway";
            case USE_CASE -> "Use Case";
            case COMMAND -> "Command Handler";
            case QUERY -> "Query Handler";
            case EVENT_PUBLISHER -> "Event Publisher";
            case GENERIC -> "Generic Port";
        };
    }

    /**
     * Formats a PortDirection for human-readable display.
     *
     * @param direction the port direction to format
     * @return formatted direction with explanatory text
     */
    public static String formatDirection(PortDirection direction) {
        return switch (direction) {
            case DRIVING -> "Driving (Primary/Inbound)";
            case DRIVEN -> "Driven (Secondary/Outbound)";
        };
    }

    /**
     * Formats a confidence level as a visual badge.
     *
     * <p>EXPLICIT and HIGH confidence have no badge (assumed reliable).
     * MEDIUM and LOW confidence get warning badges to alert reviewers.
     *
     * @param confidence the confidence level
     * @return badge text, or empty string for high confidence
     */
    public static String formatConfidenceBadge(ConfidenceLevel confidence) {
        return switch (confidence) {
            case EXPLICIT -> "";
            case HIGH -> "";
            case MEDIUM -> " [Medium Confidence]";
            case LOW -> " [Low Confidence - Verify]";
        };
    }

    // ============ String-based overloads for documentation models ============

    /**
     * Formats a domain kind string for human-readable display.
     *
     * <p>Delegates to {@link #formatKind(ElementKind)} after parsing the string.
     *
     * @param kind the domain kind string (e.g., "AGGREGATE_ROOT")
     * @return formatted kind name, or "Unknown" if null, or the original string if not a valid enum value
     */
    public static String formatElementKind(String kind) {
        if (kind == null) {
            return "Unknown";
        }
        try {
            return formatKind(ElementKind.valueOf(kind));
        } catch (IllegalArgumentException e) {
            return kind;
        }
    }

    /**
     * Formats a port kind string for human-readable display.
     *
     * <p>Delegates to {@link #formatKind(PortKind)} after parsing the string.
     *
     * @param kind the port kind string (e.g., "REPOSITORY")
     * @return formatted kind name, or "Unknown" if null, or the original string if not a valid enum value
     */
    public static String formatPortKind(String kind) {
        if (kind == null) {
            return "Unknown";
        }
        try {
            return formatKind(PortKind.valueOf(kind));
        } catch (IllegalArgumentException e) {
            return kind;
        }
    }

    /**
     * Formats a port direction string for human-readable display.
     *
     * <p>Delegates to {@link #formatDirection(PortDirection)} after parsing the string.
     *
     * @param direction the port direction string (e.g., "DRIVING")
     * @return formatted direction with explanatory text, or "Unknown" if null, or the original string if not a valid enum value
     */
    public static String formatPortDirection(String direction) {
        if (direction == null) {
            return "Unknown";
        }
        try {
            return formatDirection(PortDirection.valueOf(direction));
        } catch (IllegalArgumentException e) {
            return direction;
        }
    }

    /**
     * Formats a confidence level string as a visual badge.
     *
     * <p>Delegates to {@link #formatConfidenceBadge(ConfidenceLevel)} after parsing the string.
     *
     * @param confidence the confidence level string (e.g., "HIGH")
     * @return badge text, or empty string for high confidence, null, or invalid enum values
     */
    public static String formatConfidenceBadgeFromString(String confidence) {
        if (confidence == null) {
            return "";
        }
        try {
            return formatConfidenceBadge(ConfidenceLevel.valueOf(confidence));
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    /**
     * Gets a stereotype label for domain types in diagrams.
     *
     * <p>Note: Returns "Event" instead of "Domain Event" for compact diagram display.
     *
     * @param kind the domain kind
     * @return stereotype label for UML diagrams
     */
    public static String getStereotype(ElementKind kind) {
        return switch (kind) {
            case AGGREGATE -> "Aggregate";
            case AGGREGATE_ROOT -> "Aggregate Root";
            case ENTITY -> "Entity";
            case VALUE_OBJECT -> "Value Object";
            case IDENTIFIER -> "Identifier";
            case DOMAIN_EVENT -> "Event";
            case EXTERNALIZED_EVENT -> "Externalized Event";
            case DOMAIN_SERVICE -> "Domain Service";
            case APPLICATION_SERVICE -> "Application Service";
            case INBOUND_ONLY -> "Inbound Only";
            case OUTBOUND_ONLY -> "Outbound Only";
            case SAGA -> "Saga";
            case DRIVING_PORT -> "Driving Port";
            case DRIVEN_PORT -> "Driven Port";
            case DRIVING_ADAPTER -> "Driving Adapter";
            case DRIVEN_ADAPTER -> "Driven Adapter";
            case UNCLASSIFIED -> "Unclassified";
        };
    }

    /**
     * Gets a stereotype label for domain types in diagrams.
     *
     * <p>Delegates to {@link #getStereotype(ElementKind)} after parsing the string.
     *
     * @param kind the domain kind string
     * @return stereotype label for UML diagrams, or null if null/invalid
     */
    public static String getStereotype(String kind) {
        if (kind == null) {
            return null;
        }
        try {
            return getStereotype(ElementKind.valueOf(kind));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Formats cardinality for display.
     *
     * @param cardinality the cardinality string (e.g., "SINGLE")
     * @return formatted cardinality
     */
    public static String formatCardinality(String cardinality) {
        if (cardinality == null) {
            return "?";
        }
        return switch (cardinality) {
            case "SINGLE" -> "Single";
            case "OPTIONAL" -> "Optional";
            case "COLLECTION" -> "Collection";
            default -> cardinality;
        };
    }
}
