package com.legionforge.app.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.legionforge.app.data.model.ListEntry
import com.legionforge.app.data.model.ArmadaSlot
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.legionforge.app.R
import com.legionforge.app.data.model.GameSystem
import com.legionforge.app.data.model.CardKind
import com.legionforge.app.ui.screens.ArmyBuilderScreen
import com.legionforge.app.ui.screens.CardDetailScreen
import com.legionforge.app.ui.screens.CollectionScreen
import com.legionforge.app.ui.screens.FactionPickerScreen
import com.legionforge.app.ui.screens.HomeScreen
import com.legionforge.app.ui.screens.SearchScreen
import com.legionforge.app.ui.screens.SettingsScreen
import com.legionforge.app.ui.screens.ShareScreen
import com.legionforge.app.ui.screens.WikiScreen
import com.legionforge.app.ui.viewmodel.ArmyBuilderViewModel
import com.legionforge.app.ui.viewmodel.ShareListPayload
import com.legionforge.app.ui.viewmodel.PlaySyncViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination

object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val FACTION_PICKER = "faction_picker/{system}"
    const val ARMY_BUILDER = "army_builder/{listId}"
    const val SETTINGS = "settings"
    const val COLLECTION = "collection"
    const val WIKI = "wiki"
    const val CARD_DETAIL = "card_detail/{listId}/{entryInstanceId}"
        const val SHARE = "share/{listId}"
    const val TABLETOP = "tabletop/{listId}"
        fun factionPicker(system: GameSystem) = "faction_picker/${system.name}"
        fun armyBuilder(listId: String) = "army_builder/$listId"
        fun cardDetail(listId: String, entryInstanceId: String) = "card_detail/$listId/$entryInstanceId"
        fun share(listId: String) = "share/$listId"
        fun tabletop(listId: String) = "tabletop/$listId"
    }

