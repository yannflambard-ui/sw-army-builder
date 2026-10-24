package com.legionforge.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    // Mode 2 joueurs : viewingArmy = null (liste des armées) | "own" (mon armée) | "opp" (armée adverse).
    var twoPlayer by remember { mutableStateOf(false) }
    var viewingArmy by remember { mutableStateOf<String?>(null) }
    val syncMode by playSyncVm.mode.collectAsState()
    val syncRound by playSyncVm.receivedRound.collectAsState()
    val opponentListJson by playSyncVm.opponentListJson.collectAsState()
    // Wiki des règles : chargé pour rendre les mots-clés du texte de règles cliquables.
    val wikiSections by vm.wikiSections.collectAsState()
    var ruleSection by remember { mutableStateOf<WikiSectionEntity?>(null) }
    val onRuleClick: (WikiSectionEntity) -> Unit = { ruleSection = it }
    LaunchedEffect(Unit) { vm.loadAllWiki() }
    // Applique le round reçu de l'autre téléphone.
    LaunchedEffect(syncRound) { if (twoPlayer && syncRound != null && syncRound != round) round = syncRound!! }
    // Émet le round quand il change en mode 2 joueurs.
    LaunchedEffect(round) { if (twoPlayer && syncMode == ShareMode.CONNECTED) playSyncVm.sendRound(round) }
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
    var showCmdSheet by remember { mutableStateOf(false) }
    // Battle deck (Legion v2) : 9 cartes (3+3+3) + mission (carte révélée par catégorie).
    val battleCards = remember { loadBattleCards(legionContext) }
    var battleDeck by remember { mutableStateOf(setOf<String>()) }
    var battleRevealed by remember { mutableStateOf(mapOf<String, String>()) }
    var showBattleSheet by remember { mutableStateOf(false) }
    // Store d'état de jeu : mon armée (éditable) + armée adverse (reçue, lecture seule).
    val ownStates by playSyncVm.ownStates.collectAsState()
    val oppStates by playSyncVm.oppStates.collectAsState()
    // Armée adverse : résolue depuis le payload partagé (cardId -> carte du catalogue local).
    val opp = remember(opponentListJson) {
        try { com.google.gson.Gson().fromJson(opponentListJson, com.legionforge.app.ui.viewmodel.ShareListPayload::class.java) } catch (_: Exception) { null }
    }
    val oppEntries = remember(opp, vm.cards.value) {
        opp?.entries?.mapNotNull { e ->
            vm.cards.value.firstOrNull { it.id == e.cardId }?.let { card ->
                ListEntry(
                    instanceId = e.instanceId ?: "opp_${e.cardId}_${e.parentInstanceId ?: "root"}",
                    card = card,
                    parentInstanceId = e.parentInstanceId,
                    quantity = e.quantity,
                    chosenSlot = e.chosenSlot?.let { s -> runCatching { ArmadaSlot.valueOf(s) }.getOrNull() }
                )
            }
        } ?: emptyList()
    }
    val oppPlayable = oppEntries.filter { e ->
        e.parentInstanceId == null && (e.card.kind == CardKind.LEGION_UNIT || e.card.kind == CardKind.ARMADA_SHIP || e.card.kind == CardKind.ARMADA_SQUADRON || e.card.kind == CardKind.COMMANDER)
    }
    val oppPagerState = rememberPagerState(pageCount = { oppPlayable.size.coerceAtLeast(1) })

    Scaffold(topBar = {
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
                        },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0A0E15)))
    }, bottomBar = {
        if (isLegion) {
            BottomAppBar(containerColor = Color(0xFF111827)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    // Deck de commandement
                    Surface(onClick = { showCmdSheet = true }, shape = RoundedCornerShape(10.dp), color = if (commandDeck.isNotEmpty()) Color(0xFF1E3A2A) else Color(0xFF2A3A4A)) {
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFFC857), modifier = Modifier.size(18.dp))
                            Text("CMD ${commandDeck.size}", color = Color(0xFFFFC857), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    // Battle deck
                    Surface(onClick = { showBattleSheet = true }, shape = RoundedCornerShape(10.dp), color = if (battleDeck.isNotEmpty()) Color(0xFF1E3A2A) else Color(0xFF2A3A4A)) {
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Info, contentDescription = null, tint = Color(0xFFFFC857), modifier = Modifier.size(18.dp))
                            Text("BAT ${battleDeck.size}", color = Color(0xFFFFC857), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    // Compteur de round
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(onClick = { if (round > 1) round-- }, shape = CircleShape, color = Color(0xFF2A3A4A), modifier = Modifier.size(40.dp)) { Box(contentAlignment = Alignment.Center) { Text("-", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { contentDescription = "Round précédent" }) } }
                        Text("R$round", color = Color(0xFFFFC857), fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.combinedClickable(onClick = {}, onLongClick = { round = 1 }))
                        Surface(onClick = { if (round < 6) round++ }, shape = CircleShape, color = Color(0xFF2A3A4A), modifier = Modifier.size(40.dp)) { Box(contentAlignment = Alignment.Center) { Text("+", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { contentDescription = "Round suivant" }) } }
                    }
                    // Fin de round : reset cartes jouées + avance.
                    Surface(onClick = { playedCommands = emptySet(); if (round < 6) round++ }, shape = RoundedCornerShape(10.dp), color = Color(0xFF3B2224)) {
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFFFF6B6B), modifier = Modifier.size(18.dp))
                            Text(stringResource(R.string.end_round), color = Color(0xFFFFC7B7), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }) { pad ->
        if (playable.isEmpty()) { Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) { Text(stringResource(R.string.add_units_play), color = Color.Gray) }; return@Scaffold }
        Box(Modifier.fillMaxSize().padding(pad)) {
            // ── ÉCRAN 2 JOUEURS : liste des armées (Mon armée / Armée adverse) ──
            if (twoPlayer && viewingArmy == null) {
                Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.play_2players), color = Color(0xFFFFC857), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    // Connexion Nearby : panneau si pas connecté, sinon indicateur.
                    if (syncMode != ShareMode.CONNECTED) {
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
                    // ── Score (VP + unités vaincues) ──
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2330))) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.score), color = Color(0xFFFFC857), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                ScoreStepper(stringResource(R.string.play_my_army), vp, kills, { vp = (vp + 1).coerceAtMost(15) }, { vp = (vp - 1).coerceAtLeast(0) }, { kills = kills + 1 }, { kills = (kills - 1).coerceAtLeast(0) })
                                ScoreStepper(stringResource(R.string.play_opponent), oppVp, oppKills, { oppVp = (oppVp + 1).coerceAtMost(15) }, { oppVp = (oppVp - 1).coerceAtLeast(0) }, { oppKills = oppKills + 1 }, { oppKills = (oppKills - 1).coerceAtLeast(0) })
                            }
                            // Priorité + jetons de passe.
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(stringResource(R.string.priority), color = Color(0xFF9EACBC), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(onClick = { priority = "own" }, shape = RoundedCornerShape(8.dp), color = if (priority == "own") Color(0xFFFFC857) else Color(0xFF2A3A4A)) { Text(stringResource(R.string.play_my_army), Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = if (priority == "own") Color(0xFF0A0E15) else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                                    Surface(onClick = { priority = "opp" }, shape = RoundedCornerShape(8.dp), color = if (priority == "opp") Color(0xFFFFC857) else Color(0xFF2A3A4A)) { Text(stringResource(R.string.play_opponent), Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = if (priority == "opp") Color(0xFF0A0E15) else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                                }
                            }
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(stringResource(R.string.pass_tokens), color = Color(0xFF9EACBC), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    PassCounter(passes, { passes = (passes + 1).coerceAtMost(5) }, { passes = (passes - 1).coerceAtLeast(0) })
                                    PassCounter(oppPasses, { oppPasses = (oppPasses + 1).coerceAtMost(5) }, { oppPasses = (oppPasses - 1).coerceAtLeast(0) })
                                }
                            }
                        }
                    }
                    // ── Mon armée ──
                    Card(onClick = { viewingArmy = "own" }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2330))) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(stringResource(R.string.play_my_army), color = Color(0xFFFFC857), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("${vm.currentList.value?.name ?: ""} • ${vm.currentList.value?.factionId ?: ""} • ${vm.currentList.value?.pointsLimit ?: 0} pts", color = Color.White, style = MaterialTheme.typography.labelMedium)
                            Text("${playable.size} unités", color = Color(0xFF8F9BAD), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    // ── Armée adverse ──
                    if (opp == null) {
                        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2330))) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(stringResource(R.string.play_opponent), color = Color(0xFFFFC857), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(stringResource(R.string.play_connecting), color = Color(0xFF8F9BAD), style = MaterialTheme.typography.bodyMedium)
                                Button(onClick = { playSyncVm.requestList() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A3A4A))) {
                                    Text("Renvoyer la synchro", color = Color(0xFFFFC857), fontWeight = FontWeight.Bold)
                                }
                                TextButton(onClick = {
                                    com.legionforge.app.util.CrashReporter.reportEvent(
                                        "Synchro 2 joueurs: liste adverse absente (${android.os.Build.MODEL}) ${System.currentTimeMillis()}",
                                        "Mode=${syncMode} · connecté=${playSyncVm.connectedEndpoint.value != null} · " +
                                        "liste adverse reçue=${opponentListJson != null} · " +
                                        "ma liste chargée=${vm.currentList.value != null} · " +
                                        "erreur=${playSyncVm.error.value ?: "aucune"}"
                                    )
                                }) { Text("Signaler sur GitHub", color = Color(0xFFFFC857), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            }
                        }
                    } else if (oppPlayable.isEmpty()) {
                        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2330))) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(stringResource(R.string.play_opponent), color = Color(0xFFFFC857), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("${opp.name} • ${opp.gameSystem} • ${opp.pointsLimit} pts", color = Color.White, style = MaterialTheme.typography.labelMedium)
                                Text("0 unité reçue — la liste adverse n'est pas encore arrivée", color = Color(0xFF8F9BAD), style = MaterialTheme.typography.bodySmall)
                                Button(onClick = { playSyncVm.requestList() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A3A4A))) {
                                    Text("Renvoyer la synchro", color = Color(0xFFFFC857), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        Card(onClick = { viewingArmy = "opp" }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2330))) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(stringResource(R.string.play_opponent), color = Color(0xFFFFC857), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("${opp.name} • ${opp.gameSystem} • ${opp.pointsLimit} pts", color = Color.White, style = MaterialTheme.typography.labelMedium)
                                Text("${oppPlayable.size} unités", color = Color(0xFF8F9BAD), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                return@Box
            }
            // ── PAGER : mon armée (éditable) ou armée adverse (lecture seule live) ──
            val isOpp = twoPlayer && viewingArmy == "opp"
            val pagerEntries = if (isOpp) oppPlayable else playable
            val pagerAll = if (isOpp) oppEntries else entries
            val states = if (isOpp) oppStates else ownStates
            val pagerState = if (isOpp) oppPagerState else ownPagerState
            val readOnly = isOpp
            val onUnitChange: (String, SyncedUnitState) -> Unit = if (isOpp) { _, _ -> } else { id, next -> playSyncVm.setOwnState(id, next) }
            if (pagerEntries.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (isOpp) "Armée adverse vide (aucune unité reçue)" else stringResource(R.string.add_units_play), color = Color.Gray)
                }
            } else {
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    val unit = pagerEntries[page]; val children = pagerAll.filter { it.parentInstanceId == unit.instanceId }
                    // Liens wiki : ne montrer que les mots-clés du jeu courant (pas de mélange Legion/Armada)
                    val unitWikiSections = remember(unit, wikiSections) {
                        wikiSections.filter { it.gameSystem == unit.card.gameSystem.name }
                    }
                    val unitState = states[unit.instanceId] ?: SyncedUnitState(unit.instanceId)
                    when (unit.card.kind) {
                        CardKind.ARMADA_SHIP -> ArmadaShipPage(unit, children, pagerAll, unitWikiSections, onRuleClick,
                            state = unitState, onChange = { next -> onUnitChange(unit.instanceId, next) }, readOnly = readOnly)
                        CardKind.ARMADA_SQUADRON -> ArmadaSquadronPage(unit, unitWikiSections, onRuleClick,
                            state = unitState, onChange = { next -> onUnitChange(unit.instanceId, next) }, readOnly = readOnly)
                        // Degats critiques reserves aux vaisseaux capitaux (par Regle Armada). Un commandant est equipe sur un vaisseau, il n'a pas de page de degats propres.
                        CardKind.COMMANDER -> CommanderPage(unit, unitWikiSections, onRuleClick)
                        else -> LegionUnitPage(unit, children, pagerAll, unitWikiSections, onRuleClick,
                            state = unitState, onChange = { next -> onUnitChange(unit.instanceId, next) }, readOnly = readOnly)
                    }
                }
                if (pagerEntries.size > 1) {
                    Row(Modifier.fillMaxWidth().padding(bottom = 50.dp).align(Alignment.BottomCenter), horizontalArrangement = Arrangement.Center) {
                        repeat(pagerEntries.size) { i -> Box(Modifier.padding(3.dp).size(if (i == pagerState.currentPage) 12.dp else 9.dp).clip(CircleShape).background(if (i == pagerState.currentPage) Color(0xFFFFC857) else Color(0xFF3A4A5A))) }
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
            onToggleDeck = { id -> commandDeck = if (id in commandDeck) commandDeck - id else commandDeck + id },
            onTogglePlayed = { id -> playedCommands = if (id in playedCommands) playedCommands - id else playedCommands + id },
            onClose = { showCmdSheet = false }
        )
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
    val imageAssetPath: String? = null
)

