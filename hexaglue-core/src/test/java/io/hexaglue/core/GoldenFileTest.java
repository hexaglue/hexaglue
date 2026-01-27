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

package io.hexaglue.core;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.model.AggregateRoot;
import io.hexaglue.arch.model.DomainType;
import io.hexaglue.arch.model.DrivenPort;
import io.hexaglue.arch.model.DrivingPort;
import io.hexaglue.arch.model.Field;
import io.hexaglue.arch.model.Identifier;
import io.hexaglue.arch.model.PortType;
import io.hexaglue.arch.model.TypeRegistry;
import io.hexaglue.arch.model.UnclassifiedType;
import io.hexaglue.arch.model.ValueObject;
import io.hexaglue.core.engine.EngineConfig;
import io.hexaglue.core.engine.EngineResult;
import io.hexaglue.core.engine.HexaGlueEngine;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Golden file tests for ArchitecturalModel non-regression.
 *
 * <p>Compares generated ArchitecturalModel JSON snapshot against expected golden files.
 * Any change to the classification behavior that modifies the model structure will
 * cause these tests to fail, signaling a potential regression.
 *
 * <p>This test covers multiple domains:
 * <ul>
 *   <li>Coffeeshop - Basic domain with implicit classification</li>
 *   <li>Banking - Complex domain with explicit annotations</li>
 *   <li>E-commerce - Multi-aggregate domain</li>
 * </ul>
 *
 * @since 5.0.0 Rewritten to use ArchitecturalModel instead of IrSnapshot
 */
class GoldenFileTest {

    private static final Path GOLDEN_DIR = Path.of("src/test/resources/golden");

    @TempDir
    Path tempDir;

    // =========================================================================
    // Coffeeshop Domain - Basic implicit classification
    // =========================================================================

    @Nested
    @DisplayName("Coffeeshop Domain")
    class CoffeeshopDomainTest {

        @BeforeEach
        void setUp() throws IOException {
            createCoffeeshopDomain();
        }

        @Test
        @DisplayName("Should match golden file")
        void shouldMatchGoldenFile() throws IOException {
            assertGoldenFile("com.coffeeshop", "coffeeshop-arch-model.json");
        }

        @Test
        @DisplayName("Classification should be deterministic")
        void classificationShouldBeDeterministic() {
            assertDeterministic("com.coffeeshop", 10);
        }

        @Test
        @DisplayName("Should classify domain types correctly")
        void shouldClassifyDomainTypesCorrectly() {
            EngineResult result = analyze("com.coffeeshop");
            TypeRegistry registry = result.model().typeRegistry().orElseThrow();

            assertThat(registry.all(AggregateRoot.class))
                    .extracting(t -> t.id().simpleName())
                    .contains("Order");

            assertThat(registry.all(Identifier.class))
                    .extracting(t -> t.id().simpleName())
                    .contains("OrderId");

            assertThat(registry.all(ValueObject.class))
                    .extracting(t -> t.id().simpleName())
                    .contains("LineItem");
        }
    }

    // =========================================================================
    // Banking Domain - Explicit annotations
    // =========================================================================

    @Nested
    @DisplayName("Banking Domain")
    class BankingDomainTest {

        @BeforeEach
        void setUp() throws IOException {
            createBankingDomain();
        }

        @Test
        @DisplayName("Should match golden file")
        void shouldMatchGoldenFile() throws IOException {
            assertGoldenFile("com.example.banking", "banking-arch-model.json");
        }

        @Test
        @DisplayName("Classification should be deterministic")
        void classificationShouldBeDeterministic() {
            assertDeterministic("com.example.banking", 10);
        }

        @Test
        @DisplayName("Should classify with explicit annotations")
        void shouldClassifyWithExplicitAnnotations() {
            EngineResult result = analyze("com.example.banking");
            TypeRegistry registry = result.model().typeRegistry().orElseThrow();

            // Explicit @Entity annotation
            assertThat(registry.all(io.hexaglue.arch.model.Entity.class))
                    .extracting(t -> t.id().simpleName())
                    .contains("Transaction");

            // Explicit @ValueObject annotation
            assertThat(registry.all(ValueObject.class))
                    .extracting(t -> t.id().simpleName())
                    .contains("Money");

            // Explicit @Repository annotation
            assertThat(registry.all(DrivenPort.class))
                    .extracting(t -> t.id().simpleName())
                    .contains("AccountRepository");
        }
    }

