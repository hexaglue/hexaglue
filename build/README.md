# HexaGlue Build Infrastructure

This directory contains build infrastructure modules for development, quality assurance, and distribution.

## Directory Structure

```
build/
├── tools/              # Shared configuration files (Checkstyle, SpotBugs, PMD)
├── coverage/           # JaCoCo coverage report aggregation
├── distribution/       # Maven Central publishing configuration
├── integration-tests/  # Integration tests runner
└── build.log           # Build output (generated)
```

## Prerequisites

- Java 17+
- Maven 3.9+
- GPG keys configured (for release only)

---

## Quality Commands

### Install Dependencies

Before running quality checks, ensure all modules are installed:

```zsh
mvn install -DskipTests
```

### Code Formatting

Apply code formatting:

```zsh
mvn com.diffplug.spotless:spotless-maven-plugin:apply
```

Check code formatting (Palantir Java Format):

```zsh
mvn com.diffplug.spotless:spotless-maven-plugin:check
```

### Run All Quality Checks

Run Checkstyle, SpotBugs, and PMD on all modules:

```zsh
mvn verify -Pquality -DskipTests
```

Run quality checks on a specific module:

```zsh
mvn verify -Pquality -pl hexaglue-core -DskipTests
```

### Individual Quality Tools

Run Checkstyle only:

```zsh
mvn checkstyle:check -Pquality
```

Run SpotBugs only:

```zsh
mvn spotbugs:check -Pquality
```

Run PMD only:

```zsh
mvn pmd:check -Pquality
```

### Save Quality Report

Run quality checks and save output to log file:

```zsh
mvn verify -Pquality -DskipTests > build/build.log 2>&1
```

---

## Testing Commands

### Run All Tests

```zsh
mvn test
```

### Run Tests for a Specific Module

```zsh
mvn test -pl hexaglue-core
```

### Run a Single Test Class

```zsh
mvn test -Dtest=DomainClassifierTest
```

### Run a Single Test Method

```zsh
mvn test -Dtest=DomainClassifierTest#shouldClassifyAggregateRoot
```

---

## Coverage Commands

### Generate Coverage Reports

Run tests with JaCoCo coverage:

```zsh
mvn verify
```

### Generate Aggregated Coverage Report

Generate consolidated coverage report for all modules:

```zsh
mvn verify -pl build/coverage
```

Coverage report location: `build/target/jacoco-aggregate/index.html`

---

## Integration Tests Commands

### Run Integration Tests

Run integration tests on example applications:

```zsh
mvn verify -pl build/integration-tests
```

This will:
1. Build each example application (coffeeshop, ecommerce, minimal)
2. Run their test suites
3. Verify Spring Boot context loads correctly

---

## Distribution Commands

### Prerequisites for Release

1. **GPG key configured with gpg-agent and pinentry**

   Create or edit `~/.gnupg/gpg-agent.conf`:

   ```conf
   # Use pinentry for passphrase entry
   pinentry-program /opt/homebrew/bin/pinentry-mac
   # Allow loopback for Maven integration
   allow-loopback-pinentry
   # Cache passphrase for 1 hour
   default-cache-ttl 3600
   max-cache-ttl 7200
   ```

   Create or edit `~/.gnupg/gpg.conf`:

   ```conf
   use-agent
   pinentry-mode loopback
   ```

   Restart the GPG agent:

   ```zsh
   gpgconf --kill gpg-agent
   gpg-agent --daemon
   ```

   Test GPG signing:

   ```zsh
   echo "test" | gpg --clearsign
   ```

2. **Maven `settings.xml` configured with GPG and Central Portal credentials**:

   ```xml
   <settings>
       <servers>
           <server>
               <id>central</id>
               <username>YOUR_TOKEN_USERNAME</username>
               <password>YOUR_TOKEN_PASSWORD</password>
           </server>
       </servers>

       <profiles>
           <profile>
               <id>gpg</id>
               <properties>
                   <gpg.keyname>YOUR_GPG_KEY_ID</gpg.keyname>
               </properties>
           </profile>
       </profiles>

       <activeProfiles>
           <activeProfile>gpg</activeProfile>
       </activeProfiles>
   </settings>
   ```

### Build Release Artifacts

Build with source JARs, Javadoc, and GPG signatures:

```zsh
mvn verify -Prelease -DskipTests
```

En cas d'erreur GPG :

```zsh
# Vérifier que l'agent fonctionne
gpg --armor --sign --default-key B98107E7F52CF48B -o /dev/null /dev/null

# Si besoin, relancer l'agent
gpgconf --kill gpg-agent
gpg-agent --daemon
```

### Deploy to Maven Central

Deploy all artifacts to Maven Central (Central Portal):

```zsh
mvn deploy -Prelease -DskipTests
```

### Deploy a Specific Module

```zsh
mvn deploy -Prelease -pl hexaglue-spi -DskipTests
```

### Set Release Versions

Remove `-SNAPSHOT` from all module versions before deploying:

