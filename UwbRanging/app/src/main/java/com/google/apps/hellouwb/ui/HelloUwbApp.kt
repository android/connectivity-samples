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

package com.google.apps.hellouwb.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.apps.hellouwb.AppContainer
import com.google.apps.hellouwb.ui.nav.AppNavBar
import com.google.apps.hellouwb.ui.ranging.RangingViewModel
import com.google.apps.hellouwb.ui.theme.HellouwbTheme

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
      stopRanging = { rangingViewModel.stopRanging() }
    )
  }
}
