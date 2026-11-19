package com.legionforge.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.legionforge.app.ui.theme.LegionColors
import com.legionforge.app.ui.theme.LegionType

/**
 * Bandeau des 5 cartes de mission au-dessus du plateau (HORS du Canvas), façon
 * playmat IRON SQUADRON. Les emplacements Objectif/Map/Secondaire affichent la
 * VRAIE carte choisie (image), cliquable pour en changer.
 */
@Composable
fun MissionCardRail(
    round: Int,
    victoryPoints: Int = 0,
    labels: Map<String, String>,
    selectedCards: Map<String, LegionBattleCard?> = emptyMap(),
    cardBitmaps: Map<String, ImageBitmap> = emptyMap(),
    onPickCard: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val scroll = rememberScrollState()
    Column(
        modifier.fillMaxWidth()
            .background(LegionColors.Background)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(scroll),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.Top,
        ) {
            MissionCardRailData.slots.forEach { slot ->
                val (c1, c2) = MissionCardRailData.colors(slot.kind)
                val w = if (slot.wide) 96.dp else 62.dp
                val h = if (slot.wide) 62.dp else 84.dp
                val card = selectedCards[slot.label]
                val clickable = card != null || slot.kind == MissionCardKind.COMMAND || slot.kind == MissionCardKind.COMMAND2
                val bmp = card?.imagePath()?.let { cardBitmaps[it] }
                Box(
                    Modifier.size(w, h)
                        .shadow(4.dp, RoundedCornerShape(6.dp))
                        .background(Brush.verticalGradient(listOf(c1, c2)), RoundedCornerShape(6.dp))
                        .border(2.dp, Color(0xFFBFC4C8), RoundedCornerShape(6.dp))
                        .then(if (clickable) Modifier.clickable { onPickCard(slot.label) } else Modifier)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (bmp != null) {
                        Image(bmp, contentDescription = card?.name, contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize())
                    } else {
                        Text(
                            card?.name ?: labels[slot.label] ?: slot.label,
                            color = Color.White,
                            fontSize = LegionType.Caption,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            lineHeight = LegionType.Caption * 1.1f,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(labels["round"] ?: "ROUND", color = LegionColors.TextSecondary, fontSize = LegionType.Caption)
            (1..5).forEach { n ->
                Box(
                    Modifier.size(24.dp)
                        .background(
                            if (n == round) LegionColors.Accent else LegionColors.SurfaceRaised,
                            RoundedCornerShape(4.dp),
                        )
                        .border(1.dp, if (n == round) LegionColors.AccentStrong else LegionColors.Border, RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(n.toString(), color = if (n == round) LegionColors.Background else LegionColors.TextSecondary,
                        fontSize = LegionType.Caption, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(labels["vp"] ?: "VP", color = LegionColors.TextSecondary, fontSize = LegionType.Caption)
            Text("$victoryPoints/12", color = LegionColors.Accent, fontSize = LegionType.Caption, fontWeight = FontWeight.Bold)
        }
    }
}
