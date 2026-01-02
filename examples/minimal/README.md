# Minimal HexaGlue Example

The simplest possible HexaGlue example - perfect for getting started.

## Overview

This example demonstrates the core concepts of HexaGlue with a minimal task management domain:

- **1 Aggregate Root**: `Task`
- **1 Identifier**: `TaskId`
- **1 Driving Port**: `TaskUseCases`
- **1 Driven Port**: `TaskRepository`

## Project Structure

```
minimal/
└── src/main/java/com/example/
    ├── domain/
    │   ├── Task.java          # Aggregate Root
    │   └── TaskId.java        # Identifier (Value Object)
    └── ports/
        ├── in/
        │   └── TaskUseCases.java     # Driving Port (Use Cases)
        └── out/
            └── TaskRepository.java   # Driven Port (Repository)
```

## Quick Start

### 1. Build and Generate

```bash
mvn clean compile
```

### 2. View Generated Files

```bash
# JPA infrastructure
ls target/generated-sources/hexaglue/

# Documentation
ls target/generated-sources/generated-docs/
```

## What Gets Generated

### JPA Plugin Output

| File | Description |
|------|-------------|
| `TaskEntity.java` | JPA entity with `@Entity`, `@Id` |
| `TaskJpaRepository.java` | Spring Data repository |
| `TaskMapper.java` | MapStruct mapper (Task ↔ TaskEntity) |
| `TaskAdapter.java` | Implements `TaskRepository` port |

### Living Documentation Plugin Output

| File | Description |
|------|-------------|
| `README.md` | Architecture overview with diagram |
| `domain.md` | Task aggregate documentation |
| `ports.md` | Port interfaces documentation |
| `diagrams.md` | Mermaid class diagrams |

## Domain Model

### Task (Aggregate Root)

```java
public class Task {
    private final TaskId id;
    private String title;
    private String description;
    private boolean completed;
    private final Instant createdAt;

    // Behavior
    void update(String title, String description);
    void complete();
    void reopen();
}
```

### TaskId (Identifier)

```java
public record TaskId(UUID value) {
    static TaskId generate();
    static TaskId fromString(String id);
}
```

## Ports

### Driving Port: TaskUseCases

Defines the use cases available to external actors:

```java
public interface TaskUseCases {
    Task createTask(String title, String description);
    Optional<Task> getTask(TaskId id);
    List<Task> listAllTasks();
    void completeTask(TaskId id);
    void deleteTask(TaskId id);
}
```

### Driven Port: TaskRepository

Defines what the domain needs from persistence:

```java
public interface TaskRepository {
    Task save(Task task);
    Optional<Task> findById(TaskId id);
    List<Task> findAll();
    void delete(Task task);
}
```

## How HexaGlue Classifies This Code

| Type | Classification | Confidence | Reason |
|------|----------------|------------|--------|
| `Task` | AGGREGATE_ROOT | HIGH | Has identity, used in repository |
| `TaskId` | IDENTIFIER | HIGH | Record wrapping UUID |
| `TaskUseCases` | DRIVING port | HIGH | Package `ports.in` |
| `TaskRepository` | DRIVEN port | HIGH | Package `ports.out`, name pattern |

## Next Steps

1. **Add More Complexity**: See the [coffeeshop](../coffeeshop/) example
2. **Rich Domain**: See the [ecommerce](../ecommerce/) example
3. **Configuration**: Add `hexaglue.yaml` to customize generation
