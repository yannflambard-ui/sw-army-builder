package com.legionforge.app.ui

import com.legionforge.app.ui.screens.LegionPawn
import com.legionforge.app.ui.screens.formatPawnLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Garde-fou ANTI-RÉGRESSION « info en 2s » : les étiquettes nom + PV des pions
 * doivent TOUJOURS être produites (un refactor du dedup Canvas les avait toutes
 * supprimées en 2.73.0 sans que la CI ne le voie, les goldens ayant été re-rec).
 */
class LegionPawnLabelTest {
    private fun fixture() = listOf(
        LegionPawn("a1", "AT-TE", 0f, 0f, baseSize = "large", maxWounds = 8, wounds = 8),
        LegionPawn("a2", "General Grievous (Wheel Bike Warlord)", 40f, -40f, baseSize = "medium", maxWounds = 5, wounds = 3),
        LegionPawn("b1", "Clone Troopers", -40f, 90f, baseSize = "small", maxWounds = 4, wounds = 2, isOpp = true, cover = 1),
    )

    @Test
    fun `chaque pion de la fixture produit une etiquette non vide`() {
        val labels = fixture().map(::formatPawnLabel)
        assertEquals("une étiquette par pion", fixture().size, labels.size)
        assertTrue("aucune étiquette vide", labels.all { it.isNotBlank() })
    }

    @Test
    fun `etiquette contient nom court et PV explicites`() {
        val labels = fixture().map(::formatPawnLabel)
        assertTrue("AT-TE 8/8 PV présent", labels.any { it == "AT-TE · 8/8 PV" })
        assertTrue("nom court sans upgrade", labels.any { it.startsWith("General Grievous ·") && !it.contains("(") })
        assertTrue("PV explicite pour tous", labels.all { it.contains(" PV") })
        assertTrue("couvert abrégé", labels.any { it.contains("☂1") })
    }
}
