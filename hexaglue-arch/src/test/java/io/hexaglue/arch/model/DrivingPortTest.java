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

package io.hexaglue.arch.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.hexaglue.arch.ClassificationTrace;
import io.hexaglue.arch.ElementKind;
import io.hexaglue.arch.model.UseCase.UseCaseType;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DrivingPort}.
 *
 * @since 4.1.0
 */
@DisplayName("DrivingPort")
class DrivingPortTest {

    private static final TypeId ID = TypeId.of("com.example.OrderService");
    private static final TypeStructure STRUCTURE =
            TypeStructure.builder(TypeNature.INTERFACE).build();
    private static final ClassificationTrace TRACE = ClassificationTrace.highConfidence(
            ElementKind.DRIVING_PORT, "explicit-driving-port", "Has port annotation");
    private static final TypeRef VOID_TYPE = new TypeRef("void", "void", List.of(), false, false, 0);
    private static final TypeRef ORDER_TYPE = new TypeRef("com.example.Order", "Order", List.of(), false, false, 0);
    private static final TypeRef ORDER_ID_TYPE =
            new TypeRef("com.example.OrderId", "OrderId", List.of(), false, false, 0);
    private static final Method CREATE_ORDER_METHOD = Method.of("createOrder", VOID_TYPE, Set.of(MethodRole.COMMAND));
    private static final Method GET_ORDER_METHOD = Method.of("getOrder", ORDER_TYPE, Set.of(MethodRole.QUERY));
    private static final UseCase CREATE_ORDER_USE_CASE = UseCase.of(CREATE_ORDER_METHOD, UseCaseType.COMMAND);
    private static final UseCase GET_ORDER_USE_CASE = UseCase.of(GET_ORDER_METHOD, UseCaseType.QUERY);

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("should create with all required fields")
        void shouldCreateWithAllRequiredFields() {
            // when
            DrivingPort port = DrivingPort.of(ID, STRUCTURE, TRACE);

            // then
            assertThat(port.id()).isEqualTo(ID);
            assertThat(port.structure()).isEqualTo(STRUCTURE);
            assertThat(port.classification()).isEqualTo(TRACE);
            assertThat(port.useCases()).isEmpty();
            assertThat(port.inputTypes()).isEmpty();
            assertThat(port.outputTypes()).isEmpty();
        }

        @Test
        @DisplayName("should create with use cases and types")
        void shouldCreateWithUseCasesAndTypes() {
            // when
            DrivingPort port = DrivingPort.of(
                    ID,
                    STRUCTURE,
                    TRACE,
                    List.of(CREATE_ORDER_USE_CASE, GET_ORDER_USE_CASE),
                    List.of(ORDER_ID_TYPE),
                    List.of(ORDER_TYPE));

            // then
            assertThat(port.useCases()).containsExactly(CREATE_ORDER_USE_CASE, GET_ORDER_USE_CASE);
            assertThat(port.inputTypes()).containsExactly(ORDER_ID_TYPE);
            assertThat(port.outputTypes()).containsExactly(ORDER_TYPE);
        }

        @Test
        @DisplayName("should return DRIVING_PORT kind")
        void shouldReturnDrivingPortKind() {
            // when
            DrivingPort port = DrivingPort.of(ID, STRUCTURE, TRACE);

            // then
            assertThat(port.kind()).isEqualTo(ArchKind.DRIVING_PORT);
        }

        @Test
        @DisplayName("should reject null id")
        void shouldRejectNullId() {
            assertThatThrownBy(() -> DrivingPort.of(null, STRUCTURE, TRACE))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("id");
        }

        @Test
        @DisplayName("should reject null structure")
        void shouldRejectNullStructure() {
            assertThatThrownBy(() -> DrivingPort.of(ID, null, TRACE))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("structure");
        }

