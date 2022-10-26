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

package com.google.apps.hellouwb.ui.home

import androidx.core.uwb.RangingPosition
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.apps.hellouwb.data.UwbRangingControlSource
import com.google.apps.uwbranging.EndpointEvents
import com.google.apps.uwbranging.UwbEndpoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*

class HomeViewModel(uwbRangingControlSource: UwbRangingControlSource) : ViewModel() {

  private val _uiState: MutableStateFlow<HomeUiState> =
    MutableStateFlow(HomeUiStateImpl(listOf(), listOf(), false))

  private val endpoints = mutableListOf<UwbEndpoint>()
  private val endpointPositions = mutableMapOf<UwbEndpoint, RangingPosition>()
  private var isRanging = false

  private fun updateUiState(): HomeUiState {
    return HomeUiStateImpl(
      endpoints
        .mapNotNull { endpoint ->
          endpointPositions[endpoint]?.let { position -> ConnectedEndpoint(endpoint, position) }
        }
        .toList(),
      endpoints.filter { !endpointPositions.containsKey(it) }.toList(),
      isRanging
    )
  }

  val uiState = _uiState.asStateFlow()

  init {
    uwbRangingControlSource
      .observeRangingResults()
      .onEach { result ->
        when (result) {
          is EndpointEvents.EndpointFound -> endpoints.add(result.endpoint)
          is EndpointEvents.UwbDisconnected -> endpointPositions.remove(result.endpoint)
          is EndpointEvents.PositionUpdated -> endpointPositions[result.endpoint] = result.position
          is EndpointEvents.EndpointLost -> {
            endpoints.remove(result.endpoint)

            endpointPositions.remove(result.endpoint)
          }
          else -> return@onEach
        }
        _uiState.update { updateUiState() }
      }
      .launchIn(viewModelScope)

    uwbRangingControlSource.isRunning
      .onEach { running ->
        isRanging = running
        if (!running) {
          endpoints.clear()
          endpointPositions.clear()
        }
        _uiState.update { updateUiState() }
      }
      .launchIn(CoroutineScope(Dispatchers.IO))
  }

  private data class HomeUiStateImpl(
      override val connectedEndpoints: List<ConnectedEndpoint>,
      override val disconnectedEndpoints: List<UwbEndpoint>,
      override val isRanging: Boolean,
  ) : HomeUiState

  companion object {
    fun provideFactory(
      uwbRangingControlSource: UwbRangingControlSource
    ): ViewModelProvider.Factory =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
          return HomeViewModel(uwbRangingControlSource) as T
        }
      }
  }
}

interface HomeUiState {
  val connectedEndpoints: List<ConnectedEndpoint>
  val disconnectedEndpoints: List<UwbEndpoint>
  val isRanging: Boolean
}

data class ConnectedEndpoint(val endpoint: UwbEndpoint, val position: RangingPosition)
