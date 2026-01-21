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
 * Complete specification for generating a JPA entity class.
 *
 * <p>This record aggregates all the information needed to generate a JPA entity:
 * identity, properties, relationships, and metadata annotations.
 *
 * <p>Design decision: Using a Builder pattern (despite records) for complex
 * construction with many optional fields. The record provides immutability
 * benefits while the builder enables fluent API for construction.
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * EntitySpec spec = EntitySpec.builder()
 *     .packageName("com.example.infrastructure.jpa")
 *     .className("OrderEntity")
 *     .tableName("orders")
 *     .domainQualifiedName("com.example.domain.Order")
 *     .idField(idSpec)
 *     .addProperty(propertySpec1)
 *     .addProperty(propertySpec2)
 *     .addRelation(relationSpec)
 *     .enableAuditing(true)
 *     .build();
 * }</pre>
 *
 * @param packageName the package for the generated entity class
 * @param className the simple class name (e.g., "OrderEntity")
 * @param tableName the database table name (e.g., "orders")
 * @param domainQualifiedName the fully qualified name of the domain type
 * @param idField the identity field specification
 * @param properties the list of simple property fields
 * @param relations the list of relationship fields
 * @param enableAuditing true to add JPA auditing annotations
 * @param enableOptimisticLocking true to add version field for optimistic locking
 * @since 2.0.0
 */
public record EntitySpec(
        String packageName,
        String className,
        String tableName,
        String domainQualifiedName,
        IdFieldSpec idField,
        List<PropertyFieldSpec> properties,
        List<RelationFieldSpec> relations,
        boolean enableAuditing,
        boolean enableOptimisticLocking) {

    public EntitySpec {
        properties = List.copyOf(properties);
        relations = List.copyOf(relations);
    }

    /**
     * Creates a new builder instance.
     *
     * @return a new EntitySpec builder
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
     * Returns true if this entity has any relationships.
     *
     * @return true if relations list is not empty
     */
    public boolean hasRelations() {
        return !relations.isEmpty();
    }

    /**
     * Returns true if this entity has any simple properties.
     *
     * @return true if properties list is not empty
     */
    public boolean hasProperties() {
        return !properties.isEmpty();
    }

    /**
     * Builder for EntitySpec.
     *
     * <p>Provides a fluent API for constructing complex EntitySpec instances.
     * All fields except the lists have default values.
     */
    public static class Builder {
        private String packageName;
        private String className;
        private String tableName;
        private String domainQualifiedName;
        private IdFieldSpec idField;
        private final List<PropertyFieldSpec> properties = new ArrayList<>();
        private final List<RelationFieldSpec> relations = new ArrayList<>();
        private boolean enableAuditing = false;
        private boolean enableOptimisticLocking = false;

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
         * Sets the table name.
         *
         * @param tableName the database table name
         * @return this builder
         */
        public Builder tableName(String tableName) {
            this.tableName = tableName;
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
         * Sets the identity field.
         *
         * @param idField the identity field spec
         * @return this builder
         */
        public Builder idField(IdFieldSpec idField) {
            this.idField = idField;
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
         * Adds a relation field.
         *
         * @param relation the relation field spec
         * @return this builder
         */
        public Builder addRelation(RelationFieldSpec relation) {
            this.relations.add(relation);
            return this;
        }

        /**
         * Adds multiple relation fields.
         *
         * @param relations the relation field specs to add
         * @return this builder
         */
        public Builder addRelations(List<RelationFieldSpec> relations) {
            this.relations.addAll(relations);
            return this;
        }

        /**
         * Enables or disables JPA auditing.
         *
         * @param enableAuditing true to enable auditing
         * @return this builder
         */
        public Builder enableAuditing(boolean enableAuditing) {
            this.enableAuditing = enableAuditing;
            return this;
        }

        /**
         * Enables or disables optimistic locking.
         *
         * @param enableOptimisticLocking true to enable optimistic locking
         * @return this builder
         */
        public Builder enableOptimisticLocking(boolean enableOptimisticLocking) {
            this.enableOptimisticLocking = enableOptimisticLocking;
            return this;
        }

        /**
         * Builds the EntitySpec.
         *
         * @return a new immutable EntitySpec instance
         * @throws IllegalStateException if required fields are missing
         */
        public EntitySpec build() {
            if (packageName == null || packageName.isEmpty()) {
                throw new IllegalStateException("packageName is required");
            }
            if (className == null || className.isEmpty()) {
                throw new IllegalStateException("className is required");
            }
            if (tableName == null || tableName.isEmpty()) {
                throw new IllegalStateException("tableName is required");
            }
            if (domainQualifiedName == null || domainQualifiedName.isEmpty()) {
                throw new IllegalStateException("domainQualifiedName is required");
            }
            if (idField == null) {
                throw new IllegalStateException("idField is required");
            }

            return new EntitySpec(
                    packageName,
                    className,
                    tableName,
                    domainQualifiedName,
                    idField,
                    List.copyOf(properties),
                    List.copyOf(relations),
                    enableAuditing,
                    enableOptimisticLocking);
        }
    }
}
