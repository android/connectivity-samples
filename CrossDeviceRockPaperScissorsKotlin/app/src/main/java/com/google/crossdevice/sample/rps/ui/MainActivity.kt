/*
 * Copyright 2022 Google LLC
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
 */

package com.google.crossdevice.sample.rps.ui

import android.content.Intent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.crossdevice.sample.rps.R

/** Activity for selecting the kind of Rock Paper Scissors game to play */
class MainActivity : AppCompatActivity(R.layout.activity_main) {
  fun onModeSelected(view: View) {
    getIntentForModeSelected(view.id)?.let { startActivity(it) }
  }

  private fun getIntentForModeSelected(id: Int): Intent? {
    return when (id) {
      R.id.two_player_discovery_api -> Intent(this, DiscoveryTwoPlayerActivity::class.java)
      R.id.two_player_sessions_api -> Intent(this, SessionsTwoPlayerActivity::class.java)
      R.id.single_player_sessions_api -> Intent(this, SessionsSinglePlayerActivity::class.java)
      else -> null
    }
  }
}
