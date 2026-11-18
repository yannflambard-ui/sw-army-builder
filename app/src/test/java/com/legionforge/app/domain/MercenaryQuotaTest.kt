package com.legionforge.app.domain

import com.legionforge.app.data.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Régression V2 : conformité des mercenaires Legion (rulebook:2810-2816).
 *  Les mercenaires/Mandaloriens ne comptent pas vers les min de rang, max 2 Corps / 1 autre,
 *  et ne constituent pas une faction principale (pas de mixed_factions). */
class MercenaryQuotaTest {
    private fun unit(id: String, rank: LegionRank, faction: String) = CardDefinition(
        id, GameSystem.LEGION_V2, CardKind.LEGION_UNIT, id, 50, faction, rank
    )
    private fun entry(id: String, c: CardDefinition) = ListEntry(id, c, null, 1, null)
    private fun list(vararg e: ListEntry) = BuilderList("l", "t", GameSystem.LEGION_V2, "empire", 1000, e.toList())

    @Test fun mercenariesDontCountTowardRankMinimums() {
        // 1 commander + 3 corps (minimum standard) + mercenaires => valide.
        val cmd = unit("cmd", LegionRank.COMMANDER, "empire")
        val c1 = unit("c1", LegionRank.CORPS, "empire")
        val c2 = unit("c2", LegionRank.CORPS, "empire")
        val c3 = unit("c3", LegionRank.CORPS, "empire")
        val spec = unit("spec", LegionRank.SPECIAL_FORCES, "mercenary")
        val r = LegionV2Validator().validate(list(entry("1", cmd), entry("2", c1), entry("3", c2), entry("4", c3), entry("5", spec)))
        assertTrue("un mercenaire Special Forces ne doit pas casser les quotas", r.violations.none { it.code.startsWith("quota_") })
    }

    @Test fun mercenaryRankCapEnforced() {
        val cmd = unit("cmd", LegionRank.COMMANDER, "empire")
        val c1 = unit("c1", LegionRank.CORPS, "empire")
        val c2 = unit("c2", LegionRank.CORPS, "empire")
        val c3 = unit("c3", LegionRank.CORPS, "empire")
        val m1 = unit("m1", LegionRank.SPECIAL_FORCES, "mercenary")
        val m2 = unit("m2", LegionRank.SPECIAL_FORCES, "mercenary")
        val r = LegionV2Validator().validate(list(entry("1", cmd), entry("2", c1), entry("3", c2), entry("4", c3), entry("5", m1), entry("6", m2)))
        assertTrue("max 1 mercenaire par rang hors Corps", r.violations.any { it.code == "mercenary_rank_special_forces" })
    }

    @Test fun mercenariesCountTowardRankMaximums() {
        // rulebook:2824-2827 : 3 SPECIAL_FORCES empire + 1 mercenaire SPECIAL_FORCES = 4 > 3.
        // Le mercenaire ne compte pas vers le min mais compte vers le MAX combiné.
        val cmd = unit("cmd", LegionRank.COMMANDER, "empire")
        val c1 = unit("c1", LegionRank.CORPS, "empire")
        val c2 = unit("c2", LegionRank.CORPS, "empire")
        val c3 = unit("c3", LegionRank.CORPS, "empire")
        val s1 = unit("s1", LegionRank.SPECIAL_FORCES, "empire")
        val s2 = unit("s2", LegionRank.SPECIAL_FORCES, "empire")
        val s3 = unit("s3", LegionRank.SPECIAL_FORCES, "empire")
        val merc = unit("merc", LegionRank.SPECIAL_FORCES, "mercenary")
        val r = LegionV2Validator().validate(list(
            entry("1", cmd), entry("2", c1), entry("3", c2), entry("4", c3),
            entry("5", s1), entry("6", s2), entry("7", s3), entry("8", merc)
        ))
        assertTrue("3 SF + 1 merc = 4 depasse le maximum 3", r.violations.any { it.code == "quota_special_forces" })
        assertFalse("1 seul merc SF ne declenche pas le plafond specifique", r.violations.any { it.code == "mercenary_rank_special_forces" })
    }

    @Test fun mandaloriansAreNotAFifthFaction() {
        val cmd = unit("cmd", LegionRank.COMMANDER, "empire")
        val c1 = unit("c1", LegionRank.CORPS, "empire")
        val c2 = unit("c2", LegionRank.CORPS, "empire")
        val c3 = unit("c3", LegionRank.CORPS, "empire")
        val sabine = unit("sabine", LegionRank.OPERATIVE, "mandalorians")
        val r = LegionV2Validator().validate(list(entry("1", cmd), entry("2", c1), entry("3", c2), entry("4", c3), entry("5", sabine)))
        assertFalse("les Mandaloriens ne declenchent pas mixed_factions", r.violations.any { it.code == "mixed_factions" })
    }
}
