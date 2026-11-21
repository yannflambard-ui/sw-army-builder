package com.legionforge.app.ui

import com.legionforge.app.ui.screens.ArmadaBattleMats
import com.legionforge.app.ui.screens.FiringArcs
import com.legionforge.app.ui.screens.TabletopPawn
import com.legionforge.app.ui.screens.TabletopScreen
import com.legionforge.app.ui.theme.LegionForgeTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Capture du plateau Armada avec vraie image de tapis spatial (fond cosmétique). */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-mdpi")
class ArmadaBattleMatCaptureTest {

    private val pawns = listOf(
        TabletopPawn("a1", "Imperial I-Class", "large", -200f, 150f, 90f,
            maxSpeed = 2, hull = 8, shieldFront = 2, arcs = FiringArcs(270f, 45f, 45f)),
        TabletopPawn("b1", "MC80 Liberty", "huge", 180f, -150f, 270f, isOpp = true,
            maxSpeed = 1, hull = 10, shieldFront = 3, arcs = FiringArcs(270f, 45f, 45f)),
    )

    @Test
    fun armada_tapis_scarif() {
        captureRoboImage("src/test/snapshots/armada-battlemat-scarif.png") {
            LegionForgeTheme {
                TabletopScreen(pawns = pawns, sessionId = "test-scarif", round = 1, phase = "COMMAND",
                    onBack = {}, battleMat = ArmadaBattleMats.SCARIF, battleMatName = "Orbite de Scarif",
                    onPickBattleMat = {})
            }
        }
    }

    @Test
    fun armada_tapis_kaliida() {
        captureRoboImage("src/test/snapshots/armada-battlemat-kaliida.png") {
            LegionForgeTheme {
                TabletopScreen(pawns = pawns, sessionId = "test-kaliida", round = 2, phase = "SHIP",
                    onBack = {}, battleMat = ArmadaBattleMats.KALIIDA, battleMatName = "Nébuleuse de Kaliida",
                    onPickBattleMat = {})
            }
        }
    }
}
