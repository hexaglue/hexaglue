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

package io.hexaglue.core.ir.export;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.core.frontend.TypeRef;
import io.hexaglue.core.graph.model.ParameterInfo;
import io.hexaglue.spi.ir.Identity;
import io.hexaglue.spi.ir.IdentityStrategy;
import io.hexaglue.spi.ir.IdentityWrapperKind;
import io.hexaglue.spi.ir.MethodKind;
import io.hexaglue.spi.ir.QueryModifier;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PortMethodClassifier}.
 *
 * <p>These tests verify that the classifier correctly identifies method kinds,
 * target properties, query modifiers, and other Spring Data conventions.
 */
class PortMethodClassifierTest {

    private PortMethodClassifier classifier;
    private Identity uuidIdentity;
    private Identity wrappedIdentity;

    @BeforeEach
    void setUp() {
        classifier = new PortMethodClassifier();

        // Create a simple UUID identity
        uuidIdentity =
                Identity.unwrapped("id", io.hexaglue.spi.ir.TypeRef.of("java.util.UUID"), IdentityStrategy.ASSIGNED);

        // Create a wrapped identity (OrderId wrapping UUID)
        wrappedIdentity = Identity.wrapped(
                "id",
                io.hexaglue.spi.ir.TypeRef.of("com.example.OrderId"),
                io.hexaglue.spi.ir.TypeRef.of("java.util.UUID"),
                IdentityStrategy.ASSIGNED,
                IdentityWrapperKind.RECORD,
                "value");
    }

    @Nested
    @DisplayName("CRUD Methods")
    class CrudMethods {

        @Test
        @DisplayName("save() should be classified as SAVE")
        void save_shouldBeClassifiedAsSave() {
            var result = classifier.classify(
                    "save", List.of(ParameterInfo.of("entity", TypeRef.of("com.example.Order"))), Optional.empty());

            assertThat(result.kind()).isEqualTo(MethodKind.SAVE);
        }

        @Test
        @DisplayName("saveAll() should be classified as SAVE_ALL")
        void saveAll_shouldBeClassifiedAsSaveAll() {
            var result = classifier.classify(
                    "saveAll",
                    List.of(ParameterInfo.of(
                            "entities", TypeRef.parameterized("java.util.List", TypeRef.of("com.example.Order")))),
                    Optional.empty());

            assertThat(result.kind()).isEqualTo(MethodKind.SAVE_ALL);
        }
    }

    @Nested
    @DisplayName("Find By ID Methods")
    class FindByIdMethods {

        @Test
        @DisplayName("findById() with UUID parameter should be classified as FIND_BY_ID")
        void findById_withUuidParam_shouldBeClassifiedAsFindById() {
            var result = classifier.classify(
                    "findById",
                    List.of(ParameterInfo.of("id", TypeRef.of("java.util.UUID"))),
                    Optional.of(uuidIdentity));

            assertThat(result.kind()).isEqualTo(MethodKind.FIND_BY_ID);
        }

        @Test
        @DisplayName("findById() with wrapped identity should be classified as FIND_BY_ID")
        void findById_withWrappedIdentity_shouldBeClassifiedAsFindById() {
            var result = classifier.classify(
                    "findById",
                    List.of(ParameterInfo.of("id", TypeRef.of("com.example.OrderId"))),
                    Optional.of(wrappedIdentity));

            assertThat(result.kind()).isEqualTo(MethodKind.FIND_BY_ID);
        }

        @Test
        @DisplayName("getById() should be classified as FIND_BY_ID")
        void getById_shouldBeClassifiedAsFindById() {
            var result = classifier.classify(
                    "getById",
                    List.of(ParameterInfo.of("id", TypeRef.of("java.util.UUID"))),
                    Optional.of(uuidIdentity));

            assertThat(result.kind()).isEqualTo(MethodKind.FIND_BY_ID);
        }

        @Test
        @DisplayName("loadById() should be classified as FIND_BY_ID")
        void loadById_shouldBeClassifiedAsFindById() {
            var result = classifier.classify(
                    "loadById",
                    List.of(ParameterInfo.of("id", TypeRef.of("java.util.UUID"))),
                    Optional.of(uuidIdentity));

            assertThat(result.kind()).isEqualTo(MethodKind.FIND_BY_ID);
        }

        @Test
        @DisplayName("findAllById() should be classified as FIND_ALL_BY_ID")
        void findAllById_shouldBeClassifiedAsFindAllById() {
            var result = classifier.classify(
                    "findAllById",
                    List.of(ParameterInfo.of(
                            "ids", TypeRef.parameterized("java.util.List", TypeRef.of("java.util.UUID")))),
                    Optional.of(uuidIdentity));

            assertThat(result.kind()).isEqualTo(MethodKind.FIND_ALL_BY_ID);
        }
    }

