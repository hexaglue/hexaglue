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

package io.hexaglue.plugin.jpa.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link MethodPattern} inference logic.
 *
 * <p>These tests validate that method names are correctly categorized into
 * standard repository patterns (SAVE, FIND_BY_ID, etc.) based on naming conventions.
 */
class MethodPatternTest {

    @ParameterizedTest
    @ValueSource(strings = {"save", "create", "persist", "store", "add", "update", "upsert", "Save", "CREATE"})
    void infer_shouldDetectSavePattern_forSaveOperations(String methodName) {
        // When: Inferring pattern from save-like method names
        MethodPattern pattern = MethodPattern.infer(methodName);

        // Then: Should be categorized as SAVE
        assertThat(pattern).isEqualTo(MethodPattern.SAVE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"findById", "getById", "loadById", "findId", "getId", "loadId", "findone", "getone"})
    void infer_shouldDetectFindByIdPattern_forIdLookup(String methodName) {
        // When: Inferring pattern from find-by-id method names
        MethodPattern pattern = MethodPattern.infer(methodName);

        // Then: Should be categorized as FIND_BY_ID
        assertThat(pattern).isEqualTo(MethodPattern.FIND_BY_ID);
    }

    @ParameterizedTest
    @ValueSource(strings = {"findAll", "getAll", "list", "listAll", "all", "FindAll", "GETALL"})
    void infer_shouldDetectFindAllPattern_forListOperations(String methodName) {
        // When: Inferring pattern from find-all method names
        MethodPattern pattern = MethodPattern.infer(methodName);

        // Then: Should be categorized as FIND_ALL
        assertThat(pattern).isEqualTo(MethodPattern.FIND_ALL);
    }

    @ParameterizedTest
    @ValueSource(strings = {"delete", "deleteById", "remove", "removeById", "Delete", "REMOVE"})
    void infer_shouldDetectDeletePattern_forDeleteOperations(String methodName) {
        // When: Inferring pattern from delete-like method names
        MethodPattern pattern = MethodPattern.infer(methodName);

        // Then: Should be categorized as DELETE
        assertThat(pattern).isEqualTo(MethodPattern.DELETE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"exists", "existsById", "contains", "Exists", "EXISTSBYID"})
    void infer_shouldDetectExistsPattern_forExistenceChecks(String methodName) {
        // When: Inferring pattern from exists-like method names
        MethodPattern pattern = MethodPattern.infer(methodName);

        // Then: Should be categorized as EXISTS
        assertThat(pattern).isEqualTo(MethodPattern.EXISTS);
    }

    @ParameterizedTest
    @ValueSource(strings = {"count", "countAll", "Count", "COUNTALL"})
    void infer_shouldDetectCountPattern_forCountOperations(String methodName) {
        // When: Inferring pattern from count-like method names
        MethodPattern pattern = MethodPattern.infer(methodName);

        // Then: Should be categorized as COUNT
        assertThat(pattern).isEqualTo(MethodPattern.COUNT);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "findByStatus",
                "findActiveOrders",
                "findByStatusAndDateAfter",
                "getOrdersByCustomerId",
                "searchByKeyword",
                "queryByEmail",
                "customMethod"
            })
    void infer_shouldReturnCustom_forUnknownPatterns(String methodName) {
        // When: Inferring pattern from non-standard method names
        MethodPattern pattern = MethodPattern.infer(methodName);

        // Then: Should be categorized as CUSTOM
        assertThat(pattern).isEqualTo(MethodPattern.CUSTOM);
    }

    @Test
    void infer_shouldReturnCustom_forNullMethodName() {
        // When: Inferring pattern from null
        MethodPattern pattern = MethodPattern.infer(null);

        // Then: Should default to CUSTOM
        assertThat(pattern).isEqualTo(MethodPattern.CUSTOM);
    }

    @Test
    void infer_shouldReturnCustom_forEmptyMethodName() {
        // When: Inferring pattern from empty string
        MethodPattern pattern = MethodPattern.infer("");

        // Then: Should default to CUSTOM
        assertThat(pattern).isEqualTo(MethodPattern.CUSTOM);
    }

    @Test
    void infer_shouldBeCaseInsensitive() {
        // Given: Same method name in different cases
        String lowercase = "save";
        String uppercase = "SAVE";
        String mixedCase = "SaVe";

        // When: Inferring patterns
        MethodPattern lowercasePattern = MethodPattern.infer(lowercase);
        MethodPattern uppercasePattern = MethodPattern.infer(uppercase);
        MethodPattern mixedCasePattern = MethodPattern.infer(mixedCase);

        // Then: All should be recognized as SAVE
        assertThat(lowercasePattern).isEqualTo(MethodPattern.SAVE);
        assertThat(uppercasePattern).isEqualTo(MethodPattern.SAVE);
        assertThat(mixedCasePattern).isEqualTo(MethodPattern.SAVE);
    }

    @Test
    void infer_shouldDistinguishBetweenFindAllAndFindBy() {
        // Given: Similar method names
        String findAll = "findAll";
        String findByStatus = "findAllByStatus";

        // When: Inferring patterns
        MethodPattern findAllPattern = MethodPattern.infer(findAll);
        MethodPattern findByPattern = MethodPattern.infer(findByStatus);

        // Then: findAll is FIND_ALL, but findAllByStatus is CUSTOM
        assertThat(findAllPattern).isEqualTo(MethodPattern.FIND_ALL);
        assertThat(findByPattern).isEqualTo(MethodPattern.CUSTOM);
    }

    @Test
    void infer_shouldHandlePrefixMatching() {
        // Given: Method names with standard prefixes followed by qualifiers
        String saveOrder = "saveOrder";
        String createCustomer = "createCustomer";
        String deleteById = "deleteById";

        // When: Inferring patterns
        MethodPattern savePattern = MethodPattern.infer(saveOrder);
        MethodPattern createPattern = MethodPattern.infer(createCustomer);
        MethodPattern deletePattern = MethodPattern.infer(deleteById);

        // Then: All should match their prefixes
        assertThat(savePattern).isEqualTo(MethodPattern.SAVE);
        assertThat(createPattern).isEqualTo(MethodPattern.SAVE);
        assertThat(deletePattern).isEqualTo(MethodPattern.DELETE);
    }
}
