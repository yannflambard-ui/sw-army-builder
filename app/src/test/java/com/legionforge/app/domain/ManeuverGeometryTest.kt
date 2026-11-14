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
}
