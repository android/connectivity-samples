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

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.apps.hellouwb.AppContainer
import com.google.apps.hellouwb.ui.control.ControlRoute
import com.google.apps.hellouwb.ui.control.ControlViewModel
import com.google.apps.hellouwb.ui.home.HomeRoute
import com.google.apps.hellouwb.ui.home.HomeViewModel
import com.google.apps.hellouwb.ui.send.SendRoute
import com.google.apps.hellouwb.ui.send.SendViewModel
import com.google.apps.hellouwb.ui.settings.SettingsRoute
import com.google.apps.hellouwb.ui.settings.SettingsViewModel

@Composable
fun AppNavGraph(
  appContainer: AppContainer,
  modifier: Modifier = Modifier,
  navController: NavHostController = rememberNavController(),
  startDestination: String = AppDestination.HOME_ROUTE,
) {
  NavHost(navController = navController, startDestination = startDestination, modifier = modifier) {
    composable(AppDestination.HOME_ROUTE) {
      val homeViewModel: HomeViewModel =
        viewModel(factory = HomeViewModel.provideFactory(appContainer.rangingResultSource))
      HomeRoute(homeViewModel = homeViewModel)
    }
    composable(AppDestination.CONTROL_ROUTE) {
      val controlViewModel: ControlViewModel =
        viewModel(
          factory = ControlViewModel.provideFactory(
            appContainer.rangingResultSource,
            appContainer.settingsStore
          )
        )
      //ControlRoute()
      ControlRoute(controlViewModel = controlViewModel)

    }
    composable(AppDestination.SEND_ROUTE) {
      val sendViewModel: SendViewModel =
        viewModel(
          factory =
          SendViewModel.provideFactory(
            appContainer.rangingResultSource,
            appContainer.contentResolver
          )
        )
      SendRoute(sendViewModel = sendViewModel)
    }
    composable(AppDestination.SETTINGS_ROUTE) {
      val settingsViewModel: SettingsViewModel =
        viewModel(
          factory =
          SettingsViewModel.provideFactory(
            appContainer.rangingResultSource,
            appContainer.settingsStore
          )
        )
      SettingsRoute(settingsViewModel)
    }
  }
}
