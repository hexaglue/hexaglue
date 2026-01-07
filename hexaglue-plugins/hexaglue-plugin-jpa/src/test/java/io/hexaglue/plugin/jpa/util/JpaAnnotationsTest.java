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

package io.hexaglue.plugin.jpa.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import io.hexaglue.plugin.jpa.model.RelationFieldSpec;
import io.hexaglue.spi.ir.CascadeType;
import io.hexaglue.spi.ir.DomainKind;
import io.hexaglue.spi.ir.FetchType;
import io.hexaglue.spi.ir.Identity;
import io.hexaglue.spi.ir.IdentityStrategy;
import io.hexaglue.spi.ir.IdentityWrapperKind;
import io.hexaglue.spi.ir.Nullability;
import io.hexaglue.spi.ir.RelationKind;
import io.hexaglue.spi.ir.TypeRef;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link JpaAnnotations}.
 *
 * <p>Tests validate the behavior of JPA annotation builders, ensuring correct
 * generation of all JPA, MapStruct, and Spring annotations with appropriate attributes.
 */
class JpaAnnotationsTest {

    // =====================================================================
    // Class-level annotations tests
    // =====================================================================

    @Test
    void generated_shouldCreateAnnotationWithGeneratorName() {
        AnnotationSpec annotation = JpaAnnotations.generated("io.hexaglue.plugin.jpa.JpaPlugin");

        String annotationCode = annotation.toString();
        assertThat(annotationCode)
                .contains("@javax.annotation.processing.Generated")
                .contains("\"io.hexaglue.plugin.jpa.JpaPlugin\"");
    }

