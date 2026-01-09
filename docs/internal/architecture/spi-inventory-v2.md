# HexaGlue SPI v2 Public API Inventory

## Document Purpose

This document inventories all public APIs in the `hexaglue-spi` module as of version 2.0.0-SNAPSHOT. This serves as the baseline for v3 API evolution, ensuring backward compatibility.

**Version**: 2.0.0-SNAPSHOT  
**Date**: 2026-01-08  
**Module**: hexaglue-spi  
**Status**: Baseline for v3 redesign  

---

## SPI Stability Contract

The HexaGlue SPI follows strict stability rules:

1. **ZERO external dependencies** - Plugins depend only on hexaglue-spi
2. **Backward compatibility required** - Breaking changes require major version bump
3. **Deprecation policy** - Deprecated APIs must be supported for one major version
4. **Extension via default methods** - New interface methods must have default implementations

---

## Package Structure

```
io.hexaglue.spi
├── ir                      # Intermediate Representation (IR) types
│   └── testing             # Test builders for IR types
└── plugin                  # Plugin API contracts
```

---

## IR Types (`io.hexaglue.spi.ir`)

### Core IR

#### `IrSnapshot` (record)
**Purpose**: Root of the IR tree, contains complete analyzed application.

**API**:
```java
public record IrSnapshot(
    DomainModel domain,
    PortModel ports,
    IrMetadata metadata
)

// Methods
public Optional<DomainType> findDomainType(String fqn)
public Optional<Port> findPort(String fqn)
public List<DomainType> aggregateRoots()
public List<DomainType> entities()
public List<DomainType> valueObjects()
public List<Port> drivingPorts()
public List<Port> drivenPorts()
```

**Since**: 1.0.0
**Stability**: STABLE

---

#### `DomainModel` (record)
**Purpose**: Collection of all domain types.

**API**:
```java
public record DomainModel(List<DomainType> types)

// Methods
public List<DomainType> ofKind(DomainKind kind)
public Optional<DomainType> findByName(String fqn)
```

**Since**: 1.0.0  
**Stability**: STABLE  

---

#### `DomainType` (record)
**Purpose**: Represents a classified domain type (entity, value object, etc.).

**API**:
```java
public record DomainType(
    String fqn,                           // Fully qualified name
    String simpleName,
    String packageName,
    DomainKind kind,                      // AGGREGATE_ROOT, ENTITY, VALUE_OBJECT, etc.
    ConfidenceLevel confidence,
    JavaConstruct construct,              // CLASS, RECORD, INTERFACE, ENUM
    List<DomainProperty> properties,
    List<DomainRelation> relations,
    Optional<Identity> identity,
    Map<String, Object> metadata,
    SourceRef sourceRef
)

// Query methods
public boolean isAggregateRoot()
public boolean isEntity()
public boolean isValueObject()
public boolean isDomainEvent()
public boolean hasIdentity()
public Optional<DomainProperty> findProperty(String name)
public List<DomainProperty> identityProperties()
public List<DomainRelation> ownedRelations()
```

**Since**: 1.0.0  
**Stability**: STABLE  

**Notes**:
- `metadata` map allows extensibility without breaking changes
- Immutable - all collections are unmodifiable

---

#### `DomainProperty` (record)
**Purpose**: Represents a field/property of a domain type.

**API**:
```java
public record DomainProperty(
    String name,
    TypeRef type,
    Nullability nullability,
    boolean collection,
    Optional<Cardinality> cardinality,
    Optional<RelationInfo> relationInfo,
    Map<String, Object> metadata
)

// Query methods
public boolean isIdentity()
public boolean isRelation()
public boolean isPrimitive()
public boolean isEmbedded()
```

**Since**: 1.0.0  
**Stability**: STABLE  

---

#### `DomainRelation` (record)
**Purpose**: Represents an association between domain types.

**API**:
```java
public record DomainRelation(
    String name,
    String sourceType,
    String targetType,
    RelationKind kind,              // ONE_TO_ONE, ONE_TO_MANY, MANY_TO_ONE, MANY_TO_MANY
    Cardinality cardinality,
    boolean owning,
    Optional<String> mappedBy,
    FetchType fetchType,
    List<CascadeType> cascades
)
```

**Since**: 1.0.0  
**Stability**: STABLE  

---

#### `PortModel` (record)
**Purpose**: Collection of all ports.

