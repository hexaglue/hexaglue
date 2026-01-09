# ADR-XXX: [Short Title]

## Status

[Proposed | Accepted | Deprecated | Superseded by ADR-YYY]

**Date**: YYYY-MM-DD
**Authors**: [Name(s)]
**Deciders**: [Team/Role]

---

## Context

[Describe the issue or decision that needs to be made. Include:]
- What is the architectural concern or problem?
- What are the forces at play (constraints, requirements, trade-offs)?
- What is the current state (if replacing existing design)?
- Why is this decision important?

[Keep this section factual and objective. Avoid proposing solutions here.]

---

## Decision

[State the decision that was made in clear, declarative language:]

We will [chosen approach/solution].

[Provide enough detail that someone can understand the decision without reading the entire document. Be specific about WHAT is being decided.]

---

## Rationale

[Explain WHY this decision was made:]

### Key Principles

[List the core principles guiding the decision, e.g.:]
- Determinism over flexibility
- Simplicity over cleverness
- Backward compatibility required

### Alternatives Considered

#### Option 1: [Name]
**Description**: [Brief description]
**Pros**:
- [Advantage 1]
- [Advantage 2]

**Cons**:
- [Disadvantage 1]
- [Disadvantage 2]

**Why rejected**: [Specific reason]

#### Option 2: [Name]
[Same structure as Option 1]

#### Option 3: [Chosen solution]
[Same structure - this is the winner]

**Why chosen**: [Specific reasons why this option was selected over the others]

---

## Consequences

### Positive

[What benefits does this decision bring?]
- [Benefit 1]
- [Benefit 2]
- [Benefit 3]

### Negative

[What costs or limitations does this introduce?]
- [Trade-off 1]
- [Trade-off 2]
- [Trade-off 3]

### Neutral

[Changes that are neither clearly good nor bad]
- [Change 1]
- [Change 2]

---

## Implementation

[High-level implementation approach - not detailed design]

### Changes Required

1. **Module X**: [Brief description of changes]
2. **Module Y**: [Brief description of changes]
3. **Tests**: [Testing approach]

### Migration Path

[If this changes existing behavior, describe how users/plugins migrate]

**For plugin developers**:
- [Step 1]
- [Step 2]

**For core developers**:
- [Step 1]
- [Step 2]

### Backward Compatibility

[Impact on existing APIs and code]
- **Breaking changes**: [Yes/No, details if yes]
- **Deprecations**: [List any APIs being deprecated]
- **Migration timeline**: [e.g., "Deprecated in v3.0, removed in v4.0"]

---

## Risks and Mitigation

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| [Risk 1] | High/Medium/Low | High/Medium/Low | [How we address it] |
| [Risk 2] | High/Medium/Low | High/Medium/Low | [How we address it] |

---

## Success Metrics

[How will we know if this decision was successful?]

**Quantitative**:
- [Metric 1, e.g., "Classification performance within 10% of baseline"]
- [Metric 2, e.g., "Test coverage > 90%"]

**Qualitative**:
- [Goal 1, e.g., "Deterministic classification across runs"]
- [Goal 2, e.g., "Plugin developers find API intuitive"]

---

## References

### Related Documents

- [Link to design doc]
- [Link to issue]
- [Link to RFC]

### Related ADRs

- ADR-XXX: [Related decision]
- ADR-YYY: [Supersedes/depends on]

### External Resources

- [Research paper]
- [Blog post]
- [Library documentation]

---

## Notes

[Any additional context, discussions, or future considerations]

### Open Questions

- [Question 1 that remains unresolved]
- [Question 2 that may need follow-up]

### Future Work

- [Enhancement 1 that builds on this decision]
- [Enhancement 2 to consider later]

---

## Changelog

| Date | Author | Change |
|------|--------|--------|
| YYYY-MM-DD | [Name] | Initial version |
| YYYY-MM-DD | [Name] | Updated after review |
