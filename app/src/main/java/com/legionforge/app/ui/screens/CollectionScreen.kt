package com.legionforge.app.ui.screens
import com.legionforge.app.ui.theme.LegionColors

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.legionforge.app.R
import com.legionforge.app.data.model.CardDefinition
import com.legionforge.app.data.repository.CollectionPack
import com.legionforge.app.data.repository.CollectionRepository
import com.legionforge.app.ui.viewmodel.ArmyBuilderViewModel

/**
 * Ma collection (Réglages) : cocher le matériel acheté PAR PACK (boîte d'extension),
 * avec l'image miniature de la boîte. Cocher un pack marque possédées toutes ses
 * cartes (union). Un onglet « Cartes » permet le coche carte par carte si besoin.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(onBack: () -> Unit, vm: ArmyBuilderViewModel = viewModel()) {
    val allCards by vm.allCatalog.collectAsState()
    val ownedPacks by CollectionRepository.ownedPacks.collectAsState()
    val ownedCards by CollectionRepository.ownedCards.collectAsState()
    val packs by CollectionRepository.packs.collectAsState()
    var search by remember { mutableStateOf("") }
    var showPacks by remember { mutableStateOf(true) } // onglet Packs / Cartes

    LaunchedEffect(Unit) { vm.loadAllCatalogForCollection() }

    val q = search.trim()
    val byId = allCards.associateBy { it.id }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.my_collection)) },
                navigationIcon = { TextButton(onClick = onBack) { Text("‹") } }
            )
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().background(LegionColors.Background).padding(pad)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "${ownedPacks.size} ${stringResource(R.string.my_collection_packs).lowercase()}",
                    color = LegionColors.Accent,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 2.dp)) {
                TabRow(selectedTabIndex = if (showPacks) 0 else 1) {
                    Tab(showPacks, onClick = { showPacks = true }, text = { Text(stringResource(R.string.my_collection_packs), fontSize = 13.sp) })
                    Tab(!showPacks, onClick = { showPacks = false }, text = { Text(stringResource(R.string.my_collection_cards, ownedCards.size), fontSize = 13.sp) })
                }
            }

            if (showPacks) {
                PackList(packs, ownedPacks, q)
            } else {
                CardList(allCards, byId, ownedCards, q)
            }
        }
    }
}

// ── vue Packs ─────────────────────────────────────────────────────
@Composable
private fun PackList(packs: List<CollectionPack>, ownedPacks: Set<String>, q: String) {
    if (packs.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.no_packs), color = LegionColors.TextTertiary)
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
        items(packs.size, key = { packs[it].id }) { i ->
            val pack = packs[i]
            if (q.isEmpty() || pack.name.contains(q, ignoreCase = true)) {
                PackRow(pack, pack.id in ownedPacks) { CollectionRepository.togglePack(pack.id) }
            }
        }
    }
}

@Composable
private fun PackRow(pack: CollectionPack, isOwned: Boolean, onToggle: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle),
        color = if (isOwned) Color(0xFF2A3A1E) else LegionColors.Surface
    ) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            val imgSrc = pack.imageName?.let { "file:///android_asset/cards/packs/$it" }
            Box(Modifier.size(width = 88.dp, height = 74.dp).clip(RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                if (imgSrc != null) {
                    AsyncImage(model = imgSrc, contentDescription = pack.name,
                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Text("📦", fontSize = 30.sp)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(pack.name, color = Color.White, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${pack.cards.size} cartes", color = LegionColors.TextTertiary, style = MaterialTheme.typography.labelSmall)
            }
            Checkbox(
                checked = isOwned, onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = if (isOwned) LegionColors.Success else LegionColors.Border, checkmarkColor = Color.Black)
            )
        }
    }
}

// ── vue Cartes (coche par carte) ──────────────────────────────────
@Composable
private fun CardList(allCards: List<CardDefinition>, byId: Map<String, CardDefinition>, ownedCards: Set<String>, q: String) {
    val filtered = if (q.isEmpty()) allCards else allCards.filter {
        it.displayName().contains(q, ignoreCase = true) || it.name.contains(q, ignoreCase = true) || it.factionId.contains(q, ignoreCase = true)
    }
    val grouped = filtered.sortedWith(compareBy({ it.gameSystem.ordinal }, { it.factionId }, { it.name.lowercase() }))
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
        items(grouped.size, key = { grouped[it].id }) { i ->
            val card = grouped[i]
            val isOwned = card.id in ownedCards
            CollectionRow(card, isOwned) { CollectionRepository.toggleCard(card.id) }
        }
    }
}

@Composable
private fun CollectionRow(card: CardDefinition, isOwned: Boolean, onToggle: () -> Unit) {
    val accent = FactionCardColor(card.factionId)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onToggle),
        color = if (isOwned) accent.copy(alpha = 0.12f) else LegionColors.Surface
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = isOwned,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = accent, checkmarkColor = Color.Black)
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(card.displayName(), color = Color.White, style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(kindLabel(card.kind), color = LegionColors.TextTertiary, style = MaterialTheme.typography.labelSmall)
                    if (card.kind.name == "LEGION_UNIT" && card.legionRank != null) {
                        Text(rankLabel(card.legionRank), color = rankColor(card.legionRank), style = MaterialTheme.typography.labelSmall)
                    }
                    Text("${card.points} pts", color = LegionColors.Accent, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}