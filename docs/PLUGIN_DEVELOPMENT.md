# Plugin Development Guide

***How to create custom HexaGlue plugins.***

---

## Overview

HexaGlue plugins receive an analyzed domain model and generate code. This guide walks through creating a plugin from scratch.

**What you'll learn**:
1. Plugin structure and lifecycle
2. Accessing the domain model
3. Generating code
4. Testing plugins
5. Best practices

---

## Prerequisites

- Java 17+
- Maven 3.8+
- Familiarity with the [SPI Reference](SPI_REFERENCE.md)

---

## Creating a Plugin

### 1. Project Setup

Create a new Maven project with the SPI dependency:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project>
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>my-hexaglue-plugin</artifactId>
    <version>1.0.0-SNAPSHOT</version>

    <properties>
        <java.version>17</java.version>
        <maven.compiler.release>${java.version}</maven.compiler.release>
        <hexaglue.version>2.0.0-SNAPSHOT</hexaglue.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>io.hexaglue</groupId>
            <artifactId>hexaglue-spi</artifactId>
            <version>${hexaglue.version}</version>
        </dependency>

        <!-- Test dependencies -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.10.0</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

### 2. Implement the Plugin Interface

Create your plugin class implementing `HexaGluePlugin`:

```java
package com.example.plugin;

import io.hexaglue.spi.plugin.HexaGluePlugin;
import io.hexaglue.spi.plugin.PluginContext;

public class MyPlugin implements HexaGluePlugin {

    @Override
    public String id() {
        return "com.example.plugin.my-plugin";
    }

    @Override
    public void execute(PluginContext context) {
        context.diagnostics().info("MyPlugin is executing...");

        // Your plugin logic here
    }
}
```

### 3. Register the Plugin

Create the service loader file:

**File**: `src/main/resources/META-INF/services/io.hexaglue.spi.plugin.HexaGluePlugin`

```
com.example.plugin.MyPlugin
```

---

## Plugin Lifecycle

Plugins are executed in order:

1. **Discovery** - Plugins are discovered via `ServiceLoader`
2. **Dependency Resolution** - Plugins are ordered by dependencies
3. **Execution** - Each plugin's `execute()` method is called
4. **Completion** - All plugins have finished

```
┌─────────────────┐
│   Discovery     │  ServiceLoader finds all HexaGluePlugin implementations
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Dep. Resolution │  Plugins sorted by dependsOn()
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   Execution     │  execute(PluginContext) called for each plugin
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   Completion    │  All diagnostics collected and reported
└─────────────────┘
```

---

## Accessing the Domain Model

The `PluginContext` provides access to the analyzed application:

### Getting Domain Types

```java
@Override
public void execute(PluginContext context) {
    IrSnapshot ir = context.ir();
    DomainModel domain = ir.domain();

    // All aggregate roots
    for (DomainType aggregate : domain.aggregateRoots()) {
        processAggregate(aggregate);
    }

    // All entities (including aggregate roots)
    for (DomainType entity : domain.entities()) {
        processEntity(entity);
    }

    // All value objects
    for (DomainType valueObject : domain.valueObjects()) {
        processValueObject(valueObject);
    }

    // Find a specific type
    domain.findByQualifiedName("com.example.domain.Order")
        .ifPresent(this::processOrder);
}
```

### Working with Domain Types

```java
private void processAggregate(DomainType type) {
    // Basic info
    String className = type.simpleName();        // "Order"
    String fullName = type.qualifiedName();      // "com.example.domain.Order"
    String pkg = type.packageName();             // "com.example.domain"

    // Classification
    DomainKind kind = type.kind();               // AGGREGATE_ROOT
    ConfidenceLevel conf = type.confidence();    // HIGH, EXPLICIT, etc.
    JavaConstruct construct = type.construct();  // CLASS, RECORD, ENUM

    // Identity
    if (type.hasIdentity()) {
        Identity id = type.identity().get();
        String fieldName = id.fieldName();       // "id"
        TypeRef idType = id.type();              // OrderId
        TypeRef unwrapped = id.unwrappedType();  // UUID
        boolean wrapped = id.isWrapped();        // true
    }

    // Properties
    for (DomainProperty prop : type.properties()) {
        String name = prop.name();
        TypeRef propType = prop.type();
        Cardinality card = prop.cardinality();   // SINGLE, OPTIONAL, COLLECTION

        // Handle collections
        if (propType.isCollectionLike()) {
            TypeRef elementType = propType.unwrapElement();
            // elementType is the type inside the collection
        }
    }

    // Relationships
    for (DomainRelation relation : type.relations()) {
        RelationKind relKind = relation.kind();  // ONE_TO_MANY, etc.
        String target = relation.targetType();
        // ...
    }
}
```

