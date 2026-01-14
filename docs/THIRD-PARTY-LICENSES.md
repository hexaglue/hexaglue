# Third-Party Licenses

This document lists the main dependencies used by HexaGlue and their licenses.

## Runtime Dependencies

| Dependency | Group ID | License | SPDX ID |
|------------|----------|---------|---------|
| Spoon | fr.inria.gforge.spoon | CeCILL-C | CECILL-C |
| SnakeYAML | org.yaml | Apache 2.0 | Apache-2.0 |
| SLF4J API | org.slf4j | MIT | MIT |
| Maven Plugin API | org.apache.maven | Apache 2.0 | Apache-2.0 |
| Maven Core | org.apache.maven | Apache 2.0 | Apache-2.0 |

## Test Dependencies

| Dependency | Group ID | License | SPDX ID |
|------------|----------|---------|---------|
| JUnit Jupiter | org.junit.jupiter | EPL 2.0 | EPL-2.0 |
| AssertJ | org.assertj | Apache 2.0 | Apache-2.0 |
| jMolecules | org.jmolecules | Apache 2.0 | Apache-2.0 |
| SLF4J Simple | org.slf4j | MIT | MIT |

## License Summaries

### Apache License 2.0
- Permissive license
- Allows commercial use, modification, distribution
- Requires license and copyright notice preservation
- Provides patent grant

### MIT License
- Permissive license
- Allows commercial use, modification, distribution
- Requires license and copyright notice preservation

### Eclipse Public License 2.0 (EPL-2.0)
- Weak copyleft license
- Allows commercial use
- Modifications must be shared under EPL-2.0
- Compatible with GPL when secondary license specified

### CeCILL-C License
- French open source license (INRIA, CNRS, CEA)
- Compatible with LGPL
- Allows commercial use, modification, distribution
- Modifications to the library must be shared

## Module Dependencies

### hexaglue-spi
**Zero external runtime dependencies** - by design, plugins depend only on this module.

### hexaglue-core
- Spoon (CeCILL-C)
- SnakeYAML (Apache 2.0) - optional
- SLF4J API (MIT)

### hexaglue-maven-plugin
- Maven Plugin API (Apache 2.0) - provided
- Maven Core (Apache 2.0) - provided
- SnakeYAML (Apache 2.0)

## Compatibility

All dependencies use OSI-approved licenses compatible with HexaGlue's MPL-2.0 license.

---

<div align="center">

**HexaGlue - Compile your architecture, not just your code**

Made with ❤️ by Scalastic<br>
Copyright 2026 Scalastic - Released under MPL-2.0

</div>