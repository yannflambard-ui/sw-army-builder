package com.legionforge.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.legionforge.app.data.model.BuilderListEntity
import com.legionforge.app.data.model.ListEntry
import com.legionforge.app.data.nearby.NearbyShareManager
import com.legionforge.app.data.nearby.ShareMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Payload de partage d'une liste (sérialisé en JSON, envoyé via Nearby). */
data class ShareListPayload(
    val name: String = "",
    val gameSystem: String = "",
    val factionId: String = "",
    val pointsLimit: Int = 0,
    val entries: List<ShareEntry> = emptyList(),
)

data class ShareEntry(
    val cardId: String = "",
    val parentInstanceId: String? = null,
    val quantity: Int = 0,
    val chosenSlot: String?,
    val instanceId: String? = null,
    val parentIndex: Int? = null,
)

class ShareViewModel(application: Application) : AndroidViewModel(application) {
    private val gson = Gson()
    init { NearbyShareManager.init(application) }
    private val nearby = NearbyShareManager

    val mode: StateFlow<ShareMode> = nearby.mode
    val endpoints: StateFlow<List<com.legionforge.app.data.nearby.DiscoveredEndpoint>> = nearby.endpoints
    val connectedEndpoint: StateFlow<String?> = nearby.connectedEndpoint
    val error: StateFlow<String?> = nearby.error

    private val _receivedList = MutableStateFlow<ShareListPayload?>(null)
    val receivedList: StateFlow<ShareListPayload?> = _receivedList.asStateFlow()

    private val _sent = MutableStateFlow(false)
    val sent: StateFlow<Boolean> = _sent.asStateFlow()

    init {
        viewModelScope.launch {
            nearby.receivedPayload.collect { json ->
                if (json != null) {
                    _receivedList.value = try { gson.fromJson(json, ShareListPayload::class.java) } catch (_: Exception) { null }
                }
            }
        }
    }

    fun startHost() = nearby.startAdvertising()
    fun startClient() = nearby.startDiscovery()
    fun connectTo(endpointId: String) = nearby.connectTo(endpointId)
    fun stop() = nearby.stop()

    /** Sérialise la liste courante et l'envoie à l'endpoint connecté. */
    fun shareList(list: BuilderListEntity?, entries: List<ListEntry>) {
        val l = list ?: return
        // Un seul JSON : les parents (vaisseaux, unités) d'abord, chaque amélioration juste après
        // son parent. parentIndex = position du parent DANS cette liste ordonnée.
        val parents = entries.filter { it.parentInstanceId == null }
        val ordered = parents.flatMap { p ->
            listOf(p) + entries.filter { it.parentInstanceId == p.instanceId }
        }
        // Entrées orphelines (parent absent) : envoyées à la fin, sans parent.
        val orphans = entries.filter { e -> e !in ordered }
        val all = ordered + orphans
        val indexById = all.withIndex().associate { it.value.instanceId to it.index }
        val payload = ShareListPayload(
            name = l.name,
            gameSystem = l.gameSystem,
            factionId = l.factionId,
            pointsLimit = l.pointsLimit,
            entries = all.map { e ->
                ShareEntry(e.card.id, e.parentInstanceId, e.quantity, e.chosenSlot?.name, e.instanceId,
                    parentIndex = e.parentInstanceId?.let { indexById[it] })
            }
        )
        nearby.sendList(gson.toJson(payload))
        _sent.value = true
    }

    fun clearReceived() {
        _receivedList.value = null
        _sent.value = false
    }

    override fun onCleared() {
        nearby.stop()
        super.onCleared()
    }
}
