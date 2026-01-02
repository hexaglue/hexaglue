# Getting Started with HexaGlue

***A progressive tutorial from simple to advanced.***

---

This guide takes you from your first HexaGlue project to advanced domain modeling. Choose your starting point based on your experience:

| Level | Duration | What You'll Learn |
|-------|----------|-------------------|
| [Level 1: Discovery](#level-1-discovery) | 15 min | Run your first HexaGlue project |
| [Level 2: Fundamentals](#level-2-fundamentals) | 30 min | Project structure, configuration, conventions |
| [Level 3: Intermediate](#level-3-intermediate) | 1 hour | Rich domain modeling, relationships |
| [Level 4: Advanced](#level-4-advanced) | 2 hours | Enterprise patterns, custom plugins |

---

## Prerequisites

Before starting, ensure you have:

- **Java 17+** - `java -version`
- **Maven 3.8+** - `mvn -version`

---

# Level 1: Discovery

**Goal**: Run your first HexaGlue project and understand what it generates.

## Step 1.1: Create a New Maven Project

```bash
mkdir hexaglue-tutorial
cd hexaglue-tutorial
```

Create `pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>hexaglue-tutorial</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <properties>
        <java.version>17</java.version>
        <maven.compiler.release>${java.version}</maven.compiler.release>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <hexaglue.version>2.0.0-SNAPSHOT</hexaglue.version>
    </properties>

    <build>
        <plugins>
            <plugin>
                <groupId>io.hexaglue</groupId>
                <artifactId>hexaglue-maven-plugin</artifactId>
                <version>${hexaglue.version}</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>generate</goal>
                        </goals>
                    </execution>
                </executions>
                <dependencies>
                    <dependency>
                        <groupId>io.hexaglue.plugins</groupId>
                        <artifactId>hexaglue-plugin-living-doc</artifactId>
                        <version>${hexaglue.version}</version>
                    </dependency>
                </dependencies>
            </plugin>
        </plugins>
    </build>
</project>
```

## Step 1.2: Create Your First Domain Class

Create `src/main/java/com/example/domain/Task.java`:

```java
package com.example.domain;

import java.util.UUID;

/**
 * A simple task in our todo application.
 */
public class Task {
    private TaskId id;
    private String title;
    private String description;
    private boolean completed;

    public Task(TaskId id, String title) {
        this.id = id;
        this.title = title;
        this.completed = false;
    }

    public TaskId getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public boolean isCompleted() { return completed; }

    public void complete() {
        this.completed = true;
    }
}
```

Create `src/main/java/com/example/domain/TaskId.java`:

```java
package com.example.domain;

import java.util.UUID;

/**
 * Identifier for a Task - a typed wrapper around UUID.
 */
public record TaskId(UUID value) {
    public static TaskId generate() {
        return new TaskId(UUID.randomUUID());
    }
}
```

## Step 1.3: Define Your First Port

Create `src/main/java/com/example/ports/out/TaskRepository.java`:

```java
package com.example.ports.out;

import com.example.domain.Task;
import com.example.domain.TaskId;
import java.util.Optional;

/**
 * Driven port for task persistence.
 * HexaGlue will detect this as a repository port.
 */
public interface TaskRepository {
    Optional<Task> findById(TaskId id);
    Task save(Task task);
    void delete(TaskId id);
}
```

## Step 1.4: Run HexaGlue

```bash
mvn compile
```

You should see output like:

```
[INFO] --- hexaglue:2.0.0-SNAPSHOT:generate (default) @ minimal ---
[INFO] HexaGlue analyzing: com.example
[INFO] Building semantic model for base package: com.example
[INFO] Building Spoon model for base package: com.example
[INFO] Spoon model built: 3 types
[INFO] Semantic model built: 3 types
[INFO] Building application graph
[INFO] Building Spoon model for base package: com.example
[INFO] Spoon model built: 3 types
[INFO] Building Spoon model for base package: com.example
[INFO] Spoon model built: 3 types
[INFO] Building graph from 3 types
[INFO] Graph built: 28 nodes, 43 edges
[INFO] Application graph built: 28 nodes, 43 edges
[INFO] Classifying types
[INFO] Classification complete: 2 domain types, 1 ports, 0 conflicts
[INFO] Exporting to IR
[INFO] Executing plugins
[INFO] Discovered 1 plugins
[INFO] Executing plugin: io.hexaglue.plugin.livingdoc
[INFO] [io.hexaglue.plugin.livingdoc] Generating living documentation in: docs/architecture
[INFO] [io.hexaglue.plugin.livingdoc] Generated architecture overview
[INFO] [io.hexaglue.plugin.livingdoc] Generated domain model documentation
[INFO] [io.hexaglue.plugin.livingdoc] Generated ports documentation
[INFO] [io.hexaglue.plugin.livingdoc] Generated architecture diagrams
[INFO] [io.hexaglue.plugin.livingdoc] Living documentation complete: 2 domain types, 2 ports documented
[INFO] Plugin io.hexaglue.plugin.livingdoc completed in 4ms, generated 4 files
[INFO] Plugins executed: 1 plugins, 5 files generated
[INFO] Analysis complete in 365ms
[INFO] Analysis complete: 3 types, 3 classified, 1 ports in 365ms
[INFO] Generated 5 files
```

## Step 1.5: Explore the Generated Output

Check the generated documentation:

```bash
cat docs/architecture/ports/TaskRepository.md
```

```markdown
# TaskRepository

**Type**: Driven Port (Repository)
**Package**: `com.example.ports.out`

## Methods

| Method | Parameters | Returns |
|--------|------------|---------|
| `findById` | `TaskId id` | `Optional<Task>` |
| `save` | `Task task` | `Task` |
| `delete` | `TaskId id` | `void` |

## Domain Types Used

- `Task` (Aggregate Root)
- `TaskId` (Identifier)
```

**Congratulations!** You've run your first HexaGlue project.

### What Just Happened?

1. HexaGlue **analyzed** your source code
2. It **detected** that `Task` is an Aggregate Root (has identity, is referenced by a Repository)
3. It **detected** that `TaskId` is an Identifier (record with single `value` field, used as ID)
4. It **detected** that `TaskRepository` is a Driven Port (interface in `ports.out` package)
5. The Living Documentation plugin **generated** Markdown documentation

---

# Level 2: Fundamentals

> [!WARNING]
```text
╔════════════════════════════════════════════════════════════╗
║ Work in Progress:                                          ║
║ This section needs to be updated to reflect recent changes ║
╚════════════════════════════════════════════════════════════╝
```

**Goal**: Understand project structure, configuration, and naming conventions.

## Step 2.1: Understanding Package Conventions

HexaGlue uses package names to understand your architecture. The recommended structure:

```
src/main/java/com/example/
├── domain/              # Domain model (entities, value objects)
│   ├── Task.java
│   ├── TaskId.java
│   └── order/           # Sub-domains are fine
│       └── Order.java
├── ports/
│   ├── in/              # Driving ports (use cases, commands, queries)
│   │   └── ManagingTasks.java
│   └── out/             # Driven ports (repositories, gateways)
│       └── TaskRepository.java
└── application/         # Application services (optional)
    └── TaskService.java
```

**Key conventions**:
- `ports.in` or `ports/in` → Driving ports (inbound)
- `ports.out` or `ports/out` → Driven ports (outbound)
- `domain` → Domain types

## Step 2.2: Add a Driving Port

Create `src/main/java/com/example/ports/in/ManagingTasks.java`:

```java
package com.example.ports.in;

import com.example.domain.Task;
import com.example.domain.TaskId;
import java.util.List;

/**
 * Driving port - defines what the application can do.
 * This represents the use cases of our application.
 */
public interface ManagingTasks {

    /**
     * Create a new task.
     */
    Task createTask(String title, String description);

    /**
     * Get a task by its ID.
     */
    Task getTask(TaskId id);

    /**
     * List all tasks.
     */
    List<Task> listAllTasks();

    /**
     * Mark a task as completed.
     */
    void completeTask(TaskId id);
}
```

## Step 2.3: Add Configuration

Create `hexaglue.yaml` in your project root:

```yaml
hexaglue:
  # Base package for analysis (optional - auto-detected if not specified)
  basePackage: com.example

  # Plugin configuration
  plugins:
    io.hexaglue.plugin.livingdoc:
      outputDir: docs/architecture/
      includePortMethods: true
      includeDomainProperties: true
```

## Step 2.4: Rebuild and Explore

```bash
mvn clean compile
```

Now check the generated documentation:

```bash
ls docs/architecture/
```

```
ports/
├── ManagingTasks.md    # Driving port
└── TaskRepository.md   # Driven port
domain/
├── Task.md             # Aggregate Root
└── TaskId.md           # Identifier
README.md               # Overview
```

## Step 2.5: Understanding Classification

HexaGlue classifies your types automatically. View the classification in logs:

```
Classification results:
  Task -> AGGREGATE_ROOT (HIGH confidence)
    Reason: Has identity field 'id' of type TaskId, referenced by Repository
  TaskId -> IDENTIFIER (HIGH confidence)
    Reason: Record with single 'value' field of type UUID
  ManagingTasks -> USE_CASE port (DRIVING)
    Reason: Interface in 'ports.in' package
  TaskRepository -> REPOSITORY port (DRIVEN)
    Reason: Interface in 'ports.out' package, name ends with 'Repository'
```

**Classification hierarchy**:

| Domain Type | Description | Detection |
|-------------|-------------|-----------|
| AGGREGATE_ROOT | Entry point to a consistency boundary | Referenced by Repository, has identity |
| ENTITY | Has identity but not an aggregate root | Has ID field, referenced by aggregate |
| VALUE_OBJECT | Immutable, no identity | Record or immutable class without ID |
| IDENTIFIER | Wraps an ID value | Record with single field, used as ID |

| Port Type | Direction | Detection |
|-----------|-----------|-----------|
| USE_CASE | DRIVING | In `ports.in`, represents a use case |
| COMMAND | DRIVING | In `ports.in`, methods that change state |
| QUERY | DRIVING | In `ports.in`, methods that read state |
| REPOSITORY | DRIVEN | In `ports.out`, name ends with `Repository` |
| GATEWAY | DRIVEN | In `ports.out`, name ends with `Gateway` or `Client` |

---

# Level 3: Intermediate

> [!WARNING]
```text
╔════════════════════════════════════════════════════════════╗
║ Work in Progress:                                          ║
║ This section needs to be updated to reflect recent changes ║
╚════════════════════════════════════════════════════════════╝
```

**Goal**: Model a rich domain with relationships and add JPA generation.

## Step 3.1: Expand Your Domain

Let's build a task management system with projects:

Create `src/main/java/com/example/domain/Project.java`:

```java
package com.example.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A project contains multiple tasks.
 * This is an Aggregate Root.
 */
public class Project {
    private ProjectId id;
    private String name;
    private String description;
    private List<Task> tasks;

    public Project(ProjectId id, String name) {
        this.id = id;
        this.name = name;
        this.tasks = new ArrayList<>();
    }

    public ProjectId getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }

    public List<Task> getTasks() {
        return Collections.unmodifiableList(tasks);
    }

    public void addTask(Task task) {
        this.tasks.add(task);
    }

    public void removeTask(TaskId taskId) {
        tasks.removeIf(t -> t.getId().equals(taskId));
    }
}
```

Create `src/main/java/com/example/domain/ProjectId.java`:

```java
package com.example.domain;

import java.util.UUID;

public record ProjectId(UUID value) {
    public static ProjectId generate() {
        return new ProjectId(UUID.randomUUID());
    }
}
```

Update `Task.java` to reference its project:

```java
package com.example.domain;

public class Task {
    private TaskId id;
    private String title;
    private String description;
    private boolean completed;
    private Priority priority;  // Value Object

    // ... constructors and methods
}
```

Create `src/main/java/com/example/domain/Priority.java`:

```java
package com.example.domain;

/**
 * Task priority - a Value Object (enum).
 */
public enum Priority {
    LOW, MEDIUM, HIGH, CRITICAL
}
```

## Step 3.2: Add JPA Plugin

Update your `pom.xml` to include the JPA plugin:

```xml
<dependencies>
    <!-- Spring Boot for JPA -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
        <version>3.2.0</version>
    </dependency>
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <version>2.2.224</version>
        <scope>runtime</scope>
    </dependency>
    <!-- MapStruct for mapping -->
    <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct</artifactId>
        <version>1.5.5.Final</version>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>io.hexaglue</groupId>
            <artifactId>hexaglue-maven-plugin</artifactId>
            <version>${hexaglue.version}</version>
            <executions>
                <execution>
                    <goals>
                        <goal>generate</goal>
                    </goals>
                </execution>
            </executions>
            <dependencies>
                <dependency>
                    <groupId>io.hexaglue</groupId>
                    <artifactId>hexaglue-plugin-living-doc</artifactId>
                    <version>${hexaglue.version}</version>
                </dependency>
                <dependency>
                    <groupId>io.hexaglue</groupId>
                    <artifactId>hexaglue-plugin-jpa</artifactId>
                    <version>${hexaglue.version}</version>
                </dependency>
            </dependencies>
        </plugin>
    </plugins>
</build>
```

## Step 3.3: Configure JPA Generation

Update `hexaglue.yaml`:

```yaml
hexaglue:
  basePackage: com.example

  plugins:
    io.hexaglue.plugin.livingdoc:
      outputDir: docs/architecture/

    io.hexaglue.plugin.jpa:
      basePackage: com.example.infrastructure.persistence
      entitySuffix: Entity
      repositorySuffix: JpaRepository
      features:
        auditing: false
        optimisticLocking: false
```

## Step 3.4: Add Repository Port

Create `src/main/java/com/example/ports/out/ProjectRepository.java`:

```java
package com.example.ports.out;

import com.example.domain.Project;
import com.example.domain.ProjectId;
import java.util.Optional;
import java.util.List;

public interface ProjectRepository {
    Optional<Project> findById(ProjectId id);
    List<Project> findAll();
    Project save(Project project);
    void delete(ProjectId id);
}
```

## Step 3.5: Generate and Explore

```bash
mvn clean compile
```

Check the generated JPA code:

```
target/generated-sources/hexaglue/
└── com/example/infrastructure/persistence/
    ├── ProjectEntity.java
    ├── ProjectJpaRepository.java
    ├── ProjectMapper.java
    ├── JpaProjectRepositoryAdapter.java
    ├── TaskEntity.java
    └── PriorityConverter.java
```

Example generated `ProjectEntity.java`:

```java
package com.example.infrastructure.persistence;

import jakarta.persistence.*;
import java.util.List;
import java.util.UUID;

/**
 * JPA entity for {@link com.example.domain.Project}.
 * Generated by HexaGlue JPA Plugin.
 */
@Entity
@Table(name = "project")
public class ProjectEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String description;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "project_id")
    private List<TaskEntity> tasks;

    protected ProjectEntity() {}

    // Getters and setters...
}
```

---

# Level 4: Advanced

> [!WARNING]
```text
╔════════════════════════════════════════════════════════════╗
║ Work in Progress:                                          ║
║ This section needs to be updated to reflect recent changes ║
╚════════════════════════════════════════════════════════════╝
```

**Goal**: Enterprise patterns, explicit annotations, and custom plugins.

## Step 4.1: Using jMolecules Annotations

For explicit domain modeling, add jMolecules:

```xml
<dependency>
    <groupId>org.jmolecules</groupId>
    <artifactId>jmolecules-ddd</artifactId>
    <version>1.9.0</version>
</dependency>
```

Annotate your domain:

```java
package com.example.domain;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.jmolecules.ddd.annotation.ValueObject;

@AggregateRoot
public class Project {
    @Identity
    private ProjectId id;
    // ...
}

@ValueObject
public record Priority(String level) {}
```

**Benefits**:
- Removes ambiguity in classification
- Documents intent in code
- Provides EXPLICIT confidence level

## Step 4.2: Advanced JPA Configuration

```yaml
hexaglue:
  plugins:
    io.hexaglue.plugin.jpa:
      basePackage: com.example.infrastructure.persistence

      # ID generation strategy
      idStrategy: UUID  # UUID, IDENTITY, SEQUENCE, ASSIGNED

      # Features
      features:
        auditing: true            # @CreatedDate, @LastModifiedDate
        optimisticLocking: true   # @Version
        softDelete: true          # Logical deletion
        generateQueryMethods: true

      # Naming conventions
      naming:
        tablePrefix: app_
        columnStyle: SNAKE_CASE
```

## Step 4.3: Auditing and Soft Delete

With auditing enabled, generated entities include:

```java
@Entity
@Table(name = "app_project")
@EntityListeners(AuditingEntityListener.class)
public class ProjectEntity {

    @Id
    private UUID id;

    // Auditing fields
    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    // Soft delete
    @Column(name = "deleted")
    private boolean deleted = false;

    // Optimistic locking
    @Version
    private Long version;
}
```

## Step 4.4: Creating a Custom Plugin

Create a plugin that generates OpenAPI specifications:

```java
package com.example.plugin;

import io.hexaglue.spi.plugin.*;
import io.hexaglue.spi.ir.*;

public class OpenApiPlugin implements HexaGluePlugin {

    public static final String PLUGIN_ID = "com.example.plugin.openapi";

    @Override
    public String id() {
        return PLUGIN_ID;
    }

    @Override
    public void execute(PluginContext context) {
        IrSnapshot ir = context.ir();
        PluginConfig config = context.config();

        String outputFile = config.getString("outputFile")
            .orElse("openapi.yaml");

        // Generate OpenAPI spec from driving ports
        StringBuilder spec = new StringBuilder();
        spec.append("openapi: 3.0.3\n");
        spec.append("info:\n");
        spec.append("  title: ").append(ir.metadata().basePackage()).append("\n");
        spec.append("paths:\n");

        for (Port port : ir.ports().drivingPorts()) {
            for (PortMethod method : port.methods()) {
                generatePath(spec, port, method);
            }
        }

        context.writer().writeResource(outputFile, spec.toString());
        context.diagnostics().info("Generated OpenAPI spec: " + outputFile);
    }

    private void generatePath(StringBuilder spec, Port port, PortMethod method) {
        // Generate path for each method...
    }
}
```

Register your plugin in `META-INF/services/io.hexaglue.spi.plugin.HexaGluePlugin`:

```
com.example.plugin.OpenApiPlugin
```

---

## What's Next?

- [User Guide](USER_GUIDE.md) - Deep dive into all concepts
- [Configuration Reference](CONFIGURATION.md) - All configuration options
- [SPI Reference](SPI_REFERENCE.md) - Plugin development API
- [Plugin Development](PLUGIN_DEVELOPMENT.md) - Build your own plugins

---

<div align="center">

**HexaGlue - Focus on business code, not infrastructure glue.**

Made with ❤️ by Scalastic<br>
Copyright 2026 Scalastic - Released under MPL-2.0

</div>