package com.legionforge.app.data.model

import com.legionforge.app.data.nearby.SyncedUnitState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** Core de la persistance de partie : le snapshot doit survive à un round-trip JSON (sans Room/UI). */
class PlaySessionCoreTest {
    @Test fun snapshotRoundTripsThroughJson() {
        val own = mapOf(
            "A:u1" to SyncedUnitState(instanceId = "A:u1", wounds = 3, suppressions = 1, activated = true, cover = 1, tokens = listOf("aim", "dodge")),
            "A:u2" to SyncedUnitState(instanceId = "A:u2", defeated = true)
        )
        val snap = PlaySessionSnapshot(
            listId = "l1", gameSystem = "LEGION_V2", phase = "GAME", playPhase = "activation",
            round = 3, vp = 5, oppVp = 2, kills = 1, oppKills = 2, priority = "own",
            passes = 1, oppPasses = 0, orderPool = mapOf("corps" to 1, "commander" to 0),
            gameResult = null, gameLog = listOf("[R3] x activée", "[R3] y"), turn = "own",
            activeObjectives = listOf("objA"), battleDeck = listOf("b1", "b2"),
            battleRevealed = mapOf("objective" to "o1"), missionRevealed = mapOf("own_objective" to "o1"),
            bluePlayer = "own", modsLeft = mapOf("own" to 1, "opp" to 2),
            commandDeck = listOf("c1"), playedCommands = listOf("p1"), roundCommand = "c2",
            discardedCommands = listOf(), soloOppRoundCommand = "c3", soloOppCommandDeck = listOf("c4"),
            soloOppListId = "l2", ownStates = own,
            soloOppStates = mapOf("B:o1" to SyncedUnitState("B:o1", activated = true))
        )
        val restored = PlaySessionSnapshot.fromJson(snap.toJson())
        assertEquals(snap, restored)
        assertEquals("A:u1", restored.ownStates["A:u1"]?.instanceId)
        assertEquals(3, restored.ownStates["A:u1"]?.wounds)
        assertEquals(listOf("aim", "dodge"), restored.ownStates["A:u1"]?.tokens)
    }

    @Test fun emptySnapshotRoundTrips() {
        val restored = PlaySessionSnapshot.fromJson(PlaySessionSnapshot().toJson())
        assertEquals(PlaySessionSnapshot(), restored)
    }

    @Test fun schemaVersionGuardsFutureEdits() {
        val restored = PlaySessionSnapshot.fromJson(PlaySessionSnapshot(schemaVersion = 1).toJson())
        assertEquals(1, restored.schemaVersion)
        assertNotEquals(0, restored.schemaVersion)
        // liste null + phase PREP = snapshot vide (rien à restaurer)
        assertEquals("PREP", restored.phase)
        assertEquals(null, restored.listId)
    }

    @Test fun armadaFieldsPersistThroughJson() {
        // Manche + phase Armada + pile FIFO de cadrans survivent au round-trip (reprise solo).
        val snap = PlaySessionSnapshot(
            listId = "flotte1", gameSystem = "ARMADA_V15", phase = "GAME", armadaRound = 5, armadaPhase = "ship",
            ownStates = mapOf(
                "S1" to SyncedUnitState("S1", activated = true, commandOrder = "NAVIGATE",
                    commandDialStack = listOf("COMMANDER_CLONE", "SQUADRON", "REPAIR"),
                    orderTokens = mapOf("NAVIGATE" to 1))
            )
        )
        val restored = PlaySessionSnapshot.fromJson(snap.toJson())
        assertEquals(5, restored.armadaRound)
        assertEquals("ship", restored.armadaPhase)
        assertEquals(listOf("COMMANDER_CLONE", "SQUADRON", "REPAIR"), restored.ownStates["S1"]?.commandDialStack)
        assertEquals("NAVIGATE", restored.ownStates["S1"]?.commandOrder)
        assertEquals(1, restored.ownStates["S1"]?.orderTokens?.get("NAVIGATE"))
    }

    @Test fun legacyLegionSnapshotWithoutArmadaFieldsIsSafe() {
        // Un snapshot Legion ancien (sans champs Armada) se désérialise sans crash (Gson -> défauts).
        val json = """{"schemaVersion":1,"listId":"l","gameSystem":"LEGION_V2","phase":"GAME","round":3}"""
        val restored = PlaySessionSnapshot.fromJson(json)
        assertEquals(1, restored.armadaRound)
        assertEquals("command", restored.armadaPhase)
    }
}
