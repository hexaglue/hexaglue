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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import io.hexaglue.arch.ElementKind;
import io.hexaglue.plugin.jpa.model.AttributeOverride;
import io.hexaglue.plugin.jpa.model.EntitySpec;
import io.hexaglue.plugin.jpa.model.IdFieldSpec;
import io.hexaglue.plugin.jpa.model.PropertyFieldSpec;
import io.hexaglue.plugin.jpa.model.RelationFieldSpec;
import io.hexaglue.spi.ir.CascadeType;
import io.hexaglue.spi.ir.FetchType;
import io.hexaglue.spi.ir.IdentityStrategy;
import io.hexaglue.spi.ir.IdentityWrapperKind;
import io.hexaglue.spi.ir.Nullability;
import io.hexaglue.spi.ir.RelationKind;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link JpaEntityCodegen}.
 *
 * <p>These tests validate that the JPA entity code generator produces correct JPA annotations,
 * fields, constructors, and accessor methods according to JPA specifications.
 *
 * <p>Test strategy: Each test creates an EntitySpec with specific configurations and verifies
 * that the generated code contains the expected annotations, fields, and methods.
 */
class JpaEntityCodegenTest {

    // =====================================================================
    // Test fixtures
    // =====================================================================

    private static final String TEST_PACKAGE = "com.example.infrastructure.jpa";
    private static final String TEST_CLASS = "OrderEntity";
    private static final String TEST_TABLE = "orders";
    private static final String DOMAIN_FQN = "com.example.domain.Order";

    /**
     * Creates a minimal valid EntitySpec for testing.
     */
    private EntitySpec createMinimalSpec() {
        IdFieldSpec idField = new IdFieldSpec(
                "id",
                TypeName.get(UUID.class),
                TypeName.get(UUID.class),
                IdentityStrategy.AUTO,
                IdentityWrapperKind.NONE);

        return EntitySpec.builder()
                .packageName(TEST_PACKAGE)
                .className(TEST_CLASS)
                .tableName(TEST_TABLE)
                .domainQualifiedName(DOMAIN_FQN)
                .idField(idField)
                .build();
    }

    /**
     * Creates an IdFieldSpec with AUTO generation strategy.
     */
    private IdFieldSpec createAutoIdField() {
        return new IdFieldSpec(
                "id",
                TypeName.get(UUID.class),
                TypeName.get(UUID.class),
                IdentityStrategy.AUTO,
                IdentityWrapperKind.NONE);
    }

    /**
     * Creates an IdFieldSpec with ASSIGNED generation strategy.
     */
    private IdFieldSpec createAssignedIdField() {
        return new IdFieldSpec(
                "id",
                TypeName.get(UUID.class),
                TypeName.get(UUID.class),
                IdentityStrategy.ASSIGNED,
                IdentityWrapperKind.NONE);
    }

    /**
     * Creates a simple PropertyFieldSpec.
     */
    private PropertyFieldSpec createSimpleProperty(String fieldName, TypeName type, Nullability nullability) {
        return new PropertyFieldSpec(
                fieldName, type, nullability, fieldName, false, false, false, type.toString(), false, null, null,
                List.of());
    }

    /**
     * Creates an embedded PropertyFieldSpec.
     */
    private PropertyFieldSpec createEmbeddedProperty(String fieldName, TypeName type) {
        return new PropertyFieldSpec(
                fieldName,
                type,
                Nullability.NON_NULL,
                fieldName,
                true,
                false,
                false,
                type.toString(),
                false,
                null,
                null,
                List.of());
    }

    /**
     * Creates a RelationFieldSpec for one-to-many relationship.
     */
    private RelationFieldSpec createOneToManyRelation(String fieldName, TypeName targetClass) {
        return new RelationFieldSpec(
                fieldName,
                ParameterizedTypeName.get(com.palantir.javapoet.ClassName.get(List.class), targetClass),
                RelationKind.ONE_TO_MANY,
                ElementKind.ENTITY,
                null,
                CascadeType.ALL,
                FetchType.LAZY,
                true);
    }

    /**
     * Creates a RelationFieldSpec for many-to-one relationship.
     */
    private RelationFieldSpec createManyToOneRelation(String fieldName, TypeName targetClass) {
        return new RelationFieldSpec(
                fieldName,
                targetClass,
                RelationKind.MANY_TO_ONE,
                ElementKind.AGGREGATE_ROOT,
                null,
                CascadeType.NONE,
                FetchType.LAZY,
                false);
    }

    // =====================================================================
    // Class-level annotation tests
    // =====================================================================

    @Test
    void generate_shouldAddGeneratedAnnotation() {
        // Given
        EntitySpec spec = createMinimalSpec();

        // When
        TypeSpec typeSpec = JpaEntityCodegen.generate(spec);
        String code = typeSpec.toString();

        // Then - Check for @Generated annotation with value, comments and date
        assertThat(code)
                .contains("@javax.annotation.processing.Generated")
                .contains("value = \"io.hexaglue.plugin.jpa\"")
                .contains("comments = \"Generated by HexaGlue JPA Plugin")
                .contains("date = ");
    }

    @Test
    void generate_shouldAddEntityAnnotation() {
        // Given
        EntitySpec spec = createMinimalSpec();

        // When
        TypeSpec typeSpec = JpaEntityCodegen.generate(spec);
        String code = typeSpec.toString();

        // Then
        assertThat(code).contains("@jakarta.persistence.Entity");
    }

    @Test
    void generate_shouldAddTableAnnotation() {
        // Given
        EntitySpec spec = createMinimalSpec();

        // When
        TypeSpec typeSpec = JpaEntityCodegen.generate(spec);
        String code = typeSpec.toString();

        // Then
        assertThat(code).contains("@jakarta.persistence.Table");
        assertThat(code).contains("name = \"orders\"");
    }

