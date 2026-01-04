# SPI Reference

***Complete reference for HexaGlue's Service Provider Interface.***

---

## Overview

The HexaGlue SPI (Service Provider Interface) allows plugins to receive analyzed domain models and generate code. Plugins implement the `HexaGluePlugin` interface and are discovered via Java's `ServiceLoader` mechanism.

**Module**: `hexaglue-spi`

**Maven Dependency**:
```xml
<dependency>
    <groupId>io.hexaglue</groupId>
    <artifactId>hexaglue-spi</artifactId>
    <version>${hexaglue.version}</version>
</dependency>
```

---

## Plugin Interface

### HexaGluePlugin

The main interface that all plugins must implement.

```java
package io.hexaglue.spi.plugin;

public interface HexaGluePlugin {

    /**
     * Unique identifier for this plugin.
     * Convention: use reverse domain notation, e.g., "io.hexaglue.plugin.jpa"
     */
    String id();

    /**
     * Returns the list of plugin IDs this plugin depends on.
     * Dependencies are executed before this plugin.
     */
    default List<String> dependsOn() {
        return List.of();
    }

    /**
     * Executes the plugin with the given context.
     */
    void execute(PluginContext context);
}
```

**Example**:
```java
public class MyPlugin implements HexaGluePlugin {

    @Override
    public String id() {
        return "com.example.plugin.myplugin";
    }

    @Override
    public void execute(PluginContext context) {
        IrSnapshot ir = context.ir();

        for (DomainType type : ir.domain().aggregateRoots()) {
            generateEntity(context, type);
        }
    }

    private void generateEntity(PluginContext context, DomainType type) {
        // Generate code...
    }
}
```

### Plugin Registration

Register plugins via `META-INF/services/io.hexaglue.spi.plugin.HexaGluePlugin`:

```
com.example.plugin.MyPlugin
```

---

## PluginContext

The execution context provided to plugins during execution.

```java
public interface PluginContext {

    /** Returns the analyzed application model. */
    IrSnapshot ir();

    /** Returns the plugin configuration. */
    PluginConfig config();

    /** Returns the code writer for generating files. */
    CodeWriter writer();

    /** Returns the diagnostic reporter. */
    DiagnosticReporter diagnostics();

    /** Returns the template engine for code generation. */
    TemplateEngine templates();

    /** Stores an output value that can be retrieved by other plugins. */
    <T> void setOutput(String key, T value);

    /** Retrieves an output value stored by another plugin. */
    <T> Optional<T> getOutput(String pluginId, String key, Class<T> type);

    /** Returns the ID of the currently executing plugin. */
    String currentPluginId();
}
```

### Plugin Communication

Plugins can share data using `setOutput()` and `getOutput()`:

```java
// In first plugin (io.hexaglue.plugin.jpa)
Map<String, EntityInfo> entities = generateEntities();
context.setOutput("generated-entities", entities);

// In second plugin (depends on io.hexaglue.plugin.jpa)
Optional<Map<String, EntityInfo>> entities = context.getOutput(
    "io.hexaglue.plugin.jpa",
    "generated-entities",
    Map.class
);
```

---

## Intermediate Representation (IR)

The IR is the complete analyzed model of the application, passed to plugins as an immutable snapshot.

### IrSnapshot

```java
public record IrSnapshot(
    DomainModel domain,    // Domain types
    PortModel ports,       // Port interfaces
    IrMetadata metadata    // Analysis metadata
) {
    /** Returns true if this snapshot has no domain types or ports. */
    public boolean isEmpty();

    /** Creates an empty snapshot (for error cases). */
    public static IrSnapshot empty(String basePackage);
}
```

### IrMetadata

```java
public record IrMetadata(
    String basePackage,      // The base package that was analyzed
    Instant timestamp,       // When the analysis was performed
    String engineVersion,    // The HexaGlue engine version
    int typeCount,           // Total number of types analyzed
    int portCount            // Total number of ports detected
) {}
```

