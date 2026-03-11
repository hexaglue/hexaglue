# Releasing HexaGlue

Ce document décrit le processus complet de release, de la mise à jour des versions à la publication de la documentation.

## Versioning

HexaGlue utilise un versioning à deux niveaux :

| Composant | GroupId | Version actuelle | Exemple |
|-----------|---------|------------------|---------|
| Core + Maven plugin | `io.hexaglue` | 6.1.1 | `hexaglue-parent`, `hexaglue-core`, `hexaglue-maven-plugin` |
| Plugins | `io.hexaglue.plugins` | 3.1.1 | `hexaglue-plugin-jpa`, `hexaglue-plugin-rest`, etc. |

Les plugins ont leur propre cycle de version, indépendant du core.

## Scripts d'automatisation

Deux scripts automatisent la mise à jour des versions dans l'ensemble des fichiers :

| Script | Périmètre | Fichiers |
|--------|-----------|----------|
| `scripts/bump-versions.js` | Repo `hexaglue/` | POMs, exemples, docs Markdown, CHANGELOG, README plugins |
| `scripts/bump-site-versions.js` | Repo `hexaglue-site/` | `versions.ts`, getting-started, case studies |

### bump-versions.js

```bash
# Dry-run : affiche les changements prévus sans modifier les fichiers
node scripts/bump-versions.js --core 6.2.0 --plugins 3.2.0 --dry-run

# Release : met à jour POMs, exemples, et documentation
node scripts/bump-versions.js --core 6.2.0 --plugins 3.2.0 --yes

# Post-release SNAPSHOT : uniquement POMs et exemples, pas la doc
node scripts/bump-versions.js --core 6.3.0-SNAPSHOT --plugins 3.3.0-SNAPSHOT --snapshot --yes

# Via Make
make bump-versions ARGS="--core 6.2.0 --plugins 3.2.0 --dry-run"
```

| Flag | Role |
|------|------|
| `--core <ver>` | Nouvelle version core |
| `--plugins <ver>` | Nouvelle version plugins |
| `--snapshot` | Mode post-release : MAJ POMs + exemples uniquement, pas les docs |
| `--yes` | Pas de confirmation interactive |
| `--dry-run` | Affiche les changements sans écrire |

Le script effectue :
1. `mvn versions:set` pour le core et les plugins
2. MAJ des propriétés manuelles (`hexaglue.version`, `hexaglue-build-tools.version`, BOM)
3. MAJ des 57 POMs d'exemples (remplacement context-aware près des `<artifactId>` HexaGlue)
4. MAJ de la documentation Markdown (sauf en mode `--snapshot`)

### bump-site-versions.js

```bash
# Dry-run
node scripts/bump-site-versions.js --core 6.2.0 --plugins 3.2.0 --dry-run

# Avec copie des JSON de métadonnées
node scripts/bump-site-versions.js --core 6.2.0 --plugins 3.2.0 --copy-metadata --yes

# Via Make
make bump-site-versions ARGS="--core 6.2.0 --plugins 3.2.0 --copy-metadata --yes"
```

| Flag | Role |
|------|------|
| `--core <ver>` | Nouvelle version core |
| `--plugins <ver>` | Nouvelle version plugins |
| `--site-dir <path>` | Chemin vers hexaglue-site (défaut : `../hexaglue-site`) |
| `--copy-metadata` | Copie les JSON générés vers le site |
| `--yes` | Pas de confirmation interactive |
| `--dry-run` | Affiche les changements sans écrire |

Les pages d'archive (`*/2.0.0.astro`, `*/2.1.0.astro`, `*/3.0.0.astro`) ne sont jamais modifiées.

## Processus de release

### 1. Mettre à jour les versions

```bash
# Dry-run pour vérifier
make bump-versions ARGS="--core 6.2.0 --plugins 3.2.0 --dry-run"

# Appliquer
make bump-versions ARGS="--core 6.2.0 --plugins 3.2.0 --yes"
```

### 2. Mettre à jour la documentation générée

```bash
make compile
make doc-metadata
make doc-readmes
make doc-check
```

### 3. Mettre à jour le site

```bash
make bump-site-versions ARGS="--core 6.2.0 --plugins 3.2.0 --copy-metadata --yes"
```

Si de nouveaux paramètres ont été ajoutés côté Java, ajouter leur traduction dans le fichier `descriptions-*.ts` correspondant :

- `src/data/generated/descriptions-jpa.ts`
- `src/data/generated/descriptions-rest.ts`
- `src/data/generated/descriptions-living-doc.ts`
- `src/data/generated/descriptions-audit.ts`

Sans traduction, la description anglaise du Javadoc est utilisée en fallback.

```bash
cd ../hexaglue-site && pnpm build
```

### 4. Release Maven

```bash
make release-check    # Dry-run
make release          # Déployer sur Maven Central
```

### 5. Post-release : passer en SNAPSHOT

```bash
make bump-versions ARGS="--core 6.3.0-SNAPSHOT --plugins 3.3.0-SNAPSHOT --snapshot --yes"
```

## Checklist rapide

- [ ] `make bump-versions ARGS="--core X.Y.Z --plugins A.B.C --yes"`
- [ ] `make compile && make doc-metadata && make doc-readmes`
- [ ] `make doc-check` passe
- [ ] `make bump-site-versions ARGS="--core X.Y.Z --plugins A.B.C --copy-metadata --yes"`
- [ ] Descriptions françaises ajoutées pour les nouveaux paramètres
- [ ] `cd ../hexaglue-site && pnpm build` passe
- [ ] `make release`
- [ ] `make bump-versions ARGS="--core X'.Y'.Z'-SNAPSHOT --plugins A'.B'.C'-SNAPSHOT --snapshot --yes"`
