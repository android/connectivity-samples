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

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.apps.hellouwb.AppContainer
import com.google.apps.hellouwb.ui.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavBar(
  appContainer: AppContainer,
  isRanging: Boolean,
  startRanging: () -> Unit,
  stopRanging: () -> Unit,
) {
  val navController = rememberNavController()
  val rangingState = remember { mutableStateOf(isRanging) }
  Scaffold(
    bottomBar = {
      NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        items.forEach { screen ->
          NavigationBarItem(
            icon = { Image(imageVector = screen.icon, contentDescription = null) },
            label = { Text(screen.title) },
            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
            onClick = { AppNavigation(navController).navTo(screen.route) }
          )
        }
      }
    },
    floatingActionButtonPosition = FabPosition.End,
    floatingActionButton = {
      FloatingActionButton(shape = CircleShape, onClick = {}, contentColor = White) {
        RangingControlIcon(selected = rangingState.value) {
          rangingState.value = it
          if (it) {
            startRanging()
          } else {
            stopRanging()
          }
        }
      }
    }
  ) { innerPadding ->
    AppNavGraph(
      appContainer = appContainer,
      modifier = Modifier.padding(innerPadding),
      navController = navController
    )
  }
}

private val items = listOf(Screen.Home, Screen.Control, Screen.Send, Screen.Settings)