@Composable
fun LegionForgeNavHost(navController: NavHostController = rememberNavController()) {
    val vm: ArmyBuilderViewModel = viewModel()
    val appContext = androidx.compose.ui.platform.LocalContext.current
    val shipSizes = androidx.compose.runtime.remember { com.legionforge.app.ui.screens.loadShipSizes(appContext) }
    val shipImages = androidx.compose.runtime.remember { com.legionforge.app.ui.screens.loadShipImages(appContext) }
    val shipArcs = androidx.compose.runtime.remember { com.legionforge.app.ui.screens.loadFiringArcs(appContext) }
    val shipBases = androidx.compose.runtime.remember { com.legionforge.app.ui.screens.loadShipBases(appContext) }
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            androidx.compose.runtime.LaunchedEffect(Unit) { vm.loadAllCatalog(GameSystem.LEGION_V2) }
            HomeScreen(
                onNewList = { system -> navController.navigate(Routes.factionPicker(system)) },
                onOpenList = { listId -> vm.openList(listId); navController.navigate(Routes.armyBuilder(listId)) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
                onSearch = { navController.navigate(Routes.SEARCH) },
                onWiki = { navController.navigate(Routes.WIKI) },
                vm = vm
            )
        }
        composable(Routes.SEARCH) {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onCardClick = { navController.popBackStack() },
                vm = vm
            )
        }
        composable(Routes.WIKI) {
            WikiScreen(
                onBack = { navController.popBackStack() },
                vm = vm
            )
        }
        composable(Routes.FACTION_PICKER, arguments = listOf(navArgument("system") { type = NavType.StringType })) { entry ->
            val system = runCatching { GameSystem.valueOf(entry.arguments?.getString("system") ?: "LEGION_V2") }.getOrDefault(GameSystem.LEGION_V2)
            val defaultName = if (system == GameSystem.LEGION_V2) stringResource(R.string.new_list_title) else stringResource(R.string.new_fleet_title)
            FactionPickerScreen(system, onFactionSelected = { factionId ->
                vm.createList(defaultName, system, factionId, if (system == GameSystem.LEGION_V2) 1000 else 400) { id ->
                    navController.navigate(Routes.armyBuilder(id))
                }
            }, vm = vm)
        }
        composable(Routes.ARMY_BUILDER, arguments = listOf(navArgument("listId") { type = NavType.StringType })) { entry ->
            val id = entry.arguments?.getString("listId") ?: return@composable
            ArmyBuilderScreen(listId = id, viewModel = vm, onBack = {
                navController.navigate(Routes.HOME) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = false }
                    launchSingleTop = true
                }
            }, onPlayCard = { listId, entryId ->
                navController.navigate(Routes.cardDetail(listId, entryId))
            }, onPlayCondensed = { listId ->
                            // Le mode play condensé est désormais fusionné dans la vue plein écran :
                            // le bouton ▶ ouvre CardDetailScreen sur le premier élément jouable.
                            val firstPlayable = vm.entries.value.firstOrNull { it.parentInstanceId == null && (it.card.kind == CardKind.LEGION_UNIT || it.card.kind == CardKind.ARMADA_SHIP) }
                            if (firstPlayable != null) navController.navigate(Routes.cardDetail(listId, firstPlayable.instanceId))
                        }, onShare = {
                            navController.navigate(Routes.share(id))
                        }, onTabletop = {
                            navController.navigate(Routes.tabletop(id))
                        })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onCollection = { navController.navigate(Routes.COLLECTION) }
            )
        }
        composable(Routes.COLLECTION) {
            CollectionScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.CARD_DETAIL, arguments = listOf(navArgument("listId") { type = NavType.StringType }, navArgument("entryInstanceId") { type = NavType.StringType })) { entry ->
                    val lid = entry.arguments?.getString("listId") ?: return@composable
                    val eid = entry.arguments?.getString("entryInstanceId") ?: return@composable
                    // Charge la liste dans le VM : sinon vm.currentList est null et la synchro
                    // 2 joueurs ne peut pas envoyer la liste adverse (sendList -> return).
                    androidx.compose.runtime.LaunchedEffect(lid) { vm.openList(lid) }
                    val allEntries = vm.entries.value
                    val idx = allEntries.indexOfFirst { it.instanceId == eid }.coerceAtLeast(0)
                    CardDetailScreen(entries = allEntries, initialIndex = idx, listId = lid, onBack = { navController.popBackStack() }, onOpenTabletop = { id -> navController.navigate(Routes.tabletop(id)) })
                }
        composable(Routes.TABLETOP, arguments = listOf(navArgument("listId") { type = NavType.StringType })) { entry ->
            val lid = entry.arguments?.getString("listId") ?: return@composable
            androidx.compose.runtime.LaunchedEffect(lid) { vm.openList(lid) }
            val ships = vm.entries.value.filter { it.card.kind.name == "ARMADA_SHIP" || it.card.kind.name == "ARMADA_SQUADRON" }
            val playSyncVmTabletop: PlaySyncViewModel = viewModel()
            val appCtxT = androidx.compose.ui.platform.LocalContext.current.applicationContext
            val tabletopRound by androidx.compose.runtime.produceState(initialValue = 1, lid) {
                value = try {
                    val ent = com.legionforge.app.data.local.LegionForgeDatabase.getInstance(appCtxT).gameSessionDao().getByList(lid)
                    com.legionforge.app.data.model.PlaySessionSnapshot.fromJson(ent?.snapshot ?: "")?.armadaRound ?: 1
                } catch (_: Exception) { 1 }
            }
            val tabletopPhase by androidx.compose.runtime.produceState(initialValue = "COMMAND", lid) {
                value = try {
                    val ent = com.legionforge.app.data.local.LegionForgeDatabase.getInstance(appCtxT).gameSessionDao().getByList(lid)
                    (com.legionforge.app.data.model.PlaySessionSnapshot.fromJson(ent?.snapshot ?: "")?.armadaPhase ?: "command").uppercase()
                } catch (_: Exception) { "COMMAND" }
            }
            val oppListJson by playSyncVmTabletop.opponentListJson.collectAsState()
            val oppShipsEntries = remember(oppListJson, vm.cards.value) {
                val opp = try { com.google.gson.Gson().fromJson(oppListJson, com.legionforge.app.ui.viewmodel.ShareListPayload::class.java) } catch (_: Exception) { null }
                (opp?.entries ?: emptyList()).mapNotNull { e ->
                    vm.cards.value.firstOrNull { it.id == e.cardId }?.let { card ->
                        ListEntry(
                            instanceId = e.instanceId?.let { "B:$it" } ?: "opp_${e.cardId}_${e.parentInstanceId ?: "root"}",
                            card = card,
                            parentInstanceId = e.parentInstanceId?.let { "B:$it" },
                            quantity = e.quantity,
                            chosenSlot = e.chosenSlot?.let { s -> runCatching { ArmadaSlot.valueOf(s) }.getOrNull() }
                        )
                    }
                }.filter { it.card.kind.name == "ARMADA_SHIP" || it.card.kind.name == "ARMADA_SQUADRON" }
            }
            val allShips = ships + oppShipsEntries
            val sizes = shipSizes
            val pawns = allShips.mapIndexed { i, e ->
                com.legionforge.app.ui.screens.TabletopPawn(
                    id = e.instanceId,
                    name = e.card.name,
                    sizeClass = sizes[e.card.id] ?: "small",
                    imagePath = shipImages[e.card.id],
                    arcs = shipArcs[e.card.id],
                    basePath = shipBases[e.card.id],
                    hull = com.legionforge.app.ui.screens.ArmadaStatsParser.parse(e.card.shipStats, com.legionforge.app.data.model.CardKind.ARMADA_SHIP)?.hull ?: 0,
                    shieldFront = com.legionforge.app.ui.screens.ArmadaStatsParser.parse(e.card.shipStats, com.legionforge.app.data.model.CardKind.ARMADA_SHIP)?.shieldFront ?: 0,
                    maxSpeed = (com.legionforge.app.ui.screens.ArmadaStatsParser.parse(e.card.shipStats, com.legionforge.app.data.model.CardKind.ARMADA_SHIP)?.maxSpeed ?: 0),
                    speedChart = (com.legionforge.app.ui.screens.ArmadaStatsParser.parse(e.card.shipStats, com.legionforge.app.data.model.CardKind.ARMADA_SHIP)?.speedChart ?: emptyList()).map { pos ->
                        pos.mapNotNull { (k, v) -> k.toIntOrNull()?.let { it to v } }.toMap()
                    },
                    xMm = (run { val mine = allShips.count { !(it.instanceId.startsWith("B:") || it.instanceId.startsWith("opp_")) }; val sameSide = allShips.take(i).count { (it.instanceId.startsWith("B:") || it.instanceId.startsWith("opp_")) == (e.instanceId.startsWith("B:") || e.instanceId.startsWith("opp_")) }; val isOppE = e.instanceId.startsWith("B:") || e.instanceId.startsWith("opp_"); val n = if (isOppE) allShips.size - mine else mine; (sameSide - n / 2f) * 160f }),
                    isOpp = e.instanceId.startsWith("B:") || e.instanceId.startsWith("opp_"),
                    isSquadron = e.card.kind.name == "ARMADA_SQUADRON",
                    yMm = if (e.instanceId.startsWith("B:") || e.instanceId.startsWith("opp_")) -420f else 420f,
                )
            }
            com.legionforge.app.ui.screens.TabletopScreen(
                pawns = pawns,
                round = tabletopRound,
                phase = tabletopPhase,
                onBack = { navController.popBackStack() },
                onOpenPlay = { instanceId -> navController.navigate(Routes.cardDetail(lid, instanceId)) },
            )
        }
        composable(Routes.SHARE, arguments = listOf(navArgument("listId") { type = NavType.StringType })) { entry ->
            val lid = entry.arguments?.getString("listId") ?: return@composable
            val list = vm.currentList.value
            ShareScreen(
                list = list,
                entries = vm.entries.value,
                onImport = { payload ->
                    vm.importSharedList(payload) { newId ->
                        navController.navigate(Routes.armyBuilder(newId)) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = false }
                            launchSingleTop = true
                        }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
