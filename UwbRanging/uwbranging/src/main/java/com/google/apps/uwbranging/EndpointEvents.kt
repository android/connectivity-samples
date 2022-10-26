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

import androidx.core.uwb.RangingPosition

/** A data class for ranging result update. */
abstract class EndpointEvents internal constructor() {
  /** Represents a UWB endpoint. */
  abstract val endpoint: UwbEndpoint

  /**
   * Device position update.
   *
   * @property position Position of the UWB device during Ranging
   */
  data class PositionUpdated(override val endpoint: UwbEndpoint, val position: RangingPosition) :
    EndpointEvents()

  /** A ranging result with peer disconnected status update. */
  data class UwbDisconnected(override val endpoint: UwbEndpoint) : EndpointEvents()

  /** Endpoint is found. */
  data class EndpointFound(override val endpoint: UwbEndpoint) : EndpointEvents()

  /** Endpoint is lost. */
  data class EndpointLost(override val endpoint: UwbEndpoint) : EndpointEvents()

  /** Received message through OOB. */
  data class EndpointMessage(override val endpoint: UwbEndpoint, val message: ByteArray) :
    EndpointEvents()
}
