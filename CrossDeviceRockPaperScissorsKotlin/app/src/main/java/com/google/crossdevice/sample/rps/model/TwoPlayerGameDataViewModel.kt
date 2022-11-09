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

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import java.util.Random
import org.json.JSONException
import org.json.JSONObject

/**
 * GameData object used to store information about a Game and to communicate those changes to
 * observers in the UI.
 */
class TwoPlayerGameDataViewModel : ViewModel(), GameData {
  /** Stores Local Player's name */
  override val localPlayerName = MutableLiveData<String?>()

  /** Stores Opponent Player's name */
  override val opponentPlayerName = MutableLiveData<String?>()

  /** Stores Local Player's score. */
  override val localPlayerScore = MutableLiveData<Int?>()

  /** Stores Opponent Player's score. */
  override val opponentPlayerScore = MutableLiveData<Int?>()

  /** Stores current Game State */
  override val gameState = MutableLiveData<GameData.GameState?>()

  /** Gets current number of opponents. */
  override val numberOfOpponents = MutableLiveData<Int?>()

  /** Gets current number of opponents. */
  override val numberOfOpponentsValue = 1

  /** Stores Local Player's game choice. */
  override var localPlayerChoice: GameChoice? = null

  /** Stores Opponent Player's game choice. */
  override var opponentPlayerChoice: GameChoice? = null

  /** Stores Local Player's game choice confirmation state. */
  var isLocalPlayerChoiceConfirmed = false

  /** Stores the winner for each round or pending if we are still waiting on results */
  override var roundWinner: GameData.RoundWinner? = null
    private set

  /** Stores current number of rounds completed. */
  override var roundsCompleted = 0
    private set

  private val randomGenerator = Random()

  /** Resets all variables to default values prior to starting a game. */
  fun resetGameData() {
    localPlayerName.value = CodenameGenerator.generate()
    opponentPlayerName.value = null
    localPlayerScore.value = 0
    opponentPlayerScore.value = 0
    localPlayerChoice = null
    opponentPlayerChoice = null
    isLocalPlayerChoiceConfirmed = false
    gameState.value = GameData.GameState.DISCONNECTED
    roundWinner = GameData.RoundWinner.PENDING
    roundsCompleted = 0
  }

  /** Stores the value of Local Player's score. */
  private val localPlayerScoreValue: Int
    get() = localPlayerScore.value ?: 0

  /** Stores the value of Opponent Player's score. */
  private val opponentPlayerScoreValue: Int
    get() = opponentPlayerScore.value ?: 0

  /** Processes the round to determine who wins and auto-increment the winner's score. */
  fun processRound() {
    roundWinner =
      if (localPlayerChoice!!.beats(opponentPlayerChoice!!)) {
        incrementLocalPlayerScore()
        GameData.RoundWinner.LOCAL_PLAYER
      } else if (opponentPlayerChoice!!.beats(localPlayerChoice!!)) {
        incrementOpponentPlayerScore()
        GameData.RoundWinner.OPPONENT
      } else {
        GameData.RoundWinner.TIE
      }
    gameState.value = GameData.GameState.ROUND_RESULT
    roundsCompleted++
    resetRound()
  }

  /** Reset's player choices and game state after each round. */
  private fun resetRound() {
    localPlayerChoice = null
    opponentPlayerChoice = null
    isLocalPlayerChoiceConfirmed = false
    roundWinner = GameData.RoundWinner.PENDING
    gameState.value = GameData.GameState.WAITING_FOR_PLAYER_INPUT
  }

  fun setRandomOpponentChoice() {
    opponentPlayerChoice = getRandomGameChoice()
  }

  private fun getRandomGameChoice() =
    GameChoice.values()[randomGenerator.nextInt(GameChoice.values().size)]

  private fun incrementLocalPlayerScore() {
    localPlayerScore.value = localPlayerScoreValue + 1
  }

  private fun incrementOpponentPlayerScore() {
    opponentPlayerScore.value = opponentPlayerScoreValue + 1
  }

  override fun getSerializableState(): JSONObject {
    val jsonGameData = JSONObject()
    try {
      jsonGameData
        .put(KEY_LOCAL_PLAYER_NAME, localPlayerName.value)
        .put(KEY_OPPONENT_PLAYER_NAME, opponentPlayerName.value)
        .put(KEY_LOCAL_PLAYER_SCORE, localPlayerScoreValue)
        .put(KEY_OPPONENT_PLAYER_SCORE, opponentPlayerScoreValue)
        .put(KEY_ROUNDS_COMPLETED, roundsCompleted)
    } catch (e: JSONException) {
      Log.d(TAG, "Failed to get state", e)
    }
    return jsonGameData
  }

  override fun loadSerializableState(gameData: JSONObject) {
    try {
      val localPlayerName = gameData.getString(KEY_LOCAL_PLAYER_NAME)
      val opponentPlayerName = gameData.getString(KEY_OPPONENT_PLAYER_NAME)
      val localPlayerScore = gameData.getInt(KEY_LOCAL_PLAYER_SCORE)
      val opponentPlayerScore = gameData.getInt(KEY_OPPONENT_PLAYER_SCORE)
      val roundsCompleted = gameData.getInt(KEY_ROUNDS_COMPLETED)

      // Assign values to instance
      this.localPlayerName.value = localPlayerName
      this.opponentPlayerName.value = opponentPlayerName
      this.localPlayerScore.value = localPlayerScore
      this.opponentPlayerScore.value = opponentPlayerScore
      this.roundsCompleted = roundsCompleted
    } catch (e: JSONException) {
      Log.d(TAG, "Failed to load game data", e)
    }
  }

  companion object {
    private const val TAG = "TwoPlayerGameDataViewModel"
    private const val KEY_LOCAL_PLAYER_NAME = "LOCAL_PLAYER_NAME"
    private const val KEY_OPPONENT_PLAYER_NAME = "OPPONENT_PLAYER_NAME"
    private const val KEY_LOCAL_PLAYER_SCORE = "LOCAL_PLAYER_SCORE"
    private const val KEY_OPPONENT_PLAYER_SCORE = "OPPONENT_PLAYER_SCORE"
    private const val KEY_ROUNDS_COMPLETED = "ROUNDS_COMPLETED"
    fun createInstance(context: Context?): TwoPlayerGameDataViewModel {
      return ViewModelProvider((context as ViewModelStoreOwner?)!!)[
        TwoPlayerGameDataViewModel::class.java]
    }
  }
}