internal fun loadCommandCards(context: android.content.Context): List<LegionCommandCard> {
    return try {
        val s = context.assets.open("command_cards.json").bufferedReader().use { it.readText() }
        val arr = org.json.JSONObject(s).getJSONArray("cards")
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val cmdrs = buildList { val a = o.optJSONArray("commanders"); if (a != null) for (j in 0 until a.length()) add(a.getString(j)) }
                val kws = buildList { val a = o.optJSONArray("keywords"); if (a != null) for (j in 0 until a.length()) add(a.getString(j)) }
                add(LegionCommandCard(o.optString("id"), o.optString("name"), o.optString("pip"), o.optString("faction"), o.optBoolean("generic"), cmdrs, kws, o.optString("imageAssetPath").takeIf { it.isNotBlank() }))
            }
        }
    } catch (_: Exception) { emptyList() }
}

internal data class LegionBattleCard(val category: String, val name: String, val imageAssetPath: String? = null)

internal fun loadBattleCards(context: android.content.Context): List<LegionBattleCard> {
    return try {
        val s = context.assets.open("battle_cards.json").bufferedReader().use { it.readText() }
        val arr = org.json.JSONObject(s).getJSONArray("cards")
        buildList { for (i in 0 until arr.length()) { val o = arr.getJSONObject(i); add(LegionBattleCard(o.optString("category"), o.optString("name"), o.optString("imageAssetPath").takeIf { it.isNotBlank() })) } }
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
    onClose: () -> Unit
) {
    val order = listOf("objective" to "OBJECTIVE + MAP", "secondary" to "SECONDARY OBJECTIVE", "advantage" to "ADVANTAGE")
    var zoomCard by remember { mutableStateOf<LegionBattleCard?>(null) }
    ModalBottomSheet(onDismissRequest = onClose, containerColor = Color(0xFF111827)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("BATTLE DECK (9 cartes)", color = Color(0xFFFFC857), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Officiel : 3 Objective(+Map) + 3 Secondary + 3 Advantage, sans doublon.", color = Color(0xFF9EACBC), style = MaterialTheme.typography.bodySmall)
            Box(Modifier.fillMaxWidth().height(2.dp).background(Color(0xFF2A3A4A)))
            order.forEach { (cat, label) ->
                val catCards = cards.filter { it.category == cat }
                if (catCards.isEmpty()) return@forEach
                val sel = deck.count { id -> catCards.any { it.name == id } }
                Text("$label  ($sel/3)", color = Color(0xFFB4BFCE), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    catCards.forEach { c ->
                        val inDeck = c.name in deck
                        val full = sel >= 3
                        DeckCardThumb(
                            imageAssetPath = c.imageAssetPath,
                            name = c.name,
                            badge = if (inDeck && revealed[cat] == c.name) "★" else null,
                            selected = inDeck,
                            dimmed = full && !inDeck,
                            onClick = { zoomCard = c }
                        )
                    }
                }
                // Mission : révéler / remplacer la carte courante de cette catégorie.
                val rev = revealed[cat]
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Mission → ", color = Color(0xFF9EACBC), fontSize = 10.sp)
                    Text(rev.takeIf { !it.isNullOrEmpty() } ?: "—", color = Color(0xFFFFC857), fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    if (deckComplete(cat)) Button(onClick = { onCycleReveal(cat) }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) { Text("↻", fontSize = 14.sp) }
                }
            }
        }
    }
    zoomCard?.let { c ->
        val inDeck = c.name in deck
        val cat = c.category
        val full = deck.count { id -> cards.any { it.name == id && it.category == cat } } >= 3
        DeckCardZoom(
            imageAssetPath = c.imageAssetPath,
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
    onToggleDeck: (String) -> Unit,
    onTogglePlayed: (String) -> Unit,
    onClose: () -> Unit
) {
    // Règle officielle v2 : main = 2×1-pip + 2×2-pip + 2×3-pip + Standing Orders (4-pip, obligatoire).
    val standingOrders = cards.firstOrNull { it.pip == "4" }
    val hand = deck + (standingOrders?.let { setOf(it.id) } ?: emptySet())
    fun selectedCount(pip: String): Int = cards.count { it.pip == pip && it.id in hand }
    var zoomCard by remember { mutableStateOf<LegionCommandCard?>(null) }
    ModalBottomSheet(onDismissRequest = onClose, containerColor = Color(0xFF111827)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("MAIN DE COMMANDEMENT", color = Color(0xFFFFC857), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Officielle : 2×1-pip + 2×2-pip + 2×3-pip + Standing Orders (4-pip).  ${hand.size}/7", color = Color(0xFF9EACBC), style = MaterialTheme.typography.bodySmall)
            // Standing Orders : toujours incluse, verrouillée.
            if (standingOrders != null) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    DeckCardThumb(
                        imageAssetPath = standingOrders.imageAssetPath,
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
                            imageAssetPath = c.imageAssetPath,
                            name = c.name,
                            badge = pip,
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
            imageAssetPath = c.imageAssetPath,
            title = c.name,
            subtitle = "PIP $pip",
            inDeck = inDeck,
            canAdd = !full,
            locked = locked,
            onToggleDeck = { onToggleDeck(c.id) },
            onTogglePlayed = { onTogglePlayed(c.id) },
            isPlayed = isPlayed,
            onClose = { zoomCard = null }
        )
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
                    Surface(onClick = onClose, shape = RoundedCornerShape(8.dp), color = Color(0xFF2A3A4A)) {
                        Text("Fermer", Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = Color(0xFF9EACBC), fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(onClick = { if (cover > 0) set(state.copy(cover = cover - 1)) }, shape = CircleShape, color = Color(0xFF2A3A4A), modifier = Modifier.size(40.dp)) { Box(contentAlignment = Alignment.Center) { Text("-", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) } }
                    Text("$cover", color = if (cover > 0) Color(0xFF77D9A7) else Color(0xFF5A6A7A), fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.width(30.dp))
                    Surface(onClick = { if (cover < 2) set(state.copy(cover = cover + 1)) }, shape = CircleShape, color = Color(0xFF2A3A4A), modifier = Modifier.size(40.dp)) { Box(contentAlignment = Alignment.Center) { Text("+", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) } }
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
                Column(Modifier.weight(1f)) {
                    Text(unit.card.displayName(), color = factionColor(unit.card.factionId), style = MaterialTheme.typography.headlineSmall)
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
