package com.legionforge.app.ui

import com.legionforge.app.ui.screens.BattleMats
import com.legionforge.app.ui.screens.LegionPawn
import com.legionforge.app.ui.screens.LegionTabletopScreen
import com.legionforge.app.ui.theme.LegionForgeTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Capture du plateau avec vraie image de battle mat (Tatooine) + couche logique. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-mdpi")
class LegionBattleMatCaptureTest {

    private val pawns = listOf(
        LegionPawn("a1", "B1 Battle Droids", -60f, -80f, baseSize = "small", maxWounds = 4, wounds = 4, speed = 2),
        LegionPawn("a2", "General Grievous", 40f, -40f, baseSize = "medium", maxWounds = 5, wounds = 3, speed = 2),
        LegionPawn("b1", "Clone Troopers", -40f, 90f, baseSize = "small", maxWounds = 4, wounds = 2, speed = 2, isOpp = true, cover = 1),
    )

    @Test
    fun battlemat_tatooine_avec_logique() {
        captureRoboImage("src/test/snapshots/legion-battlemat-tatooine.png") {
            LegionForgeTheme {
                LegionTabletopScreen(pawns = pawns, round = 1, unitsLeft = 3,
                    onBack = {}, onOpenPlay = {}, onActivate = {}, onCoverChange = { _, _ -> },
                    onMove = { _, _, _, _ -> }, onRoundChange = {}, onWoundsChange = { _, _ -> },
                    battleMat = BattleMats.TATOOINE, battleMatName = "Tatooine",
                    onPickBattleMat = {})
            }
        }
    }

    @Test
    fun battlemat_scarif_sans_logique() {
        captureRoboImage("src/test/snapshots/legion-battlemat-scarif-photo.png") {
            LegionForgeTheme {
                LegionTabletopScreen(pawns = pawns, round = 1, unitsLeft = 3,
                    onBack = {}, onOpenPlay = {}, onActivate = {}, onCoverChange = { _, _ -> },
                    onMove = { _, _, _, _ -> }, onRoundChange = {}, onWoundsChange = { _, _ -> },
                    battleMat = BattleMats.SCARIF, battleMatName = "Scarif",
                    onPickBattleMat = {})
            }
        }
    }
}
