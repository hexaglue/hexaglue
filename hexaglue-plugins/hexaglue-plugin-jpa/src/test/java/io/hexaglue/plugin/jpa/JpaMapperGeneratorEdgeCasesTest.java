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

import static io.hexaglue.plugin.jpa.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.spi.ir.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Edge case tests for {@link JpaMapperGenerator}.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Inter-aggregate references (e.g., CustomerId in Order)</li>
 *   <li>Composite identifiers (multi-property - no unwrapping)</li>
 *   <li>Enum properties</li>
 *   <li>Temporal types</li>
 *   <li>Collections of primitive types</li>
 * </ul>
 */
@DisplayName("JpaMapperGenerator - Edge Cases")
class JpaMapperGeneratorEdgeCasesTest {

    private static final String INFRA_PKG = "com.example.infrastructure.persistence";
    private static final String DOMAIN_PKG = "com.example.domain";

    private JpaMapperGenerator generator;
    private JpaConfig config;

    @BeforeEach
    void setUp() {
        config = JpaConfig.defaults();
        generator = new JpaMapperGenerator(INFRA_PKG, DOMAIN_PKG, config);
    }

    // =========================================================================
    // Inter-Aggregate Reference Tests
    // =========================================================================

    @Nested
    @DisplayName("Inter-Aggregate References")
    class InterAggregateReferenceTests {

        @Test
        @DisplayName("should generate @Mapping for inter-aggregate reference unwrapping (e.g., customerId)")
        void shouldGenerateMappingForInterAggregateReferenceUnwrapping() {
            // Order has a CustomerId field (reference to Customer aggregate)
            // The CustomerId is a single-property IDENTIFIER that should be unwrapped
            DomainType customerId = simpleIdentifier("CustomerId", DOMAIN_PKG);
            DomainType order = orderWithCustomerId(DOMAIN_PKG);

            String code = generator.generateMapper(order, List.of(customerId));

            // toEntity should unwrap: customerId.value -> customerId
            assertThat(code)
                    .as("toEntity should unwrap inter-aggregate reference")
                    .contains("@Mapping(source = \"customerId.value\", target = \"customerId\")");
        }

        @Test
        @DisplayName("should generate @Mapping expression for inter-aggregate reference wrapping in toDomain")
        void shouldGenerateMappingExpressionForInterAggregateReferenceWrapping() {
            DomainType customerId = simpleIdentifier("CustomerId", DOMAIN_PKG);
            DomainType order = orderWithCustomerId(DOMAIN_PKG);

            String code = generator.generateMapper(order, List.of(customerId));

            // toDomain should wrap: entity.getCustomerId() -> new CustomerId(...)
            assertThat(code)
                    .as("toDomain should wrap inter-aggregate reference")
                    .contains(
                            "@Mapping(target = \"customerId\", expression = \"java(new CustomerId(entity.getCustomerId()))\")");
        }

        @Test
        @DisplayName("should NOT generate @Mapping for non-identifier reference types")
        void shouldNotGenerateMappingForNonIdentifierReferenceTypes() {
            // Order has a simple UUID customerId (not wrapped in a typed ID)
            DomainType order = orderWithRawCustomerId(DOMAIN_PKG);

            String code = generator.generateMapper(order, List.of());

            // No mapping needed for raw UUID
            assertThat(code).doesNotContain("customerId.value");
            assertThat(code).doesNotContain("new CustomerId");
        }

        @Test
        @DisplayName("should handle multiple inter-aggregate references")
        void shouldHandleMultipleInterAggregateReferences() {
            // Order has CustomerId and SellerId
            DomainType customerId = simpleIdentifier("CustomerId", DOMAIN_PKG);
            DomainType sellerId = simpleIdentifier("SellerId", DOMAIN_PKG);
            DomainType order = orderWithMultipleReferences(DOMAIN_PKG);

            String code = generator.generateMapper(order, List.of(customerId, sellerId));

            assertThat(code).contains("@Mapping(source = \"customerId.value\", target = \"customerId\")");
            assertThat(code).contains("@Mapping(source = \"sellerId.value\", target = \"sellerId\")");
        }
    }

    // =========================================================================
    // Composite Identifier Tests
    // =========================================================================

