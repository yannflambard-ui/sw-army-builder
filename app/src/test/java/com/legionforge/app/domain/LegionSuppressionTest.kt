package com.legionforge.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verrou JVM des seuils Supprimé / Paniqué (rulebook 973-977). */
class LegionSuppressionTest {

    @Test
    fun seuils_et_actions() {
        // Courage 1 : 0 token = 2 actions, 1 = Supprimé (1 action), 2 = Paniqué (0).
        assertFalse(LegionSuppression.isSuppressed(0, 1))
        assertFalse(LegionSuppression.isPanicked(0, 1))
        assertEquals(2, LegionSuppression.actions(0, 1))

        // CAS LIMITE : 1 token = Supprimé mais PAS Paniqué.
        assertTrue(LegionSuppression.isSuppressed(1, 1))
        assertFalse(LegionSuppression.isPanicked(1, 1))
        assertEquals(1, LegionSuppression.actions(1, 1))

        assertTrue(LegionSuppression.isPanicked(2, 1))
        assertEquals(0, LegionSuppression.actions(2, 1))
        assertEquals(1, LegionSuppression.tokensRemovedOnPanic(2, 1))
        assertEquals(1, LegionSuppression.afterPanic(2, 1))
    }

    @Test
    fun courage_2_limite_et_panique() {
        assertTrue(LegionSuppression.isSuppressed(2, 2))
        assertFalse(LegionSuppression.isPanicked(2, 2))
        assertEquals(1, LegionSuppression.actions(2, 2))

        assertTrue(LegionSuppression.isSuppressed(3, 2))
        assertFalse(LegionSuppression.isPanicked(3, 2))
        assertEquals(1, LegionSuppression.actions(3, 2))

        assertTrue(LegionSuppression.isPanicked(4, 2))
        assertEquals(0, LegionSuppression.actions(4, 2))
        assertEquals(2, LegionSuppression.tokensRemovedOnPanic(4, 2))
        assertEquals(2, LegionSuppression.afterPanic(4, 2))
    }

    @Test
    fun courage_0_jamais_supprime_ni_panique() {
        assertFalse(LegionSuppression.isSuppressed(2, 0))
        assertFalse(LegionSuppression.isPanicked(2, 0))
        assertEquals(2, LegionSuppression.actions(2, 0))
        assertEquals(0, LegionSuppression.tokensRemovedOnPanic(2, 0))
    }
}
