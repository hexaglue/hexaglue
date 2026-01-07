# Configuration Reference

***Configuration options for HexaGlue.***

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
                <goal>generate</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        ...
    </configuration>
    <dependencies>
        <!-- Add plugins here -->
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

## Plugin Configuration (hexaglue.yaml) (📅 Planned )

> **Note**: Plugin-specific configuration via `hexaglue.yaml` is planned but not yet implemented. See each plugin's documentation for available configuration options and current configuration methods.

Planned structure:

```yaml
hexaglue:
  plugins:
    io.hexaglue.plugin.livingdoc:
      # Plugin-specific options (see plugin README)

    io.hexaglue.plugin.jpa:
      # Plugin-specific options (see plugin README)
```

File locations (when implemented):
- Project root: `./hexaglue.yaml`
- Resources: `src/main/resources/hexaglue.yaml`

---

## Complete Example

```xml
<project>
    <properties>
        <java.version>21</java.version>
        <maven.compiler.release>${java.version}</maven.compiler.release>
        <hexaglue.version>2.0.0-SNAPSHOT</hexaglue.version>
        <hexaglue.plugin.version>1.0.0-SNAPSHOT</hexaglue.plugin.version>
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
                <configuration>
                    <basePackage>com.example.myapp</basePackage>
                    <!-- Optional: use a classification profile -->
                    <classificationProfile>repository-aware</classificationProfile>
                </configuration>
                <dependencies>
                    <dependency>
                        <groupId>io.hexaglue.plugins</groupId>
                        <artifactId>hexaglue-plugin-living-doc</artifactId>
                        <version>${hexaglue.plugin.version}</version>
                    </dependency>
                    <dependency>
                        <groupId>io.hexaglue.plugins</groupId>
                        <artifactId>hexaglue-plugin-jpa</artifactId>
                        <version>${hexaglue.plugin.version}</version>
                    </dependency>
                </dependencies>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## Plugin Documentation

For plugin-specific options, refer to each plugin's README:

- Living Documentation Plugin - `hexaglue-plugins/hexaglue-plugin-living-doc/README.md`
- JPA Repository Plugin - `hexaglue-plugins/hexaglue-plugin-jpa/README.md`

---

<div align="center">

**HexaGlue - Focus on business code, not infrastructure glue.**

Made with ❤️ by Scalastic<br>
Copyright 2026 Scalastic - Released under MPL-2.0

</div>
