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

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlScreen(uiState: ControlUiState, modifier: Modifier = Modifier) {

  CenterAlignedTopAppBar(
    title = { androidx.compose.material3.Text("Device Control") }, modifier = modifier
  )

  Column(
    modifier = Modifier
      .padding(100.dp)
      .fillMaxWidth()
      .fillMaxHeight(),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    if (uiState is ControlUiState.KeyState) {
      KeyScreen()
    } else if (uiState is ControlUiState.LockState) {
      LockScreen(isLocked = uiState.isLocked)
    }
  }
}

@Composable
fun LockScreen(isLocked: Boolean) {
  val icon = if (isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen
  Image(
    imageVector = icon,
    modifier = Modifier
      .height(200.dp)
      .fillMaxWidth(),
    contentDescription = null
  )
}

@Composable
fun KeyScreen() {
  val icon = Icons.Filled.Key

  Image(
    imageVector = icon,
    modifier = Modifier
      .height(200.dp)
      .fillMaxWidth(),
    contentDescription = null
  )

}

@Preview
@Composable
fun PreviewControlScreen() {
  ControlScreen(
    uiState = ControlUiState.KeyState
  )
}
