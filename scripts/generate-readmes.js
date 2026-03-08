#!/usr/bin/env node

/**
 * generate-readmes.js
 *
 * Generates / updates README configuration tables for HexaGlue plugins.
 *
 * For existing READMEs (JPA, Living-Doc, Audit):
 *   Replaces content between <!-- GENERATED:CONFIG:START --> / <!-- GENERATED:CONFIG:END -->
 *   and <!-- GENERATED:MAVEN:START --> / <!-- GENERATED:MAVEN:END --> markers.
 *   If markers are missing, prints a warning and skips.
 *
 * For REST (no existing README):
 *   Generates a full README from the extracted metadata.
 *
 * Prerequisite: run `make doc-metadata` first (or `node scripts/extract-doc-metadata.js`).
 */

import { readFileSync, writeFileSync, existsSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const ROOT = join(__dirname, '..');
const METADATA_DIR = join(ROOT, 'docs', 'generated-metadata');

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function loadJson(name) {
  const path = join(METADATA_DIR, name);
  if (!existsSync(path)) {
    console.error(`ERROR: ${path} not found. Run 'make doc-metadata' first.`);
    process.exit(1);
  }
  return JSON.parse(readFileSync(path, 'utf8'));
}

function formatDefault(raw) {
  if (raw === 'null') return '`null`';
  if (raw === '{}') return '`{}`';
  if (raw === '""') return '`""`';
  // Strip surrounding quotes for string values
  const stripped = raw.replace(/^"|"$/g, '');
  return `\`${stripped}\``;
}

function formatType(type) {
  if (type === 'String') return 'string';
  if (type === 'Boolean') return 'boolean';
  if (type === 'File') return 'string';
  if (type === 'IdentityStrategy') return 'string';
  if (type === 'Map<String, Integer>') return 'map';
  return type;
}

/**
 * Build a Markdown table for YAML config options.
 * Columns: Option | Default | Description
 */
function buildConfigTable(params, descriptionOverrides = {}) {
  const lines = [
    '| Option | Default | Description |',
    '|--------|---------|-------------|',
  ];
  for (const p of params) {
    const desc = descriptionOverrides[p.name] || p.description;
    lines.push(`| \`${p.name}\` | ${formatDefault(p.defaultValue)} | ${desc} |`);
  }
  return lines.join('\n');
}

/**
 * Build a Markdown table for Maven parameters.
 * Columns: Parameter | Type | Default | Description
 */
function buildMavenTable(params, descriptionOverrides = {}) {
  const lines = [
    '| Parameter | Type | Default | Description |',
    '|-----------|------|---------|-------------|',
  ];
  for (const p of params) {
    const type = formatType(p.type);
    const defaultVal = p.required
      ? '(required)'
      : p.defaultValue
        ? `\`${p.defaultValue}\``
        : '';
    const desc = descriptionOverrides[p.name] || p.description;
    lines.push(`| \`${p.name}\` | ${type} | ${defaultVal} | ${desc} |`);
  }
  return lines.join('\n');
}

/**
 * Replace content between marker comments in a README.
 * Returns the updated content, or null if markers not found.
 */
function replaceMarkerSection(content, markerName, replacement) {
  const startTag = `<!-- GENERATED:${markerName}:START -->`;
  const endTag = `<!-- GENERATED:${markerName}:END -->`;
  const startIdx = content.indexOf(startTag);
  const endIdx = content.indexOf(endTag);
  if (startIdx === -1 || endIdx === -1) {
    return null;
  }
  return (
    content.substring(0, startIdx + startTag.length) +
    '\n' +
    replacement +
    '\n' +
    content.substring(endIdx)
  );
}

// ---------------------------------------------------------------------------
// Plugin definitions
// ---------------------------------------------------------------------------

const mavenGoals = loadJson('maven-goals.json');

function findGoalParams(goalName) {
  const goal = mavenGoals.goals.find((g) => g.name === goalName);
  return goal ? goal.parameters : [];
}

// ---------------------------------------------------------------------------
// JPA README
// ---------------------------------------------------------------------------

function updateJpaReadme() {
  const readmePath = join(ROOT, 'hexaglue-plugins', 'hexaglue-plugin-jpa', 'README.md');
  if (!existsSync(readmePath)) {
    console.warn('WARN: JPA README not found, skipping.');
    return;
  }
  let content = readFileSync(readmePath, 'utf8');
  const config = loadJson('plugin-jpa.json');

  // All YAML params: config record + extra params
  const allParams = [...config.parameters, ...config.extraParameters];

  // Config table
  const configTable = buildConfigTable(allParams);
  let updated = replaceMarkerSection(content, 'CONFIG', configTable);
  if (updated === null) {
    console.warn('WARN: JPA README missing CONFIG markers, skipping config table.');
    updated = content;
  }

  // Maven table
  const mvnParams = findGoalParams('generate');
  const mavenTable = buildMavenTable(mvnParams);
  const updated2 = replaceMarkerSection(updated, 'MAVEN', mavenTable);
  if (updated2 === null) {
    console.warn('WARN: JPA README missing MAVEN markers, skipping maven table.');
  } else {
    updated = updated2;
  }

  writeFileSync(readmePath, updated);
  console.log('OK: JPA README updated.');
}

// ---------------------------------------------------------------------------
// Living-Doc README
// ---------------------------------------------------------------------------

function updateLivingDocReadme() {
  const readmePath = join(ROOT, 'hexaglue-plugins', 'hexaglue-plugin-living-doc', 'README.md');
  if (!existsSync(readmePath)) {
    console.warn('WARN: Living-Doc README not found, skipping.');
    return;
  }
  let content = readFileSync(readmePath, 'utf8');
  const config = loadJson('plugin-living-doc.json');

  const allParams = [...config.parameters, ...config.extraParameters];
  const configTable = buildConfigTable(allParams);
  let updated = replaceMarkerSection(content, 'CONFIG', configTable);
  if (updated === null) {
    console.warn('WARN: Living-Doc README missing CONFIG markers, skipping config table.');
    updated = content;
  }

  const mvnParams = findGoalParams('generate');
  const mavenTable = buildMavenTable(mvnParams);
  const updated2 = replaceMarkerSection(updated, 'MAVEN', mavenTable);
  if (updated2 === null) {
    console.warn('WARN: Living-Doc README missing MAVEN markers, skipping maven table.');
  } else {
    updated = updated2;
  }

  writeFileSync(readmePath, updated);
  console.log('OK: Living-Doc README updated.');
}

// ---------------------------------------------------------------------------
// Audit README
// ---------------------------------------------------------------------------

function updateAuditReadme() {
  const readmePath = join(ROOT, 'hexaglue-plugins', 'hexaglue-plugin-audit', 'README.md');
  if (!existsSync(readmePath)) {
    console.warn('WARN: Audit README not found, skipping.');
    return;
  }
  let content = readFileSync(readmePath, 'utf8');
  const config = loadJson('plugin-audit.json');

  // Audit YAML params: 3 failure params + generateDocs extra + severityOverrides
  const yamlParams = [
    { name: 'failOnError', type: 'boolean', defaultValue: 'true', description: 'Fail build when audit errors are found' },
    { name: 'errorOnBlocker', type: 'boolean', defaultValue: 'true', description: 'Treat BLOCKER violations as errors' },
    { name: 'errorOnCritical', type: 'boolean', defaultValue: 'false', description: 'Treat CRITICAL violations as errors' },
  ];
  for (const ep of config.extraParameters) {
    yamlParams.push(ep);
  }

  const configTable = buildConfigTable(yamlParams);
  let updated = replaceMarkerSection(content, 'CONFIG', configTable);
  if (updated === null) {
    console.warn('WARN: Audit README missing CONFIG markers, skipping config table.');
    updated = content;
  }

  // Maven params: audit goal + tolerantResolution from generate + validationReportPath from validate
  const auditParams = [...findGoalParams('audit')];
  const genParams = findGoalParams('generate');
  const tolerant = genParams.find((p) => p.name === 'tolerantResolution');
  if (tolerant) auditParams.push(tolerant);
  const valParams = findGoalParams('validate');
  const validationReport = valParams.find((p) => p.name === 'validationReportPath');
  if (validationReport) auditParams.push(validationReport);

  const mavenTable = buildMavenTable(auditParams);
  const updated2 = replaceMarkerSection(updated, 'MAVEN', mavenTable);
  if (updated2 === null) {
    console.warn('WARN: Audit README missing MAVEN markers, skipping maven table.');
  } else {
    updated = updated2;
  }

  writeFileSync(readmePath, updated);
  console.log('OK: Audit README updated.');
}

// ---------------------------------------------------------------------------
// REST README (generated from scratch if missing, updated if markers exist)
// ---------------------------------------------------------------------------

function generateRestReadme() {
  const readmePath = join(ROOT, 'hexaglue-plugins', 'hexaglue-plugin-rest', 'README.md');
  const config = loadJson('plugin-rest.json');
  const mvnParams = findGoalParams('generate');

  const configTable = buildConfigTable(config.parameters);
  const mavenTable = buildMavenTable(mvnParams);

  // If README exists and has markers, update in place
  if (existsSync(readmePath)) {
    let content = readFileSync(readmePath, 'utf8');
    let modified = false;

    const updated1 = replaceMarkerSection(content, 'CONFIG', configTable);
    if (updated1 !== null) {
      content = updated1;
      modified = true;
    }
    const updated2 = replaceMarkerSection(content, 'MAVEN', mavenTable);
    if (updated2 !== null) {
      content = updated2;
      modified = true;
    }

    if (modified) {
      writeFileSync(readmePath, content);
      console.log('OK: REST README updated (markers found).');
      return;
    }
  }

  // Generate full README
  const yamlExample = config.parameters
    .filter((p) => p.defaultValue !== 'null' && p.defaultValue !== '{}')
    .map((p) => {
      const val = p.type === 'boolean'
        ? p.defaultValue
        : p.defaultValue.replace(/^"|"$/g, '');
      return `    ${p.name}: ${p.type === 'String' ? `"${val}"` : val}`;
    })
    .join('\n');

  const readme = `# HexaGlue REST Plugin

Generates Spring REST controllers, DTOs, exception handler and configuration from your driving ports.

## Features

- **REST Controllers** - \`@RestController\` classes with CRUD endpoints derived from driving port methods
- **Request/Response DTOs** - Immutable Java records for request and response payloads
- **OpenAPI Annotations** - Optional \`@Tag\`, \`@Operation\`, \`@ApiResponse\` annotations for Swagger/OpenAPI docs
- **Exception Handler** - Optional \`@RestControllerAdvice\` for centralized error handling
- **Configuration Class** - Optional \`@Configuration\` exposing application services via their driving ports
- **Value Object Flattening** - Multi-field value objects are automatically flattened in DTOs

## Installation

Add the plugin as a dependency to the HexaGlue Maven plugin:

\`\`\`xml
<plugin>
    <groupId>io.hexaglue</groupId>
    <artifactId>hexaglue-maven-plugin</artifactId>
    <version>\${hexaglue.version}</version>
    <extensions>true</extensions>
    <configuration>
        <basePackage>com.example</basePackage>
    </configuration>
    <dependencies>
        <dependency>
            <groupId>io.hexaglue.plugins</groupId>
            <artifactId>hexaglue-plugin-rest</artifactId>
            <version>\${hexaglue.version}</version>
        </dependency>
    </dependencies>
</plugin>
\`\`\`

## Generated Files

For each driving port, the plugin generates:

\`\`\`
target/generated-sources/hexaglue/
└── com/example/api/
    ├── OrderController.java          # REST controller
    ├── CreateOrderRequest.java       # Request DTO
    ├── OrderResponse.java            # Response DTO
    ├── GlobalExceptionHandler.java   # Exception handler (optional)
    └── RestConfiguration.java        # Spring @Configuration (optional)
\`\`\`

## Configuration Options

<!-- GENERATED:CONFIG:START -->
${configTable}
<!-- GENERATED:CONFIG:END -->

### Maven Parameters

These parameters are set in the \`<configuration>\` block of the Maven plugin:

<!-- GENERATED:MAVEN:START -->
${mavenTable}
<!-- GENERATED:MAVEN:END -->

### YAML Configuration

\`\`\`yaml
plugins:
  io.hexaglue.plugin.rest:
${yamlExample}
\`\`\`

## Plugin ID

\`io.hexaglue.plugin.rest\`

---

**HexaGlue - Focus on business code, not infrastructure glue.**
`;

  writeFileSync(readmePath, readme);
  console.log('OK: REST README generated.');
}

// ---------------------------------------------------------------------------
// Main
// ---------------------------------------------------------------------------

console.log('Generating/updating plugin READMEs from extracted metadata...');
console.log();

updateJpaReadme();
updateLivingDocReadme();
updateAuditReadme();
generateRestReadme();

console.log();
console.log('Done.');
