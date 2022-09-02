
package com.google.location.nearby.apps.hellouwb.ui.control

import android.graphics.Color
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.uwb.RangingMeasurement
import androidx.core.uwb.RangingPosition
import com.google.location.nearby.apps.hellouwb.ui.home.ConnectedEndpoint
import com.google.location.nearby.apps.hellouwb.ui.home.HomeScreen
import com.google.location.nearby.apps.hellouwb.ui.home.HomeUiState
import com.google.location.nearby.apps.uwbranging.UwbEndpoint

@Composable fun ControlScreen(modifier: Modifier = Modifier) {

    Column(modifier = Modifier.padding(40.dp)) {
        Text(text = "Switch")
        Button(onClick = {
            //your onclick code here
        }) {
            Text(text = "  On  ")
        }

        Button(onClick = {
            //your onclick code here
        }) {
            Text(text = "  Off  ")
        }
    }
}

@Preview
@Composable
fun PreviewControlScreen(modifier: Modifier = Modifier) {
    ControlScreen(
        modifier = modifier
    )
}



