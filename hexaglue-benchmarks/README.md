# HexaGlue Benchmarks

JMH benchmarks for measuring HexaGlue performance across different components of the analysis pipeline.

**Version**: 3.0.0-SNAPSHOT
**Status**: Fully Implemented
**Last Measured**: 2026-01-09

---

## Overview

This module contains performance benchmarks that measure the execution time of critical HexaGlue components:

- **ParsingBenchmark**: Measures SpoonFrontend parsing performance (Java source → JavaSemanticModel)
- **GraphBuildingBenchmark**: Measures ApplicationGraph construction performance (JavaSemanticModel → ApplicationGraph)
- **ClassificationBenchmark**: Measures SinglePassClassifier performance (ApplicationGraph → Classifications)
- **EndToEndBenchmark**: Measures complete pipeline performance (Java source → IR export)

---

## Test Corpus

Benchmarks run against realistic DDD hexagonal architecture codebases:

| Corpus | Types | Nodes | Edges | Description |
|--------|-------|-------|-------|-------------|
| **Small** | 50 | 372 | 593 | E-commerce domain (Order, Customer) |
| **Medium** | 196 | 1,189 | 1,754 | Order Management System (14 bounded contexts) |
| **Large** | 540 | ~3,000 | ~5,000 | Enterprise System (10 bounded contexts) |

Each corpus includes:
- Domain model (entities, value objects, aggregates)
- Ports (driving and driven interfaces)
- Application services (use cases)
- Specifications and domain services
- `ground-truth.json` - Expected DDD classifications for correctness validation

### Ground Truth Files

Each corpus contains a `ground-truth.json` file with expected classification results:

```
test-corpus/
├── small/ground-truth.json
├── medium/ground-truth.json
└── large/ground-truth.json
```

**Purpose**: These files define the expected DDD classification for each type in the corpus (aggregate roots, entities, value objects, driving/driven ports). They are intended for:

1. **Determinism validation**: Verify that repeated runs produce identical classifications
2. **Correctness testing**: Ensure classification algorithm produces expected results
3. **Regression detection**: Catch unintended changes in classification behavior

**Status**: Currently not used by benchmarks (which measure performance only). Reserved for future classification validation tests.

---

## Latest Benchmark Results (v3)

**Platform**: macOS (Apple Silicon)
**JDK**: OpenJDK 17.0.17
**JVM Options**: -Xms2G -Xmx2G

### Parsing Performance

| Benchmark | Score | Error | Unit |
|-----------|-------|-------|------|
| parseSmall | 26.17 | ± 12.5 | ms/op |
| parseMedium | 47.73 | ± 8.2 | ms/op |
| parseLarge | 120.37 | ± 15.3 | ms/op |

### Graph Building Performance

| Benchmark | Score | Error | Unit |
|-----------|-------|-------|------|
| buildGraphSmall | 1.08 | ± 0.3 | ms/op |
| buildGraphMedium | 3.60 | ± 0.5 | ms/op |
| buildGraphLarge | 16.23 | ± 1.2 | ms/op |

### Classification Performance

| Benchmark | Score | Error | Unit |
|-----------|-------|-------|------|
| classifySmall | 0.20 | ± 0.05 | ms/op |
| classifyMedium | 0.67 | ± 0.13 | ms/op |
| classifyLarge | 1.97 | ± 1.25 | ms/op |

### End-to-End Performance

| Benchmark | Score | Error | Unit |
|-----------|-------|-------|------|
| analyzeSmall | 29.98 | ± 5.2 | ms/op |
| analyzeMedium | 56.87 | ± 8.1 | ms/op |
| analyzeLarge | 153.64 | ± 12.4 | ms/op |

### Time Budget Breakdown

| Corpus | Parsing | Graph | Classification | Total |
|--------|---------|-------|----------------|-------|
| Small (50 types) | 26.2 ms (87%) | 1.1 ms (4%) | 0.2 ms (1%) | 30.0 ms |
| Medium (196 types) | 47.7 ms (84%) | 3.6 ms (6%) | 0.7 ms (1%) | 56.9 ms |
| Large (540 types) | 120.4 ms (78%) | 16.2 ms (11%) | 2.0 ms (1%) | 153.6 ms |

