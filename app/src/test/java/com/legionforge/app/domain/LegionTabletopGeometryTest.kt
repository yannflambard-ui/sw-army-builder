package com.legionforge.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests de la géométrie tabletop Legion — valeurs calculées à la main. */
class LegionTabletopGeometryTest {

    @Test fun `socles diametres`() {
        // Diamètres officiels CSR 2018 / AMG 06-2026.
        assertEquals(27f, LegionTabletopGeometry.baseDiameterMm("small"), 0.001f)
        assertEquals(50f, LegionTabletopGeometry.baseDiameterMm("medium"), 0.001f)
        assertEquals(70f, LegionTabletopGeometry.baseDiameterMm("large"), 0.001f)
        assertEquals(100f, LegionTabletopGeometry.baseDiameterMm("huge"), 0.001f)
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

    @Test fun `bandes de portee 6 pouces par segment`() {
        assertEquals(0, LegionTabletopGeometry.rangeBand(0f))
        assertEquals(1, LegionTabletopGeometry.rangeBand(25.4f))       // 1"
        assertEquals(1, LegionTabletopGeometry.rangeBand(152.4f))     // 6" → R1
        assertEquals(2, LegionTabletopGeometry.rangeBand(152.5f))     // >6" → R2
        assertEquals(2, LegionTabletopGeometry.rangeBand(304.8f))     // 12" → R2
        assertEquals(3, LegionTabletopGeometry.rangeBand(304.9f))     // >12" → R3
        assertEquals(3, LegionTabletopGeometry.rangeBand(457.2f))     // 18" → R3
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

    @Test fun `portee segment 6 pouces`() {
        assertEquals(1, LegionTabletopGeometry.rangeBand(38.1f)) // 1,5" toujours dans R1
    }
}

/** Socles : règle officielle rulebook:298-303 (liste blanche des petits socles). */
class LegionBaseSizeTest {
    @Test fun `troopers sur petits socles`() {
        listOf("trooper", "Clone Trooper", "droid trooper", "Wookiee Trooper").forEach {
            assertFalse("$it doit etre un petit socle", LegionTabletopGeometry.isNotchedBase(it))
        }
    }

    @Test fun `tout le reste est crante`() {
        listOf("repulsor vehicle", "ground vehicle", "emplacement trooper",
               "creature trooper", "heavy droid trooper", "mandalorian trooper", null).forEach {
            assertTrue("$it doit etre crante", LegionTabletopGeometry.isNotchedBase(it))
        }
    }
}

/** Couvert = pool de dés blancs (rulebook:2262-2272, exemple 2232-2237). */
class LegionCoverPoolTest {
    @Test fun `exemple source 3 hits heavy`() {
        val rolls = listOf("Hit", "Hit", "Hit", "Crit")
        // heavy (2) : Bloc OU Surge annulent. [Vide, Surge, Surge] => 2 Hits annulés.
        // Reste 1 Hit + 1 Crit = 2.
        assertEquals(2, LegionDiceRules.hitsAfterCover(rolls, 2, listOf("Vide", "Surge", "Surge")))
    }

    @Test fun `leger annule seulement sur bloc`() {
        val rolls = listOf("Hit", "Hit", "Crit")
        assertEquals(3, LegionDiceRules.hitsAfterCover(rolls, 1, listOf("Surge", "Vide")))
        assertEquals(2, LegionDiceRules.hitsAfterCover(rolls, 1, listOf("Bloc", "Vide")))
    }

    @Test fun `crits jamais annules`() {
        val rolls = listOf("Hit", "Crit", "Crit")
        assertEquals(2, LegionDiceRules.hitsAfterCover(rolls, 2, listOf("Bloc", "Bloc")))
    }

    @Test fun `cover zero n annule rien`() {
        assertEquals(4, LegionDiceRules.hitsAfterCover(
            listOf("Hit", "Hit", "Hit", "Crit"), 0, listOf("Bloc", "Vide", "Vide")))
    }

    @Test fun `anneaux de bandes 6 pouces`() {
        val r = { i: Int -> LegionTabletopGeometry.rangeRingPx(6 * i, 1f) }
        assertEquals(r(1) * 2, r(2), 0.01f)
        assertEquals(r(1) * 3, r(3), 0.01f)
        assertEquals(r(1) * 5, r(5), 0.01f)
    }

    @Test fun `pool auto un des par hit`() {
        val rolls = listOf("Hit", "Hit", "Crit", "Hit")
        assertEquals(3, LegionDiceRules.rollCoverPool(rolls).size)
    }
}

/** Blessures : bornage 0..max et defeated dérivé (garde-fou anti-régression). */
class LegionWoundsTest {
    @Test fun `blessures bornees et defeated`() {
        assertEquals(0f to true, LegionTabletopGeometry.applyWounds(4, 4, -1).let { it.first.toFloat() to it.second })
        val dead = LegionTabletopGeometry.applyWounds(4, 4, 0)
        assertEquals(0, dead.first); assertTrue(dead.second)
        val alive = LegionTabletopGeometry.applyWounds(4, 4, 1)
        assertEquals(1, alive.first); assertFalse(alive.second)
        val clamped = LegionTabletopGeometry.applyWounds(2, 4, 99)
        assertEquals(4, clamped.first); assertFalse(clamped.second)
    }
}
