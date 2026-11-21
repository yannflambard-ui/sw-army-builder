package com.legionforge.app.ui.screens

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Force le paysage pendant la durée d'un écran à plateau 2:1 (Legion 6'x3',
 * Armada 6'x3'). Sans cela, en portrait le tapis occupe toute la largeur mais
 * ~28 % de la hauteur et l'image paraît floue alors que la source est déjà
 * sur-échantillonnée (1408px affichés à ~390px).
 *
 * L'orientation d'origine est RESTAURÉE à la sortie (DisposableEffect), pour ne
 * pas piéger le reste de l'app en paysage. Robolectric/Roborazzi : l'Activity
 * n'est pas une Activity réelle -> le cast échoue et on ignore proprement.
 */
@Composable
fun ForceLandscapeWhileMounted(enabled: Boolean = true) {
    val ctx = LocalContext.current
    DisposableEffect(ctx, enabled) {
        val activity = ctx as? Activity
        val previous = activity?.requestedOrientation
        if (enabled && activity != null && previous != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else if (!enabled && activity != null && previous != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        onDispose {
            if (activity != null && previous != null) {
                activity.requestedOrientation = previous
            }
        }
    }
}
