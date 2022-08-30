package com.google.location.nearby.apps.hellouwb

import android.app.Application
import com.google.location.nearby.apps.hellouwb.data.AppContainerImpl
import com.google.location.nearby.apps.hellouwb.data.SettingsStore
import com.google.location.nearby.apps.hellouwb.data.UwbRangingControlSource
import kotlinx.coroutines.flow.launchIn

class HelloUwbApplication : Application() {

  lateinit var container: AppContainer

  override fun onCreate() {
    super.onCreate()
    container = AppContainerImpl(applicationContext)
  }
}

interface AppContainer {
  val rangingResultSource: UwbRangingControlSource
  val settingsStore: SettingsStore
  fun destroy()
}
