package com.legionforge.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.legionforge.app.ui.theme.LegionColors
import com.legionforge.app.ui.theme.LegionType

/** Sélecteur de carte de terrain : 2 dioramas pré-construits + « aucun ». */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerrainPickerSheet(
    current: TerrainCard?,
    onPick: (TerrainCard?) -> Unit,
    onDismiss: () -> Unit,
    nameMonument: String,
    nameRuins: String,
    nameNone: String,
    title: String,
    coverHint: String,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = LegionColors.Surface,
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = LegionColors.Accent, fontWeight = FontWeight.Bold, fontSize = LegionType.Title)
            Text(coverHint, color = LegionColors.TextSecondary, fontSize = LegionType.Caption)
            TerrainRow(nameMonument, current?.id == TerrainCards.MONUMENT.id) { onPick(TerrainCards.MONUMENT) }
            TerrainRow(nameRuins, current?.id == TerrainCards.RUINS.id) { onPick(TerrainCards.RUINS) }
            TerrainRow(nameNone, current == null) { onPick(null) }
        }
    }
}

@Composable
private fun TerrainRow(name: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                Brush.horizontalGradient(
                    if (selected) listOf(LegionColors.AccentMuted, LegionColors.Accent)
                    else listOf(LegionColors.SurfaceRaised, LegionColors.SurfaceRaised),
                ),
                RoundedCornerShape(8.dp),
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name, color = if (selected) LegionColors.Background else LegionColors.TextPrimary,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, fontSize = LegionType.Body)
    }
}
