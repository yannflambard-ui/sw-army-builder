package com.legionforge.app.ui.screens

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.legionforge.app.ui.theme.LegionColors
import com.legionforge.app.ui.theme.LegionType

/**
 * Cartes de mission du bandeau supérieur (hors plateau), inspirées d'un playmat
 * de type IRON SQUADRON : 5 emplacements, la carte centrale en paysage.
 * Données pures + rendu Composable séparés du moteur de jeu.
 */
enum class MissionCardKind { COMMAND, PRIMARY, MAP, SECONDARY, COMMAND2 }

data class MissionCardSlot(val kind: MissionCardKind, val label: String, val wide: Boolean = false)

object MissionCardRailData {
    // Ordre et libellés (i18n appliquée à l'affichage via map de label).
    val slots = listOf(
        MissionCardSlot(MissionCardKind.COMMAND, "command"),
        MissionCardSlot(MissionCardKind.PRIMARY, "primary"),
        MissionCardSlot(MissionCardKind.MAP, "map", wide = true),
        MissionCardSlot(MissionCardKind.SECONDARY, "secondary"),
        MissionCardSlot(MissionCardKind.COMMAND2, "command"),
    )
    fun colors(kind: MissionCardKind): Pair<Color, Color> = when (kind) {
        MissionCardKind.COMMAND, MissionCardKind.COMMAND2 -> Color(0xFF1A1A1A) to Color(0xFF2E2E2E)
        MissionCardKind.PRIMARY -> Color(0xFF4A0A0A) to Color(0xFF8B1414)
        MissionCardKind.MAP -> Color(0xFF0B1229) to Color(0xFF1D3A7A)
        MissionCardKind.SECONDARY -> Color(0xFF5C3212) to Color(0xFFC27A2C)
    }
}

/** Type d'élément de paysage déposable. */
enum class TerrainKind {
    TEMPLE, ROTUNDA, TOWER, MARKET, WALL, CRATE, TREE, STATUE, FOUNTAIN,
    // Bâtiments vectoriels (cartes planète) — DOC62 / rulebook personal-use inspiration.
    ICE_BASE, GENERATOR, LAVA_RIG, PYLON, SKYSCRAPER, TREE_PLATFORM,
    BUNKER, TOWER_SPIRE, HANGAR, MOISTURE_VAPORIZER,
}

/** Taille DOC62 : 2 LARGE / 6 MEDIUM / 8 SMALL / 16-24 FILL_IN par battlefield. */
enum class TerrainSize { LARGE, MEDIUM, SMALL, FILL_IN }

/** Surface rulebook:731-842. */
enum class TerrainSurface { OPEN, DIFFICULT, IMPASSABLE }

/** Palette 3 teintes d'une carte (ombre / clair / accent). */
data class TerrainPalette(val base: Color, val shade: Color, val accent: Color)

/** Un élément de terrain, positionné en mm par rapport au centre du tapis (0,0). */
data class TerrainPiece(
    val id: String,
    val kind: TerrainKind,
    val xMm: Float,
    val yMm: Float,
    val wMm: Float,
    val hMm: Float,
    val cover: Int, // VALEUR PROPOSEE a valider en pre-game (rulebook:2256-2261), pas une regle
    val blocksLos: Boolean = false,
    val size: TerrainSize = TerrainSize.SMALL,
    val half: Char = 'A',          // un layout DOC62 = 2 moities A/B assemblees
    val rotation: Int = 0,         // 0 ou 180
    val surface: TerrainSurface = TerrainSurface.IMPASSABLE,
)

/** Une carte de terrain pré-construite, indépendante des cartes Map du jeu. */
data class TerrainCard(
    val id: String,
    val nameKey: String,
    val pieces: List<TerrainPiece>,
    val palette: TerrainPalette? = null, // null = couleurs historiques par kind
)