    @Nested
    @DisplayName("Composite Identifiers")
    class CompositeIdentifierTests {

        @Test
        @DisplayName("should NOT generate unwrap mapping for composite identifiers")
        void shouldNotGenerateUnwrapMappingForCompositeIdentifiers() {
            // CompositeOrderId has region and sequence - should NOT be unwrapped
            DomainType compositeId = compositeIdentifier("CompositeOrderId", DOMAIN_PKG);
            DomainType order = orderWithCompositeId(DOMAIN_PKG);

            String code = generator.generateMapper(order, List.of(compositeId));

            // Composite IDs should use embedded mapping, not .value unwrapping
            assertThat(code)
                    .as("Composite identifiers should NOT have .value unwrap mapping")
                    .doesNotContain("id.value");
        }

        @Test
        @DisplayName("should use Embeddable mapper for composite identifiers")
        void shouldUseEmbeddableMapperForCompositeIdentifiers() {
            DomainType compositeId = compositeIdentifier("CompositeOrderId", DOMAIN_PKG);
            DomainType order = orderWithCompositeId(DOMAIN_PKG);

            String code = generator.generateMapper(order, List.of(compositeId));

            // Should include CompositeOrderIdMapper in uses clause
            assertThat(code).contains("uses = {CompositeOrderIdMapper.class}");
        }
    }

    // =========================================================================
    // Enum Property Tests
    // =========================================================================

    @Nested
    @DisplayName("Enum Properties")
    class EnumPropertyTests {

        @Test
        @DisplayName("should NOT require special mapping for enum properties")
        void shouldNotRequireSpecialMappingForEnumProperties() {
            // Order has OrderStatus enum - MapStruct handles enums automatically
            DomainType order = orderWithStatus(DOMAIN_PKG);

            String code = generator.generateMapper(order, List.of());

            // Enums don't need special mapping - MapStruct handles them
            assertThat(code).doesNotContain("status.value");
            assertThat(code).doesNotContain("new OrderStatus");
        }

        @Test
        @DisplayName("should handle enum in value object")
        void shouldHandleEnumInValueObject() {
            DomainType money = moneyWithCurrencyEnum(DOMAIN_PKG);

            String code = generator.generateValueObjectMapper(money);

            // Enums are handled automatically
            assertThat(code).contains("MoneyEmbeddable toEmbeddable(Money domain)");
            assertThat(code).doesNotContain("currency.value");
        }
    }

    // =========================================================================
    // ONE_TO_MANY with Entity Mapper Tests
    // =========================================================================

    @Nested
    @DisplayName("ONE_TO_MANY Relations")
    class OneToManyRelationTests {

        @Test
        @DisplayName("should include entity mapper in uses clause for ONE_TO_MANY")
        void shouldIncludeEntityMapperInUsesClauseForOneToMany() {
            DomainType orderLine = simpleEntity("OrderLine", DOMAIN_PKG);
            DomainType order = aggregateRootWithRelations(
                    "Order", DOMAIN_PKG, List.of(oneToManyRelation("lines", DOMAIN_PKG + ".OrderLine")));

            String code = generator.generateMapper(order, List.of(orderLine));

            assertThat(code)
                    .as("Should include entity mapper for ONE_TO_MANY relation")
                    .contains("OrderLineMapper.class");
        }

        @Test
        @DisplayName("should include multiple mappers for complex aggregate")
        void shouldIncludeMultipleMappersForComplexAggregate() {
            DomainType orderLine = simpleEntity("OrderLine", DOMAIN_PKG);
            DomainType address = simpleValueObject("Address", DOMAIN_PKG);
            DomainType order = aggregateRootWithRelations(
                    "Order",
                    DOMAIN_PKG,
                    List.of(
                            oneToManyRelation("lines", DOMAIN_PKG + ".OrderLine"),
                            embeddedRelation("shippingAddress", DOMAIN_PKG + ".Address")));

            String code = generator.generateMapper(order, List.of(orderLine, address));

            assertThat(code).contains("uses = {");
            assertThat(code).contains("AddressMapper.class");
            assertThat(code).contains("OrderLineMapper.class");
        }
    }

    // =========================================================================
    // Identity with Non-Standard Field Name Tests
    // =========================================================================

