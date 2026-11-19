package com.legionforge.app.ui.screens

import com.legionforge.app.R

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
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
import androidx.compose.ui.platform.testTag
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.legionforge.app.domain.LegionTabletopGeometry
import com.legionforge.app.ui.theme.LegionColors
import com.legionforge.app.ui.theme.LegionDims
import com.legionforge.app.ui.theme.LegionType

/**
 * Mode Tabletop Legion — plateau 2D tactile.
 *
 * ⚠ ÉTAT LOCAL NON SYNCHRONISÉ entre appareils. L'adversaire est affiché (Nearby/solo)
 * mais les déplacements, activations, couverts et blessures ne sont PAS transmis à
 * l'autre joueur. Les positions ne sont PAS persistées. A utiliser « à côté de la table ».
 *
 * RÈGLES APPLIQUÉES (sources vérifiées) :
 * - bandes de portée = segments de réglette de 6" (rulebook:648-664 ; legion.takras.net/range) ;
 *   mesure bord-à-bord en pouces (1" = 25,4 mm, convention d'échelle) ;
 * - socles : petits = trooper/clone/droid/wookiee, tout le reste cranté (rulebook:298-303) ;
 *   modélisés en DISQUES (approximation : l'empreinte crantée et ses 4 arcs ne sont pas dessinés) ;
 * - arc avant 90° = APPROXIMATION d'affichage, PAS une restriction de tir (la règle ne
 *   chiffre aucun angle ; les socles crantés ont front/sides/rear, rulebook:2686-2689) ;
 * - couvert 0/1/2 saisi manuellement ; annule 1 Hit par niveau, jamais de Crit
 *   (rulebook:2268-2272) ; la LOS dépendant du terrain reste à arbitrer (rulebook:2226-2260) ;
 * - dés : attaque d8 / défense d6, faces FFG (LegionDiceRules).
 *
 * RESTE MANUEL — NON IMPLÉMENTÉ (arbitrage joueur) :
 * LOS/terrain 3D, mêlée et contact de socles, cohésion, conversion des Surges
 * (selon les keywords/upgrades de carte), jetons Aim/Dodge/Surge/Standby/Suppression,
 * tour d'activation, victoire/VP et départage (rulebook:1332-1339), keywords de cartes.
 * La feuille de dés est un CALCULATEUR DE POOL, pas une résolution d'attaque complète.
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
        // Cadrage initial sur le PLAYMAT ENTIER (36") pour voir les zones de déploiement,
    // les objectifs et les graduations dès l'ouverture. L'utilisateur zoome ensuite
    // sur ses pions au besoin (boutons −/+ ou pinch).
    val matSpanPx = 2f * 18f * LegionTabletopGeometry.MM_PER_INCH * LegionTabletopGeometry.LEGION_PX_PER_MM
    val s = minOf(vpW.toFloat(), vpH.toFloat()) / matSpanPx
    mutableFloatStateOf(s.coerceIn(0.25f, 4f))
    }

    var pan by remember { mutableStateOf<Offset?>(null) } // null = pas encore cadré
    // Translation figée pendant le drag d'un pion : effPan() recalculerait sinon
    // la bounding-box à chaque frame et la coordonnée mm dériverait sous le doigt.
    var dragPan by remember { mutableStateOf<Offset?>(null) }
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
    fun effPan(): Offset = dragPan ?: pan ?: viewCenter // centré sur le tapis (0,0)
    fun toScreen(p: LegionPawn): Offset = Offset(
        effPan().x + p.xMm * LegionTabletopGeometry.LEGION_PX_PER_MM * scale,
        effPan().y + p.yMm * LegionTabletopGeometry.LEGION_PX_PER_MM * scale,
    )
    fun screenToMm(at: Offset): Pair<Float, Float> = Pair(
        (at.x - effPan().x) / (LegionTabletopGeometry.LEGION_PX_PER_MM * scale),
        (at.y - effPan().y) / (LegionTabletopGeometry.LEGION_PX_PER_MM * scale),
    )

    fun fitScale(widthPx: Int, heightPx: Int): Float {
        // Fit sur le tapis entier (36") : les zones de déploiement, les objectifs
        // et les graduations sont visibles dès l'ouverture, comme un vrai playmat.
        val matSpanPx = 2f * 18f * LegionTabletopGeometry.MM_PER_INCH * LegionTabletopGeometry.LEGION_PX_PER_MM
        return (minOf(widthPx.toFloat(), heightPx.toFloat()) / matSpanPx).coerceIn(0.25f, 4f)
    }
    // Fit SYNCHRONE : deterministe en local comme en CI (pas de race onSizeChanged).
    // Clé = IDENTITÉ des pions, PAS leurs positions : sans cela, chaque onMove
    // redéclenche le fit et la transformation bouge pendant le drag.
    val pawnIds = pawns.joinToString("|") { it.id }
    LaunchedEffect(vpW, vpH, pawnIds) { scale = fitScale(vpW, vpH) }

    // Réfs STABLES pour le pointerInput : sans cela, pawns/scale/effPan changent
    // à chaque frame de drag, ce qui redémarre detectDragGestures et casse le geste.
    val livePawns by rememberUpdatedState(pawns)
    val liveScale by rememberUpdatedState(scale)
    val livePan by rememberUpdatedState(effPan())
    val liveTool by rememberUpdatedState(tool)
    val liveOnMove by rememberUpdatedState(onMove)
    val liveOnOpenPlay by rememberUpdatedState(onOpenPlay)
    fun toScreenLive(p: LegionPawn): Offset = Offset(
        livePan.x + p.xMm * LegionTabletopGeometry.LEGION_PX_PER_MM * liveScale,
        livePan.y + p.yMm * LegionTabletopGeometry.LEGION_PX_PER_MM * liveScale,
    )
    fun screenToMmLive(at: Offset): Pair<Float, Float> = Pair(
        (at.x - livePan.x) / (LegionTabletopGeometry.LEGION_PX_PER_MM * liveScale),
        (at.y - livePan.y) / (LegionTabletopGeometry.LEGION_PX_PER_MM * liveScale),
    )

    Box(Modifier.fillMaxSize().background(LegionColors.Background)) {
        // ── Plateau ──────────────────────────────────────────────────────────
        val textMeasurer = rememberTextMeasurer()
        Canvas(
            Modifier.fillMaxSize().testTag("legion_board").pointerInput(Unit) {
                // Pinch-zoom (2 doigts) : absent auparavant sur Legion. Gardé figé pendant
                // le drag d'un pion (dragPan != null) pour ne pas faire dériver la coordonnée mm.
                detectTransformGestures { centroid, panChange, zoom, _ ->
                    if (dragPan == null && (zoom != 1f || panChange != androidx.compose.ui.geometry.Offset.Zero)) {
                        val old = scale
                        val next = (scale * zoom).coerceIn(0.25f, 6f)
                        val base = pan ?: viewCenter
                        // Le plateau sous le centroid reste sous le centroid après zoom :
                        // pan' = centroid - (centroid - pan) * (next/old) + panChange
                        val ratio = next / old
                        pan = androidx.compose.ui.geometry.Offset(
                            centroid.x - (centroid.x - base.x) * ratio + panChange.x,
                            centroid.y - (centroid.y - base.y) * ratio + panChange.y,
                        )
                        scale = next
                    }
                }
                detectTapGestures { at ->
                    val hit = livePawns.lastOrNull { p ->
                        val c = toScreenLive(p)
                        val r = maxOf(LegionTabletopGeometry.baseRadiusPx(p.baseSize, liveScale) + 8f, LegionDims.TouchMin.toPx() / 2f)
                        (at - c).getDistance() <= r
                    }
                    when {
                        hit == null -> { selectedId = null; targetId = null; measureEnd = null }
                        liveTool == LegionTool.RANGE && selected != null && hit.id != selectedId -> targetId = hit.id
                        liveTool == LegionTool.SELECT -> {
                            // Tap sur un pion = sélection ET ouverture de la fiche dépliante
                            // du mode Play (même écran, même contenu que l'utilisateur connaît).
                            selectedId = hit.id
                            liveOnOpenPlay(hit.id)
                        }
                        else -> targetId = hit.id
                    }
                }
                detectDragGestures(
                    onDragStart = { at ->
                        val hit = livePawns.lastOrNull { p ->
                            val c = toScreenLive(p)
                            (at - c).getDistance() <= maxOf(LegionTabletopGeometry.baseRadiusPx(p.baseSize, liveScale) + 8f, LegionDims.TouchMin.toPx() / 2f)
                        }
                        if (hit != null && liveTool == LegionTool.SELECT) {
                            selectedId = hit.id
                            dragPan = livePan // figer la translation pendant le geste
                        }
                    },
                    onDrag = { change, delta ->
                        change.consume()
                        val hit = selectedId?.let { id -> livePawns.firstOrNull { it.id == id } }
                        if (hit != null && liveTool == LegionTool.SELECT) {
                            val (mx, my) = screenToMmLive(change.position)
                            liveOnMove(hit.id, mx, my, hit.headingDeg)
                        } else {
                            pan = livePan + delta
                        }
                        if (liveTool == LegionTool.RANGE) measureEnd = change.position
                    },
                    onDragEnd = { if (dragPan != null) pan = dragPan; dragPan = null },
                    onDragCancel = { dragPan = null },
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
                    Text(stringResource(R.string.tabletop_units_left, unitsLeft), color = LegionColors.TextSecondary, style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
                }
                IconButton(onClick = { val base = pan ?: viewCenter; val next = (scale / 1.25f).coerceIn(0.25f, 6f); pan = base; scale = next }, modifier = Modifier.size(LegionDims.TouchMin)) {
                    Text("−", color = LegionColors.TextPrimary, fontSize = 22.sp)
                }
                Text("${"%.0f".format(scale * 100)}%", color = LegionColors.TextSecondary, fontSize = 10.sp)
                IconButton(onClick = { val base = pan ?: viewCenter; val next = (scale * 1.25f).coerceIn(0.25f, 6f); pan = base; scale = next }, modifier = Modifier.size(LegionDims.TouchMin)) {
                    Text("+", color = LegionColors.TextPrimary, fontSize = 22.sp)
                }
                IconButton(
                    onClick = { tool = if (tool == LegionTool.RANGE) LegionTool.SELECT else LegionTool.RANGE },
                    modifier = Modifier.size(LegionDims.TouchMin),
                ) {
                    Icon(Icons.Default.Build, contentDescription = stringResource(R.string.tabletop_measure), tint = if (tool == LegionTool.RANGE) LegionColors.Accent else LegionColors.TextSecondary)
                }
                Surface(onClick = { onRoundChange(round + 1) }, shape = RoundedCornerShape(LegionDims.Radius), color = LegionColors.Accent, modifier = Modifier.heightIn(min = LegionDims.TouchMin)) {
                    Row(Modifier.padding(horizontal = LegionDims.SpaceS), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = LegionColors.Background, modifier = Modifier.size(18.dp))
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
                        SmallAction(stringResource(R.string.tabletop_dice), false, enabled = targetId != null) { showDice = true }
                    }
                }
            }
        }

        // ── Feuille de dés compacte ──────────────────────────────────────────
        if (showDice) LegionCompactDiceSheet(
            cover = pawns.firstOrNull { it.id == targetId }?.cover ?: 0,
            targetName = pawns.firstOrNull { it.id == targetId }?.name ?: "",
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
private fun SmallAction(label: String, active: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(6.dp),
        color = if (active) LegionColors.Accent else if (enabled) LegionColors.Border else LegionColors.SurfaceRaised,
        modifier = Modifier.heightIn(min = LegionDims.TouchMin).widthIn(min = LegionDims.TouchMin),
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
    val pxPerMm = LegionTabletopGeometry.LEGION_PX_PER_MM * scale
    fun mm(x: Float, y: Float) = Offset(center.x + x * pxPerMm, center.y + y * pxPerMm)
    val inchPx = LegionTabletopGeometry.MM_PER_INCH * pxPerMm

    // ── PLAYMAT 36"x36" (91,4 cm) : surface finie, pas une grille infinie ──
    val halfMat = 18f * LegionTabletopGeometry.MM_PER_INCH // 457,2 mm
    val tl = mm(-halfMat, -halfMat)
    val br = mm(halfMat, halfMat)
    val matSize = androidx.compose.ui.geometry.Size(br.x - tl.x, br.y - tl.y)

    // Halo extérieur (table sombre autour du tapis).
    drawRect(
        brush = androidx.compose.ui.graphics.Brush.radialGradient(
            colors = listOf(LegionColors.Surface, LegionColors.Background),
            center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f),
            radius = maxOf(size.width, size.height) * 0.75f,
        ),
        size = size,
    )
    // Surface du tapis (toile mate, dégradé central très discret).
    if (matSize.width > 0f && matSize.height > 0f) {
        drawRect(
            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                colors = listOf(LegionColors.SurfaceRaised, LegionColors.Surface),
                center = androidx.compose.ui.geometry.Offset(tl.x + matSize.width / 2f, tl.y + matSize.height / 2f),
                radius = maxOf(matSize.width, matSize.height) * 0.7f,
            ),
            topLeft = tl,
            size = matSize,
        )
        // Quadrillage en pouces, LIMITÉ au tapis.
        var gx = -halfMat
        while (gx <= halfMat) {
            val x = mm(gx, 0f).x
            if (x in tl.x..br.x) drawLine(LegionColors.Border.copy(alpha = 0.45f), Offset(x, tl.y), Offset(x, br.y), strokeWidth = 1f)
            gx += LegionTabletopGeometry.MM_PER_INCH
        }
        var gy = -halfMat
        while (gy <= halfMat) {
            val y = mm(0f, gy).y
            if (y in tl.y..br.y) drawLine(LegionColors.Border.copy(alpha = 0.45f), Offset(tl.x, y), Offset(br.x, y), strokeWidth = 1f)
            gy += LegionTabletopGeometry.MM_PER_INCH
        }
        // Double bordure du tapis.
        drawRect(LegionColors.Accent.copy(alpha = 0.8f), topLeft = tl, size = matSize, style = Stroke(width = 6f))
        drawRect(LegionColors.Border, topLeft = mm(-halfMat + 4f, -halfMat + 4f), size = androidx.compose.ui.geometry.Size(matSize.width - 8f, matSize.height - 8f), style = Stroke(width = 1.5f))

        // Zones de déploiement (12" depuis chaque bord) + lignes de déploiement.
        val dep = 12f * LegionTabletopGeometry.MM_PER_INCH
        val dHaut = androidx.compose.ui.geometry.Size(matSize.width, dep * pxPerMm)
        val dBas = androidx.compose.ui.geometry.Size(matSize.width, dep * pxPerMm)
        drawRect(LegionColors.Danger.copy(alpha = 0.10f), topLeft = tl, size = dHaut)
        drawRect(LegionColors.Info.copy(alpha = 0.10f), topLeft = mm(-halfMat, halfMat - dep), size = dBas)
        val dash = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
        val lineHaut = mm(0f, -halfMat + dep).y
        val lineBas = mm(0f, halfMat - dep).y
        drawLine(LegionColors.Danger.copy(alpha = 0.55f), Offset(tl.x, lineHaut), Offset(br.x, lineHaut), strokeWidth = 2f, pathEffect = dash)
        drawLine(LegionColors.Info.copy(alpha = 0.55f), Offset(tl.x, lineBas), Offset(br.x, lineBas), strokeWidth = 2f, pathEffect = dash)

        // Zones d'objectif (3 disques sur la ligne médiane, rayon 6").
        val objR = 6f * LegionTabletopGeometry.MM_PER_INCH * pxPerMm
        for (ox in listOf(-12f, 0f, 12f)) {
            val c = mm(ox * LegionTabletopGeometry.MM_PER_INCH, 0f)
            drawCircle(LegionColors.Accent.copy(alpha = 0.16f), radius = objR, center = c, style = Stroke(width = 2f, pathEffect = dash))
            drawCircle(LegionColors.Warning.copy(alpha = 0.7f), radius = 3f, center = c)
        }

        // Graduations en pouces le long des bords (toutes les 6").
        for (inch in -18..18 step 6) {
            val x = mm(inch * LegionTabletopGeometry.MM_PER_INCH, 0f).x
            val y = mm(0f, inch * LegionTabletopGeometry.MM_PER_INCH).y
            drawLine(LegionColors.TextSecondary, Offset(x, tl.y), Offset(x, tl.y + 10f), strokeWidth = 1.5f)
            drawLine(LegionColors.TextSecondary, Offset(x, br.y - 10f), Offset(x, br.y), strokeWidth = 1.5f)
            drawLine(LegionColors.TextSecondary, Offset(tl.x, y), Offset(tl.x + 10f, y), strokeWidth = 1.5f)
            drawLine(LegionColors.TextSecondary, Offset(br.x - 10f, y), Offset(br.x, y), strokeWidth = 1.5f)
        }

        // Légendes CENTRÉES dans la zone :meme sur petit ecran, si les bords du tapis
        // sortent du cadre, le texte reste lisible au centre du viewport.
        val legendStyle = androidx.compose.ui.text.TextStyle(fontSize = LegionType.Caption, fontWeight = FontWeight.Bold, color = LegionColors.Danger.copy(alpha = 0.8f))
        fun drawCenteredLegend(text: String, y: Float, st: androidx.compose.ui.text.TextStyle) {
            val w = textMeasurer.measure(text, st).size.width.toFloat() + 16f
            drawText(textMeasurer, text, topLeft = Offset(center.x - w / 2f, y), size = androidx.compose.ui.geometry.Size(maxOf(1f, w), 32f), style = st)
        }
        drawCenteredLegend("DEPLOIEMENT ADVERSE", lineHaut - 26f, legendStyle)
        drawCenteredLegend("DEPLOIEMENT", lineBas + 4f, legendStyle.copy(color = LegionColors.Info.copy(alpha = 0.9f)))
    }
    // Bornes de bandes de portée (6" par segment, rulebook:648-664) autour de
    // l'unité sélectionnée : R1=6", R2=12", R3=18", R4=24", R5=30".
    if (selected != null && (tool == LegionTool.RANGE || tool == LegionTool.ARC)) {
        val sc = toScreen(selected)
        for (i in 1..5) {
            drawCircle(
                LegionColors.Accent.copy(alpha = 0.22f),
                radius = LegionTabletopGeometry.rangeRingPx(6 * i, scale),
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
        // Ombre portée : donne du relief au socle (effet figurine sur tapis).
        drawCircle(LegionColors.Background.copy(alpha = 0.55f), radius = r, center = Offset(c.x + 4f, c.y + 5f))
        val bmp = p.imagePath?.let { cardBitmaps[it] }
        if (bmp != null) {
            // Miniature de carte légale (déjà dans l'app) dessinée en disque sur le socle.
            val side = maxOf(1f, r * 2f)
            drawImage(bmp, dstOffset = androidx.compose.ui.unit.IntOffset((c.x - r).toInt(), (c.y - r).toInt()), dstSize = androidx.compose.ui.unit.IntSize(side.toInt().coerceAtLeast(1), side.toInt().coerceAtLeast(1)))
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
        // Largeur calculée sur le TEXTE MESURÉ : plus de troncature, et size
        // explicite = maxWidth toujours >= 0 (évite le crash Constraints(-12 < 0)).
        val label = "${p.name} ${p.wounds}/${p.maxWounds}" + if (p.cover > 0) " Couv${p.cover}" else ""
        val labelStyle = TextStyle(fontSize = LegionType.Caption, fontWeight = FontWeight.Bold,
            color = if (p.isOpp) LegionColors.Danger else LegionColors.TextPrimary)
        val labelW = textMeasurer.measure(label, labelStyle).size.width.toFloat() + 8.dp.toPx()
        val labelH = 18.dp.toPx()
        val labelX = c.x - labelW / 2f
        val labelY = c.y + r + 4.dp.toPx()
        drawRoundRect(
            color = LegionColors.Background.copy(alpha = 0.85f),
            topLeft = Offset(labelX, labelY),
            size = Size(labelW, labelH),
            cornerRadius = CornerRadius(6.dp.toPx()),
        )
        drawText(
            textMeasurer = textMeasurer,
            text = label,
            topLeft = Offset(labelX + 4.dp.toPx(), labelY),
            size = Size(maxOf(1f, labelW - 8.dp.toPx()), labelH),
            style = labelStyle,
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
private fun LegionCompactDiceSheet(cover: Int, targetName: String, onClose: () -> Unit) {
    var mode by remember { mutableStateOf("attack") }
    var red by remember { mutableIntStateOf(2) }
    var black by remember { mutableIntStateOf(0) }
    var white by remember { mutableIntStateOf(0) }
    var lastRoll by remember { mutableStateOf<List<String>>(emptyList()) }
    var coverRoll by remember { mutableStateOf<List<String>>(emptyList()) }

    ModalBottomSheet(onDismissRequest = onClose, containerColor = LegionColors.Surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.tabletop_roll_dice), color = LegionColors.Accent, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.tabletop_dice_note), color = LegionColors.TextTertiary, style = MaterialTheme.typography.labelSmall)
            if (cover > 0) Text(stringResource(R.string.tabletop_cover, cover), color = LegionColors.Warning, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(stringResource(R.string.tabletop_attack), mode == "attack") { mode = "attack"; black = 0; lastRoll = emptyList(); coverRoll = emptyList() }
                FilterChip(stringResource(R.string.tabletop_defense), mode == "defense") { mode = "defense"; black = 0; lastRoll = emptyList(); coverRoll = emptyList() }
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
                    // Couvert = 1 dé blanc par Hit de la réserve (rulebook 2262-2272).
                    coverRoll = if (mode == "attack" && cover > 0)
                        com.legionforge.app.domain.LegionDiceRules.rollCoverPool(lastRoll) else emptyList()
                },
                Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
            ) { Text(stringResource(R.string.tabletop_roll), fontSize = LegionType.Body, fontWeight = FontWeight.Bold) }
            if (lastRoll.isNotEmpty()) {
                Text(stringResource(R.string.tabletop_results, lastRoll.joinToString(" · ")), color = LegionColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
                val successes = if (mode == "attack")
                    com.legionforge.app.domain.LegionDiceRules.hitsAfterCover(lastRoll, cover, coverRoll)
                else lastRoll.count { com.legionforge.app.domain.LegionDiceRules.isSuccess(mode, it) }
                Text(stringResource(R.string.tabletop_successes, successes), color = LegionColors.Accent, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                if (mode == "attack") {
                    if (targetName.isNotBlank()) Text(stringResource(R.string.tabletop_target, targetName), color = LegionColors.TextSecondary, style = MaterialTheme.typography.labelSmall)
                    if (cover > 0) Text(
                        stringResource(R.string.tabletop_cover_pool, stringResource(if (cover >= 2) R.string.tabletop_cover_heavy else R.string.tabletop_cover_light), coverRoll.joinToString(" ")),
                        color = LegionColors.Warning, style = MaterialTheme.typography.labelSmall,
                    )
                }
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