    // =====================================================================
    // Identity field tests
    // =====================================================================

    @Test
    void generate_shouldAddIdFieldWithAnnotations() {
        // Given
        EntitySpec spec = createMinimalSpec();

        // When
        TypeSpec typeSpec = JpaEntityCodegen.generate(spec);
        String code = typeSpec.toString();

        // Then
        assertThat(code)
                .contains("@jakarta.persistence.Id")
                .contains("@jakarta.persistence.Column")
                .contains("name = \"id\"")
                .contains("private java.util.UUID id;");
    }

    @Test
    void generate_shouldAddGeneratedValueWhenRequired() {
        // Given
        EntitySpec spec = createMinimalSpec();

        // When
        TypeSpec typeSpec = JpaEntityCodegen.generate(spec);
        String code = typeSpec.toString();

        // Then
        assertThat(code).contains("@jakarta.persistence.GeneratedValue");
        assertThat(code).contains("strategy = jakarta.persistence.GenerationType.AUTO");
    }

    @Test
    void generate_shouldNotAddGeneratedValueForAssignedStrategy() {
        // Given
        IdFieldSpec assignedId = createAssignedIdField();
        EntitySpec spec = EntitySpec.builder()
                .packageName(TEST_PACKAGE)
                .className(TEST_CLASS)
                .tableName(TEST_TABLE)
                .domainQualifiedName(DOMAIN_FQN)
                .idField(assignedId)
                .build();

        // When
        TypeSpec typeSpec = JpaEntityCodegen.generate(spec);
        String code = typeSpec.toString();

        // Then
        assertThat(code).doesNotContain("@jakarta.persistence.GeneratedValue");
    }

    @Test
    void generate_shouldAddIdentityGenerationStrategy() {
        // Given
        IdFieldSpec identityId = new IdFieldSpec(
                "id",
                TypeName.get(Long.class),
                TypeName.get(Long.class),
                IdentityStrategy.IDENTITY,
                IdentityWrapperKind.NONE);
        EntitySpec spec = EntitySpec.builder()
                .packageName(TEST_PACKAGE)
                .className(TEST_CLASS)
                .tableName(TEST_TABLE)
                .domainQualifiedName(DOMAIN_FQN)
                .idField(identityId)
                .build();

        // When
        TypeSpec typeSpec = JpaEntityCodegen.generate(spec);
        String code = typeSpec.toString();

        // Then
        assertThat(code).contains("@jakarta.persistence.GeneratedValue");
        assertThat(code).contains("strategy = jakarta.persistence.GenerationType.IDENTITY");
    }

    @Test
    void generate_shouldAddSequenceGenerationStrategy() {
        // Given
        IdFieldSpec sequenceId = new IdFieldSpec(
                "id",
                TypeName.get(Long.class),
                TypeName.get(Long.class),
                IdentityStrategy.SEQUENCE,
                IdentityWrapperKind.NONE);
        EntitySpec spec = EntitySpec.builder()
                .packageName(TEST_PACKAGE)
                .className(TEST_CLASS)
                .tableName(TEST_TABLE)
                .domainQualifiedName(DOMAIN_FQN)
                .idField(sequenceId)
                .build();

        // When
        TypeSpec typeSpec = JpaEntityCodegen.generate(spec);
        String code = typeSpec.toString();

        // Then
        assertThat(code).contains("@jakarta.persistence.GeneratedValue");
        assertThat(code).contains("strategy = jakarta.persistence.GenerationType.SEQUENCE");
    }

    // =====================================================================
    // Property field tests
    // =====================================================================

    @Test
    void generate_shouldAddPropertyFieldWithColumn() {
        // Given
        PropertyFieldSpec property =
                createSimpleProperty("totalAmount", TypeName.get(BigDecimal.class), Nullability.NON_NULL);
        EntitySpec spec = EntitySpec.builder()
                .packageName(TEST_PACKAGE)
                .className(TEST_CLASS)
                .tableName(TEST_TABLE)
                .domainQualifiedName(DOMAIN_FQN)
                .idField(createAutoIdField())
                .addProperty(property)
                .build();

        // When
        TypeSpec typeSpec = JpaEntityCodegen.generate(spec);
        String code = typeSpec.toString();

        // Then
        assertThat(code)
                .contains("@jakarta.persistence.Column")
                .contains("name = \"totalAmount\"")
                .contains("private java.math.BigDecimal totalAmount;");
    }

    @Test
    void generate_shouldSetNullableFalseForNonNullProperty() {
        // Given
        PropertyFieldSpec property = createSimpleProperty("status", TypeName.get(String.class), Nullability.NON_NULL);
        EntitySpec spec = EntitySpec.builder()
                .packageName(TEST_PACKAGE)
                .className(TEST_CLASS)
                .tableName(TEST_TABLE)
                .domainQualifiedName(DOMAIN_FQN)
                .idField(createAutoIdField())
                .addProperty(property)
                .build();

        // When
        TypeSpec typeSpec = JpaEntityCodegen.generate(spec);
        String code = typeSpec.toString();

        // Then
        assertThat(code).contains("nullable = false");
    }

