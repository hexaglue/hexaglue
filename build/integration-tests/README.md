# HexaGlue Integration Tests

Integration tests that verify example applications start correctly with HexaGlue.

## Tested Applications

| Example | Description |
|---------|-------------|
| `coffeeshop` | Coffee shop app with orders and line items |
| `ecommerce` | E-commerce app with multiple aggregates |
| `minimal` | Minimal app for the Quick Start guide |

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
cd examples/coffeeshop
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
