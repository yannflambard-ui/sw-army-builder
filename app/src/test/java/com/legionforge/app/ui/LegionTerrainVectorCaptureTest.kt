package com.legionforge.app.ui

import com.github.takahirom.roborazzi.captureRoboImage
import com.legionforge.app.ui.theme.LegionForgeTheme
import com.legionforge.app.ui.screens.LegionPawn
import com.legionforge.app.ui.screens.LegionTabletopScreen
import com.legionforge.app.ui.screens.TerrainCard
import com.legionforge.app.ui.screens.TerrainCards
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Captures des 8 cartes planète vectorielles pour contrôle visuel (25 % / mobile). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-mdpi")
class LegionTerrainVectorCaptureTest {
    private val pawns = listOf(
        LegionPawn("a1", "B1 Battle Droids", -180f, 130f, baseSize = "small", maxWounds = 4, wounds = 4), // hors silhouette centrale
        LegionPawn("a2", "AT-TE", 90f, -120f, baseSize = "large", maxWounds = 8, wounds = 8),
        LegionPawn("b1", "Captain Rex", 60f, 60f, baseSize = "medium", maxWounds = 4, wounds = 4, isOpp = true),
    )

    private fun capture(card: TerrainCard) {
        captureRoboImage("src/test/snapshots/terrain-vector-${card.id}.png") {
            LegionForgeTheme {
                LegionTabletopScreen(pawns = pawns, round = 2, unitsLeft = 3,
                    onBack = {}, onOpenPlay = {}, onActivate = {}, onCoverChange = { _, _ -> },
                    onMove = { _, _, _, _ -> }, onRoundChange = {}, onWoundsChange = { _, _ -> },
                    terrainCard = card, victoryPoints = 6)
            }
        }
    }

    @Test fun hoth() = capture(TerrainCards.HOTH)
    @Test fun mustafar() = capture(TerrainCards.MUSTAFAR)
    @Test fun coruscant() = capture(TerrainCards.CORUSCANT)
    @Test fun kashyyyk() = capture(TerrainCards.KASHYYYK)
    @Test fun scarif() = capture(TerrainCards.SCARIF)
    @Test fun jakku() = capture(TerrainCards.JAKKU)
    @Test fun bespin() = capture(TerrainCards.BESPIN)
    @Test fun dagobah() = capture(TerrainCards.DAGOBAH)
}
