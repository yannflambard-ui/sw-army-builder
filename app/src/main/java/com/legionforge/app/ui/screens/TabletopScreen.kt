package com.legionforge.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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

/** Pion de vaisseau sur le plateau. Position en mm, plateau centré sur (0,0). */
data class TabletopPawn(
    val id: String,
    val name: String,
    val sizeClass: String,      // small | medium | large | huge | flotilla
    val xMm: Float,
    val yMm: Float,
    val headingDeg: Float = 0f, // 0 = proue vers le haut
)

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
) {
    var scale by remember { mutableStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var positions by remember(pawns) { mutableStateOf(pawns) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var dragId by remember { mutableStateOf<String?>(null) }

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
                        detectTapGestures { tap ->
                            val c = Offset(size.width / 2 + pan.x, size.height / 2 + pan.y)
                            selectedId = positions.firstOrNull { p -> hitPawn(p, c, scale, tap) }?.id
                        }
                    }
                    .pointerInput(positions, scale, pan) {
                        detectDragGestures(
                            onDragStart = { start ->
                                val c = Offset(size.width / 2 + pan.x, size.height / 2 + pan.y)
                                dragId = positions.firstOrNull { p -> hitPawn(p, c, scale, start) }?.id
                                selectedId = dragId
                            },
                            onDrag = { change, delta ->
                                val id = dragId ?: return@detectDragGestures
                                change.consume()
                                positions = positions.map { p ->
                                    if (p.id == id) p.copy(
                                        xMm = p.xMm + delta.x / (PX_PER_MM * scale),
                                        yMm = p.yMm + delta.y / (PX_PER_MM * scale),
                                    ) else p
                                }
                            },
                            onDragEnd = { dragId = null },
                            onDragCancel = { dragId = null },
                        )
                    }
            ) {
                val c = Offset(size.width / 2 + pan.x, size.height / 2 + pan.y)
                // plateau : fond + grille de 100 mm
                drawGrid(c, scale)
                positions.forEach { p ->
                    val (w, h) = baseSizeMm(p.sizeClass)
                    val center = Offset(c.x + p.xMm * PX_PER_MM * scale, c.y + p.yMm * PX_PER_MM * scale)
                    drawBase(center, w * PX_PER_MM * scale, h * PX_PER_MM * scale, p.headingDeg, selected = p.id == selectedId)
                }
            }
        }

        // Règle de distance : entre les deux pions sélectionnés (ou les deux premiers)
        val sel = positions.firstOrNull { it.id == selectedId }
        Text(
            if (sel != null) "${sel.name} · ${"%.0f".format(hypot(sel.xMm, sel.yMm))} mm du centre"
            else "Touchez un pion pour le sélectionner",
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

private fun DrawScope.drawBase(center: Offset, w: Float, h: Float, headingDeg: Float, selected: Boolean) {
    val topLeft = Offset(center.x - w / 2, center.y - h / 2)
    drawRect(Color(0xFF1E2A3A), topLeft, androidx.compose.ui.geometry.Size(w, h))
    drawRect(
        if (selected) Color(0xFFFFC857) else Color(0xFF4A6080),
        topLeft, androidx.compose.ui.geometry.Size(w, h),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = if (selected) 3f else 1.5f),
    )
    // proue : petit repère vers le haut du socle (heading 0)
    drawLine(Color(0xFF77D9A7), center, Offset(center.x, topLeft.y + h * 0.1f), strokeWidth = 2f)
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