    // =========================================================================
    // E-commerce Domain - Multi-aggregate
    // =========================================================================

    @Nested
    @DisplayName("E-commerce Domain")
    class EcommerceDomainTest {

        @BeforeEach
        void setUp() throws IOException {
            createEcommerceDomain();
        }

        @Test
        @DisplayName("Should match golden file")
        void shouldMatchGoldenFile() throws IOException {
            assertGoldenFile("com.ecommerce", "ecommerce-arch-model.json");
        }

        @Test
        @DisplayName("Classification should be deterministic")
        void classificationShouldBeDeterministic() {
            assertDeterministic("com.ecommerce", 10);
        }

        @Test
        @DisplayName("Should classify multiple aggregates")
        void shouldClassifyMultipleAggregates() {
            EngineResult result = analyze("com.ecommerce");
            TypeRegistry registry = result.model().typeRegistry().orElseThrow();

            // Multiple aggregate roots
            assertThat(registry.all(AggregateRoot.class))
                    .extracting(t -> t.id().simpleName())
                    .containsExactlyInAnyOrder("Order", "Product", "Customer");

            // Multiple identifiers
            assertThat(registry.all(Identifier.class))
                    .extracting(t -> t.id().simpleName())
                    .containsExactlyInAnyOrder("OrderId", "ProductId", "CustomerId");
        }
    }

    // =========================================================================
    // Common Test Methods
    // =========================================================================

    private EngineResult analyze(String basePackage) {
        EngineConfig config = EngineConfig.minimal(tempDir, basePackage);
        HexaGlueEngine engine = HexaGlueEngine.create();
        return engine.analyze(config);
    }

    private void assertGoldenFile(String basePackage, String goldenFileName) throws IOException {
        EngineResult result = analyze(basePackage);

        assertThat(result.model()).isNotNull();
        assertThat(result.model().size()).isGreaterThan(0);

        String actualSnapshot = ArchModelSnapshotSerializer.serialize(result.model());

        Path goldenPath =
                Path.of(System.getProperty("user.dir")).resolve(GOLDEN_DIR).resolve(goldenFileName);
        if (Files.exists(goldenPath)) {
            String expectedSnapshot = Files.readString(goldenPath, StandardCharsets.UTF_8);
            assertThat(actualSnapshot)
                    .as("ArchitecturalModel snapshot should match golden file: %s", goldenFileName)
                    .isEqualTo(expectedSnapshot);
        } else {
            Files.createDirectories(goldenPath.getParent());
            Files.writeString(goldenPath, actualSnapshot, StandardCharsets.UTF_8);
            System.out.println("Golden file created: " + goldenPath);
            System.out.println("Please review and commit the golden file.");
        }
    }

    private void assertDeterministic(String basePackage, int runs) {
        String firstSnapshot = null;

        for (int i = 0; i < runs; i++) {
            EngineResult result = analyze(basePackage);
            String snapshot = ArchModelSnapshotSerializer.serialize(result.model());

            if (firstSnapshot == null) {
                firstSnapshot = snapshot;
            } else {
                assertThat(snapshot)
                        .as("Run %d should produce identical results", i)
                        .isEqualTo(firstSnapshot);
            }
        }
    }

    // =========================================================================
    // Test Domain Setup - Coffeeshop
    // =========================================================================

    private void createCoffeeshopDomain() throws IOException {
        writeSource("com/coffeeshop/domain/order/OrderId.java", """
                package com.coffeeshop.domain.order;
                import java.util.UUID;
                public record OrderId(UUID value) {}
                """);

        writeSource("com/coffeeshop/domain/order/Location.java", """
                package com.coffeeshop.domain.order;
                public enum Location { IN_STORE, TAKE_AWAY }
                """);

        writeSource("com/coffeeshop/domain/order/LineItem.java", """
                package com.coffeeshop.domain.order;
                import java.math.BigDecimal;
                import org.jmolecules.ddd.annotation.ValueObject;
                @ValueObject
                public record LineItem(String productName, int quantity, BigDecimal unitPrice) {}
                """);

        writeSource("com/coffeeshop/domain/order/Order.java", """
                package com.coffeeshop.domain.order;
                import java.util.List;
                public class Order {
                    private final OrderId id;
                    private final String customerName;
                    private final Location location;
                    private final List<LineItem> items;
                    public Order(OrderId id, String customerName, Location location, List<LineItem> items) {
                        this.id = id;
                        this.customerName = customerName;
                        this.location = location;
                        this.items = items;
                    }
                    public OrderId getId() { return id; }
                    public String getCustomerName() { return customerName; }
                    public Location getLocation() { return location; }
                    public List<LineItem> getItems() { return items; }
                }
                """);

        writeSource("com/coffeeshop/ports/in/OrderingCoffee.java", """
                package com.coffeeshop.ports.in;
                import com.coffeeshop.domain.order.*;
                import java.util.Optional;
                import org.jmolecules.architecture.hexagonal.PrimaryPort;
                @PrimaryPort
                public interface OrderingCoffee {
                    Order createOrder(String customerName, Location location);
                    Optional<Order> findOrder(OrderId id);
                }
                """);

        writeSource("com/coffeeshop/ports/out/OrderRepository.java", """
                package com.coffeeshop.ports.out;
                import com.coffeeshop.domain.order.*;
                import java.util.Optional;
                import org.jmolecules.ddd.annotation.Repository;
                @Repository
                public interface OrderRepository {
                    Order save(Order order);
                    Optional<Order> findById(OrderId id);
                }
                """);
    }