---

## Domain Model

### DomainModel

Container for all analyzed domain types.

```java
public record DomainModel(List<DomainType> types) {

    /** Finds a domain type by its qualified name. */
    Optional<DomainType> findByQualifiedName(String qualifiedName);

    /** Returns all types of a specific kind. */
    List<DomainType> typesOfKind(DomainKind kind);

    /** Returns all aggregate roots. */
    List<DomainType> aggregateRoots();

    /** Returns all entities (including aggregate roots). */
    List<DomainType> entities();

    /** Returns all value objects. */
    List<DomainType> valueObjects();
}
```

### DomainType

A single domain type with its classification and properties.

```java
public record DomainType(
    String qualifiedName,           // e.g., "com.example.domain.Order"
    String simpleName,              // e.g., "Order"
    String packageName,             // e.g., "com.example.domain"
    DomainKind kind,                // AGGREGATE_ROOT, ENTITY, VALUE_OBJECT, etc.
    ConfidenceLevel confidence,     // EXPLICIT, HIGH, MEDIUM, LOW
    JavaConstruct construct,        // CLASS, RECORD, ENUM, INTERFACE
    Optional<Identity> identity,    // Identity information if applicable
    List<DomainProperty> properties,// Domain properties
    List<DomainRelation> relations, // Relationships to other types
    List<String> annotations,       // Annotation qualified names
    SourceRef sourceRef             // Source location
) {
    boolean hasIdentity();
    boolean isAggregateRoot();
    boolean isEntity();
    boolean isValueObject();
    boolean isRecord();
    boolean hasRelations();
    List<DomainRelation> relationsOfKind(RelationKind kind);
    List<DomainRelation> embeddedRelations();
    List<DomainRelation> entityRelations();
}
```

### DomainKind

Classification of domain types according to DDD tactical patterns.

| Value | Description |
|-------|-------------|
| `AGGREGATE_ROOT` | Entry point to an aggregate, has identity, manages invariants |
| `ENTITY` | Entity within an aggregate (not the root), has identity |
| `VALUE_OBJECT` | Immutable, no identity, defined by its attributes |
| `IDENTIFIER` | Wraps a primitive identity value |
| `DOMAIN_EVENT` | Immutable record of something that happened |
| `DOMAIN_SERVICE` | Stateless operation that doesn't belong to an entity |

### DomainProperty

A property of a domain type.

```java
public record DomainProperty(
    String name,                 // Property name
    TypeRef type,                // Type reference with generics info
    Cardinality cardinality,     // SINGLE, OPTIONAL, COLLECTION
    Nullability nullability,     // NULLABLE, NON_NULL
    boolean isIdentity,          // True if this is the identity property
    boolean isEmbedded,          // True if embedded value object
    RelationInfo relationInfo    // Relationship info (null if simple)
) {
    boolean hasRelation();
    boolean isSimple();
    boolean isEntityCollection();
    boolean isEmbeddedCollection();
}
```

### Identity

Identity information for an entity or aggregate root.

```java
public record Identity(
    String fieldName,              // e.g., "id", "orderId"
    TypeRef type,                  // Declared type (e.g., OrderId)
    TypeRef unwrappedType,         // Underlying type (e.g., UUID)
    IdentityStrategy strategy,     // ASSIGNED, AUTO, SEQUENCE, UUID
    IdentityWrapperKind wrapperKind // RECORD, CLASS, or NONE
) {
    boolean isWrapped();
}
```

**Identity Strategies**:
| Value | Description |
|-------|-------------|
| `ASSIGNED` | Application assigns the ID |
| `AUTO` | Database auto-generates the ID |
| `SEQUENCE` | Database sequence generates the ID |
| `UUID` | UUID generated by application |

### TypeRef

Reference to a type with support for generics.

