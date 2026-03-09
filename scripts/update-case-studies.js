#!/usr/bin/env node

/**
 * update-case-studies.js — Bump versions in case study POMs, build them,
 * and collect audit-report.json files into the hexaglue-site data directory.
 *
 * Usage:
 *   node scripts/update-case-studies.js --core 6.1.0 --plugins 3.1.0 [--yes] [--dry-run]
 *
 * Flags:
 *   --core <ver>     New core version (io.hexaglue)
 *   --plugins <ver>  New plugins version (io.hexaglue.plugins)
 *   --yes            Skip interactive confirmation
 *   --dry-run        Show planned changes without writing files or building
 */

import { execSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import { createInterface } from 'node:readline';

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

const ROOT = path.resolve(import.meta.dirname, '..');
const PROJECTS_ROOT = path.resolve(ROOT, '..');
const SITE_DIR = path.join(PROJECTS_ROOT, 'hexaglue-site');
const DEST_DIR = path.join(SITE_DIR, 'src', 'data', 'case-studies');

const CASE_STUDIES = [
  {
    key: 'banking-legacy',
    dir: path.join(PROJECTS_ROOT, 'case-study-banking', 'legacy'),
    versionStrategy: 'inline', // versions hardcoded near <artifactId>
  },
  {
    key: 'banking-hexagonal',
    dir: path.join(PROJECTS_ROOT, 'case-study-banking', 'hexagonal'),
    versionStrategy: 'inline',
  },
  {
    key: 'ecommerce-legacy',
    dir: path.join(PROJECTS_ROOT, 'case-study-ecommerce', 'legacy'),
    versionStrategy: 'properties', // uses <hexaglue.version> / <hexaglue-plugins.version>
  },
  {
    key: 'ecommerce-hexagonal',
    dir: path.join(PROJECTS_ROOT, 'case-study-ecommerce', 'hexagonal'),
    versionStrategy: 'properties',
  },
];

const CORE_ARTIFACTS = ['hexaglue-maven-plugin'];
const PLUGIN_ARTIFACTS = [
  'hexaglue-plugin-audit',
  'hexaglue-plugin-jpa',
  'hexaglue-plugin-living-doc',
  'hexaglue-plugin-rest',
];

// ---------------------------------------------------------------------------
// CLI parsing
// ---------------------------------------------------------------------------

function parseArgs() {
  const args = process.argv.slice(2);
  const opts = { core: null, plugins: null, yes: false, dryRun: false };
  for (let i = 0; i < args.length; i++) {
    switch (args[i]) {
      case '--core':
        opts.core = args[++i];
        break;
      case '--plugins':
        opts.plugins = args[++i];
        break;
      case '--yes':
        opts.yes = true;
        break;
      case '--dry-run':
        opts.dryRun = true;
        break;
      default:
        console.error(`Unknown flag: ${args[i]}`);
        process.exit(1);
    }
  }
  if (!opts.core || !opts.plugins) {
    console.error('Usage: node scripts/update-case-studies.js --core <ver> --plugins <ver> [--yes] [--dry-run]');
    process.exit(1);
  }
  return opts;
}

// ---------------------------------------------------------------------------
// Utility functions
// ---------------------------------------------------------------------------

function escapeRegex(str) {
  return str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

/**
 * Replace <version>…</version> closest to a given <artifactId> in XML content.
 */
function replaceVersionNearArtifact(content, artifactId, newVersion) {
  const re = new RegExp(
    `(<artifactId>${escapeRegex(artifactId)}</artifactId>[\\s\\S]{0,200}?<version>)([^<]+)(</version>)`,
    'g'
  );
  let count = 0;
  const result = content.replace(re, (_, before, _oldVer, after) => {
    count++;
    return `${before}${newVersion}${after}`;
  });
  return { content: result, count };
}

/**
 * Replace a Maven XML property value: <propName>…</propName>
 */
function replaceXmlProperty(content, propertyName, newValue) {
  const re = new RegExp(
    `(<${escapeRegex(propertyName)}>)[^<]+(</${escapeRegex(propertyName)}>)`,
    'g'
  );
  return content.replace(re, `$1${newValue}$2`);
}

async function confirm(message) {
  const rl = createInterface({ input: process.stdin, output: process.stdout });
  return new Promise((resolve) => {
    rl.question(`${message} [y/N] `, (answer) => {
      rl.close();
      resolve(answer.toLowerCase() === 'y');
    });
  });
}

function stripSnapshot(version) {
  return version.replace(/-SNAPSHOT$/, '');
}

/**
 * Detect the Java version required by a Maven project from its POM.
 * @returns {string|null} e.g. "21", or null if not found
 */
function detectJavaVersion(pomPath) {
  try {
    const content = fs.readFileSync(pomPath, 'utf8');
    const match = content.match(/<java\.version>(\d+)<\/java\.version>/);
    return match ? match[1] : null;
  } catch {
    return null;
  }
}

/**
 * Resolve JAVA_HOME for a given Java version on macOS.
 * @returns {string|null} the JAVA_HOME path, or null if unavailable
 */
function resolveJavaHome(javaVersion) {
  if (process.platform !== 'darwin') return null;
  try {
    return execSync(`/usr/libexec/java_home -v ${javaVersion}`, { encoding: 'utf8' }).trim();
  } catch {
    return null;
  }
}

// ---------------------------------------------------------------------------
// Step 1: Bump versions in POMs
// ---------------------------------------------------------------------------

function bumpVersions(coreVersion, pluginsVersion, dryRun) {
  const changes = [];

  for (const cs of CASE_STUDIES) {
    const pomPath = path.join(cs.dir, 'pom.xml');
    if (!fs.existsSync(pomPath)) {
      console.warn(`  Warning: ${pomPath} not found, skipping`);
      continue;
    }

    let content = fs.readFileSync(pomPath, 'utf8');
    let modified = false;

    if (cs.versionStrategy === 'properties') {
      // Replace <hexaglue.version> and <hexaglue-plugins.version>
      const newContent1 = replaceXmlProperty(content, 'hexaglue.version', coreVersion);
      if (newContent1 !== content) { content = newContent1; modified = true; }

      const newContent2 = replaceXmlProperty(content, 'hexaglue-plugins.version', pluginsVersion);
      if (newContent2 !== content) { content = newContent2; modified = true; }
    } else {
      // Inline: replace near <artifactId>
      for (const aid of CORE_ARTIFACTS) {
        const r = replaceVersionNearArtifact(content, aid, coreVersion);
        if (r.count > 0) { content = r.content; modified = true; }
      }
      for (const aid of PLUGIN_ARTIFACTS) {
        const r = replaceVersionNearArtifact(content, aid, pluginsVersion);
        if (r.count > 0) { content = r.content; modified = true; }
      }
    }

    if (modified) {
      changes.push({ key: cs.key, file: pomPath });
      if (!dryRun) fs.writeFileSync(pomPath, content, 'utf8');
    }
  }

  return changes;
}

// ---------------------------------------------------------------------------
// Step 2: Build case studies
// ---------------------------------------------------------------------------

function buildCaseStudies(dryRun) {
  for (const cs of CASE_STUDIES) {
    if (!fs.existsSync(cs.dir)) {
      console.warn(`  Warning: ${cs.dir} not found, skipping`);
      continue;
    }

    const pomPath = path.join(cs.dir, 'pom.xml');
    const javaVersion = detectJavaVersion(pomPath);
    const javaHome = javaVersion ? resolveJavaHome(javaVersion) : null;
    const jdkLabel = javaVersion ? ` (JDK ${javaVersion})` : '';

    console.log(`  Building ${cs.key}...${jdkLabel}`);
    if (!dryRun) {
      try {
        const execOpts = { cwd: cs.dir, stdio: 'inherit' };
        if (javaHome) {
          execOpts.env = { ...process.env, JAVA_HOME: javaHome };
        }
        execSync('mvn clean verify -q', execOpts);
      } catch (e) {
        console.error(`  ERROR: Build failed for ${cs.key}`);
        process.exit(1);
      }
    }
  }
}

// ---------------------------------------------------------------------------
// Step 3: Collect audit-report.json
// ---------------------------------------------------------------------------

function collectReports(dryRun) {
  const collected = [];

  if (!dryRun) {
    fs.mkdirSync(DEST_DIR, { recursive: true });
  }

  for (const cs of CASE_STUDIES) {
    const reportPath = path.join(cs.dir, 'target', 'hexaglue', 'reports', 'audit', 'audit-report.json');
    const destPath = path.join(DEST_DIR, `${cs.key}.json`);

    if (dryRun) {
      collected.push({ key: cs.key, src: reportPath, dest: destPath });
      continue;
    }

    if (!fs.existsSync(reportPath)) {
      console.warn(`  Warning: ${reportPath} not found, skipping`);
      continue;
    }

    fs.copyFileSync(reportPath, destPath);

    // Strip -SNAPSHOT suffixes from version metadata
    try {
      const json = JSON.parse(fs.readFileSync(destPath, 'utf8'));
      let patched = false;
      if (json.metadata?.hexaglueVersion?.endsWith('-SNAPSHOT')) {
        json.metadata.hexaglueVersion = stripSnapshot(json.metadata.hexaglueVersion);
        patched = true;
      }
      if (json.metadata?.pluginVersion?.endsWith('-SNAPSHOT')) {
        json.metadata.pluginVersion = stripSnapshot(json.metadata.pluginVersion);
        patched = true;
      }
      if (patched) {
        fs.writeFileSync(destPath, JSON.stringify(json, null, 2) + '\n', 'utf8');
      }
    } catch {
      // JSON parse error — leave file as-is
    }

    collected.push({ key: cs.key, src: reportPath, dest: destPath });
  }

  return collected;
}

// ---------------------------------------------------------------------------
// Step 4: Summary
// ---------------------------------------------------------------------------

function printSummary(collected) {
  console.log('');
  console.log('Summary:');
  console.log('--------');

  for (const item of collected) {
    const destPath = item.dest;
    if (fs.existsSync(destPath)) {
      try {
        const data = JSON.parse(fs.readFileSync(destPath, 'utf8'));
        const score = data.verdict?.score ?? '?';
        const grade = data.verdict?.grade ?? '?';
        console.log(`  ${item.key}: score=${score}, grade=${grade}`);
      } catch {
        console.log(`  ${item.key}: collected (could not parse)`);
      }
    } else {
      console.log(`  ${item.key}: (dry-run, no file)`);
    }
  }
}

// ---------------------------------------------------------------------------
// Main
// ---------------------------------------------------------------------------

async function main() {
  const opts = parseArgs();

  console.log('');
  console.log('HexaGlue Case Studies Update');
  console.log('============================');
  console.log(`  Core version:    ${opts.core}`);
  console.log(`  Plugins version: ${opts.plugins}`);
  console.log(`  Destination:     ${DEST_DIR}`);
  if (opts.dryRun) console.log('  *** DRY RUN — no files will be modified ***');
  console.log('');

  // Step 1: Preview version bumps
  console.log('Step 1: Bump versions in case study POMs');
  const versionChanges = bumpVersions(opts.core, opts.plugins, true);
  for (const c of versionChanges) {
    console.log(`  ${c.key}: pom.xml updated`);
  }
  if (versionChanges.length === 0) {
    console.log('  (no changes needed)');
  }

  // Preview report collection
  console.log('');
  console.log('Step 2: Build 4 case studies (mvn clean verify)');
  console.log('Step 3: Collect audit-report.json files');
  for (const cs of CASE_STUDIES) {
    console.log(`  ${cs.key} → ${cs.key}.json`);
  }

  console.log('');
  console.log(`Total: ${versionChanges.length} POM(s) to update, ${CASE_STUDIES.length} builds, ${CASE_STUDIES.length} JSON(s) to collect`);

  if (opts.dryRun) {
    console.log('');
    console.log('Dry run complete. No files were modified.');
    return;
  }

  // Confirm
  if (!opts.yes) {
    const ok = await confirm('Proceed?');
    if (!ok) {
      console.log('Aborted.');
      process.exit(0);
    }
  }

  // Execute
  console.log('');
  console.log('Applying version bumps...');
  bumpVersions(opts.core, opts.plugins, false);

  console.log('');
  console.log('Building case studies...');
  buildCaseStudies(false);

  console.log('');
  console.log('Collecting audit reports...');
  const collected = collectReports(false);

  printSummary(collected);

  console.log('');
  console.log(`Done. ${collected.length} audit report(s) collected to ${DEST_DIR}`);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
