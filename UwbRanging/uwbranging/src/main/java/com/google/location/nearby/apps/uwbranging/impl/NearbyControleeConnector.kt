package com.google.location.nearby.apps.uwbranging.impl

import androidx.core.uwb.*
import com.google.common.primitives.Shorts
import com.google.location.nearby.apps.uwbranging.UwbEndpoint
import com.google.location.nearby.apps.uwbranging.impl.proto.UwbCapabilities
import com.google.location.nearby.apps.uwbranging.impl.proto.UwbConnectionInfo
import com.google.location.nearby.apps.uwbranging.impl.proto.UwbSessionInfo
import com.google.protobuf.ByteString
import com.google.protobuf.InvalidProtocolBufferException
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch

/**
 * UWB OOB connector for a controlee device using Nearby Connections.
 *
 * @param localEndpoint Local endpoint.
 * @param connections: OOB connection.
 * @param sessionScopeCreator a function that returns a [UwbControleeSessionScope].
 */
internal class NearbyControleeConnector(
    private val localEndpoint: UwbEndpoint,
    private val connections: NearbyConnections,
    private val sessionScopeCreator: suspend () -> UwbControleeSessionScope,
) {

  private val controllerMap = mutableMapOf<String, UwbEndpoint>()

  private val sessionMap = mutableMapOf<String, UwbClientSessionScope>()

  private fun uwbSessionInfo(scope: UwbClientSessionScope) =
      UwbSessionInfo.newBuilder()
          .setId(localEndpoint.id)
          .setMetadata(ByteString.copyFrom(localEndpoint.metadata))
          .setLocalAddress(Shorts.fromByteArray(scope.localAddress.address).toInt())
          .setConnectionInfo(
              UwbConnectionInfo.newBuilder()
                  .setCapabilities(
                      UwbCapabilities.newBuilder()
                          .addAllSupportedConfigIds(listOf(RangingParameters.UWB_CONFIG_ID_1))
                          .setSupportsAzimuth(scope.rangingCapabilities.isAzimuthalAngleSupported)
                          .setSupportsElevation(scope.rangingCapabilities.isElevationAngleSupported)
                          .build())
                  .build())
          .build()

  fun start() = channelFlow {
    val events = connections.startAdvertising()
    val job = launch {
      events.collect { event ->
        when (event) {
          is NearbyEvent.EndpointConnected -> processEndpointConnected(event)
          is NearbyEvent.EndpointLost -> processEndpointLost(event)
          is NearbyEvent.PayloadReceived -> processPayload(event)
        }
      }
    }

    awaitClose { job.cancel() }
  }

  private suspend fun processEndpointConnected(event: NearbyEvent) {
    val sessionScope = sessionScopeCreator()
    sessionMap[event.endpointId] = sessionScope
    connections.sendPayload(event.endpointId, uwbSessionInfo(sessionScope).toByteArray())
  }

  private fun ProducerScope<UwbOobEvent>.processEndpointLost(event: NearbyEvent) {
    sessionMap.remove(event.endpointId)
    (controllerMap.remove(event.endpointId))?.let { trySend(UwbOobEvent.UwbEndpointLost(it)) }
  }

  private fun ProducerScope<UwbOobEvent>.processPayload(event: NearbyEvent.PayloadReceived) {
    val endpointInfo =
        try {
          UwbSessionInfo.parseFrom(event.payload)
        } catch (_: InvalidProtocolBufferException) {
          return
        }
    val configuration =
        if (endpointInfo.connectionInfo.hasConfiguration())
            endpointInfo.connectionInfo.configuration
        else return
    val sessionScope = sessionMap[event.endpointId] ?: return
    val endpoint = UwbEndpoint(endpointInfo.id, endpointInfo.metadata.toByteArray())
    controllerMap[event.endpointId] = endpoint
    trySend(
        UwbOobEvent.UwbEndpointFound(
            endpoint,
            configuration.configId,
            UwbAddress(Shorts.toByteArray(endpointInfo.localAddress.toShort())),
            UwbComplexChannel(configuration.channel, configuration.preambleIndex),
            configuration.sessionId,
            configuration.securityInfo.toByteArray(),
            sessionScope))
  }
}
