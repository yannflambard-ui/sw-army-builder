package com.legionforge.app.ui

import com.legionforge.app.ui.screens.BattleMats
import com.legionforge.app.ui.screens.LegionTabletopScreen
import com.legionforge.app.ui.theme.LegionForgeTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * NON-RÉGRESSION bug zoom : la zone de déploiement Map Card doit couvrir la
 * MÊME surface plateau quel que soit le zoom (ancien bug : taille en mm passée
 * directement à drawRect -> la zone grandissait/rétrécissait au zoom).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w891dp-h411dp-mdpi")
class LegionDeployZoomCaptureTest {

    private fun capture(name: String, zoom: Float) {
        captureRoboImage("src/test/snapshots/$name.png") {
            LegionForgeTheme {
                LegionTabletopScreen(
                    pawns = emptyList(), round = 0, unitsLeft = 0,
                    onBack = {}, onOpenPlay = {}, onActivate = {}, onCoverChange = { _, _ -> },
                    onMove = { _, _, _, _ -> }, onRoundChange = {}, onWoundsChange = { _, _ -> },
                    battleMat = BattleMats.NONE,
                    forceScale = zoom,
                    forceRailCollapsed = true,
                    forceMapLayoutName = "Recover the Research",
                    forceShowMatLogic = true,
                    initialPan = androidx.compose.ui.geometry.Offset(445f, 205f))
            }
        }
    }

    @Test
    fun deploiement_zoom_24() = capture("legion-deploy-zoom-24", 0.24f)

    @Test
    fun deploiement_zoom_32() = capture("legion-deploy-zoom-32", 0.32f)
}
