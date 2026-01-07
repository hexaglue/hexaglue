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

package io.hexaglue.plugin.livingdoc.content;

import static io.hexaglue.plugin.livingdoc.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.plugin.livingdoc.model.DebugInfo;
import io.hexaglue.plugin.livingdoc.model.DomainTypeDoc;
import io.hexaglue.plugin.livingdoc.model.IdentityDoc;
import io.hexaglue.plugin.livingdoc.model.PropertyDoc;
import io.hexaglue.plugin.livingdoc.model.RelationDoc;
import io.hexaglue.spi.ir.DomainKind;
import io.hexaglue.spi.ir.DomainProperty;
import io.hexaglue.spi.ir.DomainRelation;
import io.hexaglue.spi.ir.DomainType;
import io.hexaglue.spi.ir.Identity;
import io.hexaglue.spi.ir.IrSnapshot;
import io.hexaglue.spi.ir.RelationKind;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DomainContentSelectorTest {

    private DomainContentSelector selector;

    @Nested
    class AggregateRootSelection {

        @BeforeEach
        void setUp() {
            Identity orderId = uuidIdentity("id", "OrderId");
            DomainProperty statusProp = simpleProperty("status", "java.lang.String");
            DomainRelation lineItemsRel =
                    oneToManyRelation("lineItems", "OrderLineItem", io.hexaglue.spi.ir.DomainKind.ENTITY);

            DomainType order =
                    aggregateRoot("Order", "com.example.domain", orderId, List.of(statusProp), List.of(lineItemsRel));

            IrSnapshot ir = createIrSnapshot(List.of(order), List.of());
            selector = new DomainContentSelector(ir);
        }

        @Test
        void shouldSelectAggregateRoots() {
            List<DomainTypeDoc> results = selector.selectAggregateRoots();

            assertThat(results).hasSize(1);
            DomainTypeDoc doc = results.get(0);
            assertThat(doc.name()).isEqualTo("Order");
            assertThat(doc.packageName()).isEqualTo("com.example.domain");
            assertThat(doc.kind()).isEqualTo(DomainKind.AGGREGATE_ROOT);
        }

        @Test
        void shouldTransformIdentityCorrectly() {
            List<DomainTypeDoc> results = selector.selectAggregateRoots();
            DomainTypeDoc doc = results.get(0);

            assertThat(doc.identity()).isNotNull();
            IdentityDoc identity = doc.identity();
            assertThat(identity.fieldName()).isEqualTo("id");
            assertThat(identity.type()).isEqualTo("OrderId");
            assertThat(identity.underlyingType()).isEqualTo("UUID");
            assertThat(identity.strategy()).isEqualTo("ASSIGNED");
            assertThat(identity.wrapperKind()).isEqualTo("RECORD");
            assertThat(identity.isWrapped()).isTrue();
            assertThat(identity.requiresGeneratedValue()).isFalse();
            assertThat(identity.jpaGenerationType()).isNull();
        }

        @Test
        void shouldTransformPropertiesCorrectly() {
            List<DomainTypeDoc> results = selector.selectAggregateRoots();
            DomainTypeDoc doc = results.get(0);

            assertThat(doc.properties()).hasSize(1);
            PropertyDoc prop = doc.properties().get(0);
            assertThat(prop.name()).isEqualTo("status");
            assertThat(prop.type()).isEqualTo("java.lang.String");
            assertThat(prop.cardinality()).isEqualTo("SINGLE");
            assertThat(prop.nullability()).isEqualTo("NON_NULL");
            assertThat(prop.isIdentity()).isFalse();
            assertThat(prop.isEmbedded()).isFalse();
            assertThat(prop.isSimple()).isTrue();
            assertThat(prop.isParameterized()).isFalse();
            assertThat(prop.typeArguments()).isEmpty();
        }

        @Test
        void shouldTransformRelationsCorrectly() {
            List<DomainTypeDoc> results = selector.selectAggregateRoots();
            DomainTypeDoc doc = results.get(0);

            assertThat(doc.relations()).hasSize(1);
            RelationDoc rel = doc.relations().get(0);
            assertThat(rel.propertyName()).isEqualTo("lineItems");
            assertThat(rel.targetType()).isEqualTo("OrderLineItem");
            assertThat(rel.targetKind()).isEqualTo("ENTITY");
            assertThat(rel.kind()).isEqualTo("ONE_TO_MANY");
            assertThat(rel.isOwning()).isTrue();
            assertThat(rel.isBidirectional()).isFalse();
            assertThat(rel.mappedBy()).isNull();
            assertThat(rel.cascade()).isEqualTo("ALL");
            assertThat(rel.fetch()).isEqualTo("LAZY");
            assertThat(rel.orphanRemoval()).isTrue();
        }

        @Test
        void shouldCreateDebugInfoWithSourceLocation() {
            List<DomainTypeDoc> results = selector.selectAggregateRoots();
            DomainTypeDoc doc = results.get(0);

            assertThat(doc.debug()).isNotNull();
            DebugInfo debug = doc.debug();
            assertThat(debug.qualifiedName()).isEqualTo("com.example.domain.Order");
            assertThat(debug.annotations()).containsExactly("jakarta.persistence.Entity", "jakarta.persistence.Table");
            assertThat(debug.sourceFile()).isEqualTo("src/main/java/com/example/domain/Order.java");
            assertThat(debug.lineStart()).isEqualTo(10);
            assertThat(debug.lineEnd()).isEqualTo(50);
        }
    }

    @Nested
    class EntitySelection {

        @BeforeEach
        void setUp() {
            Identity lineItemId = generatedIdentity("id");
            DomainProperty quantityProp = simpleProperty("quantity", "java.lang.Integer");

            DomainType lineItem = entity("OrderLineItem", "com.example.domain", lineItemId, List.of(quantityProp));

            IrSnapshot ir = createIrSnapshot(List.of(lineItem), List.of());
            selector = new DomainContentSelector(ir);
        }

        @Test
        void shouldSelectEntities() {
            List<DomainTypeDoc> results = selector.selectEntities();

            assertThat(results).hasSize(1);
            DomainTypeDoc doc = results.get(0);
            assertThat(doc.name()).isEqualTo("OrderLineItem");
            assertThat(doc.kind()).isEqualTo(DomainKind.ENTITY);
        }

        @Test
        void shouldTransformUnwrappedIdentity() {
            List<DomainTypeDoc> results = selector.selectEntities();
            DomainTypeDoc doc = results.get(0);

            assertThat(doc.identity()).isNotNull();
            IdentityDoc identity = doc.identity();
            assertThat(identity.fieldName()).isEqualTo("id");
            assertThat(identity.type()).isEqualTo("Long");
            assertThat(identity.underlyingType()).isEqualTo("Long");
            assertThat(identity.strategy()).isEqualTo("AUTO");
            assertThat(identity.wrapperKind()).isEqualTo("NONE");
            assertThat(identity.isWrapped()).isFalse();
            assertThat(identity.requiresGeneratedValue()).isTrue();
            assertThat(identity.jpaGenerationType()).isEqualTo("AUTO");
        }
    }

    @Nested
    class ValueObjectSelection {

        @BeforeEach
        void setUp() {
            DomainProperty amountProp = simpleProperty("amount", "java.math.BigDecimal");
            DomainProperty currencyProp = simpleProperty("currency", "java.lang.String");

            DomainType money = valueObject("Money", "com.example.domain", List.of(amountProp, currencyProp));

            IrSnapshot ir = createIrSnapshot(List.of(money), List.of());
            selector = new DomainContentSelector(ir);
        }

        @Test
        void shouldSelectValueObjects() {
            List<DomainTypeDoc> results = selector.selectValueObjects();

            assertThat(results).hasSize(1);
            DomainTypeDoc doc = results.get(0);
            assertThat(doc.name()).isEqualTo("Money");
            assertThat(doc.kind()).isEqualTo(DomainKind.VALUE_OBJECT);
            assertThat(doc.isRecord()).isTrue();
            assertThat(doc.construct()).isEqualTo("RECORD");
        }

        @Test
        void shouldHaveNoIdentity() {
            List<DomainTypeDoc> results = selector.selectValueObjects();
            DomainTypeDoc doc = results.get(0);

            assertThat(doc.identity()).isNull();
        }

        @Test
        void shouldHaveMultipleProperties() {
            List<DomainTypeDoc> results = selector.selectValueObjects();
            DomainTypeDoc doc = results.get(0);

            assertThat(doc.properties()).hasSize(2);
            assertThat(doc.properties()).extracting("name").containsExactly("amount", "currency");
        }
    }

    @Nested
    class IdentifierSelection {

        @BeforeEach
        void setUp() {
            DomainProperty valueProp = simpleProperty("value", "java.util.UUID");
            DomainType orderId = identifier("OrderId", "com.example.domain", valueProp);

            IrSnapshot ir = createIrSnapshot(List.of(orderId), List.of());
            selector = new DomainContentSelector(ir);
        }

        @Test
        void shouldSelectIdentifiers() {
            List<DomainTypeDoc> results = selector.selectIdentifiers();

            assertThat(results).hasSize(1);
            DomainTypeDoc doc = results.get(0);
            assertThat(doc.name()).isEqualTo("OrderId");
            assertThat(doc.kind()).isEqualTo(DomainKind.IDENTIFIER);
        }
    }

    @Nested
    class PropertyTransformation {

        @Test
        void shouldTransformNullableProperty() {
            DomainProperty nameProp = nullableProperty("name", "java.lang.String");
            DomainType customer = aggregateRoot(
                    "Customer", "com.example.domain", generatedIdentity("id"), List.of(nameProp), List.of());

            IrSnapshot ir = createIrSnapshot(List.of(customer), List.of());
            selector = new DomainContentSelector(ir);

            List<DomainTypeDoc> results = selector.selectAggregateRoots();
            PropertyDoc prop = results.get(0).properties().get(0);

            assertThat(prop.nullability()).isEqualTo("NULLABLE");
        }

        @Test
        void shouldTransformCollectionPropertyWithTypeArguments() {
            DomainProperty tagsProp = collectionProperty("tags", "java.lang.String");
            DomainType product = aggregateRoot(
                    "Product", "com.example.domain", generatedIdentity("id"), List.of(tagsProp), List.of());

            IrSnapshot ir = createIrSnapshot(List.of(product), List.of());
            selector = new DomainContentSelector(ir);

            List<DomainTypeDoc> results = selector.selectAggregateRoots();
            PropertyDoc prop = results.get(0).properties().get(0);

            assertThat(prop.cardinality()).isEqualTo("COLLECTION");
            assertThat(prop.isParameterized()).isTrue();
            assertThat(prop.typeArguments()).containsExactly("java.lang.String");
        }

        @Test
        void shouldTransformOptionalProperty() {
            DomainProperty emailProp = optionalProperty("email", "java.lang.String");
            DomainType user =
                    aggregateRoot("User", "com.example.domain", generatedIdentity("id"), List.of(emailProp), List.of());

            IrSnapshot ir = createIrSnapshot(List.of(user), List.of());
            selector = new DomainContentSelector(ir);

            List<DomainTypeDoc> results = selector.selectAggregateRoots();
            PropertyDoc prop = results.get(0).properties().get(0);

            assertThat(prop.cardinality()).isEqualTo("OPTIONAL");
            assertThat(prop.isParameterized()).isTrue();
            assertThat(prop.typeArguments()).containsExactly("java.lang.String");
        }

        @Test
        void shouldTransformEmbeddedProperty() {
            DomainProperty addressProp = embeddedProperty("address", "com.example.domain.Address");
            DomainType customer = aggregateRoot(
                    "Customer", "com.example.domain", generatedIdentity("id"), List.of(addressProp), List.of());

            IrSnapshot ir = createIrSnapshot(List.of(customer), List.of());
            selector = new DomainContentSelector(ir);

            List<DomainTypeDoc> results = selector.selectAggregateRoots();
            PropertyDoc prop = results.get(0).properties().get(0);

            assertThat(prop.isEmbedded()).isTrue();
        }

        @Test
        void shouldTransformPropertyWithRelationInfo() {
            DomainProperty ownerProp = propertyWithRelation(
                    "owner", "com.example.domain.User", RelationKind.MANY_TO_ONE, "com.example.domain.User");
            DomainType order = aggregateRoot(
                    "Order", "com.example.domain", generatedIdentity("id"), List.of(ownerProp), List.of());

            IrSnapshot ir = createIrSnapshot(List.of(order), List.of());
            selector = new DomainContentSelector(ir);

            List<DomainTypeDoc> results = selector.selectAggregateRoots();
            PropertyDoc prop = results.get(0).properties().get(0);

            assertThat(prop.relationInfo()).isNotNull();
            assertThat(prop.relationInfo().kind()).isEqualTo("MANY_TO_ONE");
            assertThat(prop.relationInfo().targetType()).isEqualTo("com.example.domain.User");
            assertThat(prop.relationInfo().owning()).isTrue();
            assertThat(prop.relationInfo().mappedBy()).isNull();
            assertThat(prop.relationInfo().isBidirectional()).isFalse();
            assertThat(prop.relationInfo().isEmbedded()).isFalse();
        }
    }

    @Nested
    class RelationTransformation {

        @Test
        void shouldTransformManyToOneRelation() {
            DomainRelation orderRel =
                    manyToOneRelation("order", "Order", io.hexaglue.spi.ir.DomainKind.AGGREGATE_ROOT, "lineItems");
            DomainType lineItem = aggregateRoot(
                    "OrderLineItem", "com.example.domain", generatedIdentity("id"), List.of(), List.of(orderRel));

            IrSnapshot ir = createIrSnapshot(List.of(lineItem), List.of());
            selector = new DomainContentSelector(ir);

            List<DomainTypeDoc> results = selector.selectAggregateRoots();
            RelationDoc rel = results.get(0).relations().get(0);

            assertThat(rel.kind()).isEqualTo("MANY_TO_ONE");
            assertThat(rel.isOwning()).isFalse();
            assertThat(rel.isBidirectional()).isTrue();
            assertThat(rel.mappedBy()).isEqualTo("lineItems");
            assertThat(rel.cascade()).isEqualTo("NONE");
            assertThat(rel.fetch()).isEqualTo("EAGER");
            assertThat(rel.orphanRemoval()).isFalse();
        }

        @Test
        void shouldTransformEmbeddedRelation() {
            DomainRelation addressRel =
                    embeddedRelation("address", "Address", io.hexaglue.spi.ir.DomainKind.VALUE_OBJECT);
            DomainType customer = aggregateRoot(
                    "Customer", "com.example.domain", generatedIdentity("id"), List.of(), List.of(addressRel));

            IrSnapshot ir = createIrSnapshot(List.of(customer), List.of());
            selector = new DomainContentSelector(ir);

            List<DomainTypeDoc> results = selector.selectAggregateRoots();
            RelationDoc rel = results.get(0).relations().get(0);

            assertThat(rel.kind()).isEqualTo("EMBEDDED");
            assertThat(rel.targetKind()).isEqualTo("VALUE_OBJECT");
        }
    }

    @Nested
    class DebugInfoTransformation {

        @Test
        void shouldCreateDebugInfoForSyntheticType() {
            DomainType syntheticType = new io.hexaglue.spi.ir.DomainType(
                    "com.example.domain.Synthetic",
                    "Synthetic",
                    "com.example.domain",
                    io.hexaglue.spi.ir.DomainKind.VALUE_OBJECT,
                    io.hexaglue.spi.ir.ConfidenceLevel.LOW,
                    io.hexaglue.spi.ir.JavaConstruct.CLASS,
                    java.util.Optional.empty(),
                    List.of(),
                    List.of(),
                    List.of(),
                    io.hexaglue.spi.ir.SourceRef.unknown());

            IrSnapshot ir = createIrSnapshot(List.of(syntheticType), List.of());
            selector = new DomainContentSelector(ir);

            List<DomainTypeDoc> results = selector.selectValueObjects();
            DebugInfo debug = results.get(0).debug();

            assertThat(debug.sourceFile()).isNull();
            assertThat(debug.lineStart()).isZero();
            assertThat(debug.lineEnd()).isZero();
        }

        @Test
        void shouldIncludeAnnotationsInDebugInfo() {
            Identity id = generatedIdentity("id");
            DomainType order = aggregateRoot("Order", "com.example.domain", id, List.of(), List.of());

            IrSnapshot ir = createIrSnapshot(List.of(order), List.of());
            selector = new DomainContentSelector(ir);

            List<DomainTypeDoc> results = selector.selectAggregateRoots();
            DebugInfo debug = results.get(0).debug();

            assertThat(debug.annotations()).contains("jakarta.persistence.Entity", "jakarta.persistence.Table");
        }
    }

    @Nested
    class EmptySelections {

        @BeforeEach
        void setUp() {
            IrSnapshot ir = emptyIrSnapshot();
            selector = new DomainContentSelector(ir);
        }

        @Test
        void shouldReturnEmptyListWhenNoAggregateRoots() {
            assertThat(selector.selectAggregateRoots()).isEmpty();
        }

        @Test
        void shouldReturnEmptyListWhenNoEntities() {
            assertThat(selector.selectEntities()).isEmpty();
        }

        @Test
        void shouldReturnEmptyListWhenNoValueObjects() {
            assertThat(selector.selectValueObjects()).isEmpty();
        }

        @Test
        void shouldReturnEmptyListWhenNoIdentifiers() {
            assertThat(selector.selectIdentifiers()).isEmpty();
        }
    }
}
