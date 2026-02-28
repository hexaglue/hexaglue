# Quick Start

Get started with HexaGlue in 10 minutes. This tutorial walks you through creating a minimal project and generating infrastructure code from your domain.

**Prerequisites**: Java 17+, Maven 3.8+

## What You'll Build

A simple task management domain with:
- One aggregate root (`Task`)
- One value object (`TaskId`)
- Two ports (`TaskUseCases` driving, `TaskRepository` driven)

## Step 1: Create Your Domain

Create a new Maven project with the following structure:

```
my-project/
  pom.xml
  src/main/java/com/example/
    domain/
      Task.java
      TaskId.java
    ports/
      in/
        TaskUseCases.java
      out/
        TaskRepository.java
```

### Task.java (Aggregate Root)

```java
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
```

### TaskId.java (Value Object)

```java
package com.example.domain;

import java.util.UUID;

public record TaskId(UUID value) {
    public static TaskId generate() {
        return new TaskId(UUID.randomUUID());
    }
}
```

### TaskUseCases.java (Driving Port)

```java
package com.example.ports.in;

import com.example.domain.Task;
import com.example.domain.TaskId;
import java.util.Optional;

public interface TaskUseCases {
    Task createTask(String title, String description);
    Optional<Task> getTask(TaskId id);
    void completeTask(TaskId id);
}
```

### TaskRepository.java (Driven Port)

```java
package com.example.ports.out;

import com.example.domain.Task;
import com.example.domain.TaskId;
import java.util.Optional;

public interface TaskRepository {
    Task save(Task task);
    Optional<Task> findById(TaskId id);
    void delete(Task task);
}
```

## Step 2: Add HexaGlue (Minimal Configuration)

Add this to your `pom.xml`:

```xml
<plugin>
    <groupId>io.hexaglue</groupId>
    <artifactId>hexaglue-maven-plugin</artifactId>
    <version>6.0.0</version>
    <extensions>true</extensions>
    <configuration>
        <basePackage>com.example</basePackage>
    </configuration>
</plugin>
```

Run:
```bash
mvn compile
```

HexaGlue analyzes your domain and outputs a classification summary:

```
[INFO] --- hexaglue:6.0.0:generate (default-cli) @ my-project ---
[INFO]
[INFO] CLASSIFICATION SUMMARY
[INFO] --------------------------------------------------------------
[INFO] EXPLICIT:                0 (  0,0%)
[INFO] INFERRED:                4 (100,0%)
[INFO] UNCLASSIFIED:            0 (  0,0%)
[INFO] TOTAL:                   4
[INFO]
[INFO] Status: PASSED
```

HexaGlue inferred:
- `Task` as AGGREGATE_ROOT
- `TaskId` as VALUE_OBJECT
- `TaskUseCases` as DRIVING port
- `TaskRepository` as DRIVEN port

No code is generated yet - no plugins are configured.

## Step 3: Add the JPA Plugin

To generate JPA entities and repositories, add the plugin dependency:

```xml
<plugin>
    <groupId>io.hexaglue</groupId>
    <artifactId>hexaglue-maven-plugin</artifactId>
    <version>6.0.0</version>
    <extensions>true</extensions>
    <configuration>
        <basePackage>com.example</basePackage>
    </configuration>
    <dependencies>
        <dependency>
            <groupId>io.hexaglue.plugins</groupId>
            <artifactId>hexaglue-plugin-jpa</artifactId>
            <version>3.0.0</version>
        </dependency>
    </dependencies>
</plugin>
```

Also add the required runtime dependencies:

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct</artifactId>
        <version>1.6.3</version>
    </dependency>
</dependencies>
```

Run again:
```bash
mvn compile
```

## Step 4: Explore Generated Code

Check `target/generated-sources/hexaglue/`. HexaGlue generated:

```
target/generated-sources/hexaglue/
  com/example/infrastructure/
    persistence/
      TaskEntity.java          # JPA entity
      TaskJpaRepository.java   # Spring Data repository
      TaskMapper.java          # MapStruct mapper
    adapters/
      TaskRepositoryAdapter.java  # Port implementation
```

### Generated TaskEntity.java

```java
@Entity
@Table(name = "task")
public class TaskEntity {
    @Id
    private UUID id;
    private String title;
    private String description;
    private boolean completed;
    private Instant createdAt;
    // getters, setters...
}
```

### Generated TaskJpaRepository.java

```java
public interface TaskJpaRepository extends JpaRepository<TaskEntity, UUID> {
}
```

### Generated TaskRepositoryAdapter.java

```java
@Component
public class TaskRepositoryAdapter implements TaskRepository {
    private final TaskJpaRepository jpaRepository;
    private final TaskMapper mapper;

    // Implementation of save, findById, delete...
}
```

## What's Next?

You've successfully generated JPA infrastructure from your domain model.

**Continue learning:**
- [Classification](CLASSIFICATION.md) - Understand how HexaGlue classifies types
- [JPA Generation](JPA_GENERATION.md) - Deep dive into JPA generation options
- [Validation](VALIDATION.md) - Validate classification before generation

**Full examples:**
- [sample-basic](../examples/sample-basic/) - Minimal example with classification and living documentation
- [sample-multi-aggregate](../examples/sample-multi-aggregate/) - Complete JPA generation with multiple aggregates

---

<div align="center">

**HexaGlue - Compile your architecture, not just your code**

Made with ❤️ by Scalastic<br>
Copyright 2026 Scalastic - Released under MPL-2.0

</div>

