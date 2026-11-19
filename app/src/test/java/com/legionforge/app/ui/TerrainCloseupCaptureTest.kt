package com.legionforge.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.legionforge.app.ui.screens.TerrainCards
import com.legionforge.app.ui.screens.TerrainPiece
import com.legionforge.app.ui.screens.drawTerrainCard
import com.legionforge.app.ui.theme.LegionForgeTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Gros plan d'une carte vectorielle seule, pour juger le style isométrique 2D. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-mdpi")
class TerrainCloseupCaptureTest {
    private fun closeup(name: String, cardId: String) {
        val base = TerrainCards.ALL.first { it.id == cardId }
        // Neutraliser les overlays de règle (anneaux de couvert / LOS) pour juger le dessin pur.
        val card = base.copy(pieces = base.pieces.map { p: TerrainPiece ->
            p.copy(cover = 0, blocksLos = false)
        })
        captureRoboImage("src/test/snapshots/terrain-closeup-$name.png") {
            LegionForgeTheme {
                val density = LocalDensity.current
                val tm = rememberTextMeasurer()
                Canvas(Modifier.size(400.dp)) {
                    // Gros plan : 1 mm plateau = 2 px écran, centré sur l'origine de la carte.
                    val scale = 1f
                    drawTerrainCard(
                        card = card,
                        mm = { x, y -> Offset(size.width / 2f + x * scale, size.height / 2f + y * scale) },
                        pxPerMm = scale,
                        textMeasurer = tm,
                    )
                }
            }
        }
    }

    @Test fun hoth() = closeup("hoth", "hoth")
    @Test fun mustafar() = closeup("mustafar", "mustafar")
    @Test fun coruscant() = closeup("coruscant", "coruscant")
    @Test fun kashyyyk() = closeup("kashyyyk", "kashyyyk")
    @Test fun scarif() = closeup("scarif", "scarif")
    @Test fun jakku() = closeup("jakku", "jakku")
    @Test fun bespin() = closeup("bespin", "bespin")
    @Test fun dagobah() = closeup("dagobah", "dagobah")
}
