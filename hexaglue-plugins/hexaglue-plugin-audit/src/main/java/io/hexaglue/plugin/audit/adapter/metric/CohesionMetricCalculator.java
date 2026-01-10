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

package io.hexaglue.plugin.audit.adapter.metric;

import io.hexaglue.plugin.audit.domain.model.Metric;
import io.hexaglue.plugin.audit.domain.model.MetricThreshold;
import io.hexaglue.plugin.audit.domain.port.driving.MetricCalculator;
import io.hexaglue.spi.audit.CodeUnit;
import io.hexaglue.spi.audit.Codebase;
import io.hexaglue.spi.audit.FieldDeclaration;
import io.hexaglue.spi.audit.MethodDeclaration;
import io.hexaglue.spi.audit.RoleClassification;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Calculates LCOM4 (Lack of Cohesion of Methods) for aggregate roots.
 *
 * <p>LCOM4 measures cohesion within a class by building a graph where methods are nodes
 * and edges connect methods that share instance variables. The metric is the number of
 * connected components in this graph.
 *
 * <p><strong>Note:</strong> Since the SPI does not expose method body analysis
 * (field access information), this implementation uses a heuristic approach based on
 * method naming patterns and field types to estimate field usage. This provides a
 * simplified approximation of LCOM4.
 *
 * <p><strong>Metric:</strong> aggregate.cohesion.lcom4<br>
 * <strong>Unit:</strong> components<br>
 * <strong>Threshold:</strong> Warning if > 2 components<br>
 * <strong>Interpretation:</strong> Lower is better. LCOM4 = 1 indicates perfect cohesion
 * (all methods work together). Higher values suggest the class may need to be split into
 * multiple classes with distinct responsibilities.
 *
 * <h2>LCOM4 Algorithm</h2>
 * <ol>
 *   <li>Build a graph where nodes are methods</li>
 *   <li>Connect two methods if they share instance variables (estimated via heuristics)</li>
 *   <li>Count connected components using union-find</li>
 *   <li>LCOM4 = number of connected components</li>
 * </ol>
 *
 * <h2>Heuristic Approach</h2>
 * <p>To estimate which methods access which fields without method body analysis:
 * <ul>
 *   <li>Getter/setter methods: Assumed to access the field matching their name</li>
 *   <li>Methods with parameter types matching field types: Assumed to access those fields</li>
 *   <li>Methods with return types matching field types: Assumed to access those fields</li>
 *   <li>Constructors: Assumed to access all fields</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class CohesionMetricCalculator implements MetricCalculator {

    private static final String METRIC_NAME = "aggregate.cohesion.lcom4";
    private static final double WARNING_THRESHOLD = 2.0;

    @Override
    public String metricName() {
        return METRIC_NAME;
    }

    @Override
    public Metric calculate(Codebase codebase) {
        List<CodeUnit> aggregates = codebase.unitsWithRole(RoleClassification.AGGREGATE_ROOT);

        if (aggregates.isEmpty()) {
            return Metric.of(
                    METRIC_NAME, 0.0, "components", "Average LCOM4 cohesion for aggregates (no aggregates found)");
        }

        double avgLcom4 = aggregates.stream()
                .filter(this::hasMethodsAndFields)
                .mapToInt(this::calculateLcom4)
                .average()
                .orElse(1.0); // Default to 1 if no valid aggregates

        return Metric.of(
                METRIC_NAME,
                avgLcom4,
                "components",
                "Average LCOM4 cohesion for aggregates",
                MetricThreshold.greaterThan(WARNING_THRESHOLD));
    }

    /**
     * Checks if the code unit has both methods and fields.
     *
     * @param unit the code unit
     * @return true if the unit has at least one method and one field
     */
    private boolean hasMethodsAndFields(CodeUnit unit) {
        return !unit.methods().isEmpty() && !unit.fields().isEmpty();
    }

    /**
     * Calculates LCOM4 for a single aggregate.
     *
     * <p>LCOM4 is the number of connected components in the method-field graph.
     *
     * @param aggregate the aggregate to analyze
     * @return the LCOM4 value (1 = cohesive, >1 = lacks cohesion)
     */
    private int calculateLcom4(CodeUnit aggregate) {
        List<MethodDeclaration> methods = aggregate.methods();
        List<FieldDeclaration> fields = aggregate.fields();

        if (methods.isEmpty() || fields.isEmpty()) {
            return 1; // Cohesive by default if no methods or fields
        }

        // Build method-to-fields mapping using heuristics
        Map<MethodDeclaration, Set<FieldDeclaration>> methodFieldAccess = estimateFieldAccess(methods, fields);

        // Build connectivity graph and find connected components
        return countConnectedComponents(methods, methodFieldAccess);
    }

    /**
     * Estimates which fields each method accesses using heuristics.
     *
     * @param methods the methods to analyze
     * @param fields  the fields in the class
     * @return map of method to set of fields it likely accesses
     */
    private Map<MethodDeclaration, Set<FieldDeclaration>> estimateFieldAccess(
            List<MethodDeclaration> methods, List<FieldDeclaration> fields) {

        Map<MethodDeclaration, Set<FieldDeclaration>> access = new HashMap<>();

        for (MethodDeclaration method : methods) {
            Set<FieldDeclaration> accessedFields = new HashSet<>();

            // Heuristic 1: Getter/setter pattern
            accessedFields.addAll(matchByGetterSetter(method, fields));

            // Heuristic 2: Type matching (parameters and return type)
            accessedFields.addAll(matchByType(method, fields));

            // Heuristic 3: Constructors access all fields
            if (isConstructor(method)) {
                accessedFields.addAll(fields);
            }

            access.put(method, accessedFields);
        }

        return access;
    }

    /**
     * Matches fields by getter/setter naming convention.
     *
     * @param method the method to check
     * @param fields the fields to match against
     * @return set of fields matching the getter/setter pattern
     */
    private Set<FieldDeclaration> matchByGetterSetter(MethodDeclaration method, List<FieldDeclaration> fields) {

        Set<FieldDeclaration> matched = new HashSet<>();
        String methodName = method.name();

        if (methodName.startsWith("get") || methodName.startsWith("set") || methodName.startsWith("is")) {
            String fieldName = extractFieldNameFromAccessor(methodName);
            fields.stream()
                    .filter(f -> f.name().equalsIgnoreCase(fieldName))
                    .findFirst()
                    .ifPresent(matched::add);
        }

        return matched;
    }

    /**
     * Extracts field name from accessor method name.
     *
     * @param methodName the method name (e.g., "getName", "setAge", "isActive")
     * @return the field name (e.g., "name", "age", "active")
     */
    private String extractFieldNameFromAccessor(String methodName) {
        if (methodName.startsWith("is")) {
            return decapitalize(methodName.substring(2));
        }
        if (methodName.startsWith("get") || methodName.startsWith("set")) {
            return decapitalize(methodName.substring(3));
        }
        return methodName;
    }

    /**
     * Decapitalizes the first letter of a string.
     *
     * @param str the string to decapitalize
     * @return the decapitalized string
     */
    private String decapitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }

    /**
     * Matches fields by type (parameter types or return type).
     *
     * <p>This only matches when the return type or parameter type exactly matches
     * a field type AND is not a common primitive wrapper type that could create
     * false connections.
     *
     * @param method the method to check
     * @param fields the fields to match against
     * @return set of fields with matching types
     */
    private Set<FieldDeclaration> matchByType(MethodDeclaration method, List<FieldDeclaration> fields) {

        Set<FieldDeclaration> matched = new HashSet<>();

        // Only match by type if it's not a common primitive/wrapper
        // (to avoid false positives where unrelated methods share common types)

        // Match by return type (if non-primitive)
        if (!isCommonType(method.returnType())) {
            fields.stream().filter(f -> f.type().equals(method.returnType())).forEach(matched::add);
        }

        // Match by parameter types (if non-primitive)
        for (String paramType : method.parameterTypes()) {
            if (!isCommonType(paramType)) {
                fields.stream().filter(f -> f.type().equals(paramType)).forEach(matched::add);
            }
        }

        return matched;
    }

    /**
     * Checks if a type is a common primitive or wrapper type.
     *
     * <p>Common types should not be used for field matching as they create
     * false positives.
     *
     * @param type the type to check
     * @return true if the type is common (primitive, wrapper, String)
     */
    private boolean isCommonType(String type) {
        if (type == null || type.isEmpty()) {
            return true;
        }

        return type.equals("void")
                || type.equals("boolean")
                || type.equals("byte")
                || type.equals("char")
                || type.equals("short")
                || type.equals("int")
                || type.equals("long")
                || type.equals("float")
                || type.equals("double")
                || type.equals("java.lang.Boolean")
                || type.equals("java.lang.Byte")
                || type.equals("java.lang.Character")
                || type.equals("java.lang.Short")
                || type.equals("java.lang.Integer")
                || type.equals("java.lang.Long")
                || type.equals("java.lang.Float")
                || type.equals("java.lang.Double")
                || type.equals("java.lang.String")
                || type.equals("java.lang.Object");
    }

    /**
     * Checks if a method is a constructor.
     *
     * <p>A constructor is identified by having a null or empty return type.
     * Note that "void" is NOT a constructor - it's a regular method with no return value.
     *
     * @param method the method to check
     * @return true if the method appears to be a constructor
     */
    private boolean isConstructor(MethodDeclaration method) {
        // Constructors have no return type (null or empty string)
        // Methods with "void" return type are NOT constructors
        return method.returnType() == null || method.returnType().isEmpty();
    }

    /**
     * Counts connected components in the method connectivity graph.
     *
     * <p>Uses a union-find approach where two methods are connected if they
     * share at least one field access.
     *
     * @param methods           the methods in the class
     * @param methodFieldAccess the method-to-fields mapping
     * @return the number of connected components
     */
    private int countConnectedComponents(
            List<MethodDeclaration> methods, Map<MethodDeclaration, Set<FieldDeclaration>> methodFieldAccess) {

        int methodCount = methods.size();
        UnionFind uf = new UnionFind(methodCount);

        // Connect methods that share fields
        for (int i = 0; i < methodCount; i++) {
            for (int j = i + 1; j < methodCount; j++) {
                MethodDeclaration method1 = methods.get(i);
                MethodDeclaration method2 = methods.get(j);

                Set<FieldDeclaration> fields1 = methodFieldAccess.getOrDefault(method1, Set.of());
                Set<FieldDeclaration> fields2 = methodFieldAccess.getOrDefault(method2, Set.of());

                if (sharesFields(fields1, fields2)) {
                    uf.union(i, j);
                }
            }
        }

        return uf.countComponents();
    }

    /**
     * Checks if two field sets have any common fields.
     *
     * @param fields1 the first field set
     * @param fields2 the second field set
     * @return true if the sets share at least one field
     */
    private boolean sharesFields(Set<FieldDeclaration> fields1, Set<FieldDeclaration> fields2) {
        if (fields1.isEmpty() || fields2.isEmpty()) {
            return false;
        }

        Set<FieldDeclaration> intersection = new HashSet<>(fields1);
        intersection.retainAll(fields2);
        return !intersection.isEmpty();
    }

    /**
     * Union-Find data structure for tracking connected components.
     */
    private static class UnionFind {
        private final int[] parent;
        private final int[] rank;
        private int componentCount;

        UnionFind(int size) {
            parent = new int[size];
            rank = new int[size];
            componentCount = size;

            for (int i = 0; i < size; i++) {
                parent[i] = i;
                rank[i] = 0;
            }
        }

        int find(int x) {
            if (parent[x] != x) {
                parent[x] = find(parent[x]); // Path compression
            }
            return parent[x];
        }

        void union(int x, int y) {
            int rootX = find(x);
            int rootY = find(y);

            if (rootX != rootY) {
                // Union by rank
                if (rank[rootX] < rank[rootY]) {
                    parent[rootX] = rootY;
                } else if (rank[rootX] > rank[rootY]) {
                    parent[rootY] = rootX;
                } else {
                    parent[rootY] = rootX;
                    rank[rootX]++;
                }
                componentCount--;
            }
        }

        int countComponents() {
            return componentCount;
        }
    }
}
