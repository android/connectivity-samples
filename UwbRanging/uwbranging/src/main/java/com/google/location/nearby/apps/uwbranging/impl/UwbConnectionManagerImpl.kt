package com.google.location.nearby.apps.uwbranging.impl

import android.content.Context
import androidx.core.uwb.UwbManager
import com.google.location.nearby.apps.uwbranging.UwbConnectionManager
import com.google.location.nearby.apps.uwbranging.UwbEndpoint
import com.google.location.nearby.apps.uwbranging.UwbSessionScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
internal class UwbConnectionManagerImpl(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1)
) : UwbConnectionManager {

  private val uwbManager = UwbManager.createInstance(context)

  override suspend fun controllerUwbScope(endpoint: UwbEndpoint, configId: Int): UwbSessionScope {
    return UwbSessionScopeImpl(endpoint) {
      val connector =
          NearbyControllerConnector(endpoint, configId, NearbyConnections(context, dispatcher)) {
            uwbManager.controllerSessionScope()
          }
      connector.start()
    }
  }

  override suspend fun controleeUwbScope(endpoint: UwbEndpoint): UwbSessionScope {
    return UwbSessionScopeImpl(endpoint) {
      val connector =
          NearbyControleeConnector(endpoint, NearbyConnections(context, dispatcher)) {
            uwbManager.controleeSessionScope()
          }
      connector.start()
    }
  }
}
