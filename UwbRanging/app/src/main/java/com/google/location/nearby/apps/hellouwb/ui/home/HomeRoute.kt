package com.google.location.nearby.apps.hellouwb.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun HomeRoute(homeViewModel: HomeViewModel) {
  val uiState by homeViewModel.uiState.collectAsState()
  HomeScreen(uiState = uiState)
}
