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
 * Edge case tests for {@link JpaEntityGenerator}.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Enum properties (with @Enumerated annotation)</li>
 *   <li>Temporal types (LocalDate, Instant, etc.)</li>
 *   <li>BigDecimal with precision/scale</li>
 *   <li>Byte arrays with @Lob</li>
 *   <li>Composite identifiers as @Embedded</li>
 * </ul>
 */
@DisplayName("JpaEntityGenerator - Edge Cases")
class JpaEntityGeneratorEdgeCasesTest {

    private static final String INFRA_PKG = "com.example.infrastructure.persistence";
    private static final String DOMAIN_PKG = "com.example.domain";

    private JpaConfig config;

    @BeforeEach
    void setUp() {
        config = JpaConfig.defaults();
    }

    private JpaEntityGenerator generatorWithTypes(List<DomainType> allTypes) {
        return new JpaEntityGenerator(INFRA_PKG, config, allTypes);
    }

    // =========================================================================
    // Enum Property Tests
    // =========================================================================

    @Nested
    @DisplayName("Enum Properties")
    class EnumPropertyTests {

        @Test
        @DisplayName("should generate @Enumerated(EnumType.STRING) for enum properties")
        void shouldGenerateEnumeratedAnnotationForEnumProperties() {
            DomainType order = orderWithEnumStatus(DOMAIN_PKG);
            JpaEntityGenerator generator = generatorWithTypes(List.of(order));

            String code = generator.generateEntity(order);

            assertThat(code)
                    .as("Enum properties should have @Enumerated(EnumType.STRING)")
                    .contains("@Enumerated(EnumType.STRING)")
                    .contains("private OrderStatus status;");
        }

        @Test
        @DisplayName("should import EnumType when enum properties exist")
        void shouldImportEnumTypeWhenEnumPropertiesExist() {
            DomainType order = orderWithEnumStatus(DOMAIN_PKG);
            JpaEntityGenerator generator = generatorWithTypes(List.of(order));

            String code = generator.generateEntity(order);

            assertThat(code).contains("import jakarta.persistence.EnumType;");
            assertThat(code).contains("import jakarta.persistence.Enumerated;");
        }

        @Test
        @DisplayName("should handle enum in value object embeddable")
        void shouldHandleEnumInValueObjectEmbeddable() {
            DomainType money = moneyWithCurrencyEnum(DOMAIN_PKG);
            JpaEntityGenerator generator = generatorWithTypes(List.of(money));

            String code = generator.generateEmbeddable(money);

            // Should work even without @Enumerated (JPA default behavior)
            assertThat(code).contains("private Currency currency;");
        }

        @Test
        @DisplayName("should generate @ElementCollection for collection of enums")
        void shouldGenerateElementCollectionForEnumCollection() {
            DomainType order = orderWithEnumCollection(DOMAIN_PKG);
            JpaEntityGenerator generator = generatorWithTypes(List.of(order));

            String code = generator.generateEntity(order);

            assertThat(code)
                    .contains("@ElementCollection")
                    .contains("@Enumerated(EnumType.STRING)")
                    .contains("private Set<OrderTag> tags;");
        }
    }

    // =========================================================================
    // Temporal Type Tests
    // =========================================================================

    @Nested
    @DisplayName("Temporal Types")
    class TemporalTypeTests {

        @Test
        @DisplayName("should generate LocalDate property without special annotations")
        void shouldGenerateLocalDateProperty() {
            DomainType order = orderWithDates(DOMAIN_PKG);
            JpaEntityGenerator generator = generatorWithTypes(List.of(order));

            String code = generator.generateEntity(order);

            // LocalDate is handled by JPA without @Temporal (only for java.util.Date)
            assertThat(code).contains("private LocalDate orderDate;");
        }

        @Test
        @DisplayName("should generate LocalDateTime property")
        void shouldGenerateLocalDateTimeProperty() {
            DomainType order = orderWithTimestamps(DOMAIN_PKG);
            JpaEntityGenerator generator = generatorWithTypes(List.of(order));

            String code = generator.generateEntity(order);

            assertThat(code).contains("private LocalDateTime createdAt;");
        }

