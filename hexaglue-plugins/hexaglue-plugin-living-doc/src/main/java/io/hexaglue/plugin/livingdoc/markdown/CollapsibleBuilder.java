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

package io.hexaglue.plugin.livingdoc.markdown;

import java.util.function.Consumer;

/**
 * Nested builder for collapsible sections using HTML details/summary tags.
 *
 * <p>This builder is created from {@link MarkdownBuilder#collapsible(String)} and allows
 * adding content to a collapsible section before returning to the parent builder with {@link #end()}.
 *
 * <p>Generates HTML details/summary tags that work in Markdown:
 * <pre>
 * &lt;details&gt;
 * &lt;summary&gt;Click to expand&lt;/summary&gt;
 *
 * Content goes here...
 *
 * &lt;/details&gt;
 * </pre>
 *
 * <p>Example usage:
 * <pre>{@code
 * new MarkdownBuilder()
 *     .h2("Documentation")
 *     .collapsible("Debug Information")
 *         .content(debugBuilder -> debugBuilder
 *             .h4("Details")
 *             .paragraph("More info here"))
 *         .end()
 *     .build();
 * }</pre>
 *
 * @since 0.1.0
 */
public final class CollapsibleBuilder {

    private final MarkdownBuilder parent;
    private final StringBuilder content;
    private boolean useBlockquote;

    /**
     * Creates a new collapsible section builder.
     *
     * @param parent the parent MarkdownBuilder to return to
     * @param summary the clickable summary text
     */
    CollapsibleBuilder(MarkdownBuilder parent, String summary) {
        this.parent = parent;
        this.content = parent.getContent();
        this.useBlockquote = false;

        // Start the details tag
        content.append("<details>\n");
        content.append("<summary>").append(summary).append("</summary>\n\n");
    }

    /**
     * Enables blockquote wrapping for the content.
     *
     * <p>When enabled, all content added via {@link #content(Consumer)} will be
     * wrapped in HTML blockquote tags, providing visual distinction from
     * surrounding content.
     *
     * @return this builder for method chaining
     */
    public CollapsibleBuilder withBlockquote() {
        this.useBlockquote = true;
        return this;
    }

    /**
     * Adds content to the collapsible section using a nested MarkdownBuilder.
     *
     * <p>The consumer receives a new MarkdownBuilder that shares the same
     * underlying StringBuilder, allowing you to use the full Markdown API
     * within the collapsible section.
     *
     * <p>If {@link #withBlockquote()} was called, the content will be wrapped
     * in HTML blockquote tags for visual distinction.
     *
     * @param contentBuilder a consumer that builds the content
     * @return this builder for method chaining
     */
    public CollapsibleBuilder content(Consumer<MarkdownBuilder> contentBuilder) {
        if (useBlockquote) {
            content.append("<blockquote>\n\n");
        }
        MarkdownBuilder nestedBuilder = new MarkdownBuilder(content);
        contentBuilder.accept(nestedBuilder);
        if (useBlockquote) {
            content.append("</blockquote>\n");
        }
        return this;
    }

    /**
     * Adds raw content directly to the collapsible section.
     *
     * <p>Use this when you have pre-generated content or need to add
     * content that doesn't fit the builder API.
     *
     * @param rawContent the raw content to add
     * @return this builder for method chaining
     */
    public CollapsibleBuilder rawContent(String rawContent) {
        content.append(rawContent);
        return this;
    }

    /**
     * Completes the collapsible section and returns to the parent MarkdownBuilder.
     *
     * @return the parent MarkdownBuilder
     */
    public MarkdownBuilder end() {
        content.append("\n</details>\n\n");
        return parent;
    }
}
