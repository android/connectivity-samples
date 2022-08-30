package com.google.location.nearby.apps.hellouwb.data

import kotlinx.coroutines.flow.StateFlow

/** Loads and updates [AppSettings]. */
interface SettingsStore {

  val appSettings: StateFlow<AppSettings>

  fun updateDeviceType(deviceType: DeviceType)

  fun updateDeviceDisplayName(displayName: String)
}
