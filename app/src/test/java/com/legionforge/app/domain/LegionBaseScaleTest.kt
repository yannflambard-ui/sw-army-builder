package com.legionforge.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Échelle officielle des socles Legion (CSR 2018 / AMG 06-2026) + emprise
 * d'escouade multi-figurines (legionStats.miniCount).
 */
class LegionBaseScaleTest {

    @Test
    fun diametres_officiels() {
        assertEquals(27f, LegionTabletopGeometry.baseDiameterMm("small"), 0.01f)
        assertEquals(50f, LegionTabletopGeometry.baseDiameterMm("medium"), 0.01f)
        assertEquals(70f, LegionTabletopGeometry.baseDiameterMm("large"), 0.01f)
        assertEquals(100f, LegionTabletopGeometry.baseDiameterMm("huge"), 0.01f)
    }

    @Test
    fun taille_depuis_carte() {
        // Infanterie -> small 27 mm
        assertEquals("small", LegionTabletopGeometry.baseSizeFor("trooper", "Rebel Troopers", 4))
        assertEquals("small", LegionTabletopGeometry.baseSizeFor("droid trooper", "B1 Battle Droids", 6))
        assertEquals("small", LegionTabletopGeometry.baseSizeFor("wookiee trooper", "Wookiee Warriors", 3))
        // Bikes / cavalerie / droïdes lourds -> medium 50 mm
        assertEquals("medium", LegionTabletopGeometry.baseSizeFor("repulsor vehicle", "74-Z Speeder Bikes", 2))
        assertEquals("medium", LegionTabletopGeometry.baseSizeFor("creature trooper", "Tauntaun Riders", 2))
        assertEquals("medium", LegionTabletopGeometry.baseSizeFor("heavy droid trooper", "Droidekas", 1))
        // AT-RT / emplacements -> large 70 mm
        assertEquals("large", LegionTabletopGeometry.baseSizeFor("ground vehicle", "AT-RT", 1))
        assertEquals("large", LegionTabletopGeometry.baseSizeFor("emplacement trooper", "E-Web Heavy Blaster Team", 1))
        // Gros véhicules -> huge 100 mm
        assertEquals("huge", LegionTabletopGeometry.baseSizeFor("ground vehicle", "AT-ST", 1))
        assertEquals("huge", LegionTabletopGeometry.baseSizeFor("repulsor vehicle", "T-47 Airspeeder", 1))
        assertEquals("huge", LegionTabletopGeometry.baseSizeFor("repulsor vehicle", "AAT Battle Tank", 1))
    }

    @Test
    fun emprise_escouade_multi_socles() {
        val one = LegionTabletopGeometry.squadFootprintRadiusMm("small", 1)
        val four = LegionTabletopGeometry.squadFootprintRadiusMm("small", 4)
        val six = LegionTabletopGeometry.squadFootprintRadiusMm("small", 6)
        // 1 figurine = rayon d'un socle unique (27/2 = 13,5 mm).
        assertEquals(13.5f, one, 0.01f)
        // 4 figurines = grille 2x2 de socles de 27 mm -> rayon englobant ~38,2 mm.
        assertTrue("4 socles doivent dépasser 1 socle", four > one * 2f)
        // 6 figurines = grille 3x2 -> encore plus large que 4.
        assertTrue("6 socles > 4 socles", six > four)
        // Escouade plus grosse avec socle plus grand -> emprise croissante.
        assertTrue(LegionTabletopGeometry.squadFootprintRadiusMm("large", 3) >
                   LegionTabletopGeometry.squadFootprintRadiusMm("small", 3))
    }

    @Test
    fun plateau_standard_6x3() {
        // Table standard Legion = 72" x 36" = 1828,8 x 914,4 mm.
        assertEquals(1828.8f, 72f * LegionTabletopGeometry.MM_PER_INCH, 0.01f)
        assertEquals(914.4f, 36f * LegionTabletopGeometry.MM_PER_INCH, 0.01f)
    }
}
