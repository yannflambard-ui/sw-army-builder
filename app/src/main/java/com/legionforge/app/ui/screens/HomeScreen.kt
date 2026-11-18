package com.legionforge.app.ui.screens
import com.legionforge.app.ui.theme.LegionColors

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.legionforge.app.R
import com.legionforge.app.data.model.GameSystem
import com.legionforge.app.ui.viewmodel.ArmyBuilderViewModel

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNewList: (GameSystem) -> Unit, onOpenList: (String) -> Unit, onSettings: () -> Unit, onSearch: () -> Unit, onWiki: () -> Unit, vm: ArmyBuilderViewModel = viewModel()) {
    val lists by vm.allLists.collectAsState()
    val loading by vm.loading.collectAsState()
    val catalogError by vm.catalogError.collectAsState()
    Scaffold(topBar = {
            TopAppBar(title = {
                Image(
                    painter = painterResource(R.drawable.ic_logo_app),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier.height(56.dp)
                )
            },
                actions = {
                    IconButton(onClick = onSettings) {
                        Text("\u2699", color = LegionColors.Accent, style = MaterialTheme.typography.titleMedium)
                    }
                })
        }) { pad ->
            Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(LegionColors.Surface, Color(0xFF080B12)))).padding(pad).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Barre de recherche compacte, bien visible
                Card(onClick = onSearch, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = LegionColors.SurfaceRaised)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("\uD83D\uDD0E", color = LegionColors.Accent, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.search_hint), color = Color(0xFF8F9BAD), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CompactGameTile(stringResource(R.string.game_legion), stringResource(R.string.legion_desc), GameSystem.LEGION_V2, Modifier.weight(1f), onClick = { onNewList(GameSystem.LEGION_V2) })
                    CompactGameTile(stringResource(R.string.game_armada), stringResource(R.string.armada_desc), GameSystem.ARMADA_V15, Modifier.weight(1f), onClick = { onNewList(GameSystem.ARMADA_V15) })
                }
                // Accès au wiki des règles officielles
                Card(onClick = onWiki, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = LegionColors.SurfaceRaised)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("\uD83D\uDCD6", color = LegionColors.Accent, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.wiki_rules), color = Color(0xFF8F9BAD), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            Text(stringResource(R.string.recent_lists), color = LegionColors.Accent, style = MaterialTheme.typography.labelLarge)
            if (lists.isEmpty()) Text(stringResource(R.string.saved_lists_hint), color = Color.LightGray)
            if (loading) {
                Spacer(Modifier.height(40.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp), color = LegionColors.Accent)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.loading_catalog), color = LegionColors.TextSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            if (catalogError != null) {
                Card(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = LegionColors.DangerSurface)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(stringResource(R.string.catalog_error), color = LegionColors.Accent, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text(catalogError ?: "", color = Color(0xFFFFC7B7), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            lists.forEach { list ->
                OutlinedButton(onClick = { onOpenList(list.id) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth()) {
                        Text(list.name, color = Color.White)
                        Text("${GameSystem.valueOf(list.gameSystem).label()} • ${list.factionId} • ${list.pointsLimit} pts", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.offline_mode), color = Color(0xFF8F9BAD), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun CompactGameTile(title: String, subtitle: String, system: GameSystem, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = LegionColors.SurfaceRaised)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(gameIconRes(system)),
                contentDescription = stringResource(R.string.icon_desc, title),
                modifier = Modifier.fillMaxWidth().height(96.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

fun GameSystem.label() = if (this == GameSystem.LEGION_V2) "LEGION V2" else "ARMADA V1.5"
