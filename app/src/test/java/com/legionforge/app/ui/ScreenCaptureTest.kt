package com.legionforge.app.ui

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.geometry.Offset
import com.github.takahirom.roborazzi.captureRoboImage
import com.legionforge.app.ui.screens.HomeScreen
import com.legionforge.app.ui.screens.SearchScreen
import com.legionforge.app.ui.screens.LegionPawn
import com.legionforge.app.ui.screens.LegionTabletopScreen
import com.legionforge.app.ui.screens.TerrainCards
import com.legionforge.app.ui.theme.LegionForgeTheme
import com.legionforge.app.ui.viewmodel.ArmyBuilderViewModel
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Captures réelles (sans émulateur ni appareil) des écrans clés pour valider le rendu
 * « info en 2s » et le contraste. Roborazzi 1.47 + Robolectric 4.13, dark theme figé.
 *
 * Générer : ./gradlew :app:recordRoborazziDebug (1er run en ligne), puis
 *           ./gradlew :app:testDebugUnitTest -Proborazzi.test.record=true
 * Vérifier en CI : ./gradlew :app:verifyRoborazziDebug
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenCaptureTest {

    private fun vm(): ArmyBuilderViewModel =
        ArmyBuilderViewModel(ApplicationProvider.getApplicationContext<Application>())

    @Test
    fun home_411x891() {
        captureRoboImage("src/test/snapshots/home-411x891.png") {
            LegionForgeTheme {
                HomeScreen(
                    onNewList = {}, onOpenList = {}, onSettings = {},
                    onSearch = {}, onWiki = {}, vm = vm()
                )
            }
        }
    }

    @Test
    fun search_411x891() {
        captureRoboImage("src/test/snapshots/search-411x891.png") {
            LegionForgeTheme {
                SearchScreen(onBack = {}, onCardClick = {}, vm = vm())
            }
        }
    }
}

