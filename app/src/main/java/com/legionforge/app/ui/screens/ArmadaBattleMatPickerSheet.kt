package com.legionforge.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.legionforge.app.R
import com.legionforge.app.ui.theme.LegionColors

/** Sélecteur des tapsis spatiaux Armada + grille standard. Cibles >= 48dp. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArmadaBattleMatPickerSheet(
    current: ArmadaBattleMat,
    onPick: (ArmadaBattleMat) -> Unit,
    onDismiss: () -> Unit,
) {
    val ctx = LocalContext.current
    val mats = remember { ArmadaBattleMats.ALL }
    ModalBottomSheet(onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(), containerColor = LegionColors.Surface) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.armada_battlemat_title), color = LegionColors.Accent,
                fontWeight = FontWeight.Bold, fontSize = 18.sp)
            mats.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { mat ->
                        val bmp = remember(mat.assetPath) { loadAssetBitmap(ctx, mat.assetPath) }
                        Column(Modifier.weight(1f).clickable { onPick(mat) }
                            .background(LegionColors.SurfaceRaised, RoundedCornerShape(8.dp))
                            .border(if (mat.id == current.id) 2.dp else 1.dp,
                                if (mat.id == current.id) LegionColors.Accent else LegionColors.Border,
                                RoundedCornerShape(8.dp)).padding(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(Modifier.fillMaxWidth().height(56.dp)
                                .background(androidx.compose.ui.graphics.Color(0xFF0B0B0D), RoundedCornerShape(4.dp))) {
                                if (bmp != null) Image(bmp, contentDescription = null,
                                    modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.Crop)
                            }
                            Text(stringResource(nameRes(mat.id)),
                                color = if (mat.id == current.id) LegionColors.Accent else LegionColors.TextPrimary,
                                fontSize = 12.sp)
                        }
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(48.dp)
                .background(LegionColors.SurfaceRaised, RoundedCornerShape(8.dp))
                .clickable { onPick(ArmadaBattleMats.NONE) }
                .padding(12.dp), contentAlignment = Alignment.CenterStart) {
                Text(stringResource(R.string.armada_battlemat_none), color = LegionColors.TextPrimary)
            }
        }
    }
}

private fun nameRes(id: String): Int = when (id) {
    "armada_scarif" -> R.string.armada_battlemat_scarif
    "armada_hoth" -> R.string.armada_battlemat_hoth
    "armada_kaliida" -> R.string.armada_battlemat_kaliida
    "armada_coruscant" -> R.string.armada_battlemat_coruscant
    else -> R.string.armada_battlemat_none
}
