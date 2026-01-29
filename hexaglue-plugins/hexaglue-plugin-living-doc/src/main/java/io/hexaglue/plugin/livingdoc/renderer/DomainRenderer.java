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

import io.hexaglue.arch.model.graph.RelationType;
import io.hexaglue.plugin.livingdoc.markdown.MarkdownBuilder;
import io.hexaglue.plugin.livingdoc.markdown.TableBuilder;
import io.hexaglue.plugin.livingdoc.model.ArchitecturalDependency;
import io.hexaglue.plugin.livingdoc.model.DomainTypeDoc;
import io.hexaglue.plugin.livingdoc.model.IdentityDoc;
import io.hexaglue.plugin.livingdoc.model.PropertyDoc;
import io.hexaglue.plugin.livingdoc.model.RelationDoc;
import io.hexaglue.plugin.livingdoc.util.TypeDisplayUtil;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Renders domain type documentation to Markdown.
 */
public final class DomainRenderer {

    public String renderType(DomainTypeDoc type) {
        MarkdownBuilder md = new MarkdownBuilder();

        // Type header
        md.h3(type.name());

        // Documentation (Javadoc)
        if (type.documentation() != null && !type.documentation().isBlank()) {
            md.paragraph(type.documentation());
        }

        // Metadata table
        md.table("Property", "Value")
                .row(
                        "**Kind**",
                        TypeDisplayUtil.formatKind(type.kind())
                                + TypeDisplayUtil.formatConfidenceBadge(type.confidence()))
                .row("**Package**", "`" + type.packageName() + "`")
                .row("**Type**", type.construct())
                .row("**Confidence**", type.confidence().toString())
                .end();

        // Identity
        if (type.identity() != null) {
            md.raw(renderIdentity(type.identity()));
        }

        // Properties
        if (!type.properties().isEmpty()) {
            md.raw(renderProperties(type.properties()));
        }

        // Relations
        if (!type.relations().isEmpty()) {
            md.raw(renderRelations(type.relations()));
        }

        // Debug information
        md.raw(renderDebugSection(type));

        md.horizontalRule();

        return md.build();
    }

    public String renderIdentity(IdentityDoc identity) {
        return new MarkdownBuilder()
                .paragraph("**Identity**")
                .table("Field", "Type", "Underlying", "Strategy", "Wrapper")
                .row(
                        "`" + identity.fieldName() + "`",
                        "`" + identity.type() + "`",
                        "`" + identity.underlyingType() + "`",
                        identity.strategy(),
                        identity.wrapperKind())
                .end()
                .build();
    }

    public String renderProperties(List<PropertyDoc> properties) {
        MarkdownBuilder md = new MarkdownBuilder();
        md.paragraph("**Properties**");

        TableBuilder table = md.table("Name", "Type", "Cardinality", "Notes");
        for (PropertyDoc prop : properties) {
            table.row(
                    "`" + prop.name() + "`",
                    "`" + formatPropertyType(prop) + "`",
                    TypeDisplayUtil.formatCardinality(prop.cardinality()),
                    formatPropertyNotes(prop));
        }
        table.end();

        return md.build();
    }

    public String renderRelations(List<RelationDoc> relations) {
        MarkdownBuilder md = new MarkdownBuilder();
        md.paragraph("**Relationships**");

        TableBuilder table = md.table("Target", "Kind", "Direction", "Cascade", "Fetch");
        for (RelationDoc rel : relations) {
            table.row(
                    "`" + rel.targetType() + "`",
                    rel.kind(),
                    rel.isOwning() ? "Owning" : "Inverse",
                    rel.cascade(),
                    rel.fetch());
        }
        table.end();

        return md.build();
    }