        @Test
        @DisplayName("should generate Instant property")
        void shouldGenerateInstantProperty() {
            DomainType order = orderWithInstant(DOMAIN_PKG);
            JpaEntityGenerator generator = generatorWithTypes(List.of(order));

            String code = generator.generateEntity(order);

            assertThat(code).contains("private Instant lastModifiedAt;");
        }

        @Test
        @DisplayName("should generate ZonedDateTime property")
        void shouldGenerateZonedDateTimeProperty() {
            DomainType event = eventWithZonedDateTime(DOMAIN_PKG);
            JpaEntityGenerator generator = generatorWithTypes(List.of(event));

            String code = generator.generateEntity(event);

            assertThat(code).contains("private ZonedDateTime occurredAt;");
        }
    }

    // =========================================================================
    // BigDecimal Tests
    // =========================================================================

    @Nested
    @DisplayName("BigDecimal Properties")
    class BigDecimalTests {

        @Test
        @DisplayName("should generate BigDecimal property without special annotations (default)")
        void shouldGenerateBigDecimalProperty() {
            DomainType order = orderWithTotal(DOMAIN_PKG);
            JpaEntityGenerator generator = generatorWithTypes(List.of(order));

            String code = generator.generateEntity(order);

            assertThat(code).contains("private BigDecimal total;");
        }

        @Test
        @DisplayName("should generate @Column with precision and scale for currency amounts")
        void shouldGenerateColumnWithPrecisionAndScale() {
            // Money typically needs precision(19) and scale(2)
            DomainType money = moneyValueObject(DOMAIN_PKG);
            JpaEntityGenerator generator = generatorWithTypes(List.of(money));

            String code = generator.generateEmbeddable(money);

            assertThat(code)
                    .as("Currency amounts should have precision/scale")
                    .contains("@Column(precision = 19, scale = 2)")
                    .contains("private BigDecimal amount;");
        }
    }

    // =========================================================================
    // Byte Array Tests
    // =========================================================================

    @Nested
    @DisplayName("Byte Array Properties")
    class ByteArrayTests {

        @Test
        @DisplayName("should generate byte[] property")
        void shouldGenerateByteArrayProperty() {
            DomainType document = documentWithContent(DOMAIN_PKG);
            JpaEntityGenerator generator = generatorWithTypes(List.of(document));

            String code = generator.generateEntity(document);

            assertThat(code).contains("private byte[] content;");
        }

        @Test
        @DisplayName("should generate @Lob for large byte arrays")
        void shouldGenerateLobForLargeByteArrays() {
            DomainType document = documentWithContent(DOMAIN_PKG);
            JpaEntityGenerator generator = generatorWithTypes(List.of(document));

            String code = generator.generateEntity(document);

            assertThat(code)
                    .as("Large byte arrays should have @Lob")
                    .contains("@Lob")
                    .contains("private byte[] content;");
        }
    }

    // =========================================================================
    // Composite Identifier Tests
    // =========================================================================

    @Nested
    @DisplayName("Composite Identifiers")
    class CompositeIdentifierTests {

        @Test
        @DisplayName("should generate @EmbeddedId for composite identifiers")
        void shouldGenerateEmbeddedIdForCompositeIdentifiers() {
            DomainType compositeId = compositeIdentifier("CompositeOrderId", DOMAIN_PKG);
            DomainType order = orderWithCompositeId(DOMAIN_PKG);
            JpaEntityGenerator generator = generatorWithTypes(List.of(compositeId, order));

            String code = generator.generateEntity(order);

            assertThat(code)
                    .as("Composite identifiers should use @EmbeddedId")
                    .contains("@EmbeddedId")
                    .doesNotContain("@Id")
                    .contains("private CompositeOrderIdEmbeddable id;");
        }
    }

    // =========================================================================
    // Inter-Aggregate Reference Tests
    // =========================================================================

    @Nested
    @DisplayName("Inter-Aggregate References")
    class InterAggregateReferenceTests {

