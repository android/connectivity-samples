package com.google.location.nearby.apps.uwbranging.impl

import androidx.core.uwb.UwbAddress
import androidx.core.uwb.UwbClientSessionScope
import androidx.core.uwb.UwbComplexChannel
import com.google.location.nearby.apps.uwbranging.UwbEndpoint

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
      val sessionScope: UwbClientSessionScope
  ) : UwbOobEvent()

  /** An event that notifies a UWB endpoint is lost. */
  data class UwbEndpointLost(override val endpoint: UwbEndpoint) : UwbOobEvent()
}
