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

package io.hexaglue.plugin.jpa;

import io.hexaglue.spi.ir.Cardinality;
import io.hexaglue.spi.ir.DomainKind;
import io.hexaglue.spi.ir.DomainProperty;
import io.hexaglue.spi.ir.DomainRelation;
import io.hexaglue.spi.ir.DomainType;
import io.hexaglue.spi.ir.Identity;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Generates JPA entity source code from domain types.
 */
final class JpaEntityGenerator {

    private static final Set<String> JDK_PACKAGES = Set.of("java.", "javax.", "jakarta.", "sun.", "com.sun.", "jdk.");

    private static final Set<String> PRIMITIVE_TYPES =
            Set.of("boolean", "byte", "char", "short", "int", "long", "float", "double", "void");

    private final String infrastructurePackage;
    private final JpaConfig config;
    private final List<DomainType> allTypes;

    JpaEntityGenerator(String infrastructurePackage, JpaConfig config, List<DomainType> allTypes) {
        this.infrastructurePackage = infrastructurePackage;
        this.config = config;
        this.allTypes = allTypes;
    }

    /**
     * Checks if a type is likely an enum (custom type not in domain types).
     */
    private boolean isLikelyEnum(String qualifiedName) {
        // Primitive types are not enums
        if (PRIMITIVE_TYPES.contains(qualifiedName)) {
            return false;
        }
        // JDK types are not enums we care about
        for (String prefix : JDK_PACKAGES) {
            if (qualifiedName.startsWith(prefix)) {
                return false;
            }
        }
        // If it's a known domain type, it's not an enum
        return allTypes.stream().noneMatch(t -> t.qualifiedName().equals(qualifiedName));
    }

    /**
     * Checks if a property needs @Column(precision, scale) for currency.
     */
    private boolean isCurrencyAmount(DomainProperty prop) {
        String typeName = prop.type().unwrapElement().qualifiedName();
        return (typeName.equals("java.math.BigDecimal") || typeName.equals("BigDecimal"))
                && prop.name().equalsIgnoreCase("amount");
    }

    /**
     * Checks if a property is a byte array (large binary data).
     */
    private boolean isByteArray(DomainProperty prop) {
        String typeName = prop.type().qualifiedName();
        return typeName.equals("byte[]") || typeName.equals("[B");
    }

    /**
     * Finds an Identifier type by its qualified name and returns its unwrapped type.
     * Returns empty if the type is not an Identifier or has multiple properties (composite ID).
     *
     * <p>Single-property Identifiers (like {@code record CustomerId(UUID value)}) are unwrapped
     * to their inner type (UUID) for JPA mapping. Multi-property identifiers are treated as
     * composite keys and should use embeddables instead.
     */
    private Optional<String> findIdentifierUnwrappedType(String qualifiedName) {
        return allTypes.stream()
                .filter(t -> t.qualifiedName().equals(qualifiedName))
                .filter(t -> t.kind() == DomainKind.IDENTIFIER)
                .findFirst()
                .filter(t -> t.properties().size() == 1) // Only unwrap single-property identifiers
                .map(t -> t.properties().get(0).type().qualifiedName());
    }

    /**
     * Checks if a type is a composite Identifier (multi-property).
     *
     * <p>Composite identifiers (like {@code record CompositeOrderId(String region, Long sequence)})
     * should be treated as embeddables for JPA mapping.
     */
    private boolean isCompositeIdentifier(String qualifiedName) {
        return allTypes.stream()
                .filter(t -> t.qualifiedName().equals(qualifiedName))
                .filter(t -> t.kind() == DomainKind.IDENTIFIER)
                .findFirst()
                .map(t -> t.properties().size() > 1)
                .orElse(false);
    }

