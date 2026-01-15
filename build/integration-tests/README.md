# HexaGlue Integration Tests

Integration tests that verify example applications compile and start correctly with HexaGlue.

## Tested Applications

### Tutorial Examples

| Example | Focus |
|---------|-------|
| `tutorial-validation` | Classification validation phase |
| `tutorial-living-doc` | Living Documentation plugin |
| `tutorial-audit` | Architecture audit (clean) |

### Sample Applications

| Example | Complexity | Description |
|---------|------------|-------------|
| `sample-basic` | Beginner | Single aggregate, basic entity generation |
| `sample-value-objects` | Intermediate | Value objects, enums, embedded collections |
| `sample-multi-aggregate` | Advanced | Multiple aggregates, relationships |
| `sample-audit-violations` | Advanced | Intentional violations for audit testing |
| `sample-starwars` | Intermediate | Hexagonal app with Star Wars theme |
| `sample-pokedex` | Intermediate | Hexagonal app with Pokemon theme |

## How It Works

Each example application contains a `@SpringBootTest` that:
1. Compiles the application with the HexaGlue plugin
2. Loads the Spring Boot context
3. Verifies the application starts without errors

## Commands

### Run all tests (full build)

```bash
# From project root
mvn clean install
```

### Run integration tests only

```bash
# Prerequisite: HexaGlue artifacts must be installed
mvn install -DskipTests

# Then run ITs
mvn -pl build/integration-tests invoker:run
```

### Run a specific example

```bash
# From the example directory
cd examples/sample-basic
mvn clean verify
```

### View detailed logs

```bash
mvn -pl build/integration-tests invoker:run -Dinvoker.streamLogs=true
```

## Structure

```
build/integration-tests/
├── pom.xml              # maven-invoker-plugin configuration
├── README.md            # This file
└── src/it/
    └── settings.xml     # Maven settings for tests
```

## Adding a New Example

1. Create the project in `examples/`
2. Add a Spring Boot test:

```java
package com.example;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MyApplicationTest {

    @Test
    void contextLoads() {
        // Verifies Spring context starts
    }
}
```

3. Add the test dependency in `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

The maven-invoker-plugin will automatically detect the new example.

## Troubleshooting

### Tests fail with "artifact not found"

HexaGlue SNAPSHOT artifacts must be installed locally:

```bash
mvn install -DskipTests
```

### An example does not compile

Verify the example compiles standalone:

```bash
cd examples/problematic-example
mvn clean compile -X
```

### View execution report

After execution, the report is available at:

```
build/integration-tests/target/invoker-reports/
```

---

<div align="center">

**HexaGlue - Focus on business code, not infrastructure glue.**

Made with ❤️ by Scalastic<br>
Copyright 2026 Scalastic - Released under MPL-2.0

</div>
