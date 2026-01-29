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

import io.hexaglue.plugin.livingdoc.markdown.MarkdownBuilder;
import io.hexaglue.plugin.livingdoc.markdown.TableBuilder;
import io.hexaglue.plugin.livingdoc.model.MethodDoc;
import io.hexaglue.plugin.livingdoc.model.PortDoc;
import io.hexaglue.plugin.livingdoc.util.TypeDisplayUtil;
import java.util.List;

/**
 * Renders port documentation to Markdown.
 */
public final class PortRenderer {

    public String renderPort(PortDoc port) {
        MarkdownBuilder md = new MarkdownBuilder();

        // Port header
        md.h3(port.name());

        // Documentation (Javadoc)
        if (port.documentation() != null && !port.documentation().isBlank()) {
            md.paragraph(port.documentation());
        }

        // Metadata table
        md.table("Property", "Value")
                .row(
                        "**Kind**",
                        TypeDisplayUtil.formatKind(port.kind())
                                + TypeDisplayUtil.formatConfidenceBadge(port.confidence()))
                .row("**Direction**", TypeDisplayUtil.formatDirection(port.direction()))
                .row("**Package**", "`" + port.packageName() + "`")
                .row("**Confidence**", port.confidence().toString())
                .end();

        // Managed types
        List<String> managedTypes = port.managedTypes();
        if (!managedTypes.isEmpty()) {
            md.paragraph("**Managed Domain Types**");
            for (String type : managedTypes) {
                String simpleName = type.substring(type.lastIndexOf('.') + 1);
                md.bulletItem("`" + simpleName + "`");
            }
            md.newline();
        }

        // Methods
        if (!port.methods().isEmpty()) {
            md.raw(renderMethods(port.methods()));
            md.raw(renderMethodSignatures(port));
        }

        // Debug information
        md.raw(renderDebugSection(port));

        md.horizontalRule();

        return md.build();
    }

    public String renderMethods(List<MethodDoc> methods) {
        MarkdownBuilder md = new MarkdownBuilder();
        md.paragraph("**Methods**");

        boolean hasDocumentation = methods.stream()
                .anyMatch(m -> m.documentation() != null && !m.documentation().isBlank());

        if (hasDocumentation) {
            TableBuilder table = md.table("Method", "Return Type", "Parameters", "Description");
            for (MethodDoc method : methods) {
                String description = method.documentation() != null
                                && !method.documentation().isBlank()
                        ? method.documentation()
                        : "-";
                table.row(
                        "`" + method.name() + "`",
                        "`" + TypeDisplayUtil.simplifyType(method.returnType()) + "`",
                        formatParameters(method.parameters()),
                        description);
            }
            table.end();
        } else {
            TableBuilder table = md.table("Method", "Return Type", "Parameters");
            for (MethodDoc method : methods) {
                table.row(
                        "`" + method.name() + "`",
                        "`" + TypeDisplayUtil.simplifyType(method.returnType()) + "`",
                        formatParameters(method.parameters()));
            }
            table.end();
        }

        return md.build();
    }

    public String renderMethodSignatures(PortDoc port) {
        // Build the Java code block content first
        StringBuilder codeBlock = new StringBuilder();
        codeBlock.append("```java\n");
        codeBlock.append("public interface ").append(port.name()).append(" {\n");
        for (MethodDoc method : port.methods()) {
            codeBlock
                    .append("    ")
                    .append(TypeDisplayUtil.simplifyType(method.returnType()))
                    .append(" ")
                    .append(method.name())
                    .append("(")
                    .append(formatMethodParams(method.parameters()))
                    .append(");\n");
        }
        codeBlock.append("}\n");
        codeBlock.append("```\n\n");

        return new MarkdownBuilder()
                .collapsible("Method Signatures")
                .rawContent(codeBlock.toString())
                .end()
                .build();
    }

    public String renderDebugSection(PortDoc port) {
        return new MarkdownBuilder()
                .collapsible("Debug Information")
                .withBlockquote()
                .content(inner -> {
                    // Port Information
                    inner.h4("Port Information")
                            .table("Property", "Value")
                            .row("**Qualified Name**", "`" + port.debug().qualifiedName() + "`")
                            .row("**Package**", "`" + port.packageName() + "`")
                            .row("**Kind**", port.kind().toString())
                            .row("**Direction**", port.direction().toString())
                            .row("**Confidence**", port.confidence().toString())
                            .end();

                    // Managed types (full qualified names)
                    if (!port.managedTypes().isEmpty()) {
                        inner.h4("Managed Domain Types");
                        for (String type : port.managedTypes()) {
                            inner.bulletItem("`" + type + "`");
                        }
                        inner.newline();
                    }

                    // Methods Details
                    if (!port.methods().isEmpty()) {
                        inner.h4("Methods Details");
                        for (MethodDoc method : port.methods()) {
                            inner.bold(method.name()).newline();

                            TableBuilder table = inner.table("Property", "Value")
                                    .row("**Return Type**", "`" + method.returnType() + "`");

                            if (method.parameters().isEmpty()) {
                                table.row("**Parameters**", "*none*");
                            } else {
                                StringBuilder params = new StringBuilder();
                                for (int i = 0; i < method.parameters().size(); i++) {
                                    if (i > 0) params.append(", ");
                                    params.append("`")
                                            .append(method.parameters().get(i))
                                            .append("`");
                                }
                                table.row("**Parameters**", params.toString());
                            }
                            table.end();
                        }
                    }

                    // Annotations
                    inner.h4("Annotations");
                    List<String> annotations = port.debug().annotations();
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
                    if (port.debug().sourceFile() != null) {
                        inner.table("Property", "Value")
                                .row("**File**", "`" + port.debug().sourceFile() + "`")
                                .row(
                                        "**Lines**",
                                        port.debug().lineStart() + "-"
                                                + port.debug().lineEnd())
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

    private String formatParameters(List<String> parameters) {
        if (parameters.isEmpty()) {
            return "-";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("`")
                    .append(TypeDisplayUtil.simplifyType(parameters.get(i)))
                    .append("`");
        }
        return sb.toString();
    }

    private String formatMethodParams(List<String> parameters) {
        if (parameters.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(TypeDisplayUtil.simplifyType(parameters.get(i)))
                    .append(" arg")
                    .append(i);
        }
        return sb.toString();
    }
}
