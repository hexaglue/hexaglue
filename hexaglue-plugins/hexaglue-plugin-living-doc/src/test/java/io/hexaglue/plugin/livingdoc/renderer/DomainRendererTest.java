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

package io.hexaglue.plugin.livingdoc.renderer;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.arch.ElementKind;
import io.hexaglue.plugin.livingdoc.model.DebugInfo;
import io.hexaglue.plugin.livingdoc.model.DomainTypeDoc;
import io.hexaglue.plugin.livingdoc.model.IdentityDoc;
import io.hexaglue.plugin.livingdoc.model.PropertyDoc;
import io.hexaglue.plugin.livingdoc.model.RelationDoc;
import io.hexaglue.arch.model.ir.ConfidenceLevel;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DomainRendererTest {

    private DomainRenderer renderer;

    @BeforeEach
    void setUp() {
        renderer = new DomainRenderer();
    }

    @Nested
    class TypeRendering {

        @Test
        void shouldRenderBasicTypeHeader() {
            DomainTypeDoc type = createSimpleAggregate();
            String result = renderer.renderType(type);

            assertThat(result).contains("### Order");
            assertThat(result).contains("| **Kind** | Aggregate Root |");
            assertThat(result).contains("| **Package** | `com.example.domain` |");
            assertThat(result).contains("| **Type** | CLASS |");
            assertThat(result).contains("| **Confidence** | HIGH |");
        }

        @Test
        void shouldIncludeSeparatorAtEnd() {
            DomainTypeDoc type = createSimpleAggregate();
            String result = renderer.renderType(type);

            assertThat(result).endsWith("---\n\n");
        }

        @Test
        void shouldRenderValueObjectWithRecordConstruct() {
            DomainTypeDoc type = new DomainTypeDoc(
                    "Money",
                    "com.example.domain",
                    ElementKind.VALUE_OBJECT,
                    ConfidenceLevel.EXPLICIT,
                    "RECORD",
                    true,
                    null,
                    List.of(),
                    List.of(),
                    createDebugInfo("Money"));

            String result = renderer.renderType(type);

            assertThat(result).contains("### Money");
            assertThat(result).contains("| **Kind** | Value Object |");
            assertThat(result).contains("| **Type** | RECORD |");
        }

        @Test
        void shouldNotShowConfidenceBadgeForHighConfidence() {
            DomainTypeDoc type = createSimpleAggregate();
            String result = renderer.renderType(type);

            assertThat(result).contains("| **Kind** | Aggregate Root |");
            assertThat(result).doesNotContain("[Medium Confidence]");
            assertThat(result).doesNotContain("[Low Confidence - Verify]");
        }

        @Test
        void shouldShowConfidenceBadgeForMediumConfidence() {
            DomainTypeDoc type = new DomainTypeDoc(
                    "Order",
                    "com.example.domain",
                    ElementKind.AGGREGATE_ROOT,
                    ConfidenceLevel.MEDIUM,
                    "CLASS",
                    false,
                    null,
                    List.of(),
                    List.of(),
                    createDebugInfo("Order"));

            String result = renderer.renderType(type);

            assertThat(result).contains("| **Kind** | Aggregate Root [Medium Confidence] |");
        }

        @Test
        void shouldShowConfidenceBadgeForLowConfidence() {
            DomainTypeDoc type = new DomainTypeDoc(
                    "Order",
                    "com.example.domain",
                    ElementKind.AGGREGATE_ROOT,
                    ConfidenceLevel.LOW,
                    "CLASS",
                    false,
                    null,
                    List.of(),
                    List.of(),
                    createDebugInfo("Order"));

            String result = renderer.renderType(type);

            assertThat(result).contains("| **Kind** | Aggregate Root [Low Confidence - Verify] |");
        }
    }

    @Nested
    class IdentityRendering {

        @Test
        void shouldRenderWrappedIdentity() {
            IdentityDoc identity = new IdentityDoc("id", "OrderId", "UUID", "ASSIGNED", "RECORD", true, false, null);

            String result = renderer.renderIdentity(identity);

            assertThat(result).contains("**Identity**");
            assertThat(result).contains("| Field | Type | Underlying | Strategy | Wrapper |");
            assertThat(result).contains("| `id` | `OrderId` | `UUID` | ASSIGNED | RECORD |");
        }

        @Test
        void shouldRenderUnwrappedIdentity() {
            IdentityDoc identity = new IdentityDoc("id", "Long", "Long", "AUTO", "PRIMITIVE", false, true, "AUTO");

            String result = renderer.renderIdentity(identity);

            assertThat(result).contains("| `id` | `Long` | `Long` | AUTO | PRIMITIVE |");
        }

        @Test
        void shouldRenderIdentityInCompleteTypeRendering() {
            IdentityDoc identity = new IdentityDoc("id", "OrderId", "UUID", "ASSIGNED", "RECORD", true, false, null);
            DomainTypeDoc type = new DomainTypeDoc(
                    "Order",
                    "com.example.domain",
                    ElementKind.AGGREGATE_ROOT,
                    ConfidenceLevel.HIGH,
                    "CLASS",
                    false,
                    identity,
                    List.of(),
                    List.of(),
                    createDebugInfo("Order"));

            String result = renderer.renderType(type);

            assertThat(result).contains("**Identity**");
            assertThat(result).contains("OrderId");
        }

        @Test
        void shouldNotRenderIdentitySectionWhenNoIdentity() {
            DomainTypeDoc type = new DomainTypeDoc(
                    "Money",
                    "com.example.domain",
                    ElementKind.VALUE_OBJECT,
                    ConfidenceLevel.EXPLICIT,
                    "RECORD",
                    true,
                    null,
                    List.of(),
                    List.of(),
                    createDebugInfo("Money"));

            String result = renderer.renderType(type);

            assertThat(result).doesNotContain("**Identity**");
        }
    }

    @Nested
    class PropertiesRendering {

        @Test
        void shouldRenderPropertiesTable() {
            PropertyDoc prop1 = new PropertyDoc(
                    "name", "java.lang.String", "SINGLE", "NON_NULL", false, false, true, false, List.of(), null);
            PropertyDoc prop2 = new PropertyDoc(
                    "quantity", "java.lang.Integer", "SINGLE", "NON_NULL", false, false, true, false, List.of(), null);

            List<PropertyDoc> properties = List.of(prop1, prop2);

            String result = renderer.renderProperties(properties);

            assertThat(result).contains("**Properties**");
            assertThat(result).contains("| Name | Type | Cardinality | Notes |");
            assertThat(result).contains("| `name` | `String` | Single | NON_NULL |");
            assertThat(result).contains("| `quantity` | `Integer` | Single | NON_NULL |");
        }

        @Test
        void shouldRenderCollectionPropertyWithTypeArguments() {
            PropertyDoc prop = new PropertyDoc(
                    "tags",
                    "java.util.List",
                    "COLLECTION",
                    "NON_NULL",
                    false,
                    false,
                    false,
                    true,
                    List.of("java.lang.String"),
                    null);

            String result = renderer.renderProperties(List.of(prop));

            assertThat(result).contains("| `tags` | `List<String>` | Collection | NON_NULL |");
        }

        @Test
        void shouldRenderOptionalPropertyWithTypeArguments() {
            PropertyDoc prop = new PropertyDoc(
                    "email",
                    "java.util.Optional",
                    "OPTIONAL",
                    "NON_NULL",
                    false,
                    false,
                    false,
                    true,
                    List.of("java.lang.String"),
                    null);

            String result = renderer.renderProperties(List.of(prop));

            assertThat(result).contains("| `email` | `Optional<String>` | Optional | NON_NULL |");
        }

        @Test
        void shouldRenderIdentityPropertyNote() {
            PropertyDoc prop = new PropertyDoc(
                    "id", "java.lang.Long", "SINGLE", "NON_NULL", true, false, true, false, List.of(), null);

            String result = renderer.renderProperties(List.of(prop));

            assertThat(result).contains("| `id` | `Long` | Single | Identity, NON_NULL |");
        }

        @Test
        void shouldRenderEmbeddedPropertyNote() {
            PropertyDoc prop = new PropertyDoc(
                    "address",
                    "com.example.domain.Address",
                    "SINGLE",
                    "NON_NULL",
                    false,
                    true,
                    false,
                    false,
                    List.of(),
                    null);

            String result = renderer.renderProperties(List.of(prop));

            assertThat(result).contains("Embedded");
        }

        @Test
        void shouldNotRenderPropertiesSectionWhenEmpty() {
            DomainTypeDoc type = new DomainTypeDoc(
                    "EmptyType",
                    "com.example.domain",
                    ElementKind.VALUE_OBJECT,
                    ConfidenceLevel.HIGH,
                    "CLASS",
                    false,
                    null,
                    List.of(),
                    List.of(),
                    createDebugInfo("EmptyType"));

            String result = renderer.renderType(type);

            assertThat(result).doesNotContain("**Properties**");
        }
    }

    @Nested
    class RelationsRendering {

        @Test
        void shouldRenderRelationsTable() {
            RelationDoc rel = new RelationDoc(
                    "lineItems",
                    "OrderLineItem",
                    ElementKind.ENTITY.toString(),
                    "ONE_TO_MANY",
                    true,
                    false,
                    null,
                    "ALL",
                    "LAZY",
                    true);

            String result = renderer.renderRelations(List.of(rel));

            assertThat(result).contains("**Relationships**");
            assertThat(result).contains("| Target | Kind | Direction | Cascade | Fetch |");
            assertThat(result).contains("| `OrderLineItem` | ONE_TO_MANY | Owning | ALL | LAZY |");
        }

        @Test
        void shouldRenderInverseRelation() {
            RelationDoc rel = new RelationDoc(
                    "order",
                    "Order",
                    ElementKind.AGGREGATE_ROOT.toString(),
                    "MANY_TO_ONE",
                    false,
                    true,
                    "lineItems",
                    "NONE",
                    "EAGER",
                    false);

            String result = renderer.renderRelations(List.of(rel));

            assertThat(result).contains("| `Order` | MANY_TO_ONE | Inverse | NONE | EAGER |");
        }

        @Test
        void shouldRenderMultipleRelations() {
            RelationDoc rel1 = new RelationDoc(
                    "customer",
                    "Customer",
                    ElementKind.AGGREGATE_ROOT.toString(),
                    "MANY_TO_ONE",
                    false,
                    false,
                    null,
                    "NONE",
                    "LAZY",
                    false);
            RelationDoc rel2 = new RelationDoc(
                    "items",
                    "Item",
                    ElementKind.ENTITY.toString(),
                    "ONE_TO_MANY",
                    true,
                    false,
                    null,
                    "ALL",
                    "LAZY",
                    true);

            String result = renderer.renderRelations(List.of(rel1, rel2));

            assertThat(result).contains("Customer");
            assertThat(result).contains("Item");
        }

        @Test
        void shouldNotRenderRelationsSectionWhenEmpty() {
            DomainTypeDoc type = new DomainTypeDoc(
                    "SimpleType",
                    "com.example.domain",
                    ElementKind.VALUE_OBJECT,
                    ConfidenceLevel.HIGH,
                    "CLASS",
                    false,
                    null,
                    List.of(),
                    List.of(),
                    createDebugInfo("SimpleType"));

            String result = renderer.renderType(type);

            assertThat(result).doesNotContain("**Relationships**");
        }
    }

    @Nested
    class DebugSectionRendering {

        @Test
        void shouldRenderDebugSectionWithSourceLocation() {
            DomainTypeDoc type = new DomainTypeDoc(
                    "Order",
                    "com.example.domain",
                    ElementKind.AGGREGATE_ROOT,
                    ConfidenceLevel.HIGH,
                    "CLASS",
                    false,
                    null,
                    List.of(),
                    List.of(),
                    new DebugInfo(
                            "com.example.domain.Order",
                            List.of("jakarta.persistence.Entity"),
                            "src/main/java/com/example/domain/Order.java",
                            10,
                            50));

            String result = renderer.renderDebugSection(type);

            assertThat(result).contains("<details>");
            assertThat(result).contains("<summary>Debug Information</summary>");
            assertThat(result).contains("#### Type Information");
            assertThat(result).contains("| **Qualified Name** | `com.example.domain.Order` |");
            assertThat(result).contains("#### Source Location");
            assertThat(result).contains("| **File** | `src/main/java/com/example/domain/Order.java` |");
            assertThat(result).contains("| **Lines** | 10-50 |");
            assertThat(result).contains("</details>");
        }

        @Test
        void shouldRenderDebugSectionForSyntheticType() {
            DomainTypeDoc type = new DomainTypeDoc(
                    "Synthetic",
                    "com.example.domain",
                    ElementKind.VALUE_OBJECT,
                    ConfidenceLevel.LOW,
                    "CLASS",
                    false,
                    null,
                    List.of(),
                    List.of(),
                    new DebugInfo("com.example.domain.Synthetic", List.of(), null, 0, 0));

            String result = renderer.renderDebugSection(type);

            assertThat(result).contains("| **File** | *synthetic* |");
        }

        @Test
        void shouldRenderAnnotations() {
            DomainTypeDoc type = new DomainTypeDoc(
                    "Order",
                    "com.example.domain",
                    ElementKind.AGGREGATE_ROOT,
                    ConfidenceLevel.HIGH,
                    "CLASS",
                    false,
                    null,
                    List.of(),
                    List.of(),
                    new DebugInfo(
                            "com.example.domain.Order",
                            List.of("jakarta.persistence.Entity", "jakarta.persistence.Table"),
                            "Order.java",
                            10,
                            50));

            String result = renderer.renderDebugSection(type);

            assertThat(result).contains("#### Annotations");
            assertThat(result).contains("- `@Entity`");
            assertThat(result).contains("- `@Table`");
        }

        @Test
        void shouldRenderNoAnnotationsMessage() {
            DomainTypeDoc type = new DomainTypeDoc(
                    "Plain",
                    "com.example.domain",
                    ElementKind.VALUE_OBJECT,
                    ConfidenceLevel.HIGH,
                    "CLASS",
                    false,
                    null,
                    List.of(),
                    List.of(),
                    new DebugInfo("com.example.domain.Plain", List.of(), "Plain.java", 5, 15));

            String result = renderer.renderDebugSection(type);

            assertThat(result).contains("#### Annotations");
            assertThat(result).contains("*none*");
        }

        @Test
        void shouldRenderIdentityDetailsInDebugSection() {
            IdentityDoc identity = new IdentityDoc("id", "OrderId", "UUID", "ASSIGNED", "RECORD", true, false, null);
            DomainTypeDoc type = new DomainTypeDoc(
                    "Order",
                    "com.example.domain",
                    ElementKind.AGGREGATE_ROOT,
                    ConfidenceLevel.HIGH,
                    "CLASS",
                    false,
                    identity,
                    List.of(),
                    List.of(),
                    createDebugInfo("Order"));

            String result = renderer.renderDebugSection(type);

            assertThat(result).contains("#### Identity Details");
            assertThat(result).contains("| **Field Name** | `id` |");
            assertThat(result).contains("| **Declared Type** | `OrderId` |");
            assertThat(result).contains("| **Underlying Type** | `UUID` |");
            assertThat(result).contains("| **Is Wrapped** | true |");
        }

        @Test
        void shouldRenderGeneratedValueInfoWhenRequired() {
            IdentityDoc identity = new IdentityDoc("id", "Long", "Long", "AUTO", "PRIMITIVE", false, true, "AUTO");
            DomainTypeDoc type = new DomainTypeDoc(
                    "Entity",
                    "com.example.domain",
                    ElementKind.ENTITY,
                    ConfidenceLevel.HIGH,
                    "CLASS",
                    false,
                    identity,
                    List.of(),
                    List.of(),
                    createDebugInfo("Entity"));

            String result = renderer.renderDebugSection(type);

            assertThat(result).contains("| **Requires @GeneratedValue** | true |");
            assertThat(result).contains("| **JPA Generation Type** | AUTO |");
        }

        @Test
        void shouldRenderPropertyDetailsInDebugSection() {
            PropertyDoc prop = new PropertyDoc(
                    "tags",
                    "java.util.List",
                    "COLLECTION",
                    "NON_NULL",
                    false,
                    false,
                    false,
                    true,
                    List.of("java.lang.String"),
                    null);
            DomainTypeDoc type = new DomainTypeDoc(
                    "Product",
                    "com.example.domain",
                    ElementKind.AGGREGATE_ROOT,
                    ConfidenceLevel.HIGH,
                    "CLASS",
                    false,
                    null,
                    List.of(prop),
                    List.of(),
                    createDebugInfo("Product"));

            String result = renderer.renderDebugSection(type);

            assertThat(result).contains("#### Property: tags");
            assertThat(result).contains("| **Type** | `java.util.List` |");
            assertThat(result).contains("| **Is Parameterized** | true |");
            assertThat(result).contains("| **Type Arguments** | `java.lang.String` |");
        }

        @Test
        void shouldRenderRelationDetailsInDebugSection() {
            RelationDoc rel = new RelationDoc(
                    "lineItems",
                    "OrderLineItem",
                    ElementKind.ENTITY.toString(),
                    "ONE_TO_MANY",
                    true,
                    false,
                    null,
                    "ALL",
                    "LAZY",
                    true);
            DomainTypeDoc type = new DomainTypeDoc(
                    "Order",
                    "com.example.domain",
                    ElementKind.AGGREGATE_ROOT,
                    ConfidenceLevel.HIGH,
                    "CLASS",
                    false,
                    null,
                    List.of(),
                    List.of(rel),
                    createDebugInfo("Order"));

            String result = renderer.renderDebugSection(type);

            assertThat(result).contains("#### Relation: lineItems");
            assertThat(result).contains("| **Kind** | ONE_TO_MANY |");
            assertThat(result).contains("| **Orphan Removal** | true |");
        }
    }

    @Nested
    class CompleteTypeRendering {

        @Test
        void shouldRenderCompleteAggregateRoot() {
            IdentityDoc identity = new IdentityDoc("id", "OrderId", "UUID", "ASSIGNED", "RECORD", true, false, null);
            PropertyDoc prop = new PropertyDoc(
                    "status", "java.lang.String", "SINGLE", "NON_NULL", false, false, true, false, List.of(), null);
            RelationDoc rel = new RelationDoc(
                    "lineItems",
                    "OrderLineItem",
                    ElementKind.ENTITY.toString(),
                    "ONE_TO_MANY",
                    true,
                    false,
                    null,
                    "ALL",
                    "LAZY",
                    true);

            DomainTypeDoc type = new DomainTypeDoc(
                    "Order",
                    "com.example.domain",
                    ElementKind.AGGREGATE_ROOT,
                    ConfidenceLevel.HIGH,
                    "CLASS",
                    false,
                    identity,
                    List.of(prop),
                    List.of(rel),
                    createDebugInfo("Order"));

            String result = renderer.renderType(type);

            // Should contain all sections
            assertThat(result).contains("### Order");
            assertThat(result).contains("**Identity**");
            assertThat(result).contains("**Properties**");
            assertThat(result).contains("**Relationships**");
            assertThat(result).contains("<details>");
            assertThat(result).contains("</details>");
            assertThat(result).endsWith("---\n\n");
        }
    }

    // Helper methods

    private DomainTypeDoc createSimpleAggregate() {
        return new DomainTypeDoc(
                "Order",
                "com.example.domain",
                ElementKind.AGGREGATE_ROOT,
                ConfidenceLevel.HIGH,
                "CLASS",
                false,
                null,
                List.of(),
                List.of(),
                createDebugInfo("Order"));
    }

    private DebugInfo createDebugInfo(String name) {
        return new DebugInfo(
                "com.example.domain." + name,
                List.of("jakarta.persistence.Entity"),
                "src/main/java/com/example/domain/" + name + ".java",
                10,
                50);
    }
}
