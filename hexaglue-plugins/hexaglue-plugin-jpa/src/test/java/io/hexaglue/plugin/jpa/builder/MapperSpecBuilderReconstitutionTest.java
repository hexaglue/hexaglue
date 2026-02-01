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

package io.hexaglue.plugin.jpa.builder;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.ClassificationTrace;
import io.hexaglue.arch.ElementKind;
import io.hexaglue.arch.ProjectContext;
import io.hexaglue.arch.model.AggregateRoot;
import io.hexaglue.arch.model.Constructor;
import io.hexaglue.arch.model.Entity;
import io.hexaglue.arch.model.Field;
import io.hexaglue.arch.model.FieldRole;
import io.hexaglue.arch.model.Identifier;
import io.hexaglue.arch.model.Method;
import io.hexaglue.arch.model.MethodRole;
import io.hexaglue.arch.model.Parameter;
import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.TypeNature;
import io.hexaglue.arch.model.TypeRegistry;
import io.hexaglue.arch.model.TypeStructure;
import io.hexaglue.arch.model.ValueObject;
import io.hexaglue.arch.model.index.DomainIndex;
import io.hexaglue.plugin.jpa.JpaConfig;
import io.hexaglue.plugin.jpa.model.MapperSpec;
import io.hexaglue.plugin.jpa.model.MapperSpec.ConversionKind;
import io.hexaglue.plugin.jpa.model.MapperSpec.ReconstitutionSpec;
import io.hexaglue.syntax.Modifier;
import io.hexaglue.syntax.TypeRef;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link MapperSpecBuilder} reconstitution detection logic.
 *
 * <p>These tests validate the detection of domain factory methods (e.g., {@code reconstitute()})
 * and the generation of {@link ReconstitutionSpec} for rich domain objects that cannot be
 * instantiated via MapStruct's default no-arg constructor + setters strategy.
 *
 * @since 5.0.0
 */
@DisplayName("MapperSpecBuilder - Reconstitution Detection")
class MapperSpecBuilderReconstitutionTest {

    private static final String DOMAIN_PKG = "com.example.domain";
    private static final String INFRA_PKG = "com.example.infrastructure.jpa";

    private JpaConfig config;

    @BeforeEach
    void setUp() {
        config = new JpaConfig(
                "Entity", "Embeddable", "JpaRepository", "Adapter", "Mapper", "", false, false, true, true, true, true);
    }

    private ClassificationTrace highConfidence(ElementKind kind) {
        return ClassificationTrace.highConfidence(kind, "test", "Test classification");
    }

    @Nested
    @DisplayName("Factory Method Detection")
    class FactoryMethodDetection {

        @Test
        @DisplayName("should detect reconstitute() method")
        void should_detectReconstituteMethod() {
            // Given: A domain class with private constructor and reconstitute() factory
            AggregateRoot aggregate = createAggregateWithReconstitute(
                    "reconstitute",
                    List.of(
                            Parameter.of("id", TypeRef.of(DOMAIN_PKG + ".CustomerId")),
                            Parameter.of("firstName", TypeRef.of("java.lang.String"))));
            ArchitecturalModel model = buildModelWith(aggregate, createCustomerIdIdentifier());

            // When
            MapperSpec spec = MapperSpecBuilder.builder()
                    .aggregateRoot(aggregate)
                    .model(model)
                    .config(config)
                    .infrastructurePackage(INFRA_PKG)
                    .build();

            // Then
            assertThat(spec.reconstitutionSpec()).isNotNull();
            assertThat(spec.reconstitutionSpec().factoryMethodName()).isEqualTo("reconstitute");
        }

        @Test
        @DisplayName("should prefer reconstitute over other factory methods")
        void should_preferReconstitute_overOtherFactoryMethods() {
            // Given: A domain class with both reconstitute() and create() factory methods
            Method reconstituteMethod = createStaticFactoryMethod(
                    "reconstitute",
                    TypeRef.of(DOMAIN_PKG + ".Customer"),
                    List.of(
                            Parameter.of("id", TypeRef.of(DOMAIN_PKG + ".CustomerId")),
                            Parameter.of("firstName", TypeRef.of("java.lang.String"))));
            Method createMethod = createStaticFactoryMethod(
                    "create",
                    TypeRef.of(DOMAIN_PKG + ".Customer"),
                    List.of(Parameter.of("firstName", TypeRef.of("java.lang.String"))));

            AggregateRoot aggregate = createAggregateWithMethods(List.of(reconstituteMethod, createMethod), List.of());
            ArchitecturalModel model = buildModelWith(aggregate, createCustomerIdIdentifier());

            // When
            MapperSpec spec = MapperSpecBuilder.builder()
                    .aggregateRoot(aggregate)
                    .model(model)
                    .config(config)
                    .infrastructurePackage(INFRA_PKG)
                    .build();

            // Then
            assertThat(spec.reconstitutionSpec()).isNotNull();
            assertThat(spec.reconstitutionSpec().factoryMethodName()).isEqualTo("reconstitute");
        }

