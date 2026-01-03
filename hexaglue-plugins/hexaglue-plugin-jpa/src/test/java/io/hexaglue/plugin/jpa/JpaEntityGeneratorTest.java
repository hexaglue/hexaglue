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

@DisplayName("JpaEntityGenerator")
class JpaEntityGeneratorTest {

    private static final String INFRA_PKG = "com.example.infrastructure.persistence";
    private static final String DOMAIN_PKG = "com.example.domain";

    private JpaEntityGenerator generator;
    private JpaConfig config;

    @BeforeEach
    void setUp() {
        config = JpaConfig.defaults();
        generator = new JpaEntityGenerator(INFRA_PKG, config, List.of());
    }

    @Nested
    @DisplayName("generateEntity()")
    class GenerateEntityTests {

        @Test
        @DisplayName("should generate package declaration")
        void shouldGeneratePackageDeclaration() {
            DomainType type = simpleAggregateRoot("Order", DOMAIN_PKG);

            String code = generator.generateEntity(type);

            assertThat(code).startsWith("package " + INFRA_PKG + ";");
        }

        @Test
        @DisplayName("should generate @Entity annotation")
        void shouldGenerateEntityAnnotation() {
            DomainType type = simpleAggregateRoot("Order", DOMAIN_PKG);

            String code = generator.generateEntity(type);

            assertThat(code).contains("@Entity");
        }

        @Test
        @DisplayName("should generate @Table annotation with snake_case name")
        void shouldGenerateTableAnnotation() {
            DomainType type = simpleAggregateRoot("OrderLine", DOMAIN_PKG);

            String code = generator.generateEntity(type);

            assertThat(code).contains("@Table(name = \"order_line\")");
        }

        @Test
        @DisplayName("should generate @Table with prefix when configured")
        void shouldGenerateTableWithPrefix() {
            JpaConfig customConfig = new JpaConfig(
                    "Entity", "JpaRepository", "Adapter", "Mapper", "app_", false, false, true, true, true);
            JpaEntityGenerator gen = new JpaEntityGenerator(INFRA_PKG, customConfig, List.of());
            DomainType type = simpleAggregateRoot("Order", DOMAIN_PKG);

            String code = gen.generateEntity(type);

            assertThat(code).contains("@Table(name = \"app_order\")");
        }

        @Test
        @DisplayName("should generate class with Entity suffix")
        void shouldGenerateClassWithEntitySuffix() {
            DomainType type = simpleAggregateRoot("Order", DOMAIN_PKG);

            String code = generator.generateEntity(type);

            assertThat(code).contains("public class OrderEntity {");
        }

        @Test
        @DisplayName("should generate @Id annotated field")
        void shouldGenerateIdAnnotatedField() {
            DomainType type = simpleAggregateRoot("Order", DOMAIN_PKG);

            String code = generator.generateEntity(type);

            assertThat(code).contains("@Id");
            assertThat(code).contains("private UUID id;");
        }

        @Test
        @DisplayName("should generate @GeneratedValue for AUTO strategy")
        void shouldGenerateGeneratedValueForAuto() {
            Identity autoId = new Identity(
                    "id",
                    TypeRef.of("java.lang.Long"),
                    TypeRef.of("java.lang.Long"),
                    IdentityStrategy.AUTO,
                    IdentityWrapperKind.NONE);

            DomainType type = new DomainType(
                    DOMAIN_PKG + ".Order",
                    "Order",
                    DOMAIN_PKG,
                    DomainKind.AGGREGATE_ROOT,
                    ConfidenceLevel.HIGH,
                    JavaConstruct.CLASS,
                    Optional.of(autoId),
                    List.of(identityProperty("id", longType())),
                    List.of(),
                    List.of(),
                    SourceRef.unknown());

            String code = generator.generateEntity(type);

            assertThat(code).contains("@GeneratedValue(strategy = GenerationType.AUTO)");
        }

