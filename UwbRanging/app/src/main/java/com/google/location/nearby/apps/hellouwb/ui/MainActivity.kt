package com.google.location.nearby.apps.hellouwb.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.google.location.nearby.apps.hellouwb.HelloUwbApplication

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val appContainer = (application as HelloUwbApplication).container
    setContent { HelloUwbApp(appContainer) }
  }
}
