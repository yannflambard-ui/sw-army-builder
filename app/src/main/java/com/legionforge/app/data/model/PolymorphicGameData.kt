package com.legionforge.app.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Miroir EXACT de canonical_family() dans scripts/enrich_armada_links.py.
 * Réduit un nom de vaisseau (ou la cible "Title - X") à une base de famille
 * canonique, pour lier chaque upgrade unique au(x) vaisseau(x) compatible(s). */
fun canonicalShipFamily(name: String): String {
    var low = name.lowercase().replace('-', ' ').replace("dreadought", "dreadnought")
    low = low.replace(Regex("\\b(i{1,3}|iv|v|a|b|c|mk|mark|class|type)\\b" +
        "|\\b(assault|battle|command|star|combat|medium|light|scout|torpedo|" +
        "armored|ordnance|suppression|carrier|dreadnought|transport|refit|" +
        "retrofit|escort|support|flotilla|patrol|special|troopship|missile|" +
        "artillery|juggernaut|interdiction|city)\\b"), " ")
    low = low.replace(Regex("\\s+"), " ").trim()
    val base = mapOf(
        "super star destroyer" to "executor", "super destroyer" to "executor",
        "star dreadnought" to "executor", "executor" to "executor",
        "imperial star destroyer" to "imperial star destroyer",
        "gladiator" to "gladiator", "argo-type transport" to "argotype",
        "gonzanti" to "gozanti", "gozanti" to "gozanti",
        "gr 75" to "gr75", "cr90" to "cr90", "nebula-b" to "nebulon b",
        "nebula" to "nebula",
        "mc30" to "mc30", "mc75" to "mc75", "mc80" to "mc80",
        "mc80 cruiser home one type" to "mc80 home one", "mc80 cruiser home one" to "mc80 home one",
        "mc80 home one" to "mc80 home one", "mc80 cruiser liberty type" to "mc80 liberty",
        "mc80 cruiser liberty" to "mc80 liberty", "mc80 liberty" to "mc80 liberty",
        "home one" to "mc80 home one", "liberty" to "mc80 liberty",
        "hammerhead" to "hammerhead", "raider" to "raider", "arquitens" to "arquitens",
        "pelta" to "pelta", "modified pelta" to "pelta", "hardcell" to "hardcell",
        "acclamator" to "acclamator", "venator" to "venator", "victory" to "victory",
        "quasar" to "quasar", "onager" to "onager", "interdictor" to "interdictor",
        "muunificent" to "munificent", "munificent" to "munificent",
        "recusant" to "recusant", "providence" to "providence",
        "starhawk" to "starhawk"
    )
    for ((key, res) in base) if (key in low) return res
    return if (low.isBlank()) "unknown" else low
}

/** Une ARMADA_UPGRADE avec une cible liée (linkedUnit) est compatible avec un vaisseau
 * seulement si sa famille canonique matche celle du vaisseau. Utilisé par le filtre
 * du builder et le validateur. */
fun upgradeFitsShip(upgrade: CardDefinition, ship: CardDefinition): Boolean {
    val target = upgrade.linkedUnit ?: return true // pas de cible = compatible par slot seul
    return canonicalShipFamily(target) == canonicalShipFamily(ship.name)
}

/** Système identifié dans les catalogues JSON et les listes sauvegardées. */
enum class GameSystem { LEGION_V2, ARMADA_V15 }
enum class CardKind { LEGION_UNIT, LEGION_UPGRADE, ARMADA_SHIP, ARMADA_SQUADRON, ARMADA_UPGRADE, COMMANDER }
enum class LegionRank { COMMANDER, OPERATIVE, CORPS, SPECIAL_FORCES, SUPPORT, HEAVY }
enum class ArmadaSlot { COMMANDER, OFFENSIVE_RETROFIT, TURBOLASERS, WEAPONS_TEAM, SUPPORT_TEAM, DEFENSIVE_RETROFIT, ENGINEERING_TEAM, TITLE, OFFICER, FLEET_COMMAND, FLEET_SUPPORT, ION_CANNONS, ORDNANCE, EXPERIMENTAL_RETROFIT, SUPERWEAPON, FORCE, COMMAND, TRAINING, GEAR, ARMAMENT, PERSONNEL, COMMS, HEAVY_WEAPON, HARDPOINT, GRENADES, GENERATOR, DOCTRINE, COUNTERPART, PROGRAMMING, OTHER }

