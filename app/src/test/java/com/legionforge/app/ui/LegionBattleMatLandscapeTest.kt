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

/** Capture du plateau Legion en PAYSAGE (qualifiers 891x411) — gain de netteté du tapis 2:1. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w891dp-h411dp-mdpi")
class LegionBattleMatLandscapeTest {

    private val pawns = listOf(
        LegionPawn("a1", "B1 Battle Droids", -60f, -80f, baseSize = "small", maxWounds = 4, wounds = 4, speed = 2, miniCount = 4),
        LegionPawn("a2", "General Grievous", 40f, -40f, baseSize = "medium", maxWounds = 5, wounds = 3, speed = 2),
        LegionPawn("b1", "Clone Troopers", -40f, 90f, baseSize = "small", maxWounds = 4, wounds = 2, speed = 2, isOpp = true, cover = 1, miniCount = 3),
    )

    @Test
    fun tatooine_paysage_rail_replie() {
        captureRoboImage("src/test/snapshots/legion-paysage-rail-replie.png") {
            LegionForgeTheme {
                LegionTabletopScreen(pawns = pawns, round = 1, unitsLeft = 3,
                    onBack = {}, onOpenPlay = {}, onActivate = {}, onCoverChange = { _, _ -> },
                    onMove = { _, _, _, _ -> }, onRoundChange = {}, onWoundsChange = { _, _ -> },
                    battleMat = BattleMats.TATOOINE, battleMatName = "Tatooine",
                    onPickBattleMat = {}, forceRailCollapsed = true)
            }
        }
    }

    /** Map Card en PAYSAGE (le vrai rendu store) : zones + POI au fit réel, cadrage complet. */
    @Test
    fun mapcard_recover_paysage() {
        captureRoboImage("src/test/snapshots/legion-mapcard-recover-paysage.png") {
            LegionForgeTheme {
                LegionTabletopScreen(pawns = pawns, round = 1, unitsLeft = 3,
                    onBack = {}, onOpenPlay = {}, onActivate = {}, onCoverChange = { _, _ -> },
                    onMove = { _, _, _, _ -> }, onRoundChange = {}, onWoundsChange = { _, _ -> },
                    battleMat = BattleMats.TATOOINE, battleMatName = "Tatooine",
                    onPickBattleMat = {}, forceMapLayoutName = "Recover the Research",
                    forceRailCollapsed = true)
            }
        }
    }

    @Test
    fun tatooine_paysage() {
        captureRoboImage("src/test/snapshots/legion-battlemat-paysage.png") {
            LegionForgeTheme {
                LegionTabletopScreen(pawns = pawns, round = 1, unitsLeft = 3,
                    onBack = {}, onOpenPlay = {}, onActivate = {}, onCoverChange = { _, _ -> },
                    onMove = { _, _, _, _ -> }, onRoundChange = {}, onWoundsChange = { _, _ -> },
                    battleMat = BattleMats.TATOOINE, battleMatName = "Tatooine",
                    onPickBattleMat = {})
            }
        }
    }
}
