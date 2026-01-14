# HexaGlue Documentation

Welcome to the HexaGlue documentation. These tutorials will guide you through using HexaGlue to analyze your domain, validate your architecture, and generate infrastructure code.

## Learning Paths

### For Developers

Get started generating infrastructure code from your domain model:

| Step | Tutorial | Duration | Description |
|------|----------|----------|-------------|
| 1 | [Quick Start](QUICK_START.md) | 10 min | Your first HexaGlue project |
| 2 | [JPA Generation](JPA_GENERATION.md) | 30 min | Generate entities, repositories, mappers |
| 3 | [Living Documentation](LIVING_DOCUMENTATION.md) | 15 min | Auto-generate architecture docs |

### For Architects

Understand classification and validate architectural compliance:

| Step | Tutorial | Duration | Description |
|------|----------|----------|-------------|
| 1 | [Classification](CLASSIFICATION.md) | 20 min | How HexaGlue classifies domain types |
| 2 | [Validation](VALIDATION.md) | 15 min | Validate classification before generation |
| 3 | [Architecture Audit](ARCHITECTURE_AUDIT.md) | 30 min | DDD and Hexagonal constraints |
| 4 | [Configuration](CONFIGURATION.md) | 20 min | Maven plugin and hexaglue.yaml options |

## All Tutorials

| Tutorial | Description | Example Project |
|----------|-------------|-----------------|
| [Quick Start](QUICK_START.md) | Get started in 10 minutes | [sample-basic](../examples/sample-basic/) |
| [Classification](CLASSIFICATION.md) | Understand domain type classification | [tutorial-validation](../examples/tutorial-validation/) |
| [JPA Generation](JPA_GENERATION.md) | Generate JPA entities and repositories | [sample-value-objects](../examples/sample-value-objects/) |
| [Living Documentation](LIVING_DOCUMENTATION.md) | Generate architecture documentation | [tutorial-living-doc](../examples/tutorial-living-doc/) |
| [Architecture Audit](ARCHITECTURE_AUDIT.md) | Validate DDD and Hexagonal rules | [tutorial-audit](../examples/tutorial-audit/) |
| [Configuration](CONFIGURATION.md) | Configure Maven plugin and hexaglue.yaml | [tutorial-validation](../examples/tutorial-validation/) |
| [Validation](VALIDATION.md) | Validate classification results | [tutorial-validation](../examples/tutorial-validation/) |

## Example Projects

### Sample Applications (sample-*)

Complete domain examples demonstrating HexaGlue capabilities:

| Example | Description | Key Features |
|---------|-------------|--------------|
| [sample-basic](../examples/sample-basic/) | Simplest possible domain | Single aggregate, basic ports |
| [sample-value-objects](../examples/sample-value-objects/) | Coffee shop domain | Value objects, enums, embeddables |
| [sample-multi-aggregate](../examples/sample-multi-aggregate/) | E-commerce domain | Multiple aggregates, complex relations |
| [sample-audit-violations](../examples/sample-audit-violations/) | Intentional violations | DDD/Hexagonal violations for audit demo |
| [sample-starwars](../examples/sample-starwars/) | Star Wars fleet manager | External API integration |
| [sample-pokedex](../examples/sample-pokedex/) | Pokemon collection | Full stack with PostgreSQL, TestContainers |

### Tutorial Applications (tutorial-*)

Focused examples for specific tutorials:

| Example | Tutorial | Focus |
|---------|----------|-------|
| [tutorial-validation](../examples/tutorial-validation/) | [Classification](CLASSIFICATION.md), [Validation](VALIDATION.md) | hexaglue.yaml, UNCLASSIFIED handling |
| [tutorial-living-doc](../examples/tutorial-living-doc/) | [Living Documentation](LIVING_DOCUMENTATION.md) | Documentation generation |
| [tutorial-audit](../examples/tutorial-audit/) | [Architecture Audit](ARCHITECTURE_AUDIT.md) | Clean architecture, 100% audit pass |

---

<div align="center">

**HexaGlue - Compile your architecture, not just your code**

Made with ❤️ by Scalastic<br>
Copyright 2026 Scalastic - Released under MPL-2.0

</div>