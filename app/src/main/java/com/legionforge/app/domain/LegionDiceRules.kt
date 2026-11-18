package com.legionforge.app.domain

/**
 * Faces des dés Star Wars Legion — SOURCE PRIMAIRE : FFG/AMG + wiki StarWarsLegion.
 * Le rulebook PDF (DOC56) ne chiffre PAS les faces (pictogrammes perdus dans l'extraction
 * texte, l.310-344) : la répartition vérifiée vient des dés physiques et de la doc officielle.
 *
 * ATTENTION : attaque = d8, DÉFENSE = d6 (le skill expert disait d8 à tort).
 * - Attaque rouge  : 5 Hit, 1 Crit, 1 Surge, 1 Blank  → 6/8 = 75 % de succès
 * - Attaque noire  : 3 Hit, 1 Crit, 1 Surge, 3 Blank  → 4/8 = 50 %
 * - Attaque blanche: 1 Hit, 1 Crit, 1 Surge, 5 Blank  → 2/8 = 25 %
 * - Défense rouge  : 3 Block, 1 Surge, 2 Blank (d6)   → 3/6 = 50 %
 * - Défense blanche: 1 Block, 1 Surge, 4 Blank (d6)   → 1/6 ≈ 16,7 %
 */
object LegionDiceRules {
    /** Couleurs légales par mode. Le dé NOIR n'existe PAS en défense. */
    fun legalColors(mode: String): List<String> =
        if (mode == "defense") listOf("red", "white") else listOf("red", "black", "white")

    /** Faces d'un dé pour un mode et une couleur donnés. */
    fun faces(mode: String, color: String): List<String> = when {
        mode == "defense" && color == "red" ->
            listOf("Bloc", "Bloc", "Bloc", "Surge", "Vide", "Vide")
        mode == "defense" -> // blanc
            listOf("Bloc", "Surge", "Vide", "Vide", "Vide", "Vide")
        color == "red" ->
            listOf("Hit", "Hit", "Hit", "Hit", "Hit", "Crit", "Surge", "Vide")
        color == "black" ->
            listOf("Hit", "Hit", "Hit", "Crit", "Surge", "Vide", "Vide", "Vide")
        else -> // blanc attaque
            listOf("Hit", "Crit", "Surge", "Vide", "Vide", "Vide", "Vide", "Vide")
    }

    /** Nombre de faces du dé (8 attaque, 6 défense). */
    fun sides(mode: String): Int = if (mode == "defense") 6 else 8

    /** Succès = Hit ou Crit en attaque, Bloc en défense. */
    fun isSuccess(mode: String, face: String): Boolean =
        if (mode == "defense") face == "Bloc" else face == "Hit" || face == "Crit"
}
