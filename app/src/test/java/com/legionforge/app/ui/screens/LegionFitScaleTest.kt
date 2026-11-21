package com.legionforge.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verrouille le fit initial du plateau 6'x3' sur LES DEUX axes (régression paysage). */
class LegionFitScaleTest {

    private val longPx = 72f * 25.4f * 2f   // 3657,6
    private val shortPx = 36f * 25.4f * 2f  // 1828,8

    @Test
    fun paysage_fit_limite_par_hauteur() {
        // 891x411 : fit largeur = 891/3657,6 = 0,2436 ; fit hauteur = 411/1828,8 = 0,2247.
        // Le min est la HAUTEUR — cadrer sur la seule largeur débordait de ~8 px haut/bas.
        val fit = legionFitScale(891, 411)
        val expected = (411f / shortPx) * 0.96f
        assertEquals(expected, fit, 0.001f)
        assertTrue("fit doit être < fit largeur", fit < (891f / longPx) * 0.96f)
    }

    @Test
    fun portrait_fit_limite_par_largeur() {
        // 411x891 : fit largeur = 411/3657,6 = 0,1124 < fit hauteur = 891/1828,8 = 0,487.
        val fit = legionFitScale(411, 891)
        val expected = (411f / longPx) * 0.96f
        assertEquals(expected, fit, 0.001f)
    }

    @Test
    fun plateau_entier_tient_dans_le_viewport() {
        for ((w, h) in listOf(891 to 411, 411 to 891, 1280 to 720)) {
            val fit = legionFitScale(w, h)
            val drawW = longPx * fit
            val drawH = shortPx * fit
            assertTrue("largeur déborde pour $w x $h", drawW <= w + 0.5f)
            assertTrue("hauteur déborde pour $w x $h", drawH <= h + 0.5f)
        }
    }
}
