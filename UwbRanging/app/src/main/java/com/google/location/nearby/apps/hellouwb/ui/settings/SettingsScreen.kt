package com.google.location.nearby.apps.hellouwb.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
    CenterAlignedTopAppBar(
        title = { Text("Device settings") },
//        actions = {
//            val icon = if (isRanging) Icons.Filled.NearMe else Icons.Filled.NearMeDisabled
//            val iconColor = if (isRanging) Color.Green else Color.DarkGray
//            Image(
//                imageVector = icon,
//                colorFilter = ColorFilter.tint(iconColor),
//                contentDescription = null
//            )
//        },
//        scrollBehavior = scrollBehavior,
        modifier = modifier
    )

    val focusManager = LocalFocusManager.current
    Column(
        modifier = Modifier
          .padding(60.dp)
          .fillMaxWidth()
          .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
    )
    {
        Text("Display Name")
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
                }
            ),
            singleLine = true
        )

        Row {
            Column {
                Text("Device Type:", Modifier.padding(20.dp))
                Row(Modifier.padding(5.dp)) {
                    val selectedValue = remember { mutableStateOf(uiState.deviceType) }
                    Column(Modifier.width(120.dp)) {
                        RadioButton(
                            selected = selectedValue.value == DeviceType.CONTROLLER,
                            onClick = {
                                updateDeviceType(DeviceType.CONTROLLER)
                                selectedValue.value = DeviceType.CONTROLLER
                            },
                        )
                        Text("Controller")
                    }
                    Column(Modifier.width(120.dp)) {
                        RadioButton(
                            selected = selectedValue.value == DeviceType.CONTROLEE,
                            onClick = {
                                updateDeviceType(DeviceType.CONTROLEE)
                                selectedValue.value = DeviceType.CONTROLEE
                            }
                        )
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
            .setDeviceDisplayName("UWB")
            .setDeviceType(DeviceType.CONTROLEE)
            .build(),
        {},
        {}
    )
}
