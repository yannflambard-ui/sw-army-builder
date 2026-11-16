package com.legionforge.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.hypot
import com.legionforge.app.domain.SEGMENT_MM
import com.legionforge.app.domain.YAW_STEP_DEG
import com.legionforge.app.domain.maxClicks
import com.legionforge.app.domain.maneuverPolyline
import com.legionforge.app.domain.resolveManeuver2
import com.legionforge.app.domain.hitRotatedBase
import android.content.Context
import org.json.JSONObject

/**
 * Mode Tabletop Armada (étape 1) : plateau 2D zoomable, pions déplaçables au doigt,
 * règle de distance. Aucun contrôle de règle (choix utilisateur : arcs et distance
 * affichés, décision laissée au joueur).
 *
 * Échelle : 1 mm de table = [PX_PER_MM] px à zoom 1. Socles en mm (RRG 1.5.0).
 */
private const val PX_PER_MM = 2.0f

/** Pion de vaisseau sur le plateau. Position en mm, plateau centré sur (0,0). */
data class TabletopPawn(
    val id: String,
    val name: String,
    val sizeClass: String,      // small | medium | large | huge | flotilla
    val xMm: Float,
    val yMm: Float,
    val headingDeg: Float = 0f, // 0 = proue vers le haut
    val imagePath: String? = null, // chemin dans assets (catalogue)
    val arcs: FiringArcs? = null,  // angles réels (Star Forge), null = repli non vérifié
    val basePath: String? = null,  // socle illustré (assets), null = socle générique
    val maxSpeed: Int = 0,
    val tokenPath: String? = null,    // token carton (Swarmada.wiki), dessous
    val figurePath: String? = null,   // figurine réelle (assets), fond transparent
    val figureLengthMm: Float = 0f,   // longueur nez-queue de la figurine (RebelScale)
    val figureKind: String = "ship",  // ship = socle rectangulaire | fighter = jeton rond
    val figureCount: Int = 1,          // exemplaires sur le jeton : 3 escadron, 2 flottille
    val isOpp: Boolean = false,
    val isSquadron: Boolean = false,
    val speedChart: List<Map<Int, Int>> = emptyList(), // position 1..n -> vitesse -> crans d'embardée (absent = interdit)
    val hull: Int = 0,          // coque de départ (ArmadaStats.hull)
    val shieldFront: Int = 0,   // bouclier de secteur avant (ArmadaStats.shieldFront)
    val shieldRear: Int = 0,
    val shieldPort: Int = 0,
    val shieldStarboard: Int = 0,
    /** Armement de batterie par zone de coque (hull zone). [noir, bleu, rouge] (shipStats.attack). */
    val attack: ArmadaAttack? = null,
    /** Snipe (Embuscade) : un escadron qui tire à distance 2 au lieu de 1. */
    val snipe: Boolean = false,
)

/** Dés d'attaque d'une zone de coque : [noirs, bleus, rouges]. */
data class ArmadaDiceBag(val black: Int = 0, val blue: Int = 0, val red: Int = 0) {
    val isEmpty get() = black + blue + red == 0
}

/** Armement par zone de coque (front/right/left/rear). */
data class ArmadaAttack(
    val front: ArmadaDiceBag = ArmadaDiceBag(),
    val right: ArmadaDiceBag = ArmadaDiceBag(),
    val left: ArmadaDiceBag = ArmadaDiceBag(),
    val rear: ArmadaDiceBag = ArmadaDiceBag(),
) {
    fun sector(name: String): ArmadaDiceBag = when (name) {
        "front" -> front; "right" -> right; "left" -> left; else -> rear
    }
}

/**
 * Portée maximale d'une zone de coque (RRG 1.5.0 p.7) : close si uniquement des dés noirs,
 * medium s'il y a au moins un dé bleu, long s'il y a au moins un dé rouge.
 * Renvoie le rayon en mm depuis le bord du socle, et les couleurs jouables.
 */
fun attackRange(bag: ArmadaDiceBag): Pair<Float, List<String>> = when {
    bag.red > 0 -> RANGE_LONG_MM to listOf("noir", "bleu", "rouge")
    bag.blue > 0 -> RANGE_MEDIUM_MM to listOf("noir", "bleu")
    bag.black > 0 -> RANGE_CLOSE_MM to listOf("noir")
    else -> 0f to emptyList()
}

/** Angles de tir d'un vaisseau, en degrés. front + 2*flank + rear = 360. */
data class FiringArcs(val front: Float, val flank: Float, val rear: Float)

/** Dimensions de socle (mm), RRG 1.5.0. Les flottilles n'ont pas de taille tranchée : pion neutre. */
fun baseSizeMm(sizeClass: String): Pair<Float, Float> = when (sizeClass) {
    "small" -> 43f to 71f
    "medium" -> 63f to 102f
    "large" -> 77.5f to 129f
    "huge" -> 77.5f to 365f
    else -> 43f to 71f
}