/** Les 2 cartes fidèles aux dioramas fournis (vue 2D de dessus, tapis 36"). */
object TerrainCards {
    val MONUMENT = TerrainCard(
        id = "monument", nameKey = "terrain_monument",
        pieces = listOf(
            TerrainPiece("temple", TerrainKind.TEMPLE, -300f, -300f, 150f, 110f, 2, true),
            TerrainPiece("kiosque", TerrainKind.ROTUNDA, -40f, -290f, 70f, 70f, 2, true),
            TerrainPiece("tour", TerrainKind.TOWER, 300f, -230f, 80f, 80f, 2, true),
            TerrainPiece("rotonde", TerrainKind.ROTUNDA, 0f, 0f, 150f, 150f, 2, true), // area terrain = Silhouette bloquante
            TerrainPiece("park", TerrainKind.TEMPLE, -300f, 20f, 110f, 90f, 2, true),
            TerrainPiece("pavillon1", TerrainKind.MARKET, -180f, 280f, 100f, 70f, 1),
            TerrainPiece("pavillon2", TerrainKind.MARKET, 160f, 290f, 90f, 60f, 1),
            TerrainPiece("marche", TerrainKind.MARKET, 300f, 180f, 130f, 90f, 1),
            TerrainPiece("fontaine", TerrainKind.FOUNTAIN, 300f, 60f, 45f, 45f, 0),
            TerrainPiece("statue1", TerrainKind.STATUE, -170f, -130f, 35f, 35f, 0),
            TerrainPiece("statue2", TerrainKind.STATUE, 110f, -130f, 35f, 35f, 0),
            TerrainPiece("arbre1", TerrainKind.TREE, -120f, 150f, 45f, 45f, 1),
            TerrainPiece("arbre2", TerrainKind.TREE, 120f, 160f, 45f, 45f, 1),
            TerrainPiece("caisse1", TerrainKind.CRATE, -60f, 120f, 40f, 40f, 1),
            TerrainPiece("caisse2", TerrainKind.CRATE, 60f, -60f, 40f, 40f, 1),
        ),
    )
    val RUINS = TerrainCard(
        id = "ruins", nameKey = "terrain_ruins",
        pieces = listOf(
            TerrainPiece("tour_centrale", TerrainKind.TOWER, 0f, -40f, 120f, 120f, 2, true),
            TerrainPiece("ruine1", TerrainKind.TEMPLE, -280f, -240f, 160f, 120f, 2, true),
            TerrainPiece("ruine2", TerrainKind.TEMPLE, 280f, -240f, 150f, 110f, 2, true),
            TerrainPiece("ruine3", TerrainKind.TEMPLE, -290f, 230f, 140f, 110f, 2, true),
            TerrainPiece("ruine4", TerrainKind.TEMPLE, 270f, 240f, 160f, 110f, 2, true),
            TerrainPiece("dome1", TerrainKind.ROTUNDA, -130f, 30f, 80f, 80f, 2, true),
            TerrainPiece("dome2", TerrainKind.ROTUNDA, 130f, 40f, 80f, 80f, 2, true),
            TerrainPiece("mur1", TerrainKind.WALL, -60f, 200f, 160f, 25f, 1, true),
            TerrainPiece("mur2", TerrainKind.WALL, 90f, -180f, 25f, 150f, 1, true),
            TerrainPiece("caisse1", TerrainKind.CRATE, -70f, 100f, 40f, 40f, 1),
            TerrainPiece("caisse2", TerrainKind.CRATE, 80f, 120f, 40f, 40f, 1),
            TerrainPiece("caisse3", TerrainKind.CRATE, -100f, -110f, 40f, 40f, 1),
        ),
    )
    // Endor : bunker imperial central, village Ewok en hauteur, rochers, troncs.
    val ENDOR = TerrainCard(
        id = "endor", nameKey = "terrain_endor",
        pieces = listOf(
            TerrainPiece("bunker", TerrainKind.TEMPLE, 0f, 0f, 150f, 120f, 2, true),
            TerrainPiece("antenne", TerrainKind.TOWER, 60f, -100f, 45f, 45f, 2, true),
            TerrainPiece("mur1", TerrainKind.WALL, -80f, -70f, 120f, 25f, 1, true),
            TerrainPiece("mur2", TerrainKind.WALL, -80f, 70f, 120f, 25f, 1, true),
            TerrainPiece("ewok1", TerrainKind.TEMPLE, -320f, -180f, 110f, 90f, 2, true),
            TerrainPiece("ewok2", TerrainKind.TEMPLE, -320f, 60f, 110f, 90f, 2, true),
            TerrainPiece("pont", TerrainKind.WALL, -230f, -60f, 90f, 22f, 1, false),
            TerrainPiece("rocher1", TerrainKind.ROTUNDA, 280f, -200f, 90f, 90f, 2, true),
            TerrainPiece("rocher2", TerrainKind.ROTUNDA, 300f, 180f, 80f, 80f, 2, true),
            TerrainPiece("tronc1", TerrainKind.WALL, 140f, 220f, 130f, 22f, 1, false),
            TerrainPiece("arbre1", TerrainKind.TREE, -140f, 250f, 60f, 60f, 1),
            TerrainPiece("arbre2", TerrainKind.TREE, -60f, 300f, 60f, 60f, 1),
            TerrainPiece("arbre3", TerrainKind.TREE, 200f, 300f, 60f, 60f, 1),
            TerrainPiece("caisse1", TerrainKind.CRATE, 70f, 130f, 40f, 40f, 1),
        ),
    )
    // Tatooine : maisons adobe a dome, marche, epaves, rochers, murets.
    val TATOOINE = TerrainCard(
        id = "tatooine", nameKey = "terrain_tatooine",
        pieces = listOf(
            TerrainPiece("dome1", TerrainKind.ROTUNDA, -260f, -220f, 130f, 130f, 2, true),
            TerrainPiece("dome2", TerrainKind.ROTUNDA, 260f, -220f, 120f, 120f, 2, true),
            TerrainPiece("maison1", TerrainKind.TEMPLE, -260f, 120f, 150f, 110f, 2, true),
            TerrainPiece("maison2", TerrainKind.TEMPLE, 260f, 140f, 140f, 110f, 2, true),
            TerrainPiece("marche1", TerrainKind.MARKET, -80f, -180f, 110f, 70f, 1),
            TerrainPiece("marche2", TerrainKind.MARKET, 80f, -180f, 110f, 70f, 1),
            TerrainPiece("marche3", TerrainKind.MARKET, 0f, -80f, 100f, 70f, 1),
            TerrainPiece("epave", TerrainKind.CRATE, 100f, 60f, 100f, 70f, 1, true),
            TerrainPiece("tour", TerrainKind.TOWER, -100f, 60f, 50f, 50f, 2, true),
            TerrainPiece("mur1", TerrainKind.WALL, -50f, 240f, 140f, 25f, 1, true),
            TerrainPiece("mur2", TerrainKind.WALL, 100f, 240f, 25f, 120f, 1, true),
            TerrainPiece("rocher1", TerrainKind.ROTUNDA, -330f, -40f, 70f, 70f, 2, true),
            TerrainPiece("rocher2", TerrainKind.ROTUNDA, 330f, 0f, 70f, 70f, 2, true),
            TerrainPiece("caisse1", TerrainKind.CRATE, 40f, 180f, 40f, 40f, 1),
        ),
    )
    // Naboo : plaza centrale, arcades, fontaines, jardins, pont, raffinerie.
    val NABOO = TerrainCard(
        id = "naboo", nameKey = "terrain_naboo",
        pieces = listOf(
            TerrainPiece("plaza", TerrainKind.FOUNTAIN, 0f, 0f, 160f, 160f, 0),
            TerrainPiece("fontaine", TerrainKind.FOUNTAIN, 120f, 100f, 55f, 55f, 0), // hors emprise plaza [-80,80]^2 (rulebook:787-794)
            TerrainPiece("arcade1", TerrainKind.TEMPLE, -280f, -180f, 150f, 100f, 2, true),
            TerrainPiece("arcade2", TerrainKind.TEMPLE, 280f, -180f, 150f, 100f, 2, true),
            TerrainPiece("ruines", TerrainKind.TEMPLE, -300f, 180f, 130f, 110f, 2, true),
            TerrainPiece("pont", TerrainKind.WALL, 0f, 300f, 180f, 25f, 1, false),
            TerrainPiece("jardin1", TerrainKind.TREE, -160f, 120f, 70f, 70f, 1),
            TerrainPiece("jardin2", TerrainKind.TREE, 160f, 120f, 70f, 70f, 1),
            TerrainPiece("jardin3", TerrainKind.TREE, -150f, -140f, 60f, 60f, 1),
            TerrainPiece("statue1", TerrainKind.STATUE, -90f, 90f, 35f, 35f, 0),
            TerrainPiece("statue2", TerrainKind.STATUE, 90f, 90f, 35f, 35f, 0),
            TerrainPiece("raffinerie", TerrainKind.ROTUNDA, 300f, 220f, 100f, 100f, 2, true),
            TerrainPiece("caisse1", TerrainKind.CRATE, 220f, 100f, 40f, 40f, 1),
            TerrainPiece("caisse2", TerrainKind.CRATE, -220f, -100f, 40f, 40f, 1),
        ),
    )
    // Geonosis : ruches en spirale, canyons, fabrique separatiste, debris, barricades.
    val GEONOSIS = TerrainCard(
        id = "geonosis", nameKey = "terrain_geonosis",
        pieces = listOf(
            TerrainPiece("ruche1", TerrainKind.ROTUNDA, -180f, -180f, 130f, 130f, 2, true),
            TerrainPiece("ruche2", TerrainKind.ROTUNDA, 200f, 180f, 110f, 110f, 2, true),
            TerrainPiece("monticule", TerrainKind.ROTUNDA, 0f, 0f, 120f, 120f, 2, true),
            TerrainPiece("canyon1", TerrainKind.WALL, -300f, 60f, 60f, 180f, 2, true),
            TerrainPiece("canyon2", TerrainKind.WALL, 300f, -100f, 60f, 170f, 2, true),
            TerrainPiece("fabrique1", TerrainKind.TEMPLE, -100f, 260f, 150f, 100f, 2, true),
            TerrainPiece("fabrique2", TerrainKind.TEMPLE, 120f, -280f, 140f, 100f, 2, true),
            TerrainPiece("barricade", TerrainKind.WALL, 40f, 160f, 130f, 25f, 2, true),
            TerrainPiece("debris1", TerrainKind.CRATE, -60f, 120f, 45f, 45f, 1),
            TerrainPiece("debris2", TerrainKind.CRATE, 90f, 80f, 45f, 45f, 1),
            TerrainPiece("caisse1", TerrainKind.CRATE, -120f, -80f, 40f, 40f, 1),
            TerrainPiece("caisse2", TerrainKind.CRATE, 160f, -60f, 40f, 40f, 1),
            TerrainPiece("tunnel", TerrainKind.TEMPLE, 0f, -320f, 90f, 60f, 2, true),
        ),
    )
    // ── Cartes planète vectorielles (inspiration DOC62 / rulebook, zéro asset externe) ──
    val HOTH = TerrainCard(id = "hoth", nameKey = "terrain_hoth",
        palette = TerrainPalette(Color(0xFFE8EDF2), Color(0xFF7A9CC4), Color(0xFF2F4A66)),
        pieces = listOf(
            TerrainPiece("ice_base", TerrainKind.ICE_BASE, -60f, -40f, 210f, 150f, 2, true, TerrainSize.LARGE, 'A', 0, TerrainSurface.IMPASSABLE),
            TerrainPiece("generator", TerrainKind.GENERATOR, 250f, 120f, 110f, 130f, 2, true, TerrainSize.MEDIUM, 'B'),
            TerrainPiece("trench1", TerrainKind.WALL, -280f, 180f, 170f, 25f, 1, false, TerrainSize.MEDIUM, 'A', 0, TerrainSurface.DIFFICULT),
            TerrainPiece("trench2", TerrainKind.WALL, -120f, 250f, 25f, 140f, 1, false, TerrainSize.SMALL, 'A', 0, TerrainSurface.OPEN),
            TerrainPiece("igloo", TerrainKind.ROTUNDA, 230f, -200f, 80f, 80f, 1, true, TerrainSize.SMALL, 'B', 0, TerrainSurface.IMPASSABLE), // area terrain => blocksLos
            TerrainPiece("snow_wall", TerrainKind.WALL, 60f, 260f, 150f, 25f, 1, false, TerrainSize.SMALL, 'B', 0, TerrainSurface.OPEN),
        ))
    val MUSTAFAR = TerrainCard(id = "mustafar", nameKey = "terrain_mustafar",
        palette = TerrainPalette(Color(0xFF6E4A34), Color(0xFF7A3420), Color(0xFFB5653A)),
        pieces = listOf(
            TerrainPiece("lava_rig", TerrainKind.LAVA_RIG, -60f, -60f, 200f, 160f, 2, true, TerrainSize.LARGE, 'A', 0, TerrainSurface.IMPASSABLE),
            TerrainPiece("pylon1", TerrainKind.PYLON, 250f, -160f, 80f, 130f, 2, true, TerrainSize.MEDIUM, 'B'),
            TerrainPiece("pylon2", TerrainKind.PYLON, -300f, 180f, 70f, 110f, 2, true, TerrainSize.MEDIUM, 'A'),
            TerrainPiece("rock1", TerrainKind.ROTUNDA, 180f, 240f, 90f, 90f, 2, true, TerrainSize.MEDIUM, 'B'),
            TerrainPiece("rock2", TerrainKind.ROTUNDA, 300f, 40f, 70f, 70f, 2, true, TerrainSize.SMALL, 'B'),
            TerrainPiece("pipe", TerrainKind.WALL, 80f, 180f, 170f, 25f, 1, false, TerrainSize.SMALL, 'A', 0, TerrainSurface.OPEN),
            TerrainPiece("lava_pool", TerrainKind.FOUNTAIN, -230f, -240f, 90f, 70f, 0, false, TerrainSize.FILL_IN, 'A', 0, TerrainSurface.OPEN), // decor, aucune regle de deplacement
        ))
    val CORUSCANT = TerrainCard(id = "coruscant", nameKey = "terrain_coruscant",
        palette = TerrainPalette(Color(0xFF6E88A0), Color(0xFF5B7A99), Color(0xFF9DB4C8)),
        pieces = listOf(
            TerrainPiece("tower_main", TerrainKind.SKYSCRAPER, -70f, -70f, 150f, 170f, 2, true, TerrainSize.LARGE, 'A', 0, TerrainSurface.IMPASSABLE),
            TerrainPiece("tower_low", TerrainKind.SKYSCRAPER, 230f, 150f, 170f, 110f, 2, true, TerrainSize.MEDIUM, 'B', 180),
            TerrainPiece("pylon", TerrainKind.PYLON, 250f, -150f, 70f, 120f, 2, true, TerrainSize.MEDIUM, 'B'),
            TerrainPiece("skybridge", TerrainKind.WALL, 80f, 40f, 190f, 25f, 1, false, TerrainSize.SMALL, 'A', 0, TerrainSurface.OPEN),
            TerrainPiece("statue", TerrainKind.STATUE, -260f, 200f, 45f, 45f, 0, false, TerrainSize.SMALL, 'A', 0, TerrainSurface.OPEN),
        ))
    val KASHYYYK = TerrainCard(id = "kashyyyk", nameKey = "terrain_kashyyyk",
        palette = TerrainPalette(Color(0xFF3E6B45), Color(0xFF63A04C), Color(0xFFB08A4F)),
        pieces = listOf(
            TerrainPiece("tree_platform", TerrainKind.TREE_PLATFORM, -50f, -60f, 200f, 200f, 2, true, TerrainSize.LARGE, 'A', 0, TerrainSurface.IMPASSABLE),
            TerrainPiece("tree1", TerrainKind.TREE, 250f, -180f, 110f, 110f, 2, true, TerrainSize.MEDIUM, 'B'),
            TerrainPiece("tree2", TerrainKind.TREE, -280f, 180f, 100f, 100f, 2, true, TerrainSize.MEDIUM, 'A'),
            TerrainPiece("walkway", TerrainKind.WALL, 90f, 80f, 180f, 25f, 1, false, TerrainSize.SMALL, 'A', 0, TerrainSurface.OPEN),
            TerrainPiece("hut", TerrainKind.TEMPLE, 270f, 210f, 110f, 90f, 2, true, TerrainSize.MEDIUM, 'B'),
            TerrainPiece("palisade", TerrainKind.WALL, -250f, -200f, 140f, 25f, 1, false, TerrainSize.SMALL, 'A', 0, TerrainSurface.OPEN),
        ))
    val SCARIF = TerrainCard(id = "scarif", nameKey = "terrain_scarif",
        palette = TerrainPalette(Color(0xFFD9C9A3), Color(0xFF2E6E73), Color(0xFFC65A2C)),
        pieces = listOf(
            TerrainPiece("bunker", TerrainKind.BUNKER, -50f, -50f, 210f, 150f, 2, true, TerrainSize.LARGE, 'A', 0, TerrainSurface.IMPASSABLE),
            TerrainPiece("spire", TerrainKind.TOWER_SPIRE, 260f, -170f, 90f, 150f, 2, true, TerrainSize.MEDIUM, 'B'),
            TerrainPiece("wall1", TerrainKind.WALL, 80f, 190f, 200f, 25f, 1, false, TerrainSize.MEDIUM, 'A', 0, TerrainSurface.OPEN),
            TerrainPiece("radome", TerrainKind.ROTUNDA, -280f, 180f, 90f, 90f, 2, true, TerrainSize.MEDIUM, 'B'),
            TerrainPiece("crate", TerrainKind.CRATE, 40f, 90f, 45f, 45f, 1, false, TerrainSize.FILL_IN, 'A', 0, TerrainSurface.OPEN),
        ))
    val JAKKU = TerrainCard(id = "jakku", nameKey = "terrain_jakku",
        palette = TerrainPalette(Color(0xFFB9A878), Color(0xFF8A6B3F), Color(0xFF6B6252)),
        pieces = listOf(
            TerrainPiece("hangar", TerrainKind.HANGAR, -60f, -50f, 220f, 160f, 2, true, TerrainSize.LARGE, 'A', 0, TerrainSurface.IMPASSABLE),
            TerrainPiece("vaporizer", TerrainKind.MOISTURE_VAPORIZER, 250f, 140f, 100f, 150f, 1, false, TerrainSize.MEDIUM, 'B'),
            TerrainPiece("wreck", TerrainKind.CRATE, -280f, 190f, 120f, 70f, 1, false, TerrainSize.MEDIUM, 'A', 0, TerrainSurface.DIFFICULT),
            TerrainPiece("ctrl_tower", TerrainKind.TOWER, 250f, -180f, 60f, 60f, 2, true, TerrainSize.MEDIUM, 'B'),
            TerrainPiece("muret", TerrainKind.WALL, 70f, 240f, 150f, 25f, 1, false, TerrainSize.SMALL, 'A', 0, TerrainSurface.OPEN),
        ))
    val BESPIN = TerrainCard(id = "bespin", nameKey = "terrain_bespin",
        palette = TerrainPalette(Color(0xFFAEBAC6), Color(0xFF5D6E82), Color(0xFFD98A3C)),
        pieces = listOf(
            TerrainPiece("spire", TerrainKind.SKYSCRAPER, -60f, -60f, 140f, 180f, 2, true, TerrainSize.LARGE, 'A', 0, TerrainSurface.IMPASSABLE),
            TerrainPiece("pylon1", TerrainKind.PYLON, 240f, 130f, 90f, 120f, 1, false, TerrainSize.MEDIUM, 'B'),
            TerrainPiece("pylon2", TerrainKind.PYLON, -260f, 180f, 80f, 110f, 1, false, TerrainSize.MEDIUM, 'A'),
            TerrainPiece("dome", TerrainKind.ROTUNDA, 220f, -190f, 100f, 100f, 2, true, TerrainSize.MEDIUM, 'B'),
            TerrainPiece("bridge", TerrainKind.WALL, 50f, 60f, 190f, 25f, 1, false, TerrainSize.SMALL, 'A', 0, TerrainSurface.OPEN),
        ))
    val DAGOBAH = TerrainCard(id = "dagobah", nameKey = "terrain_dagobah",
        palette = TerrainPalette(Color(0xFF7C9A66), Color(0xFF527A44), Color(0xFF8A8F5A)),
        pieces = listOf(
            TerrainPiece("hut", TerrainKind.TEMPLE, -70f, -60f, 150f, 120f, 2, true, TerrainSize.LARGE, 'A', 0, TerrainSurface.IMPASSABLE),
            TerrainPiece("tree1", TerrainKind.TREE, 250f, -170f, 120f, 120f, 2, true, TerrainSize.MEDIUM, 'B'),
            TerrainPiece("tree2", TerrainKind.TREE, -280f, 190f, 110f, 110f, 2, true, TerrainSize.MEDIUM, 'A'),
            TerrainPiece("log", TerrainKind.WALL, 90f, 100f, 180f, 25f, 1, false, TerrainSize.MEDIUM, 'A', 0, TerrainSurface.DIFFICULT),
            TerrainPiece("moss", TerrainKind.ROTUNDA, 240f, 220f, 90f, 90f, 1, true, TerrainSize.SMALL, 'B', 0, TerrainSurface.DIFFICULT), // area terrain => blocksLos
            TerrainPiece("pool", TerrainKind.FOUNTAIN, -230f, -230f, 80f, 70f, 0, false, TerrainSize.FILL_IN, 'A', 0, TerrainSurface.OPEN), // decor
        ))
    val ALL = listOf(MONUMENT, RUINS, ENDOR, TATOOINE, NABOO, GEONOSIS,
        HOTH, MUSTAFAR, CORUSCANT, KASHYYYK, SCARIF, JAKKU, BESPIN, DAGOBAH)
}

