package com.legionforge.app.data.repository

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ArmadaVerifiedShipDataTest {
    private fun asset(name: String): JsonObject {
        val stream = javaClass.classLoader?.getResourceAsStream(name)
            ?: javaClass.classLoader?.getResourceAsStream("assets/$name")
        val text = stream?.bufferedReader()?.use { it.readText() } ?: run {
            val fromModule = File("../app/src/main/assets/$name")
            if (fromModule.exists()) fromModule.readText()
            else File("app/src/main/assets/$name").readText()
        }
        return JsonParser.parseString(text).asJsonObject
    }

    @Test
    fun catalogIdsIdentitySizesAndDiceAreConsistent() {
        val cards = asset("catalog.json").getAsJsonArray("cards").map { it.asJsonObject }
            .filter { it.get("gameSystem").asString == "ARMADA_V15" && it.get("kind").asString == "ARMADA_SHIP" }
        val byId = cards.associateBy { it.get("id").asString }
        val data = asset("armada_verified_ship_data.json")
        val ships = data.getAsJsonObject("ships")
        assertEquals(1, data.get("schemaVersion").asInt)
        assertEquals(64, cards.size)
        assertEquals("Duplicate catalogue ship IDs", cards.size, byId.size)
        assertEquals(byId.keys, ships.keySet())
        val aliases = setOf(
            "armada-v15:bsdata:galactic-republic:2baf-29a0-daae-6817",
            "armada-v15:bsdata:separatist-alliance:c286-06ca-0bc0-fcc0"
        )
        ships.entrySet().forEach { (id, element) ->
            val ship = element.asJsonObject
            val card = byId.getValue(id)
            assertEquals(id, card.get("name").asString, ship.get("name").asString)
            assertEquals(id, card.get("factionId").asString, ship.get("faction").asString)
            assertTrue(id, ship.get("size").asString in setOf("small", "medium", "large", "huge"))
            assertTrue(id, ship.get("sourceModelSlug").asString.isNotBlank())
            assertTrue(id, ship.get("sourceChassisSlug").asString.isNotBlank())
            if (id in aliases) {
                assertEquals(id, "documented_alias", ship.get("match").asString)
                assertTrue(id, ship.get("matchExplanation").asString.isNotBlank())
                assertTrue(id, ship.get("identitySource").asString.startsWith("https://"))
            } else {
                assertEquals(id, "name_case_insensitive_and_faction", ship.get("match").asString)
                assertTrue(id, ship.get("name").asString.equals(ship.get("sourceModelName").asString, ignoreCase = true))
            }
            val dice = ship.getAsJsonObject("antiSquadron")
            assertEquals(id, setOf("red", "blue", "black"), dice.keySet())
            dice.entrySet().forEach { (_, value) ->
                assertTrue(id, value.isJsonPrimitive && value.asJsonPrimitive.isNumber)
                val text = value.asString
                assertTrue(id, text.matches(Regex("0|[1-9][0-9]*")))
            }
        }
    }

    @Test
    fun victoryAndOnagerVariantsHaveTheirOwnVerifiedValues() {
        val ships = asset("armada_verified_ship_data.json").getAsJsonObject("ships")
        fun check(id: String, size: String, red: Int, blue: Int, black: Int) {
            val ship = ships.getAsJsonObject(id)
            assertEquals(id, size, ship.get("size").asString)
            val dice = ship.getAsJsonObject("antiSquadron")
            assertEquals(id, red, dice.get("red").asInt)
            assertEquals(id, blue, dice.get("blue").asInt)
            assertEquals(id, black, dice.get("black").asInt)
        }
        check("armada_v15:ship-card/galactic-empire/victory-i-class-star-destroyer", "medium", 0, 1, 0)
        check("armada_v15:ship-card/galactic-empire/victory-ii-class-star-destroyer", "medium", 0, 1, 0)
        check("armada-v15:bsdata:galactic-republic:2baf-29a0-daae-6817", "medium", 0, 1, 0)
        check("armada_v15:ship-card/galactic-empire/onager-class-star-destroyer", "large", 0, 1, 1)
        check("armada_v15:ship-card/galactic-empire/onager-class-testbed", "large", 0, 0, 1)
        check("armada-v15:bsdata:galactic-republic:3240-fd8a-04a3-ce98", "large", 0, 1, 1)
    }
}
