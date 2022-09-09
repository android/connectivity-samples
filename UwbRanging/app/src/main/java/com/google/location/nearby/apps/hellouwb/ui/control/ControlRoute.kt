package com.google.location.nearby.apps.hellouwb.ui.control

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun ControlRoute(controlViewModel : ControlViewModel) {
  //ControlScreen()
  val uiState by controlViewModel.uiState.collectAsState()
  ControlScreen(uiState = uiState)

}
