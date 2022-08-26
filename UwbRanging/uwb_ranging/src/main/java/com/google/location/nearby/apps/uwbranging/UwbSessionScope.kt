package com.google.location.nearby.apps.uwbranging

import kotlinx.coroutines.flow.Flow

interface UwbSessionScope {
    fun prepareSession(configId: Int): Flow<UwbRangingResult>
}
