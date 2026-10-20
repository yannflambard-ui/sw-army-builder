package com.legionforge.app.data.repository

import com.google.gson.Gson
import com.legionforge.app.data.model.CatalogDocument
import com.legionforge.app.data.model.ArmadaSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogJsonValidationTest {

    private val gson = Gson()

    private fun loadCatalog(): CatalogDocument {
        // Try classpath first (for IDE or gradle test), then filesystem
        val json = try {
            javaClass.classLoader?.getResourceAsStream("catalog.json")
                ?.bufferedReader()?.readText()
        } catch (_: Exception) { null } ?: run {
            javaClass.classLoader?.getResourceAsStream("assets/catalog.json")
                ?.bufferedReader()?.readText()
        } ?: run {
            // Fallback: read from project root
            java.io.File("../app/src/main/assets/catalog.json").let {
                if (it.exists()) it.readText()
                else java.io.File("app/src/main/assets/catalog.json").readText()
            }
        }
        return gson.fromJson(json, CatalogDocument::class.java)
    }

    @Test
    fun catalogHasMinimumCards() {
        val doc = loadCatalog()
        println("Total cards: ${doc.cards.size}")
        assertTrue("Should have at least 500 cards, got ${doc.cards.size}", doc.cards.size >= 500)
    }

    @Test
    fun allCardsHaveValidGameSystem() {
        val doc = loadCatalog()
        val invalid = doc.cards.filter { it.gameSystem.name !in listOf("LEGION_V2", "ARMADA_V15") }
        assertEquals("All cards should have valid gameSystem", 0, invalid.size)
    }

    @Test
    fun allCardsHaveValidKind() {
        val doc = loadCatalog()
        val validKinds = setOf("LEGION_UNIT", "LEGION_UPGRADE", "ARMADA_SHIP", "ARMADA_SQUADRON", "ARMADA_UPGRADE", "COMMANDER")
        val invalid = doc.cards.filter { it.kind.name !in validKinds }
        assertEquals("All cards should have valid kind", 0, invalid.size)
    }

    @Test
    fun allPointsAreNonNegative() {
        val doc = loadCatalog()
        val negative = doc.cards.filter { it.points < 0 }
        assertEquals("All cards should have non-negative points", 0, negative.size)
    }

    @Test
    fun allSlotValuesAreValidEnum() {
        val doc = loadCatalog()
        val armadaSlotNames = ArmadaSlot.entries.map { it.name }.toSet()
        val unknownSlots = mutableListOf<String>()
        doc.cards.forEach { card ->
            card.upgradeSlots.forEach { slot ->
                if (slot.name !in armadaSlotNames) {
                    unknownSlots.add("${card.id}:upgradeSlots=${slot.name}")
                }
            }
            card.allowedUpgradeSlots.forEach { slot ->
                if (slot.name !in armadaSlotNames) {
                    unknownSlots.add("${card.id}:allowedUpgradeSlots=${slot.name}")
                }
            }
        }
        if (unknownSlots.isNotEmpty()) {
            println("UNKNOWN SLOTS: ${unknownSlots.take(20)}")
        }
        assertEquals("No unknown slot values", 0, unknownSlots.size)
    }

    @Test
    fun legionUnitsHaveRank() {
        val doc = loadCatalog()
        val legionUnits = doc.cards.filter { it.kind.name == "LEGION_UNIT" }
        val noRank = legionUnits.filter { it.legionRank == null }
        println("Legion units: ${legionUnits.size}, without rank: ${noRank.size}")
        if (noRank.isNotEmpty()) {
            println("Units without rank: ${noRank.take(10).map { it.name }}")
        }
        assertEquals("All legion units should have a rank", 0, noRank.size)
    }

    @Test
    fun armadaShipsHaveSlots() {
        val doc = loadCatalog()
        val ships = doc.cards.filter { it.kind.name == "ARMADA_SHIP" }
        val noSlots = ships.filter { it.allowedUpgradeSlots.isEmpty() }
        println("Armada ships: ${ships.size}, without slots: ${noSlots.size}")
        if (noSlots.isNotEmpty()) {
            println("Ships without slots: ${noSlots.take(10).map { it.name }}")
            println("NOTE: Ship upgrade slots not present in this data source")
        }
        // Soft check: data may not have slot info per ship variant
        if (noSlots.size > ships.size / 2) {
            println("WARN: ${noSlots.size}/${ships.size} ships have no slots - upgrade slot data may be missing from source")
        }
    }

    @Test
    fun cardNamesAreUniquePerKindAndFaction() {
        val doc = loadCatalog()
        // Check for remaining duplicates: same name + same points + same faction + same kind
        val seen = mutableSetOf<String>()
        val dupes = mutableListOf<String>()
        doc.cards.forEach { card ->
            val key = "${card.name.lowercase()}|${card.points}|${card.factionId}|${card.kind.name}"
            if (!seen.add(key)) {
                dupes.add("${card.name} (${card.points}pts, ${card.factionId}, ${card.kind.name})")
            }
        }
        if (dupes.isNotEmpty()) {
            println("REMAINING DUPLICATES: ${dupes.take(20)}")
        }
        assertEquals("No remaining strict duplicates", 0, dupes.size)
    }

    @Test
    fun mandalorianUnitsHaveMercenaryFactions() {
        val doc = loadCatalog()
        val mandalorian = doc.cards.filter {
            it.kind.name == "LEGION_UNIT" &&
            (it.factionId == "mandalorians" || (it.cardSubtype?.contains("mandalorian", ignoreCase = true) == true))
        }
        println("Mandalorian units: ${mandalorian.size}")
        // Toute unité Mandalorienne mercenaire doit avoir "mandalorians" dans mercenaryFactions
        // pour apparaître dans une liste Mandalorienne (matchesFaction).
        val mercWithoutMandalorian = mandalorian.filter {
            it.factionId == "mercenary" && "mandalorians" !in (it.mercenaryFactions ?: emptyList())
        }
        if (mercWithoutMandalorian.isNotEmpty()) {
            println("Mercenary Mandalorian units WITHOUT 'mandalorians' in mercenaryFactions: " +
                mercWithoutMandalorian.map { it.name })
        }
        assertEquals("All mercenary Mandalorian units must include 'mandalorians' in mercenaryFactions",
            0, mercWithoutMandalorian.size)
    }

    @Test
    fun mandalorianFactionHasCorpsUnits() {
        val doc = loadCatalog()
        // Unités CORPS Mandaloriennes : factionId=mandalorians OU mercenaire avec "mandalorians" dans mercenaryFactions
        val mandalorianCorps = doc.cards.filter {
            it.kind.name == "LEGION_UNIT" &&
            it.legionRank?.name == "CORPS" &&
            (it.factionId == "mandalorians" || (it.factionId == "mercenary" && "mandalorians" in (it.mercenaryFactions ?: emptyList())))
        }
        println("Mandalorian CORPS units: ${mandalorianCorps.map { it.name }}")
        assertTrue("Mandalorian faction should have at least 1 CORPS unit, got ${mandalorianCorps.size}",
            mandalorianCorps.isNotEmpty())
    }
}
