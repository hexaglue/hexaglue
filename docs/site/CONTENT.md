# HexaGlue

--- Hero ---

## Compilez l’architecture, pas seulement le code.

### Votre Architecture Java Gouvernée à la Compilation
Sans framework imposé. Sans lock-in éditeur. Sans impact sur le runtime.

HexaGlue rend le DDD, l’architecture hexagonale et la Clean Architecture vérifiables et automatisables à la compilation.

--- HexaGlue, c’est quoi ? ---

## A quoi sert HexaGlue

HexaGlue est un moteur de génération de code à la compilation, conçu pour les applications Java en architecture hexagonale.

### Classification
HexaGlue analyse le modèle de domaine à la compilation et identifie automatiquement les concepts clés : agrégats, entités, objets valeur et événements de domaine.

### Validation
Les règles d’architecture sont appliquées comme des contraintes de build, avec détection des écarts dès la compilation.

### Génération
HexaGlue génère automatiquement les adaptateurs d’infrastructure, entités JPA, repositories et contrôleurs REST, à partir du code métier.

--- Principe ---

## Un Principe : Zéro Supposition

Toute ambiguïté architecturale est explicitement identifiée et rendue visible pour être traitée.

--- Le problème ---

## Le Coût Caché des Architectures Implicites

### Une prolifération de code d’intégration  
Adapters et câblage répétés dans chaque projet génèrent un surcoût structurel
qui ralentit durablement le delivery.

### Une documentation non synchronisée  
La documentation n’est pas alignée avec le code réel, ce qui la rend obsolète
et peu fiable pour l’onboarding et la gouvernance.

### Des modifications à haut risque  
Dépendances implicites et règles non vérifiées rendent chaque évolution
imprévisible et coûteuse.

### Des revues de code à faible valeur  
L’essentiel de l’effort porte sur le câblage et l’intégration,
au détriment des règles métier et des décisions architecturales.

--- La solution --

## Quand l’Architecture Devient une Donnée Exploitable

### Modélisation  
Le code est transformé en un modèle architectural exploitable. L’architecture cachée dans le code devient visible.

### Classification  
Les concepts architecturaux sont détectés de manière fiable et traçable. Votre architecture devient lisible et explicable.

### Validation  
Les règles architecturales sont vérifiées automatiquement. Leur conformité peut enfin être contrôlée en continue (CI/CD).

### Génération  
L’architecture devient un levier d’automatisation et alimente des usages concrets. Production de documentation, d’audits et de code d'infrastructure alignés sur l'architecture réelle.

--- Pour qui ? ---

## Conçu pour les DSI, Architectes et Tech Leads

### DSI / Transformation  
Une vision fiable des domaines, des dépendances et des zones à risque
du cœur applicatif, pour orienter les décisions de transformation.

### Architectes  
Les règles d’architecture sont formalisées et vérifiées automatiquement,
afin de gouverner les dépendances, les frontières et les responsabilités.

### Tech Leads  
Moins de code répétitif, plus de logique métier.  
La génération respecte l’intention et signale les ambiguïtés.

--- Tout ce dont vous avez besoin ---

## Une Couverture Fonctionnelle Complète

HexaGlue transforme le code métier en infrastructure prête pour la production.

### Classification du Domaine
Identification automatique des blocs de construction DDD : Aggregates, Entities, Value Objects, Domain Events et Identifiers.

### Génération JPA
Génère automatiquement les entités JPA, les repositories et le code de mapping à partir du modèle du Domaine.

### Living Documentation
Génère automatiquement une documentation qui est synchronisée avec votre code.

### Audit d'Architecture
Valide votre architecture hexagonale et DDD à la compilation.

### Validation au Build-time
Capture les violations d'architecture avant les déploiements avec l'intégration d'un plugin Maven.

### Clean Architecture
Supporte les patterns de la Clean architecture avec séparations des préoccupations (separation of concerns).

### Détection des Ports et Adapteurs
Identifie automatiquement les ports driving et driven de votre architecure hexagonale.

### Plugin Maven
Intégration simple à l'aide d'un plugin Maven pour générer, valider et auditer votre application.

### Architecture Extensible
Son système de plugin permet de construire des générateurs et des validations pour vos besoins spécifiques.

---

## Ce que Produit HexaGlue

### Compile-time  
Feedback architectural en CI : l’architecture devient un signal,
pas un document statique.

### Déterministe  
Classification traçable : ce qui est ambigu est signalé,
jamais supposé.

### At Scale 
Standardisation des patterns et réduction de la dette d’intégration
à l’échelle de l'entreprise.

--- Pour quoi faire ? ---

## Pourquoi HexaGlue

Pour piloter la transformation et fiabiliser l’architecture, à la compilation.

Sans contrainte de runtime. Sans framework imposé. Sans lock-in éditeur.

--- Adoption ---

## Une Adoption Progressive, Sans Risque

Commencez par observer, puis verouiller, puis automatiser.

1. Observer
Contruisez le modèle, obtenir la classification, générez la living documentation.

2. Gouverner
Activez l'audit et la validation en CI : les invariants deviennent des contrôles.

3. Accélérer
Branchez la génération d'infrastructure : standardisez et industrialisez.

--- Passer à l’Action ---

## Prêt à Compiler votre Architecture ?

- Démarrer en quelques minutes avec le Quick Start  
- Explorer la documentation et les outputs générés  
- Contribuer ou suivre le projet sur GitHub  

HexaGlue ne documente pas l’architecture.  
Il la rend **observable, vérifiable et actionnable**.


--- Boutons d'Action ---

- Démarrer maintenant
- Lire la documentation
- Liens vers les exemples (vers les applications dédiées sous examples/)

