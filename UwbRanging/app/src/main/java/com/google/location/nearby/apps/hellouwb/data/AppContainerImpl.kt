package com.google.location.nearby.apps.hellouwb.data

import android.content.Context
import com.google.location.nearby.apps.hellouwb.AppContainer

class AppContainerImpl(context: Context) : AppContainer {

  override val rangingResultSource: UwbRangingResultSource by lazy {
    UwbRangingResultSourceImpl(context)
  }
}
