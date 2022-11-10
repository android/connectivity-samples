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

package com.google.crossdevice.sample.rps.model;

import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

/** Contains the data needed when sharing a game data with another device */
public final class ShareableGameState {
  private static final String TAG = "ShareableGameState";

  private static final String KEY_GAME_DATA = "GAME_DATA";

  public JSONObject gameData;

  public ShareableGameState setGameData(JSONObject gameData) {
    this.gameData = gameData;
    return this;
  }

  public JSONObject getState() {
    JSONObject jsonState = new JSONObject();
    try {
      jsonState.put(KEY_GAME_DATA, gameData);
    } catch (JSONException e) {
      Log.d(TAG, "Failed to get state", e);
    }
    return jsonState;
  }

  public void loadState(JSONObject state) {
    gameData = null;
    try {
      if (state.has(KEY_GAME_DATA)) {
        gameData = state.getJSONObject(KEY_GAME_DATA);
      }
    } catch (JSONException e) {
      Log.d(TAG, "Failed to load state", e);
    }
  }

  public ShareableGameState loadBytes(byte[] bytes) {
    try {
      loadState(new JSONObject(new String(bytes)));
    } catch (JSONException e) {
      Log.d(TAG, "Failed to parse bytes");
    }
    return this;
  }
}