        @Test
        @DisplayName("should generate raw UUID for inter-aggregate references")
        void shouldGenerateRawUuidForInterAggregateReferences() {
            DomainType customerId = simpleIdentifier("CustomerId", DOMAIN_PKG);
            DomainType order = orderWithCustomerId(DOMAIN_PKG);
            JpaEntityGenerator generator = generatorWithTypes(List.of(customerId, order));

            String code = generator.generateEntity(order);

            // Inter-aggregate references should be stored as raw UUID, not @Embedded
            assertThat(code)
                    .contains("private UUID customerId;")
                    .doesNotContain("@Embedded")
                    .doesNotContain("CustomerIdEmbeddable");
        }

        @Test
        @DisplayName("should handle multiple inter-aggregate references")
        void shouldHandleMultipleInterAggregateReferences() {
            DomainType customerId = simpleIdentifier("CustomerId", DOMAIN_PKG);
            DomainType sellerId = simpleIdentifier("SellerId", DOMAIN_PKG);
            DomainType order = orderWithMultipleReferences(DOMAIN_PKG);
            JpaEntityGenerator generator = generatorWithTypes(List.of(customerId, sellerId, order));

            String code = generator.generateEntity(order);

            assertThat(code).contains("private UUID customerId;").contains("private UUID sellerId;");
        }
    }

    // =========================================================================
    // Helper Fixture Methods
    // =========================================================================

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

