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

package io.hexaglue.plugin.jpa.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Complete specification for generating a JPA embeddable class.
 *
 * <p>This record aggregates all the information needed to generate a JPA embeddable:
 * properties and metadata. Embeddables are used for VALUE_OBJECTs that need to be
 * persisted within entities (via @Embedded or @ElementCollection).
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * EmbeddableSpec spec = EmbeddableSpec.builder()
 *     .packageName("com.example.infrastructure.jpa")
 *     .className("LineItemEmbeddable")
 *     .domainQualifiedName("com.example.domain.LineItem")
 *     .addProperty(propertySpec1)
 *     .addProperty(propertySpec2)
 *     .build();
 * }</pre>
 *
 * @param packageName the package for the generated embeddable class
 * @param className the simple class name (e.g., "LineItemEmbeddable")
 * @param domainQualifiedName the fully qualified name of the domain type
 * @param properties the list of property fields
 * @since 3.0.0
 */
public record EmbeddableSpec(
        String packageName,
        String className,
        String domainQualifiedName,
        List<PropertyFieldSpec> properties) {

    /**
     * Creates a new builder instance.
     *
     * @return a new EmbeddableSpec builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the fully qualified class name.
     *
     * @return packageName + "." + className
     */
    public String fullyQualifiedClassName() {
        return packageName + "." + className;
    }

    /**
     * Returns the simple name of the domain type.
     *
     * @return the simple name extracted from domainQualifiedName
     */
    public String domainSimpleName() {
        int lastDot = domainQualifiedName.lastIndexOf('.');
        return lastDot < 0 ? domainQualifiedName : domainQualifiedName.substring(lastDot + 1);
    }

    /**
     * Returns true if this embeddable has any properties.
     *
     * @return true if properties list is not empty
     */
    public boolean hasProperties() {
        return !properties.isEmpty();
    }

    /**
     * Builder for EmbeddableSpec.
     *
     * <p>Provides a fluent API for constructing EmbeddableSpec instances.
     */
    public static class Builder {
        private String packageName;
        private String className;
        private String domainQualifiedName;
        private final List<PropertyFieldSpec> properties = new ArrayList<>();

        /**
         * Sets the package name.
         *
         * @param packageName the package name
         * @return this builder
         */
        public Builder packageName(String packageName) {
            this.packageName = packageName;
            return this;
        }

        /**
         * Sets the class name.
         *
         * @param className the simple class name
         * @return this builder
         */
        public Builder className(String className) {
            this.className = className;
            return this;
        }

        /**
         * Sets the domain qualified name.
         *
         * @param domainQualifiedName the fully qualified domain type name
         * @return this builder
         */
        public Builder domainQualifiedName(String domainQualifiedName) {
            this.domainQualifiedName = domainQualifiedName;
            return this;
        }

        /**
         * Adds a property field.
         *
         * @param property the property field spec
         * @return this builder
         */
        public Builder addProperty(PropertyFieldSpec property) {
            this.properties.add(property);
            return this;
        }

        /**
         * Adds multiple property fields.
         *
         * @param properties the property field specs to add
         * @return this builder
         */
        public Builder addProperties(List<PropertyFieldSpec> properties) {
            this.properties.addAll(properties);
            return this;
        }

        /**
         * Builds the EmbeddableSpec.
         *
         * @return a new immutable EmbeddableSpec instance
         * @throws IllegalStateException if required fields are missing
         */
        public EmbeddableSpec build() {
            if (packageName == null || packageName.isEmpty()) {
                throw new IllegalStateException("packageName is required");
            }
            if (className == null || className.isEmpty()) {
                throw new IllegalStateException("className is required");
            }
            if (domainQualifiedName == null || domainQualifiedName.isEmpty()) {
                throw new IllegalStateException("domainQualifiedName is required");
            }

            return new EmbeddableSpec(
                    packageName,
                    className,
                    domainQualifiedName,
                    List.copyOf(properties));
        }
    }
}