    /**
     * Generates JPA entity class for a domain entity or aggregate root.
     */
    String generateEntity(DomainType domainType) {
        String entityName = domainType.simpleName() + config.entitySuffix();
        StringBuilder sb = new StringBuilder();
        Set<String> imports = new HashSet<>();

        // Collect imports
        imports.add("jakarta.persistence.Entity");
        imports.add("jakarta.persistence.Id");
        imports.add("jakarta.persistence.Table");

        if (domainType.hasIdentity()) {
            Identity id = domainType.identity().get();
            collectIdImports(imports, id);
        }

        collectPropertyImports(imports, domainType);
        collectRelationImports(imports, domainType);

        if (config.enableAuditing()) {
            imports.add("org.springframework.data.annotation.CreatedDate");
            imports.add("org.springframework.data.annotation.LastModifiedDate");
            imports.add("org.springframework.data.jpa.domain.support.AuditingEntityListener");
            imports.add("jakarta.persistence.EntityListeners");
            imports.add("java.time.Instant");
        }

        if (config.enableOptimisticLocking()) {
            imports.add("jakarta.persistence.Version");
        }

        // Package
        sb.append("package ").append(infrastructurePackage).append(";\n\n");

        // Imports
        imports.stream()
                .sorted()
                .forEach(imp -> sb.append("import ").append(imp).append(";\n"));
        sb.append("\n");

        // Class javadoc
        sb.append("/**\n");
        sb.append(" * JPA entity for {@link ")
                .append(domainType.qualifiedName())
                .append("}.\n");
        sb.append(" *\n");
        sb.append(" * <p>Generated by HexaGlue JPA Plugin.\n");
        sb.append(" */\n");

        // Annotations
        sb.append("@Entity\n");
        sb.append("@Table(name = \"")
                .append(config.tablePrefix())
                .append(toSnakeCase(domainType.simpleName()))
                .append("\")\n");

        if (config.enableAuditing()) {
            sb.append("@EntityListeners(AuditingEntityListener.class)\n");
        }

        // Class declaration
        sb.append("public class ").append(entityName).append(" {\n\n");

        // Identity field
        domainType.identity().ifPresent(id -> generateIdentityField(sb, id));

        // Properties
        for (DomainProperty prop : domainType.properties()) {
            if (!prop.isIdentity()) {
                generatePropertyField(sb, prop, domainType);
            }
        }

        // Auditing fields
        if (config.enableAuditing()) {
            generateAuditingFields(sb);
        }

        // Version field for optimistic locking
        if (config.enableOptimisticLocking()) {
            generateVersionField(sb);
        }

        // Default constructor for JPA
        sb.append("    /**\n");
        sb.append("     * Default constructor for JPA.\n");
        sb.append("     */\n");
        sb.append("    public ").append(entityName).append("() {\n");
        sb.append("    }\n\n");

        // Getters and setters
        domainType.identity().ifPresent(id -> generateIdAccessors(sb, id));
        for (DomainProperty prop : domainType.properties()) {
            if (!prop.isIdentity()) {
                generatePropertyAccessors(sb, prop, domainType);
            }
        }

        if (config.enableAuditing()) {
            generateAuditingAccessors(sb);
        }

        if (config.enableOptimisticLocking()) {
            generateVersionAccessors(sb);
        }

        sb.append("}\n");

        return sb.toString();
    }

    /**
     * Generates JPA embeddable class for a value object.
     */
    String generateEmbeddable(DomainType valueObject) {
        String embeddableName = valueObject.simpleName() + "Embeddable";
        StringBuilder sb = new StringBuilder();
        Set<String> imports = new HashSet<>();

        imports.add("jakarta.persistence.Embeddable");
        collectPropertyImports(imports, valueObject);

        // Package
        sb.append("package ").append(infrastructurePackage).append(";\n\n");

        // Imports
        imports.stream()
                .sorted()
                .forEach(imp -> sb.append("import ").append(imp).append(";\n"));
        sb.append("\n");

        // Class javadoc
        sb.append("/**\n");
        sb.append(" * JPA embeddable for {@link ")
                .append(valueObject.qualifiedName())
                .append("}.\n");
        sb.append(" *\n");
        sb.append(" * <p>Generated by HexaGlue JPA Plugin.\n");
        sb.append(" */\n");

        // Annotations
        sb.append("@Embeddable\n");

        // Class declaration
        sb.append("public class ").append(embeddableName).append(" {\n\n");

        // Properties
        for (DomainProperty prop : valueObject.properties()) {
            generateSimpleField(sb, prop);
        }

        // Default constructor for JPA
        sb.append("    /**\n");
        sb.append("     * Default constructor for JPA.\n");
        sb.append("     */\n");
        sb.append("    public ").append(embeddableName).append("() {\n");
        sb.append("    }\n\n");

        // All-args constructor
        generateAllArgsConstructor(sb, embeddableName, valueObject);

        // Getters
        for (DomainProperty prop : valueObject.properties()) {
            generateGetter(sb, prop);
        }

        sb.append("}\n");

        return sb.toString();
    }

