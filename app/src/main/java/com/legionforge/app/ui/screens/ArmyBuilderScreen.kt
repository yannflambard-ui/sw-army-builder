package com.legionforge.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.legionforge.app.R
import com.legionforge.app.data.model.*
import com.legionforge.app.ui.viewmodel.ArmyBuilderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArmyBuilderScreen(listId: String, onBack: () -> Unit, onPlayCard: (String, String) -> Unit = { _, _ -> }, onPlayCondensed: (String) -> Unit = {}, onShare: () -> Unit = {}, viewModel: ArmyBuilderViewModel = viewModel()) {
    val list by viewModel.currentList.collectAsState()
    val cards by viewModel.cards.collectAsState()
    val entries by viewModel.entries.collectAsState()
    val validation by viewModel.validation.collectAsState()
    var search by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedParentId by remember { mutableStateOf<String?>(null) }
                // Filtres catalogue (P0) : rang/type, tranche de points, mot-clé
    var filterRank by remember { mutableStateOf<String?>(null) }
    var filterMaxPts by remember { mutableStateOf<Int?>(null) }
    var filterKeyword by remember { mutableStateOf<String?>(null) }
    var filtersMenuOpen by remember { mutableStateOf(false) }
    // Filtre "Ma collection" : n'affiche que le matériel possédé (coché dans Réglages).
    // Persistant : activable depuis Réglages ET le menu ⚙ du builder.
    val collectionOnly by com.legionforge.app.data.repository.CollectionRepository.collectionOnly.collectAsState()
    val ownedCards by com.legionforge.app.data.repository.CollectionRepository.owned.collectAsState()
    // Aperçu image plein écran : clic sur une carte -> affiche l'image, clic sur l'image -> referme.
    var previewCard by remember { mutableStateOf<CardDefinition?>(null) }
    // Renommage de la liste : titre cliquable + dialog avec champ texte.
    var renameOpen by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }
    LaunchedEffect(listId) { viewModel.openList(listId) }
    val game = list?.gameSystem?.let { runCatching { GameSystem.valueOf(it) }.getOrNull() } ?: GameSystem.LEGION_V2
    val allowedKinds = if (game == GameSystem.LEGION_V2) setOf(CardKind.LEGION_UNIT, CardKind.LEGION_UPGRADE) else setOf(CardKind.ARMADA_SHIP, CardKind.ARMADA_SQUADRON, CardKind.ARMADA_UPGRADE, CardKind.COMMANDER)
    // Faction de la liste : le catalogue n'affiche que les cartes neutres ou de cette faction.
    // (Certaines cartes Armada existent en 2 factions — ex: Ahsoka Tano rebel/republic — mais
    // ce sont des cartes distinctes par faction, donc le filtre factionId les gère correctement.)
    val listFaction = list?.factionId
    fun matchesFaction(c: CardDefinition) = listFaction == null || c.factionId == listFaction || c.factionId == "neutral" || (c.factionId == "mercenary" && listFaction in c.mercenaryFactions)
    // When a parent unit is selected, show ONLY the upgrades that fit in that parent's
    // slots (and, for Armada, that match the ship family via linkedUnit). No other ships,
    // squadrons or commanders — the catalogue is scoped to the selected parent.
    // Without a selection: show ONLY the units/ships/squadrons/commanders — upgrades are
    // hidden until a unit or ship is selected.
    val selectedParent = entries.firstOrNull { it.instanceId == selectedParentId }
    // Slots du parent déjà pleins : un slot est plein quand le nombre d'upgrades attachées
    // de ce type atteint le nombre de fois que le slot apparaît dans allowedUpgradeSlots
    // (ex. Executor I = 4 slots OFFICER). Les upgrades de ces slots sont masquées du catalogue.
    val parentChildren = entries.filter { it.parentInstanceId == selectedParent?.instanceId }
    val fullSlots: Set<ArmadaSlot> = selectedParent?.card?.allowedUpgradeSlots?.toSet()?.filter { slot ->
        parentChildren.count { (it.chosenSlot ?: it.card.upgradeSlots.firstOrNull()) == slot } >= selectedParent.card.allowedUpgradeSlots.count { it == slot }
    }?.toSet() ?: emptySet()
    val filteredAdditions = if (selectedParent != null) {
            cards.filter { it.kind in allowedKinds && matchesFaction(it) }.filter { c ->
                when {
                    game == GameSystem.LEGION_V2 && c.kind == CardKind.LEGION_UPGRADE ->
                        c.upgradeSlots.any { it in selectedParent.card.allowedUpgradeSlots } && c.upgradeSlots.none { it in fullSlots } &&
                        // Règle Legion : une unité ne peut pas équiper 2× la même amélioration.
                        !parentChildren.any { it.card.id == c.id } &&
                        // Restriction d'unité (requirements cardName) : ex. RPS-6 ARF Trooper = ARF Troopers only,
                        // Radiation Cannon B1 = B1 Battle Droids only. Sans ce check, ces upgrades apparaissent
                        // sur toutes les unités partageant le même slot (ex. Snowtroopers).
                        com.legionforge.app.domain.LegionRequirements.matches(c.rulesText, selectedParent.card)
                    game == GameSystem.ARMADA_V15 && c.kind == CardKind.ARMADA_UPGRADE ->
                        c.upgradeSlots.any { it in selectedParent.card.allowedUpgradeSlots } && upgradeFitsShip(c, selectedParent.card) &&
                        c.upgradeSlots.none { it in fullSlots } &&
                        // Règle Armada : un vaisseau ne peut pas équiper 2× la même amélioration.
                        !parentChildren.any { it.card.id == c.id }
                    // Commandant Armada : n'apparaît que sur un vaisseau capital, et seulement
                    // si la flotte n'a pas déjà de commandant (une fois choisi, les autres sont masqués).
                    game == GameSystem.ARMADA_V15 && c.kind == CardKind.COMMANDER ->
                        selectedParent.card.kind == CardKind.ARMADA_SHIP &&
                            c.upgradeSlots.any { it in selectedParent.card.allowedUpgradeSlots } &&
                            !entries.any { it.card.kind == CardKind.COMMANDER }
                    else -> false
                }
            }
        } else cards.filter { it.kind in allowedKinds && matchesFaction(it) && it.kind != CardKind.LEGION_UPGRADE && it.kind != CardKind.ARMADA_UPGRADE && it.kind != CardKind.COMMANDER }
    val additions = filteredAdditions
                .filter { it.name.contains(search, ignoreCase = true) || it.factionId.contains(search, ignoreCase = true) }
                .let { list ->
                    val rk = filterRank; val mp = filterMaxPts; val kw = filterKeyword
                    var out = list.filter { rk == null || it.legionRank?.name == rk }
                        .filter { mp == null || it.points <= mp }
                        .filter { kw == null || it.name.contains(kw, ignoreCase = true) || (it.rulesText ?: "").contains(kw, ignoreCase = true) || (it.legionStats ?: "").contains(kw, ignoreCase = true) || (it.shipStats ?: "").contains(kw, ignoreCase = true) }
                    // Filtre "Ma collection" : n'affiche que les cartes possédées. Les UPGRADES
                    // suivent l'unité/vaisseau sélectionné (on possède sa boîte = on a ses upgrades),
                    // donc elles ne sont filtrées que si le parent n'est pas possédé — ce cas est
                    // déjà exclu car le parent ne serait pas dans le catalogue. On ne filtre donc que
                    // les cartes standalone (unités/vaisseaux/escadrons/commandants).
                    if (collectionOnly) out = out.filter {
                        it.kind == CardKind.LEGION_UPGRADE || it.kind == CardKind.ARMADA_UPGRADE || it.id in ownedCards
                    }
                    out
                }
    // Bouton play global : actif (vert) uniquement si la liste est valide.
    val listValid = validation.violations.isEmpty() && entries.isNotEmpty()
    val firstPlayable = entries.firstOrNull { it.parentInstanceId == null && (it.card.kind == CardKind.LEGION_UNIT || it.card.kind == CardKind.ARMADA_SHIP) }
    Scaffold(topBar = {
        TopAppBar(title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f, fill = false)) {
                    TextButton(onClick = {
                        renameText = list?.name ?: ""
                        renameOpen = true
                    }) {
                        Text(list?.name ?: stringResource(R.string.new_list_title), style = MaterialTheme.typography.titleLarge, color = Color.White, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                    Text("${game.label()}  •  ${list?.factionId.orEmpty()}", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFC857))
                }
                // Bouton play compact à côté du nom de la liste
                Button(
                    onClick = { if (firstPlayable != null) onPlayCondensed(listId) },
                    enabled = listValid && firstPlayable != null,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (listValid) Color(0xFF1E7A3C) else Color(0xFF2A3A4A),
                        contentColor = if (listValid) Color.White else Color(0xFF718096)
                    )
                ) {
                                    Text("\u25B6", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                // Bouton partage (Nearby Connections)
                                Button(
                                    onClick = onShare,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A2330), contentColor = Color(0xFFFFC857))
                                ) {
                                    Text("\uD83D\uDD0C", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
        }, navigationIcon = { TextButton(onClick = onBack) { Text("‹") } })
    }) { pad ->
        Column(Modifier.fillMaxSize().background(Color(0xFF0A0E15)).padding(pad)) {
            val total = validation.totalPoints
            val limit = list?.pointsLimit ?: if (game == GameSystem.LEGION_V2) 1000 else 400
            Card(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 2.dp), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))) {
                Column(Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("$total", color = Color(0xFFFFC857), style = MaterialTheme.typography.titleLarge)
                            Text(stringResource(R.string.points_label, limit), color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
                        }
                        val count = entries.filter { it.card.kind == if (game == GameSystem.LEGION_V2) CardKind.LEGION_UNIT else CardKind.ARMADA_SHIP }.sumOf { it.quantity }
                        Text("$count ${if (game == GameSystem.LEGION_V2) stringResource(R.string.units_vehicles) else stringResource(R.string.ships)}", color = Color.White)
                    }
                    if (game == GameSystem.ARMADA_V15) {
                        val squadronPts = entries.filter { it.card.kind == CardKind.ARMADA_SQUADRON }.sumOf { it.card.points * it.quantity }
                        LinearProgressIndicator(progress = { (squadronPts.toFloat() / ((limit + 2) / 3).coerceAtLeast(1)).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(), color = Color(0xFFFFC857))
                        Text(stringResource(R.string.squadrons_label, squadronPts, (limit + 2) / 3), color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
                    } else {
                        val counts = entries.filter { it.card.kind == CardKind.LEGION_UNIT }.groupBy { it.card.legionRank }.mapValues { (_, items) -> items.sumOf { it.quantity } }
                        Text("C ${counts[LegionRank.COMMANDER] ?: 0}/1–2   •   T ${counts[LegionRank.CORPS] ?: 0}/3–6   •   FS ${counts[LegionRank.SPECIAL_FORCES] ?: 0}/0–3", color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
                    }
                    // Indicateur de validité dans l'encadré des points
                    if (entries.isNotEmpty()) {
                        Text(if (listValid) stringResource(R.string.list_valid) else stringResource(R.string.list_invalid), color = if (listValid) Color(0xFF77D9A7) else Color(0xFFFF927F), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (validation.violations.isNotEmpty()) {
                Card(Modifier.fillMaxWidth().padding(horizontal = 14.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF3B2224))) {
                    Column(Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        validation.violations.take(3).forEach { Text("! ${it.message}", color = Color(0xFFFFC7B7), style = MaterialTheme.typography.bodySmall) }
                        if (validation.violations.size > 3) Text(stringResource(R.string.rules_to_fix, validation.violations.size - 3), color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text(stringResource(R.string.tab_list, entries.size)) })
                Tab(selectedTab == 1, onClick = { selectedTab = 1 }, text = {
                    if (selectedParent != null) Text("→ ${selectedParent.card.displayName().take(18)}")
                    else Text(stringResource(R.string.tab_catalog, additions.size))
                })
            }
            if (selectedParent != null && selectedTab == 1) {
                Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 2.dp), shape = RoundedCornerShape(12.dp), color = Color(0xFFFFB800).copy(alpha = 0.15f)) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.adding_to, selectedParent.card.displayName()), color = Color(0xFFFFC857), style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { selectedParentId = null }) { Text("✕", color = Color.White) }
                    }
                }
            }
            if (selectedTab == 0) {
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    if (entries.isEmpty()) item { Text(stringResource(R.string.empty_army), color = Color.LightGray, modifier = Modifier.padding(16.dp)) }
                    // Group entries hierarchically: parents first, then their upgrades indented
                    val parents = entries.filter { it.parentInstanceId == null }
                    val allParentIds = parents.map { it.instanceId }.toSet()
                    val grouped = parents.map { parent ->
                        val children = entries.filter { it.parentInstanceId == parent.instanceId }
                        parent to children
                    }
                    val orphans = entries.filter { it.parentInstanceId != null && it.parentInstanceId !in allParentIds }
                    grouped.forEach { (parent, children) ->
                        item(key = parent.instanceId) {
                            FactionGroup(parent, children, onRemove = { viewModel.remove(it) }, onSelectParent = {
                                if (parent.card.kind == CardKind.LEGION_UNIT || parent.card.kind == CardKind.ARMADA_SHIP) {
                                    selectedParentId = parent.instanceId
                                    selectedTab = 1
                                }
                            })
                        }
                    }
                    orphans.forEach { orphan ->
                        item(key = orphan.instanceId) {
                            BuilderEntryCard(orphan, isChild = true, onRemove = { viewModel.remove(orphan) })
                        }
                    }
                }
            } else {
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = search, onValueChange = { search = it }, modifier = Modifier.weight(1f).height(40.dp), placeholder = { Text(stringResource(R.string.search_hint), fontSize = 12.sp) }, singleLine = true, textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, lineHeight = 16.sp))
                    // Bouton filtre compact à droite de la barre de recherche
                    Box {
                        Surface(onClick = { filtersMenuOpen = true }, shape = RoundedCornerShape(10.dp), color = if (filterRank != null || filterMaxPts != null || filterKeyword != null || collectionOnly) Color(0xFFFFC857) else Color(0xFF2A3A4A)) {
                            Text("\u2699", Modifier.padding(horizontal = 12.dp, vertical = 12.dp), color = if (filterRank != null || filterMaxPts != null || filterKeyword != null || collectionOnly) Color(0xFF0A0E15) else Color(0xFFFFC857), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        DropdownMenu(expanded = filtersMenuOpen, onDismissRequest = { filtersMenuOpen = false }) {
                            // Rang/type : petits chips à lettre
                            val ranks = if (game == GameSystem.LEGION_V2) listOf("COMMANDER" to "C", "OPERATIVE" to "O", "CORPS" to "Co", "SPECIAL_FORCES" to "FS", "SUPPORT" to "S", "HEAVY" to "H") else listOf("ARMADA_SHIP" to "V", "ARMADA_SQUADRON" to "E", "COMMANDER" to "Cmd")
                            Text(stringResource(R.string.filter_rank), color = Color(0xFF9EACBC), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                            Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                ranks.forEach { (r, abbr) ->
                                    FilterChip(selected = filterRank == r, onClick = { filterRank = if (filterRank == r) null else r }, label = { Text(abbr, fontSize = 10.sp) })
                                }
                            }
                            HorizontalDivider(color = Color(0xFF2A3A4A), modifier = Modifier.padding(vertical = 6.dp))
                            Text(stringResource(R.string.filter_max_pts), color = Color(0xFF9EACBC), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                            Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(25, 50, 100).forEach { maxPts ->
                                    FilterChip(selected = filterMaxPts == maxPts, onClick = { filterMaxPts = if (filterMaxPts == maxPts) null else maxPts }, label = { Text("≤$maxPts", fontSize = 10.sp) })
                                }
                            }
                            HorizontalDivider(color = Color(0xFF2A3A4A), modifier = Modifier.padding(vertical = 6.dp))
                            OutlinedTextField(value = filterKeyword ?: "", onValueChange = { filterKeyword = it.ifBlank { null } }, modifier = Modifier.padding(horizontal = 12.dp).width(180.dp), placeholder = { Text(stringResource(R.string.filter_keyword), fontSize = 12.sp) }, singleLine = true, textStyle = MaterialTheme.typography.bodySmall)
                            HorizontalDivider(color = Color(0xFF2A3A4A), modifier = Modifier.padding(vertical = 6.dp))
                            Text(stringResource(R.string.my_collection), color = Color(0xFF9EACBC), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                            Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(selected = collectionOnly, onClick = { com.legionforge.app.data.repository.CollectionRepository.setCollectionOnly(!collectionOnly) }, label = { Text(stringResource(R.string.my_collection), fontSize = 11.sp) }, leadingIcon = if (collectionOnly) { { Text("\u2713", fontSize = 10.sp) } } else null)
                                Text("(${ownedCards.size})", color = Color(0xFFFFC857), style = MaterialTheme.typography.labelSmall)
                            }
                            if (filterRank != null || filterMaxPts != null || filterKeyword != null || collectionOnly) {
                                TextButton(onClick = { filterRank = null; filterMaxPts = null; filterKeyword = null; com.legionforge.app.data.repository.CollectionRepository.setCollectionOnly(false) }, modifier = Modifier.padding(horizontal = 8.dp)) { Text(stringResource(R.string.clear_filters), color = Color(0xFFFF927F), style = MaterialTheme.typography.labelSmall) }
                            }
                        }
                    }
                }
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(additions, key = { it.id }) { card -> CatalogCard(card, entries, selectedParentId, onAdd = { parent, slot -> viewModel.add(card, parent, slot) }, onPreview = { previewCard = card }) }
                                }
            }
            Text(stringResource(R.string.offline_catalog_note), Modifier.align(Alignment.CenterHorizontally).padding(bottom = 6.dp), color = Color(0xFF718096), style = MaterialTheme.typography.labelSmall)
        }
    }
    if (renameOpen) {
        AlertDialog(
            onDismissRequest = { renameOpen = false },
            title = { Text(stringResource(R.string.rename_title), color = Color.White) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.rename_placeholder)) },
                    textStyle = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.renameList(renameText)
                    renameOpen = false
                }) { Text(stringResource(R.string.save), color = Color(0xFFFFC857), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { renameOpen = false }) { Text(stringResource(R.string.cancel), color = Color(0xFF9EACBC)) }
            },
            containerColor = Color(0xFF192330)
        )
    }
    // Aperçu plein écran de l'image : clic sur une carte l'ouvre, clic sur l'image la referme.
    previewCard?.let { card ->
        ImagePreviewDialog(card = card, onClose = { previewCard = null })
    }
}