**API**:
```java
public record PortModel(List<Port> ports)

// Query methods
public List<Port> ofDirection(PortDirection direction)
public List<Port> ofKind(PortKind kind)
public Optional<Port> findByName(String fqn)
```

**Since**: 1.0.0  
**Stability**: STABLE  

---

#### `Port` (record)
**Purpose**: Represents a classified port interface.

**API**:
```java
public record Port(
    String fqn,
    String simpleName,
    String packageName,
    PortDirection direction,        // DRIVING or DRIVEN
    PortKind kind,                  // REPOSITORY, GATEWAY, NOTIFICATION_PROVIDER, etc.
    ConfidenceLevel confidence,
    List<PortMethod> methods,
    Map<String, Object> metadata,
    SourceRef sourceRef
)

// Query methods
public boolean isDriving()
public boolean isDriven()
public boolean isRepository()
public boolean isGateway()
```

**Since**: 1.0.0  
**Stability**: STABLE  

---

#### `PortMethod` (record)
**Purpose**: Represents a method on a port interface.

**API**:
```java
public record PortMethod(
    String name,
    String returnType,
    List<String> parameters
)
```

**Since**: 1.0.0  
**Stability**: STABLE  

---

### Supporting Types

#### `Identity` (record)
**Purpose**: Describes identity characteristics of an entity.

**API**:
```java
public record Identity(
    String propertyName,
    TypeRef type,
    IdentityStrategy strategy,      // AUTO, MANUAL, UUID, etc.
    IdentityWrapperKind wrapperKind // UNWRAPPED, VALUE_OBJECT, etc.
)
```

**Since**: 1.0.0  
**Stability**: STABLE  

---

#### `TypeRef` (record)
**Purpose**: Reference to a Java type (primitive, class, generic).

**API**:
```java
public record TypeRef(
    String fqn,
    String simpleName,
    boolean primitive,
    boolean generic,
    List<TypeRef> typeArguments
)

// Factory methods
public static TypeRef of(String fqn)
public static TypeRef primitive(String name)
public static TypeRef generic(String fqn, List<TypeRef> args)
```

**Since**: 1.0.0  
**Stability**: STABLE  

---

#### `SourceRef` (record)
**Purpose**: Location in source code for traceability.

**API**:
```java
public record SourceRef(
    String filePath,
    int lineStart,
    int lineEnd
)
```

**Since**: 1.0.0  
**Stability**: STABLE  

---

#### `IrMetadata` (record)
**Purpose**: Metadata about the IR snapshot.

**API**:
```java
public record IrMetadata(
    String basePackage,
    Instant timestamp,
    String engineVersion,
    int typeCount,
    int portCount
)
```

**Since**: 1.0.0  
**Stability**: STABLE  

---

### Enumerations

#### `DomainKind` (enum)
**Purpose**: Classification of domain types.

**Values**:
```java
public enum DomainKind {
    AGGREGATE_ROOT,
    ENTITY,
    VALUE_OBJECT,
    DOMAIN_EVENT,
    IDENTIFIER,
    APPLICATION_SERVICE,
    INBOUND_ONLY,
    OUTBOUND_ONLY,
    SAGA
}
```

**Since**: 1.0.0  
**Stability**: STABLE  

**Notes**:
- Adding new values is backward compatible
- Removing values requires major version bump

---

#### `PortKind` (enum)
**Purpose**: Classification of port interfaces.

**Values**:
```java
public enum PortKind {
    REPOSITORY,
    GATEWAY,
    NOTIFICATION_PROVIDER,
    USE_CASE,
    QUERY,
    COMMAND
}
```

**Since**: 1.0.0  
**Stability**: STABLE  

---

#### `PortDirection` (enum)
**Purpose**: Port direction in hexagonal architecture.

**Values**:
```java
public enum PortDirection {
    DRIVING,    // Primary, inbound
    DRIVEN      // Secondary, outbound
}
```

**Since**: 1.0.0  
**Stability**: STABLE  

---

#### `ConfidenceLevel` (enum)
**Purpose**: Classification confidence.

**Values**:
```java
public enum ConfidenceLevel {
    HIGH(100),
    MEDIUM(75),
    LOW(50),
    VERY_LOW(25);

    public int weight();
}
```

**Since**: 1.0.0  
**Stability**: STABLE  

---

