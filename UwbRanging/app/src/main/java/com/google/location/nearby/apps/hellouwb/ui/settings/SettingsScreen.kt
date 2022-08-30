package com.google.location.nearby.apps.hellouwb.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.location.nearby.apps.hellouwb.data.AppSettings
import com.google.location.nearby.apps.hellouwb.data.DeviceType

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun SettingsScreen(
    uiState: AppSettings,
    updateDeviceDisplayName: (String) -> Unit,
    updateDeviceType: (DeviceType) -> Unit,
    modifier: Modifier = Modifier,
) {
  val focusManager = LocalFocusManager.current
  Column {
    Row {
      Column {
        Text("Display Name", color = MaterialTheme.colorScheme.outline)
        var fieldValue by remember { mutableStateOf(uiState.deviceDisplayName) }
        OutlinedTextField(
            fieldValue,
            onValueChange = { fieldValue = it },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions =
                KeyboardActions(
                    onDone = {
                      updateDeviceDisplayName(fieldValue)
                      focusManager.clearFocus(true)
                    }),
            singleLine = true)
      }
    }
    Divider(thickness = 2.dp)
    Row {
      Column {
        Text("Device Type", color = MaterialTheme.colorScheme.outline)
        Row(Modifier.padding(8.dp)) {
          val selectedValue = remember { mutableStateOf(uiState.deviceType) }
          Column(Modifier.width(100.dp)) {
            RadioButton(
                selected = selectedValue.value == DeviceType.CONTROLLER,
                onClick = {
                  updateDeviceType(DeviceType.CONTROLLER)
                  selectedValue.value = DeviceType.CONTROLLER
                },
            )
            Text("Controller")
          }
          Column(Modifier.width(100.dp)) {
            RadioButton(
                selected = selectedValue.value == DeviceType.CONTROLEE,
                onClick = {
                  updateDeviceType(DeviceType.CONTROLEE)
                  selectedValue.value = DeviceType.CONTROLEE
                })
            Text("Controlee")
          }
        }
      }
    }
  }
}

@Preview
@Composable
fun PreviewSettingsScreen() {
  SettingsScreen(
      AppSettings.newBuilder()
          .setDeviceDisplayName("qazasx")
          .setDeviceType(DeviceType.CONTROLEE)
          .build(),
      {},
      {})
}
