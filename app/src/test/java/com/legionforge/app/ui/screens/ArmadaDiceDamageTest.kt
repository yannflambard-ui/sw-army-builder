package com.legionforge.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

/** Régression V1 : les dégâts Armada doivent utiliser la table vérifiée via damage(),
 *  et le critique ne compte que vaisseau contre vaisseau (RRG l.89-92, 402). */
class ArmadaDiceDamageTest {

    @Test fun critCountsOnlyShipVsShip() {
        val critRolls = listOf(ArmadaRoll(ArmadaDieColor.RED, ArmadaFace.CRIT))
        // CRIT pur : 1 dégât vs vaisseau, 0 vs escadron.
        assertEquals(1, ArmadaDiceRoller.damage(critRolls, true, true))
        assertEquals(0, ArmadaDiceRoller.damage(critRolls, true, false))
        assertEquals(0, ArmadaDiceRoller.damage(critRolls, false, true))
        assertEquals(0, ArmadaDiceRoller.damage(critRolls, false, false))
    }

    @Test fun hitCritTraitementSelonCible() {
        // HIT_CRIT : ship=2, squadron=1 (la face compte 1 touche + 1 critique vs vaisseau).
        val rolls = listOf(ArmadaRoll(ArmadaDieColor.BLACK, ArmadaFace.HIT_CRIT))
        assertEquals(2, ArmadaDiceRoller.damage(rolls, true, true))
        assertEquals(1, ArmadaDiceRoller.damage(rolls, true, false))
    }

    @Test fun hitsToujoursComptes() {
        val rolls = listOf(
            ArmadaRoll(ArmadaDieColor.BLACK, ArmadaFace.HIT),
            ArmadaRoll(ArmadaDieColor.BLUE, ArmadaFace.HIT_HIT),
        )
        assertEquals(3, ArmadaDiceRoller.damage(rolls, true, true))
        assertEquals(3, ArmadaDiceRoller.damage(rolls, false, false))
    }

    @Test fun tableAvec8FacesParDe() {
        assertEquals(true, ArmadaDiceTable.selfCheck())
    }
}
