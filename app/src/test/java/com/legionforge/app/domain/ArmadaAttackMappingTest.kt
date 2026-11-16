package com.legionforge.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArmadaAttackMappingTest {
    @Test fun squadronDiceFollowBsDataOrder() {
        // Valeurs réelles de catalog.json : TIE Fighter et X-wing Squadron.
        assertEquals(ArmadaRangeDice(blue = 3), ArmadaAttackMapping.squadronDice(listOf(3, 0, 0)))
        assertEquals(ArmadaRangeDice(blue = 1), ArmadaAttackMapping.squadronDice(listOf(1, 0, 0)))
        assertEquals(ArmadaRangeDice(blue = 4), ArmadaAttackMapping.squadronDice(listOf(4, 0, 0)))
        assertEquals(ArmadaRangeDice(red = 1), ArmadaAttackMapping.squadronDice(listOf(0, 1, 0)))
        assertEquals(ArmadaRangeDice(black = 3, blue = 1, red = 2), ArmadaAttackMapping.squadronDice(listOf(1, 2, 3)))
    }

    @Test fun shipDiceFollowShipOrderAndMissingValuesAreZero() {
        assertEquals(ArmadaRangeDice(black = 3, blue = 2, red = 1), ArmadaAttackMapping.shipDice(listOf(1, 2, 3)))
        assertEquals(ArmadaRangeDice(red = 2), ArmadaAttackMapping.shipDice(listOf(2)))
        assertEquals(ArmadaRangeDice(), ArmadaAttackMapping.squadronDice(emptyList()))
        assertEquals(ArmadaRangeDice(blue = 2), ArmadaAttackMapping.squadronDice(listOf(2)))
    }

    @Test fun declaredSectorSelectsOnlyItsOwnBattery() {
        val attack = mapOf(
            "front" to listOf(1, 2, 3), "right" to listOf(4, 5, 6),
            "left" to listOf(7, 8, 9), "rear" to listOf(10, 11, 12),
            "right_aux" to listOf(13, 14, 15), "left_aux" to listOf(16, 17, 18),
        )
        ShipSector.values().forEach { sector ->
            assertEquals(ArmadaAttackMapping.shipDice(attack.getValue(sector.key)),
                ArmadaAttackMapping.shipBattery(attack, sector))
        }
        assertEquals(ArmadaRangeDice(red = 4, blue = 5, black = 6),
            ArmadaAttackMapping.shipBattery(attack, ShipSector.RIGHT))
        assertNull(ArmadaAttackMapping.shipBattery(mapOf("rear" to listOf(9)), ShipSector.FRONT))
    }

    @Test fun headingNormalizationHandlesNegativeAndMultipleTurns() {
        val cases = mapOf(-1081.0 to 359.0, -720.0 to 0.0, -450.0 to 270.0,
            -1.0 to 359.0, 0.0 to 0.0, 360.0 to 0.0, 721.0 to 1.0, 1080.0 to 0.0)
        cases.forEach { (input, expected) ->
            assertEquals(expected, ArmadaAttackMapping.normalizeHeading(input), 0.0)
        }
        assertEquals(0.0, ArmadaAttackMapping.normalizeHeading(-Double.MIN_VALUE), 0.0)
    }

    @Test fun invalidDiceAndNonFiniteHeadingsAreRejected() {
        val invalid = listOf<() -> Unit>(
            { ArmadaAttackMapping.squadronDice(listOf(-1, 0, 0)) },
            { ArmadaAttackMapping.shipDice(listOf(0, -1, 0)) },
            { ArmadaAttackMapping.normalizeHeading(Double.NaN) },
            { ArmadaAttackMapping.normalizeHeading(Double.POSITIVE_INFINITY) },
            { ArmadaAttackMapping.normalizeHeading(Double.NEGATIVE_INFINITY) },
        )
        invalid.forEach { operation ->
            var rejected = false
            try { operation() } catch (_: IllegalArgumentException) { rejected = true }
            assertTrue(rejected)
        }
    }
}