@Composable
fun TabletopScreen(
    pawns: List<TabletopPawn>,
    round: Int = 1,
    phase: String = "COMMAND",
    onBack: () -> Unit,
    onOpenPlay: (String) -> Unit = {},
) {
    // Cadrage initial : le tapis 1800x900 mm tient dans la largeur (PX_PER_MM = 2 px/mm, ~1080 px écran)
    var scale by remember { mutableStateOf(0.5f) }
    // Scale de cadrage : le plateau entier tient dans l'écran (ergonome).
    var fitScale by remember { mutableStateOf(0.5f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var positions by remember(pawns) { mutableStateOf(pawns) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var dragId by remember { mutableStateOf<String?>(null) }
    // Position du doigt pendant un drag (en mm plateau) — pour le trait de visée et le dépôt
    var dragPosMm by remember { mutableStateOf<Offset?>(null) }
    // Volet « Manœuvre » (vitesse + coudes) : replié par défaut, ouvert si un pion est sélectionné
    var showManeuver by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(selectedId) { showManeuver = selectedId != null }
    var targetId by remember { mutableStateOf<String?>(null) }
    var speedIdx by remember { mutableStateOf(0) }
    var attackResult by remember { mutableStateOf<String?>(null) }
    var blackDice by remember { mutableStateOf(1) }
    var blueDice by remember { mutableStateOf(0) }
    var redDice by remember { mutableStateOf(0) }
    // État de pliage du gabarit : 4 articulations, crans SIGNÉS (- = gauche, + = droite). RRG 1519-1524.
    var bends by remember { mutableStateOf(listOf(0, 0, 0, 0)) }
    var activeJoint by remember { mutableStateOf(1) }
    val selPawn = positions.firstOrNull { it.id == selectedId }
    // Vitesse max du vaisseau sélectionné (lue dans sa carte, pas en dur)
    val maxSpeed = selPawn?.maxSpeed ?: 0
    // Rayon atteignable (mm) pour un escadron : vitesse max × 1 distance (76,2 mm).
    // Un escadron se déplace librement (pas de gabarit), dans un disque de ce rayon.
    val squadronReachMm: Float = if (selPawn?.isSquadron == true) (maxSpeed.coerceAtLeast(1).toFloat() * 76.2f) else 0f
    // Bornes de crans par articulation, lues dans la Speed Chart (position 1..4, vitesse courante)
    val jointMax = { pos: Int -> if (selPawn != null) maxClicks(selPawn.speedChart, pos, speedIdx) else 0 }
    // Crans effectifs bornés : aucune mutation d'état pendant la composition
    val effectiveBends = List(4) { i -> bends[i].coerceIn(-jointMax(i + 1), jointMax(i + 1)) }
    val bendsSummary = effectiveBends.mapIndexedNotNull { i, b ->
        if (b == 0) null else "${"①②③④"[i]}${if (b < 0) "←" else "→"}${kotlin.math.abs(b)}"
    }.joinToString(" ").ifEmpty { "droit" }
    val appCtx = androidx.compose.ui.platform.LocalContext.current
    val bitmaps = remember(pawns) {
        (pawns.mapNotNull { it.imagePath } + pawns.mapNotNull { it.basePath } + pawns.mapNotNull { it.figurePath } + pawns.mapNotNull { it.tokenPath }).distinct().associateWith { loadAssetBitmap(appCtx, it) }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF0B1220))) {
        // Top bar : retour (icône) · manche + zoom · recentrer (icône). Tout en maxLines=1.
        Row(
            Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.material3.IconButton(onClick = onBack) {
                Text("←", color = Color(0xFFFFC857), fontSize = 22.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Manche $round/6 · ${phase.lowercase().replaceFirstChar { it.uppercase() }}", color = Color(0xFFFFC857), fontSize = 13.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, maxLines = 1)
                Text("${"%.0f".format(scale * 100)}%", color = Color(0xFF9EACBC), fontSize = 10.sp, maxLines = 1)
            }
            androidx.compose.material3.IconButton(onClick = { scale = fitScale; pan = Offset.Zero }) {
                Text("◎", color = Color(0xFFFFC857), fontSize = 22.sp)
            }
        }


        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .onSizeChanged { sz ->
                    // Cadrage : le plateau 6x3 ft tient dans l'écran (ergonome : * 0.95)
                    val need = minOf(
                        sz.width / (BOARD_W_MM * PX_PER_MM),
                        sz.height / (BOARD_H_MM * PX_PER_MM),
                    ) * 0.95f
                    fitScale = need
                    scale = need
                    pan = Offset.Zero
                }
                // Zoom et déplacement du plateau (deux doigts) : on ne touche pas aux pions
                .pointerInput(Unit) {
                    detectTransformGestures { _, panChange, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.3f, 4f)
                        pan += panChange
                    }
                }
        ) {
            // Init auto des dés quand la cible change (arc attaquant + bande de portée)
            androidx.compose.runtime.LaunchedEffect(targetId, selectedId) {
                val att = positions.firstOrNull { it.id == selectedId }
                val tgt = positions.firstOrNull { it.id == targetId }
                if (att != null && tgt != null) {
                    val dist = kotlin.math.hypot((tgt.xMm - att.xMm).toDouble(), (tgt.yMm - att.yMm).toDouble()).toFloat()
                    val sect = (Math.toDegrees(Math.atan2((tgt.xMm - att.xMm).toDouble(), -((tgt.yMm - att.yMm).toDouble()))) - att.headingDeg).toFloat().let {
                        // atan2 renvoie un angle dans le repère image (x vers la droite, y vers le bas).
                        // Notre repère : cap 0 = prôe vers le haut, donc on retire 90°.
                        attackedSector(att.headingDeg, it)
                    }
                    val bag = att.attack?.sector(sect)
                    val (nb, bl, rd) = initDiceFor(bag, dist, att.isSquadron, att.snipe)
                    blackDice = nb; blueDice = bl; redDice = rd
                } else if (att != null) {
                    // Pas de cible : proposer l'armement du secteur avant
                    val bag = att.attack?.sector("front")
                    val (nb, bl, rd) = initDiceFor(bag, 0f, att.isSquadron, att.snipe)
                    blackDice = nb; blueDice = bl; redDice = rd
                }
            }
            Canvas(
                Modifier
                    .fillMaxSize()
                    .pointerInput(positions, scale, pan) {
                        // Drag d'un pion (escadron surtout) : déplacement libre dans la zone atteignable
                        detectDragGestures(
                            onDragStart = { at: androidx.compose.ui.geometry.Offset ->
                                val cc = Offset(size.width / 2f + pan.x, size.height / 2f + pan.y)
                                val hit = positions.firstOrNull { p -> hitPawn(p, cc, scale, at) }
                                if (hit != null) {
                                    dragId = hit.id
                                    dragPosMm = Offset(
                                        (at.x - cc.x) / (PX_PER_MM * scale),
                                        (at.y - cc.y) / (PX_PER_MM * scale),
                                    )
                                }
                            },
                            onDragEnd = {
                                val id = dragId
                                val pos = dragPosMm
                                if (id != null && pos != null) {
                                    positions = positions.map { p ->
                                        if (p.id != id) p else {
                                            val reach = if (p.isSquadron) p.maxSpeed.coerceAtLeast(1) * 76.2f else 0f
                                            val d = kotlin.math.hypot(pos.x - p.xMm, pos.y - p.yMm)
                                            if (p.isSquadron && d > reach) {
                                                // hors zone atteignable : on garde la position
                                                p
                                            } else {
                                                p.copy(xMm = pos.x, yMm = pos.y)
                                            }
                                        }
                                    }
                                }
                                dragId = null
                                dragPosMm = null
                            },
                            onDragCancel = { dragId = null; dragPosMm = null },
                        ) { change: androidx.compose.ui.input.pointer.PointerInputChange, _: androidx.compose.ui.geometry.Offset ->
                            change.consume()
                            val cc = Offset(size.width / 2f + pan.x, size.height / 2f + pan.y)
                            dragPosMm = Offset(
                                (change.position.x - cc.x) / (PX_PER_MM * scale),
                                (change.position.y - cc.y) / (PX_PER_MM * scale),
                            )
                        }
                        detectTapGestures(
                            onLongPress = { at ->
                                val c = Offset(size.width / 2 + pan.x, size.height / 2 + pan.y)
                                positions.firstOrNull { p -> hitPawn(p, c, scale, at) }?.let { onOpenPlay(it.id) }
                            }
                        ) { tap ->
                            val c = Offset(size.width / 2 + pan.x, size.height / 2 + pan.y)
                            val hit = positions.firstOrNull { p -> hitPawn(p, c, scale, tap) }
                            when {
                                hit == null -> { selectedId = null; targetId = null }
                                selectedId != null && hit.id != selectedId -> targetId = hit.id
                                else -> { selectedId = hit.id; targetId = null }
                            }
                        }
                    }
            ) {
                val c = Offset(size.width / 2 + pan.x, size.height / 2 + pan.y)
                // plateau : zones de déploiement + grille de 100 mm
                drawDeploymentZones(c, scale)
                drawGrid(c, scale)
                // Aperçu du gabarit : polyligne d'autant de segments que la vitesse, poignées aux
                // articulations et vaisseau fantôme d'arrivée. INDICATIF : 22,5°/cran et 40 mm/segment non sourcés.
                if (dragId == null && speedIdx > 0 && selPawn != null) {
                    val poly = maneuverPolyline(selPawn.xMm, selPawn.yMm, selPawn.headingDeg, speedIdx, effectiveBends)
                    val pts = poly.map { (x, y) -> Offset(c.x + x * PX_PER_MM * scale, c.y + y * PX_PER_MM * scale) }
                    val dash = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                    for (i in 1 until pts.size) {
                        drawLine(Color(0xCCFFC857), pts[i - 1], pts[i], strokeWidth = 3f, pathEffect = dash)
                    }
                    for (i in 1 until pts.lastIndex) {
                        drawCircle(Color(0x44FFC857), radius = 9f, center = pts[i])
                        drawCircle(Color(0xFFFFC857), radius = 3f, center = pts[i])
                    }
                    val ghost = resolveManeuver2(selPawn.xMm, selPawn.yMm, selPawn.headingDeg, speedIdx, effectiveBends)
                    val (gw, gh) = baseSizeMm(selPawn.sizeClass)
                    val gwPx = gw * PX_PER_MM * scale
                    val ghPx = gh * PX_PER_MM * scale
                    rotate(degrees = ghost.headingDeg, pivot = pts.last()) {
                        drawRect(
                            Color(0x55FFC857),
                            topLeft = Offset(pts.last().x - gwPx / 2, pts.last().y - ghPx / 2),
                            size = androidx.compose.ui.geometry.Size(gwPx, ghPx),
                        )
                        // Proue du fantôme : vers le haut du repère local, la rotation fait le reste.
                        drawLine(
                            Color(0x8877D9A7), pts.last(),
                            Offset(pts.last().x, pts.last().y - ghPx * 0.4f),
                            strokeWidth = 2f,
                        )
                    }
                }
                // Zone atteignable d'un escadron sélectionné (disque de rayon vitesse × 1 distance)
                if (selectedId != null && selPawn?.isSquadron == true && squadronReachMm > 0f) {
                    val sc = Offset(c.x + selPawn.xMm * PX_PER_MM * scale, c.y + selPawn.yMm * PX_PER_MM * scale)
                    val r = squadronReachMm * PX_PER_MM * scale
                    drawCircle(Color(0x2222D3EE), radius = r, center = sc)
                    drawCircle(
                        Color(0xAA22D3EE), radius = r, center = sc,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 2.5f,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(14f, 10f)),
                        ),
                    )
                }
                // Pendant un drag : trait de visée + position fantôme + limite de zone
                if (dragId != null && dragPosMm != null) {
                    val dp = dragPosMm!!
                    val dragPawn = positions.find { it.id == dragId }
                    if (dragPawn != null) {
                        val from = Offset(c.x + dragPawn.xMm * PX_PER_MM * scale, c.y + dragPawn.yMm * PX_PER_MM * scale)
                        val to = Offset(c.x + dp.x * PX_PER_MM * scale, c.y + dp.y * PX_PER_MM * scale)
                        val reach = if (dragPawn.isSquadron) dragPawn.maxSpeed.coerceAtLeast(1) * 76.2f else 0f
                        val dist = kotlin.math.hypot((dp.x - dragPawn.xMm).toDouble(), (dp.y - dragPawn.yMm).toDouble()).toFloat()
                        val inRange = !dragPawn.isSquadron || dist <= reach
                        val col = if (inRange) Color(0xCC22D3EE) else Color(0xCCF87171)
                        drawLine(col, from, to, strokeWidth = 3f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 8f)))
                        // fantôme du pion à la position du doigt
                        val (pw, ph) = baseSizeMm(dragPawn.sizeClass)
                        val pwPx = pw * PX_PER_MM * scale
                        val phPx = ph * PX_PER_MM * scale
                        drawRect(col.copy(alpha = 0.35f), topLeft = Offset(to.x - pwPx / 2, to.y - phPx / 2), size = androidx.compose.ui.geometry.Size(pwPx, phPx))
                        drawRect(col, topLeft = Offset(to.x - pwPx / 2, to.y - phPx / 2), size = androidx.compose.ui.geometry.Size(pwPx, phPx), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))
                    }
                }
                // Anneaux de portée du vaisseau sélectionné (noir=Close, bleu=Medium, rouge=Long)
                if (dragId == null && selectedId != null) {
                    positions.find { it.id == selectedId }?.let { sel ->
                        val (sw, sh) = baseSizeMm(sel.sizeClass)
                        val sc = Offset(c.x + sel.xMm * PX_PER_MM * scale, c.y + sel.yMm * PX_PER_MM * scale)
                        val bag = sel.attack?.sector("front") ?: ArmadaDiceBag(
                            black = if (sel.isSquadron) 1 else 0, blue = 0, red = 0
                        )
                        drawRangeRings(sc, scale, if (sel.isSquadron) null else bag, sel.isSquadron, sel.snipe)
                    }
                }
                // Triangle de flanc attaqué (attaquant → cible)
                if (dragId == null && selectedId != null && targetId != null) {
                    val att = positions.find { it.id == selectedId }
                    val tgt = positions.find { it.id == targetId }
                    if (att != null && tgt != null) {
                        val (aw, ah) = baseSizeMm(att.sizeClass)
                        val (tw, th) = baseSizeMm(tgt.sizeClass)
                        val ac = Offset(c.x + att.xMm * PX_PER_MM * scale, c.y + att.yMm * PX_PER_MM * scale)
                        val tc = Offset(c.x + tgt.xMm * PX_PER_MM * scale, c.y + tgt.yMm * PX_PER_MM * scale)
                        val dx = (tgt.xMm - att.xMm).toDouble()
                        val dy = (tgt.yMm - att.yMm).toDouble()
                        val tAngle = Math.toDegrees(Math.atan2(dx, -dy)).toFloat()
                        val sect = attackedSector(att.headingDeg, tAngle)
                        val bag = att.attack?.sector(sect)
                        drawAttackedFlank(ac, att.headingDeg, tc, tgt.headingDeg, tw / 2f, th / 2f, bag, scale)
                    }
                }
                // Arcs de tir du vaisseau sélectionné : masqués pendant un drag, non bloquants
                if (dragId == null) {
                    positions.firstOrNull { it.id == selectedId }?.let { p ->
                        val center = Offset(c.x + p.xMm * PX_PER_MM * scale, c.y + p.yMm * PX_PER_MM * scale)
                        rotate(degrees = p.headingDeg, pivot = center) {
                            drawFiringArcs(center, ARC_RADIUS_MM * PX_PER_MM * scale, p.arcs)
                        }
                    }
                }
                positions.forEach { p ->
                    val (w, h) = baseSizeMm(p.sizeClass)
                    val center = Offset(c.x + p.xMm * PX_PER_MM * scale, c.y + p.yMm * PX_PER_MM * scale)
                    val wp = w * PX_PER_MM * scale
                    val hp = h * PX_PER_MM * scale
                    // Le socle SUIT le cap : rotate autour de son centre (RRG : le vaisseau est parallèle
                    // au dernier segment du gabarit à l'arrivée).
                    rotate(degrees = p.headingDeg, pivot = center) {
                        // 1) DESSOUS : plus de carte de l'unité. Socle seulement si PAS de figurine.
                        val figBmp0 = p.figurePath?.let { bitmaps[it] }
                        val hasFig = figBmp0 != null && p.figureLengthMm > 0f
                        val baseBmp = p.basePath?.let { bitmaps[it] }
                        if (!hasFig) {
                            if (baseBmp != null) {
                                drawCardImage(baseBmp, center, wp, hp)
                            } else {
                                drawBase(center, wp, hp, p.headingDeg, selected = p.id == selectedId, target = p.id == targetId, round = p.isSquadron)
                            }
                        }
                        // Figurine réelle à l'ÉCHELLE DE LA FIGURINE : longueur nez-queue = lengthMm.
                        val figBmp = p.figurePath?.let { bitmaps[it] }
                        if (figBmp != null && p.figureLengthMm > 0f) {
                            val longPx = p.figureLengthMm * PX_PER_MM * scale
                            val k = longPx / maxOf(figBmp.width, figBmp.height).toFloat()
                            val fw = figBmp.width * k
                            val fh = figBmp.height * k
                            val n = p.figureCount.coerceAtLeast(1)
                            if (n == 1) {
                                drawCardImage(figBmp, center, fw, fh)
                            } else {
                                // Escadre (3 chasseurs) ou flottille (2 vaisseaux) : jetons en quinconce
                                val step = longPx * 0.17f
                                val half = (n - 1) / 2f
                                for (i in 0 until n) {
                                    drawCardImage(
                                        figBmp,
                                        Offset(center.x + (i - half) * step, center.y + (i - half) * step * 0.5f),
                                        fw, fh,
                                    )
                                }
                            }
                        } else if (baseBmp == null) {
                            drawCardImage(p.imagePath?.let { bitmaps[it] }, center, wp, hp)
                        }
                    }
                }
            }
        }

        // Attaque : dés N/B/R (colonnes +/- empilés), lancer (attaquant = sélection, cible = 2e tap)
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
            DiceColumn("N", Color(0xFF1F2937), blackDice, selectedId != null) { blackDice = it }
            DiceColumn("B", Color(0xFF3B82F6), blueDice, selectedId != null) { blueDice = it }
            DiceColumn("R", Color(0xFFEF4444), redDice, selectedId != null) { redDice = it }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            TextButton(
                enabled = selectedId != null && targetId != null && (blackDice + blueDice + redDice) > 0,
                onClick = {
                    val rolls = com.legionforge.app.ui.screens.ArmadaDiceRoller.roll(blackDice, blueDice, redDice)
                    val tgt = positions.firstOrNull { it.id == targetId }
                    val att = positions.firstOrNull { it.id == selectedId }
                    val targetIsShip = tgt?.isSquadron != true
                    val attackerIsShip = att?.isSquadron != true
                    val dmg = com.legionforge.app.ui.screens.ArmadaDiceRoller.damage(rolls, attackerIsShip, targetIsShip)
                    val acc = com.legionforge.app.ui.screens.ArmadaDiceRoller.accuracies(rolls)
                    val crit = rolls.count { it.face.crit }
                    attackResult = "Dégâts $dmg · Critiques $crit · Précisions $acc"
                    // Appliquer les dégâts sur la cible : boucliers du secteur avant, puis coque, puis cartes (RRG)
                    if (tgt != null && dmg > 0) {
                        positions = positions.map { p ->
                            if (p.id != tgt.id) p else {
                                val (sh, hl, cards) = com.legionforge.app.ui.screens.ArmadaDefense.applyDamage(p.shieldFront, p.hull, dmg)
                                attackResult = "Dégâts $dmg → boucliers ${sh}, coque ${hl}, cartes ${cards} · Critiques $crit · Précisions $acc"
                                p.copy(shieldFront = sh, hull = hl)
                            }
                        }
                    }
                },
            ) { Text("Attaquer", color = Color(0xFFF87171), fontSize = 12.sp) }
        }
        attackResult?.let { Text(it, color = Color(0xFFFFC857), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp)) }
        // Volet « Manœuvre » : replié pour laisser de la place au plateau
        androidx.compose.material3.TextButton(onClick = { showManeuver = !showManeuver }, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            Text(if (showManeuver) "Manœuvre ▼" else "Manœuvre ▲", color = Color(0xFFFFC857), fontSize = 13.sp, maxLines = 1)
        }
        androidx.compose.animation.AnimatedVisibility(visible = showManeuver) {
        // Sélecteur de trajectoire : flèches gauche/droite (vitesse 0 à max)
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { speedIdx = (speedIdx - 1).coerceAtLeast(0) }) { Text("◀ vitesse", color = Color(0xFFFFC857), fontSize = 12.sp) }
            Text("Vitesse ${speedIdx}", color = Color(0xFF9EACBC), fontSize = 12.sp)
            // L'estimation de trajectoire n'est pas sourcée RRG : une icône info remplace le jargon.
            Text("ℹ", color = Color(0xFF55637A), fontSize = 13.sp)
            TextButton(onClick = { speedIdx = (speedIdx + 1).coerceAtMost(maxSpeed) }) { Text("vitesse ▶", color = Color(0xFFFFC857), fontSize = 12.sp) }
        }
        // Déplacement multi-coudes : UNE ligne, 4 chips + ◄ ► ⊘. Bornes lues dans speedChart (RRG 1519-1524).
        if (selPawn != null && maxSpeed > 0) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = {
                    val j = activeJoint
                    bends = bends.toMutableList().also { it[j - 1] = (it[j - 1] - 1).coerceAtLeast(-jointMax(j)) }
                }) { Text("◄", color = Color(0xFFFFC857), fontSize = 14.sp) }
                for (j in 1..4) {
                    JointChip(
                        label = "①②③④"[j - 1].toString(),
                        value = effectiveBends[j - 1],
                        max = jointMax(j),
                        active = activeJoint == j,
                        onTap = {
                            if (activeJoint == j && effectiveBends[j - 1] != 0) {
                                bends = bends.toMutableList().also { it[j - 1] = -it[j - 1] }
                            } else activeJoint = j
                        },
                        onReset = { bends = bends.toMutableList().also { it[j - 1] = 0 } },
                    )
                }
                TextButton(onClick = {
                    val j = activeJoint
                    bends = bends.toMutableList().also { it[j - 1] = (it[j - 1] + 1).coerceAtMost(jointMax(j)) }
                }) { Text("►", color = Color(0xFFFFC857), fontSize = 14.sp) }
                TextButton(onClick = { bends = listOf(0, 0, 0, 0) }) { Text("⊘", color = Color(0xFF9EACBC), fontSize = 14.sp) }
            }
            Button(
                enabled = speedIdx > 0,
                onClick = {
                    val out = resolveManeuver(selPawn, positions, speedIdx, effectiveBends)
                    positions = positions.map { p ->
                        if (p.id == selPawn.id) p.copy(xMm = out.xMm, yMm = out.yMm, headingDeg = out.heading) else p
                    }
                    speedIdx = 0
                    bends = listOf(0, 0, 0, 0)
                    attackResult = out.collidedWith?.let {
                        "⚠ Chevauchement avec $it : vitesse réduite à ${out.speedUsed} (RRG 1032). À faire à la main : carte de dégâts face cachée à ${selPawn.name} et à $it."
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            ) { Text(if (speedIdx > 0) "Déplacer V$speedIdx" else "Déplacer", fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, maxLines = 1) }
        }
        }
        // Règle de distance : entre les deux pions sélectionnés (ou les deux premiers)
        Text(
            if (selPawn != null && speedIdx > 0)
                "V$speedIdx · $bendsSummary · cap final ${"%.1f".format(selPawn.headingDeg + effectiveBends.take(speedIdx).sum() * YAW_STEP_DEG)}° ≈ · ◄► plient le coude ${"①②③④"[activeJoint - 1]} · appui long = 0 · ${YAW_STEP_DEG}°/cran et ${SEGMENT_MM.toInt()} mm non sourcés"
            else infoLine(positions, selectedId, targetId),
            color = Color(0xFF9EACBC), fontSize = 11.sp,
            modifier = Modifier.padding(8.dp),
        )
    }
}

