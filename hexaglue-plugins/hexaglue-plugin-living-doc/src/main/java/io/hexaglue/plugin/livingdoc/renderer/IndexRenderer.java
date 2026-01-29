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

package io.hexaglue.plugin.livingdoc.renderer;

import io.hexaglue.arch.model.ArchKind;
import io.hexaglue.plugin.livingdoc.markdown.MarkdownBuilder;
import io.hexaglue.plugin.livingdoc.markdown.TableBuilder;
import io.hexaglue.plugin.livingdoc.model.GlossaryEntry;
import io.hexaglue.plugin.livingdoc.util.MarkdownUtil;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Renders a type index organized by architectural kind.
 *
 * <p>Each kind gets its own sub-section with a table linking to the
 * corresponding documentation file ({@code domain.md} or {@code ports.md}).
 *
 * @since 5.0.0
 */
public final class IndexRenderer {

    /**
     * Renders a type index section from the given glossary entries.
     *
     * <p>Returns an empty string if the list is empty, so it can be safely
     * appended without producing orphan headers.
     *
     * @param entries the glossary entries to index
     * @return the Markdown content, or empty string if no entries
     * @since 5.0.0
     */
    public String renderTypeIndex(List<GlossaryEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return "";
        }

        MarkdownBuilder md = new MarkdownBuilder();
        md.h2("Index des Types");

        // Group by kind, preserving a display-friendly order
        Map<ArchKind, List<GlossaryEntry>> byKind = entries.stream()
                .sorted(Comparator.comparing(GlossaryEntry::term, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.groupingBy(GlossaryEntry::archKind, LinkedHashMap::new, Collectors.toList()));

        // Render in a fixed display order
        for (ArchKind kind : DISPLAY_ORDER) {
            List<GlossaryEntry> kindEntries = byKind.get(kind);
            if (kindEntries == null || kindEntries.isEmpty()) {
                continue;
            }

            String sectionTitle = pluralLabel(kind);
            md.h3(sectionTitle);

            TableBuilder table = md.table("Type", "Package", "Link");
            for (GlossaryEntry entry : kindEntries) {
                String anchor = MarkdownUtil.toAnchor(entry.term());
                String targetFile = targetFileFor(kind);
                String link = "[" + entry.term() + "](./" + targetFile + "#" + anchor + ")";

                table.row(entry.term(), "`" + entry.packageName() + "`", link);
            }
            table.end();
        }

        return md.build();
    }

    /**
     * Display order for architectural kinds in the index.
     */
    private static final List<ArchKind> DISPLAY_ORDER = List.of(
            ArchKind.AGGREGATE_ROOT,
            ArchKind.ENTITY,
            ArchKind.VALUE_OBJECT,
            ArchKind.IDENTIFIER,
            ArchKind.DOMAIN_EVENT,
            ArchKind.DOMAIN_SERVICE,
            ArchKind.APPLICATION_SERVICE,
            ArchKind.COMMAND_HANDLER,
            ArchKind.QUERY_HANDLER,
            ArchKind.DRIVING_PORT,
            ArchKind.DRIVEN_PORT,
            ArchKind.UNCLASSIFIED);

    /**
     * Returns the documentation file for a given kind.
     */
    private static String targetFileFor(ArchKind kind) {
        return switch (kind) {
            case DRIVING_PORT, DRIVEN_PORT -> "ports.md";
            default -> "domain.md";
        };
    }

    /**
     * Returns the plural section label for a kind.
     */
    private static String pluralLabel(ArchKind kind) {
        return switch (kind) {
            case AGGREGATE_ROOT -> "Aggregate Roots";
            case ENTITY -> "Entities";
            case VALUE_OBJECT -> "Value Objects";
            case IDENTIFIER -> "Identifiers";
            case DOMAIN_EVENT -> "Domain Events";
            case DOMAIN_SERVICE -> "Domain Services";
            case APPLICATION_SERVICE -> "Application Services";
            case COMMAND_HANDLER -> "Command Handlers";
            case QUERY_HANDLER -> "Query Handlers";
            case DRIVING_PORT -> "Driving Ports";
            case DRIVEN_PORT -> "Driven Ports";
            case UNCLASSIFIED -> "Unclassified";
        };
    }
}
