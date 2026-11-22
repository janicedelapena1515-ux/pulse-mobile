package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.CherryBackground
import com.example.ui.components.CustomBottomNavigation
import com.example.ui.components.PulseTab
import com.example.ui.screens.BoostScreen
import com.example.ui.screens.GamesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MonitorScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StorageCleanerScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.theme.CardBackground
import com.example.ui.theme.MainText
import com.example.ui.theme.PulseMobileTheme
import com.example.ui.theme.SoftPinkHighlight
import com.example.ui.viewmodel.GamingViewModel
import com.example.ui.viewmodel.PulseMainViewModel
import com.example.ui.viewmodel.StorageViewModel

private const val ROUTE_STORAGE_CLEANER = "storage_cleaner"
private const val ROUTE_SETTINGS = "settings"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            PulseMobileApp()
        }
    }
}

@Composable
fun PulseMobileApp() {
    val mainViewModel: PulseMainViewModel = viewModel()
    val storageViewModel: StorageViewModel = viewModel()
    val gamingViewModel: GamingViewModel = viewModel()

    val telemetry by mainViewModel.telemetry.collectAsStateWithLifecycle()
    val history by mainViewModel.history.collectAsStateWithLifecycle()
    val preferences by mainViewModel.preferences.collectAsStateWithLifecycle()
    val opportunities by mainViewModel.opportunities.collectAsStateWithLifecycle()
    val logs by mainViewModel.optimizationLogs.collectAsStateWithLifecycle()
    val totalRecovered by mainViewModel.totalBytesRecovered.collectAsStateWithLifecycle()
    val isScanning by mainViewModel.isBoostScanning.collectAsStateWithLifecycle()
    val scanProgress by mainViewModel.boostScanProgress.collectAsStateWithLifecycle()
    val scanStage by mainViewModel.boostStageMessage.collectAsStateWithLifecycle()
    val lastReport by mainViewModel.lastBoostReport.collectAsStateWithLifecycle()
    val mainMessage by mainViewModel.uiMessage.collectAsStateWithLifecycle()

    val safFolders by storageViewModel.safFolders.collectAsStateWithLifecycle()
    val scanSummary by storageViewModel.scanSummary.collectAsStateWithLifecycle()
    val visibleFiles by storageViewModel.visibleFiles.collectAsStateWithLifecycle()
    val selectedCategory by storageViewModel.selectedCategory.collectAsStateWithLifecycle()
    val sortOption by storageViewModel.sortOption.collectAsStateWithLifecycle()
    val selectedUris by storageViewModel.selectedUris.collectAsStateWithLifecycle()
    val inspectedMedia by storageViewModel.inspectedMedia.collectAsStateWithLifecycle()
    val storageMessage by storageViewModel.feedbackMessage.collectAsStateWithLifecycle()

    val games by gamingViewModel.games.collectAsStateWithLifecycle()
    val gamingSessions by gamingViewModel.sessions.collectAsStateWithLifecycle()
    val selectedProfile by gamingViewModel.selectedProfile.collectAsStateWithLifecycle()
    val activeSession by gamingViewModel.activeSession.collectAsStateWithLifecycle()
    val installedCandidates by gamingViewModel.installedCandidates.collectAsStateWithLifecycle()
    val isLoadingApps by gamingViewModel.isLoadingApps.collectAsStateWithLifecycle()
    val gamingMessage by gamingViewModel.gamingFeedback.collectAsStateWithLifecycle()

    // Lifecycle-aware monitoring: only poll hardware sensors & telemetry when app is active
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        mainViewModel.startMonitoring()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        mainViewModel.refreshNow()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        mainViewModel.stopMonitoring()
    }

    val reducedMotion = preferences.reducedAnimations ||
        telemetry.systemReducedAnimations ||
        (activeSession?.profile?.reduceUiEffects == true)

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(mainMessage) {
        mainMessage?.let {
            snackbarHostState.showSnackbar(it)
            mainViewModel.clearUiMessage()
        }
    }
    LaunchedEffect(storageMessage) {
        storageMessage?.let {
            snackbarHostState.showSnackbar(it)
            storageViewModel.clearFeedback()
        }
    }
    LaunchedEffect(gamingMessage) {
        gamingMessage?.let {
            snackbarHostState.showSnackbar(it)
            gamingViewModel.clearFeedback()
        }
    }

    PulseMobileTheme(
        reducedMotion = reducedMotion,
        hapticsEnabled = preferences.hapticsEnabled
    ) {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route
        val showBottomNav = currentRoute in PulseTab.entries.map { it.route }

        val navigateToTab: (PulseTab) -> Unit = { tab ->
            navController.navigate(tab.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }

        CherryBackground {
            Scaffold(
                containerColor = Color.Transparent,
                snackbarHost = {
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(bottom = if (showBottomNav) 84.dp else 12.dp)
                    ) { data ->
                        Snackbar(
                            snackbarData = data,
                            containerColor = CardBackground,
                            contentColor = MainText,
                            actionColor = SoftPinkHighlight
                        )
                    }
                }
            ) { _ ->
                Box(modifier = Modifier.fillMaxSize()) {
                    val animDur = if (reducedMotion) 0 else 240
                    NavHost(
                        navController = navController,
                        startDestination = PulseTab.HOME.route,
                        enterTransition = { fadeIn(animationSpec = tween(animDur)) },
                        exitTransition = { fadeOut(animationSpec = tween(animDur)) }
                    ) {
                        composable(PulseTab.HOME.route) {
                            HomeScreen(
                                telemetry = telemetry,
                                isScanning = isScanning,
                                scanProgress = scanProgress,
                                scanStageMessage = scanStage,
                                lastReport = lastReport,
                                recentLogs = logs,
                                totalBytesRecovered = totalRecovered,
                                onBoostClick = { mainViewModel.runBoostDeviceAnalysis() },
                                onOpenSettings = { navController.navigate(ROUTE_SETTINGS) },
                                onNavigateToBoost = { navigateToTab(PulseTab.BOOST) },
                                onNavigateToStorageCleaner = { navController.navigate(ROUTE_STORAGE_CLEANER) }
                            )
                        }

                        composable(PulseTab.BOOST.route) {
                            BoostScreen(
                                telemetry = telemetry,
                                opportunities = opportunities,
                                isScanning = isScanning,
                                scanProgress = scanProgress,
                                scanStageMessage = scanStage,
                                lastReport = lastReport,
                                logs = logs,
                                onRunFullBoost = { mainViewModel.runBoostDeviceAnalysis() },
                                onExecuteOpportunity = { opp ->
                                    mainViewModel.executeOpportunity(opp) {
                                        navController.navigate(ROUTE_STORAGE_CLEANER)
                                    }
                                },
                                onClearLogs = { mainViewModel.clearOptimizationLogs() }
                            )
                        }

                        composable(PulseTab.GAMES.route) {
                            GamesScreen(
                                telemetry = telemetry,
                                games = games,
                                sessions = gamingSessions,
                                selectedProfile = selectedProfile,
                                activeSession = activeSession,
                                installedCandidates = installedCandidates,
                                isLoadingApps = isLoadingApps,
                                onSelectProfile = { gamingViewModel.selectProfile(it) },
                                onStartSession = { game, launch ->
                                    gamingViewModel.startSession(game, launch) { overrideMs ->
                                        mainViewModel.setGamingPollingOverride(overrideMs)
                                    }
                                },
                                onStopSession = {
                                    gamingViewModel.stopSession { overrideMs ->
                                        mainViewModel.setGamingPollingOverride(overrideMs)
                                    }
                                },
                                onLoadInstalledApps = { gamingViewModel.loadInstalledApps() },
                                onAddGame = { gamingViewModel.addGame(it) },
                                onRemoveGame = { gamingViewModel.removeGame(it) }
                            )
                        }

                        composable(PulseTab.MONITOR.route) {
                            MonitorScreen(
                                telemetry = telemetry,
                                history = history,
                                refreshIntervalMs = preferences.monitorRefreshMs,
                                onSelectRefreshInterval = { mainViewModel.setMonitorRefreshMs(it) }
                            )
                        }

                        composable(PulseTab.TOOLS.route) {
                            ToolsScreen(
                                telemetry = telemetry,
                                scanSummary = scanSummary,
                                totalRecoveredBytes = totalRecovered,
                                inspectedMedia = inspectedMedia,
                                onOpenStorageCleaner = { navController.navigate(ROUTE_STORAGE_CLEANER) },
                                onOpenSettings = { navController.navigate(ROUTE_SETTINGS) },
                                onInspectPhotoPickerUri = { storageViewModel.inspectPhotoPickerUri(it) },
                                onDismissInspectedMedia = { storageViewModel.dismissInspectedMedia() }
                            )
                        }

                        composable(ROUTE_STORAGE_CLEANER) {
                            StorageCleanerScreen(
                                summary = scanSummary,
                                safFolders = safFolders,
                                visibleFiles = visibleFiles,
                                selectedCategory = selectedCategory,
                                sortOption = sortOption,
                                selectedUris = selectedUris,
                                totalBytesRecovered = totalRecovered,
                                onBack = { navController.popBackStack() },
                                onAddSafFolder = { storageViewModel.addSafFolder(it) },
                                onRemoveSafFolder = { storageViewModel.removeSafFolder(it) },
                                onRescan = { storageViewModel.rescanAll() },
                                onSelectCategory = { storageViewModel.selectCategory(it) },
                                onSelectSortOption = { storageViewModel.selectSortOption(it) },
                                onToggleSelect = { storageViewModel.toggleFileSelection(it) },
                                onSelectAllVisible = { storageViewModel.selectAllVisible() },
                                onDeleteSelected = { storageViewModel.deleteSelectedFiles() },
                                onClearAppCache = { storageViewModel.clearAppCache() }
                            )
                        }

                        composable(ROUTE_SETTINGS) {
                            SettingsScreen(
                                preferences = preferences,
                                telemetry = telemetry,
                                safFoldersCount = safFolders.size,
                                onBack = { navController.popBackStack() },
                                onSetRefreshMs = { mainViewModel.setMonitorRefreshMs(it) },
                                onSetReducedAnimations = { mainViewModel.setReducedAnimations(it) },
                                onSetHaptics = { mainViewModel.setHapticsEnabled(it) },
                                onSetLargeFileMb = { mainViewModel.setLargeFileThresholdMb(it) },
                                onSetOldDownloadDays = { mainViewModel.setOldDownloadDays(it) },
                                onSetScheduledHealthCheck = { mainViewModel.setScheduledHealthCheck(it) }
                            )
                        }
                    }

                    if (showBottomNav) {
                        CustomBottomNavigation(
                            currentRoute = currentRoute,
                            onNavigate = navigateToTab,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }
    }
}
