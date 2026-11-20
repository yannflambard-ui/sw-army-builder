package com.legionforge.app.domain

import com.legionforge.app.ui.screens.TerrainKind
import com.legionforge.app.ui.screens.TerrainPiece
import com.legionforge.app.ui.screens.TerrainSurface
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LegionTerrainCollisionTest {

    private fun wall(
        x: Float, y: Float, w: Float = 100f, h: Float = 100f,
        surface: TerrainSurface = TerrainSurface.IMPASSABLE,
    ) = TerrainPiece("w", TerrainKind.WALL, x, y, w, h, 2, surface = surface)

    @Test
    fun `drop au centre d un mur IMPASSABLE est bloque`() {
        assertTrue(LegionTerrainCollision.isDropBlocked(0f, 0f, 12.5f, listOf(wall(0f, 0f))))
    }

    @Test
    fun `drop juste au bord avec chevauchement est bloque`() {
        // Mur [-50,50]. Socle centre x=55, r=12.5 -> deborde sur le mur.
        assertTrue(LegionTerrainCollision.isDropBlocked(55f, 0f, 12.5f, listOf(wall(0f, 0f))))
    }

    @Test
    fun `drop hors emprise est autorise`() {
        // Socle centre x=70, r=12.5 -> bord gauche 57.5 > 50, aucun contact.
        assertFalse(LegionTerrainCollision.isDropBlocked(70f, 0f, 12.5f, listOf(wall(0f, 0f))))
    }

    @Test
    fun `contact exact compte comme collision`() {
        // Socle centre x=62.5, r=12.5 -> bord gauche exactement 50 = bord du mur.
        assertTrue(LegionTerrainCollision.isDropBlocked(62.5f, 0f, 12.5f, listOf(wall(0f, 0f))))
    }

    @Test
    fun `terrain DIFFICULT est traversable jamais bloque`() {
        assertFalse(LegionTerrainCollision.isDropBlocked(
            0f, 0f, 12.5f, listOf(wall(0f, 0f, surface = TerrainSurface.DIFFICULT))
        ))
    }

    @Test
    fun `terrain OPEN est praticable jamais bloque`() {
        assertFalse(LegionTerrainCollision.isDropBlocked(
            0f, 0f, 12.5f, listOf(wall(0f, 0f, surface = TerrainSurface.OPEN))
        ))
    }

    @Test
    fun `liste vide autorise le drop`() {
        assertFalse(LegionTerrainCollision.isDropBlocked(0f, 0f, 12.5f, emptyList()))
    }

    @Test
    fun `plusieurs murs un seul suffit a bloquer`() {
        val murs = listOf(wall(-200f, 0f), wall(0f, 0f), wall(200f, 0f))
        assertTrue(LegionTerrainCollision.isDropBlocked(0f, 0f, 12.5f, murs))
        assertFalse(LegionTerrainCollision.isDropBlocked(100f, 0f, 12.5f, murs))
    }

    @Test
    fun `socle large chevauche plus tot qu un petit`() {
        // x=66 : small r=12.5 -> bord 53.5 (>50) libre ; large r=25 -> bord 41 bloque.
        assertFalse(LegionTerrainCollision.isDropBlocked(66f, 0f, 12.5f, listOf(wall(0f, 0f))))
        assertTrue(LegionTerrainCollision.isDropBlocked(66f, 0f, 25f, listOf(wall(0f, 0f))))
    }
}
