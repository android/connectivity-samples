package com.google.location.nearby.apps.hellouwb

import android.app.Application
import com.google.location.nearby.apps.hellouwb.data.AppContainerImpl

class HelloUwbApplication : Application() {

  lateinit var container: AppContainer

  fun initContainer(afterLoading: () -> Unit) {
    container = AppContainerImpl(applicationContext, afterLoading)
  }
}
