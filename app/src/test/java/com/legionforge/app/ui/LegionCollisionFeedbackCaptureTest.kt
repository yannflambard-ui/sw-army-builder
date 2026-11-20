package com.legionforge.app.ui

import com.legionforge.app.ui.screens.LegionPawn
import com.legionforge.app.ui.screens.LegionTabletopScreen
import com.legionforge.app.ui.theme.LegionForgeTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Capture du feedback de collision pion/bâtiment IMPASSABLE (halo rouge + toast). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-mdpi")
class LegionCollisionFeedbackCaptureTest {

    @Test
    fun collision_drop_bloque_feedback() {
        val pawns = listOf(
            LegionPawn("a1", "B1 Battle Droids", -60f, -80f, baseSize = "small", maxWounds = 4, wounds = 4, speed = 2),
            LegionPawn("a2", "General Grievous", 40f, -40f, baseSize = "medium", maxWounds = 5, wounds = 3, speed = 2),
        )
        captureRoboImage("src/test/snapshots/legion-collision-blocked.png") {
            LegionForgeTheme {
                LegionTabletopScreen(pawns = pawns, round = 2, unitsLeft = 2,
                    onBack = {}, onOpenPlay = {}, onActivate = {}, onCoverChange = { _, _ -> },
                    onMove = { _, _, _, _ -> }, onRoundChange = {}, onWoundsChange = { _, _ -> },
                    forceBlockedDrop = true)
            }
        }
    }
}