/** Couleur d'un élément de terrain selon son type. */
private fun terrainColor(kind: TerrainKind): Color = when (kind) {
    TerrainKind.TEMPLE, TerrainKind.TOWER, TerrainKind.WALL -> Color(0xFFD9CBB0)
    TerrainKind.ROTUNDA -> Color(0xFF6FA89A)
    TerrainKind.MARKET -> Color(0xFFC27A2C)
    TerrainKind.CRATE -> Color(0xFF6B7280)
    TerrainKind.TREE -> Color(0xFF4E8B4A)
    TerrainKind.STATUE, TerrainKind.FOUNTAIN -> Color(0xFF9CA3AF)
    // Nouveaux bâtiments (palette par defaut si la carte n'en a pas).
    TerrainKind.ICE_BASE -> Color(0xFFDCE7F2)
    TerrainKind.GENERATOR -> Color(0xFF4E7FB0)
    TerrainKind.LAVA_RIG -> Color(0xFF6E2C18)
    TerrainKind.PYLON -> Color(0xFF7A8C99)
    TerrainKind.SKYSCRAPER -> Color(0xFF4B647E)
    TerrainKind.TREE_PLATFORM -> Color(0xFF3E6B45)
    TerrainKind.BUNKER -> Color(0xFFB7A47F)
    TerrainKind.TOWER_SPIRE -> Color(0xFF8A9AA8)
    TerrainKind.HANGAR -> Color(0xFF9C8B63)
    TerrainKind.MOISTURE_VAPORIZER -> Color(0xFF9AA3AB)
}