@Composable
private fun ImagePreviewDialog(card: CardDefinition, onClose: () -> Unit) {
    val source: Any? = card.imagePath()?.let { "file:///android_asset/$it" } ?: card.imageUrl
    Dialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(
                modifier = Modifier.fillMaxSize().background(Color(0xE6000000)),
                color = Color.Transparent
            ) {
            Box(contentAlignment = Alignment.Center) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(card.displayName(), color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Surface(onClick = onClose, modifier = Modifier.clip(RoundedCornerShape(14.dp)), shape = RoundedCornerShape(14.dp), color = Color(0xFF18212D)) {
                        if (source != null) {
                            AsyncImage(
                                model = source,
contentDescription = stringResource(R.string.icon_desc, card.displayName()),
                                modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Box(Modifier.fillMaxWidth().aspectRatio(cardAspectRatio(card)).background(Brush.linearGradient(listOf(Color(0xFF253344), Color(0xFF111820)))), contentAlignment = Alignment.Center) {
                                Text(stringResource(R.string.no_image), color = Color(0xFF8494A8), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    Text(stringResource(R.string.click_to_close), color = Color(0xFF718096), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
internal fun factionLabel(factionId: String?): String = when (factionId) {
    "rebel" -> stringResource(R.string.faction_rebel)
    "empire" -> stringResource(R.string.faction_empire)
    "republic", "republics" -> stringResource(R.string.faction_republic)
    "separatist", "separatists" -> stringResource(R.string.faction_separatist)
    "neutral" -> stringResource(R.string.faction_neutral)
    else -> factionId?.replace('-', ' ') ?: stringResource(R.string.faction_neutral)
}

/** Libellé court du type de carte affiché dans le catalogue (pas la valeur de filtre). */
@Composable
internal fun kindLabel(kind: CardKind): String = when (kind) {
    CardKind.LEGION_UNIT -> stringResource(R.string.kind_unit)
    CardKind.LEGION_UPGRADE -> stringResource(R.string.kind_upgrade)
    CardKind.ARMADA_SHIP -> stringResource(R.string.kind_ship)
    CardKind.ARMADA_SQUADRON -> stringResource(R.string.kind_squadron)
    CardKind.ARMADA_UPGRADE -> stringResource(R.string.kind_upgrade)
    CardKind.COMMANDER -> stringResource(R.string.kind_commander)
}

// Libellé + couleur du rang/type d'unité Legion (badge dans le catalogue).
@Composable
internal fun rankLabel(rank: LegionRank?): String = when (rank) {
    LegionRank.COMMANDER -> stringResource(R.string.rank_commander)
    LegionRank.OPERATIVE -> stringResource(R.string.rank_operative)
    LegionRank.CORPS -> stringResource(R.string.rank_corps)
    LegionRank.SPECIAL_FORCES -> stringResource(R.string.rank_special)
    LegionRank.SUPPORT -> stringResource(R.string.rank_support)
    LegionRank.HEAVY -> stringResource(R.string.rank_heavy)
    null -> ""
}
internal fun rankColor(rank: LegionRank?): Color = when (rank) {
    LegionRank.COMMANDER -> Color(0xFFFF6B6B)      // rouge commandant
    LegionRank.OPERATIVE -> Color(0xFFB388FF)      // violet agent
    LegionRank.CORPS -> Color(0xFF4FC3F7)          // bleu corps
    LegionRank.SPECIAL_FORCES -> Color(0xFF81C784) // vert forces spéciales
    LegionRank.SUPPORT -> Color(0xFF4DD0E1)        // cyan soutien
    LegionRank.HEAVY -> Color(0xFFFF8A65)          // orange lourd
    null -> Color(0xFF9EACBC)
}

@Composable
internal fun FactionCardColor(factionId: String?): Color = when (factionId) {
    "rebel" -> Color(0xFF4EC9E0)        // cyan
    "empire" -> Color(0xFFFF5A5A)       // rouge impérial
    "republic", "republics" -> Color(0xFFE8B54E) // or/jaune
    "separatist", "separatists" -> Color(0xFF9B6DFF) // violet
    "mercenary", "mercenaries" -> Color(0xFFB8395A)  // bordeaux mercenaire
    "mandalorians", "mandalorian" -> Color(0xFF7A8B9E) // acier mandalorien
    "neutral" -> Color(0xFF9EACBC)      // gris
    else -> Color(0xFF9EACBC)
}

@Composable
private fun FactionGroup(parent: ListEntry, children: List<ListEntry>, onRemove: (ListEntry) -> Unit, onSelectParent: () -> Unit) {
    val isSelectable = parent.card.kind == CardKind.LEGION_UNIT || parent.card.kind == CardKind.ARMADA_SHIP
    val accent = FactionCardColor(parent.card.factionId)
    Column(Modifier.fillMaxWidth().border(1.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(15.dp)).padding(5.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        BuilderEntryCard(parent, isChild = false, accentColor = accent, onRemove = { onRemove(parent) }, onSelectParent = onSelectParent, isSelectable = isSelectable)
        children.forEach { child ->
            BuilderEntryCard(child, isChild = true, onRemove = { onRemove(child) })
        }
    }
}

@Composable
private fun BuilderEntryCard(entry: ListEntry, isChild: Boolean = false, accentColor: Color = Color(0xFF9EACBC), isSelectable: Boolean = false, onRemove: () -> Unit, onSelectParent: () -> Unit = {}) {
    Card(Modifier
        .fillMaxWidth()
        .then(if (isSelectable) Modifier.clickable { onSelectParent() } else Modifier)
        .then(if (isChild) Modifier.padding(start = 28.dp) else Modifier),
        shape = RoundedCornerShape(if (isChild) 10.dp else 15.dp),
        colors = CardDefaults.cardColors(containerColor = if (isChild) Color(0xFF1E2A3A) else Color(0xFF18212D))) {
        Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            CardArtwork(entry.card, Modifier.width(if (entry.card.kind == CardKind.LEGION_UNIT) 96.dp else 64.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(entry.card.displayName(), color = Color.White, style = MaterialTheme.typography.titleSmall)
                Text("${entry.card.points * entry.quantity} pts  •  ${entry.card.legionRank?.name?.replace('_', ' ') ?: kindLabel(entry.card.kind)}", color = accentColor, style = MaterialTheme.typography.labelSmall)
                if (entry.parentInstanceId != null) Text("↳ ${entry.chosenSlot?.name?.replace('_', ' ') ?: stringResource(R.string.linked_upgrade)}", color = Color(0xFF77D9A7), style = MaterialTheme.typography.labelSmall)
                if (entry.card.kind == CardKind.LEGION_UNIT || entry.card.kind == CardKind.ARMADA_SHIP) {
                    if (entry.card.allowedUpgradeSlots.isNotEmpty()) Text(stringResource(R.string.slots_label, entry.card.allowedUpgradeSlots.joinToString { it.name.lowercase().replace('_', ' ') }), color = Color(0xFF9EACBC), style = MaterialTheme.typography.labelSmall, maxLines = 2)
                }
            }
            // Bouton − de retrait (remplace le swipe gauche)
            Surface(
                onClick = onRemove,
                shape = CircleShape,
                color = Color(0xFF3B2224),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) { Text("−", color = Color(0xFFFF927F), fontSize = 20.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun CatalogCard(card: CardDefinition, entries: List<ListEntry>, preselectedParentId: String? = null, onAdd: (String?, ArmadaSlot?) -> Unit, onPreview: () -> Unit) {
    val armadaShip = card.kind == CardKind.ARMADA_SHIP
    val isUpgrade = card.kind == CardKind.LEGION_UPGRADE || card.kind == CardKind.ARMADA_UPGRADE
    val isCommander = card.kind == CardKind.COMMANDER
    val requiresTarget = isUpgrade || isCommander
    val targetUnits = if (card.kind == CardKind.LEGION_UPGRADE) entries.filter { it.card.kind == CardKind.LEGION_UNIT } else entries.filter { it.card.kind == CardKind.ARMADA_SHIP }
    val eligibleTargets = if (card.kind == CardKind.LEGION_UPGRADE) targetUnits.filter { target -> card.upgradeSlots.firstOrNull()?.let { it in target.card.allowedUpgradeSlots } == true && com.legionforge.app.domain.LegionRequirements.matches(card.rulesText, target.card) } else if (card.kind == CardKind.ARMADA_UPGRADE) targetUnits.filter { target -> card.upgradeSlots.any { it in target.card.allowedUpgradeSlots } && upgradeFitsShip(card, target.card) } else targetUnits
    val preselectedTarget = eligibleTargets.firstOrNull { it.instanceId == preselectedParentId && it.card.allowedUpgradeSlots.any { slot -> slot in card.upgradeSlots } }
    var targetId by remember(card.id, eligibleTargets.size, preselectedTarget) { mutableStateOf(preselectedTarget?.instanceId ?: eligibleTargets.firstOrNull()?.instanceId) }
    var selectedSlot by remember(card.id, preselectedTarget) { mutableStateOf(
        if (preselectedTarget != null) card.upgradeSlots.firstOrNull { it in preselectedTarget.card.allowedUpgradeSlots } ?: card.upgradeSlots.firstOrNull()
        else card.upgradeSlots.firstOrNull()
    ) }
    val eligibleSlots = if (card.kind == CardKind.ARMADA_UPGRADE) card.upgradeSlots.filter { slot -> eligibleTargets.any { target -> slot in target.card.allowedUpgradeSlots } } else if (card.kind == CardKind.LEGION_UPGRADE) card.upgradeSlots.filter { slot -> eligibleTargets.any { target -> slot in target.card.allowedUpgradeSlots } } else card.upgradeSlots
    val slotCounts = entries.filter { it.parentInstanceId != null && (it.card.kind == CardKind.ARMADA_UPGRADE || it.card.kind == CardKind.LEGION_UPGRADE) }.groupBy { it.parentInstanceId to (it.chosenSlot ?: it.card.upgradeSlots.firstOrNull()) }.mapValues { (_, items) -> items.sumOf { it.quantity } }
    val addingTo = eligibleTargets.firstOrNull { it.instanceId == targetId }
    val validSelection = (!requiresTarget || addingTo != null) && (!isUpgrade || (selectedSlot != null && addingTo != null && selectedSlot in addingTo.card.allowedUpgradeSlots && (slotCounts[targetId to selectedSlot] ?: 0) < addingTo.card.allowedUpgradeSlots.count { it == selectedSlot }))
    val alreadyAdded = entries.any { it.card.id == card.id && (card.unique || card.kind == CardKind.COMMANDER) }
    val compatibleSlotFull = isUpgrade && eligibleSlots.isNotEmpty() && eligibleSlots.all { slot -> eligibleTargets.all { target -> (slotCounts[target.instanceId to slot] ?: 0) >= target.card.allowedUpgradeSlots.count { it == slot } } }
    val alreadyCount = entries.filter { it.card.id == card.id }.sumOf { it.quantity }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF18212D))) {
            Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(11.dp), verticalAlignment = Alignment.CenterVertically) {
                CardArtwork(card, Modifier.width(if (card.kind == CardKind.LEGION_UNIT) 88.dp else 58.dp).clickable { onPreview() })
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(card.displayName(), color = Color.White, style = MaterialTheme.typography.titleSmall)
                Text("${card.points} pts • ${kindLabel(card.kind)}", color = Color(0xFFFFC857), style = MaterialTheme.typography.labelSmall)
                if (card.kind == CardKind.LEGION_UNIT && card.legionRank != null) {
                    Text(rankLabel(card.legionRank), color = rankColor(card.legionRank), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                Text("${factionLabel(card.factionId)} • ${if (card.unique) stringResource(R.string.unique) else stringResource(R.string.standard)}", maxLines = 1, color = Color(0xFFB4BFCE), style = MaterialTheme.typography.bodySmall)
                if (requiresTarget && eligibleTargets.isNotEmpty()) {
                    // Quand un parent est déjà sélectionné, le catalogue est scoped à ce parent :
                    // le sélecteur "Pour :" est redondant, on l'affiche seulement sans présélection.
                    if (preselectedParentId == null) {
                        var expandedTarget by remember { mutableStateOf(false) }
                        Box {
                            TextButton(onClick = { expandedTarget = true }) { Text(stringResource(R.string.for_label, eligibleTargets.firstOrNull { it.instanceId == targetId }?.card?.name ?: stringResource(R.string.choose_unit))) }
                            DropdownMenu(expandedTarget, onDismissRequest = { expandedTarget = false }) {
                                eligibleTargets.forEach { target -> DropdownMenuItem(text = { Text(target.card.displayName()) }, onClick = { targetId = target.instanceId; expandedTarget = false }) }
                            }
                        }
                    }
                    if (armadaShip && card.allowedUpgradeSlots.isNotEmpty()) Text(stringResource(R.string.slots_label, card.allowedUpgradeSlots.groupingBy { it }.eachCount().entries.joinToString { "${it.value}× ${it.key.name.lowercase().replace('_', ' ')}" }), color = Color(0xFF9EACBC), style = MaterialTheme.typography.labelSmall)
                    if (card.kind == CardKind.ARMADA_UPGRADE && eligibleSlots.size > 1) {
                        var expandedSlot by remember { mutableStateOf(false) }
                        Box {
                            TextButton(onClick = { expandedSlot = true }) { Text(stringResource(R.string.slot_label, selectedSlot?.name?.replace('_', ' ') ?: stringResource(R.string.select))) }
                            DropdownMenu(expandedSlot, onDismissRequest = { expandedSlot = false }) {
                                eligibleSlots.forEach { slot -> DropdownMenuItem(text = { Text(slot.name.replace('_', ' ')) }, onClick = { selectedSlot = slot; expandedSlot = false }) }
                            }
                        }
                    }
                    if (requiresTarget && eligibleTargets.isEmpty()) Text(if (card.kind == CardKind.LEGION_UPGRADE) stringResource(R.string.add_unit_first) else stringResource(R.string.no_compatible_ship), color = Color(0xFFFF927F), style = MaterialTheme.typography.labelSmall)
                    if (compatibleSlotFull) Text(stringResource(R.string.slots_full), color = Color(0xFFFF927F), style = MaterialTheme.typography.labelSmall)
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Button(
                    enabled = (!requiresTarget || eligibleTargets.isNotEmpty()) && !alreadyAdded && !compatibleSlotFull && validSelection,
                    onClick = { onAdd(if (requiresTarget) targetId else null, selectedSlot) },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) { Text("+", fontWeight = FontWeight.Bold) }
                Text("$alreadyCount", color = if (alreadyCount > 0) Color(0xFFFFC857) else Color(0xFF5A6A7A), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/** Ratio d'affichage des cartes : les cartes unité Legion sont des bandeaux horizontaux
 * (paysage ~1.43) ; les autres (upgrades, vaisseaux, escadrons, commandants) sont en
 * portrait (~0.7). Le ratio suit le type de carte pour que chaque carte prenne toute
 * la largeur dans son propre format. */
private fun cardAspectRatio(card: CardDefinition): Float =
    if (card.kind == CardKind.LEGION_UNIT) 1.43f else 0.7f

@Composable
fun CardArtwork(card: CardDefinition, modifier: Modifier = Modifier) {
    val source: Any? = card.imagePath()?.let { "file:///android_asset/$it" } ?: card.imageUrl
    val ratio = cardAspectRatio(card)
    val sized = modifier.aspectRatio(ratio)
    if (source != null) AsyncImage(model = source, contentDescription = stringResource(R.string.icon_desc, card.displayName()), modifier = sized, contentScale = ContentScale.Crop)
    else Card(sized, shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF253344))) {
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF253344), Color(0xFF111820)))), contentAlignment = Alignment.Center) {
            Text(card.displayName().split(' ').take(2).joinToString("\n"), color = Color(0xFF8494A8), style = MaterialTheme.typography.labelSmall)
        }
    }
}
