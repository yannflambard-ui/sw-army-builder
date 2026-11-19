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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.legionforge.app.ui.theme.LegionColors
import com.legionforge.app.ui.theme.LegionType

/** Sélecteur de carte de mission pour un emplacement du bandeau. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionCardPickerSheet(
    title: String,
    cards: List<LegionBattleCard>,
    current: LegionBattleCard?,
    onPick: (LegionBattleCard) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = LegionColors.Surface,
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = LegionColors.Accent, fontWeight = FontWeight.Bold, fontSize = LegionType.Title)
            cards.forEach { card ->
                val selected = current?.name == card.name
                Row(
                    Modifier.fillMaxWidth()
                        .clickable { onPick(card) }
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
                    Text(card.name, color = if (selected) LegionColors.Background else LegionColors.TextPrimary,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, fontSize = LegionType.Body)
                }
            }
        }
    }
}
