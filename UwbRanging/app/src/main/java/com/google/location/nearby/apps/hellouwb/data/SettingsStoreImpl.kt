package com.google.location.nearby.apps.hellouwb.data

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