/** Représentation d'une carte de catalogue, directement désérialisable depuis le pipeline JSON. */
data class CardDefinition(
    val id: String,
    val gameSystem: GameSystem,
    val kind: CardKind,
    val name: String,
    val points: Int,
    val factionId: String,
    val legionRank: LegionRank? = null,
        val upgradeSlots: List<ArmadaSlot> = emptyList(),
        val allowedUpgradeSlots: List<ArmadaSlot> = emptyList(),
        val commander: Boolean = false,
        val unique: Boolean = false,
        val imageUrl: String? = null,
        val imageAssetPath: String? = null,
        val imageAssetPaths: Map<String, String> = emptyMap(),
        val rulesText: String? = null,
        val effectText: String? = null,
        val names: Map<String, String> = emptyMap(),
        val shipStats: String? = null,
        val linkedUnit: String? = null,
        val legionStats: String? = null,
        val cardSubtype: String? = null,
        val title: String? = null,
        val forceAffinity: String? = null,
        val affiliation: String? = null,
        val mercenaryFactions: List<String> = emptyList(),
        /** Drapeau « flottille » (Armada) : présent dans catalog.json quand une coque est une flottille.
         * Aucune carte du catalogue actuel ne le pose → le garde-fou D6 reste inactif sur les données réelles. */
        val flotilla: Boolean = false
            ) {
    fun displayName(locale: String = java.util.Locale.getDefault().language): String {
        return names[locale] ?: name
    }

    /** Chemin d'image localisé (imageAssetPaths[locale]), fallback EN (imageAssetPath). */
    fun imagePath(locale: String = java.util.Locale.getDefault().language): String? =
        (imageAssetPaths ?: emptyMap())[locale] ?: imageAssetPath
}

data class CatalogDocument(
    val schemaVersion: Int = 1,
    val cards: List<CardDefinition> = emptyList()
)

data class ListEntry(
    val instanceId: String,
    val card: CardDefinition,
    val parentInstanceId: String? = null,
    val quantity: Int = 1,
    val chosenSlot: ArmadaSlot? = null
)

data class BuilderList(
    val id: String,
    val name: String,
    val gameSystem: GameSystem,
    val factionId: String,
    val pointsLimit: Int,
    val entries: List<ListEntry>
)

@Entity(tableName = "catalog_cards", indices = [Index("gameSystem"), Index("factionId")])
data class CatalogCardEntity(
    @PrimaryKey val id: String,
    val gameSystem: String,
    val kind: String,
    val name: String,
    val points: Int,
    val factionId: String,
    val legionRank: String?,
    /** Valeurs d'enum séparées par virgules; sérialisation simple, stable et inspectable. */
    val upgradeSlots: String,
    val allowedUpgradeSlots: String,
    val commander: Boolean,
    val unique: Boolean,
    val imageUrl: String?,
    val imageAssetPath: String?,
    val imageAssetPaths: String? = null,
    val rulesText: String?,
    val effectText: String? = null,
    val names: String? = null,
    val shipStats: String? = null,
    val linkedUnit: String? = null,
    val legionStats: String? = null,
    val cardSubtype: String? = null,
    val title: String? = null,
    val forceAffinity: String? = null,
    val affiliation: String? = null,
    val mercenaryFactions: String? = null
) {
    fun toDefinition() = CardDefinition(
        id, GameSystem.valueOf(gameSystem), CardKind.valueOf(kind), name, points, factionId,
        legionRank?.let(LegionRank::valueOf), parseSlots(upgradeSlots), parseSlots(allowedUpgradeSlots),
        commander, unique, imageUrl, imageAssetPath, parseJsonNames(imageAssetPaths), rulesText, effectText, parseJsonNames(names), shipStats, linkedUnit, legionStats,
        cardSubtype, title, forceAffinity, affiliation, parseFactions(mercenaryFactions)
    )

    companion object {
        fun from(card: CardDefinition) = CatalogCardEntity(
            card.id, card.gameSystem.name, card.kind.name, card.name, card.points, card.factionId,
            card.legionRank?.name, card.upgradeSlots.joinToString(",") { it.name },
            card.allowedUpgradeSlots.joinToString(",") { it.name }, card.commander, card.unique,
            card.imageUrl, card.imageAssetPath, toJsonNames(card.imageAssetPaths), card.rulesText, card.effectText, toJsonNames(card.names), card.shipStats, card.linkedUnit, card.legionStats,
            card.cardSubtype, card.title, card.forceAffinity, card.affiliation, (card.mercenaryFactions ?: emptyList()).joinToString(",")
        )
        private fun parseSlots(value: String) = value.split(',').filter(String::isNotBlank).map(ArmadaSlot::valueOf)
        private fun parseFactions(value: String?) = value?.split(',')?.filter(String::isNotBlank) ?: emptyList()
        private fun parseJsonNames(value: String?): Map<String, String> {
            if (value == null || value.isEmpty()) return emptyMap()
            return try { com.google.gson.Gson().fromJson(value, Map::class.java) as? Map<String, String> ?: emptyMap() } catch (_: Exception) { emptyMap() }
        }
        // Gson n'applique PAS les valeurs par défaut Kotlin : un champ absent du JSON
        // reste null (défaut JVM), pas emptyMap(). D'où le null-check obligatoire.
        private fun toJsonNames(names: Map<String, String>?): String? {
            if (names == null || names.isEmpty()) return null
            return try { com.google.gson.Gson().toJson(names) } catch (_: Exception) { null }
        }
    }
}

@Entity(tableName = "builder_lists")
data class BuilderListEntity(
    @PrimaryKey val id: String,
    val name: String,
    val gameSystem: String,
    val factionId: String,
    val pointsLimit: Int,
    val updatedAt: Long
)

@Entity(tableName = "builder_entries", primaryKeys = ["instanceId"], indices = [Index("listId"), Index("cardId")])
data class BuilderEntryEntity(
    val instanceId: String,
    val listId: String,
    val cardId: String,
    val parentInstanceId: String?,
    val quantity: Int,
    val chosenSlot: String? = null
)
