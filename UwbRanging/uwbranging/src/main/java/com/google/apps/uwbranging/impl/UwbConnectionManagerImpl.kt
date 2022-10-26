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

import android.content.Context
import androidx.core.uwb.UwbManager
import com.google.apps.uwbranging.UwbConnectionManager
import com.google.apps.uwbranging.UwbEndpoint
import com.google.apps.uwbranging.UwbSessionScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal class UwbConnectionManagerImpl(
  private val context: Context,
  private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : UwbConnectionManager {

  private val uwbManager = UwbManager.createInstance(context)

  override fun controllerUwbScope(endpoint: UwbEndpoint, configId: Int): UwbSessionScope {
    val connector =
      NearbyControllerConnector(endpoint, configId, NearbyConnections(context, dispatcher)) {
        uwbManager.controllerSessionScope()
      }
    return UwbSessionScopeImpl(endpoint, connector)
  }

  override fun controleeUwbScope(endpoint: UwbEndpoint): UwbSessionScope {
    val connector =
      NearbyControleeConnector(endpoint, NearbyConnections(context, dispatcher)) {
        uwbManager.controleeSessionScope()
      }
    return UwbSessionScopeImpl(endpoint, connector)
  }
}
