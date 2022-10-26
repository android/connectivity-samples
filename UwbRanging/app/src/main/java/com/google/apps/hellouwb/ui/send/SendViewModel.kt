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

package com.google.apps.hellouwb.ui.send

import android.content.ContentResolver
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.apps.hellouwb.data.UwbRangingControlSource
import com.google.apps.uwbranging.EndpointEvents
import com.google.apps.uwbranging.UwbEndpoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

private const val SEND_IMAGE_OP_CODE: Byte = 1

private const val RECEIVED_FILE_PATH = "received"

class SendViewModel(
  private val uwbRangingControlSource: UwbRangingControlSource,
  private val contentResolver: ContentResolver
) : ViewModel() {

  private val _uiState: MutableStateFlow<SendUiState> = MutableStateFlow(SendUiState.InitialState)

  val uiState = _uiState.asStateFlow()

  private var sendJob: Job? = null

  private var receiveJob: Job? = null

  fun clear() {
    receiveJob?.cancel()
    receiveJob = startReceivingJob()
    sendJob?.cancel()
    sendJob = null
    _uiState.update { SendUiState.InitialState }
  }

  fun setSentUri(uri: Uri) {
    sendJob?.cancel()
    sendJob = null
    startSendingJob(uri)?.let {
      sendJob = it
      _uiState.update { SendUiState.SendingState(uri, null) }
    }
  }

  fun messageShown() {
    when (val state = _uiState.value) {
      is SendUiState.SendingState -> _uiState.update { state.copy(message = null) }
      else -> {}
    }
  }

  private fun startReceivingJob(): Job {
    return CoroutineScope(Dispatchers.IO).launch {
      uwbRangingControlSource
        .observeRangingResults()
        .filterIsInstance<EndpointEvents.EndpointMessage>()
        .filter { it.message[0] == SEND_IMAGE_OP_CODE }
        .collect { event ->
          onImageReceived(event.endpoint, event.message.sliceArray(1 until event.message.size))
        }
    }
  }

  private fun onImageReceived(endpoint: UwbEndpoint, imageBytes: ByteArray) {
    receiveJob?.cancel()
    receiveJob = null
    val file = File.createTempFile(RECEIVED_FILE_PATH, null)
    contentResolver.openOutputStream(file.toUri())?.use { it.write(imageBytes) }
    _uiState.update { SendUiState.ReceivedState(endpoint, file.toUri()) }
  }

  private fun startSendingJob(uri: Uri): Job? {
    contentResolver.openInputStream(uri)?.use { inputStream ->
      val bytesToSend = byteArrayOf(SEND_IMAGE_OP_CODE) + inputStream.readBytes()
      val endpointsSent = mutableSetOf<UwbEndpoint>()
      return CoroutineScope(Dispatchers.IO).launch {
        uwbRangingControlSource
          .observeRangingResults()
          .filterNot { it.endpoint in endpointsSent }
          .filterIsInstance<EndpointEvents.PositionUpdated>()
          .collect { event ->
            event.position.azimuth?.let { azimuth ->
              if (azimuth.value > -5.0f && azimuth.value < 5.0f) {
                endpointsSent.add(event.endpoint)
                uwbRangingControlSource.sendOobMessage(event.endpoint, bytesToSend)
                val endpointDisplayName = event.endpoint.id.split("|")[0]
                _uiState.update {
                  SendUiState.SendingState(
                    uri,
                    "Image has been sent to $endpointDisplayName"
                  )
                }
              }
            }
          }
      }
    }
    return null
  }

  init {
    clear()
  }

  companion object {
    fun provideFactory(
      uwbRangingControlSource: UwbRangingControlSource,
      contentResolver: ContentResolver
    ): ViewModelProvider.Factory =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
          return SendViewModel(uwbRangingControlSource, contentResolver) as T
        }
      }
  }
}

sealed class SendUiState {

  data class SendingState(val sendImageUri: Uri, val message: String?) : SendUiState()

  data class ReceivedState(val endpoint: UwbEndpoint, val receivedImageUri: Uri) : SendUiState()

  object InitialState : SendUiState()
}