        @Test
        @DisplayName("should generate @GeneratedValue for SEQUENCE strategy")
        void shouldGenerateGeneratedValueForSequence() {
            Identity seqId = sequenceIdentity("id");

            DomainType type = new DomainType(
                    DOMAIN_PKG + ".Order",
                    "Order",
                    DOMAIN_PKG,
                    DomainKind.AGGREGATE_ROOT,
                    ConfidenceLevel.HIGH,
                    JavaConstruct.CLASS,
                    Optional.of(seqId),
                    List.of(identityProperty("id", longType())),
                    List.of(),
                    List.of(),
                    SourceRef.unknown());

            String code = generator.generateEntity(type);

            assertThat(code).contains("@GeneratedValue(strategy = GenerationType.SEQUENCE)");
        }

        @Test
        @DisplayName("should NOT generate @GeneratedValue for ASSIGNED strategy")
        void shouldNotGenerateGeneratedValueForAssigned() {
            DomainType type = simpleAggregateRoot("Order", DOMAIN_PKG);

            String code = generator.generateEntity(type);

            assertThat(code).doesNotContain("@GeneratedValue");
        }

        @Test
        @DisplayName("should generate simple property fields")
        void shouldGenerateSimplePropertyFields() {
            DomainType type = simpleAggregateRoot("Order", DOMAIN_PKG);

            String code = generator.generateEntity(type);

            assertThat(code).contains("private String name;");
        }

        @Test
        @DisplayName("should generate protected default constructor")
        void shouldGenerateProtectedDefaultConstructor() {
            DomainType type = simpleAggregateRoot("Order", DOMAIN_PKG);

            String code = generator.generateEntity(type);

            assertThat(code).contains("protected OrderEntity() {");
        }

        @Test
        @DisplayName("should generate getters and setters")
        void shouldGenerateGettersAndSetters() {
            DomainType type = simpleAggregateRoot("Order", DOMAIN_PKG);

            String code = generator.generateEntity(type);

            assertThat(code).contains("public UUID getId()");
            assertThat(code).contains("public void setId(UUID id)");
            assertThat(code).contains("public String getName()");
            assertThat(code).contains("public void setName(String name)");
        }

        @Test
        @DisplayName("should generate javadoc linking to domain class")
        void shouldGenerateJavadocLinkingToDomainClass() {
            DomainType type = simpleAggregateRoot("Order", DOMAIN_PKG);

            String code = generator.generateEntity(type);

            assertThat(code).contains("JPA entity for {@link " + DOMAIN_PKG + ".Order}");
            assertThat(code).contains("Generated by HexaGlue JPA Plugin");
        }
    }

    @Nested
    @DisplayName("generateEntity() with relations")
    class GenerateEntityWithRelationsTests {

        @Test
        @DisplayName("should generate @OneToMany for entity collections")
        void shouldGenerateOneToManyForEntityCollections() {
            DomainType type = aggregateRootWithRelations(
                    "Order", DOMAIN_PKG, List.of(oneToManyRelation("lines", DOMAIN_PKG + ".OrderLine")));

            String code = generator.generateEntity(type);

            assertThat(code).contains("@OneToMany");
            assertThat(code).contains("cascade = CascadeType.ALL");
            assertThat(code).contains("orphanRemoval = true");
            assertThat(code).contains("private List<OrderLineEntity> lines = new ArrayList<>();");
        }

        @Test
        @DisplayName("should generate @Embedded for embedded value objects")
        void shouldGenerateEmbeddedForValueObjects() {
            DomainType type = aggregateRootWithRelations(
                    "Order", DOMAIN_PKG, List.of(embeddedRelation("shippingAddress", DOMAIN_PKG + ".Address")));

            String code = generator.generateEntity(type);

            assertThat(code).contains("@Embedded");
            assertThat(code).contains("private AddressEmbeddable shippingAddress;");
        }

        @Test
        @DisplayName("should generate @ElementCollection for value object collections")
        void shouldGenerateElementCollectionForVOCollections() {
            DomainType type = aggregateRootWithRelations(
                    "Order", DOMAIN_PKG, List.of(elementCollectionRelation("tags", DOMAIN_PKG + ".Tag")));

            String code = generator.generateEntity(type);

            assertThat(code).contains("@ElementCollection");
            assertThat(code).contains("@CollectionTable(name = \"tags\")");
            assertThat(code).contains("private List<TagEmbeddable> tags = new ArrayList<>();");
        }

