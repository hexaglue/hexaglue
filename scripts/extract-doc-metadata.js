#!/usr/bin/env node

/**
 * extract-doc-metadata.js
 *
 * Extracts documentation metadata from HexaGlue Java source code and Maven plugin
 * descriptor. Produces JSON files consumed by hexaglue-site and README generation.
 *
 * Prerequisites: mvn compile must have been run (plugin.xml must exist).
 *
 * Output: docs/generated-metadata/*.json
 */

import { readFileSync, writeFileSync, mkdirSync, existsSync, readdirSync } from 'node:fs';
import { join, resolve, dirname, basename } from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
const ROOT = resolve(__dirname, '..');

const OUTPUT_DIR = process.env.DOC_METADATA_OUTPUT_DIR || join(ROOT, 'docs', 'generated-metadata');

// ---------------------------------------------------------------------------
// XML mini-parser (no external deps)
// ---------------------------------------------------------------------------

function xmlText(xml, tag) {
  const re = new RegExp(`<${tag}>([\\s\\S]*?)</${tag}>`);
  const m = xml.match(re);
  return m ? m[1].trim() : null;
}

function xmlAll(xml, tag) {
  const re = new RegExp(`<${tag}>([\\s\\S]*?)</${tag}>`, 'g');
  const results = [];
  let m;
  while ((m = re.exec(xml)) !== null) {
    results.push(m[1]);
  }
  return results;
}

function xmlAttr(xml, tag, attr) {
  const re = new RegExp(`<${tag}\\s[^>]*${attr}="([^"]*)"[^>]*/?>`, 'g');
  const results = [];
  let m;
  while ((m = re.exec(xml)) !== null) {
    results.push(m[1]);
  }
  return results;
}

// ---------------------------------------------------------------------------
// Java source helpers
// ---------------------------------------------------------------------------

function readJavaFile(relativePath) {
  const abs = join(ROOT, relativePath);
  if (!existsSync(abs)) {
    console.error(`  WARNING: File not found: ${relativePath}`);
    return null;
  }
  return readFileSync(abs, 'utf-8');
}

/**
 * Parse a Config record's from(PluginConfig) method to extract parameters.
 * Handles patterns:
 *   config.getString("key", "default")
 *   config.getBoolean("key", default)
 *   config.getInteger("key").orElse(default)
 *   config.getString("key").orElse(default)
 *   config.getIntegerMap("key").orElse(...)
 *   IdentityStrategy.valueOf(config.getString("key", "default"))
 */
