# Contributing to HexaGlue

Thank you for your interest in contributing to **HexaGlue**! We welcome contributions from the community.

**HexaGlue** is a compile-time code generation engine for Java applications following Hexagonal Architecture (Ports & Adapters).

---

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Types of Contributions](#types-of-contributions)
- [Development Setup](#development-setup)
- [Contributing Code](#contributing-code)
- [Testing](#testing)
- [Code Style](#code-style)
- [Commit Message Convention](#commit-message-convention)
- [Pull Request Process](#pull-request-process)
- [Reporting Issues](#reporting-issues)

---

## Code of Conduct

### Our Pledge

We are committed to providing a welcoming and inclusive environment for all contributors.

### Our Standards

- **Be respectful** - Treat everyone with respect
- **Be collaborative** - Work together constructively
- **Be inclusive** - Welcome diverse perspectives
- **Be professional** - Keep discussions focused on technical merit

### Enforcement

Instances of unacceptable behavior may be reported to info@hexaglue.io.

---

## Getting Started

### Understanding the Project Structure

HexaGlue is organized as a monorepo with the following modules:

```
hexaglue/
├── hexaglue-spi/           # Service Provider Interface (STABLE API for plugins)
├── hexaglue-core/          # Core analysis engine and graph-based classification
├── hexaglue-testing/       # Testing harness for plugin developers
├── hexaglue-maven-plugin/  # Maven plugin for build integration
├── hexaglue-plugins/       # Official plugins
│   ├── hexaglue-plugin-jpa/        # JPA infrastructure generator
│   └── hexaglue-plugin-living-doc/ # Living documentation generator
├── examples/               # Working example applications
│   ├── coffeeshop/         # Coffee shop domain example
│   ├── minimal/            # Minimal quick-start example
│   └── ecommerce/          # Rich e-commerce domain example
└── docs/                   # Documentation
```

### Key Architectural Concepts

- **SPI (hexaglue-spi)**: The stable API that plugins depend on. Changes here require backward compatibility.
- **Core (hexaglue-core)**: The analysis engine using graph-based classification. Internal implementation details.
- **Plugins**: Infrastructure code generators that consume the SPI.

---

## Types of Contributions

We welcome many types of contributions:

### Core Engine (hexaglue-core)

- **Bug fixes** - Fix issues in the analysis pipeline or classification logic
- **Performance improvements** - Optimize analysis speed or memory usage
- **New features** - Add capabilities to the core engine (after discussion)
- **Diagnostics** - Improve error messages and diagnostic codes

### SPI (hexaglue-spi)

- **API improvements** - Propose additions to the stable SPI (with backward compatibility)
- **Documentation** - Improve SPI reference documentation
- **Type system** - Enhance type resolution and representation

### Plugins (hexaglue-plugins)

- **Bug fixes** - Fix issues in existing plugins
- **New plugins** - Create generators for new frameworks (after discussion)
- **Enhancements** - Add features to existing plugins

### Testing Harness (hexaglue-testing)

- **Test utilities** - Add helpers for plugin testing
- **Documentation** - Improve testing guide and examples

### Examples

- **New examples** - Demonstrate specific use cases or patterns
- **Improvements** - Enhance existing examples with better documentation

### Documentation

- **Architecture guides** - Explain compilation pipeline, IR design
- **Contributor guides** - Improve development setup, troubleshooting
- **API documentation** - Enhance Javadocs

---

## Development Setup

### Prerequisites

- **Java 17** or later
- **Maven 3.8+**
- Your favorite IDE (IntelliJ IDEA, Eclipse, VS Code)

### Build Individual Modules

```bash
# Build only core modules
mvn clean install -pl hexaglue-spi,hexaglue-core,hexaglue-testing

# Build only plugins
mvn clean install -pl hexaglue-plugins -am

# Build and test a specific plugin
cd hexaglue-plugins/hexaglue-plugin-jpa
mvn clean test
```

### Verify Setup

```bash
# Run example compilation to verify code generation
cd examples/minimal
mvn clean compile
```

---

## Contributing Code

### 1. Find or Create an Issue

- Check [GitHub Issues](https://github.com/hexaglue/hexaglue/issues)
- Comment on the issue to claim it
- For new features, open a discussion first

### 2. Create a Branch

```bash
# Update your main branch
git checkout main
git pull upstream main

# Create a feature branch
git checkout -b feature/my-feature-name
# or
git checkout -b fix/issue-123-description
```

### 3. Make Your Changes

- Write code following the existing style
- Add tests for new functionality
- Update documentation as needed
- Keep changes focused and atomic

### 4. Document Your Changes

- Add Javadoc to all public APIs
- Update relevant markdown documentation
- Add examples if introducing new features
- Follow existing documentation patterns

---

## Testing

### Running Tests

```bash
# Run all tests
mvn clean test

# Run tests for a specific module
cd hexaglue-core
mvn test

# Run a specific test class
mvn test -Dtest=DomainClassifierTest

# Run tests with coverage
mvn clean test jacoco:report
```

### Writing Tests

**Unit tests** - Test individual components:
```java
@Test
@DisplayName("should classify type as AGGREGATE_ROOT when it has identity and repository port")
void shouldClassifyAsAggregateRoot() {
    // Arrange
    var graph = buildGraphWithType("Order");

    // Act
    var result = classifier.classify(graph);

    // Assert
    assertThat(result.kind()).isEqualTo(DomainKind.AGGREGATE_ROOT);
}
```

**Plugin tests** - Test code generation:
```java
@Test
@DisplayName("should generate JPA entity with @Id annotation")
void shouldGenerateEntityWithIdAnnotation() {
    DomainType type = TestFixtures.simpleAggregateRoot("Order", DOMAIN_PKG);

    String code = generator.generateEntity(type);

    assertThat(code).contains("@Id");
    assertThat(code).contains("private UUID id;");
}
```

### Test Requirements

- All new code must have tests
- Aim for >80% code coverage
- Test both success and error cases
- Use meaningful test names with `@DisplayName`

---

## Code Style

HexaGlue follows strict code style guidelines:

### Java Style

- **Java 17** language features (records, sealed classes, pattern matching)
- **Palantir Java Format** (enforced via Spotless)
- **Javadoc** on all public APIs
- **Meaningful names** - No abbreviations
- **Clear comments** explaining the "why", not the "what"

### Formatting

```bash
# Format code before committing
mvn spotless:apply

# Check formatting
mvn spotless:check
```

### Module Dependencies

**Critical rule**: Plugins MUST only depend on `hexaglue-spi`, NEVER on `hexaglue-core`.

```
hexaglue-spi (no dependencies, JDK only)
    ^
    |  (plugins depend on SPI only)
    |
hexaglue-core (depends on SPI)
    ^
    |  (test scope only)
    |
hexaglue-testing (depends on core for testing)
```

### SPI Stability

When modifying the SPI (`hexaglue-spi`):

- **NEVER** break backward compatibility in MAJOR releases
- Use default methods to add new interface methods
- Mark new APIs with `@since M.m.0`
- Deprecate before removal (one MAJOR version warning)

---

## Commit Message Convention

We follow **Conventional Commits**:

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Types

- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, no logic change)
- `refactor`: Code refactoring (no behavior change)
- `perf`: Performance improvements
- `test`: Adding or updating tests
- `chore`: Maintenance tasks (dependencies, build, etc.)

### Scopes

- `spi` - SPI changes
- `core` - Core engine changes
- `graph` - Graph-based analysis
- `classification` - Type classification
- `maven` - Maven plugin
- `jpa` - JPA plugin
- `livingdoc` - Living documentation plugin
- `testing` - Testing harness
- `examples` - Example applications
- `docs` - Documentation

### Examples

```
feat(jpa): add support for @ElementCollection mapping

- Generate embeddable collections for value object lists
- Add @CollectionTable annotation with proper naming
- Includes tests for OrderLine collection scenario

Closes #123
```

```
fix(core): resolve incorrect port direction for UseCase suffix

The port classifier was incorrectly marking interfaces ending
with "UseCase" as DRIVEN instead of DRIVING.

- Fixed suffix detection in PortClassifier
- Added regression test
- Updated classification criteria priority

Fixes #456
```

---

## Pull Request Process

### 1. Prepare Your PR

```bash
# Ensure code is formatted
mvn spotless:apply

# Run all tests
mvn clean test

# Build the entire project
mvn clean install

# Commit and push
git add .
git commit -m "feat(scope): your change description"
git push origin feature/your-branch
```

### 2. Create Pull Request

Open a PR on GitHub with a clear description:

**Title:** Follow conventional commits format
```
feat(jpa): add support for @ManyToMany relationships
```

**Description template:**
```markdown
## Summary
Brief description of what this PR does.

## Motivation
Why is this change needed? What problem does it solve?

## Changes
- Bullet point list of changes
- Be specific and concise

## Testing
- How was this tested?
- What test cases were added?

## Breaking Changes
- List any breaking changes (should be rare)
- Provide migration guide if applicable

## Related Issues
Closes #123
```

### 3. Code Review

- Respond to review comments promptly
- Make requested changes in new commits (don't force-push during review)
- Mark conversations as resolved when addressed
- Request re-review when ready

### 4. Merge

- Maintainers will merge approved PRs
- Commits may be squashed for cleaner history
- Delete your branch after merge

### CI Requirements (📅 Planned )

All PRs must pass:
- All tests passing
- Code formatted with Spotless
- No new compiler warnings
- Javadoc builds without errors

---

## Reporting Issues

### Bug Reports

Include:
- Clear, descriptive title
- Steps to reproduce
- Expected vs actual behavior
- HexaGlue version
- Java version (`java --version` output)
- Relevant code snippets or diagnostic codes

### Feature Requests

Include:
- Clear use case description
- Proposed solution
- Alternatives considered
- Impact on existing functionality
- Willingness to contribute implementation

### Security Issues

**DO NOT** open public issues for security vulnerabilities.

Email security concerns to: info@hexaglue.io

---

## Questions?

- **General questions**: [GitHub Discussions](https://github.com/hexaglue/hexaglue/discussions)
- **Bug reports**: [GitHub Issues](https://github.com/hexaglue/hexaglue/issues)
- **Security**: info@hexaglue.io

---

## License

By submitting code to HexaGlue, you grant the project full rights to use, modify, and distribute your contributions under the **Mozilla Public License 2.0 (MPL-2.0)**.

See [LICENSE](LICENSE) for details.

---

<div align="center">

**HexaGlue - Focus on business code, not infrastructure glue.**

Made with ❤️ by Scalastic<br>
Copyright 2026 Scalastic - Released under MPL-2.0

</div>
