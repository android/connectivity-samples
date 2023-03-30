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

package com.google.apps.hellouwb.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.apps.hellouwb.data.ConfigType
import com.google.apps.hellouwb.data.DeviceType
import com.google.apps.hellouwb.data.SettingsStore
import com.google.apps.hellouwb.data.UwbRangingControlSource

class SettingsViewModel(
  private val uwbRangingControlSource: UwbRangingControlSource,
  private val settingsStore: SettingsStore,
) : ViewModel() {

  val uiState = settingsStore.appSettings

  fun updateDeviceDisplayName(deviceName: String) {
    if (deviceName == uiState.value.deviceDisplayName) {
      return
    }
    settingsStore.updateDeviceDisplayName(deviceName)
  }

  fun updateDeviceType(deviceType: DeviceType) {
    if (deviceType == uiState.value.deviceType) {
      return
    }
    uwbRangingControlSource.deviceType = deviceType
    settingsStore.updateDeviceType(deviceType)
  }

  fun updateConfigType(configType: ConfigType) {
    if (configType == uiState.value.configType) {
      return
    }
    uwbRangingControlSource.configType = configType
    settingsStore.updateConfigType(configType)
  }

  companion object {
    fun provideFactory(
      uwbRangingControlSource: UwbRangingControlSource,
      settingsStore: SettingsStore,
    ): ViewModelProvider.Factory =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
          return SettingsViewModel(uwbRangingControlSource, settingsStore) as T
        }
      }
  }
}
