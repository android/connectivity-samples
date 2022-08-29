package com.google.location.nearby.apps.uwbranging

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
