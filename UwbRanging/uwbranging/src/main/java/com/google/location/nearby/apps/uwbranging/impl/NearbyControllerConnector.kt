package com.google.location.nearby.apps.uwbranging.impl

import androidx.core.uwb.UwbAddress
import androidx.core.uwb.UwbControllerSessionScope
import com.google.common.primitives.Shorts
import com.google.location.nearby.apps.uwbranging.UwbEndpoint
import com.google.location.nearby.apps.uwbranging.impl.proto.Control
import com.google.location.nearby.apps.uwbranging.impl.proto.Oob
import com.google.location.nearby.apps.uwbranging.impl.proto.UwbConfiguration
import com.google.location.nearby.apps.uwbranging.impl.proto.UwbConnectionInfo
import com.google.protobuf.ByteString
import kotlin.random.Random
import kotlinx.coroutines.flow.Flow

/**
 * UWB OOB connector for a controller device using Nearby Connections.
 *
 * @param localEndpoint Local [UwbEndpoint].
 * @param configId UWB Config ID
 * @param connections OOB connections.
 * @param sessionScopeCreator a function that returns a [UwbControllerSessionScope].
 */
internal class NearbyControllerConnector(
  private val localEndpoint: UwbEndpoint,
  private val configId: Int,
  connections: NearbyConnections,
  private val sessionScopeCreator: suspend () -> UwbControllerSessionScope,
) : NearbyConnector(connections) {

  override fun prepareEventFlow(): Flow<NearbyEvent> {
    return connections.startDiscovery()
  }

  override suspend fun processEndpointConnected(endpointId: String) {}

  override suspend fun processUwbSessionInfo(
    endpointId: String,
    sessionInfo: Control,
  ): UwbOobEvent.UwbEndpointFound? {

    val capabilities =
      if (sessionInfo.connectionInfo.hasCapabilities()) sessionInfo.connectionInfo.capabilities
      else return null
    if (!capabilities.supportedConfigIdsList.contains(configId)) {
      return null
    }
    val endpoint = UwbEndpoint(sessionInfo.id, sessionInfo.metadata.toByteArray())
    addEndpoint(endpointId, endpoint)
    val sessionScope = sessionScopeCreator()
    val sessionId = Random.nextInt()
    val sessionKeyInfo = Random.nextBytes(8)
    val endpointAddress = UwbAddress(Shorts.toByteArray(sessionInfo.localAddress.toShort()))
    val localAddress = sessionScope.localAddress
    val complexChannel = sessionScope.uwbComplexChannel
    val endpointFoundEvent =
      UwbOobEvent.UwbEndpointFound(
        endpoint,
        configId,
        endpointAddress,
        complexChannel,
        sessionId,
        sessionKeyInfo,
        sessionScope
      )

    connections.sendPayload(
      endpointId,
      Oob.newBuilder()
        .setControl(
          Control.newBuilder()
            .setId(localEndpoint.id)
            .setMetadata(ByteString.copyFrom(localEndpoint.metadata))
            .setLocalAddress(Shorts.fromByteArray(localAddress.address).toInt())
            .setConnectionInfo(
              UwbConnectionInfo.newBuilder()
                .setConfiguration(
                  UwbConfiguration.newBuilder()
                    .setConfigId(configId)
                    .setSessionId(sessionId)
                    .setChannel(complexChannel.channel)
                    .setPreambleIndex(complexChannel.preambleIndex)
                    .setSecurityInfo(ByteString.copyFrom(sessionKeyInfo))
                    .build()
                )
                .build()
            )
            .build()
        )
        .build()
        .toByteArray()
    )
    return endpointFoundEvent
  }
}
