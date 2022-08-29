package com.google.location.nearby.apps.hellouwb.ui.home

import androidx.core.uwb.RangingPosition
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.location.nearby.apps.hellouwb.data.UwbRangingResultSource
import com.google.location.nearby.apps.uwbranging.EndpointEvents
import com.google.location.nearby.apps.uwbranging.UwbEndpoint
import kotlinx.coroutines.flow.*

class HomeViewModel(uwbRangingResultSource: UwbRangingResultSource) : ViewModel() {

  private val _uiState: MutableStateFlow<HomeUiState> =
    MutableStateFlow(HomeUiStateImpl(listOf(), listOf(), false))

  private val endpoints = mutableListOf<UwbEndpoint>()
  private val endpointPositions = mutableMapOf<UwbEndpoint, RangingPosition>()
  private var isRanging = false

  private fun updateUiState(): HomeUiState {
    return HomeUiStateImpl(
      endpoints
        .map { endpoint ->
          endpointPositions[endpoint]?.let { position -> ConnectedEndpoint(endpoint, position) }
        }
        .filterNotNull()
        .toList(),
      endpoints.filter { !endpointPositions.containsKey(it) }.toList(),
      isRanging
    )
  }

  val uiState = _uiState.asStateFlow()

  init {
    uwbRangingResultSource
      .observeRangingResults()
      .onEach { result ->
        when (result) {
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
    uwbRangingResultSource.isRunning
      .onEach { running ->
        isRanging = running
        _uiState.update { _uiState.value }
      }
      .launchIn(viewModelScope)
  }

  private data class HomeUiStateImpl(
    override val connectedEndpoints: List<ConnectedEndpoint>,
    override val disconnectedEndpoints: List<UwbEndpoint>,
    override val isRanging: Boolean
  ) : HomeUiState

  companion object {
    fun provideFactory(uwbRangingResultSource: UwbRangingResultSource): ViewModelProvider.Factory =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
          return HomeViewModel(uwbRangingResultSource) as T
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