    /**
     * Renders architectural dependencies (outgoing and incoming) as Markdown tables.
     *
     * @param outgoing the outgoing dependencies
     * @param incoming the incoming dependencies
     * @return the Markdown content
     * @since 5.0.0
     */
    public String renderArchitecturalDependencies(
            List<ArchitecturalDependency> outgoing, List<ArchitecturalDependency> incoming) {
        MarkdownBuilder md = new MarkdownBuilder();
        md.paragraph("**Architectural Dependencies**");

        if (!outgoing.isEmpty()) {
            md.raw("*Outgoing:*\n\n");
            TableBuilder table = md.table("Target", "Relationship");
            for (ArchitecturalDependency dep : outgoing) {
                table.row("`" + dep.targetSimpleName() + "`", formatRelationType(dep.relationType()));
            }
            table.end();
        }

        if (!incoming.isEmpty()) {
            md.raw("*Incoming:*\n\n");
            TableBuilder table = md.table("Source", "Relationship");
            for (ArchitecturalDependency dep : incoming) {
                table.row("`" + dep.targetSimpleName() + "`", formatRelationType(dep.relationType()));
            }
            table.end();
        }

        return md.build();
    }

    private String formatRelationType(RelationType type) {
        return switch (type) {
            case CONTAINS -> "Contains";
            case OWNS -> "Owns";
            case REFERENCES -> "References";
            case DEPENDS_ON -> "Depends on";
            case EXTENDS -> "Extends";
            case IMPLEMENTS -> "Implements";
            case EXPOSES -> "Exposes";
            case ADAPTS -> "Adapts";
            case PERSISTS -> "Persists";
            case EMITS -> "Emits";
            case HANDLES -> "Handles";
        };
    }

    public String renderDebugSection(DomainTypeDoc type) {
        return new MarkdownBuilder()
                .collapsible("Debug Information")
                .withBlockquote()
                .content(inner -> {
                    // Type Information
                    inner.h4("Type Information")
                            .table("Property", "Value")
                            .row("**Qualified Name**", "`" + type.debug().qualifiedName() + "`")
                            .row("**Package**", "`" + type.packageName() + "`")
                            .row("**Construct**", type.construct())
                            .row("**Confidence**", type.confidence().toString())
                            .row("**Is Record**", String.valueOf(type.isRecord()))
                            .end();

                    // Identity Details (if present)
                    if (type.identity() != null) {
                        inner.raw(renderIdentityDebug(
                                type.identity(), type.debug().qualifiedName()));
                    }

                    // Property Details (if any)
                    if (!type.properties().isEmpty()) {
                        inner.raw(renderPropertiesDebug(type.properties()));
                    }

                    // Relation Details (explicit relations)
                    if (!type.relations().isEmpty()) {
                        inner.raw(renderRelationsDebug(type.relations()));
                    }

                    // Annotations
                    inner.h4("Annotations");
                    List<String> annotations = type.debug().annotations();
                    if (!annotations.isEmpty()) {
                        for (String annotation : annotations) {
                            inner.bulletItem("`@" + TypeDisplayUtil.simplifyType(annotation) + "`");
                        }
                        inner.newline();
                    } else {
                        inner.paragraph("*none*");
                    }

                    // Source reference
                    inner.h4("Source Location");
                    if (type.debug().sourceFile() != null) {
                        inner.table("Property", "Value")
                                .row("**File**", "`" + type.debug().sourceFile() + "`")
                                .row(
                                        "**Lines**",
                                        type.debug().lineStart() + "-"
                                                + type.debug().lineEnd())
                                .end();
                    } else {
                        inner.table("Property", "Value")
                                .row("**File**", "*synthetic*")
                                .end();
                    }
                })
                .end()
                .build();
    }

    private String renderIdentityDebug(IdentityDoc identity, String typeQualifiedName) {
        MarkdownBuilder md = new MarkdownBuilder();
        md.h4("Identity Details");

        TableBuilder table = md.table("Property", "Value")
                .row("**Field Name**", "`" + identity.fieldName() + "`")
                .row("**Declared Type**", "`" + identity.type() + "`")
                .row("**Underlying Type**", "`" + identity.underlyingType() + "`")
                .row("**Wrapper Kind**", identity.wrapperKind())
                .row("**Strategy**", identity.strategy())
                .row("**Is Wrapped**", String.valueOf(identity.isWrapped()));

        if (identity.requiresGeneratedValue()) {
            table.row("**Requires @GeneratedValue**", "true")
                    .row("**JPA Generation Type**", identity.jpaGenerationType());
        }

        table.end();
        return md.build();
    }

