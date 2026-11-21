package com.legionforge.app.ui

import com.legionforge.app.ui.screens.BattleMat
import com.legionforge.app.ui.screens.BattleMats
import com.legionforge.app.ui.screens.LegionPawn
import com.legionforge.app.ui.screens.LegionTabletopScreen
import com.legionforge.app.ui.theme.LegionForgeTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Capture du plateau avec vraie image de battle mat (Tatooine) + couche logique. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-mdpi")
class LegionBattleMatCaptureTest {

    private val pawns = listOf(
        LegionPawn("a1", "B1 Battle Droids", -60f, -80f, baseSize = "small", maxWounds = 4, wounds = 4, speed = 2, miniCount = 4),
        LegionPawn("a2", "General Grievous", 40f, -40f, baseSize = "medium", maxWounds = 5, wounds = 3, speed = 2),
        LegionPawn("b1", "Clone Troopers", -40f, 90f, baseSize = "small", maxWounds = 4, wounds = 2, speed = 2, isOpp = true, cover = 1, miniCount = 3),
    )

    @Test
    fun battlemat_tatooine_avec_logique() {
        captureRoboImage("src/test/snapshots/legion-battlemat-tatooine.png") {
            LegionForgeTheme {
                LegionTabletopScreen(pawns = pawns, round = 1, unitsLeft = 3,
                    onBack = {}, onOpenPlay = {}, onActivate = {}, onCoverChange = { _, _ -> },
                    onMove = { _, _, _, _ -> }, onRoundChange = {}, onWoundsChange = { _, _ -> },
                    battleMat = BattleMats.TATOOINE, battleMatName = "Tatooine",
                    onPickBattleMat = {})
            }
        }
    }

    @Test
    fun battlemat_tatooine_zoom_alignement() {
        captureRoboImage("src/test/snapshots/legion-battlemat-tatooine-zoom.png") {
            LegionForgeTheme {
                LegionTabletopScreen(pawns = pawns, round = 1, unitsLeft = 3,
                    onBack = {}, onOpenPlay = {}, onActivate = {}, onCoverChange = { _, _ -> },
                    onMove = { _, _, _, _ -> }, onRoundChange = {}, onWoundsChange = { _, _ -> },
                    battleMat = BattleMats.TATOOINE, battleMatName = "Tatooine",
                    onPickBattleMat = {}, forceScale = 0.35f)
            }
        }
    }

    /**
     * VRAI GROS PLAN (zoom 100 %) sur le dôme HG de Tatooine + légende, pour valider
     * que les emprises logiques (hachures magenta) tombent sur les bâtiments dessinés.
     * initialPan = centre écran - (mm cible × pxPerMm) avec pxPerMm = 2 × scale = 2.
     */
    @Test
    fun battlemat_tatooine_gros_plan_legende() {
        captureRoboImage("src/test/snapshots/legion-battlemat-tatooine-grosplan.png") {
            LegionForgeTheme {
                LegionTabletopScreen(pawns = pawns, round = 1, unitsLeft = 3,
                    onBack = {}, onOpenPlay = {}, onActivate = {}, onCoverChange = { _, _ -> },
                    onMove = { _, _, _, _ -> }, onRoundChange = {}, onWoundsChange = { _, _ -> },
                    battleMat = BattleMats.TATOOINE, battleMatName = "Tatooine",
                    onPickBattleMat = {}, forceScale = 1.0f,
                    initialPan = androidx.compose.ui.geometry.Offset(1405f, 1105f))
            }
        }
    }

    @Test
    fun ficelle_deplacement_speed3() {
        captureRoboImage("src/test/snapshots/legion-ficelle-deplacement.png") {
            LegionForgeTheme {
                LegionTabletopScreen(pawns = pawns, round = 1, unitsLeft = 3,
                    onBack = {}, onOpenPlay = {}, onActivate = {}, onCoverChange = { _, _ -> },
                    onMove = { _, _, _, _ -> }, onRoundChange = {}, onWoundsChange = { _, _ -> },
                    battleMat = BattleMats.NONE,
                    forceDeployOrigin = -60f to -80f,
                    forceDeployCurrent = 120f to 40f,
                    forceDeploySpeed = 3)
            }
        }
    }

    private fun captureMat(mat: BattleMat, name: String, file: String) {
        captureRoboImage("src/test/snapshots/$file") {
            LegionForgeTheme {
                LegionTabletopScreen(pawns = pawns, round = 1, unitsLeft = 3,
                    onBack = {}, onOpenPlay = {}, onActivate = {}, onCoverChange = { _, _ -> },
                    onMove = { _, _, _, _ -> }, onRoundChange = {}, onWoundsChange = { _, _ -> },
                    battleMat = mat, battleMatName = name, onPickBattleMat = {})
            }
        }
    }

