package com.google.location.nearby.apps.uwbranging.impl

import android.content.Context
import com.google.location.nearby.apps.uwbranging.UwbConnectionManager
import com.google.location.nearby.apps.uwbranging.UwbEndpoint
import com.google.location.nearby.apps.uwbranging.UwbSessionScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class UwbConnectionManagerImpl(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1)
) : UwbConnectionManager {
    override suspend fun controllerUwbScope(endpoint: UwbEndpoint, configId: Int): UwbSessionScope {
        return ControllerSessionScopeImpl(context, endpoint, configId, dispatcher)
    }

    override suspend fun controleeUwbScope(endpoint: UwbEndpoint): UwbSessionScope {
        return ControleeSessionScopeImpl(context, endpoint, dispatcher)
    }
}
