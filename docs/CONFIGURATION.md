# Configuration Reference

***Configuration options for HexaGlue.***

---

## Maven Plugin Goals

HexaGlue provides three Maven goals:

| Goal | Phase | Description |
|------|-------|-------------|
| `hexaglue:generate` | `generate-sources` | Code generation via plugins |
| `hexaglue:audit` | `verify` | Architecture audit only |
| `hexaglue:generate-and-audit` | `generate-sources` | Both combined |

---

## Maven Plugin Configuration

HexaGlue is primarily configured through the Maven plugin in your `pom.xml`:

```xml
<plugin>
    <groupId>io.hexaglue</groupId>
    <artifactId>hexaglue-maven-plugin</artifactId>
    <version>${hexaglue.version}</version>
    <executions>
        <execution>
            <goals>
                <goal>generate</goal>  <!-- or audit, or generate-and-audit -->
            </goals>
        </execution>
    </executions>
    <configuration>
        ...
    </configuration>
    <dependencies>
        <!-- Add plugins here (for generate goal) -->
        <dependency>
            <groupId>io.hexaglue.plugins</groupId>
            <artifactId>hexaglue-plugin-living-doc</artifactId>
            <version>${hexaglue.version}</version>
        </dependency>
    </dependencies>
</plugin>
```

---

## Maven Plugin Parameters

### `basePackage` (required)

The root package for analysis. Only types within this package (and sub-packages) are analyzed.

```xml
<configuration>
    <basePackage>com.example.myapp</basePackage>
</configuration>
```

Can also be set via command line:
```bash
mvn compile -Dhexaglue.basePackage=com.example
```

### `outputDirectory`

Output directory for generated source files.

```xml
<configuration>
    <outputDirectory>${project.build.directory}/generated-sources/hexaglue</outputDirectory>
</configuration>
```

**Default**: `target/generated-sources/hexaglue`

### `skip`

Skip HexaGlue execution entirely.

```xml
<configuration>
    <skip>true</skip>
</configuration>
```

Or via command line:
```bash
mvn compile -Dhexaglue.skip=true
```

**Default**: `false`

### `classificationProfile`

The classification profile controls how HexaGlue classifies ports and domain types. Profiles adjust the priority of different classification criteria.

```xml
<configuration>
    <classificationProfile>repository-aware</classificationProfile>
</configuration>
```

Or via command line:
```bash
mvn compile -Dhexaglue.classificationProfile=repository-aware
```

**Available Profiles**:

| Profile | Description | Use Case |
|---------|-------------|----------|
| *(none)* | Legacy behavior, default priorities | Standard projects |
| `default` | Documented default priorities | Reference configuration |
| `strict` | Favors explicit annotations over heuristics | Projects with consistent DDD annotations |
| `annotation-only` | Only trusts explicit annotations | Gradual migration, maximum control |
| `repository-aware` | Better detection of repository ports with plural names | Projects using `Orders`, `Customers` instead of `OrderRepository` |

**Default**: *(none)* - uses legacy behavior

---

## Audit Configuration Parameters

These parameters apply to the `audit` and `generate-and-audit` goals.

### `failOnError`

Fail the build if ERROR-level violations are found.

```xml
<configuration>
    <failOnError>true</failOnError>
</configuration>
```

**Default**: `true`

### `failOnWarning`

Fail the build if WARNING-level violations are found.

```xml
<configuration>
    <failOnWarning>false</failOnWarning>
</configuration>
```

**Default**: `false`

### Report Format Parameters

Enable or disable specific report formats:

```xml
<configuration>
    <consoleReport>true</consoleReport>   <!-- Output to logs -->
    <htmlReport>true</htmlReport>          <!-- Rich HTML report -->
    <jsonReport>false</jsonReport>         <!-- Machine-readable -->
    <markdownReport>false</markdownReport> <!-- Documentation -->
</configuration>
```

### `reportDirectory`

