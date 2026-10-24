package com.legionforge.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.legionforge.app.R
import com.legionforge.app.data.model.CardDefinition
import com.legionforge.app.data.model.GameSystem
import com.legionforge.app.data.model.CardKind

/** Recherche globale plein texte sur toutes les cartes (Legion + Armada) :
 *  nom, texte de règles, stats, mots-clés. Interface compacte.
 *  Cliquer un résultat ouvre une bottom sheet listant les variantes du même nom de base. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onCardClick: (CardDefinition) -> Unit,
    vm: com.legionforge.app.ui.viewmodel.ArmyBuilderViewModel
) {
    var query by remember { mutableStateOf("") }
    val results by vm.searchResults.collectAsState()
    val searching by vm.searching.collectAsState()
    var selected by remember { mutableStateOf<CardDefinition?>(null) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.search_title), style = MaterialTheme.typography.titleMedium) },
            navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.back)) } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0A0E15))
        )
    }) { pad ->
        Column(Modifier.fillMaxSize().background(Color(0xFF0A0E15)).padding(pad).padding(horizontal = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Barre de recherche compacte
            OutlinedTextField(
                value = query,
                onValueChange = { query = it; vm.searchCards(it) },
                placeholder = { Text(stringResource(R.string.search_placeholder), color = Color(0xFF5A6A7A), fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, lineHeight = 16.sp)
            )
            when {
                searching -> Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFFFFC857), strokeWidth = 2.dp) }
                results.isEmpty() && query.isNotBlank() -> Text(stringResource(R.string.no_results, query), color = Color(0xFF5A6A7A), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 12.dp))
                results.isNotEmpty() -> LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                    items(results) { card ->
                        SearchResultRow(card, onClick = { selected = card })
                    }
                }
            }
        }
    }

    // Bottom sheet : variantes du même nom de base + fiche de la carte sélectionnée
    selected?.let { sel ->
        val variants = results.filter { baseName(it) == baseName(sel) }
        AlertDialog(
            onDismissRequest = { selected = null },
            containerColor = Color(0xFF192330),
            title = { Text(baseName(sel), color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.variants, variants.size), color = Color(0xFF9EACBC), style = MaterialTheme.typography.labelSmall)
                    variants.forEach { v ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (v.id == sel.id) Color(0xFF2A3A4A) else Color(0xFF0A0E15)).clickable { onCardClick(v); selected = null }.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(v.displayName(), color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text("${kindLabel(v)} • ${gameLabel(v)}", color = Color(0xFF9EACBC), style = MaterialTheme.typography.labelSmall)
                            }
                            Text("${v.points} pts", color = Color(0xFFFFC857), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { selected = null }) { Text(stringResource(R.string.close), color = Color(0xFFFFC857)) } }
        )
    }
}

@Composable
private fun SearchResultRow(card: CardDefinition, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Image de la carte directement (au lieu du petit résumé texte).
            CardArtwork(card, Modifier.width(64.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(card.displayName(), color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(kindLabel(card), color = Color(0xFFFFC857), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text(gameLabel(card), color = Color(0xFF9EACBC), style = MaterialTheme.typography.labelSmall)
                }
            }
            Text("${card.points} pts", color = Color(0xFFFFC857), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(6.dp))
            Text("\u203A", color = Color(0xFF5A6A7A), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

/** Nom de base : retire le suffixe de variante entre parenthèses (ex "Wookiee Warriors (Freedom Fighters)" → "Wookiee Warriors"). */
private fun baseName(card: CardDefinition): String = card.name.substringBefore(" (").trim()

@Composable
private fun kindLabel(card: CardDefinition): String = when (card.kind) {
    CardKind.LEGION_UNIT -> stringResource(R.string.kind_unit)
    CardKind.LEGION_UPGRADE -> stringResource(R.string.kind_upgrade)
    CardKind.ARMADA_SHIP -> stringResource(R.string.kind_ship)
    CardKind.ARMADA_SQUADRON -> stringResource(R.string.kind_squadron)
    CardKind.ARMADA_UPGRADE -> stringResource(R.string.kind_upgrade)
    CardKind.COMMANDER -> stringResource(R.string.kind_commander)
    else -> "Carte"
}
private fun gameLabel(card: CardDefinition): String = if (card.gameSystem == GameSystem.LEGION_V2) "LEGION V2" else "ARMADA V1.5"