### Working with Ports

```java
@Override
public void execute(PluginContext context) {
    PortModel ports = context.ir().ports();

    // All repositories
    for (Port repo : ports.repositories()) {
        processRepository(repo);
    }

    // All driving ports (use cases, commands, queries)
    for (Port port : ports.drivingPorts()) {
        processDrivingPort(port);
    }

    // All driven ports (repositories, gateways)
    for (Port port : ports.drivenPorts()) {
        processDrivenPort(port);
    }
}

private void processRepository(Port repo) {
    String name = repo.simpleName();           // "OrderRepository"
    PortKind kind = repo.kind();               // REPOSITORY
    PortDirection dir = repo.direction();      // DRIVEN

    // Types managed by this repository
    List<String> managed = repo.managedTypes(); // ["com.example.Order"]

    // Methods
    for (PortMethod method : repo.methods()) {
        String methodName = method.name();      // "findById"
        String returnType = method.returnType(); // "Optional<Order>"
        List<String> params = method.parameters();
    }
}
```

---

## Generating Code

### Writing Java Source Files

```java
@Override
public void execute(PluginContext context) {
    for (DomainType type : context.ir().domain().aggregateRoots()) {
        try {
            generateEntity(context, type);
        } catch (IOException e) {
            context.diagnostics().error("Failed to generate " + type.simpleName(), e);
        }
    }
}

private void generateEntity(PluginContext context, DomainType type) throws IOException {
    String packageName = "com.example.generated";
    String className = type.simpleName() + "Entity";

    String code = """
        package %s;

        public class %s {
            // Generated code
        }
        """.formatted(packageName, className);

    context.writer().writeJavaSource(packageName, className, code);
}
```

### Using Templates

For complex code generation, use the template engine:

```java
private void generateEntity(PluginContext context, DomainType type) throws IOException {
    String template = """
        package ${package};

        import jakarta.persistence.*;

        @Entity
        @Table(name = "${tableName}")
        public class ${className} {

            @Id
            private ${idType} id;

        ${fields}

            // Getters and setters
        ${accessors}
        }
        """;

    String code = context.templates().render(template, Map.of(
        "package", "com.example.infrastructure.persistence",
        "tableName", toSnakeCase(type.simpleName()),
        "className", type.simpleName() + "Entity",
        "idType", getIdType(type),
        "fields", generateFields(type),
        "accessors", generateAccessors(type)
    ));

    context.writer().writeJavaSource(
        "com.example.infrastructure.persistence",
        type.simpleName() + "Entity",
        code
    );
}

private String generateFields(DomainType type) {
    StringBuilder sb = new StringBuilder();
    for (DomainProperty prop : type.properties()) {
        if (!prop.isIdentity()) {
            sb.append("    private ")
              .append(prop.type().simpleName())
              .append(" ")
              .append(prop.name())
              .append(";\n");
        }
    }
    return sb.toString();
}
```

### Writing Documentation

Generate markdown or other documentation files:

```java
private void generateDocumentation(PluginContext context) throws IOException {
    StringBuilder md = new StringBuilder();
    md.append("# Domain Model\n\n");

    for (DomainType type : context.ir().domain().types()) {
        md.append("## ").append(type.simpleName()).append("\n\n");
        md.append("**Kind**: ").append(type.kind()).append("\n\n");

        if (!type.properties().isEmpty()) {
            md.append("### Properties\n\n");
            for (DomainProperty prop : type.properties()) {
                md.append("- `").append(prop.name())
                  .append("`: ").append(prop.type().simpleName())
                  .append("\n");
            }
            md.append("\n");
        }
    }

    context.writer().writeMarkdown("domain-model", md.toString());
}
```

### Writing Resources

Generate resource files (e.g., configuration files):

```java
private void generateServiceFile(PluginContext context) throws IOException {
    String content = "com.example.generated.MyGeneratedService";
    context.writer().writeResource(
        "META-INF/services/com.example.Service",
        content
    );
}
```

---

## Plugin Dependencies

Plugins can depend on other plugins:

```java
public class LiquibasePlugin implements HexaGluePlugin {

