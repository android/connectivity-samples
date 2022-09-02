package com.google.location.nearby.apps.hellouwb.ui.send

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.location.nearby.apps.hellouwb.ui.control.ControlScreen

@Composable fun SendScreen(modifier: Modifier = Modifier) {

    Column(modifier = Modifier.padding(40.dp)) {
        Text(text = "Point to share file")
        Button(onClick = {
            //your onclick code here
        }) {
            Text(text = "Share file")
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



