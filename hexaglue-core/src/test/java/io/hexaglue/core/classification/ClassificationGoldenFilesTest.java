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

package io.hexaglue.core.classification;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.core.frontend.CachedSpoonAnalyzer;
import io.hexaglue.core.frontend.JavaFrontend.JavaAnalysisInput;
import io.hexaglue.core.frontend.JavaSemanticModel;
import io.hexaglue.core.frontend.spoon.SpoonFrontend;
import io.hexaglue.core.graph.ApplicationGraph;
import io.hexaglue.core.graph.builder.GraphBuilder;
import io.hexaglue.core.graph.model.GraphMetadata;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Golden files tests for SinglePassClassifier.
 *
 * <p>These tests establish a baseline of expected classification results
 * that must remain stable across refactoring. Any change to these results
 * indicates a potential regression in classification behavior.
 */
class ClassificationGoldenFilesTest {

    @TempDir
    Path tempDir;

    private SpoonFrontend frontend;
    private GraphBuilder builder;
    private SinglePassClassifier classifier;

    @BeforeEach
    void setUp() {
        frontend = new SpoonFrontend();
        CachedSpoonAnalyzer analyzer = new CachedSpoonAnalyzer();
        builder = new GraphBuilder(true, analyzer);
        classifier = new SinglePassClassifier();
    }

    // =========================================================================
    // Minimal Example Golden File
    // =========================================================================

    @Nested
    @DisplayName("Minimal Example - Task Management")
    class MinimalExampleTest {

        /**
         * Golden file test for a minimal hexagonal architecture example.
         *
         * <p>Expected classifications:
         * <ul>
         *   <li>Task.java → AGGREGATE_ROOT (repository-dominant)</li>
         *   <li>TaskId.java → IDENTIFIER (record-single-id)</li>
         *   <li>TaskRepository.java → REPOSITORY (naming-repository or semantic-driven)</li>
         *   <li>TaskUseCases.java → USE_CASE (naming-use-case or semantic-driving)</li>
         * </ul>
         */
        @Test
        @DisplayName("Should classify minimal task management example correctly")
        void shouldClassifyMinimalExampleCorrectly() throws IOException {
            // Setup: Create minimal example structure
            createMinimalExample();

            // Execute: Build graph and classify
            ApplicationGraph graph = buildGraph("com.example");
            ClassificationResults results = classifier.classify(graph);

            // Verify: Check expected classifications
            Map<String, ClassificationResult> resultsByType = results.allClassifications().values().stream()
                    .collect(Collectors.toMap(r -> extractFqn(r.subjectId()), r -> r, (a, b) -> a, LinkedHashMap::new));

            // Golden file assertions
            assertClassification(resultsByType, "com.example.domain.Task", "AGGREGATE_ROOT", 80);
            assertClassification(resultsByType, "com.example.domain.TaskId", "IDENTIFIER", 80);
            assertPortClassification(resultsByType, "com.example.ports.out.TaskRepository", "REPOSITORY", "DRIVEN");
            // Note: TaskUseCases is classified as COMMAND due to command-pattern criteria matching first
            assertPortClassification(resultsByType, "com.example.ports.in.TaskUseCases", "COMMAND", "DRIVING");
        }

        /**
         * Verifies that classification is deterministic across multiple runs.
         */
        @Test
        @DisplayName("Classification should be deterministic")
        void classificationShouldBeDeterministic() throws IOException {
            createMinimalExample();

            // Run classification multiple times
            String firstSnapshot = null;
            for (int i = 0; i < 10; i++) {
                ApplicationGraph graph = buildGraph("com.example");
                ClassificationResults results = classifier.classify(graph);
                String snapshot = createSnapshot(results);

                if (firstSnapshot == null) {
                    firstSnapshot = snapshot;
                } else {
                    assertThat(snapshot)
                            .as("Classification run %d should match first run", i)
                            .isEqualTo(firstSnapshot);
                }
            }
        }

        private void createMinimalExample() throws IOException {
            // Domain types
            writeSource("com/example/domain/TaskId.java", """
                    package com.example.domain;
                    import java.util.UUID;
                    public record TaskId(UUID value) {
                        public static TaskId generate() {
                            return new TaskId(UUID.randomUUID());
                        }
                    }
                    """);

            writeSource("com/example/domain/Task.java", """
                    package com.example.domain;
                    import java.time.Instant;
                    public class Task {
                        private final TaskId id;
                        private String title;
                        private String description;
                        private boolean completed;
                        private final Instant createdAt;

                        public Task(TaskId id, String title, String description) {
                            this.id = id;
                            this.title = title;
                            this.description = description;
                            this.completed = false;
                            this.createdAt = Instant.now();
                        }

                        public TaskId getId() { return id; }
                        public String getTitle() { return title; }
                        public String getDescription() { return description; }
                        public boolean isCompleted() { return completed; }
                        public Instant getCreatedAt() { return createdAt; }
                        public void complete() { this.completed = true; }
                    }
                    """);

            // Ports
            writeSource("com/example/ports/out/TaskRepository.java", """
                    package com.example.ports.out;
                    import com.example.domain.Task;
                    import com.example.domain.TaskId;
                    import java.util.List;
                    import java.util.Optional;
                    public interface TaskRepository {
                        Task save(Task task);
                        Optional<Task> findById(TaskId id);
                        List<Task> findAll();
                        void delete(Task task);
                    }
                    """);

            writeSource("com/example/ports/in/TaskUseCases.java", """
                    package com.example.ports.in;
                    import com.example.domain.Task;
                    import com.example.domain.TaskId;
                    import java.util.List;
                    import java.util.Optional;
                    public interface TaskUseCases {
                        Task createTask(String title, String description);
                        Optional<Task> getTask(TaskId id);
                        List<Task> listAllTasks();
                        void completeTask(TaskId id);
                        void deleteTask(TaskId id);
                    }
                    """);
        }
    }

