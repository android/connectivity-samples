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

/** Contains the data needed when transferring a game to another device */
class TransferableGameState {
  var gameData: JSONObject? = null
  var statusText: String? = null

  fun getState(): JSONObject {
    val jsonState = JSONObject()
    try {
      jsonState.put(KEY_GAME_DATA, gameData)
      jsonState.put(KEY_STATUS_TEXT, statusText)
    } catch (e: JSONException) {
      Log.d(TAG, "Failed to get state", e)
    }
    return jsonState
  }

  fun loadState(state: JSONObject) {
    gameData = null
    statusText = null
    try {
      if (state.has(KEY_GAME_DATA)) {
        gameData = state.getJSONObject(KEY_GAME_DATA)
      }
      if (state.has(KEY_STATUS_TEXT)) {
        statusText = state.getString(KEY_STATUS_TEXT)
      }
    } catch (e: JSONException) {
      Log.d(TAG, "Failed to load state", e)
    }
  }

  fun loadBytes(bytes: ByteArray?) {
    try {
      loadState(JSONObject(String(bytes!!)))
    } catch (e: JSONException) {
      Log.d(TAG, "Failed to parse bytes")
    }
  }

  companion object {
    private const val TAG = "TransferableGameState"
    private const val KEY_GAME_DATA = "GAME_DATA"
    private const val KEY_STATUS_TEXT = "STATUS_TEXT"
  }
}
