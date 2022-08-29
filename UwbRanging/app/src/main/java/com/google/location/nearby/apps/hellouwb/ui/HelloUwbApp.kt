package com.google.location.nearby.apps.hellouwb.ui

import androidx.compose.runtime.Composable
import com.google.location.nearby.apps.hellouwb.AppContainer
import com.google.location.nearby.apps.hellouwb.ui.theme.HellouwbTheme

@Composable
fun HelloUwbApp(appContainer: AppContainer) {
  HellouwbTheme { AppNavBar(appContainer = appContainer, isRanging = false) }
}
