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
 * - [enabled] = null  -> SUIT L'ORIENTATION DU TÉLÉPHONE : aucun verrouillage posé,
 *   le système reste libre de changer `Configuration.orientation` quand on tourne
 *   l'appareil. C'est le mode par défaut voulu par l'utilisateur (portrait ET
 *   paysage fonctionnels).
 * - [enabled] = true  -> paysage forcé (bouton ▭).
 * - [enabled] = false -> portrait forcé (bouton ▯).
 *
 * L'orientation d'origine est RESTAURÉE à la sortie (DisposableEffect) pour ne pas
 * piéger le reste de l'app. Robolectric/Roborazzi : `LocalContext` n'est pas une
 * Activity -> le cast échoue et on ignore proprement.
 */
@Composable
fun ForceLandscapeWhileMounted(enabled: Boolean? = true) {
    val ctx = LocalContext.current
    DisposableEffect(ctx, enabled) {
        if (enabled == null) {
            // Mode AUTO : ne RIEN verrouiller, sinon le téléphone ne peut plus
            // signaler sa rotation et le portrait devient inatteignable.
            onDispose { }
        } else {
        val activity = ctx as? Activity
        val previous = activity?.requestedOrientation
        val wanted = if (enabled) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                     else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        if (activity != null && previous != wanted) {
            activity.requestedOrientation = wanted
        }
        onDispose {
            if (activity != null && previous != null) {
                activity.requestedOrientation = previous
            }
        }
        }
    }
}
