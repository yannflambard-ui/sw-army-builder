package com.legionforge.app.domain

import com.google.gson.Gson
import com.legionforge.app.data.model.ArmadaSlot
import com.legionforge.app.data.model.CardDefinition
import com.legionforge.app.data.model.CardKind
import com.legionforge.app.data.model.CatalogDocument
import com.legionforge.app.data.model.GameSystem
import com.legionforge.app.data.model.ListEntry
import com.legionforge.app.ui.viewmodel.ShareEntry
import com.legionforge.app.ui.viewmodel.ShareListPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class FleetShareImportTest {

    private var seq = 0
    private fun newId() = "id${++seq}"

    private fun card(id: String, kind: CardKind, faction: String, slots: List<ArmadaSlot> = emptyList()) =
        CardDefinition(id = id, gameSystem = GameSystem.ARMADA_V15, kind = kind, name = id, points = 10,
            factionId = faction, allowedUpgradeSlots = slots)

    private val ship = card("rebel-ship", CardKind.ARMADA_SHIP, "rebel", listOf(ArmadaSlot.OFFICER, ArmadaSlot.WEAPONS_TEAM))
    private val cmdr = card("rebel-cmdr", CardKind.COMMANDER, "rebel")
    private val officer = card("neutral-officer", CardKind.ARMADA_UPGRADE, "neutral", listOf(ArmadaSlot.OFFICER))
    private val team = card("neutral-team", CardKind.ARMADA_UPGRADE, "neutral", listOf(ArmadaSlot.WEAPONS_TEAM))
    private val empireShip = card("empire-ship", CardKind.ARMADA_SHIP, "empire")
    private val rebelCatalog = listOf(ship, cmdr, officer, team)

    private fun le(id: String, c: CardDefinition, parent: String? = null, slot: ArmadaSlot? = null) =
        ListEntry(id, c, parent, 1, slot)

    /** Flotte : vaisseau, commandant, deux améliorations rattachées au vaisseau. */
    private fun fleet() = listOf(
        le("u1", officer, "s1", ArmadaSlot.OFFICER),
        le("c1", cmdr),
        le("s1", ship),
        le("u2", team, "s1", ArmadaSlot.WEAPONS_TEAM),
    )

    private fun payloadOf(entries: List<ListEntry>) =
        FleetShareCodec.buildPayload("L", "ARMADA_V15", "rebel", 400, entries)

    // ── Reproduction du bug (AVANT correctif) ──────────────────────────────────────────────
    /** Copie de l'ancien import (ArmyBuilderViewModel.importSharedList, v2.52.17) : résolution
     *  dans le catalogue DÉJÀ présent en mémoire (`_cards`), sans attendre celui de la faction. */
    private fun legacyResolveAgainstState(payload: ShareListPayload, stateCards: List<CardDefinition>): List<ListEntry> {
        val byId = stateCards.associateBy(CardDefinition::id)
        val ids = payload.entries.map { newId() }
        val out = mutableListOf<ListEntry>()
        payload.entries.forEachIndexed { i, e ->
            val card = byId[e.cardId] ?: return@forEachIndexed
            out += ListEntry(ids[i], card, e.parentIndex?.let { ids.getOrNull(it) }, e.quantity,
                e.chosenSlot?.let(ArmadaSlot::valueOf))
        }
        return out
    }

    @Test fun bugReproduction_legacyImportAgainstPreviousFactionLosesShipAndCommander() {
        // État mémoire = catalogue de la liste précédente (empire) ; la liste reçue est rebelle.
        val stateCards = FleetShareCodec.catalogForFaction(listOf(empireShip, officer, team, ship, cmdr), GameSystem.ARMADA_V15, "empire")
        val legacy = legacyResolveAgainstState(payloadOf(fleet()), stateCards)
        assertEquals("symptôme : 0 vaisseau", 0, legacy.count { it.card.kind == CardKind.ARMADA_SHIP })
        assertEquals("symptôme : 0 commandant", 0, legacy.count { it.card.kind == CardKind.COMMANDER })
        assertEquals("les améliorations neutres arrivent", 2, legacy.count { it.card.kind == CardKind.ARMADA_UPGRADE })
    }

    @Test fun fixed_importWithFactionCatalogKeepsShipCommanderAndUpgrades() {
        val factionCards = FleetShareCodec.catalogForFaction(rebelCatalog + empireShip, GameSystem.ARMADA_V15, "rebel")
        val r = FleetShareCodec.rebuild(payloadOf(fleet()), factionCards, ::newId)
        assertEquals(1, r.entries.count { it.card.kind == CardKind.ARMADA_SHIP })
        assertEquals(1, r.entries.count { it.card.kind == CardKind.COMMANDER })
        assertEquals(2, r.entries.count { it.card.kind == CardKind.ARMADA_UPGRADE })
        assertTrue(r.missingCardIds.isEmpty())
    }

    // ── Export : ordre et parentIndex ───────────────────────────────────────────────────────
    @Test fun export_parentsFirstThenChildren() {
        val p = payloadOf(fleet().shuffled(Random(7)))
        val n = p.entries.count { it.parentIndex == null }
        assertEquals(2, n) // ship + commander
        assertTrue(p.entries.take(n).all { it.parentIndex == null })
        assertTrue(p.entries.drop(n).all { it.parentIndex != null })
        val shipIdx = p.entries.indexOfFirst { it.cardId == "rebel-ship" }
        assertEquals(2, p.entries.count { it.parentIndex == shipIdx })
    }

    @Test fun export_orphanGoesLastWithNoParentIndex() {
        val p = payloadOf(listOf(le("u9", officer, "absent", ArmadaSlot.OFFICER), le("s1", ship)))
        assertEquals("rebel-ship", p.entries.first().cardId)
        assertNull(p.entries.last().parentIndex)
    }

    // ── Import : rattachement parent -> enfant, quel que soit l'ordre reçu ──────────────────
    @Test fun import_shuffledOrder_attachesEveryUpgradeToItsShip() {
        val p = payloadOf(fleet().shuffled(Random(42)))
        val r = FleetShareCodec.rebuild(p, rebelCatalog, ::newId)
        assertEquals(4, r.entries.size)
        assertTrue(r.missingCardIds.isEmpty())
        val s = r.entries.single { it.card.kind == CardKind.ARMADA_SHIP }
        assertNull(s.parentInstanceId)
        assertEquals(2, r.entries.count { it.parentInstanceId == s.instanceId })
        assertEquals(1, r.entries.count { it.card.kind == CardKind.COMMANDER && it.parentInstanceId == null })
    }

    @Test fun import_newIdsAreFreshNotReusedFromExport() {
        val source = fleet()
        val r = FleetShareCodec.rebuild(payloadOf(source), rebelCatalog, ::newId)
        assertFalse(r.entries.any { e -> source.any { it.instanceId == e.instanceId } })
    }

    // ── Carte absente du catalogue ─────────────────────────────────────────────────────────
    @Test fun import_cardMissingFromCatalogIsSkippedAndReported() {
        val r = FleetShareCodec.rebuild(payloadOf(fleet()), rebelCatalog.filterNot { it.id == "neutral-team" }, ::newId)
        assertEquals(listOf("neutral-team"), r.missingCardIds)
        assertEquals(3, r.entries.size)
        assertEquals(1, r.entries.count { it.card.kind == CardKind.ARMADA_SHIP })
    }

    @Test fun import_shipMissingFromCatalogDoesNotAttachChildrenToOtherEntries() {
        val r = FleetShareCodec.rebuild(payloadOf(fleet()), listOf(cmdr, officer, team), ::newId)
        assertEquals(listOf("rebel-ship"), r.missingCardIds)
        assertEquals(0, r.entries.count { it.card.kind == CardKind.ARMADA_SHIP })
        // Les améliorations ne doivent pointer vers aucune entrée (ni le commandant).
        assertTrue(r.entries.filter { it.card.kind == CardKind.ARMADA_UPGRADE }.all { it.parentInstanceId == null })
    }

    // ── Orpheline ───────────────────────────────────────────────────────────────────────────
    @Test fun import_orphanUpgradeHasNoParent() {
        val p = payloadOf(listOf(le("s1", ship), le("u9", officer, "absent", ArmadaSlot.OFFICER)))
        val r = FleetShareCodec.rebuild(p, rebelCatalog, ::newId)
        assertNull(r.entries.single { it.card.kind == CardKind.ARMADA_UPGRADE }.parentInstanceId)
    }

    // ── JSON Gson avec champs null ─────────────────────────────────────────────────────────
    @Test fun gsonNullFieldsDoNotCrashAndAttachCorrectly() {
        val json = """{"name":null,"gameSystem":null,"entries":[{"cardId":"rebel-ship","quantity":1},{"cardId":"neutral-officer","parentIndex":0,"quantity":1,"chosenSlot":"OFFICER"},null]}"""
        val payload = Gson().fromJson(json, ShareListPayload::class.java)
        val r = FleetShareCodec.rebuild(payload, rebelCatalog, ::newId)
        assertEquals(2, r.entries.size)
        assertEquals(r.entries[0].instanceId, r.entries[1].parentInstanceId)
        assertEquals("entrée null signalée (cardId vide)", listOf(""), r.missingCardIds)
    }

    @Test fun gsonMissingEntriesGivesEmptyFleet() {
        val payload = Gson().fromJson("""{"name":"x"}""", ShareListPayload::class.java)
        assertTrue(FleetShareCodec.rebuild(payload, rebelCatalog, ::newId).entries.isEmpty())
    }

    @Test fun gsonNullEntriesListDoesNotCrash() {
        val payload = Gson().fromJson("""{"name":"x","entries":null}""", ShareListPayload::class.java)
        assertTrue(FleetShareCodec.rebuild(payload, rebelCatalog, ::newId).entries.isEmpty())
    }

    @Test fun roundTripJsonKeepsStructure() {
        val p = payloadOf(fleet().shuffled(Random(3)))
        val back = Gson().fromJson(Gson().toJson(p), ShareListPayload::class.java)
        val r = FleetShareCodec.rebuild(back, rebelCatalog, ::newId)
        val s = r.entries.single { it.card.kind == CardKind.ARMADA_SHIP }
        assertEquals(2, r.entries.count { it.parentInstanceId == s.instanceId })
    }

    // ── Catalogue réel (assets/catalog.json) ───────────────────────────────────────────────
    @Test fun realCatalogRebelFleetRoundTrips() {
        val text = java.io.File("src/main/assets/catalog.json").takeIf { it.exists() }?.readText()
            ?: java.io.File("app/src/main/assets/catalog.json").readText()
        val all = Gson().fromJson(text, CatalogDocument::class.java).cards
        val cards = FleetShareCodec.catalogForFaction(all, GameSystem.ARMADA_V15, "rebel")
        val s = cards.first { it.kind == CardKind.ARMADA_SHIP && it.factionId == "rebel" }
        val up = cards.first { it.kind == CardKind.ARMADA_UPGRADE && it.factionId == "neutral" && ArmadaSlot.OFFICER in it.upgradeSlots }
        val c = cards.first { it.kind == CardKind.COMMANDER }
        val r = FleetShareCodec.rebuild(payloadOf(listOf(le("u", up, "s", ArmadaSlot.OFFICER), le("c", c), le("s", s))), cards, ::newId)
        assertEquals(3, r.entries.size)
        assertTrue(r.missingCardIds.isEmpty())
        assertEquals(r.entries.single { it.card.kind == CardKind.ARMADA_SHIP }.instanceId,
            r.entries.single { it.card.kind == CardKind.ARMADA_UPGRADE }.parentInstanceId)
    }
}
