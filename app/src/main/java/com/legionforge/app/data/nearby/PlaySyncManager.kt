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
    val commandOrder: String? = null,  // Armada roue de commandement (nom de l'ordre) — cadran révélé (tête de pile)
    val commandDialStack: List<String> = emptyList(),   // Armada pile FIFO de cadrans face cachée (capacité = valeur de commande)
    val commandDialRevealed: Boolean = false,  // Armada : cadran déjà révélé cette manche (1 révélation/vaisseau/manche)
    val activeCrits: List<String> = emptyList(),  // noms des dégâts critiques actifs
    val usedUpgrades: Set<String> = emptySet(),   // ids des upgrades marqués utilisés
    val activated: Boolean? = null,  // Armada squadron (activé/désactivé)
    val cover: Int? = null,           // Legion couverture (0/1/2)
    val defeated: Boolean? = null,    // Legion unité vaincue
    val vehicleDamage: List<String> = emptyList(),  // Legion véhicule : états (immobilized, weapon)
    val promoted: Boolean? = null,    // Legion unité promue commandant
    val deployZone: Int? = null,       // Legion déploiement : null=non déployé, 1=zone1, 2=zone2
    val detachedThreshold: Int? = null,  // Legion : blessures avant détachée
    val eliminatedThreshold: Int? = null, // Legion : blessures avant éliminée
    val detached: Boolean? = null,       // Legion : unité détachée (perte de cohésion)
    val eliminated: Boolean? = null,     // Legion : unité éliminée
    val orderReceived: Boolean? = null,  // Legion : a reçu un ordre ce round
    val panicThreshold: Int? = null,     // Legion : suppressions avant panique
    val panicking: Boolean? = null       // Legion : unité paniquée
)

/** Payload de synchronisation d'état de jeu (envoyé via Nearby). */
data class PlaySyncPayload(
    val type: String,           // "round" | "state" | "list" | "request"
    val round: Int? = null,
    val units: List<SyncedUnitState> = emptyList(),
    val listJson: String? = null, // liste de l'armée adverse (sérialisée)
    val deploymentDone: Boolean? = null, // Legion : déploiement terminé (par joueur)
    val roundCard: String? = null, // Legion : id de la carte de commandement jouée ce round
    val turnOwner: String? = null, // Legion : qui active ("A" ou "B")
    val gameState: GameStatePayload? = null, // Legion : état de jeu partagé complet (VP/round/objectifs/tour)
    val seq: Long = 0                       // numéro d'émission monotone (last-write-wins côté réception)
)