#### `JavaConstruct` (enum)
**Purpose**: Java language construct kind.

**Values**:
```java
public enum JavaConstruct {
    CLASS,
    RECORD,
    INTERFACE,
    ENUM,
    ANNOTATION
}
```

**Since**: 1.0.0  
**Stability**: STABLE  

---

#### `Cardinality` (enum)
**Values**: `OPTIONAL`, `REQUIRED`, `MANY`

**Since**: 1.0.0  
**Stability**: STABLE  

---

#### `RelationKind` (enum)
**Values**: `ONE_TO_ONE`, `ONE_TO_MANY`, `MANY_TO_ONE`, `MANY_TO_MANY`

**Since**: 1.0.0  
**Stability**: STABLE  

---

#### `FetchType` (enum)
**Values**: `EAGER`, `LAZY`

**Since**: 1.0.0  
**Stability**: STABLE  

---

#### `CascadeType` (enum)
**Values**: `ALL`, `PERSIST`, `MERGE`, `REMOVE`, `REFRESH`, `DETACH`

**Since**: 1.0.0  
**Stability**: STABLE  

---

#### `IdentityStrategy` (enum)
**Values**: `AUTO`, `MANUAL`, `UUID`, `SEQUENCE`, `IDENTITY`

**Since**: 1.0.0  
**Stability**: STABLE  

---

#### `IdentityWrapperKind` (enum)
**Values**: `UNWRAPPED`, `VALUE_OBJECT`, `BOXED_PRIMITIVE`

**Since**: 1.0.0  
**Stability**: STABLE  

---

#### `Nullability` (enum)
**Values**: `NULLABLE`, `NON_NULL`, `UNKNOWN`

**Since**: 1.0.0  
**Stability**: STABLE  

---

## Plugin API (`io.hexaglue.spi.plugin`)

### Core Plugin Interface

#### `HexaGluePlugin` (interface)
**Purpose**: Plugin lifecycle contract.

**API**:
```java
public interface HexaGluePlugin {
    String id();
    List<String> dependsOn();
    void execute(IrSnapshot snapshot, PluginContext context);
}
```

**Since**: 1.0.0  
**Stability**: STABLE  

**Notes**:
- Plugins discovered via Java ServiceLoader
- `dependsOn()` enables plugin ordering
- `execute()` is the main plugin entry point

---

#### `PluginContext` (interface)
**Purpose**: Execution environment for plugins.

**API**:
```java
public interface PluginContext {
    CodeWriter codeWriter();
    DiagnosticReporter diagnostics();
    PluginConfig config();
    Path outputDirectory();
    Path sourceDirectory();
}
```

**Since**: 1.0.0  
**Stability**: STABLE  

---

#### `CodeWriter` (interface)
**Purpose**: File writing abstraction.

**API**:
```java
public interface CodeWriter {
    void writeJavaClass(String packageName, String className, String content);
    void writeFile(Path relativePath, String content);
    void writeFile(Path relativePath, byte[] content);
    Path resolveOutputPath(String packageName, String fileName);
}
```

**Since**: 1.0.0  
**Stability**: STABLE  

---

#### `DiagnosticReporter` (interface)
**Purpose**: Error/warning reporting.

**API**:
```java
public interface DiagnosticReporter {
    void info(String message);
    void warn(String message);
    void error(String message);
    void error(String message, Throwable cause);
}
```

**Since**: 1.0.0  
**Stability**: STABLE  

---

#### `PluginConfig` (interface)
**Purpose**: Plugin configuration access.

**API**:
```java
public interface PluginConfig {
    Optional<String> getString(String key);
    Optional<Integer> getInteger(String key);
    Optional<Boolean> getBoolean(String key);
    Map<String, Object> getAll();
}
```

**Since**: 1.0.0  
**Stability**: STABLE  

---

#### `TemplateEngine` (interface)
**Purpose**: Template processing abstraction.

**API**:
```java
public interface TemplateEngine {
    String render(String template, Map<String, Object> context);
}
```

**Since**: 1.0.0  
**Stability**: STABLE  

---

#### `SimpleTemplateEngine` (class)
**Purpose**: Basic template engine implementation.

**API**:
```java
public final class SimpleTemplateEngine implements TemplateEngine {
    public SimpleTemplateEngine();
    public String render(String template, Map<String, Object> context);
}
```

**Since**: 1.0.0  
**Stability**: STABLE  

