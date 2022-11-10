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

package com.google.crossdevice.sample.rps.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.crossdevice.sample.rps.R;

/** Activity for selecting the kind of Rock Paper Scissors game to play */
public final class MainActivity extends AppCompatActivity {

  @Override
  protected void onCreate(@Nullable Bundle bundle) {
    super.onCreate(bundle);
    setContentView(R.layout.activity_main);
  }

  public void onModeSelected(View view) {
    Intent intent = getIntentForModeSelected(view.getId());

    if (intent != null) {
      startActivity(intent);
    }
  }

  private Intent getIntentForModeSelected(int id) {
    if (id == R.id.two_player_discovery_api) {
      return new Intent(this, DiscoveryTwoPlayerActivity.class);
    } else if (id == R.id.two_player_sessions_api) {
      return new Intent(this, SessionsTwoPlayerActivity.class);
    } else if (id == R.id.single_player_sessions_api) {
      return new Intent(this, SessionsSinglePlayerActivity.class);
    }
    return null;
  }
}
