/*
 *
 *  * Copyright (C) 2022 The Android Open Source Project
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.google.location.nearby.apps.hellouwb.data

import android.content.Context
import androidx.core.uwb.RangingParameters
import com.google.location.nearby.apps.uwbranging.EndpointEvents
import com.google.location.nearby.apps.uwbranging.UwbConnectionManager
import com.google.location.nearby.apps.uwbranging.UwbEndpoint
import com.google.location.nearby.apps.uwbranging.UwbSessionScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.security.SecureRandom
import kotlin.properties.Delegates

internal class UwbRangingControlSourceImpl(
  context: Context,
  endpointId: String,
  private val coroutineScope: CoroutineScope,
  private val uwbConnectionManager: UwbConnectionManager =
    UwbConnectionManager.getInstance(context),
) : UwbRangingControlSource {

  private var uwbEndpoint = UwbEndpoint(endpointId, SecureRandom.getSeed(8))

  private var uwbSessionScope: UwbSessionScope = getSessionScope(DeviceType.CONTROLLER)

  private var rangingJob: Job? = null

  private fun getSessionScope(deviceType: DeviceType): UwbSessionScope {
    return when (deviceType) {
      DeviceType.CONTROLEE -> uwbConnectionManager.controleeUwbScope(uwbEndpoint)
      DeviceType.CONTROLLER ->
        uwbConnectionManager.controllerUwbScope(uwbEndpoint, RangingParameters.UWB_CONFIG_ID_1)
      else -> throw IllegalStateException()
    }
  }

  private var dispatchResult: (result: EndpointEvents) -> Unit = {}
  override fun observeRangingResults() = channelFlow {
    dispatchResult = { trySend(it) }
    awaitClose { dispatchResult = {} }
  }

  override var deviceType by
    Delegates.observable(DeviceType.CONTROLLER) { _, oldValue, newValue ->
      if (oldValue != newValue) {
        stop()
        uwbSessionScope = getSessionScope(newValue)
      }
    }

  override fun updateEndpointId(id: String) {
    if (id != uwbEndpoint.id) {
      stop()
      uwbEndpoint = UwbEndpoint(id, SecureRandom.getSeed(8))
      uwbSessionScope = getSessionScope(deviceType)
    }
  }

  override fun start() {
    if (rangingJob == null) {
      rangingJob =
        coroutineScope.launch { uwbSessionScope.prepareSession().collect { dispatchResult(it) } }
      runningStateFlow.update { true }
    }
  }

  override fun stop() {
    val job = rangingJob ?: return
    job.cancel()
    rangingJob = null
    runningStateFlow.update { false }
  }

  private val runningStateFlow = MutableStateFlow(false)

  override val isRunning = runningStateFlow.asStateFlow()
}
