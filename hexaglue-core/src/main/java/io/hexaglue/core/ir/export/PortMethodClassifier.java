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

import io.hexaglue.core.graph.model.ParameterInfo;
import io.hexaglue.spi.ir.Identity;
import io.hexaglue.spi.ir.MethodKind;
import io.hexaglue.spi.ir.QueryModifier;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Classifies port methods according to Spring Data conventions.
 *
 * <p>This classifier analyzes method names and parameters to determine:
 * <ul>
 *   <li>The method kind (FIND_BY_ID, FIND_BY_PROPERTY, EXISTS_BY_PROPERTY, etc.)</li>
 *   <li>Target properties for derived queries (e.g., "email" for findByEmail)</li>
 *   <li>Query modifiers (DISTINCT, IGNORE_CASE, etc.)</li>
 *   <li>Limit size for Top/First queries</li>
 *   <li>Order by property and direction</li>
 * </ul>
 *
 * <p>This class performs classification in the Core, ensuring plugins consume
 * pre-classified information from the SPI rather than re-implementing classification logic.
 *
 * @since 3.0.0
 */
public final class PortMethodClassifier {

    // Pattern to extract Top/First limit: findTop10By... or findFirst5By...
    private static final Pattern TOP_N_PATTERN = Pattern.compile("^(find|get|read|query|search)(Top|First)(\\d+)By.*$");

    // Pattern to detect First/Top methods: findFirst..., findTop..., findFirstBy..., findTopBy...
    private static final Pattern FIRST_TOP_PATTERN =
            Pattern.compile("^(find|get|read|query|search)(First|Top)(\\d*)($|By.*)$");

    // Pattern to extract OrderBy clause: ...OrderByXxxAsc or ...OrderByXxxDesc
    // Use non-greedy matching to properly capture direction
    private static final Pattern ORDER_BY_PATTERN = Pattern.compile(".*OrderBy([A-Z][a-zA-Z0-9]*?)(Asc|Desc)$");

    // Subject keywords that Spring Data recognizes
    private static final List<String> FIND_PREFIXES = List.of("find", "get", "read", "query", "search", "load");
    private static final List<String> COUNT_PREFIXES = List.of("count");
    private static final List<String> EXISTS_PREFIXES = List.of("exists");
    private static final List<String> DELETE_PREFIXES = List.of("delete", "remove");
    private static final List<String> STREAM_PREFIXES = List.of("stream");
    private static final List<String> SAVE_PREFIXES = List.of("save");

    /**
     * Result of method classification.
     *
     * @param kind the classified method kind
     * @param targetProperties properties targeted by the query
     * @param modifiers query modifiers detected
     * @param limitSize limit size for Top/First queries
     * @param orderByProperty property for ordering
     */
    public record ClassificationResult(
            MethodKind kind,
            List<String> targetProperties,
            Set<QueryModifier> modifiers,
            Optional<Integer> limitSize,
            Optional<String> orderByProperty) {

        public static ClassificationResult of(MethodKind kind) {
            return new ClassificationResult(kind, List.of(), Set.of(), Optional.empty(), Optional.empty());
        }

        public static ClassificationResult withProperty(MethodKind kind, String property) {
            return new ClassificationResult(kind, List.of(property), Set.of(), Optional.empty(), Optional.empty());
        }

        public static ClassificationResult withProperties(MethodKind kind, List<String> properties) {
            return new ClassificationResult(kind, properties, Set.of(), Optional.empty(), Optional.empty());
        }
    }

