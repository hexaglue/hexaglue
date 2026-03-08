#!/usr/bin/env node

/**
 * bump-site-versions.js — Updates version references in the hexaglue-site repo.
 *
 * Usage:
 *   node scripts/bump-site-versions.js --core 6.1.0 --plugins 3.1.0 [--site-dir ../hexaglue-site] [--copy-metadata] [--yes] [--dry-run]
 *
 * Flags:
 *   --core <ver>        New core version
 *   --plugins <ver>     New plugins version
 *   --site-dir <path>   Path to hexaglue-site (default: ../hexaglue-site)
 *   --copy-metadata     Copy generated JSON metadata to site
 *   --yes               Skip interactive confirmation
 *   --dry-run           Show planned changes without writing files
 */

import fs from 'node:fs';
import path from 'node:path';
import { createInterface } from 'node:readline';

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

const ROOT = path.resolve(import.meta.dirname, '..');

// Archive pages that must NEVER be touched
const ARCHIVE_PATTERN = /\/\d+\.\d+\.\d+\.astro$/;

// ---------------------------------------------------------------------------
// CLI parsing
// ---------------------------------------------------------------------------

function parseArgs() {
  const args = process.argv.slice(2);
  const opts = {
    core: null,
    plugins: null,
    siteDir: path.resolve(ROOT, '..', 'hexaglue-site'),
    copyMetadata: false,
    yes: false,
    dryRun: false,
  };
  for (let i = 0; i < args.length; i++) {
    switch (args[i]) {
      case '--core':
        opts.core = args[++i];
        break;
      case '--plugins':
        opts.plugins = args[++i];
        break;
      case '--site-dir':
        opts.siteDir = path.resolve(args[++i]);
        break;
      case '--copy-metadata':
        opts.copyMetadata = true;
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
    console.error('Usage: node scripts/bump-site-versions.js --core <ver> --plugins <ver> [--site-dir <path>] [--copy-metadata] [--yes] [--dry-run]');
    process.exit(1);
  }
  if (!fs.existsSync(opts.siteDir)) {
    console.error(`Site directory not found: ${opts.siteDir}`);
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

function isArchivePage(filePath) {
  return ARCHIVE_PATTERN.test(filePath);
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
// Update functions
// ---------------------------------------------------------------------------

function updateVersionsTs(siteDir, coreVersion, pluginsVersion, dryRun) {
  const filePath = path.join(siteDir, 'src', 'data', 'versions.ts');
  if (!fs.existsSync(filePath)) return [];

  let content = fs.readFileSync(filePath, 'utf8');
  let modified = false;

  const newContent1 = content.replace(
    new RegExp("(hexaglue:\\s*')[\\d.]+'"),
    `$1${coreVersion}'`
  );
  if (newContent1 !== content) { content = newContent1; modified = true; }

  const newContent2 = content.replace(
    new RegExp("(plugins:\\s*')[\\d.]+'"),
    `$1${pluginsVersion}'`
  );
  if (newContent2 !== content) { content = newContent2; modified = true; }

  if (modified) {
    if (!dryRun) fs.writeFileSync(filePath, content, 'utf8');
    return [{ file: 'src/data/versions.ts', description: `hexaglue → ${coreVersion}, plugins → ${pluginsVersion}` }];
  }
  return [];
}

function updateGettingStarted(siteDir, coreVersion, dryRun) {
  const filePath = path.join(siteDir, 'src', 'pages', 'docs', 'getting-started.astro');
  if (!fs.existsSync(filePath)) return [];

  let content = fs.readFileSync(filePath, 'utf8');
  const newContent = content.replace(/hexaglue:\d+\.\d+\.\d+:/g, `hexaglue:${coreVersion}:`);

  if (newContent !== content) {
    if (!dryRun) fs.writeFileSync(filePath, newContent, 'utf8');
    return [{ file: 'src/pages/docs/getting-started.astro', description: `hexaglue:X.Y.Z: → hexaglue:${coreVersion}:` }];
  }
  return [];
}

function updateCaseStudy(siteDir, relativePath, coreVersion, pluginsVersion, dryRun) {
  const filePath = path.join(siteDir, relativePath);
  if (!fs.existsSync(filePath) || isArchivePage(filePath)) return [];

  let content = fs.readFileSync(filePath, 'utf8');
  let modified = false;

  // Replace core artifact versions
  for (const aid of ['hexaglue-maven-plugin']) {
    const r = replaceVersionNearArtifact(content, aid, coreVersion);
    if (r.count > 0) { content = r.content; modified = true; }
  }

  // Replace plugin artifact versions
  for (const aid of ['hexaglue-plugin-jpa', 'hexaglue-plugin-living-doc', 'hexaglue-plugin-audit', 'hexaglue-plugin-rest']) {
    const r = replaceVersionNearArtifact(content, aid, pluginsVersion);
    if (r.count > 0) { content = r.content; modified = true; }
  }

  if (modified) {
    if (!dryRun) fs.writeFileSync(filePath, content, 'utf8');
    return [{ file: relativePath, description: 'Case study versions updated' }];
  }
  return [];
}

function copyMetadata(siteDir, dryRun) {
  const srcDir = path.join(ROOT, 'docs', 'generated-metadata');
  const destDir = path.join(siteDir, 'src', 'data', 'generated');
  if (!fs.existsSync(srcDir)) {
    console.warn(`  Warning: ${srcDir} not found, skipping metadata copy`);
    return [];
  }

  const changes = [];
  const files = fs.readdirSync(srcDir).filter(f => f.endsWith('.json'));
  for (const file of files) {
    const src = path.join(srcDir, file);
    const dest = path.join(destDir, file);
    changes.push({ file: `src/data/generated/${file}`, description: 'Metadata JSON copied' });
    if (!dryRun) {
      fs.mkdirSync(destDir, { recursive: true });
      fs.copyFileSync(src, dest);
    }
  }
  return changes;
}

// ---------------------------------------------------------------------------
// Main
// ---------------------------------------------------------------------------

async function main() {
  const opts = parseArgs();

  console.log('');
  console.log('HexaGlue Site Version Bump');
  console.log('==========================');
  console.log(`  Site dir:    ${opts.siteDir}`);
  console.log(`  New core:    ${opts.core}`);
  console.log(`  New plugins: ${opts.plugins}`);
  if (opts.copyMetadata) console.log('  Copy metadata: yes');
  if (opts.dryRun) console.log('  *** DRY RUN — no files will be modified ***');
  console.log('');

  const allChanges = [];

  // versions.ts
  allChanges.push(...updateVersionsTs(opts.siteDir, opts.core, opts.plugins, true));

  // getting-started.astro
  allChanges.push(...updateGettingStarted(opts.siteDir, opts.core, true));

  // Case studies
  const caseStudies = [
    'src/pages/case-studies/ecommerce-migration/index.astro',
    'src/pages/case-studies/banking-migration/index.astro',
  ];
  for (const cs of caseStudies) {
    allChanges.push(...updateCaseStudy(opts.siteDir, cs, opts.core, opts.plugins, true));
  }

  // Metadata copy
  if (opts.copyMetadata) {
    allChanges.push(...copyMetadata(opts.siteDir, true));
  }

  // Display summary
  for (const c of allChanges) {
    console.log(`  ${c.file}: ${c.description}`);
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

  updateVersionsTs(opts.siteDir, opts.core, opts.plugins, false);
  updateGettingStarted(opts.siteDir, opts.core, false);
  for (const cs of caseStudies) {
    updateCaseStudy(opts.siteDir, cs, opts.core, opts.plugins, false);
  }
  if (opts.copyMetadata) {
    copyMetadata(opts.siteDir, false);
  }

  console.log('');
  console.log(`Done. ${allChanges.length} change(s) applied.`);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