        @Test
        @DisplayName("should reject null classification")
        void shouldRejectNullClassification() {
            assertThatThrownBy(() -> DrivingPort.of(ID, STRUCTURE, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("classification");
        }
    }

    @Nested
    @DisplayName("ArchType Contract")
    class ArchTypeContract {

        @Test
        @DisplayName("should implement PortType interface")
        void shouldImplementPortType() {
            // given
            DrivingPort port = DrivingPort.of(ID, STRUCTURE, TRACE);

            // then
            assertThat(port).isInstanceOf(PortType.class);
            assertThat(port).isInstanceOf(ArchType.class);
        }

        @Test
        @DisplayName("should provide convenience methods from ArchType")
        void shouldProvideConvenienceMethods() {
            // given
            DrivingPort port = DrivingPort.of(ID, STRUCTURE, TRACE);

            // then
            assertThat(port.qualifiedName()).isEqualTo("com.example.OrderService");
            assertThat(port.simpleName()).isEqualTo("OrderService");
            assertThat(port.packageName()).isEqualTo("com.example");
        }
    }

    @Nested
    @DisplayName("Use Cases and Types")
    class UseCasesAndTypes {

        @Test
        @DisplayName("hasUseCases should return true when use cases are present")
        void hasUseCasesShouldReturnTrueWhenPresent() {
            // given
            DrivingPort port =
                    DrivingPort.of(ID, STRUCTURE, TRACE, List.of(CREATE_ORDER_USE_CASE), List.of(), List.of());

            // then
            assertThat(port.hasUseCases()).isTrue();
        }

        @Test
        @DisplayName("hasUseCases should return false when no use cases")
        void hasUseCasesShouldReturnFalseWhenAbsent() {
            // given
            DrivingPort port = DrivingPort.of(ID, STRUCTURE, TRACE);

            // then
            assertThat(port.hasUseCases()).isFalse();
        }

        @Test
        @DisplayName("commands should return only command use cases")
        void commandsShouldReturnOnlyCommands() {
            // given
            DrivingPort port = DrivingPort.of(
                    ID, STRUCTURE, TRACE, List.of(CREATE_ORDER_USE_CASE, GET_ORDER_USE_CASE), List.of(), List.of());

            // then
            assertThat(port.commands()).containsExactly(CREATE_ORDER_USE_CASE);
        }

        @Test
        @DisplayName("queries should return only query use cases")
        void queriesShouldReturnOnlyQueries() {
            // given
            DrivingPort port = DrivingPort.of(
                    ID, STRUCTURE, TRACE, List.of(CREATE_ORDER_USE_CASE, GET_ORDER_USE_CASE), List.of(), List.of());

            // then
            assertThat(port.queries()).containsExactly(GET_ORDER_USE_CASE);
        }

        @Test
        @DisplayName("hasInputTypes should return true when input types are present")
        void hasInputTypesShouldReturnTrueWhenPresent() {
            // given
            DrivingPort port = DrivingPort.of(ID, STRUCTURE, TRACE, List.of(), List.of(ORDER_ID_TYPE), List.of());

            // then
            assertThat(port.hasInputTypes()).isTrue();
        }

        @Test
        @DisplayName("hasOutputTypes should return true when output types are present")
        void hasOutputTypesShouldReturnTrueWhenPresent() {
            // given
            DrivingPort port = DrivingPort.of(ID, STRUCTURE, TRACE, List.of(), List.of(), List.of(ORDER_TYPE));

            // then
            assertThat(port.hasOutputTypes()).isTrue();
        }

        @Test
        @DisplayName("should make defensive copies of lists")
        void shouldMakeDefensiveCopies() {
            // given
            List<UseCase> mutableUseCases = new java.util.ArrayList<>();
            mutableUseCases.add(CREATE_ORDER_USE_CASE);
            List<TypeRef> mutableInputs = new java.util.ArrayList<>();
            mutableInputs.add(ORDER_ID_TYPE);

            // when
            DrivingPort port = DrivingPort.of(ID, STRUCTURE, TRACE, mutableUseCases, mutableInputs, List.of());
            mutableUseCases.add(GET_ORDER_USE_CASE);

            // then
            assertThat(port.useCases()).containsExactly(CREATE_ORDER_USE_CASE);
            assertThat(port.inputTypes()).containsExactly(ORDER_ID_TYPE);
        }

        @Test
        @DisplayName("should reject null useCases")
        void shouldRejectNullUseCases() {
            assertThatThrownBy(() -> DrivingPort.of(ID, STRUCTURE, TRACE, null, List.of(), List.of()))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("useCases");
        }

        @Test
        @DisplayName("should reject null inputTypes")
        void shouldRejectNullInputTypes() {
            assertThatThrownBy(() -> DrivingPort.of(ID, STRUCTURE, TRACE, List.of(), null, List.of()))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("inputTypes");
        }

        @Test
        @DisplayName("should reject null outputTypes")
        void shouldRejectNullOutputTypes() {
            assertThatThrownBy(() -> DrivingPort.of(ID, STRUCTURE, TRACE, List.of(), List.of(), null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("outputTypes");
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("should be equal when all fields match")
        void shouldBeEqualWhenAllFieldsMatch() {
            // given
            DrivingPort p1 = DrivingPort.of(ID, STRUCTURE, TRACE);
            DrivingPort p2 = DrivingPort.of(ID, STRUCTURE, TRACE);

            // then
            assertThat(p1).isEqualTo(p2);
            assertThat(p1.hashCode()).isEqualTo(p2.hashCode());
        }

        @Test
        @DisplayName("should not be equal when ids differ")
        void shouldNotBeEqualWhenIdsDiffer() {
            // given
            TypeId otherId = TypeId.of("com.example.CustomerService");
            DrivingPort p1 = DrivingPort.of(ID, STRUCTURE, TRACE);
            DrivingPort p2 = DrivingPort.of(otherId, STRUCTURE, TRACE);

            // then
            assertThat(p1).isNotEqualTo(p2);
        }
    }
}
