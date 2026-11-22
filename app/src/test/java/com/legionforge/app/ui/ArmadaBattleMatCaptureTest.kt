package com.legionforge.app.ui

import com.legionforge.app.ui.screens.ArmadaBattleMats
import com.legionforge.app.ui.screens.FiringArcs
import com.legionforge.app.ui.screens.TabletopPawn
import com.legionforge.app.ui.screens.TabletopScreen
import com.legionforge.app.ui.theme.LegionForgeTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Capture du plateau Armada avec vraie image de tapis spatial (fond cosmétique). */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-mdpi")
class ArmadaBattleMatCaptureTest {

    private val pawns = listOf(
        TabletopPawn("a1", "Imperial I-Class", "large", -200f, 150f, 90f,
            maxSpeed = 2, hull = 8, shieldFront = 2, arcs = FiringArcs(270f, 45f, 45f)),
        TabletopPawn("b1", "MC80 Liberty", "huge", 180f, -150f, 270f, isOpp = true,
            maxSpeed = 1, hull = 10, shieldFront = 3, arcs = FiringArcs(270f, 45f, 45f)),
    )

    /** Aperçu du déplacement au pouce : destinations légales du gabarit en
     *  pointillé, cible la plus proche du doigt. */
    @Test
    fun armada_drag_destinations_gabarit() {
        val vaisseaux = listOf(
            TabletopPawn("g1", "Imperial I-Class", "large", 0f, 0f, 0f,
                maxSpeed = 2, hull = 8,
                speedChart = listOf(mapOf(2 to 1), mapOf(2 to 1))),
        )
        captureRoboImage("src/test/snapshots/armada-drag-destinations.png") {
            LegionForgeTheme {
                TabletopScreen(pawns = vaisseaux, sessionId = "test-drag", round = 2,
                    phase = "SHIP", onBack = {}, battleMat = ArmadaBattleMats.SCARIF,
                    battleMatName = "Orbite de Scarif", onPickBattleMat = {},
                    forceScale = 0.7f, forceDragPawnId = "g1",
                    forceDragPosMm = androidx.compose.ui.geometry.Offset(-150f, -60f),
                    forceSpeedIdx = 2)
            }
        }
    }

    /** Vérifie que les escadrons affichent de vraies silhouettes de chasseurs
     *  (ex-x_wing_img06.png) au lieu des anciens ronds noirs drawBase(round). */
    /** Non-régression : avec un pan qui pousse la carte vers le haut, le plateau
     *  ne doit JAMAIS recouvrir la top bar (titre « Orbite de Scarif », bouton
     *  « Zones », icône de recentrage). */
    @Test
    fun armada_pan_ne_recouvre_pas_le_header() {
        captureRoboImage("src/test/snapshots/armada-pan-header.png") {
            LegionForgeTheme {
                TabletopScreen(pawns = pawns, sessionId = "test-pan", round = 2,
                    phase = "SHIP", onBack = {}, battleMat = ArmadaBattleMats.SCARIF,
                    battleMatName = "Orbite de Scarif", onPickBattleMat = {},
                    forcePan = androidx.compose.ui.geometry.Offset(0f, 600f))
            }
        }
    }

    /** Vérifie que les escadrons affichent de vraies silhouettes de chasseurs
     *  (ex-x_wing_img06.png) au lieu des anciens ronds noirs drawBase(round). */
    /** Déploiement par défaut : escadrons en formation décalée, sans superposition. */
    @Test
    fun armada_deploiement_formation() {
        val flotte = listOf(
            TabletopPawn("flag", "Imperial I-Class", "large", 0f, 0f, 0f,
                maxSpeed = 2, hull = 8,
                speedChart = listOf(mapOf(2 to 1), mapOf(2 to 1))),
        ) + (0 until 6).map { i ->
            TabletopPawn("sq$i", "TIE/ln", "small", 0f, 0f, 0f,
                isSquadron = true, maxSpeed = 2, hull = 1)
        }
        captureRoboImage("src/test/snapshots/armada-deploiement-formation.png") {
            LegionForgeTheme {
                TabletopScreen(pawns = flotte, sessionId = "test-deploy-formation",
                    round = 0, phase = "COMMAND", onBack = {},
                    battleMat = ArmadaBattleMats.NONE, battleMatName = "Sans fond",
                    onPickBattleMat = {}, forceScale = 0.45f)
            }
        }
    }

    /** Vérifie que les escadrons affichent de vraies silhouettes de chasseurs
     *  (ex-x_wing_img06.png) au lieu des anciens ronds noirs drawBase(round). */
    @Test
    fun armada_chasseurs_silhouettes() {
        val fighters = listOf(
            TabletopPawn("f1", "X-wing Squadron", "squadron", -45f, 40f, 0f,
                isSquadron = true, maxSpeed = 4, hull = 5,
                figurePath = "armada_figures/x_wing_img06.png",
                figureLengthMm = 9.48f, figureKind = "fighter", figureCount = 3),
            TabletopPawn("f2", "TIE/ln Squadron", "squadron", 0f, 40f, 0f,
                isSquadron = true, maxSpeed = 4, hull = 3, isOpp = true,
                figurePath = "armada_figures/tie_fighter_external.png",
                figureLengthMm = 9.48f, figureKind = "fighter", figureCount = 3),
            TabletopPawn("f3", "A-wing Squadron", "squadron", 45f, 40f, 0f,
                isSquadron = true, maxSpeed = 5, hull = 3,
                figurePath = "armada_figures/a_wing_img06.png",
                figureLengthMm = 9.48f, figureKind = "fighter", figureCount = 3),
            TabletopPawn("f4", "Vulture Squadron", "squadron", 0f, -40f, 0f,
                isSquadron = true, maxSpeed = 4, hull = 3, isOpp = true,
                figurePath = "armada_figures/vulture_img06.png",
                figureLengthMm = 9.48f, figureKind = "fighter", figureCount = 3),
        )
        captureRoboImage("src/test/snapshots/armada-chasseurs-silhouettes.png") {
            LegionForgeTheme {
                TabletopScreen(pawns = fighters, sessionId = "test-fighters", round = 1, phase = "SQUADRON",
                    onBack = {}, battleMat = ArmadaBattleMats.SCARIF, battleMatName = "Orbite de Scarif",
                    onPickBattleMat = {}, forceScale = 1.2f)
            }
        }
    }

    @Test
    fun armada_tapis_scarif() {
        captureRoboImage("src/test/snapshots/armada-battlemat-scarif.png") {
            LegionForgeTheme {
                TabletopScreen(pawns = pawns, sessionId = "test-scarif", round = 1, phase = "COMMAND",
                    onBack = {}, battleMat = ArmadaBattleMats.SCARIF, battleMatName = "Orbite de Scarif",
                    onPickBattleMat = {})
            }
        }
    }

    @Test
    fun armada_tapis_kaliida() {
        captureRoboImage("src/test/snapshots/armada-battlemat-kaliida.png") {
            LegionForgeTheme {
                TabletopScreen(pawns = pawns, sessionId = "test-kaliida", round = 2, phase = "SHIP",
                    onBack = {}, battleMat = ArmadaBattleMats.KALIIDA, battleMatName = "Nébuleuse de Kaliida",
                    onPickBattleMat = {})
            }
        }
    }
}
