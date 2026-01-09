#!/bin/bash
#
# Script to generate a large test corpus (~500+ types) for HexaGlue benchmarks
# Follows DDD Hexagonal Architecture patterns

set -e

LARGE_DIR="~/Projets/hexaglue-projects/hexaglue/hexaglue-benchmarks/test-corpus/large"
SRC_DIR="$LARGE_DIR/src/main/java/com/example/enterprise"

echo "Generating large test corpus..."

# Clean and create directory structure
rm -rf "$LARGE_DIR/src"
mkdir -p "$SRC_DIR"

# Define bounded contexts (each will generate ~50 types)
CONTEXTS=(
    "ordering"
    "inventory"
    "catalog"
    "customer"
    "payment"
    "shipping"
    "warehouse"
    "supplier"
    "analytics"
    "marketing"
)

# Generate code for each bounded context
for CTX in "${CONTEXTS[@]}"; do
    echo "  Generating context: $CTX"

    CTX_DIR="$SRC_DIR/$CTX"
    mkdir -p "$CTX_DIR/domain/model"
    mkdir -p "$CTX_DIR/domain/service"
    mkdir -p "$CTX_DIR/domain/exception"
    mkdir -p "$CTX_DIR/domain/validation"
    mkdir -p "$CTX_DIR/domain/specification"
    mkdir -p "$CTX_DIR/domain/event"
    mkdir -p "$CTX_DIR/port/driving"
    mkdir -p "$CTX_DIR/port/driven"
    mkdir -p "$CTX_DIR/application"

    # Capitalize first letter for class names
    CTX_NAME=$(echo "$CTX" | sed 's/./\U&/')

    # Generate 3 aggregate roots per context
    for i in 1 2 3; do
        AGG="${CTX_NAME}Aggregate${i}"
        cat > "$CTX_DIR/domain/model/${AGG}.java" << EOF
package com.example.enterprise.${CTX}.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregate Root for ${CTX} bounded context.
 */
public class ${AGG} {
    private final ${AGG}Id id;
    private final String name;
    private final List<${CTX_NAME}Item${i}> items;
    private ${CTX_NAME}Status${i} status;
    private final Instant createdAt;
    private Instant updatedAt;

