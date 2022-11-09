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

package com.google.crossdevice.sample.rps.model

import android.util.Log
import org.json.JSONException
import org.json.JSONObject

/** Contains the data needed when sharing a game data with another device */
class ShareableGameState {
  var gameData: JSONObject? = null

  fun setGameData(gameData: JSONObject?): ShareableGameState {
    this.gameData = gameData
    return this
  }

  fun getState(): JSONObject {
    val jsonState = JSONObject()
    try {
      jsonState.put(KEY_GAME_DATA, gameData)
    } catch (e: JSONException) {
      Log.d(TAG, "Failed to get state", e)
    }
    return jsonState
  }

  fun loadState(state: JSONObject) {
    gameData = null
    try {
      if (state.has(KEY_GAME_DATA)) {
        gameData = state.getJSONObject(KEY_GAME_DATA)
      }
    } catch (e: JSONException) {
      Log.d(TAG, "Failed to load state", e)
    }
  }

  fun loadBytes(bytes: ByteArray): ShareableGameState {
    try {
      loadState(JSONObject(String(bytes)))
    } catch (e: JSONException) {
      Log.d(TAG, "Failed to parse bytes")
    }
    return this
  }

  companion object {
    private const val TAG = "ShareableGameState"
    private const val KEY_GAME_DATA = "GAME_DATA"
  }
}
