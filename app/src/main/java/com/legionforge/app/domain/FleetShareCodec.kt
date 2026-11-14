package com.legionforge.app.domain

import com.legionforge.app.data.model.ArmadaSlot
import com.legionforge.app.data.model.CardDefinition
import com.legionforge.app.data.model.GameSystem
import com.legionforge.app.data.model.ListEntry
import com.legionforge.app.ui.viewmodel.ShareEntry
import com.legionforge.app.ui.viewmodel.ShareListPayload

/** Résultat de la reconstruction d'une liste reçue. */
data class ImportedFleet(
    val entries: List<ListEntry>,
    /** cardId reçus absents du catalogue fourni (entrée ignorée, jamais rattachée à tort). */
    val missingCardIds: List<String>,
)

/**
 * Export/import d'une flotte, SANS Android, Room ni Nearby (fonctions pures, testables en JVM).
 *
 * Un seul JSON, ordre : 1) parents (vaisseaux, commandant, entrées sans parent)
 * 2) enfants (améliorations) dans l'ordre de leur parent 3) orphelines.
 * `parentIndex` = position du parent dans CE tableau exporté.
 */
object FleetShareCodec {

    /**
     * Cartes utilisables pour une liste de [factionId] : même règle que le DAO
     * (observeCards : faction, neutral, mercenary si la faction figure dans mercenaryFactions).
     */
    fun catalogForFaction(all: Collection<CardDefinition>, system: GameSystem, factionId: String): List<CardDefinition> =
        all.filter { c ->
            c.gameSystem == system && (c.factionId == factionId || c.factionId == "neutral" ||
                (c.factionId == "mercenary" && (c.mercenaryFactions as List<String>?)?.contains(factionId) == true))
        }

    /** Ordre d'export : parents d'abord, puis enfants groupés par parent, orphelines en fin. */
    fun orderForExport(entries: List<ListEntry>): List<ListEntry> {
        val parents = entries.filter { it.parentInstanceId == null }
        val children = parents.flatMap { p -> entries.filter { it.parentInstanceId == p.instanceId } }
        val placed = (parents + children).map { it.instanceId }.toSet()
        val orphans = entries.filter { it.instanceId !in placed }
        return parents + children + orphans
    }

    /** Construit le payload à partir des entrées (ordre d'export, parentIndex calculé). */
    fun buildPayload(
        name: String,
        gameSystem: String,
        factionId: String,
        pointsLimit: Int,
        entries: List<ListEntry>,
    ): ShareListPayload {
        val all = orderForExport(entries)
        val indexById = all.withIndex().associate { it.value.instanceId to it.index }
        return ShareListPayload(
            name = name,
            gameSystem = gameSystem,
            factionId = factionId,
            pointsLimit = pointsLimit,
            entries = all.map { e ->
                ShareEntry(
                    cardId = e.card.id,
                    parentInstanceId = e.parentInstanceId,
                    quantity = e.quantity,
                    chosenSlot = e.chosenSlot?.name,
                    instanceId = e.instanceId,
                    parentIndex = e.parentInstanceId?.let { indexById[it] },
                )
            },
        )
    }

    /**
     * Gson contourne les défauts Kotlin : un JSON incomplet produit des null à l'exécution.
     * Normalise le payload avant toute lecture (entrées nulles incluses).
     */
    fun normalize(payload: ShareListPayload?): ShareListPayload {
        val p = payload ?: return ShareListPayload()
        val raw: List<ShareEntry?> = (p.entries as List<ShareEntry?>?) ?: emptyList()
        return ShareListPayload(
            name = p.name ?: "",
            gameSystem = p.gameSystem ?: "",
            factionId = p.factionId ?: "",
            pointsLimit = p.pointsLimit,
            entries = raw.map { e ->
                if (e == null) ShareEntry(cardId = "", chosenSlot = null)
                else ShareEntry(
                    cardId = e.cardId ?: "",
                    parentInstanceId = e.parentInstanceId,
                    quantity = e.quantity,
                    chosenSlot = e.chosenSlot,
                    instanceId = e.instanceId,
                    parentIndex = e.parentIndex,
                )
            },
        )
    }

    /**
     * Reconstruit la flotte. [cards] = catalogue de la faction de la liste (faction + neutral + mercenary).
     * Une carte absente est ignorée et signalée dans [ImportedFleet.missingCardIds].
     * Rattachement : parent = nouvel id de l'entrée à `parentIndex` ; si ce parent est absent
     * (carte manquante ou index invalide), l'enfant n'est PAS rattaché (jamais à une autre entrée).
     */
    fun rebuild(payload: ShareListPayload, cards: Collection<CardDefinition>, newId: () -> String): ImportedFleet {
        val p = normalize(payload)
        val byId = cards.associateBy { it.id }

        val missing = mutableListOf<String>()
        val newIdByIndex = HashMap<Int, String>()
        p.entries.forEachIndexed { i, e ->
            if (byId.containsKey(e.cardId)) newIdByIndex[i] = newId()
            else missing += e.cardId
        }

        val entries = mutableListOf<ListEntry>()
        p.entries.forEachIndexed { i, e ->
            val mine = newIdByIndex[i] ?: return@forEachIndexed
            val parentNew = e.parentIndex?.let { newIdByIndex[it] }
            entries += ListEntry(
                instanceId = mine,
                card = byId.getValue(e.cardId),
                parentInstanceId = parentNew,
                quantity = if (e.quantity < 1) 1 else e.quantity,
                chosenSlot = e.chosenSlot?.let { runCatching { ArmadaSlot.valueOf(it) }.getOrNull() },
            )
        }
        return ImportedFleet(entries, missing)
    }
}
