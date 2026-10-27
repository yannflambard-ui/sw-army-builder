package com.legionforge.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.legionforge.app.data.model.*
import com.legionforge.app.data.repository.BuilderRepository
import com.legionforge.app.data.repository.WikiRepository
import com.legionforge.app.domain.*
import com.legionforge.app.util.CrashReporter
import com.legionforge.app.ui.viewmodel.ShareListPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import java.util.UUID

class ArmyBuilderViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = BuilderRepository(application)
    private val wikiRepository = WikiRepository(application)
    private val _cards = MutableStateFlow<List<CardDefinition>>(emptyList())
    val cards: StateFlow<List<CardDefinition>> = _cards.asStateFlow()
    private val _allLists = MutableStateFlow<List<BuilderListEntity>>(emptyList())
    val allLists: StateFlow<List<BuilderListEntity>> = _allLists.asStateFlow()
    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    private val _catalogError = MutableStateFlow<String?>(null)
    val catalogError: StateFlow<String?> = _catalogError.asStateFlow()
    private val _currentList = MutableStateFlow<BuilderListEntity?>(null)
    val currentList: StateFlow<BuilderListEntity?> = _currentList.asStateFlow()
    private val _entries = MutableStateFlow<List<ListEntry>>(emptyList())
    val entries: StateFlow<List<ListEntry>> = _entries.asStateFlow()
    // Armée adverse en mode solo (2 armées sur le même appareil).
    private val _soloOppList = MutableStateFlow<BuilderListEntity?>(null)
    val soloOppList: StateFlow<BuilderListEntity?> = _soloOppList.asStateFlow()
    private val _soloOppEntries = MutableStateFlow<List<ListEntry>>(emptyList())
    val soloOppEntries: StateFlow<List<ListEntry>> = _soloOppEntries.asStateFlow()
    private val _validation = MutableStateFlow(RuleValidationResult(false, 0, emptyList()))
    val validation: StateFlow<RuleValidationResult> = _validation.asStateFlow()
    private val _searchResults = MutableStateFlow<List<CardDefinition>>(emptyList())
    val searchResults: StateFlow<List<CardDefinition>> = _searchResults.asStateFlow()
    private val _searching = MutableStateFlow(false)
    val searching: StateFlow<Boolean> = _searching.asStateFlow()
    private var searchCollector: kotlinx.coroutines.Job? = null

    // ── Wiki des règles officielles ──
    private val _wikiSections = MutableStateFlow<List<WikiSectionEntity>>(emptyList())
    val wikiSections: StateFlow<List<WikiSectionEntity>> = _wikiSections.asStateFlow()
    private val _wikiSearchResults = MutableStateFlow<List<WikiSectionEntity>>(emptyList())
    val wikiSearchResults: StateFlow<List<WikiSectionEntity>> = _wikiSearchResults.asStateFlow()
    private val _wikiSearching = MutableStateFlow(false)
    val wikiSearching: StateFlow<Boolean> = _wikiSearching.asStateFlow()
    private var wikiSearchCollector: kotlinx.coroutines.Job? = null

    fun searchCards(query: String) {
        searchCollector?.cancel()
        val q = query.trim()
        if (q.isEmpty()) { _searchResults.value = emptyList(); _searching.value = false; return }
        _searching.value = true
        searchCollector = viewModelScope.launch {
            // Recherche plein texte sur name + regles + stats + mots-cles.
            repository.searchCards(q).collect { results ->
                _searchResults.value = results
                _searching.value = false
            }
        }
    }

    fun searchWiki(query: String) {
        wikiSearchCollector?.cancel()
        val q = query.trim()
        if (q.isEmpty()) { _wikiSearchResults.value = emptyList(); _wikiSearching.value = false; return }
        _wikiSearching.value = true
        wikiSearchCollector = viewModelScope.launch {
            wikiRepository.searchSections(q).collect { results ->
                _wikiSearchResults.value = results
                _wikiSearching.value = false
            }
        }
    }

    fun loadWiki(system: GameSystem) {
        viewModelScope.launch {
            wikiRepository.observeSections(system).collect { _wikiSections.value = it }
        }
    }

    fun loadAllWiki() {
        viewModelScope.launch {
            wikiRepository.observeAllSections().collect { _wikiSections.value = it }
        }
    }

    private var entryCollector: kotlinx.coroutines.Job? = null
    private val saveMutex = kotlinx.coroutines.sync.Mutex()
    private var seedJob: kotlinx.coroutines.Job? = null

    init {
        // Force loading to end after 30s even if seed hangs
        viewModelScope.launch {
            kotlinx.coroutines.delay(30_000)
            if (_loading.value) {
                android.util.Log.w("VM", "Catalog seed timed out after 30s - forcing loading=false")
                _loading.value = false
                _catalogError.value = "Chargement du catalogue interrompu (timeout)"
            }
        }
        seedJob = viewModelScope.launch {
            try {
                android.util.Log.i("VM", "Starting catalog seed")
                repository.seedCatalog(getApplication())
                android.util.Log.i("VM", "Catalog seed complete")
            } catch (e: Exception) {
                android.util.Log.e("VM", "Catalog seed crashed", e)
                _catalogError.value = "Erreur: ${e.message?.take(80) ?: e.javaClass.simpleName}"
            } finally {
                _loading.value = false
                // Diagnostic inconditionnel : rapporte toujours l'état réel de la DB après seed,
                // que les données soient bonnes ou non. Indispensable pour comprendre un seed muet.
                reportSeedState()
            }
        }
        // Seed du wiki des règles (indépendant du catalogue, ne bloque pas le chargement).
        viewModelScope.launch {
            try {
                wikiRepository.seedWiki(getApplication())
            } catch (e: Exception) {
                android.util.Log.e("VM", "Wiki seed crashed", e)
            }
        }
        viewModelScope.launch { repository.observeLists().collect { _allLists.value = it } }
    }

    private suspend fun awaitCatalog() {
        seedJob?.let {
            kotlinx.coroutines.withTimeoutOrNull(10_000) { it.join() }
        }
    }

    private var catalogCollector: kotlinx.coroutines.Job? = null

    private fun reportSeedState() {
        if (seedReported) return
        seedReported = true
        viewModelScope.launch(Dispatchers.IO) {
            val diag = try { repository.catalogDiagnostics() } catch (e: Exception) { "diag-error:${e.message}" }
            CrashReporter.reportEvent(
                "Seed report: ${getVersionName()}",
                "État de la base après seed (rapport inconditionnel).\nApp version: ${getVersionName()}\n$diag"
            )
        }
    }
    private var seedReported = false

    private fun reportIfEmpty(system: GameSystem, cards: List<CardDefinition>) {
        if (cards.isEmpty() && !_loading.value && _catalogError.value == null) {
            viewModelScope.launch(Dispatchers.IO) {
                val diag = try { repository.catalogDiagnostics() } catch (e: Exception) { "diag-error:${e.message}" }
                CrashReporter.reportEvent(
                    "Catalogue vide: ${system.name}",
                    "Le catalogue reste vide pour ${system.name} sans erreur.\nApp version: ${getVersionName()}\n$diag"
                )
            }
        }
    }

    private fun getVersionName(): String = try {
        getApplication<Application>().packageManager.getPackageInfo(getApplication<Application>().packageName, 0).versionName ?: "?"
    } catch (e: Exception) { "?" }

    fun loadCatalog(system: GameSystem, factionId: String) {
        catalogCollector?.cancel()
        catalogCollector = viewModelScope.launch {
            awaitCatalog()
            repository.observeCards(system, factionId).collect { cards ->
                _cards.value = cards
                recalculate()
                reportIfEmpty(system, cards)
                _currentList.value?.takeIf { it.factionId == factionId }?.let(::watchEntries)
            }
        }
    }

    fun loadAllCatalog(system: GameSystem) {
        catalogCollector?.cancel()
        catalogCollector = viewModelScope.launch {
            awaitCatalog()
            repository.observeCards(system).collect { cards ->
                _cards.value = cards
                reportIfEmpty(system, cards)
            }
        }
    }

    // ── Ma collection (écran Réglages) ──
    private val _allCatalog = MutableStateFlow<List<CardDefinition>>(emptyList())
    val allCatalog: StateFlow<List<CardDefinition>> = _allCatalog.asStateFlow()
    private var allCatalogCollector: kotlinx.coroutines.Job? = null

    fun loadAllCatalogForCollection() {
        allCatalogCollector?.cancel()
        allCatalogCollector = viewModelScope.launch {
            awaitCatalog()
            repository.observeAllCards().collect { cards ->
                _allCatalog.value = cards
            }
        }
    }

    fun createList(name: String, system: GameSystem, factionId: String, limit: Int, onCreated: (String) -> Unit = {}) {
        viewModelScope.launch {
            awaitCatalog()
            val list = repository.createList(name, system, factionId, limit)
            _currentList.value = list
            _entries.value = emptyList()
            loadCatalog(system, factionId)
            persistAndValidate()
            onCreated(list.id)
        }
    }

    fun openList(id: String) {
        viewModelScope.launch {
            awaitCatalog()
            val list = repository.getList(id) ?: return@launch
            _currentList.value = list
            val system = GameSystem.valueOf(list.gameSystem)
            catalogCollector?.cancel()
            // Collect continu (pas .first()) : si le seed n'est pas fini, la liste
            // se remplit dès que Room invalide la table quand les cartes arrivent.
            catalogCollector = viewModelScope.launch {
                repository.observeCards(system, list.factionId).collect { cards ->
                    _cards.value = cards
                    recalculate()
                    _currentList.value?.takeIf { it.factionId == list.factionId }?.let(::watchEntries)
                }
            }
        }
    }

    /** Charge une liste comme armée adverse (mode solo 2 armées). */
    fun loadOpponentList(id: String) {
        viewModelScope.launch {
            awaitCatalog()
            val list = repository.getList(id) ?: return@launch
            _soloOppList.value = list
            val system = GameSystem.valueOf(list.gameSystem)
            repository.observeCards(system, list.factionId).collect { cards ->
                repository.observeEntries(list.id, cards).collect { loaded ->
                    // Préfixe "B:" sur les instanceId adverses → noms disjoints de mes UUID ("A:" implicite),
                    // évite la collision d'état si les 2 armées partagent la même liste (mêmes UUID).
                    _soloOppEntries.value = loaded.map { e ->
                        e.copy(
                            instanceId = "B:${e.instanceId}",
                            parentInstanceId = e.parentInstanceId?.let { "B:$it" }
                        )
                    }
                }
            }
        }
    }

    private fun watchEntries(list: BuilderListEntity) {
        entryCollector?.cancel()
        entryCollector = viewModelScope.launch {
            repository.observeEntries(list.id, _cards.value).collect { loaded ->
                _entries.value = loaded
                recalculate()
            }
        }
    }

    fun add(card: CardDefinition, parentId: String? = null, chosenSlot: ArmadaSlot? = null) {
        _entries.value = _entries.value + ListEntry(UUID.randomUUID().toString(), card, parentId, 1, chosenSlot)
        persistAndValidate()
    }

    fun remove(entry: ListEntry) {
        val removedIds = (_entries.value.filter { it.instanceId == entry.instanceId || it.parentInstanceId == entry.instanceId }).map { it.instanceId }.toSet()
        _entries.value = _entries.value.filterNot { it.instanceId in removedIds }
        persistAndValidate()
    }

    /** Renomme la liste courante et la persiste. Le nom vide est ignoré. */
    fun renameList(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val list = _currentList.value ?: return
        val renamed = list.copy(name = trimmed, updatedAt = System.currentTimeMillis())
        _currentList.value = renamed
        viewModelScope.launch { saveMutex.withLock { repository.renameList(list, trimmed) } }
    }

    private fun persistAndValidate() {
        val list = _currentList.value ?: return
        val updated = list.copy(updatedAt = System.currentTimeMillis())
        _currentList.value = updated
        val snapshot = _entries.value.toList()
        viewModelScope.launch { saveMutex.withLock { repository.saveList(updated, snapshot) } }
        recalculate()
    }

    private fun recalculate() {
        val list = _currentList.value ?: return
        val system = GameSystem.valueOf(list.gameSystem)
        val builder = BuilderList(list.id, list.name, system, list.factionId, list.pointsLimit, _entries.value)
        _validation.value = if (system == GameSystem.LEGION_V2) LegionV2Validator().validate(builder) else ArmadaV15Validator().validate(builder)
    }

    fun cardById(id: String) = _cards.value.firstOrNull { it.id == id }

        /** Importe une liste reçue via Nearby : crée une nouvelle liste et ajoute les entrées. */
        fun importSharedList(payload: ShareListPayload, onImported: (String) -> Unit = {}) {
            viewModelScope.launch {
                awaitCatalog()
                val system = runCatching { GameSystem.valueOf(payload.gameSystem) }.getOrDefault(GameSystem.LEGION_V2)
                val list = repository.createList(payload.name, system, payload.factionId, payload.pointsLimit)
                _currentList.value = list
                _entries.value = emptyList()
                loadCatalog(system, payload.factionId)
                // Ajoute les entrées reçues (cardId -> carte du catalogue).
                val byId = _cards.value.associateBy(CardDefinition::id)
                payload.entries.forEach { e ->
                    val card = byId[e.cardId] ?: return@forEach
                    _entries.value = _entries.value + ListEntry(
                        instanceId = UUID.randomUUID().toString(),
                        card = card,
                        parentInstanceId = e.parentInstanceId,
                        quantity = e.quantity,
                        chosenSlot = e.chosenSlot?.let(ArmadaSlot::valueOf)
                    )
                }
                persistAndValidate()
                onImported(list.id)
            }
        }
    }
