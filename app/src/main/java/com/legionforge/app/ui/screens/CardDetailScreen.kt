package com.legionforge.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.legionforge.app.R
import com.legionforge.app.data.model.*
import com.legionforge.app.data.model.WikiSectionEntity
import com.legionforge.app.ui.viewmodel.ArmyBuilderViewModel
import com.legionforge.app.ui.viewmodel.PlaySyncViewModel
import com.legionforge.app.data.nearby.ShareMode
import com.legionforge.app.data.nearby.SyncedUnitState
import com.legionforge.app.util.rememberNearbyPermissionAction
import com.legionforge.app.util.NearbyDiagnosticPanel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

// Retrouve la section wiki correspondant à un mot-clé/titre (insensible à la casse).
internal fun findWikiSection(wikiSections: List<WikiSectionEntity>, keyword: String): WikiSectionEntity? {
    val k = keyword.lowercase()
    return wikiSections.firstOrNull { s ->
        s.title.lowercase() == k || s.keywordList().any { it.lowercase() == k }
    }
}

// Retire le préfixe "[Nom]\n" du rulesText (le nom est déjà affiché dans le label de l'effet).
internal fun stripBracketName(desc: String): String {
    val t = desc.trimStart()
    return if (t.startsWith("[")) {
        val end = t.indexOf(']')
        if (end > 0) t.substring(end + 1).trimStart() else t
    } else t
}

// Retire le nom de l'effet en tête de description (déjà affiché dans le label), pour ne pas le répéter
// quand l'effet est déplié. Gère "[Nom]\n..." et "Nom\n...".
internal fun stripLeadingName(desc: String, name: String): String {
    val t = desc.trimStart()
    if (t.startsWith("[")) {
        val end = t.indexOf(']')
        if (end > 0) return t.substring(end + 1).trimStart()
    }
    if (name.isNotEmpty() && t.startsWith(name)) {
        val rest = t.removePrefix(name).trimStart()
        if (rest.startsWith("\n")) return rest.trimStart()
    }
    return t
}

// Icône officielle du slot d'upgrade (assets/icons/upg_*.webp), teintée par la couleur du slot.
internal fun slotIconPath(slot: ArmadaSlot): String? = when (slot) {
    ArmadaSlot.COMMANDER -> "icons/upg_commander.webp"
    ArmadaSlot.OFFICER -> "icons/upg_officer.webp"
    ArmadaSlot.TITLE -> "icons/upg_title.webp"
    ArmadaSlot.DEFENSIVE_RETROFIT -> "icons/upg_defensive.webp"
    ArmadaSlot.OFFENSIVE_RETROFIT -> "icons/upg_offensive.webp"
    ArmadaSlot.WEAPONS_TEAM -> "icons/upg_weapons.webp"
    ArmadaSlot.SUPPORT_TEAM -> "icons/upg_support.webp"
    ArmadaSlot.ORDNANCE -> "icons/upg_ordnance.webp"
    ArmadaSlot.TURBOLASERS -> "icons/upg_turbo.webp"
    ArmadaSlot.ION_CANNONS -> "icons/upg_ion.webp"
    ArmadaSlot.EXPERIMENTAL_RETROFIT -> "icons/upg_experimental.webp"
    ArmadaSlot.FLEET_COMMAND -> "icons/upg_fleetcmd.webp"
    ArmadaSlot.FLEET_SUPPORT -> "icons/upg_fleetsup.webp"
    else -> null
}

// Badge d'upgrade : lettre + couleur distincte par type de slot (au lieu du bleu unique).
internal fun factionColor(factionId: String): Color = when (factionId.lowercase()) {
        "rebel" -> Color(0xFF4FC3F7)      // bleu Rebelle
        "empire" -> Color(0xFFFF6B6B)     // rouge Empire
        "republic" -> Color(0xFFFFB74D)   // orange République
        "separatist" -> Color(0xFFE57373) // rouge Séparatiste
        "mercenary" -> Color(0xFF77D9A7)  // vert Mercenaire
        "mandalorian" -> Color(0xFF9B6DFF) // violet Mandalorien
        else -> Color(0xFFFFC857)         // or neutre
    }

internal fun slotBadge(slot: ArmadaSlot): Pair<String, Color> = when (slot) {
    ArmadaSlot.COMMANDER -> "C" to Color(0xFFFFC857)
    ArmadaSlot.OFFICER -> "O" to Color(0xFFFFB74D)
    ArmadaSlot.TITLE -> "T" to Color(0xFFCE93D8)
    ArmadaSlot.DEFENSIVE_RETROFIT -> "DR" to Color(0xFF4DD0E1)
    ArmadaSlot.OFFENSIVE_RETROFIT -> "OR" to Color(0xFFFF8A65)
    ArmadaSlot.WEAPONS_TEAM -> "W" to Color(0xFFF06292)
    ArmadaSlot.SUPPORT_TEAM -> "S" to Color(0xFFAED581)
    ArmadaSlot.ORDNANCE -> "ORD" to Color(0xFFFFD54F)
    ArmadaSlot.TURBOLASERS -> "TB" to Color(0xFF64B5F6)
    ArmadaSlot.ION_CANNONS -> "ION" to Color(0xFF90CAF9)
    ArmadaSlot.EXPERIMENTAL_RETROFIT -> "ER" to Color(0xFFBA68C8)
    ArmadaSlot.SUPERWEAPON -> "SW" to Color(0xFFEF5350)
    else -> slot.name.first().toString() to Color(0xFF4FC3F7)
}

// ── common crit cards ───────────────────────────────────
data class CritCard(val name: String, val effect: String)
internal val armadaCrits = listOf(
    CritCard("Blinded Laser", "Le vaisseau ne peut pas utiliser l'armement principal ce tour."),
    CritCard("Burning Sensors", "Le vaisseau saute son activation et gagne 1 jeton d'état."),
    CritCard("Damaged Control", "Le vaisseau ne peut pas utiliser son jeton de commandement suivant."),
    CritCard("Damaged Weapons", "Les dés rouges du vaisseau sont relancés. Piochez une nouvelle carte dégâts."),
    CritCard("Engines Damaged", "Le vaisseau ne peut pas se déplacer de sa vitesse maximale."),
    CritCard("Hull Breach", "Ignorez les boucliers sur les dés de dégâts suivants."),
    CritCard("Shaken Crew", "Le vaisseau saute son activation."),
    CritCard("Structural Damage", "Tous les dégâts subis par la coque sont doublés ce tour.")
)
internal val legionCrits = listOf(
    CritCard("Blessure critique", "La figurine subit une blessure negligee."),
    CritCard("Sonne", "La figurine ne peut pas effectuer d'action ce tour."),
    CritCard("Desequilibre", "Retirez un de de la reserve de des."),
    CritCard("Statut altere", "Appliquez un marqueur d'etat a la figurine.")
)

// ── defence tokens for Armada ────────────────────────────
enum class ArmadaDefenseToken(val label: String, val icon: String) {
    BRACE("Brace", "\uD83D\uDEE1"),
    REDIRECT("Redirect", "\u21C4"),
    EVADE("Evade", "\u21BA"),
    SCATTER("Scatter", "\u2601"),
    CONTAIN("Contain", "\u26D4"),
    SALVO("Salvo", "\u21BA")
}

// ── command orders (roue de commandement) for Armada ────
enum class ArmadaCommandOrder(val label: String, val iconFile: String, val orderFile: String) {
    NAVIGATE("Navigation", "cmd_navigate.webp", "order_navigate.webp"),
    CONCENTRATE("Concentrate Fire", "cmd_concentrate.webp", "order_concentrate.webp"),
    SQUADRON("Squadron", "cmd_squadron.webp", "order_squadron.webp"),
    REPAIR("Repair", "cmd_repair.webp", "order_repair.webp")
}

// ── active effect model ─────────────────────────────────
internal enum class EffectType { COMMANDER, UPGRADE, CRIT, ABILITY }
internal data class ActiveEffect(val type: EffectType, val label: String, val desc: String, val id: String, val used: Boolean = false, val slot: ArmadaSlot? = null, val pts: Int = 0, val imageAssetPath: String? = null)