/** État de jeu partagé (round, VP, tour, objectifs, phase) — synchronisé entre les 2 joueurs. */
data class GameStatePayload(
    val round: Int = 1,
    val vpA: Int = 0,
    val vpB: Int = 0,
    val turnOwner: String? = null,   // "A" ou "B"
    val activeObjectives: List<String> = emptyList(),
    val phase: String = "game",      // "setup" | "game" | "end" (Legion)
    val seq: Long = 0,               // horodatage/ordre pour last-write-wins
    val armadaRound: Int? = null,    // Armada : manche en cours (1..6)
    val armadaPhase: String? = null  // Armada : phase ("COMMAND"/"SHIP"/"SQUADRON"/"STATUS")
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
    fun consumeListRequest() { _listRequested.value = false }

    private val _receivedDeploymentDone = MutableStateFlow<Boolean?>(null)
    val receivedDeploymentDone: StateFlow<Boolean?> = _receivedDeploymentDone.asStateFlow()

    private val _receivedRoundCard = MutableStateFlow<String?>(null)
    val receivedRoundCard: StateFlow<String?> = _receivedRoundCard.asStateFlow()

    private val _receivedTurn = MutableStateFlow<String?>(null)
    val receivedTurn: StateFlow<String?> = _receivedTurn.asStateFlow()

    private val _receivedGameState = MutableStateFlow<GameStatePayload?>(null)
    val receivedGameState: StateFlow<GameStatePayload?> = _receivedGameState.asStateFlow()

    // Numéro d'émission monotone : tout envoi porte un seq croissant (horodatage + compteur, jamais en arrière).
    private var outSeq: Long = 0
    private fun nextSeq(): Long { outSeq = maxOf(outSeq + 1, System.currentTimeMillis()); return outSeq }
    // Dernier seq appliqué par type de payload : un payload plus ancien que le dernier appliqué est ignoré.
    private val lastApplied = mutableMapOf<String, Long>()
    private fun fresh(type: String, seq: Long): Boolean {
        val last = lastApplied[type] ?: 0L
        if (seq != 0L && seq < last) return false
        if (seq != 0L) lastApplied[type] = seq
        return true
    }

    // Identité de joueur : hôte = "A", client = "B".
    var playerId: String = "A"
        private set

    init {
        // Décode les payloads reçus.
        CoroutineScope(Dispatchers.Main).launch {
            nearby.receivedPayload.collect { json ->
                if (json == null) return@collect
                val payload = try { gson.fromJson(json, PlaySyncPayload::class.java) } catch (_: Exception) { null } ?: return@collect
                if (!fresh(payload.type, payload.seq)) return@collect  // payload obsolète (last-write-wins)
                when (payload.type) {
                    "round" -> _receivedRound.value = payload.round
                    "state" -> _receivedUnits.value = payload.units
                    "list" -> _receivedListJson.value = payload.listJson
                    "request" -> _listRequested.value = true
                    "deploydone" -> _receivedDeploymentDone.value = payload.deploymentDone
                    "roundcard" -> _receivedRoundCard.value = payload.roundCard
                    "turn" -> _receivedTurn.value = payload.turnOwner
                    "game" -> _receivedGameState.value = payload.gameState
                }
            }
        }
    }

    fun startHost() { playerId = "A"; nearby.startAdvertising() }
    fun startClient() { playerId = "B"; nearby.startDiscovery() }
    fun connectTo(endpointId: String) = nearby.connectTo(endpointId)
    fun stop() = nearby.stop()

    fun sendRound(round: Int) {
        nearby.sendList(gson.toJson(PlaySyncPayload(type = "round", round = round, seq = nextSeq())))
    }

    fun sendState(units: List<SyncedUnitState>) {
        nearby.sendList(gson.toJson(PlaySyncPayload(type = "state", units = units, seq = nextSeq())))
    }

    fun sendList(json: String) {
        nearby.sendList(gson.toJson(PlaySyncPayload(type = "list", listJson = json, seq = nextSeq())))
    }

    /** Envoie l'état "déploiement terminé" (Legion setup). */
    fun sendDeploymentDone(done: Boolean) {
        nearby.sendList(gson.toJson(PlaySyncPayload(type = "deploydone", deploymentDone = done, seq = nextSeq())))
    }

    /** Envoie la carte de commandement jouée ce round (id). */
    fun sendRoundCard(cardId: String?) {
        nearby.sendList(gson.toJson(PlaySyncPayload(type = "roundcard", roundCard = cardId, seq = nextSeq())))
    }

    /** Envoie qui active ("A" ou "B"). */
    fun sendTurn(turnOwner: String) {
        nearby.sendList(gson.toJson(PlaySyncPayload(type = "turn", turnOwner = turnOwner, seq = nextSeq())))
    }

    /** Envoie l'état de jeu partagé (round, VP, tour, objectifs, phase). */
    fun sendGameState(state: GameStatePayload) {
        nearby.sendList(gson.toJson(PlaySyncPayload(type = "game", gameState = state, seq = nextSeq())))
    }

    /** Demande à l'autre joueur de renvoyer sa liste (synchro manuelle). */
    fun requestList() {
        nearby.sendList(gson.toJson(PlaySyncPayload(type = "request", seq = nextSeq())))
    }
}