    private void collectIdImports(Set<String> imports, Identity id) {
        String declaredTypeName = id.type().qualifiedName();

        // Check if this is a composite identifier (multi-property)
        if (isCompositeIdentifier(declaredTypeName)) {
            imports.add("jakarta.persistence.EmbeddedId");
            return;
        }

        String typeName = id.unwrappedType().qualifiedName();
        if (typeName.equals("java.util.UUID")) {
            imports.add("java.util.UUID");
        }
        if (id.strategy() == io.hexaglue.spi.ir.IdentityStrategy.AUTO
                || id.strategy() == io.hexaglue.spi.ir.IdentityStrategy.SEQUENCE) {
            imports.add("jakarta.persistence.GeneratedValue");
            imports.add("jakarta.persistence.GenerationType");
        }
    }

    private void collectPropertyImports(Set<String> imports, DomainType type) {
        for (DomainProperty prop : type.properties()) {
            String typeName = prop.type().unwrapElement().qualifiedName();

            // Check if this is a composite identifier - needs @Embedded
            if (isCompositeIdentifier(typeName)) {
                imports.add("jakarta.persistence.Embedded");
                continue;
            }

            // Check if this is a single-property Identifier type - unwrap to primitive
            Optional<String> unwrappedType = findIdentifierUnwrappedType(typeName);
            if (unwrappedType.isPresent()) {
                // Add import for the unwrapped type (e.g., UUID)
                if (needsImport(unwrappedType.get())) {
                    imports.add(unwrappedType.get());
                }
            } else if (needsImport(typeName)) {
                imports.add(typeName);
            }

            if (prop.cardinality() == Cardinality.COLLECTION) {
                // Check if collection of enums
                if (isLikelyEnum(typeName)) {
                    imports.add("java.util.Set");
                    imports.add("java.util.HashSet");
                    imports.add("jakarta.persistence.ElementCollection");
                    imports.add("jakarta.persistence.Enumerated");
                    imports.add("jakarta.persistence.EnumType");
                } else {
                    imports.add("java.util.List");
                    imports.add("java.util.ArrayList");
                }
            } else if (isLikelyEnum(typeName)) {
                // Single enum property
                imports.add("jakarta.persistence.Enumerated");
                imports.add("jakarta.persistence.EnumType");
            }

            // Check for @Lob (byte arrays)
            if (isByteArray(prop)) {
                imports.add("jakarta.persistence.Lob");
            }

            // Check for @Column with precision/scale (currency amounts)
            if (isCurrencyAmount(prop)) {
                imports.add("jakarta.persistence.Column");
            }

            if (prop.isEmbedded()) {
                imports.add("jakarta.persistence.Embedded");
            }
        }
    }

    private void collectRelationImports(Set<String> imports, DomainType type) {
        for (DomainRelation rel : type.relations()) {
            switch (rel.kind()) {
                case ONE_TO_ONE -> imports.add("jakarta.persistence.OneToOne");
                case ONE_TO_MANY -> {
                    imports.add("jakarta.persistence.OneToMany");
                    imports.add("jakarta.persistence.CascadeType");
                    imports.add("java.util.List");
                    imports.add("java.util.ArrayList");
                }
                case MANY_TO_ONE -> imports.add("jakarta.persistence.ManyToOne");
                case MANY_TO_MANY -> {
                    imports.add("jakarta.persistence.ManyToMany");
                    imports.add("java.util.Set");
                    imports.add("java.util.HashSet");
                }
                case EMBEDDED -> imports.add("jakarta.persistence.Embedded");
                case ELEMENT_COLLECTION -> {
                    imports.add("jakarta.persistence.ElementCollection");
                    imports.add("jakarta.persistence.CollectionTable");
                    imports.add("java.util.List");
                    imports.add("java.util.ArrayList");
                }
            }
        }
    }

