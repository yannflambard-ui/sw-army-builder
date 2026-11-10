package com.legionforge.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.legionforge.app.data.model.BuilderListEntity
import com.legionforge.app.data.model.ListEntry
import com.legionforge.app.data.nearby.PlaySyncManager
import com.legionforge.app.data.nearby.ShareMode
import com.legionforge.app.data.nearby.SyncedUnitState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** ViewModel du mode play 2 joueurs : sync live via Nearby. */
class PlaySyncViewModel(application: Application) : AndroidViewModel(application) {
    private val gson = Gson()
    private val sync = PlaySyncManager(application)
    private val journalDao by lazy { com.legionforge.app.data.local.LegionForgeDatabase.getInstance(application).playJournalDao() }
    // Partie en cours (clé du journal). Posée par l'écran de jeu.
    @Volatile var journalListId: String? = null
    // Compteur monotone du journal (un seq unique par entrée, même si deux mutations tombent dans la même milliseconde).
    private val journalSeq = java.util.concurrent.atomic.AtomicLong(0)

    val mode: StateFlow<ShareMode> = sync.mode
    val endpoints: StateFlow<List<com.legionforge.app.data.nearby.DiscoveredEndpoint>> = sync.endpoints
    val connectedEndpoint: StateFlow<String?> = sync.connectedEndpoint
    val error: StateFlow<String?> = sync.error
    val receivedRound: StateFlow<Int?> = sync.receivedRound
    val receivedUnits: StateFlow<List<SyncedUnitState>> = sync.receivedUnits
    val receivedDeploymentDone: StateFlow<Boolean?> = sync.receivedDeploymentDone
    val receivedRoundCard: StateFlow<String?> = sync.receivedRoundCard
    val receivedTurn: StateFlow<String?> = sync.receivedTurn
    val receivedGameState: StateFlow<com.legionforge.app.data.nearby.GameStatePayload?> = sync.receivedGameState
    val playerId: String get() = sync.playerId
    // Déploiement terminé (Legion setup) : mon état + état adverse reçu.
    private val _deploymentDone = MutableStateFlow(false)
    val deploymentDone: StateFlow<Boolean> = _deploymentDone.asStateFlow()
    // Store d'état de jeu : mon armée (éditable, diffusée) + armée adverse (reçue, lecture seule).
    private val _ownStates = MutableStateFlow<Map<String, SyncedUnitState>>(emptyMap())
    val ownStates: StateFlow<Map<String, SyncedUnitState>> = _ownStates.asStateFlow()
    private val _oppStates = MutableStateFlow<Map<String, SyncedUnitState>>(emptyMap())
    val oppStates: StateFlow<Map<String, SyncedUnitState>> = _oppStates.asStateFlow()

    private val _opponentListJson = MutableStateFlow<String?>(null)
    val opponentListJson: StateFlow<String?> = _opponentListJson.asStateFlow()

    // Cache de la dernière liste envoyée, pour pouvoir la renvoyer sur demande.
    private var lastList: BuilderListEntity? = null
    private var lastEntries: List<ListEntry> = emptyList()

    init {
        viewModelScope.launch {
            sync.receivedListJson.collect { json -> _opponentListJson.value = json }
        }
        // Quand l'autre joueur demande la synchro, on lui renvoie notre liste.
        viewModelScope.launch {
            sync.listRequested.collect { requested ->
                if (requested) {
                    val l = lastList
                    if (l != null) sendList(l, lastEntries)
                    sync.consumeListRequest()
                }
            }
        }
        // Applique les états reçus de l'adversaire dans le store oppStates (remplacement complet :
        // chaque broadcast envoie l'état complet de l'unité).
        viewModelScope.launch {
            sync.receivedUnits.collect { units ->
                val m = _oppStates.value.toMutableMap()
                // Préfixe "B:" sur les clés adverses → disjointes de mes UUID ("A:"), évite la collision
                // d'état si les 2 joueurs partagent la même liste (mêmes UUID). Cohérent avec oppEntries.
                units.forEach { st -> val pid = "B:${st.instanceId}"; m[pid] = st.copy(instanceId = pid) }
                _oppStates.value = m
            }
        }
    }

    fun startHost() = sync.startHost()
    fun startClient() = sync.startClient()
    fun connectTo(endpointId: String) = sync.connectTo(endpointId)
    fun stop() = sync.stop()

    fun sendRound(round: Int) = sync.sendRound(round)
    fun sendState(units: List<SyncedUnitState>) = sync.sendState(units)

    /** Demande à l'autre joueur de renvoyer sa liste (synchro manuelle). */
    fun requestList() = sync.requestList()

