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

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Container for all Mermaid diagrams generated for the report.
 *
 * <p>Diagrams are stored as Mermaid-formatted strings and are shared
 * between HTML and Markdown renderers to ensure consistency.
 *
 * <p>Note: Diagrams are NOT stored in the JSON pivot format; they are
 * generated separately and injected into HTML/Markdown renders.
 *
 * <h2>Required Diagrams</h2>
 * <ul>
 *   <li>{@link #scoreRadar()} - radar-beta chart showing scores by dimension</li>
 *   <li>{@link #c4Context()} - C4Context diagram showing system and external actors</li>
 *   <li>{@link #c4Component()} - C4Component diagram showing ports, aggregates, adapters</li>
 *   <li>{@link #domainModel()} - classDiagram showing aggregates (combined)</li>
 *   <li>{@link #violationsPie()} - pie chart showing violation distribution</li>
 *   <li>{@link #packageZones()} - quadrantChart showing main sequence analysis</li>
 * </ul>
 *
 * <h2>Optional Diagrams (since 5.0.0)</h2>
 * <ul>
 *   <li>{@link #applicationLayer()} - classDiagram showing application services layer</li>
 *   <li>{@link #portsLayer()} - classDiagram showing ports layer</li>
 *   <li>{@link #fullArchitecture()} - C4Component showing full hexagonal architecture overview</li>
 * </ul>
 *
 * @param scoreRadar radar-beta chart showing scores by dimension
 * @param c4Context C4Context diagram showing system and external actors
 * @param c4Component C4Component diagram showing ports, aggregates, adapters
 * @param domainModel combined classDiagram showing all aggregates (for backwards compatibility)
 * @param aggregateDiagrams list of individual diagrams per aggregate
 * @param violationsPie pie chart showing violation distribution by severity
 * @param packageZones quadrantChart showing main sequence analysis
 * @param applicationLayer classDiagram showing application services, command/query handlers (optional)
 * @param portsLayer classDiagram showing driving and driven ports (optional)
 * @param fullArchitecture C4Component showing full hexagonal architecture with all layers (optional)
 * @since 5.0.0
 */
public record DiagramSet(
        String scoreRadar,
        String c4Context,
        String c4Component,
        String domainModel,
        List<AggregateDiagram> aggregateDiagrams,
        String violationsPie,
        String packageZones,
        String applicationLayer,
        String portsLayer,
        String fullArchitecture) {

    /**
     * Individual diagram for an aggregate.
     *
     * @param aggregateName name of the aggregate root
     * @param diagram Mermaid classDiagram code
     * @param hasErrors true if the aggregate has errors (cycles, etc.)
     */
    public record AggregateDiagram(String aggregateName, String diagram, boolean hasErrors) {}

    /**
     * Creates a diagram set with validation.
     * Core diagrams are mandatory as per specification.
     * Application layer and ports layer diagrams are optional.
     */
    public DiagramSet {
        Objects.requireNonNull(scoreRadar, "scoreRadar diagram is required");
        Objects.requireNonNull(c4Context, "c4Context diagram is required");
        Objects.requireNonNull(c4Component, "c4Component diagram is required");
        Objects.requireNonNull(domainModel, "domainModel diagram is required");
        aggregateDiagrams = aggregateDiagrams != null ? List.copyOf(aggregateDiagrams) : List.of();
        Objects.requireNonNull(violationsPie, "violationsPie diagram is required");
        Objects.requireNonNull(packageZones, "packageZones diagram is required");
        // applicationLayer is optional - may be null
        // portsLayer is optional - may be null
        // fullArchitecture is optional - may be null
    }

    /**
     * Builder for DiagramSet.
     */
    public static class Builder {
        private String scoreRadar;
        private String c4Context;
        private String c4Component;
        private String domainModel;
        private List<AggregateDiagram> aggregateDiagrams = List.of();
        private String violationsPie;
        private String packageZones;
        private String applicationLayer;
        private String portsLayer;
        private String fullArchitecture;

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

        public Builder aggregateDiagrams(List<AggregateDiagram> diagrams) {
            this.aggregateDiagrams = diagrams;
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

        /**
         * Sets the application layer diagram (optional).
         *
         * @param applicationLayer the classDiagram for application services layer
         * @return this builder
         * @since 5.0.0
         */
        public Builder applicationLayer(String applicationLayer) {
            this.applicationLayer = applicationLayer;
            return this;
        }

        /**
         * Sets the ports layer diagram (optional).
         *
         * @param portsLayer the classDiagram for ports layer
         * @return this builder
         * @since 5.0.0
         */
        public Builder portsLayer(String portsLayer) {
            this.portsLayer = portsLayer;
            return this;
        }

        /**
         * Sets the full architecture diagram (optional).
         *
         * @param fullArchitecture the C4Component diagram showing full hexagonal architecture
         * @return this builder
         * @since 5.0.0
         */
        public Builder fullArchitecture(String fullArchitecture) {
            this.fullArchitecture = fullArchitecture;
            return this;
        }

        public DiagramSet build() {
            return new DiagramSet(
                    scoreRadar,
                    c4Context,
                    c4Component,
                    domainModel,
                    aggregateDiagrams,
                    violationsPie,
                    packageZones,
                    applicationLayer,
                    portsLayer,
                    fullArchitecture);
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

    /**
     * Returns the application layer diagram as optional.
     *
     * @return optional application layer diagram
     * @since 5.0.0
     */
    public Optional<String> applicationLayerOpt() {
        return Optional.ofNullable(applicationLayer);
    }

    /**
     * Returns the application layer diagram wrapped in a markdown code block.
     *
     * @return wrapped diagram, or null if not present
     * @since 5.0.0
     */
    public String applicationLayerMarkdown() {
        return applicationLayer != null ? wrapInCodeBlock(applicationLayer) : null;
    }

    /**
     * Returns the ports layer diagram as optional.
     *
     * @return optional ports layer diagram
     * @since 5.0.0
     */
    public Optional<String> portsLayerOpt() {
        return Optional.ofNullable(portsLayer);
    }

    /**
     * Returns the ports layer diagram wrapped in a markdown code block.
     *
     * @return wrapped diagram, or null if not present
     * @since 5.0.0
     */
    public String portsLayerMarkdown() {
        return portsLayer != null ? wrapInCodeBlock(portsLayer) : null;
    }

    /**
     * Returns the full architecture diagram as optional.
     *
     * @return optional full architecture diagram
     * @since 5.0.0
     */
    public Optional<String> fullArchitectureOpt() {
        return Optional.ofNullable(fullArchitecture);
    }

    /**
     * Returns the full architecture diagram wrapped in a markdown code block.
     *
     * @return wrapped diagram, or null if not present
     * @since 5.0.0
     */
    public String fullArchitectureMarkdown() {
        return fullArchitecture != null ? wrapInCodeBlock(fullArchitecture) : null;
    }
}
