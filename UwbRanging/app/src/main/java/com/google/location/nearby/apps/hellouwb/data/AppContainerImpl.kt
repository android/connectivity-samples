package com.google.location.nearby.apps.hellouwb.data

import android.content.Context
import com.google.location.nearby.apps.hellouwb.AppContainer
import kotlinx.coroutines.*

internal class AppContainerImpl(context: Context) : AppContainer {

  private val coroutineScope = CoroutineScope(Dispatchers.IO + Job())

  override val rangingResultSource: UwbRangingControlSource

  override val settingsStore: SettingsStore

  override fun destroy() {
    coroutineScope.cancel()
  }

  init {
    settingsStore = SettingsStoreImpl(context, coroutineScope)
    val appSettings = settingsStore.appSettings.value
    rangingResultSource =
        UwbRangingControlSourceImpl(
            context, appSettings.deviceDisplayName + "|" + appSettings.deviceUuid, coroutineScope)
    coroutineScope.launch {
      settingsStore.appSettings.collect {
        rangingResultSource.deviceType = it.deviceType
        rangingResultSource.updateEndpointId(it.deviceDisplayName + "|" + it.deviceUuid)
      }
    }
  }
}
