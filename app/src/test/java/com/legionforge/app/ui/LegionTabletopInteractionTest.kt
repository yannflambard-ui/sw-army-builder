package com.legionforge.app.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.legionforge.app.ui.screens.LegionPawn
import com.legionforge.app.ui.screens.LegionTabletopScreen
import com.legionforge.app.ui.theme.LegionForgeTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Garde-fou ANTI-NO-OP : vérifie que les callbacks de la barre d'outils sont
 * réellement appellés (un futur refactor qui les remplace par {} fait échouer ce test).
 * Les callbacks de la barre contextuelle (activer/couvert/blessures/déplacement)
 * nécessitent une sélection préalable via le Canvas ; ils sont couverts par les tests
 * JVM de logique pure (applyWounds, hitsAfterCover) et le câblage est vérifié en revue.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class LegionTabletopInteractionTest {
    @get:Rule val compose = createComposeRule()

    private fun pawn() = LegionPawn(
        id = "a1", name = "B1 Battle Droids", xMm = 0f, yMm = 0f,
        baseSize = "small", maxWounds = 4, wounds = 4, speed = 2,
    )

    @Test
    fun round_callback_fire() {
        var nextRound = -1
        compose.setContent {
            LegionForgeTheme {
                LegionTabletopScreen(
                    pawns = listOf(pawn()), round = 1,
                    onBack = {}, onOpenPlay = {}, onActivate = {},
                    onCoverChange = { _, _ -> }, onMove = { _, _, _, _ -> },
                    onRoundChange = { nextRound = it }, onWoundsChange = { _, _ -> },
                )
            }
        }
        compose.onNodeWithText("End round").performClick()
        assertTrue("onRoundChange(2) doit être appelé", nextRound == 2)
    }

    @Test
    fun back_callback_fire() {
        var back = false
        compose.setContent {
            LegionForgeTheme {
                LegionTabletopScreen(
                    pawns = listOf(pawn()), round = 1,
                    onBack = { back = true }, onOpenPlay = {}, onActivate = {},
                    onCoverChange = { _, _ -> }, onMove = { _, _, _, _ -> },
                    onRoundChange = {}, onWoundsChange = { _, _ -> },
                )
            }
        }
        compose.onNodeWithContentDescription("Back").performClick()
        assertTrue("onBack doit être appelé", back)
    }
}
