package com.legionforge.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Verrouille defenseSectorFor (audit gameplay V3 : angle inversé de 180°).
 * Cap 0 = proue vers le haut (-y). Un tir de face doit vider le bouclier AVANT,
 * pas l'arrière.
 */
class DefenseSectorTest {

    private fun defender(heading: Float = 0f) = TabletopPawn(
        id = "def", name = "Cible", xMm = 0f, yMm = 0f, headingDeg = heading, sizeClass = "medium",
    )

    private fun attackerAt(x: Float, y: Float) = TabletopPawn(
        id = "att", name = "Tireur", xMm = x, yMm = y, sizeClass = "medium",
    )

    @Test
    fun tir_du_nord_donne_front() {
        assertEquals("front", defenseSectorFor(defender(), attackerAt(0f, -100f)))
    }

    @Test
    fun tir_du_sud_donne_rear() {
        assertEquals("rear", defenseSectorFor(defender(), attackerAt(0f, 100f)))
    }

    @Test
    fun tir_de_lest_donne_right() {
        assertEquals("right", defenseSectorFor(defender(), attackerAt(100f, 0f)))
    }

    @Test
    fun tir_de_louest_donne_left() {
        assertEquals("left", defenseSectorFor(defender(), attackerAt(-100f, 0f)))
    }

    @Test
    fun cap_pivote_le_secteur() {
        // Cible tournée vers l'Est (cap 90) : un tir de l'Est est de face.
        assertEquals("front", defenseSectorFor(defender(90f), attackerAt(100f, 0f)))
        assertEquals("rear", defenseSectorFor(defender(90f), attackerAt(-100f, 0f)))
    }
}
