package com.legionforge.app.ui

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.legionforge.app.ui.screens.HomeScreen
import com.legionforge.app.ui.screens.SearchScreen
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