    @Test
    void generate_shouldSetNullableTrueForNullableProperty() {
        // Given
        PropertyFieldSpec property =
                createSimpleProperty("description", TypeName.get(String.class), Nullability.NULLABLE);
        EntitySpec spec = EntitySpec.builder()
                .packageName(TEST_PACKAGE)
                .className(TEST_CLASS)
                .tableName(TEST_TABLE)
                .domainQualifiedName(DOMAIN_FQN)
                .idField(createAutoIdField())
                .addProperty(property)
                .build();

        // When
        TypeSpec typeSpec = JpaEntityCodegen.generate(spec);
        String code = typeSpec.toString();

        // Then
        // When nullable is true, it's often omitted in the output as it's the default
        // The generated code should have the Column annotation for description without nullable = false
        // We verify the presence of the column and that there's no explicit nullable constraint on it
        assertThat(code)
                .contains("@jakarta.persistence.Column")
                .contains("name = \"description\"")
                .contains("private java.lang.String description;");

        // Extract just the description field annotation section to verify nullable is not set to false for it
        // Since the ID field will have nullable = false, we need to be more precise
        String[] lines = code.split("\n");
        boolean foundDescriptionColumn = false;
        boolean nullableFalseAfterDescription = false;
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains("name = \"description\"")) {
                foundDescriptionColumn = true;
                // Check the next few lines until we hit the field declaration
                for (int j = i; j < Math.min(i + 5, lines.length); j++) {
                    if (lines[j].contains("nullable = false")) {
                        nullableFalseAfterDescription = true;
                        break;
                    }
                    if (lines[j].contains("private java.lang.String description;")) {
                        break;
                    }
                }
                break;
            }
        }

        assertThat(foundDescriptionColumn)
                .as("Should find description column annotation")
                .isTrue();
        assertThat(nullableFalseAfterDescription)
                .as("Description field should not have nullable = false")
                .isFalse();
    }

    @Test
    void generate_shouldAddEmbeddedAnnotationForEmbeddedProperty() {
        // Given
        PropertyFieldSpec embeddedProperty =
                createEmbeddedProperty("address", TypeName.get(Object.class)); // Using Object as placeholder
        EntitySpec spec = EntitySpec.builder()
                .packageName(TEST_PACKAGE)
                .className(TEST_CLASS)
                .tableName(TEST_TABLE)
                .domainQualifiedName(DOMAIN_FQN)
                .idField(createAutoIdField())
                .addProperty(embeddedProperty)
                .build();

        // When
        TypeSpec typeSpec = JpaEntityCodegen.generate(spec);
        String code = typeSpec.toString();

        // Then
        assertThat(code).contains("@jakarta.persistence.Embedded").contains("private java.lang.Object address;");
    }

    // =====================================================================
    // Relationship field tests
    // =====================================================================

    @Test
    void generate_shouldAddOneToManyRelation() {
        // Given
        TypeName lineItemType = TypeName.get(Object.class); // Using Object as placeholder
        RelationFieldSpec relation = createOneToManyRelation("items", lineItemType);
        EntitySpec spec = EntitySpec.builder()
                .packageName(TEST_PACKAGE)
                .className(TEST_CLASS)
                .tableName(TEST_TABLE)
                .domainQualifiedName(DOMAIN_FQN)
                .idField(createAutoIdField())
                .addRelation(relation)
                .build();

        // When
        TypeSpec typeSpec = JpaEntityCodegen.generate(spec);
        String code = typeSpec.toString();

        // Then
        assertThat(code)
                .contains("@jakarta.persistence.OneToMany")
                .contains("cascade = jakarta.persistence.CascadeType.ALL")
                .contains("orphanRemoval = true")
                .contains("private java.util.List<java.lang.Object> items");
    }

    @Test
    void generate_shouldAddManyToOneRelation() {
        // Given
        TypeName customerType = TypeName.get(Object.class); // Using Object as placeholder
        RelationFieldSpec relation = createManyToOneRelation("customer", customerType);
        EntitySpec spec = EntitySpec.builder()
                .packageName(TEST_PACKAGE)
                .className(TEST_CLASS)
                .tableName(TEST_TABLE)
                .domainQualifiedName(DOMAIN_FQN)
                .idField(createAutoIdField())
                .addRelation(relation)
                .build();

        // When
        TypeSpec typeSpec = JpaEntityCodegen.generate(spec);
        String code = typeSpec.toString();

        // Then
        assertThat(code)
                .contains("@jakarta.persistence.ManyToOne")
                .contains("fetch = jakarta.persistence.FetchType.LAZY")
                .contains("private java.lang.Object customer;");
    }

    @Test
    void generate_shouldInitializeCollectionFields() {
        // Given
        TypeName lineItemType = TypeName.get(Object.class);
        RelationFieldSpec relation = createOneToManyRelation("items", lineItemType);
        EntitySpec spec = EntitySpec.builder()
                .packageName(TEST_PACKAGE)
                .className(TEST_CLASS)
                .tableName(TEST_TABLE)
                .domainQualifiedName(DOMAIN_FQN)
                .idField(createAutoIdField())
                .addRelation(relation)
                .build();

        // When
        TypeSpec typeSpec = JpaEntityCodegen.generate(spec);
        String code = typeSpec.toString();

        // Then
        assertThat(code).contains("private java.util.List<java.lang.Object> items = new java.util.ArrayList<>();");
    }

    // =====================================================================
    // BUG-001 and BUG-002 regression tests
    // =====================================================================

    /**
     * BUG-001 regression test: @ManyToMany owning side should have @JoinTable.
     *
     * <p>Without @JoinTable, JPA uses default table naming which doesn't follow
     * the project's snake_case conventions.
     */
    @Test
    void generate_shouldAddJoinTableForOwningSideManyToMany_BUG001() {
        // Given: ManyToMany owning side (mappedBy = null)
        TypeName categoryEntityType = ParameterizedTypeName.get(
                ClassName.get(Set.class), ClassName.bestGuess("com.example.CategoryEntity"));
        RelationFieldSpec relation = new RelationFieldSpec(
                "categories",
                categoryEntityType,
                RelationKind.MANY_TO_MANY,
                ElementKind.ENTITY,
                null, // owning side
                CascadeType.PERSIST,
                FetchType.LAZY,
                false);

        EntitySpec spec = EntitySpec.builder()
                .packageName(TEST_PACKAGE)
                .className("ProductEntity")
                .tableName("products")
                .domainQualifiedName("com.example.domain.Product")
                .idField(createAutoIdField())
                .addRelation(relation)
                .build();

        // When
        TypeSpec typeSpec = JpaEntityCodegen.generate(spec);
        String code = typeSpec.toString();

        // Then: Should have @JoinTable with proper naming
        assertThat(code)
                .contains("@jakarta.persistence.ManyToMany")
                .contains("@jakarta.persistence.JoinTable")
                .contains("name = \"category_product\"") // alphabetical order
                .contains("joinColumns")
                .contains("inverseJoinColumns")
                .contains("\"product_id\"")
                .contains("\"category_id\"");
    }

    /**
     * BUG-001 inverse test: @ManyToMany inverse side should NOT have @JoinTable.
     */
    @Test
    void generate_shouldNotAddJoinTableForInverseSideManyToMany_BUG001() {
        // Given: ManyToMany inverse side (mappedBy = "products")
        TypeName productEntityType = ParameterizedTypeName.get(
                ClassName.get(Set.class), ClassName.bestGuess("com.example.ProductEntity"));
        RelationFieldSpec relation = new RelationFieldSpec(
                "products",
                productEntityType,
                RelationKind.MANY_TO_MANY,
                ElementKind.ENTITY,
                "categories", // inverse side - points to owning field
                CascadeType.NONE,
                FetchType.LAZY,
                false);

        EntitySpec spec = EntitySpec.builder()
                .packageName(TEST_PACKAGE)
                .className("CategoryEntity")
                .tableName("categories")
                .domainQualifiedName("com.example.domain.Category")
                .idField(createAutoIdField())
                .addRelation(relation)
                .build();

        // When
        TypeSpec typeSpec = JpaEntityCodegen.generate(spec);
        String code = typeSpec.toString();

        // Then: Should have mappedBy but NOT @JoinTable
        assertThat(code)
                .contains("@jakarta.persistence.ManyToMany")
                .contains("mappedBy = \"categories\"")
                .doesNotContain("@jakarta.persistence.JoinTable");
    }

    /**
     * BUG-002 regression test: @ManyToOne should have @JoinColumn.
     *
     * <p>Without explicit @JoinColumn, JPA uses default column naming which
     * may not follow the project's snake_case conventions.
     */
    @Test
    void generate_shouldAddJoinColumnForManyToOne_BUG002() {
        // Given: ManyToOne relation
        TypeName orderEntityType = ClassName.bestGuess("com.example.OrderEntity");
        RelationFieldSpec relation = new RelationFieldSpec(
                "order",
                orderEntityType,
                RelationKind.MANY_TO_ONE,
                ElementKind.AGGREGATE_ROOT,
                null,
                CascadeType.NONE,
                FetchType.LAZY,
                false);

        EntitySpec spec = EntitySpec.builder()
                .packageName(TEST_PACKAGE)
                .className("OrderLineEntity")
                .tableName("order_lines")
                .domainQualifiedName("com.example.domain.OrderLine")
                .idField(createAutoIdField())
                .addRelation(relation)
                .build();

        // When
        TypeSpec typeSpec = JpaEntityCodegen.generate(spec);
        String code = typeSpec.toString();

        // Then: Should have @ManyToOne AND @JoinColumn
        assertThat(code)
                .contains("@jakarta.persistence.ManyToOne")
                .contains("@jakarta.persistence.JoinColumn")
                .contains("name = \"order_id\"");
    }

    /**
     * BUG-004 regression test: @ElementCollection of enums should have @Enumerated(STRING).
     *
     * <p>Without @Enumerated(EnumType.STRING), Hibernate uses ORDINAL by default,
     * which stores the enum index instead of the name.
     */
    @Test
    void generate_shouldAddEnumeratedForEnumElementCollection_BUG004() {
        // Given: ElementCollection of enum type
        TypeName statusListType = ParameterizedTypeName.get(
                ClassName.get(List.class),
                ClassName.bestGuess("com.example.OrderStatus"));
        RelationFieldSpec relation = new RelationFieldSpec(
                "previousStatuses",
                statusListType,
                RelationKind.ELEMENT_COLLECTION,
                ElementKind.VALUE_OBJECT,
                null,
                CascadeType.NONE,
                FetchType.LAZY,
                false,
                List.of(),
                true);  // isElementTypeEnum = true

        EntitySpec spec = EntitySpec.builder()
                .packageName(TEST_PACKAGE)
                .className("OrderEntity")
                .tableName("orders")
                .domainQualifiedName(DOMAIN_FQN)
                .idField(createAutoIdField())
                .addRelation(relation)
                .build();

        // When
        TypeSpec typeSpec = JpaEntityCodegen.generate(spec);
        String code = typeSpec.toString();

        // Then: Should have @ElementCollection, @CollectionTable, AND @Enumerated
        assertThat(code)
                .contains("@jakarta.persistence.ElementCollection")
                .contains("@jakarta.persistence.CollectionTable")
                .contains("@jakarta.persistence.Enumerated");
        assertThat(code).contains("EnumType.STRING");
    }

    /**
     * BUG-004 negative test: Non-enum @ElementCollection should NOT have @Enumerated.
     */
    @Test
    void generate_shouldNotAddEnumeratedForNonEnumElementCollection_BUG004() {
        // Given: ElementCollection of non-enum type (e.g., embedded value object)
        TypeName tagListType = ParameterizedTypeName.get(
                ClassName.get(List.class),
                ClassName.bestGuess("com.example.TagEmbeddable"));
        RelationFieldSpec relation = new RelationFieldSpec(
                "tags",
                tagListType,
                RelationKind.ELEMENT_COLLECTION,
                ElementKind.VALUE_OBJECT,
                null,
                CascadeType.NONE,
                FetchType.LAZY,
                false,
                List.of(),
                false);  // isElementTypeEnum = false

        EntitySpec spec = EntitySpec.builder()
                .packageName(TEST_PACKAGE)
                .className("ArticleEntity")
                .tableName("articles")
                .domainQualifiedName("com.example.domain.Article")
                .idField(createAutoIdField())
                .addRelation(relation)
                .build();

        // When
        TypeSpec typeSpec = JpaEntityCodegen.generate(spec);
        String code = typeSpec.toString();

        // Then: Should have @ElementCollection but NOT @Enumerated
        assertThat(code)
                .contains("@jakarta.persistence.ElementCollection")
                .contains("@jakarta.persistence.CollectionTable")
                .doesNotContain("@jakarta.persistence.Enumerated");
    }

    // =====================================================================
    // BUG-006 and BUG-007 regression tests (Auditing & Optimistic Locking)
    // =====================================================================

    /**
     * BUG-006 regression test: enableAuditing=true should generate auditing fields.
     *
     * <p>When auditing is enabled, the entity should have:
     * - @EntityListeners(AuditingEntityListener.class) on the class
     * - @CreatedDate field (createdAt)
     * - @LastModifiedDate field (updatedAt)
     * - Getters/setters for both fields
     */
    @Test
    void generate_shouldAddAuditingFieldsWhenEnabled_BUG006() {
        // Given: EntitySpec with enableAuditing = true
        EntitySpec spec = EntitySpec.builder()
                .packageName(TEST_PACKAGE)
                .className(TEST_CLASS)
                .tableName(TEST_TABLE)
                .domainQualifiedName(DOMAIN_FQN)
                .idField(createAutoIdField())
                .enableAuditing(true)
                .enableOptimisticLocking(false)
                .build();

        // When
        TypeSpec typeSpec = JpaEntityCodegen.generate(spec);
        String code = typeSpec.toString();

        // Then: Should have @EntityListeners and auditing fields
        assertThat(code)
                .contains("@jakarta.persistence.EntityListeners")
                .contains("AuditingEntityListener.class")
                .contains("@org.springframework.data.annotation.CreatedDate")
                .contains("@org.springframework.data.annotation.LastModifiedDate")
                .contains("private java.time.Instant createdAt;")
                .contains("private java.time.Instant updatedAt;")
                // createdAt should not be updatable
                .contains("updatable = false")
                // Accessors
                .contains("public java.time.Instant getCreatedAt()")
                .contains("public void setCreatedAt(java.time.Instant createdAt)")
                .contains("public java.time.Instant getUpdatedAt()")
                .contains("public void setUpdatedAt(java.time.Instant updatedAt)");
    }

    /**
     * BUG-006 negative test: enableAuditing=false should NOT generate auditing fields.
     */
    @Test
    void generate_shouldNotAddAuditingFieldsWhenDisabled_BUG006() {
        // Given: EntitySpec with enableAuditing = false (default)
        EntitySpec spec = EntitySpec.builder()
                .packageName(TEST_PACKAGE)
                .className(TEST_CLASS)
                .tableName(TEST_TABLE)
                .domainQualifiedName(DOMAIN_FQN)
                .idField(createAutoIdField())
                .enableAuditing(false)
                .enableOptimisticLocking(false)
                .build();

        // When
        TypeSpec typeSpec = JpaEntityCodegen.generate(spec);
        String code = typeSpec.toString();

        // Then: Should NOT have auditing annotations or fields
        assertThat(code)
                .doesNotContain("@jakarta.persistence.EntityListeners")
                .doesNotContain("AuditingEntityListener")
                .doesNotContain("@org.springframework.data.annotation.CreatedDate")
                .doesNotContain("@org.springframework.data.annotation.LastModifiedDate")
                .doesNotContain("private java.time.Instant createdAt;")
                .doesNotContain("private java.time.Instant updatedAt;");
    }

    /**
     * BUG-007 regression test: enableOptimisticLocking=true should generate @Version field.
     *
     * <p>When optimistic locking is enabled, the entity should have:
     * - @Version field (version)
     * - @Column(name = "version")
     * - Getter/setter for version field
     */
    @Test
    void generate_shouldAddVersionFieldWhenOptimisticLockingEnabled_BUG007() {
        // Given: EntitySpec with enableOptimisticLocking = true
        EntitySpec spec = EntitySpec.builder()
                .packageName(TEST_PACKAGE)
                .className(TEST_CLASS)
                .tableName(TEST_TABLE)
                .domainQualifiedName(DOMAIN_FQN)
                .idField(createAutoIdField())
                .enableAuditing(false)
                .enableOptimisticLocking(true)
                .build();

        // When
        TypeSpec typeSpec = JpaEntityCodegen.generate(spec);
        String code = typeSpec.toString();

        // Then: Should have @Version field
        assertThat(code)
                .contains("@jakarta.persistence.Version")
                .contains("@jakarta.persistence.Column")
                .contains("name = \"version\"")
                .contains("private java.lang.Long version;")
                // Accessors
                .contains("public java.lang.Long getVersion()")
                .contains("public void setVersion(java.lang.Long version)");
    }

    /**
     * BUG-007 negative test: enableOptimisticLocking=false should NOT generate @Version field.
     */
    @Test
    void generate_shouldNotAddVersionFieldWhenOptimisticLockingDisabled_BUG007() {
        // Given: EntitySpec with enableOptimisticLocking = false (default)
        EntitySpec spec = EntitySpec.builder()
                .packageName(TEST_PACKAGE)
                .className(TEST_CLASS)
                .tableName(TEST_TABLE)
                .domainQualifiedName(DOMAIN_FQN)
                .idField(createAutoIdField())
                .enableAuditing(false)
                .enableOptimisticLocking(false)
                .build();

        // When
        TypeSpec typeSpec = JpaEntityCodegen.generate(spec);
        String code = typeSpec.toString();

        // Then: Should NOT have @Version field
        assertThat(code)
                .doesNotContain("@jakarta.persistence.Version")
                .doesNotContain("private java.lang.Long version;");
    }

    /**
     * Combined test: Both auditing AND optimistic locking enabled.
     */
    @Test
    void generate_shouldAddBothAuditingAndVersionFields_FEAT04() {
        // Given: EntitySpec with both features enabled
        EntitySpec spec = EntitySpec.builder()
                .packageName(TEST_PACKAGE)
                .className(TEST_CLASS)
                .tableName(TEST_TABLE)
                .domainQualifiedName(DOMAIN_FQN)
                .idField(createAutoIdField())
                .enableAuditing(true)
                .enableOptimisticLocking(true)
                .build();

        // When
        TypeSpec typeSpec = JpaEntityCodegen.generate(spec);
        String code = typeSpec.toString();

        // Then: Should have all features
        assertThat(code)
                // Auditing
                .contains("@jakarta.persistence.EntityListeners")
                .contains("AuditingEntityListener.class")
                .contains("@org.springframework.data.annotation.CreatedDate")
                .contains("@org.springframework.data.annotation.LastModifiedDate")
                .contains("private java.time.Instant createdAt;")
                .contains("private java.time.Instant updatedAt;")
                // Optimistic locking
                .contains("@jakarta.persistence.Version")
                .contains("private java.lang.Long version;")
                // All accessors
                .contains("getCreatedAt()")
                .contains("getUpdatedAt()")
                .contains("getVersion()");
    }

    // =====================================================================
    // Constructor tests
    // =====================================================================

    @Test
    void generate_shouldAddNoArgsConstructor() {
        // Given
        EntitySpec spec = createMinimalSpec();

        // When
        TypeSpec typeSpec = JpaEntityCodegen.generate(spec);
        String code = typeSpec.toString();

        // Then
        assertThat(code).contains("public OrderEntity() {");
    }

    // =====================================================================
    // Accessor tests
    // =====================================================================

    @Test
    void generate_shouldAddGetterForIdField() {
        // Given
        EntitySpec spec = createMinimalSpec();

        // When
        TypeSpec typeSpec = JpaEntityCodegen.generate(spec);
        String code = typeSpec.toString();

        // Then
        assertThat(code).contains("public java.util.UUID getId() {").contains("return id;");
    }

    @Test
    void generate_shouldAddSetterForIdField() {
        // Given
        EntitySpec spec = createMinimalSpec();

        // When
        TypeSpec typeSpec = JpaEntityCodegen.generate(spec);
        String code = typeSpec.toString();

        // Then
        assertThat(code).contains("public void setId(java.util.UUID id) {").contains("this.id = id;");
    }

    @Test
    void generate_shouldAddGetterForEachPropertyField() {
        // Given
        PropertyFieldSpec property = createSimpleProperty("status", TypeName.get(String.class), Nullability.NON_NULL);
        EntitySpec spec = EntitySpec.builder()
                .packageName(TEST_PACKAGE)
                .className(TEST_CLASS)
                .tableName(TEST_TABLE)
                .domainQualifiedName(DOMAIN_FQN)
                .idField(createAutoIdField())
                .addProperty(property)
                .build();

        // When
        TypeSpec typeSpec = JpaEntityCodegen.generate(spec);
        String code = typeSpec.toString();

        // Then
        assertThat(code).contains("public java.lang.String getStatus() {").contains("return status;");
    }

    @Test
    void generate_shouldUseisPrefixForBooleanGetter() {
        // Given: a primitive boolean property
        PropertyFieldSpec property = createSimpleProperty("active", TypeName.BOOLEAN, Nullability.NON_NULL);
        EntitySpec spec = EntitySpec.builder()
                .packageName(TEST_PACKAGE)
                .className(TEST_CLASS)
                .tableName(TEST_TABLE)
                .domainQualifiedName(DOMAIN_FQN)
                .idField(createAutoIdField())
                .addProperty(property)
                .build();

        // When
        TypeSpec typeSpec = JpaEntityCodegen.generate(spec);
        String code = typeSpec.toString();

        // Then: JavaBeans convention requires "is" prefix for primitive boolean
        assertThat(code)
                .as("Primitive boolean getter should use 'is' prefix per JavaBeans convention")
                .contains("public boolean isActive() {")
                .contains("return active;");
        // And: Should NOT use "get" prefix
        assertThat(code).doesNotContain("getActive()");
    }

    @Test
    void generate_shouldAddSetterForEachPropertyField() {
        // Given
        PropertyFieldSpec property = createSimpleProperty("status", TypeName.get(String.class), Nullability.NON_NULL);
        EntitySpec spec = EntitySpec.builder()
                .packageName(TEST_PACKAGE)
                .className(TEST_CLASS)
                .tableName(TEST_TABLE)
                .domainQualifiedName(DOMAIN_FQN)
                .idField(createAutoIdField())
                .addProperty(property)
                .build();

        // When
        TypeSpec typeSpec = JpaEntityCodegen.generate(spec);
        String code = typeSpec.toString();

        // Then
        assertThat(code)
                .contains("public void setStatus(java.lang.String status) {")
                .contains("this.status = status;");
    }

    @Test
    void generate_shouldAddGetterForEachRelationField() {
        // Given
        TypeName lineItemType = TypeName.get(Object.class);
        RelationFieldSpec relation = createOneToManyRelation("items", lineItemType);
        EntitySpec spec = EntitySpec.builder()
                .packageName(TEST_PACKAGE)
                .className(TEST_CLASS)
                .tableName(TEST_TABLE)
                .domainQualifiedName(DOMAIN_FQN)
                .idField(createAutoIdField())
                .addRelation(relation)
                .build();

        // When
        TypeSpec typeSpec = JpaEntityCodegen.generate(spec);
        String code = typeSpec.toString();

        // Then
        assertThat(code)
                .contains("public java.util.List<java.lang.Object> getItems() {")
                .contains("return items;");
    }

    @Test
    void generate_shouldAddSetterForEachRelationField() {
        // Given
        TypeName lineItemType = TypeName.get(Object.class);
        RelationFieldSpec relation = createOneToManyRelation("items", lineItemType);
        EntitySpec spec = EntitySpec.builder()
                .packageName(TEST_PACKAGE)
                .className(TEST_CLASS)
                .tableName(TEST_TABLE)
                .domainQualifiedName(DOMAIN_FQN)
                .idField(createAutoIdField())
                .addRelation(relation)
                .build();

        // When
        TypeSpec typeSpec = JpaEntityCodegen.generate(spec);
        String code = typeSpec.toString();

        // Then
        assertThat(code)
                .contains("public void setItems(java.util.List<java.lang.Object> items) {")
                .contains("this.items = items;");
    }

    // =====================================================================
    // JavaFile generation tests
    // =====================================================================

    @Test
    void generateFile_shouldCreateJavaFileWithCorrectPackage() {
        // Given
        EntitySpec spec = createMinimalSpec();

        // When
        JavaFile javaFile = JpaEntityCodegen.generateFile(spec);
        String code = javaFile.toString();

        // Then
        assertThat(code).contains("package com.example.infrastructure.jpa;").contains("class OrderEntity");
    }

    @Test
    void generateFile_shouldUseCorrectIndentation() {
        // Given
        EntitySpec spec = createMinimalSpec();

        // When
        JavaFile javaFile = JpaEntityCodegen.generateFile(spec);
        String code = javaFile.toString();

        // Then - JavaPoet uses 2-space indentation by default, but we configured 4 spaces
        assertThat(code).contains("    private UUID id;"); // 4 spaces
    }

    // =====================================================================
    // Validation tests
    // =====================================================================

    @Test
    void generate_shouldThrowForNullSpec() {
        // When / Then
        assertThatThrownBy(() -> JpaEntityCodegen.generate(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("EntitySpec cannot be null");
    }

    @Test
    void generateFile_shouldThrowForNullSpec() {
        // When / Then
        assertThatThrownBy(() -> JpaEntityCodegen.generateFile(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("EntitySpec cannot be null");
    }

    // =====================================================================
    // Complex integration tests
    // =====================================================================

    @Test
    void generate_shouldHandleCompleteEntityWithAllFeatures() {
        // Given - Entity with ID, properties, and relationships
        IdFieldSpec idField = createAutoIdField();
        PropertyFieldSpec statusProperty =
                createSimpleProperty("status", TypeName.get(String.class), Nullability.NON_NULL);
        PropertyFieldSpec amountProperty =
                createSimpleProperty("totalAmount", TypeName.get(BigDecimal.class), Nullability.NON_NULL);
        TypeName lineItemType = TypeName.get(Object.class);
        RelationFieldSpec itemsRelation = createOneToManyRelation("items", lineItemType);

        EntitySpec spec = EntitySpec.builder()
                .packageName(TEST_PACKAGE)
                .className(TEST_CLASS)
                .tableName(TEST_TABLE)
                .domainQualifiedName(DOMAIN_FQN)
                .idField(idField)
                .addProperty(statusProperty)
                .addProperty(amountProperty)
                .addRelation(itemsRelation)
                .build();

        // When
        TypeSpec typeSpec = JpaEntityCodegen.generate(spec);
        String code = typeSpec.toString();

        // Then - Should have all components
        assertThat(code)
                // Class annotations
                .contains("@javax.annotation.processing.Generated")
                .contains("@jakarta.persistence.Entity")
                .contains("@jakarta.persistence.Table")
                // ID field
                .contains("@jakarta.persistence.Id")
                .contains("private java.util.UUID id;")
                // Properties
                .contains("private java.lang.String status;")
                .contains("private java.math.BigDecimal totalAmount;")
                // Relationships
                .contains("@jakarta.persistence.OneToMany")
                .contains("private java.util.List<java.lang.Object> items = new java.util.ArrayList<>")
                // Constructor
                .contains("public OrderEntity()")
                // Accessors
                .contains("public java.util.UUID getId()")
                .contains("public void setId(java.util.UUID id)")
                .contains("public java.lang.String getStatus()")
                .contains("public void setStatus(java.lang.String status)")
                .contains("public java.math.BigDecimal getTotalAmount()")
                .contains("public void setTotalAmount(java.math.BigDecimal totalAmount)")
                .contains("public java.util.List<java.lang.Object> getItems()")
                .contains("public void setItems(java.util.List<java.lang.Object> items)");
    }

    @Test
    void generate_shouldHandleMultiplePropertiesOfSameType() {
        // Given
        PropertyFieldSpec firstName =
                createSimpleProperty("firstName", TypeName.get(String.class), Nullability.NON_NULL);
        PropertyFieldSpec lastName = createSimpleProperty("lastName", TypeName.get(String.class), Nullability.NON_NULL);
        EntitySpec spec = EntitySpec.builder()
                .packageName(TEST_PACKAGE)
                .className(TEST_CLASS)
                .tableName(TEST_TABLE)
                .domainQualifiedName(DOMAIN_FQN)
                .idField(createAutoIdField())
                .addProperty(firstName)
                .addProperty(lastName)
                .build();

        // When
        TypeSpec typeSpec = JpaEntityCodegen.generate(spec);
        String code = typeSpec.toString();

        // Then
        assertThat(code)
                .contains("private java.lang.String firstName;")
                .contains("private java.lang.String lastName;")
                .contains("public java.lang.String getFirstName()")
                .contains("public java.lang.String getLastName()")
                .contains("public void setFirstName(java.lang.String firstName)")
                .contains("public void setLastName(java.lang.String lastName)");
    }

    // =====================================================================
    // M11 - @AttributeOverride for multiple embedded fields of same type
    // =====================================================================

    /**
     * Creates an embedded PropertyFieldSpec with attribute overrides.
     */
    private PropertyFieldSpec createEmbeddedPropertyWithOverrides(
            String fieldName, TypeName type, List<AttributeOverride> overrides) {
        return new PropertyFieldSpec(
                fieldName,
                type,
                Nullability.NON_NULL,
                fieldName,
                true,
                false,
                false,
                type.toString(),
                false,
                null,
                null,
                overrides);
    }

    @Test
    void generate_shouldAddAttributeOverrides_whenMultipleEmbeddedFieldsOfSameType() {
        // Given - Entity with two Money fields (price and discount)
        TypeName moneyType = com.palantir.javapoet.ClassName.get("com.example.infrastructure.jpa", "MoneyEmbeddable");

        // First embedded field with overrides
        PropertyFieldSpec priceProperty = createEmbeddedPropertyWithOverrides(
                "price",
                moneyType,
                List.of(
                        new AttributeOverride("amount", "price_amount"),
                        new AttributeOverride("currency", "price_currency")));

        // Second embedded field with overrides
        PropertyFieldSpec discountProperty = createEmbeddedPropertyWithOverrides(
                "discount",
                moneyType,
                List.of(
                        new AttributeOverride("amount", "discount_amount"),
                        new AttributeOverride("currency", "discount_currency")));

        EntitySpec spec = EntitySpec.builder()
                .packageName(TEST_PACKAGE)
                .className(TEST_CLASS)
                .tableName(TEST_TABLE)
                .domainQualifiedName(DOMAIN_FQN)
                .idField(createAutoIdField())
                .addProperty(priceProperty)
                .addProperty(discountProperty)
                .build();

        // When
        TypeSpec typeSpec = JpaEntityCodegen.generate(spec);
        String code = typeSpec.toString();

        // Then - Should have @AttributeOverrides for both fields
        assertThat(code)
                .contains("@jakarta.persistence.Embedded")
                .contains("@jakarta.persistence.AttributeOverrides")
                .contains("@jakarta.persistence.AttributeOverride")
                // Price field overrides
                .contains("name = \"amount\"")
                .contains("name = \"price_amount\"")
                .contains("name = \"price_currency\"")
                // Discount field overrides
                .contains("name = \"discount_amount\"")
                .contains("name = \"discount_currency\"")
                // Fields
                .contains("private com.example.infrastructure.jpa.MoneyEmbeddable price;")
                .contains("private com.example.infrastructure.jpa.MoneyEmbeddable discount;");
    }

    @Test
    void generate_shouldNotAddAttributeOverrides_whenSingleEmbeddedField() {
        // Given - Entity with only one embedded Money field (no conflict)
        TypeName moneyType = com.palantir.javapoet.ClassName.get("com.example.infrastructure.jpa", "MoneyEmbeddable");

        // Single embedded field without overrides
        PropertyFieldSpec priceProperty = createEmbeddedProperty("price", moneyType);

        EntitySpec spec = EntitySpec.builder()
                .packageName(TEST_PACKAGE)
                .className(TEST_CLASS)
                .tableName(TEST_TABLE)
                .domainQualifiedName(DOMAIN_FQN)
                .idField(createAutoIdField())
                .addProperty(priceProperty)
                .build();

        // When
        TypeSpec typeSpec = JpaEntityCodegen.generate(spec);
        String code = typeSpec.toString();

        // Then - Should have @Embedded but no @AttributeOverrides
        assertThat(code)
                .contains("@jakarta.persistence.Embedded")
                .doesNotContain("@jakarta.persistence.AttributeOverrides")
                .doesNotContain("@jakarta.persistence.AttributeOverride")
                .contains("private com.example.infrastructure.jpa.MoneyEmbeddable price;");
    }

    @Test
    void generate_shouldAddAttributeOverrides_forNestedEmbeddable() {
        // Given - Embeddable with an embedded field that has overrides
        TypeName moneyType = com.palantir.javapoet.ClassName.get("com.example.infrastructure.jpa", "MoneyEmbeddable");

        PropertyFieldSpec unitPriceProperty = createEmbeddedPropertyWithOverrides(
                "unitPrice",
                moneyType,
                List.of(
                        new AttributeOverride("amount", "unit_price_amount"),
                        new AttributeOverride("currency", "unit_price_currency")));

        EntitySpec spec = EntitySpec.builder()
                .packageName(TEST_PACKAGE)
                .className("OrderLineEmbeddable")
                .tableName("order_lines")
                .domainQualifiedName("com.example.domain.OrderLine")
                .idField(createAutoIdField())
                .addProperty(unitPriceProperty)
                .build();

        // When
        TypeSpec typeSpec = JpaEntityCodegen.generate(spec);
        String code = typeSpec.toString();

        // Then - Should have @AttributeOverrides for nested embeddable
        assertThat(code)
                .contains("@jakarta.persistence.Embedded")
                .contains("@jakarta.persistence.AttributeOverrides")
                .contains("name = \"unit_price_amount\"")
                .contains("name = \"unit_price_currency\"");
    }
}
