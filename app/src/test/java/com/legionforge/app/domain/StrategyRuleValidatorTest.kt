package com.legionforge.app.domain

import com.legionforge.app.data.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StrategyRuleValidatorTest {
    private fun card(id: String, system: GameSystem, kind: CardKind, points: Int, rank: LegionRank? = null, commander: Boolean = false, slots: List<ArmadaSlot> = emptyList(), bar: List<ArmadaSlot> = emptyList()) =
        CardDefinition(id, system, kind, id, points, "f", rank, upgradeSlots = slots, allowedUpgradeSlots = bar, commander = commander)
    private fun list(system: GameSystem, limit: Int, vararg entries: ListEntry) = BuilderList("l", "test", system, "f", limit, entries.toList())
    private fun entry(id: String, c: CardDefinition, parent: String? = null, chosen: ArmadaSlot? = null) = ListEntry(id, c, parent, 1, chosen)

    @Test fun legionQuotasAndPointsAreValidated() {
        val commander = card("cmd", GameSystem.LEGION_V2, CardKind.LEGION_UNIT, 100, LegionRank.COMMANDER)
        val corps = card("corps", GameSystem.LEGION_V2, CardKind.LEGION_UNIT, 100, LegionRank.CORPS)
        val valid = list(GameSystem.LEGION_V2, 1000, entry("1", commander), entry("2", corps), entry("3", corps), entry("4", corps))
        assertTrue(LegionV2Validator().validate(valid).valid)
        val invalid = list(GameSystem.LEGION_V2, 1000, entry("1", commander), entry("2", corps), entry("3", corps))
        assertFalse(LegionV2Validator().validate(invalid).valid)
    }

    @Test fun armadaSquadronCapCommanderAndChassisSlots() {
        val commander = card("commander", GameSystem.ARMADA_V15, CardKind.COMMANDER, 0, commander = true)
        val ship = card("munificent", GameSystem.ARMADA_V15, CardKind.ARMADA_SHIP, 100, bar = listOf(ArmadaSlot.TURBOLASERS))
        val squadron = card("squadron", GameSystem.ARMADA_V15, CardKind.ARMADA_SQUADRON, 134)
        val valid = list(GameSystem.ARMADA_V15, 400, entry("cmd", commander), entry("ship", ship))
        assertTrue(ArmadaV15Validator().validate(valid).valid)
        val exactlyThird = list(GameSystem.ARMADA_V15, 400, entry("cmd", commander), entry("ship", ship), entry("squad", squadron))
        assertTrue("134 points is allowed for a 400-point fleet", ArmadaV15Validator().validate(exactlyThird).valid)
        val tooMuch = exactlyThird.copy(entries = exactlyThird.entries + entry("squad2", squadron.copy(points = 1)))
        assertEquals("squadron_third", ArmadaV15Validator().validate(tooMuch).violations.first().code)
        val wrongUpgrade = card("upgrade", GameSystem.ARMADA_V15, CardKind.ARMADA_UPGRADE, 5, slots = listOf(ArmadaSlot.OFFENSIVE_RETROFIT))
        val badChassis = list(GameSystem.ARMADA_V15, 400, entry("cmd", commander), entry("ship", ship), entry("up", wrongUpgrade, "ship"))
        assertTrue(ArmadaV15Validator().validate(badChassis).violations.any { it.code == "invalid_upgrade_slot" })
    }

    @Test fun armadaPointsLimitEnforced() {
        val commander = card("commander", GameSystem.ARMADA_V15, CardKind.COMMANDER, 0, commander = true)
        val ship = card("ship", GameSystem.ARMADA_V15, CardKind.ARMADA_SHIP, 500)
        val over = list(GameSystem.ARMADA_V15, 400, entry("cmd", commander), entry("s1", ship))
        val r = ArmadaV15Validator().validate(over)
        assertFalse(r.valid)
        assertEquals("points_limit", r.violations.first().code)
    }

    @Test fun armadaDuplicateNonUniqueShipsAllowed() {
        val commander = card("commander", GameSystem.ARMADA_V15, CardKind.COMMANDER, 0, commander = true)
        val ship = card("obt", GameSystem.ARMADA_V15, CardKind.ARMADA_SHIP, 100)
        val two = list(GameSystem.ARMADA_V15, 400, entry("c", commander), entry("a", ship), entry("b", ship))
        assertTrue("deux vaisseaux indentiques non-uniques sont LEGAUX (jetons d'ID)", ArmadaV15Validator().validate(two).valid)
    }
}