    /**
     * Classifies a port method based on its name, parameters, and optional aggregate identity.
     *
     * @param methodName the method name (e.g., "findById", "existsByEmail")
     * @param params the method parameters
     * @param aggregateIdentity the aggregate's identity information, if available
     * @return the classification result
     */
    public ClassificationResult classify(
            String methodName, List<ParameterInfo> params, Optional<Identity> aggregateIdentity) {

        // Handle save methods
        if (isSaveMethod(methodName)) {
            return ClassificationResult.of(methodName.equals("saveAll") ? MethodKind.SAVE_ALL : MethodKind.SAVE);
        }

        // Extract modifiers and clean name
        Set<QueryModifier> modifiers = extractModifiers(methodName);
        String cleanName = removeModifiers(methodName);

        // Extract limit for Top/First
        Optional<Integer> limitSize = extractLimitSize(methodName);

        // Extract OrderBy
        Optional<String> orderByProperty = extractOrderByProperty(methodName);

        // Classify by method type
        MethodKind kind = classifyKind(cleanName, params, aggregateIdentity);

        // Extract target properties
        List<String> targetProperties = extractTargetProperties(cleanName, kind);

        return new ClassificationResult(kind, targetProperties, modifiers, limitSize, orderByProperty);
    }

    private boolean isSaveMethod(String methodName) {
        return SAVE_PREFIXES.stream().anyMatch(methodName::startsWith);
    }

    private MethodKind classifyKind(
            String methodName, List<ParameterInfo> params, Optional<Identity> aggregateIdentity) {

        // find/get/read/query/search methods
        if (startsWithAny(methodName, FIND_PREFIXES)) {
            return classifyFindMethod(methodName, params, aggregateIdentity);
        }

        // exists methods
        if (startsWithAny(methodName, EXISTS_PREFIXES)) {
            return classifyExistsMethod(methodName, params, aggregateIdentity);
        }

        // count methods
        if (startsWithAny(methodName, COUNT_PREFIXES)) {
            return classifyCountMethod(methodName, params);
        }

        // delete/remove methods
        if (startsWithAny(methodName, DELETE_PREFIXES)) {
            return classifyDeleteMethod(methodName, params, aggregateIdentity);
        }

        // stream methods
        if (startsWithAny(methodName, STREAM_PREFIXES)) {
            return classifyStreamMethod(methodName, params);
        }

        return MethodKind.CUSTOM;
    }

    private MethodKind classifyFindMethod(
            String methodName, List<ParameterInfo> params, Optional<Identity> aggregateIdentity) {

        // findAll without parameters
        if (methodName.equals("findAll") && params.isEmpty()) {
            return MethodKind.FIND_ALL;
        }

        // findAllById
        if (isIdBasedMethod(methodName, "All") && hasCollectionParameter(params)) {
            return MethodKind.FIND_ALL_BY_ID;
        }

        // findById, getById, etc.
        if (isIdBasedMethod(methodName, "") && hasIdentityParameter(params, aggregateIdentity)) {
            return MethodKind.FIND_BY_ID;
        }

        // findFirst / findTop without "By" - collection-based
        if (isFirstOrTopMethod(methodName) && !methodName.contains("By")) {
            return MethodKind.FIND_FIRST;
        }

        // findFirstBy... / findTopBy... with property
        if (isFirstOrTopMethod(methodName) && methodName.contains("By")) {
            // Check if it's findFirst or findTopN
            if (extractLimitSize(methodName).isPresent()) {
                return MethodKind.FIND_TOP_N;
            }
            return MethodKind.FIND_FIRST;
        }

        // findAllBy... with property
        if (isPropertyBasedAllMethod(methodName)) {
            return MethodKind.FIND_ALL_BY_PROPERTY;
        }

        // findBy... with property (single result)
        if (hasPropertySuffix(methodName)) {
            return MethodKind.FIND_BY_PROPERTY;
        }

        return MethodKind.CUSTOM;
    }

    private MethodKind classifyExistsMethod(
            String methodName, List<ParameterInfo> params, Optional<Identity> aggregateIdentity) {

        // existsById
        if (methodName.equals("existsById") && hasIdentityParameter(params, aggregateIdentity)) {
            return MethodKind.EXISTS_BY_ID;
        }

        // existsBy... (property-based)
        if (methodName.startsWith("existsBy") && !methodName.equals("existsById")) {
            return MethodKind.EXISTS_BY_PROPERTY;
        }

        return MethodKind.CUSTOM;
    }

