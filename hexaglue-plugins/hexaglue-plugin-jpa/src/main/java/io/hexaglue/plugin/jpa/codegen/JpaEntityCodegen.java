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

package io.hexaglue.plugin.jpa.codegen;

import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import io.hexaglue.arch.model.ir.Nullability;
import io.hexaglue.plugin.jpa.model.EntitySpec;
import io.hexaglue.plugin.jpa.model.IdFieldSpec;
import io.hexaglue.plugin.jpa.model.PropertyFieldSpec;
import io.hexaglue.plugin.jpa.model.RelationFieldSpec;
import io.hexaglue.plugin.jpa.util.JpaAnnotations;
import io.hexaglue.plugin.jpa.util.NamingConventions;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import javax.lang.model.element.Modifier;

/**
 * JavaPoet-based code generator for JPA entity classes.
 *
 * <p>This generator produces complete JPA entity classes with all required annotations,
 * fields, constructors, and accessor methods. It strictly follows JPA conventions:
 * <ul>
 *   <li>Public class with {@code @Entity} and {@code @Table} annotations</li>
 *   <li>Primary key field with {@code @Id} and optional {@code @GeneratedValue}</li>
 *   <li>Property fields with {@code @Column} annotations (snake_case naming)</li>
 *   <li>Relationship fields with appropriate JPA annotations ({@code @OneToMany}, etc.)</li>
 *   <li>No-args constructor required by JPA specification</li>
 *   <li>Getter/setter methods for all fields</li>
 *   <li>Collection fields initialized to avoid NullPointerException</li>
 * </ul>
 *
 * <h3>Design Decisions:</h3>
 * <ul>
 *   <li><b>Immutability through delegation</b>: Uses utility classes ({@link JpaAnnotations},
 *       {@link NamingConventions}) for annotation generation</li>
 *   <li><b>Single Responsibility</b>: Focuses solely on entity generation, not repositories or mappers</li>
 *   <li><b>Type Safety</b>: Leverages JavaPoet's type-safe API for all code generation</li>
 *   <li><b>DRY Principle</b>: Reuses annotation builders from {@link JpaAnnotations}</li>
 * </ul>
 *
 * <h3>Generated Code Example:</h3>
 * <pre>{@code
 * @Generated("io.hexaglue.plugin.jpa")
 * @Entity
 * @Table(name = "orders")
 * public class OrderEntity {
 *
 *     @Id
 *     @GeneratedValue(strategy = GenerationType.AUTO)
 *     @Column(name = "id")
 *     private UUID id;
 *
 *     @Column(name = "total_amount", nullable = false)
 *     private BigDecimal totalAmount;
 *
 *     @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
 *     private List<OrderItemEntity> items = new ArrayList<>();
 *
 *     public OrderEntity() {}
 *
 *     public UUID getId() { return id; }
 *     public void setId(UUID id) { this.id = id; }
 *     // ... other getters/setters
 * }
 * }</pre>
 *
 * @since 2.0.0
 */
public final class JpaEntityCodegen {

    /**
     * Generator identifier used in {@code @Generated} annotations.
     */
    private static final String GENERATOR_NAME = "io.hexaglue.plugin.jpa";

    /**
     * The plugin version, read from MANIFEST.MF at runtime.
     */
    private static final String PLUGIN_VERSION = Optional.ofNullable(
                    JpaEntityCodegen.class.getPackage().getImplementationVersion())
            .orElse("dev");

    private JpaEntityCodegen() {
        // Utility class - prevent instantiation
    }

    /**
     * Generates a complete JPA entity class from an entity specification.
     *
     * <p>This is the main entry point for entity generation. It orchestrates
     * the entire generation process:
     * <ol>
     *   <li>Create class builder with annotations</li>
     *   <li>Add identity field</li>
     *   <li>Add property fields</li>
     *   <li>Add relationship fields</li>
     *   <li>Add no-args constructor</li>
     *   <li>Add getters and setters</li>
     * </ol>
     *
     * @param spec the entity specification containing all generation metadata
     * @return a TypeSpec representing the complete entity class
     * @throws IllegalArgumentException if spec is null
     */
    public static TypeSpec generate(EntitySpec spec) {
        if (spec == null) {
            throw new IllegalArgumentException("EntitySpec cannot be null");
        }

        TypeSpec.Builder builder = TypeSpec.classBuilder(spec.className())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(JpaAnnotations.generated(GENERATOR_NAME, PLUGIN_VERSION));

        // Add @EntityListeners if auditing is enabled (BUG-006 fix)
        if (spec.enableAuditing()) {
            builder.addAnnotation(JpaAnnotations.entityListeners());
        }

        builder.addAnnotation(JpaAnnotations.entity()).addAnnotation(JpaAnnotations.table(spec.tableName()));

        // Add identity field
        addIdField(builder, spec.idField());

        // Add property fields
        for (PropertyFieldSpec property : spec.properties()) {
            addPropertyField(builder, property);
        }

        // Add relationship fields
        for (RelationFieldSpec relation : spec.relations()) {
            addRelationField(builder, relation, spec);
        }

        // Add @Version field if optimistic locking is enabled (BUG-007 fix)
        if (spec.enableOptimisticLocking()) {
            addVersionField(builder);
        }

        // Add auditing fields if enabled (BUG-006 fix)
        if (spec.enableAuditing()) {
            addAuditingFields(builder);
        }

        // Add no-args constructor (required by JPA)
        builder.addMethod(createNoArgsConstructor());

        // Add getters and setters for all fields
        addAccessors(builder, spec);

        return builder.build();
    }

