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
import io.hexaglue.plugin.livingdoc.model.BoundedContextDoc;
import java.util.List;

/**
 * Renders bounded context documentation as a Markdown section.
 *
 * @since 5.0.0
 */
public final class BoundedContextRenderer {

    /**
     * Renders the bounded contexts section.
     *
     * <p>Returns an empty string if the list is empty, so it can be safely
     * appended without producing orphan headers.
     *
     * @param contexts the bounded contexts to render
     * @return the Markdown content, or empty string if no contexts
     * @since 5.0.0
     */
    public String renderBoundedContextsSection(List<BoundedContextDoc> contexts) {
        if (contexts == null || contexts.isEmpty()) {
            return "";
        }

        MarkdownBuilder md = new MarkdownBuilder();
        md.h2("Bounded Contexts");
        md.paragraph("Bounded contexts detected from package structure analysis.");

        TableBuilder table = md.table("Context", "Root Package", "Aggregates", "Entities", "VOs", "Ports", "Total");
        for (BoundedContextDoc ctx : contexts) {
            table.row(
                    "**" + capitalize(ctx.name()) + "**",
                    "`" + ctx.rootPackage() + "`",
                    String.valueOf(ctx.aggregateCount()),
                    String.valueOf(ctx.entityCount()),
                    String.valueOf(ctx.valueObjectCount()),
                    String.valueOf(ctx.portCount()),
                    String.valueOf(ctx.totalTypeCount()));
        }
        table.end();

        return md.build();
    }

    /**
     * Capitalizes the first letter of a string.
     */
    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
