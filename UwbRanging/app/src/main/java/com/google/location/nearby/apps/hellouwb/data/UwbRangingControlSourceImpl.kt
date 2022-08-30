package com.google.location.nearby.apps.hellouwb.data

import android.content.Context
import androidx.core.uwb.RangingParameters
import com.google.location.nearby.apps.uwbranging.EndpointEvents
import com.google.location.nearby.apps.uwbranging.UwbConnectionManager
import com.google.location.nearby.apps.uwbranging.UwbEndpoint
import com.google.location.nearby.apps.uwbranging.UwbSessionScope
import java.security.SecureRandom
import kotlin.properties.Delegates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class UwbRangingControlSourceImpl(
  context: Context,
  endpointId: String,
  private val coroutineScope: CoroutineScope,
  private val uwbConnectionManager: UwbConnectionManager =
    UwbConnectionManager.getInstance(context),
) : UwbRangingControlSource {

  private var uwbEndpoint = UwbEndpoint(endpointId, SecureRandom.getSeed(8))

  private var uwbSessionScope: UwbSessionScope = getSessionScope(DeviceType.CONTROLLER)

  private var rangingJob: Job? = null

  private fun getSessionScope(deviceType: DeviceType): UwbSessionScope {
    return when (deviceType) {
      DeviceType.CONTROLEE -> uwbConnectionManager.controleeUwbScope(uwbEndpoint)
      DeviceType.CONTROLLER ->
        uwbConnectionManager.controllerUwbScope(uwbEndpoint, RangingParameters.UWB_CONFIG_ID_1)
      else -> throw IllegalStateException()
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

  override fun updateEndpointId(id: String) {
    if (id != uwbEndpoint.id) {
      stop()
      uwbEndpoint = UwbEndpoint(id, SecureRandom.getSeed(8))
      uwbSessionScope = getSessionScope(deviceType)
    }
  }

  override fun start() {
    if (rangingJob == null) {
      rangingJob =
        coroutineScope.launch { uwbSessionScope.prepareSession().collect { dispatchResult(it) } }
      runningStateFlow.update { true }
    }
  }

  override fun stop() {
    val job = rangingJob ?: return
    job.cancel()
    rangingJob = null
    runningStateFlow.update { false }
  }

  private val runningStateFlow = MutableStateFlow(false)

  override val isRunning = runningStateFlow.asStateFlow()
}