    /**
     * Generates a complete JavaFile for the entity.
     *
     * <p>This method wraps the generated TypeSpec in a JavaFile with proper
     * package declaration and indentation settings.
     *
     * <p>The generated file uses 4-space indentation (Palantir Java Format standard).
     *
     * @param spec the entity specification
     * @return a JavaFile ready to be written to disk
     * @throws IllegalArgumentException if spec is null
     */
    public static JavaFile generateFile(EntitySpec spec) {
        if (spec == null) {
            throw new IllegalArgumentException("EntitySpec cannot be null");
        }

        return JavaFile.builder(spec.packageName(), generate(spec))
                .indent("    ") // 4 spaces
                .build();
    }

    // =====================================================================
    // Field generation methods
    // =====================================================================

    /**
     * Adds the identity field with {@code @Id}, {@code @GeneratedValue}, and {@code @Column} annotations.
     *
     * <p>The {@code @GeneratedValue} annotation is only added if the identity strategy
     * requires it (e.g., AUTO, IDENTITY, UUID). Natural/assigned identities do not
     * receive this annotation.
     *
     * <p>For wrapped identity types (e.g., {@code OrderId}), the field uses the unwrapped
     * type directly. AttributeConverter generation is handled separately.
     *
     * @param builder the class builder
     * @param id the identity field specification
     */
    private static void addIdField(TypeSpec.Builder builder, IdFieldSpec id) {
        // Use unwrapped type for JPA persistence
        TypeName fieldType = id.isWrapped() ? id.unwrappedType() : id.javaType();

        FieldSpec.Builder fieldBuilder = FieldSpec.builder(fieldType, id.fieldName(), Modifier.PRIVATE)
                .addAnnotation(JpaAnnotations.id())
                .addAnnotation(JpaAnnotations.column(
                        NamingConventions.toColumnName(id.fieldName()),
                        io.hexaglue.arch.model.ir.Nullability.NON_NULL));

        // Add @GeneratedValue if the strategy requires it
        if (id.requiresGeneratedValue()) {
            // Build @GeneratedValue annotation directly since we can't create Identity instances
            String jpaStrategy = id.jpaGenerationType();
            if (jpaStrategy != null) {
                AnnotationSpec generatedValue = AnnotationSpec.builder(jakarta.persistence.GeneratedValue.class)
                        .addMember("strategy", "$T.$L", jakarta.persistence.GenerationType.class, jpaStrategy)
                        .build();
                fieldBuilder.addAnnotation(generatedValue);
            }
        }

        builder.addField(fieldBuilder.build());
    }

