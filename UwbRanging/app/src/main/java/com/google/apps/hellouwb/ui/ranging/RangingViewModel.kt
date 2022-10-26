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

package com.google.apps.hellouwb.ui.ranging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.apps.hellouwb.data.UwbRangingControlSource
import kotlinx.coroutines.flow.*

class RangingViewModel(private val uwbRangingControlSource: UwbRangingControlSource) : ViewModel() {

  private val _uiState: MutableStateFlow<Boolean> = MutableStateFlow(false)

  val uiState = _uiState.asStateFlow()

  init {
    uwbRangingControlSource.isRunning.onEach { _uiState.update { it } }.launchIn(viewModelScope)
  }

  fun startRanging() {
    uwbRangingControlSource.start()
  }

  fun stopRanging() {
    uwbRangingControlSource.stop()
  }

  companion object {
    fun provideFactory(
      uwbRangingControlSource: UwbRangingControlSource,
    ): ViewModelProvider.Factory =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
          return RangingViewModel(uwbRangingControlSource) as T
        }
      }
  }
}