@Composable
private fun JointChip(
    label: String, value: Int, max: Int, active: Boolean,
    onTap: () -> Unit, onReset: () -> Unit,
) {
    val locked = max == 0
    val glyph = when {
        locked -> "⊘"
        value < 0 -> "←${-value}"
        value > 0 -> "→$value"
        else -> "·"
    }
    val tint = when {
        locked -> Color(0xFF55637A)
        value < 0 -> Color(0xFF7EC8FF)
        value > 0 -> Color(0xFFFFAE6B)
        else -> Color(0xFF9EACBC)
    }
    val bg = if (active) Color(0x33FFC857) else Color(0x11FFFFFF)
    Box(
        Modifier
            .pointerInput(Unit) { detectTapGestures(onTap = { onTap() }, onLongPress = { onReset() }) }
            .background(bg, androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
            .padding(horizontal = 5.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) { Text("$label$glyph", color = tint, fontSize = 12.sp) }
}

private /**
 * Zones de déploiement (Armada 400 pts) : deux bandes, une par joueur, sur toute la largeur
 * du plateau. Bande = distance 1 à 3 depuis le bord de table du joueur.
 * Dessinées en teinte pleine + liseré pour rester lisibles quand les pions sont posés dessus.
 */
fun DrawScope.drawDeploymentZones(c: Offset, scale: Float) {
    val k = PX_PER_MM * scale
    val halfW = BOARD_W_MM / 2f * k   // 6 ft = bord long = bord du joueur
    val halfH = BOARD_H_MM / 2f * k   // 3 ft = bord court
    val depMin = DEPLOY_MIN_MM * k
    val depMax = DEPLOY_MAX_MM * k
    // plateau : cadre
    drawRect(
        Color(0x2238BDF8),
        topLeft = Offset(c.x - halfW, c.y - halfH),
        size = androidx.compose.ui.geometry.Size(2 * halfW, 2 * halfH),
    )
    // zone de mise en place : 4 x 3 ft, centrée (marqueurs à 1 ft des bords courts)
    val setupHalfW = SETUP_W_MM / 2f * k
    drawRect(
        Color(0x55FFC857),
        topLeft = Offset(c.x - setupHalfW, c.y - halfH),
        size = androidx.compose.ui.geometry.Size(2 * setupHalfW, 2 * halfH),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f),
    )
    // zone de déploiement SUD (joueur 1) : distance 1..3 depuis le bord bas (bord long)
    val southTop = c.y + halfH - depMax
    val southBottom = c.y + halfH - depMin
    drawRect(
        Color(0x5522D3EE),
        topLeft = Offset(c.x - setupHalfW, southTop),
        size = androidx.compose.ui.geometry.Size(2 * setupHalfW, southBottom - southTop),
    )
    drawRect(
        Color(0xCC22D3EE),
        topLeft = Offset(c.x - setupHalfW, southTop),
        size = androidx.compose.ui.geometry.Size(2 * setupHalfW, southBottom - southTop),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f),
    )
    // zone de déploiement NORD (joueur 2) : symétrique
    val northTop = c.y - halfH + depMin
    val northBottom = c.y - halfH + depMax
    drawRect(
        Color(0x55F87171),
        topLeft = Offset(c.x - setupHalfW, northTop),
        size = androidx.compose.ui.geometry.Size(2 * setupHalfW, northBottom - northTop),
    )
    drawRect(
        Color(0xCCF87171),
        topLeft = Offset(c.x - setupHalfW, northTop),
        size = androidx.compose.ui.geometry.Size(2 * setupHalfW, northBottom - northTop),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f),
    )
    // libellés de zone (drawIntoCanvas pour le texte)
    drawIntoCanvas { canvas ->
        val p = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 13f * scale + 9f
            isAntiAlias = true
            setShadowLayer(4f, 0f, 0f, android.graphics.Color.BLACK)
        }
        val label = "ZONE DE DÉPLOIEMENT"
        val w = p.measureText(label)
        canvas.nativeCanvas.drawText(label, c.x - w / 2f, southTop + (southBottom - southTop) / 2f + 5f, p)
        canvas.nativeCanvas.drawText(label, c.x - w / 2f, northTop + (northBottom - northTop) / 2f + 5f, p)
        p.textSize = 11f * scale + 8f
        val setup = "Zone de mise en place (4×3 ft)"
        val w2 = p.measureText(setup)
        canvas.nativeCanvas.drawText(setup, c.x - w2 / 2f, c.y - halfH + 22f, p)
    }
}

