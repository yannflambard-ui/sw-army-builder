# Process de build — SW Army Builder

## Règle AAB (format Play Store)

L'AAB signé n'est produit **que sur demande explicite**, jamais automatiquement.

Pour le générer :
1. GitHub → Actions → workflow **Build APK** → *Run workflow*
2. Cocher **« Générer aussi l'AAB signé (format Play Store) »**
3. L'artefact `legion-forge-release-aab` apparaît dans le run

Le keystore utilisé est `LEGIONFORGE_KEYSTORE` (déjà présent dans les secrets du
repo) — le même que celui de l'APK diffusé, indispensable pour les mises à jour.

Les pushes et pull requests normaux ne génèrent **que l'APK debug**, les tests et
les snapshots Roborazzi. Le départ vers le store reste une décision manuelle.

## Versions

Toute modification de code implique un bump `versionCode` + `versionName` dans
`app/build.gradle.kts`, puis un push sur `master`. La CI doit être verte avant
toute mise à jour du Drive.
