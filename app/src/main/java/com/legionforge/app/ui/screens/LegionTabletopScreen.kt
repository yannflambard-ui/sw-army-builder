package com.legionforge.app.ui.screens

import com.legionforge.app.R

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
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
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
    onBack: () -> Unit,
    onOpenPlay: (String) -> Unit,
    onActivate: (String) -> Unit,
    onCoverChange: (String, Int) -> Unit,
    onMove: (String, Float, Float, Float) -> Unit,
    onRoundChange: (Int) -> Unit,
    onWoundsChange: (String, Int) -> Unit,
) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    var targetId by remember { mutableStateOf<String?>(null) }
    var tool by remember { mutableStateOf(LegionTool.SELECT) }
    // Viewport synchrone (PAS via onSizeChanged, non déterministe en CI Roborazzi).
    val cfg = LocalConfiguration.current
    val densityF = LocalDensity.current.density
    val vpW = (cfg.screenWidthDp * densityF).toInt().coerceAtLeast(1)
    val vpH = (cfg.screenHeightDp * densityF).toInt().coerceAtLeast(1)
    // Fit SYNCHRONE dès la 1re composition : déterministe en local comme en CI.
    var scale by remember {
        val padMm = 45f
        val s = if (pawns.isEmpty()) 2.2f else {
            val spanX = (pawns.maxOf { it.xMm } - pawns.minOf { it.xMm } + 2 * padMm) * LegionTabletopGeometry.LEGION_PX_PER_MM
            val spanY = (pawns.maxOf { it.yMm } - pawns.minOf { it.yMm } + 2 * padMm) * LegionTabletopGeometry.LEGION_PX_PER_MM
            minOf(vpW.toFloat() / spanX, vpH.toFloat() / spanY).coerceIn(0.6f, 4f)
        }
        mutableFloatStateOf(s)
    }

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
    var viewCenter by remember { mutableStateOf(Offset(vpW / 2f, vpH / 2f)) }
    fun effPan(): Offset = pan ?: run {
        if (pawns.isEmpty()) viewCenter else Offset(
            viewCenter.x - (pawns.maxOf { it.xMm } + pawns.minOf { it.xMm }) / 2f * LegionTabletopGeometry.LEGION_PX_PER_MM * scale,
            viewCenter.y - (pawns.maxOf { it.yMm } + pawns.minOf { it.yMm }) / 2f * LegionTabletopGeometry.LEGION_PX_PER_MM * scale,
        )
    }
    fun toScreen(p: LegionPawn): Offset = Offset(
        effPan().x + p.xMm * LegionTabletopGeometry.LEGION_PX_PER_MM * scale,
        effPan().y + p.yMm * LegionTabletopGeometry.LEGION_PX_PER_MM * scale,
    )
    fun screenToMm(at: Offset): Pair<Float, Float> = Pair(
        (at.x - effPan().x) / (LegionTabletopGeometry.LEGION_PX_PER_MM * scale),
        (at.y - effPan().y) / (LegionTabletopGeometry.LEGION_PX_PER_MM * scale),
    )

    fun fitScale(widthPx: Int, heightPx: Int): Float {
        if (pawns.isEmpty()) return 2.2f
        val padMm = 45f
        val spanX = (pawns.maxOf { it.xMm } - pawns.minOf { it.xMm } + 2 * padMm) * LegionTabletopGeometry.LEGION_PX_PER_MM
        val spanY = (pawns.maxOf { it.yMm } - pawns.minOf { it.yMm } + 2 * padMm) * LegionTabletopGeometry.LEGION_PX_PER_MM
        return minOf(widthPx.toFloat() / spanX, heightPx.toFloat() / spanY).coerceIn(0.6f, 4f)
    }
    // Fit SYNCHRONE : deterministe en local comme en CI (pas de race onSizeChanged).
    LaunchedEffect(vpW, vpH, pawns) { scale = fitScale(vpW, vpH) }

    Box(Modifier.fillMaxSize().background(LegionColors.Background)) {
        // ── Plateau ──────────────────────────────────────────────────────────
        val textMeasurer = rememberTextMeasurer()
        Canvas(
            Modifier.fillMaxSize().pointerInput(pawns, scale, effPan()) {
                detectTapGestures { at ->
                    val hit = pawns.lastOrNull { p ->
                        val c = toScreen(p)
                        val r = maxOf(LegionTabletopGeometry.baseRadiusPx(p.baseSize, scale) + 8f, LegionDims.TouchMin.toPx() / 2f)
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
                            (at - c).getDistance() <= maxOf(LegionTabletopGeometry.baseRadiusPx(p.baseSize, scale) + 8f, LegionDims.TouchMin.toPx() / 2f)
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
            drawLegionBoard(scale, effPan(), selected, target, tool, measureEnd, pawns, ::toScreen, cardBitmaps, textMeasurer)
        }

        // ── Barre haute 48dp ─────────────────────────────────────────────────
        Column(Modifier.fillMaxWidth().background(LegionColors.Surface)) {
            Row(
                Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(LegionDims.TouchMin)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.tabletop_back), tint = LegionColors.TextPrimary)
                }
                Spacer(Modifier.width(4.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (cfg.screenWidthDp < 380) stringResource(R.string.tabletop_title_short, round)
                        else stringResource(R.string.tabletop_title, round),
                        color = LegionColors.TextPrimary, style = MaterialTheme.typography.titleMedium, maxLines = 1,
                    )
                    Text(stringResource(R.string.tabletop_units_left, unitsLeft), color = LegionColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
                }
                IconButton(
                    onClick = { tool = if (tool == LegionTool.RANGE) LegionTool.SELECT else LegionTool.RANGE },
                    modifier = Modifier.size(LegionDims.TouchMin),
                ) {
                    Icon(Icons.Default.Build, contentDescription = stringResource(R.string.tabletop_measure), tint = if (tool == LegionTool.RANGE) LegionColors.Accent else LegionColors.TextSecondary)
                }
                Surface(onClick = { onRoundChange(round + 1) }, shape = RoundedCornerShape(LegionDims.Radius), color = LegionColors.Accent, modifier = Modifier.heightIn(min = LegionDims.TouchMin)) {
                    Row(Modifier.padding(horizontal = LegionDims.SpaceS), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = LegionColors.Background, modifier = Modifier.size(18.dp))
                        Text(stringResource(R.string.tabletop_end_round), color = LegionColors.Background, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
                IconButton(
                    onClick = {
                        pan = null; scale = fitScale(vpW, vpH)
                        selectedId = null; targetId = null
                        tool = LegionTool.SELECT; measureEnd = null; showDice = false
                    },
                    modifier = Modifier.size(LegionDims.TouchMin),
                ) {
                    Icon(Icons.Default.Info, contentDescription = stringResource(R.string.tabletop_recenter), tint = LegionColors.TextSecondary)
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
                    Text(stringResource(R.string.tabletop_wounds, p.wounds, p.maxWounds, p.speed, p.baseSize), color = LegionColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        SmallAction("−", false) { onWoundsChange(p.id, p.wounds - 1) }
                        SmallAction("+", false) { onWoundsChange(p.id, p.wounds + 1) }
                        SmallAction(stringResource(R.string.tabletop_activate), p.activated) { onActivate(p.id) }
                        if (p.canRotate) SmallAction(stringResource(R.string.tabletop_rotate), false) { onMove(p.id, p.xMm, p.yMm, (p.headingDeg + 90f) % 360f) }
                        if (!p.defeated && p.baseSize != "small") SmallAction(stringResource(R.string.tabletop_arc), tool == LegionTool.ARC) { tool = if (tool == LegionTool.ARC) LegionTool.SELECT else LegionTool.ARC }
                        SmallAction(stringResource(R.string.tabletop_cover, p.cover), p.cover > 0) {
                            onCoverChange(p.id, (p.cover + 1) % 3)
                        }
                        SmallAction(stringResource(R.string.tabletop_dice), false) { showDice = true }
                    }
                    SmallAction(stringResource(R.string.tabletop_open_play), false) { onOpenPlay(p.id) }
                }
            }
        }

        // ── Feuille de dés compacte ──────────────────────────────────────────
        if (showDice) LegionCompactDiceSheet(
            cover = pawns.firstOrNull { it.id == selectedId }?.cover ?: 0,
            onClose = { showDice = false },
        )

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
                    "mesure ~${LegionTabletopGeometry.formatInches(inches)} · Portée ${LegionTabletopGeometry.rangeBand(dMm)}",
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
    textMeasurer: TextMeasurer,
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
        // Arc = socles crantés uniquement (rulebook:2686-2689). 90° = approximation
        // d'affichage, PAS une restriction de tir (360° par défaut).
        if (tool == LegionTool.ARC && !p.defeated && p.baseSize != "small") {
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
        // Couvert NON directionnel (rulebook:2229-2260) : anneau neutre, pas un secteur.
        if (p.cover > 0 && !p.defeated) {
            drawCircle(LegionColors.Warning, radius = r + 4.dp.toPx(), center = c, style = Stroke(width = (2 * p.cover).dp.toPx()))
        }
        // Info en 2s : nom + blessures sous chaque socle, lisible sans tap.
        drawRoundRect(
            color = LegionColors.Background.copy(alpha = 0.85f),
            topLeft = Offset(c.x - r, c.y + r + 4.dp.toPx()),
            size = Size(2 * r, 18.dp.toPx()),
            cornerRadius = CornerRadius(6.dp.toPx()),
        )
        drawText(
            textMeasurer = textMeasurer,
            text = "${p.name} ${p.wounds}/${p.maxWounds}" + if (p.cover > 0) " C${p.cover}" else "",
            topLeft = Offset(c.x - r + 3.dp.toPx(), c.y + r + 5.dp.toPx()),
            style = TextStyle(fontSize = LegionType.Caption, fontWeight = FontWeight.Bold,
                color = if (p.isOpp) LegionColors.Danger else LegionColors.TextPrimary),
        )
    }
    // Ligne de visée tireur -> cible.
    if (selected != null && target != null) {
        drawLine(LegionColors.Accent, toScreen(selected), toScreen(target), strokeWidth = 2f)
    }
}

/** Feuille de dés compacte (peek), sans clavier ni saisie numérique. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LegionCompactDiceSheet(cover: Int, onClose: () -> Unit) {
    var mode by remember { mutableStateOf("attack") }
    var red by remember { mutableIntStateOf(2) }
    var black by remember { mutableIntStateOf(0) }
    var white by remember { mutableIntStateOf(0) }
    var lastRoll by remember { mutableStateOf<List<String>>(emptyList()) }

    ModalBottomSheet(onDismissRequest = onClose, containerColor = LegionColors.Surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.tabletop_roll_dice), color = LegionColors.Accent, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.tabletop_dice_note), color = LegionColors.TextTertiary, style = MaterialTheme.typography.labelSmall)
            if (cover > 0) Text(stringResource(R.string.tabletop_cover, cover), color = LegionColors.Warning, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(stringResource(R.string.tabletop_attack), mode == "attack") { mode = "attack"; black = 0; lastRoll = emptyList() }
                FilterChip(stringResource(R.string.tabletop_defense), mode == "defense") { mode = "defense"; black = 0; lastRoll = emptyList() }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                LegionDieStepper("R", red, LegionColors.Danger) { red = it }
                // Le dé noir n'existe PAS en défense (rulebook:336) : masqué, pas simplification.
                if (mode == "attack") LegionDieStepper("N", black, LegionColors.TextSecondary) { black = it }
                else Spacer(Modifier.size(LegionDims.TouchMin))
                LegionDieStepper("B", white, LegionColors.Info) { white = it }
            }
            Button(
                onClick = {
                    // Une liste de faces par COULEUR et par MODE (LegionDiceRules, source FFG).
                    lastRoll = buildList {
                        repeat(red) { add(com.legionforge.app.domain.LegionDiceRules.faces(mode, "red").random()) }
                        if (mode == "attack") repeat(black) { add(com.legionforge.app.domain.LegionDiceRules.faces(mode, "black").random()) }
                        repeat(white) { add(com.legionforge.app.domain.LegionDiceRules.faces(mode, "white").random()) }
                    }
                },
                Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
            ) { Text(stringResource(R.string.tabletop_roll), fontSize = LegionType.Body, fontWeight = FontWeight.Bold) }
            if (lastRoll.isNotEmpty()) {
                Text(stringResource(R.string.tabletop_results, lastRoll.joinToString(" · ")), color = LegionColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
                val successes = if (mode == "attack")
                    com.legionforge.app.domain.LegionDiceRules.hitsAfterCover(lastRoll, cover)
                else lastRoll.count { com.legionforge.app.domain.LegionDiceRules.isSuccess(mode, it) }
                Text(stringResource(R.string.tabletop_successes, successes), color = LegionColors.Accent, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                if (mode == "attack" && cover > 0) Text("Couvert appliqué : −$cover Hit(s) (− Crit)", color = LegionColors.TextTertiary, style = MaterialTheme.typography.labelSmall)
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