    @Nested
    @DisplayName("Find By Property Methods")
    class FindByPropertyMethods {

        @Test
        @DisplayName("findByEmail() should be classified as FIND_BY_PROPERTY with 'email' target")
        void findByEmail_shouldBeClassifiedAsFindByProperty() {
            var result = classifier.classify(
                    "findByEmail",
                    List.of(ParameterInfo.of("email", TypeRef.of("java.lang.String"))),
                    Optional.of(uuidIdentity));

            assertThat(result.kind()).isEqualTo(MethodKind.FIND_BY_PROPERTY);
            assertThat(result.targetProperties()).containsExactly("email");
        }

        @Test
        @DisplayName("findByStatus() should extract 'status' as target property")
        void findByStatus_shouldExtractStatusAsTargetProperty() {
            var result = classifier.classify(
                    "findByStatus",
                    List.of(ParameterInfo.of("status", TypeRef.of("com.example.OrderStatus"))),
                    Optional.empty());

            assertThat(result.kind()).isEqualTo(MethodKind.FIND_BY_PROPERTY);
            assertThat(result.targetProperties()).containsExactly("status");
        }

        @Test
        @DisplayName("findAllByStatus() should be classified as FIND_ALL_BY_PROPERTY")
        void findAllByStatus_shouldBeClassifiedAsFindAllByProperty() {
            var result = classifier.classify(
                    "findAllByStatus",
                    List.of(ParameterInfo.of("status", TypeRef.of("java.lang.String"))),
                    Optional.empty());

            assertThat(result.kind()).isEqualTo(MethodKind.FIND_ALL_BY_PROPERTY);
            assertThat(result.targetProperties()).containsExactly("status");
        }

        @Test
        @DisplayName("findByFirstNameAndLastName() should extract multiple properties")
        void findByFirstNameAndLastName_shouldExtractMultipleProperties() {
            var result = classifier.classify(
                    "findByFirstNameAndLastName",
                    List.of(
                            ParameterInfo.of("firstName", TypeRef.of("java.lang.String")),
                            ParameterInfo.of("lastName", TypeRef.of("java.lang.String"))),
                    Optional.empty());

            assertThat(result.kind()).isEqualTo(MethodKind.FIND_BY_PROPERTY);
            assertThat(result.targetProperties()).containsExactly("firstName", "lastName");
        }
    }

    @Nested
    @DisplayName("Exists Methods")
    class ExistsMethods {

        @Test
        @DisplayName("existsById() should be classified as EXISTS_BY_ID")
        void existsById_shouldBeClassifiedAsExistsById() {
            var result = classifier.classify(
                    "existsById",
                    List.of(ParameterInfo.of("id", TypeRef.of("java.util.UUID"))),
                    Optional.of(uuidIdentity));

            assertThat(result.kind()).isEqualTo(MethodKind.EXISTS_BY_ID);
        }

        @Test
        @DisplayName("existsByEmail() should be classified as EXISTS_BY_PROPERTY")
        void existsByEmail_shouldBeClassifiedAsExistsByProperty() {
            var result = classifier.classify(
                    "existsByEmail",
                    List.of(ParameterInfo.of("email", TypeRef.of("java.lang.String"))),
                    Optional.of(uuidIdentity));

            assertThat(result.kind()).isEqualTo(MethodKind.EXISTS_BY_PROPERTY);
            assertThat(result.targetProperties()).containsExactly("email");
        }
    }

    @Nested
    @DisplayName("Delete Methods")
    class DeleteMethods {

        @Test
        @DisplayName("deleteById() should be classified as DELETE_BY_ID")
        void deleteById_shouldBeClassifiedAsDeleteById() {
            var result = classifier.classify(
                    "deleteById",
                    List.of(ParameterInfo.of("id", TypeRef.of("java.util.UUID"))),
                    Optional.of(uuidIdentity));

            assertThat(result.kind()).isEqualTo(MethodKind.DELETE_BY_ID);
        }

        @Test
        @DisplayName("deleteAll() should be classified as DELETE_ALL")
        void deleteAll_shouldBeClassifiedAsDeleteAll() {
            var result = classifier.classify("deleteAll", List.of(), Optional.empty());

            assertThat(result.kind()).isEqualTo(MethodKind.DELETE_ALL);
        }

