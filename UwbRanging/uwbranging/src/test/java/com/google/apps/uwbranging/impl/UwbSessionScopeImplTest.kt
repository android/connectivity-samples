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
import androidx.core.uwb.RangingPosition
import androidx.core.uwb.RangingResult
import androidx.core.uwb.UwbAddress
import androidx.core.uwb.UwbClientSessionScope
import androidx.core.uwb.UwbComplexChannel
import androidx.core.uwb.UwbDevice
import com.google.common.truth.Truth.assertThat
import com.google.apps.uwbranging.EndpointEvents
import com.google.apps.uwbranging.UwbEndpoint
import com.google.apps.uwbranging.impl.NearbyConnector
import com.google.apps.uwbranging.impl.UwbOobEvent
import com.google.apps.uwbranging.impl.UwbSessionScopeImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
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
  fun testFlow() = runTest {
    whenever(uwbSessionScope.prepareSession(any()))
      .thenReturn(
        channelFlow {
          uwbEventPipe = { trySend(it) }
          awaitClose {}
        }
      )

    val connector = mock<NearbyConnector>()
    whenever(connector.start())
      .thenReturn(
        channelFlow {
          oobEventPipe = { trySend(it) }
          awaitClose {}
        }
      )

    val sessionScopeImpl = UwbSessionScopeImpl(localEndpoint, connector)
    val events = sessionScopeImpl.prepareSession()
    val rangingResults = mutableListOf<EndpointEvents>()
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

    assertThat(rangingResults[0]).isInstanceOf(EndpointEvents.EndpointFound::class.java)
    assertThat(rangingResults[0].endpoint).isEqualTo(remoteEndpoint)

    val rangingParameterCaptor = argumentCaptor<RangingParameters>()
    verify(uwbSessionScope).prepareSession(rangingParameterCaptor.capture())
    val rangingParameters = rangingParameterCaptor.lastValue
    assertThat(rangingParameters.uwbConfigType).isEqualTo(RangingParameters.UWB_CONFIG_ID_1)
    assertThat(rangingParameters.peerDevices.size).isEqualTo(1)
    assertThat(rangingParameters.peerDevices[0].address).isEqualTo(remoteAddress)
    assertThat(rangingParameters.complexChannel!!.channel).isEqualTo(complexChannel.channel)
    assertThat(rangingParameters.complexChannel!!.preambleIndex)
      .isEqualTo(complexChannel.preambleIndex)
    assertThat(rangingParameters.sessionId).isEqualTo(sessionId)
    assertThat(rangingParameters.sessionKeyInfo).isEqualTo(sessionKeyInfo)

    uwbEventPipe(RangingResult.RangingResultPosition(remoteUwbDevice, position))
    advanceUntilIdle()

    assertThat(rangingResults[1]).isInstanceOf(EndpointEvents.PositionUpdated::class.java)
    val result = rangingResults[1] as EndpointEvents.PositionUpdated
    assertThat(result.endpoint).isEqualTo(remoteEndpoint)
    assertThat(result.position).isEqualTo(position)

    uwbEventPipe(RangingResult.RangingResultPeerDisconnected(remoteUwbDevice))
    advanceUntilIdle()

    assertThat(rangingResults[2]).isInstanceOf(EndpointEvents.UwbDisconnected::class.java)
    assertThat(rangingResults[2].endpoint).isEqualTo(remoteEndpoint)

    oobEventPipe(UwbOobEvent.UwbEndpointLost(remoteEndpoint))
    advanceUntilIdle()

    assertThat(rangingResults[3]).isInstanceOf(EndpointEvents.EndpointLost::class.java)
    assertThat(rangingResults[3].endpoint).isEqualTo(remoteEndpoint)

    job.cancel()
  }
}