/**
 * Triangle de flanc : pointe vers le hull zone de la cible qui est attaqué.
 * Collé au bord du socle de la cible, couleur = dé dominant de l'armement attaquant.
 * + trait de visée pointillé entre les deux centres de socle.
 */
fun DrawScope.drawAttackedFlank(
    attackerCenter: Offset, attackerHeadingDeg: Float,
    targetCenter: Offset, targetHeadingDeg: Float,
    targetHalfW: Float, targetHalfH: Float,
    bag: ArmadaDiceBag?,
    scale: Float,
) {
    // angle du vecteur attaquant→cible dans notre repère (cap 0 = haut, horaire positif)
    val dx = (targetCenter.x - attackerCenter.x).toDouble()
    val dy = (targetCenter.y - attackerCenter.y).toDouble()
    val targetAngleDeg = Math.toDegrees(Math.atan2(dx, -dy)).toFloat()  // Double -> Float
    val sect = attackedSector(attackerHeadingDeg, targetAngleDeg)
    // couleur du dé dominant de la zone attaquée
    val col = when {
        (bag?.black ?: 0) >= (bag?.blue ?: 0) && (bag?.black ?: 0) >= (bag?.red ?: 0) -> Color(0xCC1F2937)
        (bag?.blue ?: 0) >= (bag?.red ?: 0) -> Color(0xCC3B82F6)
        else -> Color(0xCEF4444)
    }
    // trait de visée pointillé
    drawLine(
        Color(0x88FFC857),
        start = attackerCenter, end = targetCenter,
        strokeWidth = 2f * scale,
        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 8f)),
    )
    // triangle collé au milieu du flanc touché (dans le repère de la cible)
    val tSize = 24f * scale
    val (ux, uy, ang) = when (sect) {
        "front" -> Triple(0f, -1f, 0f)
        "right" -> Triple(1f, 0f, 90f)
        "left" -> Triple(-1f, 0f, -90f)
        else -> Triple(0f, 1f, 180f)
    }
    // point d'accroche sur le bord du socle
    val anchor = Offset(
        targetCenter.x + ux * targetHalfW * PX_PER_MM * scale,
        targetCenter.y + uy * targetHalfH * PX_PER_MM * scale,
    )
    rotate(degrees = ang + targetHeadingDeg, pivot = anchor) {
        val p = androidx.compose.ui.graphics.Path().apply {
            moveTo(anchor.x, anchor.y - tSize)
            lineTo(anchor.x - tSize * 0.86f, anchor.y + tSize * 0.5f)
            lineTo(anchor.x + tSize * 0.86f, anchor.y + tSize * 0.5f)
            close()
        }
        drawPath(p, col)
    }
}

