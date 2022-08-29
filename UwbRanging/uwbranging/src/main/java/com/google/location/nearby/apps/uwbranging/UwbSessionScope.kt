package com.google.location.nearby.apps.uwbranging

import kotlinx.coroutines.flow.Flow

interface UwbSessionScope {

  fun prepareSession(): Flow<EndpointEvents>

  fun sendMessage(endpoint: UwbEndpoint, message: ByteArray)
}