        @Test
        @DisplayName("should return null when public no-arg constructor and setters exist")
        void should_returnNull_whenPublicNoArgCtorAndSetters() {
            // Given: A domain class with public no-arg constructor and setters (MapStruct-compatible)
            Constructor publicNoArgCtor = new Constructor(
                    List.of(), Set.of(Modifier.PUBLIC), List.of(), Optional.empty(), List.of(), Optional.empty());
            Method setter = new Method(
                    "setFirstName",
                    TypeRef.of("void"),
                    List.of(Parameter.of("firstName", TypeRef.of("java.lang.String"))),
                    Set.of(Modifier.PUBLIC),
                    List.of(),
                    Optional.empty(),
                    List.of(),
                    Set.of(MethodRole.SETTER),
                    OptionalInt.empty(),
                    Optional.empty());

            AggregateRoot aggregate = createAggregateWithMethods(List.of(setter), List.of(publicNoArgCtor));
            ArchitecturalModel model = buildModelWith(aggregate);

            // When
            MapperSpec spec = MapperSpecBuilder.builder()
                    .aggregateRoot(aggregate)
                    .model(model)
                    .config(config)
                    .infrastructurePackage(INFRA_PKG)
                    .build();

            // Then: MapStruct can handle this natively
            assertThat(spec.reconstitutionSpec()).isNull();
        }

        @Test
        @DisplayName("should return null when no factory methods exist")
        void should_returnNull_whenNoFactoryMethods() {
            // Given: A domain class with no factory methods and no public no-arg ctor
            Constructor privateCtor = new Constructor(
                    List.of(Parameter.of("id", TypeRef.of(DOMAIN_PKG + ".CustomerId"))),
                    Set.of(Modifier.PRIVATE),
                    List.of(),
                    Optional.empty(),
                    List.of(),
                    Optional.empty());

            Method getter = new Method(
                    "getFirstName",
                    TypeRef.of("java.lang.String"),
                    List.of(),
                    Set.of(Modifier.PUBLIC),
                    List.of(),
                    Optional.empty(),
                    List.of(),
                    Set.of(MethodRole.GETTER),
                    OptionalInt.empty(),
                    Optional.empty());

            AggregateRoot aggregate = createAggregateWithMethods(List.of(getter), List.of(privateCtor));
            ArchitecturalModel model = buildModelWith(aggregate);

            // When
            MapperSpec spec = MapperSpecBuilder.builder()
                    .aggregateRoot(aggregate)
                    .model(model)
                    .config(config)
                    .infrastructurePackage(INFRA_PKG)
                    .build();

            // Then: No factory method found, fallback to abstract
            assertThat(spec.reconstitutionSpec()).isNull();
        }

        @Test
        @DisplayName("should select method with most params when no reconstitute name")
        void should_selectMethodWithMostParams_whenNoReconstitute() {
            // Given: Two factory methods, neither named "reconstitute"
            Method fromMethod = createStaticFactoryMethod(
                    "from",
                    TypeRef.of(DOMAIN_PKG + ".Customer"),
                    List.of(Parameter.of("id", TypeRef.of(DOMAIN_PKG + ".CustomerId"))));
            Method restoreMethod = createStaticFactoryMethod(
                    "restore",
                    TypeRef.of(DOMAIN_PKG + ".Customer"),
                    List.of(
                            Parameter.of("id", TypeRef.of(DOMAIN_PKG + ".CustomerId")),
                            Parameter.of("firstName", TypeRef.of("java.lang.String")),
                            Parameter.of("lastName", TypeRef.of("java.lang.String"))));

            AggregateRoot aggregate = createAggregateWithMethods(List.of(fromMethod, restoreMethod), List.of());
            ArchitecturalModel model = buildModelWith(aggregate, createCustomerIdIdentifier());

            // When
            MapperSpec spec = MapperSpecBuilder.builder()
                    .aggregateRoot(aggregate)
                    .model(model)
                    .config(config)
                    .infrastructurePackage(INFRA_PKG)
                    .build();

            // Then: "restore" has more params, so it's selected
            assertThat(spec.reconstitutionSpec()).isNotNull();
            assertThat(spec.reconstitutionSpec().factoryMethodName()).isEqualTo("restore");
        }
    }

    @Nested
    @DisplayName("Parameter Matching")
    class ParameterMatching {

        @Test
        @DisplayName("should match param to entity field by name")
        void should_matchParamToEntityFieldByName() {
            // Given
            AggregateRoot aggregate = createAggregateWithReconstitute(
                    "reconstitute",
                    List.of(
                            Parameter.of("id", TypeRef.of(DOMAIN_PKG + ".CustomerId")),
                            Parameter.of("firstName", TypeRef.of("java.lang.String"))));
            ArchitecturalModel model = buildModelWith(aggregate, createCustomerIdIdentifier());

            // When
            MapperSpec spec = MapperSpecBuilder.builder()
                    .aggregateRoot(aggregate)
                    .model(model)
                    .config(config)
                    .infrastructurePackage(INFRA_PKG)
                    .build();

            // Then
            assertThat(spec.reconstitutionSpec()).isNotNull();
            assertThat(spec.reconstitutionSpec().parameters()).hasSize(2);
            assertThat(spec.reconstitutionSpec().parameters().get(1).entityFieldName())
                    .isEqualTo("firstName");
        }

        @Test
        @DisplayName("should map identity param to id field")
        void should_mapIdentityParamToIdField() {
            // Given: reconstitute(CustomerId id, ...) - the identity type param maps to JPA "id" field
            AggregateRoot aggregate = createAggregateWithReconstitute(
                    "reconstitute",
                    List.of(
                            Parameter.of("id", TypeRef.of(DOMAIN_PKG + ".CustomerId")),
                            Parameter.of("firstName", TypeRef.of("java.lang.String"))));
            ArchitecturalModel model = buildModelWith(aggregate, createCustomerIdIdentifier());

            // When
            MapperSpec spec = MapperSpecBuilder.builder()
                    .aggregateRoot(aggregate)
                    .model(model)
                    .config(config)
                    .infrastructurePackage(INFRA_PKG)
                    .build();

            // Then: The identity-type parameter maps to "id" (JPA entity field)
            assertThat(spec.reconstitutionSpec()).isNotNull();
            assertThat(spec.reconstitutionSpec().parameters().get(0).entityFieldName())
                    .isEqualTo("id");
        }

