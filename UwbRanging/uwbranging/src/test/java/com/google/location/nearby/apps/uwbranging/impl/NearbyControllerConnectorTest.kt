package com.google.location.nearby.apps.uwbranging.impl

import androidx.core.uwb.RangingParameters
import androidx.core.uwb.UwbAddress
import androidx.core.uwb.UwbComplexChannel
import androidx.core.uwb.UwbControllerSessionScope
import com.google.common.primitives.Shorts
import com.google.common.truth.Truth.assertThat
import com.google.location.nearby.apps.uwbranging.UwbEndpoint
import com.google.location.nearby.apps.uwbranging.impl.proto.UwbCapabilities
import com.google.location.nearby.apps.uwbranging.impl.proto.UwbConfiguration
import com.google.location.nearby.apps.uwbranging.impl.proto.UwbConnectionInfo
import com.google.location.nearby.apps.uwbranging.impl.proto.UwbSessionInfo
import com.google.protobuf.ByteString
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
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
class NearbyControllerConnectorTest {

    private val connections = mock<NearbyConnections>()
    private val controllerSessionScope = mock<UwbControllerSessionScope>()

    private lateinit var controllerConnector: NearbyControllerConnector

    private val uwbEndpoint = UwbEndpoint("UWB2", byteArrayOf(3, 4, 5))

    private val controleeSessionInfo = UwbSessionInfo.newBuilder().setId("UWB1")
        .setMetadata(ByteString.copyFrom(byteArrayOf(1, 2, 3)))
        .setLocalAddress(Shorts.fromByteArray(byteArrayOf(1, 2)).toInt())
        .setConnectionInfo(
            UwbConnectionInfo.newBuilder().setCapabilities(
                UwbCapabilities.newBuilder()
                    .addAllSupportedConfigIds(listOf(RangingParameters.UWB_CONFIG_ID_1))
                    .setSupportsAzimuth(true).setSupportsElevation(true)
                    .build()
            ).build()
        ).build()

    private lateinit var eventPipe: (event: NearbyEvent) -> Unit

    @Before
    fun setUp() {
        whenever(controllerSessionScope.localAddress).thenReturn(UwbAddress(byteArrayOf(3, 4)))
        whenever(controllerSessionScope.uwbComplexChannel).thenReturn(UwbComplexChannel(9, 11))
        controllerConnector =
            NearbyControllerConnector(uwbEndpoint, RangingParameters.UWB_CONFIG_ID_1, connections) {
                controllerSessionScope
            }
    }

    @Test
    fun testDiscovery() = runTest {
        whenever(connections.startDiscovery()).thenReturn(
            channelFlow {
                eventPipe = { trySend(it) }
                awaitClose {}
            }
        )
        val flow = controllerConnector.start()
        val events = mutableListOf<UwbOobEvent>()
        val job = launch { flow.collect { events.add(it) } }
        advanceUntilIdle()

        eventPipe(NearbyEvent.EndpointConnected("EP1"))
        eventPipe(NearbyEvent.PayloadReceived("EP1", controleeSessionInfo.toByteArray()))
        advanceUntilIdle()

        assertThat(events[0]).isInstanceOf(UwbOobEvent.UwbEndpointFound::class.java)
        val event = events[0] as UwbOobEvent.UwbEndpointFound
        assertThat(event.sessionScope).isSameInstanceAs(controllerSessionScope)
        assertThat(event.complexChannel.channel).isEqualTo(9)
        assertThat(event.complexChannel.preambleIndex).isEqualTo(11)
        assertThat(event.configId).isEqualTo(RangingParameters.UWB_CONFIG_ID_1)
        assertThat(event.endpointAddress).isEqualTo(UwbAddress(byteArrayOf(1, 2)))
        assertThat(event.endpoint.id).isEqualTo("UWB1")
        assertThat(event.endpoint.metadata).isEqualTo(byteArrayOf(1, 2, 3))

        val bytesCaptor = argumentCaptor<ByteArray>()
        verify(connections).sendPayload(eq("EP1"), bytesCaptor.capture())

        val sentSessionInfo = UwbSessionInfo.parseFrom(bytesCaptor.lastValue)
        val controllerSessionInfo = UwbSessionInfo.newBuilder().setId("UWB2")
            .setMetadata(ByteString.copyFrom(byteArrayOf(3, 4, 5)))
            .setLocalAddress(Shorts.fromByteArray(byteArrayOf(3, 4)).toInt())
            .setConnectionInfo(
                UwbConnectionInfo.newBuilder().setConfiguration(
                    UwbConfiguration.newBuilder().setConfigId(RangingParameters.UWB_CONFIG_ID_1)
                        .setChannel(9).setPreambleIndex(11)
                        .setSessionId(event.sessionId)
                        .setSecurityInfo(ByteString.copyFrom(event.sessionKeyInfo))
                        .build()
                ).build()
            ).build()

        assertThat(sentSessionInfo).isEqualTo(controllerSessionInfo)

        eventPipe(NearbyEvent.EndpointLost("EP1"))
        advanceUntilIdle()

        assertThat(events[1]).isInstanceOf(UwbOobEvent.UwbEndpointLost::class.java)
        assertThat(events[1].endpoint.id).isEqualTo("UWB1")

        job.cancel()
    }
}