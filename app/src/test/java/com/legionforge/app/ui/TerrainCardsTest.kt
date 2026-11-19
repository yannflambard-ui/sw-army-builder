package com.legionforge.app.ui

import com.legionforge.app.ui.screens.TerrainCards
import com.legionforge.app.ui.screens.TerrainKind
import com.legionforge.app.ui.screens.TerrainSize
import com.legionforge.app.ui.screens.TerrainSurface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Garde-fou des cartes de terrain pré-construites (6 dioramas + 8 planètes vectorielles). */
class TerrainCardsTest {
    @Test
    fun `quatorze cartes avec paysage non vide`() {
        assertEquals(14, TerrainCards.ALL.size)
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
    fun `douze planetes et dioramas presentes`() {
        val ids = TerrainCards.ALL.map { it.id }.toSet()
        listOf("endor", "tatooine", "naboo", "geonosis", "monument", "ruins",
            "hoth", "mustafar", "coruscant", "kashyyyk", "scarif", "jakku", "bespin", "dagobah")
            .forEach { assertTrue(it, ids.contains(it)) }
    }

    @Test
    fun `les dix nouveaux kinds sont dessines`() {
        val newKinds = listOf(TerrainKind.ICE_BASE, TerrainKind.GENERATOR, TerrainKind.LAVA_RIG,
            TerrainKind.PYLON, TerrainKind.SKYSCRAPER, TerrainKind.TREE_PLATFORM,
            TerrainKind.BUNKER, TerrainKind.TOWER_SPIRE, TerrainKind.HANGAR, TerrainKind.MOISTURE_VAPORIZER)
        val used = TerrainCards.ALL.flatMap { it.pieces }.map { it.kind }.toSet()
        newKinds.forEach { assertTrue("kind ${it.name} utilise", used.contains(it)) }
    }

    @Test
    fun `comptage DOC62 par carte une piece LARGE par planete`() {
        // Les cartes sont des interprétations thematiques (1 LARGE / carte), pas des
        // reproductions DOC62 (2/6/8/16-24 pour un battlefield complet de 2 layouts).
        val planetCards = listOf(TerrainCards.HOTH, TerrainCards.MUSTAFAR, TerrainCards.CORUSCANT,
            TerrainCards.KASHYYYK, TerrainCards.SCARIF, TerrainCards.JAKKU, TerrainCards.BESPIN, TerrainCards.DAGOBAH)
        planetCards.forEach { card ->
            assertEquals("${card.id} : 1 piece LARGE", 1, card.pieces.count { it.size == TerrainSize.LARGE })
            assertTrue("${card.id} : entre 5 et 7 pieces", card.pieces.size in 5..7)
        }
        val all = TerrainCards.ALL.flatMap { it.pieces }
        assertTrue("FILL_IN present", all.any { it.size == TerrainSize.FILL_IN })
        assertTrue("moitie A", all.any { it.half == 'A' })
        assertTrue("moitie B", all.any { it.half == 'B' })
        assertTrue("rotation 180", all.any { it.rotation == 180 })
    }

    @Test
    fun `area terrain bloque la LOS`() {
        // rulebook:787-793 — une area terrain (volume) bloque la ligne de vue.
        val rotonde = TerrainCards.MONUMENT.pieces.first { it.id == "rotonde" }
        assertTrue("rotonde blocksLos", rotonde.blocksLos)
    }

    @Test
    fun `area terrain secondaires bloquent la LOS`() {
        // rulebook:787-793 — toute area terrain (volume) bloque la ligne de vue.
        val igloo = TerrainCards.HOTH.pieces.first { it.id == "igloo" }
        val moss = TerrainCards.DAGOBAH.pieces.first { it.id == "moss" }
        assertTrue("igloo blocksLos", igloo.blocksLos)
        assertTrue("moss blocksLos", moss.blocksLos)
    }

    @Test
    fun `surfaces coherentes pour barricades et decors`() {
        // rulebook:749-753 — barricade = scatter OPEN, pas IMPASSABLE.
        val trench = TerrainCards.HOTH.pieces.first { it.id == "trench2" }
        assertEquals("trench OPEN", TerrainSurface.OPEN, trench.surface)
        // Bassins/flaques cover 0 = decor, aucune regle de deplacement inventee.
        val pool = TerrainCards.DAGOBAH.pieces.first { it.id == "pool" }
        assertEquals("pool OPEN", TerrainSurface.OPEN, pool.surface)
        val lava = TerrainCards.MUSTAFAR.pieces.first { it.id == "lava_pool" }
        assertEquals("lava_pool OPEN", TerrainSurface.OPEN, lava.surface)
    }

    @Test
    fun `fontaine Naboo hors de l emprise de la plaza`() {
        val plaza = TerrainCards.NABOO.pieces.first { it.id == "plaza" }
        val fontaine = TerrainCards.NABOO.pieces.first { it.id == "fontaine" }
        // plaza : [-80,80]^2 ; la fontaine doit etre hors de cette emprise.
        val plazaMin = plaza.xMm - plaza.wMm / 2f
        val plazaMax = plaza.xMm + plaza.wMm / 2f
        val fMin = fontaine.xMm - fontaine.wMm / 2f
        val fMax = fontaine.xMm + fontaine.wMm / 2f
        val overlapX = fMin < plazaMax && fMax > plazaMin
        val fMinY = fontaine.yMm - fontaine.hMm / 2f
        val fMaxY = fontaine.yMm + fontaine.hMm / 2f
        val plazaMinY = plaza.yMm - plaza.hMm / 2f
        val plazaMaxY = plaza.yMm + plaza.hMm / 2f
        val overlapY = fMinY < plazaMaxY && fMaxY > plazaMinY
        assertTrue("fontaine hors de l'emprise plaza", !(overlapX && overlapY))
    }

    @Test
    fun `rotation 180 presente et appliquee au rendu`() {
        val rotated = TerrainCards.ALL.flatMap { it.pieces }.filter { it.rotation == 180 }
        assertTrue("au moins une piece rotatee", rotated.isNotEmpty())
        assertEquals("tower_low rotatee", 180, TerrainCards.CORUSCANT.pieces.first { it.id == "tower_low" }.rotation)
    }

    @Test
    fun `six cartes d origine gardent palette nulle et kinds historiques`() {
        // Garde-fou anti-régression : le rendu historique (palette == null) doit rester
        // appliqué aux 6 cartes d'origine, sans Quoi le golden change silencieusement.
        val historiques = listOf(TerrainCards.MONUMENT, TerrainCards.RUINS, TerrainCards.ENDOR,
            TerrainCards.TATOOINE, TerrainCards.NABOO, TerrainCards.GEONOSIS)
        historiques.forEach { card ->
            assertTrue("${card.id} : palette nulle (rendu historique)", card.palette == null)
        }
        // Aucun des 10 nouveaux kinds ne doit apparaître dans les 6 cartes d'origine.
        val nouveaux = setOf("ICE_BASE", "GENERATOR", "LAVA_RIG", "PYLON", "SKYSCRAPER",
            "TREE_PLATFORM", "BUNKER", "TOWER_SPIRE", "HANGAR", "MOISTURE_VAPORIZER")
        historiques.flatMap { it.pieces }.forEach { p ->
            assertTrue("${p.id} : kind historique", !nouveaux.contains(p.kind.name))
        }
    }

    @Test
    fun `pixels dans les bornes du tapis 36 pouces`() {
        val halfMm = 18f * 25.4f // 457.2 mm
        TerrainCards.ALL.flatMap { it.pieces }.forEach { p ->
            assertTrue("${p.id} x dans le tapis", kotlin.math.abs(p.xMm) <= halfMm)
            assertTrue("${p.id} y dans le tapis", kotlin.math.abs(p.yMm) <= halfMm)
            // Empreinte entière dans le tapis.
            assertTrue("${p.id} emprise x", kotlin.math.abs(p.xMm) + p.wMm / 2f <= halfMm)
            assertTrue("${p.id} emprise y", kotlin.math.abs(p.yMm) + p.hMm / 2f <= halfMm)
        }
    }
}