    private MethodKind classifyCountMethod(String methodName, List<ParameterInfo> params) {
        // count() without parameters
        if (methodName.equals("count") && params.isEmpty()) {
            return MethodKind.COUNT_ALL;
        }

        // countBy... (property-based)
        if (methodName.startsWith("countBy")) {
            return MethodKind.COUNT_BY_PROPERTY;
        }

        return MethodKind.CUSTOM;
    }

    private MethodKind classifyDeleteMethod(
            String methodName, List<ParameterInfo> params, Optional<Identity> aggregateIdentity) {

        // deleteAll without parameters
        if ((methodName.equals("deleteAll") || methodName.equals("removeAll")) && params.isEmpty()) {
            return MethodKind.DELETE_ALL;
        }

        // deleteById
        if (isDeleteByIdMethod(methodName) && hasIdentityParameter(params, aggregateIdentity)) {
            return MethodKind.DELETE_BY_ID;
        }

        // deleteBy... (property-based)
        if (methodName.startsWith("deleteBy") || methodName.startsWith("removeBy")) {
            String byPart = methodName.startsWith("deleteBy")
                    ? methodName.substring("deleteBy".length())
                    : methodName.substring("removeBy".length());
            if (!byPart.isEmpty() && !byPart.equals("Id")) {
                return MethodKind.DELETE_BY_PROPERTY;
            }
        }

        return MethodKind.CUSTOM;
    }

    private MethodKind classifyStreamMethod(String methodName, List<ParameterInfo> params) {
        // streamAll
        if (methodName.equals("streamAll") && params.isEmpty()) {
            return MethodKind.STREAM_ALL;
        }

        // streamBy... (property-based)
        if (methodName.startsWith("streamBy")) {
            return MethodKind.STREAM_BY_PROPERTY;
        }

        return MethodKind.CUSTOM;
    }

    /**
     * Extracts target properties from a method name for property-based queries.
     *
     * <p>Examples:
     * <ul>
     *   <li>"findByEmail" -> ["email"]</li>
     *   <li>"findByFirstNameAndLastName" -> ["firstName", "lastName"]</li>
     *   <li>"existsByStatusOrPriority" -> ["status", "priority"]</li>
     * </ul>
     *
     * @param methodName the method name
     * @param kind the classified method kind
     * @return list of target property names
     */
    List<String> extractTargetProperties(String methodName, MethodKind kind) {
        // Only extract properties for property-based methods
        if (!isPropertyBasedKind(kind)) {
            return List.of();
        }

        String propertyPart = extractPropertyPart(methodName);
        if (propertyPart == null || propertyPart.isEmpty()) {
            return List.of();
        }

        return parseProperties(propertyPart);
    }

    private String extractPropertyPart(String methodName) {
        // Remove OrderBy clause if present
        String cleanName = methodName.replaceFirst("OrderBy[A-Z][a-zA-Z0-9]*(Asc|Desc)?$", "");

        // Find the "By" keyword and extract what follows
        int byIndex = findByKeywordIndex(cleanName);
        if (byIndex < 0) {
            return null;
        }

        return cleanName.substring(byIndex + 2);
    }

    private int findByKeywordIndex(String methodName) {
        // Handle special cases like "findAllBy", "findFirstBy", "findTop10By"
        for (String prefix :
                List.of("findAllBy", "findFirstBy", "existsBy", "countBy", "deleteBy", "removeBy", "streamBy")) {
            if (methodName.startsWith(prefix)) {
                return methodName.indexOf("By");
            }
        }

        // Handle findTopNBy
        Matcher topMatcher = Pattern.compile("^(find|get|read|query|search)(Top|First)\\d*By")
                .matcher(methodName);
        if (topMatcher.find()) {
            return topMatcher.end() - 2;
        }

        // Standard findBy, getBy, etc.
        for (String prefix : FIND_PREFIXES) {
            String byPrefix = prefix + "By";
            if (methodName.startsWith(byPrefix)) {
                return prefix.length();
            }
        }

        return -1;
    }

