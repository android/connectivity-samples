package com.google.location.nearby.apps.uwbranging.impl

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


private const val CONNECTION_SERVICE_ID = "com.google.location.nearby.apps.hellouwb"
private const val CONNECTION_NAME = "hellouwb"

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
    private val connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(
        context
    )
) {

    // Book keeping of active endpoints.
    private val endpoints = mutableSetOf<String>()

    // Connection-phase Callbacks used by both controller and controlee
    private val connectionLifecycleCallback =
        object : ConnectionLifecycleCallback() {
            override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
                CoroutineScope(dispatcher).launch {
                    connectionsClient.acceptConnection(endpointId, payloadCallback).await()
                }
            }

            override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
                if (result.status.statusCode == ConnectionsStatusCodes.STATUS_OK) {
                    endpoints.add(endpointId)
                    dispatchEvent(NearbyEvent.EndpointConnected(endpointId))
                }
            }

            override fun onDisconnected(endpointId: String) {
                if (endpoints.remove(endpointId)) {
                    dispatchEvent(NearbyEvent.EndpointLost(endpointId))
                }
            }
        }

    private val payloadCallback =
        object : PayloadCallback() {
            override fun onPayloadReceived(endpointId: String, payload: Payload) {
                val bytes = payload.asBytes() ?: return
                dispatchEvent(NearbyEvent.PayloadReceived(endpointId, bytes))
            }

            override fun onPayloadTransferUpdate(
                endpointId: String,
                update: PayloadTransferUpdate
            ) {
            }
        }

    private val endpointDiscoveryCallback =
        object : EndpointDiscoveryCallback() {
            override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                CoroutineScope(dispatcher).launch {
                    connectionsClient
                        .requestConnection(CONNECTION_NAME, endpointId, connectionLifecycleCallback)
                        .await()
                }
            }

            override fun onEndpointLost(endpointId: String) {
                if (endpoints.remove(endpointId)) {
                    dispatchEvent(NearbyEvent.EndpointLost(endpointId))
                }
            }
        }

    fun sendPayload(endpointId: String, bytes: ByteArray) {
        CoroutineScope(dispatcher).launch {
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

        connectionsClient
            .startDiscovery(
                CONNECTION_SERVICE_ID,
                endpointDiscoveryCallback,
                DiscoveryOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
            )
            .await()


        awaitClose {
            connectionsClient.stopDiscovery()
        }
    }

    /**
     * Starts advertising.
     * @return a flow of [NearbyEvent].
     */
    fun startAdvertising() = callbackFlow {
        dispatchEvent = { trySend(it) }
        connectionsClient
            .startAdvertising(
                CONNECTION_NAME,
                CONNECTION_SERVICE_ID,
                connectionLifecycleCallback,
                AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
            )
            .await()

        awaitClose {
            connectionsClient.stopAdvertising()
        }
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