    private boolean needsImport(String typeName) {
        return typeName.contains(".") && !typeName.startsWith("java.lang.") && !typeName.equals("java.lang.String");
    }

    private void generateIdentityField(StringBuilder sb, Identity id) {
        String declaredTypeName = id.type().qualifiedName();

        // Check if this is a composite identifier (multi-property)
        if (isCompositeIdentifier(declaredTypeName)) {
            String embeddableType = id.type().simpleName() + "Embeddable";
            sb.append("    @EmbeddedId\n");
            sb.append("    private ")
                    .append(embeddableType)
                    .append(" ")
                    .append(id.fieldName())
                    .append(";\n\n");
            return;
        }

        sb.append("    @Id\n");

        if (id.strategy() == io.hexaglue.spi.ir.IdentityStrategy.AUTO) {
            sb.append("    @GeneratedValue(strategy = GenerationType.AUTO)\n");
        } else if (id.strategy() == io.hexaglue.spi.ir.IdentityStrategy.SEQUENCE) {
            sb.append("    @GeneratedValue(strategy = GenerationType.SEQUENCE)\n");
        }

        String jpaType = mapToJpaType(id.unwrappedType().qualifiedName());
        sb.append("    private ")
                .append(jpaType)
                .append(" ")
                .append(id.fieldName())
                .append(";\n\n");
    }

    private void generatePropertyField(StringBuilder sb, DomainProperty prop, DomainType domainType) {
        // Check if this property has a relation
        DomainRelation relation = findRelationForProperty(prop, domainType);

        if (relation != null) {
            generateRelationField(sb, prop, relation);
        } else if (prop.isEmbedded()) {
            sb.append("    @Embedded\n");
            String embeddableType = prop.type().simpleName() + "Embeddable";
            sb.append("    private ")
                    .append(embeddableType)
                    .append(" ")
                    .append(prop.name())
                    .append(";\n\n");
        } else {
            generateSimpleField(sb, prop);
        }
    }

    private DomainRelation findRelationForProperty(DomainProperty prop, DomainType type) {
        return type.relations().stream()
                .filter(r -> r.propertyName().equals(prop.name()))
                .findFirst()
                .orElse(null);
    }

    private void generateRelationField(StringBuilder sb, DomainProperty prop, DomainRelation relation) {
        String targetEntity = relation.targetSimpleName() + config.entitySuffix();

        switch (relation.kind()) {
            case ONE_TO_ONE -> {
                sb.append("    @OneToOne");
                if (relation.mappedBy() != null) {
                    sb.append("(mappedBy = \"").append(relation.mappedBy()).append("\")");
                }
                sb.append("\n");
                sb.append("    private ")
                        .append(targetEntity)
                        .append(" ")
                        .append(prop.name())
                        .append(";\n\n");
            }
            case ONE_TO_MANY -> {
                sb.append("    @OneToMany(");
                if (relation.mappedBy() != null) {
                    sb.append("mappedBy = \"").append(relation.mappedBy()).append("\", ");
                }
                sb.append("cascade = CascadeType.ALL, orphanRemoval = true)\n");
                sb.append("    private List<")
                        .append(targetEntity)
                        .append("> ")
                        .append(prop.name())
                        .append(" = new ArrayList<>();\n\n");
            }
            case MANY_TO_ONE -> {
                sb.append("    @ManyToOne\n");
                sb.append("    private ")
                        .append(targetEntity)
                        .append(" ")
                        .append(prop.name())
                        .append(";\n\n");
            }
            case MANY_TO_MANY -> {
                sb.append("    @ManyToMany");
                if (relation.mappedBy() != null) {
                    sb.append("(mappedBy = \"").append(relation.mappedBy()).append("\")");
                }
                sb.append("\n");
                sb.append("    private Set<")
                        .append(targetEntity)
                        .append("> ")
                        .append(prop.name())
                        .append(" = new HashSet<>();\n\n");
            }
            case EMBEDDED -> {
                sb.append("    @Embedded\n");
                String embeddableType = relation.targetSimpleName() + "Embeddable";
                sb.append("    private ")
                        .append(embeddableType)
                        .append(" ")
                        .append(prop.name())
                        .append(";\n\n");
            }
            case ELEMENT_COLLECTION -> {
                String embeddableType = relation.targetSimpleName() + "Embeddable";
                sb.append("    @ElementCollection\n");
                sb.append("    @CollectionTable(name = \"")
                        .append(toSnakeCase(prop.name()))
                        .append("\")\n");
                sb.append("    private List<")
                        .append(embeddableType)
                        .append("> ")
                        .append(prop.name())
                        .append(" = new ArrayList<>();\n\n");
            }
        }
    }

