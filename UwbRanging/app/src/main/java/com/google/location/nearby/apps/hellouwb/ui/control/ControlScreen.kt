package com.google.location.nearby.apps.hellouwb.ui.control

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.location.nearby.apps.hellouwb.R.drawable


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlScreen(uiState: ControlViewModel.ControlUiState, modifier: Modifier = Modifier) {

    var buttonModifier = Modifier
        .padding(5.dp)
        .width(150.dp)
    var locked by remember { mutableStateOf(false) }

    CenterAlignedTopAppBar(
        title = { androidx.compose.material3.Text("UWB Control") },
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
        Image(
            painter = painterResource(drawable.lock_close24px),
            modifier = Modifier
                .padding(10.dp)
                // Set image size to 40 dp
                .size(200.dp),
            contentDescription = "",
        )
        ExtendedFloatingActionButton(
            modifier = buttonModifier,
            icon = {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = "Lock"
                )
            },
            onClick = {
                locked = !locked
                uiState.isDoorLocked = locked
            },
            text = { Text(if (locked) "Locked" else "Unlocked") }
        )
    }
}


@Preview
@Composable
fun PreviewControlScreen(modifier: Modifier = Modifier) {
    ControlScreen(
        uiState =
        object : ControlViewModel.ControlUiState {
            override var isDoorLocked = true
        }
    )

}



