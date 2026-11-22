package com.legionforge.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

class ArmadaDeploymentDefaultsTest {

    private fun squads(n: Int) = (0 until n).map {
        ArmadaDeploymentDefaults.Spec("sq$it", isSquadron = true, isOpponent = false)
    }

    @Test
    fun aucun_escadron_ne_se_superpose() {
        for (n in 2..8) {
            val spec = squads(n) + ArmadaDeploymentDefaults.Spec("flag", false, false)
            val p = ArmadaDeploymentDefaults.placements(spec, false)
            val pts = spec.filter { it.isSquadron }.map { p.getValue(it.id) }
            for (a in pts.indices) for (b in a + 1 until pts.size) {
                val d = hypot(pts[a].xMm - pts[b].xMm, pts[a].yMm - pts[b].yMm)
                assertTrue("n=$n sq$a/sq$b trop proches: $d", d >= 60f)
            }
        }
    }

    @Test
    fun vaisseaux_dans_la_bande_RRG() {
        val spec = listOf(
            ArmadaDeploymentDefaults.Spec("a", false, false, "large"),
            ArmadaDeploymentDefaults.Spec("b", false, false, "huge"),
        ) + squads(4)
        val p = ArmadaDeploymentDefaults.placements(spec, false)
        spec.filter { !it.isSquadron }.forEach {
            val q = p.getValue(it.id)
            // Un socle huge (258 mm) est plus long que la bande 1-3 (152 mm) :
            // on contraint le CENTRE, pas le bord (limite de modélisation RRG).
            val fromEdge = ArmadaDeployment.distanceFromEdge(q.yMm, ArmadaDeployment.Side.SOUTH)
            assertTrue("${it.id} centre hors bande: ${q.xMm},${q.yMm} d=$fromEdge",
                kotlin.math.abs(q.xMm) <= ArmadaDeployment.SETUP_W_MM / 2f &&
                fromEdge in ArmadaDeployment.DEPLOY_MIN_MM - 12f..ArmadaDeployment.DEPLOY_MAX_MM + 12f)
        }
    }

    @Test
    fun miroir_nord_sud() {
        val spec = listOf(ArmadaDeploymentDefaults.Spec("a", false, false)) + squads(3)
        val sud = ArmadaDeploymentDefaults.placements(spec, false).getValue("a")
        val nordSpec = spec.map { it.copy(isOpponent = true) }
        val nord = ArmadaDeploymentDefaults.placements(nordSpec, true).getValue("a")
        assertEquals(sud.xMm, nord.xMm, 0.001f)
        assertEquals(-sud.yMm, nord.yMm, 0.001f)
    }

    @Test
    fun deterministe_meme_seed_meme_placement() {
        val spec = listOf(ArmadaDeploymentDefaults.Spec("a", false, false)) + squads(5)
        assertEquals(ArmadaDeploymentDefaults.placements(spec, false),
            ArmadaDeploymentDefaults.placements(spec, false))
    }

    @Test
    fun formations_sont_etalees() {
        ArmadaDeploymentDefaults.Formation.entries.forEach { f ->
            val pts = ArmadaDeploymentDefaults.formationOffsets(6, f)
            assertEquals(6, pts.size)
            val xs = pts.map { it.xMm }
            assertTrue("formation $f pas étalée en x", (xs.maxOrNull()!! - xs.minOrNull()!!) > 100f)
        }
    }
}
