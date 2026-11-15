package com.legionforge.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
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
    val figurePath: String? = null,   // figurine réelle (assets), fond transparent
    val figureLengthMm: Float = 0f,   // longueur nez-queue de la figurine (RebelScale)
    val figureKind: String = "ship",  // ship = socle rectangulaire | fighter = jeton rond
    val isOpp: Boolean = false,
    val isSquadron: Boolean = false,
    val speedChart: List<Map<Int, Int>> = emptyList(), // position 1..n -> vitesse -> crans d'embardée (absent = interdit)
    val hull: Int = 0,          // coque de départ (ArmadaStats.hull)
    val shieldFront: Int = 0,   // bouclier de secteur avant (ArmadaStats.shieldFront)
    val shieldRear: Int = 0,
    val shieldPort: Int = 0,
    val shieldStarboard: Int = 0,
)

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
    var pan by remember { mutableStateOf(Offset.Zero) }
    var positions by remember(pawns) { mutableStateOf(pawns) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var dragId by remember { mutableStateOf<String?>(null) }
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
    // Bornes de crans par articulation, lues dans la Speed Chart (position 1..4, vitesse courante)
    val jointMax = { pos: Int -> if (selPawn != null) maxClicks(selPawn.speedChart, pos, speedIdx) else 0 }
    // Crans effectifs bornés : aucune mutation d'état pendant la composition
    val effectiveBends = List(4) { i -> bends[i].coerceIn(-jointMax(i + 1), jointMax(i + 1)) }
    val bendsSummary = effectiveBends.mapIndexedNotNull { i, b ->
        if (b == 0) null else "${"①②③④"[i]}${if (b < 0) "←" else "→"}${kotlin.math.abs(b)}"
    }.joinToString(" ").ifEmpty { "droit" }
    val appCtx = androidx.compose.ui.platform.LocalContext.current
    val bitmaps = remember(pawns) {
        (pawns.mapNotNull { it.imagePath } + pawns.mapNotNull { it.basePath } + pawns.mapNotNull { it.figurePath }).distinct().associateWith { loadAssetBitmap(appCtx, it) }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF0B1220))) {
        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) { Text("← Retour", color = Color(0xFFFFC857), fontSize = 12.sp) }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Manche $round/6 · ${phase.lowercase().replaceFirstChar { it.uppercase() }}", color = Color(0xFFFFC857), fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                Text("Tabletop · ${"%.0f".format(scale * 100)}%", color = Color(0xFF9EACBC), fontSize = 10.sp)
            }
            TextButton(onClick = { scale = 0.5f; pan = Offset.Zero }) { Text("Recentrer", color = Color(0xFFFFC857), fontSize = 12.sp) }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                // Zoom et déplacement du plateau (deux doigts) : on ne touche pas aux pions
                .pointerInput(Unit) {
                    detectTransformGestures { _, panChange, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.3f, 4f)
                        pan += panChange
                    }
                }
        ) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .pointerInput(positions, scale, pan) {
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
                // plateau : fond + grille de 100 mm
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
                // Arcs de tir du vaisseau sélectionné : masqués pendant un drag, non bloquants
                if (dragId == null) {
                    positions.firstOrNull { it.id == selectedId }?.let { p ->
                        val center = Offset(c.x + p.xMm * PX_PER_MM * scale, c.y + p.yMm * PX_PER_MM * scale)
                        rotate(degrees = p.headingDeg, pivot = center) {
                        val baseBmp = p.basePath?.let { bitmaps[it] }
                        if (baseBmp != null) {
                            drawCardImage(baseBmp, center, wp, hp)
                        } else {
                            drawBase(center, wp, hp, p.headingDeg, selected = p.id == selectedId, target = p.id == targetId, round = p.isSquadron)
                        }
                        // Figurine réelle à l'ÉCHELLE DE LA FIGURINE : longueur nez-queue = lengthMm.
                        val figBmp = p.figurePath?.let { bitmaps[it] }
                        if (figBmp != null && p.figureLengthMm > 0f) {
                            val longPx = p.figureLengthMm * PX_PER_MM * scale
                            val k = longPx / maxOf(figBmp.width, figBmp.height).toFloat()
                            drawCardImage(figBmp, center, figBmp.width * k, figBmp.height * k)
                        } else if (baseBmp == null) {
                            drawCardImage(p.imagePath?.let { bitmaps[it] }, center, wp, hp)
                        }
                    }
                }
            }
        }

        // Attaque : choisir les dés, lancer (attaquant = sélection, cible = 2e tap)
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { blackDice = (blackDice - 1).coerceAtLeast(0) }) { Text("− noir", color = Color(0xFF9EACBC), fontSize = 11.sp) }
            Text("N$blackDice B$blueDice R$redDice", color = Color(0xFF9EACBC), fontSize = 11.sp)
            TextButton(onClick = { blackDice = (blackDice + 1).coerceAtMost(6) }) { Text("noir +", color = Color(0xFF9EACBC), fontSize = 11.sp) }
            TextButton(onClick = { blueDice = (blueDice + 1).coerceAtMost(6) }) { Text("bleu +", color = Color(0xFF9EACBC), fontSize = 11.sp) }
            TextButton(onClick = { redDice = (redDice + 1).coerceAtMost(6) }) { Text("rouge +", color = Color(0xFF9EACBC), fontSize = 11.sp) }
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
        // Sélecteur de trajectoire : flèches gauche/droite (vitesse 0 à max)
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { speedIdx = (speedIdx - 1).coerceAtLeast(0) }) { Text("◀ vitesse", color = Color(0xFFFFC857), fontSize = 12.sp) }
            Text("Vitesse ${speedIdx} · ≈ indicatif", color = Color(0xFF9EACBC), fontSize = 11.sp)
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
            ) { Text("Déplacer (V$speedIdx · $bendsSummary) ≈ indicatif", fontSize = 12.sp) }
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

private fun DrawScope.drawGrid(c: Offset, scale: Float) {
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
)

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
private fun infoLine(pawns: List<TabletopPawn>, selId: String?, tgtId: String?): String {
    val att = pawns.firstOrNull { it.id == selId } ?: return "Touchez un pion pour le sélectionner"
    val tgt = pawns.firstOrNull { it.id == tgtId }
    if (tgt == null) return "${att.name} sélectionné · touchez une cible"
    val d = hypot(tgt.xMm - att.xMm, tgt.yMm - att.yMm)
    return "${att.name} → ${tgt.name} · ${"%.0f".format(d)} mm centre à centre · arc ${sectorOf(att, tgt)} (non vérifié)"
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
