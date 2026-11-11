package com.legionforge.app.data.nearby

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** État global du partage Nearby Connections. */
enum class ShareMode { IDLE, ADVERTISING, DISCOVERING, CONNECTED }

data class DiscoveredEndpoint(val endpointId: String, val name: String)

/**
 * Gestionnaire Nearby Connections — SINGLETON partagé par DIX points d'entrée
 * (ShareViewModel du partage ET PlaySyncViewModel du mode 2 joueurs).
 *
 * Nearby Connections n'autorise qu'UN SEUL client actif par application : si deux
 * écrans instancient chacun un client, leur startAdvertising/startDiscovery se
 * percutent -> erreurs « déjà hôte/client » (8001/8002) et sessions perdues
 * (8034/8038). En partageant un unique ConnectionsClient (ce singleton), le mode
 * 2 joueurs se comporte identiquement quel que soit l'écran d'entrée.
 */
object NearbyShareManager {
    private lateinit var appContext: Context
    fun init(context: Context) { appContext = context.applicationContext }

    val connectionsClient: ConnectionsClient get() = Nearby.getConnectionsClient(appContext)
    private val serviceId = "com.legionforge.app.share"

    private val _mode = MutableStateFlow(ShareMode.IDLE)
    val mode: StateFlow<ShareMode> = _mode.asStateFlow()

    private val _endpoints = MutableStateFlow<List<DiscoveredEndpoint>>(emptyList())
    val endpoints: StateFlow<List<DiscoveredEndpoint>> = _endpoints.asStateFlow()

    private val _connectedEndpoint = MutableStateFlow<String?>(null)
    val connectedEndpoint: StateFlow<String?> = _connectedEndpoint.asStateFlow()

    private val _receivedPayload = MutableStateFlow<String?>(null)
    val receivedPayload: StateFlow<String?> = _receivedPayload.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Reconnexion automatique : dernier endpoint connecté, et tentatives restantes (bornées).
    @Volatile private var lastEndpoint: String? = null
    @Volatile private var retriesLeft = 0
    private val _reconnecting = MutableStateFlow(false)
    val reconnecting: StateFlow<Boolean> = _reconnecting.asStateFlow()
    private val reconnectHandler = android.os.Handler(android.os.Looper.getMainLooper())

    private val lifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            // Accepter automatiquement la connexion (pas de code d'appairage).
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    _connectedEndpoint.value = endpointId
                    _mode.value = ShareMode.CONNECTED
                    lastEndpoint = endpointId
                    retriesLeft = 5
                    _reconnecting.value = false
                }
                else -> {
                    val c = result.status.statusCode
                    _error.value = "Connexion refusée (${nearbyStatusMsg(c)})"
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            if (_connectedEndpoint.value == endpointId) {
                _connectedEndpoint.value = null
                _mode.value = ShareMode.IDLE
                scheduleReconnect()
            }
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            val bytes = payload.asBytes() ?: return
            _receivedPayload.value = String(bytes, Charsets.UTF_8)
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // Progression d'envoi/réception (ignorée pour l'instant).
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            _endpoints.value = _endpoints.value + DiscoveredEndpoint(endpointId, info.endpointName)
        }

        override fun onEndpointLost(endpointId: String) {
            _endpoints.value = _endpoints.value.filterNot { it.endpointId == endpointId }
        }
    }

    /** Hôte : diffuse sa présence et attend un client. */
    fun startAdvertising() {
        stop()
        _endpoints.value = emptyList()
        _error.value = null
        val options = AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
        connectionsClient.startAdvertising(
            android.os.Build.MODEL, serviceId, lifecycleCallback, options
        ).addOnSuccessListener { _mode.value = ShareMode.ADVERTISING }
            .addOnFailureListener { e -> _error.value = "Erreur hôte: ${e.message}" }
    }

    /** Client : cherche les hôtes à proximité. */
    fun startDiscovery() {
        stop()
        _endpoints.value = emptyList()
        _error.value = null
        val options = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
        connectionsClient.startDiscovery(serviceId, endpointDiscoveryCallback, options)
            .addOnSuccessListener { _mode.value = ShareMode.DISCOVERING }
            .addOnFailureListener { e -> _error.value = "Erreur client: ${e.message}" }
    }

    /** Client : se connecte à un hôte découvert. */
    /** Relance la connexion au dernier endpoint (backoff 2s→32s, 5 tentatives max). */
    private fun scheduleReconnect() {
        val target = lastEndpoint ?: return
        if (retriesLeft <= 0) { _reconnecting.value = false; _error.value = "Connexion perdue : relancez le mode 2 joueurs"; return }
        _reconnecting.value = true
        val delayMs = 2000L shl (5 - retriesLeft).coerceIn(0, 4)
        retriesLeft -= 1
        reconnectHandler.postDelayed({
            if (_mode.value != ShareMode.CONNECTED) {
                connectionsClient.requestConnection(android.os.Build.MODEL, target, lifecycleCallback)
                    .addOnFailureListener { scheduleReconnect() }
            }
        }, delayMs)
    }

    fun connectTo(endpointId: String) {
        connectionsClient.requestConnection(android.os.Build.MODEL, endpointId, lifecycleCallback)
            .addOnFailureListener { e -> _error.value = "Connexion impossible: ${e.message}" }
    }

    /** Envoie la liste (JSON) à l'endpoint connecté. */
    fun sendList(json: String) {
        val endpoint = _connectedEndpoint.value ?: return
        connectionsClient.sendPayload(endpoint, Payload.fromBytes(json.toByteArray(Charsets.UTF_8)))
    }

    fun stop() {
        reconnectHandler.removeCallbacksAndMessages(null)
        lastEndpoint = null; retriesLeft = 0; _reconnecting.value = false
        if (::appContext.isInitialized) {
            connectionsClient.stopAdvertising()
            connectionsClient.stopDiscovery()
            connectionsClient.stopAllEndpoints()
        }
        _mode.value = ShareMode.IDLE
        _endpoints.value = emptyList()
        _connectedEndpoint.value = null
    }
}

// ── Lecture des codes de statut Nearby (connecté → erreur lisible) ──────────
internal fun nearbyStatusMsg(c: Int): String = when (c) {
    0 -> "connecté"
    8001 -> "déjà en publicité (hôte) sur cet appareil"
    8002 -> "déjà en découverte (client) sur cet appareil"
    8003 -> "déjà à l'écoute Nearby"
    8004 -> "erreur Bluetooth (activez le Bluetooth)"
    8007 -> "pas de réseau Nearby (Wi-Fi/Bluetooth coupé)"
    8008 -> "erreur de transfert (réessayez)"
    8019 -> "connexion acceptée par l'autre joueur"
    8020 -> "connexion refusée par l'autre joueur"
    8024 -> "endpoint trop loin / hors de portée"
    8031 -> "l'autre joueur n'est plus à l'écoute"
    8034 -> "connexion perdue pendant l'échange (réessayez, déplacez vos téléphones)"
    8038 -> "session Nearby interrompue (relancez les 2 boutons 2 JOUEURS)"
    else -> "code $c"
}
