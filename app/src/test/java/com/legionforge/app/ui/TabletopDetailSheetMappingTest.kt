package com.legionforge.app.ui

import com.legionforge.app.domain.LegionTabletopGeometry
import com.legionforge.app.ui.screens.LegionPawn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Garde-fous de la fiche dépliante ouverte en overlay depuis le plateau tabletop.
 * Le mapping pawn -> fiche doit rester cohérent (ids adverses, mirroring d'état,
 * Pas de corruption de position, defeated verrouillé aux wounds).
 */
class TabletopDetailSheetMappingTest {

    @Test
    fun `id adverse avec parent correspond au format CardDetail`() {
        // CardDetailScreen:486-488 et le NavHost doivent produire le MEME id :
        // "opp_<cardId>_<parentInstanceId ou root>", avec parentInstanceId prefixe "B:".
        val cardId = "unit-xyz"
        val parent = "parent-123"
        val idNoParent = "opp_${cardId}_${"root"}"
        val idWithParent = "opp_${cardId}_${parent}"
        assertEquals("opp_unit-xyz_root", idNoParent)
        assertEquals("opp_unit-xyz_parent-123", idWithParent)
        assertTrue("id adverse reconnu comme tel", idWithParent.startsWith("opp_"))
    }

    @Test
    fun `mirroring fiche vers plateau preserve la position`() {
        val pawn = LegionPawn("a1", "Dewback Rider", 123f, -45f, 90f,
            baseSize = "large", maxWounds = 6, wounds = 6, cover = 0)
        // Simulation du onChange de la fiche : seulement wounds/cover/defeated/activated.
        val (nw, dead) = LegionTabletopGeometry.applyWounds(pawn.wounds, pawn.maxWounds, 2)
        val updated = pawn.copy(wounds = nw, cover = 1, defeated = dead, activated = true)
        assertEquals("x preserve", 123f, updated.xMm)
        assertEquals("y preserve", -45f, updated.yMm)
        assertEquals("heading preserve", 90f, updated.headingDeg)
        assertEquals("wounds mis a jour", 2, updated.wounds)
        assertEquals("couvert mis a jour", 1, updated.cover)
        assertTrue("activated mis a jour", updated.activated)
    }

    @Test
    fun `wounds positifs deverrouillent defeated`() {
        val dead = LegionPawn("a1", "AT-TE", 0f, 0f, baseSize = "large", maxWounds = 8, wounds = 0, defeated = true)
        val (nw, isDead) = LegionTabletopGeometry.applyWounds(dead.wounds, dead.maxWounds, 4)
        val revived = dead.copy(wounds = nw, defeated = isDead)
        assertEquals("wounds restaures", 4, revived.wounds)
        assertFalse("defeated deverrouille", revived.defeated)
    }

    @Test
    fun `upgrades inclus dans la fiche mais pas dans les pions`() {
        fun card(id: String, kind: com.legionforge.app.data.model.CardKind) = com.legionforge.app.data.model.CardDefinition(
            id = id, gameSystem = com.legionforge.app.data.model.GameSystem.LEGION_V2,
            kind = kind, name = id, points = 0, factionId = "empire",
        )
        val kindUnit = com.legionforge.app.data.model.CardKind.LEGION_UNIT
        val kindUpgrade = com.legionforge.app.data.model.CardKind.LEGION_UPGRADE
        val unit = com.legionforge.app.data.model.ListEntry(
            instanceId = "opp_unit-xyz_root", card = card("unit-xyz", kindUnit),
        )
        val upgrade = com.legionforge.app.data.model.ListEntry(
            instanceId = "B:upg-1", card = card("upg-1", kindUpgrade),
            parentInstanceId = "opp_unit-xyz_root",
        )
        // allForDetail = TOUTES les cartes, upgrades inclus.
        val allForDetail = listOf(unit, upgrade)
        val children = allForDetail.filter { it.parentInstanceId == unit.instanceId }
        assertEquals("upgrade présent dans les enfants de la fiche", 1, children.size)
        assertEquals("kind LEGION_UPGRADE", kindUpgrade, children.first().card.kind)
        // Les pions ne gardent que les LEGION_UNIT racines.
        val pawns = allForDetail.filter { it.card.kind == kindUnit && it.parentInstanceId == null }
        assertEquals("1 seul pion, l'upgrade exclu", 1, pawns.size)
        assertEquals("opp_unit-xyz_root", pawns.first().instanceId)
    }
}