```java
public record TypeRef(
    String qualifiedName,           // e.g., "java.util.List"
    String simpleName,              // e.g., "List"
    List<TypeRef> typeArguments,    // Type arguments if parameterized
    boolean primitive,              // True if primitive type
    boolean array,                  // True if array type
    int arrayDimensions,            // Array dimensions (0 if not array)
    Cardinality cardinality         // SINGLE, OPTIONAL, COLLECTION
) {
    boolean isParameterized();
    boolean isOptionalLike();
    boolean isCollectionLike();
    boolean isMapLike();
    TypeRef firstArgument();
    TypeRef unwrapElement();        // Unwraps Optional/Collection
    boolean requiresImport();
    String packageName();

    // Factory methods
    static TypeRef of(String qualifiedName);
    static TypeRef primitive(String name);
    static TypeRef parameterized(String qualifiedName, TypeRef... arguments);
    static TypeRef array(TypeRef componentType, int dimensions);
}
```

**Example - Working with Collections**:
```java
DomainProperty itemsProp = ...;  // List<LineItem> items

// Get the element type from a collection
TypeRef elementType = itemsProp.type().unwrapElement();
String elementName = elementType.qualifiedName();  // "com.example.LineItem"
```

### RelationInfo

Information about relationships between domain types.

```java
public record RelationInfo(
    RelationKind kind,      // ONE_TO_ONE, ONE_TO_MANY, etc.
    String targetType,      // Fully qualified target type name
    String mappedBy,        // Field on inverse side (bidirectional)
    boolean owning          // True if owning side
) {
    boolean isBidirectional();
    boolean isCollection();
    boolean isEmbedded();

    static RelationInfo unidirectional(RelationKind kind, String targetType);
    static RelationInfo owning(RelationKind kind, String targetType);
    static RelationInfo inverse(RelationKind kind, String targetType, String mappedBy);
}
```

### RelationKind

Types of relationships between domain types.

| Value | Description | Example |
|-------|-------------|---------|
| `ONE_TO_ONE` | One-to-one relationship | Order has one ShippingAddress |
| `ONE_TO_MANY` | One-to-many relationship | Order has many LineItems |
| `MANY_TO_ONE` | Many-to-one relationship | LineItem belongs to one Order |
| `MANY_TO_MANY` | Many-to-many relationship | Product has many Categories |
| `EMBEDDED` | Embedded value object | Order embeds Address |
| `ELEMENT_COLLECTION` | Collection of embeddables | Order has Tags collection |

### ConfidenceLevel

Confidence level of classification decisions.

| Value | Description |
|-------|-------------|
| `EXPLICIT` | Based on explicit annotation (highest) |
| `HIGH` | Based on strong heuristics |
| `MEDIUM` | Based on moderate heuristics |
| `LOW` | Based on weak heuristics (verify manually) |

```java
public enum ConfidenceLevel {
    EXPLICIT, HIGH, MEDIUM, LOW;

    boolean isReliable();                  // True for EXPLICIT and HIGH
    boolean isAtLeast(ConfidenceLevel other);
}
```

### Cardinality

Cardinality of a property.

| Value | Description | Example |
|-------|-------------|---------|
| `SINGLE` | Single required value | `String name` |
| `OPTIONAL` | Optional value | `Optional<String> middleName` |
| `COLLECTION` | Collection of values | `List<LineItem> items` |

---

## Port Model

### PortModel

Container for all analyzed port interfaces.

```java
public record PortModel(List<Port> ports) {

    Optional<Port> findByQualifiedName(String qualifiedName);
    List<Port> portsOfKind(PortKind kind);
    List<Port> drivingPorts();    // Primary/inbound ports
    List<Port> drivenPorts();     // Secondary/outbound ports
    List<Port> repositories();
}
```

### Port

A port interface in the hexagonal architecture.

