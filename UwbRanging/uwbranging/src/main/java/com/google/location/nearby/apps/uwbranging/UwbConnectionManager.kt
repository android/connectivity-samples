package com.google.location.nearby.apps.uwbranging

import android.content.Context
import com.google.location.nearby.apps.uwbranging.impl.UwbConnectionManagerImpl

/**
 * Manages OOB connection and UWB ranging. It starts OOB connection first, if peer is found, it
 * starts the UWB ranging.
 */
interface UwbConnectionManager {
  fun controllerUwbScope(endpoint: UwbEndpoint, configId: Int): UwbSessionScope

  fun controleeUwbScope(endpoint: UwbEndpoint): UwbSessionScope

  companion object {
    fun getInstance(context: Context): UwbConnectionManager {
      return UwbConnectionManagerImpl(context)
    }
  }
}
