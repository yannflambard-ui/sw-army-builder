package com.legionforge.app.ui

import com.legionforge.app.ui.screens.LegionPawn
import com.legionforge.app.ui.screens.formatPawnLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Garde-fou « info en 2s » : étiquettes COURTES (initiales + upgrade) et PV.
 * Un refactor du dedup Canvas avait supprimé toutes les étiquettes en 2.73.0.
 */
class LegionPawnLabelTest {
    private fun fixture() = listOf(
        LegionPawn("a1", "AT-TE", 0f, 0f, baseSize = "large", maxWounds = 8, wounds = 8),
        LegionPawn("a2", "Darth Vader (Commandant)", 40f, -40f, baseSize = "medium", maxWounds = 5, wounds = 3),
        LegionPawn("b1", "Clone Troopers", -40f, 90f, baseSize = "small", maxWounds = 4, wounds = 2, isOpp = true, cover = 1),
    )

    @Test
    fun `chaque pion produit une etiquette courte non vide`() {
        val labels = fixture().map(::formatPawnLabel)
        assertEquals("une étiquette par pion", fixture().size, labels.size)
        assertTrue("aucune étiquette vide", labels.all { it.isNotBlank() })
    }

    @Test
    fun `initiales et upgrade commandant`() {
        val labels = fixture().map(::formatPawnLabel)
        assertTrue("DV(c) présent", labels.any { it.startsWith("DV(c)") })
        assertTrue("PV explicite pour tous", labels.all { it.contains(" PV") })
    }

    @Test
    fun `couvert abrege`() {
        val labels = fixture().map(::formatPawnLabel)
        assertTrue("couvert abrégé", labels.any { it.contains("☂1") })
    }

    @Test
    fun `nom a un seul mot garde ses initiales`() {
        assertEquals("AT · 8/8 PV", formatPawnLabel(fixture()[0]))
    }
}
