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

  override fun controllerUwbScope(endpoint: UwbEndpoint, configId: Int): UwbSessionScope {
    val connector =
      NearbyControllerConnector(endpoint, configId, NearbyConnections(context, dispatcher)) {
        uwbManager.controllerSessionScope()
      }
    return UwbSessionScopeImpl(endpoint, connector)
  }

  override fun controleeUwbScope(endpoint: UwbEndpoint): UwbSessionScope {
    val connector =
      NearbyControleeConnector(endpoint, NearbyConnections(context, dispatcher)) {
        uwbManager.controleeSessionScope()
      }
    return UwbSessionScopeImpl(endpoint, connector)
  }
}