function parseConfigRecord(source, recordName) {
  if (!source) return [];

  const params = [];

  // Extract Javadoc @param tags from the record declaration
  const javadocParams = {};
  const recordDeclIdx = source.indexOf(`record ${recordName}`);
  if (recordDeclIdx > 0) {
    // Look for Javadoc above the record
    const before = source.substring(0, recordDeclIdx);
    // Match the Javadoc closest to the record (may have "public" or annotations between)
    const allJavadocs = [...before.matchAll(/\/\*\*([\s\S]*?)\*\//g)];
    const javadocMatch = allJavadocs.length > 0 ? allJavadocs[allJavadocs.length - 1] : null;
    if (javadocMatch) {
      const paramRe = /@param\s+(\w+)\s+(.*?)(?=@|\*\/)/gs;
      let pm;
      while ((pm = paramRe.exec(javadocMatch[1])) !== null) {
        javadocParams[pm[1]] = pm[2].replace(/\s*\*\s*/g, ' ').trim();
      }
    }
  }

  // Find the from() or fromPluginConfig() method body
  const fromMethodRe = /(?:static\s+\w+\s+from(?:PluginConfig)?)\s*\(\s*PluginConfig\s+\w+\s*\)\s*\{([\s\S]*?)^\s{4}\}/m;
  const fromMatch = source.match(fromMethodRe);
  if (!fromMatch) return params;

  const body = fromMatch[1];

  // All patterns use \w+ to match any variable name (config, pluginConfig, etc.)
  let m;

  // Pattern: xxx.getString("key", CONSTANT)
  const stringWithConstRe = /\w+\.getString\(\s*"(\w+)"\s*,\s*([A-Z_]+)\s*\)/g;
  while ((m = stringWithConstRe.exec(body)) !== null) {
    if (params.some(p => p.name === m[1])) continue;
    params.push({
      name: m[1],
      type: 'String',
      defaultValue: resolveConstant(source, m[2]),
      description: javadocParams[m[1]] || ''
    });
  }

  // Pattern: xxx.getString("key", "default")
  const stringWithDefaultRe = /\w+\.getString\(\s*"(\w+)"\s*,\s*"([^"]*)"\s*\)/g;
  while ((m = stringWithDefaultRe.exec(body)) !== null) {
    // Check if wrapped in something like IdentityStrategy.valueOf(...)
    const lineStart = body.lastIndexOf('\n', m.index);
    const beforeMatch = body.substring(lineStart, m.index);
    const enumMatch = beforeMatch.match(/(\w+)\.valueOf\s*\(\s*$/);

    if (enumMatch) {
      params.push({
        name: m[1],
        type: enumMatch[1],
        defaultValue: m[2],
        description: javadocParams[m[1]] || ''
      });
    } else {
      params.push({
        name: m[1],
        type: 'String',
        defaultValue: m[2] === '' ? '""' : `"${m[2]}"`,
        description: javadocParams[m[1]] || ''
      });
    }
  }

  // Pattern: xxx.getString("key").orElse(...)
  const stringOrElseRe = /\w+\.getString\(\s*"(\w+)"\s*\)\.orElse\(\s*(.+?)\s*\)/g;
  while ((m = stringOrElseRe.exec(body)) !== null) {
    if (params.some(p => p.name === m[1])) continue;
    params.push({
      name: m[1],
      type: 'String',
      defaultValue: formatDefault(m[2]),
      description: javadocParams[m[1]] || ''
    });
  }

  // Pattern: xxx.getBoolean("key", default)
  const boolRe = /\w+\.getBoolean\(\s*"(\w+)"\s*,\s*(true|false)\s*\)/g;
  while ((m = boolRe.exec(body)) !== null) {
    if (params.some(p => p.name === m[1])) continue;
    params.push({
      name: m[1],
      type: 'boolean',
      defaultValue: m[2],
      description: javadocParams[m[1]] || ''
    });
  }

  // Pattern: xxx.getInteger("key").orElse(...)
  const intOrElseRe = /\w+\.getInteger\(\s*"(\w+)"\s*\)\.orElse\(\s*(.+?)\s*\)/g;
  while ((m = intOrElseRe.exec(body)) !== null) {
    if (params.some(p => p.name === m[1])) continue;
    params.push({
      name: m[1],
      type: 'int',
      defaultValue: resolveConstant(source, m[2]),
      description: javadocParams[m[1]] || ''
    });
  }

  // Pattern: xxx.getInteger("key", default)
  const intWithDefaultRe = /\w+\.getInteger\(\s*"(\w+)"\s*,\s*(\d+)\s*\)/g;
  while ((m = intWithDefaultRe.exec(body)) !== null) {
    if (params.some(p => p.name === m[1])) continue;
    params.push({
      name: m[1],
      type: 'int',
      defaultValue: m[2],
      description: javadocParams[m[1]] || ''
    });
  }

  // Pattern: xxx.getIntegerMap("key").orElse(...)
  const intMapRe = /\w+\.getIntegerMap\(\s*"(\w+)"\s*\)\.orElse\(\s*(.+?)\s*\)/g;
  while ((m = intMapRe.exec(body)) !== null) {
    if (params.some(p => p.name === m[1])) continue;
    params.push({
      name: m[1],
      type: 'Map<String, Integer>',
      defaultValue: '{}',
      description: javadocParams[m[1]] || ''
    });
  }

  return params;
}

/** Resolve constant references like DEFAULT_OUTPUT_DIR to their values. */
function resolveConstant(source, expr) {
  const trimmed = expr.trim();
  // If it's a numeric literal
  if (/^\d+$/.test(trimmed)) return trimmed;
  // If it's a string literal
  if (/^".*"$/.test(trimmed)) return trimmed;
  // Try to find the constant in the same file
  const constRe = new RegExp(`${trimmed}\\s*=\\s*(.+?)\\s*;`);
  const cm = source.match(constRe);
  if (cm) {
    const val = cm[1].trim();
    if (/^".*"$/.test(val)) return val;
    if (/^\d+$/.test(val)) return val;
    return val;
  }
  return trimmed;
}

function formatDefault(expr) {
  const trimmed = expr.trim();
  if (trimmed === 'null') return 'null';
  if (/^".*"$/.test(trimmed)) return trimmed;
  return trimmed;
}

/**
 * Scan a Plugin class for extra parameters not in the Config record.
 * Looks for pluginConfig.getString/getBoolean calls outside the Config record.
 */
function parseExtraParams(source, knownParamNames) {
  if (!source) return [];
  const extras = [];
  const patterns = [
    { re: /pluginConfig\.getString\(\s*"(\w+)"\s*\)\.orElse\(\s*(.+?)\s*\)/g, type: 'String' },
    { re: /pluginConfig\.getBoolean\(\s*"(\w+)"\s*,\s*(true|false)\s*\)/g, type: 'boolean' },
    { re: /pluginConfig\.getString\(\s*"(\w+)"\s*,\s*"([^"]*)"\s*\)/g, type: 'String' },
    { re: /pluginConfig\.getInteger\(\s*"(\w+)"\s*,\s*(\d+)\s*\)/g, type: 'int' },
    // Also match config. (without plugin prefix) for audit case
    { re: /config\.getBoolean\(\s*"(\w+)"\s*,\s*(true|false)\s*\)/g, type: 'boolean' },
    { re: /config\.getString\(\s*"(\w+)"\s*\)\.orElse\(\s*(.+?)\s*\)/g, type: 'String' },
  ];

  for (const { re, type } of patterns) {
    let m;
    while ((m = re.exec(source)) !== null) {
      const name = m[1];
      if (knownParamNames.has(name)) continue;
      if (extras.some(e => e.name === name)) continue;
      extras.push({
        name,
        type,
        defaultValue: formatDefault(m[2]),
        description: ''
      });
    }
  }
  return extras;
}

// ---------------------------------------------------------------------------
// Plugin config extraction
// ---------------------------------------------------------------------------

const PLUGINS = [
  {
    id: 'jpa',
    configFile: 'hexaglue-plugins/hexaglue-plugin-jpa/src/main/java/io/hexaglue/plugin/jpa/JpaConfig.java',
    configRecord: 'JpaConfig',
    pluginClass: 'hexaglue-plugins/hexaglue-plugin-jpa/src/main/java/io/hexaglue/plugin/jpa/JpaPlugin.java',
    pluginId: 'io.hexaglue.plugin.jpa'
  },
  {
    id: 'rest',
    configFile: 'hexaglue-plugins/hexaglue-plugin-rest/src/main/java/io/hexaglue/plugin/rest/RestConfig.java',
    configRecord: 'RestConfig',
    pluginClass: null,
    pluginId: 'io.hexaglue.plugin.rest'
  },
  {
    id: 'living-doc',
    configFile: 'hexaglue-plugins/hexaglue-plugin-living-doc/src/main/java/io/hexaglue/plugin/livingdoc/config/LivingDocConfig.java',
    configRecord: 'LivingDocConfig',
    pluginClass: null,
    pluginId: 'io.hexaglue.plugin.livingdoc'
  },
  {
    id: 'audit',
    configFile: 'hexaglue-plugins/hexaglue-plugin-audit/src/main/java/io/hexaglue/plugin/audit/config/AuditConfiguration.java',
    configRecord: 'AuditConfiguration',
    pluginClass: 'hexaglue-plugins/hexaglue-plugin-audit/src/main/java/io/hexaglue/plugin/audit/DddAuditPlugin.java',
    pluginId: 'io.hexaglue.plugin.audit'
  }
];

function extractPluginConfigs() {
  const results = {};

  for (const plugin of PLUGINS) {
    console.log(`Extracting config for plugin: ${plugin.id}`);

    const configSource = readJavaFile(plugin.configFile);
    const parameters = parseConfigRecord(configSource, plugin.configRecord);
    const paramNames = new Set(parameters.map(p => p.name));

    let extraParameters = [];
    if (plugin.pluginClass) {
      const pluginSource = readJavaFile(plugin.pluginClass);
      extraParameters = parseExtraParams(pluginSource, paramNames);
    }

    console.log(`  ${parameters.length} config params, ${extraParameters.length} extra params`);

    results[plugin.id] = {
      pluginId: plugin.pluginId,
      configClass: plugin.configRecord,
      configFile: plugin.configFile,
      parameters,
      extraParameters
    };
  }

  return results;
}

// ---------------------------------------------------------------------------
// Maven goals extraction from plugin.xml
// ---------------------------------------------------------------------------

const INTERNAL_PARAM_TYPES = [
  'org.apache.maven.project.MavenProject',
  'org.apache.maven.execution.MavenSession'
];

function extractMavenGoals() {
  const pluginXmlPath = join(ROOT,
    'hexaglue-maven-plugin/target/classes/META-INF/maven/plugin.xml');

  if (!existsSync(pluginXmlPath)) {
    console.error('ERROR: plugin.xml not found. Run "mvn compile" first.');
    process.exit(1);
  }

  const xml = readFileSync(pluginXmlPath, 'utf-8');
  const mojoBlocks = xmlAll(xml, 'mojo');
  const goals = [];

  for (const mojoXml of mojoBlocks) {
    const goal = xmlText(mojoXml, 'goal');
    const phase = xmlText(mojoXml, 'phase');
    const implementation = xmlText(mojoXml, 'implementation');
    const aggregator = xmlText(mojoXml, 'aggregator') === 'true';
    const threadSafe = xmlText(mojoXml, 'threadSafe') === 'true';
    const description = cleanDescription(xmlText(mojoXml, 'description'));

    // Extract <since> at the mojo level only (not nested inside <parameters>)
    const mojoWithoutParams = mojoXml.replace(/<parameters>[\s\S]*?<\/parameters>/, '');
    const since = xmlText(mojoWithoutParams, 'since') || null;

    // Extract parameters
    const paramBlocks = xmlAll(mojoXml, 'parameter');
    const parameters = [];

    // Also extract default values from <configuration> block
    const configBlock = mojoXml.match(/<configuration>([\s\S]*?)<\/configuration>/);
    const defaults = {};
    if (configBlock) {
      const defaultRe = /<(\w+)\s[^>]*default-value="([^"]*)"[^>]*>/g;
      let dm;
      while ((dm = defaultRe.exec(configBlock[1])) !== null) {
        defaults[dm[1]] = dm[2];
      }
      // Also extract property references
      const propRe = /<(\w+)\s[^>]*>(\$\{[^}]+\})<\/\1>/g;
      while ((dm = propRe.exec(configBlock[1])) !== null) {
        if (!defaults[dm[1] + '_property']) {
          defaults[dm[1] + '_property'] = dm[2];
        }
      }
    }

    for (const paramXml of paramBlocks) {
      const pName = xmlText(paramXml, 'name');
      const pType = xmlText(paramXml, 'type');
      const pRequired = xmlText(paramXml, 'required') === 'true';
      const pEditable = xmlText(paramXml, 'editable') === 'true';
      const pSince = xmlText(paramXml, 'since') || null;
      const pDesc = cleanDescription(xmlText(paramXml, 'description'));

      // Skip internal Maven params
      if (INTERNAL_PARAM_TYPES.includes(pType)) continue;
      // Skip non-editable params
      if (!pEditable) continue;

      parameters.push({
        name: pName,
        type: simplifyType(pType),
        required: pRequired,
        defaultValue: defaults[pName] || null,
        property: defaults[pName + '_property'] || null,
        description: pDesc,
        since: pSince
      });
    }

    goals.push({
      name: goal,
      phase,
      mojoClass: implementation,
      aggregator,
      threadSafe,
      since,
      description,
      parameters
    });
  }

  return { goals };
}