        @Test
        @DisplayName("deleteByStatus() should be classified as DELETE_BY_PROPERTY")
        void deleteByStatus_shouldBeClassifiedAsDeleteByProperty() {
            var result = classifier.classify(
                    "deleteByStatus",
                    List.of(ParameterInfo.of("status", TypeRef.of("java.lang.String"))),
                    Optional.empty());

            assertThat(result.kind()).isEqualTo(MethodKind.DELETE_BY_PROPERTY);
            assertThat(result.targetProperties()).containsExactly("status");
        }
    }

    @Nested
    @DisplayName("Count Methods")
    class CountMethods {

        @Test
        @DisplayName("count() should be classified as COUNT_ALL")
        void count_shouldBeClassifiedAsCountAll() {
            var result = classifier.classify("count", List.of(), Optional.empty());

            assertThat(result.kind()).isEqualTo(MethodKind.COUNT_ALL);
        }

        @Test
        @DisplayName("countByStatus() should be classified as COUNT_BY_PROPERTY")
        void countByStatus_shouldBeClassifiedAsCountByProperty() {
            var result = classifier.classify(
                    "countByStatus",
                    List.of(ParameterInfo.of("status", TypeRef.of("java.lang.String"))),
                    Optional.empty());

            assertThat(result.kind()).isEqualTo(MethodKind.COUNT_BY_PROPERTY);
            assertThat(result.targetProperties()).containsExactly("status");
        }
    }

    @Nested
    @DisplayName("Find All Methods")
    class FindAllMethods {

        @Test
        @DisplayName("findAll() without parameters should be classified as FIND_ALL")
        void findAll_shouldBeClassifiedAsFindAll() {
            var result = classifier.classify("findAll", List.of(), Optional.empty());

            assertThat(result.kind()).isEqualTo(MethodKind.FIND_ALL);
        }
    }

    @Nested
    @DisplayName("Stream Methods")
    class StreamMethods {

        @Test
        @DisplayName("streamAll() should be classified as STREAM_ALL")
        void streamAll_shouldBeClassifiedAsStreamAll() {
            var result = classifier.classify("streamAll", List.of(), Optional.empty());

            assertThat(result.kind()).isEqualTo(MethodKind.STREAM_ALL);
        }

        @Test
        @DisplayName("streamByStatus() should be classified as STREAM_BY_PROPERTY")
        void streamByStatus_shouldBeClassifiedAsStreamByProperty() {
            var result = classifier.classify(
                    "streamByStatus",
                    List.of(ParameterInfo.of("status", TypeRef.of("java.lang.String"))),
                    Optional.empty());

            assertThat(result.kind()).isEqualTo(MethodKind.STREAM_BY_PROPERTY);
            assertThat(result.targetProperties()).containsExactly("status");
        }
    }

    @Nested
    @DisplayName("Top/First Methods")
    class TopFirstMethods {

        @Test
        @DisplayName("findTop10ByStatus() should be classified as FIND_TOP_N with limit 10")
        void findTop10ByStatus_shouldBeClassifiedAsFindTopN() {
            var result = classifier.classify(
                    "findTop10ByStatus",
                    List.of(ParameterInfo.of("status", TypeRef.of("java.lang.String"))),
                    Optional.empty());

            assertThat(result.kind()).isEqualTo(MethodKind.FIND_TOP_N);
            assertThat(result.limitSize()).hasValue(10);
            assertThat(result.targetProperties()).containsExactly("status");
        }

        @Test
        @DisplayName("findFirst5ByCategory() should be classified as FIND_TOP_N with limit 5")
        void findFirst5ByCategory_shouldBeClassifiedAsFindTopN() {
            var result = classifier.classify(
                    "findFirst5ByCategory",
                    List.of(ParameterInfo.of("category", TypeRef.of("java.lang.String"))),
                    Optional.empty());

            assertThat(result.kind()).isEqualTo(MethodKind.FIND_TOP_N);
            assertThat(result.limitSize()).hasValue(5);
        }

        @Test
        @DisplayName("findFirstByStatus() should be classified as FIND_FIRST")
        void findFirstByStatus_shouldBeClassifiedAsFindFirst() {
            var result = classifier.classify(
                    "findFirstByStatus",
                    List.of(ParameterInfo.of("status", TypeRef.of("java.lang.String"))),
                    Optional.empty());

            assertThat(result.kind()).isEqualTo(MethodKind.FIND_FIRST);
        }
    }

    @Nested
    @DisplayName("Query Modifiers")
    class QueryModifiers {

        @Test
        @DisplayName("findDistinctByStatus() should detect DISTINCT modifier")
        void findDistinctByStatus_shouldDetectDistinctModifier() {
            var result = classifier.classify(
                    "findDistinctByStatus",
                    List.of(ParameterInfo.of("status", TypeRef.of("java.lang.String"))),
                    Optional.empty());

            assertThat(result.modifiers()).contains(QueryModifier.DISTINCT);
        }