```java
public record Port(
    String qualifiedName,           // e.g., "com.example.ports.OrderRepository"
    String simpleName,              // e.g., "OrderRepository"
    String packageName,             // e.g., "com.example.ports"
    PortKind kind,                  // REPOSITORY, GATEWAY, USE_CASE, etc.
    PortDirection direction,        // DRIVING or DRIVEN
    ConfidenceLevel confidence,     // Classification confidence
    List<String> managedTypes,      // Domain types in signatures
    List<PortMethod> methods,       // Port methods
    List<String> annotations,       // Annotation qualified names
    SourceRef sourceRef             // Source location
) {
    boolean isRepository();
    boolean isDriving();
    boolean isDriven();
}
```

### PortKind

Classification of port interfaces.

| Value | Description |
|-------|-------------|
| `REPOSITORY` | Persistence abstraction for aggregates |
| `GATEWAY` | External system integration abstraction |
| `USE_CASE` | Application service defining a business operation |
| `COMMAND` | Handles a specific command |
| `QUERY` | Handles a specific query |
| `EVENT_PUBLISHER` | Publishes domain events |
| `GENERIC` | Could not be classified more specifically |

### PortDirection

Direction of a port in hexagonal architecture.

| Value | Description |
|-------|-------------|
| `DRIVING` | Primary/inbound port (called by external actors) |
| `DRIVEN` | Secondary/outbound port (called by the application) |

### PortMethod

A method declared on a port interface.

```java
public record PortMethod(
    String name,                 // Method name
    String returnType,           // Return type (qualified name)
    List<String> parameters      // Parameter types (qualified names)
) {}
```

---

## Code Generation

### CodeWriter

Interface for writing generated files.

```java
public interface CodeWriter {

    // Java source generation
    void writeJavaSource(String packageName, String className, String content) throws IOException;
    boolean exists(String packageName, String className);
    void delete(String packageName, String className) throws IOException;
    Path getOutputDirectory();

    // Resource generation
    void writeResource(String path, String content) throws IOException;
    boolean resourceExists(String path);
    void deleteResource(String path) throws IOException;

    // Documentation generation
    void writeDoc(String path, String content) throws IOException;
    void writeMarkdown(String path, String content) throws IOException;
    boolean docExists(String path);
    void deleteDoc(String path) throws IOException;
    Path getDocsOutputDirectory();
}
```

**Example - Generating Java source**:
```java
String code = """
    package %s;

    public class %sEntity {
        private %s id;
        // ...
    }
    """.formatted(packageName, type.simpleName(), idType);

context.writer().writeJavaSource(packageName, type.simpleName() + "Entity", code);
```

### TemplateEngine

Simple template engine for code generation.

```java
public interface TemplateEngine {

    /** Renders a template with ${variable} placeholders. */
    String render(String template, Map<String, Object> context);

    /** Registers a custom helper function. */
    void registerHelper(String name, Function<String, String> helper);

    /** Loads a template from a resource path. */
    String loadTemplate(String resourcePath);
}
```

**Example**:
```java
String template = """
    package ${package};

    public class ${className} {
        private ${idType} id;
    }
    """;

String code = context.templates().render(template, Map.of(
    "package", "com.example.infrastructure",
    "className", "OrderEntity",
    "idType", "UUID"
));
```

---

## Diagnostics

### DiagnosticReporter

Reports diagnostics (info, warnings, errors) during plugin execution.

```java
public interface DiagnosticReporter {

    void info(String message);
    void warn(String message);
    void error(String message);
    void error(String message, Throwable cause);
}
```

**Example**:
```java
if (!type.hasIdentity()) {
    context.diagnostics().warn(
        "Type " + type.simpleName() + " has no identity field"
    );
}
```

---

## Configuration

### PluginConfig

Type-safe access to plugin configuration.

```java
public interface PluginConfig {

    Optional<String> getString(String key);
    Optional<Boolean> getBoolean(String key);
    Optional<Integer> getInteger(String key);

    String getString(String key, String defaultValue);
    boolean getBoolean(String key, boolean defaultValue);
}
```