/**
 * Anneaux de portée d'un vaisseau sélectionné (RRG 1.5.0 p.7/9).
 * Trois cercles concentriques depuis le bord du socle : noir = Close, bleu = Medium, rouge = Long.
 * Pour un escadron : un seul cercle (distance 1, ou 2 avec Snipe).
 */
fun DrawScope.drawRangeRings(center: Offset, scale: Float, bag: ArmadaDiceBag?, isSquadron: Boolean, snipe: Boolean) {
    val base = if (isSquadron) {
        listOf(Triple(if (snipe) SNIPE_RANGE_MM else SQUADRON_RANGE_MM, Color(0x99FFC857), "escadron"))
    } else {
        val (maxRange, _) = attackRange(bag ?: ArmadaDiceBag())
        val rings = mutableListOf<Triple<Float, Color, String>>()
        if (maxRange >= RANGE_CLOSE_MM) rings += Triple(RANGE_CLOSE_MM, Color(0xAAFFFFFF), "noir")
        if (maxRange >= RANGE_MEDIUM_MM) rings += Triple(RANGE_MEDIUM_MM, Color(0xAA38BDF8), "bleu")
        if (maxRange >= RANGE_LONG_MM) rings += Triple(RANGE_LONG_MM, Color(0xAAF87171), "rouge")
        rings
    }
    if (base.isEmpty()) return
    val k = PX_PER_MM * scale
    val maxR = base.maxOf { it.first }
    // disque de fond pour démarquer la zone de portée
    drawCircle(Color(0x2238BDF8), radius = maxR * k, center = center)
    base.forEach { (mm, col, _) ->
        drawCircle(col, radius = mm * k, center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f))
    }
}

