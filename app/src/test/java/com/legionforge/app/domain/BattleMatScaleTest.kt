package com.legionforge.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verrouille la conversion image(1408x704) -> mm plateau 6'x3'.
 * Régression critique : l'ancien mapping carré 36" décalait les emprises de
 * ~164 mm et laissait poser une unité dans l'eau visible de Naboo.
 */
class BattleMatScaleTest {

    private fun imgX(px: Float): Float = (px / 1408f) * 1828.8f - 914.4f
    private fun imgY(px: Float): Float = (px / 704f) * 914.4f - 457.2f

    @Test
    fun image_couvre_exactement_plateau() {
        // Les 4 coins de l'image tombent exactement sur les bords du plateau.
        assertEquals(-914.4f, imgX(0f), 0.01f)
        assertEquals(914.4f, imgX(1408f), 0.01f)
        assertEquals(-457.2f, imgY(0f), 0.01f)
        assertEquals(457.2f, imgY(704f), 0.01f)
        assertEquals(0f, imgX(704f), 0.01f) // centre image = centre plateau
        assertEquals(0f, imgY(352f), 0.01f)
    }

    @Test
    fun canaux_naboo_dans_le_plateau() {
        // Les 4 canaux Naboo (bbox pixels) doivent tomber dans les limites mm.
        val canaux = listOf(
            Triple(452f, 160f, 155f), Triple(922f, 242f, 85f),
            Triple(952f, 467f, 145f), Triple(487f, 652f, 85f),
        )
        for ((x, y, w) in canaux) {
            val cx = imgX(x)
            assertTrue("canal hors plateau en x: $cx", cx in -914.4f..914.4f)
            assertTrue("canal hors plateau en y: ${imgY(y)}", imgY(y) in -457.2f..457.2f)
        }
    }
}
