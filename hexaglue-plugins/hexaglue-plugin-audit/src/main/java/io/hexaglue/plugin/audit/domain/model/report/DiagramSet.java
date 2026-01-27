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

package io.hexaglue.plugin.audit.domain.model.report;

import java.util.Objects;

/**
 * Container for all Mermaid diagrams generated for the report.
 *
 * <p>Diagrams are stored as Mermaid-formatted strings and are shared
 * between HTML and Markdown renderers to ensure consistency.
 *
 * <p>Note: Diagrams are NOT stored in the JSON pivot format; they are
 * generated separately and injected into HTML/Markdown renders.
 *
 * @param scoreRadar radar-beta chart showing scores by dimension
 * @param c4Context C4Context diagram showing system and external actors
 * @param c4Component C4Component diagram showing ports, aggregates, adapters
 * @param domainModel classDiagram showing aggregates with value objects
 * @param violationsPie pie chart showing violation distribution by severity
 * @param packageZones quadrantChart showing main sequence analysis
 * @since 5.0.0
 */
public record DiagramSet(
        String scoreRadar,
        String c4Context,
        String c4Component,
        String domainModel,
        String violationsPie,
        String packageZones) {

    /**
     * Creates a diagram set with validation.
     * All diagrams are mandatory as per specification.
     */
    public DiagramSet {
        Objects.requireNonNull(scoreRadar, "scoreRadar diagram is required");
        Objects.requireNonNull(c4Context, "c4Context diagram is required");
        Objects.requireNonNull(c4Component, "c4Component diagram is required");
        Objects.requireNonNull(domainModel, "domainModel diagram is required");
        Objects.requireNonNull(violationsPie, "violationsPie diagram is required");
        Objects.requireNonNull(packageZones, "packageZones diagram is required");
    }

    /**
     * Builder for DiagramSet.
     */
    public static class Builder {
        private String scoreRadar;
        private String c4Context;
        private String c4Component;
        private String domainModel;
        private String violationsPie;
        private String packageZones;

        public Builder scoreRadar(String scoreRadar) {
            this.scoreRadar = scoreRadar;
            return this;
        }

        public Builder c4Context(String c4Context) {
            this.c4Context = c4Context;
            return this;
        }

        public Builder c4Component(String c4Component) {
            this.c4Component = c4Component;
            return this;
        }

        public Builder domainModel(String domainModel) {
            this.domainModel = domainModel;
            return this;
        }

        public Builder violationsPie(String violationsPie) {
            this.violationsPie = violationsPie;
            return this;
        }

        public Builder packageZones(String packageZones) {
            this.packageZones = packageZones;
            return this;
        }

        public DiagramSet build() {
            return new DiagramSet(scoreRadar, c4Context, c4Component, domainModel, violationsPie, packageZones);
        }
    }

    /**
     * Creates a new builder.
     *
     * @return new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Wraps a diagram in Mermaid markdown code block.
     *
     * @param diagram the Mermaid diagram content
     * @return diagram wrapped in code block
     */
    public static String wrapInCodeBlock(String diagram) {
        return "```mermaid\n" + diagram + "\n```";
    }

    /**
     * Returns the score radar wrapped in a markdown code block.
     *
     * @return wrapped diagram
     */
    public String scoreRadarMarkdown() {
        return wrapInCodeBlock(scoreRadar);
    }

    /**
     * Returns the C4 context diagram wrapped in a markdown code block.
     *
     * @return wrapped diagram
     */
    public String c4ContextMarkdown() {
        return wrapInCodeBlock(c4Context);
    }

    /**
     * Returns the C4 component diagram wrapped in a markdown code block.
     *
     * @return wrapped diagram
     */
    public String c4ComponentMarkdown() {
        return wrapInCodeBlock(c4Component);
    }

    /**
     * Returns the domain model diagram wrapped in a markdown code block.
     *
     * @return wrapped diagram
     */
    public String domainModelMarkdown() {
        return wrapInCodeBlock(domainModel);
    }

    /**
     * Returns the violations pie chart wrapped in a markdown code block.
     *
     * @return wrapped diagram
     */
    public String violationsPieMarkdown() {
        return wrapInCodeBlock(violationsPie);
    }

    /**
     * Returns the package zones chart wrapped in a markdown code block.
     *
     * @return wrapped diagram
     */
    public String packageZonesMarkdown() {
        return wrapInCodeBlock(packageZones);
    }
}