Output directory for audit reports.

```xml
<configuration>
    <reportDirectory>${project.build.directory}/hexaglue-reports</reportDirectory>
</configuration>
```

**Default**: `target/hexaglue-reports`

### `auditConfig`

Configure audit rules and quality thresholds:

```xml
<configuration>
    <auditConfig>
        <!-- Enable specific rules -->
        <enabledRules>
            <rule>hexaglue.layer.domain-purity</rule>
            <rule>hexaglue.dependency.no-cycles</rule>
        </enabledRules>

        <!-- Disable specific rules -->
        <disabledRules>
            <rule>hexaglue.complexity.cyclomatic</rule>
        </disabledRules>

        <!-- Quality thresholds -->
        <thresholds>
            <maxCyclomaticComplexity>10</maxCyclomaticComplexity>
            <maxMethodLength>50</maxMethodLength>
            <maxClassLength>500</maxClassLength>
            <maxMethodParameters>7</maxMethodParameters>
            <maxNestingDepth>4</maxNestingDepth>
            <minTestCoverage>80.0</minTestCoverage>
            <minDocumentationCoverage>70.0</minDocumentationCoverage>
            <maxTechnicalDebtMinutes>480</maxTechnicalDebtMinutes>
            <minMaintainabilityRating>3.0</minMaintainabilityRating>
        </thresholds>
    </auditConfig>
</configuration>
```

### Threshold Reference

| Threshold | Default | Description |
|-----------|---------|-------------|
| `maxCyclomaticComplexity` | 10 | Max cyclomatic complexity per method |
| `maxMethodLength` | 50 | Max lines per method |
| `maxClassLength` | 500 | Max lines per class |
| `maxMethodParameters` | 7 | Max parameters per method |
| `maxNestingDepth` | 4 | Max nesting depth |
| `minTestCoverage` | 80.0 | Min test coverage (%) |
| `minDocumentationCoverage` | 70.0 | Min documentation coverage (%) |
| `maxTechnicalDebtMinutes` | 480 | Max technical debt (8 hours) |
| `minMaintainabilityRating` | 3.0 | Min maintainability (0-5 scale) |

For complete audit rules reference, see the [Architecture Audit Guide](ARCHITECTURE_AUDIT.md).

---

## Classification Profile

**Example - Repository-Aware Profile**:

If your driven ports use plural naming (e.g., `Orders`, `Products`) instead of the `Repository` suffix, the default classification may incorrectly identify them as driving/command ports. Use the `repository-aware` profile:

```xml
<configuration>
    <basePackage>com.example</basePackage>
    <classificationProfile>repository-aware</classificationProfile>
</configuration>
```

This profile:
- Increases priority of signature-based driven port detection (70 → 78)
- Increases priority of `ports.out` package detection (60 → 74)
- Decreases priority of command/query pattern detection (75 → 72)

---

## Java Version

The Java version is automatically detected from your Maven compiler configuration:

```xml
<properties>
    <maven.compiler.release>21</maven.compiler.release>
</properties>
```

HexaGlue uses this value to configure the parser for your source files.

---

## Adding Plugins

Plugins are added as dependencies to the Maven plugin:

```xml
<plugin>
    <groupId>io.hexaglue</groupId>
    <artifactId>hexaglue-maven-plugin</artifactId>
    <version>${hexaglue.version}</version>
    <dependencies>
        <!-- Living Documentation Plugin -->
        <dependency>
            <groupId>io.hexaglue.plugins</groupId>
            <artifactId>hexaglue-plugin-living-doc</artifactId>
            <version>${hexaglue.plugin.version}</version>
        </dependency>

        <!-- JPA Repository Plugin -->
        <dependency>
            <groupId>io.hexaglue.plugins</groupId>
            <artifactId>hexaglue-plugin-jpa</artifactId>
            <version>${hexaglue.plugin.version}</version>
        </dependency>
    </dependencies>
</plugin>
```