        @Test
        @DisplayName("should detect WRAPPED_IDENTITY conversion for identity types")
        void should_detectWrappedIdentityConversion() {
            // Given
            AggregateRoot aggregate = createAggregateWithReconstitute(
                    "reconstitute", List.of(Parameter.of("id", TypeRef.of(DOMAIN_PKG + ".CustomerId"))));
            ArchitecturalModel model = buildModelWith(aggregate, createCustomerIdIdentifier());

            // When
            MapperSpec spec = MapperSpecBuilder.builder()
                    .aggregateRoot(aggregate)
                    .model(model)
                    .config(config)
                    .infrastructurePackage(INFRA_PKG)
                    .build();

            // Then
            assertThat(spec.reconstitutionSpec()).isNotNull();
            assertThat(spec.reconstitutionSpec().parameters().get(0).conversionKind())
                    .isEqualTo(ConversionKind.WRAPPED_IDENTITY);
        }

        @Test
        @DisplayName("should detect VALUE_OBJECT conversion for single-value VOs")
        void should_detectValueObjectConversion() {
            // Given: An aggregate with an email field and a reconstitute factory that includes it
            ValueObject emailVo = createSingleValueVO("Email", "java.lang.String");
            AggregateRoot aggregate = createAggregateWithEmailField(
                    "reconstitute",
                    List.of(
                            Parameter.of("id", TypeRef.of(DOMAIN_PKG + ".CustomerId")),
                            Parameter.of("email", TypeRef.of(DOMAIN_PKG + ".Email"))));
            ArchitecturalModel model = buildModelWith(aggregate, createCustomerIdIdentifier(), emailVo);

            // When
            MapperSpec spec = MapperSpecBuilder.builder()
                    .aggregateRoot(aggregate)
                    .model(model)
                    .config(config)
                    .infrastructurePackage(INFRA_PKG)
                    .build();

            // Then
            assertThat(spec.reconstitutionSpec()).isNotNull();
            assertThat(spec.reconstitutionSpec().parameters()).anySatisfy(param -> {
                assertThat(param.parameterName()).isEqualTo("email");
                assertThat(param.conversionKind()).isEqualTo(ConversionKind.VALUE_OBJECT);
            });
        }

        @Test
        @DisplayName("should detect VALUE_OBJECT conversion for foreign key identity types")
        void should_detectValueObjectConversion_forForeignKeyIdentityTypes() {
            // Given: An Order aggregate with CustomerId foreign key
            Identifier orderIdIdentifier = createIdentifier("OrderId", "java.util.UUID");
            Identifier customerIdIdentifier = createIdentifier("CustomerId", "java.util.UUID");

            AggregateRoot aggregate = createOrderAggregateWithForeignKey(
                    "reconstitute",
                    List.of(
                            Parameter.of("id", TypeRef.of(DOMAIN_PKG + ".OrderId")),
                            Parameter.of("customerId", TypeRef.of(DOMAIN_PKG + ".CustomerId"))));

            ArchitecturalModel model = buildModelWith(aggregate, orderIdIdentifier, customerIdIdentifier);

            // When
            MapperSpec spec = MapperSpecBuilder.builder()
                    .aggregateRoot(aggregate)
                    .model(model)
                    .config(config)
                    .infrastructurePackage(INFRA_PKG)
                    .build();

            // Then: Own identity → WRAPPED_IDENTITY, foreign key → VALUE_OBJECT
            assertThat(spec.reconstitutionSpec()).isNotNull();
            assertThat(spec.reconstitutionSpec().parameters().get(0).conversionKind())
                    .isEqualTo(ConversionKind.WRAPPED_IDENTITY);
            assertThat(spec.reconstitutionSpec().parameters().get(1).conversionKind())
                    .isEqualTo(ConversionKind.VALUE_OBJECT);
        }

        @Test
        @DisplayName("should detect EMBEDDED_VALUE_OBJECT conversion for multi-value VOs")
        void should_detectEmbeddedValueObjectConversion_forMultiValueVOs() {
            // Given: An aggregate with a Money field (multi-value VO: amount + currency)
            ValueObject moneyVo = createMultiValueVO(
                    "Money",
                    List.of(
                            Field.of("amount", TypeRef.of("java.math.BigDecimal")),
                            Field.of("currency", TypeRef.of("java.lang.String"))));
            AggregateRoot aggregate = createAggregateWithMoneyField(
                    "reconstitute",
                    List.of(
                            Parameter.of("id", TypeRef.of(DOMAIN_PKG + ".CustomerId")),
                            Parameter.of("amount", TypeRef.of(DOMAIN_PKG + ".Money"))));
            ArchitecturalModel model = buildModelWith(aggregate, createCustomerIdIdentifier(), moneyVo);

            Map<String, String> embeddableMap = Map.of(DOMAIN_PKG + ".Money", INFRA_PKG + ".MoneyEmbeddable");

            // When
            MapperSpec spec = MapperSpecBuilder.builder()
                    .aggregateRoot(aggregate)
                    .model(model)
                    .config(config)
                    .infrastructurePackage(INFRA_PKG)
                    .embeddableMapping(embeddableMap)
                    .build();

            // Then: Multi-value VO in embeddableMapping → EMBEDDED_VALUE_OBJECT
            assertThat(spec.reconstitutionSpec()).isNotNull();
            assertThat(spec.reconstitutionSpec().parameters()).anySatisfy(param -> {
                assertThat(param.parameterName()).isEqualTo("amount");
                assertThat(param.conversionKind()).isEqualTo(ConversionKind.EMBEDDED_VALUE_OBJECT);
            });
        }

