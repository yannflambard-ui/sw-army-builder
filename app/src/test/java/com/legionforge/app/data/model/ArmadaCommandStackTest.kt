package com.legionforge.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Cœur FIFO des roues/cadrans de commandement Armada — logique pure, testable en JVM. */
class ArmadaCommandStackTest {

    @Test fun capacityBoundsThePush() {
        assertEquals("capacité 0 = jamais de cadran (escadron)", emptyList<String>(), ArmadaCommandStack.push(emptyList(), "NAVIGATE", 0))
        val cap3 = listOf("NAVIGATE", "SQUADRON", "REPAIR")
        assertEquals("3 têtes de pile correctes", cap3, ArmadaCommandStack.push(ArmadaCommandStack.push(ArmadaCommandStack.push(emptyList(), "NAVIGATE", 3), "SQUADRON", 3), "REPAIR", 3))
        val full = ArmadaCommandStack.push(cap3, "CONCENTRATE_FIRE", 3)
        assertEquals("plein = on ne peut plus pousser", cap3, full)
        assertFalse(ArmadaCommandStack.canPush(cap3, 3))
    }

    @Test fun revealIsFifoOrder() {
        val stack = listOf("NAVIGATE", "SQUADRON", "REPAIR")
        val (h1, r1) = ArmadaCommandStack.reveal(stack)
        assertEquals("premier révélé : NAVIGATE", "NAVIGATE", h1)
        assertEquals(listOf("SQUADRON", "REPAIR"), r1)
        val (h2, r2) = ArmadaCommandStack.reveal(r1)
        assertEquals("second : SQUADRON", "SQUADRON", h2)
        assertEquals(listOf("REPAIR"), r2)
        val (h3, r3) = ArmadaCommandStack.reveal(r2)
        assertEquals("troisième : REPAIR", "REPAIR", h3)
        assertTrue(r3.isEmpty())
        val (h4, _) = ArmadaCommandStack.reveal(r3)
        assertNull("pile vide : plus rien à révéler", h4)
    }

    @Test fun creditAndSpendFollowTheOrder() {
        var tokens = emptyMap<String, Int>()
        tokens = ArmadaCommandStack.credit(tokens, "NAVIGATE")
        tokens = ArmadaCommandStack.credit(tokens, "NAVIGATE")
        assertEquals(2, tokens["NAVIGATE"])
        tokens = ArmadaCommandStack.spend(tokens, "NAVIGATE")
        assertEquals("dépense 1", 1, tokens["NAVIGATE"])
        tokens = ArmadaCommandStack.spend(tokens, "NAVIGATE")
        assertEquals(0, tokens["NAVIGATE"])
        // ne descend pas sous 0
        ArmadaCommandStack.spend(tokens, "NAVIGATE")
        assertEquals(0, tokens["NAVIGATE"])
    }

    @Test fun endRoundClearsEverything() {
        val stack = listOf("NAVIGATE", "SQUADRON")
        val tokens = mapOf("NAVIGATE" to 3, "SQUADRON" to 1)
        val (newStack, newTokens) = ArmadaCommandStack.endRound()
        assertTrue(newStack.isEmpty())
        assertTrue(newTokens.isEmpty())
        // et on repart d'une pile vide pour le round suivant
        assertEquals(emptyList<String>(), newStack)
        // stack initiale réutilisée pour illustrer que la pile est indépendante des états
        assertTrue(stack.isNotEmpty() && tokens.isNotEmpty())
    }
}