enum class PlayPhase { PREP, GAME }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun CardDetailScreen(entries: List<ListEntry>, initialIndex: Int = 0, onBack: () -> Unit, listId: String? = null, vm: ArmyBuilderViewModel = viewModel(), playSyncVm: PlaySyncViewModel = viewModel()) {
    // Charge la liste dans CE vm (scopé à l'écran play) : sinon currentList est null et la
    // synchro 2 joueurs ne peut pas envoyer la liste adverse.
    if (listId != null) androidx.compose.runtime.LaunchedEffect(listId) { vm.openList(listId) }
    // Charge le catalogue COMPLET du système (toutes factions) pour résoudre l'armée adverse :
    // openList ne charge que le catalogue filtré par MA faction (observeCards(system, factionId)),
    // or l'adversaire est d'une faction DIFFÉRENTE -> ses cartes seraient introuvables (0 unité reçue).
    LaunchedEffect(vm.currentList.value?.gameSystem) {
        vm.currentList.value?.gameSystem?.let { vm.loadAllCatalog(GameSystem.valueOf(it)) }
    }
    val playable = entries.filter { e ->
        e.parentInstanceId == null && (e.card.kind == CardKind.LEGION_UNIT || e.card.kind == CardKind.ARMADA_SHIP || e.card.kind == CardKind.ARMADA_SQUADRON || e.card.kind == CardKind.COMMANDER)
    }
    val safeIndex = initialIndex.coerceIn(0, (playable.size - 1).coerceAtLeast(0))
    val ownPagerState = rememberPagerState(pageCount = { playable.size.coerceAtLeast(1) }, initialPage = safeIndex)
    var round by remember { mutableIntStateOf(1) }
    // Points de victoire + unités vaincues (état global de partie, mode 2 joueurs).
    var vp by remember { mutableIntStateOf(0) }
    var oppVp by remember { mutableIntStateOf(0) }
    var kills by remember { mutableIntStateOf(0) }
    var oppKills by remember { mutableIntStateOf(0) }
    // Priorité (qui active en premier) + jetons de passe (règle v2).
    var priority by remember { mutableStateOf("own") }
    var passes by remember { mutableIntStateOf(0) }
    var oppPasses by remember { mutableIntStateOf(0) }
    // Pool d'ordres (pions d'ordre par rang) — état global de partie.
    var orderPool by remember { mutableStateOf(mapOf("commander" to 0, "corps" to 0, "special" to 0, "support" to 0, "heavy" to 0)) }
    // Résultat de partie (PR7) : null = en cours, sinon vainqueur.
    var gameResult by remember { mutableStateOf<String?>(null) }
    // Journal de partie (P2) : log chronologique exportable.
    var gameLog by remember { mutableStateOf<List<String>>(emptyList()) }
    var showLogSheet by remember { mutableStateOf(false) }
    fun logEvent(msg: String) { gameLog = gameLog + "[R$round] $msg" }
    // Phase de jeu (Setup vs partie) : les features préparatoires sont isolées dans l'écran Setup.
    var phase by remember { mutableStateOf(PlayPhase.PREP) }
    // Tour d'activation courant (PR3) : "own" ou "opp".
    var turn by remember { mutableStateOf("own") }
    // Mode 2 joueurs : viewingArmy = null (liste des armées) | "own" (mon armée) | "opp" (armée adverse).
    var twoPlayer by remember { mutableStateOf(false) }
    var viewingArmy by remember { mutableStateOf<String?>(null) }
    val syncMode by playSyncVm.mode.collectAsState()
    val syncRound by playSyncVm.receivedRound.collectAsState()
    val opponentListJson by playSyncVm.opponentListJson.collectAsState()
    val myDeployDone by playSyncVm.deploymentDone.collectAsState()
    val oppDeployDone by playSyncVm.receivedDeploymentDone.collectAsState()
    val oppRoundCommand by playSyncVm.receivedRoundCard.collectAsState()
    val receivedTurn by playSyncVm.receivedTurn.collectAsState()
    // Wiki des règles : chargé pour rendre les mots-clés du texte de règles cliquables.
    val wikiSections by vm.wikiSections.collectAsState()
    var ruleSection by remember { mutableStateOf<WikiSectionEntity?>(null) }
    val onRuleClick: (WikiSectionEntity) -> Unit = { ruleSection = it }
    LaunchedEffect(Unit) { vm.loadAllWiki() }
    // Applique le round reçu de l'autre téléphone.
    LaunchedEffect(syncRound) { if (twoPlayer && syncRound != null && syncRound != round) round = syncRound!! }
    // Émet le round quand il change en mode 2 joueurs.
    LaunchedEffect(round) { if (twoPlayer && syncMode == ShareMode.CONNECTED) playSyncVm.sendRound(round) }
    // Synchro du tour (identité de joueur) : corrige le clignotement "VOTRE TOUR" sur les 2 tablettes.
    val myPlayerId = playSyncVm.playerId
    fun toggleTurn() {
        turn = if (turn == "own") "opp" else "own"
        if (twoPlayer && syncMode == ShareMode.CONNECTED) {
            val owner = if (turn == "own") myPlayerId else (if (myPlayerId == "A") "B" else "A")
            playSyncVm.sendTurn(owner)
        }
    }
    LaunchedEffect(receivedTurn) {
        if (twoPlayer && receivedTurn != null) {
            turn = if (receivedTurn == myPlayerId) "own" else "opp"
        }
    }
    // Envoie la liste dès qu'elle est chargée ET qu'on est connecté. openList est async :
    // currentList ET entries se remplissent en arrière-plan, donc on réessaie à chaque
    // changement des deux (sinon on peut envoyer une liste VIDE si entries n'est pas encore chargé).
    LaunchedEffect(vm.currentList.value, vm.entries.value, syncMode) {
        val l = vm.currentList.value
        if (twoPlayer && syncMode == ShareMode.CONNECTED && l != null && vm.entries.value.isNotEmpty()) {
            playSyncVm.sendList(l, vm.entries.value)
        }
    }
    // Quand on ouvre l'écran ADVERSE : renvoie notre liste + demande celle de l'adversaire.
    // (Le sendList initial peut rater si la liste n'était pas chargée à la connexion.)
    LaunchedEffect(viewingArmy, syncMode) {
        if (viewingArmy == "opp" && syncMode == ShareMode.CONNECTED) {
            playSyncVm.sendList(vm.currentList.value, vm.entries.value)
            playSyncVm.requestList()
        }
    }
    val isConnected = syncMode == ShareMode.CONNECTED
    // Gain de permission runtime avant toute action Nearby (mode 2 joueurs).
    val runWithPermission = rememberNearbyPermissionAction()
    // Cartes de commandement Legion : chargement + deck (ajout/marquage jouée).
    val legionContext = androidx.compose.ui.platform.LocalContext.current
    val cmdFaction = vm.currentList.value?.factionId ?: ""
    val isLegion = vm.currentList.value?.gameSystem == "LEGION_V2"
    val cmdCards = remember { loadCommandCards(legionContext) }
    var commandDeck by remember { mutableStateOf(setOf<String>()) }
    var playedCommands by remember { mutableStateOf(setOf<String>()) }
    var roundCommand by remember { mutableStateOf<String?>(null) }  // ma carte de commandement jouée ce round
    var playPhase by remember { mutableStateOf("command") }  // "command" | "activation" — flux guidé : choisir la carte du round puis activer
    // Émet la carte de commandement jouée ce round en mode 2 joueurs.
    LaunchedEffect(roundCommand) { if (twoPlayer && syncMode == ShareMode.CONNECTED) playSyncVm.sendRoundCard(roundCommand) }
    // Nouveau round → réinitialise la carte jouée + revient au flux guidé (on re-pique une carte).
    LaunchedEffect(round) { roundCommand = null; playPhase = "command" }
    // M6: fin de partie automatique (12 VP ou fin du round 5).
    val victoryOwn = stringResource(R.string.victory_own)
    val victoryOpp = stringResource(R.string.victory_opp)
    val victoryDraw = stringResource(R.string.victory_draw)
    LaunchedEffect(vp, oppVp, round) {
        if (gameResult == null) {
            if (vp >= 12 && vp > oppVp) gameResult = victoryOwn
            else if (oppVp >= 12 && oppVp > vp) gameResult = victoryOpp
            else if (round >= 5) gameResult = when { vp > oppVp -> victoryOwn; oppVp > vp -> victoryOpp; else -> victoryDraw }
        }
    }
    var showCmdSheet by remember { mutableStateOf(false) }
    // Battle deck (Legion v2) : 9 cartes (3+3+3) + mission (carte révélée par catégorie).
    val battleCards = remember { loadBattleCards(legionContext) }
    var battleDeck by remember { mutableStateOf(setOf<String>()) }
    var battleRevealed by remember { mutableStateOf(mapOf<String, String>()) }
    var showBattleSheet by remember { mutableStateOf(false) }
    // Lanceur de dés (PR5) : pool d'attaque + résultats.
    var showDiceSheet by remember { mutableStateOf(false) }
    var showDeploySheet by remember { mutableStateOf(false) }
    var showDeployment by remember { mutableStateOf(false) }  // déploiement des 2 armées (sheet)
    var showMenu by remember { mutableStateOf(false) }
    var setupStep by remember { mutableIntStateOf(1) }
    var showPoolSheet by remember { mutableStateOf(false) }
    var showOppPicker by remember { mutableStateOf(false) }
    var panicRoll by remember { mutableStateOf<Int?>(null) }
    var vpA by remember { mutableStateOf(0) }
    var vpB by remember { mutableStateOf(0) }
    var activeObjectives by remember { mutableStateOf<Set<String>>(emptySet()) }
    var soloOppStates by remember { mutableStateOf<Map<String, SyncedUnitState>>(emptyMap()) }
    // Round 0 / Build a Mission (v2.6).
    var showMissionSheet by remember { mutableStateOf(false) }
    var bluePlayer by remember { mutableStateOf<String?>(null) }  // "own" | "opp"
    var missionRevealed by remember { mutableStateOf<Map<String, String>>(emptyMap()) }  // "own_objective" -> nom
    var modsLeft by remember { mutableStateOf<Map<String, Int>>(mapOf("own" to 2, "opp" to 2)) }
    // Refonte liste/détail : unité sélectionnée pour le détail, sélection comparée, sheet comparé.
    var selectedUnit by remember { mutableStateOf<ListEntry?>(null) }
    var compareSel by remember { mutableStateOf<List<ListEntry>>(emptyList()) }
    var showCompare by remember { mutableStateOf(false) }
    // Store d'état de jeu : mon armée (éditable) + armée adverse (reçue, lecture seule).
    val ownStates by playSyncVm.ownStates.collectAsState()
    val oppStates by playSyncVm.oppStates.collectAsState()
    val soloOppEntries by vm.soloOppEntries.collectAsState()
    val soloOppList by vm.soloOppList.collectAsState()
    // Armée adverse : en 2 joueurs depuis le payload partagé, en solo depuis la liste adverse choisie.
    val opp = if (twoPlayer) {
        remember(opponentListJson) {
            try { com.google.gson.Gson().fromJson(opponentListJson, com.legionforge.app.ui.viewmodel.ShareListPayload::class.java) } catch (_: Exception) { null }
        }
    } else null
    val oppEntries = if (twoPlayer) {
        remember(opp, vm.cards.value) {
            opp?.entries?.mapNotNull { e ->
                vm.cards.value.firstOrNull { it.id == e.cardId }?.let { card ->
                    ListEntry(
                        // Préfixe "B:" sur les instanceId adverses → noms disjoints de mes UUID ("A:"),
                        // évite la collision d'état si les 2 joueurs partagent la même liste (mêmes UUID).
                        instanceId = e.instanceId?.let { "B:$it" } ?: "opp_${e.cardId}_${e.parentInstanceId ?: "root"}",
                        card = card,
                        parentInstanceId = e.parentInstanceId?.let { "B:$it" },
                        quantity = e.quantity,
                        chosenSlot = e.chosenSlot?.let { s -> runCatching { ArmadaSlot.valueOf(s) }.getOrNull() }
                    )
                }
            } ?: emptyList()
        }
    } else soloOppEntries
    val oppPlayable = oppEntries.filter { e ->
        e.parentInstanceId == null && (e.card.kind == CardKind.LEGION_UNIT || e.card.kind == CardKind.ARMADA_SHIP || e.card.kind == CardKind.ARMADA_SQUADRON || e.card.kind == CardKind.COMMANDER)
    }
    val oppPagerState = rememberPagerState(pageCount = { oppPlayable.size.coerceAtLeast(1) })
    // Snackbar pour les confirmations/annulations (EV2).
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(topBar = {
        if (!(phase == PlayPhase.GAME && viewingArmy == null)) {
        TopAppBar(title = { Text(stringResource(R.string.play_mode), style = MaterialTheme.typography.titleMedium) },
            navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.back)) } },
            actions = {
                            // Bouton 2 joueurs : active/désactive le mode sync live.
                            Surface(onClick = {
                                twoPlayer = !twoPlayer
                                if (twoPlayer && syncMode == ShareMode.IDLE) runWithPermission { playSyncVm.startHost() }
                                if (!twoPlayer) { viewingArmy = null; playSyncVm.stop() }
                            }, shape = RoundedCornerShape(8.dp), color = if (twoPlayer) Color(0xFFFFC857) else Color(0xFF2A3A4A), modifier = Modifier.padding(end = 4.dp)) {
                                Box(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
                                    Text(stringResource(R.string.play_2players), color = if (twoPlayer) Color(0xFF0A0E15) else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            // Bouton Armées : revient à la liste des armées (quand on est dans une armée).
                            if (twoPlayer && viewingArmy != null) {
                                Surface(onClick = { viewingArmy = null }, shape = RoundedCornerShape(8.dp), color = Color(0xFF1A2330), modifier = Modifier.padding(end = 4.dp)) {
                                    Box(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
                                        Text("Armées", color = Color(0xFFFFC857), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            // Menu ⋮ (tiroir) : fin de partie, pool, battle deck, réglages.
                            Box {
                                Surface(onClick = { showMenu = true }, shape = RoundedCornerShape(8.dp), color = Color(0xFF1A2330), modifier = Modifier.padding(end = 4.dp)) {
                                    Box(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
                                        Text("⋮", color = Color(0xFFFFC857), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                    val victoryOwn = stringResource(R.string.victory_own)
                                    val victoryOpp = stringResource(R.string.victory_opp)
                                    val victoryDraw = stringResource(R.string.victory_draw)
                                    DropdownMenuItem(text = { Text("Fin de partie", color = Color(0xFFFFC7B7)) }, onClick = {
                                        showMenu = false
                                        gameResult = when { vp > oppVp -> victoryOwn; oppVp > vp -> victoryOpp; else -> victoryDraw }
                                    })
                                    DropdownMenuItem(text = { Text("Pool d'ordres", color = Color.White) }, onClick = { showMenu = false; showPoolSheet = true })
                                    DropdownMenuItem(text = { Text("Battle deck", color = Color.White) }, onClick = { showMenu = false; showBattleSheet = true })
                                    DropdownMenuItem(text = { Text(if (phase == PlayPhase.PREP) "Réglages setup" else "Réglages setup (verrouillés)", color = if (phase == PlayPhase.PREP) Color.White else Color(0xFF5A6A7A)) }, enabled = phase == PlayPhase.PREP, onClick = { showMenu = false; showDeploySheet = true })
                                    DropdownMenuItem(text = { Text("Journal", color = Color.White) }, onClick = { showMenu = false; showLogSheet = true })
                                }
                            }
                        },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0A0E15)))
        }
    }, snackbarHost = { SnackbarHost(snackbarHostState) }, bottomBar = {
        if (isLegion && !(phase == PlayPhase.GAME && viewingArmy == null)) {
            BottomAppBar(containerColor = Color(0xFF111827)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    // Deck de commandement (seul bouton de la barre en phase de jeu)
                    Surface(onClick = { showCmdSheet = true }, shape = RoundedCornerShape(10.dp), color = if (commandDeck.isNotEmpty()) Color(0xFF1E3A2A) else Color(0xFF2A3A4A)) {
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFFC857), modifier = Modifier.size(18.dp))
                            Text("CMD ${commandDeck.size}", color = Color(0xFFFFC857), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }) { pad ->
        if (playable.isEmpty()) { Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) { Text(stringResource(R.string.add_units_play), color = Color.Gray) }; return@Scaffold }
        Box(Modifier.fillMaxSize().padding(pad)) {
            // ── ÉCRAN DE PARTIE : setup + features de jeu (solo ou 2 joueurs) ──
            if (viewingArmy == null) {
                Column(
                    if (phase == PlayPhase.GAME) Modifier.fillMaxSize().padding(8.dp)
                    else Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(if (phase == PlayPhase.GAME) 8.dp else 12.dp)
                ) {
                    if (phase == PlayPhase.PREP) {
                    Text(stringResource(R.string.play_2players), color = Color(0xFFFFC857), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    // Connexion Nearby (2 joueurs uniquement) : panneau si pas connecté, sinon indicateur.
                    if (twoPlayer && syncMode != ShareMode.CONNECTED) {
                        val endpoints by playSyncVm.endpoints.collectAsState()
                        Text(stringResource(R.string.play_2p_guidance), color = Color(0xFF8F9BAD), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 4.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { runWithPermission { playSyncVm.startHost() } }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (syncMode == ShareMode.ADVERTISING) Color(0xFFFFC857) else Color(0xFF1A2330))) {
                                Text(stringResource(R.string.play_host), color = if (syncMode == ShareMode.ADVERTISING) Color(0xFF0A0E15) else Color.White, fontWeight = FontWeight.Bold)
                            }
                            Button(onClick = { runWithPermission { playSyncVm.startClient() } }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (syncMode == ShareMode.DISCOVERING) Color(0xFFFFC857) else Color(0xFF1A2330))) {
                                Text(stringResource(R.string.play_client), color = if (syncMode == ShareMode.DISCOVERING) Color(0xFF0A0E15) else Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (syncMode == ShareMode.DISCOVERING) {
                            if (endpoints.isEmpty()) Text(stringResource(R.string.share_no_endpoints), color = Color(0xFF8F9BAD), style = MaterialTheme.typography.bodySmall)
                            endpoints.forEach { ep ->
                                Card(onClick = { runWithPermission { playSyncVm.connectTo(ep.endpointId) } }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2330))) {
                                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(ep.name, color = Color.White, fontWeight = FontWeight.Bold)
                                        Text(stringResource(R.string.share_connect), color = Color(0xFFFFC857), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        val syncError by playSyncVm.error.collectAsState()
                        NearbyDiagnosticPanel(
                            error = syncError,
                            onReport = { body -> com.legionforge.app.util.CrashReporter.reportEvent("Nearby 2 joueurs (${android.os.Build.MODEL})", body) }
                        )
                    } else {
                        Text("Connecté — choisissez une armée", color = Color(0xFF77D9A7), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                    // Mode solo : choisir l'armée adverse (2 armées sur le même appareil).
                    if (!twoPlayer) {
                        Surface(onClick = { showOppPicker = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), color = Color(0xFF1A2330)) {
                            Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Info, contentDescription = null, tint = Color(0xFFFFC857), modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(if (soloOppList != null) "Armée adverse : ${soloOppList?.name}" else "Choisir l'armée adverse (solo)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Text("Choisir", color = Color(0xFFFFC857), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    // ── Bascule Setup / Partie ──
                    if (phase == PlayPhase.PREP) {
                        // Écran Setup : features préparatoires isolées (battle deck, command deck, déploiement, priorité, pool d'ordres).
                        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A2A))) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(stringResource(R.string.setup_title), color = Color(0xFF77D9A7), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(stringResource(R.string.setup_desc), color = Color(0xFF9EACBC), style = MaterialTheme.typography.bodySmall)
                                // Indicateur d'étape (wizard).
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("Étape $setupStep / 3", color = Color(0xFFFFC857), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        repeat(3) { i -> Box(Modifier.size(8.dp).clip(CircleShape).background(if (i + 1 == setupStep) Color(0xFFFFC857) else Color(0xFF2A3A4A))) }
                                    }
                                }
                                if (setupStep == 1) {
                                // Battle deck (construction 3+3+3).
                                Surface(onClick = { showBattleSheet = true }, shape = RoundedCornerShape(10.dp), color = Color(0xFF2A3A4A), modifier = Modifier.fillMaxWidth()) {
                                    Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Info, contentDescription = null, tint = Color(0xFFFFC857), modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(stringResource(R.string.setup_battle_deck), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.weight(1f))
                                        Text("${battleDeck.size}/9", color = Color(0xFFFFC857), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                // Command deck (main de 7).
                                Surface(onClick = { showCmdSheet = true }, shape = RoundedCornerShape(10.dp), color = Color(0xFF2A3A4A), modifier = Modifier.fillMaxWidth()) {
                                    Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFFC857), modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(stringResource(R.string.setup_command_deck), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.weight(1f))
                                        Text("${commandDeck.size + 1}/7", color = Color(0xFFFFC857), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                }
                                if (setupStep == 2) {
                                // Déploiement.
                                Surface(onClick = { showDeploySheet = true }, shape = RoundedCornerShape(10.dp), color = Color(0xFF2A3A4A), modifier = Modifier.fillMaxWidth()) {
                                    Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Place, contentDescription = null, tint = Color(0xFF77D9A7), modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(stringResource(R.string.deploy), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                // Déployer : ouvrir le panneau des 2 armées pour marquer Zone 1/2 (plus de vue "une seule armée").
                                Surface(onClick = { showDeployment = true }, shape = RoundedCornerShape(10.dp), color = Color(0xFF1E3A2A), modifier = Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Déployer les armées", color = Color(0xFF77D9A7), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text("Marquez la Zone 1 / Zone 2 de chaque unité des 2 armées", color = Color(0xFF8F9BAD), fontSize = 10.sp)
                                    }
                                }
                                // Priorité initiale.
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(stringResource(R.string.priority), color = Color(0xFF9EACBC), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Surface(onClick = { priority = "own" }, shape = RoundedCornerShape(8.dp), color = if (priority == "own") Color(0xFFFFC857) else Color(0xFF2A3A4A)) { Text(stringResource(R.string.play_my_army), Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = if (priority == "own") Color(0xFF0A0E15) else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                                        Surface(onClick = { priority = "opp" }, shape = RoundedCornerShape(8.dp), color = if (priority == "opp") Color(0xFFFFC857) else Color(0xFF2A3A4A)) { Text(stringResource(R.string.play_opponent), Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = if (priority == "opp") Color(0xFF0A0E15) else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                                    }
                                }
                                // Déploiement terminé (A3) : signal qui détermine qui finit en premier.
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Déploiement terminé", color = Color(0xFF9EACBC), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Surface(onClick = { playSyncVm.setDeploymentDone(!myDeployDone) }, shape = RoundedCornerShape(8.dp), color = if (myDeployDone) Color(0xFF77D9A7) else Color(0xFF2A3A4A)) { Text("Moi", Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = if (myDeployDone) Color(0xFF0A0E15) else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                                        Text(if (oppDeployDone == true) "Adversaire ✓" else "Adversaire", color = if (oppDeployDone == true) Color(0xFF77D9A7) else Color(0xFF9EACBC), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                }
                                if (setupStep == 3) {
                                // Pool d'ordres initial.
                                Text(stringResource(R.string.order_pool), color = Color(0xFF9EACBC), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                listOf("commander" to stringResource(R.string.rank_commander), "corps" to stringResource(R.string.rank_corps), "special" to stringResource(R.string.rank_special), "support" to stringResource(R.string.rank_support), "heavy" to stringResource(R.string.rank_heavy)).forEach { (rank, label) ->
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(label, color = Color(0xFF9EACBC), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Stepper("${orderPool[rank] ?: 0}", { orderPool = orderPool + (rank to ((orderPool[rank] ?: 0) - 1).coerceAtLeast(0)) }, { orderPool = orderPool + (rank to ((orderPool[rank] ?: 0) + 1).coerceAtMost(9)) }, size = 34.dp, valueWidth = 26.dp)
                                    }
                                }
                                // Round 0 / Build a Mission (v2.6) : détermination blue + révélation + modifications.
                                Surface(onClick = { showMissionSheet = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), color = Color(0xFF1E3A2A)) {
                                    Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Info, contentDescription = null, tint = Color(0xFF77D9A7), modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Round 0 — Build a Mission", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                        Text(if (bluePlayer != null) "✓" else "→", color = Color(0xFF77D9A7), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                // Lancer la partie (garde-fou : battle deck 3+3+3 complet + main de 7).
                                val battleComplete = listOf("objective", "secondary", "advantage").all { cat -> battleCards.count { it.category == cat && it.name in battleDeck } >= 3 }
                                val cmdComplete = commandDeck.size + 1 >= 7 // +1 = Standing Orders auto-ajouté (7e carte de la main)
                                Button(onClick = { phase = PlayPhase.GAME }, enabled = battleComplete && cmdComplete, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = if (battleComplete && cmdComplete) Color(0xFF77D9A7) else Color(0xFF2A3A4A))) {
                                    Text(if (battleComplete && cmdComplete) stringResource(R.string.start_game) else stringResource(R.string.setup_incomplete), color = if (battleComplete && cmdComplete) Color(0xFF0A0E15) else Color(0xFF8F9BAD), fontWeight = FontWeight.Bold)
                                }
                                }
                                // Navigation (wizard).
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    if (setupStep > 1) {
                                        Surface(onClick = { setupStep-- }, shape = RoundedCornerShape(8.dp), color = Color(0xFF2A3A4A)) { Text("Précédent", Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                                    }
                                    if (setupStep < 3) {
                                        Surface(onClick = { setupStep++ }, shape = RoundedCornerShape(8.dp), color = Color(0xFF1E3A2A)) { Text("Suivant", Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = Color(0xFF77D9A7), fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                                    }
                                }
                            }
                        }
                    } else {
                        // ── Bouton retour Setup ──
                        Button(onClick = { phase = PlayPhase.PREP }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A3A4A))) {
                            Text(stringResource(R.string.back_to_setup), color = Color(0xFFFFC857), fontWeight = FontWeight.Bold)
                        }
                    }
                    }  // fin bloc setup (masqué en écran dédié plein écran phase GAME)
                    // ── Features de jeu (phase GAME) — écran dédié plein écran ──
                    if (phase == PlayPhase.GAME) {
                    Column(Modifier.fillMaxSize()) {
                        Column(Modifier.weight(1f).statusBarsPadding(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Bouton discret pour revenir au setup (seule sortie du plein écran de jeu).
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Surface(onClick = { phase = PlayPhase.PREP }, shape = RoundedCornerShape(8.dp), color = Color(0xFF1A2330)) {
                            Text("✕ Retour setup", Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = Color(0xFFFFC857), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    // ── Barre compacte : MissionDashboard (ROUND/VP/TOUR + mission dépliable) ──
                    MissionDashboard(
                        round = round, vpOwn = vp, vpOpp = oppVp, killsOwn = kills, killsOpp = oppKills,
                        objectives = battleCards.filter { it.name in battleDeck },
                        bluePlayer = bluePlayer,
                        cmdMine = roundCommand?.let { id -> cmdCards.firstOrNull { it.id == id } },
                        cmdOpp = oppRoundCommand?.let { id -> cmdCards.firstOrNull { it.id == id } },
                        missionRevealed = missionRevealed,
                        onOpenMission = { showMissionSheet = true },
                        turn = turn,
                        onRoundChange = { r ->
                            if (r in 1..5) {
                                playedCommands = emptySet()
                                orderPool = mapOf("commander" to 0, "corps" to 0, "special" to 0, "support" to 0, "heavy" to 0)
                                ownStates.keys.forEach { id -> playSyncVm.updateOwnState(id) { st -> st.copy(activated = false, orderReceived = false) } }
                                if (!twoPlayer) soloOppStates = soloOppStates.mapValues { (_, st) -> st.copy(activated = false, orderReceived = false) }
                                if (r > round) logEvent("Début du round R$r")
                                round = r
                            }
                        },
                        onToggleTurn = { toggleTurn() }
                    )
                    // ── Bannière ACTIVER (à qui d'activer) — état statique coloré, pas de clignotement ──
                    Surface(onClick = { toggleTurn() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), color = if (turn == "own") Color(0xFFFFC857) else Color(0xFF3B2224)) {
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Text(if (turn == "own") "VOTRE TOUR — Activez une unité" else "TOUR ADVERSE", color = if (turn == "own") Color(0xFF0A0E15) else Color(0xFFFFC7B7), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    // Passer (règle v2 : le joueur peut passer son activation).
                    Surface(onClick = { toggleTurn() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), color = Color(0xFF2A3A4A)) {
                        Text("Passer", Modifier.padding(vertical = 6.dp), color = Color(0xFFFFC857), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                    // ── Résultat de partie (déclenché via le menu ⋮) ──
                    if (gameResult != null) {
                        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A2A))) {
                            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(gameResult!!, color = Color(0xFF77D9A7), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                TextButton(onClick = { gameResult = null }) { Text(stringResource(R.string.new_game), color = Color(0xFFFFC857), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                    // ── Écran central "champ de bataille" (2 armées + commandement) ──
                    // Solo (2 armées même appareil) : opp == null, le champ dépend de l'armée adverse choisie.
                    if ((twoPlayer && opp == null) || oppPlayable.isEmpty()) {
                        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2330))) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(if (twoPlayer) stringResource(R.string.play_opponent) else "Armée adverse (solo)", color = Color(0xFFFFC857), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    when {
                                        twoPlayer && opp == null -> stringResource(R.string.play_connecting)
                                        twoPlayer -> "0 unité reçue — la liste adverse n'est pas encore arrivée"
                                        else -> "Choisissez une armée adverse dans le menu setup pour lancer le mode play"
                                    },
                                    color = Color(0xFF8F9BAD), style = MaterialTheme.typography.bodyMedium)
                                if (twoPlayer) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = { playSyncVm.requestList() }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A3A4A))) {
                                            Text("Renvoyer la synchro", color = Color(0xFFFFC857), fontWeight = FontWeight.Bold)
                                        }
                                        Button(onClick = { playSyncVm.resync() }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A2A))) {
                                            Text("Resync", color = Color(0xFF77D9A7), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        if (playPhase == "command") {
                            CommandSelectionPanel(
                                hand = commandDeck.mapNotNull { id -> cmdCards.firstOrNull { it.id == id } },
                                selected = roundCommand,
                                onSelect = { id -> roundCommand = id },
                                onConfirm = { playPhase = "activation"; roundCommand?.let { id -> cmdCards.firstOrNull { c -> c.id == id }?.let { c -> logEvent("Carte de commandement : ${c.name}") } } }
                            )
                        } else {
                        BattlefieldView(
                            myUnits = playable, oppUnits = oppPlayable,
                            myStates = ownStates, oppStates = if (twoPlayer) oppStates else soloOppStates,
                            myCmd = roundCommand?.let { id -> cmdCards.firstOrNull { it.id == id } },
                            oppCmd = oppRoundCommand?.let { id -> cmdCards.firstOrNull { it.id == id } },
                            turn = turn,
                            onOpenUnit = { selectedUnit = it },
                            onActivate = { id -> playSyncVm.setOwnState(id, (ownStates[id] ?: SyncedUnitState(id)).copy(activated = true)); playable.firstOrNull { it.instanceId == id }?.let { logEvent("${it.card.name} activée") } },
                            onCmdClick = { showCmdSheet = true },
                            showOpponent = true,
                            onOppActivate = if (twoPlayer) null else { id -> soloOppStates = soloOppStates + (id to (soloOppStates[id] ?: SyncedUnitState(id)).copy(activated = true)) },
                            onWound = { id, delta ->
                                if (id in ownStates) {
                                    playSyncVm.updateOwnState(id) { st -> st.copy(wounds = ((st.wounds ?: 0) + delta).coerceAtLeast(0)) }
                                    playable.firstOrNull { it.instanceId == id }?.let { u -> logEvent("${u.card.name} : ${((ownStates[id]?.wounds ?: 0) + delta).coerceAtLeast(0)} blessures") }
                                } else if (!twoPlayer) {
                                    soloOppStates = soloOppStates + (id to ((soloOppStates[id] ?: SyncedUnitState(id)).copy(wounds = ((soloOppStates[id]?.wounds ?: 0) + delta).coerceAtLeast(0))))
                                    oppPlayable.firstOrNull { it.instanceId == id }?.let { u -> logEvent("${u.card.name} (adv) : ${((soloOppStates[id]?.wounds ?: 0) + delta).coerceAtLeast(0)} blessures") }
                                }
                            },
                            onOrder = { id ->
                                if (id in ownStates) {
                                    playSyncVm.updateOwnState(id) { st -> st.copy(orderReceived = !(st.orderReceived == true)) }
                                } else if (!twoPlayer) {
                                    soloOppStates = soloOppStates + (id to ((soloOppStates[id] ?: SyncedUnitState(id)).copy(orderReceived = !(soloOppStates[id]?.orderReceived == true))))
                                }
                            },
                            onCover = { id ->
                                if (id in ownStates) {
                                    playSyncVm.updateOwnState(id) { st -> st.copy(cover = ((st.cover ?: 0) + 1) % 3) }
                                } else if (!twoPlayer) {
                                    soloOppStates = soloOppStates + (id to ((soloOppStates[id] ?: SyncedUnitState(id)).copy(cover = ((soloOppStates[id]?.cover ?: 0) + 1) % 3)))
                                }
                            },
                            round = round, vpA = vp, vpB = oppVp, activeObjectives = activeObjectives,
                            objectives = battleCards.filter { it.category == "objective" },
                            onRound = { r -> round = r },
                            onVP = { side, delta -> if (side == "A") vp = (vp + delta).coerceAtLeast(0) else oppVp = (oppVp + delta).coerceAtLeast(0) },
                            onToggleObjective = { name -> activeObjectives = if (name in activeObjectives) activeObjectives - name else activeObjectives + name },
                            onNextRound = {
                                round += 1
                                ownStates.keys.forEach { id -> playSyncVm.updateOwnState(id) { st -> st.copy(activated = false, orderReceived = false) } }
                                if (!twoPlayer) soloOppStates = soloOppStates.mapValues { (_, st) -> st.copy(activated = false, orderReceived = false) }
                            },
                            onJournal = { showLogSheet = true },
                            onToken = { id, tk, delta ->
                                if (id in ownStates) {
                                    playSyncVm.updateOwnState(id) { st ->
                                        if (tk == "supp") st.copy(suppressions = ((st.suppressions ?: 0) + delta).coerceAtLeast(0))
                                        else { val cur = st.tokens.toMutableList(); if (delta > 0) cur.add(tk) else cur.remove(tk); st.copy(tokens = cur) }
                                    }
                                } else if (!twoPlayer) {
                                    soloOppStates = soloOppStates + (id to ((soloOppStates[id] ?: SyncedUnitState(id)).let { st ->
                                        if (tk == "supp") st.copy(suppressions = ((st.suppressions ?: 0) + delta).coerceAtLeast(0))
                                        else { val cur = st.tokens.toMutableList(); if (delta > 0) cur.add(tk) else cur.remove(tk); st.copy(tokens = cur) }
                                    }))
                                }
                            }
                        )
                        }
                    }
                        }  // fin colonne scrollable du champ de bataille
                        // ── Main de commandement (façon Pokémon TCG) : barre réservée en bas, ne recouvre jamais les unités ──
                        CommandHand(
                            hand = commandDeck.mapNotNull { id -> cmdCards.firstOrNull { it.id == id } },
                            roundCommand = roundCommand,
                            onSelect = { card ->
                                roundCommand = card.id
                                playPhase = "activation"
                                logEvent("Carte de commandement : ${card.name}")
                            }
                        )
                    }  // fin colonne plein écran jeu
                    }  // fin phase GAME
                }
                return@Box
            }
            // ── LISTE : mon armée (éditable) ou armée adverse (lecture seule live) ──
            val isOpp = twoPlayer && viewingArmy == "opp"
            val listEntries = if (isOpp) oppPlayable else playable
            val listAll = if (isOpp) oppEntries else entries
            val states = if (isOpp) oppStates else ownStates
            val readOnly = isOpp
            val onUnitChange: (String, SyncedUnitState) -> Unit = if (isOpp) { _, _ -> } else { id, next -> playSyncVm.setOwnState(id, next) }
            if (listEntries.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (isOpp) "Armée adverse vide (aucune unité reçue)" else stringResource(R.string.add_units_play), color = Color.Gray)
                }
            } else {
                if (phase == PlayPhase.PREP) {
                    // En setup : liste avec boutons de zone de déploiement.
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listEntries, key = { it.instanceId }) { unit ->
                            val unitState = states[unit.instanceId] ?: SyncedUnitState(unit.instanceId)
                            val children = listAll.filter { it.parentInstanceId == unit.instanceId }
                            UnitListRow(
                                unit = unit, state = unitState, children = children,
                                isPrep = true, readOnly = readOnly,
                                onOpen = { selectedUnit = unit },
                                onCycleDeploy = if (readOnly) null else {
                                    { onUnitChange(unit.instanceId, unitState.copy(deployZone = when (unitState.deployZone) { null -> 1; 1 -> 2; else -> null })) }
                                },
                                onCompare = {}
                            )
                        }
                    }
                } else {
                    // En jeu : bandeau d'aperçu (état) + grille de miniatures (action).
                    Column(Modifier.fillMaxSize()) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Activation", color = Color(0xFF9EACBC), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Surface(onClick = {
                                // Simule le tirage du pool d'ordres : unité avec ordre d'abord, sinon au hasard.
                                val candidates = listEntries.filter { u -> val st = states[u.instanceId]; st?.defeated != true && st?.activated != true }
                                val next = candidates.filter { u -> states[u.instanceId]?.orderReceived == true }.shuffled().firstOrNull() ?: candidates.shuffled().firstOrNull()
                                if (next != null) onUnitChange(next.instanceId, (states[next.instanceId] ?: SyncedUnitState(next.instanceId)).copy(activated = true, orderReceived = false))
                            }, shape = RoundedCornerShape(8.dp), color = Color(0xFF1E3A2A)) {
                                Text("⚡ Auto", Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = Color(0xFF77D9A7), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(onClick = { panicRoll = (1..6).random() }, shape = RoundedCornerShape(8.dp), color = Color(0xFF3B2224)) {
                                Text("⚡ Jet de panique", Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = Color(0xFFFFC7B7), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Surface(onClick = {
                                listEntries.forEach { u -> if (states[u.instanceId]?.panicking == true) onUnitChange(u.instanceId, (states[u.instanceId] ?: SyncedUnitState(u.instanceId)).copy(panicking = false)) }
                                panicRoll = null
                            }, shape = RoundedCornerShape(8.dp), color = Color(0xFF2A3A4A)) {
                                Text("Lever la panique", Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = Color(0xFF9EACBC), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            if (panicRoll != null) {
                                Text("Résultat : $panicRoll", color = Color(0xFFFFC857), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        UnitActivationStrip(units = listEntries, states = states, onOpen = { selectedUnit = it })
                        Spacer(Modifier.height(4.dp))
                        UnitActivationGrid(
                            units = listEntries, allEntries = listAll, states = states, readOnly = readOnly,
                            onActivate = { id -> onUnitChange(id, (states[id] ?: SyncedUnitState(id)).copy(activated = true)) },
                            onOpen = { selectedUnit = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
    // Popup de règle : clic sur un mot-clé du texte de règles -> point de règle officiel.
    ruleSection?.let { section ->
        RulePopup(section = section, onClose = { ruleSection = null })
    }
    // Sheet battle deck (battle cards v2, Legion) — 9 cartes 3+3+3 + mission.
    if (showBattleSheet) {
        BattleDeckSheet(
            cards = battleCards,
            deck = battleDeck,
            revealed = battleRevealed,
            onToggleDeck = { name ->
                val cat = battleCards.firstOrNull { it.name == name }?.category
                if (name in battleDeck) {
                    battleDeck = battleDeck - name
                    if (cat != null && battleRevealed[cat] == name) battleRevealed = battleRevealed - cat
                } else {
                    battleDeck = battleDeck + name
                    if (cat != null && battleRevealed[cat] == null) battleRevealed = battleRevealed + (cat to name)
                }
            },
            onCycleReveal = { cat ->
                val opts = battleCards.filter { it.category == cat && it.name in battleDeck }.map { it.name }.distinct()
                if (opts.isNotEmpty()) {
                    val cur = battleRevealed[cat]
                    val idx = opts.indexOf(cur).coerceAtLeast(0)
                    battleRevealed = battleRevealed + (cat to opts[(idx + 1) % opts.size])
                }
            },
            deckComplete = { cat -> battleCards.count { it.category == cat && it.name in battleDeck } >= 3 },
            onAutoFill = {
                val auto = battleCards.groupBy { it.category }
                val newDeck = battleDeck.toMutableSet()
                listOf("objective", "secondary", "advantage").forEach { cat ->
                    val have = battleCards.count { it.category == cat && it.name in newDeck }
                    val need = 3 - have
                    if (need > 0) auto[cat]?.filter { it.name !in newDeck }?.take(need)?.forEach { newDeck.add(it.name) }
                }
                battleDeck = newDeck
                listOf("objective", "secondary", "advantage").forEach { cat ->
                    if (battleRevealed[cat] == null) {
                        val first = battleCards.firstOrNull { it.category == cat && it.name in newDeck }?.name
                        if (first != null) battleRevealed = battleRevealed + (cat to first)
                    }
                }
            },
            onClose = { showBattleSheet = false }
        )
    }
    // Sheet du deck de cartes de commandement (Legion) — main officielle de 7.
    if (showCmdSheet) {
        val armyNames = remember { vm.entries.value.map { it.card.displayName() }.toSet() }
        CommandDeckSheet(
            cards = cmdCards.filter {
                (it.generic || it.faction == cmdFaction) &&
                (it.commanders.isEmpty() || it.commanders.any { c -> c in armyNames })
            },
            deck = commandDeck,
            played = playedCommands,
            roundCommand = roundCommand,
            onToggleDeck = { id -> commandDeck = if (id in commandDeck) commandDeck - id else commandDeck + id },
            onTogglePlayed = { id -> playedCommands = if (id in playedCommands) playedCommands - id else playedCommands + id },
            onSelectRound = { id -> roundCommand = if (roundCommand == id) null else id },
            onAutoFill = {
                val avail = cmdCards.filter {
                    (it.generic || it.faction == cmdFaction) &&
                    (it.commanders.isEmpty() || it.commanders.any { c -> c in armyNames })
                }
                val newDeck = commandDeck.toMutableSet()
                listOf("1", "2", "3").forEach { pip ->
                    val have = commandDeck.count { id -> avail.any { it.id == id && it.pip == pip } }
                    val need = 2 - have
                    if (need > 0) avail.filter { it.pip == pip && it.id !in newDeck }.take(need).forEach { newDeck.add(it.id) }
                }
                commandDeck = newDeck
            },
            onClose = { showCmdSheet = false }
        )
    }
    // Sheet du lanceur de dés (PR5) — résolution d'attaque.
    if (showDiceSheet) {
        DiceRollerSheet(onClose = { showDiceSheet = false })
    }
    if (showDeploySheet) {
        DeploymentSheet(onClose = { showDeploySheet = false })
    }
    if (showPoolSheet) {
        PoolSheet(orderPool, { rank, v -> orderPool = orderPool + (rank to v) }, onClose = { showPoolSheet = false })
    }
    // Déploiement des 2 armées (solo = les 2, 2 joueurs = la mienne).
    if (showDeployment) {
        DeploymentSheet(
            myUnits = playable, oppUnits = oppPlayable,
            myStates = ownStates, oppStates = if (twoPlayer) oppStates else soloOppStates,
            allMy = entries, allOpp = if (twoPlayer) oppEntries else soloOppEntries,
            showOpp = !twoPlayer,
            onCycleMy = { id -> playSyncVm.updateOwnState(id) { st -> st.copy(deployZone = when (st.deployZone) { null -> 1; 1 -> 2; else -> null }) } },
            onCycleOpp = {
                soloOppStates = soloOppStates + (it to ((soloOppStates[it] ?: SyncedUnitState(it)).copy(deployZone = when ((soloOppStates[it] ?: SyncedUnitState(it)).deployZone) { null -> 1; 1 -> 2; else -> null })))
            },
            onOpenMy = { selectedUnit = it }, onOpenOpp = { selectedUnit = it },
            onClose = { showDeployment = false }
        )
    }
    // Journal de partie (P2) : log chronologique exportable.
    if (showLogSheet) {
        GameLogSheet(gameLog, onClose = { showLogSheet = false })
    }
    // Sélecteur d'armée adverse (mode solo).
    if (showOppPicker) {
        val allLists by vm.allLists.collectAsState()
        ModalBottomSheet(onDismissRequest = { showOppPicker = false }, containerColor = Color(0xFF111827)) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Choisir l'armée adverse", color = Color(0xFFFFC857), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (allLists.isEmpty()) Text("Aucune liste enregistrée", color = Color(0xFF8F9BAD), style = MaterialTheme.typography.bodySmall)
                allLists.forEach { l ->
                    Surface(onClick = { vm.loadOpponentList(l.id); showOppPicker = false }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), color = Color(0xFF1A2330)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(l.name, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text("${l.factionId} • ${l.pointsLimit} pts", color = Color(0xFF8F9BAD), fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
    // Round 0 / Build a Mission (v2.6).
    if (showMissionSheet) {
        MissionBuilderSheet(
            battleCards = battleCards, battleDeck = battleDeck,
            bluePlayer = bluePlayer, missionRevealed = missionRevealed, modsLeft = modsLeft,
            onSetBlue = { bluePlayer = it },
            onReveal = { key, name -> missionRevealed = missionRevealed + (key to name) },
            onMod = { p, opt ->
                when (opt) {
                    "objective", "secondary", "advantage" -> {
                        val key = "${p}_$opt"
                        val opts = battleCards.filter { it.category == opt && it.name in battleDeck }.map { it.name }.distinct()
                        if (opts.isNotEmpty()) {
                            val cur = missionRevealed[key]
                            val idx = opts.indexOf(cur).coerceAtLeast(0)
                            missionRevealed = missionRevealed + (key to opts[(idx + 1) % opts.size])
                        }
                    }
                    "steal" -> bluePlayer = if (bluePlayer == "own") "opp" else "own"
                    "pass" -> {}
                }
                modsLeft = modsLeft + (p to ((modsLeft[p] ?: 0) - 1).coerceAtLeast(0))
            },
            onClose = { showMissionSheet = false }
        )
    }
    // Détail d'unité (refonte A1/A8) : sheet réutilisant la page unité.
    selectedUnit?.let { unit ->
        val isOppDetail = twoPlayer && viewingArmy == "opp"
        val detailAll = if (isOppDetail) oppEntries else entries
        val detailStates = if (isOppDetail) oppStates else ownStates
        val detailState = detailStates[unit.instanceId] ?: SyncedUnitState(unit.instanceId)
        val detailChildren = detailAll.filter { it.parentInstanceId == unit.instanceId }
        val detailWiki = remember(unit, wikiSections) { wikiSections.filter { it.gameSystem == unit.card.gameSystem.name } }
        val detailReadOnly = isOppDetail
        val detailOnChange: (SyncedUnitState) -> Unit = if (detailReadOnly) { _ -> } else { next -> playSyncVm.setOwnState(unit.instanceId, next) }
        UnitDetailSheet(unit, detailChildren, detailAll, detailWiki, onRuleClick, detailState, detailOnChange, detailReadOnly, onDice = { showDiceSheet = true }, onCompare = { val next = (compareSel + unit).distinctBy { it.instanceId }; compareSel = next; if (next.size >= 2) showCompare = true }, onClose = { selectedUnit = null })
    }
    // Vue comparée 2 unités (A2).
    if (showCompare && compareSel.size >= 2) {
        val isOppCompare = twoPlayer && viewingArmy == "opp"
        val compareAll = if (isOppCompare) oppEntries else entries
        val compareStates = if (isOppCompare) oppStates else ownStates
        val compareReadOnly = isOppCompare
        val compareOnChange: (String, SyncedUnitState) -> Unit = if (compareReadOnly) { _, _ -> } else { id, next -> playSyncVm.setOwnState(id, next) }
        UnitCompareSheet(compareSel[0], compareSel[1], compareAll, compareStates, wikiSections, onRuleClick, compareOnChange, compareReadOnly, onClose = { showCompare = false; compareSel = emptyList() })
    }
}

// ═══════════════════  LEGION  ═══════════════════════════
// Stats réels des cartes Legion v2 embarqués dans card.legionStats (JSON),
// générés par build_legion_v2.py depuis le bundle LegionHQ V2 (2.6).
internal data class LegionWeapon(val name: String, val rangeMin: Int, val rangeMax: Int,
                                val red: Int, val black: Int, val white: Int)
internal data class LegionStats(
    val health: Int = 1,
    val courage: Int = 1,
    val speed: Int = 1,
    val defenseDie: String = "w",        // 'r' | 'w'
    val surgeAttack: String = "",        // 'h' crit | 'a' hit | 'b' block | '' none
    val surgeDefense: String = "",
    val miniCount: Int = 1,
    val keywords: List<String> = emptyList(),
    val weapons: List<LegionWeapon> = emptyList()
)
internal object LegionStatsParser {
    fun parse(legionStats: String?): LegionStats? {
        if (legionStats.isNullOrBlank()) return null
        return try {
            val o = org.json.JSONObject(legionStats)
            val wp = buildList {
                val wa = o.optJSONArray("weapons")
                if (wa != null) for (i in 0 until wa.length()) {
                    val w = wa.getJSONObject(i)
                    val rng = w.optJSONObject("range")
                    val dice = w.optJSONObject("dice")
                    add(LegionWeapon(
                        w.optString("name", "Arme"),
                        rng?.optInt("min", 0) ?: 0, rng?.optInt("max", 0) ?: 0,
                        dice?.optInt("red", 0) ?: 0, dice?.optInt("black", 0) ?: 0, dice?.optInt("white", 0) ?: 0
                    ))
                }
            }
            val kw = buildList {
                val ka = o.optJSONArray("keywords")
                if (ka != null) for (i in 0 until ka.length()) add(ka.getString(i))
            }
            LegionStats(
                health = o.optInt("health", 1).coerceAtLeast(1),
                courage = o.optInt("courage", 1).coerceAtLeast(1),
                speed = o.optInt("speed", 1).coerceAtLeast(1),
                defenseDie = o.optString("defenseDie", "w"),
                surgeAttack = o.optString("surgeAttack", ""),
                surgeDefense = o.optString("surgeDefense", ""),
                miniCount = o.optInt("miniCount", 1).coerceAtLeast(1),
                keywords = kw,
                weapons = wp
            )
        } catch (_: Exception) { null }
    }
}

// ── Rendu lisible des conditions d'équipement Legion ─────────────────────────
// Le rulesText des upgrades LEGION est du JSON de "requirements" (conditions
// pour équiper l'upgrade sur une unité), PAS un texte d'effet. On le rend
// humainement lisible au lieu d'afficher le JSON brut. (Armada = déjà lisible.)
internal fun readableUpgradeText(rulesText: String?): String? {
    val rt = rulesText?.trim() ?: return null
    if (!rt.startsWith("{")) return stripBracketName(rt) // Armada : texte déjà lisible
    return try {
        val req = renderLegionRequirement(org.json.JSONObject(rt).opt("requirements")) ?: return null
        "Équipable sur : $req"
    } catch (_: Exception) { null }
}

/** Texte d'effet d'une upgrade pour le mode play : effectText réel (source
 * DoctorDizzee) prioritaire, sinon les conditions d'équipement lisibles. */
internal fun upgradeEffectText(card: com.legionforge.app.data.model.CardDefinition): String? =
    card.effectText?.takeIf { it.isNotBlank() } ?: readableUpgradeText(card.rulesText)

private fun renderLegionRequirement(node: Any?): String? = when (node) {
    is org.json.JSONArray -> {
        val items = (0 until node.length()).map { node.get(it) }
        if (items.isNotEmpty() && items[0] is String) {
            val op = items[0] as String
            val rest = items.drop(1).mapNotNull { renderLegionRequirement(it) }
            when {
                op.equals("OR", true) -> rest.joinToString(" OU ")
                op.equals("AND", true) -> rest.joinToString(" ET ")
                op.equals("NOT", true) -> "pas ${rest.firstOrNull() ?: ""}"
                else -> rest.joinToString(" ")
            }
        } else items.mapNotNull { renderLegionRequirement(it) }.joinToString(" ")
    }
    is org.json.JSONObject -> {
        val parts = mutableListOf<String>()
        node.keys().forEach { k ->
            when (k) {
                "cardName" -> parts.add(node.optString("cardName"))
                "title" -> parts.add("\"${node.optString("title")}\"")
                "cardSubtype" -> parts.add(titleCase(node.optString("cardSubtype")))
                "faction" -> parts.add(titleCase(node.optString("faction")))
                "rank" -> parts.add(titleCase(node.optString("rank")))
                "forceAffinity" -> parts.add(if (node.optString("forceAffinity") == "light side") "côté clair" else "côté obscur")
                "upgradeBar" -> { val a = node.optJSONArray("upgradeBar"); if (a != null && a.length() > 0) parts.add("slot ${titleCase(a.getString(0))}") }
                "keywords" -> { val a = node.optJSONArray("keywords"); if (a != null && a.length() > 0) parts.add(a.getString(0)) }
                "stats" -> { val s = node.optJSONObject("stats"); if (s != null) parts.add("défense ${s.optString("defense")}") }
            }
        }
        parts.joinToString(" ").ifEmpty { null }
    }
    is String -> node
    else -> node?.toString()
}

private fun titleCase(s: String): String = s.replace('_', ' ').trim().replaceFirstChar { it.uppercase() }

// ── Cartes de commandement Legion ────────────────────────────────────────
internal data class LegionCommandCard(
    val id: String,
    val name: String,
    val pip: String,             // "1" | "2" | "3" | "4"
    val faction: String,
    val generic: Boolean = false,
    val commanders: List<String> = emptyList(),
    val keywords: List<String> = emptyList(),
    val imageAssetPath: String? = null,
    val imageAssetPathFr: String? = null
) {
    fun imagePath(): String? = if (java.util.Locale.getDefault().language == "fr") imageAssetPathFr ?: imageAssetPath else imageAssetPath
}

internal fun loadCommandCards(context: android.content.Context): List<LegionCommandCard> {
    return try {
        val s = context.assets.open("command_cards.json").bufferedReader().use { it.readText() }
        val arr = org.json.JSONObject(s).getJSONArray("cards")
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val cmdrs = buildList { val a = o.optJSONArray("commanders"); if (a != null) for (j in 0 until a.length()) add(a.getString(j)) }
                val kws = buildList { val a = o.optJSONArray("keywords"); if (a != null) for (j in 0 until a.length()) add(a.getString(j)) }
                add(LegionCommandCard(o.optString("id"), o.optString("name"), o.optString("pip"), o.optString("faction"), o.optBoolean("generic"), cmdrs, kws, o.optString("imageAssetPath").takeIf { it.isNotBlank() }, o.optString("imageAssetPathFr").takeIf { it.isNotBlank() }))
            }
        }
    } catch (_: Exception) { emptyList() }
}

internal data class LegionBattleCard(val category: String, val name: String, val imageAssetPath: String? = null, val imageAssetPathFr: String? = null) {
    fun imagePath(): String? = if (java.util.Locale.getDefault().language == "fr") imageAssetPathFr ?: imageAssetPath else imageAssetPath
}

internal fun loadBattleCards(context: android.content.Context): List<LegionBattleCard> {
    return try {
        val s = context.assets.open("battle_cards.json").bufferedReader().use { it.readText() }
        val arr = org.json.JSONObject(s).getJSONArray("cards")
        buildList { for (i in 0 until arr.length()) { val o = arr.getJSONObject(i); add(LegionBattleCard(o.optString("category"), o.optString("name"), o.optString("imageAssetPath").takeIf { it.isNotBlank() }, o.optString("imageAssetPathFr").takeIf { it.isNotBlank() })) } }
    } catch (_: Exception) { emptyList() }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun BattleDeckSheet(
    cards: List<LegionBattleCard>,
    deck: Set<String>,
    revealed: Map<String, String>,           // category -> carte révélée
    onToggleDeck: (String) -> Unit,
    onCycleReveal: (String) -> Unit,         // category -> passer à la carte suivante du deck
    deckComplete: (String) -> Boolean,       // category -> 3 sélectionnées
    onAutoFill: () -> Unit,
    onClose: () -> Unit
) {
    val order = listOf("objective" to "OBJECTIVE + MAP", "secondary" to "SECONDARY OBJECTIVE", "advantage" to "ADVANTAGE")
    var zoomCard by remember { mutableStateOf<LegionBattleCard?>(null) }
    ModalBottomSheet(onDismissRequest = onClose, containerColor = Color(0xFF111827)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("BATTLE DECK (9 cartes)", color = Color(0xFFFFC857), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Officiel : 3 Objective(+Map) + 3 Secondary + 3 Advantage, sans doublon.", color = Color(0xFF9EACBC), style = MaterialTheme.typography.bodySmall)
                }
                Surface(onClick = onAutoFill, shape = RoundedCornerShape(8.dp), color = Color(0xFF1E3A2A)) {
                    Text("⚡ Auto", Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = Color(0xFF77D9A7), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Box(Modifier.fillMaxWidth().height(2.dp).background(Color(0xFF2A3A4A)))
            order.forEach { (cat, label) ->
                val catCards = cards.filter { it.category == cat }
                if (catCards.isEmpty()) return@forEach
                val sel = deck.count { id -> catCards.any { it.name == id } }
                val rev = revealed[cat]
                // En-tête compact : label + count + mission + bouton ↻ sur une ligne.
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("$label ($sel/3)", color = Color(0xFFB4BFCE), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Text("→ ${rev.takeIf { !it.isNullOrEmpty() } ?: "—"}", color = Color(0xFFFFC857), fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (deckComplete(cat)) Surface(onClick = { onCycleReveal(cat) }, shape = RoundedCornerShape(6.dp), color = Color(0xFF2A3A4A)) { Text("↻", Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = Color(0xFFFFC857), fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                }
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    catCards.forEach { c ->
                        val inDeck = c.name in deck
                        val full = sel >= 3
                        DeckCardThumb(
                            imageAssetPath = c.imagePath(),
                            name = c.name,
                            badge = if (inDeck && revealed[cat] == c.name) "★" else null,
                            selected = inDeck,
                            dimmed = full && !inDeck,
                            onClick = { zoomCard = c }
                        )
                    }
                }
            }
        }
    }
    zoomCard?.let { c ->
        val inDeck = c.name in deck
        val cat = c.category
        val full = deck.count { id -> cards.any { it.name == id && it.category == cat } } >= 3
        DeckCardZoom(
            imageAssetPath = c.imagePath(),
            title = c.name,
            subtitle = order.firstOrNull { it.first == cat }?.second,
            inDeck = inDeck,
            canAdd = !full,
            onToggleDeck = { onToggleDeck(c.name) },
            onClose = { zoomCard = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun CommandDeckSheet(
    cards: List<LegionCommandCard>,
    deck: Set<String>,
    played: Set<String>,
    roundCommand: String?,
    onToggleDeck: (String) -> Unit,
    onTogglePlayed: (String) -> Unit,
    onSelectRound: (String) -> Unit,
    onAutoFill: () -> Unit,
    onClose: () -> Unit
) {
    // Règle officielle v2 : main = 2×1-pip + 2×2-pip + 2×3-pip + Standing Orders (4-pip, obligatoire).
    val standingOrders = cards.firstOrNull { it.pip == "4" }
    val hand = deck + (standingOrders?.let { setOf(it.id) } ?: emptySet())
    fun selectedCount(pip: String): Int = cards.count { it.pip == pip && it.id in hand }
    var zoomCard by remember { mutableStateOf<LegionCommandCard?>(null) }
    ModalBottomSheet(onDismissRequest = onClose, containerColor = Color(0xFF111827)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("MAIN DE COMMANDEMENT", color = Color(0xFFFFC857), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Officielle : 2×1-pip + 2×2-pip + 2×3-pip + Standing Orders (4-pip).  ${hand.size}/7", color = Color(0xFF9EACBC), style = MaterialTheme.typography.bodySmall)
                }
                Surface(onClick = onAutoFill, shape = RoundedCornerShape(8.dp), color = Color(0xFF1E3A2A)) {
                    Text("⚡ Auto", Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = Color(0xFF77D9A7), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            // Standing Orders : toujours incluse, verrouillée.
            if (standingOrders != null) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    DeckCardThumb(
                        imageAssetPath = standingOrders.imagePath(),
                        name = standingOrders.name,
                        badge = "4",
                        selected = true,
                        dimmed = false,
                        onClick = { zoomCard = standingOrders }
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(standingOrders.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("✓ obligatoire", color = Color(0xFF77D9A7), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(2.dp).background(Color(0xFF2A3A4A)))
            listOf("1", "2", "3").forEach { pip ->
                val pipCards = cards.filter { it.pip == pip }
                if (pipCards.isEmpty()) return@forEach
                val sel = selectedCount(pip)
                Text("PIP $pip  ($sel/2)", color = Color(0xFFB4BFCE), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    pipCards.forEach { c ->
                        val inDeck = c.id in deck
                        val isPlayed = c.id in played
                        val full = sel >= 2
                        DeckCardThumb(
                            imageAssetPath = c.imagePath(),
                            name = c.name,
                            badge = if (c.id == roundCommand) "★$pip" else pip,
                            selected = inDeck,
                            dimmed = full && !inDeck,
                            onClick = { zoomCard = c }
                        )
                    }
                }
            }
        }
    }
    zoomCard?.let { c ->
        val inDeck = c.id in deck
        val isPlayed = c.id in played
        val pip = c.pip
        val full = selectedCount(pip) >= 2
        val locked = pip == "4"   // Standing Orders : toujours dans la main, non retirable.
        DeckCardZoom(
            imageAssetPath = c.imagePath(),
            title = c.name,
            subtitle = "PIP $pip",
            inDeck = inDeck,
            canAdd = !full,
            locked = locked,
            onToggleDeck = { onToggleDeck(c.id) },
            onTogglePlayed = { onTogglePlayed(c.id) },
            isPlayed = isPlayed,
            roundSelected = c.id == roundCommand,
            onSelectRound = { onSelectRound(c.id) },
            onClose = { zoomCard = null }
        )
    }
}

// ── Sheet de déploiement (PR1) : ordre + zones ──────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DeploymentSheet(onClose: () -> Unit) {
    var first by remember { mutableStateOf("own") }  // qui déploie en premier
    ModalBottomSheet(onDismissRequest = onClose, containerColor = Color(0xFF111827)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.deploy), color = Color(0xFF77D9A7), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            // Ordre de déploiement (seule info de déploiement utile — les zones se marquent en tapant les unités de la liste).
            Text(stringResource(R.string.deploy_order), color = Color(0xFF9EACBC), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Surface(onClick = { first = "own" }, shape = RoundedCornerShape(8.dp), color = if (first == "own") Color(0xFF77D9A7) else Color(0xFF2A3A4A), modifier = Modifier.weight(1f)) { Text(stringResource(R.string.play_my_army), Modifier.padding(vertical = 10.dp), color = if (first == "own") Color(0xFF0A0E15) else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
                Spacer(Modifier.width(8.dp))
                Surface(onClick = { first = "opp" }, shape = RoundedCornerShape(8.dp), color = if (first == "opp") Color(0xFF77D9A7) else Color(0xFF2A3A4A), modifier = Modifier.weight(1f)) { Text(stringResource(R.string.play_opponent), Modifier.padding(vertical = 10.dp), color = if (first == "opp") Color(0xFF0A0E15) else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
            }
            Text("Marquez les unités déployées en les tapant dans la liste (Zone 1 / Zone 2).", color = Color(0xFF5A6A7A), fontSize = 11.sp)
        }
    }
}

// ── Pool d'ordres (accessible via menu ⋮) ────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PoolSheet(orderPool: Map<String, Int>, onSet: (String, Int) -> Unit, onClose: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onClose, containerColor = Color(0xFF111827)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.order_pool), color = Color(0xFFFFC857), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            listOf("commander" to stringResource(R.string.rank_commander), "corps" to stringResource(R.string.rank_corps), "special" to stringResource(R.string.rank_special), "support" to stringResource(R.string.rank_support), "heavy" to stringResource(R.string.rank_heavy)).forEach { (rank, label) ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label, color = Color(0xFF9EACBC), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Stepper("${orderPool[rank] ?: 0}", { onSet(rank, ((orderPool[rank] ?: 0) - 1).coerceAtLeast(0)) }, { onSet(rank, ((orderPool[rank] ?: 0) + 1).coerceAtMost(9)) }, size = 34.dp, valueWidth = 26.dp)
                }
            }
        }
    }
}


// ── Journal de partie (P2) : log chronologique exportable ──
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GameLogSheet(log: List<String>, onClose: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    ModalBottomSheet(onDismissRequest = onClose, containerColor = Color(0xFF111827)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Journal de partie", color = Color(0xFFFFC857), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (log.isEmpty()) {
                Text("Aucun événement enregistré", color = Color(0xFF8F9BAD), style = MaterialTheme.typography.bodySmall)
            } else {
                Column(Modifier.fillMaxWidth().heightIn(max = 320.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    log.forEach { e -> Text(e, color = Color(0xFFD5DEE8), fontSize = 12.sp) }
                }
            }
            Surface(onClick = {
                val text = log.joinToString("\n")
                val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_TEXT, text)
                }
                ctx.startActivity(android.content.Intent.createChooser(send, "Exporter le journal"))
            }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), color = Color(0xFF1E3A2A)) {
                Text("Exporter le journal", Modifier.padding(vertical = 12.dp), color = Color(0xFF77D9A7), fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
        }
    }
}

// ── Round 0 / Build a Mission (v2.6) : blue player + révélation + modifications ──
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MissionBuilderSheet(
    battleCards: List<LegionBattleCard>,
    battleDeck: Set<String>,
    bluePlayer: String?,
    missionRevealed: Map<String, String>,
    modsLeft: Map<String, Int>,
    onSetBlue: (String) -> Unit,
    onReveal: (String, String) -> Unit,
    onMod: (String, String) -> Unit,
    onClose: () -> Unit
) {
    val deckCards = battleCards.filter { it.name in battleDeck }
    fun catCards(cat: String) = deckCards.filter { it.category == cat }
    ModalBottomSheet(onDismissRequest = onClose, containerColor = Color(0xFF111827)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("ROUND 0 — BUILD A MISSION", color = Color(0xFFFFC857), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Blue Player (détermine qui commence)", color = Color(0xFF9EACBC), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(onClick = { onSetBlue("own") }, shape = RoundedCornerShape(8.dp), color = if (bluePlayer == "own") Color(0xFF4FC3F7) else Color(0xFF2A3A4A), modifier = Modifier.weight(1f)) { Text("MOI", Modifier.padding(vertical = 8.dp), color = if (bluePlayer == "own") Color(0xFF0A0E15) else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
                Surface(onClick = { onSetBlue("opp") }, shape = RoundedCornerShape(8.dp), color = if (bluePlayer == "opp") Color(0xFF4FC3F7) else Color(0xFF2A3A4A), modifier = Modifier.weight(1f)) { Text("ADV", Modifier.padding(vertical = 8.dp), color = if (bluePlayer == "opp") Color(0xFF0A0E15) else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
            }
            listOf("own" to "MOI", "opp" to "ADV").forEach { (p, label) ->
                Text("$label — révélation", color = Color(0xFFB4BFCE), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                listOf("objective" to "Objectif", "secondary" to "Secondaire", "advantage" to "Avantage").forEach { (cat, clabel) ->
                    val key = "${p}_$cat"
                    val cur = missionRevealed[key]
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(clabel, color = Color(0xFF9EACBC), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        if (cur != null) {
                            Text(cur, color = Color(0xFFFFC857), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
                        } else {
                            val opts = catCards(cat)
                            if (opts.isNotEmpty()) {
                                Surface(onClick = { onReveal(key, opts.first().name) }, shape = RoundedCornerShape(6.dp), color = Color(0xFF1E3A2A)) { Text("Révéler", Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = Color(0xFF77D9A7), fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                }
            }
            Text("Modifications (2 chacun)", color = Color(0xFFB4BFCE), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
            listOf("own" to "MOI", "opp" to "ADV").forEach { (p, label) ->
                val left = modsLeft[p] ?: 0
                Text("$label : $left restante(s)", color = Color(0xFF9EACBC), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                if (left > 0) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(onClick = { onMod(p, "objective") }, shape = RoundedCornerShape(6.dp), color = Color(0xFF2A3A4A), modifier = Modifier.weight(1f)) { Text("Obj", Modifier.padding(vertical = 6.dp), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
                        Surface(onClick = { onMod(p, "secondary") }, shape = RoundedCornerShape(6.dp), color = Color(0xFF2A3A4A), modifier = Modifier.weight(1f)) { Text("Sec", Modifier.padding(vertical = 6.dp), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
                        Surface(onClick = { onMod(p, "advantage") }, shape = RoundedCornerShape(6.dp), color = Color(0xFF2A3A4A), modifier = Modifier.weight(1f)) { Text("Adv", Modifier.padding(vertical = 6.dp), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
                        Surface(onClick = { onMod(p, "steal") }, shape = RoundedCornerShape(6.dp), color = Color(0xFF3B2224), modifier = Modifier.weight(1f)) { Text("Voler blue", Modifier.padding(vertical = 6.dp), color = Color(0xFFFFC7B7), fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
                        Surface(onClick = { onMod(p, "pass") }, shape = RoundedCornerShape(6.dp), color = Color(0xFF2A3A4A), modifier = Modifier.weight(1f)) { Text("Passer", Modifier.padding(vertical = 6.dp), color = Color(0xFF9EACBC), fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
                    }
                }
            }
        }
    }
}

// ── Emplacement de carte du Mission Dashboard (slots Iron Squadron) ──
@Composable
internal fun DashSlot(title: String, name: String, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(8.dp), color = Color(0xFF1A2330), border = BorderStroke(1.dp, color)) {
        Column(Modifier.padding(horizontal = 4.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = color, fontSize = 7.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, textAlign = TextAlign.Center)
            Text(name, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        }
    }
}

// ── MISSION DASHBOARD (style officiel AMG) : repliable, à déployer pour visualiser la mission ──
@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun MissionDashboard(
    round: Int,
    vpOwn: Int, vpOpp: Int,
    killsOwn: Int, killsOpp: Int,
    objectives: List<LegionBattleCard>,
    bluePlayer: String?,
    cmdMine: LegionCommandCard? = null,
    cmdOpp: LegionCommandCard? = null,
    missionRevealed: Map<String, String> = emptyMap(),
    turn: String = "own",
    onRoundChange: (Int) -> Unit = {},
    onToggleTurn: () -> Unit = {},
    onOpenMission: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }  // replié : barre compacte, mission dépliable
    // Mission en jeu (règles v2) : 1 Objectif + 1 Secondaire + 1 Avantage/joueur + les 2 cartes de commandement. La Map est appariée à l'Objectif.
    val objName = missionRevealed["own_objective"] ?: missionRevealed["opp_objective"]
    val secName = missionRevealed["own_secondary"] ?: missionRevealed["opp_secondary"]
    val advOwn = missionRevealed["own_advantage"]
    val advOpp = missionRevealed["opp_advantage"]
    val mapName = objName
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = Color(0xFF16212E), border = BorderStroke(1.dp, if (expanded) Color(0xFFF59E0B) else Color(0xFF2A3A4A))) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Barre compacte : ROUND -/+ | VP | TOUR | MISSION (dépliable)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(onClick = { onRoundChange(round - 1) }, shape = CircleShape, color = Color(0xFF2A3A4A), modifier = Modifier.size(24.dp)) { Box(contentAlignment = Alignment.Center) { Text("-", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold) } }
                    Text("R$round", color = Color(0xFFFFC857), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Surface(onClick = { onRoundChange(round + 1) }, shape = CircleShape, color = Color(0xFF1E3A2A), modifier = Modifier.size(24.dp)) { Box(contentAlignment = Alignment.Center) { Text("+", color = Color(0xFF77D9A7), fontSize = 10.sp, fontWeight = FontWeight.Bold) } }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("$vpOwn", color = Color(0xFFFFC857), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("—", color = Color(0xFF5A6A7A), fontSize = 11.sp)
                    Text("$vpOpp", color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Surface(onClick = onToggleTurn, shape = RoundedCornerShape(8.dp), color = if (turn == "own") Color(0xFFFFC857) else Color(0xFF2A3A4A)) {
                    Text(if (turn == "own") "VOTRE TOUR" else "Tour adv →", Modifier.padding(horizontal = 8.dp, vertical = 5.dp), color = if (turn == "own") Color(0xFF0A0E15) else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                Surface(onClick = { expanded = !expanded }, shape = RoundedCornerShape(8.dp), color = Color(0xFF1A2330)) {
                    Text("MISSION ${if (expanded) "▲" else "▼"}", Modifier.padding(horizontal = 8.dp, vertical = 5.dp), color = Color(0xFFF59E0B), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                Surface(onClick = onOpenMission, shape = RoundedCornerShape(8.dp), color = Color(0xFF1E3A2A)) {
                    Text("⚙", Modifier.padding(horizontal = 8.dp, vertical = 5.dp), color = Color(0xFF77D9A7), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            // ── Cartes mission (toujours visibles, compactes) ──
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                DashSlot("CMD", cmdMine?.name ?: "?", Color(0xFF6B7280), Modifier.weight(1f))
                DashSlot("OBJECTIF", objName ?: "—", Color(0xFFFF6B6B), Modifier.weight(1f))
                DashSlot("CARTE", mapName ?: "MAP", Color(0xFF38BDF8), Modifier.weight(1f))
                DashSlot("SECONDAIRE", secName ?: "—", Color(0xFFF59E0B), Modifier.weight(1f))
                DashSlot("CMD", cmdOpp?.name ?: "?", Color(0xFF6B7280), Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("AV MOI : ${advOwn ?: "—"}", color = Color(0xFF77D9A7), fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text("ADV : ${advOpp ?: "—"}", color = Color(0xFF77D9A7), fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (expanded) {
                // ── Traqueur de rounds (5 slots) ──
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("ROUND", color = Color(0xFF94A3B8), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(end = 8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (1..5).forEach { r ->
                            Box(Modifier.size(26.dp).clip(RoundedCornerShape(6.dp)).background(if (r == round.coerceAtMost(5)) Color(0xFFFFC857) else Color(0xFF2A3A4A)), contentAlignment = Alignment.Center) {
                                Text("$r", color = if (r == round.coerceAtMost(5)) Color(0xFF0A0E15) else Color(0xFF9EACBC), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                // ── VP + piste 1-12 ──
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("MOI", color = Color(0xFFF59E0B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("$vpOwn", color = Color(0xFFFFC857), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Box(Modifier.size(3.dp).clip(CircleShape).background(Color(0xFF5A6A7A)))
                        Text("$vpOpp", color = Color(0xFF38BDF8), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("ADV", color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(if (bluePlayer == "own") "Blue : MOI" else if (bluePlayer == "opp") "Blue : ADV" else "Blue : —", color = Color(0xFF77D9A7), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    (1..12).forEach { n ->
                        val mine = vpOwn >= n; val theirs = vpOpp >= n
                        Box(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(2.dp)).background(if (mine && theirs) Color(0xFF8B5CF6) else if (mine) Color(0xFFF59E0B) else if (theirs) Color(0xFF38BDF8) else Color(0xFF2A3A4A)))
                    }
                }
                Text("Victoire à 12 VP ou au plus de VP au round 5", color = Color(0xFF8F9BAD), fontSize = 8.sp)
            }
        }
    }
}

// ── Main de commandement (façon Pokémon TCG) : ton éventail de cartes de commande, tap = jouer ce round ──
@Composable
internal fun CommandHand(
    hand: List<LegionCommandCard>,
    roundCommand: String?,
    onSelect: (LegionCommandCard) -> Unit
) {
    val played = hand.firstOrNull { it.id == roundCommand }
    Surface(Modifier.fillMaxWidth().navigationBarsPadding(), shape = RoundedCornerShape(14.dp), color = Color(0xEE0F172A), shadowElevation = 8.dp) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("MAIN DE COMMANDEMENT", color = Color(0xFFF59E0B), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
                Text(
                    if (played != null) "Jouée : ${played.name}" else "Tapez une carte pour ce round",
                    color = if (played != null) Color(0xFF77D9A7) else Color(0xFF8F9BAD), fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(hand, key = { it.id }) { card ->
                    val isSel = card.id == roundCommand
                    Surface(onClick = { onSelect(card) }, shape = RoundedCornerShape(8.dp), color = if (isSel) Color(0xFF3A2E12) else Color(0xFF1E293B), border = if (isSel) BorderStroke(2.dp, Color(0xFFF59E0B)) else BorderStroke(1.dp, Color(0xFF2A3A4A))) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.size(16.dp).clip(CircleShape).background(if (isSel) Color(0xFFF59E0B) else Color(0xFF2A3A4A)), contentAlignment = Alignment.Center) { Text("${card.pip}", color = if (isSel) Color(0xFF0A0E15) else Color(0xFFF59E0B), fontSize = 9.sp, fontWeight = FontWeight.Bold) }
                            Text(card.name, color = if (isSel) Color(0xFFFFC857) else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

// ── Lanceur de dés (PR5) : résolution d'attaque ─────────────────────────
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun DiceRollerSheet(onClose: () -> Unit) {
    var red by remember { mutableIntStateOf(0) }
    var black by remember { mutableIntStateOf(0) }
    var white by remember { mutableIntStateOf(0) }
    var results by remember { mutableStateOf<List<Pair<String, Color>>>(emptyList()) }
    var mode by remember { mutableStateOf("attack") }  // "attack" | "defense"
    // Faces des dés : attaque (hit/crit/surge/blank) ou défense (block/surge/blank).
    fun rollDie(color: String): Pair<String, Color> {
        val faces = if (mode == "defense") {
            when (color) {
                "red" -> listOf("block", "block", "surge", "blank", "blank", "blank", "blank", "blank")
                "black" -> listOf("block", "block", "surge", "blank", "blank", "blank", "blank", "blank")
                else -> listOf("block", "surge", "blank", "blank", "blank", "blank", "blank", "blank")
            }
        } else {
            when (color) {
                "red" -> listOf("hit", "hit", "crit", "surge", "blank", "blank", "blank", "blank")
                "black" -> listOf("hit", "hit", "crit", "surge", "blank", "blank", "blank", "blank")
                else -> listOf("hit", "crit", "surge", "blank", "blank", "blank", "blank", "blank")
            }
        }
        val face = faces.random()
        val c = when (face) {
            "hit" -> Color(0xFFFF6B6B); "crit" -> Color(0xFFFFC857); "block" -> Color(0xFF77D9A7); "surge" -> Color(0xFF4FC3F7); else -> Color(0xFF5A6A7A)
        }
        return face to c
    }
    ModalBottomSheet(onDismissRequest = onClose, containerColor = Color(0xFF111827)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.dice_roller), color = Color(0xFFFFC857), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            // Mode attaque / défense.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Surface(onClick = { mode = "attack"; results = emptyList() }, shape = RoundedCornerShape(8.dp), color = if (mode == "attack") Color(0xFFFFC857) else Color(0xFF2A3A4A), modifier = Modifier.weight(1f)) { Text(stringResource(R.string.attack), Modifier.padding(vertical = 8.dp), color = if (mode == "attack") Color(0xFF0A0E15) else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
                Spacer(Modifier.width(8.dp))
                Surface(onClick = { mode = "defense"; results = emptyList() }, shape = RoundedCornerShape(8.dp), color = if (mode == "defense") Color(0xFF77D9A7) else Color(0xFF2A3A4A), modifier = Modifier.weight(1f)) { Text(stringResource(R.string.defense), Modifier.padding(vertical = 8.dp), color = if (mode == "defense") Color(0xFF0A0E15) else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
            }
            // Sélection du pool de dés.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                DicePoolSelector("R", red, Color(0xFFFF6B6B), { red = (red + 1).coerceAtMost(8) }, { red = (red - 1).coerceAtLeast(0) })
                DicePoolSelector("N", black, Color(0xFF9EACBC), { black = (black + 1).coerceAtMost(8) }, { black = (black - 1).coerceAtLeast(0) })
                DicePoolSelector("B", white, Color(0xFF4FC3F7), { white = (white + 1).coerceAtMost(8) }, { white = (white - 1).coerceAtLeast(0) })
            }
            // Bouton lancer.
            Button(onClick = {
                val pool = buildList { repeat(red) { add("red") }; repeat(black) { add("black") }; repeat(white) { add("white") } }
                results = if (pool.isEmpty()) emptyList() else pool.map { rollDie(it) }
            }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC857))) {
                Text(stringResource(R.string.roll), color = Color(0xFF0A0E15), fontWeight = FontWeight.Bold)
            }
            // Résultats.
            if (results.isNotEmpty()) {
                Text(stringResource(R.string.results), color = Color(0xFF9EACBC), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    results.forEach { (face, c) ->
                        Surface(shape = RoundedCornerShape(8.dp), color = c.copy(alpha = 0.22f), border = androidx.compose.foundation.BorderStroke(1.dp, c)) {
                            Text(face.uppercase(), Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = c, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (mode == "defense") {
                    val blocks = results.count { it.first == "block" }
                    val surges = results.count { it.first == "surge" }
                    Text(stringResource(R.string.dice_summary_def, blocks, surges), color = Color(0xFFFFC857), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                } else {
                    val hits = results.count { it.first == "hit" }
                    val crits = results.count { it.first == "crit" }
                    val surges = results.count { it.first == "surge" }
                    Text(stringResource(R.string.dice_summary, hits, crits, surges), color = Color(0xFFFFC857), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Sélecteur de pool de dés (couleur + compte).
@Composable
internal fun DicePoolSelector(label: String, count: Int, color: Color, onInc: () -> Unit, onDec: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Stepper("$count", onDec, onInc, size = 36.dp, valueColor = color, valueWidth = 28.dp)
    }
}

// ── Miniature de carte (deck commandement / battle) ─────────────────────
@Composable
internal fun DeckCardThumb(
    imageAssetPath: String?,
    name: String,
    badge: String? = null,
    selected: Boolean,
    dimmed: Boolean,
    onClick: () -> Unit
) {
    val source = imageAssetPath?.let { "file:///android_asset/$it" }
    Box(
        Modifier.width(96.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Color(0xFF1E3A2A) else if (dimmed) Color(0xFF11151D) else Color(0xFF192330))
            .clickable(onClick = onClick)
    ) {
        if (source != null) {
            AsyncImage(model = source, contentDescription = name, modifier = Modifier.fillMaxWidth().height(134.dp), contentScale = ContentScale.Fit)
        } else {
            Box(Modifier.fillMaxWidth().height(134.dp), contentAlignment = Alignment.Center) {
                Text(name, color = if (dimmed) Color(0xFF5A6A7A) else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(6.dp))
            }
        }
        if (selected) {
            Box(Modifier.matchParentSize().clip(RoundedCornerShape(10.dp)).border(2.dp, Color(0xFFFFC857), RoundedCornerShape(10.dp)))
        }
        if (badge != null) {
            Box(Modifier.padding(4.dp).clip(CircleShape).background(Color(0xFF0A0E15).copy(alpha = 0.85f)).padding(horizontal = 6.dp, vertical = 2.dp), contentAlignment = Alignment.Center) {
                Text(badge, color = Color(0xFFFFC857), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Zoom carte (deck commandement / battle) : grand + Retirer / Fermer ──
@Composable
internal fun DeckCardZoom(
    imageAssetPath: String?,
    title: String,
    subtitle: String? = null,
    inDeck: Boolean,
    canAdd: Boolean,
    locked: Boolean = false,
    onToggleDeck: () -> Unit,
    onTogglePlayed: (() -> Unit)? = null,
    isPlayed: Boolean = false,
    roundSelected: Boolean = false,
    onSelectRound: (() -> Unit)? = null,
    onClose: () -> Unit
) {
    val source = imageAssetPath?.let { "file:///android_asset/$it" }
    androidx.compose.ui.window.Dialog(onDismissRequest = onClose) {
        androidx.compose.material3.Surface(
            modifier = Modifier.fillMaxWidth(0.96f).verticalScroll(rememberScrollState()),
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF192330)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, color = Color(0xFFFFC857), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                if (subtitle != null) Text(subtitle, color = Color(0xFF9EACBC), style = MaterialTheme.typography.bodySmall)
                if (source != null) {
                    AsyncImage(model = source, contentDescription = title, modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp), contentScale = ContentScale.Fit)
                } else {
                    Box(Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF2A3A4A)), contentAlignment = Alignment.Center) {
                        Text(title, color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(12.dp))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (onTogglePlayed != null && inDeck && !locked) {
                        Surface(onClick = onTogglePlayed, shape = RoundedCornerShape(8.dp), color = if (isPlayed) Color(0xFF5A2E2E) else Color(0xFF2A3A4A)) {
                            Text(if (isPlayed) "✓ JOUÉE" else "Jouée", Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = if (isPlayed) Color(0xFFFFC857) else Color(0xFF77D9A7), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (locked) {
                        Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF1E3A2A)) {
                            Text("✓ Obligatoire", Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = Color(0xFF77D9A7), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (inDeck || canAdd) {
                        Surface(onClick = onToggleDeck, shape = RoundedCornerShape(8.dp), color = if (inDeck) Color(0xFF5A2E2E) else Color(0xFF1E3A2A)) {
                            Text(if (inDeck) "Retirer" else "Ajouter", Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (onSelectRound != null && inDeck) {
                        Surface(onClick = onSelectRound, shape = RoundedCornerShape(8.dp), color = if (roundSelected) Color(0xFF3B2224) else Color(0xFF1E3A2A)) {
                            Text(if (roundSelected) "★ Carte du round" else "♛ Jouer ce round", Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = if (roundSelected) Color(0xFF77D9A7) else Color(0xFFFFC857), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Surface(onClick = onClose, shape = RoundedCornerShape(8.dp), color = Color(0xFF2A3A4A)) {
                        Text("Fermer", Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = Color(0xFF9EACBC), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ── Ligne d'unité ultrafine (P1-4) : nom + jauge coque + activé + vaincu (+ zone déploiement en PREP) ──
@Composable
internal fun UnitListRow(
    unit: ListEntry,
    state: SyncedUnitState,
    children: List<ListEntry>,
    isPrep: Boolean,
    readOnly: Boolean,
    onOpen: () -> Unit,
    onCycleDeploy: (() -> Unit)?,
    onCompare: () -> Unit
) {
    val totalPts = unit.card.points + children.sumOf { it.card.points * it.quantity }
    val maxHp = when (unit.card.kind) {
        CardKind.LEGION_UNIT -> {
            val st = remember(unit.instanceId) { LegionStatsParser.parse(unit.card.legionStats) }
            (st?.health?.takeIf { it > 0 } ?: 1) * (st?.miniCount?.takeIf { it > 0 } ?: 1)
        }
        else -> null
    }
    val remaining = when {
        unit.card.kind == CardKind.LEGION_UNIT -> state.wounds ?: maxHp
        else -> state.hull
    }
    val defeated = state.defeated == true
    val activated = state.activated == true
    val deployZone = state.deployZone
    val isWounded = remaining != null && maxHp != null && remaining < maxHp
    Surface(onClick = onOpen, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), color = if (defeated) Color(0xFF2A1A1A) else Color(0xFF1A2330)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text(unit.card.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${totalPts} pts • ${unit.card.legionRank?.name ?: ""}", color = Color(0xFF8F9BAD), fontSize = 10.sp, maxLines = 1)
                // Jauge de vie uniquement si l'unité est blessée (sinon bruit visuel en déploiement).
                if (isWounded) {
                    LinearProgressIndicator(progress = { (remaining!!.toFloat() / (maxHp ?: 1)).coerceIn(0f, 1f) }, modifier = Modifier.width(48.dp).height(6.dp).clip(RoundedCornerShape(3.dp)), color = if ((remaining ?: 0) <= (maxHp ?: 1) / 2) Color(0xFFFF6B6B) else Color(0xFF77D9A7), trackColor = Color(0xFF2A3A4A))
                }
            }
            if (isWounded) { Text("$remaining/${maxHp ?: "?"}", color = if ((remaining ?: 0) <= (maxHp ?: 1) / 2) Color(0xFFFF6B6B) else Color(0xFF9EACBC), fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            if (activated) { Icon(Icons.Filled.CheckCircle, contentDescription = "Activée", tint = Color(0xFF77D9A7), modifier = Modifier.size(16.dp)) }
            if (defeated) { Text("Vaincue", color = Color(0xFFFF6B6B), fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            if (isPrep && onCycleDeploy != null) {
                Surface(onClick = onCycleDeploy, shape = RoundedCornerShape(6.dp), color = when (deployZone) { 1 -> Color(0xFF1E3A2A); 2 -> Color(0xFF2A3A4A); else -> Color(0xFF3A3A4A) }) {
                    Text(if (deployZone == null) "Zone ?" else "Zone $deployZone", Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = if (deployZone == null) Color(0xFF9EACBC) else Color(0xFFFFC857), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}



// ── Déploiement : LES 2 ARMÉES côte à côte, marquage Zone 1/2 par unité (plus de vue "une seule armée") ──
@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun DeploymentSheet(
    myUnits: List<ListEntry>, oppUnits: List<ListEntry>,
    myStates: Map<String, SyncedUnitState>, oppStates: Map<String, SyncedUnitState>,
    allMy: List<ListEntry>, allOpp: List<ListEntry>,
    showOpp: Boolean,
    onCycleMy: (String) -> Unit, onCycleOpp: (String) -> Unit,
    onOpenMy: (ListEntry) -> Unit, onOpenOpp: (ListEntry) -> Unit,
    onClose: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onClose, containerColor = Color(0xFF111827)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Déploiement — Zone 1 / Zone 2", color = Color(0xFFFFC857), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Tapez le bouton Zone de chaque unité pour l'assigner à une zone de déploiement", color = Color(0xFF8F9BAD), fontSize = 11.sp)
            // MOI
            Text("MOI", color = Color(0xFFF59E0B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            myUnits.forEach { unit ->
                val st = myStates[unit.instanceId] ?: SyncedUnitState(unit.instanceId)
                UnitListRow(unit, st, allMy.filter { it.parentInstanceId == unit.instanceId }, isPrep = true, readOnly = false,
                    onOpen = { onOpenMy(unit) }, onCycleDeploy = { onCycleMy(unit.instanceId) }, onCompare = {})
            }
            if (myUnits.isEmpty()) Text("Aucune unité à déployer", color = Color(0xFF5A6A7A), fontSize = 11.sp)
            Spacer(Modifier.height(4.dp))
            if (showOpp) {
                Text("ADVERSAIRE", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                oppUnits.forEach { unit ->
                    val st = oppStates[unit.instanceId] ?: SyncedUnitState(unit.instanceId)
                    UnitListRow(unit, st, allOpp.filter { it.parentInstanceId == unit.instanceId }, isPrep = true, readOnly = false,
                        onOpen = { onOpenOpp(unit) }, onCycleDeploy = { onCycleOpp(unit.instanceId) }, onCompare = {})
                }
                if (oppUnits.isEmpty()) Text("Aucune armée adverse chargée (choisissez-la dans le setup)", color = Color(0xFF5A6A7A), fontSize = 11.sp)
            }
        }
    }
}

// ── Grille d'activation (P1) : miniatures d'unités pour choisir une activation ──
@Composable
internal fun UnitActivationGrid(
    units: List<ListEntry>,
    allEntries: List<ListEntry>,
    states: Map<String, SyncedUnitState>,
    readOnly: Boolean,
    onActivate: (String) -> Unit,
    onOpen: (ListEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    // Tri : activables en tête, activées puis vaincues en queue.
    val sorted = remember(units, states) {
        units.sortedBy { u ->
            val st = states[u.instanceId]
            when {
                st?.defeated == true -> 2
                st?.activated == true -> 1
                else -> 0
            }
        }
    }
    LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(sorted, key = { it.instanceId }) { unit ->
            val st = states[unit.instanceId] ?: SyncedUnitState(unit.instanceId)
            val children = allEntries.filter { it.parentInstanceId == unit.instanceId }
            UnitActivationCell(unit, st, children, readOnly, onActivate, onOpen)
        }
    }
}

// ── Cellule d'activation : miniature + nom + jauge coque + état (rouge=activée, gris=vaincue) ──
@Composable
internal fun UnitActivationCell(
    unit: ListEntry, state: SyncedUnitState, children: List<ListEntry>,
    readOnly: Boolean, onActivate: (String) -> Unit, onOpen: (ListEntry) -> Unit
) {
    val totalPts = unit.card.points + children.sumOf { it.card.points * it.quantity }
    val maxHp = when (unit.card.kind) {
        CardKind.LEGION_UNIT -> {
            val st = remember(unit.instanceId) { LegionStatsParser.parse(unit.card.legionStats) }
            (st?.health?.takeIf { it > 0 } ?: 1) * (st?.miniCount?.takeIf { it > 0 } ?: 1)
        }
        else -> null
    }
    val remaining = when {
        unit.card.kind == CardKind.LEGION_UNIT -> state.wounds ?: maxHp
        else -> state.hull
    }
    val defeated = state.defeated == true
    val activated = state.activated == true
    val activable = !defeated && !activated && !readOnly
    val artPath = unit.card.imagePath()
    Card(
        onClick = { if (activable) onActivate(unit.instanceId) else onOpen(unit) },
        modifier = Modifier.fillMaxWidth().aspectRatio(0.7f).border(if (activated) 2.dp else if (defeated) 1.dp else 0.dp, if (activated) Color(0xFFFF6B6B) else if (defeated) Color(0xFF3A3A4A) else Color.Transparent, RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2330))
    ) {
        Box(Modifier.fillMaxSize()) {
            if (artPath != null) {
                AsyncImage(model = "file:///android_asset/$artPath", contentDescription = unit.card.name,
                    modifier = Modifier.fillMaxSize().alpha(if (defeated) 0.3f else if (activated) 0.6f else 1f),
                    contentScale = ContentScale.Fit)
            } else {
                Box(Modifier.fillMaxSize().background(Color(0xFF2A3A4A)), contentAlignment = Alignment.Center) { Text(unit.card.name, color = Color.White, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(4.dp)) }
            }
            // Jauge coque en bas.
            if (remaining != null && maxHp != null) {
                LinearProgressIndicator(progress = { (remaining.toFloat() / maxHp).coerceIn(0f, 1f) }, modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(4.dp), color = if (remaining <= maxHp / 2) Color(0xFFFF6B6B) else Color(0xFF77D9A7), trackColor = Color(0xFF2A3A4A))
            }
            // Nom en surimpression bas.
            Text(unit.card.name, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color(0xAA0A0E15)).padding(horizontal = 4.dp, vertical = 2.dp))
            // Badge état en haut-droit (symbole + couleur pour daltonisme).
            if (activated) {
                Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFEF5350), modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) { Text("✓", Modifier.padding(horizontal = 5.dp, vertical = 2.dp), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            } else if (defeated) {
                Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF3A3A4A), modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) { Text("✕", Modifier.padding(horizontal = 5.dp, vertical = 2.dp), color = Color(0xFF9EACBC), fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            } else if (activable) {
                Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF2E7D32), modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) { Text("●", Modifier.padding(horizontal = 5.dp, vertical = 2.dp), color = Color(0xFF66BB6A), fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}


// ── Bandeau horizontal d'activation (aperçu d'état) : miniatures rouge=activée, vert=activable, gris=vaincue ──
@Composable
internal fun UnitActivationStrip(
    units: List<ListEntry>,
    states: Map<String, SyncedUnitState>,
    onOpen: (ListEntry) -> Unit
) {
    LazyRow(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(units, key = { it.instanceId }) { unit ->
            val st = states[unit.instanceId] ?: SyncedUnitState(unit.instanceId)
            val defeated = st.defeated == true
            val activated = st.activated == true
            val artPath = unit.card.imagePath()
            val borderColor = when {
                activated -> Color(0xFFEF5350)
                defeated -> Color(0xFF3A3A4A)
                else -> Color(0xFF66BB6A)
            }
            Box(Modifier.width(64.dp).aspectRatio(0.7f).clip(RoundedCornerShape(8.dp)).border(2.dp, borderColor, RoundedCornerShape(8.dp)).clickable { onOpen(unit) }) {
                if (artPath != null) {
                    AsyncImage(model = "file:///android_asset/$artPath", contentDescription = unit.card.name,
                        modifier = Modifier.fillMaxSize().alpha(if (defeated) 0.3f else if (activated) 0.6f else 1f),
                        contentScale = ContentScale.Fit)
                } else {
                    Box(Modifier.fillMaxSize().background(Color(0xFF2A3A4A)), contentAlignment = Alignment.Center) { Text(unit.card.name, color = Color.White, fontSize = 8.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(2.dp)) }
                }
                // Symbole d'état (daltonisme) : ✓ activée, ✕ vaincue, ● activable.
                Text(if (activated) "✓" else if (defeated) "✕" else "●", color = borderColor, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.TopEnd).padding(2.dp).background(Color(0xAA0A0E15), RoundedCornerShape(4.dp)).padding(horizontal = 3.dp, vertical = 1.dp))
            }
        }
    }
}


// ── Écran central "champ de bataille" (façon Pokemon TCG) : adversaire haut, commandement centre, moi bas ──
@Composable
internal fun BattlefieldView(
    myUnits: List<ListEntry>, oppUnits: List<ListEntry>,
    myStates: Map<String, SyncedUnitState>, oppStates: Map<String, SyncedUnitState>,
    myCmd: LegionCommandCard?, oppCmd: LegionCommandCard?,
    turn: String,
    onOpenUnit: (ListEntry) -> Unit,
    onActivate: (String) -> Unit,
    onCmdClick: () -> Unit,
    showOpponent: Boolean = true,
    onOppActivate: ((String) -> Unit)? = null,
    onWound: ((String, Int) -> Unit)? = null,
    onToken: ((String, String, Int) -> Unit)? = null,
    onOrder: ((String) -> Unit)? = null, onCover: ((String) -> Unit)? = null,
    round: Int = 1, vpA: Int = 0, vpB: Int = 0, activeObjectives: Set<String> = emptySet(),
    objectives: List<LegionBattleCard> = emptyList(),
    onRound: (Int) -> Unit = {}, onVP: (String, Int) -> Unit = { _, _ -> },
    onToggleObjective: (String) -> Unit = {}, onNextRound: () -> Unit = {},
    onJournal: () -> Unit = {}
) {
    Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B)))).padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // (ScoreRoundBar retiré : le MissionDashboard affiche déjà round/VP en tête d'écran de jeu)
        if (showOpponent) CampRow("ADVERSAIRE", oppUnits, oppStates, isActive = turn == "opp", onOpenUnit, onActivate = onOppActivate, onWound = onWound, onToken = onToken, onOrder = onOrder, onCover = onCover)
        CommandCenterRow(myCmd, oppCmd, turn, onCmdClick, activeObjectives = activeObjectives)
        CampRow("MOI", myUnits, myStates, isActive = turn == "own", onOpenUnit, onActivate, onWound = onWound, onToken = onToken, onOrder = onOrder, onCover = onCover)
    }
}


// ── Bandeau score/round/objectifs (F5) ──
@Composable
internal fun ScoreRoundBar(
    round: Int, vpA: Int, vpB: Int, activeObjectives: Set<String>,
    objectives: List<LegionBattleCard>,
    onRound: (Int) -> Unit, onVP: (String, Int) -> Unit, onToggleObjective: (String) -> Unit,
    onNextRound: () -> Unit, onJournal: () -> Unit = {}
) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = Color(0xFF111827)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("ROUND", color = Color(0xFF94A3B8), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Surface(onClick = { onRound(round + 1) }, shape = RoundedCornerShape(6.dp), color = Color(0xFF1E293B)) { Text("$round", Modifier.padding(horizontal = 10.dp, vertical = 4.dp), color = Color(0xFFF59E0B), fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("VP", color = Color(0xFF94A3B8), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Surface(onClick = { onVP("A", -1) }, shape = RoundedCornerShape(4.dp), color = Color(0xFF1E293B)) { Text("−", Modifier.padding(horizontal = 5.dp, vertical = 2.dp), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                Text("$vpA", color = Color(0xFFF59E0B), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Surface(onClick = { onVP("A", 1) }, shape = RoundedCornerShape(4.dp), color = Color(0xFF1E293B)) { Text("+", Modifier.padding(horizontal = 5.dp, vertical = 2.dp), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                Text("—", color = Color(0xFF5A6A7A), fontSize = 12.sp)
                Surface(onClick = { onVP("B", -1) }, shape = RoundedCornerShape(4.dp), color = Color(0xFF1E293B)) { Text("−", Modifier.padding(horizontal = 5.dp, vertical = 2.dp), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                Text("$vpB", color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Surface(onClick = { onVP("B", 1) }, shape = RoundedCornerShape(4.dp), color = Color(0xFF1E293B)) { Text("+", Modifier.padding(horizontal = 5.dp, vertical = 2.dp), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(onClick = onJournal, shape = RoundedCornerShape(8.dp), color = Color(0xFF1E293B)) {
                    Text("📓", Modifier.padding(horizontal = 8.dp, vertical = 5.dp), fontSize = 11.sp)
                }
                Surface(onClick = onNextRound, shape = RoundedCornerShape(8.dp), color = Color(0xFF1E3A2A)) {
                    Text("⚡ Round+", Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = Color(0xFF77D9A7), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (objectives.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 10.dp, end = 10.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                objectives.forEach { obj ->
                    val active = obj.name in activeObjectives
                    Surface(onClick = { onToggleObjective(obj.name) }, shape = RoundedCornerShape(6.dp), color = if (active) Color(0xFF1E3A2A) else Color(0xFF1A2330)) {
                        Text(if (active) "● ${obj.name}" else "○ ${obj.name}", Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = if (active) Color(0xFF77D9A7) else Color(0xFF8F9BAD), fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}


// ── Panneau guidé : choisir la carte de commandement du round ──
@Composable
internal fun CommandSelectionPanel(
    hand: List<LegionCommandCard>, selected: String?,
    onSelect: (String) -> Unit, onConfirm: () -> Unit
) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = Color(0xFF111827)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("1. CARTE DE COMMANDEMENT DU ROUND", color = Color(0xFFF59E0B), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("choisissez votre carte", color = Color(0xFF94A3B8), fontSize = 10.sp)
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(hand, key = { it.id }) { card ->
                    val isSel = card.id == selected
                    Surface(onClick = { onSelect(card.id) }, shape = RoundedCornerShape(10.dp), color = if (isSel) Color(0xFF3A2E12) else Color(0xFF1E293B), border = if (isSel) BorderStroke(2.dp, Color(0xFFF59E0B)) else BorderStroke(1.dp, Color(0xFF2A3A4A))) {
                        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("PIP ${card.pip}", color = Color(0xFFF59E0B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(card.name, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            if (selected != null) {
                Surface(onClick = onConfirm, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), color = Color(0xFF1E3A2A)) {
                    Text("✓ Jouer cette carte — Passer à l'activation", Modifier.padding(vertical = 12.dp), color = Color(0xFF77D9A7), fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

// ── Rangée d'un camp : miniatures d'unités, bordure colorée si camp actif ──
@Composable
@OptIn(ExperimentalFoundationApi::class)
internal fun CampRow(
    label: String, units: List<ListEntry>, states: Map<String, SyncedUnitState>,
    isActive: Boolean, onOpenUnit: (ListEntry) -> Unit, onActivate: ((String) -> Unit)?,
    onWound: ((String, Int) -> Unit)? = null, onToken: ((String, String, Int) -> Unit)? = null,
    onOrder: ((String) -> Unit)? = null, onCover: ((String) -> Unit)? = null
) {
    val campColor = if (label == "MOI") Color(0xFFF59E0B) else Color(0xFF38BDF8)
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = if (isActive) Color(0xFF1A2330) else Color(0xFF111827), border = BorderStroke(if (isActive) 2.dp else 1.dp, if (isActive) campColor else Color(0xFF2A3A4A))) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = if (isActive) campColor else Color(0xFF9EACBC), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                val nonDefeated = units.count { u -> states[u.instanceId]?.defeated != true }
                val activated = units.count { u -> states[u.instanceId]?.activated == true }
                Text("$activated/$nonDefeated activées", color = if (activated >= nonDefeated && nonDefeated > 0) Color(0xFF77D9A7) else Color(0xFF8F9BAD), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            if (units.isEmpty()) {
                Text("Aucune unité", color = Color(0xFF5A6A7A), fontSize = 10.sp)
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(units, key = { it.instanceId }) { unit ->
                        val st = states[unit.instanceId] ?: SyncedUnitState(unit.instanceId)
                        val defeated = st.defeated == true
                        val activated = st.activated == true
                        val activable = !defeated && !activated && onActivate != null && isActive
                        val artPath = unit.card.imagePath()
                        val panicking = (st.suppressions ?: 0) >= (st.panicThreshold ?: Int.MAX_VALUE) && (st.suppressions ?: 0) > 0
                        val campColor = if (label == "MOI") Color(0xFFF59E0B) else Color(0xFF38BDF8)
                        val stateColor = when {
                            panicking -> Color(0xFFFF5252)
                            activated -> Color(0xFFEF5350)
                            defeated -> Color(0xFF3A3A4A)
                            else -> campColor
                        }
                        val scale = animateFloatAsState(if (activable) 1.06f else 1f, label = "tileScale")
                        Column(Modifier.width(64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.width(64.dp).aspectRatio(0.7f).clip(RoundedCornerShape(6.dp))
                                .background(if (defeated) Color(0xFF1A2330) else Color(0xFF1E293B))
                                .border(2.dp, stateColor, RoundedCornerShape(6.dp))
                                .graphicsLayer { this.scaleX = scale.value; this.scaleY = scale.value; this.alpha = if (defeated) 0.35f else if (activated) 0.6f else 1f }
                                .combinedClickable(onClick = { if (activable) onActivate?.invoke(unit.instanceId) else onOpenUnit(unit) }, onLongClick = { onOpenUnit(unit) }),
                                contentAlignment = Alignment.Center) {
                                if (artPath != null) {
                                    AsyncImage(model = "file:///android_asset/$artPath", contentDescription = unit.card.name,
                                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                                } else {
                                    Text(unit.card.name, color = Color.White, fontSize = 7.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(2.dp))
                                }
                                Text(if (activated) "✓" else if (defeated) "✕" else "●", color = stateColor, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.align(Alignment.TopEnd).padding(2.dp).background(Color(0xAA0A0E15), RoundedCornerShape(4.dp)).padding(horizontal = 3.dp, vertical = 1.dp))
                                if (panicking) {
                                    Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFB71C1C), modifier = Modifier.align(Alignment.TopCenter).padding(2.dp)) {
                                        Text("PANIQUE", Modifier.padding(horizontal = 3.dp, vertical = 1.dp), color = Color.White, fontSize = 6.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (onWound != null && st.wounds != null) {
                                    Surface(shape = RoundedCornerShape(4.dp), color = Color(0xCC0A0E15), modifier = Modifier.align(Alignment.BottomCenter).padding(2.dp).clickable { onWound(unit.instanceId, -1) }) {
                                        Text("${st.wounds}/${st.eliminatedThreshold ?: "?"}", Modifier.padding(horizontal = 3.dp, vertical = 1.dp), color = if (st.wounds == 0) Color(0xFFFF6B6B) else campColor, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (onCover != null) {
                                    Surface(shape = RoundedCornerShape(4.dp), color = if ((st.cover ?: 0) > 0) Color(0xFF1E3A2A) else Color(0xCC0A0E15), modifier = Modifier.align(Alignment.BottomEnd).padding(2.dp).clickable { onCover(unit.instanceId) }) {
                                        Text("🛡${st.cover ?: 0}", Modifier.padding(horizontal = 3.dp, vertical = 1.dp), color = if ((st.cover ?: 0) > 0) Color(0xFF77D9A7) else Color(0xFF8F9BAD), fontSize = 7.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (st.orderReceived == true || onOrder != null) {
                                    Surface(shape = RoundedCornerShape(4.dp), color = if (st.orderReceived == true) Color(0xFF1E3A2A) else Color(0xCC0A0E15), modifier = Modifier.align(Alignment.TopStart).padding(2.dp).clickable(enabled = onOrder != null) { onOrder?.invoke(unit.instanceId) }) {
                                        Text(if (st.orderReceived == true) "📨" else "＋", Modifier.padding(horizontal = 3.dp, vertical = 1.dp), fontSize = 7.sp)
                                    }
                                }
                            }
                            Text(unit.card.name, color = if (defeated) Color(0xFF5A6A7A) else Color(0xFFE2E8F0), fontSize = 7.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(64.dp))
                            if (onToken != null) {
                                Row(Modifier.padding(top = 1.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    listOf("aim" to "◎", "dodge" to "◈", "surge" to "⚡", "supp" to "✚", "stdby" to "◉").forEach { (tk, sym) ->
                                        val count = when (tk) {
                                            "aim" -> st.tokens.count { it == "aim" }
                                            "dodge" -> st.tokens.count { it == "dodge" }
                                            "surge" -> st.tokens.count { it == "surge" }
                                            "supp" -> st.suppressions ?: 0
                                            "stdby" -> st.tokens.count { it == "standby" }
                                            else -> 0
                                        }
                                        if (count > 0 || tk == "supp") {
                                            Surface(shape = RoundedCornerShape(3.dp), color = if (tk == "supp" && count > 0) Color(0xFFB71C1C) else Color(0xFF2A3A4A), modifier = Modifier.combinedClickable(onClick = { onToken(unit.instanceId, tk, 1) }, onLongClick = { onToken(unit.instanceId, tk, -1) })) {
                                                Text("$sym$count", Modifier.padding(horizontal = 3.dp, vertical = 1.dp), color = if (tk == "supp" && count > 0) Color.White else Color(0xFF9EACBC), fontSize = 6.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Centre : cartes de commandement des 2 camps + qui joue ──
@Composable
internal fun CommandCenterRow(myCmd: LegionCommandCard?, oppCmd: LegionCommandCard?, turn: String, onCmdClick: () -> Unit, activeObjectives: Set<String> = emptySet()) {
    val myPrio = (myCmd?.pip?.toIntOrNull() ?: 9) <= (oppCmd?.pip?.toIntOrNull() ?: 9)
    val turnColor = if (turn == "own") Color(0xFFF59E0B) else Color(0xFF38BDF8)
    // EV3 : halo pulsant sur la zone centrale pour montrer qui joue (100% présentation, aucun état).
    val pulse = rememberInfiniteTransition(label = "turnPulse")
    val glowAlpha by pulse.animateFloat(initialValue = 0.25f, targetValue = 0.6f, animationSpec = infiniteRepeatable(animation = tween(900), repeatMode = RepeatMode.Reverse), label = "glow")
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = Color(0xFF111827), border = BorderStroke(1.dp, turnColor.copy(alpha = glowAlpha))) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            // Ma carte (chip, tap → ouvrir le deck)
            Column(horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Surface(onClick = onCmdClick, shape = RoundedCornerShape(8.dp), color = Color(0xFF1E293B)) {
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Box(Modifier.size(18.dp).clip(CircleShape).background(if (myCmd != null) Color(0xFFF59E0B) else Color(0xFF2A3A4A)), contentAlignment = Alignment.Center) {
                            Text(if (myCmd != null) "★" else "?", color = Color(0xFF0A0E15), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(if (myCmd != null) myCmd.name else "—", color = Color(0xFFE2E8F0), fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Text("PIP ${myCmd?.pip ?: "—"}${if (myCmd != null && oppCmd != null && myPrio) " • ⚑" else ""}", color = Color(0xFFF59E0B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                // Objectifs actifs en jetons (EV4 : zone centrale = no man's land).
                if (activeObjectives.isNotEmpty()) {
                    activeObjectives.forEach { name ->
                        Surface(shape = CircleShape, color = Color(0xFF1E3A2A), border = BorderStroke(1.dp, Color(0xFF77D9A7))) {
                            Text("●", Modifier.padding(6.dp), color = Color(0xFF77D9A7), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (turn == "own") "▶ À VOUS" else "⏳ TOUR ADV", color = turnColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            // Carte adverse (dos, tap → suivre la commande adverse)
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Surface(onClick = onCmdClick, shape = RoundedCornerShape(8.dp), color = Color(0xFF1E293B)) {
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(if (oppCmd != null) "?" else "—", color = Color(0xFFE2E8F0), fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Box(Modifier.size(18.dp).clip(CircleShape).background(if (oppCmd != null) Color(0xFF38BDF8) else Color(0xFF2A3A4A)), contentAlignment = Alignment.Center) {
                            Text(if (oppCmd != null) "★" else "?", color = Color(0xFF0A0E15), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text("PIP ${oppCmd?.pip ?: "—"}${if (oppCmd != null && myCmd != null && !myPrio) " • ⚑" else ""}", color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Contenu d'une page unité (réutilisé par le détail et la vue comparée) ──
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun UnitPageContent(
    unit: ListEntry, children: List<ListEntry>, allEntries: List<ListEntry>,
    wikiSections: List<WikiSectionEntity>, onRuleClick: (WikiSectionEntity) -> Unit,
    state: SyncedUnitState, onChange: (SyncedUnitState) -> Unit, readOnly: Boolean
) {
    val unitWikiSections = remember(unit, wikiSections) { wikiSections.filter { it.gameSystem == unit.card.gameSystem.name } }
    when (unit.card.kind) {
        CardKind.ARMADA_SHIP -> ArmadaShipPage(unit, children, allEntries, unitWikiSections, onRuleClick, state = state, onChange = onChange, readOnly = readOnly)
        CardKind.ARMADA_SQUADRON -> ArmadaSquadronPage(unit, unitWikiSections, onRuleClick, state = state, onChange = onChange, readOnly = readOnly)
        CardKind.COMMANDER -> CommanderPage(unit, unitWikiSections, onRuleClick)
        else -> LegionUnitPage(unit, children, allEntries, unitWikiSections, onRuleClick, state = state, onChange = onChange, readOnly = readOnly)
    }
}

// ── Détail d'unité (A8) : sheet plein écran réutilisant la page unité ──
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UnitDetailSheet(
    unit: ListEntry, children: List<ListEntry>, allEntries: List<ListEntry>,
    wikiSections: List<WikiSectionEntity>, onRuleClick: (WikiSectionEntity) -> Unit,
    state: SyncedUnitState, onChange: (SyncedUnitState) -> Unit, readOnly: Boolean,
    onDice: () -> Unit, onCompare: () -> Unit, onClose: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onClose, containerColor = Color(0xFF0A0E15)) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(unit.card.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Surface(onClick = onDice, shape = RoundedCornerShape(8.dp), color = Color(0xFF2A3A4A)) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFFFC857), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.dice), color = Color(0xFFFFC857), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.width(6.dp))
                Surface(onClick = onCompare, shape = RoundedCornerShape(8.dp), color = Color(0xFF2A3A4A)) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("⚔", color = Color(0xFFFFC857), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(4.dp))
                        Text("Comparer", color = Color(0xFFFFC857), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Box(Modifier.fillMaxWidth().heightIn(max = 900.dp)) {
                UnitPageContent(unit, children, allEntries, wikiSections, onRuleClick, state, onChange, readOnly)
            }
        }
    }
}

// ── Vue comparée 2 unités (A2) : résolution de combat ──
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UnitCompareSheet(
    a: ListEntry, b: ListEntry, allEntries: List<ListEntry>,
    states: Map<String, SyncedUnitState>, wikiSections: List<WikiSectionEntity>,
    onRuleClick: (WikiSectionEntity) -> Unit,
    onChange: (String, SyncedUnitState) -> Unit, readOnly: Boolean,
    onClose: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onClose, containerColor = Color(0xFF0A0E15)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(bottom = 16.dp)) {
            Text("Comparer", color = Color(0xFFFFC857), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(a, b).forEach { unit ->
                    val st = states[unit.instanceId] ?: SyncedUnitState(unit.instanceId)
                    val children = allEntries.filter { it.parentInstanceId == unit.instanceId }
                    Box(Modifier.weight(1f).heightIn(max = 700.dp)) {
                        UnitPageContent(unit, children, allEntries, wikiSections, onRuleClick, st, { next -> onChange(unit.instanceId, next) }, readOnly)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LegionUnitPage(unit: ListEntry, children: List<ListEntry>, allEntries: List<ListEntry>, wikiSections: List<WikiSectionEntity> = emptyList(), onRuleClick: (WikiSectionEntity) -> Unit = {}, state: SyncedUnitState = SyncedUnitState(unit.instanceId), onChange: (SyncedUnitState) -> Unit = {}, readOnly: Boolean = false) {
    val totalPts = unit.card.points + children.sumOf { it.card.points * it.quantity }
    val stats = remember(unit.instanceId) { LegionStatsParser.parse(unit.card.legionStats) }
    // Points de vie réels de l'unité = santé par figurine × nombre de figurines.
    // Tracker de VALEUR RESTANTE : démarre plein, descend sous les dégâts (comme la coque Armada).
    val maxHp = (stats?.health?.takeIf { it > 0 } ?: 1) * (stats?.miniCount?.takeIf { it > 0 } ?: 1)
    val wounds = state.wounds ?: maxHp
    val tokens = state.tokens
    // Courage de l'unité (depuis legionStats) : pilote les états Supprimé (> courage) / Paniqué (>= 2x courage).
    val courage = stats?.courage?.takeIf { it > 0 } ?: 1
    val suppressions = state.suppressions ?: 0
    val isWounded = wounds < maxHp
    val isSuppressed = suppressions >= courage
    val isPanicked = suppressions >= courage * 2
    val activeCrits = state.activeCrits.mapNotNull { name -> legionCrits.firstOrNull { it.name == name } }
    var showCard by remember(unit.instanceId) { mutableStateOf(false) }
    val usedUpgrades = state.usedUpgrades
    val defColor = if ((stats?.defenseDie ?: "w") == "r") Color(0xFFFF6B6B) else Color.White
    // Écriture dans le store (no-op en lecture seule = armée adverse).
    val set: (SyncedUnitState) -> Unit = { if (!readOnly) onChange(it) }

    Column(Modifier.fillMaxSize().background(Color(0xFF0A0E15)).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (readOnly) {
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), color = Color(0xFF3B2224)) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = Color(0xFFFF6B6B), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.read_only), color = Color(0xFFFFC7B7), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        CardBlock(unit, children, totalPts, wikiSections, onRuleClick, onCardClick = { showCard = true })
        // ── stats réelles de la carte (repliable) ──
        CollapsibleSection(stringResource(R.string.profile), initiallyExpanded = false) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                StatChip(stringResource(R.string.stat_health), "${stats?.miniCount ?: 1} × ${stats?.health ?: 1}", Color(0xFFFF6B6B))
                StatChip(stringResource(R.string.stat_courage), "${stats?.courage ?: 1}", Color(0xFFFFC857))
                StatChip(stringResource(R.string.stat_speed), "${stats?.speed ?: 1}", Color(0xFF4FC3F7))
                StatChip(stringResource(R.string.stat_defense), (stats?.defenseDie ?: "w").uppercase(), defColor)
            }
            if (!stats?.surgeAttack.isNullOrBlank() || !stats?.surgeDefense.isNullOrBlank()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.surge_attack, surgeLabel(stats?.surgeAttack)), color = Color(0xFFB4BFCE), style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(R.string.surge_defense, surgeLabel(stats?.surgeDefense)), color = Color(0xFFB4BFCE), style = MaterialTheme.typography.bodySmall)
                }
            }
            if (stats?.keywords?.isNotEmpty() == true) {
                            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                stats.keywords.forEach { kw ->
                                    val section = findWikiSection(wikiSections, kw)
                                    Surface(
                                        onClick = { if (section != null) onRuleClick(section) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF2A3A4A)
                                    ) {
                                        Text(com.legionforge.app.data.i18n.I18n.keyword(kw), Modifier.padding(horizontal = 10.dp, vertical = 4.dp), color = if (section != null) Color(0xFFFFC857) else Color(0xFF77D9A7), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }           }
                                }
                                                        }
                                        }
                                        // ── suivi des blessures (valeur restante) (repliable) ──
        CollapsibleSection(stringResource(R.string.tracking), initiallyExpanded = true) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { BigCounter(stringResource(R.string.minis), wounds, maxHp, Color(0xFFFF6B6B), { if (wounds < maxHp) set(state.copy(wounds = wounds + 1)) }, { if (wounds > 0) set(state.copy(wounds = wounds - 1)) }) }
            HealthBar(wounds.toFloat() / maxHp, wounds, maxHp)
            // ── état de l'unité (auto-calculé depuis blessures + suppression / courage) ──
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StateChip(stringResource(R.string.legion_state_wounded), isWounded, Color(0xFFFFC857))
                StateChip(stringResource(R.string.legion_state_suppressed), isSuppressed, Color(0xFFFFB74D))
                StateChip(stringResource(R.string.legion_state_panicked), isPanicked, Color(0xFFFF6B6B))
            }
            // Activation de l'unité (drapeau 'activée' dans le round courant).
            val activated = state.activated ?: false
            Surface(onClick = { set(state.copy(activated = !activated)) }, shape = RoundedCornerShape(10.dp), color = if (activated) Color(0xFF1E3A2A) else Color(0xFF2A3A4A), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (activated) Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF77D9A7), modifier = Modifier.size(18.dp)) else Box(Modifier.size(18.dp).border(2.dp, Color(0xFF5A6A7A), CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(if (activated) R.string.legion_activated else R.string.legion_not_activated), color = if (activated) Color(0xFF77D9A7) else Color(0xFF9EACBC), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            // Unité vaincue (retirée du jeu).
            val defeated = state.defeated ?: false
            Surface(onClick = { set(state.copy(defeated = !defeated)) }, shape = RoundedCornerShape(10.dp), color = if (defeated) Color(0xFF3B2224) else Color(0xFF2A3A4A), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (defeated) Icons.Filled.Close else Icons.Filled.Check, contentDescription = null, tint = if (defeated) Color(0xFFFF6B6B) else Color(0xFF5A6A7A), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(if (defeated) R.string.legion_defeated else R.string.legion_active), color = if (defeated) Color(0xFFFFC7B7) else Color(0xFF9EACBC), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            // Promotion de commandant (règle v2 : quand tous les commandants sont vaincus).
            val promoted = state.promoted ?: false
            Surface(onClick = { set(state.copy(promoted = !promoted)) }, shape = RoundedCornerShape(10.dp), color = if (promoted) Color(0xFF1E3A2A) else Color(0xFF2A3A4A), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = if (promoted) Color(0xFFFFC857) else Color(0xFF5A6A7A), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(if (promoted) R.string.legion_promoted else R.string.legion_promote), color = if (promoted) Color(0xFFFFC857) else Color(0xFF9EACBC), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            // Compteur de suppression : alimente l'état Supprimé (>= courage) / Paniqué (>= 2x courage).
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.legion_suppression_label), color = Color(0xFF9EACBC), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(onClick = { if (suppressions > 0) set(state.copy(suppressions = suppressions - 1)) }, shape = CircleShape, color = Color(0xFF2A3A4A), modifier = Modifier.size(44.dp)) { Box(contentAlignment = Alignment.Center) { Text("-", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) } }
                    Text("$suppressions / $courage", color = if (isPanicked) Color(0xFFFF6B6B) else if (isSuppressed) Color(0xFFFFB74D) else Color(0xFF77D9A7), fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Surface(onClick = { set(state.copy(suppressions = suppressions + 1)) }, shape = CircleShape, color = Color(0xFF2A3A4A), modifier = Modifier.size(44.dp)) { Box(contentAlignment = Alignment.Center) { Text("+", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) } }
                }
            }
            // Couverture de l'unité (0/1/2) — état de couverture en défense.
            val cover = state.cover ?: 0
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.legion_cover), color = Color(0xFF9EACBC), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Stepper("$cover", { if (cover > 0) set(state.copy(cover = cover - 1)) }, { if (cover < 2) set(state.copy(cover = cover + 1)) }, valueColor = if (cover > 0) Color(0xFF77D9A7) else Color(0xFF5A6A7A), valueWidth = 30.dp)
            }
            // Dégâts de véhicule (immobilisé, arme détruite) — véhicules uniquement.
            if (unit.card.cardSubtype?.contains("vehicle") == true) {
                val vd = state.vehicleDamage
                Text(stringResource(R.string.vehicle_damage), color = Color(0xFF9EACBC), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("immobilized" to stringResource(R.string.vd_immobilized), "weapon" to stringResource(R.string.vd_weapon)).forEach { (key, label) ->
                        val active = key in vd
                        Surface(onClick = { set(state.copy(vehicleDamage = if (active) vd - key else vd + key)) }, shape = RoundedCornerShape(8.dp), color = if (active) Color(0xFF3B2224) else Color(0xFF2A3A4A)) {
                            Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (active) Icons.Filled.Close else Icons.Filled.Check, contentDescription = null, tint = if (active) Color(0xFFFF6B6B) else Color(0xFF5A6A7A), modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(label, color = if (active) Color(0xFFFFC7B7) else Color(0xFF9EACBC), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            // Armes de l'unité (range + dés)
            if (stats?.weapons?.isNotEmpty() == true) {
                HorizontalDivider(color = Color(0xFF2A3A4A))
                Text(stringResource(R.string.weapons), color = Color(0xFF9EACBC), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                stats.weapons.forEach { w ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(w.name, color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.range_label, w.rangeMin, w.rangeMax), color = Color(0xFF9EACBC), style = MaterialTheme.typography.labelSmall)
                        }
                        Text(diceText(w.red, w.black, w.white), color = Color(0xFF77D9A7), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
            TokenSection(tokens, { set(state.copy(tokens = tokens + it)) }, { set(state.copy(tokens = tokens - it)) }, isVehicle = unit.card.cardSubtype?.contains("vehicle") == true)
        }
        CollapsibleSection(stringResource(R.string.active_effects), initiallyExpanded = false) {
            EffectsPanel(
                effects = buildList {
                    children.forEach { c -> add(ActiveEffect(EffectType.UPGRADE, c.card.displayName(), upgradeEffectText(c.card) ?: stringResource(R.string.upgrade_installed), c.instanceId, slot = c.card.upgradeSlots.firstOrNull(), pts = c.card.points, imageAssetPath = c.card.imagePath()).copy(used = usedUpgrades.contains(c.instanceId))) }
                    activeCrits.forEach { c -> add(ActiveEffect(EffectType.CRIT, c.name, c.effect, c.name)) }
                },
                onRemoveCrit = { crit -> set(state.copy(activeCrits = state.activeCrits - crit.name)) },
                                allCrits = legionCrits,
                                onToggleUsed = { eff ->
                                    if (eff.type == EffectType.UPGRADE) {
                                        set(state.copy(usedUpgrades = if (eff.id in usedUpgrades) usedUpgrades - eff.id else usedUpgrades + eff.id))
                                    }
                                }
                            )
        }
        CardZoomOverlay(unit.card, showCard) { showCard = false }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
internal fun StatChip(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color(0xFF9EACBC), fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}
@Composable
internal fun surgeLabel(s: String?): String = when (s) { "h" -> stringResource(R.string.surge_crit); "a" -> stringResource(R.string.surge_hit); "b" -> stringResource(R.string.surge_block); "r" -> stringResource(R.string.surge_counter); else -> "—" }
internal fun diceText(red: Int, black: Int, white: Int): String {
    val parts = buildList { if (red > 0) add("R$red"); if (black > 0) add("N$black"); if (white > 0) add("B$white") }
    return if (parts.isEmpty()) "—" else parts.joinToString(" ")
}

// // ═══════════════════  ARMADA SHIP  ═══════════════════════
// Stats embarqués depuis le catalogue BSData ("fleet builder") dans card.shipStats (JSON).
internal data class ArmadaStats(
    val hull: Int = 0,
    val shieldFront: Int = 0,
    val shieldRear: Int = 0,
    val shieldPort: Int = 0,
    val shieldStarboard: Int = 0,
    val shieldPortAux: Int = 0,      // vaisseaux huge : 2e cadran bâbord
    val shieldStarboardAux: Int = 0, // vaisseaux huge : 2e cadran tribord
    val maxSpeed: Int = 1,
    val speed: Int = 3, // valeur fixe des squadrons
    val command: Int = 1, // niveau de commande = stock max de pions d'ordre
    val squadron: Int = 0,
    val engineering: Int = 0,
    val attackFront: List<Int> = emptyList(), // [rouge, bleu, noir]
    val attackRear: List<Int> = emptyList(),
    val attackPort: List<Int> = emptyList(),
    val attackStarboard: List<Int> = emptyList(),
    val attackPortAux: List<Int> = emptyList(),      // huge : dés bâbord auxiliaire
    val attackStarboardAux: List<Int> = emptyList(), // huge : dés tribord auxiliaire
    val antiSquadron: List<Int> = emptyList(), // [rouge, bleu, noir]
    val battery: List<Int> = emptyList(),      // [rouge, bleu, noir]
    val keywords: List<String> = emptyList(),
    val defenseTokens: List<String> = emptyList(),
    val speedChart: List<Map<String, Int>> = emptyList(), // liste de positions, chaque position = {vitesse: nb de clics}
    val size: String = "small" // huge = 2 cadrans de bouclier par côté (Executor, Starhawk)
)

internal object ArmadaStatsParser {
    fun parse(shipStats: String?, kind: CardKind): ArmadaStats? {
        if (shipStats.isNullOrBlank()) return null
        return try {
            val o = org.json.JSONObject(shipStats)
            val shield = o.optJSONObject("shield")
            val tokArr = o.optJSONArray("defenseTokens")
            val tokens = buildList {
                if (tokArr != null) for (i in 0 until tokArr.length()) add(tokArr.getString(i))
            }
            when (kind) {
                CardKind.ARMADA_SHIP -> {
                    val attack = o.optJSONObject("attack")
                    fun arc(name: String): List<Int> {
                        val a = attack?.optJSONArray(name) ?: return emptyList()
                        return buildList { for (i in 0 until a.length()) add(a.optInt(i)) }
                    }
                    val scArr = o.optJSONArray("speedChart")
                    val speedChart = buildList {
                        if (scArr != null) for (i in 0 until scArr.length()) {
                            val pos = scArr.getJSONObject(i)
                            val vals = pos.optJSONObject("values")
                            val m = buildMap {
                                if (vals != null) {
                                    val it = vals.keys()
                                    while (it.hasNext()) { val k = it.next(); put(k, vals.optInt(k)) }
                                }
                            }
                            add(m)
                        }
                    }
                    ArmadaStats(
                        hull = o.optInt("hull"),
                        shieldFront = shield?.optInt("front", 0) ?: 0,
                        shieldRear = shield?.optInt("rear", 0) ?: 0,
                        shieldPort = shield?.optInt("left", 0) ?: 0,
                        shieldStarboard = shield?.optInt("right", 0) ?: 0,
                        shieldPortAux = shield?.optInt("left_aux", 0) ?: 0,
                        shieldStarboardAux = shield?.optInt("right_aux", 0) ?: 0,
                        maxSpeed = o.optInt("maxSpeed", 1).coerceAtLeast(1),
                        command = o.optInt("command", 1).coerceAtLeast(1),
                        squadron = o.optInt("squadron", 0),
                        engineering = o.optInt("engineering", 0),
                        attackFront = arc("front"),
                        attackRear = arc("rear"),
                        attackPort = arc("left"),
                        attackStarboard = arc("right"),
                        attackPortAux = arc("left_aux"),
                        attackStarboardAux = arc("right_aux"),
                        defenseTokens = tokens,
                        speedChart = speedChart,
                        size = o.optString("size", "small")
                    )
                }
                CardKind.ARMADA_SQUADRON -> {
                    val kwArr = o.optJSONArray("keywords")
                    val keywords = buildList { if (kwArr != null) for (i in 0 until kwArr.length()) add(kwArr.getString(i)) }
                    fun diceArr(name: String): List<Int> {
                        val a = o.optJSONArray(name) ?: return emptyList()
                        return buildList { for (i in 0 until a.length()) add(a.optInt(i)) }
                    }
                    ArmadaStats(
                        hull = o.optInt("hull"),
                        speed = o.optInt("speed", 3).coerceAtLeast(1),
                        antiSquadron = diceArr("antiSquadron"),
                        battery = diceArr("battery"),
                        defenseTokens = tokens,
                        keywords = keywords
                    )
                }
                else -> null
            }
        } catch (_: Exception) { null }
    }
}

// Matrice de manœuvres Armada : triangulaire (X = vitesse 1..maxSpeed, Y = position).
// Position 1 EN BAS, position max EN HAUT (inversé). Valeur : I = 1, II = 2, - = 0. Cases vides = bordeaux.
@Composable
private fun ManeuverMatrix(stats: ArmadaStats?, selectedSpeed: Int, onSelectSpeed: (Int) -> Unit) {
    if (stats?.speedChart?.isNotEmpty() != true) return
    val maxSpeed = stats.maxSpeed
    val maxPos = stats.speedChart.size.coerceAtLeast(1)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // En-tête : vitesses (X).
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Spacer(Modifier.width(30.dp)) // coin vide (label position)
            (1..maxSpeed).forEach { v ->
                val isSel = v == selectedSpeed
                Surface(
                    onClick = { onSelectSpeed(if (selectedSpeed == v) 0 else v) },
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSel) Color(0xFFFFC857) else Color(0xFF2A3A4A),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(Modifier.padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
                        Text("V$v", color = if (isSel) Color(0xFF0A0E15) else Color(0xFFFFC857), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        // Lignes : positions (Y), inversées (max en haut, 1 en bas).
        stats.speedChart.reversed().forEachIndexed { revIdx, posMap ->
            val posLabel = maxPos - revIdx
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(30.dp), contentAlignment = Alignment.Center) {
                    Text("$posLabel", color = Color(0xFFFFC857), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                (1..maxSpeed).forEach { v ->
                    val n = posMap[v.toString()] ?: -1 // -1 = case vide (hors triangle)
                    val cellColor = if (n < 0) Color(0xFF800000) else Color.White
                    val cellText = when { n < 0 -> ""; n == 0 -> "-"; n == 1 -> "I"; n == 2 -> "II"; else -> "III" }
                    val isSel = v == selectedSpeed
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = cellColor,
                        modifier = Modifier.weight(1f).height(32.dp),
                        border = if (isSel) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFFC857)) else null
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(cellText, color = if (n < 0) Color(0xFF800000) else Color(0xFF1B2B4B), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ArmadaShipPage(unit: ListEntry, children: List<ListEntry>, allEntries: List<ListEntry>, wikiSections: List<WikiSectionEntity> = emptyList(), onRuleClick: (WikiSectionEntity) -> Unit = {}, state: SyncedUnitState = SyncedUnitState(unit.instanceId), onChange: (SyncedUnitState) -> Unit = {}, readOnly: Boolean = false) {
    val totalPts = unit.card.points + children.sumOf { it.card.points * it.quantity }
    val stats = remember(unit.instanceId) { ArmadaStatsParser.parse(unit.card.shipStats, CardKind.ARMADA_SHIP) }
    // Coque + boucliers initialisés aux valeurs de base de la carte ; speed démarre à 2.
    // La coque est un tracker de valeur restante (démarre au hull de base, descend sous les dégâts).
    val maxHp = stats?.hull?.takeIf { it > 0 } ?: 15
    val hull = state.hull ?: maxHp
    // Boucliers stockés dans state.shields : [F,P,S,R] (+ [P2,S2] si huge).
    val shields = state.shields
    val sF = shields.getOrElse(0) { stats?.shieldFront ?: 0 }
    val sP = shields.getOrElse(1) { stats?.shieldPort ?: 0 }
    val sS = shields.getOrElse(2) { stats?.shieldStarboard ?: 0 }
    val sR = shields.getOrElse(3) { stats?.shieldRear ?: 0 }
    // Vaisseaux "huge" (Executor, Starhawk) : 2 cadrans de bouclier par flanc (haut + bas).
    val isHuge = stats?.size == "huge"
    val sP2 = shields.getOrElse(4) { if (isHuge) (stats?.shieldPortAux ?: 0) else 0 }
    val sS2 = shields.getOrElse(5) { if (isHuge) (stats?.shieldStarboardAux ?: 0) else 0 }
    val maxShield = 9
    val maxSpeed = stats?.maxSpeed ?: 3
    val speed = state.speed ?: 2
    // Colonne de vitesse sélectionnée dans la matrice de manoeuvres (défaut = 2, vitesse de départ des vaisseaux).
    val selectedSpeed = speed
    val activeCrits = state.activeCrits.mapNotNull { name -> armadaCrits.firstOrNull { it.name == name } }
    var showCritSelector by remember(unit.instanceId) { mutableStateOf(false) }
    var showCard by remember(unit.instanceId) { mutableStateOf(false) }
    val usedUpgrades = state.usedUpgrades
    val defTokenNames = remember(unit.instanceId) {
        val fromStats = stats?.defenseTokens.orEmpty()
        if (fromStats.isNotEmpty()) fromStats
        else ArmadaDefenseToken.entries.take(4).map { it.name }
    }
    val defTokenStates = defTokenNames.mapNotNull { name ->
        val def = ArmadaDefenseToken.entries.firstOrNull { it.name == name.uppercase() }
        if (def == null) null else def to name
    }
    // Chaque jeton (même en doublon) est une instance indépendante, clé = "name_i".
    val defTokens = state.defTokens
    // Roue de commandement : ordre courant du vaisseau (null = non défini).
    val commandOrder = state.commandOrder?.let { name -> ArmadaCommandOrder.entries.firstOrNull { it.name == name } }
    // Pions d'ordre en stock : chaque commande a un compteur (0..N), total max = niveau de commande.
    val maxOrderStock = stats?.command ?: 1
    val orderTokens = state.orderTokens
    val commander = allEntries.firstOrNull { it.card.kind == CardKind.COMMANDER || (it.card.kind == CardKind.ARMADA_UPGRADE && ArmadaSlot.COMMANDER in it.card.upgradeSlots) }
    // Écriture dans le store (no-op en lecture seule = armée adverse).
    val set: (SyncedUnitState) -> Unit = { if (!readOnly) onChange(it) }
    fun setShield(i: Int, v: Int) {
        val arr = shields.toMutableList()
        while (arr.size <= i) arr.add(0)
        arr[i] = v
        set(state.copy(shields = arr))
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF0A0E15)).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // ── ship card (zone du haut compacte, reprise de la vue condensée) ──
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CardArtwork(unit.card, Modifier.width(52.dp).clickable { showCard = true })
                    Column(Modifier.weight(1f)) { Text(unit.card.displayName(), color = Color.White, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis); Text("${stringResource(R.string.kind_ship)}  •  ${unit.card.factionId.replace('-', ' ').replaceFirstChar { it.uppercase() }}", color = Color(0xFFFFC857), style = MaterialTheme.typography.labelSmall) }
                    if (isHuge) Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF3A2050)) { Text("HUGE", Modifier.padding(horizontal = 6.dp, vertical = 1.dp), color = Color(0xFFD7A6FF), fontSize = 9.sp, fontWeight = FontWeight.Bold) }
                    Text("${unit.card.points}", color = Color(0xFFFFC857), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                if (stats != null) ShipStatsBlock(stats)
                else if (!unit.card.rulesText.isNullOrBlank()) ClickableRulesText(unit.card.rulesText, wikiSections, onRuleClick, color = Color(0xFFB4BFCE), style = MaterialTheme.typography.bodySmall, maxLines = 2)
                // ── résumé coque (avec +/-) + vitesse + bouton +DGT CRIT (toujours visible) ──
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    HullCounter(hull, maxHp, { if (hull < maxHp) set(state.copy(hull = hull + 1)) }, { if (hull > 0) set(state.copy(hull = hull - 1)) })
                    StatChip(stringResource(R.string.stat_speed), speed, Color(0xFF77D9A7), modifier = Modifier.weight(1f), display = "$speed/$maxSpeed")
                    // Bouton +DGT CRIT : ajoute une carte de dégât critique (toujours visible).
                    Surface(onClick = { showCritSelector = true }, shape = RoundedCornerShape(10.dp), color = Color(0xFF5A2020)) {
                        Text(stringResource(R.string.add_crit), Modifier.padding(horizontal = 10.dp, vertical = 8.dp), color = Color(0xFFFF6B6B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                // Sélecteur de dégât critique (ouvert par le bouton +DGT CRIT).
                CritSelectorDropdown(armadaCrits, showCritSelector, { showCritSelector = false }, { c -> set(state.copy(activeCrits = state.activeCrits + c.name)); showCritSelector = false })
            }
        }
                // ── matrice de manœuvres (repliable, repliée par défaut) ; pilote la vitesse ──
                CollapsibleSection(stringResource(R.string.maneuver_matrix), initiallyExpanded = false) {
                    if (stats?.speedChart?.isNotEmpty() == true) {
                        ManeuverMatrix(stats, selectedSpeed, { v ->
                            set(state.copy(speed = if (speed == v) 0 else v))
                        })
                    } else {
                        Surface(shape = RoundedCornerShape(9.dp), color = Color(0xFF1F2C3D), modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(stringResource(R.string.cadrans), color = Color(0xFF9EACBC), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.weight(1f))
                                Text("🛡 ${sF}/${sP}/${sS}/${sR}", color = Color(0xFF4FC3F7), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text("⚄ ${(stats?.attackFront?.sum() ?: 0)}", color = Color(0xFFFF6B6B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                // ── command dial (roue de commandement) (en haut, repliable) ──
                CollapsibleSection(stringResource(R.string.command_wheel), initiallyExpanded = false) {
                    CommandDialCard(
                        selected = commandOrder,
                        onSelect = { it -> set(state.copy(commandOrder = if (commandOrder?.name == it.name) null else it.name)) },
                        wikiSections = wikiSections,
                        onRuleClick = onRuleClick
                    )
                }
                // ── pions d'ordre (stock) (repliable) ──
                CollapsibleSection(stringResource(R.string.order_tokens), initiallyExpanded = false) {
                    OrderTokensCard(
                        orderTokens = orderTokens,
                        maxStock = maxOrderStock,
                        onIncOrder = { name ->
                            if (orderTokens.values.sum() < maxOrderStock) set(state.copy(orderTokens = orderTokens + (name to (orderTokens[name] ?: 0) + 1)))
                        },
                        onDecOrder = { name -> set(state.copy(orderTokens = orderTokens + (name to ((orderTokens[name] ?: 0) - 1).coerceAtLeast(0)))) },
                        wikiSections = wikiSections,
                        onRuleClick = onRuleClick
                    )
                }
                // ── defense tokens (repliable) ──
                CollapsibleSection(stringResource(R.string.defense_tokens), initiallyExpanded = false) {
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        defTokenStates.forEachIndexed { i, (def, _) ->
                            val key = "${def.name}_$i"
                            val used = defTokens[key] ?: false
                            DefenseTokenDisc(def, used, onClick = { set(state.copy(defTokens = defTokens + (key to !used))) }, onWikiClick = { findWikiSection(wikiSections, def.label)?.let(onRuleClick) })
                        }
                    }
                }
        // ── effects panel (améliorations remontées au-dessus de la roue de commandement) (repliable) ──
        CollapsibleSection(stringResource(R.string.active_effects), initiallyExpanded = false) {
            EffectsPanel(
                effects = buildList {
                    if (commander != null) add(ActiveEffect(EffectType.COMMANDER, commander.card.displayName(), stripBracketName(commander.card.rulesText ?: stringResource(R.string.fleet_commander)), "cmd_${commander.instanceId}", pts = commander.card.points, imageAssetPath = commander.card.imagePath())
                        .copy(used = usedUpgrades.contains("cmd")))
                    children.forEach { c ->
                        if (c.card.kind == CardKind.COMMANDER || (c.card.kind == CardKind.ARMADA_UPGRADE && ArmadaSlot.COMMANDER in c.card.upgradeSlots)) return@forEach
                        add(ActiveEffect(EffectType.UPGRADE, c.card.displayName(), stripBracketName(c.card.rulesText ?: stringResource(R.string.upgrade_installed)), c.instanceId, slot = c.card.upgradeSlots.firstOrNull(), pts = c.card.points, imageAssetPath = c.card.imagePath())
                            .copy(used = usedUpgrades.contains(c.instanceId)))
                    }
                    activeCrits.forEach { c -> add(ActiveEffect(EffectType.CRIT, c.name, c.effect, "crit_${c.name}")) }
                },
                onRemoveCrit = { crit -> set(state.copy(activeCrits = state.activeCrits - crit.name)) },
                allCrits = armadaCrits
            )
        }
        // ── cadrans (boucliers & coque) + dés d'attaque par côté (repliable) ──
        CollapsibleSection(stringResource(R.string.shields_hull), initiallyExpanded = false) {
                // ── cadrans des 4 arcs en croix (comme sur la carte officielle) : rectangle de dés + cercle de bouclier ──
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // AVANT : rectangle de dés AU-DESSUS du cercle de bouclier.
                    ArcCadran(ArcPos.ABOVE, listOf(stats?.attackFront), listOf(sF), { if (sF < maxShield) setShield(0, sF + 1) }, { if (sF > 0) setShield(0, sF - 1) }, { if (sF < maxShield) setShield(0, sF + 1) }, { if (sF > 0) setShield(0, sF - 1) })
                    // Ligne centrale : BAB (gauche) | TRIB (droite). Vaisseaux huge = 2 cadrans par flanc.
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Top) {
                        ArcCadran(ArcPos.LEFT,
                            if (isHuge) listOf(stats?.attackPort, stats?.attackPortAux) else listOf(stats?.attackPort),
                            if (isHuge) listOf(sP, sP2) else listOf(sP),
                            { if (sP < maxShield) setShield(1, sP + 1) }, { if (sP > 0) setShield(1, sP - 1) },
                            { if (sP2 < maxShield) setShield(4, sP2 + 1) }, { if (sP2 > 0) setShield(4, sP2 - 1) })
                        ArcCadran(ArcPos.RIGHT,
                            if (isHuge) listOf(stats?.attackStarboard, stats?.attackStarboardAux) else listOf(stats?.attackStarboard),
                            if (isHuge) listOf(sS, sS2) else listOf(sS),
                            { if (sS < maxShield) setShield(2, sS + 1) }, { if (sS > 0) setShield(2, sS - 1) },
                            { if (sS2 < maxShield) setShield(5, sS2 + 1) }, { if (sS2 > 0) setShield(5, sS2 - 1) })
                    }
                    // ARRIERE : rectangle de dés EN DESSOUS du cercle de bouclier.
                    ArcCadran(ArcPos.BELOW, listOf(stats?.attackRear), listOf(sR), { if (sR < maxShield) setShield(3, sR + 1) }, { if (sR > 0) setShield(3, sR - 1) }, { if (sR < maxShield) setShield(3, sR + 1) }, { if (sR > 0) setShield(3, sR - 1) })
                }
                HorizontalDivider(color = Color(0xFF2A3A4A))
                                // Résumé coque/vitesse déplacé en haut (header ship card) ; gros compteurs rouges retirés pour compacter.
                                // La matrice de manœuvres est désormais dans le header (remplace la ligne "Cadrans").
                        }
                        CardZoomOverlay(unit.card, showCard) { showCard = false }
        Spacer(Modifier.height(20.dp))
    }
}
// ═══════════════════  COMMANDER (pas de degats critiques, equipe sur le flagship)  ═══════════════════
@Composable
internal fun CommanderPage(unit: ListEntry, wikiSections: List<WikiSectionEntity> = emptyList(), onRuleClick: (WikiSectionEntity) -> Unit = {}) {
    var showCard by remember(unit.instanceId) { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(Color(0xFF0A0E15)).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CardBlock(unit, emptyList(), unit.card.points, wikiSections, onRuleClick, onCardClick = { showCard = true })
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.fleet_command), color = Color(0xFFFFC857), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
        }
        CardZoomOverlay(unit.card, showCard) { showCard = false }
        Spacer(Modifier.height(20.dp))
    }
}

// ═══════════════════  SQUADRON  ═══════════════════════════
@Composable
internal fun ArmadaSquadronPage(unit: ListEntry, wikiSections: List<WikiSectionEntity> = emptyList(), onRuleClick: (WikiSectionEntity) -> Unit = {}, state: SyncedUnitState = SyncedUnitState(unit.instanceId), onChange: (SyncedUnitState) -> Unit = {}, readOnly: Boolean = false) {
    var showCard by remember(unit.instanceId) { mutableStateOf(false) }
    val stats = remember(unit.instanceId) { ArmadaStatsParser.parse(unit.card.shipStats, CardKind.ARMADA_SQUADRON) }
    val maxHp = stats?.hull?.takeIf { it > 0 } ?: 8
    val hull = state.hull ?: maxHp
    val activated = state.activated ?: false
    // Jetons de défense des escadrons uniques (ex: "2 Brace", "Brace, Scatter").
    val defTokenNames = remember(unit.instanceId) { stats?.defenseTokens.orEmpty() }
    val defTokenStates = defTokenNames.mapNotNull { name ->
        val def = ArmadaDefenseToken.entries.firstOrNull { it.name == name.uppercase() }
        if (def == null) null else def to name
    }
    val defTokens = state.defTokens
    // Écriture dans le store (no-op en lecture seule = armée adverse).
    val set: (SyncedUnitState) -> Unit = { if (!readOnly) onChange(it) }
    Column(Modifier.fillMaxSize().background(Color(0xFF0A0E15)).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CardBlock(unit, emptyList(), unit.card.points, wikiSections, onRuleClick, onCardClick = { showCard = true })
        CollapsibleSection(stringResource(R.string.squadron_tracking), initiallyExpanded = false) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                BigCounter(stringResource(R.string.hull), hull, maxHp, Color(0xFFFF6B6B), { if (hull < maxHp) set(state.copy(hull = hull + 1)) }, { if (hull > 0) set(state.copy(hull = hull - 1)) })
                Spacer(Modifier.width(20.dp))
                // Jeton d'activation/désactivation de l'escadron (à la place du coût, redondant avec le cadre du haut).
                SquadronActivationToken(
                    activated = activated,
                    onToggle = { set(state.copy(activated = !activated)) },
                    onWikiClick = { findWikiSection(wikiSections, "activation")?.let(onRuleClick) }
                )
            }
            if (hull < maxHp) HealthBar(hull.toFloat() / maxHp, hull, maxHp)
            if (stats?.keywords?.isNotEmpty() == true) {
                HorizontalDivider(color = Color(0xFF2A3A4A))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    stats.keywords.forEach { kw ->
                        val section = findWikiSection(wikiSections, kw)
                        Surface(
                            onClick = { if (section != null) onRuleClick(section) },
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF2A3A4A)
                        ) {
                            Text(kw, Modifier.padding(horizontal = 10.dp, vertical = 4.dp), color = if (section != null) Color(0xFFFFC857) else Color(0xFF77D9A7), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        if (defTokenStates.isNotEmpty()) {
            CollapsibleSection(stringResource(R.string.defense_tokens), initiallyExpanded = false) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    defTokenStates.forEachIndexed { i, (def, _) ->
                        val key = "${def.name}_$i"
                        val used = defTokens[key] ?: false
                        DefenseTokenDisc(def, used, onClick = { set(state.copy(defTokens = defTokens + (key to !used))) }, onWikiClick = { findWikiSection(wikiSections, def.label)?.let(onRuleClick) })
                    }
                }
            }
        }
        CardZoomOverlay(unit.card, showCard) { showCard = false }
        Spacer(Modifier.height(20.dp))
    }
}

// ═══════════════════  SHARED COMPONENTS  ═══════════════════

// Overlay zoom : la vraie carte en grand, au-dessus du contenu, ouverte au clic sur la miniature.
@Composable
internal fun CardZoomOverlay(card: CardDefinition, show: Boolean, onDismiss: () -> Unit) {
    if (!show) return
    val source: Any? = card.imagePath()?.let { "file:///android_asset/$it" } ?: card.imageUrl
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        androidx.compose.material3.Surface(
            modifier = Modifier.fillMaxWidth(0.96f).verticalScroll(rememberScrollState()),
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF192330)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(card.displayName(), color = Color(0xFFFFC857), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                if (source != null) {
                    AsyncImage(model = source, contentDescription = stringResource(R.string.icon_desc, card.displayName()),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 640.dp), contentScale = ContentScale.Fit)
                }
                Surface(onClick = onDismiss, shape = RoundedCornerShape(8.dp), color = Color(0xFF2A3A4A)) {
                    Text("✕", Modifier.padding(horizontal = 14.dp, vertical = 6.dp), color = Color(0xFF9EACBC), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
internal fun CardBlock(unit: ListEntry, children: List<ListEntry>, totalPts: Int, wikiSections: List<WikiSectionEntity> = emptyList(), onRuleClick: (WikiSectionEntity) -> Unit = {}, onCardClick: (() -> Unit)? = null) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                if (onCardClick != null) CardArtwork(unit.card, Modifier.width(96.dp).clickable { onCardClick() })
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(unit.card.displayName(), color = factionColor(unit.card.factionId), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val kind = unit.card.legionRank?.name?.replace('_', ' ')?.lowercase()?.replaceFirstChar { it.uppercase() } ?: unit.card.kind.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
                    Text("$kind  •  ${unit.card.factionId.replace('-', ' ').replaceFirstChar { it.uppercase() }}", color = Color(0xFFFFC857), style = MaterialTheme.typography.labelLarge)
                }
                Text("${unit.card.points} pts", color = Color(0xFFFFC857), style = MaterialTheme.typography.titleLarge)
            }
            if (!unit.card.rulesText.isNullOrBlank()) ClickableRulesText(unit.card.rulesText, wikiSections, onRuleClick, color = Color(0xFFB4BFCE), style = MaterialTheme.typography.bodySmall, maxLines = 6)
            if (children.isNotEmpty()) {
                HorizontalDivider(color = Color(0xFF2A3A4A), modifier = Modifier.padding(vertical = 4.dp))
                children.forEach { c -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Row(modifier = Modifier.weight(1f)) { Text("+ ", color = Color(0xFF77D9A7), fontWeight = FontWeight.Bold); Text(c.card.displayName(), color = Color.White, style = MaterialTheme.typography.bodyMedium) }; Text("${c.card.points}", color = Color(0xFFFFC857), style = MaterialTheme.typography.labelMedium) } }
                HorizontalDivider(color = Color(0xFF2A3A4A))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { Text(stringResource(R.string.total_pts, totalPts), color = Color(0xFFFFC857), fontWeight = FontWeight.Bold) }
            }
        }
    }
}

// Section repliable : en-tete cliquable (titre + chevron) + contenu masque/deplie.
// Permet de compacter le mode partie sur un ecran de telephone : les infos secondaires
// (roue de commandement, effets, profil) sont repliees par defaut, le suivi principal reste visible.
@Composable
internal fun CollapsibleSection(
    title: String,
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))) {
        Column {
            Row(
                Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = Color(0xFFFFC857), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Icon(if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, contentDescription = if (expanded) stringResource(R.string.collapse) else stringResource(R.string.expand), tint = Color(0xFF9EACBC), modifier = Modifier.size(18.dp))
            }
            if (expanded) {
                HorizontalDivider(color = Color(0xFF2A3A4A))
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
            }
        }
    }
}

@Composable
internal fun EffectsPanel(
    effects: List<ActiveEffect>,
    onRemoveCrit: (CritCard) -> Unit,
    allCrits: List<CritCard>,
    onToggleUsed: (ActiveEffect) -> Unit = {}
) {
    var selectedEffect by remember { mutableStateOf<ActiveEffect?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Effect list
        if (effects.isEmpty()) {
            Text(stringResource(R.string.no_effects), color = Color(0xFF5A6A7A), style = MaterialTheme.typography.bodySmall)
        } else {
            effects.forEach { eff ->
                val (icon, iconColor) = when {
                    eff.type == EffectType.UPGRADE -> if (eff.slot != null) slotBadge(eff.slot) else ("U" to Color(0xFF4FC3F7))
                    eff.type == EffectType.COMMANDER -> "C" to Color(0xFFFFC857)
                    eff.type == EffectType.CRIT -> "\u26A1" to Color(0xFFFF6B6B)
                    else -> "?" to Color.Gray
                }
                Surface(
                                    onClick = { onToggleUsed(eff) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = when { eff.used -> Color(0xFF1A1A2A); eff.type == EffectType.CRIT -> Color(0xFF3B2224); eff.type == EffectType.ABILITY -> Color(0xFF1E3A2A); else -> Color(0xFF1E2A3A) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                    Column(Modifier.padding(8.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(24.dp).clip(CircleShape).background(if (eff.used) Color(0xFF3A3A4A) else Color(0xFF1E2A3A)), contentAlignment = Alignment.Center) {
                                    val iconPath = when {
                                        eff.type == EffectType.UPGRADE && eff.slot != null -> slotIconPath(eff.slot)
                                        eff.type == EffectType.COMMANDER -> "icons/upg_commander.webp"
                                        else -> null
                                    }
                                    if (iconPath != null) {
                                        AsyncImage(
                                            model = "file:///android_asset/$iconPath",
                                            contentDescription = eff.label,
                                            modifier = Modifier.size(18.dp),
                                            colorFilter = ColorFilter.tint(if (eff.used) Color(0xFF5A6A7A) else iconColor)
                                        )
                                    } else {
                                        val fallbackIcon = when (eff.type) {
                                            EffectType.UPGRADE -> Icons.Filled.Build
                                            EffectType.COMMANDER -> Icons.Filled.Star
                                            EffectType.CRIT -> Icons.Filled.Warning
                                            else -> Icons.Filled.Info
                                        }
                                        Icon(fallbackIcon, contentDescription = eff.label, tint = if (eff.used) Color(0xFF5A6A7A) else iconColor, modifier = Modifier.size(16.dp))
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(eff.label, color = if (eff.used) Color(0xFF5A6A7A) else Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = if (eff.type == EffectType.CRIT) FontWeight.Bold else FontWeight.Normal)
                                        if (eff.used) { Spacer(Modifier.width(6.dp)); Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF3A3A4A)) { Text(stringResource(R.string.activated), Modifier.padding(horizontal = 5.dp, vertical = 2.dp), color = Color(0xFF5A6A7A), fontSize = 8.sp, fontWeight = FontWeight.Bold) } }
                                    }
                                    Text(
                                        when (eff.type) { EffectType.COMMANDER -> stringResource(R.string.effect_commander); EffectType.UPGRADE -> stringResource(R.string.effect_upgrade); EffectType.CRIT -> stringResource(R.string.effect_crit); EffectType.ABILITY -> stringResource(R.string.effect_ability) },
                                        color = (if (eff.used) Color(0xFF5A6A7A) else iconColor).copy(alpha = 0.6f), fontSize = 10.sp
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (eff.pts > 0) Text("${eff.pts} pts", color = Color(0xFFFFC857), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                                Surface(onClick = { selectedEffect = if (selectedEffect == eff) null else eff }, shape = RoundedCornerShape(6.dp), color = Color(0xFF2A3A4A)) {
                                                                    Icon(if (selectedEffect == eff) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = Color(0xFF9EACBC), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).size(16.dp))
                                                                }
                            }
                        }
                        // Expanded description
                                                if (selectedEffect == eff) {
                                                    Spacer(Modifier.height(8.dp))
                                                    if (eff.imageAssetPath != null) {
                                                        val effSource = "file:///android_asset/${eff.imageAssetPath}"
                                                        Surface(Modifier.fillMaxWidth().heightIn(max = 420.dp), shape = RoundedCornerShape(8.dp), color = Color(0xFF0A0E15)) {
                                                            AsyncImage(model = effSource, contentDescription = eff.label, modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp), contentScale = ContentScale.Fit)
                                                        }
                                                        if (eff.desc.isNotBlank()) {
                                                            Spacer(Modifier.height(6.dp))
                                                            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), color = Color(0xFF0A0E15)) {
                                                                Text(stripLeadingName(eff.desc, eff.label), Modifier.padding(12.dp), color = Color(0xFFB4BFCE), style = MaterialTheme.typography.bodySmall)
                                                            }
                                                        }
                                                    } else {
                                                        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), color = Color(0xFF0A0E15)) {
                                                            Text(stripLeadingName(eff.desc, eff.label), Modifier.padding(12.dp), color = Color(0xFFB4BFCE), style = MaterialTheme.typography.bodySmall)
                                                        }
                                                    }
                            if (eff.type == EffectType.CRIT) {
                                Spacer(Modifier.height(4.dp))
                                TextButton(onClick = { onRemoveCrit(allCrits.firstOrNull { it.name == eff.label } ?: return@TextButton) }) { Text(stringResource(R.string.remove_crit), color = Color(0xFFFF6B6B), style = MaterialTheme.typography.labelSmall) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun CritSelectorDropdown(crits: List<CritCard>, expanded: Boolean, onDismiss: () -> Unit, onSelect: (CritCard) -> Unit) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        crits.forEach { crit ->
            DropdownMenuItem(text = { Column { Text(crit.name, fontWeight = FontWeight.Bold, fontSize = 14.sp); Text(crit.effect.take(60)+"...", fontSize = 11.sp, color = Color.Gray) } }, onClick = { onSelect(crit) })
        }
    }
}

@Composable
internal fun HealthBar(ratio: Float, current: Int, max: Int) {
    val barColor = when { ratio > 0.66f -> Color(0xFF77D9A7); ratio > 0.33f -> Color(0xFFFFC857); else -> Color(0xFFFF6B6B) }
    Box(Modifier.fillMaxWidth().height(20.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF2A3A4A))) {
        Box(Modifier.fillMaxWidth(fraction = ratio.coerceIn(0f, 1f)).fillMaxHeight().background(barColor))
        Text("$current / $max", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
internal fun DefenseTokenDisc(def: ArmadaDefenseToken, used: Boolean, onClick: () -> Unit, onWikiClick: (() -> Unit)? = null) {
    val ringColor = if (used) Color(0xFFFF6B6B) else Color(0xFF77D9A7)
    // Les jetons de defense officiels (render fandom, 100x52) : vert = PRET, rouge = UTILISE.
    val tokenFile = "tokens/${def.name.lowercase()}_${if (used) "exhausted" else "ready"}.webp"
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Surface(
                onClick = onClick,
                shape = CircleShape,
                color = Color(0xFF192330),
                modifier = Modifier.size(52.dp),
                border = androidx.compose.foundation.BorderStroke(3.dp, ringColor)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(3.dp)) {
                    AsyncImage(
                        model = "file:///android_asset/$tokenFile",
                        contentDescription = def.label,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
        // Le libellé du jeton est cliquable vers le wiki (plus de bouton "i").
        Text(
            def.label,
            color = ringColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = if (onWikiClick != null) Modifier.clickable { onWikiClick() } else Modifier
        )
    }
}

// ── stats du vaisseau (au lieu du JSON brut) ─────────────
@Composable
internal fun ShipStatsBlock(stats: ArmadaStats) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StatChip(stringResource(R.string.stat_command), stats.command, Color(0xFFFFC857), modifier = Modifier.weight(1f))
            StatChip(stringResource(R.string.stat_squadron), stats.squadron, Color(0xFF4FC3F7), modifier = Modifier.weight(1f))
            StatChip(stringResource(R.string.stat_engineering), stats.engineering, Color(0xFF77D9A7), modifier = Modifier.weight(1f))
        }
    }
}

@Composable
internal fun StatChip(label: String, value: Int, color: Color, modifier: Modifier = Modifier, display: String? = null) {
    Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.15f), modifier = modifier) {
        Column(Modifier.padding(vertical = 6.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(display ?: "$value", color = color, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(label, color = color.copy(alpha = 0.8f), fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

// ── dés d'attaque : UN losange par dé (comme la carte officielle), sans chiffre ──
// Losange plein de couleur, un par dé de cette couleur.
@Composable
internal fun DiceDiamond(color: Color, size: Dp = 14.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width; val h = this.size.height
        val path = Path().apply {
            moveTo(w / 2f, 0f)          // pointe haut
            lineTo(w, h / 2f)           // pointe droite
            lineTo(w / 2f, h)           // pointe bas
            lineTo(0f, h / 2f)          // pointe gauche
            close()
        }
        drawPath(path, color = color)
    }
}

// Dés d'attaque d'un arc : UN losange par dé, ordre ROUGE, BLEU, NOIR.
// vertical=false (AV/ARR) : rangée horizontale, 2 lignes si >2 dés.
// vertical=true (BAB/TRIB) : 2 colonnes pour gagner de la largeur.
@Composable
internal fun AttackDiceRow(dice: List<Int>?, vertical: Boolean = false) {
    if (dice.isNullOrEmpty() || dice.all { it <= 0 }) return
    val red = dice.getOrNull(0) ?: 0
    val blue = dice.getOrNull(1) ?: 0
    val black = dice.getOrNull(2) ?: 0
    val total = blue + red + black
    // Ordre d'affichage : rouge, bleu, noir. Un losange par dé.
    val colors = listOf(Color(0xFFFF6B6B), Color(0xFF4FC3F7), Color(0xFF3A3A4A))
    val counts = listOf(red, blue, black)
    // Construit la liste des losanges (un par dé) dans l'ordre rouge/bleu/noir.
    val all = buildList { counts.forEachIndexed { i, c -> repeat(c) { add(colors[i]) } } }
    if (total <= 2) {
        // Une seule rangée.
        if (vertical) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                all.forEach { DiceDiamond(it) }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                all.forEach { DiceDiamond(it) }
            }
        }
    } else {
        // Répartir en 2 groupes équilibrés (moitié/moitié).
        val g1 = mutableListOf<Color>()
        val g2 = mutableListOf<Color>()
        counts.forEachIndexed { i, c ->
            val half = c / 2
            val rem = c - half
            repeat(half) { g1.add(colors[i]) }
            repeat(rem) { g2.add(colors[i]) }
        }
        if (vertical) {
            // 2 colonnes côte à côte (étroit).
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) { g1.forEach { DiceDiamond(it) } }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) { g2.forEach { DiceDiamond(it) } }
            }
        } else {
            // 2 lignes empilées.
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) { g1.forEach { DiceDiamond(it) } }
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) { g2.forEach { DiceDiamond(it) } }
            }
        }
    }
}

// ── cadrans des arcs (imité de la carte officielle) ──────────
// Position du rectangle de dés par rapport au cercle de bouclier.
internal enum class ArcPos { ABOVE, BELOW, LEFT, RIGHT }

// Cadran d'un arc : rectangle(s) de dés positionné(s) autour du/des cercle(s) de bouclier.
// diceList = liste des dés par cadran (1 pour normal, 2 pour huge). values = valeurs de bouclier par cadran.
// onInc1/onDec1 = 1er cadran, onInc2/onDec2 = 2e.
@Composable
internal fun ArcCadran(pos: ArcPos, diceList: List<List<Int>?>, values: List<Int>, onInc1: () -> Unit, onDec1: () -> Unit, onInc2: () -> Unit, onDec2: () -> Unit) {
    val isFlank = pos == ArcPos.LEFT || pos == ArcPos.RIGHT
    val rects: @Composable () -> Unit = {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            DiceRect(diceList.getOrNull(0), isFlank)
            if (diceList.size > 1) DiceRect(diceList.getOrNull(1), isFlank)
        }
    }
    val shields: @Composable () -> Unit = {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            CircleShield(values.getOrElse(0) { 0 }, onInc1, onDec1)
            if (values.size > 1) CircleShield(values.getOrElse(1) { 0 }, onInc2, onDec2)
        }
    }
    when (pos) {
        ArcPos.ABOVE -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) { rects(); shields() }
        ArcPos.BELOW -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) { shields(); rects() }
        ArcPos.LEFT -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { rects(); shields() }
        ArcPos.RIGHT -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { shields(); rects() }
    }
}

// Rectangle blanc contenant les dés d'attaque en losanges de couleur (comme le cadran de la carte).
@Composable
internal fun DiceRect(dice: List<Int>?, vertical: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFE8EDF5),   // fond blanc cassé du rectangle sur la carte
        modifier = Modifier.defaultMinSize(minWidth = if (vertical) 30.dp else 44.dp, minHeight = 30.dp)
    ) {
        Box(Modifier.padding(horizontal = 6.dp, vertical = 5.dp), contentAlignment = Alignment.Center) {
            if (dice.isNullOrEmpty() || dice.all { it <= 0 }) {
                Text("—", color = Color(0xFF9AA7B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            } else {
                AttackDiceRow(dice, vertical)
            }
        }
    }
}

// Cercle blanc à fine bordure bleue contenant la valeur de bouclier (comme sur la carte officielle).
@Composable
internal fun CircleShield(value: Int, onInc: () -> Unit, onDec: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Surface(onClick = onDec, shape = CircleShape, color = Color(0xFF2A3A4A), modifier = Modifier.size(44.dp)) { Box(contentAlignment = Alignment.Center) { Text("-", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) } }
            Surface(
                shape = CircleShape,
                color = Color.White,
                modifier = Modifier.size(44.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF4FC3F7))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("$value", color = Color(0xFF1B2B4B), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            Surface(onClick = onInc, shape = CircleShape, color = Color(0xFF2A3A4A), modifier = Modifier.size(44.dp)) { Box(contentAlignment = Alignment.Center) { Text("+", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) } }
        }
    }
}

// ── roue de commandement (dial) ──────────
@Composable
internal fun CommandDialCard(
    selected: ArmadaCommandOrder?,
    onSelect: (ArmadaCommandOrder) -> Unit,
    wikiSections: List<WikiSectionEntity> = emptyList(),
    onRuleClick: (WikiSectionEntity) -> Unit = {}
) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // N'affiche QUE l'ordre sélectionné (pastille compacte) ; sinon un selecteur compact par tap.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                if (selected != null) {
                    CommandDialButton(order = selected!!, selected = true, onClick = { onSelect(selected!!) }, modifier = Modifier.size(width = 96.dp, height = 84.dp))
                } else {
                    // Aucun ordre choisi : 4 pions cliquables pour sélectionner.
                    ArmadaCommandOrder.entries.forEach { order ->
                        CommandDialButton(order = order, selected = false, onClick = { onSelect(order) }, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// ── pions d'ordre (stock) ──────────
@Composable
internal fun OrderTokensCard(
    orderTokens: Map<String, Int>,
    maxStock: Int,
    onIncOrder: (String) -> Unit,
    onDecOrder: (String) -> Unit,
    wikiSections: List<WikiSectionEntity> = emptyList(),
    onRuleClick: (WikiSectionEntity) -> Unit = {}
) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                // Compteur de pions compact (le titre est déjà dans l'en-tête du menu repliable).
                Text(stringResource(R.string.orders_stock, orderTokens.values.sum(), maxStock), color = Color(0xFF77D9A7), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                ArmadaCommandOrder.entries.forEach { order ->
                    val count = orderTokens[order.name] ?: 0
                    OrderTokenDisc(
                        order = order,
                        count = count,
                        onWikiClick = { findWikiSection(wikiSections, order.label)?.let(onRuleClick) },
                        onInc = { onIncOrder(order.name) },
                        onDec = { onDecOrder(order.name) }
                    )
                }
            }
        }
    }
}

@Composable
internal fun CommandDialButton(order: ArmadaCommandOrder, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val ringColor = if (selected) Color(0xFFFFC857) else Color(0xFF2A3A4A)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = if (selected) Color(0xFF2A3A4A) else Color(0xFF192330),
            modifier = Modifier.size(64.dp),
            border = androidx.compose.foundation.BorderStroke(3.dp, ringColor)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(4.dp)) {
                AsyncImage(
                    model = "file:///android_asset/tokens/${order.iconFile}",
                    contentDescription = order.label,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }
        Text(order.label, color = if (selected) Color(0xFFFFC857) else Color(0xFF9EACBC), fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun OrderTokenDisc(order: ArmadaCommandOrder, count: Int, onWikiClick: () -> Unit, onInc: () -> Unit, onDec: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Surface(
            onClick = onWikiClick,
            shape = CircleShape,
            color = Color(0xFF192330),
            modifier = Modifier.size(52.dp),
            border = androidx.compose.foundation.BorderStroke(3.dp, Color(0xFF77D9A7))
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(3.dp)) {
                AsyncImage(
                    model = "file:///android_asset/tokens/${order.orderFile}",
                    contentDescription = order.label,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        }
        // Compteur de stock sous le pion (empilable, max total = niveau de commande).
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Surface(onClick = onDec, shape = CircleShape, color = Color(0xFF2A3A4A), modifier = Modifier.size(20.dp)) { Box(contentAlignment = Alignment.Center) { Text("-", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) } }
            Text("$count", color = Color(0xFF77D9A7), fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.width(20.dp))
            Surface(onClick = onInc, shape = CircleShape, color = Color(0xFF2A3A4A), modifier = Modifier.size(20.dp)) { Box(contentAlignment = Alignment.Center) { Text("+", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) } }
        }
        Text(order.label, color = Color(0xFF9EACBC), fontSize = 8.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

// Jeton d'activation d'escadron : disque pleine-face bleu (ACTIF) / rouge (INACTIF) comme le jeton du socle de chasseur.
// Tap sur le disque = retourner (activer/désactiver). Petit "i" à côté = définition wiki.
@Composable
internal fun SquadronActivationToken(activated: Boolean, onToggle: () -> Unit, onWikiClick: () -> Unit) {
    val face = if (activated) Color(0xFF2E7DD1) else Color(0xFFC0392B)   // bleu actif / rouge inactif
    val faceLight = if (activated) Color(0xFF4FC3F7) else Color(0xFFE57373)
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
                onClick = onToggle,
                shape = CircleShape,
                color = face,
                modifier = Modifier.size(58.dp),
                border = androidx.compose.foundation.BorderStroke(3.dp, faceLight)
            ) {
                Canvas(Modifier.fillMaxSize().padding(8.dp)) {
                    // Fuseau de chasseur (silhouette delta) au centre du jeton.
                    val w = size.width; val h = size.height
                    val cx = w / 2f; val cy = h / 2f
                    val body = Path().apply {
                        moveTo(cx, cy - h * 0.30f)          // nez
                        lineTo(cx + w * 0.10f, cy + h * 0.18f) // arrière droit
                        lineTo(cx + w * 0.30f, cy + h * 0.30f) // aile droite
                        lineTo(cx + w * 0.16f, cy + h * 0.10f)
                        lineTo(cx + w * 0.16f, cy + h * 0.30f) // aile droite bas
                        lineTo(cx, cy + h * 0.22f)
                        lineTo(cx - w * 0.16f, cy + h * 0.30f)
                        lineTo(cx - w * 0.16f, cy + h * 0.10f)
                        lineTo(cx - w * 0.30f, cy + h * 0.30f) // aile gauche
                        lineTo(cx - w * 0.10f, cy + h * 0.18f)
                        close()
                    }
                    drawPath(body, color = Color.White)
                }
            }
        }
        // Le texte ACTIF/INACTIF est cliquable vers le wiki (plus de bouton "i").
        Text(
            if (activated) "ACTIF" else "INACTIF",
            color = faceLight,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { onWikiClick() }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TokenSection(tokens: List<String>, onAdd: (String) -> Unit, onRemove: (String) -> Unit, isVehicle: Boolean = false) {
    // Jetons d'action Legion v2 : Aim (jaune), Dodge (violet), Surge (bleu), Standby (orange).
    // Vehicle Damage ne s'applique qu'aux véhicules (règle stricte) -> masqué sinon.
    val allTokens = listOf(
        LegionToken(stringResource(R.string.legion_token_aim), Color(0xFFFFC857), "\u25CE"),
        LegionToken(stringResource(R.string.legion_token_dodge), Color(0xFF9B6DFF), "\u25CC"),
        LegionToken(stringResource(R.string.legion_token_surge), Color(0xFF4FC3F7), "\u26A1"),
        LegionToken(stringResource(R.string.legion_token_standby), Color(0xFFFF8A65), "\u25C9"),
        LegionToken(stringResource(R.string.legion_token_observation), Color(0xFF90CAF9), "\u25C8"),
        LegionToken(stringResource(R.string.legion_token_advantage), Color(0xFF77D9A7), "\u25B2"),
        LegionToken(stringResource(R.string.legion_token_smoke), Color(0xFF9EACBC), "\u25CF"),
        LegionToken(stringResource(R.string.legion_token_charge), Color(0xFFFFB74D), "\u2694"),
        // Jetons de keywords + mécaniques (rulebook p.10) : Order, Commander, Ion, Poison, Immobilize, Shield, Incognito, Bane, Graffiti.
        LegionToken(stringResource(R.string.legion_token_order), Color(0xFF26C6DA), "\u25C6", "keyword"),
        LegionToken(stringResource(R.string.legion_token_commander), Color(0xFFFFC857), "\u2605", "keyword"),
        LegionToken(stringResource(R.string.legion_token_ion), Color(0xFF4DD0E1), "\u26A1", "keyword"),
        LegionToken(stringResource(R.string.legion_token_poison), Color(0xFF81C784), "\u2620", "keyword"),
        LegionToken(stringResource(R.string.legion_token_immobilize), Color(0xFFB388FF), "\u2696", "keyword"),
        LegionToken(stringResource(R.string.legion_token_shield), Color(0xFF64B5F6), "\u25C9", "keyword"),
        LegionToken(stringResource(R.string.legion_token_incognito), Color(0xFF9EACBC), "\u25C7", "keyword"),
        LegionToken(stringResource(R.string.legion_token_bane), Color(0xFFB8395A), "\u2691", "keyword"),
        LegionToken(stringResource(R.string.legion_token_graffiti), Color(0xFFFF8A65), "\u270E", "keyword")
    ) + if (isVehicle) listOf(LegionToken(stringResource(R.string.legion_token_vehicle_damage), Color(0xFFFF6B6B), "\u26A0", "vehicle")) else emptyList()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.tokens_markers), color = Color(0xFF9EACBC), style = MaterialTheme.typography.labelLarge)
        // Jetons posés (avec quantité) : chips retirables.
        if (tokens.isNotEmpty()) {
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                tokens.groupingBy { it }.eachCount().forEach { (t, count) ->
                    val meta = allTokens.firstOrNull { it.label == t }
                    Surface(onClick = { onRemove(t) }, shape = RoundedCornerShape(8.dp), color = Color(0xFF3B2224)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text(meta?.symbol ?: "\u25A3", color = meta?.color ?: Color(0xFFFFC7B7), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(6.dp))
                            Text("$t ×$count  \u2715", color = Color(0xFFFFC7B7), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        // Disques de jetons regroupés par catégorie (Action / Keyword / Véhicule).
        val categories = listOf("action" to stringResource(R.string.tokens_action), "keyword" to stringResource(R.string.tokens_keyword), "vehicle" to stringResource(R.string.tokens_vehicle))
        categories.forEach { (cat, catLabel) ->
            val catTokens = allTokens.filter { it.category == cat }
            if (catTokens.isEmpty()) return@forEach
            Text(catLabel, color = Color(0xFF5A6A7A), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                catTokens.forEach { t ->
                    val count = tokens.count { it == t.label }
                    LegionTokenDisc(t.label, t.color, t.symbol, count = count, onClick = { onAdd(t.label) })
                }
            }
        }
        Text(stringResource(R.string.tap_token), color = Color(0xFF5A6A7A), style = MaterialTheme.typography.labelSmall)
    }
}

// Jeton d'action Legion : disque coloré avec symbole, cliquable pour l'ajouter.
@Composable
internal fun LegionTokenDisc(label: String, color: Color, symbol: String, count: Int = 0, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            Surface(
                onClick = onClick,
                shape = CircleShape,
                color = color.copy(alpha = 0.22f),
                border = androidx.compose.foundation.BorderStroke(2.dp, color),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) { Text(symbol, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
            }
            if (count > 0) {
                Box(Modifier.align(Alignment.TopEnd).size(16.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
                    Text("$count", color = Color(0xFF0A0E15), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(label, color = color, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

// Modèle d'un jeton d'action Legion (label localisé + couleur + symbole).
internal data class LegionToken(val label: String, val color: Color, val symbol: String, val category: String = "action")

// Badge d'état auto (Blessé / Supprimé / Paniqué) : actif = coloré, sinon grisé.
@Composable
internal fun StateChip(label: String, active: Boolean, color: Color) {
    Surface(shape = RoundedCornerShape(8.dp), color = if (active) color.copy(alpha = 0.22f) else Color(0xFF1E2A3A)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (active) Icons.Filled.Check else Icons.Filled.Close, contentDescription = null, tint = if (active) color else Color(0xFF5A6A7A), modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, color = if (active) color else Color(0xFF5A6A7A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun BigCounter(label: String, value: Int, max: Int, color: Color, onInc: () -> Unit, onDec: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color(0xFF9EACBC), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(5.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Surface(onClick = onDec, shape = CircleShape, color = Color(0xFF2A3A4A), modifier = Modifier.size(44.dp)) { Box(contentAlignment = Alignment.Center) { Text("-", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) } }
            Text("$value", color = color, fontSize = 32.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.width(54.dp))
            Surface(onClick = onInc, shape = CircleShape, color = Color(0xFF2A3A4A), modifier = Modifier.size(44.dp)) { Box(contentAlignment = Alignment.Center) { Text("+", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) } }
        }
    }
}

// Stepper générique +/- réutilisable (blessures, suppression, couverture, coque).
@Composable
internal fun Stepper(value: String, onDec: () -> Unit, onInc: () -> Unit, size: Dp = 40.dp, valueColor: Color = Color.White, valueWidth: Dp = 40.dp) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(onClick = onDec, shape = CircleShape, color = Color(0xFF2A3A4A), modifier = Modifier.size(size)) { Box(contentAlignment = Alignment.Center) { Text("-", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) } }
        Text(value, color = valueColor, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.width(valueWidth))
        Surface(onClick = onInc, shape = CircleShape, color = Color(0xFF2A3A4A), modifier = Modifier.size(size)) { Box(contentAlignment = Alignment.Center) { Text("+", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) } }
    }
}

// Stepper de score (VP + unités vaincues) pour l'écran 2 joueurs.
@Composable
internal fun ScoreStepper(label: String, vp: Int, kills: Int, onVpInc: () -> Unit, onVpDec: () -> Unit, onKillInc: () -> Unit, onKillDec: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color(0xFF9EACBC), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Surface(onClick = onVpDec, shape = CircleShape, color = Color(0xFF2A3A4A), modifier = Modifier.size(40.dp)) { Box(contentAlignment = Alignment.Center) { Text("-", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) } }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$vp VP", color = Color(0xFFFFC857), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("$kills ✕", color = Color(0xFF77D9A7), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Surface(onClick = onVpInc, shape = CircleShape, color = Color(0xFF2A3A4A), modifier = Modifier.size(40.dp)) { Box(contentAlignment = Alignment.Center) { Text("+", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) } }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Surface(onClick = onKillDec, shape = RoundedCornerShape(6.dp), color = Color(0xFF2A3A4A), modifier = Modifier.size(28.dp)) { Box(contentAlignment = Alignment.Center) { Text("-", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) } }
            Surface(onClick = onKillInc, shape = RoundedCornerShape(6.dp), color = Color(0xFF2A3A4A), modifier = Modifier.size(28.dp)) { Box(contentAlignment = Alignment.Center) { Text("+", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) } }
        }
    }
}

// Compteur de jetons de passe (règle v2) : petit stepper compact.
@Composable
internal fun PassCounter(value: Int, onInc: () -> Unit, onDec: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Surface(onClick = onDec, shape = CircleShape, color = Color(0xFF2A3A4A), modifier = Modifier.size(30.dp)) { Box(contentAlignment = Alignment.Center) { Text("-", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold) } }
        Text("$value", color = Color(0xFFFFC857), fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.width(24.dp))
        Surface(onClick = onInc, shape = CircleShape, color = Color(0xFF2A3A4A), modifier = Modifier.size(30.dp)) { Box(contentAlignment = Alignment.Center) { Text("+", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold) } }
    }
}

// Compteur de coque compact avec +/- (pilote la coque actuelle, toujours visible dans le header).
@Composable
internal fun HullCounter(value: Int, max: Int, onInc: () -> Unit, onDec: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.hull), color = Color(0xFFFF8A80), fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Surface(onClick = onDec, shape = CircleShape, color = Color(0xFF2A3A4A), modifier = Modifier.size(44.dp)) { Box(contentAlignment = Alignment.Center) { Text("-", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) } }
            Text("$value/$max", color = Color(0xFFFF8A80), fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.width(56.dp))
            Surface(onClick = onInc, shape = CircleShape, color = Color(0xFF2A3A4A), modifier = Modifier.size(44.dp)) { Box(contentAlignment = Alignment.Center) { Text("+", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) } }
        }
    }
}
