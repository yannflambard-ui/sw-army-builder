package com.legionforge.app.domain

import com.legionforge.app.data.model.ArmadaSlot
import com.legionforge.app.data.model.BuilderList
import com.legionforge.app.data.model.CardKind
import com.legionforge.app.data.model.GameSystem
import com.legionforge.app.data.model.LegionRank
import com.legionforge.app.data.model.ListEntry
import com.legionforge.app.data.model.canonicalShipFamily
import com.legionforge.app.data.model.upgradeFitsShip

data class RuleViolation(val code: String, val message: String)
data class RuleValidationResult(val valid: Boolean, val totalPoints: Int, val violations: List<RuleViolation>)

interface RuleValidator {
    val gameSystem: GameSystem
    fun validate(list: BuilderList): RuleValidationResult
}

class LegionV2Validator : RuleValidator {
    override val gameSystem = GameSystem.LEGION_V2

    override fun validate(list: BuilderList): RuleValidationResult {
        val violations = mutableListOf<RuleViolation>()
        val total = list.entries.sumOf { it.card.points * it.quantity }
        if (list.gameSystem != gameSystem) violations += violation("wrong_game", "La liste n'est pas une liste Legion V2.")
        if (total > list.pointsLimit) violations += violation("points_limit", "Total $total pts supérieur à la limite ${list.pointsLimit} pts.")
        val units = list.entries.filter { it.card.kind == CardKind.LEGION_UNIT }
        val counts = units.groupBy { it.card.legionRank }.mapValues { (_, entries) -> entries.sumOf(ListEntry::quantity) }
        val quotas = listOf(
            LegionRank.COMMANDER to (1..2), LegionRank.OPERATIVE to (0..2), LegionRank.CORPS to (3..6),
            LegionRank.SPECIAL_FORCES to (0..3), LegionRank.SUPPORT to (0..3), LegionRank.HEAVY to (0..2)
        )
        quotas.forEach { (rank, range) ->
            val count = counts[rank] ?: 0
            if (count !in range) violations += violation("quota_${rank.name.lowercase()}", "${rank.toDisplay()} : quota ${range.first}–${range.last}, actuellement $count.")
        }
        if (units.any { it.card.legionRank == null }) violations += violation("missing_rank", "Une unité Legion n'a pas de rang renseigné.")
        val legionEntries = list.entries.filter { it.card.gameSystem == gameSystem }
        if (list.entries.any { it.card.gameSystem != gameSystem }) violations += violation("mixed_games", "La liste contient une carte d'un autre système.")
        if (legionEntries.any { it.quantity < 1 }) violations += violation("invalid_quantity", "Chaque entrée doit avoir une quantité d'au moins 1.")
        if (units.filter { it.card.unique }.groupBy { it.card.id }.any { (_, same) -> same.sumOf { it.quantity } > 1 }) violations += violation("unique_duplicate", "Une unité unique ne peut être sélectionnée qu'une fois.")
        val factionIds = units.map { it.card.factionId }.filter { it != "mercenary" }.distinct()
        if (factionIds.size > 1) violations += violation("mixed_factions", "Une armée ne peut pas mélanger plusieurs factions principales.")
        if (units.filter { it.card.unique }.groupBy { it.card.name }.any { (_, same) -> same.sumOf { it.quantity } > 1 }) violations += violation("unique_title_duplicate", "Une unité avec titre unique ne peut apparaître qu'une fois.")
        val individualUpgradeSlotCounts = mutableMapOf<String, Int>()
        units.forEach { unit -> unit.card.allowedUpgradeSlots.forEach { slot -> individualUpgradeSlotCounts[unit.instanceId + ":" + slot.name] = (individualUpgradeSlotCounts[unit.instanceId + ":" + slot.name] ?: 0) + 1 } }
        legionEntries.filter { it.card.kind == CardKind.LEGION_UPGRADE }.forEach { upgrade ->
            val parent = legionEntries.firstOrNull { it.instanceId == upgrade.parentInstanceId }
            val slot = upgrade.chosenSlot ?: upgrade.card.upgradeSlots.firstOrNull()
            if (parent == null || parent.card.kind != CardKind.LEGION_UNIT) violations += violation("upgrade_parent", "${upgrade.card.name} doit être attachée à une unité.")
            else if (slot == null || slot !in parent.card.allowedUpgradeSlots) violations += violation("invalid_legion_slot", "${upgrade.card.name} n'est pas autorisée sur ${parent.card.name}.")
            else {
                val key = parent.instanceId + ":" + slot.name
                individualUpgradeSlotCounts[key] = (individualUpgradeSlotCounts[key] ?: 0) - 1
                if (individualUpgradeSlotCounts[key] ?: 0 < 0) violations += violation("legion_slot_capacity", "${parent.card.name} ne dispose plus de slot ${slot.toDisplay()}.")
                if (upgrade.card.unique && legionEntries.any { it.instanceId != upgrade.instanceId && it.card.id == upgrade.card.id }) violations += violation("unique_upgrade_duplicate", "${upgrade.card.name} ne peut être sélectionnée qu'une seule fois.")
                if (!LegionRequirements.matches(upgrade.card.rulesText, parent.card)) violations += violation("upgrade_requirement", "${upgrade.card.name} ne peut être équipée que sur une unité répondant à ses exigences (pas ${parent.card.name}).")
            }
        }
        return RuleValidationResult(violations.isEmpty(), total, violations.distinctBy { it.code + it.message })
    }
}

