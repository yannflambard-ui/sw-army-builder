package com.legionforge.app.ui.screens
import com.legionforge.app.ui.theme.LegionColors

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.legionforge.app.R
import com.legionforge.app.data.model.BuilderListEntity
import com.legionforge.app.data.model.ListEntry
import com.legionforge.app.data.nearby.ShareMode
import com.legionforge.app.ui.viewmodel.ShareListPayload
import com.legionforge.app.ui.viewmodel.ShareEntry
import com.legionforge.app.ui.viewmodel.ShareViewModel
import com.legionforge.app.util.rememberNearbyPermissionAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareScreen(
    list: BuilderListEntity?,
    entries: List<ListEntry>,
    onImport: (ShareListPayload) -> Unit,
    onBack: () -> Unit,
    vm: ShareViewModel = viewModel()
) {
    val mode by vm.mode.collectAsState()
    val endpoints by vm.endpoints.collectAsState()
    val connected by vm.connectedEndpoint.collectAsState()
    val error by vm.error.collectAsState()
    val received by vm.receivedList.collectAsState()
    val sent by vm.sent.collectAsState()
    val runWithPermission = rememberNearbyPermissionAction()

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.share_title), style = MaterialTheme.typography.titleMedium) },
            navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.back)) } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = LegionColors.Background)
        )
    }) { pad ->
        Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(LegionColors.Surface, Color(0xFF080B12)))).padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Choix du mode : Hôte ou Client
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { runWithPermission { vm.startHost() } }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (mode == ShareMode.ADVERTISING) LegionColors.Accent else LegionColors.SurfaceRaised)) {
                    Text(stringResource(R.string.share_host), color = if (mode == ShareMode.ADVERTISING) LegionColors.Background else Color.White, fontWeight = FontWeight.Bold)
                }
                Button(onClick = { runWithPermission { vm.startClient() } }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (mode == ShareMode.DISCOVERING) LegionColors.Accent else LegionColors.SurfaceRaised)) {
                    Text(stringResource(R.string.share_client), color = if (mode == ShareMode.DISCOVERING) LegionColors.Background else Color.White, fontWeight = FontWeight.Bold)
                }
            }

            // Statut
            when (mode) {
                ShareMode.ADVERTISING -> StatusLine(stringResource(R.string.share_hosting), LegionColors.Accent)
                ShareMode.DISCOVERING -> StatusLine(stringResource(R.string.share_discovering), Color(0xFF4FC3F7))
                ShareMode.CONNECTED -> StatusLine(stringResource(R.string.share_connected), LegionColors.Success)
                ShareMode.IDLE -> StatusLine("—", LegionColors.TextTertiary)
            }
            error?.let { StatusLine(it, LegionColors.Danger) }

            // Client : liste des hôtes découverts
            if (mode == ShareMode.DISCOVERING) {
                if (endpoints.isEmpty()) {
                    Text(stringResource(R.string.share_no_endpoints), color = LegionColors.TextTertiary, style = MaterialTheme.typography.bodySmall)
                }
                endpoints.forEach { ep ->
                    Card(onClick = { runWithPermission { vm.connectTo(ep.endpointId) } }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = LegionColors.SurfaceRaised)) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(ep.name, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.share_connect), color = LegionColors.Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Connecté : envoyer la liste
            if (mode == ShareMode.CONNECTED) {
                Button(onClick = { vm.shareList(list, entries) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = LegionColors.Accent)) {
                    Text(if (sent) stringResource(R.string.share_sent) else stringResource(R.string.share_send), color = LegionColors.Background, fontWeight = FontWeight.Bold)
                }
            }

            // Liste reçue : importer
            received?.let { payload ->
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = LegionColors.SuccessSurface)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.share_received), color = LegionColors.Success, fontWeight = FontWeight.Bold)
                        Text((payload.name as String?) ?: "", color = Color.White, style = MaterialTheme.typography.titleSmall)
                        Text("${payload.gameSystem} • ${payload.factionId} • ${payload.pointsLimit} pts • ${(payload.entries as List<ShareEntry>?)?.size ?: 0} entrées", color = LegionColors.TextTertiary, fontSize = 11.sp)
                        Button(onClick = { onImport(payload); vm.clearReceived() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = LegionColors.Success)) {
                            Text(stringResource(R.string.share_import), color = LegionColors.Background, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))
            if (mode != ShareMode.IDLE) {
                OutlinedButton(onClick = { vm.stop() }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.share_stop), color = LegionColors.Danger)
                }
            }
        }
    }
}

@Composable
private fun StatusLine(text: String, color: Color) {
    Text(text, color = color, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
}
