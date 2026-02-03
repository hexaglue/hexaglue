# Classification Priorities Reference

This document lists all classification criteria and their priorities in the HexaGlue classification engine.

## Tie-Break Algorithm

When multiple criteria match for a type, the winner is determined by:
1. **Priority** (descending) - higher priority wins
2. **Confidence** (descending) - higher confidence wins if priorities are equal
3. **Criteria name** (ascending) - alphabetical order for determinism if both are equal

## Domain Classification Criteria

### Priority 100 - Explicit Annotations (EXPLICIT confidence)

| Criteria | Target Kind | Description |
|----------|-------------|-------------|
| `explicit-aggregate-root` | AGGREGATE_ROOT | Type annotated with `@AggregateRoot` (jMolecules) |
| `explicit-entity` | ENTITY | Type annotated with `@Entity` (jMolecules) |
| `explicit-value-object` | VALUE_OBJECT | Type annotated with `@ValueObject` (jMolecules) |
| `explicit-identifier` | IDENTIFIER | Type annotated with `@Identity` (jMolecules) |
| `explicit-domain-event` | DOMAIN_EVENT | Type annotated with `@DomainEvent` (jMolecules) |
| `explicit-externalized-event` | EXTERNALIZED_EVENT | Type annotated with `@Externalized` (jMolecules) |
| `implements-jmolecules-interface` | varies | Type implementing jMolecules DDD type interfaces |

**Rationale**: Explicit annotations represent developer intent and should always take precedence.

### Priority 80 - Strong Heuristics (HIGH confidence)

| Criteria | Target Kind | Description |
|----------|-------------|-------------|
| `repository-dominant` | AGGREGATE_ROOT | Type used in repository interface signature + has identity field |
| `record-single-id` | IDENTIFIER | Record named `*Id` with single component |

**Rationale**: These patterns strongly correlate with the target classification in real codebases.

### Priority 75 - Inherited Classification (HIGH confidence)

| Criteria | Target Kind | Description |
|----------|-------------|-------------|
| `inherited-classification` | varies | Type inheriting from explicitly annotated type |

**Rationale**: Classification inheritance follows OOP principles.

### Priority 74 - Semantic Actors (HIGH confidence)

| Criteria | Target Kind | Dependency | Description |
|----------|-------------|------------|-------------|
| `flexible-application-service` | APPLICATION_SERVICE | CoreAppClassIndex | Pivot class implementing driving ports and depending on driven ports |

**Rationale**: APPLICATION_SERVICE (pivot) is the most specific actor classification; higher priority ensures it takes precedence over SAGA, INBOUND_ONLY, and OUTBOUND_ONLY.

### Priority 72 - Structural / Semantic Heuristics (HIGH confidence)

| Criteria | Target Kind | Dependency | Description |
|----------|-------------|------------|-------------|
| `domain-enum` | VALUE_OBJECT | - | Enum types (immutable, identity-less domain concepts) |
| `flexible-saga` | SAGA | CoreAppClassIndex | Outbound-only class with 2+ driven dependencies and state fields |

**Rationale**: Enum types are natural value objects. Saga detection requires CoreAppClass context; higher than OUTBOUND_ONLY to take precedence.

### Priority 70 - Medium Heuristics (HIGH confidence)

| Criteria | Target Kind | Dependency | Description |
|----------|-------------|------------|-------------|
| `contained-entity` | ENTITY | - | Type with identity contained in aggregate-like containers |
| `embedded-value-object` | VALUE_OBJECT | - | Immutable type without identity embedded in aggregates |
| `flexible-inbound-only` | INBOUND_ONLY | CoreAppClassIndex | CoreAppClass implementing driving ports without driven dependencies |

**Rationale**: Structural patterns within aggregates and semantic actor detection provide reliable classification signals.

### Priority 68 - Naming / Semantic Heuristics (HIGH / MEDIUM confidence)