fun DrawScope.drawGrid(c: Offset, scale: Float) {
    val step = 100f * PX_PER_MM * scale
    if (step < 8f) return
    var x = c.x % step
    while (x < size.width) { drawLine(Color(0x22FFFFFF), Offset(x, 0f), Offset(x, size.height)); x += step }
    var y = c.y % step
    while (y < size.height) { drawLine(Color(0x22FFFFFF), Offset(0f, y), Offset(size.width, y)); y += step }
}

private fun DrawScope.drawBase(center: Offset, w: Float, h: Float, headingDeg: Float, selected: Boolean, target: Boolean = false, round: Boolean = false) {
    val topLeft = Offset(center.x - w / 2, center.y - h / 2)
    val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = if (selected) 3f else 1.5f)
    val border = if (target) Color(0xFFF87171) else if (selected) Color(0xFFFFC857) else Color(0xFF4A6080)
    if (round) {
        // escadron (chasseur) : jeton rond, diamètre = plus petit côté
        val r = minOf(w, h) / 2
        drawCircle(Color(0xFF1E2A3A), r, center)
        drawCircle(border, r, center, style = stroke)
        return
    }
    drawRect(Color(0xFF1E2A3A), topLeft, androidx.compose.ui.geometry.Size(w, h))
    drawRect(border, topLeft, androidx.compose.ui.geometry.Size(w, h), style = stroke)
    // proue : petit repère vers le haut du socle (heading 0)
    drawLine(Color(0xFF77D9A7), center, Offset(center.x, topLeft.y + h * 0.1f), strokeWidth = 2f)
}

/** Image de la carte au-dessus du socle (miniature). Le bitmap est chargé hors thread UI. */
private fun DrawScope.drawCardImage(bmp: androidx.compose.ui.graphics.ImageBitmap?, center: Offset, w: Float, h: Float) {
    if (bmp == null) return
    val dst = androidx.compose.ui.unit.IntSize(w.toInt().coerceAtLeast(1), h.toInt().coerceAtLeast(1))
    drawImage(bmp, dstOffset = androidx.compose.ui.unit.IntOffset((center.x - w / 2).toInt(), (center.y - h / 2).toInt()), dstSize = dst)
}

/** Vrai si le point touché tombe dans le socle du pion (heading 0 uniquement, étape 1). */
/** Demi-dimensions du socle en mm (RRG : petit 43x71, moyen 63x102, gros 77.5x129). Immense = 2 bases, non traité ici. */
private fun baseHalfMm(sizeClass: String): Pair<Float, Float> = when (sizeClass) {
    "medium" -> 31.5f to 51f
    "large", "huge" -> 38.75f to 64.5f
    else -> 21.5f to 35.5f
}

/** Résultat d'une manœuvre : position finale, vitesse effective, vaisseau chevauché (s'il y en a un). */
data class ManeuverOutcome(val xMm: Float, val yMm: Float, val heading: Float, val speedUsed: Int, val collidedWith: String?)

/**
 * RRG 1032-1036 : si le vaisseau chevauche un autre vaisseau au terme de sa manœuvre, sa vitesse est
 * temporairement réduite de 1 et il se déplace à cette vitesse ; on répète jusqu'à une position libre
 * ou une vitesse 0 (sur place). Le cadran de vitesse n'est pas modifié.
 * position(v) = point d'arrivée pour la vitesse v (géométrie indicative : 40 mm/vitesse, cap donné).
 */
fun resolveManeuver(
    mover: TabletopPawn,
    others: List<TabletopPawn>,
    speed: Int,
    bends: List<Int>,
): ManeuverOutcome {
    var v = speed
    while (true) {
        // RRG 786-787 : à vitesse v, seules les v premières articulations comptent.
        val out = resolveManeuver2(mover.xMm, mover.yMm, mover.headingDeg, v, bends.take(v))
        val probe = mover.copy(xMm = out.xMm, yMm = out.yMm, headingDeg = out.headingDeg)
        val hit = others.firstOrNull { it.id != mover.id && basesOverlap(probe, it) }
        if (hit == null || v == 0) {
            return ManeuverOutcome(out.xMm, out.yMm, out.headingDeg, v, hit?.name)
        }
        v -= 1
    }
}

/** Chevauchement de deux socles rectangulaires orientés (axes = cap de chaque pion). Test SAT simplifié. */
fun basesOverlap(a: TabletopPawn, b: TabletopPawn): Boolean {
    if (a.isSquadron || b.isSquadron) return false
    val (ahw, ahl) = baseHalfMm(a.sizeClass)
    val (bhw, bhl) = baseHalfMm(b.sizeClass)
    val ra = Math.toRadians(a.headingDeg.toDouble())
    val rb = Math.toRadians(b.headingDeg.toDouble())
    val axes = listOf(ra, ra + Math.PI / 2, rb, rb + Math.PI / 2)
    val dx = (b.xMm - a.xMm).toDouble(); val dy = (b.yMm - a.yMm).toDouble()
    for (ax in axes) {
        val ux = kotlin.math.cos(ax); val uy = kotlin.math.sin(ax)
        val dist = kotlin.math.abs(dx * ux + dy * uy)
        val ra2 = ahw * kotlin.math.abs(ux) + ahl * kotlin.math.abs(uy)
        val rb2 = bhw * kotlin.math.abs(ux) + bhl * kotlin.math.abs(uy)
        if (dist > ra2 + rb2) return false // axe séparateur trouvé
    }
    return true
}