    @Nested
    @DisplayName("Non-Standard Identity Field Names")
    class NonStandardIdentityFieldNameTests {

        @Test
        @DisplayName("should handle identity field named 'orderId' instead of 'id'")
        void shouldHandleIdentityFieldNamedOrderId() {
            Identity customId = new Identity(
                    "orderId",
                    TypeRef.of(DOMAIN_PKG + ".OrderId"),
                    TypeRef.of("java.util.UUID"),
                    IdentityStrategy.ASSIGNED,
                    IdentityWrapperKind.RECORD);

            DomainType type = new DomainType(
                    DOMAIN_PKG + ".Order",
                    "Order",
                    DOMAIN_PKG,
                    DomainKind.AGGREGATE_ROOT,
                    ConfidenceLevel.HIGH,
                    JavaConstruct.CLASS,
                    Optional.of(customId),
                    List.of(identityProperty("orderId", TypeRef.of(DOMAIN_PKG + ".OrderId"))),
                    List.of(),
                    List.of(),
                    SourceRef.unknown());

            String code = generator.generateMapper(type, List.of());

            assertThat(code).contains("@Mapping(source = \"orderId.value\", target = \"orderId\")");
            assertThat(code)
                    .contains(
                            "@Mapping(target = \"orderId\", expression = \"java(new OrderId(entity.getOrderId()))\")");
        }

        @Test
        @DisplayName("should handle identity field with unusual casing")
        void shouldHandleIdentityFieldWithUnusualCasing() {
            Identity customId = new Identity(
                    "transactionID",
                    TypeRef.of(DOMAIN_PKG + ".TransactionID"),
                    TypeRef.of("java.util.UUID"),
                    IdentityStrategy.ASSIGNED,
                    IdentityWrapperKind.RECORD);

            DomainType type = new DomainType(
                    DOMAIN_PKG + ".Transaction",
                    "Transaction",
                    DOMAIN_PKG,
                    DomainKind.AGGREGATE_ROOT,
                    ConfidenceLevel.HIGH,
                    JavaConstruct.CLASS,
                    Optional.of(customId),
                    List.of(identityProperty("transactionID", TypeRef.of(DOMAIN_PKG + ".TransactionID"))),
                    List.of(),
                    List.of(),
                    SourceRef.unknown());

            String code = generator.generateMapper(type, List.of());

            assertThat(code).contains("@Mapping(source = \"transactionID.value\", target = \"transactionID\")");
            assertThat(code).contains("entity.getTransactionID()");
        }
    }

    // =========================================================================
    // Naming Edge Cases
    // =========================================================================

    @Nested
    @DisplayName("Naming Edge Cases")
    class NamingEdgeCasesTests {

        @Test
        @DisplayName("should generate valid mapper for multi-word property names (camelCase)")
        void shouldHandleMultiWordPropertyNames() {
            DomainType order = aggregateWithProperties(
                    "Order",
                    DOMAIN_PKG,
                    List.of(
                            simpleProperty("shippingAddress", TypeRef.of(DOMAIN_PKG + ".Address")),
                            simpleProperty("billingContactEmail", stringType()),
                            simpleProperty("lastModifiedDateTime", TypeRef.of("java.time.LocalDateTime"))));

            String code = generator.generateMapper(order, List.of());

            // MapStruct handles camelCase properties automatically (no explicit @Mapping needed)
            // Just verify the mapper is generated correctly with toEntity/toDomain methods
            assertThat(code)
                    .as("Mapper should generate valid methods for multi-word properties")
                    .contains("OrderEntity toEntity(Order domain)")
                    .contains("Order toDomain(OrderEntity entity)");
        }

        @Test
        @DisplayName("should generate valid mapper for acronym property names (UUID, XML)")
        void shouldHandleAcronymPropertyNames() {
            DomainType document = aggregateWithProperties(
                    "Document",
                    DOMAIN_PKG,
                    List.of(
                            simpleProperty("documentUUID", uuidType()),
                            simpleProperty("xmlContent", stringType()),
                            simpleProperty("htmlPreview", stringType())));

            String code = generator.generateMapper(document, List.of());

            // MapStruct handles properties with acronyms automatically
            assertThat(code)
                    .as("Mapper should generate valid methods for acronym properties")
                    .contains("DocumentEntity toEntity(Document domain)")
                    .contains("Document toDomain(DocumentEntity entity)");
        }

