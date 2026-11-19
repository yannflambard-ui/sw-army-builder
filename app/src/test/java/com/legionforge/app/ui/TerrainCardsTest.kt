package com.legionforge.app.ui

import com.legionforge.app.ui.screens.TerrainCards
import com.legionforge.app.ui.screens.TerrainKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Garde-fou des cartes de terrain pré-construites (2 dioramas). */
class TerrainCardsTest {
    @Test
    fun `deux cartes avec paysage non vide`() {
        assertEquals(2, TerrainCards.ALL.size)
        TerrainCards.ALL.forEach { card ->
            assertTrue("${card.id} a du terrain", card.pieces.isNotEmpty())
            assertTrue("ids uniques", card.pieces.map { it.id }.toSet().size == card.pieces.size)
        }
    }

    @Test
    fun `couvert leger et lourd representes`() {
        val all = TerrainCards.ALL.flatMap { it.pieces }
        assertTrue("couvert leger (1) present", all.any { it.cover == 1 })
        assertTrue("couvert lourd (2) present", all.any { it.cover == 2 })
        assertTrue("sans couvert (0) present", all.any { it.cover == 0 })
    }

    @Test
    fun `dioramas contiennent leurs elements caracteristiques`() {
        val monument = TerrainCards.MONUMENT.pieces.map { it.kind }
        assertTrue("temple", monument.contains(TerrainKind.TEMPLE))
        assertTrue("rotonde", monument.contains(TerrainKind.ROTUNDA))
        assertTrue("marche", monument.contains(TerrainKind.MARKET))
        val ruins = TerrainCards.RUINS.pieces.map { it.kind }
        assertTrue("tour centrale", ruins.contains(TerrainKind.TOWER))
        assertTrue("ruines", ruins.contains(TerrainKind.TEMPLE))
    }

    @Test
    fun `pixels dans les bornes du tapis 36 pouces`() {
        val halfMm = 18f * 25.4f // 457.2 mm
        TerrainCards.ALL.flatMap { it.pieces }.forEach { p ->
            assertTrue("${p.id} x dans le tapis", kotlin.math.abs(p.xMm) <= halfMm)
            assertTrue("${p.id} y dans le tapis", kotlin.math.abs(p.yMm) <= halfMm)
        }
    }
}
