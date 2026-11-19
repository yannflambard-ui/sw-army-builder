package com.legionforge.app.ui

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.legionforge.app.domain.LegionMissionBuilder
import com.legionforge.app.ui.screens.LegionBattleCard
import com.legionforge.app.ui.screens.MissionBuilderSheetV2
import com.legionforge.app.ui.screens.loadBattleCards
import com.legionforge.app.ui.theme.LegionForgeTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Captures des 3 étapes du Round 0 refondu (cartes visibles des 2 côtés). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class MissionBuilderCaptureTest {
    private val MB = LegionMissionBuilder
    private val cards: List<LegionBattleCard> by lazy {
        loadBattleCards(ApplicationProvider.getApplicationContext<Application>())
    }
    private val deck by lazy {
        (cards.filter { it.category == "objective" }.take(3) +
            cards.filter { it.category == "secondary" }.take(3) +
            cards.filter { it.category == "advantage" }.take(3)).map { it.name }.toSet()
    }

    private fun render(name: String, revealed: Map<String, String>, blue: String?, modsLeft: Map<String, Int>, modsTurn: String) {
        captureRoboImage("src/test/snapshots/mission-builder-$name.png") {
            LegionForgeTheme {
                MissionBuilderSheetV2(
                    battleCards = cards, battleDeck = deck,
                    bluePlayer = blue,
                    blueRoll = if (blue != null) listOf("Hit", "Hit", "Blank", "Surge") to listOf("Hit", "Blank", "Blank", "Blank") else null,
                    missionRevealed = revealed, modsLeft = modsLeft, modsTurn = modsTurn,
                    onRollBlue = {}, onInitialReveal = { _ -> }, onMod = { _, _ -> },
                    onZoom = { _, _ -> }, onStart = {}, onClose = {},
                )
            }
        }
    }

    @Test fun etape1_blue() = render("step1-blue", emptyMap(), null, mapOf("own" to 2, "opp" to 2), "own")

    @Test fun etape2_revelation() = render("step2-reveal", emptyMap(), "own", mapOf("own" to 2, "opp" to 2), "own")

    @Test fun etape3_modifications() {
        val obj = cards.first { it.category == "objective" }.name
        val sec = cards.first { it.category == "secondary" }.name
        val advO = cards.first { it.category == "advantage" }.name
        val advP = cards.filter { it.category == "advantage" }[1].name
        val rev = mapOf(MB.KEY_OBJECTIVE to obj, MB.KEY_SECONDARY to sec,
            MB.KEY_ADV_OWN to advO, MB.KEY_ADV_OPP to advP)
        render("step3-modify", rev, "own", mapOf("own" to 2, "opp" to 2), "own")
    }

    @Test fun synthese() {
        val obj = cards.first { it.category == "objective" }.name
        val sec = cards.first { it.category == "secondary" }.name
        val advO = cards.first { it.category == "advantage" }.name
        val advP = cards.filter { it.category == "advantage" }[1].name
        val rev = mapOf(MB.KEY_OBJECTIVE to obj, MB.KEY_SECONDARY to sec,
            MB.KEY_ADV_OWN to advO, MB.KEY_ADV_OPP to advP)
        render("done", rev, "own", mapOf("own" to 0, "opp" to 0), "own")
    }
}
