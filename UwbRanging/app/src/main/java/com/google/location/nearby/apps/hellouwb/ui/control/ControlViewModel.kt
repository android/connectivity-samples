/*
 *
 *  * Copyright (C) 2022 The Android Open Source Project
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.google.location.nearby.apps.hellouwb.ui.control

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.location.nearby.apps.hellouwb.data.UwbRangingControlSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ControlViewModel(uwbRangingControlSource: UwbRangingControlSource) : ViewModel() {

  private val _uiState: MutableStateFlow<ControlUiState> =
    MutableStateFlow(ControlUiStateImpl(false))

  companion object {
    fun provideFactory(
      uwbRangingControlSource: UwbRangingControlSource
    ): ViewModelProvider.Factory =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
          return ControlViewModel(uwbRangingControlSource) as T
        }
      }
  }

  private data class ControlUiStateImpl(
    override var isDoorLocked: Boolean,
  ) : ControlUiState


  val uiState = _uiState.asStateFlow()

  interface ControlUiState {
    var isDoorLocked: Boolean
  }

}