**Notes**:
- Supports `{{variable}}` placeholders
- Supports `{{#each list}}...{{/each}}` loops
- Plugins can use custom template engines

---

## Testing API (`io.hexaglue.spi.ir.testing`)

### Builders

#### `IrSnapshotBuilder` (class)
**Purpose**: Fluent builder for IrSnapshot in tests.

**API**:
```java
public final class IrSnapshotBuilder {
    public static IrSnapshotBuilder snapshot();
    public IrSnapshotBuilder withDomainType(DomainType type);
    public IrSnapshotBuilder withPort(Port port);
    public IrSnapshotBuilder withBasePackage(String pkg);
    public IrSnapshot build();
}
```

**Since**: 1.0.0  
**Stability**: TEST_ONLY  

---

#### `DomainTypeBuilder` (class)
**Purpose**: Fluent builder for DomainType in tests.

**API**:
```java
public final class DomainTypeBuilder {
    public static DomainTypeBuilder domainType(String fqn);
    public DomainTypeBuilder kind(DomainKind kind);
    public DomainTypeBuilder confidence(ConfidenceLevel level);
    public DomainTypeBuilder withProperty(DomainProperty prop);
    public DomainTypeBuilder withRelation(DomainRelation rel);
    public DomainTypeBuilder withIdentity(Identity id);
    public DomainType build();
}
```

**Since**: 1.0.0  
**Stability**: TEST_ONLY  

---

#### `PortBuilder` (class)
**Purpose**: Fluent builder for Port in tests.

**API**:
```java
public final class PortBuilder {
    public static PortBuilder port(String fqn);
    public PortBuilder direction(PortDirection dir);
    public PortBuilder kind(PortKind kind);
    public PortBuilder confidence(ConfidenceLevel level);
    public PortBuilder withMethod(PortMethod method);
    public Port build();
}
```

**Since**: 1.0.0  
**Stability**: TEST_ONLY  

---

## Dependency Graph

```
hexaglue-spi (ZERO external dependencies)
    ↑
    ├── hexaglue-core (depends on Spoon)
    ├── hexaglue-testing
    └── hexaglue-plugins/*
```

**Critical**: Plugins MUST only depend on hexaglue-spi, never hexaglue-core.

---

## API Evolution Strategy

### Adding New API (Minor Version)

1. **New types**: Add new records/enums without breaking existing code
2. **New enum values**: Safe if consumers use exhaustive switch
3. **New interface methods**: MUST have default implementations
4. **New record fields**: Use overloaded constructors or builder pattern

Example:
```java
// Before (v2.0)
public record DomainType(String fqn, DomainKind kind, ...)

// After (v2.1) - Add new field with backward compat
public record DomainType(
    String fqn,
    DomainKind kind,
    ...,
    Optional<NewField> newField  // New field as Optional
) {
    // Provide constructor without new field
    public DomainType(String fqn, DomainKind kind, ...) {
        this(fqn, kind, ..., Optional.empty());
    }
}
```

### Deprecating API (Minor Version)

1. **Mark as @Deprecated** with @since and removal version
2. **Provide migration path** in Javadoc
3. **Maintain functionality** for one major version

Example:
```java
/**
 * @deprecated Since 2.1, use {@link #newMethod()} instead.
 *             Will be removed in 3.0.
 */
@Deprecated(since = "2.1", forRemoval = true)
public void oldMethod() {
    // Delegate to new method
    newMethod();
}
```

### Breaking Changes (Major Version)

Requires major version bump (v3.0.0):
- Remove deprecated APIs
- Change existing method signatures
- Remove enum values
- Change record structure without compat constructor

---

## Testing Requirements

All SPI types must have:
1. **Unit tests** for record constructors and methods
2. **Builder tests** for test utilities
3. **Serialization tests** if used in plugin communication

Current coverage: ~85% (v2.0.0)

---

## Notes for v3 Redesign

### Candidates for Enhancement

1. **Classification metadata**: Expose classification reasoning (criteria matched, conflicts)
2. **Graph queries**: Add graph traversal queries to IR
3. **Validation API**: Plugin validation hooks
4. **Incremental IR**: Support for incremental analysis

### No Backward Compatibility Constraints

All v3 changes do not need to maintain compatibility with v2 plugins.

---

## References

- `hexaglue-spi/src/main/java/io/hexaglue/spi/**`
