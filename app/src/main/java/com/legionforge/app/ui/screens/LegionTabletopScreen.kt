package com.legionforge.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.legionforge.app.domain.LegionTabletopGeometry
import com.legionforge.app.ui.theme.LegionColors
import com.legionforge.app.ui.theme.LegionDims
import com.legionforge.app.ui.theme.LegionType

/**
 * Mode Tabletop Legion — plateau 2D tactile.
 *
 * Couche de VISUALISATION au-dessus du mode Play : réutilise les données synchronisées
 * (wounds, cover, defeated, tokens) sans dupliquer le moteur de règles.
 *
 * Règles appliquées (voir expert règles v2.6) :
 * - socles ronds 25/40/50 mm, mesure en pouces (1" = 25,4 mm), bord-à-bord ;
 * - arc avant 90° (APPROXIMATION : le rulebook ne chiffre pas d'angle) ;
 * - couvert manuel aucun/léger/lourd (0/1/2).
 *
 * Reste MANUEL (arbitrage joueur) : LOS 3D, nature du terrain, lancer des dés physiques,
 * keywords de cartes, scoring.
 */
data class LegionPawn(
    val id: String,
    val name: String,
    val xMm: Float,
    val yMm: Float,
    val headingDeg: Float = 0f,
    val baseSize: String = "small",   // small | medium | large
    val maxWounds: Int = 1,
    val wounds: Int = 1,
    val speed: Int = 1,
    val cover: Int = 0,               // 0/1/2
    val activated: Boolean = false,
    val defeated: Boolean = false,
    val isOpp: Boolean = false,
    val canRotate: Boolean = false,   // socle cranté (pivot avant Move)
    val imagePath: String? = null,    // miniature de carte locale (assets), token légal
)