    private List<String> parseProperties(String propertyPart) {
        List<String> properties = new ArrayList<>();

        // Split by And/Or (case sensitive, must be followed by uppercase letter)
        String[] parts = propertyPart.split("(?=And[A-Z])|(?=Or[A-Z])");

        for (String part : parts) {
            String cleanPart = part;
            // Remove leading And/Or
            if (cleanPart.startsWith("And")) {
                cleanPart = cleanPart.substring(3);
            } else if (cleanPart.startsWith("Or")) {
                cleanPart = cleanPart.substring(2);
            }

            // Remove trailing operators (Is, Equals, Not, In, etc.)
            cleanPart = removeTrailingOperators(cleanPart);

            if (!cleanPart.isEmpty()) {
                // Convert to camelCase (first char lowercase)
                properties.add(Character.toLowerCase(cleanPart.charAt(0)) + cleanPart.substring(1));
            }
        }

        return properties;
    }

    private String removeTrailingOperators(String property) {
        // Remove trailing comparison operators
        String[] operators = {
            "IsNull",
            "IsNotNull",
            "NotNull",
            "Null",
            "IsTrue",
            "IsFalse",
            "True",
            "False",
            "LessThan",
            "LessThanEqual",
            "GreaterThan",
            "GreaterThanEqual",
            "Before",
            "After",
            "Between",
            "Like",
            "NotLike",
            "StartingWith",
            "EndingWith",
            "Containing",
            "NotContaining",
            "In",
            "NotIn",
            "IgnoreCase",
            "IgnoringCase",
            "IsNot",
            "Not",
            "Is",
            "Equals"
        };

        String result = property;
        for (String op : operators) {
            if (result.endsWith(op) && result.length() > op.length()) {
                // Make sure we're not cutting off part of the property name
                String potential = result.substring(0, result.length() - op.length());
                if (!potential.isEmpty() && Character.isUpperCase(potential.charAt(potential.length() - 1))) {
                    continue; // This might be part of the property name
                }
                result = potential;
                break;
            }
        }

        return result;
    }

    /**
     * Extracts query modifiers from a method name.
     */
    Set<QueryModifier> extractModifiers(String methodName) {
        Set<QueryModifier> modifiers = EnumSet.noneOf(QueryModifier.class);

        if (methodName.contains("Distinct")) {
            modifiers.add(QueryModifier.DISTINCT);
        }

        if (methodName.contains("AllIgnoreCase") || methodName.contains("AllIgnoringCase")) {
            modifiers.add(QueryModifier.ALL_IGNORE_CASE);
        } else if (methodName.contains("IgnoreCase") || methodName.contains("IgnoringCase")) {
            modifiers.add(QueryModifier.IGNORE_CASE);
        }

        Matcher orderMatcher = ORDER_BY_PATTERN.matcher(methodName);
        if (orderMatcher.matches()) {
            String direction = orderMatcher.group(2);
            if ("Desc".equals(direction)) {
                modifiers.add(QueryModifier.ORDER_BY_DESC);
            } else {
                modifiers.add(QueryModifier.ORDER_BY_ASC);
            }
        }

        return modifiers;
    }

