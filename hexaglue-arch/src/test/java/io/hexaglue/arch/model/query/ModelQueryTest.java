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

package io.hexaglue.arch.model.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.hexaglue.arch.ClassificationTrace;
import io.hexaglue.arch.ElementKind;
import io.hexaglue.arch.model.AggregateRoot;
import io.hexaglue.arch.model.DrivingPort;
import io.hexaglue.arch.model.Field;
import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.TypeNature;
import io.hexaglue.arch.model.TypeRegistry;
import io.hexaglue.arch.model.TypeStructure;
import io.hexaglue.arch.model.index.DomainIndex;
import io.hexaglue.arch.model.index.PortIndex;
import io.hexaglue.syntax.TypeRef;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ModelQuery}.
 *
 * @since 5.0.0
 */
@DisplayName("ModelQuery")
class ModelQueryTest {

    private static final TypeId ORDER_ID = TypeId.of("com.example.order.Order");
    private static final TypeId ORDER_SERVICE_ID = TypeId.of("com.example.order.OrderService");

    private TypeRegistry registry;
    private DomainIndex domainIndex;
    private PortIndex portIndex;
    private ModelQuery query;

    @BeforeEach
    void setUp() {
        TypeStructure classStructure = TypeStructure.builder(TypeNature.CLASS).build();
        TypeStructure interfaceStructure =
                TypeStructure.builder(TypeNature.INTERFACE).build();

        ClassificationTrace aggTrace = ClassificationTrace.highConfidence(ElementKind.AGGREGATE_ROOT, "test", "Test");
        ClassificationTrace portTrace = ClassificationTrace.highConfidence(ElementKind.DRIVING_PORT, "test", "Test");

        Field idField = Field.builder("id", TypeRef.of("java.util.UUID")).build();

        AggregateRoot orderAggregate = AggregateRoot.builder(ORDER_ID, classStructure, aggTrace, idField)
                .build();

        DrivingPort orderService = DrivingPort.of(ORDER_SERVICE_ID, interfaceStructure, portTrace);

        registry = TypeRegistry.builder().add(orderAggregate).add(orderService).build();

        domainIndex = DomainIndex.from(registry);
        portIndex = PortIndex.from(registry);
        query = ModelQuery.of(registry, domainIndex, portIndex);
    }

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("should create with all required parameters")
        void shouldCreateWithAllRequiredParameters() {
            // when
            ModelQuery modelQuery = ModelQuery.of(registry, domainIndex, portIndex);

            // then
            assertThat(modelQuery).isNotNull();
            assertThat(modelQuery.registry()).isSameAs(registry);
            assertThat(modelQuery.domainIndex()).isSameAs(domainIndex);
            assertThat(modelQuery.portIndex()).isSameAs(portIndex);
        }

        @Test
        @DisplayName("should reject null registry")
        void shouldRejectNullRegistry() {
            assertThatThrownBy(() -> ModelQuery.of(null, domainIndex, portIndex))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("registry");
        }

        @Test
        @DisplayName("should reject null domainIndex")
        void shouldRejectNullDomainIndex() {
            assertThatThrownBy(() -> ModelQuery.of(registry, null, portIndex))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("domainIndex");
        }

        @Test
        @DisplayName("should reject null portIndex")
        void shouldRejectNullPortIndex() {
            assertThatThrownBy(() -> ModelQuery.of(registry, domainIndex, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("portIndex");
        }
    }

    @Nested
    @DisplayName("Query Methods")
    class QueryMethods {

        @Test
        @DisplayName("aggregates() should return AggregateQuery")
        void aggregatesShouldReturnAggregateQuery() {
            // when
            AggregateQuery aggQuery = query.aggregates();

            // then
            assertThat(aggQuery).isNotNull();
            assertThat(aggQuery.toList()).hasSize(1);
        }

        @Test
        @DisplayName("drivingPorts() should return PortQuery")
        void drivingPortsShouldReturnPortQuery() {
            // when
            PortQuery portQuery = query.drivingPorts();

            // then
            assertThat(portQuery).isNotNull();
            assertThat(portQuery.toList()).hasSize(1);
        }

        @Test
        @DisplayName("drivenPorts() should return PortQuery")
        void drivenPortsShouldReturnPortQuery() {
            // when
            PortQuery portQuery = query.drivenPorts();

            // then
            assertThat(portQuery).isNotNull();
        }

        @Test
        @DisplayName("allPorts() should return PortQuery for all ports")
        void allPortsShouldReturnPortQueryForAllPorts() {
            // when
            PortQuery portQuery = query.allPorts();

            // then
            assertThat(portQuery).isNotNull();
            assertThat(portQuery.toList()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Direct Lookup")
    class DirectLookup {

        @Test
        @DisplayName("aggregate() should find by TypeId")
        void aggregateShouldFindByTypeId() {
            // when
            Optional<AggregateRoot> result = query.aggregate(ORDER_ID);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo(ORDER_ID);
        }

        @Test
        @DisplayName("aggregate() should return empty for unknown TypeId")
        void aggregateShouldReturnEmptyForUnknown() {
            // when
            Optional<AggregateRoot> result = query.aggregate(TypeId.of("com.unknown.Unknown"));

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("find() should find any type by TypeId")
        void findShouldFindAnyTypeByTypeId() {
            // when
            Optional<AggregateRoot> result = query.find(ORDER_ID);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo(ORDER_ID);
        }
    }
}
