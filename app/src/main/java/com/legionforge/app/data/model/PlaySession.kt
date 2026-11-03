package com.legionforge.app.data.model

import com.google.gson.Gson
import com.legionforge.app.data.nearby.SyncedUnitState

/**
 * Instantané complet d'une partie (mode solo) — permet la reprise après rotation de l'écran
 * ou fermeture de l'app. Persisté en JSON (Gson) dans la table `game_sessions`, clé = listId.
 *
 * NE capture PAS l'état 2 joueurs (sync Nearby éphémère : VP/round/tour re-synchronisés en live).
 * Séparé et testable : `toJson()`/`fromJson()` font un round-trip sans dépendance Room/UI.
 */
data class PlaySessionSnapshot(
    val schemaVersion: Int = 1,
    val listId: String? = null,
    val gameSystem: String? = null,
    val phase: String = "PREP",          // PREP | GAME
    val playPhase: String = "command",   // command | activation
    val round: Int = 1,
    val vp: Int = 0, val oppVp: Int = 0,
    val kills: Int = 0, val oppKills: Int = 0,
    val priority: String = "own",
    val passes: Int = 0, val oppPasses: Int = 0,
    val orderPool: Map<String, Int> = emptyMap(),
    val gameResult: String? = null,
    val gameLog: List<String> = emptyList(),
    val turn: String = "own",
    val activeObjectives: List<String> = emptyList(),
    val battleDeck: List<String> = emptyList(),
    val battleRevealed: Map<String, String> = emptyMap(),
    val missionRevealed: Map<String, String> = emptyMap(),
    val bluePlayer: String? = null,
    val modsLeft: Map<String, Int> = emptyMap(),
    val commandDeck: List<String> = emptyList(),
    val playedCommands: List<String> = emptyList(),
    val roundCommand: String? = null,
    val discardedCommands: List<String> = emptyList(),
    val soloOppRoundCommand: String? = null,
    val soloOppCommandDeck: List<String> = emptyList(),
    val soloOppListId: String? = null,
    val ownStates: Map<String, SyncedUnitState> = emptyMap(),
    val soloOppStates: Map<String, SyncedUnitState> = emptyMap()
) {
    fun toJson(): String = GSON.toJson(this)

    companion object {
        private val GSON = Gson()
        @JvmStatic
        fun fromJson(json: String): PlaySessionSnapshot = GSON.fromJson(json, PlaySessionSnapshot::class.java)
    }
}
