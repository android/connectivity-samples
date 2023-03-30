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

package com.google.apps.hellouwb.data

import android.content.Context
import androidx.core.uwb.RangingParameters
import com.google.apps.uwbranging.EndpointEvents
import com.google.apps.uwbranging.UwbConnectionManager
import com.google.apps.uwbranging.UwbEndpoint
import com.google.apps.uwbranging.UwbSessionScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
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

  private var uwbSessionScope: UwbSessionScope =
    getSessionScope(DeviceType.CONTROLLER, ConfigType.CONFIG_UNICAST_DS_TWR)

  private var rangingJob: Job? = null

  private val resultFlow = MutableSharedFlow<EndpointEvents>(
    replay = 0,
    onBufferOverflow = BufferOverflow.DROP_OLDEST,
    extraBufferCapacity = 1
  )

  private val runningStateFlow = MutableStateFlow(false)

  override val isRunning = runningStateFlow.asStateFlow()

  private fun getSessionScope(deviceType: DeviceType, configType: ConfigType): UwbSessionScope {
    return when (deviceType) {
      DeviceType.CONTROLEE -> uwbConnectionManager.controleeUwbScope(uwbEndpoint)
      DeviceType.CONTROLLER ->
        uwbConnectionManager.controllerUwbScope(uwbEndpoint, when (configType) {
          ConfigType.CONFIG_UNICAST_DS_TWR -> RangingParameters.CONFIG_UNICAST_DS_TWR
          ConfigType.CONFIG_MULTICAST_DS_TWR -> RangingParameters.CONFIG_MULTICAST_DS_TWR
          else -> throw java.lang.IllegalStateException()
        })
      else -> throw IllegalStateException()
    }
  }

  override fun observeRangingResults(): Flow<EndpointEvents> {
    return resultFlow
  }

  override var deviceType: DeviceType by
  Delegates.observable(DeviceType.CONTROLLER) { _, oldValue, newValue ->
    if (oldValue != newValue) {
      stop()
      uwbSessionScope = getSessionScope(newValue, configType)
    }
  }

  override var configType: ConfigType by
  Delegates.observable(ConfigType.CONFIG_UNICAST_DS_TWR) { _, oldValue, newValue ->
    if (oldValue != newValue) {
      stop()
      uwbSessionScope = getSessionScope(deviceType, newValue)
    }
  }

  override fun updateEndpointId(id: String) {
    if (id != uwbEndpoint.id) {
      stop()
      uwbEndpoint = UwbEndpoint(id, SecureRandom.getSeed(8))
      uwbSessionScope = getSessionScope(deviceType, configType)
    }
  }

  override fun start() {
    if (rangingJob == null) {
      rangingJob =
        coroutineScope.launch {
          uwbSessionScope.prepareSession().collect {
            resultFlow.tryEmit(it)
          }
        }
      runningStateFlow.update { true }
    }
  }

  override fun stop() {
    val job = rangingJob ?: return
    job.cancel()
    rangingJob = null
    runningStateFlow.update { false }
  }

  override fun sendOobMessage(endpoint: UwbEndpoint, message: ByteArray) {
    uwbSessionScope.sendMessage(endpoint, message)
  }
}
