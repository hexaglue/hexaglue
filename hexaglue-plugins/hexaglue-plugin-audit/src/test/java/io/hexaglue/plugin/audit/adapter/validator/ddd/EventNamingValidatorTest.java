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

import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.spi.audit.CodeMetrics;
import io.hexaglue.spi.audit.CodeUnit;
import io.hexaglue.spi.audit.CodeUnitKind;
import io.hexaglue.spi.audit.Codebase;
import io.hexaglue.spi.audit.DocumentationInfo;
import io.hexaglue.spi.audit.LayerClassification;
import io.hexaglue.spi.audit.RoleClassification;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link EventNamingValidator}.
 *
 * <p>Validates that domain events are correctly checked for past-tense naming.
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
        Codebase codebase = createCodebaseWithEvent("OrderPlacedEvent");

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass when event is named with 'Created' suffix")
    void shouldPass_whenEventNamedWithCreatedSuffix() {
        // Given
        Codebase codebase = createCodebaseWithEvent("UserCreatedEvent");

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass when event is named with 'Deleted' suffix")
    void shouldPass_whenEventNamedWithDeletedSuffix() {
        // Given
        Codebase codebase = createCodebaseWithEvent("AccountDeletedEvent");

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass when event is named with 'Failed' suffix")
    void shouldPass_whenEventNamedWithFailedSuffix() {
        // Given
        Codebase codebase = createCodebaseWithEvent("PaymentFailedEvent");

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass when event is named with 'Completed' suffix")
    void shouldPass_whenEventNamedWithCompletedSuffix() {
        // Given
        Codebase codebase = createCodebaseWithEvent("OrderCompletedEvent");

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass when event is named with 'Sent' suffix")
    void shouldPass_whenEventNamedWithSentSuffix() {
        // Given
        Codebase codebase = createCodebaseWithEvent("EmailSentEvent");

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass when event is named with 'Received' suffix")
    void shouldPass_whenEventNamedWithReceivedSuffix() {
        // Given
        Codebase codebase = createCodebaseWithEvent("PaymentReceivedEvent");

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass when event name without Event suffix is in past tense")
    void shouldPass_whenEventNameWithoutSuffixInPastTense() {
        // Given: Event without "Event" suffix but still identified as event
        Codebase codebase = createCodebaseWithEvent("OrderPlacedNotification");

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail when event is named in present tense")
    void shouldFail_whenEventNamedInPresentTense() {
        // Given
        Codebase codebase = createCodebaseWithEvent("OrderPlaceEvent");

        // When
        List<Violation> violations = validator.validate(codebase, null);

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
        Codebase codebase = createCodebaseWithEvent("OrderEvent");

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).message()).contains("OrderEvent").contains("past tense");
    }

    @Test
    @DisplayName("Should fail when event is named with gerund form")
    void shouldFail_whenEventNamedWithGerund() {
        // Given
        Codebase codebase = createCodebaseWithEvent("OrderPlacingEvent");

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).message()).contains("OrderPlacingEvent").contains("past tense");
    }

    @Test
    @DisplayName("Should fail when event is named in infinitive form")
    void shouldFail_whenEventNamedInInfinitiveForm() {
        // Given
        Codebase codebase = createCodebaseWithEvent("OrderCreateEvent");

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).message()).contains("OrderCreateEvent").contains("past tense");
    }

    @Test
    @DisplayName("Should check multiple events with mixed naming")
    void shouldCheckMultipleEventsWithMixedNaming() {
        // Given: Mix of valid and invalid event names
        Codebase codebase = createCodebaseWithEvents(
                "OrderPlacedEvent", // Valid
                "UserCreatedEvent", // Valid
                "PaymentProcessEvent", // Invalid - present tense
                "OrderCancelledEvent", // Valid
                "EmailSendEvent" // Invalid - infinitive
                );

        // When
        List<Violation> violations = validator.validate(codebase, null);

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
        // Given: Codebase with only regular value objects (no events)
        Codebase codebase = createCodebaseWithValueObject("Address");

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass when codebase is empty")
    void shouldPass_whenEmptyCodebase() {
        // Given: Empty codebase
        Codebase codebase = new Codebase("test", BASE_PACKAGE, List.of(), Map.of());

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should ignore non-event value objects")
    void shouldIgnore_nonEventValueObjects() {
        // Given: Regular value objects without event naming
        Codebase codebase = createCodebaseWithValueObjects("Address", "Money", "EmailAddress", "PhoneNumber");

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then: Should not report violations for regular value objects
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should only check domain layer events")
    void shouldOnlyCheckDomainLayerEvents() {
        // Given: Events in different layers
        CodeUnit domainEvent = createEvent("OrderPlaceEvent", LayerClassification.DOMAIN);
        CodeUnit appEvent = createEvent("OrderPlaceEvent", LayerClassification.APPLICATION);
        Codebase codebase = new Codebase("test", BASE_PACKAGE, List.of(domainEvent, appEvent), Map.of());

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then: Should only check domain layer event
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).affectedTypes()).contains(BASE_PACKAGE + ".OrderPlaceEvent");
    }

    @Test
    @DisplayName("Should provide structural evidence")
    void shouldProvideStructuralEvidence() {
        // Given
        Codebase codebase = createCodebaseWithEvent("OrderPlaceEvent");

        // When
        List<Violation> violations = validator.validate(codebase, null);

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

    // === Helper Methods ===

    /**
     * Creates a codebase with a single event.
     */
    private Codebase createCodebaseWithEvent(String eventName) {
        CodeUnit event = createEvent(eventName, LayerClassification.DOMAIN);
        return new Codebase("test", BASE_PACKAGE, List.of(event), Map.of());
    }

    /**
     * Creates a codebase with multiple events.
     */
    private Codebase createCodebaseWithEvents(String... eventNames) {
        List<CodeUnit> events = List.of(eventNames).stream()
                .map(name -> createEvent(name, LayerClassification.DOMAIN))
                .toList();
        return new Codebase("test", BASE_PACKAGE, events, Map.of());
    }

    /**
     * Creates a codebase with a single value object (not an event).
     */
    private Codebase createCodebaseWithValueObject(String name) {
        CodeUnit valueObject = createValueObject(name);
        return new Codebase("test", BASE_PACKAGE, List.of(valueObject), Map.of());
    }

    /**
     * Creates a codebase with multiple value objects (not events).
     */
    private Codebase createCodebaseWithValueObjects(String... names) {
        List<CodeUnit> valueObjects =
                List.of(names).stream().map(this::createValueObject).toList();
        return new Codebase("test", BASE_PACKAGE, valueObjects, Map.of());
    }

    /**
     * Creates an event code unit.
     */
    private CodeUnit createEvent(String simpleName, LayerClassification layer) {
        String qualifiedName = BASE_PACKAGE + "." + simpleName;
        return new CodeUnit(
                qualifiedName,
                CodeUnitKind.CLASS,
                layer,
                RoleClassification.VALUE_OBJECT,
                List.of(),
                List.of(),
                new CodeMetrics(0, 0, 0, 0, 100.0),
                new DocumentationInfo(false, 0, List.of()));
    }

    /**
     * Creates a regular value object code unit (not an event).
     */
    private CodeUnit createValueObject(String simpleName) {
        String qualifiedName = BASE_PACKAGE + "." + simpleName;
        return new CodeUnit(
                qualifiedName,
                CodeUnitKind.CLASS,
                LayerClassification.DOMAIN,
                RoleClassification.VALUE_OBJECT,
                List.of(),
                List.of(),
                new CodeMetrics(0, 0, 0, 0, 100.0),
                new DocumentationInfo(false, 0, List.of()));
    }
}
