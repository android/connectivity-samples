package com.google.location.nearby.apps.hellouwb.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.location.nearby.apps.hellouwb.AppContainer
import com.google.location.nearby.apps.hellouwb.ui.nav.AppNavBar
import com.google.location.nearby.apps.hellouwb.ui.ranging.RangingViewModel
import com.google.location.nearby.apps.hellouwb.ui.theme.HellouwbTheme

@Composable
fun HelloUwbApp(appContainer: AppContainer) {
  val rangingViewModel: RangingViewModel =
      viewModel(factory = RangingViewModel.provideFactory(appContainer.rangingResultSource))
  val uiState by rangingViewModel.uiState.collectAsState()
  HellouwbTheme {
    AppNavBar(
        appContainer = appContainer,
        isRanging = uiState,
        startRanging = { rangingViewModel.startRanging() },
        stopRanging = { rangingViewModel.stopRanging() })
  }
}
