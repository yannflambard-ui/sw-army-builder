package com.legionforge.app.ui.screens

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.legionforge.app.R
import com.legionforge.app.ui.theme.LegionColors
import com.legionforge.app.ui.theme.LegionType

/**
 * Sélecteur des battle mats planète (vraies images) + « plateau vectoriel ».
 * Grille 2 colonnes de vignettes 2:1, cible tactile >= 48dp.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BattleMatPickerSheet(
    current: BattleMat,
    onPick: (BattleMat) -> Unit,
    onDismiss: () -> Unit,
) {
    val ctx = LocalContext.current
    val mats = remember { BattleMats.ALL.filter { it != BattleMats.NONE } }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = LegionColors.Surface,
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.battlemat_title), color = LegionColors.Accent,
                fontWeight = FontWeight.Bold, fontSize = LegionType.Title)
            mats.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { mat ->
                        BattleMatTile(ctx, mat, current.id == mat.id, Modifier.weight(1f)) { onPick(mat) }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            // Plateau vectoriel d'origine (aucune image).
            Box(
                Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .background(LegionColors.SurfaceRaised, RoundedCornerShape(8.dp))
                    .border(if (current == BattleMats.NONE) 2.dp else 1.dp,
                        if (current == BattleMats.NONE) LegionColors.Accent else Color.Transparent,
                        RoundedCornerShape(8.dp))
                    .clickable { onPick(BattleMats.NONE) }
                    .padding(12.dp), contentAlignment = Alignment.CenterStart,
            ) {
                Text(stringResource(R.string.battlemat_none), color = LegionColors.TextPrimary,
                    fontSize = LegionType.Body)
            }
        }
    }
}

@Composable
private fun BattleMatTile(
    ctx: Context,
    mat: BattleMat,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val bmp = remember(mat.assetPath) { loadLegionAssetBitmap(ctx, mat.assetPath) }
    Column(
        modifier.clickable(onClick = onClick)
            .background(LegionColors.SurfaceRaised, RoundedCornerShape(8.dp))
            .border(if (selected) 2.dp else 1.dp,
                if (selected) LegionColors.Accent else LegionColors.Border, RoundedCornerShape(8.dp))
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(56.dp).background(Color(0xFF0B0B0D), RoundedCornerShape(4.dp))) {
            if (bmp != null) {
                Image(bmp, contentDescription = null, modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Crop)
            }
        }
        Text(stringResource(mat.nameRes()), color = if (selected) LegionColors.Accent else LegionColors.TextPrimary,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, fontSize = LegionType.Caption,
            modifier = Modifier.padding(horizontal = 2.dp))
    }
}

private fun BattleMat.nameRes(): Int = when (id) {
    "battlemat_tatooine" -> R.string.battlemat_tatooine
    "battlemat_hoth" -> R.string.battlemat_hoth
    "battlemat_endor" -> R.string.battlemat_endor
    "battlemat_scarif" -> R.string.battlemat_scarif
    "battlemat_naboo" -> R.string.battlemat_naboo
    "battlemat_geonosis" -> R.string.battlemat_geonosis
    else -> R.string.battlemat_none
}
