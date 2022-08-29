package com.google.location.nearby.apps.hellouwb.data

import android.content.Context
import androidx.core.uwb.RangingParameters
import com.google.location.nearby.apps.uwbranging.EndpointEvents
import com.google.location.nearby.apps.uwbranging.UwbConnectionManager
import com.google.location.nearby.apps.uwbranging.UwbEndpoint
import com.google.location.nearby.apps.uwbranging.UwbSessionScope
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.update
import java.security.SecureRandom
import java.util.*
import kotlin.properties.Delegates

class UwbRangingResultSourceImpl(
  context: Context,
  private val uwbConnectionManager: UwbConnectionManager = UwbConnectionManager.getInstance(context)
) : UwbRangingResultSource {

  private val uwbEndpoint = UwbEndpoint(UUID.randomUUID().toString(), SecureRandom.getSeed(8))

  private var uwbSessionScope: UwbSessionScope = getSessionScope(DeviceType.CONTROLLER)

  private var flowLaunchingJob: Job? = null

  private val coroutineScope = CoroutineScope(Dispatchers.IO + Job())

  private fun getSessionScope(deviceType: DeviceType): UwbSessionScope {
    return when (deviceType) {
      DeviceType.CONTROLEE -> uwbConnectionManager.controleeUwbScope(uwbEndpoint)
      DeviceType.CONTROLLER ->
        uwbConnectionManager.controllerUwbScope(uwbEndpoint, RangingParameters.UWB_CONFIG_ID_1)
    }
  }

  private var dispatchResult: (result: EndpointEvents) -> Unit = {}
  override fun observeRangingResults() = channelFlow {
    dispatchResult = { trySend(it) }
    awaitClose { dispatchResult = {} }
  }

  override var deviceType by
    Delegates.observable(DeviceType.CONTROLLER) { _, oldValue, newValue ->
      if (oldValue != newValue) {
        stop()
        uwbSessionScope = getSessionScope(newValue)
      }
    }

  override fun start() {
    if (flowLaunchingJob == null) {
      flowLaunchingJob =
        coroutineScope.launch { uwbSessionScope.prepareSession().collect { dispatchResult(it) } }
      runningStateFlow.update { true }
    }
  }

  override fun stop() {
    val job = flowLaunchingJob ?: return
    job.cancel()
    flowLaunchingJob = null
    runningStateFlow.update { false }
  }

  override fun cancel() {
    coroutineScope.cancel()
  }

  private val runningStateFlow = MutableStateFlow(false)

  override val isRunning = runningStateFlow.asStateFlow()
}
