package io.hexaglue.spi.ir.testing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.hexaglue.spi.ir.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("DomainTypeBuilder")
class DomainTypeBuilderTest {

    @Nested
    @DisplayName("Factory methods")
    class FactoryMethodsTest {

        @Test
        @DisplayName("should create aggregate root")
        void shouldCreateAggregateRoot() {
            DomainType type =
                    DomainTypeBuilder.aggregateRoot("com.example.Order").build();

            assertThat(type.kind()).isEqualTo(DomainKind.AGGREGATE_ROOT);
            assertThat(type.qualifiedName()).isEqualTo("com.example.Order");
            assertThat(type.simpleName()).isEqualTo("Order");
            assertThat(type.packageName()).isEqualTo("com.example");
            assertThat(type.isAggregateRoot()).isTrue();
        }

        @Test
        @DisplayName("should create entity")
        void shouldCreateEntity() {
            DomainType type = DomainTypeBuilder.entity("com.example.LineItem").build();

            assertThat(type.kind()).isEqualTo(DomainKind.ENTITY);
            assertThat(type.isEntity()).isTrue();
        }

        @Test
        @DisplayName("should create value object as record")
        void shouldCreateValueObject() {
            DomainType type =
                    DomainTypeBuilder.valueObject("com.example.Address").build();

            assertThat(type.kind()).isEqualTo(DomainKind.VALUE_OBJECT);
            assertThat(type.construct()).isEqualTo(JavaConstruct.RECORD);
            assertThat(type.isValueObject()).isTrue();
        }

        @Test
        @DisplayName("should create identifier")
        void shouldCreateIdentifier() {
            DomainType type =
                    DomainTypeBuilder.identifier("com.example.OrderId").build();

            assertThat(type.kind()).isEqualTo(DomainKind.IDENTIFIER);
            assertThat(type.construct()).isEqualTo(JavaConstruct.RECORD);
        }
    }

    @Nested
    @DisplayName("Identity configuration")
    class IdentityTest {

        @Test
        @DisplayName("should add wrapped identity")
        void shouldAddWrappedIdentity() {
            DomainType type = DomainTypeBuilder.aggregateRoot("com.example.Order")
                    .withIdentity("id", "com.example.OrderId", "java.util.UUID")
                    .build();

            assertThat(type.hasIdentity()).isTrue();
            Identity id = type.identity().orElseThrow();
            assertThat(id.fieldName()).isEqualTo("id");
            assertThat(id.type().qualifiedName()).isEqualTo("com.example.OrderId");
            assertThat(id.unwrappedType().qualifiedName()).isEqualTo("java.util.UUID");
            assertThat(id.isWrapped()).isTrue();
            assertThat(id.wrapperKind()).isEqualTo(IdentityWrapperKind.RECORD);
        }

        @Test
        @DisplayName("should add unwrapped identity")
        void shouldAddUnwrappedIdentity() {
            DomainType type = DomainTypeBuilder.entity("com.example.LineItem")
                    .withUnwrappedIdentity("id", "java.util.UUID")
                    .build();

            Identity id = type.identity().orElseThrow();
            assertThat(id.isWrapped()).isFalse();
            assertThat(id.wrapperKind()).isEqualTo(IdentityWrapperKind.NONE);
        }

        @Test
        @DisplayName("should add UUID identity with strategy")
        void shouldAddUuidIdentityWithStrategy() {
            DomainType type = DomainTypeBuilder.entity("com.example.Order")
                    .withUuidIdentity("id", IdentityStrategy.UUID)
                    .build();

            Identity id = type.identity().orElseThrow();
            assertThat(id.strategy()).isEqualTo(IdentityStrategy.UUID);
        }
    }

    @Nested
    @DisplayName("Properties")
    class PropertiesTest {

        @Test
        @DisplayName("should add simple property")
        void shouldAddSimpleProperty() {
            DomainType type = DomainTypeBuilder.aggregateRoot("com.example.Order")
                    .withProperty("status", "com.example.OrderStatus")
                    .build();

            assertThat(type.properties()).hasSize(1);
            DomainProperty prop = type.properties().get(0);
            assertThat(prop.name()).isEqualTo("status");
            assertThat(prop.type().qualifiedName()).isEqualTo("com.example.OrderStatus");
            assertThat(prop.cardinality()).isEqualTo(Cardinality.SINGLE);
            assertThat(prop.nullability()).isEqualTo(Nullability.NON_NULL);
        }

        @Test
        @DisplayName("should add optional property")
        void shouldAddOptionalProperty() {
            DomainType type = DomainTypeBuilder.aggregateRoot("com.example.Order")
                    .withOptionalProperty("notes", "java.lang.String")
                    .build();

            DomainProperty prop = type.properties().get(0);
            assertThat(prop.cardinality()).isEqualTo(Cardinality.OPTIONAL);
            assertThat(prop.type().isOptionalLike()).isTrue();
        }