**Example**:
```java
String basePackage = context.config().getString(
    "basePackage",
    "com.example.infrastructure"
);

boolean generateAuditing = context.config().getBoolean("auditing", false);
```

> **Note**: Plugin configuration via `hexaglue.yaml` is planned but not yet implemented. Configuration will be available under each plugin's namespace when implemented.

---

## Complete Plugin Example

```java
package com.example.plugin;

import io.hexaglue.spi.ir.*;
import io.hexaglue.spi.plugin.*;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class EntityGeneratorPlugin implements HexaGluePlugin {

    @Override
    public String id() {
        return "com.example.plugin.entity-generator";
    }

    @Override
    public List<String> dependsOn() {
        return List.of();  // No dependencies
    }

    @Override
    public void execute(PluginContext context) {
        IrSnapshot ir = context.ir();
        String basePackage = context.config().getString(
            "basePackage",
            ir.metadata().basePackage() + ".infrastructure"
        );

        context.diagnostics().info(
            "Generating entities for " + ir.domain().aggregateRoots().size() + " aggregates"
        );

        for (DomainType aggregate : ir.domain().aggregateRoots()) {
            try {
                generateEntity(context, aggregate, basePackage);
            } catch (IOException e) {
                context.diagnostics().error(
                    "Failed to generate entity for " + aggregate.simpleName(),
                    e
                );
            }
        }
    }

    private void generateEntity(
            PluginContext context,
            DomainType type,
            String basePackage) throws IOException {

        String entityPackage = basePackage + ".persistence";
        String entityName = type.simpleName() + "Entity";

        // Get identity type
        String idType = type.identity()
            .map(id -> id.unwrappedType().simpleName())
            .orElse("Long");

        // Generate entity code using template
        String code = context.templates().render("""
            package ${package};

            import jakarta.persistence.*;

            @Entity
            @Table(name = "${tableName}")
            public class ${entityName} {

                @Id
                private ${idType} id;

                ${fields}

                // Getters and setters...
            }
            """, Map.of(
                "package", entityPackage,
                "tableName", toSnakeCase(type.simpleName()),
                "entityName", entityName,
                "idType", idType,
                "fields", generateFields(type)
            ));

        context.writer().writeJavaSource(entityPackage, entityName, code);
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

    private String toSnakeCase(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
}
```

---

## Summary of Key Types

| Package | Type | Purpose |
|---------|------|---------|
| `io.hexaglue.spi.plugin` | `HexaGluePlugin` | Main plugin interface |
| `io.hexaglue.spi.plugin` | `PluginContext` | Execution context |
| `io.hexaglue.spi.plugin` | `CodeWriter` | File generation |
| `io.hexaglue.spi.plugin` | `DiagnosticReporter` | Error/warning reporting |
| `io.hexaglue.spi.plugin` | `PluginConfig` | Configuration access |
| `io.hexaglue.spi.plugin` | `TemplateEngine` | Template rendering |
| `io.hexaglue.spi.ir` | `IrSnapshot` | Complete analyzed model |
| `io.hexaglue.spi.ir` | `DomainModel` | Domain types container |
| `io.hexaglue.spi.ir` | `DomainType` | Single domain type |
| `io.hexaglue.spi.ir` | `DomainProperty` | Property of a type |
| `io.hexaglue.spi.ir` | `Identity` | Identity information |
| `io.hexaglue.spi.ir` | `TypeRef` | Type reference with generics |
| `io.hexaglue.spi.ir` | `PortModel` | Ports container |
| `io.hexaglue.spi.ir` | `Port` | Single port interface |
| `io.hexaglue.spi.ir` | `PortMethod` | Port method |

---

<div align="center">

**HexaGlue - Focus on business code, not infrastructure glue.**

Made with ❤️ by Scalastic<br>
Copyright 2026 Scalastic - Released under MPL-2.0

</div>
