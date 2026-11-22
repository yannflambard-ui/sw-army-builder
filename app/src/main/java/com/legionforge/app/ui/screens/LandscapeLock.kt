package com.legionforge.app.ui.screens

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Contrôle l'orientation d'un écran à plateau 2:1 (Legion / Armada 6'x3').
 *
 * - [enabled] = null  -> AUTO : suit l'orientation PHYSIQUE du téléphone.
 *   On pose explicitement [ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED] : sans
 *   cela, un verrou posé par un écran précédent RESTE en place et le portrait
 *   devient inatteignable (bug rapporté). UNSPECIFIED rend le contrôle au
 *   capteur et au réglage système.
 * - [enabled] = true  -> paysage forcé (bouton ▭).
 * - [enabled] = false -> portrait forcé (bouton ▯).
 *
 * L'orientation d'origine est RESTAURÉE à la sortie (DisposableEffect) pour ne
 * pas piéger le reste de l'app. Robolectric/Roborazzi : `LocalContext` n'est pas
 * une Activity -> le cast échoue et on ignore proprement.
 */
@Composable
fun ForceLandscapeWhileMounted(enabled: Boolean? = true) {
    val ctx = LocalContext.current
    DisposableEffect(ctx, enabled) {
        val activity = ctx as? Activity
        val previous = activity?.requestedOrientation
        val wanted = when (enabled) {
            null -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED // -1 : suit le téléphone
            true -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            false -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        if (activity != null && previous != wanted) {
            activity.requestedOrientation = wanted
        }
        onDispose {
            // Restaure TOUJOURS, y compris depuis le mode AUTO (qui avait pu
            // déverrouiller une orientation verrouillée ailleurs).
            if (activity != null && previous != null && activity.requestedOrientation == wanted) {
                activity.requestedOrientation = previous
            }
        }
    }
}
