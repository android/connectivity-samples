package com.google.location.nearby.apps.uwbranging.impl

import com.google.location.nearby.apps.uwbranging.UwbEndpoint
import com.google.location.nearby.apps.uwbranging.impl.proto.Control
import com.google.location.nearby.apps.uwbranging.impl.proto.Data
import com.google.location.nearby.apps.uwbranging.impl.proto.Oob
import com.google.protobuf.ByteString
import com.google.protobuf.InvalidProtocolBufferException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch

internal abstract class NearbyConnector(protected val connections: NearbyConnections) {

  private val peerMap = mutableMapOf<String, UwbEndpoint>()

  protected fun addEndpoint(endpointId: String, endpoint: UwbEndpoint) {
    peerMap[endpointId] = endpoint
  }

  private fun lookupEndpoint(endpointId: String): UwbEndpoint? {
    return peerMap[endpointId]
  }

  private fun lookupEndpointId(endpoint: UwbEndpoint): String? {
    return peerMap.firstNotNullOfOrNull { if (it.value == endpoint) it.key else null }
  }

  private fun tryParseOobMessage(payload: ByteArray): Data? {
    return try {
      val oob = Oob.parseFrom(payload)
      if (oob.hasData()) oob.data else null
    } catch (_: InvalidProtocolBufferException) {
      null
    }
  }

  private fun tryParseUwbSessionInfo(payload: ByteArray): Control? {
    return try {
      val oob = Oob.parseFrom(payload)
      if (oob.hasControl()) oob.control else null
    } catch (_: InvalidProtocolBufferException) {
      null
    }
  }

  protected abstract fun prepareEventFlow(): Flow<NearbyEvent>

  protected abstract suspend fun processEndpointConnected(endpointId: String)

  protected abstract suspend fun processUwbSessionInfo(
    endpointId: String,
    sessionInfo: Control
  ): UwbOobEvent.UwbEndpointFound?

  private fun processEndpointLost(event: NearbyEvent.EndpointLost): UwbOobEvent? {
    val endpoint = peerMap.remove(event.endpointId) ?: return null
    return UwbOobEvent.UwbEndpointLost(endpoint)
  }

  fun start() = channelFlow {
    val events = prepareEventFlow()
    val job = launch {
      events.collect { event ->
        when (event) {
          is NearbyEvent.EndpointConnected -> {
            processEndpointConnected(event.endpointId)
            null
          }
          is NearbyEvent.PayloadReceived -> processPayload(event)
          is NearbyEvent.EndpointLost -> processEndpointLost(event)
          else -> null
        }?.let { trySend(it) }
      }
    }
    awaitClose { job.cancel() }
  }

  private suspend fun processPayload(event: NearbyEvent.PayloadReceived): UwbOobEvent? {

    tryParseUwbSessionInfo(event.payload)?.let {
      return processUwbSessionInfo(event.endpointId, it)
    }

    val endpoint = lookupEndpoint(event.endpointId) ?: return null

    tryParseOobMessage(event.payload)?.let {
      return UwbOobEvent.MessageReceived(endpoint, it.message.toByteArray())
    }
    return null
  }

  fun sendMessage(endpoint: UwbEndpoint, message: ByteArray) {
    val endpointId = lookupEndpointId(endpoint) ?: return
    connections.sendPayload(
      endpointId,
      Oob.newBuilder()
        .setData(Data.newBuilder().setMessage(ByteString.copyFrom(message)).build())
        .build()
        .toByteArray()
    )
  }
}