        @Test
        @DisplayName("should detect AUDIT_TEMPORAL conversion when auditing enabled and LocalDateTime params")
        void should_detectAuditTemporalConversion_whenAuditingEnabled_andLocalDateTimeParams() {
            // Given: An aggregate with createdAt/updatedAt LocalDateTime params and auditing enabled
            JpaConfig auditConfig = new JpaConfig(
                    "Entity",
                    "Embeddable",
                    "JpaRepository",
                    "Adapter",
                    "Mapper",
                    "",
                    true,
                    false,
                    true,
                    true,
                    true,
                    true);

            AggregateRoot aggregate = createAggregateWithAuditFields(
                    "reconstitute",
                    List.of(
                            Parameter.of("id", TypeRef.of(DOMAIN_PKG + ".CustomerId")),
                            Parameter.of("firstName", TypeRef.of("java.lang.String")),
                            Parameter.of("createdAt", TypeRef.of("java.time.LocalDateTime")),
                            Parameter.of("updatedAt", TypeRef.of("java.time.LocalDateTime"))));
            ArchitecturalModel model = buildModelWith(aggregate, createCustomerIdIdentifier());

            // When
            MapperSpec spec = MapperSpecBuilder.builder()
                    .aggregateRoot(aggregate)
                    .model(model)
                    .config(auditConfig)
                    .infrastructurePackage(INFRA_PKG)
                    .build();

            // Then: createdAt and updatedAt should be AUDIT_TEMPORAL
            assertThat(spec.reconstitutionSpec()).isNotNull();
            assertThat(spec.reconstitutionSpec().parameters()).anySatisfy(param -> {
                assertThat(param.parameterName()).isEqualTo("createdAt");
                assertThat(param.conversionKind()).isEqualTo(ConversionKind.AUDIT_TEMPORAL);
            });
            assertThat(spec.reconstitutionSpec().parameters()).anySatisfy(param -> {
                assertThat(param.parameterName()).isEqualTo("updatedAt");
                assertThat(param.conversionKind()).isEqualTo(ConversionKind.AUDIT_TEMPORAL);
            });
        }

        @Test
        @DisplayName("should detect DIRECT conversion when auditing disabled and LocalDateTime params")
        void should_detectDirectConversion_whenAuditingDisabled_andLocalDateTimeParams() {
            // Given: Same aggregate but auditing disabled
            JpaConfig noAuditConfig = new JpaConfig(
                    "Entity",
                    "Embeddable",
                    "JpaRepository",
                    "Adapter",
                    "Mapper",
                    "",
                    false,
                    false,
                    true,
                    true,
                    true,
                    true);

            AggregateRoot aggregate = createAggregateWithAuditFields(
                    "reconstitute",
                    List.of(
                            Parameter.of("id", TypeRef.of(DOMAIN_PKG + ".CustomerId")),
                            Parameter.of("firstName", TypeRef.of("java.lang.String")),
                            Parameter.of("createdAt", TypeRef.of("java.time.LocalDateTime")),
                            Parameter.of("updatedAt", TypeRef.of("java.time.LocalDateTime"))));
            ArchitecturalModel model = buildModelWith(aggregate, createCustomerIdIdentifier());

            // When
            MapperSpec spec = MapperSpecBuilder.builder()
                    .aggregateRoot(aggregate)
                    .model(model)
                    .config(noAuditConfig)
                    .infrastructurePackage(INFRA_PKG)
                    .build();

            // Then: createdAt and updatedAt should be DIRECT (no audit conversion needed)
            assertThat(spec.reconstitutionSpec()).isNotNull();
            assertThat(spec.reconstitutionSpec().parameters()).anySatisfy(param -> {
                assertThat(param.parameterName()).isEqualTo("createdAt");
                assertThat(param.conversionKind()).isEqualTo(ConversionKind.DIRECT);
            });
            assertThat(spec.reconstitutionSpec().parameters()).anySatisfy(param -> {
                assertThat(param.parameterName()).isEqualTo("updatedAt");
                assertThat(param.conversionKind()).isEqualTo(ConversionKind.DIRECT);
            });
        }

        @Test
        @DisplayName("should detect DIRECT conversion for primitive types")
        void should_detectDirectConversion_forPrimitiveTypes() {
            // Given
            AggregateRoot aggregate = createAggregateWithReconstitute(
                    "reconstitute",
                    List.of(
                            Parameter.of("id", TypeRef.of(DOMAIN_PKG + ".CustomerId")),
                            Parameter.of("firstName", TypeRef.of("java.lang.String"))));
            ArchitecturalModel model = buildModelWith(aggregate, createCustomerIdIdentifier());

            // When
            MapperSpec spec = MapperSpecBuilder.builder()
                    .aggregateRoot(aggregate)
                    .model(model)
                    .config(config)
                    .infrastructurePackage(INFRA_PKG)
                    .build();

            // Then
            assertThat(spec.reconstitutionSpec()).isNotNull();
            assertThat(spec.reconstitutionSpec().parameters()).anySatisfy(param -> {
                assertThat(param.parameterName()).isEqualTo("firstName");
                assertThat(param.conversionKind()).isEqualTo(ConversionKind.DIRECT);
            });
        }
    }

