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

package com.google.apps.uwbranging

import android.content.Context
import com.google.apps.uwbranging.impl.UwbConnectionManagerImpl

/**
 * Manages OOB connection and UWB ranging. It starts OOB connection first, if peer is found, it
 * starts the UWB ranging.
 */
interface UwbConnectionManager {
  fun controllerUwbScope(endpoint: UwbEndpoint, configId: Int): UwbSessionScope

  fun controleeUwbScope(endpoint: UwbEndpoint): UwbSessionScope

  companion object {
    fun getInstance(context: Context): UwbConnectionManager {
      return UwbConnectionManagerImpl(context)
    }
  }
}
