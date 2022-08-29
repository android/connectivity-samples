package com.google.location.nearby.apps.hellouwb.ui.control

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.location.nearby.apps.hellouwb.data.UwbRangingResultSource

class ControlViewModel(uwbRangingResultSource: UwbRangingResultSource) : ViewModel() {

  companion object {
    fun provideFactory(uwbRangingResultSource: UwbRangingResultSource): ViewModelProvider.Factory =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
          return ControlViewModel(uwbRangingResultSource) as T
        }
      }
  }
}
