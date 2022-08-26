package com.google.location.nearby.apps.uwbranging.impl

import androidx.core.uwb.*
import com.google.common.truth.Truth.assertThat
import com.google.location.nearby.apps.uwbranging.UwbRangingResult
import com.google.location.nearby.apps.uwbranging.UwbEndpoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import androidx.core.uwb.RangingResult

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class UwbSessionScopeImplTest {

    private lateinit var oobEventPipe: (event: UwbOobEvent) -> Unit
    private lateinit var uwbEventPipe: (event: RangingResult) -> Unit

    private val localEndpoint = UwbEndpoint("UWB1", byteArrayOf(1, 2, 3))
    private val remoteEndpoint = UwbEndpoint("UWB2", byteArrayOf(3, 4, 5))
    private val complexChannel = UwbComplexChannel(9, 11)
    private val remoteAddress = UwbAddress(byteArrayOf(3, 4))
    private val sessionId = 0x12345678
    private val sessionKeyInfo = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
    private val uwbSessionScope = mock<UwbClientSessionScope>()
    private val remoteUwbDevice = UwbDevice(remoteAddress)
    private val position = mock<RangingPosition>()

    @Test
    fun test() = runTest {
        whenever(uwbSessionScope.prepareSession(any())).thenReturn(
            channelFlow {
                uwbEventPipe = { trySend(it) }
                awaitClose {}
            }
        )
        val sessionScopeImpl = TestUwbSessionScopeImpl(localEndpoint,
            channelFlow {
                oobEventPipe = { trySend(it) }
                awaitClose {}
            }
        )
        val events = sessionScopeImpl.prepareSession(RangingParameters.UWB_CONFIG_ID_1)
        val rangingResults = mutableListOf<UwbRangingResult>()
        val job = launch { events.collect { rangingResults.add(it) } }
        advanceUntilIdle()

        oobEventPipe(
            UwbOobEvent.UwbEndpointFound(
                remoteEndpoint,
                RangingParameters.UWB_CONFIG_ID_1,
                remoteAddress,
                complexChannel,
                sessionId,
                sessionKeyInfo,
                uwbSessionScope
            )
        )
        advanceUntilIdle()

        val rangingParameterCaptor = argumentCaptor<RangingParameters>()
        verify(uwbSessionScope).prepareSession(rangingParameterCaptor.capture())
        val rangingParameters = rangingParameterCaptor.lastValue
        assertThat(rangingParameters.uwbConfigType).isEqualTo(RangingParameters.UWB_CONFIG_ID_1)
        assertThat(rangingParameters.peerDevices.size).isEqualTo(1)
        assertThat(rangingParameters.peerDevices[0].address).isEqualTo(remoteAddress)
        assertThat(rangingParameters.complexChannel!!.channel).isEqualTo(complexChannel.channel)
        assertThat(rangingParameters.complexChannel!!.preambleIndex).isEqualTo(complexChannel.preambleIndex)
        assertThat(rangingParameters.sessionId).isEqualTo(sessionId)
        assertThat(rangingParameters.sessionKeyInfo).isEqualTo(sessionKeyInfo)

        uwbEventPipe(RangingResult.RangingResultPosition(remoteUwbDevice, position))
        advanceUntilIdle()

        assertThat(rangingResults[0]).isInstanceOf(UwbRangingResult.RangingResultPosition::class.java)
        val result = rangingResults[0] as UwbRangingResult.RangingResultPosition
        assertThat(result.endpoint).isEqualTo(remoteEndpoint)
        assertThat(result.position).isEqualTo(position)

        uwbEventPipe(RangingResult.RangingResultPeerDisconnected(remoteUwbDevice))
        advanceUntilIdle()

        assertThat(rangingResults[1]).isInstanceOf(UwbRangingResult.RangingResultPeerDisconnected::class.java)
        assertThat(rangingResults[1].endpoint).isEqualTo(remoteEndpoint)

        job.cancel()
    }

    private class TestUwbSessionScopeImpl(
        localEndpoint: UwbEndpoint,
        private val eventFlow: Flow<UwbOobEvent>
    ) : UwbSessionScopeImpl(localEndpoint) {
        override fun startOob(): Flow<UwbOobEvent> {
            return eventFlow
        }
    }
}