---

## Plugin Configuration (hexaglue.yaml)

Plugin-specific configuration can be provided via a YAML file in your project root.

### File Locations

HexaGlue looks for configuration in this order:
1. `./hexaglue.yaml` (preferred)
2. `./hexaglue.yml`

If no file is found, an empty configuration is used (plugins use their defaults).

### Configuration Structure

```yaml
plugins:
  jpa:
    enabled: true
    entitySuffix: "Entity"
    repositoryPackage: "infrastructure.persistence"
    auditing: true
    optimisticLocking: true

  living-doc:
    enabled: true
    outputFormat: "html"
    includePrivateMethods: false
```

### Plugin Configuration Keys

Each plugin defines its own configuration keys. See the plugin's README for details:

- **JPA Plugin** (`jpa`): Entity generation, auditing, locking
- **Living Doc Plugin** (`living-doc`): Documentation format, output options

### Example

Create `hexaglue.yaml` in your project root:

```yaml
plugins:
  jpa:
    entitySuffix: "Entity"
    auditing: true
  living-doc:
    outputFormat: "markdown"
```

The Maven plugin automatically loads this configuration during the `generate` goal.

---

## Complete Examples

### Generation Only

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
    <configuration>
        <basePackage>com.example.myapp</basePackage>
        <classificationProfile>repository-aware</classificationProfile>
    </configuration>
    <dependencies>
        <dependency>
            <groupId>io.hexaglue.plugins</groupId>
            <artifactId>hexaglue-plugin-living-doc</artifactId>
            <version>${hexaglue.version}</version>
        </dependency>
        <dependency>
            <groupId>io.hexaglue.plugins</groupId>
            <artifactId>hexaglue-plugin-jpa</artifactId>
            <version>${hexaglue.version}</version>
        </dependency>
    </dependencies>
</plugin>
```

### Audit Only

```xml
<plugin>
    <groupId>io.hexaglue</groupId>
    <artifactId>hexaglue-maven-plugin</artifactId>
    <version>${hexaglue.version}</version>
    <executions>
        <execution>
            <goals>
                <goal>audit</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <basePackage>com.example.myapp</basePackage>
        <failOnError>true</failOnError>
        <htmlReport>true</htmlReport>
        <auditConfig>
            <thresholds>
                <minTestCoverage>80.0</minTestCoverage>
                <maxTechnicalDebtMinutes>240</maxTechnicalDebtMinutes>
            </thresholds>
        </auditConfig>
    </configuration>
</plugin>
```

### Generation + Audit (CI/CD)

```xml
<plugin>
    <groupId>io.hexaglue</groupId>
    <artifactId>hexaglue-maven-plugin</artifactId>
    <version>${hexaglue.version}</version>
    <executions>
        <execution>
            <goals>
                <goal>generate-and-audit</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <basePackage>com.example.myapp</basePackage>
        <classificationProfile>repository-aware</classificationProfile>
        <!-- Audit settings -->
        <failOnError>true</failOnError>
        <htmlReport>true</htmlReport>
        <jsonReport>true</jsonReport>
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

---

## Related Documentation

- [Architecture Audit Guide](ARCHITECTURE_AUDIT.md) - Complete audit rules and CI/CD integration
- [Getting Started](GETTING_STARTED.md) - Tutorial from audit to generation
- [User Guide](USER_GUIDE.md) - Concepts and classification reference

### Plugin Documentation

For plugin-specific options, refer to each plugin's README:

- Living Documentation Plugin - `hexaglue-plugins/hexaglue-plugin-living-doc/README.md`
- JPA Repository Plugin - `hexaglue-plugins/hexaglue-plugin-jpa/README.md`

---

<div align="center">

**HexaGlue - Design, Audit, and Generate Hexagonal Architecture**

Made with ❤️ by Scalastic<br>
Copyright 2026 Scalastic - Released under MPL-2.0

</div>
