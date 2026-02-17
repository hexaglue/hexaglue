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

import io.hexaglue.arch.model.TypeId;
import io.hexaglue.plugin.livingdoc.content.DomainContentSelector;
import io.hexaglue.plugin.livingdoc.content.RelationshipEnricher;
import io.hexaglue.plugin.livingdoc.markdown.MarkdownBuilder;
import io.hexaglue.plugin.livingdoc.model.ArchitecturalDependency;
import io.hexaglue.plugin.livingdoc.model.DomainTypeDoc;
import io.hexaglue.plugin.livingdoc.renderer.DomainRenderer;
import io.hexaglue.plugin.livingdoc.util.MarkdownUtil;
import io.hexaglue.plugin.livingdoc.util.PluginVersion;
import java.util.List;

/**
 * Generates documentation for the domain model.
 *
 * <p>Uses three-layer architecture:
 * <ol>
 *   <li>ContentSelector - selects and transforms model data to documentation models</li>
 *   <li>DocumentationModel - immutable records representing documentation content</li>
 *   <li>Renderer - renders documentation models to Markdown</li>
 * </ol>
 *
 * @since 4.0.0
 */
public final class DomainDocGenerator {

    private final DomainContentSelector contentSelector;
    private final DomainRenderer renderer;
    private final RelationshipEnricher relationshipEnricher;

    /**
     * Creates a generator using a pre-built content selector.
     *
     * <p>The selector is shared across generators to avoid redundant
     * index lookups and object creation.</p>
     *
     * @param contentSelector the domain content selector
     * @since 4.0.0
     * @since 5.0.0 - Accepts DomainContentSelector instead of ArchitecturalModel
     */
    public DomainDocGenerator(DomainContentSelector contentSelector) {
        this(contentSelector, null);
    }

    /**
     * Creates a generator with relationship enrichment support.
     *
     * @param contentSelector the domain content selector
     * @param relationshipEnricher the relationship enricher, or {@code null} if unavailable
     * @since 5.0.0
     */
    public DomainDocGenerator(DomainContentSelector contentSelector, RelationshipEnricher relationshipEnricher) {
        this(contentSelector, relationshipEnricher, true);
    }

    /**
     * Creates a generator with relationship enrichment and configurable debug sections.
     *
     * @param contentSelector the domain content selector
     * @param relationshipEnricher the relationship enricher, or {@code null} if unavailable
     * @param includeDebugSections whether to include debug sections in the generated documentation
     * @since 5.1.0
     */
    public DomainDocGenerator(
            DomainContentSelector contentSelector,
            RelationshipEnricher relationshipEnricher,
            boolean includeDebugSections) {
        this.contentSelector = contentSelector;
        this.renderer = new DomainRenderer(includeDebugSections);
        this.relationshipEnricher = relationshipEnricher;
    }

    public String generate() {
        MarkdownBuilder md = new MarkdownBuilder()
                .h1("Domain Model")
                .paragraph(PluginVersion.generatorHeader())
                .link("Back to Overview", "README.md")
                .newline()
                .newline()
                .horizontalRule();

        // Table of contents
        generateTableOfContents(md);

        // Aggregate Roots
        List<DomainTypeDoc> aggregates = contentSelector.selectAggregateRoots();
        if (!aggregates.isEmpty()) {
            md.h2("Aggregate Roots")
                    .paragraph(
                            "Aggregate roots are the entry points to aggregates. They ensure consistency boundaries and manage their own invariants.");
            for (DomainTypeDoc agg : aggregates) {
                renderTypeWithDependencies(md, agg);
            }
        }

        // Entities
        List<DomainTypeDoc> entities = contentSelector.selectEntities();
        if (!entities.isEmpty()) {
            md.h2("Entities").paragraph("Entities have identity and are accessed through their aggregate root.");
            for (DomainTypeDoc entity : entities) {
                renderTypeWithDependencies(md, entity);
            }
        }

        // Value Objects
        List<DomainTypeDoc> valueObjects = contentSelector.selectValueObjects();
        if (!valueObjects.isEmpty()) {
            md.h2("Value Objects")
                    .paragraph("Value objects are immutable and defined by their attributes, not identity.");
            for (DomainTypeDoc vo : valueObjects) {
                renderTypeWithDependencies(md, vo);
            }
        }

        // Identifiers
        List<DomainTypeDoc> identifiers = contentSelector.selectIdentifiers();
        if (!identifiers.isEmpty()) {
            md.h2("Identifiers").paragraph("Identifier types wrap primitive identity values for type safety.");
            for (DomainTypeDoc id : identifiers) {
                renderTypeWithDependencies(md, id);
            }
        }

        // Domain Events
        List<DomainTypeDoc> events = contentSelector.selectDomainEvents();
        if (!events.isEmpty()) {
            md.h2("Domain Events")
                    .paragraph("Domain events represent something meaningful that happened in the domain.");
            for (DomainTypeDoc event : events) {
                renderTypeWithDependencies(md, event);
            }
        }

        // Domain Services
        List<DomainTypeDoc> domainServices = contentSelector.selectDomainServices();
        if (!domainServices.isEmpty()) {
            md.h2("Domain Services")
                    .paragraph(
                            "Domain services contain domain logic that doesn't belong to entities. They have NO dependencies on infrastructure ports.");
            for (DomainTypeDoc service : domainServices) {
                renderTypeWithDependencies(md, service);
            }
        }

        // Application Services
        List<DomainTypeDoc> appServices = contentSelector.selectApplicationServices();
        if (!appServices.isEmpty()) {
            md.h2("Application Services")
                    .paragraph(
                            "Application services orchestrate use cases by coordinating domain logic and infrastructure through ports. They have dependencies on DRIVEN ports.");
            for (DomainTypeDoc service : appServices) {
                renderTypeWithDependencies(md, service);
            }
        }

        return md.build();
    }

    private void generateTableOfContents(MarkdownBuilder md) {
        md.h2("Contents");

        addTocSection(md, "Aggregate Roots", contentSelector.selectAggregateRoots());
        addTocSection(md, "Entities", contentSelector.selectEntities());
        addTocSection(md, "Value Objects", contentSelector.selectValueObjects());
        addTocSection(md, "Identifiers", contentSelector.selectIdentifiers());
        addTocSection(md, "Domain Events", contentSelector.selectDomainEvents());
        addTocSection(md, "Domain Services", contentSelector.selectDomainServices());
        addTocSection(md, "Application Services", contentSelector.selectApplicationServices());

        md.horizontalRule();
    }

    private void addTocSection(MarkdownBuilder md, String title, List<DomainTypeDoc> types) {
        if (!types.isEmpty()) {
            md.h3(title);
            for (DomainTypeDoc type : types) {
                md.bulletItem("[" + type.name() + "](#" + MarkdownUtil.toAnchor(type.name()) + ")");
            }
            md.newline();
        }
    }

    /**
     * Renders a type followed by its architectural dependencies (if enricher is available).
     */
    private void renderTypeWithDependencies(MarkdownBuilder md, DomainTypeDoc type) {
        md.raw(renderer.renderType(type));

        if (relationshipEnricher != null && relationshipEnricher.isAvailable()) {
            TypeId typeId = TypeId.of(type.debug().qualifiedName());
            List<ArchitecturalDependency> outgoing = relationshipEnricher.outgoingFrom(typeId);
            List<ArchitecturalDependency> incoming = relationshipEnricher.incomingTo(typeId);

            if (!outgoing.isEmpty() || !incoming.isEmpty()) {
                md.raw(renderer.renderArchitecturalDependencies(outgoing, incoming));
            }
        }
    }
}