    @Nested
    @DisplayName("Child Entity Detection")
    class ChildEntityDetection {

        @Test
        @DisplayName("should detect child entity conversion for aggregate with collection field")
        void should_detectChildEntityConversion_forAggregateWithCollectionField() {
            // Given: An Order aggregate with a lines collection field of type List<OrderLine>
            Entity orderLineEntity = createOrderLineEntity();
            AggregateRoot aggregate = createOrderAggregateWithLines(
                    "reconstitute",
                    List.of(
                            Parameter.of("id", TypeRef.of(DOMAIN_PKG + ".OrderId")),
                            Parameter.of("orderNumber", TypeRef.of("java.lang.String")),
                            Parameter.of(
                                    "lines",
                                    TypeRef.parameterized(
                                            "java.util.List", List.of(TypeRef.of(DOMAIN_PKG + ".OrderLine"))))));

            Identifier orderIdIdentifier = createIdentifier("OrderId", "java.util.UUID");
            ArchitecturalModel model = buildModelWith(aggregate, orderIdIdentifier, orderLineEntity);

            // When
            MapperSpec spec = MapperSpecBuilder.builder()
                    .aggregateRoot(aggregate)
                    .model(model)
                    .config(config)
                    .infrastructurePackage(INFRA_PKG)
                    .childEntityFqns(Set.of(DOMAIN_PKG + ".OrderLine"))
                    .entityMapping(Map.of(DOMAIN_PKG + ".OrderLine", INFRA_PKG + ".OrderLineJpaEntity"))
                    .build();

            // Then
            assertThat(spec.childEntityConversions()).hasSize(1);
            assertThat(spec.childEntityConversions().get(0).childDomainSimpleName())
                    .isEqualTo("OrderLine");
        }

        @Test
        @DisplayName("should detect child entity collection conversion kind in reconstitution params")
        void should_detectChildEntityCollectionConversionKind_inReconstitutionParams() {
            // Given: Same aggregate setup with reconstitute() factory method
            Entity orderLineEntity = createOrderLineEntity();
            AggregateRoot aggregate = createOrderAggregateWithLines(
                    "reconstitute",
                    List.of(
                            Parameter.of("id", TypeRef.of(DOMAIN_PKG + ".OrderId")),
                            Parameter.of("orderNumber", TypeRef.of("java.lang.String")),
                            Parameter.of(
                                    "lines",
                                    TypeRef.parameterized(
                                            "java.util.List", List.of(TypeRef.of(DOMAIN_PKG + ".OrderLine"))))));

            Identifier orderIdIdentifier = createIdentifier("OrderId", "java.util.UUID");
            ArchitecturalModel model = buildModelWith(aggregate, orderIdIdentifier, orderLineEntity);

            // When
            MapperSpec spec = MapperSpecBuilder.builder()
                    .aggregateRoot(aggregate)
                    .model(model)
                    .config(config)
                    .infrastructurePackage(INFRA_PKG)
                    .childEntityFqns(Set.of(DOMAIN_PKG + ".OrderLine"))
                    .entityMapping(Map.of(DOMAIN_PKG + ".OrderLine", INFRA_PKG + ".OrderLineJpaEntity"))
                    .build();

            // Then: Verify that reconstitutionSpec parameters contain CHILD_ENTITY_COLLECTION for "lines"
            assertThat(spec.reconstitutionSpec()).isNotNull();
            assertThat(spec.reconstitutionSpec().parameters()).anySatisfy(param -> {
                assertThat(param.parameterName()).isEqualTo("lines");
                assertThat(param.conversionKind()).isEqualTo(ConversionKind.CHILD_ENTITY_COLLECTION);
            });
        }

        @Test
        @DisplayName("should not detect child entity conversion when no child entity fqns")
        void should_notDetectChildEntityConversion_whenNoChildEntityFqns() {
            // Given: Same aggregate but with empty childEntityFqns
            Entity orderLineEntity = createOrderLineEntity();
            AggregateRoot aggregate = createOrderAggregateWithLines(
                    "reconstitute",
                    List.of(
                            Parameter.of("id", TypeRef.of(DOMAIN_PKG + ".OrderId")),
                            Parameter.of("orderNumber", TypeRef.of("java.lang.String")),
                            Parameter.of(
                                    "lines",
                                    TypeRef.parameterized(
                                            "java.util.List", List.of(TypeRef.of(DOMAIN_PKG + ".OrderLine"))))));

            Identifier orderIdIdentifier = createIdentifier("OrderId", "java.util.UUID");
            ArchitecturalModel model = buildModelWith(aggregate, orderIdIdentifier, orderLineEntity);

            // When
            MapperSpec spec = MapperSpecBuilder.builder()
                    .aggregateRoot(aggregate)
                    .model(model)
                    .config(config)
                    .infrastructurePackage(INFRA_PKG)
                    .build();

            // Then
            assertThat(spec.childEntityConversions()).isEmpty();
        }