        @Test
        @DisplayName("should handle single character property names")
        void shouldHandleSingleCharPropertyNames() {
            DomainType point = new DomainType(
                    DOMAIN_PKG + ".Point",
                    "Point",
                    DOMAIN_PKG,
                    DomainKind.VALUE_OBJECT,
                    ConfidenceLevel.HIGH,
                    JavaConstruct.RECORD,
                    Optional.empty(),
                    List.of(
                            simpleProperty("x", TypeRef.of("double")),
                            simpleProperty("y", TypeRef.of("double")),
                            simpleProperty("z", TypeRef.of("double"))),
                    List.of(),
                    List.of(),
                    SourceRef.unknown());

            String code = generator.generateValueObjectMapper(point);

            // Single char properties should work
            assertThat(code).contains("PointEmbeddable toEmbeddable(Point domain)");
        }

        @Test
        @DisplayName("should handle boolean property with 'is' prefix")
        void shouldHandleBooleanWithIsPrefix() {
            DomainType product = aggregateWithProperties(
                    "Product",
                    DOMAIN_PKG,
                    List.of(
                            simpleProperty("isActive", TypeRef.of("boolean")),
                            simpleProperty("isAvailable", TypeRef.of("java.lang.Boolean")),
                            simpleProperty("hasStock", TypeRef.of("boolean"))));

            String code = generator.generateMapper(product, List.of());

            // Verify 'is' prefix properties are handled
            assertThat(code).contains("ProductEntity toEntity(Product domain)");
        }

        @Test
        @DisplayName("should handle reserved keywords as property names")
        void shouldHandleReservedKeywords() {
            // Properties with names starting with reserved words (defaultValue, importPath)
            // should be handled correctly - MapStruct maps them implicitly by name
            DomainType config = aggregateWithProperties(
                    "Config",
                    DOMAIN_PKG,
                    List.of(simpleProperty("defaultValue", stringType()), simpleProperty("importPath", stringType())));

            String code = generator.generateMapper(config, List.of());

            // Mapper should generate correctly without syntax errors
            // MapStruct handles simple properties automatically by name
            assertThat(code)
                    .contains("ConfigEntity toEntity(Config domain)")
                    .contains("Config toDomain(ConfigEntity entity)")
                    .contains("@Mapper");
        }
    }

    // =========================================================================
    // Helper Fixture Methods
    // =========================================================================

    private static DomainType aggregateWithProperties(String name, String pkg, List<DomainProperty> additionalProps) {
        List<DomainProperty> props = new java.util.ArrayList<>();
        props.add(identityProperty("id", TypeRef.of(pkg + "." + name + "Id")));
        props.addAll(additionalProps);

        return new DomainType(
                pkg + "." + name,
                name,
                pkg,
                DomainKind.AGGREGATE_ROOT,
                ConfidenceLevel.HIGH,
                JavaConstruct.CLASS,
                Optional.of(wrappedUuidIdentity("id", pkg + "." + name + "Id")),
                props,
                List.of(),
                List.of(),
                SourceRef.unknown());
    }

    private static DomainType simpleIdentifier(String name, String pkg) {
        return new DomainType(
                pkg + "." + name,
                name,
                pkg,
                DomainKind.IDENTIFIER,
                ConfidenceLevel.HIGH,
                JavaConstruct.RECORD,
                Optional.empty(),
                List.of(simpleProperty("value", uuidType())),
                List.of(),
                List.of(),
                SourceRef.unknown());
    }

    private static DomainType compositeIdentifier(String name, String pkg) {
        return new DomainType(
                pkg + "." + name,
                name,
                pkg,
                DomainKind.IDENTIFIER,
                ConfidenceLevel.HIGH,
                JavaConstruct.RECORD,
                Optional.empty(),
                List.of(simpleProperty("region", stringType()), simpleProperty("sequence", longType())),
                List.of(),
                List.of(),
                SourceRef.unknown());
    }