function simplifyType(javaType) {
  if (!javaType) return null;
  const map = {
    'java.lang.String': 'String',
    'java.lang.Boolean': 'Boolean',
    'java.io.File': 'File',
    'boolean': 'boolean',
    'int': 'int'
  };
  return map[javaType] || javaType;
}

function cleanDescription(desc) {
  if (!desc) return '';
  return desc
    .replace(/<[^>]+>/g, '')       // strip XML/HTML tags
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&amp;/g, '&')
    .replace(/\$\{[^}]+}/g, '')    // strip Maven expressions
    .replace(/\s+/g, ' ')          // normalize whitespace
    .trim()
    .split('. ')
    .slice(0, 2)                   // keep first 2 sentences max
    .join('. ')
    .replace(/\.\s*$/, '');
}

// ---------------------------------------------------------------------------
// YAML sections extraction from MojoConfigLoader
// ---------------------------------------------------------------------------

function extractYamlSections() {
  const source = readJavaFile(
    'hexaglue-maven-plugin/src/main/java/io/hexaglue/maven/MojoConfigLoader.java');
  if (!source) return { sections: [] };

  const sections = [];

  // classification section
  const classKeys = [];
  const classExcludeMatch = source.match(/classification.*?exclude/s);
  if (classExcludeMatch || source.includes('"classification"')) {
    classKeys.push(
      { name: 'exclude', type: 'List<String>', description: 'Glob patterns for types to exclude from classification' },
      { name: 'explicit', type: 'Map<String, String>', description: 'Explicit type classifications (qualified name to ElementKind)' },
      { name: 'validation.failOnUnclassified', type: 'boolean', description: 'Fail build if unclassified types remain' },
      { name: 'validation.allowInferred', type: 'boolean', description: 'Allow inferred classifications' }
    );
    sections.push({ name: 'classification', keys: classKeys });
  }

  // plugins section
  if (source.includes('"plugins"') || source.includes('loadPluginConfigs')) {
    sections.push({
      name: 'plugins',
      description: 'Per-plugin configuration maps, keyed by plugin fully-qualified ID',
      keys: [
        { name: '<pluginId>', type: 'Map<String, Object>', description: 'Plugin-specific configuration (see each plugin doc)' }
      ]
    });
  }

  // modules section
  if (source.includes('"modules"') || source.includes('loadModuleConfigs')) {
    sections.push({
      name: 'modules',
      description: 'Module role assignments for multi-module projects',
      keys: [
        { name: '<moduleName>', type: 'ModuleRole', description: 'DOMAIN, APPLICATION, INFRASTRUCTURE, or SHARED' }
      ]
    });
  }

  // output section
  if (source.includes('"output"') || source.includes('loadOutputConfig') || source.includes('OutputConfig')) {
    sections.push({
      name: 'output',
      keys: [
        { name: 'sources', type: 'String', defaultValue: 'target/generated-sources/hexaglue', description: 'Base directory for generated sources' },
        { name: 'reports', type: 'String', defaultValue: 'target/hexaglue/reports', description: 'Base directory for reports' }
      ]
    });
  }

  return { sections };
}