    /**
     * Adds a simple property field with appropriate JPA annotation.
     *
     * <p>The annotation is selected based on the property type:
     * <ul>
     *   <li>Enum types: {@code @Enumerated(EnumType.STRING)} + {@code @Column}</li>
     *   <li>Embedded value objects: {@code @Embedded} (with {@code @AttributeOverrides} if needed)</li>
     *   <li>Wrapped foreign keys: {@code @Column} with unwrapped type (e.g., CustomerId → UUID)</li>
     *   <li>Simple properties: {@code @Column} with snake_case name and nullability</li>
     * </ul>
     *
     * <p>Value Objects are automatically treated as embedded because they are immutable
     * and identity-less, making them ideal candidates for JPA's {@code @Embedded} mapping.
     *
     * <p>When multiple embedded fields of the same type exist, {@code @AttributeOverrides}
     * is added to specify distinct column names for each embedded field's attributes.
     *
     * <p>Wrapped foreign keys (e.g., {@code CustomerId}) are unwrapped to their primitive
     * type (e.g., {@code UUID}) for JPA persistence, with conversion handled by MapStruct.
     *
     * @param builder the class builder
     * @param property the property field specification
     */
    private static void addPropertyField(TypeSpec.Builder builder, PropertyFieldSpec property) {
        // Use effectiveJpaType() to get unwrapped type for foreign keys
        TypeName fieldType = property.effectiveJpaType();
        FieldSpec.Builder fieldBuilder = FieldSpec.builder(fieldType, property.fieldName(), Modifier.PRIVATE);

        if (property.isEnum()) {
            // Enum types need @Enumerated(EnumType.STRING) for readable persistence
            fieldBuilder.addAnnotation(JpaAnnotations.enumerated());
            fieldBuilder.addAnnotation(JpaAnnotations.column(property.columnName(), property.nullability()));
        } else if (property.shouldBeEmbedded()) {
            fieldBuilder.addAnnotation(JpaAnnotations.embedded());
            // Add @AttributeOverrides if there are column name conflicts
            if (property.hasAttributeOverrides()) {
                JpaAnnotations.attributeOverrides(property.attributeOverrides()).ifPresent(fieldBuilder::addAnnotation);
            }
        } else {
            // Simple properties and wrapped foreign keys both use @Column
            fieldBuilder.addAnnotation(JpaAnnotations.column(property.columnName(), property.nullability()));
        }

        builder.addField(fieldBuilder.build());
    }

    /**
     * Adds a relationship field with appropriate JPA annotation.
     *
     * <p>Delegates to {@link JpaAnnotations#relationAnnotation(RelationFieldSpec)} to
     * generate the correct annotation based on the relationship kind.
     *
     * <p>For collection relationships (one-to-many, many-to-many, element collection),
     * initializes the field to an empty {@code ArrayList} to avoid null pointer issues.
     *
     * <p>For {@code @ElementCollection}, also adds {@code @CollectionTable} annotation
     * with proper table and join column names.
     *
     * <p>For {@code @Embedded} relations with attribute overrides, also adds
     * {@code @AttributeOverrides} to specify distinct column names when multiple
     * embedded fields of the same type exist.
     *
     * @param builder the class builder
     * @param relation the relation field specification
     * @param entitySpec the entity specification (for table name derivation)
     */
    private static void addRelationField(TypeSpec.Builder builder, RelationFieldSpec relation, EntitySpec entitySpec) {
        FieldSpec.Builder fieldBuilder = FieldSpec.builder(
                        relation.targetType(), relation.fieldName(), Modifier.PRIVATE)
                .addAnnotation(JpaAnnotations.relationAnnotation(relation));

        // For EMBEDDED relations, add @AttributeOverrides if there are column name conflicts
        if (relation.kind() == io.hexaglue.arch.model.ir.RelationKind.EMBEDDED && relation.hasAttributeOverrides()) {
            JpaAnnotations.attributeOverrides(relation.attributeOverrides()).ifPresent(fieldBuilder::addAnnotation);
        }

        // For ELEMENT_COLLECTION, add @CollectionTable annotation
        if (relation.kind() == io.hexaglue.arch.model.ir.RelationKind.ELEMENT_COLLECTION) {
            // Derive collection table name: entity_table + "_" + field_name (plural)
            String tableName = NamingConventions.toSnakeCase(entitySpec.domainSimpleName()) + "_"
                    + NamingConventions.toSnakeCase(relation.fieldName());
            String joinColumnName = NamingConventions.toSnakeCase(entitySpec.domainSimpleName()) + "_id";

            fieldBuilder.addAnnotation(JpaAnnotations.collectionTable(tableName, joinColumnName));

            // For enum element collections, add @Enumerated(EnumType.STRING) (BUG-004 fix)
            if (relation.isElementTypeEnum()) {
                fieldBuilder.addAnnotation(JpaAnnotations.enumerated());
            }
        }

        // For MANY_TO_MANY owning side, add @JoinTable annotation (BUG-001 fix)
        if (relation.kind() == io.hexaglue.arch.model.ir.RelationKind.MANY_TO_MANY && relation.isOwning()) {
            String targetSimpleName = extractTargetSimpleName(relation.targetType());
            String joinTableName = NamingConventions.toJoinTableName(entitySpec.domainSimpleName(), targetSimpleName);
            String joinColumnName = NamingConventions.toForeignKeyColumnName(entitySpec.domainSimpleName());
            String inverseJoinColumnName = NamingConventions.toForeignKeyColumnName(targetSimpleName);

            fieldBuilder.addAnnotation(JpaAnnotations.joinTable(joinTableName, joinColumnName, inverseJoinColumnName));
        }

        // For MANY_TO_ONE, add @JoinColumn annotation (BUG-002 fix)
        if (relation.kind() == io.hexaglue.arch.model.ir.RelationKind.MANY_TO_ONE) {
            String joinColumnName = NamingConventions.toForeignKeyColumnName(relation.fieldName());
            fieldBuilder.addAnnotation(JpaAnnotations.joinColumn(joinColumnName));
        }

        // Issue 13 fix: unidirectional @OneToMany needs @JoinColumn for FK on child table.
        // Bidirectional relationships (with mappedBy) rely on the inverse side's @JoinColumn.
        if (relation.kind() == io.hexaglue.arch.model.ir.RelationKind.ONE_TO_MANY && relation.isOwning()) {
            String joinColumnName = NamingConventions.toForeignKeyColumnName(entitySpec.domainSimpleName());
            fieldBuilder.addAnnotation(JpaAnnotations.joinColumn(joinColumnName));
        }

        // Initialize collections to avoid NullPointerException
        if (relation.isCollection()) {
            // Use ArrayList for List-based collections, regardless of element type
            fieldBuilder.initializer("new $T<>()", ArrayList.class);
        }

        builder.addField(fieldBuilder.build());
    }

