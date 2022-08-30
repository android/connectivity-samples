package com.google.location.nearby.apps.hellouwb.ui.nav

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

object AppDestination {
  const val HOME_ROUTE = "home"
  const val CONTROL_ROUTE = "control"
  const val SEND_ROUTE = "send"
  const val SETTINGS_ROUTE = "settings"
}

class AppNavigation(private val navController: NavHostController) {

  val navToHome: () -> Unit = { navTo(AppDestination.HOME_ROUTE) }

  val navToControl: () -> Unit = { navTo(AppDestination.CONTROL_ROUTE) }

  val navToSend: () -> Unit = { navTo(AppDestination.SEND_ROUTE) }

  val navToSettings: () -> Unit = { navTo(AppDestination.SETTINGS_ROUTE) }

  fun navTo(destination: String) {
    navController.navigate(destination) {
      popUpTo(navController.graph.findStartDestination().id) { saveState = true }
      launchSingleTop = true
      restoreState = true
    }
  }
}
