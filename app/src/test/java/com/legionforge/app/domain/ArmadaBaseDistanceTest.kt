package com.legionforge.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class ArmadaBaseDistanceTest {
    private val tolerance = 1e-8
    private fun ship(x: Double, y: Double, width: Double = 20.0, length: Double = 40.0, heading: Double = 0.0) =
        ArmadaBase.Ship(x, y, width, length, heading)
    private fun squad(x: Double, y: Double, radius: Double = 5.0) = ArmadaBase.Squadron(x, y, radius)
    private fun distance(expected: Double, a: ArmadaBase, b: ArmadaBase) {
        assertEquals(expected, minimumBaseDistanceMm(a, b), tolerance)
        assertEquals(expected, minimumBaseDistanceMm(b, a), tolerance)
    }

    @Test fun rectanglesAlignedSeparatedAndDiagonal() {
        distance(10.0, ship(0.0, 0.0), ship(30.0, 0.0))
        distance(5.0, ship(0.0, 0.0), ship(23.0, 44.0)) // gap 3 x 4
    }

    @Test fun rectanglesRotatedNinetyAndFortyFiveDegrees() {
        distance(10.0, ship(0.0, 0.0), ship(40.0, 0.0, heading = 90.0))
        // Square's 45° vertex lies sqrt(200) mm from its center.
        distance(30.0 - 10.0 - sqrt(200.0), ship(0.0, 0.0, 20.0, 20.0),
            ship(30.0, 0.0, 20.0, 20.0, 45.0))
    }

    @Test fun rectanglesCrossContainedAndTouching() {
        distance(0.0, ship(0.0, 0.0, 10.0, 100.0), ship(0.0, 0.0, 10.0, 100.0, 90.0))
        distance(0.0, ship(0.0, 0.0, 100.0, 100.0), ship(0.0, 0.0, 10.0, 10.0, 37.0))
        distance(0.0, ship(0.0, 0.0), ship(20.0, 0.0))
    }

    @Test fun squadronsDistanceContactOverlapAndShipCorner() {
        distance(20.0, squad(0.0, 0.0), squad(30.0, 0.0))
        distance(0.0, squad(0.0, 0.0), squad(10.0, 0.0))
        distance(0.0, squad(0.0, 0.0), squad(1.0, 1.0))
        distance(0.0, ship(0.0, 0.0), squad(0.0, 0.0))
        distance(5.0, ship(0.0, 0.0), squad(16.0, 28.0)) // sqrt(6²+8²)-5
        distance(5.0, ship(0.0, 0.0, heading = 90.0), squad(30.0, 0.0))
    }

    @Test fun thresholdsInclusiveAndJustBeyond() {
        val bag = ArmadaRangeDice(black = 2, blue = 3, red = 4)
        assertEquals(bag, filterPossibleShipAttackDice(bag, ARMADA_BLACK_RANGE_MM))
        assertEquals(ArmadaRangeDice(blue = 3, red = 4), filterPossibleShipAttackDice(bag, ARMADA_BLACK_RANGE_MM + 0.001))
        assertEquals(ArmadaRangeDice(blue = 3, red = 4), filterPossibleShipAttackDice(bag, ARMADA_BLUE_RANGE_MM))
        assertEquals(ArmadaRangeDice(red = 4), filterPossibleShipAttackDice(bag, ARMADA_BLUE_RANGE_MM + 0.001))
        assertEquals(ArmadaRangeDice(red = 4), filterPossibleShipAttackDice(bag, ARMADA_RED_RANGE_MM))
        assertEquals(ArmadaRangeDice(), filterPossibleShipAttackDice(bag, ARMADA_RED_RANGE_MM + 0.001))
    }

    @Test fun captureOutsideRangeUsesEdgesNotCenters() {
        val attacker = ship(0.0, 0.0, 43.0, 71.0)
        val target = ship(400.0, 0.0, 43.0, 71.0)
        val minimum = minimumBaseDistanceMm(attacker, target)
        assertEquals(357.0, minimum, tolerance)
        assertEquals(ArmadaRangeDice(), filterPossibleShipAttackDice(ArmadaRangeDice(1, 2, 3), minimum))
        // Nearer centers can be beyond long range while the edges are not.
        val reachableEdges = minimumBaseDistanceMm(attacker, ship(340.0, 0.0, 43.0, 71.0))
        assertEquals(297.0, reachableEdges, tolerance)
        assertEquals(ArmadaRangeDice(red = 3), filterPossibleShipAttackDice(ArmadaRangeDice(1, 2, 3), reachableEdges))
    }

    @Test fun entireBaseDistanceCannotValidateDeclaredSector() {
        val a = ship(0.0, 0.0)
        val b = ship(0.0, 100.0)
        // Whole-base gap 60; opposite ends of these socles are 140 mm apart.
        assertEquals(60.0, minimumBaseDistanceMm(a, b), tolerance)
        assertEquals(ArmadaRangeDice(black = 1), filterPossibleShipAttackDice(ArmadaRangeDice(black = 1), 60.0))
        assertTrue(140.0 > ARMADA_BLACK_RANGE_MM)
    }

    @Test fun invalidInputIsRejected() {
        val failures = listOf<() -> Unit>(
            { ship(0.0, 0.0, width = 0.0) },
            { squad(0.0, 0.0, radius = -1.0) },
            { ship(Double.NaN, 0.0) },
            { ArmadaRangeDice(red = -1) },
            { filterPossibleShipAttackDice(ArmadaRangeDice(), Double.NaN) },
            { filterPossibleShipAttackDice(ArmadaRangeDice(), -1.0) },
        )
        failures.forEach { operation ->
            var rejected = false
            try { operation() } catch (_: IllegalArgumentException) { rejected = true }
            assertTrue(rejected)
        }
    }
}