    private static DomainType orderWithEnumStatus(String pkg) {
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

    private static DomainType orderWithEnumCollection(String pkg) {
        TypeRef setOfTags =
                TypeRef.parameterized("java.util.Set", Cardinality.COLLECTION, List.of(TypeRef.of(pkg + ".OrderTag")));

        List<DomainProperty> props = List.of(
                identityProperty("id", TypeRef.of(pkg + ".OrderId")),
                new DomainProperty(
                        "tags", setOfTags, Cardinality.COLLECTION, Nullability.NON_NULL, false, false, null));

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

    private static DomainType orderWithDates(String pkg) {
        List<DomainProperty> props = List.of(
                identityProperty("id", TypeRef.of(pkg + ".OrderId")),
                simpleProperty("orderDate", TypeRef.of("java.time.LocalDate")));

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

    private static DomainType orderWithTimestamps(String pkg) {
        List<DomainProperty> props = List.of(
                identityProperty("id", TypeRef.of(pkg + ".OrderId")),
                simpleProperty("createdAt", TypeRef.of("java.time.LocalDateTime")));

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

    private static DomainType orderWithInstant(String pkg) {
        List<DomainProperty> props = List.of(
                identityProperty("id", TypeRef.of(pkg + ".OrderId")),
                simpleProperty("lastModifiedAt", TypeRef.of("java.time.Instant")));

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

    private static DomainType eventWithZonedDateTime(String pkg) {
        List<DomainProperty> props = List.of(
                identityProperty("id", TypeRef.of(pkg + ".EventId")),
                simpleProperty("occurredAt", TypeRef.of("java.time.ZonedDateTime")));

        return new DomainType(
                pkg + ".Event",
                "Event",
                pkg,
                DomainKind.AGGREGATE_ROOT,
                ConfidenceLevel.HIGH,
                JavaConstruct.CLASS,
                Optional.of(wrappedUuidIdentity("id", pkg + ".EventId")),
                props,
                List.of(),
                List.of(),
                SourceRef.unknown());
    }

    private static DomainType orderWithTotal(String pkg) {
        List<DomainProperty> props = List.of(
                identityProperty("id", TypeRef.of(pkg + ".OrderId")), simpleProperty("total", bigDecimalType()));

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

    private static DomainType documentWithContent(String pkg) {
        List<DomainProperty> props = List.of(
                identityProperty("id", TypeRef.of(pkg + ".DocumentId")),
                simpleProperty("content", TypeRef.of("byte[]")));

        return new DomainType(
                pkg + ".Document",
                "Document",
                pkg,
                DomainKind.AGGREGATE_ROOT,
                ConfidenceLevel.HIGH,
                JavaConstruct.CLASS,
                Optional.of(wrappedUuidIdentity("id", pkg + ".DocumentId")),
                props,
                List.of(),
                List.of(),
                SourceRef.unknown());
    }

    private static DomainType orderWithCompositeId(String pkg) {
        Identity compositeId = new Identity(
                "id",
                TypeRef.of(pkg + ".CompositeOrderId"),
                TypeRef.of(pkg + ".CompositeOrderId"),
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

    // =========================================================================
    // Primitive Type Tests (regression for @Enumerated bug)
    // =========================================================================

    @Nested
    @DisplayName("Primitive Types")
    class PrimitiveTypeTests {

        @Test
        @DisplayName("should NOT generate @Enumerated for boolean primitive")
        void shouldNotGenerateEnumeratedForBooleanPrimitive() {
            DomainType task = taskWithBooleanCompleted(DOMAIN_PKG);
            JpaEntityGenerator generator = generatorWithTypes(List.of(task));

            String code = generator.generateEntity(task);

            assertThat(code)
                    .as("Primitive boolean should NOT have @Enumerated annotation")
                    .doesNotContain("@Enumerated")
                    .contains("private boolean completed;");
        }

        @Test
        @DisplayName("should NOT generate @Enumerated for int primitive")
        void shouldNotGenerateEnumeratedForIntPrimitive() {
            DomainType item = itemWithIntQuantity(DOMAIN_PKG);
            JpaEntityGenerator generator = generatorWithTypes(List.of(item));

            String code = generator.generateEntity(item);

            // Note: int primitive is converted to Integer for JPA (nullable by default)
            assertThat(code)
                    .as("Primitive int should NOT have @Enumerated annotation")
                    .doesNotContain("@Enumerated")
                    .contains("private Integer quantity;");
        }

        @Test
        @DisplayName("should NOT generate @Enumerated for Integer wrapper")
        void shouldNotGenerateEnumeratedForIntegerWrapper() {
            DomainType item = itemWithIntegerQuantity(DOMAIN_PKG);
            JpaEntityGenerator generator = generatorWithTypes(List.of(item));

            String code = generator.generateEntity(item);

            assertThat(code)
                    .as("Integer wrapper should NOT have @Enumerated annotation")
                    .doesNotContain("@Enumerated")
                    .contains("private Integer quantity;");
        }
    }

    private static DomainType taskWithBooleanCompleted(String pkg) {
        List<DomainProperty> props = List.of(
                identityProperty("id", longType()),
                simpleProperty("name", stringType()),
                simpleProperty("completed", booleanPrimitiveType()));

        return new DomainType(
                pkg + ".Task",
                "Task",
                pkg,
                DomainKind.ENTITY,
                ConfidenceLevel.HIGH,
                JavaConstruct.CLASS,
                Optional.of(rawLongIdentity("id")),
                props,
                List.of(),
                List.of(),
                SourceRef.unknown());
    }

    private static DomainType itemWithIntQuantity(String pkg) {
        List<DomainProperty> props = List.of(
                identityProperty("id", longType()),
                simpleProperty("name", stringType()),
                simpleProperty("quantity", intPrimitiveType()));

        return new DomainType(
                pkg + ".Item",
                "Item",
                pkg,
                DomainKind.ENTITY,
                ConfidenceLevel.HIGH,
                JavaConstruct.CLASS,
                Optional.of(rawLongIdentity("id")),
                props,
                List.of(),
                List.of(),
                SourceRef.unknown());
    }

    private static DomainType itemWithIntegerQuantity(String pkg) {
        List<DomainProperty> props = List.of(
                identityProperty("id", longType()),
                simpleProperty("name", stringType()),
                simpleProperty("quantity", intType()));

        return new DomainType(
                pkg + ".Item",
                "Item",
                pkg,
                DomainKind.ENTITY,
                ConfidenceLevel.HIGH,
                JavaConstruct.CLASS,
                Optional.of(rawLongIdentity("id")),
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