```zsh
# Core, SPI, Maven plugin, and all non-plugin modules
mvn versions:set -DnewVersion=X.Y.0 -DgenerateBackupPoms=false

# Plugins (separate versioning scheme)
mvn versions:set -DnewVersion=A.B.0 -DgenerateBackupPoms=false \
    -pl hexaglue-plugins/hexaglue-plugin-jpa,hexaglue-plugins/hexaglue-plugin-living-doc,hexaglue-plugins/hexaglue-plugin-audit

# Update the core version referenced by plugins parent POM
mvn versions:set-property -Dproperty=hexaglue.version -DnewVersion=X.Y.0 \
    -DgenerateBackupPoms=false -pl hexaglue-plugins
```

**Fichiers non geres par `versions:set` (mise a jour manuelle requise) :**

| Fichier | Propriete / element | Description |
|---------|---------------------|-------------|
| `build/tools/pom.xml` | `<version>` | Module standalone sans parent reactor |
| `pom.xml` | `hexaglue-build-tools.version` | Propriete custom referant build-tools |
| `hexaglue-plugins/hexaglue-plugins-bom/pom.xml` | `hexaglue-plugin-*.version` | 3 proprietes de version des plugins |
| `build/coverage/pom.xml` | `<version>` des deps plugins | Versions hardcodees des plugins JPA et Living-doc |

### Full Release Process

Complete release workflow:

```zsh
# 1. Ensure clean state
mvn clean

# 2. Run all tests
mvn test

# 3. Run quality checks
mvn verify -Pquality -DskipTests

# 4. Set release versions (see "Set Release Versions" above)
#    + mettre a jour manuellement les 4 fichiers listes

# 5. Build release artifacts (dry-run)
make release-check

# 6. Deploy to Maven Central
make release

# 7. Tag and GitHub release
git tag -a vX.Y.0 -m "Release HexaGlue X.Y.0 / Plugins A.B.0"
git push origin vX.Y.0
gh release create vX.Y.0 --title "HexaGlue X.Y.0 / Plugins A.B.0" --notes "..."

# 8. Bump to next SNAPSHOT (see "Bump to Next Development Cycle" below)
```

### Bump to Next Development Cycle

After a release, bump all versions to the next SNAPSHOT:

```zsh
# Core, SPI, Maven plugin, and all non-plugin modules
mvn versions:set -DnewVersion=X.Z.0-SNAPSHOT -DgenerateBackupPoms=false

# Plugins (separate versioning scheme)
mvn versions:set -DnewVersion=A.C.0-SNAPSHOT -DgenerateBackupPoms=false \
    -pl hexaglue-plugins/hexaglue-plugin-jpa,hexaglue-plugins/hexaglue-plugin-living-doc,hexaglue-plugins/hexaglue-plugin-audit

# Update the core version referenced by plugins parent POM
mvn versions:set-property -Dproperty=hexaglue.version -DnewVersion=X.Z.0-SNAPSHOT \
    -DgenerateBackupPoms=false -pl hexaglue-plugins
```

**Mettre a jour manuellement les memes 4 fichiers que pour la release (voir tableau ci-dessus).**

---

## Complete Build Pipeline

### Development Build

Quick build for development:

```zsh
mvn clean install -DskipTests
```

### CI Build

Full CI pipeline with tests and quality:

```zsh
mvn clean verify -Pquality
```

### Release Build

Full release pipeline:

```zsh
mvn clean deploy -Prelease -Pquality
```

---

## Makefile Targets Reference

Suggested Makefile targets based on the commands above:

| Target | Command | Description |
|--------|---------|-------------|
| `install` | `mvn install -DskipTests` | Install all modules locally |
| `test` | `mvn test` | Run all tests |
| `format` | `mvn spotless:apply` | Apply code formatting |
| `format-check` | `mvn spotless:check` | Check code formatting |
| `quality` | `mvn verify -Pquality -DskipTests` | Run all quality checks |
| `coverage` | `mvn verify -pl build/coverage` | Generate coverage report |
| `integration` | `mvn verify -pl build/integration-tests` | Run integration tests |
| `release-check` | `mvn verify -Prelease -DskipTests` | Build release artifacts |
| `release` | `mvn deploy -Prelease -DskipTests` | Deploy to Maven Central |
| `ci` | `mvn clean verify -Pquality` | Full CI build |
| `clean` | `mvn clean` | Clean all build artifacts |

---

## Troubleshooting

### Quality check fails with missing hexaglue-build-tools

Run install first:

```zsh
mvn install -pl build/tools -DskipTests
```

### Coverage report shows 0%

Ensure Surefire argLine includes JaCoCo agent:

```xml
<argLine>@{argLine} ...</argLine>
```

### GPG signing fails

Ensure GPG agent is running:

```zsh
gpg-agent --daemon
```

Or specify GPG passphrase:

```zsh
mvn deploy -Prelease -Dgpg.passphrase=YOUR_PASSPHRASE
```

---

<div align="center">

**HexaGlue - Focus on business code, not infrastructure glue.**

Made with ❤️ by Scalastic<br>
Copyright 2026 Scalastic - Released under MPL-2.0

</div>
