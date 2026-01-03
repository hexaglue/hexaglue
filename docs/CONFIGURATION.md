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
