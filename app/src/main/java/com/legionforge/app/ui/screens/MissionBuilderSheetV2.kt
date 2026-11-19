package com.legionforge.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.legionforge.app.domain.LegionMissionBuilder
import com.legionforge.app.ui.theme.LegionColors
import com.legionforge.app.ui.theme.LegionType

private val SIDE_OWN = Color(0xFFF59E0B) // orange = MOI
private val SIDE_OPP = Color(0xFFF87171) // rouge = ADV (bleu réservé au rôle Blue Player)

/** Vignette de carte du builder (image réelle, aspect portrait, tap = zoom). */
@Composable
private fun BuilderCardSlot(
    card: LegionBattleCard?,
    label: String,
    sideColor: Color,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            Modifier.fillMaxWidth().aspectRatio(0.7f).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
            shape = RoundedCornerShape(8.dp),
            color = LegionColors.SurfaceRaised,
            border = BorderStroke(1.dp, sideColor),
        ) {
            Box(Modifier.fillMaxWidth()) {
                if (card?.imagePath() != null) {
                    AsyncImage(
                        model = "file:///android_asset/${card.imagePath()}",
                        contentDescription = card.name,
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(card?.name ?: "—", color = Color.White, fontSize = 11.sp,
                            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                            modifier = Modifier.padding(4.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        Text(label, color = sideColor, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp))
    }
}

/** Sélecteur de la carte à révéler : vraies miniatures du deck, tap = choix explicite. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RevealPicker(
    options: List<LegionBattleCard>,
    onPick: (LegionBattleCard) -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        androidx.compose.material3.Surface(
            Modifier.fillMaxWidth(0.94f).heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
            shape = RoundedCornerShape(16.dp), color = LegionColors.SurfaceRaised,
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Choisir une carte", color = LegionColors.Accent,
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Fermer", tint = LegionColors.TextSecondary)
                    }
                }
                if (options.isEmpty()) {
                    Text("Aucune carte de ce type dans le deck", color = LegionColors.TextSecondary, fontSize = 11.sp)
                } else {
                    LazyVerticalGrid(GridCells.Adaptive(96.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(options) { c ->
                            Column(Modifier.clickable { onPick(c) }, horizontalAlignment = Alignment.CenterHorizontally) {
                                BuilderCardSlot(c, c.name, LegionColors.Accent, null)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Un panneau de côté (MOI orange / ADV bleu) avec 4 cartes visibles et badge Blue. */
@Composable
private fun MissionSideColumn(
    side: String,
    color: Color,
    isBlue: Boolean,
    isTurn: Boolean,
    state: LegionMissionBuilder.MissionState,
    deckCards: List<LegionBattleCard>,
    mapNames: Set<String>,
    onZoom: (String, String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = if (side == "own") "MOI" else "ADV"
    val adv = if (side == "own") state.advOwn else state.advOpp
    val sharedObj = state.objective
    val sharedSec = state.secondary

    Column(modifier.fillMaxWidth().background(LegionColors.SurfaceRaised, RoundedCornerShape(10.dp))
        .border(1.dp, if (isTurn) color else LegionColors.Border, RoundedCornerShape(10.dp))
        .padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // En-tête : couleur = CÔTÉ uniquement.
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(6.dp))
            Text(label, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            if (isBlue) {
                Spacer(Modifier.width(4.dp))
                Text("BLUE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.background(Color(0xFF38BDF8), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 2.dp))
            }
            Spacer(Modifier.weight(1f))
            if (isTurn) Text("À VOUS", color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        // 3 miniatures côte à côte (Obj / Sec / Avantage) — le texte se lit au zoom.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            BuilderCardSlot(deckCards.firstOrNull { it.name == sharedObj }, "Obj${if (isBlue) "★" else ""}",
                color, sharedObj?.let { n -> { onZoom(n, deckCards.firstOrNull { c -> c.name == n }?.imagePath()) } },
                Modifier.weight(1f))
            BuilderCardSlot(deckCards.firstOrNull { it.name == sharedSec }, "Sec",
                color, sharedSec?.let { n -> { onZoom(n, deckCards.firstOrNull { c -> c.name == n }?.imagePath()) } },
                Modifier.weight(1f))
            BuilderCardSlot(deckCards.firstOrNull { it.name == adv }, "Avantage",
                color, adv?.let { n -> { onZoom(n, deckCards.firstOrNull { c -> c.name == n }?.imagePath()) } },
                Modifier.weight(1f))
        }
        // Modifications d'Avantage UNIQUEMENT via les 6 actions de l'étape 3 (Adv / Adv adv),
        // qui respectent l'alternance et consomment un mod. Panneau purement informatif.
    }
}

/**
 * Refonte Round 0 — 3 étapes guidées + synthèse, avec vraies cartes des 2 côtés.
 * Remplace l'ancien MissionBuilderSheet tout-texte (4/10 ergonomie + Legion).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MissionBuilderSheetV2(
    battleCards: List<LegionBattleCard>,
    battleDeck: Set<String>,
    bluePlayer: String?,
    blueRoll: Pair<List<String>, List<String>>?,
    missionRevealed: Map<String, String>,
    modsLeft: Map<String, Int>,
    modsTurn: String,
    onRollBlue: () -> Unit,
    onInitialReveal: (String) -> Unit,
    onMod: (String, String) -> Unit,
    onZoom: (String, String?) -> Unit,
    onStart: () -> Unit,
    onClose: () -> Unit,
) {
    val deckCards = battleCards.filter { it.name in battleDeck }
    val mapNames = battleCards.filter { it.category == "map" }.map { it.name }.toSet()
    val objName = missionRevealed[LegionMissionBuilder.KEY_OBJECTIVE]
    val secName = missionRevealed[LegionMissionBuilder.KEY_SECONDARY]
    val advOwn = missionRevealed[LegionMissionBuilder.KEY_ADV_OWN]
    val advOpp = missionRevealed[LegionMissionBuilder.KEY_ADV_OPP]
    val step = when {
        bluePlayer == null -> 1
        objName == null || secName == null || advOwn == null || advOpp == null -> 2
        (modsLeft["own"] ?: 0) + (modsLeft["opp"] ?: 0) > 0 -> 3
        else -> 4
    }

    ModalBottomSheet(onDismissRequest = onClose, containerColor = LegionColors.Surface,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // En-tête + stepper 3 étapes + fermer.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("PRÉPARATION DE LA MISSION", color = LegionColors.Accent,
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("Étape ${step.coerceAtMost(3)}/3" + if (step == 4) " · prêt" else "",
                        color = LegionColors.TextSecondary, fontSize = 11.sp)
                }
                IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Fermer", tint = LegionColors.TextSecondary)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                (1..3).forEach { n ->
                    Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp))
                        .background(if (n <= step) LegionColors.Accent else LegionColors.Border.copy(alpha = 0.35f)))
                }
            }

            if (step == 1) {
                Text("1 · BLUE PLAYER", color = LegionColors.Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("Chacun lance 4 dés d'attaque noirs. Le plus de Hits, puis Crits, puis Surges est Blue ",
                    color = LegionColors.TextSecondary, fontSize = 11.sp)
                if (blueRoll != null) {
                    Text("MOI  ${blueRoll.first.joinToString(" ")}   |   ADV  ${blueRoll.second.joinToString(" ")}",
                        color = LegionColors.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Button(onClick = onRollBlue, modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LegionColors.Accent, contentColor = LegionColors.Background)) {
                    Text("Lancer pour le Blue Player", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            } else if (step == 2) {
                // ÉTAPE 2 — RÉVÉLATION INITIALE ORDONNÉE (rulebook:1200-1204).
                // Le Blue choisit Objectif OU Secondaire de SON deck, l'adversaire
                // révèle l'AUTRE type, puis chacun son Avantage. Explorer les vraies cartes.
                Text("2 · RÉVÉLATION", color = LegionColors.Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(if (bluePlayer == "own")
                        "MOI est Blue : choisissez le type à révéler en premier. L'ADV révèlera l'autre type, puis chacun son Avantage."
                    else "ADV est Blue : choisissez le type à révéler en premier. MOI révèlera l'autre type, puis chacun son Avantage.",
                    color = LegionColors.TextSecondary, fontSize = 11.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onInitialReveal("objective") }, modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LegionColors.Accent, contentColor = LegionColors.Background)) {
                        Text("Révéler l'Objectif", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(onClick = { onInitialReveal("secondary") }, modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LegionColors.Accent, contentColor = LegionColors.Background)) {
                        Text("Révéler le Secondaire", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                // Aperçu : vraies cartes des 2 camps (encore à révéler).
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MissionSideColumn("own", SIDE_OWN, bluePlayer == "own", false,
                        LegionMissionBuilder.MissionState(revealed = missionRevealed, bluePlayer = bluePlayer, turn = modsTurn),
                        deckCards, mapNames, onZoom, Modifier.weight(1f))
                    MissionSideColumn("opp", SIDE_OPP, bluePlayer == "opp", false,
                        LegionMissionBuilder.MissionState(revealed = missionRevealed, bluePlayer = bluePlayer, turn = modsTurn),
                        deckCards, mapNames, onZoom, Modifier.weight(1f))
                }
            } else {
                // ACTIONS EN PREMIER (visibles sans scroll) — étape 3.
                if (step == 3) {
                    Text("3 · MODIFICATIONS — tour de ${if (modsTurn == "own") "MOI" else "ADV"} · ${modsLeft["own"] ?: 0}/${modsLeft["opp"] ?: 0} restantes",
                        color = LegionColors.Warning, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("objective" to "Obj", "secondary" to "Sec", "advantage" to "Adv",
                            "opp_advantage" to "Adv adv", "become_blue" to "Blue", "pass" to "Passer").forEach { (act, label) ->
                            Button(onClick = { onMod(modsTurn, act) },
                                modifier = Modifier.defaultMinSize(48.dp).height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = LegionColors.Border, contentColor = Color.White)) {
                                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                if (step == 4) {
                    Text("✓ Mission prête. Blue choisit un long bord, l'adversaire le bord opposé. Suivez les instructions de l'Objectif, du Secondaire, puis des Avantages en commençant par Blue. Résolvez enfin les effets de Setup en commençant par Blue.",
                        color = LegionColors.Success, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LegionColors.Success, contentColor = LegionColors.Background)) {
                        Text("Commencer la mission", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                // Carte de territoire unique, compacte (appariée à l'Objectif par titre, rulebook:1181-1185).
                val mapName = objName?.takeIf { it in mapNames }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Territoire", color = LegionColors.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f))
                    BuilderCardSlot(deckCards.firstOrNull { it.name == mapName }, mapName ?: "—", LegionColors.Accent,
                        mapName?.let { n -> { onZoom(n, deckCards.firstOrNull { c -> c.name == n }?.imagePath()) } },
                        Modifier.width(64.dp))
                }
                // Étapes 2/3/4 : les 2 côtés toujours visibles, avec vraies cartes compactes.
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MissionSideColumn("own", SIDE_OWN, bluePlayer == "own", modsTurn == "own" && step == 3,
                        LegionMissionBuilder.MissionState(revealed = missionRevealed, bluePlayer = bluePlayer, turn = modsTurn),
                        deckCards, mapNames, onZoom, Modifier.weight(1f))
                    MissionSideColumn("opp", SIDE_OPP, bluePlayer == "opp", modsTurn == "opp" && step == 3,
                        LegionMissionBuilder.MissionState(revealed = missionRevealed, bluePlayer = bluePlayer, turn = modsTurn),
                        deckCards, mapNames, onZoom, Modifier.weight(1f))
                }
            }
        }
    }
}
