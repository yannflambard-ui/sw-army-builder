package com.legionforge.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.sqrt

class ManeuverGeometryTest {
    private fun eq(x: Float, y: Float, h: Float, r: ManeuverResult) {
        assertEquals(x, r.xMm, 0.05f)
        assertEquals(y, r.yMm, 0.05f)
        assertEquals(h, r.headingDeg, 0.01f)
    }

    @Test fun ligneDroiteV2() {
        eq(0f, -80f, 0f, resolveManeuver2(0f, 0f, 0f, 2, listOf(0, 0)))
    }

    @Test fun unCoudeDroiteV1() {
        val s = 40f * 0.3826834f
        val c = 40f * 0.9238795f
        eq(s, -c, 22.5f, resolveManeuver2(0f, 0f, 0f, 1, listOf(1)))
    }

    @Test fun deuxCoudesOpposesV2() {
        val s = 40f * 0.3826834f
        val c = 40f * 0.9238795f
        eq(s, -c - 40f, 0f, resolveManeuver2(0f, 0f, 0f, 2, listOf(1, -1)))
    }

    @Test fun vitesseZeroResteSurPlace() {
        eq(0f, 0f, 0f, resolveManeuver2(0f, 0f, 0f, 0, listOf(2, -2)))
        eq(5f, 7f, 33f, resolveManeuver2(5f, 7f, 33f, 0, listOf(1)))
    }

    @Test fun cranMaxV1() {
        val v = 40f / sqrt(2f)
        eq(v, -v, 45f, resolveManeuver2(0f, 0f, 0f, 1, listOf(2)))
    }

    @Test fun articulationsAuDelaDeLaVitesseIgnorees() {
        val a = resolveManeuver2(0f, 0f, 0f, 2, listOf(0, 0))
        val b = resolveManeuver2(0f, 0f, 0f, 2, listOf(0, 0, 2, 2))
        eq(a.xMm, a.yMm, a.headingDeg, b)
    }

    @Test fun capEstCumulatif() {
        val r = resolveManeuver2(0f, 0f, 0f, 2, listOf(1, 1))
        assertEquals(45f, r.headingDeg, 0.01f)
    }

    @Test fun speedChartBornesParArticulation() {
        val chart = listOf(
            mapOf(1 to 2, 2 to 0, 3 to 0),
            mapOf(2 to 2, 3 to 0),
            mapOf(3 to 2),
        )
        assertEquals(2, maxClicks(chart, 1, 1))
        assertEquals(0, maxClicks(chart, 1, 2))
        assertEquals(2, maxClicks(chart, 2, 2))
        assertEquals(0, maxClicks(chart, 2, 3))
        assertEquals(2, maxClicks(chart, 3, 3))
        assertEquals(0, maxClicks(chart, 4, 1))
    }

    @Test fun polylineNombreDeSommets() {
        assertEquals(3, maneuverPolyline(0f, 0f, 0f, 2, listOf(0, 0)).size)
        assertEquals(1, maneuverPolyline(0f, 0f, 0f, 0, listOf(1)).size)
    }

    @Test fun polylineDernierPointEgaleArrivee() {
        val bends = listOf(1, -1, 2)
        val r = resolveManeuver2(0f, 0f, 0f, 3, bends)
        val last = maneuverPolyline(0f, 0f, 0f, 3, bends).last()
        assertEquals(r.xMm, last.first, 0.001f)
        assertEquals(r.yMm, last.second, 0.001f)
    }

    @Test fun hitRotatedBaseSuitLaRotation() {
        // Socle 43x71 (demi 21,5 / 35,5), centre (0,0), cap 0 = proue vers le haut.
        org.junit.Assert.assertTrue(hitRotatedBase(0f, -30f, 0f, 0f, 21.5f, 35.5f, 0f))
        org.junit.Assert.assertFalse(hitRotatedBase(30f, 0f, 0f, 0f, 21.5f, 35.5f, 0f))
        // Cap 90 : le socle est couché, sa longueur est selon x. Un point à +30 en x est DEDANS.
        org.junit.Assert.assertTrue(hitRotatedBase(30f, 0f, 0f, 0f, 21.5f, 35.5f, 90f))
        org.junit.Assert.assertFalse(hitRotatedBase(0f, -30f, 0f, 0f, 21.5f, 35.5f, 90f))
        // Cap 45 : un point sur l'axe du socle tourné est dedans.
        org.junit.Assert.assertTrue(hitRotatedBase(21.21f, -21.21f, 0f, 0f, 21.5f, 35.5f, 45f))
    }

    @Test fun reductionDeVitesseTronqueLesCoudes() {
        // RRG 1032 : vitesse réduite de 1. À v on ne compte que les v premières articulations.
        val bends = listOf(1, -1, 2)
        val v1 = resolveManeuver2(0f, 0f, 0f, 1, bends.take(1))
        val v2 = resolveManeuver2(0f, 0f, 0f, 2, bends.take(2))
        val v3 = resolveManeuver2(0f, 0f, 0f, 3, bends.take(3))
        assertEquals(22.5f, v1.headingDeg, 0.01f)
        assertEquals(0f, v2.headingDeg, 0.01f)
        assertEquals(45f, v3.headingDeg, 0.01f)
        // v1 et v2 ont des arrivées distinctes
        org.junit.Assert.assertFalse(v1.xMm == v2.xMm && v1.yMm == v2.yMm)
    }
}
