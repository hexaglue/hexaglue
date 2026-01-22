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

package io.hexaglue.plugin.audit.adapter.validator.ddd;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.plugin.audit.util.TestCodebaseBuilder;
import io.hexaglue.plugin.audit.util.TestModelBuilder;
import io.hexaglue.spi.audit.Codebase;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link EventNamingValidator}.
 *
 * <p>Validates that domain events are correctly checked for past-tense naming
 * using the v5 ArchType API.
 *
 * @since 5.0.0 Migrated to v5 ArchType API
 */
class EventNamingValidatorTest {

    private static final String BASE_PACKAGE = "com.example.domain";

    private EventNamingValidator validator;

    @BeforeEach
    void setUp() {
        validator = new EventNamingValidator();
    }

    @Test
    @DisplayName("Should pass when event is named with 'ed' suffix")
    void shouldPass_whenEventNamedWithEdSuffix() {
        // Given
        ArchitecturalModel model = new TestModelBuilder()
                .addDomainEvent(BASE_PACKAGE + ".OrderPlacedEvent")
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass when event is named with 'Created' suffix")
    void shouldPass_whenEventNamedWithCreatedSuffix() {
        // Given
        ArchitecturalModel model = new TestModelBuilder()
                .addDomainEvent(BASE_PACKAGE + ".UserCreatedEvent")
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass when event is named with 'Deleted' suffix")
    void shouldPass_whenEventNamedWithDeletedSuffix() {
        // Given
        ArchitecturalModel model = new TestModelBuilder()
                .addDomainEvent(BASE_PACKAGE + ".AccountDeletedEvent")
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass when event is named with 'Failed' suffix")
    void shouldPass_whenEventNamedWithFailedSuffix() {
        // Given
        ArchitecturalModel model = new TestModelBuilder()
                .addDomainEvent(BASE_PACKAGE + ".PaymentFailedEvent")
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass when event is named with 'Completed' suffix")
    void shouldPass_whenEventNamedWithCompletedSuffix() {
        // Given
        ArchitecturalModel model = new TestModelBuilder()
                .addDomainEvent(BASE_PACKAGE + ".OrderCompletedEvent")
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass when event is named with 'Sent' suffix")
    void shouldPass_whenEventNamedWithSentSuffix() {
        // Given
        ArchitecturalModel model = new TestModelBuilder()
                .addDomainEvent(BASE_PACKAGE + ".EmailSentEvent")
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass when event is named with 'Received' suffix")
    void shouldPass_whenEventNamedWithReceivedSuffix() {
        // Given
        ArchitecturalModel model = new TestModelBuilder()
                .addDomainEvent(BASE_PACKAGE + ".PaymentReceivedEvent")
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass when event name without Event suffix is in past tense")
    void shouldPass_whenEventNameWithoutSuffixInPastTense() {
        // Given: Event without "Event" suffix but still identified as event
        ArchitecturalModel model = new TestModelBuilder()
                .addDomainEvent(BASE_PACKAGE + ".OrderPlacedNotification")
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail when event is named in present tense")
    void shouldFail_whenEventNamedInPresentTense() {
        // Given
        ArchitecturalModel model = new TestModelBuilder()
                .addDomainEvent(BASE_PACKAGE + ".OrderPlaceEvent")
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).constraintId().value()).isEqualTo("ddd:event-naming");
        assertThat(violations.get(0).severity()).isEqualTo(Severity.MINOR);
        assertThat(violations.get(0).message()).contains("OrderPlaceEvent").contains("past tense");
        assertThat(violations.get(0).affectedTypes()).contains(BASE_PACKAGE + ".OrderPlaceEvent");
    }

    @Test
    @DisplayName("Should fail when event is named as a noun")
    void shouldFail_whenEventNamedAsNoun() {
        // Given
        ArchitecturalModel model = new TestModelBuilder()
                .addDomainEvent(BASE_PACKAGE + ".OrderEvent")
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).message()).contains("OrderEvent").contains("past tense");
    }

    @Test
    @DisplayName("Should fail when event is named with gerund form")
    void shouldFail_whenEventNamedWithGerund() {
        // Given
        ArchitecturalModel model = new TestModelBuilder()
                .addDomainEvent(BASE_PACKAGE + ".OrderPlacingEvent")
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).message()).contains("OrderPlacingEvent").contains("past tense");
    }

    @Test
    @DisplayName("Should fail when event is named in infinitive form")
    void shouldFail_whenEventNamedInInfinitiveForm() {
        // Given
        ArchitecturalModel model = new TestModelBuilder()
                .addDomainEvent(BASE_PACKAGE + ".OrderCreateEvent")
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).message()).contains("OrderCreateEvent").contains("past tense");
    }

    @Test
    @DisplayName("Should check multiple events with mixed naming")
    void shouldCheckMultipleEventsWithMixedNaming() {
        // Given: Mix of valid and invalid event names
        ArchitecturalModel model = new TestModelBuilder()
                .addDomainEvent(BASE_PACKAGE + ".OrderPlacedEvent") // Valid
                .addDomainEvent(BASE_PACKAGE + ".UserCreatedEvent") // Valid
                .addDomainEvent(BASE_PACKAGE + ".PaymentProcessEvent") // Invalid - present tense
                .addDomainEvent(BASE_PACKAGE + ".OrderCancelledEvent") // Valid
                .addDomainEvent(BASE_PACKAGE + ".EmailSendEvent") // Invalid - infinitive
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then: Should find 2 violations
        assertThat(violations).hasSize(2);
        assertThat(violations)
                .extracting(v -> v.message())
                .anyMatch(msg -> msg.contains("PaymentProcessEvent"))
                .anyMatch(msg -> msg.contains("EmailSendEvent"));
    }

    @Test
    @DisplayName("Should pass when codebase has no events")
    void shouldPass_whenNoEvents() {
        // Given: Model with only value objects (no events)
        ArchitecturalModel model =
                new TestModelBuilder().addValueObject(BASE_PACKAGE + ".Address").build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass when codebase is empty")
    void shouldPass_whenEmptyCodebase() {
        // Given: Empty model
        ArchitecturalModel model = TestModelBuilder.emptyModel();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should provide structural evidence")
    void shouldProvideStructuralEvidence() {
        // Given
        ArchitecturalModel model = new TestModelBuilder()
                .addDomainEvent(BASE_PACKAGE + ".OrderPlaceEvent")
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).evidence()).isNotEmpty();
        assertThat(violations.get(0).evidence().get(0).description())
                .contains("Domain events")
                .contains("already occurred")
                .contains("past-tense");
    }

    @Test
    @DisplayName("Should return correct default severity")
    void shouldReturnCorrectDefaultSeverity() {
        // When/Then
        assertThat(validator.defaultSeverity()).isEqualTo(Severity.MINOR);
    }

    @Test
    @DisplayName("Should return correct constraint ID")
    void shouldReturnCorrectConstraintId() {
        // When/Then
        assertThat(validator.constraintId().value()).isEqualTo("ddd:event-naming");
    }
}