private fun hitPawn(p: TabletopPawn, c: Offset, scale: Float, at: Offset): Boolean {
    val (w, h) = baseSizeMm(p.sizeClass)
    val cx = c.x + p.xMm * PX_PER_MM * scale
    val cy = c.y + p.yMm * PX_PER_MM * scale
    val hw = w * PX_PER_MM * scale / 2
    val hh = h * PX_PER_MM * scale / 2
    return hitRotatedBase(at.x, at.y, cx, cy, hw, hh, p.headingDeg)
}

/** Figurine réelle d'une carte Armada : asset, classe de socle, longueur de figurine. */
data class ArmadaFigure(
    val asset: String,
    val sizeClass: String,
    val figureKind: String,
    val lengthMm: Float,
    val figureCount: Int = 1,
)

/** Charge armada_ship_tokens.json : id de carte -> token carton (carte de jeu). */
fun loadArmadaTokens(context: Context): Map<String, String> = try {
    val txt = context.assets.open("armada_ship_tokens.json").bufferedReader().use { it.readText() }
    val items = JSONObject(txt).getJSONObject("images")
    buildMap {
        items.keys().forEach { k -> put(k, items.getString(k)) }
    }
} catch (_: Exception) { emptyMap() }

/** Charge armada_figures_map.json : id de carte -> figurine réelle (fond transparent). */
fun loadArmadaFigures(context: Context): Map<String, ArmadaFigure> = try {
    val txt = context.assets.open("armada_figures_map.json").bufferedReader().use { it.readText() }
    val items = JSONObject(txt).getJSONObject("items")
    buildMap {
        items.keys().forEach { k ->
            val o = items.getJSONObject(k)
            put(k, ArmadaFigure(
                asset = o.getString("asset"),
                sizeClass = o.optString("size_class", "small"),
                figureKind = o.optString("figure_kind", "ship"),
                lengthMm = o.optDouble("length_mm", 0.0).toFloat(),
                figureCount = o.optInt("figure_count", 1),
            ))
        }
    }
} catch (_: Exception) { emptyMap() }

/** Charge armada_ship_size_table.json : id de carte -> classe de socle. */
fun loadShipSizes(context: Context): Map<String, String> = try {
    val txt = context.assets.open("armada_ship_size_table.json").bufferedReader().use { it.readText() }
    val ships = JSONObject(txt).getJSONObject("ships")
    buildMap {
        ships.keys().forEach { k -> put(k, ships.getJSONObject(k).getString("size")) }
    }
} catch (_: Exception) { emptyMap() }

/** Rayon d'affichage des arcs (mm). Visuel seulement, pas une règle de portée. */
private const val ARC_RADIUS_MM = 400f

/**
 * Portées d'attaque (RRG 1.5.0 p.7/9). La règle de portée fait 1 pied (304,8 mm) et se lit
 * depuis le point le plus proche du SOCLE attaquant (jamais le centre).
 * Les bandes de la règle physique sont INÉGALES ; mesures relevées :
 *  - Close  ≈ 0..124 mm : dés NOIRS + bleus + rouges
 *  - Medium ≈ 0..188 mm : dés BLEUS + rouges (la bande close est incluse)
 *  - Long   ≈ 0..304,8 mm : dés ROUGES seuls
 * ATTENTION escadrons : ils utilisent le côté DISTANCE de la règle (bandes 1..5),
 * JAMAIS le côté range. Distance 1 = 60,96 mm, distance 2 = 121,92 mm.
 */
private const val RANGE_RULER_MM = 304.8f      // 1 pied = 12 pouces
// Bandes INÉGALES de la réglette physique (mesures relevées, tolérance impression 1-2 mm) :
private const val RANGE_CLOSE_MM = 123.19f     // noir  = 4 7/8" ≈ 12,3 cm
private const val RANGE_MEDIUM_MM = 186.69f    // bleu  = 7 3/8" ≈ 18,7 cm
private const val RANGE_LONG_MM = RANGE_RULER_MM // rouge = 12"  ≈ 30,5 cm
// Côté DISTANCE de la réglette (escadrons) : 1 distance = 3" = 76,2 mm
private const val SQUADRON_RANGE_MM = 76.2f    // distance 1
private const val SNIPE_RANGE_MM = 152.4f      // distance 2 (Snipe/Embuscade)

/**
 * Plateau standard Armada : 3 x 6 pieds (914 x 1829 mm), centré sur (0,0).
 * Zone de mise en place : 3 x 4 pieds au centre (914 x 1219 mm).
 * Zone de déploiement : bande de la distance 1 à 3 depuis VOTRE bord de table,
 * sur toute la largeur de la zone de mise en place.
 */
private const val BOARD_W_MM = 1828.8f   // 6 pieds (bord long = bord du joueur, HORIZONTAL)
private const val BOARD_H_MM = 914.4f    // 3 pieds (bord court = vertical)
private const val SETUP_W_MM = 1219.2f   // 4 pieds (zone de mise en place, centrée)
private const val DEPLOY_MIN_MM = 76.2f  // distance 1 (3")
private const val DEPLOY_MAX_MM = 228.6f // distance 3 (9")

/**
 * Secteurs de tir : avant 60°, arrière 60°, flancs 120° chacun.
 * ANGLES NON VÉRIFIÉS : la RRG ne donne pas de degrés (arcs = lignes imprimées sur le pion).
 * Origine = centre du socle (écart négligeable avec le centre de coque).
 */
private fun DrawScope.drawFiringArcs(center: Offset, r: Float, arcs: FiringArcs?) {
    val tl = Offset(center.x - r, center.y - r)
    val sz = androidx.compose.ui.geometry.Size(2 * r, 2 * r)
    // Angles Compose : 0 = 3 h, sens horaire. Proue = -90.
    // Secteurs centrés sur la proue (-90 en Compose). Avant = front, flancs = flank, arrière = rear.
    val f = arcs?.front ?: 60f
    val fl = arcs?.flank ?: 120f
    val rr = arcs?.rear ?: 60f
    val sectors = listOf(
        Triple(-90f - f / 2, f, Color(0x3338BDF8)),       // avant
        Triple(-90f + f / 2, fl, Color(0x3334D399)),      // tribord
        Triple(-90f + f / 2 + fl, rr, Color(0x33F87171)), // arrière
        Triple(-90f + f / 2 + fl + rr, fl, Color(0x33A78BFA)), // bâbord
    )
    sectors.forEach { (start, sweep, col) -> drawArc(col, start, sweep, useCenter = true, topLeft = tl, size = sz) }
    // bordures des arcs : bornes de chaque secteur
    val bounds = listOf(-90f - f / 2, -90f + f / 2, -90f + f / 2 + fl, -90f + f / 2 + fl + rr)
    bounds.forEach { a ->
        val rad = Math.toRadians(a.toDouble())
        val end = Offset(center.x + r * kotlin.math.cos(rad).toFloat(), center.y + r * kotlin.math.sin(rad).toFloat())
        drawLine(Color(0x8838BDF8), center, end, strokeWidth = 1.5f)
    }
}