    /** Sérialise la liste courante et l'envoie à l'autre téléphone. */
    fun sendList(list: BuilderListEntity?, entries: List<ListEntry>) {
        val l = list ?: return
        lastList = l
        lastEntries = entries
        val payload = ShareListPayload(
            name = l.name, gameSystem = l.gameSystem, factionId = l.factionId,
            pointsLimit = l.pointsLimit,
            entries = entries.map { e -> ShareEntry(e.card.id, e.parentInstanceId, e.quantity, e.chosenSlot?.name, e.instanceId) }
        )
        sync.sendList(gson.toJson(payload))
    }

    /** Met à jour l'état d'une de MES unités et diffuse le changement à l'adversaire. */
    fun updateOwnState(id: String, transform: (SyncedUnitState) -> SyncedUnitState) {
        val cur = _ownStates.value[id] ?: SyncedUnitState(id)
        val next = transform(cur)
        _ownStates.value = _ownStates.value + (id to next)
        journalAppend("state", gson.toJson(listOf(next)))
        if (sync.mode.value == ShareMode.CONNECTED) sync.sendState(listOf(next))
    }

    /** Journal append-only : chaque mutation sortante est écrite avant envoi (rejouée à la reconnexion). */
    private fun journalAppend(kind: String, payload: String) {
        val lid = journalListId ?: return
        viewModelScope.launch {
            journalDao.append(com.legionforge.app.data.model.PlayJournalEntity(
                listId = lid, seq = journalSeq.incrementAndGet(), kind = kind, payload = payload, createdAt = System.currentTimeMillis()))
        }
    }

    /** Reconnexion : rejoue le journal non acquitté, puis renvoie l'état complet (filet de sécurité). */
    suspend fun replayJournal() {
        val lid = journalListId ?: return
        val pending = journalDao.pending(lid)
        pending.forEach { e ->
            when (e.kind) {
                "state" -> {
                    val units = gson.fromJson(e.payload, Array<SyncedUnitState>::class.java).toList()
                    sync.sendState(units)
                }
            }
        }
        if (pending.isNotEmpty()) journalDao.ack(lid, pending.last().seq)
    }

    /** Fin de partie / retour setup : purge le journal de la partie. */
    fun clearJournal() {
        val lid = journalListId ?: return
        viewModelScope.launch { journalDao.clear(lid) }
    }

    /** Met à jour l'état d'une unité ADVERSE (reçue) — lecture seule, pas de broadcast. */
    fun updateOppState(id: String, transform: (SyncedUnitState) -> SyncedUnitState) {
        val cur = _oppStates.value[id] ?: SyncedUnitState(id)
        _oppStates.value = _oppStates.value + (id to transform(cur))
    }

    /** Remplace l'état complet d'une de MES unités et diffuse le changement à l'adversaire. */
    fun setOwnState(id: String, next: SyncedUnitState) {
        _ownStates.value = _ownStates.value + (id to next)
        journalAppend("state", gson.toJson(listOf(next)))
        if (sync.mode.value == ShareMode.CONNECTED) sync.sendState(listOf(next))
    }

    /** Restaure les états ADVERSES depuis une partie sauvegardée (lecture seule). */
    fun restoreOppStates(states: Map<String, SyncedUnitState>) { _oppStates.value = states }

    /** Restaure MES états d'unités depuis une partie sauvegardée (aucun broadcast). */
    fun restoreOwnStates(states: Map<String, SyncedUnitState>) {
        _ownStates.value = states
    }

    /** Marque mon déploiement terminé (ou non) et le diffuse à l'adversaire. */
    fun setDeploymentDone(done: Boolean) {
        _deploymentDone.value = done
        if (sync.mode.value == ShareMode.CONNECTED) sync.sendDeploymentDone(done)
    }

    /** Envoie la carte de commandement jouée ce round à l'adversaire. */
    fun sendRoundCard(cardId: String?) {
        if (sync.mode.value == ShareMode.CONNECTED) sync.sendRoundCard(cardId)
    }

    /** Envoie l'état de jeu partagé (round, VP, tour, objectifs, phase). */
    fun sendGameState(state: com.legionforge.app.data.nearby.GameStatePayload) {
        if (sync.mode.value == ShareMode.CONNECTED) sync.sendGameState(state)
    }

    /** Renvoie uniquement mon armée (états d'unités) à l'adversaire. Utilisé au resync auto. */
    fun resyncOwnState() {
        if (sync.mode.value == ShareMode.CONNECTED) sync.sendState(_ownStates.value.values.toList())
    }

    /** Envoie qui active ("A" ou "B"). */
    fun sendTurn(turnOwner: String) {
        if (sync.mode.value == ShareMode.CONNECTED) sync.sendTurn(turnOwner)
    }

    override fun onCleared() {
        sync.stop()
        super.onCleared()
    }
}
