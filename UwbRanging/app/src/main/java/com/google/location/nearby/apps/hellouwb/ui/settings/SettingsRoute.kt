package com.google.location.nearby.apps.hellouwb.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun SettingsRoute(settingsViewModel: SettingsViewModel) {
  val uiState by settingsViewModel.uiState.collectAsState()
  SettingsScreen(
      uiState = uiState,
      updateDeviceDisplayName = { settingsViewModel.updateDeviceDisplayName(it) },
      updateDeviceType = { settingsViewModel.updateDeviceType(it) })
}