    @Test fun battlemat_hoth() = captureMat(BattleMats.HOTH, "Hoth", "legion-battlemat-hoth.png")
    @Test fun battlemat_endor() = captureMat(BattleMats.ENDOR, "Endor", "legion-battlemat-endor.png")
    @Test fun battlemat_naboo() = captureMat(BattleMats.NABOO, "Naboo", "legion-battlemat-naboo.png")

    @Test
    fun battlemat_naboo_zoom() {
        captureRoboImage("src/test/snapshots/legion-battlemat-naboo-zoom.png") {
            LegionForgeTheme {
                LegionTabletopScreen(pawns = pawns, round = 1, unitsLeft = 3,
                    onBack = {}, onOpenPlay = {}, onActivate = {}, onCoverChange = { _, _ -> },
                    onMove = { _, _, _, _ -> }, onRoundChange = {}, onWoundsChange = { _, _ -> },
                    battleMat = BattleMats.NABOO, battleMatName = "Naboo",
                    onPickBattleMat = {}, forceScale = 0.35f)
            }
        }
    }
    @Test fun battlemat_geonosis() = captureMat(BattleMats.GEONOSIS, "Geonosis", "legion-battlemat-geonosis.png")

    @Test
    fun mapcard_none_comparaison() {
        captureRoboImage("src/test/snapshots/legion-mapcard-none.png") {
            LegionForgeTheme {
                LegionTabletopScreen(pawns = pawns, round = 1, unitsLeft = 3,
                    onBack = {}, onOpenPlay = {}, onActivate = {}, onCoverChange = { _, _ -> },
                    onMove = { _, _, _, _ -> }, onRoundChange = {}, onWoundsChange = { _, _ -> },
                    battleMat = BattleMats.TATOOINE, battleMatName = "Tatooine",
                    onPickBattleMat = {}, forceScale = 0.35f)
            }
        }
    }

    /** Bouton i OFF : fond + unités seulement, annotations masquées. */
    @Test
    fun battlemat_annotations_masquees() {
        captureRoboImage("src/test/snapshots/legion-annotations-masquees.png") {
            LegionForgeTheme {
                LegionTabletopScreen(pawns = pawns, round = 1, unitsLeft = 3,
                    onBack = {}, onOpenPlay = {}, onActivate = {}, onCoverChange = { _, _ -> },
                    onMove = { _, _, _, _ -> }, onRoundChange = {}, onWoundsChange = { _, _ -> },
                    battleMat = BattleMats.TATOOINE, battleMatName = "Tatooine",
                    onPickBattleMat = {}, forceShowMatLogic = false)
            }
        }
    }

    /** Portrait (bascule) : le plateau 2:1 dépasse, c'est assumé. */
    @Test
    fun battlemat_portrait() {
        captureRoboImage("src/test/snapshots/legion-battlemat-portrait.png") {
            LegionForgeTheme {
                LegionTabletopScreen(pawns = pawns, round = 1, unitsLeft = 3,
                    onBack = {}, onOpenPlay = {}, onActivate = {}, onCoverChange = { _, _ -> },
                    onMove = { _, _, _, _ -> }, onRoundChange = {}, onWoundsChange = { _, _ -> },
                    battleMat = BattleMats.TATOOINE, battleMatName = "Tatooine",
                    onPickBattleMat = {}, forceLandscape = false)
            }
        }
    }

    @Test
    fun mapcard_recover_research_zones() {
        captureRoboImage("src/test/snapshots/legion-mapcard-recover.png") {
            LegionForgeTheme {
                LegionTabletopScreen(pawns = pawns, round = 1, unitsLeft = 3,
                    onBack = {}, onOpenPlay = {}, onActivate = {}, onCoverChange = { _, _ -> },
                    onMove = { _, _, _, _ -> }, onRoundChange = {}, onWoundsChange = { _, _ -> },
                    battleMat = BattleMats.TATOOINE, battleMatName = "Tatooine",
                    onPickBattleMat = {}, forceMapLayoutName = "Recover the Research", forceScale = 0.104f)
            }
        }
    }

    @Test
    fun battlemat_scarif_sans_logique() {
        captureRoboImage("src/test/snapshots/legion-battlemat-scarif-photo.png") {
            LegionForgeTheme {
                LegionTabletopScreen(pawns = pawns, round = 1, unitsLeft = 3,
                    onBack = {}, onOpenPlay = {}, onActivate = {}, onCoverChange = { _, _ -> },
                    onMove = { _, _, _, _ -> }, onRoundChange = {}, onWoundsChange = { _, _ -> },
                    battleMat = BattleMats.SCARIF, battleMatName = "Scarif",
                    onPickBattleMat = {})
            }
        }
    }
}
