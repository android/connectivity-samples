package com.google.location.nearby.apps.hellouwb.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.location.nearby.apps.hellouwb.AppContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavBar(appContainer: AppContainer, isRanging: Boolean) {
  val navController = rememberNavController()
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
        RangingControlIcon(selected = isRanging)
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

@Preview @Composable fun PreviewAppNavBar() {}