    /**
     * Adds a version field for optimistic locking.
     *
     * <p>Generates:
     * <pre>{@code
     * @Version
     * @Column(name = "version")
     * private Long version;
     * }</pre>
     *
     * @param builder the class builder
     * @since 5.0.0
     */
    private static void addVersionField(TypeSpec.Builder builder) {
        FieldSpec versionField = FieldSpec.builder(Long.class, "version", Modifier.PRIVATE)
                .addAnnotation(JpaAnnotations.version())
                .addAnnotation(JpaAnnotations.column("version", Nullability.NULLABLE))
                .build();
        builder.addField(versionField);
    }

    /**
     * Adds auditing fields (createdAt, updatedAt) for Spring Data JPA auditing.
     *
     * <p>Generates:
     * <pre>{@code
     * @CreatedDate
     * @Column(name = "created_at", nullable = false, updatable = false)
     * private Instant createdAt;
     *
     * @LastModifiedDate
     * @Column(name = "updated_at", nullable = false)
     * private Instant updatedAt;
     * }</pre>
     *
     * @param builder the class builder
     * @since 5.0.0
     */
    private static void addAuditingFields(TypeSpec.Builder builder) {
        // createdAt field - not updatable after initial insert
        FieldSpec createdAtField = FieldSpec.builder(Instant.class, "createdAt", Modifier.PRIVATE)
                .addAnnotation(JpaAnnotations.createdDate())
                .addAnnotation(JpaAnnotations.column("created_at", Nullability.NON_NULL, false))
                .build();
        builder.addField(createdAtField);

        // updatedAt field - updated on each modification
        FieldSpec updatedAtField = FieldSpec.builder(Instant.class, "updatedAt", Modifier.PRIVATE)
                .addAnnotation(JpaAnnotations.lastModifiedDate())
                .addAnnotation(JpaAnnotations.column("updated_at", Nullability.NON_NULL))
                .build();
        builder.addField(updatedAtField);
    }

    // =====================================================================
    // Constructor generation
    // =====================================================================