**Key Insight**: Spoon parsing accounts for 78-87% of total analysis time.

---

## Building

Compile the benchmarks module:

```bash
cd /path/to/hexaglue
mvn clean package -DskipTests -pl hexaglue-benchmarks -am
```

This creates an executable uber-JAR at `hexaglue-benchmarks/target/benchmarks.jar`.

---

## Running Benchmarks

### List Available Benchmarks

```bash
cd hexaglue-benchmarks
java -jar target/benchmarks.jar -l
```

### Run All Benchmarks

```bash
java -jar target/benchmarks.jar
```

### Run Specific Benchmark

```bash
# Run all parsing benchmarks
java -jar target/benchmarks.jar ParsingBenchmark

# Run all classification benchmarks
java -jar target/benchmarks.jar ClassificationBenchmark

# Run all graph building benchmarks
java -jar target/benchmarks.jar GraphBuildingBenchmark

# Run all end-to-end benchmarks
java -jar target/benchmarks.jar EndToEndBenchmark
```

### Run Single Method

```bash
java -jar target/benchmarks.jar "ParsingBenchmark.parseSmall"
java -jar target/benchmarks.jar "EndToEndBenchmark.analyzeLarge"
```

### Quick Test Run

For rapid testing with reduced iterations:

```bash
# 1 fork, 1 warmup iteration, 3 measurement iterations
java -jar target/benchmarks.jar -f 1 -wi 1 -i 3
```

### Full Production Run

For reliable results with default settings:

```bash
# 2 forks, 3 warmup iterations, 5 measurement iterations
java -jar target/benchmarks.jar
```

---

## Benchmark Details

### ParsingBenchmark

**Purpose**: Measure Spoon parsing and semantic model building performance.

**What it measures**:
- Time to parse Java source files and build JavaSemanticModel
- Isolates parsing overhead from graph building and classification

**Methods**:
- `parseSmall`: 50 types → ~26 ms/op
- `parseMedium`: 196 types → ~48 ms/op
- `parseLarge`: 540 types → ~120 ms/op

### GraphBuildingBenchmark

**Purpose**: Measure ApplicationGraph construction performance.

**What it measures**:
- Time to build graph from parsed semantic model
- Includes derived edge computation
- Pre-parses models in setup to isolate graph building

**Methods**:
- `buildGraphSmall`: 50 types → 372 nodes, 593 edges → ~1.1 ms/op
- `buildGraphMedium`: 196 types → 1,189 nodes, 1,754 edges → ~3.6 ms/op
- `buildGraphLarge`: 540 types → ~3,000 nodes → ~16 ms/op

### ClassificationBenchmark

**Purpose**: Measure classification engine performance.

**What it measures**:
- Time to classify all types in a pre-built ApplicationGraph
- Uses SinglePassClassifier with default profile
- Pre-builds graphs in setup to isolate classification

**Methods**:
- `classifySmall`: 50 types → 35 domain types, 7 ports → ~0.2 ms/op
- `classifyMedium`: 196 types → ~0.7 ms/op
- `classifyLarge`: 540 types → ~2.0 ms/op

### EndToEndBenchmark

**Purpose**: Measure complete pipeline performance.

**What it measures**:
- Full pipeline from Java source to IR export
- Parsing → Graph Building → Classification → IR Export
- Simulates real-world HexaGlueEngine usage

**Methods**:
- `analyzeSmall`: 50 types → ~30 ms/op
- `analyzeMedium`: 196 types → ~57 ms/op
- `analyzeLarge`: 540 types → ~154 ms/op

---

## JMH Configuration

### Fork Settings

- **Forks**: 2 (run each benchmark in 2 separate JVMs)
- **Heap**: 2GB (-Xms2G -Xmx2G)

### Warmup

- **Iterations**: 3
- **Time**: 1-2 seconds per iteration

### Measurement

- **Iterations**: 5
- **Time**: 1-2 seconds per iteration

### Output

- **Mode**: AverageTime (mean time per operation)
- **Unit**: Milliseconds

---

## Performance Comparison: v2 vs v3

**Comparison Date**: 2026-01-09
**Commits**: v2 (0a54f73) vs v3 (c533954)

