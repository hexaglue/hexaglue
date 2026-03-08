# Releasing HexaGlue

Ce document decrit le processus complet de release, de la mise a jour des versions a la publication de la documentation.

## Versioning

HexaGlue utilise un versioning a deux niveaux :

| Composant | GroupId | Version actuelle | Exemple |
|-----------|---------|------------------|---------|
| Core + Maven plugin | `io.hexaglue` | 6.1.0 | `hexaglue-parent`, `hexaglue-core`, `hexaglue-maven-plugin` |
| Plugins | `io.hexaglue.plugins` | 3.1.0 | `hexaglue-plugin-jpa`, `hexaglue-plugin-rest`, etc. |

Les plugins ont leur propre cycle de version, independant du core.

## 1. Mettre a jour les versions Maven

### Core (tous les modules sauf plugins)

```bash
# Dans hexaglue/ (racine)
mvn versions:set -DnewVersion=6.2.0 -DgenerateBackupPoms=false
```

Cela met a jour `pom.xml` (root) et tous les modules enfants qui heritent du parent.

Mettre a jour manuellement la propriete `hexaglue.version` dans le POM plugins :

**`hexaglue-plugins/pom.xml`** :
```xml
<hexaglue.version>6.2.0</hexaglue.version>
```

### Plugins

```bash
# Dans hexaglue-plugins/
cd hexaglue-plugins
mvn versions:set -DnewVersion=3.2.0 -DgenerateBackupPoms=false
```

Mettre a jour le BOM dans `hexaglue-plugins/hexaglue-plugins-bom/pom.xml` si les versions individuelles y sont declarees.

### Verification

```bash
# Verifier qu'aucun SNAPSHOT ne traine
grep -r "SNAPSHOT" --include="pom.xml" .
```

## 2. Mettre a jour la documentation

### Extraire les metadonnees depuis le code

```bash
make compile
make doc-metadata
```

Cela regenere les JSON dans `docs/generated-metadata/` a partir du code source et de `plugin.xml`.

### Mettre a jour les READMEs des plugins

```bash
make doc-readmes
```

Met a jour les tables de configuration entre les markers `<!-- GENERATED:CONFIG:START/END -->` et `<!-- GENERATED:MAVEN:START/END -->` dans les 4 READMEs plugins.

### Verifier la coherence

```bash
make doc-check
```

Echoue si les JSON commites ou les sections README ne correspondent pas au code source.

## 3. Mettre a jour le site (`hexaglue-site/`)

### Versions affichees

**`hexaglue-site/src/data/versions.ts`** :
```typescript
export const versions = {
    hexaglue: '6.2.0',    // ← nouvelle version core
    plugins: '3.2.0',     // ← nouvelle version plugins
    groupId: 'io.hexaglue',
    pluginsGroupId: 'io.hexaglue.plugins',
} as const;
```

### Copier les JSON de metadonnees

```bash
cp docs/generated-metadata/plugin-*.json  ../hexaglue-site/src/data/generated/
cp docs/generated-metadata/maven-goals.json ../hexaglue-site/src/data/generated/
```

### Descriptions francaises

Si de nouveaux parametres ont ete ajoutes cote Java, ajouter leur traduction dans le fichier `descriptions-*.ts` correspondant :

- `src/data/generated/descriptions-jpa.ts`
- `src/data/generated/descriptions-rest.ts`
- `src/data/generated/descriptions-living-doc.ts`
- `src/data/generated/descriptions-audit.ts`

Sans traduction, la description anglaise du Javadoc est utilisee en fallback.

### Verifier le build du site

```bash
cd ../hexaglue-site
pnpm build
```

## 4. Release Maven

```bash
# Dry-run (build sans deployer)
make release-check

# Deployer sur Maven Central
make release
```

## 5. Post-release : passer en SNAPSHOT

Remonter les versions en SNAPSHOT pour le developpement :

```bash
# Core
mvn versions:set -DnewVersion=6.3.0-SNAPSHOT -DgenerateBackupPoms=false

# Plugins
cd hexaglue-plugins
mvn versions:set -DnewVersion=3.3.0-SNAPSHOT -DgenerateBackupPoms=false

# hexaglue-plugins/pom.xml : mettre a jour hexaglue.version
# <hexaglue.version>6.3.0-SNAPSHOT</hexaglue.version>
```

## Checklist rapide

- [ ] Versions Maven mises a jour (core + plugins + `hexaglue.version` dans plugins/pom.xml)
- [ ] `make compile && make doc-metadata && make doc-readmes`
- [ ] `make doc-check` passe
- [ ] `hexaglue-site/src/data/versions.ts` mis a jour
- [ ] JSON copies vers `hexaglue-site/src/data/generated/`
- [ ] Descriptions francaises ajoutees pour les nouveaux parametres
- [ ] `pnpm build` passe dans hexaglue-site
- [ ] `make release`
- [ ] Versions SNAPSHOT pour le prochain cycle
