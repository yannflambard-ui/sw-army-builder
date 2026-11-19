package com.legionforge.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
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

/** Sélecteur de carte de terrain : toutes les cartes pré-construites + « aucun ». */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerrainPickerSheet(
    current: TerrainCard?,
    cards: List<Pair<TerrainCard, String>>,
    onPick: (TerrainCard?) -> Unit,
    onDismiss: () -> Unit,
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
            cards.forEach { (card, label) ->
                TerrainRow(label, current?.id == card.id, card.palette) { onPick(card) }
            }
            TerrainRow(nameNone, current == null, null) { onPick(null) }
        }
    }
}

@Composable
private fun TerrainRow(name: String, selected: Boolean, palette: TerrainPalette?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .heightIn(min = 48.dp) // cible tactile >= 48dp
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
        // Pastille 3 teintes : distingue les 8 planètes d'un coup d'œil.
        if (palette != null) {
            listOf(palette.base, palette.shade, palette.accent).forEach { c ->
                androidx.compose.foundation.layout.Box(
                    Modifier.size(14.dp).background(c, CircleShape)
                )
                Spacer(Modifier.width(3.dp))
            }
            Spacer(Modifier.width(6.dp))
        }
        Text(name, color = if (selected) LegionColors.Background else LegionColors.TextPrimary,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, fontSize = LegionType.Body)
    }
}