private enum class LegionTool { SELECT, RANGE, ARC }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegionTabletopScreen(
    pawns: List<LegionPawn>,
    round: Int = 1,
    unitsLeft: Int = pawns.count { !it.defeated },
    onBack: () -> Unit = {},
    onOpenPlay: (String) -> Unit = {},
    onActivate: (String) -> Unit = {},
    onCoverChange: (String, Int) -> Unit = { _, _ -> },
    onMove: (String, Float, Float, Float) -> Unit = { _, _, _, _ -> },
) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    var targetId by remember { mutableStateOf<String?>(null) }
    var tool by remember { mutableStateOf(LegionTool.SELECT) }
    var scale by remember { mutableFloatStateOf(2.2f) }
    var pan by remember { mutableStateOf<Offset?>(null) } // null = pas encore cadré
    var measureEnd by remember { mutableStateOf<Offset?>(null) }
    var showDice by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val appCtx = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val cardBitmaps = remember(pawns) {
        pawns.mapNotNull { it.imagePath }.distinct().associateWith { loadLegionAssetBitmap(appCtx, it) }
    }

    val selected = pawns.firstOrNull { it.id == selectedId }
    val target = pawns.firstOrNull { it.id == targetId }

    // Conversion plateau (mm, centré 0,0) -> écran (px).
    // Centre du viewport en px (millieu de l'ecran), resolu apres mesure.
    var viewCenter by remember { mutableStateOf(Offset.Zero) }
    fun effPan(): Offset = pan ?: viewCenter
    fun toScreen(p: LegionPawn): Offset = Offset(
        effPan().x + p.xMm * LegionTabletopGeometry.LEGION_PX_PER_MM * scale,
        effPan().y + p.yMm * LegionTabletopGeometry.LEGION_PX_PER_MM * scale,
    )
    fun screenToMm(at: Offset): Pair<Float, Float> = Pair(
        (at.x - effPan().x) / (LegionTabletopGeometry.LEGION_PX_PER_MM * scale),
        (at.y - effPan().y) / (LegionTabletopGeometry.LEGION_PX_PER_MM * scale),
    )

    Box(Modifier.fillMaxSize().background(LegionColors.Background)) {
        // ── Plateau ──────────────────────────────────────────────────────────
        Canvas(
            Modifier.fillMaxSize().onSizeChanged { sz ->
                viewCenter = Offset(sz.width / 2f, sz.height / 2f)
            }.pointerInput(pawns, scale, effPan()) {
                detectTapGestures { at ->
                    val hit = pawns.lastOrNull { p ->
                        val c = toScreen(p)
                        val r = LegionTabletopGeometry.baseRadiusPx(p.baseSize, scale) + 8f
                        (at - c).getDistance() <= r
                    }
                    when {
                        hit == null -> { selectedId = null; targetId = null; measureEnd = null }
                        tool == LegionTool.RANGE && selected != null && hit.id != selectedId -> targetId = hit.id
                        selectedId == null -> selectedId = hit.id
                        hit.id == selectedId -> { /* déjà sélectionné */ }
                        else -> targetId = hit.id
                    }
                }
                detectDragGestures(
                    onDragStart = { at ->
                        val hit = pawns.lastOrNull { p ->
                            val c = toScreen(p)
                            (at - c).getDistance() <= LegionTabletopGeometry.baseRadiusPx(p.baseSize, scale) + 8f
                        }
                        if (hit != null && tool == LegionTool.SELECT) selectedId = hit.id
                    },
                    onDrag = { change, delta ->
                        change.consume()
                        val hit = selectedId?.let { id -> pawns.firstOrNull { it.id == id } }
                        if (hit != null && tool == LegionTool.SELECT) {
                            val (mx, my) = screenToMm(change.position)
                            onMove(hit.id, mx, my, hit.headingDeg)
                        } else {
                            pan = effPan() + delta
                        }
                        if (tool == LegionTool.RANGE) measureEnd = change.position
                    },
                )
            }
        ) {
            drawLegionBoard(scale, effPan(), selected, target, tool, measureEnd, pawns, ::toScreen, cardBitmaps)
        }

        // ── Barre haute 48dp ─────────────────────────────────────────────────
        Column(Modifier.fillMaxWidth().background(LegionColors.Surface)) {
            Row(
                Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(LegionDims.TouchMin)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Retour", tint = LegionColors.TextPrimary)
                }
                Spacer(Modifier.width(4.dp))
                Column(Modifier.weight(1f)) {
                    Text("Tabletop Legion · Manche $round", color = LegionColors.TextPrimary, style = MaterialTheme.typography.labelLarge)
                    Text("$unitsLeft unité(s) restante(s)", color = LegionColors.TextTertiary, style = MaterialTheme.typography.labelSmall)
                }
                IconButton(onClick = { tool = LegionTool.RANGE }, modifier = Modifier.size(LegionDims.TouchMin)) {
                    Icon(Icons.Default.Build, contentDescription = "Mesurer la portée", tint = if (tool == LegionTool.RANGE) LegionColors.Accent else LegionColors.TextSecondary)
                }
                IconButton(
                    onClick = {
                        scale = 2.2f; pan = null
                        selectedId = null; targetId = null
                    },
                    modifier = Modifier.size(LegionDims.TouchMin),
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Recadrer", tint = LegionColors.TextSecondary)
                }
            }
        }

        // ── Barre contextuelle ancrée au pion (non modale) ──────────────────
        AnimatedVisibility(visible = selected != null, modifier = Modifier.align(Alignment.BottomCenter)) {
            selected?.let { p ->
                Column(
                    Modifier
                        .padding(horizontal = 16.dp, vertical = 60.dp)
                        .background(LegionColors.SurfaceRaised, RoundedCornerShape(LegionDims.Radius))
                        .padding(LegionDims.SpaceS),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(p.name, color = LegionColors.Accent, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text("${p.wounds}/${p.maxWounds} blessures · ${p.speed}\" · socle ${p.baseSize}", color = LegionColors.TextSecondary, style = MaterialTheme.typography.labelSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        SmallAction("Activer", p.activated) { onActivate(p.id) }
                        SmallAction("Arc", tool == LegionTool.ARC) { tool = if (tool == LegionTool.ARC) LegionTool.SELECT else LegionTool.ARC }
                        SmallAction("Couvert ${p.cover}", p.cover > 0) {
                            onCoverChange(p.id, (p.cover + 1) % 3)
                        }
                        SmallAction("Dés", false) { showDice = true }
                    }
                    SmallAction("Fiche Play", false) { onOpenPlay(p.id) }
                }
            }
        }

        // ── Feuille de dés compacte ──────────────────────────────────────────
        if (showDice) LegionCompactDiceSheet(onClose = { showDice = false })

        // ── Aide mesure ──────────────────────────────────────────────────────
        if (tool == LegionTool.RANGE && target != null && selected != null) {
            val dMm = LegionTabletopGeometry.edgeDistanceMm(
                selected.xMm, selected.yMm, LegionTabletopGeometry.baseRadiusMm(selected.baseSize),
                target.xMm, target.yMm, LegionTabletopGeometry.baseRadiusMm(target.baseSize),
            )
            val inches = LegionTabletopGeometry.roundHalf(LegionTabletopGeometry.mmToInches(dMm))
            Surface(
                Modifier.align(Alignment.TopCenter).padding(top = 60.dp),
                color = LegionColors.SurfaceRaised,
                shape = RoundedCornerShape(LegionDims.Radius),
            ) {
                Text(
                    "${LegionTabletopGeometry.formatInches(inches)} · bande ${LegionTabletopGeometry.rangeBand(dMm)}",
                    Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = LegionColors.Accent,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun SmallAction(label: String, active: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = if (active) LegionColors.Accent else LegionColors.Border,
        modifier = Modifier.heightIn(min = LegionDims.TouchMin),
    ) {
        Text(
            label,
            Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            color = if (active) LegionColors.Background else LegionColors.TextSecondary,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun DrawScope.drawLegionBoard(
    scale: Float,
    pan: Offset,
    selected: LegionPawn?,
    target: LegionPawn?,
    tool: LegionTool,
    measureEnd: Offset?,
    pawns: List<LegionPawn>,
    toScreen: (LegionPawn) -> Offset,
    cardBitmaps: Map<String, androidx.compose.ui.graphics.ImageBitmap?>,
) {
    val center = pan // plateau centré sur (0,0) → écran = centre du viewport
    // Grille en pouces (1" = 25,4mm).
    val inchPx = LegionTabletopGeometry.MM_PER_INCH * LegionTabletopGeometry.LEGION_PX_PER_MM * scale
    var g = center.x % inchPx
    while (g < size.width) {
        drawLine(LegionColors.Border, Offset(g, 0f), Offset(g, size.height), strokeWidth = 1.5f, alpha = 0.6f)
        g += inchPx
    }
    g = center.y % inchPx
    while (g < size.height) {
        drawLine(LegionColors.Border, Offset(0f, g), Offset(size.width, g), strokeWidth = 1.5f, alpha = 0.6f)
        g += inchPx
    }
    // Anneaux de portée autour de l'unité sélectionnée (1..6").
    if (selected != null && (tool == LegionTool.RANGE || tool == LegionTool.ARC)) {
        val sc = toScreen(selected)
        for (i in 1..6) {
            drawCircle(
                LegionColors.Accent.copy(alpha = 0.18f),
                radius = LegionTabletopGeometry.rangeRingPx(i, scale),
                center = sc,
                style = Stroke(width = 1.5f),
            )
        }
    }
    // Ligne de mesure.
    if (selected != null && measureEnd != null) {
        drawLine(LegionColors.Accent, toScreen(selected), measureEnd, strokeWidth = 2f)
    }
    // Pions.
    pawns.forEach { p ->
        val c = toScreen(p)
        val r = LegionTabletopGeometry.baseRadiusPx(p.baseSize, scale)
        val base = when {
            p.defeated -> LegionColors.TextTertiary.copy(alpha = 0.5f)
            p.isOpp -> LegionColors.Danger
            else -> LegionColors.Info
        }
        // Arc frontal 90° (approximation).
        if (tool == LegionTool.ARC && !p.defeated) {
            drawArc(
                LegionColors.Accent.copy(alpha = 0.2f),
                startAngle = p.headingDeg - 45f,
                sweepAngle = 90f,
                useCenter = true,
                topLeft = Offset(c.x - r * 1.6f, c.y - r * 1.6f),
                size = androidx.compose.ui.geometry.Size(r * 3.2f, r * 3.2f),
            )
        }
        val bmp = p.imagePath?.let { cardBitmaps[it] }
        if (bmp != null) {
            // Miniature de carte légale (déjà dans l'app) dessinée en disque sur le socle.
            val side = r * 2f
            drawImage(bmp, dstOffset = androidx.compose.ui.unit.IntOffset((c.x - r).toInt(), (c.y - r).toInt()), dstSize = androidx.compose.ui.unit.IntSize(side.toInt(), side.toInt()))
        } else {
            drawCircle(base, radius = r, center = c)
        }
        drawCircle(
            if (p.id == selected?.id) LegionColors.Accent
            else if (p.activated) LegionColors.Success
            else LegionColors.Border,
            radius = r, center = c, style = Stroke(width = if (p.id == selected?.id) 3f else 2f),
        )
        // Couvert = bande warning sur le bord.
        if (p.cover > 0 && !p.defeated) {
            drawArc(
                LegionColors.Warning,
                startAngle = -90f,
                sweepAngle = 90f * p.cover,
                useCenter = false,
                topLeft = Offset(c.x - r, c.y - r),
                size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
                style = Stroke(width = 4.dp.toPx()),
            )
        }
    }
    // Ligne de visée tireur -> cible.
    if (selected != null && target != null) {
        drawLine(LegionColors.Accent, toScreen(selected), toScreen(target), strokeWidth = 2f)
    }
}

/** Feuille de dés compacte (peek), sans clavier ni saisie numérique. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LegionCompactDiceSheet(onClose: () -> Unit) {
    var mode by remember { mutableStateOf("attack") }
    var red by remember { mutableIntStateOf(2) }
    var black by remember { mutableIntStateOf(0) }
    var white by remember { mutableIntStateOf(0) }
    var lastRoll by remember { mutableStateOf<List<String>>(emptyList()) }

    ModalBottomSheet(onDismissRequest = onClose, containerColor = LegionColors.Surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Lancer de dés", color = LegionColors.Accent, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip("Attaque", mode == "attack") { mode = "attack" }
                FilterChip("Défense", mode == "defense") { mode = "defense" }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                LegionDieStepper("R", red, LegionColors.Danger) { red = it }
                LegionDieStepper("N", black, LegionColors.TextSecondary) { black = it }
                LegionDieStepper("B", white, LegionColors.Info) { white = it }
            }
            Button(
                onClick = {
                    val faces = if (mode == "defense") listOf("Bloc", "Bloc", "Bloc", "Surge", "Vide", "Vide", "Vide", "Vide")
                    else listOf("Hit", "Hit", "Crit", "Surge", "Vide", "Vide", "Vide", "Vide")
                    lastRoll = buildList { repeat(red) { add(faces.random()) }; repeat(black) { add(faces.random()) }; repeat(white) { add(faces.random()) } }
                },
                Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
            ) { Text("Lancer", fontSize = LegionType.Body, fontWeight = FontWeight.Bold) }
            if (lastRoll.isNotEmpty()) {
                Text(
                    "Résultats : ${lastRoll.joinToString(" · ")}",
                    color = LegionColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
                val successes = lastRoll.count { it == "Hit" || it == "Crit" || it == "Bloc" }
                Text("$successes succè(s)", color = LegionColors.Accent, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FilterChip(label: String, active: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(8.dp), color = if (active) LegionColors.Accent else LegionColors.Border) {
        Text(label, Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = if (active) LegionColors.Background else LegionColors.TextSecondary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LegionDieStepper(label: String, value: Int, color: Color, onChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(onClick = { onChange((value + 1).coerceAtMost(8)) }, shape = RoundedCornerShape(6.dp), color = LegionColors.Border, modifier = Modifier.size(LegionDims.TouchMin)) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { Text("+", color = LegionColors.TextPrimary, style = MaterialTheme.typography.titleMedium) }
        }
        Text("$label $value", color = color, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Surface(onClick = { onChange((value - 1).coerceAtLeast(0)) }, shape = RoundedCornerShape(6.dp), color = LegionColors.Border, modifier = Modifier.size(LegionDims.TouchMin)) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { Text("−", color = LegionColors.TextPrimary, style = MaterialTheme.typography.titleMedium) }
        }
    }
}


/** Charge un bitmap depuis les assets (miniature de carte Legion locale). */
fun loadLegionAssetBitmap(context: Context, path: String): androidx.compose.ui.graphics.ImageBitmap? = try {
    context.assets.open(path).use { stream ->
        val bmp = BitmapFactory.decodeStream(stream)
        bmp?.asImageBitmap()
    }
} catch (_: Exception) { null }
