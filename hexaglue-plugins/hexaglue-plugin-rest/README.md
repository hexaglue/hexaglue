# HexaGlue REST Plugin

Generates Spring REST controllers, DTOs, exception handler and configuration from your driving ports.

## Features

- **REST Controllers** - `@RestController` classes with CRUD endpoints derived from driving port methods
- **Request/Response DTOs** - Immutable Java records for request and response payloads
- **OpenAPI Annotations** - Optional `@Tag`, `@Operation`, `@ApiResponse` annotations for Swagger/OpenAPI docs
- **Exception Handler** - Optional `@RestControllerAdvice` for centralized error handling
- **Configuration Class** - Optional `@Configuration` exposing application services via their driving ports
- **Value Object Flattening** - Multi-field value objects are automatically flattened in DTOs

## Installation

Add the plugin as a dependency to the HexaGlue Maven plugin:

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
            <artifactId>hexaglue-plugin-rest</artifactId>
            <version>${hexaglue.version}</version>
        </dependency>
    </dependencies>
</plugin>
```

## Generated Files

For each driving port, the plugin generates:

```
target/generated-sources/hexaglue/
└── com/example/api/
    ├── OrderController.java          # REST controller
    ├── CreateOrderRequest.java       # Request DTO
    ├── OrderResponse.java            # Response DTO
    ├── GlobalExceptionHandler.java   # Exception handler (optional)
    └── RestConfiguration.java        # Spring @Configuration (optional)
```

## Configuration Options

<!-- GENERATED:CONFIG:START -->
| Option | Default | Description |
|--------|---------|-------------|
| `controllerSuffix` | `Controller` | suffix for controller classes |
| `requestDtoSuffix` | `Request` | suffix for request DTO records |
| `responseDtoSuffix` | `Response` | suffix for response DTO records |
| `basePath` | `/api` | URL prefix for all endpoints |
| `exceptionHandlerClassName` | `GlobalExceptionHandler` | class name of the exception handler |
| `apiPackage` | `null` | base package for generated code (null = auto-detect) |
| `targetModule` | `null` | target module for multi-module routing (null = auto) |
| `generateOpenApiAnnotations` | `true` | whether to generate OpenAPI annotations |
| `flattenValueObjects` | `true` | whether to flatten multi-field VOs in DTOs |
| `generateExceptionHandler` | `true` | whether to generate the global exception handler |
| `generateConfiguration` | `true` | whether to generate the { |
| `exceptionMappings` | `{}` | custom exception-to-HTTP-status mappings |
<!-- GENERATED:CONFIG:END -->

### Maven Parameters

These parameters are set in the `<configuration>` block of the Maven plugin:

<!-- GENERATED:MAVEN:START -->
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `basePackage` | string | (required) | The base package to analyze. Types outside this package are ignored |
| `failOnUnclassified` | boolean | `false` | Whether to fail the build if unclassified types remain. When enabled, the build will fail if any domain types cannot be classified with sufficient confidence |
| `outputDirectory` | string | `${project.build.directory}/generated-sources/hexaglue` | Output directory for generated sources |
| `skip` | boolean | `false` | Skip HexaGlue execution |
| `skipValidation` | boolean | `false` | Skip validation step before generation. When true, generation will proceed even if there are unclassified types |
| `tolerantResolution` | boolean | `false` | Enable tolerant type resolution for projects using annotation processors. When enabled, HexaGlue accepts unresolved types during analysis instead of failing |
<!-- GENERATED:MAVEN:END -->

### YAML Configuration

```yaml
plugins:
  io.hexaglue.plugin.rest:
    controllerSuffix: "Controller"
    requestDtoSuffix: "Request"
    responseDtoSuffix: "Response"
    basePath: "/api"
    exceptionHandlerClassName: "GlobalExceptionHandler"
    generateOpenApiAnnotations: true
    flattenValueObjects: true
    generateExceptionHandler: true
    generateConfiguration: true
```

## Plugin ID

`io.hexaglue.plugin.rest`

---

**HexaGlue - Focus on business code, not infrastructure glue.**