    public ${AGG}(${AGG}Id id, String name) {
        if (id == null) {
            throw new IllegalArgumentException("${AGG}Id cannot be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
        this.id = id;
        this.name = name;
        this.items = new ArrayList<>();
        this.status = ${CTX_NAME}Status${i}.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void addItem(${CTX_NAME}Item${i} item) {
        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null");
        }
        this.items.add(item);
        this.updatedAt = Instant.now();
    }

    public void activate() {
        if (status != ${CTX_NAME}Status${i}.PENDING) {
            throw new IllegalStateException("Can only activate pending items");
        }
        this.status = ${CTX_NAME}Status${i}.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void complete() {
        if (status != ${CTX_NAME}Status${i}.ACTIVE) {
            throw new IllegalStateException("Can only complete active items");
        }
        this.status = ${CTX_NAME}Status${i}.COMPLETED;
        this.updatedAt = Instant.now();
    }

    public ${AGG}Id getId() { return id; }
    public String getName() { return name; }
    public List<${CTX_NAME}Item${i}> getItems() { return Collections.unmodifiableList(items); }
    public ${CTX_NAME}Status${i} getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
EOF

        # Generate ID value object for aggregate
        cat > "$CTX_DIR/domain/model/${AGG}Id.java" << EOF
package com.example.enterprise.${CTX}.domain.model;

import java.util.UUID;

/**
 * Value Object representing a ${AGG} identifier.
 */
public record ${AGG}Id(UUID value) {
    public ${AGG}Id {
        if (value == null) {
            throw new IllegalArgumentException("${AGG}Id cannot be null");
        }
    }

    public static ${AGG}Id generate() {
        return new ${AGG}Id(UUID.randomUUID());
    }

    public static ${AGG}Id of(String value) {
        return new ${AGG}Id(UUID.fromString(value));
    }
}
EOF

        # Generate entity for aggregate
        cat > "$CTX_DIR/domain/model/${CTX_NAME}Item${i}.java" << EOF
package com.example.enterprise.${CTX}.domain.model;

import java.time.Instant;

/**
 * Entity representing an item in ${AGG}.
 */
public class ${CTX_NAME}Item${i} {
    private final ${CTX_NAME}Item${i}Id id;
    private final String description;
    private ${CTX_NAME}Amount${i} amount;
    private final Instant createdAt;

    public ${CTX_NAME}Item${i}(${CTX_NAME}Item${i}Id id, String description, ${CTX_NAME}Amount${i} amount) {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Description cannot be null or blank");
        }
        this.id = id;
        this.description = description;
        this.amount = amount;
        this.createdAt = Instant.now();
    }

    public void updateAmount(${CTX_NAME}Amount${i} newAmount) {
        if (newAmount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        this.amount = newAmount;
    }

    public ${CTX_NAME}Item${i}Id getId() { return id; }
    public String getDescription() { return description; }
    public ${CTX_NAME}Amount${i} getAmount() { return amount; }
    public Instant getCreatedAt() { return createdAt; }
}
EOF

        # Generate item ID value object
        cat > "$CTX_DIR/domain/model/${CTX_NAME}Item${i}Id.java" << EOF
package com.example.enterprise.${CTX}.domain.model;

import java.util.UUID;

public record ${CTX_NAME}Item${i}Id(UUID value) {
    public ${CTX_NAME}Item${i}Id {
        if (value == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }

    public static ${CTX_NAME}Item${i}Id generate() {
        return new ${CTX_NAME}Item${i}Id(UUID.randomUUID());
    }
}
EOF

        # Generate status enum
        cat > "$CTX_DIR/domain/model/${CTX_NAME}Status${i}.java" << EOF
package com.example.enterprise.${CTX}.domain.model;

/**
 * Status enumeration for ${AGG}.
 */
public enum ${CTX_NAME}Status${i} {
    PENDING,
    ACTIVE,
    COMPLETED,
    CANCELLED,
    ARCHIVED
}
EOF

        # Generate amount value object
        cat > "$CTX_DIR/domain/model/${CTX_NAME}Amount${i}.java" << EOF
package com.example.enterprise.${CTX}.domain.model;

import java.math.BigDecimal;

/**
 * Value Object representing an amount in ${CTX} context.
 */
public record ${CTX_NAME}Amount${i}(BigDecimal value, String currency) {
    public ${CTX_NAME}Amount${i} {
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Currency cannot be null or blank");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
    }

    public ${CTX_NAME}Amount${i} add(${CTX_NAME}Amount${i} other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add amounts with different currencies");
        }
        return new ${CTX_NAME}Amount${i}(this.value.add(other.value), this.currency);
    }

    public static ${CTX_NAME}Amount${i} zero(String currency) {
        return new ${CTX_NAME}Amount${i}(BigDecimal.ZERO, currency);
    }
}
EOF

        # Generate repository interface (driven port)
        cat > "$CTX_DIR/port/driven/${AGG}Repository.java" << EOF
package com.example.enterprise.${CTX}.port.driven;

import com.example.enterprise.${CTX}.domain.model.${AGG};
import com.example.enterprise.${CTX}.domain.model.${AGG}Id;
import java.util.List;
import java.util.Optional;

/**
 * Driven port (secondary) for ${AGG} persistence.
 */
public interface ${AGG}Repository {
    ${AGG} save(${AGG} entity);
    Optional<${AGG}> findById(${AGG}Id id);
    List<${AGG}> findAll();
    void deleteById(${AGG}Id id);
    boolean existsById(${AGG}Id id);
    long count();
}
EOF

        # Generate service interface (driving port)
        cat > "$CTX_DIR/port/driving/${AGG}Service.java" << EOF
package com.example.enterprise.${CTX}.port.driving;

import com.example.enterprise.${CTX}.domain.model.${AGG};
import com.example.enterprise.${CTX}.domain.model.${AGG}Id;
import java.util.List;

/**
 * Driving port (primary) for ${AGG} operations.
 */
public interface ${AGG}Service {
    ${AGG}Id create(Create${AGG}Command command);
    ${AGG} get(${AGG}Id id);
    List<${AGG}> list();
    void update(Update${AGG}Command command);
    void delete(${AGG}Id id);
    void activate(${AGG}Id id);
    void complete(${AGG}Id id);
}
EOF

        # Generate command objects
        cat > "$CTX_DIR/port/driving/Create${AGG}Command.java" << EOF
package com.example.enterprise.${CTX}.port.driving;

import java.util.List;

/**
 * Command to create a new ${AGG}.
 */
public record Create${AGG}Command(
    String name,
    List<String> itemDescriptions
) {
    public Create${AGG}Command {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
    }
}
EOF

        cat > "$CTX_DIR/port/driving/Update${AGG}Command.java" << EOF
package com.example.enterprise.${CTX}.port.driving;

import com.example.enterprise.${CTX}.domain.model.${AGG}Id;

/**
 * Command to update an existing ${AGG}.
 */
public record Update${AGG}Command(
    ${AGG}Id id,
    String name
) {
    public Update${AGG}Command {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
    }
}
EOF

        # Generate use case
        cat > "$CTX_DIR/application/Manage${AGG}UseCase.java" << EOF
package com.example.enterprise.${CTX}.application;

import com.example.enterprise.${CTX}.domain.model.${AGG};
import com.example.enterprise.${CTX}.domain.model.${AGG}Id;
import com.example.enterprise.${CTX}.port.driven.${AGG}Repository;
import com.example.enterprise.${CTX}.port.driving.${AGG}Service;
import com.example.enterprise.${CTX}.port.driving.Create${AGG}Command;
import com.example.enterprise.${CTX}.port.driving.Update${AGG}Command;
import com.example.enterprise.${CTX}.domain.exception.${AGG}NotFoundException;
import java.util.List;

/**
 * Use case implementation for managing ${AGG}.
 */
public class Manage${AGG}UseCase implements ${AGG}Service {
    private final ${AGG}Repository repository;

    public Manage${AGG}UseCase(${AGG}Repository repository) {
        this.repository = repository;
    }

    @Override
    public ${AGG}Id create(Create${AGG}Command command) {
        ${AGG}Id id = ${AGG}Id.generate();
        ${AGG} entity = new ${AGG}(id, command.name());
        repository.save(entity);
        return id;
    }

    @Override
    public ${AGG} get(${AGG}Id id) {
        return repository.findById(id)
            .orElseThrow(() -> new ${AGG}NotFoundException(id));
    }

    @Override
    public List<${AGG}> list() {
        return repository.findAll();
    }

    @Override
    public void update(Update${AGG}Command command) {
        ${AGG} entity = get(command.id());
        repository.save(entity);
    }

    @Override
    public void delete(${AGG}Id id) {
        if (!repository.existsById(id)) {
            throw new ${AGG}NotFoundException(id);
        }
        repository.deleteById(id);
    }

    @Override
    public void activate(${AGG}Id id) {
        ${AGG} entity = get(id);
        entity.activate();
        repository.save(entity);
    }

    @Override
    public void complete(${AGG}Id id) {
        ${AGG} entity = get(id);
        entity.complete();
        repository.save(entity);
    }
}
EOF

        # Generate exception
        cat > "$CTX_DIR/domain/exception/${AGG}NotFoundException.java" << EOF
package com.example.enterprise.${CTX}.domain.exception;

import com.example.enterprise.${CTX}.domain.model.${AGG}Id;

/**
 * Exception thrown when a ${AGG} is not found.
 */
public class ${AGG}NotFoundException extends ${CTX_NAME}DomainException {
    public ${AGG}NotFoundException(${AGG}Id id) {
        super("${AGG} not found with id: " + id.value());
    }
}
EOF

    done

    # Generate base domain exception
    cat > "$CTX_DIR/domain/exception/${CTX_NAME}DomainException.java" << EOF
package com.example.enterprise.${CTX}.domain.exception;

/**
 * Base exception for ${CTX} domain errors.
 */
public class ${CTX_NAME}DomainException extends RuntimeException {
    public ${CTX_NAME}DomainException(String message) {
        super(message);
    }

    public ${CTX_NAME}DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
EOF

    # Generate validation exception
    cat > "$CTX_DIR/domain/exception/${CTX_NAME}ValidationException.java" << EOF
package com.example.enterprise.${CTX}.domain.exception;

/**
 * Exception thrown when validation fails in ${CTX} context.
 */
public class ${CTX_NAME}ValidationException extends ${CTX_NAME}DomainException {
    public ${CTX_NAME}ValidationException(String message) {
        super(message);
    }
}
EOF

    # Generate domain service
    cat > "$CTX_DIR/domain/service/${CTX_NAME}CalculationService.java" << EOF
package com.example.enterprise.${CTX}.domain.service;

import com.example.enterprise.${CTX}.domain.model.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Domain service for ${CTX} calculations.
 */
public class ${CTX_NAME}CalculationService {
    public ${CTX_NAME}Amount1 calculateTotal(List<${CTX_NAME}Amount1> amounts) {
        if (amounts == null || amounts.isEmpty()) {
            return ${CTX_NAME}Amount1.zero("USD");
        }
        ${CTX_NAME}Amount1 total = ${CTX_NAME}Amount1.zero(amounts.get(0).currency());
        for (${CTX_NAME}Amount1 amount : amounts) {
            total = total.add(amount);
        }
        return total;
    }

    public BigDecimal calculatePercentage(BigDecimal value, BigDecimal percentage) {
        return value.multiply(percentage).divide(BigDecimal.valueOf(100));
    }
}
EOF

    # Generate another domain service
    cat > "$CTX_DIR/domain/service/${CTX_NAME}ValidationService.java" << EOF
package com.example.enterprise.${CTX}.domain.service;

import com.example.enterprise.${CTX}.domain.model.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain service for ${CTX} validation.
 */
public class ${CTX_NAME}ValidationService {
    public List<String> validate(${CTX_NAME}Aggregate1 aggregate) {
        List<String> errors = new ArrayList<>();
        if (aggregate.getName() == null || aggregate.getName().isBlank()) {
            errors.add("Name is required");
        }
        if (aggregate.getItems().isEmpty()) {
            errors.add("At least one item is required");
        }
        return errors;
    }

    public boolean isValid(${CTX_NAME}Aggregate1 aggregate) {
        return validate(aggregate).isEmpty();
    }
}
EOF

    # Generate specification
    cat > "$CTX_DIR/domain/specification/${CTX_NAME}Specifications.java" << EOF
package com.example.enterprise.${CTX}.domain.specification;

import com.example.enterprise.${CTX}.domain.model.*;
import java.util.function.Predicate;

/**
 * Specifications for ${CTX} domain queries.
 */
public final class ${CTX_NAME}Specifications {
    private ${CTX_NAME}Specifications() {}

    public static Predicate<${CTX_NAME}Aggregate1> isActive() {
        return agg -> agg.getStatus() == ${CTX_NAME}Status1.ACTIVE;
    }

    public static Predicate<${CTX_NAME}Aggregate1> isPending() {
        return agg -> agg.getStatus() == ${CTX_NAME}Status1.PENDING;
    }

    public static Predicate<${CTX_NAME}Aggregate1> isCompleted() {
        return agg -> agg.getStatus() == ${CTX_NAME}Status1.COMPLETED;
    }

    public static Predicate<${CTX_NAME}Aggregate1> hasItems() {
        return agg -> !agg.getItems().isEmpty();
    }

    public static Predicate<${CTX_NAME}Aggregate1> hasMinimumItems(int count) {
        return agg -> agg.getItems().size() >= count;
    }
}
EOF

    # Generate validator
    cat > "$CTX_DIR/domain/validation/${CTX_NAME}Validator.java" << EOF
package com.example.enterprise.${CTX}.domain.validation;

import com.example.enterprise.${CTX}.domain.model.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Validator for ${CTX} domain objects.
 */
public class ${CTX_NAME}Validator {
    public ValidationResult validate(${CTX_NAME}Aggregate1 aggregate) {
        List<String> errors = new ArrayList<>();

        if (aggregate.getId() == null) {
            errors.add("Id is required");
        }
        if (aggregate.getName() == null || aggregate.getName().isBlank()) {
            errors.add("Name is required");
        }
        if (aggregate.getStatus() == null) {
            errors.add("Status is required");
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }
}
EOF

    # Generate validation result
    cat > "$CTX_DIR/domain/validation/ValidationResult.java" << EOF
package com.example.enterprise.${CTX}.domain.validation;

import java.util.List;
import java.util.Collections;

/**
 * Result of a validation operation.
 */
public record ValidationResult(boolean valid, List<String> errors) {
    public ValidationResult {
        errors = errors == null ? Collections.emptyList() : List.copyOf(errors);
    }

    public static ValidationResult success() {
        return new ValidationResult(true, Collections.emptyList());
    }

    public static ValidationResult failure(List<String> errors) {
        return new ValidationResult(false, errors);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}
EOF

    # Generate domain event
    cat > "$CTX_DIR/domain/event/${CTX_NAME}Event.java" << EOF
package com.example.enterprise.${CTX}.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base event for ${CTX} domain events.
 */
public abstract class ${CTX_NAME}Event {
    private final UUID eventId;
    private final Instant occurredAt;

    protected ${CTX_NAME}Event() {
        this.eventId = UUID.randomUUID();
        this.occurredAt = Instant.now();
    }

    public UUID getEventId() { return eventId; }
    public Instant getOccurredAt() { return occurredAt; }
    public abstract String getEventType();
}
EOF

    # Generate specific events
    cat > "$CTX_DIR/domain/event/${CTX_NAME}CreatedEvent.java" << EOF
package com.example.enterprise.${CTX}.domain.event;

import com.example.enterprise.${CTX}.domain.model.${CTX_NAME}Aggregate1Id;

/**
 * Event fired when a ${CTX_NAME}Aggregate1 is created.
 */
public class ${CTX_NAME}CreatedEvent extends ${CTX_NAME}Event {
    private final ${CTX_NAME}Aggregate1Id aggregateId;
    private final String name;

    public ${CTX_NAME}CreatedEvent(${CTX_NAME}Aggregate1Id aggregateId, String name) {
        super();
        this.aggregateId = aggregateId;
        this.name = name;
    }

    public ${CTX_NAME}Aggregate1Id getAggregateId() { return aggregateId; }
    public String getName() { return name; }

    @Override
    public String getEventType() {
        return "${CTX_NAME}Created";
    }
}
EOF

    cat > "$CTX_DIR/domain/event/${CTX_NAME}UpdatedEvent.java" << EOF
package com.example.enterprise.${CTX}.domain.event;

import com.example.enterprise.${CTX}.domain.model.${CTX_NAME}Aggregate1Id;

/**
 * Event fired when a ${CTX_NAME}Aggregate1 is updated.
 */
public class ${CTX_NAME}UpdatedEvent extends ${CTX_NAME}Event {
    private final ${CTX_NAME}Aggregate1Id aggregateId;

    public ${CTX_NAME}UpdatedEvent(${CTX_NAME}Aggregate1Id aggregateId) {
        super();
        this.aggregateId = aggregateId;
    }

    public ${CTX_NAME}Aggregate1Id getAggregateId() { return aggregateId; }

    @Override
    public String getEventType() {
        return "${CTX_NAME}Updated";
    }
}
EOF

    # Generate driven port interfaces for external services
    cat > "$CTX_DIR/port/driven/${CTX_NAME}NotificationPort.java" << EOF
package com.example.enterprise.${CTX}.port.driven;

import com.example.enterprise.${CTX}.domain.model.${CTX_NAME}Aggregate1Id;

/**
 * Driven port for ${CTX} notifications.
 */
public interface ${CTX_NAME}NotificationPort {
    void sendCreatedNotification(${CTX_NAME}Aggregate1Id id, String name);
    void sendUpdatedNotification(${CTX_NAME}Aggregate1Id id);
    void sendCompletedNotification(${CTX_NAME}Aggregate1Id id);
}
EOF

    cat > "$CTX_DIR/port/driven/${CTX_NAME}EventPublisher.java" << EOF
package com.example.enterprise.${CTX}.port.driven;

import com.example.enterprise.${CTX}.domain.event.${CTX_NAME}Event;

/**
 * Driven port for publishing ${CTX} domain events.
 */
public interface ${CTX_NAME}EventPublisher {
    void publish(${CTX_NAME}Event event);
    void publishAsync(${CTX_NAME}Event event);
}
EOF

    # Generate query service
    cat > "$CTX_DIR/application/${CTX_NAME}QueryService.java" << EOF
package com.example.enterprise.${CTX}.application;

import com.example.enterprise.${CTX}.domain.model.*;
import com.example.enterprise.${CTX}.port.driven.${CTX_NAME}Aggregate1Repository;
import com.example.enterprise.${CTX}.domain.specification.${CTX_NAME}Specifications;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Query service for ${CTX} read operations.
 */
public class ${CTX_NAME}QueryService {
    private final ${CTX_NAME}Aggregate1Repository repository;

    public ${CTX_NAME}QueryService(${CTX_NAME}Aggregate1Repository repository) {
        this.repository = repository;
    }

    public List<${CTX_NAME}Aggregate1> findActive() {
        return repository.findAll().stream()
            .filter(${CTX_NAME}Specifications.isActive())
            .collect(Collectors.toList());
    }

    public List<${CTX_NAME}Aggregate1> findPending() {
        return repository.findAll().stream()
            .filter(${CTX_NAME}Specifications.isPending())
            .collect(Collectors.toList());
    }

    public List<${CTX_NAME}Aggregate1> findCompleted() {
        return repository.findAll().stream()
            .filter(${CTX_NAME}Specifications.isCompleted())
            .collect(Collectors.toList());
    }

    public long countActive() {
        return repository.findAll().stream()
            .filter(${CTX_NAME}Specifications.isActive())
            .count();
    }
}
EOF

    # Generate additional value objects for variety
    cat > "$CTX_DIR/domain/model/${CTX_NAME}Code.java" << EOF
package com.example.enterprise.${CTX}.domain.model;

/**
 * Value Object representing a code in ${CTX} context.
 */
public record ${CTX_NAME}Code(String value) {
    public ${CTX_NAME}Code {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Code cannot be null or blank");
        }
        if (value.length() > 50) {
            throw new IllegalArgumentException("Code cannot exceed 50 characters");
        }
    }
}
EOF

    cat > "$CTX_DIR/domain/model/${CTX_NAME}Name.java" << EOF
package com.example.enterprise.${CTX}.domain.model;

/**
 * Value Object representing a name in ${CTX} context.
 */
public record ${CTX_NAME}Name(String value) {
    public ${CTX_NAME}Name {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
        if (value.length() > 200) {
            throw new IllegalArgumentException("Name cannot exceed 200 characters");
        }
    }
}
EOF

    cat > "$CTX_DIR/domain/model/${CTX_NAME}Description.java" << EOF
package com.example.enterprise.${CTX}.domain.model;

/**
 * Value Object representing a description in ${CTX} context.
 */
public record ${CTX_NAME}Description(String value) {
    public ${CTX_NAME}Description {
        if (value != null && value.length() > 1000) {
            throw new IllegalArgumentException("Description cannot exceed 1000 characters");
        }
    }

    public static ${CTX_NAME}Description empty() {
        return new ${CTX_NAME}Description("");
    }

    public boolean isEmpty() {
        return value == null || value.isBlank();
    }
}
EOF

    cat > "$CTX_DIR/domain/model/${CTX_NAME}Priority.java" << EOF
package com.example.enterprise.${CTX}.domain.model;

/**
 * Value Object representing priority levels in ${CTX} context.
 */
public enum ${CTX_NAME}Priority {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4);

    private final int level;

    ${CTX_NAME}Priority(int level) {
        this.level = level;
    }

    public int getLevel() { return level; }

    public boolean isHigherThan(${CTX_NAME}Priority other) {
        return this.level > other.level;
    }
}
EOF

    cat > "$CTX_DIR/domain/model/${CTX_NAME}Timestamp.java" << EOF
package com.example.enterprise.${CTX}.domain.model;

import java.time.Instant;

/**
 * Value Object representing a timestamp in ${CTX} context.
 */
public record ${CTX_NAME}Timestamp(Instant value) {
    public ${CTX_NAME}Timestamp {
        if (value == null) {
            throw new IllegalArgumentException("Timestamp cannot be null");
        }
    }

    public static ${CTX_NAME}Timestamp now() {
        return new ${CTX_NAME}Timestamp(Instant.now());
    }

    public boolean isBefore(${CTX_NAME}Timestamp other) {
        return this.value.isBefore(other.value);
    }

    public boolean isAfter(${CTX_NAME}Timestamp other) {
        return this.value.isAfter(other.value);
    }
}
EOF

done

echo "Large corpus generation complete!"
echo ""
echo "Counting generated files..."
find "$SRC_DIR" -name "*.java" | wc -l