    /**
     * Extracts the limit size from Top/First queries.
     */
    Optional<Integer> extractLimitSize(String methodName) {
        Matcher matcher = TOP_N_PATTERN.matcher(methodName);
        if (matcher.matches()) {
            try {
                return Optional.of(Integer.parseInt(matcher.group(3)));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    /**
     * Extracts the OrderBy property from a method name.
     */
    Optional<String> extractOrderByProperty(String methodName) {
        int orderByIndex = methodName.indexOf("OrderBy");
        if (orderByIndex < 0) {
            return Optional.empty();
        }

        String afterOrderBy = methodName.substring(orderByIndex + 7); // after "OrderBy"
        if (afterOrderBy.isEmpty()) {
            return Optional.empty();
        }

        // Remove trailing Asc/Desc
        String property = afterOrderBy;
        if (property.endsWith("Asc")) {
            property = property.substring(0, property.length() - 3);
        } else if (property.endsWith("Desc")) {
            property = property.substring(0, property.length() - 4);
        }

        if (property.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(Character.toLowerCase(property.charAt(0)) + property.substring(1));
    }

    // === Helper methods ===

    private boolean startsWithAny(String methodName, List<String> prefixes) {
        return prefixes.stream().anyMatch(methodName::startsWith);
    }

    private boolean isIdBasedMethod(String methodName, String modifier) {
        for (String prefix : FIND_PREFIXES) {
            String pattern = prefix + modifier + "ById";
            if (methodName.equals(pattern)) {
                return true;
            }
        }
        return false;
    }

    private boolean isDeleteByIdMethod(String methodName) {
        return methodName.equals("deleteById") || methodName.equals("removeById");
    }

    private boolean isFirstOrTopMethod(String methodName) {
        // Use pattern to ensure First/Top follows directly after the find/get/etc. prefix
        return FIRST_TOP_PATTERN.matcher(methodName).matches();
    }

    private boolean isPropertyBasedAllMethod(String methodName) {
        for (String prefix : FIND_PREFIXES) {
            if (methodName.startsWith(prefix + "AllBy") && !methodName.equals(prefix + "AllById")) {
                return true;
            }
        }
        return false;
    }

    private boolean hasPropertySuffix(String methodName) {
        for (String prefix : FIND_PREFIXES) {
            String byPrefix = prefix + "By";
            if (methodName.startsWith(byPrefix) && methodName.length() > byPrefix.length()) {
                String suffix = methodName.substring(byPrefix.length());
                // Must start with uppercase (property name) and not be just "Id"
                return !suffix.isEmpty() && Character.isUpperCase(suffix.charAt(0)) && !suffix.equals("Id");
            }
        }
        return false;
    }

    private boolean hasIdentityParameter(List<ParameterInfo> params, Optional<Identity> identity) {
        if (params.isEmpty()) {
            return false;
        }
        if (identity.isEmpty()) {
            // Without identity info, check if single parameter is a common ID type
            return params.size() == 1 && isCommonIdType(params.get(0).typeQualifiedName());
        }

        String idType = identity.get().type().qualifiedName();
        String unwrappedIdType = identity.get().unwrappedType().qualifiedName();
        String firstParamType = params.get(0).typeQualifiedName();

        return firstParamType.equals(idType) || firstParamType.equals(unwrappedIdType);
    }

    private boolean hasCollectionParameter(List<ParameterInfo> params) {
        if (params.isEmpty()) {
            return false;
        }
        return params.get(0).type().isCollectionLike();
    }

    private boolean isCommonIdType(String typeName) {
        return typeName.equals("java.util.UUID")
                || typeName.equals("java.lang.Long")
                || typeName.equals("java.lang.Integer")
                || typeName.equals("java.lang.String")
                || typeName.equals("long")
                || typeName.equals("int");
    }

    private boolean isPropertyBasedKind(MethodKind kind) {
        return kind == MethodKind.FIND_BY_PROPERTY
                || kind == MethodKind.FIND_ALL_BY_PROPERTY
                || kind == MethodKind.EXISTS_BY_PROPERTY
                || kind == MethodKind.COUNT_BY_PROPERTY
                || kind == MethodKind.DELETE_BY_PROPERTY
                || kind == MethodKind.STREAM_BY_PROPERTY
                || kind == MethodKind.FIND_FIRST
                || kind == MethodKind.FIND_TOP_N;
    }

    private String removeModifiers(String methodName) {
        // Remove Distinct
        String result = methodName.replace("Distinct", "");

        // Remove IgnoreCase variations
        result = result.replace("AllIgnoreCase", "").replace("AllIgnoringCase", "");
        result = result.replace("IgnoreCase", "").replace("IgnoringCase", "");

        // Don't remove OrderBy as it's needed for property extraction

        return result;
    }
}
