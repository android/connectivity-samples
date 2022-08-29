package com.google.location.nearby.apps.hellouwb.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
  object Home : Screen("home", "Home", Icons.Filled.Home)
  object Control : Screen("control", "Control", Icons.Filled.SettingsRemote)
  object Send : Screen("send", "Send", Icons.Filled.Send)
  object Settings : Screen("settings", "Setting", Icons.Filled.Settings)
}
