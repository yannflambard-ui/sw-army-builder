package com.legionforge.app.ui.screens

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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
enum class TerrainKind { TEMPLE, ROTUNDA, TOWER, MARKET, WALL, CRATE, TREE, STATUE, FOUNTAIN }

/** Un élément de terrain, positionné en mm par rapport au centre du tapis (0,0). */
data class TerrainPiece(
    val id: String,
    val kind: TerrainKind,
    val xMm: Float,
    val yMm: Float,
    val wMm: Float,
    val hMm: Float,
    val cover: Int, // 0 = aucun, 1 = léger, 2 = lourd
    val blocksLos: Boolean = false,
)

/** Une carte de terrain pré-construite, indépendante des cartes Map du jeu. */
data class TerrainCard(
    val id: String,
    val nameKey: String,
    val pieces: List<TerrainPiece>,
)

/** Les 2 cartes fidèles aux dioramas fournis (vue 2D de dessus, tapis 36"). */
object TerrainCards {
    val MONUMENT = TerrainCard(
        id = "monument", nameKey = "terrain_monument",
        pieces = listOf(
            TerrainPiece("temple", TerrainKind.TEMPLE, -300f, -300f, 150f, 110f, 2, true),
            TerrainPiece("kiosque", TerrainKind.ROTUNDA, -40f, -290f, 70f, 70f, 2, true),
            TerrainPiece("tour", TerrainKind.TOWER, 300f, -230f, 80f, 80f, 2, true),
            TerrainPiece("rotonde", TerrainKind.ROTUNDA, 0f, 0f, 150f, 150f, 2, false),
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
            TerrainPiece("fontaine", TerrainKind.FOUNTAIN, 0f, 0f, 55f, 55f, 1),
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
    val ALL = listOf(MONUMENT, RUINS, ENDOR, TATOOINE, NABOO, GEONOSIS)
}

/** Couleur d'un élément de terrain selon son type. */
private fun terrainColor(kind: TerrainKind): Color = when (kind) {
    TerrainKind.TEMPLE, TerrainKind.TOWER, TerrainKind.WALL -> Color(0xFFD9CBB0)
    TerrainKind.ROTUNDA -> Color(0xFF6FA89A)
    TerrainKind.MARKET -> Color(0xFFC27A2C)
    TerrainKind.CRATE -> Color(0xFF6B7280)
    TerrainKind.TREE -> Color(0xFF4E8B4A)
    TerrainKind.STATUE, TerrainKind.FOUNTAIN -> Color(0xFF9CA3AF)
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
        val col = terrainColor(piece.kind)

        // Ombre portée pour le relief.
        drawRect(Color(0x55000000), topLeft = Offset(topLeft.x + 3f, topLeft.y + 4f), size = Size(maxOf(1f, w), maxOf(1f, h)))
        when (piece.kind) {
            TerrainKind.ROTUNDA, TerrainKind.TREE, TerrainKind.FOUNTAIN, TerrainKind.STATUE -> {
                drawCircle(col, radius = maxOf(1f, minOf(w, h) / 2f), center = center)
                if (piece.kind == TerrainKind.ROTUNDA) {
                    drawCircle(Color(0xFF3A6E63), radius = maxOf(1f, minOf(w, h) / 4f), center = center)
                }
            }
            else -> {
                drawRect(col, topLeft = topLeft, size = Size(maxOf(1f, w), maxOf(1f, h)))
                if (piece.kind == TerrainKind.TEMPLE || piece.kind == TerrainKind.TOWER) {
                    // Toit/dôme suggéré par un dôme au centre.
                    drawCircle(Color(0xFF6FA89A), radius = maxOf(1f, minOf(w, h) / 3f), center = center)
                }
            }
        }
        // Couvert : anneau LEGER (bleu) / LOURD (orange) autour de la pièce.
        if (piece.cover > 0) {
            val coverCol = if (piece.cover >= 2) LegionColors.Warning else LegionColors.Info
            val r = maxOf(w, h) / 2f + 3.dp.toPx()
            drawCircle(coverCol.copy(alpha = 0.75f), radius = r, center = center, style = Stroke(width = 2.dp.toPx()))
        }
        // Ligne de vue bloquée : petit trait barré.
        if (piece.blocksLos) {
            val mark = "✕"
            val st = TextStyle(fontSize = LegionType.Caption, fontWeight = FontWeight.Bold, color = LegionColors.TextPrimary)
            drawText(textMeasurer, mark, topLeft = Offset(center.x - 6.dp.toPx(), center.y - 8.dp.toPx()),
                size = Size(14.dp.toPx(), 16.dp.toPx()), style = st)
        }
    }
}
