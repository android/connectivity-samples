package com.google.location.nearby.apps.uwbranging.impl

import androidx.core.uwb.RangingParameters
import androidx.core.uwb.RangingResult
import androidx.core.uwb.UwbAddress
import androidx.core.uwb.UwbDevice
import com.google.location.nearby.apps.uwbranging.UwbEndpoint
import com.google.location.nearby.apps.uwbranging.UwbRangingResult
import com.google.location.nearby.apps.uwbranging.UwbSessionScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch

internal class UwbSessionScopeImpl(
    private val localEndpoint: UwbEndpoint,
    private val startOob: () -> Flow<UwbOobEvent>
) : UwbSessionScope {

  private val localAddresses = mutableSetOf<UwbAddress>()

  private val remoteDeviceMap = mutableMapOf<UwbAddress, UwbEndpoint>()

  private val activeJobs = mutableMapOf<UwbEndpoint, Job>()

  override fun prepareSession(configId: Int) = channelFlow {
    val job = launch {
      startOob().collect { event ->
        when (event) {
          is UwbOobEvent.UwbEndpointFound -> processEndpointFound(event)
          is UwbOobEvent.UwbEndpointLost -> processEndpointLost(event.endpoint)
        }
      }
    }
    awaitClose { job.cancel() }
  }

  private fun processEndpointLost(endpoint: UwbEndpoint) {
    activeJobs[endpoint]?.cancel()
  }

  private suspend fun ProducerScope<UwbRangingResult>.processEndpointFound(
      event: UwbOobEvent.UwbEndpointFound
  ) {
    remoteDeviceMap[event.endpointAddress] = event.endpoint
    localAddresses.add(event.sessionScope.localAddress)
    val rangingParameters =
        RangingParameters(
            event.configId,
            event.sessionId,
            event.sessionKeyInfo,
            event.complexChannel,
            listOf(UwbDevice(event.endpointAddress)),
            RangingParameters.RANGING_UPDATE_RATE_FREQUENT)

    val rangingEvents = event.sessionScope.prepareSession(rangingParameters)
    activeJobs[event.endpoint] = coroutineScope {
      launch { rangingEvents.collect { sendResult(it) } }
    }
  }

  private fun ProducerScope<UwbRangingResult>.sendResult(result: RangingResult) {
    val endpoint =
        if (localAddresses.contains(result.device.address)) localEndpoint
        else remoteDeviceMap[result.device.address] ?: return
    when (result) {
      is RangingResult.RangingResultPosition ->
          trySend(UwbRangingResult.RangingResultPosition(endpoint, result.position))
      is RangingResult.RangingResultPeerDisconnected ->
          trySend(UwbRangingResult.RangingResultPeerDisconnected(endpoint))
    }
  }
}
