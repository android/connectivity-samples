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

/** Contains the data needed when transferring a game to another device */
public final class TransferableGameState {
  private static final String TAG = "TransferableGameState";

  private static final String KEY_GAME_DATA = "GAME_DATA";
  private static final String KEY_STATUS_TEXT = "STATUS_TEXT";

  public JSONObject gameData;
  public String statusText;

  public JSONObject getState() {
    JSONObject jsonState = new JSONObject();
    try {
      jsonState.put(KEY_GAME_DATA, gameData);
      jsonState.put(KEY_STATUS_TEXT, statusText);
    } catch (JSONException e) {
      Log.d(TAG, "Failed to get state", e);
    }
    return jsonState;
  }

  public void loadState(JSONObject state) {
    gameData = null;
    statusText = null;
    try {
      if (state.has(KEY_GAME_DATA)) {
        gameData = state.getJSONObject(KEY_GAME_DATA);
      }

      if (state.has(KEY_STATUS_TEXT)) {
        statusText = state.getString(KEY_STATUS_TEXT);
      }
    } catch (JSONException e) {
      Log.d(TAG, "Failed to load state", e);
    }
  }

  public void loadBytes(byte[] bytes) {
    try {
      loadState(new JSONObject(new String(bytes)));
    } catch (JSONException e) {
      Log.d(TAG, "Failed to parse bytes");
    }
  }
}