        @Test
        @DisplayName("should add collection property")
        void shouldAddCollectionProperty() {
            DomainType type = DomainTypeBuilder.aggregateRoot("com.example.Order")
                    .withCollectionProperty("items", "com.example.LineItem")
                    .build();

            DomainProperty prop = type.properties().get(0);
            assertThat(prop.cardinality()).isEqualTo(Cardinality.COLLECTION);
            assertThat(prop.type().isCollectionLike()).isTrue();
            assertThat(prop.type().unwrapElement().qualifiedName()).isEqualTo("com.example.LineItem");
        }

        @Test
        @DisplayName("should add embedded property")
        void shouldAddEmbeddedProperty() {
            DomainType type = DomainTypeBuilder.aggregateRoot("com.example.Order")
                    .withEmbeddedProperty("shippingAddress", "com.example.Address")
                    .build();

            DomainProperty prop = type.properties().get(0);
            assertThat(prop.isEmbedded()).isTrue();
            assertThat(prop.hasRelation()).isTrue();
            assertThat(prop.relationInfo().kind()).isEqualTo(RelationKind.EMBEDDED);
        }
    }

    @Nested
    @DisplayName("Relations")
    class RelationsTest {

        @Test
        @DisplayName("should add one-to-many relation")
        void shouldAddOneToManyRelation() {
            DomainType type = DomainTypeBuilder.aggregateRoot("com.example.Order")
                    .withOneToManyRelation("items", "com.example.LineItem")
                    .build();

            assertThat(type.hasRelations()).isTrue();
            assertThat(type.relations()).hasSize(1);
            DomainRelation rel = type.relations().get(0);
            assertThat(rel.kind()).isEqualTo(RelationKind.ONE_TO_MANY);
            assertThat(rel.propertyName()).isEqualTo("items");
            assertThat(rel.targetTypeFqn()).isEqualTo("com.example.LineItem");
        }

        @Test
        @DisplayName("should add many-to-one relation")
        void shouldAddManyToOneRelation() {
            DomainType type = DomainTypeBuilder.entity("com.example.LineItem")
                    .withManyToOneRelation("order", "com.example.Order")
                    .build();

            DomainRelation rel = type.relations().get(0);
            assertThat(rel.kind()).isEqualTo(RelationKind.MANY_TO_ONE);
        }
    }

    @Nested
    @DisplayName("Annotations")
    class AnnotationsTest {

        @Test
        @DisplayName("should add custom annotation")
        void shouldAddCustomAnnotation() {
            DomainType type = DomainTypeBuilder.aggregateRoot("com.example.Order")
                    .withAnnotation("javax.persistence.Entity")
                    .build();

            assertThat(type.annotations()).containsExactly("javax.persistence.Entity");
        }

        @Test
        @DisplayName("should add jMolecules annotations")
        void shouldAddJMoleculesAnnotations() {
            DomainType type = DomainTypeBuilder.aggregateRoot("com.example.Order")
                    .withJMoleculesAggregateRoot()
                    .build();

            assertThat(type.annotations()).containsExactly("org.jmolecules.ddd.annotation.AggregateRoot");
        }
    }

    @Nested
    @DisplayName("Full scenarios")
    class FullScenarioTest {

        @Test
        @DisplayName("should build complete aggregate root")
        void shouldBuildCompleteAggregateRoot() {
            DomainType order = DomainTypeBuilder.aggregateRoot("com.example.Order")
                    .withIdentity("id", "com.example.OrderId", "java.util.UUID")
                    .withProperty("status", "com.example.OrderStatus")
                    .withEmbeddedProperty("shippingAddress", "com.example.Address")
                    .withCollectionProperty("items", "com.example.LineItem")
                    .withOneToManyRelation("items", "com.example.LineItem")
                    .withJMoleculesAggregateRoot()
                    .build();

            assertThat(order.isAggregateRoot()).isTrue();
            assertThat(order.hasIdentity()).isTrue();
            assertThat(order.properties()).hasSize(3);
            assertThat(order.hasRelations()).isTrue();
            assertThat(order.annotations()).contains("org.jmolecules.ddd.annotation.AggregateRoot");
        }
    }

    @Nested
    @DisplayName("Validation")
    class ValidationTest {

        @Test
        @DisplayName("should throw if qualifiedName not set")
        void shouldThrowIfQualifiedNameNotSet() {
            assertThatThrownBy(() -> new DomainTypeBuilder().build())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("qualifiedName is required");
        }
    }
}
