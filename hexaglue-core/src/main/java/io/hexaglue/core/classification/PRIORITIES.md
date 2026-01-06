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
| `implements-jmolecules-interface` | varies | Type implementing jMolecules interfaces |

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

### Priority 72 - Semantic Actors (HIGH confidence)

| Criteria | Target Kind | Dependency | Description |
|----------|-------------|------------|-------------|
| `flexible-saga` | SAGA | CoreAppClassIndex | Outbound-only class with 2+ driven dependencies and state fields |

**Rationale**: Saga detection requires CoreAppClass context; higher than OUTBOUND_ONLY to take precedence.

### Priority 70 - Medium Heuristics (MEDIUM confidence)

| Criteria | Target Kind | Dependency | Description |
|----------|-------------|------------|-------------|
| `embedded-value-object` | VALUE_OBJECT | - | Type embedded in another type without identity |
| `flexible-outbound-only` | OUTBOUND_ONLY | CoreAppClassIndex | CoreAppClass depending on driven ports without driving implementation |

### Priority 68 - Semantic Actors (MEDIUM confidence)

| Criteria | Target Kind | Dependency | Description |
|----------|-------------|------------|-------------|
| `flexible-inbound-only` | INBOUND_ONLY | CoreAppClassIndex | CoreAppClass implementing driving ports without driven dependencies |

### Priority 65 - Relationship Heuristics (MEDIUM confidence)

| Criteria | Target Kind | Description |
|----------|-------------|-------------|
| `has-port-dependencies` | APPLICATION_SERVICE | Type with dependencies on port interfaces |

### Priority 60 - Structural Heuristics (MEDIUM confidence)

| Criteria | Target Kind | Description |
|----------|-------------|-------------|
| `has-identity` | ENTITY | Type with `id` field or field ending with `Id` |
| `collection-element-entity` | ENTITY | Type used as collection element in an aggregate |
| `immutable-no-id` | VALUE_OBJECT | Immutable type (record or final fields) without identity |

**Rationale**: Structural patterns are reasonable indicators but may have false positives.

### Priority 55 - Naming Heuristics (MEDIUM confidence)

| Criteria | Target Kind | Description |
|----------|-------------|-------------|
| `naming-domain-event` | DOMAIN_EVENT | Class named `*Event` or `*EventV*` |
| `stateless-no-dependencies` | DOMAIN_SERVICE | Stateless class with no port dependencies |

### Priority 50 - Lower Heuristics (LOW confidence)

| Criteria | Target Kind | Description |
|----------|-------------|-------------|
| `unreferenced-in-ports` | VALUE_OBJECT | Type not referenced in any port signature |

**Rationale**: This is a weak signal that requires other evidence.

---

## Port Classification Criteria

### Priority 100 - Explicit Annotations (EXPLICIT confidence)

| Criteria | Target Kind | Direction | Description |
|----------|-------------|-----------|-------------|
| `explicit-repository` | REPOSITORY | DRIVEN | Interface annotated with `@Repository` (jMolecules) |
| `explicit-primary-port` | USE_CASE | DRIVING | Interface annotated with `@PrimaryPort` |
| `explicit-secondary-port` | GENERIC_PORT | DRIVEN | Interface annotated with `@SecondaryPort` |

**Rationale**: Explicit annotations represent developer intent.

### Priority 85 - Semantic Analysis (HIGH confidence)

| Criteria | Target Kind | Direction | Dependency | Description |
|----------|-------------|-----------|------------|-------------|
| `semantic-driving` | USE_CASE | DRIVING | InterfaceFactsIndex | Interface implemented by CoreAppClass |
| `semantic-driven` | varies | DRIVEN | InterfaceFactsIndex | Interface used by CoreAppClass with missing/internal impl |

**Rationale**: Semantic analysis based on CoreAppClass relationships is highly reliable.

### Priority 80 - Naming Heuristics (HIGH confidence)

| Criteria | Target Kind | Direction | Description |
|----------|-------------|-----------|-------------|
| `naming-repository` | REPOSITORY | DRIVEN | Interface named `*Repository` |
| `naming-use-case` | USE_CASE | DRIVING | Interface named `*UseCase` or `*UseCases` |
| `naming-gateway` | GATEWAY | DRIVEN | Interface named `*Gateway` |

**Rationale**: Common naming conventions in hexagonal architecture.

### Priority 75 - Pattern Heuristics (HIGH confidence)

| Criteria | Target Kind | Direction | Description |
|----------|-------------|-----------|-------------|
| `command-pattern` | USE_CASE | DRIVING | Interface with `execute(Command)` or `handle(Command)` methods |
| `query-pattern` | USE_CASE | DRIVING | Interface with `query(Query)` or `get*()` methods |
| `injected-as-dependency` | GENERIC_PORT | DRIVEN | Interface injected as constructor/field dependency |

### Priority 70 - Signature Analysis (MEDIUM confidence)

| Criteria | Target Kind | Direction | Description |
|----------|-------------|-----------|-------------|
| `signature-based-driven-port` | GENERIC_PORT | DRIVEN | Interface with methods suggesting external service calls |

### Priority 60 - Package Heuristics (MEDIUM confidence)

| Criteria | Target Kind | Direction | Description |
|----------|-------------|-----------|-------------|
| `package-in` | USE_CASE | DRIVING | Interface in `*.ports.in.*` or `*.port.in.*` package |
| `package-out` | GENERIC_PORT | DRIVEN | Interface in `*.ports.out.*` or `*.port.out.*` package |

**Rationale**: Package organization often follows hexagonal architecture conventions.

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
