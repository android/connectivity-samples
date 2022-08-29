package com.google.location.nearby.apps.uwbranging.impl

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.common.api.Status
import com.google.android.gms.nearby.connection.*
import com.google.android.gms.tasks.Tasks
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class NearbyConnectionsTest {

  private lateinit var connection: NearbyConnections

  private val connectionsClient = mock<ConnectionsClient>()

  @Before
  fun setUp() {
    val context: Context = ApplicationProvider.getApplicationContext()
    connection = NearbyConnections(context, Dispatchers.Unconfined, connectionsClient)
  }

  @Test
  fun testDiscovery() = runTest {
    whenever(connectionsClient.startDiscovery(any(), any(), any()))
      .thenReturn(Tasks.forResult(null))
    whenever(connectionsClient.requestConnection(any<String>(), any(), any()))
      .thenReturn(Tasks.forResult(null))
    whenever(connectionsClient.acceptConnection(any(), any())).thenReturn(Tasks.forResult(null))

    val flow = connection.startDiscovery()

    val events = mutableListOf<NearbyEvent>()
    val job = launch { flow.collect { events.add(it) } }

    advanceUntilIdle()
    val discoveryCallbackCaptor = argumentCaptor<EndpointDiscoveryCallback>()
    verify(connectionsClient).startDiscovery(any(), discoveryCallbackCaptor.capture(), any())

    val discoveryCallback = discoveryCallbackCaptor.lastValue

    val endpointId = "EP1"

    discoveryCallback.onEndpointFound(endpointId, DiscoveredEndpointInfo("b1", "test"))
    advanceUntilIdle()

    val connectionCallbackCaptor = argumentCaptor<ConnectionLifecycleCallback>()
    verify(connectionsClient)
      .requestConnection(any<String>(), eq(endpointId), connectionCallbackCaptor.capture())

    val connectionCallback = connectionCallbackCaptor.lastValue

    connectionCallback.onConnectionInitiated(endpointId, ConnectionInfo("test", "token", false))

    connectionCallback.onConnectionResult(endpointId, ConnectionResolution(Status.RESULT_SUCCESS))

    advanceUntilIdle()

    assertThat(events[0]).isInstanceOf(NearbyEvent.EndpointConnected::class.java)
    assertThat(events[0].endpointId).isEqualTo(endpointId)

    val payloadCallbackCaptor = argumentCaptor<PayloadCallback>()
    verify(connectionsClient).acceptConnection(eq(endpointId), payloadCallbackCaptor.capture())
    val payloadCallback = payloadCallbackCaptor.lastValue

    payloadCallback.onPayloadReceived(endpointId, Payload.fromBytes(byteArrayOf(1, 2, 3)))
    advanceUntilIdle()

    assertThat(events[1]).isInstanceOf(NearbyEvent.PayloadReceived::class.java)
    val event = events[1] as NearbyEvent.PayloadReceived
    assertThat(event.endpointId).isEqualTo(endpointId)
    assertThat(event.payload).isEqualTo(byteArrayOf(1, 2, 3))

    discoveryCallback.onEndpointLost(endpointId)
    advanceUntilIdle()

    assertThat(events[2]).isInstanceOf(NearbyEvent.EndpointLost::class.java)
    assertThat(events[2].endpointId).isEqualTo(endpointId)

    job.cancel()
    advanceUntilIdle()

    verify(connectionsClient).stopDiscovery()
  }

  @Test
  fun testAdvertising() = runTest {
    whenever(connectionsClient.startAdvertising(any<String>(), any(), any(), any()))
      .thenReturn(Tasks.forResult(null))
    whenever(connectionsClient.acceptConnection(any(), any())).thenReturn(Tasks.forResult(null))

    val flow = connection.startAdvertising()

    val events = mutableListOf<NearbyEvent>()
    val job = launch { flow.collect { events.add(it) } }

    advanceUntilIdle()

    val connectionCallbackCaptor = argumentCaptor<ConnectionLifecycleCallback>()
    verify(connectionsClient)
      .startAdvertising(any<String>(), any(), connectionCallbackCaptor.capture(), any())

    val endpointId = "EP1"
    val connectionCallback = connectionCallbackCaptor.lastValue

    connectionCallback.onConnectionInitiated(endpointId, ConnectionInfo("test", "token", true))

    connectionCallback.onConnectionResult(endpointId, ConnectionResolution(Status.RESULT_SUCCESS))

    advanceUntilIdle()

    assertThat(events[0]).isInstanceOf(NearbyEvent.EndpointConnected::class.java)
    assertThat(events[0].endpointId).isEqualTo(endpointId)

    val payloadCallbackCaptor = argumentCaptor<PayloadCallback>()
    verify(connectionsClient).acceptConnection(eq(endpointId), payloadCallbackCaptor.capture())
    val payloadCallback = payloadCallbackCaptor.lastValue

    payloadCallback.onPayloadReceived(endpointId, Payload.fromBytes(byteArrayOf(1, 2, 3)))
    advanceUntilIdle()

    assertThat(events[1]).isInstanceOf(NearbyEvent.PayloadReceived::class.java)
    val event = events[1] as NearbyEvent.PayloadReceived
    assertThat(event.endpointId).isEqualTo(endpointId)
    assertThat(event.payload).isEqualTo(byteArrayOf(1, 2, 3))

    connectionCallback.onDisconnected(endpointId)
    advanceUntilIdle()

    assertThat(events[2]).isInstanceOf(NearbyEvent.EndpointLost::class.java)
    assertThat(events[2].endpointId).isEqualTo(endpointId)

    job.cancel()
    advanceUntilIdle()

    verify(connectionsClient).stopAdvertising()
  }
}
