#!/usr/bin/env node

/**
 * doc-check.js
 *
 * CI validation script that ensures documentation metadata and README config
 * tables are up-to-date with the current source code.
 *
 * Steps:
 * 1. Re-runs metadata extraction to a temp directory
 * 2. Compares fresh JSON vs committed JSON in docs/generated-metadata/
 * 3. Re-generates README sections and compares vs current README content
 * 4. Exits with code 1 if any differences found
 *
 * Prerequisite: `mvn compile` must have been run (for plugin.xml to exist).
 */

import { readFileSync, writeFileSync, mkdirSync, readdirSync, existsSync, rmSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { execSync } from 'node:child_process';
import { tmpdir } from 'node:os';

const __dirname = dirname(fileURLToPath(import.meta.url));
const ROOT = join(__dirname, '..');
const METADATA_DIR = join(ROOT, 'docs', 'generated-metadata');

let failures = 0;

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function fail(message) {
  console.error(`FAIL: ${message}`);
  failures++;
}

function pass(message) {
  console.log(`OK: ${message}`);
}

// ---------------------------------------------------------------------------
// Phase 1: Compare metadata JSON files
// ---------------------------------------------------------------------------

function checkMetadata() {
  console.log('--- Checking metadata JSON freshness ---');
  console.log();

  // Create temp dir for fresh extraction
  const tempDir = join(tmpdir(), `hexaglue-doc-check-${Date.now()}`);
  mkdirSync(tempDir, { recursive: true });

  try {
    // Run extraction script, overriding output dir via env variable
    execSync(`node ${join(__dirname, 'extract-doc-metadata.js')}`, {
      cwd: ROOT,
      env: { ...process.env, DOC_METADATA_OUTPUT_DIR: tempDir },
      stdio: 'pipe',
    });

    // Compare each JSON file
    if (!existsSync(METADATA_DIR)) {
      fail(`Committed metadata directory not found: ${METADATA_DIR}`);
      return;
    }

    const committedFiles = readdirSync(METADATA_DIR).filter((f) => f.endsWith('.json'));
    const freshFiles = readdirSync(tempDir).filter((f) => f.endsWith('.json'));

    // Check for missing files
    for (const f of freshFiles) {
      if (!committedFiles.includes(f)) {
        fail(`New metadata file ${f} not committed to docs/generated-metadata/`);
      }
    }
    for (const f of committedFiles) {
      if (!freshFiles.includes(f)) {
        fail(`Stale metadata file ${f} in docs/generated-metadata/ (no longer generated)`);
      }
    }

    // Compare content
    for (const f of freshFiles) {
      if (!committedFiles.includes(f)) continue;
      const committed = readFileSync(join(METADATA_DIR, f), 'utf8').trim();
      const fresh = readFileSync(join(tempDir, f), 'utf8').trim();
      if (committed === fresh) {
        pass(`${f} is up-to-date`);
      } else {
        fail(`${f} is stale. Run 'make doc-metadata' to update.`);
        // Show a brief diff hint
        const committedLines = committed.split('\n');
        const freshLines = fresh.split('\n');
        for (let i = 0; i < Math.max(committedLines.length, freshLines.length); i++) {
          if (committedLines[i] !== freshLines[i]) {
            console.error(`  Line ${i + 1}:`);
            console.error(`    committed: ${(committedLines[i] || '(missing)').substring(0, 120)}`);
            console.error(`    fresh:     ${(freshLines[i] || '(missing)').substring(0, 120)}`);
            if (i > 2) {
              console.error('  ... (more differences)');
              break;
            }
          }
        }
      }
    }
  } finally {
    // Cleanup temp dir
    rmSync(tempDir, { recursive: true, force: true });
  }
}

// ---------------------------------------------------------------------------
// Phase 2: Compare README marker sections
// ---------------------------------------------------------------------------

function extractMarkerContent(content, markerName) {
  const startTag = `<!-- GENERATED:${markerName}:START -->`;
  const endTag = `<!-- GENERATED:${markerName}:END -->`;
  const startIdx = content.indexOf(startTag);
  const endIdx = content.indexOf(endTag);
  if (startIdx === -1 || endIdx === -1) return null;
  return content.substring(startIdx + startTag.length, endIdx).trim();
}

function checkReadmes() {
  console.log();
  console.log('--- Checking README marker sections ---');
  console.log();

  // Generate fresh READMEs to a temp location and compare marker sections
  // We'll capture the generate-readmes output by running it in a temp copy
  const tempDir = join(tmpdir(), `hexaglue-readme-check-${Date.now()}`);
  mkdirSync(tempDir, { recursive: true });

  const readmes = [
    { name: 'JPA', path: join(ROOT, 'hexaglue-plugins', 'hexaglue-plugin-jpa', 'README.md') },
    { name: 'Living-Doc', path: join(ROOT, 'hexaglue-plugins', 'hexaglue-plugin-living-doc', 'README.md') },
    { name: 'Audit', path: join(ROOT, 'hexaglue-plugins', 'hexaglue-plugin-audit', 'README.md') },
    { name: 'REST', path: join(ROOT, 'hexaglue-plugins', 'hexaglue-plugin-rest', 'README.md') },
  ];

  try {
    // Copy current READMEs to temp
    for (const r of readmes) {
      if (existsSync(r.path)) {
        const tempPath = join(tempDir, `${r.name}.md`);
        writeFileSync(tempPath, readFileSync(r.path, 'utf8'));
      }
    }

    // Run generate-readmes to update the actual READMEs
    // Then compare and restore
    const originals = {};
    for (const r of readmes) {
      if (existsSync(r.path)) {
        originals[r.name] = readFileSync(r.path, 'utf8');
      }
    }

    // Run generation
    execSync(`node ${join(__dirname, 'generate-readmes.js')}`, {
      cwd: ROOT,
      stdio: 'pipe',
    });

    // Compare marker sections
    for (const r of readmes) {
      if (!existsSync(r.path)) {
        if (r.name === 'REST') {
          // REST README should have been generated
          fail(`${r.name} README was not generated`);
        }
        continue;
      }

      const fresh = readFileSync(r.path, 'utf8');
      const original = originals[r.name];

      if (!original) {
        // README was newly generated (REST case)
        pass(`${r.name} README generated (new)`);
        continue;
      }

      for (const marker of ['CONFIG', 'MAVEN']) {
        const originalSection = extractMarkerContent(original, marker);
        const freshSection = extractMarkerContent(fresh, marker);

        if (originalSection === null && freshSection === null) {
          // No markers in either version
          continue;
        }
        if (originalSection === null) {
          fail(`${r.name} README: missing ${marker} markers in committed version`);
          continue;
        }
        if (freshSection === null) {
          fail(`${r.name} README: ${marker} markers disappeared after generation`);
          continue;
        }
        if (originalSection === freshSection) {
          pass(`${r.name} README ${marker} section is up-to-date`);
        } else {
          fail(`${r.name} README ${marker} section is stale. Run 'make doc-readmes' to update.`);
        }
      }

      // Restore original README
      writeFileSync(r.path, original);
    }
  } finally {
    rmSync(tempDir, { recursive: true, force: true });
  }
}

// ---------------------------------------------------------------------------
// Main
// ---------------------------------------------------------------------------

console.log('=== HexaGlue Documentation Check ===');
console.log();

checkMetadata();
checkReadmes();

console.log();
if (failures > 0) {
  console.error(`=== FAILED: ${failures} check(s) failed ===`);
  console.error('Run "make doc-metadata" and "make doc-readmes" to fix.');
  process.exit(1);
} else {
  console.log('=== ALL CHECKS PASSED ===');
}
