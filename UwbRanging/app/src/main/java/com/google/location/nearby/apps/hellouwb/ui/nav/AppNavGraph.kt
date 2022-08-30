package com.google.location.nearby.apps.hellouwb.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.location.nearby.apps.hellouwb.AppContainer
import com.google.location.nearby.apps.hellouwb.ui.control.ControlRoute
import com.google.location.nearby.apps.hellouwb.ui.home.HomeRoute
import com.google.location.nearby.apps.hellouwb.ui.home.HomeViewModel
import com.google.location.nearby.apps.hellouwb.ui.send.SendRoute
import com.google.location.nearby.apps.hellouwb.ui.settings.SettingsRoute
import com.google.location.nearby.apps.hellouwb.ui.settings.SettingsViewModel

@Composable
fun AppNavGraph(
  appContainer: AppContainer,
  modifier: Modifier = Modifier,
  navController: NavHostController = rememberNavController(),
  startDestination: String = AppDestination.HOME_ROUTE
) {
  NavHost(navController = navController, startDestination = startDestination, modifier = modifier) {
    composable(AppDestination.HOME_ROUTE) {
      val homeViewModel: HomeViewModel =
        viewModel(factory = HomeViewModel.provideFactory(appContainer.rangingResultSource))
      HomeRoute(homeViewModel = homeViewModel)
    }
    composable(AppDestination.CONTROL_ROUTE) { ControlRoute() }
    composable(AppDestination.SEND_ROUTE) { SendRoute() }
    composable(AppDestination.SETTINGS_ROUTE) {
      val settingsViewModel: SettingsViewModel =
        viewModel(factory = SettingsViewModel.provideFactory(appContainer.rangingResultSource, appContainer.settingsStore))
      SettingsRoute(settingsViewModel) }
  }
}
