package com.google.location.nearby.apps.hellouwb.data

import android.content.Context
import com.google.location.nearby.apps.hellouwb.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class AppContainerImpl(
  private val context: Context,
  afterLoading: () -> Unit,
) : AppContainer {

  private val coroutineScope = CoroutineScope(Dispatchers.IO + Job())

  override val rangingResultSource: UwbRangingControlSource
    get() =
      _rangingResultSource
        ?: throw IllegalStateException("rangingResultSource only can be accessed after loading.")

  private var _rangingResultSource: UwbRangingControlSource? = null

  override val settingsStore = SettingsStoreImpl(context, coroutineScope)

  init {
    coroutineScope.launch {
      settingsStore.appSettings.collect {
        val endpointId = it.deviceDisplayName + "|" + it.deviceUuid
        if (_rangingResultSource == null) {
          _rangingResultSource = UwbRangingControlSourceImpl(context, endpointId, coroutineScope)
          afterLoading()
        } else {
          rangingResultSource.deviceType = it.deviceType
          rangingResultSource.updateEndpointId(endpointId)
        }
      }
    }
  }
}