    private static DomainType orderWithCustomerId(String pkg) {
        List<DomainProperty> props = List.of(
                identityProperty("id", TypeRef.of(pkg + ".OrderId")),
                simpleProperty("customerId", TypeRef.of(pkg + ".CustomerId")),
                simpleProperty("total", bigDecimalType()));

        return new DomainType(
                pkg + ".Order",
                "Order",
                pkg,
                DomainKind.AGGREGATE_ROOT,
                ConfidenceLevel.HIGH,
                JavaConstruct.CLASS,
                Optional.of(wrappedUuidIdentity("id", pkg + ".OrderId")),
                props,
                List.of(),
                List.of(),
                SourceRef.unknown());
    }

    private static DomainType orderWithRawCustomerId(String pkg) {
        List<DomainProperty> props = List.of(
                identityProperty("id", TypeRef.of(pkg + ".OrderId")),
                simpleProperty("customerId", uuidType()),
                simpleProperty("total", bigDecimalType()));

        return new DomainType(
                pkg + ".Order",
                "Order",
                pkg,
                DomainKind.AGGREGATE_ROOT,
                ConfidenceLevel.HIGH,
                JavaConstruct.CLASS,
                Optional.of(wrappedUuidIdentity("id", pkg + ".OrderId")),
                props,
                List.of(),
                List.of(),
                SourceRef.unknown());
    }

    private static DomainType orderWithMultipleReferences(String pkg) {
        List<DomainProperty> props = List.of(
                identityProperty("id", TypeRef.of(pkg + ".OrderId")),
                simpleProperty("customerId", TypeRef.of(pkg + ".CustomerId")),
                simpleProperty("sellerId", TypeRef.of(pkg + ".SellerId")),
                simpleProperty("total", bigDecimalType()));

        return new DomainType(
                pkg + ".Order",
                "Order",
                pkg,
                DomainKind.AGGREGATE_ROOT,
                ConfidenceLevel.HIGH,
                JavaConstruct.CLASS,
                Optional.of(wrappedUuidIdentity("id", pkg + ".OrderId")),
                props,
                List.of(),
                List.of(),
                SourceRef.unknown());
    }

    private static DomainType orderWithCompositeId(String pkg) {
        Identity compositeId = new Identity(
                "id",
                TypeRef.of(pkg + ".CompositeOrderId"),
                TypeRef.of(pkg + ".CompositeOrderId"), // Not unwrapped - same type
                IdentityStrategy.ASSIGNED,
                IdentityWrapperKind.RECORD);

        List<DomainProperty> props = List.of(
                identityProperty("id", TypeRef.of(pkg + ".CompositeOrderId")),
                simpleProperty("total", bigDecimalType()));

        return new DomainType(
                pkg + ".Order",
                "Order",
                pkg,
                DomainKind.AGGREGATE_ROOT,
                ConfidenceLevel.HIGH,
                JavaConstruct.CLASS,
                Optional.of(compositeId),
                props,
                List.of(),
                List.of(),
                SourceRef.unknown());
    }

    private static DomainType orderWithStatus(String pkg) {
        List<DomainProperty> props = List.of(
                identityProperty("id", TypeRef.of(pkg + ".OrderId")),
                simpleProperty("status", TypeRef.of(pkg + ".OrderStatus")),
                simpleProperty("total", bigDecimalType()));

        return new DomainType(
                pkg + ".Order",
                "Order",
                pkg,
                DomainKind.AGGREGATE_ROOT,
                ConfidenceLevel.HIGH,
                JavaConstruct.CLASS,
                Optional.of(wrappedUuidIdentity("id", pkg + ".OrderId")),
                props,
                List.of(),
                List.of(),
                SourceRef.unknown());
    }

    private static DomainType moneyWithCurrencyEnum(String pkg) {
        return new DomainType(
                pkg + ".Money",
                "Money",
                pkg,
                DomainKind.VALUE_OBJECT,
                ConfidenceLevel.HIGH,
                JavaConstruct.RECORD,
                Optional.empty(),
                List.of(
                        simpleProperty("amount", bigDecimalType()),
                        simpleProperty("currency", TypeRef.of(pkg + ".Currency"))),
                List.of(),
                List.of(),
                SourceRef.unknown());
    }
}
