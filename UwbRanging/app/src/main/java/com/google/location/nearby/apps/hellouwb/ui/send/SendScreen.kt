/*
 *
 *  * Copyright (C) 2022 The Android Open Source Project
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.google.location.nearby.apps.hellouwb.ui.send


import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendScreen(modifier: Modifier = Modifier) {

    var imageUri by remember {
        mutableStateOf<Uri?>(null)
    }
    val context = LocalContext.current
    val bitmap =  remember {
        mutableStateOf<Bitmap?>(null)
    }

    val launcher = rememberLauncherForActivityResult(contract =
    ActivityResultContracts.GetContent()) { uri: Uri? ->
        imageUri = uri
    }


    CenterAlignedTopAppBar(
        title = { androidx.compose.material3.Text("UWB Secure Transfer") },
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

    Column(
        modifier = Modifier
            .padding(100.dp)
            .fillMaxWidth()
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally

    ) {


        Button(onClick = {
            launcher.launch("image/*")
        }) {
            Text(text = "Select a picture")
        }

        Spacer(modifier = Modifier.height(12.dp))

        imageUri?.let {

                val source = ImageDecoder
                    .createSource(context.contentResolver,it)
                bitmap.value = ImageDecoder.decodeBitmap(source)


            bitmap.value?.let {  btm ->
                Image(bitmap = btm.asImageBitmap(),
                    contentDescription =null,
                    modifier = Modifier.size(400.dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = {
            //TODO implement file sharing
        }) {
            Text(text = "Send file")
        }


    }
}

@Preview
@Composable
fun PreviewSendScreen(modifier: Modifier = Modifier) {
    SendScreen(
        modifier = modifier
    )
}



