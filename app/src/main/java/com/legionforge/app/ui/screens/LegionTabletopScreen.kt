package com.legionforge.app.ui.screens

import com.legionforge.app.R

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
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
    val faction: String = "",         // factionId pour la couleur catalogue (factionColor)
    val upgrade: String = "",         // upgrade/commandant entre parenthèses -> "(c)" sur l'étiquette
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
    initialPan: androidx.compose.ui.geometry.Offset? = null, // test hors-cadre uniquement
    terrainCard: TerrainCard? = null,
    showMissionRail: Boolean = true,
    victoryPoints: Int = 0,
    onPickTerrain: (() -> Unit)? = null,
    missionCards: Map<String, LegionBattleCard?> = emptyMap(),
    missionBitmaps: Map<String, androidx.compose.ui.graphics.ImageBitmap> = emptyMap(),
    onPickMissionCard: ((String) -> Unit)? = null,
    forceBlockedDrop: Boolean = false, // test uniquement : force le feedback collision
) {
    var selectedId by remember { mutableStateOf<String?>(if (forceBlockedDrop) "a1" else null) }
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

    var pan by remember { mutableStateOf<Offset?>(initialPan) } // null = pas encore cadré
    // Translation figée pendant le drag d'un pion : effPan() recalculerait sinon
    // la bounding-box à chaque frame et la coordonnée mm dériverait sous le doigt.
    var dragPan by remember { mutableStateOf<Offset?>(null) }
    var measureEnd by remember { mutableStateOf<Offset?>(null) }
    var showDice by remember { mutableStateOf(false) }
    // Feedback collision : true pendant le drag si le drop serait illégal (mur IMPASSABLE).
    var blockedDrop by remember { mutableStateOf(forceBlockedDrop) }
    // Dernière position MM légale du pion en cours de drag (retour si drop bloqué).
    var lastValidMm by remember { mutableStateOf<Pair<Float, Float>?>(null) }
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
    val liveTerrain by rememberUpdatedState(terrainCard)
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
        val ttEnemy = stringResource(R.string.tabletop_territory_enemy)
        val ttAllied = stringResource(R.string.tabletop_territory_allied)
        val ttNote = stringResource(R.string.tabletop_territory_note)
        val ttTerrainNote = stringResource(R.string.tabletop_terrain_note)
        val ttObj = stringResource(R.string.tabletop_obj)
        val missionLabels = mapOf(
            "command" to stringResource(R.string.rail_command),
            "primary" to stringResource(R.string.rail_primary),
            "map" to stringResource(R.string.rail_map),
            "secondary" to stringResource(R.string.rail_secondary),
            "round" to stringResource(R.string.rail_round),
            "vp" to stringResource(R.string.rail_vp),
        )
        Canvas(
            Modifier.fillMaxSize().testTag("legion_board").pointerInput(Unit) {
                // Geste UNIQUE : 1 doigt = tap/drag, 2 doigts = pinch-zoom.
                // Les detect* en parallele se volaient les evenements : avec
                // detectTransformGestures en premier, le tap sur un pion n'arrivait
                // jamais a detectTapGestures -> "je ne peux pas cliquer sur les unites".
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var lastPos = down.position
                    var moved = false
                    var pinching = false
                    var lastDist = 0f
                    var dragPawnId: String? = null

                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val pressed = event.changes.filter { it.pressed }

                        if (pressed.size >= 2) {
                            // ── PINCH (2 doigts) ──
                            pinching = true
                            dragPawnId = null
                            dragPan = null
                            val a = pressed[0].position
                            val b = pressed[1].position
                            val cen = Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f)
                            val dist = (a - b).getDistance()
                            if (lastDist > 0f && dist > 0f) {
                                val oldS = liveScale
                                val next = (oldS * (dist / lastDist)).coerceIn(0.25f, 6f)
                                val base = livePan
                                val ratio = next / oldS
                                pan = Offset(
                                    cen.x - (cen.x - base.x) * ratio,
                                    cen.y - (cen.y - base.y) * ratio,
                                )
                                scale = next
                            }
                            lastDist = dist
                            lastPos = cen
                            pressed.forEach { it.consume() }
                        } else if (pressed.size == 1) {
                            val c = pressed[0]
                            if (pinching) { // un doigt relache apres pinch : on attend la fin
                                lastDist = 0f
                                c.consume()
                                continue
                            }
                            val delta = c.position - lastPos
                            if (delta.getDistance() > 2f) moved = true
                            if (moved) {
                                // ── DRAG (1 doigt) ──
                                if (dragPawnId == null && liveTool == LegionTool.SELECT) {
                                    val hit = livePawns.lastOrNull { pl ->
                                        (down.position - toScreenLive(pl)).getDistance() <= maxOf(
                                            LegionTabletopGeometry.baseRadiusPx(pl.baseSize, liveScale) + 8f,
                                            LegionDims.TouchMin.toPx() / 2f,
                                        )
                                    }
                                    if (hit != null) {
                                        dragPawnId = hit.id
                                        selectedId = hit.id
                                        dragPan = livePan // figer la translation pendant le geste
                                    }
                                }
                                val dp = dragPawnId?.let { id -> livePawns.firstOrNull { it.id == id } }
                                if (dp != null && liveTool == LegionTool.SELECT) {
                                    val (mx, my) = screenToMmLive(c.position)
                                    val rMm = LegionTabletopGeometry.baseRadiusMm(dp.baseSize)
                                    val pieces = liveTerrain?.pieces ?: emptyList()
                                    val blocked = com.legionforge.app.domain.LegionTerrainCollision.isDropBlocked(
                                        mx, my, rMm, pieces)
                                    blockedDrop = blocked
                                    // IMPASSABLE : REFUSER le drop, garder la dernière position
                                    // légale (rulebook:846-848). Pas de snap au bord.
                                    if (!blocked) {
                                        liveOnMove(dp.id, mx, my, dp.headingDeg)
                                        lastValidMm = mx to my
                                    } else {
                                        lastValidMm?.let { (lx, ly) -> liveOnMove(dp.id, lx, ly, dp.headingDeg) }
                                    }
                                } else {
                                    pan = livePan + delta
                                }
                                if (liveTool == LegionTool.RANGE) measureEnd = c.position
                            }
                            lastPos = c.position
                            c.consume()
                        } else {
                            // ── FIN DU GESTE ──
                            if (!pinching && !moved) {
                                // TAP : ouvrir la fiche du pion touche.
                                val hit = livePawns.lastOrNull { pl ->
                                    (down.position - toScreenLive(pl)).getDistance() <= maxOf(
                                        LegionTabletopGeometry.baseRadiusPx(pl.baseSize, liveScale) + 8f,
                                        LegionDims.TouchMin.toPx() / 2f,
                                    )
                                }
                                when {
                                    hit == null -> { selectedId = null; targetId = null; measureEnd = null }
                                    liveTool == LegionTool.RANGE && selected != null && hit.id != selectedId -> targetId = hit.id
                                    // Tap = sélection locale (barre rapide) ET ouverture de la
                                    // VRAIE fiche dépliante (UnitDetailSheet en overlay dans le
                                    // NavHost, plateau visible derriere — jamais de navigation).
                                    liveTool == LegionTool.SELECT -> { selectedId = hit.id; liveOnOpenPlay(hit.id) }
                                    else -> targetId = hit.id
                                }
                            }
                            if (dragPan != null) pan = dragPan
                            dragPan = null
                            blockedDrop = forceBlockedDrop
                            lastValidMm = null
                            break
                        }
                    }
                }
            }
        ) {
            drawLegionBoard(scale, effPan(), selected, target, tool, measureEnd, pawns, ::toScreen, cardBitmaps, textMeasurer,
                ttEnemy, ttAllied, ttNote, ttObj, terrainCard, ttTerrainNote, blockedDrop)
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
                    Text(stringResource(R.string.tabletop_title_short, round),
                        color = LegionColors.TextPrimary, style = MaterialTheme.typography.titleMedium,
                        maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    Text(stringResource(R.string.tabletop_units_left, unitsLeft),
                        color = LegionColors.TextSecondary, style = MaterialTheme.typography.labelLarge,
                        maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
                IconButton(
                    onClick = { tool = if (tool == LegionTool.RANGE) LegionTool.SELECT else LegionTool.RANGE },
                    modifier = Modifier.size(LegionDims.TouchMin),
                ) {
                    Icon(Icons.Filled.Build, contentDescription = stringResource(R.string.tabletop_measure), tint = if (tool == LegionTool.RANGE) LegionColors.Accent else LegionColors.TextSecondary)
                }
                if (onPickTerrain != null) {
                    IconButton(onClick = onPickTerrain, modifier = Modifier.size(LegionDims.TouchMin)) {
                        Icon(Icons.Filled.Place, contentDescription = stringResource(R.string.terrain_pick), tint = LegionColors.TextSecondary)
                    }
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
                    Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.tabletop_recenter), tint = LegionColors.TextSecondary)
                }
            }
            // Bandeau des cartes de mission, AU-DESSUS du plateau (hors Canvas).
            if (showMissionRail) {
                MissionCardRail(round = round, victoryPoints = victoryPoints, labels = missionLabels,
                    selectedCards = missionCards, cardBitmaps = missionBitmaps,
                    onPickCard = { slot -> onPickMissionCard?.invoke(slot) })
            }
        }

        // ── Barre contextuelle ancrée au pion (non modale) ──────────────────
        AnimatedVisibility(visible = selected != null, modifier = Modifier.align(Alignment.BottomCenter)) {
            selected?.let { p ->
                Column(
                    Modifier
                        .padding(horizontal = 16.dp, vertical = 16.dp)
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

        // ── Pastille de zoom en bas (libère la barre haute) ─────────────────
        val cdZoomOut = stringResource(R.string.tabletop_zoom_out)
        val cdZoomIn = stringResource(R.string.tabletop_zoom_in)
        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)
                .background(LegionColors.SurfaceRaised, RoundedCornerShape(LegionDims.Radius)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { val base = pan ?: viewCenter; pan = base; scale = (scale / 1.25f).coerceIn(0.25f, 6f) },
                modifier = Modifier.semantics { contentDescription = cdZoomOut }) {
                Text("−", color = LegionColors.TextPrimary, fontSize = 24.sp)
            }
            Text(stringResource(R.string.tabletop_zoom, (scale * 100).toInt()), color = LegionColors.TextPrimary,
                style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 4.dp))
            IconButton(onClick = { val base = pan ?: viewCenter; pan = base; scale = (scale * 1.25f).coerceIn(0.25f, 6f) },
                modifier = Modifier.semantics { contentDescription = cdZoomIn }) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = LegionColors.TextPrimary)
            }
        }
        if (selected == null) {
            Text(stringResource(R.string.tabletop_tap_hint), Modifier.align(Alignment.BottomCenter).padding(bottom = 76.dp),
                color = LegionColors.TextSecondary, style = MaterialTheme.typography.labelLarge)
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

        // Toast de feedback (TOUT EN DESSUS) : drop refusé par un bâtiment IMPASSABLE.
        if (blockedDrop) {
            Surface(
                Modifier.align(Alignment.TopCenter).padding(top = 96.dp),
                shape = RoundedCornerShape(LegionDims.Radius),
                color = Color(0xFF8E1B14),
                shadowElevation = 8.dp,
            ) {
                Text(
                    stringResource(R.string.tabletop_placement_blocked),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
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

/** Libellé court d'un pion pour l'info en 2s. Extrait pour test de non-régression. */
internal fun formatPawnLabel(p: LegionPawn): String {
    // Nom COURT : initiales des mots du nom principal ("Darth Vader" -> "DV"),
    // ou le nom tronqué si une seule mot. L'upgrade devient "(c)"/"(x)".
    val baseName = p.name.substringBefore('(').trim()
    val words = baseName.split(" ").filter { it.isNotBlank() }
    val initials = if (words.size >= 2) words.map { it.first().uppercaseChar() }.joinToString("")
                   else baseName.split("-").first().take(3).uppercase()
    val up = p.upgrade.ifBlank {
        // fallback : contenu entre parenthèses du nom
        p.name.substringAfter('(', "").substringBefore(')', "").trim()
    }
    val tag = when {
        up.isBlank() -> ""
        up.contains("command", true) || up.contains("commander", true) || up.contains("commandant", true) -> "(c)"
        else -> "(" + up.first().uppercaseChar() + ")"
    }
    return "$initials$tag · ${p.wounds}/${p.maxWounds} PV" + if (p.cover > 0) " · ☂${p.cover}" else ""
}

/**
 * Palette "champ de bataille" — aplats chauds désaturés façon carte SNES.
 * LOCALE au plateau : ne modifie pas LegionColors (utilisé par tout le reste de l'app).
 * Couleurs vives réservées aux SIGNALS DE RÈGLE (couvert, portée, sélection).
 */
private object BattlePalette {
    val Ground = Color(0xFF6E6658)          // sol pierre/sable
    val TileA = Color(0xFF746C5C)
    val TileB = Color(0xFF665F50)
    val TileSeam = Color(0x11000000)
    val Grout = Color(0xFF8C8068)           // graduations bords
    val Wood = Color(0xFF5A4632)            // cadre bois
    val WoodLight = Color(0xFF7A5F42)
    val Parchment = Color(0xFFC2B280)       // liseré interne / plaquettes
    val ParchmentBright = Color(0xFFE8D8B0)
    val Ink = Color(0xFF3A2E1E)             // texte sur parchemin
    val InkRed = Color(0xFF8C3B32)          // PV sur parchemin
    val TapeOwn = Color(0xFF8C3B32)         // ruban déploiement ADV (rouge sourd)
    val TapeOpp = Color(0xFF4A6484)         // ruban déploiement MOI (bleu sourd)
    val CoinWood = Color(0xFF6B4F3A)        // jeton pion
    val CoinRing = Color(0xFFC2B280)
    val Select = Color(0xFFFFE08A)
    val Activated = Color(0xFF9FB59B)
    val Defeated = Color(0xFF889088)
    val CoverLight = Color(0xFF2E9BFF)
    val CoverHeavy = Color(0xFFFF7A00)
    val Objective = Color(0xFFC2B280)
    val RangeLine = Color(0xFFFFC857)
}

/** Silhouette de troupe stylisée (casque + épaules) pour les jetons sans illustration. */
private fun DrawScope.drawTroopSilhouette(cx: Float, cy: Float, r: Float, color: Color) {
    // Épaules / torse (trapèze).
    drawPath(androidx.compose.ui.graphics.Path().apply {
        moveTo(cx - r * 0.62f, cy + r * 0.7f)
        lineTo(cx - r * 0.42f, cy + r * 0.05f)
        lineTo(cx + r * 0.42f, cy + r * 0.05f)
        lineTo(cx + r * 0.62f, cy + r * 0.7f)
        close()
    }, color)
    // Casque (demi-cercle).
    drawArc(color, 180f, 180f, false,
        topLeft = Offset(cx - r * 0.34f, cy - r * 0.55f),
        size = androidx.compose.ui.geometry.Size(maxOf(1f, r * 0.68f), maxOf(1f, r * 0.68f)))
    // Visière sombre.
    drawRect(BattlePalette.Wood, topLeft = Offset(cx - r * 0.3f, cy - r * 0.08f),
        size = androidx.compose.ui.geometry.Size(maxOf(1f, r * 0.6f), maxOf(1f, r * 0.16f)))
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
    territoryEnemy: String,
    territoryAllied: String,
    territoryNote: String,
    objPrefix: String,
    terrainCard: TerrainCard? = null,
    terrainNote: String = "",
    blockedDrop: Boolean = false,
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

    // Extérieur : table sombre neutre (simple aplat, PAS de dégradé radar).
    drawRect(Color(0xFF141210), size = size)
    if (matSize.width > 0f && matSize.height > 0f) {
        // SOL PIERRE façon carte SNES : dalles rectangulaires à joints DÉCALÉS
        // (pas un damier = grille de calcul), teintes pierre sourdes + joints fins.
        drawRect(BattlePalette.Ground, topLeft = tl, size = matSize)
        val tileW = maxOf(14f, 44f * scale)
        val tileH = maxOf(10f, 30f * scale)
        var ty = tl.y
        var row = 0
        while (ty < br.y) {
            val th = minOf(tileH, br.y - ty)
            val offset = if (row % 2 == 0) 0f else tileW / 2f   // joints décalés (brique)
            var tx = tl.x - offset
            var col = 0
            while (tx < br.x) {
                val tw = minOf(tileW, br.x - tx)
                if (tw > 1f && th > 1f) {
                    // variation de teinte déterministe (3 pierres).
                    val v = (row * 7 + col * 3) % 5
                    val stone = when (v) { 0 -> BattlePalette.TileB; 1 -> BattlePalette.TileA; else -> BattlePalette.Ground }
                    drawRect(stone, topLeft = Offset(tx + 1f, ty + 1f),
                        size = androidx.compose.ui.geometry.Size(maxOf(1f, tw - 2f), maxOf(1f, th - 2f)))
                    // éclat clair en haut à gauche de la dalle (matière).
                    drawLine(BattlePalette.Parchment.copy(alpha = 0.10f), Offset(tx + 2f, ty + 2f),
                        Offset(tx + maxOf(2f, tw * 0.5f), ty + 2f), strokeWidth = 1f)
                }
                tx += tileW; col++
            }
            ty += tileH; row++
        }
        // GRAIN DE SOL 100% procédural (zéro asset) : points de sable/pierre
        // déterministes sur une grille, alpha faible. Appliqué AU SOL, avant tout volume.
        val grainStep = maxOf(10f, 14f / scale.coerceAtLeast(0.25f))
        var gy = tl.y + grainStep / 2f
        var gi = 0
        while (gy < br.y) {
            var gx = tl.x + grainStep / 2f
            var gj = 0
            while (gx < br.x) {
                // bruit déterministe par hash de position (pas de Random non déterministe en CI).
                val h = ((gi * 73856093) xor (gj * 19349663)) and 0x7fffffff
                if (h % 5 == 0) {
                    val jx = gx + ((h shr 4) % grainStep.toInt().coerceAtLeast(1)) * 0.3f
                    val jy = gy + ((h shr 8) % grainStep.toInt().coerceAtLeast(1)) * 0.3f
                    val gr = maxOf(0.8f, 1.3f * scale)
                    drawCircle(Color(0x14000000), radius = gr, center = Offset(jx, jy))
                } else if (h % 7 == 0) {
                    val gr = maxOf(0.8f, 1f * scale)
                    drawCircle(Color(0x14FFF0D0), radius = gr, center = Offset(gx, gy))
                }
                gx += grainStep; gj++
            }
            gy += grainStep; gi++
        }
        // Cadre bois avec liseré parchemin (plus de doré néon).
        drawRect(BattlePalette.Wood, topLeft = tl, size = matSize, style = Stroke(width = 8f))
        drawRect(BattlePalette.WoodLight, topLeft = mm(-halfMat + 5f, -halfMat + 5f),
            size = androidx.compose.ui.geometry.Size(matSize.width - 10f, matSize.height - 10f), style = Stroke(width = 2f))
        drawRect(BattlePalette.Parchment, topLeft = mm(-halfMat + 9f, -halfMat + 9f),
            size = androidx.compose.ui.geometry.Size(matSize.width - 18f, matSize.height - 18f), style = Stroke(width = 1.5f))

        // Frontières de déploiement : ruban sombre discret (zones INDICATIVES, voir Map Card).
        val dep = 12f * LegionTabletopGeometry.MM_PER_INCH
        val lineHaut = mm(0f, -halfMat + dep).y
        val lineBas = mm(0f, halfMat - dep).y
        val bandH = maxOf(7f, 8.dp.toPx())
        // Remplissage léger des zones (indicatives) + ruban franc sur la frontière.
        // HAUTE = ENNEMI (rouge), BASSE = ALLIÉ (bleu).
        drawRect(Color(0x556A241C), topLeft = tl,
            size = androidx.compose.ui.geometry.Size(matSize.width, lineHaut - tl.y))
        drawRect(Color(0x4D2E6FA8), topLeft = Offset(tl.x, lineBas),
            size = androidx.compose.ui.geometry.Size(matSize.width, br.y - lineBas))
        drawRect(BattlePalette.TapeOwn, topLeft = Offset(tl.x, lineHaut - bandH/2f),
            size = androidx.compose.ui.geometry.Size(matSize.width, bandH))
        drawRect(BattlePalette.TapeOpp, topLeft = Offset(tl.x, lineBas - bandH/2f),
            size = androidx.compose.ui.geometry.Size(matSize.width, bandH))

        // Objectifs : JETON AU SOL (anneau or sourd + croix), jamais un volume opaque.
        // Les bâtiments dessinés après recouvrent le jeton s'ils le chevauchent —
        // purement cosmétique (les vrais POI viennent de la Map Card, rulebook:391-401).
        // Nombre/position INDICATIFS (étiquette "voir Map Card" plus bas).
        val objR = 6f * LegionTabletopGeometry.MM_PER_INCH * pxPerMm
        val objStroke = maxOf(2f, 2.dp.toPx())
        for (ox in listOf(-12f, 0f, 12f)) {
            val c = mm(ox * LegionTabletopGeometry.MM_PER_INCH, 0f)
            // Halo sombre pour détacher le trait du sol.
            drawCircle(Color(0x33000000), radius = objR, center = c,
                style = Stroke(width = objStroke + 2f))
            // Anneau or translucide (pas de disque plein).
            drawCircle(BattlePalette.Objective.copy(alpha = 0.55f), radius = objR, center = c,
                style = Stroke(width = objStroke))
            // Croix fine au centre (repère d'objectif, pas une unité détruite).
            val g = objR * 0.16f
            drawLine(BattlePalette.Objective.copy(alpha = 0.7f),
                Offset(c.x - g, c.y - g), Offset(c.x + g, c.y + g), strokeWidth = objStroke)
            drawLine(BattlePalette.Objective.copy(alpha = 0.7f),
                Offset(c.x + g, c.y - g), Offset(c.x - g, c.y + g), strokeWidth = objStroke)
        }

        // Paysage de la carte de terrain (BÂTIMENTS) dessiné APRÈS les objectifs au sol :
        // les volumes recouvrent les jetons d'objectif qui les chevauchent (cosmétique,
        // rulebook:391-401 — les vrais POI viennent de la Map Card), jamais l'inverse.
        drawTerrainCard(terrainCard, ::mm, pxPerMm, textMeasurer)

        // Graduations discrètes sur le bord seul (toutes les 6").
        for (inch in -18..18 step 6) {
            val x = mm(inch * LegionTabletopGeometry.MM_PER_INCH, 0f).x
            val y = mm(0f, inch * LegionTabletopGeometry.MM_PER_INCH).y
            drawLine(BattlePalette.Grout, Offset(x, tl.y + 10f), Offset(x, tl.y + 18f), strokeWidth = 1.5f)
            drawLine(BattlePalette.Grout, Offset(x, br.y - 18f), Offset(x, br.y - 10f), strokeWidth = 1.5f)
            drawLine(BattlePalette.Grout, Offset(tl.x + 10f, y), Offset(tl.x + 18f, y), strokeWidth = 1.5f)
            drawLine(BattlePalette.Grout, Offset(br.x - 18f, y), Offset(br.x - 10f, y), strokeWidth = 1.5f)
        }

        // Légendes CENTRÉES avec fond opaque : lisibles même si les bords du tapis
        // sortent du cadre, et jamais barrées par la ligne de démarcation.
        val legendStyle = androidx.compose.ui.text.TextStyle(fontSize = LegionType.Caption, fontWeight = FontWeight.Bold, color = Color(0xFF6A241C))
        fun drawCenteredLegend(text: String, y: Float, st: androidx.compose.ui.text.TextStyle) {
            val m = textMeasurer.measure(text, st)
            val w = m.size.width.toFloat() + 16.dp.toPx()
            val h = m.size.height.toFloat() + 8.dp.toPx()
            val left = center.x - w / 2f
            drawRoundRect(BattlePalette.Parchment, topLeft = Offset(left, y),
                size = androidx.compose.ui.geometry.Size(maxOf(1f, w), h), cornerRadius = CornerRadius(4.dp.toPx()))
            drawRoundRect(BattlePalette.Wood, topLeft = Offset(left, y),
                size = androidx.compose.ui.geometry.Size(maxOf(1f, w), h), cornerRadius = CornerRadius(4.dp.toPx()), style = Stroke(width = 1f))
            drawText(textMeasurer, text, topLeft = Offset(left + 8.dp.toPx(), y + 4.dp.toPx()),
                size = Size(maxOf(1f, w - 16.dp.toPx()), maxOf(1f, h)), style = st)
        }
        // Légendes placées au bord du tapis, hors de la zone des pions (ligne médiane).
        val legendH = textMeasurer.measure(territoryEnemy, legendStyle).size.height.toFloat()
        drawCenteredLegend(territoryEnemy, tl.y + 58.dp.toPx(), legendStyle) // décalée sous les 2 notes (zone + couvert)
        drawCenteredLegend(territoryAllied, br.y - 36.dp.toPx() - legendH, legendStyle.copy(color = BattlePalette.TapeOpp))
        // Indication honnête : bornes indicatives, le territoire réel vient de la Map Card.
        val noteStyle = androidx.compose.ui.text.TextStyle(fontSize = LegionType.Caption, color = BattlePalette.Ink)
        val note = territoryNote
        val noteW = textMeasurer.measure(note, noteStyle).size.width.toFloat() + 16.dp.toPx()
        // Note placée juste sous la bordure HAUTE du tapis, loin des pions (ligne médiane).
        val noteY = tl.y + 8.dp.toPx()
        drawRoundRect(BattlePalette.Parchment, topLeft = Offset(center.x - noteW/2f, noteY),
            size = androidx.compose.ui.geometry.Size(noteW, 22.dp.toPx()), cornerRadius = CornerRadius(4.dp.toPx()))
        drawText(textMeasurer, note, topLeft = Offset(center.x - noteW/2f + 8.dp.toPx(), noteY + 2.dp.toPx()),
            size = Size(maxOf(1f, noteW - 16.dp.toPx()), 18.dp.toPx()), style = noteStyle)
        // Note terrain : couvert = valeur PROPOSEE a valider en pre-game (rulebook:2256-2261).
        if (terrainCard != null && terrainNote.isNotBlank()) {
            val tnW = textMeasurer.measure(terrainNote, noteStyle).size.width.toFloat() + 16.dp.toPx()
            val tnY = noteY + 22.dp.toPx()
            drawRoundRect(BattlePalette.Parchment, topLeft = Offset(center.x - tnW/2f, tnY),
                size = androidx.compose.ui.geometry.Size(tnW, 20.dp.toPx()), cornerRadius = CornerRadius(4.dp.toPx()))
            drawText(textMeasurer, terrainNote, topLeft = Offset(center.x - tnW/2f + 8.dp.toPx(), tnY + 1.dp.toPx()),
                size = Size(maxOf(1f, tnW - 16.dp.toPx()), 18.dp.toPx()), style = noteStyle)
        }
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
    // Toutes les positions de pions sont calculées AVANT le dessin, pour que
    // les étiquettes (dessinées en 2e passe) évitent TOUS les socles, y compris
    // ceux dessinés après elles.
    val pawnGeom = pawns.map { p ->
        val c = toScreen(p)
        val r = maxOf(LegionTabletopGeometry.baseRadiusPx(p.baseSize, scale), 16.dp.toPx())
        Triple(p, c, r)
    }
    // Seules les étiquettes DÉJÀ POSÉES sont des obstacles. Les socles sont
    // testés à part (et seulement les AUTRES pions) ; les bandes de légende ne
    // sont plus des obstacles bloquants : l'info en 2s reste prioritaire.
    val drawnLabels = mutableListOf<androidx.compose.ui.geometry.Rect>()
    val pawnDiscs = pawnGeom.map { (_, c, r) ->
        androidx.compose.ui.geometry.Rect(c.x - r, c.y - r, c.x + r, c.y + r)
    }
    // Bandes des légendes de territoire : obstacles SOUPLES (guident le repli,
    // mais ne font jamais supprimer une étiquette si aucune position ne convient).
    val legendBands = mutableListOf<androidx.compose.ui.geometry.Rect>()
    if (matSize.width > 0f && matSize.height > 0f) {
        legendBands.add(androidx.compose.ui.geometry.Rect(tl.x, tl.y + 30.dp.toPx(), br.x, tl.y + 66.dp.toPx()))
        legendBands.add(androidx.compose.ui.geometry.Rect(tl.x, br.y - 66.dp.toPx(), br.x, br.y - 30.dp.toPx()))
    }
    val objR = 6f * LegionTabletopGeometry.MM_PER_INCH * pxPerMm
    // ── 1re passe : dessiner tous les socles ──
    pawnGeom.forEach { (p, c, r) ->
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
        // Jeton bois ÉPAIS : ombre portée + chant sombre décalé + tranche claire.
        drawCircle(Color(0x99000000), radius = r, center = Offset(c.x + 5f, c.y + 6f))
        drawCircle(Color(0xFF3A2A1B), radius = r, center = Offset(c.x + 1.5f, c.y + 2.5f))  // chant (épaisseur)
        val bmp = p.imagePath?.let { cardBitmaps[it] }
        if (bmp != null) {
            val side = maxOf(1f, r * 2f)
            drawImage(bmp, dstOffset = androidx.compose.ui.unit.IntOffset((c.x - r).toInt(), (c.y - r).toInt()), dstSize = androidx.compose.ui.unit.IntSize(side.toInt().coerceAtLeast(1), side.toInt().coerceAtLeast(1)))
        } else {
            drawCircle(BattlePalette.CoinWood, radius = r, center = c)
            val troopCol = if (p.faction.isNotBlank()) factionColor(p.faction)
                           else if (p.isOpp) BattlePalette.TapeOwn else BattlePalette.TapeOpp
            drawTroopSilhouette(c.x, c.y - r * 0.08f, r * 0.85f, troopCol)
        }
        // Liseré d'état : 1 seul trait, jamais d'anneau multiple.
        val ring = when {
            p.id == selected?.id -> Color(0xFFFFD21A)   // or franc, distinct du parchemin
            p.activated -> Color(0xFF6FBF7A)            // vert activé franc
            p.defeated -> BattlePalette.Defeated
            p.isOpp -> Color(0xFFC0392B)                // rouge ennemi franc (≠ bleu couvert)
            else -> Color(0xFF2E6FA8)                   // bleu allié franc (≠ bleu couvert clair)
        }
        drawCircle(ring, radius = r, center = c, style = Stroke(width = if (p.id == selected?.id) 3f else 2f))
        // Feedback collision : halo rouge sur le pion dont le drop est refusé (mur IMPASSABLE).
        if (blockedDrop && p.id == selected?.id) {
            drawCircle(Color(0xFFFF3B30).copy(alpha = 0.30f), radius = r + 8.dp.toPx(), center = c)
            drawCircle(Color(0xFFFF3B30), radius = r + 3.dp.toPx(), center = c,
                style = Stroke(width = maxOf(2f, 2.dp.toPx())))
        }
        // Liseré de chant clair (bois taillé) juste à l'intérieur du pourtour.
        drawCircle(BattlePalette.Parchment.copy(alpha = 0.55f), radius = r - 2.dp.toPx(), center = c,
            style = Stroke(width = maxOf(1f, 1.dp.toPx())))
        // Couvert NON directionnel (rulebook:2229-2260) : anneau épaisseur=level ;
        // le chiffre 1/2 est dans le label (☂n) pour le daltonisme.
        if (p.cover > 0 && !p.defeated) {
            val cc = if (p.cover >= 2) BattlePalette.CoverHeavy else BattlePalette.CoverLight
            val covW = (2 * p.cover).dp.toPx()   // épaisseur = niveau de couvert (rulebook:2262)
            val covR = r + 5.dp.toPx()
            drawCircle(Color(0x99000000), radius = covR, center = c, style = Stroke(width = covW + 1.5f))
            drawCircle(cc, radius = covR, center = c, style = Stroke(width = covW))
        }
    }
    // ── 2e passe : étiquettes AU-DESSUS de tous les socles, sans chevauchement ──
    pawnGeom.forEach { (p, c, r) ->
        // Nom COURT (sans l'upgrade entre parenthèses) + PV explicite, largeur bornée.
        val label = formatPawnLabel(p)
        val labelStyle = TextStyle(fontSize = LegionType.Caption, fontWeight = FontWeight.Bold,
            color = BattlePalette.Ink)
        // +20dp : 11 pastille camp + 4 marge gauche texte + 5 marge droite (évite "PV" tronqué).
        val labelW = minOf(textMeasurer.measure(label, labelStyle).size.width.toFloat() + 28.dp.toPx(), 0.60f * size.width)
        val labelH = 19.dp.toPx()
        val gap = 4.dp.toPx()
        // 4 positions essayées dans l'ordre : haut, bas, gauche, droite.
        val candidates = listOf(
            Offset(c.x - labelW / 2f, c.y - r - labelH - gap),
            Offset(c.x - labelW / 2f, c.y + r + gap),
            Offset(c.x - r - labelW - gap, c.y - labelH / 2f),
            Offset(c.x + r + gap, c.y - labelH / 2f),
        )
        val myIndex = pawnGeom.indexOfFirst { it.first.id == p.id }
        fun fits(pos: Offset): Boolean {
            val rect = androidx.compose.ui.geometry.Rect(pos.x, pos.y, pos.x + labelW, pos.y + labelH)
            if (rect.left < 0f || rect.right > size.width || rect.top < 0f || rect.bottom > size.height) return false
            if (drawnLabels.any { it.overlaps(rect) }) return false
            if (legendBands.any { it.overlaps(rect) }) return false
            // Ne pas recouvrir un AUTRE socle (le sien est sous l'étiquette, c'est voulu).
            if (pawnDiscs.withIndex().any { (idx, disc) -> idx != myIndex && disc.overlaps(rect) }) return false
            return true
        }
        // Repli : première position valide, sinon le Haut par défaut (toujours affiché).
        val pos = candidates.firstOrNull { fits(it) } ?: candidates[0]
        val finalRect = androidx.compose.ui.geometry.Rect(pos.x, pos.y, pos.x + labelW, pos.y + labelH)
        // Plaquette parchemin ancrée au socle (anti-HUD) : fond clair, contour bois.
        drawRoundRect(
            color = BattlePalette.ParchmentBright,
            topLeft = pos,
            size = Size(labelW, labelH),
            cornerRadius = CornerRadius(4.dp.toPx()),
        )
        drawRoundRect(
            color = BattlePalette.Wood,
            topLeft = pos,
            size = Size(labelW, labelH),
            cornerRadius = CornerRadius(4.dp.toPx()),
            style = Stroke(width = 1f),
        )
        // Pastille de camp : couleur + LETTRE A/E (pas la couleur seule — daltonisme).
        val campCol = if (p.isOpp) Color(0xFFC0392B) else Color(0xFF2E6FA8)
        val campLetter = if (p.isOpp) "E" else "A"
        val pr = maxOf(6f, 7.dp.toPx())
        drawCircle(campCol, radius = pr, center = Offset(pos.x + pr + 2.dp.toPx(), pos.y + labelH/2f))
        val campStyle = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
        drawText(textMeasurer, campLetter,
            topLeft = Offset(pos.x + pr + 2.dp.toPx() - pr*0.5f, pos.y + labelH/2f - pr*0.7f),
            size = Size(maxOf(1f, pr*2f), maxOf(1f, pr*1.6f)), style = campStyle)
        drawText(
            textMeasurer = textMeasurer,
            text = label,
            topLeft = Offset(pos.x + 2*pr + 5.dp.toPx(), pos.y),
            size = Size(maxOf(1f, labelW - (2*pr + 7.dp.toPx())), labelH),
            style = labelStyle,
        )
        drawnLabels.add(finalRect)
    }
    // Badges OBJ dessinés EN DERNIER : au-dessus des socles, jamais masqués.
    val objLabelStyle = androidx.compose.ui.text.TextStyle(fontSize = LegionType.Caption, fontWeight = FontWeight.Bold, color = BattlePalette.Wood)
    for ((i, ox) in listOf(-12f, 0f, 12f).withIndex()) {
        val c = mm(ox * LegionTabletopGeometry.MM_PER_INCH, 0f)
        val tag = objPrefix.format(i + 1)
        val tw = textMeasurer.measure(tag, objLabelStyle).size.width.toFloat() + 12.dp.toPx()
        drawRoundRect(BattlePalette.ParchmentBright, topLeft = Offset(c.x - tw/2f, c.y - 11.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(tw, 22.dp.toPx()), cornerRadius = CornerRadius(4.dp.toPx()))
        drawRoundRect(BattlePalette.Wood, topLeft = Offset(c.x - tw/2f, c.y - 11.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(tw, 22.dp.toPx()), cornerRadius = CornerRadius(4.dp.toPx()), style = Stroke(width = 1f))
        drawText(textMeasurer, tag, topLeft = Offset(c.x - tw/2f + 6.dp.toPx(), c.y - 8.dp.toPx()),
            size = Size(maxOf(1f, tw - 12.dp.toPx()), 20.dp.toPx()), style = objLabelStyle)
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
