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

package com.google.apps.hellouwb.ui.control

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.apps.hellouwb.data.DeviceType
import com.google.apps.hellouwb.data.SettingsStore
import com.google.apps.hellouwb.data.UwbRangingControlSource
import com.google.apps.uwbranging.EndpointEvents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private const val LOCK_DISTANCE = 2.0f
private const val UNLOCK_DISTANCE = 0.25f

class ControlViewModel(
    private val uwbRangingControlSource: UwbRangingControlSource,
    settingsStore: SettingsStore
) : ViewModel() {

    private val _uiState: MutableStateFlow<ControlUiState> =
        MutableStateFlow(ControlUiState.KeyState)

    val uiState = _uiState.asStateFlow()

    private var lockJob: Job? = null

    private fun startLockObserving(): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            launch {
                uwbRangingControlSource
                    .observeRangingResults()
                    .filterIsInstance<EndpointEvents.PositionUpdated>()
                    .collect {
                        it.position.distance?.let {
                            val state = _uiState.value as ControlUiState.LockState
                            if (!state.isLocked && it.value > LOCK_DISTANCE) {
                                _uiState.update { ControlUiState.LockState(isLocked = true) }
                            }
                            if (state.isLocked && it.value < UNLOCK_DISTANCE) {
                                _uiState.update { ControlUiState.LockState(isLocked = false) }
                            }
                        }
                    }
            }
            launch {
                uwbRangingControlSource.isRunning.collect {
                    val state = _uiState.value as ControlUiState.LockState
                    if (!state.isLocked && !it) {
                        _uiState.update { ControlUiState.LockState(isLocked = true) }
                    }
                }
            }
        }
    }

    init {
        settingsStore.appSettings
            .onEach {
                lockJob?.cancel()
                lockJob = null
                when (it.deviceType) {
                    DeviceType.CONTROLEE -> _uiState.update { ControlUiState.KeyState }
                    DeviceType.CONTROLLER -> {
                        if (_uiState.value !is ControlUiState.LockState) {
                            _uiState.update { ControlUiState.LockState(isLocked = true) }
                            lockJob = startLockObserving()
                        }
                    }
                    else -> {}
                }
            }
            .launchIn(viewModelScope)
    }

    companion object {
        fun provideFactory(
            uwbRangingControlSource: UwbRangingControlSource,
            settingsStore: SettingsStore
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ControlViewModel(uwbRangingControlSource, settingsStore) as T
                }
            }
    }
}

sealed class ControlUiState {

    data class LockState(val isLocked: Boolean) : ControlUiState()

    object KeyState : ControlUiState()
}
