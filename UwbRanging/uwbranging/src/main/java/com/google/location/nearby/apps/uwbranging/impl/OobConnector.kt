package com.google.location.nearby.apps.uwbranging.impl

import com.google.location.nearby.apps.uwbranging.UwbEndpoint
import kotlinx.coroutines.flow.Flow

internal interface OobConnector {
    fun start(): Flow<UwbOobEvent>
    fun sendMessage(endpoint: UwbEndpoint, message: ByteArray)
}