        @Test
        @DisplayName("should generate @ManyToOne for aggregate references")
        void shouldGenerateManyToOneForAggregateReferences() {
            DomainType type = aggregateRootWithRelations(
                    "LineItem", DOMAIN_PKG, List.of(manyToOneRelation("product", DOMAIN_PKG + ".Product")));

            String code = generator.generateEntity(type);

            assertThat(code).contains("@ManyToOne");
            assertThat(code).contains("private ProductEntity product;");
        }

        @Test
        @DisplayName("should import required JPA relation annotations")
        void shouldImportRequiredJpaRelationAnnotations() {
            DomainType type = aggregateRootWithRelations(
                    "Order",
                    DOMAIN_PKG,
                    List.of(
                            oneToManyRelation("lines", DOMAIN_PKG + ".OrderLine"),
                            embeddedRelation("address", DOMAIN_PKG + ".Address")));

            String code = generator.generateEntity(type);

            assertThat(code).contains("import jakarta.persistence.OneToMany;");
            assertThat(code).contains("import jakarta.persistence.CascadeType;");
            assertThat(code).contains("import jakarta.persistence.Embedded;");
            assertThat(code).contains("import java.util.List;");
            assertThat(code).contains("import java.util.ArrayList;");
        }
    }

    @Nested
    @DisplayName("generateEntity() with auditing")
    class GenerateEntityWithAuditingTests {

        @Test
        @DisplayName("should generate auditing fields when enabled")
        void shouldGenerateAuditingFieldsWhenEnabled() {
            JpaConfig auditConfig =
                    new JpaConfig("Entity", "JpaRepository", "Adapter", "Mapper", "", true, false, true, true, true);
            JpaEntityGenerator gen = new JpaEntityGenerator(INFRA_PKG, auditConfig, List.of());
            DomainType type = simpleAggregateRoot("Order", DOMAIN_PKG);

            String code = gen.generateEntity(type);

            assertThat(code).contains("@CreatedDate");
            assertThat(code).contains("private Instant createdAt;");
            assertThat(code).contains("@LastModifiedDate");
            assertThat(code).contains("private Instant updatedAt;");
            assertThat(code).contains("@EntityListeners(AuditingEntityListener.class)");
        }

        @Test
        @DisplayName("should generate auditing accessors when enabled")
        void shouldGenerateAuditingAccessorsWhenEnabled() {
            JpaConfig auditConfig =
                    new JpaConfig("Entity", "JpaRepository", "Adapter", "Mapper", "", true, false, true, true, true);
            JpaEntityGenerator gen = new JpaEntityGenerator(INFRA_PKG, auditConfig, List.of());
            DomainType type = simpleAggregateRoot("Order", DOMAIN_PKG);

            String code = gen.generateEntity(type);

            assertThat(code).contains("public Instant getCreatedAt()");
            assertThat(code).contains("public Instant getUpdatedAt()");
        }

        @Test
        @DisplayName("should NOT generate auditing fields when disabled")
        void shouldNotGenerateAuditingFieldsWhenDisabled() {
            DomainType type = simpleAggregateRoot("Order", DOMAIN_PKG);

            String code = generator.generateEntity(type);

            assertThat(code).doesNotContain("@CreatedDate");
            assertThat(code).doesNotContain("@LastModifiedDate");
            assertThat(code).doesNotContain("@EntityListeners");
        }
    }

    @Nested
    @DisplayName("generateEntity() with optimistic locking")
    class GenerateEntityWithOptimisticLockingTests {

        @Test
        @DisplayName("should generate @Version field when enabled")
        void shouldGenerateVersionFieldWhenEnabled() {
            JpaConfig versionConfig =
                    new JpaConfig("Entity", "JpaRepository", "Adapter", "Mapper", "", false, true, true, true, true);
            JpaEntityGenerator gen = new JpaEntityGenerator(INFRA_PKG, versionConfig, List.of());
            DomainType type = simpleAggregateRoot("Order", DOMAIN_PKG);

            String code = gen.generateEntity(type);

            assertThat(code).contains("@Version");
            assertThat(code).contains("private Long version;");
            assertThat(code).contains("public Long getVersion()");
        }

