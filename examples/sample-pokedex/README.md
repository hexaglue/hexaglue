# My Hexagonal Pokedex

Pokemon API integration.

> Source: [github.com/theodo-fintech](https://github.com/theodo-fintech/my-hexagonal-pokedex)

## Hexagonal Architecture

```mermaid
flowchart LR
    subgraph External
        User([User])
        API([API Client])
    end

    subgraph Driving["Driving Ports"]
        FetchPokemon[FetchPokemon]
    end

    subgraph Domain
        Pokemon{{Pokemon}}
    end

    subgraph Driven["Driven Ports"]
        PokemonRepository[PokemonRepository]
        PokemonApi[PokemonApi]
    end

    subgraph Infra["Infrastructure"]
        DB[(Database)]
        PokeAPI[PokeAPI]
    end

    User --> Driving
    API --> Driving
    Driving --> Domain
    Driven --> Domain
    Infra --> Driven
```

## Domain

```
Pokemon ─── PokemonId
```

- **Pokemon**: Aggregate Root
- **PokemonId**: Identifier

## Ports

| Port | Kind | Role |
|------|------|------|
| `FetchPokemon` | API | Fetch pokemon by ID |
| `PokemonRepository` | SPI | Persistence |
| `PokemonApi` | SPI | External API (PokeAPI) |

## Run

```bash
mvn clean compile
mvn spring-boot:run
```