/** Socle losange isométrique + ombre portée, pour ancrer les silhouettes spéciales. */
private fun DrawScope.isoBase(tl: Offset, w: Float, h: Float, base: Color) {
    val hw = w / 2f; val hh = h / 2f; val k = 0.5f
    val cx = tl.x + hw; val cy = tl.y + hh
    val gT = Offset(cx, cy - hh * k); val gR = Offset(cx + hw, cy)
    val gB = Offset(cx, cy + hh * k); val gL = Offset(cx - hw, cy)
    val sh = Offset(maxOf(4f, 5.dp.toPx()), maxOf(3f, 3.dp.toPx()))
    drawPath(Path().apply {
        moveTo(gT.x + sh.x, gT.y + sh.y); lineTo(gR.x + sh.x, gR.y + sh.y)
        lineTo(gB.x + sh.x, gB.y + sh.y); lineTo(gL.x + sh.x, gL.y + sh.y); close()
    }, Color(0x55000000))
    drawPath(Path().apply {
        moveTo(gT.x, gT.y); lineTo(gR.x, gR.y); lineTo(gB.x, gB.y); lineTo(gL.x, gL.y); close()
    }, darken(base, 0.45f))
    drawPath(Path().apply {
        moveTo(gT.x, gT.y); lineTo(gR.x, gR.y); lineTo(gB.x, gB.y); lineTo(gL.x, gL.y); close()
    }, Color(0x88000000), style = Stroke(width = maxOf(1.5f, 1.5.dp.toPx())))
}

