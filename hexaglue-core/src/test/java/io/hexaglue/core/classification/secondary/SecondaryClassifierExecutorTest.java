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

package io.hexaglue.core.classification.secondary;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.spi.classification.CertaintyLevel;
import io.hexaglue.spi.classification.ClassificationContext;
import io.hexaglue.spi.classification.ClassificationException;
import io.hexaglue.spi.classification.ClassificationStrategy;
import io.hexaglue.spi.classification.HexaglueClassifier;
import io.hexaglue.spi.classification.PrimaryClassificationResult;
import io.hexaglue.spi.classification.SecondaryClassificationResult;
import io.hexaglue.spi.core.TypeInfo;
import io.hexaglue.spi.core.TypeKind;
import io.hexaglue.spi.ir.DomainKind;
import io.hexaglue.spi.plugin.DiagnosticReporter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SecondaryClassifierExecutorTest {

    private SecondaryClassifierExecutor executor;
    private TestDiagnosticReporter diagnostics;

    @BeforeEach
    void setUp() {
        diagnostics = new TestDiagnosticReporter();
        executor = new SecondaryClassifierExecutor(diagnostics);
    }

    @AfterEach
    void tearDown() {
        executor.shutdown();
    }

    // ========== SUCCESS SCENARIOS ==========

    @Test
    @DisplayName("Should return result when classifier succeeds within timeout")
    void successfulClassification() {
        // Given
        HexaglueClassifier classifier = new SuccessfulClassifier();
        TypeInfo type = createTestType("Order");
        ClassificationContext context = createEmptyContext();

        // When
        Optional<SecondaryClassificationResult> result =
                executor.executeWithTimeout(classifier, type, context, Optional.empty());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().kind()).isEqualTo(DomainKind.AGGREGATE_ROOT);
        assertThat(diagnostics.getInfoMessages()).anyMatch(m -> m.contains("succeeded"));
    }

    @Test
    @DisplayName("Should return empty when classifier returns null (use primary)")
    void classifierReturnsNull_usePrimary() {
        // Given
        HexaglueClassifier classifier = new NullReturningClassifier();
        TypeInfo type = createTestType("Order");

        // When
        Optional<SecondaryClassificationResult> result =
                executor.executeWithTimeout(classifier, type, createEmptyContext(), Optional.empty());

        // Then
        assertThat(result).isEmpty();
    }

    // ========== TIMEOUT SCENARIOS ==========

    @Test
    @DisplayName("Should timeout and return empty when classifier exceeds timeout")
    void timeoutScenario() {
        // Given
        HexaglueClassifier classifier = new SlowClassifier(Duration.ofMillis(100));
        TypeInfo type = createTestType("Order");

        // When
        Optional<SecondaryClassificationResult> result =
                executor.executeWithTimeout(classifier, type, createEmptyContext(), Optional.empty());

        // Then
        assertThat(result).isEmpty();
        assertThat(diagnostics.getWarnMessages()).anyMatch(m -> m.contains("timed out"));
    }

    @Test
    @DisplayName("Should cancel execution when timeout occurs")
    void timeoutCancelsExecution() throws InterruptedException {
        // Given
        AtomicBoolean wasInterrupted = new AtomicBoolean(false);
        HexaglueClassifier classifier = new InterruptibleSlowClassifier(wasInterrupted);
        TypeInfo type = createTestType("Order");

        // When
        executor.executeWithTimeout(classifier, type, createEmptyContext(), Optional.empty());
        Thread.sleep(200); // Give time for interrupt to propagate

        // Then
        assertThat(wasInterrupted.get()).isTrue();
    }

    // ========== ERROR SCENARIOS ==========

    @Test
    @DisplayName("Should return empty and log error when classifier throws exception")
    void exceptionScenario() {
        // Given
        HexaglueClassifier classifier = new ThrowingClassifier();
        TypeInfo type = createTestType("Order");

        // When
        Optional<SecondaryClassificationResult> result =
                executor.executeWithTimeout(classifier, type, createEmptyContext(), Optional.empty());

        // Then
        assertThat(result).isEmpty();
        assertThat(diagnostics.getErrorMessages()).anyMatch(m -> m.contains("failed"));
    }

    @Test
    @DisplayName("Should handle ClassificationException gracefully")
    void classificationExceptionScenario() {
        // Given
        HexaglueClassifier classifier = new ClassificationExceptionClassifier();
        TypeInfo type = createTestType("Order");

        // When
        Optional<SecondaryClassificationResult> result =
                executor.executeWithTimeout(classifier, type, createEmptyContext(), Optional.empty());

        // Then
        assertThat(result).isEmpty();
        assertThat(diagnostics.getErrorMessages()).anyMatch(m -> m.contains("Classification failed"));
    }

    // ========== EDGE CASES ==========

    @Test
    @DisplayName("Should respect custom timeout from classifier")
    void customTimeoutRespected() {
        // Given - classifier with 500ms timeout but takes 200ms
        HexaglueClassifier classifier = new CustomTimeoutClassifier(Duration.ofMillis(500), Duration.ofMillis(200));
        TypeInfo type = createTestType("Order");

        // When
        Optional<SecondaryClassificationResult> result =
                executor.executeWithTimeout(classifier, type, createEmptyContext(), Optional.empty());

        // Then - should succeed because 200ms < 500ms timeout
        assertThat(result).isPresent();
    }

    @Test
    @DisplayName("Should pass primary result to classifier")
    void primaryResultPassedToClassifier() {
        // Given
        PrimaryResultCapturingClassifier classifier = new PrimaryResultCapturingClassifier();
        TypeInfo type = createTestType("Order");
        PrimaryClassificationResult primary = new PrimaryClassificationResult(
                "Order",
                DomainKind.ENTITY,
                CertaintyLevel.UNCERTAIN,
                ClassificationStrategy.WEIGHTED,
                "test",
                List.of());

        // When
        executor.executeWithTimeout(classifier, type, createEmptyContext(), Optional.of(primary));

        // Then
        assertThat(classifier.getCapturedPrimary()).isPresent();
        assertThat(classifier.getCapturedPrimary().get().kind()).isEqualTo(DomainKind.ENTITY);
    }

    // ========== HELPER METHODS ==========

    private TypeInfo createTestType(String name) {
        return new TypeInfo("com.example." + name, name, "com.example", TypeKind.CLASS);
    }

    private ClassificationContext createEmptyContext() {
        return new ClassificationContext(Map.of(), Set.of());
    }

    // ========== TEST CLASSIFIERS ==========

    static class SuccessfulClassifier implements HexaglueClassifier {
        @Override
        public String id() {
            return "test.successful";
        }

        @Override
        public SecondaryClassificationResult classify(
                TypeInfo type, ClassificationContext context, Optional<PrimaryClassificationResult> primary) {
            return new SecondaryClassificationResult(
                    DomainKind.AGGREGATE_ROOT,
                    CertaintyLevel.INFERRED,
                    ClassificationStrategy.WEIGHTED,
                    "Test success",
                    List.of());
        }
    }

    static class NullReturningClassifier implements HexaglueClassifier {
        @Override
        public String id() {
            return "test.null";
        }

        @Override
        public SecondaryClassificationResult classify(
                TypeInfo type, ClassificationContext context, Optional<PrimaryClassificationResult> primary) {
            return null; // Signal to use primary
        }
    }

    static class SlowClassifier implements HexaglueClassifier {
        private final Duration classifierTimeout;

        SlowClassifier(Duration timeout) {
            this.classifierTimeout = timeout;
        }

        @Override
        public String id() {
            return "test.slow";
        }

        @Override
        public Duration timeout() {
            return classifierTimeout;
        }

        @Override
        public SecondaryClassificationResult classify(
                TypeInfo type, ClassificationContext context, Optional<PrimaryClassificationResult> primary) {
            try {
                Thread.sleep(5000); // Sleep way longer than timeout
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return SecondaryClassificationResult.unclassified();
        }
    }

    static class InterruptibleSlowClassifier implements HexaglueClassifier {
        private final AtomicBoolean wasInterrupted;

        InterruptibleSlowClassifier(AtomicBoolean wasInterrupted) {
            this.wasInterrupted = wasInterrupted;
        }

        @Override
        public String id() {
            return "test.interruptible";
        }

        @Override
        public Duration timeout() {
            return Duration.ofMillis(50);
        }

        @Override
        public SecondaryClassificationResult classify(
                TypeInfo type, ClassificationContext context, Optional<PrimaryClassificationResult> primary) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                wasInterrupted.set(true);
                Thread.currentThread().interrupt();
            }
            return null;
        }
    }

    static class ThrowingClassifier implements HexaglueClassifier {
        @Override
        public String id() {
            return "test.throwing";
        }

        @Override
        public SecondaryClassificationResult classify(
                TypeInfo type, ClassificationContext context, Optional<PrimaryClassificationResult> primary) {
            throw new RuntimeException("Unexpected error");
        }
    }

    static class ClassificationExceptionClassifier implements HexaglueClassifier {
        @Override
        public String id() {
            return "test.classification-exception";
        }

        @Override
        public SecondaryClassificationResult classify(
                TypeInfo type, ClassificationContext context, Optional<PrimaryClassificationResult> primary) {
            throw new ClassificationException("Classification failed for " + type.simpleName());
        }
    }

    static class CustomTimeoutClassifier implements HexaglueClassifier {
        private final Duration timeout;
        private final Duration executionTime;

        CustomTimeoutClassifier(Duration timeout, Duration executionTime) {
            this.timeout = timeout;
            this.executionTime = executionTime;
        }

        @Override
        public String id() {
            return "test.custom-timeout";
        }

        @Override
        public Duration timeout() {
            return timeout;
        }

        @Override
        public SecondaryClassificationResult classify(
                TypeInfo type, ClassificationContext context, Optional<PrimaryClassificationResult> primary) {
            try {
                Thread.sleep(executionTime.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return new SecondaryClassificationResult(
                    DomainKind.VALUE_OBJECT,
                    CertaintyLevel.INFERRED,
                    ClassificationStrategy.WEIGHTED,
                    "Custom timeout test",
                    List.of());
        }
    }

    static class PrimaryResultCapturingClassifier implements HexaglueClassifier {
        private Optional<PrimaryClassificationResult> capturedPrimary;

        @Override
        public String id() {
            return "test.capturing";
        }

        @Override
        public SecondaryClassificationResult classify(
                TypeInfo type, ClassificationContext context, Optional<PrimaryClassificationResult> primary) {
            this.capturedPrimary = primary;
            return null;
        }

        public Optional<PrimaryClassificationResult> getCapturedPrimary() {
            return capturedPrimary;
        }
    }

    // ========== TEST DIAGNOSTIC REPORTER ==========

    static class TestDiagnosticReporter implements DiagnosticReporter {
        private final List<String> infoMessages = new ArrayList<>();
        private final List<String> warnMessages = new ArrayList<>();
        private final List<String> errorMessages = new ArrayList<>();

        @Override
        public void info(String message) {
            infoMessages.add(message);
        }

        @Override
        public void warn(String message) {
            warnMessages.add(message);
        }

        @Override
        public void error(String message) {
            errorMessages.add(message);
        }

        @Override
        public void error(String message, Throwable cause) {
            errorMessages.add(message);
        }

        public List<String> getInfoMessages() {
            return infoMessages;
        }

        public List<String> getWarnMessages() {
            return warnMessages;
        }

        public List<String> getErrorMessages() {
            return errorMessages;
        }
    }
}
