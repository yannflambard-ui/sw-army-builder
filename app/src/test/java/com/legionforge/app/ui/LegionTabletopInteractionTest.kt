package com.legionforge.app.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.geometry.Offset
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
    fun tap_sur_un_pion_ouvre_la_fiche_en_overlay() {
        // Le tap doit appeler onOpenPlay pour ouvrir la VRAIE fiche dépliante
        // (UnitDetailSheet en overlay dans le NavHost, plateau visible derrière).
        var openedId: String? = null
        compose.setContent {
            LegionForgeTheme {
                LegionTabletopScreen(
                    pawns = listOf(pawn()), round = 1,
                    onBack = {}, onOpenPlay = { openedId = it }, onActivate = {},
                    onCoverChange = { _, _ -> }, onMove = { _, _, _, _ -> },
                    onRoundChange = {}, onWoundsChange = { _, _ -> },
                )
            }
        }
        compose.onNodeWithTag("legion_board").performTouchInput {
            // Le pion est en (0,0) mm = centre du viewport ; tap court sans deplacement.
            down(Offset(center.x, center.y))
            up()
        }
        compose.waitForIdle()
        assertTrue("tap doit ouvrir la fiche dépliante du pion", openedId == "a1")
    }

    @Test
    fun pinch_ne_declenche_pas_la_fiche() {
        // Un pinch (2 doigts) ne doit PAS etre interprete comme un tap sur un pion.
        var opened = false
        compose.setContent {
            LegionForgeTheme {
                LegionTabletopScreen(
                    pawns = listOf(pawn()), round = 1,
                    onBack = {}, onOpenPlay = { opened = true }, onActivate = {},
                    onCoverChange = { _, _ -> }, onMove = { _, _, _, _ -> },
                    onRoundChange = {}, onWoundsChange = { _, _ -> },
                )
            }
        }
        compose.onNodeWithTag("legion_board").performTouchInput {
            down(0, Offset(center.x - 60f, center.y))
            down(1, Offset(center.x + 60f, center.y))
            moveTo(0, Offset(center.x - 100f, center.y))
            moveTo(1, Offset(center.x + 100f, center.y))
            up(0)
            up(1)
        }
        compose.waitForIdle()
        assertTrue("pinch ne doit pas ouvrir la fiche", !opened)
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
