package com.google.location.nearby.apps.hellouwb.data

import com.google.location.nearby.apps.uwbranging.EndpointEvents
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface UwbRangingControlSource {

  fun observeRangingResults(): Flow<EndpointEvents>

  var deviceType: DeviceType

  fun updateEndpointId(id: String)

  fun start()

  fun stop()

  val isRunning: StateFlow<Boolean>
}