    @Test
    void generated_shouldThrowExceptionForNullGenerator() {
        assertThatThrownBy(() -> JpaAnnotations.generated(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Generator name cannot be null or empty");
    }

    @Test
    void generated_shouldThrowExceptionForEmptyGenerator() {
        assertThatThrownBy(() -> JpaAnnotations.generated(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Generator name cannot be null or empty");
    }

    @Test
    void entity_shouldCreateEntityAnnotation() {
        AnnotationSpec annotation = JpaAnnotations.entity();

        String annotationCode = annotation.toString();
        assertThat(annotationCode).contains("@jakarta.persistence.Entity");
    }

    @Test
    void table_shouldCreateTableAnnotationWithName() {
        AnnotationSpec annotation = JpaAnnotations.table("orders");

        String annotationCode = annotation.toString();
        assertThat(annotationCode).contains("@jakarta.persistence.Table").contains("name = \"orders\"");
    }

    @Test
    void table_shouldThrowExceptionForNullName() {
        assertThatThrownBy(() -> JpaAnnotations.table(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Table name cannot be null or empty");
    }

    @Test
    void table_shouldThrowExceptionForEmptyName() {
        assertThatThrownBy(() -> JpaAnnotations.table(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Table name cannot be null or empty");
    }

    @Test
    void embeddable_shouldCreateEmbeddableAnnotation() {
        AnnotationSpec annotation = JpaAnnotations.embeddable();

        String annotationCode = annotation.toString();
        assertThat(annotationCode).contains("@jakarta.persistence.Embeddable");
    }

    // =====================================================================
    // Identity field annotations tests
    // =====================================================================

    @Test
    void id_shouldCreateIdAnnotation() {
        AnnotationSpec annotation = JpaAnnotations.id();

        String annotationCode = annotation.toString();
        assertThat(annotationCode).contains("@jakarta.persistence.Id");
    }

    @Test
    void generatedValue_shouldReturnNullForAssignedStrategy() {
        TypeRef uuidType = TypeRef.of("java.util.UUID");
        Identity identity = new Identity("id", uuidType, uuidType, IdentityStrategy.ASSIGNED, IdentityWrapperKind.NONE);

        AnnotationSpec annotation = JpaAnnotations.generatedValue(identity);

        assertThat(annotation).isNull();
    }

    @Test
    void generatedValue_shouldReturnNullForNaturalStrategy() {
        TypeRef stringType = TypeRef.of("java.lang.String");
        Identity identity =
                new Identity("isbn", stringType, stringType, IdentityStrategy.NATURAL, IdentityWrapperKind.NONE);

        AnnotationSpec annotation = JpaAnnotations.generatedValue(identity);

        assertThat(annotation).isNull();
    }

    @Test
    void generatedValue_shouldCreateAnnotationForAutoStrategy() {
        TypeRef longType = TypeRef.of("java.lang.Long");
        Identity identity = new Identity("id", longType, longType, IdentityStrategy.AUTO, IdentityWrapperKind.NONE);

        AnnotationSpec annotation = JpaAnnotations.generatedValue(identity);

        assertThat(annotation).isNotNull();
        String annotationCode = annotation.toString();
        assertThat(annotationCode)
                .contains("@jakarta.persistence.GeneratedValue")
                .contains("strategy = jakarta.persistence.GenerationType.AUTO");
    }

    @Test
    void generatedValue_shouldCreateAnnotationForIdentityStrategy() {
        TypeRef longType = TypeRef.of("java.lang.Long");
        Identity identity = new Identity("id", longType, longType, IdentityStrategy.IDENTITY, IdentityWrapperKind.NONE);

        AnnotationSpec annotation = JpaAnnotations.generatedValue(identity);

        assertThat(annotation).isNotNull();
        String annotationCode = annotation.toString();
        assertThat(annotationCode)
                .contains("@jakarta.persistence.GeneratedValue")
                .contains("strategy = jakarta.persistence.GenerationType.IDENTITY");
    }

    @Test
    void generatedValue_shouldCreateAnnotationForSequenceStrategy() {
        TypeRef longType = TypeRef.of("java.lang.Long");
        Identity identity = new Identity("id", longType, longType, IdentityStrategy.SEQUENCE, IdentityWrapperKind.NONE);

        AnnotationSpec annotation = JpaAnnotations.generatedValue(identity);

        assertThat(annotation).isNotNull();
        String annotationCode = annotation.toString();
        assertThat(annotationCode)
                .contains("@jakarta.persistence.GeneratedValue")
                .contains("strategy = jakarta.persistence.GenerationType.SEQUENCE");
    }

    @Test
    void generatedValue_shouldCreateAnnotationForUuidStrategy() {
        TypeRef uuidType = TypeRef.of("java.util.UUID");
        Identity identity = new Identity("id", uuidType, uuidType, IdentityStrategy.UUID, IdentityWrapperKind.NONE);

        AnnotationSpec annotation = JpaAnnotations.generatedValue(identity);

        assertThat(annotation).isNotNull();
        String annotationCode = annotation.toString();
        assertThat(annotationCode)
                .contains("@jakarta.persistence.GeneratedValue")
                .contains("strategy = jakarta.persistence.GenerationType.UUID");
    }

    @Test
    void generatedValue_shouldThrowExceptionForNullIdentity() {
        assertThatThrownBy(() -> JpaAnnotations.generatedValue(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Identity cannot be null");
    }

    // =====================================================================
    // Property field annotations tests
    // =====================================================================

    @Test
    void column_shouldSetNullableFalseForNonNull() {
        AnnotationSpec annotation = JpaAnnotations.column("first_name", Nullability.NON_NULL);

        String annotationCode = annotation.toString();
        assertThat(annotationCode)
                .contains("@jakarta.persistence.Column")
                .contains("name = \"first_name\"")
                .contains("nullable = false");
    }

    @Test
    void column_shouldOmitNullableForNullable() {
        AnnotationSpec annotation = JpaAnnotations.column("middle_name", Nullability.NULLABLE);

        String annotationCode = annotation.toString();
        assertThat(annotationCode)
                .contains("@jakarta.persistence.Column")
                .contains("name = \"middle_name\"")
                .doesNotContain("nullable");
    }

    @Test
    void column_shouldOmitNullableForUnknown() {
        AnnotationSpec annotation = JpaAnnotations.column("suffix", Nullability.UNKNOWN);

        String annotationCode = annotation.toString();
        assertThat(annotationCode)
                .contains("@jakarta.persistence.Column")
                .contains("name = \"suffix\"")
                .doesNotContain("nullable");
    }

    @Test
    void column_shouldThrowExceptionForNullColumnName() {
        assertThatThrownBy(() -> JpaAnnotations.column(null, Nullability.NON_NULL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Column name cannot be null or empty");
    }

    @Test
    void column_shouldThrowExceptionForEmptyColumnName() {
        assertThatThrownBy(() -> JpaAnnotations.column("", Nullability.NON_NULL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Column name cannot be null or empty");
    }

    @Test
    void column_shouldThrowExceptionForNullNullability() {
        assertThatThrownBy(() -> JpaAnnotations.column("name", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Nullability cannot be null");
    }

    @Test
    void embedded_shouldCreateEmbeddedAnnotation() {
        AnnotationSpec annotation = JpaAnnotations.embedded();

        String annotationCode = annotation.toString();
        assertThat(annotationCode).contains("@jakarta.persistence.Embedded");
    }

    // =====================================================================
    // Relationship annotations tests
    // =====================================================================

    @Test
    void relationAnnotation_shouldCreateOneToMany() {
        RelationFieldSpec relation = new RelationFieldSpec(
                "lineItems",
                ClassName.bestGuess("com.example.LineItem"),
                RelationKind.ONE_TO_MANY,
                DomainKind.ENTITY,
                "order",
                CascadeType.ALL,
                FetchType.LAZY,
                true);

        AnnotationSpec annotation = JpaAnnotations.relationAnnotation(relation);

        String annotationCode = annotation.toString();
        assertThat(annotationCode)
                .contains("@jakarta.persistence.OneToMany")
                .contains("mappedBy = \"order\"")
                .contains("cascade = jakarta.persistence.CascadeType.ALL")
                .contains("fetch = jakarta.persistence.FetchType.LAZY")
                .contains("orphanRemoval = true");
    }

    @Test
    void relationAnnotation_shouldCreateManyToOne() {
        RelationFieldSpec relation = new RelationFieldSpec(
                "order",
                ClassName.bestGuess("com.example.Order"),
                RelationKind.MANY_TO_ONE,
                DomainKind.AGGREGATE_ROOT,
                null,
                CascadeType.PERSIST,
                FetchType.LAZY,
                false);

        AnnotationSpec annotation = JpaAnnotations.relationAnnotation(relation);

        String annotationCode = annotation.toString();
        assertThat(annotationCode)
                .contains("@jakarta.persistence.ManyToOne")
                .contains("fetch = jakarta.persistence.FetchType.LAZY")
                .contains("cascade = jakarta.persistence.CascadeType.PERSIST")
                .doesNotContain("orphanRemoval");
    }

    @Test
    void relationAnnotation_shouldCreateOneToOne() {
        RelationFieldSpec relation = new RelationFieldSpec(
                "shippingAddress",
                ClassName.bestGuess("com.example.Address"),
                RelationKind.ONE_TO_ONE,
                DomainKind.ENTITY,
                null,
                CascadeType.ALL,
                FetchType.EAGER,
                false);

        AnnotationSpec annotation = JpaAnnotations.relationAnnotation(relation);

        String annotationCode = annotation.toString();
        assertThat(annotationCode)
                .contains("@jakarta.persistence.OneToOne")
                .contains("cascade = jakarta.persistence.CascadeType.ALL")
                .contains("fetch = jakarta.persistence.FetchType.EAGER");
    }

    @Test
    void relationAnnotation_shouldCreateManyToMany() {
        RelationFieldSpec relation = new RelationFieldSpec(
                "categories",
                ClassName.bestGuess("com.example.Category"),
                RelationKind.MANY_TO_MANY,
                DomainKind.AGGREGATE_ROOT,
                "products",
                CascadeType.PERSIST,
                FetchType.LAZY,
                false);

        AnnotationSpec annotation = JpaAnnotations.relationAnnotation(relation);

        String annotationCode = annotation.toString();
        assertThat(annotationCode)
                .contains("@jakarta.persistence.ManyToMany")
                .contains("mappedBy = \"products\"")
                .contains("cascade = jakarta.persistence.CascadeType.PERSIST")
                .contains("fetch = jakarta.persistence.FetchType.LAZY");
    }

    @Test
    void relationAnnotation_shouldCreateEmbedded() {
        RelationFieldSpec relation = new RelationFieldSpec(
                "address",
                ClassName.bestGuess("com.example.Address"),
                RelationKind.EMBEDDED,
                DomainKind.VALUE_OBJECT,
                null,
                CascadeType.NONE,
                null,
                false);

        AnnotationSpec annotation = JpaAnnotations.relationAnnotation(relation);

        String annotationCode = annotation.toString();
        assertThat(annotationCode).contains("@jakarta.persistence.Embedded");
    }

    @Test
    void relationAnnotation_shouldCreateElementCollection() {
        RelationFieldSpec relation = new RelationFieldSpec(
                "tags",
                ClassName.bestGuess("com.example.Tag"),
                RelationKind.ELEMENT_COLLECTION,
                DomainKind.VALUE_OBJECT,
                null,
                CascadeType.NONE,
                FetchType.LAZY,
                false);

        AnnotationSpec annotation = JpaAnnotations.relationAnnotation(relation);

        String annotationCode = annotation.toString();
        assertThat(annotationCode)
                .contains("@jakarta.persistence.ElementCollection")
                .contains("fetch = jakarta.persistence.FetchType.LAZY");
    }

    @Test
    void relationAnnotation_shouldHandleNoCascade() {
        RelationFieldSpec relation = new RelationFieldSpec(
                "order",
                ClassName.bestGuess("com.example.Order"),
                RelationKind.MANY_TO_ONE,
                DomainKind.AGGREGATE_ROOT,
                null,
                CascadeType.NONE,
                null,
                false);

        AnnotationSpec annotation = JpaAnnotations.relationAnnotation(relation);

        String annotationCode = annotation.toString();
        assertThat(annotationCode).contains("@jakarta.persistence.ManyToOne").doesNotContain("cascade");
    }

    @Test
    void relationAnnotation_shouldHandleNoFetch() {
        RelationFieldSpec relation = new RelationFieldSpec(
                "order",
                ClassName.bestGuess("com.example.Order"),
                RelationKind.MANY_TO_ONE,
                DomainKind.AGGREGATE_ROOT,
                null,
                CascadeType.NONE,
                null,
                false);

        AnnotationSpec annotation = JpaAnnotations.relationAnnotation(relation);

        String annotationCode = annotation.toString();
        assertThat(annotationCode).contains("@jakarta.persistence.ManyToOne").doesNotContain("fetch");
    }

    @Test
    void relationAnnotation_shouldThrowExceptionForNullRelation() {
        assertThatThrownBy(() -> JpaAnnotations.relationAnnotation(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Relation cannot be null");
    }

    @Test
    void joinColumn_shouldCreateJoinColumnAnnotation() {
        AnnotationSpec annotation = JpaAnnotations.joinColumn("order_id");

        String annotationCode = annotation.toString();
        assertThat(annotationCode).contains("@jakarta.persistence.JoinColumn").contains("name = \"order_id\"");
    }

    @Test
    void joinColumn_shouldThrowExceptionForNullName() {
        assertThatThrownBy(() -> JpaAnnotations.joinColumn(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Join column name cannot be null or empty");
    }

    @Test
    void joinColumn_shouldThrowExceptionForEmptyName() {
        assertThatThrownBy(() -> JpaAnnotations.joinColumn(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Join column name cannot be null or empty");
    }

    // =====================================================================
    // MapStruct annotations tests
    // =====================================================================

    @Test
    void mapper_shouldCreateMapperWithSpringComponentModel() {
        AnnotationSpec annotation = JpaAnnotations.mapper();

        String annotationCode = annotation.toString();
        assertThat(annotationCode).contains("@org.mapstruct.Mapper").contains("componentModel = \"spring\"");
    }

    @Test
    void mapping_shouldCreateMappingWithSourceAndTarget() {
        AnnotationSpec annotation = JpaAnnotations.mapping("id", "orderId.value");

        String annotationCode = annotation.toString();
        assertThat(annotationCode)
                .contains("@org.mapstruct.Mapping")
                .contains("target = \"id\"")
                .contains("source = \"orderId.value\"");
    }

    @Test
    void mapping_shouldThrowExceptionForNullTarget() {
        assertThatThrownBy(() -> JpaAnnotations.mapping(null, "source"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Target cannot be null or empty");
    }

    @Test
    void mapping_shouldThrowExceptionForEmptyTarget() {
        assertThatThrownBy(() -> JpaAnnotations.mapping("", "source"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Target cannot be null or empty");
    }

    @Test
    void mapping_shouldThrowExceptionForNullSource() {
        assertThatThrownBy(() -> JpaAnnotations.mapping("target", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Source cannot be null or empty");
    }

    @Test
    void mapping_shouldThrowExceptionForEmptySource() {
        assertThatThrownBy(() -> JpaAnnotations.mapping("target", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Source cannot be null or empty");
    }

    @Test
    void mappingExpression_shouldCreateMappingWithExpression() {
        AnnotationSpec annotation = JpaAnnotations.mappingExpression("createdAt", "java.time.Instant.now()");

        String annotationCode = annotation.toString();
        assertThat(annotationCode)
                .contains("@org.mapstruct.Mapping")
                .contains("target = \"createdAt\"")
                .contains("expression = \"java(java.time.Instant.now())\"");
    }

    @Test
    void mappingExpression_shouldThrowExceptionForNullTarget() {
        assertThatThrownBy(() -> JpaAnnotations.mappingExpression(null, "expression"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Target cannot be null or empty");
    }

    @Test
    void mappingExpression_shouldThrowExceptionForEmptyTarget() {
        assertThatThrownBy(() -> JpaAnnotations.mappingExpression("", "expression"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Target cannot be null or empty");
    }

    @Test
    void mappingExpression_shouldThrowExceptionForNullExpression() {
        assertThatThrownBy(() -> JpaAnnotations.mappingExpression("target", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Expression cannot be null or empty");
    }

    @Test
    void mappingExpression_shouldThrowExceptionForEmptyExpression() {
        assertThatThrownBy(() -> JpaAnnotations.mappingExpression("target", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Expression cannot be null or empty");
    }

    // =====================================================================
    // Spring annotations tests
    // =====================================================================

    @Test
    void repository_shouldCreateRepositoryAnnotation() {
        AnnotationSpec annotation = JpaAnnotations.repository();

        String annotationCode = annotation.toString();
        assertThat(annotationCode).contains("@org.springframework.stereotype.Repository");
    }

    @Test
    void component_shouldCreateComponentAnnotation() {
        AnnotationSpec annotation = JpaAnnotations.component();

        String annotationCode = annotation.toString();
        assertThat(annotationCode).contains("@org.springframework.stereotype.Component");
    }
}
