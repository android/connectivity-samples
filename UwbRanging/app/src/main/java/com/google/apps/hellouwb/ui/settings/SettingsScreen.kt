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

package com.google.apps.hellouwb.ui.settings

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
import com.google.apps.hellouwb.data.AppSettings
import com.google.apps.hellouwb.data.DeviceType
import com.google.apps.hellouwb.data.ConfigType

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun SettingsScreen(
    uiState: AppSettings,
    updateDeviceDisplayName: (String) -> Unit,
    updateDeviceType: (DeviceType) -> Unit,
    updateConfigType: (ConfigType) -> Unit,
    modifier: Modifier = Modifier,
) {
    CenterAlignedTopAppBar(
        title = { Text("Device Settings") },
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
                Text("Config Type:", Modifier.padding(20.dp))
                Row(Modifier.padding(5.dp)) {
                    val selectedValue = remember { mutableStateOf(uiState.configType) }
                    Column(Modifier.width(120.dp)) {
                        RadioButton(
                            selected = selectedValue.value == ConfigType.CONFIG_UNICAST_DS_TWR,
                            onClick = {
                                updateConfigType(ConfigType.CONFIG_UNICAST_DS_TWR)
                                selectedValue.value = ConfigType.CONFIG_UNICAST_DS_TWR
                            },
                        )
                        Text("CONFIG_UNICAST_DS_TWR")
                    }
                    Column(Modifier.width(120.dp)) {
                        RadioButton(
                            selected = selectedValue.value == ConfigType.CONFIG_MULTICAST_DS_TWR,
                            onClick = {
                                updateConfigType(ConfigType.CONFIG_MULTICAST_DS_TWR)
                                selectedValue.value = ConfigType.CONFIG_MULTICAST_DS_TWR
                            }
                        )
                        Text("CONFIG_MULTICAST_DS_TWR")
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
            .setConfigType(ConfigType.CONFIG_MULTICAST_DS_TWR)
            .build(),
        {},
        {},
        {}
    )
}
