#!/usr/bin/env node

/**
 * bump-versions.js — Automates version bumping across the HexaGlue mono-repo.
 *
 * Usage:
 *   node scripts/bump-versions.js --core 6.1.0 --plugins 3.1.0 [--snapshot] [--yes] [--dry-run]
 *
 * Flags:
 *   --core <ver>     New core version (io.hexaglue)
 *   --plugins <ver>  New plugins version (io.hexaglue.plugins)
 *   --snapshot       Post-release mode: only update POMs + examples, skip docs
 *   --yes            Skip interactive confirmation
 *   --dry-run        Show planned changes without writing files
 */

import { execSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import { createInterface } from 'node:readline';

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

const ROOT = path.resolve(import.meta.dirname, '..');
const PLUGINS_DIR = path.join(ROOT, 'hexaglue-plugins');

// Artifact IDs that belong to HexaGlue (used by replaceVersionNearArtifact)
const CORE_ARTIFACTS = ['hexaglue-maven-plugin'];
const PLUGIN_ARTIFACTS = [
  'hexaglue-plugin-jpa',
  'hexaglue-plugin-living-doc',
  'hexaglue-plugin-audit',
  'hexaglue-plugin-rest',
];
const BOM_PROPERTIES = [
  'hexaglue-plugin-jpa.version',
  'hexaglue-plugin-living-doc.version',
  'hexaglue-plugin-audit.version',
];

// ---------------------------------------------------------------------------
// CLI parsing
// ---------------------------------------------------------------------------

function parseArgs() {
  const args = process.argv.slice(2);
  const opts = { snapshot: false, yes: false, dryRun: false, core: null, plugins: null };
  for (let i = 0; i < args.length; i++) {
    switch (args[i]) {
      case '--core':
        opts.core = args[++i];
        break;
      case '--plugins':
        opts.plugins = args[++i];
        break;
      case '--snapshot':
        opts.snapshot = true;
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
    console.error('Usage: node scripts/bump-versions.js --core <ver> --plugins <ver> [--snapshot] [--yes] [--dry-run]');
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
 * Works in both POM files and XML snippets embedded in Markdown.
 * Returns { content, count } with the number of replacements made.
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

/**
 * Collect all POM files under a directory (recursive glob).
 */
function findPoms(dir) {
  const results = [];
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory() && entry.name !== 'target' && entry.name !== 'node_modules' && entry.name !== '.git') {
      results.push(...findPoms(full));
    } else if (entry.isFile() && entry.name === 'pom.xml') {
      results.push(full);
    }
  }
  return results;
}

// ---------------------------------------------------------------------------
// Version detection
// ---------------------------------------------------------------------------

function detectCurrentVersions() {
  // Core: first <version> in root pom.xml (before <packaging>)
  const rootPom = fs.readFileSync(path.join(ROOT, 'pom.xml'), 'utf8');
  const coreMatch = rootPom.match(/<artifactId>hexaglue-parent<\/artifactId>\s*\n\s*<version>([^<]+)<\/version>/);
  const currentCore = coreMatch ? coreMatch[1] : null;

  // Plugins: <version> in a plugin sub-module POM
  const jpaPom = fs.readFileSync(path.join(PLUGINS_DIR, 'hexaglue-plugin-jpa', 'pom.xml'), 'utf8');
  const pluginMatch = jpaPom.match(/<artifactId>hexaglue-plugin-jpa<\/artifactId>\s*\n\s*<version>([^<]+)<\/version>/);
  const currentPlugins = pluginMatch ? pluginMatch[1] : null;

  return { currentCore, currentPlugins };
}

// ---------------------------------------------------------------------------
// Step functions — each returns a list of changes { file, description }
// ---------------------------------------------------------------------------

/**
 * Step 1: Run mvn versions:set for core and plugins.
 */
function stepMvnVersionsSet(coreVersion, pluginsVersion, dryRun) {
  const changes = [];

  if (dryRun) {
    changes.push({ file: '(mvn versions:set)', description: `Core → ${coreVersion}` });
    changes.push({ file: '(mvn versions:set)', description: `Plugins → ${pluginsVersion}` });
  } else {
    console.log(`  Running mvn versions:set for core → ${coreVersion} ...`);
    execSync(`mvn versions:set -DnewVersion=${coreVersion} -DgenerateBackupPoms=false -q`, { cwd: ROOT, stdio: 'inherit' });

    console.log(`  Running mvn versions:set for plugins → ${pluginsVersion} ...`);
    execSync(`mvn versions:set -DnewVersion=${pluginsVersion} -DgenerateBackupPoms=false -q`, { cwd: PLUGINS_DIR, stdio: 'inherit' });

    changes.push({ file: '(mvn versions:set)', description: `Core → ${coreVersion}` });
    changes.push({ file: '(mvn versions:set)', description: `Plugins → ${pluginsVersion}` });
  }

  return changes;
}

/**
 * Step 2: Update manual properties not handled by versions:set.
 */
function stepManualProperties(coreVersion, pluginsVersion, dryRun) {
  const changes = [];

  // hexaglue-plugins/pom.xml: <hexaglue.version>
  const pluginsPomPath = path.join(PLUGINS_DIR, 'pom.xml');
  let pluginsPom = fs.readFileSync(pluginsPomPath, 'utf8');
  const newPluginsPom = replaceXmlProperty(pluginsPom, 'hexaglue.version', coreVersion);
  if (newPluginsPom !== pluginsPom) {
    changes.push({ file: rel(pluginsPomPath), description: `<hexaglue.version> → ${coreVersion}` });
    if (!dryRun) fs.writeFileSync(pluginsPomPath, newPluginsPom, 'utf8');
  }

  // pom.xml: <hexaglue-build-tools.version>
  const rootPomPath = path.join(ROOT, 'pom.xml');
  let rootPom = fs.readFileSync(rootPomPath, 'utf8');
  const newRootPom = replaceXmlProperty(rootPom, 'hexaglue-build-tools.version', coreVersion);
  if (newRootPom !== rootPom) {
    changes.push({ file: rel(rootPomPath), description: `<hexaglue-build-tools.version> → ${coreVersion}` });
    if (!dryRun) fs.writeFileSync(rootPomPath, newRootPom, 'utf8');
  }

  // hexaglue-plugins-bom/pom.xml: 3 plugin version properties
  const bomPomPath = path.join(PLUGINS_DIR, 'hexaglue-plugins-bom', 'pom.xml');
  let bomPom = fs.readFileSync(bomPomPath, 'utf8');
  let bomChanged = false;
  for (const prop of BOM_PROPERTIES) {
    const updated = replaceXmlProperty(bomPom, prop, pluginsVersion);
    if (updated !== bomPom) {
      bomChanged = true;
      bomPom = updated;
    }
  }
  if (bomChanged) {
    changes.push({ file: rel(bomPomPath), description: `BOM plugin properties → ${pluginsVersion}` });
    if (!dryRun) fs.writeFileSync(bomPomPath, bomPom, 'utf8');
  }

  return changes;
}

/**
 * Step 3: Update example POMs (context-aware replacement).
 */
function stepExamplePoms(coreVersion, pluginsVersion, dryRun) {
  const changes = [];
  const examplesDir = path.join(ROOT, 'examples');
  if (!fs.existsSync(examplesDir)) return changes;

  const poms = findPoms(examplesDir);
  for (const pomPath of poms) {
    let content = fs.readFileSync(pomPath, 'utf8');
    let modified = false;

    // Replace core artifact versions
    for (const artifactId of CORE_ARTIFACTS) {
      const result = replaceVersionNearArtifact(content, artifactId, coreVersion);
      if (result.count > 0) {
        content = result.content;
        modified = true;
      }
    }

    // Replace plugin artifact versions
    for (const artifactId of PLUGIN_ARTIFACTS) {
      const result = replaceVersionNearArtifact(content, artifactId, pluginsVersion);
      if (result.count > 0) {
        content = result.content;
        modified = true;
      }
    }

    if (modified) {
      changes.push({ file: rel(pomPath), description: 'Example POM versions updated' });
      if (!dryRun) fs.writeFileSync(pomPath, content, 'utf8');
    }
  }

  return changes;
}

/**
 * Step 4: Update documentation Markdown files (skipped if --snapshot).
 */
function stepDocs(coreVersion, pluginsVersion, dryRun) {
  const changes = [];

  // --- QUICK_START.md ---
  const qsPath = path.join(ROOT, 'docs', 'QUICK_START.md');
  if (fs.existsSync(qsPath)) {
    let qs = fs.readFileSync(qsPath, 'utf8');
    let qsModified = false;

    // Replace version near hexaglue-maven-plugin artifactId
    for (const aid of CORE_ARTIFACTS) {
      const r = replaceVersionNearArtifact(qs, aid, coreVersion);
      if (r.count > 0) { qs = r.content; qsModified = true; }
    }
    // Replace version near plugin artifactIds
    for (const aid of PLUGIN_ARTIFACTS) {
      const r = replaceVersionNearArtifact(qs, aid, pluginsVersion);
      if (r.count > 0) { qs = r.content; qsModified = true; }
    }
    // Replace hexaglue:X.Y.Z: in output blocks
    const outputRe = /hexaglue:\d+\.\d+\.\d+:/g;
    const newQs = qs.replace(outputRe, `hexaglue:${coreVersion}:`);
    if (newQs !== qs) { qs = newQs; qsModified = true; }

    if (qsModified) {
      changes.push({ file: rel(qsPath), description: 'QUICK_START.md versions updated' });
      if (!dryRun) fs.writeFileSync(qsPath, qs, 'utf8');
    }
  }

  // --- VALIDATION.md ---
  const valPath = path.join(ROOT, 'docs', 'VALIDATION.md');
  if (fs.existsSync(valPath)) {
    let val = fs.readFileSync(valPath, 'utf8');
    const newVal = val.replace(/hexaglue:\d+\.\d+\.\d+:/g, `hexaglue:${coreVersion}:`);
    if (newVal !== val) {
      changes.push({ file: rel(valPath), description: 'VALIDATION.md versions updated' });
      if (!dryRun) fs.writeFileSync(valPath, newVal, 'utf8');
    }
  }

  // --- ARCHITECTURE_AUDIT.md ---
  const auditDocPath = path.join(ROOT, 'docs', 'ARCHITECTURE_AUDIT.md');
  if (fs.existsSync(auditDocPath)) {
    let ad = fs.readFileSync(auditDocPath, 'utf8');
    let adModified = false;

    for (const aid of PLUGIN_ARTIFACTS) {
      const r = replaceVersionNearArtifact(ad, aid, pluginsVersion);
      if (r.count > 0) { ad = r.content; adModified = true; }
    }
    for (const aid of CORE_ARTIFACTS) {
      const r = replaceVersionNearArtifact(ad, aid, coreVersion);
      if (r.count > 0) { ad = r.content; adModified = true; }
    }
    // Also replace hexaglue:X.Y.Z: patterns in output blocks
    const newAd = ad.replace(/hexaglue:\d+\.\d+\.\d+:/g, `hexaglue:${coreVersion}:`);
    if (newAd !== ad) { ad = newAd; adModified = true; }

    if (adModified) {
      changes.push({ file: rel(auditDocPath), description: 'ARCHITECTURE_AUDIT.md versions updated' });
      if (!dryRun) fs.writeFileSync(auditDocPath, ad, 'utf8');
    }
  }

  // --- CHANGELOG.md: replace date placeholder ---
  const clPath = path.join(ROOT, 'CHANGELOG.md');
  if (fs.existsSync(clPath)) {
    let cl = fs.readFileSync(clPath, 'utf8');
    const today = new Date().toISOString().slice(0, 10); // YYYY-MM-DD
    const dateRe = new RegExp(
      `(\\[${escapeRegex(coreVersion)}\\]\\s*-\\s*)\\d{4}-\\d{2}-XX`,
    );
    const newCl = cl.replace(dateRe, `$1${today}`);
    if (newCl !== cl) {
      changes.push({ file: rel(clPath), description: `CHANGELOG date → ${today}` });
      if (!dryRun) fs.writeFileSync(clPath, newCl, 'utf8');
    }
  }

  // --- RELEASING.md: update version table ---
  const relPath = path.join(ROOT, 'RELEASING.md');
  if (fs.existsSync(relPath)) {
    let relDoc = fs.readFileSync(relPath, 'utf8');
    let relModified = false;

    // Update core version in the table row
    const coreRowRe = /(\| Core \+ Maven plugin\s*\|[^|]+\|\s*)\S+(\s*\|)/;
    const newRelDoc1 = relDoc.replace(coreRowRe, `$1${coreVersion}$2`);
    if (newRelDoc1 !== relDoc) { relDoc = newRelDoc1; relModified = true; }

    // Update plugins version in the table row
    const pluginsRowRe = /(\| Plugins\s*\|[^|]+\|\s*)\S+(\s*\|)/;
    const newRelDoc2 = relDoc.replace(pluginsRowRe, `$1${pluginsVersion}$2`);
    if (newRelDoc2 !== relDoc) { relDoc = newRelDoc2; relModified = true; }

    if (relModified) {
      changes.push({ file: rel(relPath), description: 'RELEASING.md version table updated' });
      if (!dryRun) fs.writeFileSync(relPath, relDoc, 'utf8');
    }
  }

  // --- hexaglue-plugin-audit/README.md ---
  const auditReadmePath = path.join(PLUGINS_DIR, 'hexaglue-plugin-audit', 'README.md');
  if (fs.existsSync(auditReadmePath)) {
    let ar = fs.readFileSync(auditReadmePath, 'utf8');
    let arModified = false;

    // Replace versions near HexaGlue artifactIds (fixes stale 2.0.0, 5.0.0)
    for (const aid of ['hexaglue-plugin-audit']) {
      const r = replaceVersionNearArtifact(ar, aid, pluginsVersion);
      if (r.count > 0) { ar = r.content; arModified = true; }
    }
    for (const aid of ['hexaglue-maven-plugin']) {
      const r = replaceVersionNearArtifact(ar, aid, coreVersion);
      if (r.count > 0) { ar = r.content; arModified = true; }
    }
    // "HexaGlue X.Y.Z or higher"
    const higherRe = /HexaGlue \d+\.\d+\.\d+ or higher/g;
    const newAr = ar.replace(higherRe, `HexaGlue ${coreVersion} or higher`);
    if (newAr !== ar) { ar = newAr; arModified = true; }

    if (arModified) {
      changes.push({ file: rel(auditReadmePath), description: 'audit README versions updated' });
      if (!dryRun) fs.writeFileSync(auditReadmePath, ar, 'utf8');
    }
  }

  return changes;
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function rel(absPath) {
  return path.relative(ROOT, absPath);
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

// ---------------------------------------------------------------------------
// Main
// ---------------------------------------------------------------------------

async function main() {
  const opts = parseArgs();
  const { currentCore, currentPlugins } = detectCurrentVersions();

  console.log('');
  console.log('HexaGlue Version Bump');
  console.log('=====================');
  console.log(`  Current core:    ${currentCore || '(not detected)'}`);
  console.log(`  Current plugins: ${currentPlugins || '(not detected)'}`);
  console.log(`  New core:        ${opts.core}`);
  console.log(`  New plugins:     ${opts.plugins}`);
  console.log(`  Mode:            ${opts.snapshot ? 'SNAPSHOT (POMs + examples only)' : 'RELEASE (POMs + examples + docs)'}`);
  if (opts.dryRun) console.log('  *** DRY RUN — no files will be modified ***');
  console.log('');

  // Collect all changes
  const allChanges = [];

  // Step 1: mvn versions:set
  console.log('Step 1: Maven versions:set');
  if (opts.dryRun) {
    allChanges.push(...stepMvnVersionsSet(opts.core, opts.plugins, true));
    console.log('  (dry-run) Would run mvn versions:set for core and plugins');
  } else {
    // Defer actual execution until after confirmation
  }

  // Step 2: Manual properties
  console.log('Step 2: Manual properties');
  const propChanges = stepManualProperties(opts.core, opts.plugins, true); // always dry-run first for preview
  allChanges.push(...propChanges);
  for (const c of propChanges) console.log(`  ${c.file}: ${c.description}`);

  // Step 3: Example POMs
  console.log('Step 3: Example POMs');
  const exampleChanges = stepExamplePoms(opts.core, opts.plugins, true);
  allChanges.push(...exampleChanges);
  console.log(`  ${exampleChanges.length} example POM(s) to update`);

  // Step 4: Docs (unless --snapshot)
  let docChanges = [];
  if (!opts.snapshot) {
    console.log('Step 4: Documentation');
    docChanges = stepDocs(opts.core, opts.plugins, true);
    allChanges.push(...docChanges);
    for (const c of docChanges) console.log(`  ${c.file}: ${c.description}`);
  } else {
    console.log('Step 4: Documentation (SKIPPED — snapshot mode)');
  }

  console.log('');
  console.log(`Total: ${allChanges.length} change(s) planned`);

  if (opts.dryRun) {
    console.log('');
    console.log('Dry run complete. No files were modified.');
    return;
  }

  // Confirm
  if (!opts.yes) {
    const ok = await confirm('Proceed with these changes?');
    if (!ok) {
      console.log('Aborted.');
      process.exit(0);
    }
  }

  // Execute for real
  console.log('');
  console.log('Applying changes...');

  // Step 1 (real)
  stepMvnVersionsSet(opts.core, opts.plugins, false);

  // Step 2 (real)
  stepManualProperties(opts.core, opts.plugins, false);

  // Step 3 (real)
  stepExamplePoms(opts.core, opts.plugins, false);

  // Step 4 (real)
  if (!opts.snapshot) {
    stepDocs(opts.core, opts.plugins, false);
  }

  console.log('');
  console.log(`Done. ${allChanges.length} change(s) applied.`);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
