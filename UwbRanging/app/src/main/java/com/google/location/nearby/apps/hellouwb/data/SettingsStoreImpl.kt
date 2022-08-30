package com.google.location.nearby.apps.hellouwb.data

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

internal class SettingsStoreImpl(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) : SettingsStore {

  private val Context.settingsDataStore: DataStore<AppSettings> by
      dataStore(fileName = STORE_FILE_NAME, serializer = SettingsSerializer)

  private val _appSettings = MutableStateFlow(SettingsSerializer.defaultValue)

  override val appSettings = _appSettings.asStateFlow()

  init {
    context.settingsDataStore.data
        .onEach { appSettings -> _appSettings.update { appSettings } }
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