        @Test
        @DisplayName("should detect constructor-based reconstitution for child entity without factory method")
        void should_detectConstructorBasedReconstitution_forChildEntity() {
            // Given: An OrderLine entity with constructors but NO static factory method
            Entity orderLineEntity = createOrderLineEntityWithConstructors();
            AggregateRoot aggregate = createOrderAggregateWithLines(
                    "reconstitute",
                    List.of(
                            Parameter.of("id", TypeRef.of(DOMAIN_PKG + ".OrderId")),
                            Parameter.of("orderNumber", TypeRef.of("java.lang.String")),
                            Parameter.of(
                                    "lines",
                                    TypeRef.parameterized(
                                            "java.util.List", List.of(TypeRef.of(DOMAIN_PKG + ".OrderLine"))))));

            Identifier orderIdIdentifier = createIdentifier("OrderId", "java.util.UUID");
            ArchitecturalModel model = buildModelWith(aggregate, orderIdIdentifier, orderLineEntity);

            // When
            MapperSpec spec = MapperSpecBuilder.builder()
                    .aggregateRoot(aggregate)
                    .model(model)
                    .config(config)
                    .infrastructurePackage(INFRA_PKG)
                    .childEntityFqns(Set.of(DOMAIN_PKG + ".OrderLine"))
                    .entityMapping(Map.of(DOMAIN_PKG + ".OrderLine", INFRA_PKG + ".OrderLineJpaEntity"))
                    .build();

            // Then: Child entity reconstitution should be detected
            assertThat(spec.childEntityConversions()).hasSize(1);
            var childConversion = spec.childEntityConversions().get(0);
            assertThat(childConversion.reconstitutionSpec()).isNotNull();
            // And: factoryMethodName should be null (constructor-based)
            assertThat(childConversion.reconstitutionSpec().factoryMethodName()).isNull();
            // And: Should have parameters from the longest public constructor
            assertThat(childConversion.reconstitutionSpec().parameters()).hasSize(2);
            assertThat(childConversion.reconstitutionSpec().parameters().get(0).parameterName())
                    .isEqualTo("productName");
            assertThat(childConversion.reconstitutionSpec().parameters().get(1).parameterName())
                    .isEqualTo("quantity");
        }
    }

    // ===== Helper Methods =====

    /**
     * Creates an aggregate with a reconstitute-style factory method and no public no-arg constructor.
     */
    private AggregateRoot createAggregateWithReconstitute(String factoryMethodName, List<Parameter> params) {
        Method factoryMethod =
                createStaticFactoryMethod(factoryMethodName, TypeRef.of(DOMAIN_PKG + ".Customer"), params);
        return createAggregateWithMethods(List.of(factoryMethod), List.of());
    }

    /**
     * Creates an aggregate with an email field and a reconstitute-style factory method.
     */
    private AggregateRoot createAggregateWithEmailField(String factoryMethodName, List<Parameter> params) {
        Method factoryMethod =
                createStaticFactoryMethod(factoryMethodName, TypeRef.of(DOMAIN_PKG + ".Customer"), params);

        Field identityField = Field.builder("customerId", TypeRef.of(DOMAIN_PKG + ".CustomerId"))
                .wrappedType(TypeRef.of("java.util.UUID"))
                .roles(Set.of(FieldRole.IDENTITY))
                .build();

        TypeStructure structure = TypeStructure.builder(TypeNature.CLASS)
                .modifiers(Set.of(Modifier.PUBLIC))
                .fields(List.of(
                        identityField,
                        Field.of("firstName", TypeRef.of("java.lang.String")),
                        Field.of("lastName", TypeRef.of("java.lang.String")),
                        Field.of("email", TypeRef.of(DOMAIN_PKG + ".Email"))))
                .methods(List.of(factoryMethod))
                .constructors(List.of())
                .build();

        return AggregateRoot.builder(
                        TypeId.of(DOMAIN_PKG + ".Customer"),
                        structure,
                        highConfidence(ElementKind.AGGREGATE_ROOT),
                        identityField)
                .effectiveIdentityType(TypeRef.of("java.util.UUID"))
                .build();
    }

    /**
     * Creates an aggregate with specified methods and constructors.
     */
    private AggregateRoot createAggregateWithMethods(List<Method> methods, List<Constructor> constructors) {
        Field identityField = Field.builder("customerId", TypeRef.of(DOMAIN_PKG + ".CustomerId"))
                .wrappedType(TypeRef.of("java.util.UUID"))
                .roles(Set.of(FieldRole.IDENTITY))
                .build();

        TypeStructure structure = TypeStructure.builder(TypeNature.CLASS)
                .modifiers(Set.of(Modifier.PUBLIC))
                .fields(List.of(
                        identityField,
                        Field.of("firstName", TypeRef.of("java.lang.String")),
                        Field.of("lastName", TypeRef.of("java.lang.String"))))
                .methods(methods)
                .constructors(constructors)
                .build();

        return AggregateRoot.builder(
                        TypeId.of(DOMAIN_PKG + ".Customer"),
                        structure,
                        highConfidence(ElementKind.AGGREGATE_ROOT),
                        identityField)
                .effectiveIdentityType(TypeRef.of("java.util.UUID"))
                .build();
    }

    /**
     * Creates a static factory method with the FACTORY role.
     */
    private Method createStaticFactoryMethod(String name, TypeRef returnType, List<Parameter> params) {
        return new Method(
                name,
                returnType,
                params,
                Set.of(Modifier.PUBLIC, Modifier.STATIC),
                List.of(),
                Optional.empty(),
                List.of(),
                Set.of(MethodRole.FACTORY),
                OptionalInt.empty(),
                Optional.empty());
    }