// ---------------------------------------------------------------------------
// Main
// ---------------------------------------------------------------------------

function main() {
  console.log('=== HexaGlue Doc Metadata Extraction ===\n');

  mkdirSync(OUTPUT_DIR, { recursive: true });

  // 1. Plugin configs
  const pluginConfigs = extractPluginConfigs();
  for (const [id, data] of Object.entries(pluginConfigs)) {
    const outPath = join(OUTPUT_DIR, `plugin-${id}.json`);
    writeFileSync(outPath, JSON.stringify(data, null, 2) + '\n');
    console.log(`  -> ${outPath}`);
  }

  // 2. Maven goals
  console.log('\nExtracting Maven goals...');
  const mavenGoals = extractMavenGoals();
  const goalsPath = join(OUTPUT_DIR, 'maven-goals.json');
  writeFileSync(goalsPath, JSON.stringify(mavenGoals, null, 2) + '\n');
  console.log(`  -> ${goalsPath} (${mavenGoals.goals.length} goals)`);

  // 3. YAML sections
  console.log('\nExtracting YAML sections...');
  const yamlSections = extractYamlSections();
  const yamlPath = join(OUTPUT_DIR, 'yaml-sections.json');
  writeFileSync(yamlPath, JSON.stringify(yamlSections, null, 2) + '\n');
  console.log(`  -> ${yamlPath} (${yamlSections.sections.length} sections)`);

  console.log('\nDone.');
}

main();