/** Secteur de la cible vu depuis l'attaquant (0 = proue). Angles non vérifiés (voir drawFiringArcs). */
private fun sectorOf(att: TabletopPawn, tgt: TabletopPawn): String {
    val dx = tgt.xMm - att.xMm
    val dy = tgt.yMm - att.yMm
    // 0 = vers le haut (proue), sens horaire positif : atan2(dx, -dy)
    var rel = Math.toDegrees(kotlin.math.atan2(dx.toDouble(), (-dy).toDouble())).toFloat() - att.headingDeg
    while (rel > 180f) rel -= 360f
    while (rel < -180f) rel += 360f
    return when {
        kotlin.math.abs(rel) <= 30f -> "avant"
        kotlin.math.abs(rel) >= 150f -> "arrière"
        rel > 0f -> "tribord"
        else -> "bâbord"
    }
}

/** Texte d'information : distance centre à centre (pas une mesure de portée officielle). */
/**
 * Initialisation auto des dés selon le hull zone attaquant + la bande de portée (RRG).
 * `bag` = armement du hull zone (front/right/left/rear) ; `distMm` = distance attaquant→cible.
 * Renvoie [noirs, bleus, rouges] à mettre dans le lanceur (le joueur reste libre d'ajuster).
 */
fun initDiceFor(bag: ArmadaDiceBag?, distMm: Float, isSquadron: Boolean, snipe: Boolean): Triple<Int, Int, Int> {
    if (isSquadron) {
        // Escadron : distance 1 (ou 2 avec Snipe), toutes couleurs, Snipe = X dés bleus
        val maxD = if (snipe) SNIPE_RANGE_MM else SQUADRON_RANGE_MM
        if (distMm > maxD) return Triple(0, 0, 0)
        val b = bag ?: ArmadaDiceBag(black = 1)
        return if (snipe) Triple(0, b.black, 0) else Triple(b.black, b.blue, b.red)
    }
    val b = bag ?: return Triple(0, 0, 0)
    return when {
        distMm <= RANGE_CLOSE_MM -> Triple(b.black, b.blue, b.red)          // close : les 3 couleurs
        distMm <= RANGE_MEDIUM_MM -> Triple(0, b.blue, b.red)               // medium : bleus + rouges
        distMm <= RANGE_LONG_MM -> Triple(0, 0, b.red)                       // long : rouges seuls
        else -> Triple(0, 0, 0)                                              // hors portée
    }
}

/** Secteur (hull zone) attaqué par un attaquant dont le cap est `attHeadingDeg` vers une cible à `targetAngleDeg`. */
fun attackedSector(attHeadingDeg: Float, targetAngleDeg: Float): String {
    // angle relatif dans le repère du vaisseau (0 = devant, +90 = tribord, 180 = derrière, -90 = bâbord)
    var rel = (targetAngleDeg - attHeadingDeg + 540f) % 360f - 180f
    return when {
        rel in -45f..45f -> "front"
        rel in 45f..135f -> "right"
        rel in -135f..-45f -> "left"
        else -> "rear"
    }
}

/** Une colonne de dé : [+] au-dessus, valeur, [−] en-dessous, pastille + lettre. */
@Composable
private fun DiceColumn(label: String, color: Color, value: Int, enabled: Boolean, onChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        TextButton(
            onClick = { onChange((value + 1).coerceAtMost(6)) },
            modifier = Modifier.size(48.dp, 48.dp),
            enabled = enabled,
        ) { Text("+", color = color, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
        Text(
            "$value",
            color = color,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.defaultMinSize(minWidth = 32.dp),
        )
        TextButton(
            onClick = { onChange((value - 1).coerceAtLeast(0)) },
            modifier = Modifier.size(48.dp, 48.dp),
            enabled = enabled,
        ) { Text("−", color = color, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).background(color, CircleShape))
            Text(" $label", color = Color(0xFF9EACBC), fontSize = 12.sp)
        }
    }
}

private fun infoLine(pawns: List<TabletopPawn>, selId: String?, tgtId: String?): String {
    val att = pawns.firstOrNull { it.id == selId } ?: return "Touchez un pion pour le sélectionner"
    val bag = att.attack?.sector("front") ?: ArmadaDiceBag(black = if (att.isSquadron) 1 else 0)
    val (maxR, _) = attackRange(bag)
    val portee = if (att.isSquadron) {
        if (att.snipe) "Snipe D2 (152 mm)" else "D1 (76 mm)"
    } else if (maxR > 0f) {
        val bands = buildList {
            if (maxR >= RANGE_CLOSE_MM) add("noir")
            if (maxR >= RANGE_MEDIUM_MM) add("bleu")
            if (maxR >= RANGE_LONG_MM) add("rouge")
        }.joinToString("+")
        "%.0f mm · dés %s".format(maxR, bands)
    } else "pas d'armement"
    val tgt = pawns.firstOrNull { it.id == tgtId }
    if (tgt == null) return "%s · %s · touchez une cible".format(att.name, portee)
    val d = hypot((tgt.xMm - att.xMm).toDouble(), (tgt.yMm - att.yMm).toDouble()).toFloat()
    return "%s → %s · %.0f mm · arc %s · %s".format(att.name, tgt.name, d, sectorOf(att, tgt), portee)
}

/** Charge une image d'asset en ImageBitmap (null si absente). Appelé une fois par chemin. */
fun loadAssetBitmap(context: Context, path: String): androidx.compose.ui.graphics.ImageBitmap? = try {
    context.assets.open(path).use { stream ->
        val bmp: android.graphics.Bitmap? = android.graphics.BitmapFactory.decodeStream(stream)
        if (bmp == null) null else bmp.asImageBitmap()
    }
} catch (_: Exception) { null }

/** Table id de carte -> chemin d'image, depuis armada_ship_images.json. */
fun loadShipImages(context: Context): Map<String, String> = try {
    val txt = context.assets.open("armada_ship_images.json").bufferedReader().use { it.readText() }
    val imgs = JSONObject(txt).getJSONObject("images")
    buildMap { imgs.keys().forEach { k -> put(k, imgs.getString(k)) } }
} catch (_: Exception) { emptyMap() }

/** Charge armada_ship_bases.json : id de carte -> chemin du socle illustré (assets). */
fun loadShipBases(context: Context): Map<String, String> = try {
    val txt = context.assets.open("armada_ship_bases.json").bufferedReader().use { it.readText() }
    val bases = JSONObject(txt).getJSONObject("bases")
    buildMap { bases.keys().forEach { k -> put(k, bases.getJSONObject(k).getString("base")) } }
} catch (_: Exception) { emptyMap() }

/** Charge armada_firing_angles.json : id de carte -> angles (degrés). Vide si absent. */
fun loadFiringArcs(context: Context): Map<String, FiringArcs> = try {
    val txt = context.assets.open("armada_firing_angles.json").bufferedReader().use { it.readText() }
    val ships = JSONObject(txt).getJSONObject("ships")
    buildMap {
        ships.keys().forEach { k ->
            val o = ships.getJSONObject(k)
            put(k, FiringArcs(o.getDouble("front").toFloat(), o.getDouble("flank").toFloat(), o.getDouble("rear").toFloat()))
        }
    }
} catch (_: Exception) { emptyMap() }