    /**
     * Creates an identifier with the given simple name wrapping the specified type.
     */
    private Identifier createIdentifier(String simpleName, String wrappedTypeFqn) {
        Field valueField = Field.builder("value", TypeRef.of(wrappedTypeFqn))
                .roles(Set.of(FieldRole.IDENTITY))
                .build();

        TypeStructure structure = TypeStructure.builder(TypeNature.RECORD)
                .modifiers(Set.of(Modifier.PUBLIC))
                .fields(List.of(valueField))
                .build();

        return Identifier.of(
                TypeId.of(DOMAIN_PKG + "." + simpleName),
                structure,
                highConfidence(ElementKind.IDENTIFIER),
                TypeRef.of(wrappedTypeFqn));
    }

    /**
     * Creates an Order aggregate with an OrderId identity and a foreign key CustomerId field.
     */
    private AggregateRoot createOrderAggregateWithForeignKey(String factoryMethodName, List<Parameter> params) {
        Method factoryMethod = createStaticFactoryMethod(factoryMethodName, TypeRef.of(DOMAIN_PKG + ".Order"), params);

        Field identityField = Field.builder("orderId", TypeRef.of(DOMAIN_PKG + ".OrderId"))
                .wrappedType(TypeRef.of("java.util.UUID"))
                .roles(Set.of(FieldRole.IDENTITY))
                .build();

        TypeStructure structure = TypeStructure.builder(TypeNature.CLASS)
                .modifiers(Set.of(Modifier.PUBLIC))
                .fields(List.of(identityField, Field.of("customerId", TypeRef.of(DOMAIN_PKG + ".CustomerId"))))
                .methods(List.of(factoryMethod))
                .constructors(List.of())
                .build();

        return AggregateRoot.builder(
                        TypeId.of(DOMAIN_PKG + ".Order"),
                        structure,
                        highConfidence(ElementKind.AGGREGATE_ROOT),
                        identityField)
                .effectiveIdentityType(TypeRef.of("java.util.UUID"))
                .build();
    }

    /**
     * Creates a CustomerId identifier wrapping UUID.
     */
    private Identifier createCustomerIdIdentifier() {
        Field valueField = Field.builder("value", TypeRef.of("java.util.UUID"))
                .roles(Set.of(FieldRole.IDENTITY))
                .build();

        TypeStructure structure = TypeStructure.builder(TypeNature.RECORD)
                .modifiers(Set.of(Modifier.PUBLIC))
                .fields(List.of(valueField))
                .build();

        return Identifier.of(
                TypeId.of(DOMAIN_PKG + ".CustomerId"),
                structure,
                highConfidence(ElementKind.IDENTIFIER),
                TypeRef.of("java.util.UUID"));
    }

    /**
     * Creates a multi-value ValueObject (e.g., Money with amount + currency).
     */
    private ValueObject createMultiValueVO(String simpleName, List<Field> fields) {
        TypeStructure structure = TypeStructure.builder(TypeNature.RECORD)
                .modifiers(Set.of(Modifier.PUBLIC))
                .fields(fields)
                .build();

        return ValueObject.of(
                TypeId.of(DOMAIN_PKG + "." + simpleName), structure, highConfidence(ElementKind.VALUE_OBJECT));
    }

    /**
     * Creates an aggregate with a Money field and a reconstitute-style factory method.
     */
    private AggregateRoot createAggregateWithMoneyField(String factoryMethodName, List<Parameter> params) {
        Method factoryMethod =
                createStaticFactoryMethod(factoryMethodName, TypeRef.of(DOMAIN_PKG + ".Customer"), params);

        Field identityField = Field.builder("customerId", TypeRef.of(DOMAIN_PKG + ".CustomerId"))
                .wrappedType(TypeRef.of("java.util.UUID"))
                .roles(Set.of(FieldRole.IDENTITY))
                .build();

        TypeStructure structure = TypeStructure.builder(TypeNature.CLASS)
                .modifiers(Set.of(Modifier.PUBLIC))
                .fields(List.of(identityField, Field.of("amount", TypeRef.of(DOMAIN_PKG + ".Money"))))
                .methods(List.of(factoryMethod))
                .constructors(List.of())
                .build();

        return AggregateRoot.builder(
                        TypeId.of(DOMAIN_PKG + ".Customer"),
                        structure,
                        highConfidence(ElementKind.AGGREGATE_ROOT),
                        identityField)
                .effectiveIdentityType(TypeRef.of("java.util.UUID"))
                .build();
    }

    /**
     * Creates a single-value ValueObject (e.g., Email wrapping String).
     */
    private ValueObject createSingleValueVO(String simpleName, String wrappedTypeFqn) {
        Field valueField = Field.builder("value", TypeRef.of(wrappedTypeFqn)).build();

        TypeStructure structure = TypeStructure.builder(TypeNature.RECORD)
                .modifiers(Set.of(Modifier.PUBLIC))
                .fields(List.of(valueField))
                .build();

        return ValueObject.of(
                TypeId.of(DOMAIN_PKG + "." + simpleName), structure, highConfidence(ElementKind.VALUE_OBJECT));
    }

