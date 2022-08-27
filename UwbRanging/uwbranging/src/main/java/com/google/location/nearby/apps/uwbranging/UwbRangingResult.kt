package com.google.location.nearby.apps.uwbranging

import androidx.core.uwb.RangingPosition

/** A data class for ranging result update. */
abstract class UwbRangingResult internal constructor() {
    /** Represents a UWB endpoint. */
    abstract val endpoint: UwbEndpoint

    /**
     * A ranging result with the device position update.
     *
     * @property position Position of the UWB device during Ranging
     */
    class RangingResultPosition(override val endpoint: UwbEndpoint, val position: RangingPosition) :
        UwbRangingResult()

    /** A ranging result with peer disconnected status update. */
    class RangingResultPeerDisconnected(override val endpoint: UwbEndpoint) : UwbRangingResult()
}
