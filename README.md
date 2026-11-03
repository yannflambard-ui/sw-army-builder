# SW Army Builder — SW Legion v2 + Armada v1.5

App Android (Kotlin + Jetpack Compose + Room) pour construire des listes
d'armée Star Wars Legion v2. Voir specs complètes dans le skill
`software-development/swlegion-army-builder-app` (Hermes).

## État actuel (P1 — MVP)

Fait et vérifié (build + tests réels) :
- Structure Gradle Kotlin DSL + wrapper (Gradle 8.7, AGP 8.5.2, Kotlin 1.9.24)
- Room DB : entités Faction, UnitEntity, UpgradeSlot, Keyword, ArmyList, ArmyUnit, OwnedUnit
- Seed JSON (assets/game_data_core.json) : factions Rebel + Empire, ~13 unités core
- Repositories (GameDataRepository, ArmyListRepository) offline-first
- Validation de liste (ArmyListValidator) : points, rangs Corps min, Commandants max
  → 4 tests unitaires JUnit, tous passants
- UI Compose : Home, FactionPicker, ArmyBuilder (catalogue + liste + validation live)
- Navigation Compose (NavHost) entre les 3 écrans
- Thème sombre custom (fond #0D0D1A, accent orange #FFB800)
- `./gradlew assembleDebug` → BUILD SUCCESSFUL, APK généré
- `./gradlew testDebugUnitTest` → 4/4 tests OK

APK debug : `app/build/outputs/apk/debug/app-debug.apk`

## Pas fait (hors scope de cette session, nécessite compte/service payant ou device)

- Icônes/illustrations réelles (génération IA) — actuellement placeholder noir
- Test sur device/émulateur réel (pas d'émulateur installé, disque limité ~5.7 Go libres)
- Firebase / Supabase / RevenueCat (comptes externes, potentiellement payants)
- IAP, cloud sync, mode partie (wounds/dice roller) — Phase 2/3 des specs
- Export texte/partage de liste — prévu P1 mais pas encore implémenté
- Upgrades (slots) — modèle de données prêt (UpgradeSlot), pas encore branché à l'UI

## Build

```bash
export ANDROID_HOME=/opt/android-sdk
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
cd /opt/projects/swlegion-army-builder
./gradlew assembleDebug      # build l'APK debug
./gradlew testDebugUnitTest  # lance les tests unitaires
```

## Environnement de build sur cette VM

- JDK 17 : `openjdk-17-jdk-headless` (apt)
- Android SDK minimal : `/opt/android-sdk` (cmdline-tools, platform-tools,
  platforms;android-34, build-tools;34.0.0) — ~460 Mo, pas d'émulateur
  (system-images) installé car disque trop limité (38 Go total, ~5.7 Go
  restants après installation SDK)
- Gradle 8.7 standalone : `/opt/gradle/gradle-8.7`
- Pas de compte Google Play, Firebase, RevenueCat configuré (nécessiterait
  des identifiants/paiements externes non fournis)

## Prochaines étapes suggérées

1. Export texte / partage de liste (simple, pas de dépendance externe)
2. Brancher les UpgradeSlot à l'écran ArmyBuilder
3. Générer les illustrations IA des ~13 unités core (via l'outil image_generate
   d'Hermes, gratuit via l'abonnement Nous) et les intégrer en assets locaux
4. Tester sur un vrai device Android (adb installé, `adb install app-debug.apk`)
5. Catalogue complet v2 (autres factions/extensions) — Phase 2