| Operation | v2 Baseline | v3 Result | Change | Status |
|-----------|-------------|-----------|--------|--------|
| parseSmall | 31.46 ms | 26.17 ms | -16.8% | IMPROVED |
| parseMedium | 56.81 ms | 47.73 ms | -16.0% | IMPROVED |
| parseLarge | N/A | 120.37 ms | NEW | BASELINE |
| buildGraphSmall | 1.19 ms | 1.08 ms | -9.5% | IMPROVED |
| buildGraphMedium | 3.97 ms | 3.60 ms | -9.2% | IMPROVED |
| buildGraphLarge | N/A | 16.23 ms | NEW | BASELINE |
| classifySmall | 0.20 ms | 0.20 ms | -3.2% | OK |
| classifyMedium | 0.72 ms | 0.67 ms | -7.1% | OK |
| classifyLarge | N/A | 1.97 ms | NEW | BASELINE |

**Conclusion**: v3 deterministic classification redesign shows no regression; slight improvements observed.

Full report: `docs/internal/performance/benchmark-report-v2-vs-v3.md`

---

## Regression Thresholds

| Corpus | Threshold | Rationale |
|--------|-----------|-----------|
| Small | +20% | Fast operations, more sensitive to variance |
| Medium | +25% | Moderate operations |
| Large | +30% | Larger operations, more tolerant |

---

## Advanced Usage

### Profile with Async Profiler

```bash
java -jar target/benchmarks.jar ClassificationBenchmark \
  -prof async:output=flamegraph
```

### Generate JSON Report

```bash
java -jar target/benchmarks.jar -rf json -rff results.json
```

### Custom Iterations

```bash
java -jar target/benchmarks.jar \
  -wi 5 \     # warmup iterations
  -i 10 \     # measurement iterations
  -f 3        # forks
```

### Filter by Pattern

```bash
java -jar target/benchmarks.jar ".*Large.*"
```

---

## Interpreting Results

### Sample Output

```
Benchmark                               Mode  Cnt    Score   Error  Units
ClassificationBenchmark.classifySmall   avgt    3    0.196 ± 0.058  ms/op
ClassificationBenchmark.classifyMedium  avgt    3    0.670 ± 0.126  ms/op
ClassificationBenchmark.classifyLarge   avgt    3    1.968 ± 1.248  ms/op
ParsingBenchmark.parseSmall             avgt    3   26.171 ± 12.5   ms/op
ParsingBenchmark.parseMedium            avgt    3   47.728 ± 8.2    ms/op
ParsingBenchmark.parseLarge             avgt    3  120.367 ± 15.3   ms/op
```

**Score**: Mean time per operation
**Error**: 99.9% confidence interval
**Units**: Milliseconds per operation

### What to Look For

1. **Regression**: Score increased by >threshold vs baseline
2. **High variance**: Large error margin indicates instability
3. **Outliers**: Check individual iteration scores for anomalies

---

## Troubleshooting

### Out of Memory

Increase heap size:
```bash
java -Xms4G -Xmx4G -jar target/benchmarks.jar
```

### Benchmarks Too Slow

Reduce iterations:
```bash
java -jar target/benchmarks.jar -wi 1 -i 3
```

### Unstable Results

- Close other applications
- Disable CPU frequency scaling
- Use `-f 3` for more forks
- Increase warmup iterations

---

## References

- JMH Documentation: https://github.com/openjdk/jmh
- v2 Baseline: `docs/internal/performance/baseline-v2.md`
- v2 vs v3 Report: `docs/internal/performance/benchmark-report-v2-vs-v3.md`
- Test corpus: `test-corpus/` (includes `ground-truth.json` for each corpus)

---

## Implementation Status

- [x] ParsingBenchmark with SpoonFrontend
- [x] GraphBuildingBenchmark with pre-parsed models
- [x] ClassificationBenchmark with pre-built graphs
- [x] EndToEndBenchmark with complete pipeline
- [x] Test corpus (small, medium, large)
- [x] Executable benchmark JAR
- [x] Large corpus (~540 types, 10 bounded contexts)
- [x] Baseline measurements established
- [x] v2 vs v3 comparison report
- [ ] CI integration
- [ ] Automated regression detection
