package com.legionforge.app.domain

import com.legionforge.app.ui.screens.TerrainPiece
import com.legionforge.app.ui.screens.TerrainSurface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verrouille la géométrie polygonale des emprises (remplace les AABB sur bâtiments irréguliers). */
class TerrainPolygonTest {

    private val square = listOf(0f to 0f, 100f to 0f, 100f to 100f, 0f to 100f)

    @Test
    fun point_dans_et_hors_polygone() {
        assertTrue(TerrainPolygon.pointInPolygon(50f, 50f, square))
        assertFalse(TerrainPolygon.pointInPolygon(150f, 50f, square))
        assertFalse(TerrainPolygon.pointInPolygon(-10f, 50f, square))
    }

    @Test
    fun bord_inclus_dans_polygone() {
        assertTrue(TerrainPolygon.pointInPolygon(0f, 50f, square))
        assertTrue(TerrainPolygon.pointInPolygon(100f, 100f, square))
    }

    @Test
    fun concave_polygon_correct() {
        // L-shape : (0,0)-(100,0)-(100,40)-(40,40)-(40,100)-(0,100)
        val lshape = listOf(0f to 0f, 100f to 0f, 100f to 40f, 40f to 40f, 40f to 100f, 0f to 100f)
        assertTrue(TerrainPolygon.pointInPolygon(20f, 20f, lshape))       // bras horizontal
        assertTrue(TerrainPolygon.pointInPolygon(20f, 80f, lshape))       // bras vertical
        assertFalse(TerrainPolygon.pointInPolygon(80f, 80f, lshape))      // coin manquant
    }

    @Test
    fun disque_intersecte_polygone() {
        assertTrue(TerrainPolygon.circleIntersectsPolygon(50f, 50f, 10f, square))  // centre dedans
        assertTrue(TerrainPolygon.circleIntersectsPolygon(-5f, 50f, 10f, square))  // touche bord gauche
        assertFalse(TerrainPolygon.circleIntersectsPolygon(-20f, 50f, 10f, square)) // trop loin
        assertTrue(TerrainPolygon.circleIntersectsPolygon(110f, 110f, 20f, square)) // près du coin
    }

    @Test
    fun conversion_pixels_vers_mm_plateau() {
        assertEquals(-914.4f, TerrainPolygon.pxToMmX(0f), 0.01f)
        assertEquals(914.4f, TerrainPolygon.pxToMmX(1408f), 0.01f)
        assertEquals(-457.2f, TerrainPolygon.pxToMmY(0f), 0.01f)
        assertEquals(457.2f, TerrainPolygon.pxToMmY(704f), 0.01f)
    }

    @Test
    fun piece_avec_outline_convertit_en_mm() {
        val piece = TerrainPiece(
            id = "test", kind = com.legionforge.app.ui.screens.TerrainKind.BUNKER,
            xMm = 0f, yMm = 0f, wMm = 100f, hMm = 100f, cover = 2,
            surface = TerrainSurface.IMPASSABLE,
            outline = listOf(100f to 100f, 200f to 100f, 200f to 200f, 100f to 200f),
        )
        val mm = TerrainPolygon.outlineMm(piece)
        assertEquals(4, mm.size)
        // px 100 → x mm = (100/1408)*1828.8 - 914.4 = -784.6 (env.)
        assertTrue(mm[0].first < -700f && mm[0].first < mm[1].first)
    }

    @Test
    fun piece_sans_outline_reste_aabb() {
        val piece = TerrainPiece(
            id = "rect", kind = com.legionforge.app.ui.screens.TerrainKind.WALL,
            xMm = 0f, yMm = 0f, wMm = 100f, hMm = 100f, cover = 0,
        )
        assertTrue(TerrainPolygon.outlineMm(piece).isEmpty())
        assertEquals(null, TerrainPolygon.outlineBoundsMm(piece))
    }
}
