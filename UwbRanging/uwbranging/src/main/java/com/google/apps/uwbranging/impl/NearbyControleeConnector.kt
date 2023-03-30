/*
 *
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.google.apps.uwbranging.impl

import androidx.core.uwb.RangingParameters
import androidx.core.uwb.UwbAddress
import androidx.core.uwb.UwbClientSessionScope
import androidx.core.uwb.UwbComplexChannel
import androidx.core.uwb.UwbControleeSessionScope
import com.google.common.primitives.Shorts
import com.google.apps.uwbranging.UwbEndpoint
import com.google.apps.uwbranging.impl.proto.Control
import com.google.apps.uwbranging.impl.proto.Oob
import com.google.apps.uwbranging.impl.proto.UwbCapabilities
import com.google.apps.uwbranging.impl.proto.UwbConnectionInfo
import com.google.protobuf.ByteString
import kotlinx.coroutines.flow.Flow

/**
 * UWB OOB connector for a controlee device using Nearby Connections.
 *
 * @param localEndpoint Local endpoint.
 * @param connections: OOB connection.
 * @param sessionScopeCreator a function that returns a [UwbControleeSessionScope].
 */
internal class NearbyControleeConnector(
    private val localEndpoint: UwbEndpoint,
    connections: NearbyConnections,
    private val sessionScopeCreator: suspend () -> UwbControleeSessionScope,
) : NearbyConnector(connections) {

  private val sessionMap = mutableMapOf<String, UwbClientSessionScope>()

  override fun prepareEventFlow(): Flow<NearbyEvent> {
    return connections.startAdvertising()
  }

  override suspend fun processEndpointConnected(endpointId: String) {
    val sessionScope = sessionScopeCreator()
    sessionMap[endpointId] = sessionScope
    connections.sendPayload(
      endpointId,
      Oob.newBuilder().setControl(uwbSessionInfo(sessionScope)).build().toByteArray()
    )
  }

  override suspend fun processUwbSessionInfo(
    endpointId: String,
    sessionInfo: Control,
  ): UwbOobEvent.UwbEndpointFound? {
    val configuration =
      if (sessionInfo.connectionInfo.hasConfiguration()) sessionInfo.connectionInfo.configuration
      else return null
    val sessionScope = sessionMap[endpointId] ?: return null
    val endpoint = UwbEndpoint(sessionInfo.id, sessionInfo.metadata.toByteArray())
    addEndpoint(endpointId, endpoint)

    return UwbOobEvent.UwbEndpointFound(
        endpoint,
        configuration.configId,
        UwbAddress(Shorts.toByteArray(sessionInfo.localAddress.toShort())),
        UwbComplexChannel(configuration.channel, configuration.preambleIndex),
        configuration.sessionId,
        configuration.securityInfo.toByteArray(),
        sessionScope
    )
  }

  private fun uwbSessionInfo(scope: UwbClientSessionScope) =
    Control.newBuilder()
      .setId(localEndpoint.id)
      .setMetadata(ByteString.copyFrom(localEndpoint.metadata))
      .setLocalAddress(Shorts.fromByteArray(scope.localAddress.address).toInt())
      .setConnectionInfo(
        UwbConnectionInfo.newBuilder()
          .setCapabilities(
            UwbCapabilities.newBuilder()
              .addAllSupportedConfigIds(listOf(RangingParameters.CONFIG_UNICAST_DS_TWR,
                                               RangingParameters.CONFIG_MULTICAST_DS_TWR))
              .setSupportsAzimuth(scope.rangingCapabilities.isAzimuthalAngleSupported)
              .setSupportsElevation(scope.rangingCapabilities.isElevationAngleSupported)
              .build()
          )
          .build()
      )
      .build()
}