/** Arbre isométrique 2D : tronc 2 faces + canopée losange (pas de disque, pas de dôme). */
private fun DrawScope.isoTree(tl: Offset, w: Float, h: Float, base: Color, accent: Color) {
    val hw = w / 2f; val hh = h / 2f; val k = 0.5f
    val cx = tl.x + hw; val cy = tl.y + hh
    // Tronc : 2 faces (clair gauche / sombre droite).
    val tw = w * 0.16f
    val trunkTop = cy - h * 0.30f
    val trunkH = h * 0.34f   // se termine à cy + 0.04h, bien dans le losange (bas = cy + 0.25h)
    drawRect(darken(base, 0.35f), topLeft = Offset(cx - tw, trunkTop), size = Size(maxOf(1f, tw), maxOf(1f, trunkH)))
    drawRect(darken(base, 0.55f), topLeft = Offset(cx, trunkTop), size = Size(maxOf(1f, tw), maxOf(1f, trunkH)))
    // Canopée : losange iso (volume vu du dessus, cohérent avec roof()).
    val cw = w * 0.42f; val ch = h * 0.42f; val ccy = trunkTop
    val canopy = Path().apply {
        moveTo(cx, ccy - ch * k); lineTo(cx + cw, ccy); lineTo(cx, ccy + ch * k); lineTo(cx - cw, ccy); close()
    }
    drawPath(canopy, lighten(base, 0.22f))
    // Face inférieure de la canopée (donne l'épaisseur du feuillage).
    drawPath(Path().apply {
        moveTo(cx - cw, ccy); lineTo(cx, ccy + ch * k); lineTo(cx, ccy + ch * k + h * 0.08f); lineTo(cx - cw, ccy + h * 0.08f); close()
    }, darken(base, 0.28f))
    drawPath(canopy, accent.copy(alpha = 0.35f), style = Stroke(width = maxOf(1f, 1.dp.toPx())))
    drawPath(canopy, Color(0x88000000), style = Stroke(width = maxOf(1.5f, 1.5.dp.toPx())))
}

/** Éclaircit une couleur vers le blanc (aplats SNES : valeur, pas dégradé). */
private fun lighten(c: Color, f: Float): Color = Color(
    alpha = c.alpha,
    red = c.red + (1f - c.red) * f,
    green = c.green + (1f - c.green) * f,
    blue = c.blue + (1f - c.blue) * f,
)

/** Assombrit une couleur vers le noir. */
private fun darken(c: Color, f: Float): Color = Color(
    alpha = c.alpha,
    red = c.red * (1f - f),
    green = c.green * (1f - f),
    blue = c.blue * (1f - f),
)

/**
 * Bâtiment ISOMÉTRIQUE 2D façon Zelda SNES, géométrie COHÉRENTE :
 * socle losange au sol → murs verticaux (gauche moyenne / droite sombre) →
 * toit à 2 versants posé EXACTEMENT sur le haut des murs (mêmes sommets).
 * Une seule direction de lumière. Aplats + contours nets, zéro dégradé, zéro vraie 3D.
 */
