package com.legionforge.app.ui.nav

import androidx.compose.runtime.Composable
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
                    CardDetailScreen(entries = allEntries, initialIndex = idx, listId = lid, onBack = { navController.popBackStack() })
                }
        composable(Routes.TABLETOP, arguments = listOf(navArgument("listId") { type = NavType.StringType })) { entry ->
            val lid = entry.arguments?.getString("listId") ?: return@composable
            androidx.compose.runtime.LaunchedEffect(lid) { vm.openList(lid) }
            val ships = vm.entries.value.filter { it.card.kind.name == "ARMADA_SHIP" }
            val sizes = shipSizes
            val pawns = ships.mapIndexed { i, e ->
                com.legionforge.app.ui.screens.TabletopPawn(
                    id = e.instanceId,
                    name = e.card.name,
                    sizeClass = sizes[e.card.id] ?: "small",
                    imagePath = shipImages[e.card.id],
                    arcs = shipArcs[e.card.id],
                    basePath = shipBases[e.card.id],
                    xMm = (i - ships.size / 2f) * 120f,
                    yMm = 0f,
                )
            }
            com.legionforge.app.ui.screens.TabletopScreen(
                pawns = pawns,
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
