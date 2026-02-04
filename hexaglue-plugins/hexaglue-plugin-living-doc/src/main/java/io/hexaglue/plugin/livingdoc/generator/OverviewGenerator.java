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

package io.hexaglue.plugin.livingdoc.generator;

import io.hexaglue.plugin.livingdoc.markdown.MarkdownBuilder;
import io.hexaglue.plugin.livingdoc.markdown.TableBuilder;
import io.hexaglue.plugin.livingdoc.model.BoundedContextDoc;
import io.hexaglue.plugin.livingdoc.model.DocumentationModel;
import io.hexaglue.plugin.livingdoc.model.DocumentationModel.DocPort;
import io.hexaglue.plugin.livingdoc.model.DocumentationModel.DocType;
import io.hexaglue.plugin.livingdoc.model.GlossaryEntry;
import io.hexaglue.plugin.livingdoc.renderer.BoundedContextRenderer;
import io.hexaglue.plugin.livingdoc.renderer.IndexRenderer;
import io.hexaglue.plugin.livingdoc.util.PluginVersion;
import java.util.List;
import java.util.Objects;

/**
 * Generates the architecture overview documentation.
 *
 * <p>Since 5.0.0, the overview diagram is received pre-rendered from
 * {@link io.hexaglue.plugin.livingdoc.model.LivingDocDiagramSet}
 * rather than being generated internally.
 *
 * @since 4.0.0 - Migrated to DocumentationModel abstraction
 * @since 5.0.0 - Receives pre-rendered overview diagram from LivingDocDiagramSet;
 *     added Glossary, Structure, and Index sections
 */
public final class OverviewGenerator {

    private final DocumentationModel model;
    private final String overviewDiagram;
    private final List<BoundedContextDoc> boundedContexts;
    private final List<GlossaryEntry> glossaryEntries;
    private final String packageTree;
    private final IndexRenderer indexRenderer;

    /**
     * Creates an OverviewGenerator from a DocumentationModel and optional pre-rendered diagram.
     *
     * @param model the documentation model
     * @param overviewDiagram the pre-rendered architecture overview diagram, or {@code null} if diagrams are disabled
     * @since 5.0.0
     */
    public OverviewGenerator(DocumentationModel model, String overviewDiagram) {
        this(model, overviewDiagram, List.of());
    }

    /**
     * Creates an OverviewGenerator with bounded context information.
     *
     * @param model the documentation model
     * @param overviewDiagram the pre-rendered architecture overview diagram, or {@code null} if diagrams are disabled
     * @param boundedContexts the detected bounded contexts
     * @since 5.0.0
     */
    public OverviewGenerator(
            DocumentationModel model, String overviewDiagram, List<BoundedContextDoc> boundedContexts) {
        this(model, overviewDiagram, boundedContexts, List.of(), null, null);
    }

    /**
     * Creates an OverviewGenerator with all content sections.
     *
     * @param model the documentation model
     * @param overviewDiagram the pre-rendered architecture overview diagram, or {@code null} if diagrams are disabled
     * @param boundedContexts the detected bounded contexts
     * @param glossaryEntries the glossary entries to include
     * @param packageTree the rendered package tree, or {@code null} to skip
     * @param indexRenderer the index renderer, or {@code null} to skip
     * @since 5.0.0
     */
    public OverviewGenerator(
            DocumentationModel model,
            String overviewDiagram,
            List<BoundedContextDoc> boundedContexts,
            List<GlossaryEntry> glossaryEntries,
            String packageTree,
            IndexRenderer indexRenderer) {
        this.model = Objects.requireNonNull(model, "model must not be null");
        this.overviewDiagram = overviewDiagram;
        this.boundedContexts = Objects.requireNonNull(boundedContexts, "boundedContexts must not be null");
        this.glossaryEntries = Objects.requireNonNull(glossaryEntries, "glossaryEntries must not be null");
        this.packageTree = packageTree;
        this.indexRenderer = indexRenderer;
    }

    /**
     * Generates the architecture overview Markdown content.
     *
     * <p>Includes the architecture diagram if one was provided at construction time.
     *
     * @return the generated Markdown content
     * @since 5.0.0
     */
    public String generate() {
        MarkdownBuilder md = new MarkdownBuilder()
                .h1("Architecture Overview")
                .paragraph(PluginVersion.generatorHeader())
                .horizontalRule();

        // Summary
        generateSummary(md);

        // Quick links
        generateDocumentationLinks(md);

        // Architecture overview diagram
        if (overviewDiagram != null) {
            md.h2("Architecture Diagram");
            md.raw(overviewDiagram);
        }

        // Domain summary
        generateDomainSummary(md);

        // Ports summary
        generatePortsSummary(md);

        // Bounded contexts
        if (!boundedContexts.isEmpty()) {
            BoundedContextRenderer bcRenderer = new BoundedContextRenderer();
            md.raw(bcRenderer.renderBoundedContextsSection(boundedContexts));
        }

        // Glossary
        generateGlossarySection(md);

        // Structure
        generateStructureSection(md);

        // Index
        generateIndexSection(md);

        return md.build();
    }