private fun DrawScope.roof(tl: Offset, w: Float, h: Float, base: Color, dark: Color, accent: Color) {
    val hw = w / 2f
    val hh = h / 2f
    val k = 0.5f                                // aplatissement de la projection iso
    val cx = tl.x + hw
    val cy = tl.y + hh
    val wallH = minOf(w, h) * 0.38f             // hauteur des murs
    val roofRise = minOf(w, h) * 0.22f          // hauteur du faîtage au-dessus des gouttières

    // Coins du SOCLE au sol (losange).
    val gTop = Offset(cx, cy - hh * k)
    val gRight = Offset(cx + hw, cy)
    val gBottom = Offset(cx, cy + hh * k)
    val gLeft = Offset(cx - hw, cy)

    // 1) Ombre portée au sol (losange décalé bas-droite, net) — TOUJOURS en fond.
    val sh = Offset(maxOf(4f, 5.dp.toPx()), maxOf(3f, 3.dp.toPx()))
    drawPath(Path().apply {
        moveTo(gTop.x + sh.x, gTop.y + sh.y); lineTo(gRight.x + sh.x, gRight.y + sh.y)
        lineTo(gBottom.x + sh.x, gBottom.y + sh.y); lineTo(gLeft.x + sh.x, gLeft.y + sh.y); close()
    }, Color(0x5A000000))

    // 1b) SOCLE au sol (losange sombre) — dessiné APRÈS l'ombre, donc visible.
    // Légèrement élargi pour dépasser sous les murs = ancrage net, plus de pied flottant.
    val sGrow = 1.12f
    fun soclePt(p: Offset) = Offset(cx + (p.x - cx) * sGrow, cy + (p.y - cy) * sGrow)
    val sTop = soclePt(gTop); val sRight = soclePt(gRight)
    val sBottom = soclePt(gBottom); val sLeft = soclePt(gLeft)
    val soclePath = Path().apply {
        moveTo(sTop.x, sTop.y); lineTo(sRight.x, sRight.y)
        lineTo(sBottom.x, sBottom.y); lineTo(sLeft.x, sLeft.y); close()
    }
    drawPath(soclePath, darken(base, 0.5f))
    drawPath(soclePath, Color(0x88000000), style = Stroke(width = maxOf(1.5f, 1.5.dp.toPx())))

    // 1c) RUBBLE : 3 petits débris sourds au pied (ancrage « based terrain »).
    val rBase = minOf(w, h) * 0.05f
    drawPath(Path().apply {
        moveTo(gLeft.x - rBase * 0.5f, gLeft.y + rBase * 0.2f)
        lineTo(gLeft.x - rBase * 1.4f, gLeft.y + rBase * 0.9f)
        lineTo(gLeft.x - rBase * 0.3f, gLeft.y + rBase * 1.1f); close()
    }, darken(base, 0.4f))
    drawPath(Path().apply {
        moveTo(gRight.x + rBase * 0.4f, gRight.y + rBase * 0.3f)
        lineTo(gRight.x + rBase * 1.5f, gRight.y + rBase)
        lineTo(gRight.x + rBase * 0.6f, gRight.y + rBase * 1.2f); close()
    }, darken(base, 0.48f))
    drawCircle(darken(base, 0.35f), radius = maxOf(1f, rBase * 0.5f),
        center = Offset(gBottom.x + rBase * 0.8f, gBottom.y + rBase * 0.6f))

    // Coins du HAUT DES MURS = coins du socle remontés de wallH (mêmes x).
    val eTop = Offset(cx, cy - hh * k - wallH)
    val eRight = Offset(cx + hw, cy - wallH)
    val eBottom = Offset(cx, cy + hh * k - wallH)
    val eLeft = Offset(cx - hw, cy - wallH)

    // 2) Murs : face GAUCHE (moyenne, gLeft→gBottom), face DROITE (sombre, gBottom→gRight).
    drawPath(Path().apply {
        moveTo(gLeft.x, gLeft.y); lineTo(gBottom.x, gBottom.y)
        lineTo(eBottom.x, eBottom.y); lineTo(eLeft.x, eLeft.y); close()
    }, base)
    drawPath(Path().apply {
        moveTo(gBottom.x, gBottom.y); lineTo(gRight.x, gRight.y)
        lineTo(eRight.x, eRight.y); lineTo(eBottom.x, eBottom.y); close()
    }, darken(base, 0.3f))

    // 3) Toit à 2 versants posé sur eLeft/eTop/eRight/eBottom.
    // Faîtage = ligne entre les milieux des bords (eTop-eLeft) et (eRight-eBottom), +roofRise.
    val rA = Offset((eTop.x + eLeft.x) / 2f, (eTop.y + eLeft.y) / 2f - roofRise)
    val rB = Offset((eRight.x + eBottom.x) / 2f, (eRight.y + eBottom.y) / 2f - roofRise)
    // Toit COMPLET : d'abord remplir TOUT le losange supérieur (fond de toit),
    // puis poser les 2 pans par-dessus. Aucun trou possible.
    val roofBase = Path().apply {
        moveTo(eTop.x, eTop.y); lineTo(eRight.x, eRight.y)
        lineTo(eBottom.x, eBottom.y); lineTo(eLeft.x, eLeft.y); close()
    }
    drawPath(roofBase, lighten(base, 0.18f))
    // Pan GAUCHE (clair) : eLeft → eBottom → faîtage rB → rA.
    val leftSlope = Path().apply {
        moveTo(eLeft.x, eLeft.y); lineTo(eBottom.x, eBottom.y); lineTo(rB.x, rB.y); lineTo(rA.x, rA.y); close()
    }
    // Pan DROIT (moyen) : rA → rB → eRight → eTop.
    val rightSlope = Path().apply {
        moveTo(rA.x, rA.y); lineTo(rB.x, rB.y); lineTo(eRight.x, eRight.y); lineTo(eTop.x, eTop.y); close()
    }
    drawPath(leftSlope, lighten(base, 0.30f))
    drawPath(rightSlope, lighten(base, 0.06f))
    drawLine(lighten(base, 0.55f), rA, rB, strokeWidth = maxOf(1.5f, 1.5.dp.toPx()))

    // 4) Porte sombre sur la face gauche.
    val doorCx = (gLeft.x + gBottom.x) / 2f
    // Mur = de gLeft/gBottom (bas) à eLeft/eBottom (haut), hauteur wallH.
    // PorteEntièrement DANS le mur : haut à -0.80·wallH, hauteur 0.55·wallH → bas -0.25·wallH.
    val doorTop = (gLeft.y + gBottom.y) / 2f - wallH * 0.80f
    drawRect(Color(0xFF2A2018), topLeft = Offset(doorCx - w * 0.06f, doorTop),
        size = Size(maxOf(1.5f, w * 0.12f), maxOf(2f, wallH * 0.55f)))

    // 4b) SOUBASSEMENT : bande sombre sur le bas des 2 faces (la « matière » qui
    // manquait au placeholder). Interpolation le long des murs, hauteur ~22% de wallH.
    val plinthT = 0.22f
    fun lerp(a: Offset, b: Offset, t: Float) = Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
    val gLeftP = lerp(eLeft, gLeft, plinthT); val gBottomP = lerp(eBottom, gBottom, plinthT)
    val gRightP = lerp(eRight, gRight, plinthT)
    drawPath(Path().apply {
        moveTo(gLeft.x, gLeft.y); lineTo(gBottom.x, gBottom.y)
        lineTo(gBottomP.x, gBottomP.y); lineTo(gLeftP.x, gLeftP.y); close()
    }, darken(base, 0.42f))
    drawPath(Path().apply {
        moveTo(gBottom.x, gBottom.y); lineTo(gRight.x, gRight.y)
        lineTo(gRightP.x, gRightP.y); lineTo(gBottomP.x, gBottomP.y); close()
    }, darken(darken(base, 0.3f), 0.42f))

    // 4c) MATIÈRE : 2 traits de panneau sur le versant clair (dry-brush sourd),
    // jamais de grille de fenêtres (anti-coloriage). Couleur = toit éclairci.
    val seam = lighten(base, 0.10f)
    val seamW = maxOf(1f, 1.dp.toPx())
    drawLine(seam, lerp(eLeft, rA, 0.42f), lerp(eBottom, rB, 0.42f), seamW)
    drawLine(seam, lerp(eLeft, rA, 0.68f), lerp(eBottom, rB, 0.68f), seamW)
    drawLine(darken(base, 0.5f), gLeft, gBottom, strokeWidth = maxOf(2f, 2.dp.toPx()))
    drawLine(darken(base, 0.5f), gBottom, gRight, strokeWidth = maxOf(2f, 2.dp.toPx()))

    // 5) Contours nets 16 bits.
    val outline = maxOf(1.5f, 1.5.dp.toPx())
    drawPath(leftSlope, Color(0x99000000), style = Stroke(width = outline))
    drawPath(rightSlope, Color(0x99000000), style = Stroke(width = outline))
    drawLine(Color(0x99000000), gLeft, eLeft, outline)
    drawLine(Color(0x99000000), gBottom, eBottom, outline)
    drawLine(Color(0x99000000), gRight, eRight, outline)
}

/** Flèche / toit conique (triangle). */
private fun DrawScope.spire(tl: Offset, w: Float, h: Float, c: Color) {
    val path = Path().apply {
        moveTo(tl.x + w / 2f, tl.y - h * 0.18f)
        lineTo(tl.x, tl.y + h * 0.45f)
        lineTo(tl.x + w, tl.y + h * 0.45f)
        close()
    }
    drawPath(path, c)
}

/** Demi-cercle aplati tourné vers le haut (base Hoth). */
private fun DrawScope.domeFlat(cx: Float, cy: Float, w: Float, h: Float, c: Color) {
    drawArc(c, 180f, 180f, false, topLeft = Offset(cx - w / 2f, cy - h / 2f), size = Size(maxOf(1f, w), maxOf(1f, h)))
}

/**
 * Dessine le paysage d'une carte de terrain sur le plateau, après la surface
 * mais avant les pions. `mm` convertit des mm plateau en px écran.
 */