    private void writeSource(String relativePath, String content) throws IOException {
        Path filePath = tempDir.resolve(relativePath);
        Files.createDirectories(filePath.getParent());
        Files.writeString(filePath, content);
    }

    // =========================================================================
    // Test Domain Setup - Banking
    // =========================================================================

    private void createBankingDomain() throws IOException {
        // Value Object with explicit annotation
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
                }
                """);

        // Identifier
        writeSource("com/example/banking/domain/AccountId.java", """
                package com.example.banking.domain;
                import java.util.UUID;
                public record AccountId(UUID value) {
                    public static AccountId generate() {
                        return new AccountId(UUID.randomUUID());
                    }
                }
                """);

        // Entity with explicit annotation
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
                }
                """);

        // Repository with explicit annotation
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

        // Driving port with explicit annotation
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

    // =========================================================================
    // Test Domain Setup - E-commerce (Multi-aggregate)
    // =========================================================================

    private void createEcommerceDomain() throws IOException {
        // === Order Aggregate ===
        writeSource("com/ecommerce/order/OrderId.java", """
                package com.ecommerce.order;
                import java.util.UUID;
                public record OrderId(UUID value) {}
                """);

        writeSource("com/ecommerce/order/Order.java", """
                package com.ecommerce.order;
                import java.util.List;
                import java.time.Instant;
                public class Order {
                    private final OrderId id;
                    private final String customerId;
                    private final List<String> productIds;
                    private final Instant createdAt;
                    private String status;
                    public Order(OrderId id, String customerId, List<String> productIds) {
                        this.id = id;
                        this.customerId = customerId;
                        this.productIds = productIds;
                        this.createdAt = Instant.now();
                        this.status = "PENDING";
                    }
                    public OrderId getId() { return id; }
                    public String getCustomerId() { return customerId; }
                    public List<String> getProductIds() { return productIds; }
                    public Instant getCreatedAt() { return createdAt; }
                    public String getStatus() { return status; }
                    public void confirm() { this.status = "CONFIRMED"; }
                }
                """);

        writeSource("com/ecommerce/order/OrderRepository.java", """
                package com.ecommerce.order;
                import java.util.Optional;
                import org.jmolecules.ddd.annotation.Repository;
                @Repository
                public interface OrderRepository {
                    Order save(Order order);
                    Optional<Order> findById(OrderId id);
                }
                """);

        // === Product Aggregate ===
        writeSource("com/ecommerce/product/ProductId.java", """
                package com.ecommerce.product;
                import java.util.UUID;
                public record ProductId(UUID value) {}
                """);

        writeSource("com/ecommerce/product/Product.java", """
                package com.ecommerce.product;
                import java.math.BigDecimal;
                public class Product {
                    private final ProductId id;
                    private String name;
                    private String description;
                    private BigDecimal price;
                    private int stockQuantity;
                    public Product(ProductId id, String name, BigDecimal price) {
                        this.id = id;
                        this.name = name;
                        this.price = price;
                        this.stockQuantity = 0;
                    }
                    public ProductId getId() { return id; }
                    public String getName() { return name; }
                    public String getDescription() { return description; }
                    public BigDecimal getPrice() { return price; }
                    public int getStockQuantity() { return stockQuantity; }
                    public void addStock(int quantity) { this.stockQuantity += quantity; }
                }
                """);

        writeSource("com/ecommerce/product/ProductRepository.java", """
                package com.ecommerce.product;
                import java.util.Optional;
                import java.util.List;
                import org.jmolecules.ddd.annotation.Repository;
                @Repository
                public interface ProductRepository {
                    Product save(Product product);
                    Optional<Product> findById(ProductId id);
                    List<Product> findAll();
                }
                """);

        // === Customer Aggregate ===
        writeSource("com/ecommerce/customer/CustomerId.java", """
                package com.ecommerce.customer;
                import java.util.UUID;
                public record CustomerId(UUID value) {}
                """);

        writeSource("com/ecommerce/customer/Customer.java", """
                package com.ecommerce.customer;
                public class Customer {
                    private final CustomerId id;
                    private String name;
                    private String email;
                    public Customer(CustomerId id, String name, String email) {
                        this.id = id;
                        this.name = name;
                        this.email = email;
                    }
                    public CustomerId getId() { return id; }
                    public String getName() { return name; }
                    public String getEmail() { return email; }
                    public void updateEmail(String newEmail) { this.email = newEmail; }
                }
                """);

        writeSource("com/ecommerce/customer/CustomerRepository.java", """
                package com.ecommerce.customer;
                import java.util.Optional;
                import org.jmolecules.ddd.annotation.Repository;
                @Repository
                public interface CustomerRepository {
                    Customer save(Customer customer);
                    Optional<Customer> findById(CustomerId id);
                }
                """);
    }

    // =========================================================================
    // Snapshot Serializer
    // =========================================================================

    /**
     * Serializes an ArchitecturalModel to a deterministic JSON snapshot.
     *
     * <p>The output is sorted and formatted for stable comparison in golden file tests.
     */
    static final class ArchModelSnapshotSerializer {

        private ArchModelSnapshotSerializer() {}

        static String serialize(ArchitecturalModel model) {
            StringBuilder json = new StringBuilder();
            json.append("{\n");

            // Domain types
            json.append("  \"domain\": {\n");
            json.append("    \"types\": ");
            serializeDomainTypes(json, model);
            json.append("\n  },\n");

            // Ports
            json.append("  \"ports\": ");
            serializePorts(json, model);
            json.append(",\n");

            // Unclassified (if any)
            json.append("  \"unclassified\": ");
            serializeUnclassified(json, model);
            json.append("\n");

            json.append("}\n");
            return json.toString();
        }

        private static void serializeDomainTypes(StringBuilder json, ArchitecturalModel model) {
            TypeRegistry registry = model.typeRegistry().orElse(null);
            if (registry == null) {
                json.append("[]");
                return;
            }

            List<DomainType> domainTypes = registry.all(DomainType.class)
                    .sorted(Comparator.comparing(t -> t.id().qualifiedName()))
                    .toList();

            if (domainTypes.isEmpty()) {
                json.append("[]");
                return;
            }

            json.append("[\n");
            for (int i = 0; i < domainTypes.size(); i++) {
                DomainType type = domainTypes.get(i);
                json.append("      {\n");
                json.append("        \"qualifiedName\": ")
                        .append(quote(type.id().qualifiedName()))
                        .append(",\n");
                json.append("        \"simpleName\": ")
                        .append(quote(type.id().simpleName()))
                        .append(",\n");
                json.append("        \"kind\": ")
                        .append(quote(type.kind().name()))
                        .append(",\n");
                json.append("        \"confidence\": ")
                        .append(quote(type.classification().confidence().name()))
                        .append(",\n");
                json.append("        \"construct\": ")
                        .append(quote(type.structure().nature().name()))
                        .append(",\n");

                // Identity field for aggregates
                if (type instanceof AggregateRoot agg) {
                    json.append("        \"identity\": {\n");
                    json.append("          \"fieldName\": ")
                            .append(quote(agg.identityField().name()))
                            .append(",\n");
                    json.append("          \"typeName\": ")
                            .append(quote(agg.identityField().type().qualifiedName()))
                            .append("\n");
                    json.append("        },\n");
                }

                // Wrapped type for identifiers
                if (type instanceof Identifier id) {
                    json.append("        \"wrappedType\": ")
                            .append(quote(id.wrappedType().qualifiedName()))
                            .append(",\n");
                }

                // Properties (fields)
                json.append("        \"properties\": ");
                serializeFields(json, type.structure().fields());
                json.append("\n");

                json.append("      }");
                if (i < domainTypes.size() - 1) {
                    json.append(",");
                }
                json.append("\n");
            }
            json.append("    ]");
        }

        private static void serializePorts(StringBuilder json, ArchitecturalModel model) {
            TypeRegistry registry = model.typeRegistry().orElse(null);
            if (registry == null) {
                json.append("[]");
                return;
            }

            List<PortType> ports = registry.all(PortType.class)
                    .sorted(Comparator.comparing(t -> t.id().qualifiedName()))
                    .toList();

            if (ports.isEmpty()) {
                json.append("[]");
                return;
            }

            json.append("[\n");
            for (int i = 0; i < ports.size(); i++) {
                PortType port = ports.get(i);
                json.append("    {\n");
                json.append("      \"qualifiedName\": ")
                        .append(quote(port.id().qualifiedName()))
                        .append(",\n");
                json.append("      \"simpleName\": ")
                        .append(quote(port.id().simpleName()))
                        .append(",\n");

                if (port instanceof DrivingPort) {
                    json.append("      \"direction\": \"DRIVING\",\n");
                    json.append("      \"kind\": \"USE_CASE\",\n");
                } else if (port instanceof DrivenPort dp) {
                    json.append("      \"direction\": \"DRIVEN\",\n");
                    json.append("      \"kind\": ")
                            .append(quote(dp.portType().name()))
                            .append(",\n");
                }

                json.append("      \"confidence\": ")
                        .append(quote(port.classification().confidence().name()))
                        .append(",\n");

                // Methods
                json.append("      \"methods\": ");
                List<String> methods = port.structure().methods().stream()
                        .map(m -> m.name())
                        .sorted()
                        .toList();
                serializeStringList(json, methods);
                json.append("\n");

                json.append("    }");
                if (i < ports.size() - 1) {
                    json.append(",");
                }
                json.append("\n");
            }
            json.append("  ]");
        }

        private static void serializeUnclassified(StringBuilder json, ArchitecturalModel model) {
            TypeRegistry registry = model.typeRegistry().orElse(null);
            if (registry == null) {
                json.append("[]");
                return;
            }

            List<UnclassifiedType> unclassified = registry.all(UnclassifiedType.class)
                    .sorted(Comparator.comparing(t -> t.id().qualifiedName()))
                    .toList();

            if (unclassified.isEmpty()) {
                json.append("[]");
                return;
            }

            json.append("[\n");
            for (int i = 0; i < unclassified.size(); i++) {
                UnclassifiedType type = unclassified.get(i);
                json.append("    {\n");
                json.append("      \"qualifiedName\": ")
                        .append(quote(type.id().qualifiedName()))
                        .append(",\n");
                json.append("      \"simpleName\": ")
                        .append(quote(type.id().simpleName()))
                        .append(",\n");
                json.append("      \"category\": ")
                        .append(quote(type.category().name()))
                        .append("\n");
                json.append("    }");
                if (i < unclassified.size() - 1) {
                    json.append(",");
                }
                json.append("\n");
            }
            json.append("  ]");
        }

        private static void serializeFields(StringBuilder json, List<Field> fields) {
            List<Field> sortedFields =
                    fields.stream().sorted(Comparator.comparing(Field::name)).toList();

            if (sortedFields.isEmpty()) {
                json.append("[]");
                return;
            }

            json.append("[\n");
            for (int i = 0; i < sortedFields.size(); i++) {
                Field f = sortedFields.get(i);
                json.append("          {\n");
                json.append("            \"name\": ").append(quote(f.name())).append(",\n");
                json.append("            \"typeName\": ")
                        .append(quote(f.type().qualifiedName()))
                        .append(",\n");

                // Cardinality based on element type presence
                String cardinality = f.elementType().isPresent() ? "COLLECTION" : "SINGLE";
                json.append("            \"cardinality\": ")
                        .append(quote(cardinality))
                        .append("\n");

                json.append("          }");
                if (i < sortedFields.size() - 1) {
                    json.append(",");
                }
                json.append("\n");
            }
            json.append("        ]");
        }

        private static void serializeStringList(StringBuilder json, List<String> items) {
            if (items.isEmpty()) {
                json.append("[]");
                return;
            }
            json.append("[");
            for (int i = 0; i < items.size(); i++) {
                json.append(quote(items.get(i)));
                if (i < items.size() - 1) {
                    json.append(", ");
                }
            }
            json.append("]");
        }

        private static String quote(String s) {
            if (s == null) {
                return "null";
            }
            String escaped = s.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
            return "\"" + escaped + "\"";
        }
    }
}