    /**
     * Creates an aggregate with audit fields (createdAt, updatedAt) and a reconstitute-style factory method.
     */
    private AggregateRoot createAggregateWithAuditFields(String factoryMethodName, List<Parameter> params) {
        Method factoryMethod =
                createStaticFactoryMethod(factoryMethodName, TypeRef.of(DOMAIN_PKG + ".Customer"), params);

        Field identityField = Field.builder("customerId", TypeRef.of(DOMAIN_PKG + ".CustomerId"))
                .wrappedType(TypeRef.of("java.util.UUID"))
                .roles(Set.of(FieldRole.IDENTITY))
                .build();

        TypeStructure structure = TypeStructure.builder(TypeNature.CLASS)
                .modifiers(Set.of(Modifier.PUBLIC))
                .fields(List.of(
                        identityField,
                        Field.of("firstName", TypeRef.of("java.lang.String")),
                        Field.of("createdAt", TypeRef.of("java.time.LocalDateTime")),
                        Field.of("updatedAt", TypeRef.of("java.time.LocalDateTime"))))
                .methods(List.of(factoryMethod))
                .constructors(List.of())
                .build();

        return AggregateRoot.builder(
                        TypeId.of(DOMAIN_PKG + ".Customer"),
                        structure,
                        highConfidence(ElementKind.AGGREGATE_ROOT),
                        identityField)
                .effectiveIdentityType(TypeRef.of("java.util.UUID"))
                .build();
    }

    /**
     * Creates an Order aggregate with an OrderId identity field, an orderNumber String field,
     * and a lines collection field of type List&lt;OrderLine&gt; with COLLECTION role.
     */
    private AggregateRoot createOrderAggregateWithLines(String factoryMethodName, List<Parameter> params) {
        Method factoryMethod = createStaticFactoryMethod(factoryMethodName, TypeRef.of(DOMAIN_PKG + ".Order"), params);

        Field identityField = Field.builder("orderId", TypeRef.of(DOMAIN_PKG + ".OrderId"))
                .wrappedType(TypeRef.of("java.util.UUID"))
                .roles(Set.of(FieldRole.IDENTITY))
                .build();

        Field linesField = Field.builder(
                        "lines",
                        TypeRef.parameterized("java.util.List", List.of(TypeRef.of(DOMAIN_PKG + ".OrderLine"))))
                .roles(Set.of(FieldRole.COLLECTION))
                .elementType(TypeRef.of(DOMAIN_PKG + ".OrderLine"))
                .build();

        TypeStructure structure = TypeStructure.builder(TypeNature.CLASS)
                .modifiers(Set.of(Modifier.PUBLIC))
                .fields(List.of(identityField, Field.of("orderNumber", TypeRef.of("java.lang.String")), linesField))
                .methods(List.of(factoryMethod))
                .constructors(List.of())
                .build();

        return AggregateRoot.builder(
                        TypeId.of(DOMAIN_PKG + ".Order"),
                        structure,
                        highConfidence(ElementKind.AGGREGATE_ROOT),
                        identityField)
                .effectiveIdentityType(TypeRef.of("java.util.UUID"))
                .build();
    }

    /**
     * Creates an OrderLine entity with productName and quantity fields.
     */
    private Entity createOrderLineEntity() {
        TypeStructure structure = TypeStructure.builder(TypeNature.CLASS)
                .modifiers(Set.of(Modifier.PUBLIC))
                .fields(List.of(
                        Field.of("productName", TypeRef.of("java.lang.String")),
                        Field.of("quantity", TypeRef.of("int"))))
                .build();

        return Entity.of(TypeId.of(DOMAIN_PKG + ".OrderLine"), structure, highConfidence(ElementKind.ENTITY));
    }

    /**
     * Creates an OrderLine entity with constructors but NO static factory methods.
     *
     * <p>Simulates a child entity like OrderLine that uses constructors for reconstitution:
     * <ul>
     *   <li>Private no-arg constructor (for JPA)</li>
     *   <li>Public 2-arg constructor (productName, quantity)</li>
     * </ul>
     */
    private Entity createOrderLineEntityWithConstructors() {
        Constructor noArgCtor = new Constructor(
                List.of(), Set.of(Modifier.PRIVATE), List.of(), Optional.empty(), List.of(), Optional.empty());

        Constructor fullCtor = new Constructor(
                List.of(
                        Parameter.of("productName", TypeRef.of("java.lang.String")),
                        Parameter.of("quantity", TypeRef.of("int"))),
                Set.of(Modifier.PUBLIC),
                List.of(),
                Optional.empty(),
                List.of(),
                Optional.empty());

        TypeStructure structure = TypeStructure.builder(TypeNature.CLASS)
                .modifiers(Set.of(Modifier.PUBLIC))
                .fields(List.of(
                        Field.of("productName", TypeRef.of("java.lang.String")),
                        Field.of("quantity", TypeRef.of("int"))))
                .constructors(List.of(noArgCtor, fullCtor))
                .build();

        return Entity.of(TypeId.of(DOMAIN_PKG + ".OrderLine"), structure, highConfidence(ElementKind.ENTITY));
    }

    /**
     * Builds an ArchitecturalModel containing the given aggregate and optional domain types.
     */
    private ArchitecturalModel buildModelWith(
            AggregateRoot aggregate, io.hexaglue.arch.model.ArchType... additionalTypes) {
        TypeRegistry.Builder registryBuilder = TypeRegistry.builder();
        registryBuilder.add(aggregate);
        for (io.hexaglue.arch.model.ArchType type : additionalTypes) {
            registryBuilder.add(type);
        }
        TypeRegistry registry = registryBuilder.build();
        DomainIndex domainIndex = DomainIndex.from(registry);

        ProjectContext project = ProjectContext.of("test-project", DOMAIN_PKG, Path.of("."));
        return ArchitecturalModel.builder(project)
                .typeRegistry(registry)
                .domainIndex(domainIndex)
                .build();
    }
}
