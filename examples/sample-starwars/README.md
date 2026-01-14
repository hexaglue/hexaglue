# Hexagonal Architecture Java Springboot

External API integration (SWAPI).

> Source: [gitlab.com/beyondxscratch](https://gitlab.com/beyondxscratch/hexagonal-architecture-java-springboot)

## Hexagonal Architecture

```mermaid
flowchart LR
    subgraph External
        User([User])
        API([API Client])
    end

    subgraph Driving["Driving Ports"]
        AssembleAFleet[AssembleAFleet]
    end

    subgraph Domain
        Fleet{{Fleet}}
    end

    subgraph Driven["Driven Ports"]
        Fleets[Fleets]
        StarShipInventory[StarShipInventory]
    end

    subgraph Infra["Infrastructure"]
        DB[(Database)]
        SWAPI[SWAPI]
    end

    User --> Driving
    API --> Driving
    Driving --> Domain
    Driven --> Domain
    Infra --> Driven
```

## Domain

```
Fleet ─┬─ FleetId
       └─ StarShip (collection)
```

- **Fleet**: Aggregate Root
- **StarShip**: Value Object (name, manufacturer)

## Ports

| Port | Kind | Role |
|------|------|------|
| `AssembleAFleet` | API | Assemble fleet from starships |
| `Fleets` | SPI | Persistence |
| `StarShipInventory` | SPI | External API (SWAPI) |

## Run

```bash
mvn clean compile
mvn spring-boot:run
```

## Generated

```
target/generated-sources/hexaglue/
├── FleetEntity.java
├── StarShipEmbeddable.java
├── FleetJpaRepository.java
├── FleetMapper.java
├── StarShipMapper.java
└── FleetAdapter.java
```