    private void generateSimpleField(StringBuilder sb, DomainProperty prop) {
        String propTypeName = prop.type().unwrapElement().qualifiedName();

        // Check if this is a composite identifier (multi-property) - treat as embedded
        if (isCompositeIdentifier(propTypeName)) {
            String embeddableType = prop.type().simpleName() + "Embeddable";
            sb.append("    @Embedded\n");
            sb.append("    private ")
                    .append(embeddableType)
                    .append(" ")
                    .append(prop.name())
                    .append(";\n\n");
            return;
        }

        // Check if this is a single-property identifier - unwrap to primitive type
        String jpaType = findIdentifierUnwrappedType(propTypeName)
                .map(this::mapToJpaType)
                .orElseGet(() -> mapToJpaType(propTypeName));

        // Handle collection of enums
        if (prop.cardinality() == Cardinality.COLLECTION && isLikelyEnum(propTypeName)) {
            sb.append("    @ElementCollection\n");
            sb.append("    @Enumerated(EnumType.STRING)\n");
            sb.append("    private Set<")
                    .append(jpaType)
                    .append("> ")
                    .append(prop.name())
                    .append(";\n\n");
            return;
        }

        // Handle @Lob for byte arrays
        if (isByteArray(prop)) {
            sb.append("    @Lob\n");
            sb.append("    private byte[] ").append(prop.name()).append(";\n\n");
            return;
        }

        // Handle @Column with precision/scale for currency amounts
        if (isCurrencyAmount(prop)) {
            sb.append("    @Column(precision = 19, scale = 2)\n");
        }

        // Handle single enum property
        if (prop.cardinality() != Cardinality.COLLECTION && isLikelyEnum(propTypeName)) {
            sb.append("    @Enumerated(EnumType.STRING)\n");
        }

        if (prop.cardinality() == Cardinality.COLLECTION) {
            sb.append("    private List<")
                    .append(jpaType)
                    .append("> ")
                    .append(prop.name())
                    .append(";\n\n");
        } else {
            sb.append("    private ")
                    .append(jpaType)
                    .append(" ")
                    .append(prop.name())
                    .append(";\n\n");
        }
    }

    private void generateAuditingFields(StringBuilder sb) {
        sb.append("    @CreatedDate\n");
        sb.append("    private Instant createdAt;\n\n");

        sb.append("    @LastModifiedDate\n");
        sb.append("    private Instant updatedAt;\n\n");
    }

    private void generateVersionField(StringBuilder sb) {
        sb.append("    @Version\n");
        sb.append("    private Long version;\n\n");
    }

    private void generateIdAccessors(StringBuilder sb, Identity id) {
        String jpaType = mapToJpaType(id.unwrappedType().qualifiedName());
        String fieldName = id.fieldName();
        String capName = capitalize(fieldName);

        sb.append("    public ").append(jpaType).append(" get").append(capName).append("() {\n");
        sb.append("        return ").append(fieldName).append(";\n");
        sb.append("    }\n\n");

        sb.append("    public void set")
                .append(capName)
                .append("(")
                .append(jpaType)
                .append(" ")
                .append(fieldName)
                .append(") {\n");
        sb.append("        this.")
                .append(fieldName)
                .append(" = ")
                .append(fieldName)
                .append(";\n");
        sb.append("    }\n\n");
    }