/** Petit écran (phone compact) : mêmes écrans, autre densité/taille. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h640dp-mdpi")
class ScreenCaptureSmallTest {

    private fun vm(): ArmyBuilderViewModel =
        ArmyBuilderViewModel(ApplicationProvider.getApplicationContext<Application>())

    @Test
    fun home_360x640() {
        captureRoboImage("src/test/snapshots/home-360x640.png") {
            LegionForgeTheme {
                HomeScreen(
                    onNewList = {}, onOpenList = {}, onSettings = {},
                    onSearch = {}, onWiki = {}, vm = vm()
                )
            }
        }
    }

    @Test
    fun search_360x640() {
        captureRoboImage("src/test/snapshots/search-360x640.png") {
            LegionForgeTheme {
                SearchScreen(onBack = {}, onCardClick = {}, vm = vm())
            }
        }
    }
}


/** Plateau tabletop Legion : socles, arc frontal, barre contextuelle, dés. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class LegionTabletopCaptureTest {
    @Test
    fun legion_tabletop_411x891() {
        val pawns = listOf(
            LegionPawn("a1", "B1 Battle Droids", -60f, -80f, baseSize = "small", maxWounds = 4, wounds = 4, speed = 2, imagePath = "legion_tokens/legion_v2_lhq2_b1-battle-droids.webp"),
            LegionPawn("a2", "General Grievous", 40f, -40f, baseSize = "medium", maxWounds = 5, wounds = 3, speed = 2, canRotate = true, imagePath = "legion_tokens/legion_v2_lhq2_general-grievous-wheel-bike-warlord.webp"),
            LegionPawn("a3", "AT-TE", 90f, -120f, baseSize = "large", maxWounds = 8, wounds = 8, speed = 1, canRotate = true),
            LegionPawn("b1", "Clone Troopers", -40f, 90f, baseSize = "small", maxWounds = 4, wounds = 2, speed = 2, isOpp = true, cover = 1),
            LegionPawn("b2", "Captain Rex", 60f, 60f, baseSize = "medium", maxWounds = 4, wounds = 4, speed = 2, isOpp = true),
        )
        captureRoboImage("src/test/snapshots/legion-tabletop-411x891.png") {
            LegionForgeTheme {
                LegionTabletopScreen(pawns = pawns, round = 2, unitsLeft = 5,
                    onBack = {}, onOpenPlay = {}, onActivate = {}, onCoverChange = { _, _ -> },
                    onMove = { _, _, _, _ -> }, onRoundChange = {}, onWoundsChange = { _, _ -> })
            }
        }
    }
}


/** Plateau Legion sur petit écran (360x640). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h640dp-mdpi")
class LegionTabletopCaptureSmallTest {
    @Test
    fun legion_tabletop_360x640() {
        val pawns = listOf(
            LegionPawn("a1", "B1 Battle Droids", -40f, -50f, baseSize = "small", maxWounds = 4, wounds = 4, speed = 2),
            LegionPawn("a2", "General Grievous", 30f, -30f, baseSize = "large", maxWounds = 5, wounds = 3, speed = 2, canRotate = true),
            LegionPawn("b1", "Clone Troopers", -20f, 60f, baseSize = "small", maxWounds = 4, wounds = 2, speed = 2, isOpp = true, cover = 1),
        )
        captureRoboImage("src/test/snapshots/legion-tabletop-360x640.png") {
            LegionForgeTheme {
                LegionTabletopScreen(pawns = pawns, round = 1, unitsLeft = 3,
                    onBack = {}, onOpenPlay = {}, onActivate = {}, onCoverChange = { _, _ -> },
                    onMove = { _, _, _, _ -> }, onRoundChange = {}, onWoundsChange = { _, _ -> })
            }
        }
    }
}

/**
 * Garde-fou ANTI-CRASH : un pan extrême pousse les badges OBJ et les légendes
 * hors du canvas. Sans `size` explicite, drawText calcule maxWidth < 0 et
 * lève `maxWidth(-35) must be >= than minWidth(0)` (deuxième occurrence de ce
 * bug après le `-12` en 2.70.0). Ce test échoue si la régression revient.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-mdpi")
class LegionTabletopOffscreenTest {
    @Test
    fun pan_extreme_ne_crashe_pas() {
        val pawns = listOf(
            LegionPawn("a1", "B1 Battle Droids", -60f, -80f, baseSize = "small", maxWounds = 4, wounds = 4),
            LegionPawn("b1", "Captain Rex", 60f, 60f, baseSize = "medium", maxWounds = 4, wounds = 4, isOpp = true),
        )
        // Capture avec un pan qui envoie tout le tapis HORS de l'écran.
        captureRoboImage("src/test/snapshots/legion-tabletop-offscreen.png") {
            LegionForgeTheme {
                LegionTabletopScreen(pawns = pawns, round = 3, unitsLeft = 2,
                    onBack = {}, onOpenPlay = {}, onActivate = {}, onCoverChange = { _, _ -> },
                    onMove = { _, _, _, _ -> }, onRoundChange = {}, onWoundsChange = { _, _ -> },
                    initialPan = Offset(9000f, 9000f))
            }
        }
    }
}

/** Plateau Legion avec carte de terrain "Place monumentale" + bandeau missions. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-mdpi")
class LegionTabletopTerrainCaptureTest {
    @Test
    fun legion_tabletop_avec_terrain() {
        val pawns = listOf(
            LegionPawn("a1", "B1 Battle Droids", -60f, -80f, baseSize = "small", maxWounds = 4, wounds = 4),
            LegionPawn("a2", "AT-TE", 90f, -120f, baseSize = "large", maxWounds = 8, wounds = 8),
            LegionPawn("b1", "Captain Rex", 60f, 60f, baseSize = "medium", maxWounds = 4, wounds = 4, isOpp = true),
        )
        captureRoboImage("src/test/snapshots/legion-tabletop-terrain.png") {
            LegionForgeTheme {
                LegionTabletopScreen(pawns = pawns, round = 2, unitsLeft = 3,
                    onBack = {}, onOpenPlay = {}, onActivate = {}, onCoverChange = { _, _ -> },
                    onMove = { _, _, _, _ -> }, onRoundChange = {}, onWoundsChange = { _, _ -> },
                    terrainCard = TerrainCards.MONUMENT, victoryPoints = 6)
            }
        }
    }
}
