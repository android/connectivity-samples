package com.google.location.nearby.apps.hellouwb

import com.google.location.nearby.apps.hellouwb.data.SettingsStore
import com.google.location.nearby.apps.hellouwb.data.UwbRangingControlSource

interface AppContainer {
  val rangingResultSource: UwbRangingControlSource
  val settingsStore: SettingsStore
}
