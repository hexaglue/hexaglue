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

package io.hexaglue.plugin.livingdoc.content;

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.model.ArchKind;
import io.hexaglue.arch.model.ArchType;
import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.TypeRegistry;
import io.hexaglue.plugin.livingdoc.model.BoundedContextDoc;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Detects bounded contexts from package analysis.
 *
 * <p>Heuristic: the third segment of the package name is considered
 * the bounded context name. For example, {@code com.example.order.domain.Order}
 * belongs to the {@code order} bounded context.
 *
 * <p>Types with fewer than 3 package segments are grouped under a default context.
 *
 * <p>Results are computed and cached at construction time (immutable).
 *
 * @since 5.0.0
 */
public final class BoundedContextDetector {

    private final List<BoundedContextDoc> contexts;
    private final Map<String, String> typeToContext;

    /**
     * Creates a detector that analyzes all types in the model.
     *
     * @param model the architectural model
     * @throws IllegalStateException if the model does not contain a TypeRegistry
     * @since 5.0.0
     */
    public BoundedContextDetector(ArchitecturalModel model) {
        Objects.requireNonNull(model, "model must not be null");

        TypeRegistry registry = model.typeRegistry()
                .orElseThrow(() -> new IllegalStateException("TypeRegistry required for bounded context detection"));

        Map<String, List<ArchType>> grouped = registry.all()
                .collect(Collectors.groupingBy(
                        type -> extractContextName(type.id()), LinkedHashMap::new, Collectors.toList()));

        this.typeToContext = new LinkedHashMap<>();
        List<BoundedContextDoc> detected = new ArrayList<>();

        for (Map.Entry<String, List<ArchType>> entry : grouped.entrySet()) {
            String contextName = entry.getKey();
            List<ArchType> types = entry.getValue();

            String rootPackage = findRootPackage(types, contextName);

            int aggregateCount = 0;
            int entityCount = 0;
            int valueObjectCount = 0;
            int applicationServiceCount = 0;
            int portCount = 0;
            List<String> typeNames = new ArrayList<>();

            for (ArchType type : types) {
                typeNames.add(type.simpleName());
                typeToContext.put(type.id().qualifiedName(), contextName);

                ArchKind kind = type.kind();
                switch (kind) {
                    case AGGREGATE_ROOT -> aggregateCount++;
                    case ENTITY -> entityCount++;
                    case VALUE_OBJECT -> valueObjectCount++;
                    case APPLICATION_SERVICE -> applicationServiceCount++;
                    case DRIVING_PORT, DRIVEN_PORT -> portCount++;
                    default -> {
                        // other kinds counted in total only
                    }
                }
            }

            detected.add(new BoundedContextDoc(
                    contextName,
                    rootPackage,
                    aggregateCount,
                    entityCount,
                    valueObjectCount,
                    applicationServiceCount,
                    portCount,
                    types.size(),
                    typeNames));
        }

        detected.sort(Comparator.comparing(BoundedContextDoc::name));
        this.contexts = List.copyOf(detected);
    }

    /**
     * Returns all detected bounded contexts, sorted by name.
     *
     * @return an unmodifiable list of bounded contexts
     * @since 5.0.0
     */
    public List<BoundedContextDoc> detectAll() {
        return contexts;
    }

    /**
     * Returns the bounded context name for a given type.
     *
     * @param typeId the type identifier
     * @return the bounded context name, or empty if the type is not known
     * @since 5.0.0
     */
    public Optional<String> contextOf(TypeId typeId) {
        Objects.requireNonNull(typeId, "typeId must not be null");
        return Optional.ofNullable(typeToContext.get(typeId.qualifiedName()));
    }

    /**
     * Extracts the bounded context name from a type's package.
     *
     * <p>Uses the third segment of the package name. For packages with fewer
     * than 3 segments, uses the last segment.
     *
     * @param typeId the type identifier
     * @return the context name
     */
    static String extractContextName(TypeId typeId) {
        String packageName = typeId.packageName();
        if (packageName == null || packageName.isEmpty()) {
            return "default";
        }
        String[] segments = packageName.split("\\.");
        if (segments.length >= 3) {
            return segments[2];
        }
        return segments[segments.length - 1];
    }

    /**
     * Finds the common root package for types in a context.
     */
    private static String findRootPackage(List<ArchType> types, String contextName) {
        if (types.isEmpty()) {
            return contextName;
        }
        // Find shortest package that contains the context name
        return types.stream()
                .map(t -> t.id().packageName())
                .filter(p -> p != null && !p.isEmpty())
                .min(Comparator.comparingInt(String::length))
                .orElse(contextName);
    }
}