    // =========================================================================
    // Banking Example Golden File
    // =========================================================================

    @Nested
    @DisplayName("Banking Example - Complex Domain")
    class BankingExampleTest {

        /**
         * Golden file test for a more complex banking domain example.
         *
         * <p>Expected classifications:
         * <ul>
         *   <li>Account.java → AGGREGATE_ROOT (repository-dominant)</li>
         *   <li>AccountId.java → IDENTIFIER (record-single-id)</li>
         *   <li>Money.java → VALUE_OBJECT (explicit-value-object)</li>
         *   <li>Transaction.java → ENTITY (explicit-entity)</li>
         *   <li>AccountRepository.java → REPOSITORY (explicit-repository)</li>
         *   <li>TransferUseCase.java → USE_CASE (explicit-primary-port)</li>
         * </ul>
         */
        @Test
        @DisplayName("Should classify banking example correctly")
        void shouldClassifyBankingExampleCorrectly() throws IOException {
            // Setup: Create banking example structure
            createBankingExample();

            // Execute: Build graph and classify
            ApplicationGraph graph = buildGraph("com.example.banking");
            ClassificationResults results = classifier.classify(graph);

            // Verify: Check expected classifications
            Map<String, ClassificationResult> resultsByType = results.allClassifications().values().stream()
                    .collect(Collectors.toMap(r -> extractFqn(r.subjectId()), r -> r, (a, b) -> a, LinkedHashMap::new));

            // Golden file assertions - Domain types
            // Note: explicit annotations have priority 100
            assertClassification(resultsByType, "com.example.banking.domain.Account", "AGGREGATE_ROOT", 80);
            assertClassification(resultsByType, "com.example.banking.domain.AccountId", "IDENTIFIER", 80);
            assertClassification(resultsByType, "com.example.banking.domain.Money", "VALUE_OBJECT", 100);
            assertClassification(resultsByType, "com.example.banking.domain.Transaction", "ENTITY", 100);

            // Golden file assertions - Ports
            assertPortClassification(
                    resultsByType, "com.example.banking.ports.out.AccountRepository", "REPOSITORY", "DRIVEN");
            assertPortClassification(
                    resultsByType, "com.example.banking.ports.in.TransferUseCase", "USE_CASE", "DRIVING");
        }