        @Test
        @DisplayName("findByEmailIgnoreCase() should detect IGNORE_CASE modifier")
        void findByEmailIgnoreCase_shouldDetectIgnoreCaseModifier() {
            var result = classifier.classify(
                    "findByEmailIgnoreCase",
                    List.of(ParameterInfo.of("email", TypeRef.of("java.lang.String"))),
                    Optional.empty());

            assertThat(result.modifiers()).contains(QueryModifier.IGNORE_CASE);
        }

        @Test
        @DisplayName("findByNameOrderByAgeAsc() should detect ORDER_BY_ASC modifier")
        void findByNameOrderByAgeAsc_shouldDetectOrderByAscModifier() {
            var result = classifier.classify(
                    "findByNameOrderByAgeAsc",
                    List.of(ParameterInfo.of("name", TypeRef.of("java.lang.String"))),
                    Optional.empty());

            assertThat(result.modifiers()).contains(QueryModifier.ORDER_BY_ASC);
            assertThat(result.orderByProperty()).hasValue("age");
        }

        @Test
        @DisplayName("findByNameOrderByAgeDesc() should detect ORDER_BY_DESC modifier")
        void findByNameOrderByAgeDesc_shouldDetectOrderByDescModifier() {
            var result = classifier.classify(
                    "findByNameOrderByAgeDesc",
                    List.of(ParameterInfo.of("name", TypeRef.of("java.lang.String"))),
                    Optional.empty());

            assertThat(result.modifiers()).contains(QueryModifier.ORDER_BY_DESC);
            assertThat(result.orderByProperty()).hasValue("age");
        }
    }

    @Nested
    @DisplayName("Custom Methods")
    class CustomMethods {

        @Test
        @DisplayName("unknownMethod() should be classified as CUSTOM")
        void unknownMethod_shouldBeClassifiedAsCustom() {
            var result = classifier.classify(
                    "unknownMethod",
                    List.of(ParameterInfo.of("param", TypeRef.of("java.lang.String"))),
                    Optional.empty());

            assertThat(result.kind()).isEqualTo(MethodKind.CUSTOM);
        }

        @Test
        @DisplayName("someCustomBusinessMethod() should be classified as CUSTOM")
        void someCustomBusinessMethod_shouldBeClassifiedAsCustom() {
            var result = classifier.classify(
                    "someCustomBusinessMethod",
                    List.of(ParameterInfo.of("param", TypeRef.of("java.lang.Object"))),
                    Optional.empty());

            assertThat(result.kind()).isEqualTo(MethodKind.CUSTOM);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("findById without identity context should still classify based on common ID types")
        void findById_withoutIdentityContext_shouldClassifyBasedOnCommonIdTypes() {
            var result = classifier.classify(
                    "findById", List.of(ParameterInfo.of("id", TypeRef.of("java.util.UUID"))), Optional.empty());

            assertThat(result.kind()).isEqualTo(MethodKind.FIND_BY_ID);
        }

        @Test
        @DisplayName("findById with Long parameter should classify as FIND_BY_ID")
        void findById_withLongParam_shouldClassifyAsFindById() {
            var result = classifier.classify(
                    "findById", List.of(ParameterInfo.of("id", TypeRef.of("java.lang.Long"))), Optional.empty());

            assertThat(result.kind()).isEqualTo(MethodKind.FIND_BY_ID);
        }

        @Test
        @DisplayName("findByEmailIn() should handle 'In' operator correctly")
        void findByEmailIn_shouldHandleInOperator() {
            var result = classifier.classify(
                    "findByEmailIn",
                    List.of(ParameterInfo.of(
                            "emails", TypeRef.parameterized("java.util.List", TypeRef.of("java.lang.String")))),
                    Optional.empty());

            assertThat(result.kind()).isEqualTo(MethodKind.FIND_BY_PROPERTY);
            assertThat(result.targetProperties()).containsExactly("email");
        }

        @Test
        @DisplayName("findByStatusOrPriority() should extract both properties")
        void findByStatusOrPriority_shouldExtractBothProperties() {
            var result = classifier.classify(
                    "findByStatusOrPriority",
                    List.of(
                            ParameterInfo.of("status", TypeRef.of("java.lang.String")),
                            ParameterInfo.of("priority", TypeRef.of("java.lang.Integer"))),
                    Optional.empty());

            assertThat(result.kind()).isEqualTo(MethodKind.FIND_BY_PROPERTY);
            assertThat(result.targetProperties()).containsExactly("status", "priority");
        }
    }
}
