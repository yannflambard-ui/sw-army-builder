package com.legionforge.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests de la géométrie tabletop Legion — valeurs calculées à la main. */
class LegionTabletopGeometryTest {

    @Test fun `socles diametres`() {
        assertEquals(25f, LegionTabletopGeometry.baseDiameterMm("small"), 0.001f)
        assertEquals(40f, LegionTabletopGeometry.baseDiameterMm("medium"), 0.001f)
        assertEquals(50f, LegionTabletopGeometry.baseDiameterMm("large"), 0.001f)
    }

    @Test fun `pouces en mm`() {
        assertEquals(25.4f, LegionTabletopGeometry.MM_PER_INCH, 0.001f)
        assertEquals(1f, LegionTabletopGeometry.mmToInches(25.4f), 0.001f)
        assertEquals(6f, LegionTabletopGeometry.mmToInches(152.4f), 0.001f)
    }

    @Test fun `distance bord a bord`() {
        // Deux socles small (r=12.5) à 50mm centre-centre → bord-à-bord = 50-25 = 25mm
        assertEquals(25f, LegionTabletopGeometry.edgeDistanceMm(0f, 0f, 12.5f, 50f, 0f, 12.5f), 0.001f)
        // Chevauchement → 0
        assertEquals(0f, LegionTabletopGeometry.edgeDistanceMm(0f, 0f, 12.5f, 10f, 0f, 12.5f), 0.001f)
        // Contact exact → 0
        assertEquals(0f, LegionTabletopGeometry.edgeDistanceMm(0f, 0f, 12.5f, 25f, 0f, 12.5f), 0.001f)
    }

    @Test fun `bandes de portee`() {
        assertEquals(0, LegionTabletopGeometry.rangeBand(0f))
        assertEquals(1, LegionTabletopGeometry.rangeBand(25.4f))
        assertEquals(2, LegionTabletopGeometry.rangeBand(50.8f))
        assertEquals(6, LegionTabletopGeometry.rangeBand(152.4f))
        assertEquals(7, LegionTabletopGeometry.rangeBand(152.5f))
    }

    @Test fun `arrondi au demi pouce`() {
        assertEquals(1.5f, LegionTabletopGeometry.roundHalf(1.4f), 0.001f)
        assertEquals(2f, LegionTabletopGeometry.roundHalf(1.75f), 0.001f)
    }

    @Test fun `arc avant cap zero`() {
        // cap 0 = face vers le haut (y négatif). Cible devant (0,-100) → dans l'arc.
        assertTrue(LegionTabletopGeometry.inFrontArc(0f, 0f, 0f, 0f, -100f))
        // Cible derrière (0,+100) → hors arc.
        assertFalse(LegionTabletopGeometry.inFrontArc(0f, 0f, 0f, 0f, 100f))
        // Cible exactement à 45° → dans l'arc (inclusif).
        assertTrue(LegionTabletopGeometry.inFrontArc(0f, 0f, 0f, 100f, -100f))
        // Cible à 60° → hors arc.
        assertFalse(LegionTabletopGeometry.inFrontArc(0f, 0f, 0f, 173f, -100f))
    }

    @Test fun `arc avant cap est`() {
        // cap 90 = face vers l'est (+x). Cible à l'est → dans l'arc.
        assertTrue(LegionTabletopGeometry.inFrontArc(0f, 0f, 90f, 100f, 0f))
        // Cible au nord → hors arc (à 90° du facing).
        assertFalse(LegionTabletopGeometry.inFrontArc(0f, 0f, 90f, 0f, -100f))
    }

    @Test fun `bearing`() {
        assertEquals(0f, LegionTabletopGeometry.bearingDeg(0f, 0f, 0f, -100f), 0.001f)
        assertEquals(90f, LegionTabletopGeometry.bearingDeg(0f, 0f, 100f, 0f), 0.001f)
        assertEquals(180f, LegionTabletopGeometry.bearingDeg(0f, 0f, 0f, 100f), 0.001f)
        assertEquals(270f, LegionTabletopGeometry.bearingDeg(0f, 0f, -100f, 0f), 0.001f)
    }

    @Test fun `delta angulaire`() {
        assertEquals(10f, LegionTabletopGeometry.angleDelta(190f, 180f), 0.001f)
        assertEquals(-10f, LegionTabletopGeometry.angleDelta(170f, 180f), 0.001f)
        assertEquals(0f, LegionTabletopGeometry.angleDelta(0f, 360f), 0.001f)
    }
}


/** Tests des dés Legion — distributions vérifiées (FFG officiel, PAS le rulebook PDF). */
class LegionDiceRulesTest {
    @Test fun `attaque rouge 75 pourcent succes`() {
        val f = LegionDiceRules.faces("attack", "red")
        assertEquals(8, f.size)
        assertEquals(5, f.count { it == "Hit" })
        assertEquals(1, f.count { it == "Crit" })
        assertEquals(6, f.count { LegionDiceRules.isSuccess("attack", it) })
    }

    @Test fun `attaque noir et blanc`() {
        val b = LegionDiceRules.faces("attack", "black")
        assertEquals(3, b.count { it == "Hit" })
        assertEquals(3, b.count { it == "Vide" })
        val w = LegionDiceRules.faces("attack", "white")
        assertEquals(1, w.count { it == "Hit" })
        assertEquals(5, w.count { it == "Vide" })
    }

    @Test fun `defense d6 pas de noir`() {
        assertFalse(LegionDiceRules.legalColors("defense").contains("black"))
        assertEquals(6, LegionDiceRules.sides("defense"))
        val r = LegionDiceRules.faces("defense", "red")
        assertEquals(3, r.count { it == "Bloc" })
        assertEquals(1, r.count { it == "Surge" })
        val w = LegionDiceRules.faces("defense", "white")
        assertEquals(1, w.count { it == "Bloc" })
        assertEquals(4, w.count { it == "Vide" })
    }

    @Test fun `wounds total sante fois effectif`() {
        assertEquals(4, LegionTabletopGeometry.totalWounds(1, 4))
        assertEquals(2, LegionTabletopGeometry.totalWounds(2, 1))
    }

    @Test fun `portee segment inches entiers`() {
        assertEquals(2, LegionTabletopGeometry.rangeBand(38.1f))
    }
}