    private void generateSummary(MarkdownBuilder md) {
        md.h2("Summary")
                .table("Metric", "Count")
                .row("Aggregate Roots", String.valueOf(model.aggregateRoots().size()))
                .row("Entities", String.valueOf(model.entities().size()))
                .row("Value Objects", String.valueOf(model.valueObjects().size()))
                .row("Identifiers", String.valueOf(model.identifiers().size()))
                .row(
                        "Application Services",
                        String.valueOf(model.applicationServices().size()))
                .row("Driving Ports", String.valueOf(model.drivingPorts().size()))
                .row("Driven Ports", String.valueOf(model.drivenPorts().size()))
                .end();
    }

    private void generateDocumentationLinks(MarkdownBuilder md) {
        md.h2("Documentation")
                .bulletItem("[Domain Model](domain.md) - Aggregates, entities, and value objects")
                .bulletItem("[Ports](ports.md) - Driving and driven ports");
        if (overviewDiagram != null) {
            md.bulletItem("[Diagrams](diagrams.md) - Architecture diagrams");
        }
        md.newline().horizontalRule();
    }

    private void generateDomainSummary(MarkdownBuilder md) {
        md.h2("Domain Model");

        List<DocType> aggregates = model.aggregateRoots();
        if (!aggregates.isEmpty()) {
            md.h3("Aggregate Roots");
            var table = md.table("Name", "Package", "Properties");
            for (DocType agg : aggregates) {
                table.row(
                        "**" + agg.simpleName() + "**",
                        "`" + agg.packageName() + "`",
                        String.valueOf(agg.propertyCount()));
            }
            table.end();
        }

        List<DocType> valueObjects = model.valueObjects();
        if (!valueObjects.isEmpty()) {
            md.h3("Value Objects");
            var table = md.table("Name", "Package", "Type");
            for (DocType vo : valueObjects) {
                table.row(vo.simpleName(), "`" + vo.packageName() + "`", vo.construct());
            }
            table.end();
        }

        List<DocType> identifiers = model.identifiers();
        if (!identifiers.isEmpty()) {
            md.h3("Identifiers");
            var table = md.table("Name", "Package", "Type");
            for (DocType id : identifiers) {
                table.row(id.simpleName(), "`" + id.packageName() + "`", id.construct());
            }
            table.end();
        }

        md.paragraph("*See [domain.md](domain.md) for complete details.*");
    }

    private void generatePortsSummary(MarkdownBuilder md) {
        md.h2("Ports");

        List<DocPort> driving = model.drivingPorts();
        if (!driving.isEmpty()) {
            md.h3("Driving Ports (Primary)");
            var table = md.table("Port", "Kind", "Methods");
            for (DocPort port : driving) {
                table.row("**" + port.simpleName() + "**", port.kind(), String.valueOf(port.methodCount()));
            }
            table.end();
        }

        List<DocPort> driven = model.drivenPorts();
        if (!driven.isEmpty()) {
            md.h3("Driven Ports (Secondary)");
            var table = md.table("Port", "Kind", "Methods");
            for (DocPort port : driven) {
                table.row("**" + port.simpleName() + "**", port.kind(), String.valueOf(port.methodCount()));
            }
            table.end();
        }

        md.paragraph("*See [ports.md](ports.md) for complete details.*");
    }

    /**
     * Generates the domain glossary section.
     */
    private void generateGlossarySection(MarkdownBuilder md) {
        if (glossaryEntries.isEmpty()) {
            return;
        }

        md.h2("Glossaire du Domaine");

        TableBuilder table = md.table("Terme", "Definition", "Type", "Package");
        for (GlossaryEntry entry : glossaryEntries) {
            table.row(
                    "**" + entry.term() + "**",
                    entry.definition(),
                    io.hexaglue.plugin.livingdoc.content.GlossaryBuilder.formatKindLabel(entry.archKind()),
                    "`" + entry.packageName() + "`");
        }
        table.end();
    }

    /**
     * Generates the project structure section.
     */
    private void generateStructureSection(MarkdownBuilder md) {
        if (packageTree == null || packageTree.isEmpty()) {
            return;
        }

        md.h2("Structure du Projet");
        md.raw("\n```\n" + packageTree + "```\n\n");
    }

    /**
     * Generates the type index section.
     */
    private void generateIndexSection(MarkdownBuilder md) {
        if (indexRenderer == null || glossaryEntries.isEmpty()) {
            return;
        }

        md.raw(indexRenderer.renderTypeIndex(glossaryEntries));
    }
}
