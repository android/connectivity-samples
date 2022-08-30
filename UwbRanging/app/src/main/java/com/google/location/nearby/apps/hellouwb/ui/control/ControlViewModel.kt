package com.google.location.nearby.apps.hellouwb.ui.control

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.location.nearby.apps.hellouwb.data.UwbRangingControlSource

class ControlViewModel(uwbRangingControlSource: UwbRangingControlSource) : ViewModel() {

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
}
