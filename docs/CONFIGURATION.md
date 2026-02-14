# Configuration

Configure HexaGlue through Maven plugin parameters and `hexaglue.yaml`. This tutorial covers all configuration options.

## Configuration Sources

HexaGlue reads configuration from two sources:

1. **Maven plugin parameters** - In your `pom.xml`
2. **hexaglue.yaml** - In your project root

Maven parameters override hexaglue.yaml values.

## Maven Plugin Configuration

### Minimal Configuration

```xml
<plugin>
    <groupId>io.hexaglue</groupId>
    <artifactId>hexaglue-maven-plugin</artifactId>
    <version>${hexaglue.version}</version>
    <extensions>true</extensions>
    <configuration>
        <basePackage>com.example</basePackage>
    </configuration>
</plugin>
```

The `<extensions>true</extensions>` enables automatic binding to `mvn compile`.

### All Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| `basePackage` | (required) | Root package to analyze |
| `failOnUnclassified` | `false` | Fail build if unclassified types exist |
| `skip` | `false` | Skip HexaGlue execution |
| `outputDirectory` | `target/hexaglue/generated-sources` | Generated sources location |

### Production Configuration

```xml
<plugin>
    <groupId>io.hexaglue</groupId>
    <artifactId>hexaglue-maven-plugin</artifactId>
    <version>${hexaglue.version}</version>
    <extensions>true</extensions>
    <configuration>
        <basePackage>com.example</basePackage>
        <failOnUnclassified>true</failOnUnclassified>
    </configuration>
    <dependencies>
        <dependency>
            <groupId>io.hexaglue.plugins</groupId>
            <artifactId>hexaglue-plugin-jpa</artifactId>
            <version>${hexaglue.version}</version>
        </dependency>
        <dependency>
            <groupId>io.hexaglue.plugins</groupId>
            <artifactId>hexaglue-plugin-living-doc</artifactId>
            <version>${hexaglue.version}</version>
        </dependency>
        <dependency>
            <groupId>io.hexaglue.plugins</groupId>
            <artifactId>hexaglue-plugin-audit</artifactId>
            <version>${hexaglue.version}</version>
        </dependency>
    </dependencies>
</plugin>
```

## hexaglue.yaml Configuration

Create `hexaglue.yaml` in your project root.

### Classification Configuration

```yaml
classification:
  # Exclude types from analysis (glob patterns)
  exclude:
    - "*.util.*"           # All util packages
    - "**.*Exception"      # All exceptions
    - "**.*Config"         # All config classes
    - "**.*Test"           # All test classes

  # Explicit type classifications
  explicit:
    com.example.domain.OrderId: VALUE_OBJECT
    com.example.domain.Status: VALUE_OBJECT
    com.example.shared.Money: VALUE_OBJECT

  # Validation settings
  validation:
    failOnUnclassified: false  # true for CI/CD
    allowInferred: true        # Accept semantic classifications
```

### Plugin Configuration

```yaml
plugins:
  # JPA Plugin
  io.hexaglue.plugin.jpa:
    entitySuffix: "Entity"
    repositorySuffix: "JpaRepository"
    adapterSuffix: "Adapter"
    mapperSuffix: "Mapper"
    tablePrefix: ""
    enableAuditing: false
    enableOptimisticLocking: false

  # Living Doc Plugin
  io.hexaglue.plugin.livingdoc:
    outputDir: "living-doc"
    generateDiagrams: true

  # Audit Plugin
  io.hexaglue.plugin.audit.ddd:
    failOnBlocker: true
    failOnCritical: false
```

## Glob Pattern Syntax

For `exclude` patterns:

| Pattern | Matches |
|---------|---------|
| `*.util.*` | `com.util.Helper`, `org.util.Utils` |
| `**.*Exception` | `com.example.MyException`, `org.foo.bar.SomeException` |
| `com.example.*.model.*` | `com.example.order.model.Order` |
| `**.internal.**` | Any package containing `internal` |

- `*` matches within a single package segment
- `**` matches across multiple package segments

## Maven Goals

| Goal | Phase | Description |
|------|-------|-------------|
| `generate` | GENERATE_SOURCES | Classify and generate code |
| `validate` | VALIDATE | Classify and validate only |
| `audit` | VERIFY | Run architecture audit |
| `generate-and-audit` | GENERATE_SOURCES | Generate code + run audit |

### Running Specific Goals

```bash
mvn hexaglue:validate          # Validation only
mvn hexaglue:generate          # Generate code
mvn hexaglue:audit             # Audit only
mvn hexaglue:generate-and-audit # Both
mvn compile                    # Runs configured goals automatically
```

## Command Line Overrides

Override configuration via command line:

```bash
mvn compile -Dhexaglue.failOnUnclassified=true
mvn compile -Dhexaglue.skip=true
mvn compile -Dhexaglue.basePackage=com.other
```

## Environment-Specific Configuration

### Development Profile

```xml
<profile>
    <id>dev</id>
    <activation>
        <activeByDefault>true</activeByDefault>
    </activation>
    <properties>
        <hexaglue.failOnUnclassified>false</hexaglue.failOnUnclassified>
    </properties>
</profile>
```

### CI/CD Profile

```xml
<profile>
    <id>ci</id>
    <properties>
        <hexaglue.failOnUnclassified>true</hexaglue.failOnUnclassified>
    </properties>
</profile>
```

Use with:
```bash
mvn compile -Pci
```

## Plugin Discovery

Plugins are discovered via ServiceLoader. Simply add them as dependencies:

```xml
<dependencies>
    <dependency>
        <groupId>io.hexaglue.plugins</groupId>
        <artifactId>hexaglue-plugin-jpa</artifactId>
        <version>${hexaglue.version}</version>
    </dependency>
</dependencies>
```

No additional registration required.

## Complete Example

### pom.xml

```xml
<plugin>
    <groupId>io.hexaglue</groupId>
    <artifactId>hexaglue-maven-plugin</artifactId>
    <version>${hexaglue.version}</version>
    <extensions>true</extensions>
    <configuration>
        <basePackage>com.example</basePackage>
    </configuration>
    <dependencies>
        <dependency>
            <groupId>io.hexaglue.plugins</groupId>
            <artifactId>hexaglue-plugin-jpa</artifactId>
            <version>${hexaglue.version}</version>
        </dependency>
    </dependencies>
</plugin>
```

### hexaglue.yaml

```yaml
classification:
  exclude:
    - "*.util.*"
    - "**.*Exception"
  explicit:
    com.example.domain.OrderId: VALUE_OBJECT
  validation:
    failOnUnclassified: false

plugins:
  io.hexaglue.plugin.jpa:
    enableAuditing: true
```

## What's Next?

- [Quick Start](QUICK_START.md) - Get started with minimal configuration
- [Classification](CLASSIFICATION.md) - Understand classification options
- [JPA Generation](JPA_GENERATION.md) - JPA plugin configuration

**Example:** See [tutorial-validation](../examples/tutorial-validation/) for a complete configuration example.
