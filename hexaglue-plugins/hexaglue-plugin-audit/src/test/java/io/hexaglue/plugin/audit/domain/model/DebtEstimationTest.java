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

package io.hexaglue.plugin.audit.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DebtEstimation}.
 */
class DebtEstimationTest {

    @Test
    void shouldCreateValidDebtEstimation() {
        // When
        DebtEstimation debt = new DebtEstimation(10.0, 5000.0, 250.0);

        // Then
        assertThat(debt.totalDays()).isEqualTo(10.0);
        assertThat(debt.totalCost()).isEqualTo(5000.0);
        assertThat(debt.monthlyInterest()).isEqualTo(250.0);
    }

    @Test
    void shouldCreateZeroDebtEstimation() {
        // When
        DebtEstimation debt = new DebtEstimation(0.0, 0.0, 0.0);

        // Then
        assertThat(debt.totalDays()).isEqualTo(0.0);
        assertThat(debt.totalCost()).isEqualTo(0.0);
        assertThat(debt.monthlyInterest()).isEqualTo(0.0);
        assertThat(debt.isZero()).isTrue();
    }

    @Test
    void shouldCreateZeroDebtViaFactoryMethod() {
        // When
        DebtEstimation debt = DebtEstimation.zero();

        // Then
        assertThat(debt.totalDays()).isEqualTo(0.0);
        assertThat(debt.totalCost()).isEqualTo(0.0);
        assertThat(debt.monthlyInterest()).isEqualTo(0.0);
        assertThat(debt.isZero()).isTrue();
    }

    @Test
    void shouldDetectNonZeroDebt() {
        // When
        DebtEstimation debt = new DebtEstimation(1.0, 500.0, 25.0);

        // Then
        assertThat(debt.isZero()).isFalse();
    }

    @Test
    void shouldDetectNonZero_whenOnlyTotalDaysIsNonZero() {
        // When
        DebtEstimation debt = new DebtEstimation(1.0, 0.0, 0.0);

        // Then
        assertThat(debt.isZero()).isFalse();
    }

    @Test
    void shouldDetectNonZero_whenOnlyTotalCostIsNonZero() {
        // When
        DebtEstimation debt = new DebtEstimation(0.0, 500.0, 0.0);

        // Then
        assertThat(debt.isZero()).isFalse();
    }

    @Test
    void shouldDetectNonZero_whenOnlyMonthlyInterestIsNonZero() {
        // When
        DebtEstimation debt = new DebtEstimation(0.0, 0.0, 25.0);

        // Then
        assertThat(debt.isZero()).isFalse();
    }

    @Test
    void shouldRejectNegativeTotalDays() {
        // When/Then
        assertThatThrownBy(() -> new DebtEstimation(-1.0, 500.0, 25.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("totalDays cannot be negative");
    }

    @Test
    void shouldRejectNegativeTotalCost() {
        // When/Then
        assertThatThrownBy(() -> new DebtEstimation(1.0, -500.0, 25.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("totalCost cannot be negative");
    }

    @Test
    void shouldRejectNegativeMonthlyInterest() {
        // When/Then
        assertThatThrownBy(() -> new DebtEstimation(1.0, 500.0, -25.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("monthlyInterest cannot be negative");
    }

    @Test
    void shouldSupportRecordEquality() {
        // Given
        DebtEstimation debt1 = new DebtEstimation(10.0, 5000.0, 250.0);
        DebtEstimation debt2 = new DebtEstimation(10.0, 5000.0, 250.0);
        DebtEstimation debt3 = new DebtEstimation(20.0, 10000.0, 500.0);

        // Then
        assertThat(debt1).isEqualTo(debt2);
        assertThat(debt1).isNotEqualTo(debt3);
        assertThat(debt1.hashCode()).isEqualTo(debt2.hashCode());
    }

    @Test
    void shouldHandleLargeValues() {
        // When
        DebtEstimation debt = new DebtEstimation(1000.0, 500000.0, 25000.0);

        // Then
        assertThat(debt.totalDays()).isEqualTo(1000.0);
        assertThat(debt.totalCost()).isEqualTo(500000.0);
        assertThat(debt.monthlyInterest()).isEqualTo(25000.0);
        assertThat(debt.isZero()).isFalse();
    }

    @Test
    void shouldHandleDecimalValues() {
        // When
        DebtEstimation debt = new DebtEstimation(3.5, 1750.0, 87.5);

        // Then
        assertThat(debt.totalDays()).isEqualTo(3.5);
        assertThat(debt.totalCost()).isEqualTo(1750.0);
        assertThat(debt.monthlyInterest()).isEqualTo(87.5);
    }
}
