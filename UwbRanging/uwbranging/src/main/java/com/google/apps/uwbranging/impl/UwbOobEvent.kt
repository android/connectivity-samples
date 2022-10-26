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

import androidx.core.uwb.UwbAddress
import androidx.core.uwb.UwbClientSessionScope
import androidx.core.uwb.UwbComplexChannel
import com.google.apps.uwbranging.UwbEndpoint

/** Events that happen during UWB OOB Connections. */
internal abstract class UwbOobEvent private constructor() {

  abstract val endpoint: UwbEndpoint

  /** An event that notifies an endpoint is found through OOB. */
  data class UwbEndpointFound(
      override val endpoint: UwbEndpoint,
      val configId: Int,
      val endpointAddress: UwbAddress,
      val complexChannel: UwbComplexChannel,
      val sessionId: Int,
      val sessionKeyInfo: ByteArray,
      val sessionScope: UwbClientSessionScope,
  ) : UwbOobEvent()

  /** An event that notifies a UWB endpoint is lost. */
  data class UwbEndpointLost(override val endpoint: UwbEndpoint) : UwbOobEvent()

  /** Notifies that a message is received. */
  data class MessageReceived(override val endpoint: UwbEndpoint, val message: ByteArray) :
    UwbOobEvent()
}
