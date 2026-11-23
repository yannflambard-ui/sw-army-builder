package com.legionforge.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Plateau de dés animé et partagé (audit gameplay audit_gameplay_V1 point 4).
 *
 * Remplace les 3 présentations incompatibles (R/N/B, R/N/B+cover, N/B/R+secteur)
 * par UNE seule convention visuelle : pastille de couleur + glyphe de face.
 * Les dés apparaissent échelonnés (60 ms), ce qui donne la perception d'un lancer.
 *
 * @param rolls liste de (couleur, face). Couleur : "R"/"N"/"B".
 *              Face : Hit/Crit/Surge/Bloc/Vide ou hit/crit/acc/blank (Armada).
 */
@Composable
fun DiceTray(
    rolls: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
    staggerMs: Int = 60,
) {
    val rollKey = rolls.joinToString("|")
    var visibleCount by remember(rollKey) { mutableIntStateOf(rolls.size) }
    LaunchedEffect(rollKey) {
        visibleCount = 0
        rolls.indices.forEach { _ ->
            kotlinx.coroutines.delay(staggerMs.toLong())
            visibleCount++
        }
    }
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        rolls.forEachIndexed { i, (color, face) ->
            AnimatedVisibility(
                visible = i < visibleCount,
                enter = fadeIn(animationSpec = tween(150)) +
                    slideInVertically(animationSpec = tween(150)) { it / 2 },
            ) {
                DieChip(color, face)
            }
        }
    }
}

@Composable
private fun DieChip(color: String, face: String) {
    // Les faces de DÉFENSE (Bloc/Surge) sont blanches/grises, jamais bleues,
    // même si le dé provient d'un pool "B" (audit gameplay V2).
    val isDefense = face.equals("Bloc", true) || face.equals("Surge", true) || face.equals("Block", true)
    val effectiveColor = if (isDefense) "W" else color
    val bg by animateColorAsState(
        when (effectiveColor.uppercase()) {
            "R" -> Color(0xFFEF4444)
            "N" -> Color(0xFF374151)
            "B" -> Color(0xFF3B82F6)
            "W" -> Color(0xFFE5E7EB)
            else -> Color(0xFF6B7280)
        }, animationSpec = tween(150))
    val glyph = when (face.lowercase()) {
        "crit" -> "★"
        "hit" -> "✦"
        "surge" -> "⚡"
        "bloc", "block" -> "◆"
        "acc", "accuracy" -> "⊙"
        else -> "·"
    }
    val fg = if (effectiveColor.uppercase() == "B" || effectiveColor.uppercase() == "W") Color.Black else Color.White
    Box(Modifier.size(34.dp).background(bg, CircleShape).border(1.dp, fg.copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center) {
        Text(glyph, color = fg, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}