    private void generatePropertyAccessors(StringBuilder sb, DomainProperty prop, DomainType domainType) {
        String fieldType = getFieldType(prop, domainType);
        String fieldName = prop.name();
        String capName = capitalize(fieldName);

        // Getter
        sb.append("    public ")
                .append(fieldType)
                .append(" get")
                .append(capName)
                .append("() {\n");
        sb.append("        return ").append(fieldName).append(";\n");
        sb.append("    }\n\n");

        // Setter
        sb.append("    public void set")
                .append(capName)
                .append("(")
                .append(fieldType)
                .append(" ")
                .append(fieldName)
                .append(") {\n");
        sb.append("        this.")
                .append(fieldName)
                .append(" = ")
                .append(fieldName)
                .append(";\n");
        sb.append("    }\n\n");
    }

    private void generateGetter(StringBuilder sb, DomainProperty prop) {
        String jpaType = mapToJpaType(prop.type().unwrapElement().qualifiedName());
        String fieldName = prop.name();
        String capName = capitalize(fieldName);

        sb.append("    public ").append(jpaType).append(" get").append(capName).append("() {\n");
        sb.append("        return ").append(fieldName).append(";\n");
        sb.append("    }\n\n");
    }

    private void generateAuditingAccessors(StringBuilder sb) {
        sb.append("    public Instant getCreatedAt() {\n");
        sb.append("        return createdAt;\n");
        sb.append("    }\n\n");

        sb.append("    public Instant getUpdatedAt() {\n");
        sb.append("        return updatedAt;\n");
        sb.append("    }\n\n");
    }

    private void generateVersionAccessors(StringBuilder sb) {
        sb.append("    public Long getVersion() {\n");
        sb.append("        return version;\n");
        sb.append("    }\n\n");
    }

    private void generateAllArgsConstructor(StringBuilder sb, String className, DomainType type) {
        sb.append("    public ").append(className).append("(");

        boolean first = true;
        for (DomainProperty prop : type.properties()) {
            if (!first) sb.append(", ");
            String jpaType = mapToJpaType(prop.type().unwrapElement().qualifiedName());
            sb.append(jpaType).append(" ").append(prop.name());
            first = false;
        }
        sb.append(") {\n");

        for (DomainProperty prop : type.properties()) {
            sb.append("        this.")
                    .append(prop.name())
                    .append(" = ")
                    .append(prop.name())
                    .append(";\n");
        }
        sb.append("    }\n\n");
    }

    private String getFieldType(DomainProperty prop, DomainType domainType) {
        // Check if this property has a relation
        DomainRelation relation = findRelationForProperty(prop, domainType);

        if (relation != null) {
            String targetEntity = relation.targetSimpleName() + config.entitySuffix();
            return switch (relation.kind()) {
                case ONE_TO_ONE, MANY_TO_ONE -> targetEntity;
                case ONE_TO_MANY -> "List<" + targetEntity + ">";
                case MANY_TO_MANY -> "Set<" + targetEntity + ">";
                case EMBEDDED -> relation.targetSimpleName() + "Embeddable";
                case ELEMENT_COLLECTION -> "List<" + relation.targetSimpleName() + "Embeddable>";
            };
        }

        String propTypeName = prop.type().unwrapElement().qualifiedName();

        // Check if this is an inter-aggregate reference (Identifier type like CustomerId)
        String baseType = findIdentifierUnwrappedType(propTypeName)
                .map(this::mapToJpaType)
                .orElseGet(() -> mapToJpaType(propTypeName));

        if (prop.isEmbedded()) {
            return prop.type().simpleName() + "Embeddable";
        }
        if (prop.cardinality() == Cardinality.COLLECTION) {
            return "List<" + baseType + ">";
        }
        return baseType;
    }

    private String mapToJpaType(String domainType) {
        return switch (domainType) {
            case "java.util.UUID", "UUID" -> "UUID";
            case "java.lang.String", "String" -> "String";
            case "java.lang.Long", "Long", "long" -> "Long";
            case "java.lang.Integer", "Integer", "int" -> "Integer";
            case "java.math.BigDecimal", "BigDecimal" -> "BigDecimal";
            case "java.time.LocalDate", "LocalDate" -> "LocalDate";
            case "java.time.LocalDateTime", "LocalDateTime" -> "LocalDateTime";
            case "java.time.Instant", "Instant" -> "Instant";
            default -> {
                int lastDot = domainType.lastIndexOf('.');
                yield lastDot >= 0 ? domainType.substring(lastDot + 1) : domainType;
            }
        };
    }

    private String toSnakeCase(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