        @Test
        @DisplayName("should NOT generate @Version field when disabled")
        void shouldNotGenerateVersionFieldWhenDisabled() {
            DomainType type = simpleAggregateRoot("Order", DOMAIN_PKG);

            String code = generator.generateEntity(type);

            assertThat(code).doesNotContain("@Version");
            assertThat(code).doesNotContain("private Long version;");
        }
    }

    @Nested
    @DisplayName("generateEmbeddable()")
    class GenerateEmbeddableTests {

        @Test
        @DisplayName("should generate @Embeddable annotation")
        void shouldGenerateEmbeddableAnnotation() {
            DomainType vo = simpleValueObject("Address", DOMAIN_PKG);

            String code = generator.generateEmbeddable(vo);

            assertThat(code).contains("@Embeddable");
        }

        @Test
        @DisplayName("should generate class with Embeddable suffix")
        void shouldGenerateClassWithEmbeddableSuffix() {
            DomainType vo = simpleValueObject("Address", DOMAIN_PKG);

            String code = generator.generateEmbeddable(vo);

            assertThat(code).contains("public class AddressEmbeddable {");
        }

        @Test
        @DisplayName("should generate protected default constructor")
        void shouldGenerateProtectedDefaultConstructorForEmbeddable() {
            DomainType vo = simpleValueObject("Address", DOMAIN_PKG);

            String code = generator.generateEmbeddable(vo);

            assertThat(code).contains("protected AddressEmbeddable() {");
        }

        @Test
        @DisplayName("should generate all-args constructor")
        void shouldGenerateAllArgsConstructor() {
            DomainType vo = moneyValueObject(DOMAIN_PKG);

            String code = generator.generateEmbeddable(vo);

            assertThat(code).contains("public MoneyEmbeddable(BigDecimal amount, String currency)");
        }

        @Test
        @DisplayName("should generate getters for all properties")
        void shouldGenerateGettersForAllProperties() {
            DomainType vo = moneyValueObject(DOMAIN_PKG);

            String code = generator.generateEmbeddable(vo);

            assertThat(code).contains("public BigDecimal getAmount()");
            assertThat(code).contains("public String getCurrency()");
        }

        @Test
        @DisplayName("should NOT generate setters for value objects (immutable)")
        void shouldNotGenerateSettersForValueObjects() {
            DomainType vo = moneyValueObject(DOMAIN_PKG);

            String code = generator.generateEmbeddable(vo);

            assertThat(code).doesNotContain("public void setAmount");
            assertThat(code).doesNotContain("public void setCurrency");
        }
    }

    @Nested
    @DisplayName("Type mapping")
    class TypeMappingTests {

        @Test
        @DisplayName("should map java.util.UUID to UUID")
        void shouldMapUuidToUuid() {
            DomainType type = simpleAggregateRoot("Order", DOMAIN_PKG);

            String code = generator.generateEntity(type);

            assertThat(code).contains("private UUID id;");
            assertThat(code).contains("import java.util.UUID;");
        }

        @Test
        @DisplayName("should map java.time.Instant to Instant")
        void shouldMapInstantToInstant() {
            DomainType type = customerAggregate(DOMAIN_PKG);

            String code = generator.generateEntity(type);

            assertThat(code).contains("private Instant createdAt;");
        }

        @Test
        @DisplayName("should map java.math.BigDecimal to BigDecimal")
        void shouldMapBigDecimalToBigDecimal() {
            DomainType vo = moneyValueObject(DOMAIN_PKG);

            String code = generator.generateEmbeddable(vo);

            assertThat(code).contains("private BigDecimal amount;");
        }
    }

    @Nested
    @DisplayName("Custom suffix configuration")
    class CustomSuffixTests {

        @Test
        @DisplayName("should use custom entity suffix")
        void shouldUseCustomEntitySuffix() {
            JpaConfig customConfig =
                    new JpaConfig("Jpa", "JpaRepository", "Adapter", "Mapper", "", false, false, true, true, true);
            JpaEntityGenerator gen = new JpaEntityGenerator(INFRA_PKG, customConfig, List.of());
            DomainType type = simpleAggregateRoot("Order", DOMAIN_PKG);

            String code = gen.generateEntity(type);

            assertThat(code).contains("public class OrderJpa {");
        }
    }
}
