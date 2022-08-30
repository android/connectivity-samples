package com.google.location.nearby.apps.hellouwb.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.location.nearby.apps.hellouwb.data.AppSettings
import com.google.location.nearby.apps.hellouwb.data.DeviceType
import com.google.location.nearby.apps.hellouwb.data.SettingsStore
import com.google.location.nearby.apps.hellouwb.data.UwbRangingControlSource
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val uwbRangingControlSource: UwbRangingControlSource,
    private val settingsStore: SettingsStore
) : ViewModel() {

   val uiState = settingsStore.appSettings

  fun updateDeviceDisplayName(deviceName: String) {
      if(deviceName==uiState.value.deviceDisplayName){
          return
      }
    viewModelScope.launch {
      settingsStore.updateDeviceDisplayName(deviceName)
    }
  }

  fun updateDeviceType(deviceType: DeviceType) {
      if(deviceType==uiState.value.deviceType){
          return
      }
    viewModelScope.launch {
      uwbRangingControlSource.deviceType = deviceType
      settingsStore.updateDeviceType(deviceType)
    }
  }

  companion object {
    fun provideFactory(
        uwbRangingControlSource: UwbRangingControlSource,
        settingsStore: SettingsStore
    ): ViewModelProvider.Factory =
        object : ViewModelProvider.Factory {
          @Suppress("UNCHECKED_CAST")
          override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(uwbRangingControlSource, settingsStore) as T
          }
        }
  }
}

