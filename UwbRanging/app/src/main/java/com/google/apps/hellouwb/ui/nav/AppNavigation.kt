/*
 *
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.google.apps.hellouwb.ui.nav

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
