# Large Test Corpus (~540 types)

## Status

**IMPLEMENTED** - Synthetically generated enterprise-scale corpus.

## Overview

This corpus contains 540 Java types representing a multi-bounded-context enterprise system following DDD and hexagonal architecture patterns.

## Structure

The corpus is organized into 10 bounded contexts:
- `ordering`
- `inventory`
- `catalog`
- `customer`
- `payment`
- `shipping`
- `warehouse`
- `supplier`
- `analytics`
- `marketing`

Each bounded context contains:
- 3 Aggregate Roots
- 3 Entities
- ~18 Value Objects
- 3 Driving Ports (Service interfaces)
- 5 Driven Ports (Repositories, Event Publishers)
- 6 Command objects
- 3 Use Cases
- 1 Query Service
- 2 Domain Services
- 5 Domain Exceptions
- 2 Validators
- 1 Specification class
- 3 Domain Events

## Generation

The corpus was generated using:
```bash
./scripts/generate-large-corpus.sh
```

## Ground Truth

See `ground-truth.json` for expected DDD classifications.

## Compilation

```bash
cd test-corpus/large
mvn compile
```

## Usage in Benchmarks

The large corpus is used by all benchmark classes:
- `ParsingBenchmark.parseLarge()`
- `GraphBuildingBenchmark.buildGraphLarge()`
- `ClassificationBenchmark.classifyLarge()`
- `EndToEndBenchmark.analyzeLarge()`