class ArmadaV15Validator : RuleValidator {
    override val gameSystem = GameSystem.ARMADA_V15

    override fun validate(list: BuilderList): RuleValidationResult {
        val violations = mutableListOf<RuleViolation>()
        val total = list.entries.sumOf { it.card.points * it.quantity }
        if (list.gameSystem != gameSystem) violations += violation("wrong_game", "La liste n'est pas une flotte Armada V1.5.")
        if (total > list.pointsLimit) violations += violation("points_limit", "Total $total pts supérieur à la limite ${list.pointsLimit} pts.")
        val fleetEntries = list.entries.filter { it.card.gameSystem == gameSystem }
        val commanderEntries = fleetEntries.filter { it.card.kind == CardKind.COMMANDER || (it.card.kind == CardKind.ARMADA_UPGRADE && ArmadaSlot.COMMANDER in it.card.upgradeSlots) }
        val commanderCount = commanderEntries.sumOf { it.quantity }
        if (commanderCount != 1) violations += violation("commander_count", "Une flotte doit contenir exactement un Commandant (actuellement $commanderCount).")
        val fleetFactionIds = fleetEntries.map { it.card.factionId }.filter { it != "neutral" && it != "mercenary" }.distinct()
        if (fleetFactionIds.size > 1) violations += violation("mixed_factions", "Une flotte ne peut pas mélanger les factions principales.")
        if (fleetEntries.filter { it.card.kind == CardKind.ARMADA_SQUADRON && it.card.unique }.groupBy { it.card.id }.any { (_, same) -> same.sumOf { it.quantity } > 1 }) violations += violation("squadron_unique", "Un escadron nommé ne peut être sélectionné qu'une fois.")
        val commanderUpgradeCount = commanderEntries.filter { it.card.kind == CardKind.ARMADA_UPGRADE }.sumOf { it.quantity }
        if (commanderUpgradeCount > 1) violations += violation("commander_limit", "Maximum un slot Commandant dans la flotte.")
        val squadronPoints = list.entries.filter { it.card.kind == CardKind.ARMADA_SQUADRON }.sumOf { it.card.points * it.quantity }
        val squadronCap = (list.pointsLimit + 2) / 3
        if (list.entries.any { it.quantity < 1 }) violations += violation("invalid_quantity", "Chaque entrée doit avoir une quantité d'au moins 1.")
        if (fleetEntries.filter { it.card.unique }.groupBy { it.card.id }.any { (_, same) -> same.sumOf { it.quantity } > 1 }) violations += violation("unique_duplicate", "Une carte unique ne peut être sélectionnée qu'une fois.")
        if (fleetEntries.count { it.card.kind == CardKind.ARMADA_SHIP } > 6) violations += violation("ship_limit", "Une flotte ne peut contenir plus de 6 vaisseaux.")
        if (fleetEntries.count { it.card.kind == CardKind.ARMADA_UPGRADE && ArmadaSlot.COMMANDER in it.card.upgradeSlots } > 1) violations += violation("commander_limit", "Maximum un emplacement Commandant dans la flotte.")

        if (squadronPoints > squadronCap) violations += violation("squadron_third", "Escadrons : $squadronPoints pts, maximum $squadronCap pts (un tiers arrondi au supérieur).")
        val fleetCards = list.entries.filter { it.card.gameSystem == gameSystem }
        if (list.entries.any { it.card.gameSystem != gameSystem }) violations += violation("mixed_games", "La liste contient une carte d'un autre système.")
        // Un vaisseau unique (amiral nommé, ex. Executor) ne peut apparaître qu'une seule fois,
        // même sur deux coques différentes (Executor I + Executor II) — famille canonique.
        val uniqueShips = fleetCards.filter { it.card.kind == CardKind.ARMADA_SHIP && it.card.unique }
        if (uniqueShips.groupBy { canonicalShipFamily(it.card.name) }.any { (_, same) -> same.size > 1 }) {
            violations += violation("unique_ship_duplicate", "Un vaisseau amiral unique ne peut être sélectionné qu'une seule fois (même famille).")
        }
        val ships = fleetCards.filter { it.card.kind == CardKind.ARMADA_SHIP }
        val shipSlots = ships.flatMap { ship -> ship.card.allowedUpgradeSlots.map { ship.instanceId to it } }.groupBy({ it.first }, { it.second })
        ships.forEach { ship ->
            val installed = fleetCards.filter { it.card.kind == CardKind.ARMADA_UPGRADE && it.parentInstanceId == ship.instanceId }
            val equippedCounts = installed.groupingBy { it.chosenSlot ?: it.card.upgradeSlots.firstOrNull() }.eachCount()
            equippedCounts.forEach { (slot, count) ->
                if (slot == null || count > (shipSlots[ship.instanceId]?.count { it == slot } ?: 0)) violations += violation("slot_capacity", "${ship.card.name} ne dispose pas de $count slot(s) ${slot?.toDisplay() ?: "invalide"}.")
            }
            installed.forEach { upgrade ->
                val chosen = upgrade.chosenSlot ?: upgrade.card.upgradeSlots.firstOrNull()
                if (chosen == null || chosen !in (shipSlots[ship.instanceId] ?: emptyList())) {
                    violations += violation("invalid_upgrade_slot", "${upgrade.card.name} ne correspond pas aux slots du châssis ${ship.card.name}.")
                }
                // Une upgrade unique liée à un vaisseau précis ne peut être équipée que sur ce vaisseau.
                if (upgrade.card.linkedUnit != null && !upgradeFitsShip(upgrade.card, ship.card)) {
                    violations += violation("linked_unit_mismatch", "${upgrade.card.name} ne peut être équipée que sur un vaisseau ${upgrade.card.linkedUnit} (pas ${ship.card.name}).")
                }
                // Regle 1.6.0 : un vaisseau ne peut pas equiper plus d'une copie de la meme amelioration.
                if (installed.groupBy { it.card.id }.any { (_, arr) -> arr.size > 1 }) {
                    violations += violation("duplicate_upgrade", "${ship.card.name} ne peut pas equiper plusieurs copies de la meme amelioration.")
                }
            }
        }
        fleetCards.filter { it.card.kind == CardKind.ARMADA_UPGRADE && it.parentInstanceId !in ships.map { ship -> ship.instanceId } }
            .forEach { violations += violation("upgrade_parent", "${it.card.name} doit être attachée à un vaisseau.") }
        return RuleValidationResult(violations.isEmpty(), total, violations.distinctBy { it.code + it.message })
    }
}

private fun LegionRank.toDisplay() = when (this) {
    LegionRank.COMMANDER -> "Commandants"
    LegionRank.OPERATIVE -> "Opératifs"
    LegionRank.CORPS -> "Troupes"
    LegionRank.SPECIAL_FORCES -> "Forces spéciales"
    LegionRank.SUPPORT -> "Soutiens"
    LegionRank.HEAVY -> "Unités lourdes"
}

private fun ArmadaSlot.toDisplay() = name.lowercase().replace('_', ' ')
private fun violation(code: String, message: String) = RuleViolation(code, message)
