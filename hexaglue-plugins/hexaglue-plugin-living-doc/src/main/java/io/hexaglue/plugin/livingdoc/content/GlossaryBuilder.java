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
import io.hexaglue.arch.model.TypeRegistry;
import io.hexaglue.plugin.livingdoc.model.GlossaryEntry;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Builds a glossary of domain terms from the architectural model.
 *
 * <p>Each classified type becomes a glossary entry. The definition is extracted
 * from the type's Javadoc when available; otherwise a fallback label combining
 * the simple name and architectural kind is used.
 *
 * <p>Results are computed and cached at construction time (immutable).
 *
 * @since 5.0.0
 */
public final class GlossaryBuilder {

    private final List<GlossaryEntry> entries;

    /**
     * Creates a glossary builder that extracts entries from all types in the model.
     *
     * @param model the architectural model
     * @throws IllegalStateException if the model does not contain a TypeRegistry
     * @since 5.0.0
     */
    public GlossaryBuilder(ArchitecturalModel model) {
        Objects.requireNonNull(model, "model must not be null");

        TypeRegistry registry = model.typeRegistry()
                .orElseThrow(() -> new IllegalStateException("TypeRegistry required for glossary building"));

        this.entries = registry.all()
                .map(GlossaryBuilder::toEntry)
                .sorted(Comparator.comparing(GlossaryEntry::term, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Returns all glossary entries, sorted alphabetically by term.
     *
     * @return an unmodifiable list of glossary entries
     * @since 5.0.0
     */
    public List<GlossaryEntry> buildAll() {
        return entries;
    }

    /**
     * Groups glossary entries by bounded context using the given detector.
     *
     * @param detector the bounded context detector
     * @return entries grouped by context name (sorted by context name)
     * @since 5.0.0
     */
    public Map<String, List<GlossaryEntry>> byContext(BoundedContextDetector detector) {
        Objects.requireNonNull(detector, "detector must not be null");

        return entries.stream()
                .collect(Collectors.groupingBy(
                        entry -> detector.contextOf(io.hexaglue.arch.model.TypeId.of(entry.qualifiedName()))
                                .orElse("default"),
                        java.util.TreeMap::new,
                        Collectors.toUnmodifiableList()));
    }

    /**
     * Converts an ArchType to a GlossaryEntry.
     */
    private static GlossaryEntry toEntry(ArchType type) {
        String term = type.simpleName();
        String definition =
                type.structure().documentation().orElseGet(() -> term + " (" + formatKindLabel(type.kind()) + ")");
        return new GlossaryEntry(term, definition, type.kind(), type.qualifiedName(), type.packageName());
    }

    /**
     * Converts an ArchKind to a human-readable label.
     *
     * @param kind the architectural kind
     * @return a readable label (e.g., "Aggregate Root")
     */
    public static String formatKindLabel(ArchKind kind) {
        return switch (kind) {
            case AGGREGATE_ROOT -> "Aggregate Root";
            case ENTITY -> "Entity";
            case VALUE_OBJECT -> "Value Object";
            case IDENTIFIER -> "Identifier";
            case DOMAIN_EVENT -> "Domain Event";
            case DOMAIN_SERVICE -> "Domain Service";
            case DRIVING_PORT -> "Driving Port";
            case DRIVEN_PORT -> "Driven Port";
            case APPLICATION_SERVICE -> "Application Service";
            case COMMAND_HANDLER -> "Command Handler";
            case QUERY_HANDLER -> "Query Handler";
            case UNCLASSIFIED -> "Unclassified";
        };
    }
}