    /**
     * Creates a public no-args constructor required by JPA.
     *
     * <p>JPA specification requires entities to have a no-args constructor
     * (can be public or protected). We generate a public one for simplicity.
     *
     * @return a MethodSpec for the no-args constructor
     */
    private static MethodSpec createNoArgsConstructor() {
        return MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC).build();
    }

    // =====================================================================
    // Accessor generation (getters and setters)
    // =====================================================================

    /**
     * Adds getters and setters for all fields in the entity.
     *
     * <p>Generates standard JavaBean-style accessors:
     * <ul>
     *   <li>Getters: {@code public T getFieldName()}</li>
     *   <li>Setters: {@code public void setFieldName(T value)}</li>
     * </ul>
     *
     * @param builder the class builder
     * @param spec the entity specification
     */
    private static void addAccessors(TypeSpec.Builder builder, EntitySpec spec) {
        // Identity field accessors
        IdFieldSpec id = spec.idField();
        TypeName idType = id.isWrapped() ? id.unwrappedType() : id.javaType();
        builder.addMethod(createGetter(id.fieldName(), idType));
        builder.addMethod(createSetter(id.fieldName(), idType));

        // Property field accessors (use effectiveJpaType for wrapped foreign keys)
        for (PropertyFieldSpec property : spec.properties()) {
            TypeName propertyType = property.effectiveJpaType();
            builder.addMethod(createGetter(property.fieldName(), propertyType));
            builder.addMethod(createSetter(property.fieldName(), propertyType));
        }

        // Relationship field accessors
        for (RelationFieldSpec relation : spec.relations()) {
            builder.addMethod(createGetter(relation.fieldName(), relation.targetType()));
            builder.addMethod(createSetter(relation.fieldName(), relation.targetType()));
        }

        // Version field accessors (if optimistic locking enabled)
        if (spec.enableOptimisticLocking()) {
            builder.addMethod(createGetter("version", ClassName.get(Long.class)));
            builder.addMethod(createSetter("version", ClassName.get(Long.class)));
        }

        // Auditing field accessors (if auditing enabled)
        if (spec.enableAuditing()) {
            builder.addMethod(createGetter("createdAt", ClassName.get(Instant.class)));
            builder.addMethod(createSetter("createdAt", ClassName.get(Instant.class)));
            builder.addMethod(createGetter("updatedAt", ClassName.get(Instant.class)));
            builder.addMethod(createSetter("updatedAt", ClassName.get(Instant.class)));
        }
    }

    /**
     * Creates a getter method for a field.
     *
     * <p>Generates: {@code public T getFieldName() { return fieldName; }}
     * For primitive boolean fields, generates: {@code public boolean isFieldName() { return fieldName; }}
     *
     * @param fieldName the field name
     * @param type the field type
     * @return a MethodSpec for the getter
     */
    private static MethodSpec createGetter(String fieldName, TypeName type) {
        // JavaBeans convention: use "is" prefix for primitive boolean only
        String prefix = type.equals(TypeName.BOOLEAN) ? "is" : "get";
        String methodName = prefix + NamingConventions.capitalize(fieldName);
        return MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(type)
                .addStatement("return $N", fieldName)
                .build();
    }

    /**
     * Creates a setter method for a field.
     *
     * <p>Generates: {@code public void setFieldName(T fieldName) { this.fieldName = fieldName; }}
     *
     * @param fieldName the field name
     * @param type the field type
     * @return a MethodSpec for the setter
     */
    private static MethodSpec createSetter(String fieldName, TypeName type) {
        String methodName = "set" + NamingConventions.capitalize(fieldName);
        return MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addParameter(type, fieldName)
                .addStatement("this.$N = $N", fieldName, fieldName)
                .build();
    }

    // =====================================================================
    // Helper methods
    // =====================================================================

    /**
     * Extracts the simple name from a JavaPoet TypeName.
     *
     * <p>For collection types (Set, List), extracts the element type's simple name.
     * For example:
     * <ul>
     *   <li>{@code Set<CategoryEntity>} → {@code "Category"}</li>
     *   <li>{@code CategoryEntity} → {@code "Category"}</li>
     *   <li>{@code com.example.Product} → {@code "Product"}</li>
     * </ul>
     *
     * <p>Removes common JPA suffixes ("Entity", "Embeddable") to get the domain name.
     *
     * @param typeName the JavaPoet type name
     * @return the simple name without JPA suffixes
     */
    private static String extractTargetSimpleName(TypeName typeName) {
        String simpleName;

        if (typeName instanceof ParameterizedTypeName parameterized) {
            // For Set<CategoryEntity> or List<CategoryEntity>, get the type argument
            if (!parameterized.typeArguments().isEmpty()) {
                TypeName elementType = parameterized.typeArguments().get(0);
                if (elementType instanceof ClassName className) {
                    simpleName = className.simpleName();
                } else {
                    simpleName = elementType.toString();
                    // Extract simple name from fully qualified name
                    int lastDot = simpleName.lastIndexOf('.');
                    if (lastDot >= 0) {
                        simpleName = simpleName.substring(lastDot + 1);
                    }
                }
            } else {
                simpleName = parameterized.rawType().simpleName();
            }
        } else if (typeName instanceof ClassName className) {
            simpleName = className.simpleName();
        } else {
            // Fallback: convert to string and extract simple name
            simpleName = typeName.toString();
            int lastDot = simpleName.lastIndexOf('.');
            if (lastDot >= 0) {
                simpleName = simpleName.substring(lastDot + 1);
            }
        }

        // Remove common JPA suffixes to get the domain name
        if (simpleName.endsWith("Entity")) {
            return simpleName.substring(0, simpleName.length() - "Entity".length());
        }
        if (simpleName.endsWith("Embeddable")) {
            return simpleName.substring(0, simpleName.length() - "Embeddable".length());
        }

        return simpleName;
    }
}
