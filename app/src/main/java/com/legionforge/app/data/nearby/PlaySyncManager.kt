package com.legionforge.app.data.nearby

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** État d'une unité en mode play, synchronisé entre les 2 téléphones. */
data class SyncedUnitState(
    val instanceId: String,
    val hull: Int? = null,          // Armada ship / squadron (valeur restante)
    val wounds: Int? = null,        // Legion unit (valeur restante)
    val tokens: List<String> = emptyList(),   // Legion action tokens
    val suppressions: Int? = null,  // Legion
    val shields: List<Int> = emptyList(),     // Armada shields [F,P,S,R] (+aux si huge)
    val speed: Int? = null,         // Armada ship (vitesse courante)
    val defTokens: Map<String, Boolean> = emptyMap(),  // Armada defense tokens (clé "name_i")
    val orderTokens: Map<String, Int> = emptyMap(),    // Armada pions d'ordre (clé = nom commande)
    val commandOrder: String? = null,  // Armada roue de commandement (nom de l'ordre)
    val activeCrits: List<String> = emptyList(),  // noms des dégâts critiques actifs
    val usedUpgrades: Set<String> = emptySet(),   // ids des upgrades marqués utilisés
    val activated: Boolean? = null,  // Armada squadron (activé/désactivé)
    val cover: Int? = null,           // Legion couverture (0/1/2)
    val defeated: Boolean? = null,    // Legion unité vaincue
    val vehicleDamage: List<String> = emptyList()  // Legion véhicule : états (immobilized, weapon)
)

/** Payload de synchronisation d'état de jeu (envoyé via Nearby). */
data class PlaySyncPayload(
    val type: String,           // "round" | "state" | "list" | "request"
    val round: Int? = null,
    val units: List<SyncedUnitState> = emptyList(),
    val listJson: String? = null // liste de l'armée adverse (sérialisée)
)

/**
 * Gestionnaire de synchronisation du mode play 2 joueurs.
 * Réutilise NearbyShareManager (singleton) pour le transport, et sérialise l'état de jeu en JSON.
 */
class PlaySyncManager(context: Context) {
    // Singleton partagé : un seul client Nearby pour tout l'app (sinon les écrans
    // Share/2JOUEURS se percutent → 8001/8002 « déjà hôte/client »).
    init { NearbyShareManager.init(context) }
    private val nearby = NearbyShareManager
    private val gson = Gson()

    val mode: StateFlow<ShareMode> = nearby.mode
    val endpoints: StateFlow<List<DiscoveredEndpoint>> = nearby.endpoints
    val connectedEndpoint: StateFlow<String?> = nearby.connectedEndpoint
    val error: StateFlow<String?> = nearby.error

    private val _receivedRound = MutableStateFlow<Int?>(null)
    val receivedRound: StateFlow<Int?> = _receivedRound.asStateFlow()

    private val _receivedUnits = MutableStateFlow<List<SyncedUnitState>>(emptyList())
    val receivedUnits: StateFlow<List<SyncedUnitState>> = _receivedUnits.asStateFlow()

    private val _receivedListJson = MutableStateFlow<String?>(null)
    val receivedListJson: StateFlow<String?> = _receivedListJson.asStateFlow()

    // Signal reçu : l'autre joueur demande qu'on lui renvoie notre liste (synchro manuelle).
    private val _listRequested = MutableStateFlow(false)
    val listRequested: StateFlow<Boolean> = _listRequested.asStateFlow()

    init {
        // Décode les payloads reçus.
        CoroutineScope(Dispatchers.Main).launch {
            nearby.receivedPayload.collect { json ->
                if (json == null) return@collect
                val payload = try { gson.fromJson(json, PlaySyncPayload::class.java) } catch (_: Exception) { null } ?: return@collect
                when (payload.type) {
                    "round" -> _receivedRound.value = payload.round
                    "state" -> _receivedUnits.value = payload.units
                    "list" -> _receivedListJson.value = payload.listJson
                    "request" -> _listRequested.value = true
                }
            }
        }
    }

    fun startHost() = nearby.startAdvertising()
    fun startClient() = nearby.startDiscovery()
    fun connectTo(endpointId: String) = nearby.connectTo(endpointId)
    fun stop() = nearby.stop()

    fun sendRound(round: Int) {
        nearby.sendList(gson.toJson(PlaySyncPayload(type = "round", round = round)))
    }

    fun sendState(units: List<SyncedUnitState>) {
        nearby.sendList(gson.toJson(PlaySyncPayload(type = "state", units = units)))
    }

    fun sendList(json: String) {
        nearby.sendList(gson.toJson(PlaySyncPayload(type = "list", listJson = json)))
    }

    /** Demande à l'autre joueur de renvoyer sa liste (synchro manuelle). */
    fun requestList() {
        nearby.sendList(gson.toJson(PlaySyncPayload(type = "request")))
    }
}
