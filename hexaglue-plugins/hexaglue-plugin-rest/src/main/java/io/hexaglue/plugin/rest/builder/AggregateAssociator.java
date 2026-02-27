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

package io.hexaglue.plugin.rest.builder;

import io.hexaglue.arch.model.AggregateRoot;
import io.hexaglue.arch.model.DrivingPort;
import io.hexaglue.arch.model.Parameter;
import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.UseCase;
import io.hexaglue.arch.model.index.DomainIndex;
import io.hexaglue.plugin.rest.util.NamingConventions;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Associates a {@link DrivingPort} with its primary {@link AggregateRoot}.
 *
 * <p>Uses two strategies in order:
 * <ol>
 *   <li>Naming convention: strip port suffix, match against aggregate simple names</li>
 *   <li>Type analysis fallback: score return types (weight 2) and parameter types (weight 1)</li>
 * </ol>
 *
 * @since 3.1.0
 */
public final class AggregateAssociator {

    private final DomainIndex domainIndex;

    /**
     * Creates an associator backed by the given domain index.
     *
     * @param domainIndex the domain index for aggregate root lookup
     * @throws NullPointerException if domainIndex is null
     */
    public AggregateAssociator(DomainIndex domainIndex) {
        this.domainIndex = Objects.requireNonNull(domainIndex, "domainIndex is required");
    }

    /**
     * Associates a driving port to its primary aggregate root.
     *
     * @param port the driving port
     * @return the associated aggregate root, or empty if no match found
     */
    public Optional<AggregateRoot> associate(DrivingPort port) {
        // 1. Naming convention: strip suffix from port name, match against aggregate simple names
        String prefix = NamingConventions.stripSuffix(port.id().simpleName());
        Optional<AggregateRoot> byName = domainIndex
                .aggregateRoots()
                .filter(agg -> agg.id().simpleName().equals(prefix))
                .findFirst();
        if (byName.isPresent()) {
            return byName;
        }

        // 2. Fallback: type analysis on return types (weight 2) and param types (weight 1)
        return associateByTypeAnalysis(port);
    }

    private Optional<AggregateRoot> associateByTypeAnalysis(DrivingPort port) {
        Set<String> aggregateNames = domainIndex
                .aggregateRoots()
                .map(agg -> agg.id().qualifiedName())
                .collect(Collectors.toSet());

        Map<String, Integer> scores = new HashMap<>();
        for (UseCase uc : port.useCases()) {
            String returnType = uc.method().returnType().qualifiedName();
            if (aggregateNames.contains(returnType)) {
                scores.merge(returnType, 2, Integer::sum);
            }
            for (Parameter param : uc.method().parameters()) {
                String paramType = param.type().qualifiedName();
                if (aggregateNames.contains(paramType)) {
                    scores.merge(paramType, 1, Integer::sum);
                }
            }
        }

        return scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .flatMap(entry -> domainIndex.aggregateRoot(TypeId.of(entry.getKey())));
    }
}
