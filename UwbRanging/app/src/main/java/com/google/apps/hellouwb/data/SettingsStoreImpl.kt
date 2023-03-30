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

package com.google.apps.hellouwb.data

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.google.protobuf.InvalidProtocolBufferException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.util.*

internal class SettingsStoreImpl(
  private val context: Context,
  private val coroutineScope: CoroutineScope,
) : SettingsStore {

  private val Context.settingsDataStore: DataStore<AppSettings> by
    dataStore(fileName = STORE_FILE_NAME, serializer = SettingsSerializer)

  override val appSettings: StateFlow<AppSettings> =
    MutableStateFlow(SettingsSerializer.defaultValue)

  init {

    context.settingsDataStore.data
      .onEach { settings -> (appSettings as MutableStateFlow<AppSettings>).update { settings } }
      .shareIn(coroutineScope, SharingStarted.Eagerly)
  }

  override fun updateDeviceType(deviceType: DeviceType) {
    coroutineScope.launch {
      context.settingsDataStore.updateData { settings ->
        settings.toBuilder().setDeviceType(deviceType).build()
      }
    }
  }

  override fun updateConfigType(configType: ConfigType) {
    coroutineScope.launch {
      context.settingsDataStore.updateData { settings ->
        settings.toBuilder().setConfigType(configType).build()
      }
    }
  }

  override fun updateDeviceDisplayName(displayName: String) {
    coroutineScope.launch {
      context.settingsDataStore.updateData { settings ->
        settings.toBuilder().setDeviceDisplayName(displayName).build()
      }
    }
  }

  companion object {
    private const val STORE_FILE_NAME = "app_settings.pb"

    private object SettingsSerializer : Serializer<AppSettings> {
      override val defaultValue: AppSettings =
        AppSettings.newBuilder()
          .setDeviceType(DeviceType.CONTROLLER)
          .setDeviceDisplayName("UWB")
          .setDeviceUuid(UUID.randomUUID().toString())
          .build()

      override suspend fun readFrom(input: InputStream): AppSettings {
        try {
          return AppSettings.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
          throw CorruptionException("Cannot read proto.", exception)
        }
      }

      override suspend fun writeTo(t: AppSettings, output: OutputStream) = t.writeTo(output)
    }
  }
}