    @Override
    public String id() {
        return "com.example.plugin.liquibase";
    }

    @Override
    public List<String> dependsOn() {
        // This plugin requires JPA plugin to run first
        return List.of("io.hexaglue.plugin.jpa");
    }

    @Override
    public void execute(PluginContext context) {
        // Get data from the JPA plugin
        Optional<Map<String, EntityInfo>> entities = context.getOutput(
            "io.hexaglue.plugin.jpa",
            "generated-entities",
            Map.class
        );

        entities.ifPresent(this::generateChangeset);
    }
}
```

### Sharing Data Between Plugins

**Producer plugin**:
```java
public class JpaPlugin implements HexaGluePlugin {

    @Override
    public void execute(PluginContext context) {
        Map<String, EntityInfo> entities = new HashMap<>();

        for (DomainType type : context.ir().domain().aggregateRoots()) {
            EntityInfo info = generateEntity(context, type);
            entities.put(type.qualifiedName(), info);
        }

        // Share with other plugins
        context.setOutput("generated-entities", entities);
    }
}
```

**Consumer plugin**:
```java
public class LiquibasePlugin implements HexaGluePlugin {

    @Override
    public List<String> dependsOn() {
        return List.of("io.hexaglue.plugin.jpa");
    }

    @Override
    public void execute(PluginContext context) {
        Optional<Map<String, EntityInfo>> entities = context.getOutput(
            "io.hexaglue.plugin.jpa",
            "generated-entities",
            Map.class
        );

        // Use the entity information...
    }
}
```

---

## Error Handling

Use the `DiagnosticReporter` to report issues:

```java
@Override
public void execute(PluginContext context) {
    for (DomainType type : context.ir().domain().aggregateRoots()) {
        // Info - progress messages
        context.diagnostics().info("Processing " + type.simpleName());

        // Warn - potential issues
        if (type.confidence() == ConfidenceLevel.LOW) {
            context.diagnostics().warn(
                "Type " + type.simpleName() + " has low confidence classification"
            );
        }

        // Validate requirements
        if (!type.hasIdentity()) {
            context.diagnostics().error(
                "Aggregate root " + type.simpleName() + " has no identity field"
            );
            continue;  // Skip this type
        }

        try {
            generateCode(context, type);
        } catch (IOException e) {
            context.diagnostics().error(
                "Failed to generate code for " + type.simpleName(),
                e
            );
        }
    }
}
```

---

## Configuration

Plugins can read configuration from `hexaglue.yaml` (when implemented):

```java
@Override
public void execute(PluginContext context) {
    PluginConfig config = context.config();

    // Get string with default
    String basePackage = config.getString(
        "basePackage",
        context.ir().metadata().basePackage() + ".generated"
    );

    // Get boolean with default
    boolean generateAuditing = config.getBoolean("auditing", false);

    // Get optional value
    config.getString("customOption").ifPresent(value -> {
        // Handle custom option
    });
}
```

---

## Testing Plugins

### Unit Testing

Test individual generation logic:

```java
class MyPluginTest {

    @Test
    void shouldGenerateCorrectEntityName() {
        // Arrange
        DomainType type = createTestType("Order");

        // Act
        String entityName = MyPlugin.generateEntityName(type);

        // Assert
        assertEquals("OrderEntity", entityName);
    }

    private DomainType createTestType(String name) {
        return new DomainType(
            "com.example." + name,
            name,
            "com.example",
            DomainKind.AGGREGATE_ROOT,
            ConfidenceLevel.HIGH,
            JavaConstruct.CLASS,
            Optional.empty(),
            List.of(),
            List.of(),
            List.of(),
            SourceRef.unknown()
        );
    }
}
```

### Integration Testing

Test the full plugin execution:

```java
class MyPluginIntegrationTest {