fun DrawScope.drawTerrainCard(
    card: TerrainCard?,
    mm: (Float, Float) -> Offset,
    pxPerMm: Float,
    textMeasurer: TextMeasurer,
) {
    if (card == null) return
    for (piece in card.pieces) {
        val center = mm(piece.xMm, piece.yMm)
        val w = piece.wMm * pxPerMm
        val h = piece.hMm * pxPerMm
        val topLeft = Offset(center.x - w / 2f, center.y - h / 2f)
        // ── Chemin HISTORIQUE STRICT pour les 6 cartes d'origine (palette == null) ──
        // Aucune régression visuelle : mêmes couleurs, mêmes formes, mêmes anneaux.
        if (card.palette == null) {
            val oldCol = terrainColor(piece.kind)
            drawRect(Color(0x55000000), topLeft = Offset(topLeft.x + 3f, topLeft.y + 4f), size = Size(maxOf(1f, w), maxOf(1f, h)))
            when (piece.kind) {
                TerrainKind.ROTUNDA, TerrainKind.TREE, TerrainKind.FOUNTAIN, TerrainKind.STATUE -> {
                    drawCircle(oldCol, radius = maxOf(1f, minOf(w, h) / 2f), center = center)
                    if (piece.kind == TerrainKind.ROTUNDA) {
                        drawCircle(Color(0xFF3A6E63), radius = maxOf(1f, minOf(w, h) / 4f), center = center)
                    }
                }
                else -> {
                    drawRect(oldCol, topLeft = topLeft, size = Size(maxOf(1f, w), maxOf(1f, h)))
                    if (piece.kind == TerrainKind.TEMPLE || piece.kind == TerrainKind.TOWER) {
                        drawCircle(Color(0xFF6FA89A), radius = maxOf(1f, minOf(w, h) / 3f), center = center)
                    }
                }
            }
            if (piece.cover > 0) {
                val coverCol = if (piece.cover >= 2) LegionColors.Warning else LegionColors.Info
                val r = maxOf(w, h) / 2f + 3.dp.toPx()
                drawCircle(coverCol.copy(alpha = 0.75f), radius = r, center = center, style = Stroke(width = 2.dp.toPx()))
            }
            if (piece.blocksLos) {
                val st = TextStyle(fontSize = LegionType.Caption, fontWeight = FontWeight.Bold, color = LegionColors.TextPrimary)
                drawText(textMeasurer, "✕", topLeft = Offset(center.x - 6.dp.toPx(), center.y - 8.dp.toPx()),
                    size = Size(14.dp.toPx(), 16.dp.toPx()), style = st)
            }
            continue
        }
        val pal = card.palette ?: continue
        val col = pal.base
        val shade = pal.shade
        val accent = pal.accent
        val sw = maxOf(2f, 2.dp.toPx())
        val cx = center.x
        val cy = center.y

        // Ombre portée : l'opacité dépend de la surface (OPEN = praticable = ombre legere,
        // IMPASSABLE = masse pleine). Consomme `surface` — plus un champ mort.
        val shadowAlpha = when (piece.surface) {
            TerrainSurface.OPEN -> 0x2E
            TerrainSurface.DIFFICULT -> 0x44
            TerrainSurface.IMPASSABLE -> 0x55
        }
        // NOTE : les bâtiments iso dessinent leur PROPRE ombre + socle losange dans roof().
        // L'ombre rectaxiale globale n'est gardée que pour les kinds NON-bâtiment (décor plat).
        val isBuilding = piece.kind in setOf(
            TerrainKind.ICE_BASE, TerrainKind.GENERATOR, TerrainKind.LAVA_RIG,
            TerrainKind.SKYSCRAPER, TerrainKind.BUNKER, TerrainKind.TOWER_SPIRE,
            TerrainKind.HANGAR, TerrainKind.MOISTURE_VAPORIZER,
            TerrainKind.MARKET, TerrainKind.TEMPLE, TerrainKind.TOWER,
            TerrainKind.PYLON, TerrainKind.TREE_PLATFORM,
        )
        if (!isBuilding) {
            val shOff = maxOf(4f, 5.dp.toPx())
            drawRect(Color(shadowAlpha shl 24), topLeft = Offset(topLeft.x + shOff, topLeft.y + shOff),
                size = Size(maxOf(1f, w), maxOf(1f, h)))
            if (piece.surface != TerrainSurface.OPEN && piece.cover > 0) {
                val rr = maxOf(1.5f, 2.dp.toPx())
                drawRect(shade.copy(alpha = 0.5f), topLeft = Offset(topLeft.x - rr, topLeft.y + h * 0.82f), size = Size(rr * 2f, rr))
                drawRect(shade.copy(alpha = 0.5f), topLeft = Offset(topLeft.x + w * 0.72f, topLeft.y + h * 0.9f), size = Size(rr * 1.6f, rr))
            }
        }
        // Liseré silhouette-aware. Epaisseur = `size` DOC62 ; teinte = `half` A/B
        // (permet de distinguer les 2 moitiés d'un layout). Pas de liseré si cover=0.
        val edgeWidth = when (piece.size) {
            TerrainSize.LARGE -> maxOf(2f, 2.dp.toPx())
            TerrainSize.MEDIUM -> maxOf(1.5f, 1.5.dp.toPx())
            TerrainSize.SMALL -> maxOf(1.5f, 1.dp.toPx())
            TerrainSize.FILL_IN -> maxOf(1f, 0.5.dp.toPx())
        }
        val edgeCol = (if (piece.half == 'B') shade else accent).copy(alpha = 0.85f)
        val roundKind = piece.kind == TerrainKind.ROTUNDA || piece.kind == TerrainKind.TREE ||
            piece.kind == TerrainKind.FOUNTAIN || piece.kind == TerrainKind.TREE_PLATFORM
        if (piece.cover > 0 && !isBuilding) {
            if (roundKind) {
                drawCircle(edgeCol, radius = maxOf(1f, minOf(w, h) / 2f + 1.dp.toPx()), center = center,
                    style = Stroke(width = edgeWidth))
            } else {
                drawRect(edgeCol, topLeft = topLeft, size = Size(maxOf(1f, w), maxOf(1f, h)),
                    style = Stroke(width = edgeWidth))
            }
        }
        rotate(piece.rotation.toFloat(), center) {
        when (piece.kind) {
            // ── Anciens kinds (comportement préservé) ──
            TerrainKind.ROTUNDA -> {
                drawCircle(col, radius = maxOf(1f, minOf(w, h) / 2f), center = center)
                drawCircle(shade, radius = maxOf(1f, minOf(w, h) / 4f), center = center)
            }
            TerrainKind.TREE -> {
                isoTree(topLeft, w, h, col, accent)
            }
            TerrainKind.FOUNTAIN -> {
                drawCircle(shade, radius = maxOf(1f, minOf(w, h) / 2f), center = center)
                drawCircle(accent, radius = maxOf(1f, minOf(w, h) / 3f), center = center)
            }
            TerrainKind.STATUE -> {
                drawCircle(col, radius = maxOf(1f, minOf(w, h) / 2f), center = center)
            }
            TerrainKind.MARKET -> {
                roof(topLeft, w, h, col, shade, accent)
            }
            TerrainKind.TEMPLE, TerrainKind.TOWER -> {
                roof(topLeft, w, h, col, shade, accent)
            }
            TerrainKind.WALL, TerrainKind.CRATE -> {
                drawRect(col, topLeft = topLeft, size = Size(w, h))
            }
            // ── Nouveaux bâtiments vectoriels ──
            TerrainKind.ICE_BASE -> {
                roof(topLeft, w, h, col, shade, accent)
                val elev = minOf(w, h) * 0.38f
                drawLine(accent.copy(alpha = 0.8f), Offset(cx - w * 0.22f, cy - h * 0.5f - elev),
                    Offset(cx - w * 0.28f, cy - h * 0.5f - elev - h * 0.3f), sw)
                drawLine(accent.copy(alpha = 0.8f), Offset(cx + w * 0.22f, cy - h * 0.5f - elev),
                    Offset(cx + w * 0.28f, cy - h * 0.5f - elev - h * 0.24f), sw)
            }
            TerrainKind.GENERATOR -> {
                roof(topLeft, w, h, col, shade, accent)
                val elev = minOf(w, h) * 0.38f
                repeat(2) { i ->
                    val bx = cx + (i - 0.5f) * w * 0.3f
                    drawRect(darken(col, 0.3f), topLeft = Offset(bx, cy - h * 0.5f - elev - h * 0.2f),
                        size = Size(maxOf(1f, w * 0.1f), h * 0.2f))
                }
            }
            TerrainKind.LAVA_RIG -> {
                roof(topLeft, w, h, col, shade, accent)
                val elev = minOf(w, h) * 0.38f
                // Cheminées sur le toit (matière sourde, pas de disque orange vif).
                repeat(2) { i ->
                    val bx = cx + (i - 0.5f) * w * 0.28f
                    drawRect(darken(col, 0.35f), topLeft = Offset(bx, cy - h * 0.5f - elev - h * 0.18f),
                        size = Size(maxOf(1f, w * 0.09f), h * 0.18f))
                }
            }
            TerrainKind.PYLON -> {
                isoBase(topLeft, w, h, col)
                val path = Path().apply {
                    moveTo(cx - w * 0.5f, topLeft.y + h)
                    lineTo(cx - w * 0.18f, topLeft.y)
                    lineTo(cx + w * 0.18f, topLeft.y)
                    lineTo(cx + w * 0.5f, topLeft.y + h)
                    close()
                }
                drawPath(path, col)
                drawCircle(accent, radius = maxOf(1f, minOf(w, h) * 0.3f), center = Offset(cx, topLeft.y + h * 0.3f), style = Stroke(sw))
            }
            TerrainKind.SKYSCRAPER -> {
                // Volume iso (pas de façade ni de grille de fenêtres). Antenne = repère.
                roof(topLeft, w, h, col, shade, accent)
                val elev = minOf(w, h) * 0.42f
                drawLine(accent.copy(alpha = 0.75f), Offset(cx, cy - h * 0.28f - elev),
                    Offset(cx, cy - h * 0.28f - elev - h * 0.2f), sw)
            }
            TerrainKind.TREE_PLATFORM -> {
                // Socle losange (plateforme) + arbre iso, un seul vocabulaire de projection.
                isoBase(topLeft, w, h, col)
                isoTree(topLeft, w, h, col, accent)
            }
            TerrainKind.BUNKER -> {
                roof(topLeft, w, h, col, shade, accent)
                // Bunker : toit plus plat (faîtage écrasé) déjà géré par roof; 2 meurtrières sombres.
                val elev = minOf(w, h) * 0.38f
                drawRect(Color(0xFF1A1410), topLeft = Offset(cx - w * 0.16f, cy - elev * 0.5f),
                    size = Size(maxOf(1f, w * 0.1f), maxOf(1.5f, elev * 0.18f)))
                drawRect(Color(0xFF1A1410), topLeft = Offset(cx + w * 0.06f, cy - elev * 0.5f),
                    size = Size(maxOf(1f, w * 0.1f), maxOf(1.5f, elev * 0.18f)))
            }
            TerrainKind.TOWER_SPIRE -> {
                isoBase(topLeft, w, h, col)
                drawRect(col, topLeft = Offset(topLeft.x, cy - h * 0.1f), size = Size(w, h * 0.6f))
                spire(topLeft, w, h, shade)
                drawLine(accent, Offset(cx, topLeft.y - h * 0.18f), Offset(cx, topLeft.y - h * 0.5f), sw)
                drawCircle(accent, radius = maxOf(1f, w * 0.18f), center = Offset(cx + w * 0.3f, cy - h * 0.1f))
            }
            TerrainKind.HANGAR -> {
                roof(topLeft, w, h, col, shade, accent)
                // Porte de hangar sombre sur la face avant.
                val elev = minOf(w, h) * 0.38f
                drawRect(Color(0xFF2B2118), topLeft = Offset(cx - w * 0.13f, cy - elev * 0.65f),
                    size = Size(maxOf(1f, w * 0.26f), maxOf(2f, elev * 0.55f)))
            }
            TerrainKind.MOISTURE_VAPORIZER -> {
                isoBase(topLeft, w, h, col)
                drawRect(col, topLeft = Offset(cx - w * 0.12f, topLeft.y + h * 0.25f), size = Size(maxOf(1f, w * 0.24f), h * 0.6f))
                drawCircle(shade, radius = maxOf(1f, w * 0.3f), center = Offset(cx - w * 0.3f, topLeft.y + h * 0.45f))
                drawCircle(shade, radius = maxOf(1f, w * 0.3f), center = Offset(cx + w * 0.3f, topLeft.y + h * 0.45f))
                drawLine(accent, Offset(cx - w * 0.3f, topLeft.y + h * 0.75f), Offset(cx + w * 0.3f, topLeft.y + h * 0.75f), sw)
                drawLine(accent, Offset(cx, topLeft.y + h * 0.25f), Offset(cx, topLeft.y - h * 0.1f), sw)
            }
        }
        } // rotate
        // Couvert : couleurs DEDIEES hors palette UI — bleu franc #2E9BFF = leger(1),
        // orange sature #FF7A00 = lourd(2). Double trait (halo sombre + plein) lisible
        // a 25 % comme zoome, distinct du dore des objectifs. Chiffre 1/2 au centre.
        if (piece.cover > 0) {
            val coverCol = if (piece.cover >= 2) Color(0xFFFF7A00) else Color(0xFF2E9BFF)
            val r = maxOf(w, h) / 2f + 4.dp.toPx()
            val ringStroke = maxOf(2f, 2.dp.toPx())
            drawCircle(Color(0x99000000), radius = r + 2f, center = center, style = Stroke(width = ringStroke + 2f))
            drawCircle(coverCol, radius = r, center = center, style = Stroke(width = ringStroke))
            val badge = if (piece.cover >= 2) "2" else "1"
            val bs = TextStyle(fontSize = LegionType.Caption, fontWeight = FontWeight.Bold, color = coverCol)
            drawText(textMeasurer, badge, topLeft = Offset(cx + r - 2.dp.toPx(), cy - r - 6.dp.toPx()),
                size = Size(16.dp.toPx(), 16.dp.toPx()), style = bs)
        }
        // Ligne de vue bloquée : deux traits en coin (EN HAUT A GAUCHE), pas au centre
        // (un ✕ central se lisait comme une unite morte sur les captures).
        if (piece.blocksLos) {
            val kx = topLeft.x + 2.dp.toPx(); val ky = topLeft.y + 2.dp.toPx()
            val k = maxOf(4f, 5.dp.toPx())
            val losCol = Color(0xFFFFFFFF)
            drawLine(losCol, Offset(kx, ky), Offset(kx + k, ky + k), sw)
            drawLine(losCol, Offset(kx + k, ky), Offset(kx, ky + k), sw)
        }
    }
}
