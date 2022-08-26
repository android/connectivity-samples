package com.google.location.nearby.apps.uwbranging.impl

import androidx.core.uwb.UwbAddress
import androidx.core.uwb.UwbControllerSessionScope
import com.google.common.primitives.Shorts
import com.google.location.nearby.apps.uwbranging.UwbEndpoint
import com.google.location.nearby.apps.uwbranging.impl.proto.UwbConfiguration
import com.google.location.nearby.apps.uwbranging.impl.proto.UwbConnectionInfo
import com.google.location.nearby.apps.uwbranging.impl.proto.UwbSessionInfo
import com.google.protobuf.ByteString
import com.google.protobuf.InvalidProtocolBufferException
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

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
    private val connections: NearbyConnections,
    private val sessionScopeCreator: suspend () -> UwbControllerSessionScope
) {

    private val controleeMap = mutableMapOf<String, UwbEndpoint>()

    fun start() = channelFlow {
        val events = connections.startDiscovery()
        val job = launch {
            events.collect { event ->
                when (event) {
                    is NearbyEvent.PayloadReceived -> processPayload(event)
                    is NearbyEvent.EndpointLost -> processEndpointLost(event)
                }
            }
        }

        awaitClose { job.cancel() }
    }

    private fun ProducerScope<UwbOobEvent>.processEndpointLost(event: NearbyEvent) {
        (controleeMap.remove(event.endpointId))?.let { trySend(UwbOobEvent.UwbEndpointLost(it)) }
    }

    private suspend fun ProducerScope<UwbOobEvent>.processPayload(
        event: NearbyEvent.PayloadReceived
    ) {
        val endpointInfo =
            try {
                UwbSessionInfo.parseFrom(event.payload)
            } catch (_: InvalidProtocolBufferException) {
                return
            }
        val capabilities =
            if (endpointInfo.connectionInfo.hasCapabilities()) endpointInfo.connectionInfo.capabilities else return
        if (!capabilities.supportedConfigIdsList.contains(configId)) {
            return
        }
        val endpoint = UwbEndpoint(endpointInfo.id, endpointInfo.metadata.toByteArray())
        controleeMap[event.endpointId] = endpoint
        val sessionScope = sessionScopeCreator()
        val sessionId = Random.nextInt()
        val sessionKeyInfo = Random.nextBytes(8)
        val endpointAddress = UwbAddress(Shorts.toByteArray(endpointInfo.localAddress.toShort()))
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
        val sessionInfo =
            UwbSessionInfo.newBuilder()
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

        connections.sendPayload(event.endpointId, sessionInfo.toByteArray())
        trySend(endpointFoundEvent)
    }
}