    @Test
    void shouldGenerateEntityForAggregate() throws IOException {
        // Create test IR
        DomainType order = createOrderType();
        IrSnapshot ir = new IrSnapshot(
            new DomainModel(List.of(order)),
            new PortModel(List.of()),
            new IrMetadata("com.example", Instant.now(), "2.0.0", 1, 0)
        );

        // Create mock context
        TestPluginContext context = new TestPluginContext(ir);

        // Execute plugin
        MyPlugin plugin = new MyPlugin();
        plugin.execute(context);

        // Verify generated files
        assertTrue(context.getWriter().fileExists(
            "com.example.generated",
            "OrderEntity"
        ));

        String content = context.getWriter().getFileContent(
            "com.example.generated",
            "OrderEntity"
        );
        assertTrue(content.contains("@Entity"));
        assertTrue(content.contains("class OrderEntity"));
    }
}
```

---

## Best Practices

### 1. Plugin ID Naming

Use reverse domain notation:
```java
// Good
"io.hexaglue.plugin.jpa"
"com.mycompany.plugin.custom"

// Bad
"jpa-plugin"
"MyPlugin"
```

### 2. Handle Missing Data Gracefully

```java
@Override
public void execute(PluginContext context) {
    if (context.ir().isEmpty()) {
        context.diagnostics().info("No domain types found, skipping generation");
        return;
    }

    for (DomainType type : context.ir().domain().aggregateRoots()) {
        // Safe access to optional data
        String idType = type.identity()
            .map(id -> id.unwrappedType().simpleName())
            .orElse("Long");

        // ...
    }
}
```

### 3. Use Confidence Levels

```java
@Override
public void execute(PluginContext context) {
    for (DomainType type : context.ir().domain().aggregateRoots()) {
        // Only generate for reliable classifications
        if (!type.confidence().isReliable()) {
            context.diagnostics().warn(
                "Skipping " + type.simpleName() + " due to low confidence"
            );
            continue;
        }

        generateCode(context, type);
    }
}
```

### 4. Avoid Overwriting User Files

```java
private void generateEntity(PluginContext context, DomainType type) throws IOException {
    String packageName = "com.example.generated";
    String className = type.simpleName() + "Entity";

    // Check if file already exists
    if (context.writer().exists(packageName, className)) {
        context.diagnostics().info(
            className + " already exists, skipping"
        );
        return;
    }

    // Generate only if not exists
    context.writer().writeJavaSource(packageName, className, code);
}
```

### 5. Log Progress for Long Operations

```java
@Override
public void execute(PluginContext context) {
    List<DomainType> types = context.ir().domain().aggregateRoots();

    context.diagnostics().info(
        "Generating entities for " + types.size() + " aggregates..."
    );

    int count = 0;
    for (DomainType type : types) {
        generateEntity(context, type);
        count++;
    }

    context.diagnostics().info("Generated " + count + " entities");
}
```

---

## Project Structure

Recommended plugin project structure:

```
my-hexaglue-plugin/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/plugin/
│   │   │       ├── MyPlugin.java
│   │   │       ├── generators/
│   │   │       │   ├── EntityGenerator.java
│   │   │       │   └── MapperGenerator.java
│   │   │       └── util/
│   │   │           └── NamingUtils.java
│   │   └── resources/
│   │       ├── META-INF/services/
│   │       │   └── io.hexaglue.spi.plugin.HexaGluePlugin
│   │       └── templates/
│   │           ├── entity.java.template
│   │           └── mapper.java.template
│   └── test/
│       └── java/
│           └── com/example/plugin/
│               ├── MyPluginTest.java
│               └── generators/
│                   └── EntityGeneratorTest.java
└── README.md
```

---

## Using Your Plugin

Add your plugin as a dependency to the Maven plugin configuration:

```xml
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
        <!-- Your custom plugin -->
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>my-hexaglue-plugin</artifactId>
            <version>1.0.0-SNAPSHOT</version>
        </dependency>
    </dependencies>
</plugin>
```

---

<div align="center">

**HexaGlue - Focus on business code, not infrastructure glue.**

</div>
