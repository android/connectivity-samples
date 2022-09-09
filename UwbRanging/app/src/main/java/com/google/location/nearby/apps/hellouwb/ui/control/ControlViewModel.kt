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
