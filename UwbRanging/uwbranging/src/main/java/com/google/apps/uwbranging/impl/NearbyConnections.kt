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

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

private const val CONNECTION_SERVICE_ID = "com.google.apps.hellouwb"
private const val CONNECTION_NAME = "com.google.apps.hellouwb"

/**
 * Nearby Connections API wrapper.
 *
 * @param name Human readable name used in Nearby Connections.
 * @param dispatcher Dispatcher that runs Nearby Connections API calls.
 * @param connectionsClient a Nearby Connections client. Exposed for unit testing.
 */
internal class NearbyConnections(
  context: Context,
  private val dispatcher: CoroutineDispatcher,
  private val connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(context),
) {

  private val coroutineScope =
    CoroutineScope(
      dispatcher +
        Job() +
        CoroutineExceptionHandler { _, e -> Log.e("NearbyConnections", "Connection Error", e) }
    )

  // Connection-phase Callbacks used by both controller and controlee
  private val connectionLifecycleCallback =
    object : ConnectionLifecycleCallback() {
      override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
        coroutineScope.launch {
          connectionsClient.acceptConnection(endpointId, payloadCallback).await()
        }
      }

      override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
        if (result.status.statusCode == ConnectionsStatusCodes.STATUS_OK) {
          dispatchEvent(NearbyEvent.EndpointConnected(endpointId))
        }
      }

      override fun onDisconnected(endpointId: String) {
          dispatchEvent(NearbyEvent.EndpointLost(endpointId))
      }
    }

  private val payloadCallback =
    object : PayloadCallback() {
      override fun onPayloadReceived(endpointId: String, payload: Payload) {
        val bytes = payload.asBytes() ?: return
        dispatchEvent(NearbyEvent.PayloadReceived(endpointId, bytes))
      }

      override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }

  private val endpointDiscoveryCallback =
    object : EndpointDiscoveryCallback() {
      override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
        coroutineScope.launch {
          connectionsClient
            .requestConnection(CONNECTION_NAME, endpointId, connectionLifecycleCallback)
        }
      }

      override fun onEndpointLost(endpointId: String) {
        dispatchEvent(NearbyEvent.EndpointLost(endpointId))

      }
    }

  fun sendPayload(endpointId: String, bytes: ByteArray) {
    coroutineScope.launch {
      connectionsClient.sendPayload(endpointId, Payload.fromBytes(bytes)).await()
    }
  }

  private var dispatchEvent: (event: NearbyEvent) -> Unit = {}

  /**
   * Starts discovery.
   * @return a flow of [NearbyEvent].
   */
  fun startDiscovery() = callbackFlow {
    dispatchEvent = { trySend(it) }
    coroutineScope.launch {
      connectionsClient
        .startDiscovery(
          CONNECTION_SERVICE_ID,
          endpointDiscoveryCallback,
          DiscoveryOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
        )
        .await()
    }
    awaitClose {
      disconnectAll()
      connectionsClient.stopDiscovery()
    }
  }

  /**
   * Starts advertising.
   * @return a flow of [NearbyEvent].
   */
  fun startAdvertising() = callbackFlow {
    dispatchEvent = { trySend(it) }
    coroutineScope.launch {
      connectionsClient
        .startAdvertising(
          CONNECTION_NAME,
          CONNECTION_SERVICE_ID,
          connectionLifecycleCallback,
          AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
        )
        .await()
    }
    awaitClose {
      disconnectAll()
      connectionsClient.stopAdvertising()
    }
  }

  private fun disconnectAll() {
    connectionsClient.stopAllEndpoints()
  }
}

/** Events that happen in a Nearby Connections session. */
abstract class NearbyEvent private constructor() {

  abstract val endpointId: String

  /** An event that notifies a NC endpoint is connected. */
  data class EndpointConnected(override val endpointId: String) : NearbyEvent()

  /** An event that notifies a NC endpoint is lost. */
  data class EndpointLost(override val endpointId: String) : NearbyEvent()

  /** An event that notifies a UWB device is lost. */
  data class PayloadReceived(override val endpointId: String, val payload: ByteArray) :
    NearbyEvent()
}
