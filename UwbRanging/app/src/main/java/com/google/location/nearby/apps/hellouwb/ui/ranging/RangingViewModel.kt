package com.google.location.nearby.apps.hellouwb.ui.ranging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.location.nearby.apps.hellouwb.data.UwbRangingControlSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

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