| Criteria | Target Kind | Confidence | Dependency | Description |
|----------|-------------|------------|------------|-------------|
| `domain-event-naming` | DOMAIN_EVENT | MEDIUM | - | Class named `*Event` (excluding base names like `Event`, `DomainEvent`) |
| `flexible-outbound-only` | OUTBOUND_ONLY | HIGH | CoreAppClassIndex | CoreAppClass depending on driven ports without implementing driving ports |

### Priority 65 - Record Heuristics (MEDIUM confidence)

| Criteria | Target Kind | Description |
|----------|-------------|-------------|
| `domain-record-value-object` | VALUE_OBJECT | Record without identity that is referenced by other types |

**Rationale**: Records referenced in the domain graph that lack identity fields are likely value objects.

---

## Port Classification Criteria

### Priority 100 - Explicit Annotations (EXPLICIT confidence)

| Criteria | Target Kind | Direction | Description |
|----------|-------------|-----------|-------------|
| `explicit-repository` | REPOSITORY | DRIVEN | Interface annotated with `@Repository` (jMolecules) |
| `explicit-primary-port` | USE_CASE | DRIVING | Interface annotated with `@PrimaryPort` or implementing `PrimaryPort` |
| `explicit-secondary-port` | GATEWAY | DRIVEN | Interface annotated with `@SecondaryPort` or implementing `SecondaryPort` |

**Rationale**: Explicit annotations represent developer intent.

### Priority 85 - Semantic Analysis (HIGH confidence)

| Criteria | Target Kind | Direction | Dependency | Description |
|----------|-------------|-----------|------------|-------------|
| `semantic-driving` | USE_CASE | DRIVING | InterfaceFactsIndex | Interface implemented by CoreAppClass |
| `semantic-driven` | GENERIC | DRIVEN | InterfaceFactsIndex | Interface used by CoreAppClass with missing/internal impl |

**Rationale**: Semantic analysis based on CoreAppClass relationships is highly reliable.

### Priority 75 - Pattern Heuristics (HIGH confidence)

| Criteria | Target Kind | Direction | Description |
|----------|-------------|-----------|-------------|
| `command-pattern` | COMMAND | DRIVING | Interface with `execute(Command)` or `handle(Command)` methods |
| `query-pattern` | QUERY | DRIVING | Interface with `query(Query)` or `get*()` methods |
| `injected-as-dependency` | REPOSITORY | DRIVEN | Interface injected as constructor/field dependency |

### Priority 72 - Signature Analysis (HIGH confidence)

| Criteria | Target Kind | Direction | Description |
|----------|-------------|-----------|-------------|
| `signature-based-gateway` | GATEWAY | DRIVEN | Interface manipulating multiple aggregate-like types in signatures |

### Priority 70 - Signature Analysis (HIGH confidence)

| Criteria | Target Kind | Direction | Description |
|----------|-------------|-----------|-------------|
| `signature-based-driven` | REPOSITORY | DRIVEN | Interface manipulating aggregate-like types in signatures |

---

## Confidence Levels

| Level | Description | Typical Source |
|-------|-------------|----------------|
| EXPLICIT | Developer explicitly annotated | jMolecules annotations |
| HIGH | Strong evidence from multiple sources | Repository + identity, semantic analysis |
| MEDIUM | Reasonable inference from structure | Naming, structural patterns |
| LOW | Weak signal requiring confirmation | Absence-based rules |

---

## Compatible Kinds

Some classification kinds are considered compatible (can coexist without being marked as conflict):

### Domain
- `AGGREGATE_ROOT` ↔ `ENTITY` (an aggregate root is a special entity)

### Port
- None (all port kinds are mutually exclusive)

---

## Adding New Criteria

When adding a new criteria:
1. Choose a priority based on confidence level and evidence strength
2. Document the rationale for the priority choice
3. Add contract tests to verify the tie-break behavior
4. Update this file with the new criteria

**Guidelines**:
- Explicit annotations: priority 100
- Semantic analysis with CoreAppClass: priority 70-85
- Strong naming/structural patterns: priority 75-80
- Medium heuristics: priority 60-70
- Weak signals: priority 50-55
