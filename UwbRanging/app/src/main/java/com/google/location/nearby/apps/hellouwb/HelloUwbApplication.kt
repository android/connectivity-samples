package com.google.location.nearby.apps.hellouwb

import android.app.Application
import com.google.location.nearby.apps.hellouwb.data.AppContainerImpl
import com.google.location.nearby.apps.hellouwb.data.UwbRangingResultSource

class HelloUwbApplication : Application() {

  lateinit var container: AppContainer

  override fun onCreate() {
    super.onCreate()
    container = AppContainerImpl(applicationContext)
  }
}

interface AppContainer {
  val rangingResultSource: UwbRangingResultSource
}
