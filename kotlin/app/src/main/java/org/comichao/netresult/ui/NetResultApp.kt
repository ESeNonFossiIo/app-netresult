package org.comichao.netresult.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun NetResultApp(vm: MainViewModel = viewModel()) {
    val nav = rememberNavController()
    val paletteKey by vm.palette.collectAsState()
    NetResultTheme(paletteFor(paletteKey)) {
    NavHost(nav, startDestination = "home") {
        composable("home") {
            HomeScreen(vm, onLog = { nav.navigate("log?draft=false") }, onResume = { nav.navigate("log?draft=true") },
                onLive = { nav.navigate("live") }, onHistory = { nav.navigate("history") }, onSettings = { nav.navigate("settings") })
        }
        composable("log?draft={draft}") { entry ->
            LogGameScreen(vm, resume = entry.arguments?.getString("draft") == "true", onBack = { nav.popBackStack() })
        }
        composable("live") { LiveMatchScreen(vm, onBack = { nav.popBackStack() }) }
        composable("history") { HistoryScreen(vm, onBack = { nav.popBackStack() }) }
        composable("settings") { SettingsScreen(vm, onBack = { nav.popBackStack() }) }
    }
    }
}
