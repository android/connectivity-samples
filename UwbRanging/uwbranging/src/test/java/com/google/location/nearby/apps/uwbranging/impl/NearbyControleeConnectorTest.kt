package com.google.location.nearby.apps.uwbranging.impl

import androidx.core.uwb.RangingCapabilities
import androidx.core.uwb.RangingParameters
import androidx.core.uwb.UwbAddress
import androidx.core.uwb.UwbControleeSessionScope
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
class NearbyControleeConnectorTest {

    private val connections = mock<NearbyConnections>()
    private val controleeSessionScope = mock<UwbControleeSessionScope>()

    private lateinit var controleeConnector: NearbyControleeConnector

    private val uwbEndpoint = UwbEndpoint("UWB1", byteArrayOf(1, 2, 3))

    private val controllerSessionInfo = UwbSessionInfo.newBuilder().setId("UWB2")
        .setMetadata(ByteString.copyFrom(byteArrayOf(3, 4, 5)))
        .setLocalAddress(Shorts.fromByteArray(byteArrayOf(3, 4)).toInt())
        .setConnectionInfo(
            UwbConnectionInfo.newBuilder().setConfiguration(
                UwbConfiguration.newBuilder().setConfigId(RangingParameters.UWB_CONFIG_ID_1)
                    .setChannel(9).setPreambleIndex(11)
                    .setSessionId(0x12345678)
                    .setSecurityInfo(ByteString.copyFrom(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)))
                    .build()
            ).build()
        ).build()

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
        whenever(controleeSessionScope.localAddress).thenReturn(UwbAddress(byteArrayOf(1, 2)))
        whenever(controleeSessionScope.rangingCapabilities).thenReturn(
            RangingCapabilities(
                isDistanceSupported = true,
                isAzimuthalAngleSupported = true,
                isElevationAngleSupported = true
            )
        )
        controleeConnector =
            NearbyControleeConnector(uwbEndpoint, connections) {
                controleeSessionScope
            }
    }

    @Test
    fun testAdvertising() = runTest {
        whenever(connections.startAdvertising()).thenReturn(
            channelFlow {
                eventPipe = { trySend(it) }
                awaitClose {}
            }
        )
        val flow = controleeConnector.start()
        val events = mutableListOf<UwbOobEvent>()
        val job = launch { flow.collect { events.add(it) } }
        advanceUntilIdle()

        eventPipe(NearbyEvent.EndpointConnected("EP1"))
        advanceUntilIdle()

        val bytesCaptor = argumentCaptor<ByteArray>()
        verify(connections).sendPayload(eq("EP1"), bytesCaptor.capture())
        val sentSessionInfo = UwbSessionInfo.parseFrom(bytesCaptor.lastValue)
        assertThat(sentSessionInfo).isEqualTo(controleeSessionInfo)

        eventPipe(NearbyEvent.PayloadReceived("EP1", controllerSessionInfo.toByteArray()))
        advanceUntilIdle()

        assertThat(events[0]).isInstanceOf(UwbOobEvent.UwbEndpointFound::class.java)
        val event = events[0] as UwbOobEvent.UwbEndpointFound
        assertThat(event.sessionScope).isSameInstanceAs(controleeSessionScope)
        assertThat(event.complexChannel.channel).isEqualTo(9)
        assertThat(event.complexChannel.preambleIndex).isEqualTo(11)
        assertThat(event.configId).isEqualTo(RangingParameters.UWB_CONFIG_ID_1)
        assertThat(event.endpointAddress).isEqualTo(UwbAddress(byteArrayOf(3, 4)))
        assertThat(event.endpoint.id).isEqualTo("UWB2")
        assertThat(event.endpoint.metadata).isEqualTo(byteArrayOf(3, 4, 5))
        assertThat(event.sessionId).isEqualTo(0x12345678)
        assertThat(event.sessionKeyInfo).isEqualTo(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8))

        eventPipe(NearbyEvent.EndpointLost("EP1"))
        advanceUntilIdle()

        assertThat(events[1]).isInstanceOf(UwbOobEvent.UwbEndpointLost::class.java)
        assertThat(events[1].endpoint.id).isEqualTo("UWB2")

        job.cancel()
    }
}