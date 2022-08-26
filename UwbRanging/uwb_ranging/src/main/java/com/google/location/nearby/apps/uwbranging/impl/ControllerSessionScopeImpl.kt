package com.google.location.nearby.apps.uwbranging.impl

import android.content.Context
import androidx.core.uwb.UwbManager
import com.google.location.nearby.apps.uwbranging.UwbEndpoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn

internal class ControllerSessionScopeImpl
    (
    private val context: Context,
    private val localEndpoint: UwbEndpoint,
    private val configId: Int,
    private val dispatcher: CoroutineDispatcher
) : UwbSessionScopeImpl(localEndpoint) {

    private val uwbManager = UwbManager.createInstance(context)

    override fun startOob(): Flow<UwbOobEvent> {
        val connector =
            NearbyControllerConnector(
                localEndpoint,
                configId,
                NearbyConnections(context, dispatcher)
            ) {
                uwbManager.controllerSessionScope()
            }
        return connector.start()
    }
}