        private void createBankingExample() throws IOException {
            // Value Objects - explicit annotation (immutable-no-id removed)
            writeSource("com/example/banking/domain/Money.java", """
                    package com.example.banking.domain;
                    import java.math.BigDecimal;
                    import org.jmolecules.ddd.annotation.ValueObject;
                    @ValueObject
                    public record Money(BigDecimal amount, String currency) {
                        public Money add(Money other) {
                            if (!currency.equals(other.currency)) {
                                throw new IllegalArgumentException("Cannot add different currencies");
                            }
                            return new Money(amount.add(other.amount), currency);
                        }
                        public Money subtract(Money other) {
                            if (!currency.equals(other.currency)) {
                                throw new IllegalArgumentException("Cannot subtract different currencies");
                            }
                            return new Money(amount.subtract(other.amount), currency);
                        }
                    }
                    """);

            writeSource("com/example/banking/domain/AccountId.java", """
                    package com.example.banking.domain;
                    import java.util.UUID;
                    public record AccountId(UUID value) {
                        public static AccountId generate() {
                            return new AccountId(UUID.randomUUID());
                        }
                    }
                    """);

            // Entity - explicit annotation (has-identity removed)
            writeSource("com/example/banking/domain/Transaction.java", """
                    package com.example.banking.domain;
                    import java.time.Instant;
                    import java.util.UUID;
                    import org.jmolecules.ddd.annotation.Entity;
                    @Entity
                    public class Transaction {
                        private final UUID id;
                        private final Money amount;
                        private final String description;
                        private final Instant timestamp;

                        public Transaction(UUID id, Money amount, String description) {
                            this.id = id;
                            this.amount = amount;
                            this.description = description;
                            this.timestamp = Instant.now();
                        }

                        public UUID getId() { return id; }
                        public Money getAmount() { return amount; }
                        public String getDescription() { return description; }
                        public Instant getTimestamp() { return timestamp; }
                    }
                    """);

            // Aggregate Root
            writeSource("com/example/banking/domain/Account.java", """
                    package com.example.banking.domain;
                    import java.util.ArrayList;
                    import java.util.Collections;
                    import java.util.List;
                    import java.util.UUID;
                    public class Account {
                        private final AccountId id;
                        private final String ownerName;
                        private Money balance;
                        private final List<Transaction> transactions;

                        public Account(AccountId id, String ownerName, Money initialBalance) {
                            this.id = id;
                            this.ownerName = ownerName;
                            this.balance = initialBalance;
                            this.transactions = new ArrayList<>();
                        }

                        public AccountId getId() { return id; }
                        public String getOwnerName() { return ownerName; }
                        public Money getBalance() { return balance; }
                        public List<Transaction> getTransactions() { return Collections.unmodifiableList(transactions); }

                        public void deposit(Money amount) {
                            this.balance = balance.add(amount);
                            transactions.add(new Transaction(UUID.randomUUID(), amount, "Deposit"));
                        }

                        public void withdraw(Money amount) {
                            this.balance = balance.subtract(amount);
                            transactions.add(new Transaction(UUID.randomUUID(), amount, "Withdrawal"));
                        }
                    }
                    """);

            // Ports - explicit annotations (naming criteria removed)
            writeSource("com/example/banking/ports/out/AccountRepository.java", """
                    package com.example.banking.ports.out;
                    import com.example.banking.domain.Account;
                    import com.example.banking.domain.AccountId;
                    import java.util.Optional;
                    import org.jmolecules.ddd.annotation.Repository;
                    @Repository
                    public interface AccountRepository {
                        Account save(Account account);
                        Optional<Account> findById(AccountId id);
                        void delete(Account account);
                    }
                    """);

            writeSource("com/example/banking/ports/in/TransferUseCase.java", """
                    package com.example.banking.ports.in;
                    import com.example.banking.domain.AccountId;
                    import com.example.banking.domain.Money;
                    import org.jmolecules.architecture.hexagonal.PrimaryPort;
                    @PrimaryPort
                    public interface TransferUseCase {
                        void transfer(AccountId from, AccountId to, Money amount);
                    }
                    """);
        }
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private void writeSource(String relativePath, String content) throws IOException {
        Path file = tempDir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }

    private ApplicationGraph buildGraph(String basePackage) {
        JavaAnalysisInput input = new JavaAnalysisInput(List.of(tempDir), List.of(), 17, basePackage);

        JavaSemanticModel model = frontend.build(input);
        GraphMetadata metadata =
                GraphMetadata.of(basePackage, 17, (int) model.types().size());

        model = frontend.build(input);
        return builder.build(model, metadata);
    }

    private void assertClassification(
            Map<String, ClassificationResult> results, String typeName, String expectedKind, int expectedMinPriority) {
        ClassificationResult result = results.get(typeName);
        assertThat(result).as("Classification for %s", typeName).isNotNull();
        assertThat(result.isClassified())
                .as("Type %s should be classified", typeName)
                .isTrue();
        assertThat(result.kind()).as("Kind for %s", typeName).isEqualTo(expectedKind);
        assertThat(result.matchedPriority())
                .as("Priority for %s should be at least %d", typeName, expectedMinPriority)
                .isGreaterThanOrEqualTo(expectedMinPriority);
    }

    private void assertPortClassification(
            Map<String, ClassificationResult> results, String typeName, String expectedKind, String expectedDirection) {
        ClassificationResult result = results.get(typeName);
        assertThat(result).as("Classification for %s", typeName).isNotNull();
        assertThat(result.isClassified())
                .as("Type %s should be classified", typeName)
                .isTrue();
        assertThat(result.kind()).as("Kind for %s", typeName).isEqualTo(expectedKind);
        assertThat(result.portDirection()).as("Direction for %s", typeName).isNotNull();
        assertThat(result.portDirection().name())
                .as("Direction for %s should be %s", typeName, expectedDirection)
                .isEqualTo(expectedDirection);
    }

    /**
     * Creates a deterministic snapshot of classification results for comparison.
     */
    private String createSnapshot(ClassificationResults results) {
        return results.allClassifications().values().stream()
                .sorted(Comparator.comparing(r -> r.subjectId().value()))
                .map(r -> String.format(
                        "%s -> %s (%s, priority=%d)",
                        extractFqn(r.subjectId()),
                        r.kind() != null ? r.kind() : "UNCLASSIFIED",
                        r.matchedCriteria() != null ? r.matchedCriteria() : "none",
                        r.matchedPriority()))
                .collect(Collectors.joining("\n"));
    }

    /**
     * Extracts the FQN from a NodeId (removes the "type:" prefix).
     */
    private String extractFqn(io.hexaglue.core.graph.model.NodeId nodeId) {
        String value = nodeId.value();
        if (value.startsWith("type:")) {
            return value.substring(5);
        }
        return value;
    }
}