    private String renderPropertiesDebug(List<PropertyDoc> properties) {
        MarkdownBuilder md = new MarkdownBuilder();
        for (PropertyDoc prop : properties) {
            md.h4("Property: " + prop.name());

            TableBuilder table = md.table("Property", "Value")
                    .row("**Type**", "`" + prop.type() + "`")
                    .row("**Cardinality**", prop.cardinality())
                    .row("**Nullability**", prop.nullability())
                    .row("**Is Identity**", String.valueOf(prop.isIdentity()))
                    .row("**Is Embedded**", String.valueOf(prop.isEmbedded()))
                    .row("**Is Simple**", String.valueOf(prop.isSimple()));

            // TypeRef details
            if (prop.isParameterized()) {
                String typeArgs =
                        prop.typeArguments().stream().map(t -> "`" + t + "`").collect(Collectors.joining(", "));
                table.row("**Is Parameterized**", "true").row("**Type Arguments**", typeArgs);
            }

            // Relation info
            if (prop.relationInfo() != null) {
                table.row("**Has Relation**", "true")
                        .row("**Relation Kind**", prop.relationInfo().kind())
                        .row("**Target Type**", "`" + prop.relationInfo().targetType() + "`")
                        .row(
                                "**Owning Side**",
                                String.valueOf(prop.relationInfo().owning()));

                if (prop.relationInfo().mappedBy() != null) {
                    table.row("**Mapped By**", "`" + prop.relationInfo().mappedBy() + "`");
                }

                table.row(
                                "**Is Bidirectional**",
                                String.valueOf(prop.relationInfo().isBidirectional()))
                        .row(
                                "**Is Embedded**",
                                String.valueOf(prop.relationInfo().isEmbedded()));
            }

            table.end();
        }
        return md.build();
    }

    private String renderRelationsDebug(List<RelationDoc> relations) {
        MarkdownBuilder md = new MarkdownBuilder();
        for (RelationDoc rel : relations) {
            md.h4("Relation: " + rel.propertyName());

            TableBuilder table = md.table("Property", "Value")
                    .row("**Kind**", rel.kind())
                    .row("**Target Type (FQN)**", "`" + rel.targetType() + "`")
                    .row("**Target Kind**", rel.targetKind())
                    .row("**Is Owning**", String.valueOf(rel.isOwning()))
                    .row("**Is Bidirectional**", String.valueOf(rel.isBidirectional()));

            if (rel.mappedBy() != null) {
                table.row("**Mapped By**", "`" + rel.mappedBy() + "`");
            }

            table.row("**Cascade**", rel.cascade())
                    .row("**Fetch**", rel.fetch())
                    .row("**Orphan Removal**", String.valueOf(rel.orphanRemoval()))
                    .end();
        }
        return md.build();
    }

    private String formatPropertyType(PropertyDoc prop) {
        // Extract simple name from qualified type
        String simpleName = TypeDisplayUtil.simplifyType(prop.type());

        if (prop.cardinality().equals("COLLECTION")) {
            if (prop.isParameterized() && !prop.typeArguments().isEmpty()) {
                String elementType =
                        TypeDisplayUtil.simplifyType(prop.typeArguments().get(0));
                return simpleName + "<" + elementType + ">";
            }
        } else if (prop.cardinality().equals("OPTIONAL")) {
            if (prop.isParameterized() && !prop.typeArguments().isEmpty()) {
                String elementType =
                        TypeDisplayUtil.simplifyType(prop.typeArguments().get(0));
                return "Optional<" + elementType + ">";
            }
        }
        return simpleName;
    }

    private String formatPropertyNotes(PropertyDoc prop) {
        StringBuilder notes = new StringBuilder();
        if (prop.isIdentity()) {
            notes.append("Identity");
        }
        if (prop.isEmbedded()) {
            if (!notes.isEmpty()) notes.append(", ");
            notes.append("Embedded");
        }
        if (prop.relationInfo() != null) {
            if (!notes.isEmpty()) notes.append(", ");
            notes.append("Relation");
        }
        // Show documentation when available, otherwise fall back to nullability
        if (prop.documentation() != null && !prop.documentation().isBlank()) {
            if (!notes.isEmpty()) notes.append(" — ");
            notes.append(prop.documentation());
        } else if (prop.nullability() != null) {
            if (!notes.isEmpty()) notes.append(", ");
            notes.append(prop.nullability());
        }
        return notes.isEmpty() ? "-" : notes.toString();
    }
}
