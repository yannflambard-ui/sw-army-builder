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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.hypot
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

/** Valeur PROVISOIRE : mm par point de vitesse pour la trajectoire indicative. À étalonner sur la règle. */
private const val TRAJ_STEP_MM = 40f

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
    val speedChart: List<Map<Int, Int>> = emptyList(), // position 1..n -> vitesse -> crans d'embardée (absent = interdit)
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
    onBack: () -> Unit,
    onOpenPlay: (String) -> Unit = {},
) {
    var scale by remember { mutableStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var positions by remember(pawns) { mutableStateOf(pawns) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var dragId by remember { mutableStateOf<String?>(null) }
    var targetId by remember { mutableStateOf<String?>(null) }
    var speedIdx by remember { mutableStateOf(0) }
    var yawIdx by remember { mutableStateOf(0) }
    var maxSpeed by remember { mutableStateOf(0) }
    val appCtx = androidx.compose.ui.platform.LocalContext.current
    val bitmaps = remember(pawns) {
        (pawns.mapNotNull { it.imagePath } + pawns.mapNotNull { it.basePath }).distinct().associateWith { loadAssetBitmap(appCtx, it) }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF0B1220))) {
        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) { Text("← Retour", color = Color(0xFFFFC857), fontSize = 12.sp) }
            Text("Tabletop · ${"%.0f".format(scale * 100)}%", color = Color(0xFF9EACBC), fontSize = 11.sp)
            TextButton(onClick = { scale = 1f; pan = Offset.Zero }) { Text("Recentrer", color = Color(0xFFFFC857), fontSize = 12.sp) }
        }

        Box(
            Modifier
                .fillMaxSize()
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
                // Trajectoire indicative : ligne droite, longueur = vitesse x TRAJ_STEP_MM (à étalonner)
                if (dragId == null && speedIdx > 0) {
                    positions.firstOrNull { it.id == selectedId }?.let { p ->
                        val center = Offset(c.x + p.xMm * PX_PER_MM * scale, c.y + p.yMm * PX_PER_MM * scale)
                        val rad = Math.toRadians(p.headingDeg.toDouble())
                        val len = speedIdx * TRAJ_STEP_MM * PX_PER_MM * scale
                        val end = Offset(
                            center.x + len * kotlin.math.sin(rad).toFloat(),
                            center.y - len * kotlin.math.cos(rad).toFloat(),
                        )
                        drawLine(
                            Color(0xFFFFC857), center, end, strokeWidth = 2f,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(14f, 10f)),
                        )
                    }
                }
                // Arcs de tir du vaisseau sélectionné : masqués pendant un drag, non bloquants
                if (dragId == null) {
                    positions.firstOrNull { it.id == selectedId }?.let { p ->
                        val center = Offset(c.x + p.xMm * PX_PER_MM * scale, c.y + p.yMm * PX_PER_MM * scale)
                        drawFiringArcs(center, ARC_RADIUS_MM * PX_PER_MM * scale, p.arcs)
                    }
                }
                positions.forEach { p ->
                    val (w, h) = baseSizeMm(p.sizeClass)
                    val center = Offset(c.x + p.xMm * PX_PER_MM * scale, c.y + p.yMm * PX_PER_MM * scale)
                    val baseBmp = p.basePath?.let { bitmaps[it] }
                    if (baseBmp != null) {
                        // socle illustré : remplace le rectangle générique
                        drawCardImage(baseBmp, center, w * PX_PER_MM * scale, h * PX_PER_MM * scale)
                    } else {
                        drawBase(center, w * PX_PER_MM * scale, h * PX_PER_MM * scale, p.headingDeg, selected = p.id == selectedId, target = p.id == targetId)
                        drawCardImage(p.imagePath?.let { bitmaps[it] }, center, w * PX_PER_MM * scale, h * PX_PER_MM * scale)
                    }
                }
            }
        }

        // Sélecteur de trajectoire : flèches gauche/droite (vitesse 0 à max)
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { speedIdx = (speedIdx - 1).coerceAtLeast(0) }) { Text("◀ vitesse", color = Color(0xFFFFC857), fontSize = 12.sp) }
            Text("Vitesse ${speedIdx}", color = Color(0xFF9EACBC), fontSize = 11.sp)
            TextButton(onClick = { speedIdx = (speedIdx + 1).coerceAtMost(4) }) { Text("vitesse ▶", color = Color(0xFFFFC857), fontSize = 12.sp) }
        }
        // Règle de distance : entre les deux pions sélectionnés (ou les deux premiers)
        val sel = positions.firstOrNull { it.id == selectedId }
        Text(
            infoLine(positions, selectedId, targetId),
            color = Color(0xFF9EACBC), fontSize = 11.sp,
            modifier = Modifier.padding(8.dp),
        )
    }
}

private fun DrawScope.drawGrid(c: Offset, scale: Float) {
    val step = 100f * PX_PER_MM * scale
    if (step < 8f) return
    var x = c.x % step
    while (x < size.width) { drawLine(Color(0x22FFFFFF), Offset(x, 0f), Offset(x, size.height)); x += step }
    var y = c.y % step
    while (y < size.height) { drawLine(Color(0x22FFFFFF), Offset(0f, y), Offset(size.width, y)); y += step }
}

private fun DrawScope.drawBase(center: Offset, w: Float, h: Float, headingDeg: Float, selected: Boolean, target: Boolean = false) {
    val topLeft = Offset(center.x - w / 2, center.y - h / 2)
    drawRect(Color(0xFF1E2A3A), topLeft, androidx.compose.ui.geometry.Size(w, h))
    drawRect(
        if (target) Color(0xFFF87171) else if (selected) Color(0xFFFFC857) else Color(0xFF4A6080),
        topLeft, androidx.compose.ui.geometry.Size(w, h),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = if (selected) 3f else 1.5f),
    )
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
private fun hitPawn(p: TabletopPawn, c: Offset, scale: Float, at: Offset): Boolean {
    val (w, h) = baseSizeMm(p.sizeClass)
    val cx = c.x + p.xMm * PX_PER_MM * scale
    val cy = c.y + p.yMm * PX_PER_MM * scale
    val hw = w * PX_PER_MM * scale / 2
    val hh = h * PX_PER_MM * scale / 2
    return at.x in (cx - hw)..(cx + hw) && at.y in (cy - hh)..(cy + hh)
}

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

/** Charge armada_firing_angles.json : id de carte -> angles (degrés). Vide si absent. */
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
