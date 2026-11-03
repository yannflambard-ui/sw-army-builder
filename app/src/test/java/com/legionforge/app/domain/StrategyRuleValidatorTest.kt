package com.legionforge.app.domain

import com.legionforge.app.data.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StrategyRuleValidatorTest {
    private fun card(id: String, system: GameSystem, kind: CardKind, points: Int, rank: LegionRank? = null, commander: Boolean = false, slots: List<ArmadaSlot> = emptyList(), bar: List<ArmadaSlot> = emptyList(), unique: Boolean = false, shipStats: String? = null, flotilla: Boolean = false) =
        CardDefinition(id, system, kind, id, points, "f", rank, upgradeSlots = slots, allowedUpgradeSlots = bar, commander = commander, unique = unique, shipStats = shipStats, flotilla = flotilla)
    private fun list(system: GameSystem, limit: Int, vararg entries: ListEntry) = BuilderList("l", "test", system, "f", limit, entries.toList())
    private fun entry(id: String, c: CardDefinition, parent: String? = null, chosen: ArmadaSlot? = null) = ListEntry(id, c, parent, 1, chosen)
    private fun aceStats(vararg tokens: String) = "{\"defenseTokens\":[\"${tokens.joinToString("\",\"")}\"]}"

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

    @Test fun legionBattleForceOverridesStandardQuotas() {
        val bf = BattleForceConfig(
            id = "bf:test", name = "Test Force", factionId = "f",
            quotas = mapOf(LegionRank.COMMANDER to (1..2), LegionRank.CORPS to (0..0), LegionRank.SUPPORT to (0..8))
        )
        val commander = card("cmd", GameSystem.LEGION_V2, CardKind.LEGION_UNIT, 100, LegionRank.COMMANDER)
        val support = card("sup", GameSystem.LEGION_V2, CardKind.LEGION_UNIT, 90, LegionRank.SUPPORT)
        val armed = list(GameSystem.LEGION_V2, 800, entry("1", commander), entry("2", support), entry("3", support))
        // Quotas standard : 0 troupes hors 3..6 -> invalide, même sans battleforce.
        assertFalse("sans Battle Force, 0 troupes est invalide", LegionV2Validator().validate(armed).valid)
        // Battle Force : CORPS 0..0, Soutiens 0..8 -> valide (quotas BF remplacent les standard).
        assertTrue("avec Battle Force, 0 troupes + 3 soutiens est valide", LegionV2Validator().validate(armed, bf).valid)
        // Faction hors Battle Force -> violation dédiée.
        val wrong = list(GameSystem.LEGION_V2, 800,
            entry("1", commander.copy(factionId = "g")), entry("2", support.copy(factionId = "g")), entry("3", support.copy(factionId = "g")))
        assertTrue("Battle Force refusée pour une faction étrangère",
            LegionV2Validator().validate(wrong, bf).violations.any { it.code == "battleforce_faction" })
    }

    @Test fun armadaAceLimitOneAcePerHundredFleetPoints() {
        val commander = card("commander", GameSystem.ARMADA_V15, CardKind.COMMANDER, 0, commander = true)
        val ship = card("ship", GameSystem.ARMADA_V15, CardKind.ARMADA_SHIP, 100)
        fun ace(i: Int) = card("ace$i", GameSystem.ARMADA_V15, CardKind.ARMADA_SQUADRON, 8, unique = true, shipStats = aceStats("BRACE"))
        val generic = card("gen", GameSystem.ARMADA_V15, CardKind.ARMADA_SQUADRON, 8, unique = false, shipStats = aceStats("SCOUT"))
        // 400 pts -> cap de 4 as ; l'escadron générique ne compte pas.
        val four = list(GameSystem.ARMADA_V15, 400, entry("c", commander), entry("s", ship),
            entry("a1", ace(1)), entry("a2", ace(2)), entry("a3", ace(3)), entry("a4", ace(4)), entry("g", generic))
        assertTrue("4 as (cap 4) + 1 escadron générique = flotte OK", ArmadaV15Validator().validate(four).valid)
        val five = four.copy(entries = four.entries + entry("a5", ace(5)))
        assertEquals("ace_limit", ArmadaV15Validator().validate(five).violations.first().code)
    }

    @Test fun armadaCommanderForbiddenOnFlotilla() {
        val commander = card("cmdr", GameSystem.ARMADA_V15, CardKind.COMMANDER, 20, commander = true, slots = listOf(ArmadaSlot.COMMANDER))
        val flotilla = card("gozanti", GameSystem.ARMADA_V15, CardKind.ARMADA_SHIP, 50, bar = listOf(ArmadaSlot.COMMANDER), flotilla = true)
        val hull = card("isd", GameSystem.ARMADA_V15, CardKind.ARMADA_SHIP, 100, bar = listOf(ArmadaSlot.COMMANDER), flotilla = false)
        val bad = list(GameSystem.ARMADA_V15, 400, entry("gozanti", flotilla), entry("cmdr", commander, "gozanti"))
        assertTrue("Commandant sur flottille refusé", ArmadaV15Validator().validate(bad).violations.any { it.code == "commander_on_flotilla" })
        val ok = list(GameSystem.ARMADA_V15, 400, entry("isd", hull), entry("cmdr", commander, "isd"))
        assertTrue("Commandant sur coque non-flottille = OK", ArmadaV15Validator().validate(ok).valid)
    }

    @Test fun armadaAllowsSevenShipsBecauseSixShipLimitWasNotARealRule() {
        val commander = card("commander", GameSystem.ARMADA_V15, CardKind.COMMANDER, 0, commander = true)
        val ship = card("isd", GameSystem.ARMADA_V15, CardKind.ARMADA_SHIP, 40)
        val entries = mutableListOf<ListEntry>(entry("c", commander))
        for (i in 1..7) entries.add(entry("s$i", ship))
        val fleet = list(GameSystem.ARMADA_V15, 400, *entries.toTypedArray())
        val r = ArmadaV15Validator().validate(fleet)
        assertTrue("7 vaisseaux légal : le RRG 1.6 n'impose pas de limite de 6 vaisseaux", r.valid)
        assertFalse("aucun violation ship_limit", r.violations.any { it.code == "ship_limit" })
    }
}
