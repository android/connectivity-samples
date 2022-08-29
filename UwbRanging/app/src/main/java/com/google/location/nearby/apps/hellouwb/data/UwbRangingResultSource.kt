package com.google.location.nearby.apps.hellouwb.data

import com.google.location.nearby.apps.uwbranging.EndpointEvents
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface UwbRangingResultSource {

  fun observeRangingResults(): Flow<EndpointEvents>

  var deviceType: DeviceType

  fun start()

  fun stop()

  /** Call this when app is destroyed. */
  fun cancel()

  val isRunning: StateFlow<Boolean>
}

enum class DeviceType {
  CONTROLLER,
  CONTROLEE
}
