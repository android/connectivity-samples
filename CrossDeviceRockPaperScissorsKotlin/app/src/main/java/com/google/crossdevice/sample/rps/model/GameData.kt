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

import androidx.lifecycle.MutableLiveData
import org.json.JSONObject

/** Interface for defining game data APIs */
interface GameData {
  /** Enum class used to maintain the state of the game/round */
  enum class GameState {
    DISCONNECTED,
    SEARCHING,
    WAITING_FOR_PLAYER_INPUT,
    WAITING_FOR_ROUND_RESULT,
    ROUND_RESULT
  }

  /** Enum class used to indicate winner of a round */
  enum class RoundWinner {
    PENDING,
    LOCAL_PLAYER,
    OPPONENT,
    TIE
  }

  /** Gets the name of the local player. */
  val localPlayerName: MutableLiveData<String?>

  /** Gets the name of the opponent player. */
  val opponentPlayerName: MutableLiveData<String?>

  /** Gets the GameChoice of the local player. */
  val localPlayerChoice: GameChoice?

  /** Gets the GameChoice of the opponent player. */
  val opponentPlayerChoice: GameChoice?

  /** Gets the score of the local player. */
  val localPlayerScore: MutableLiveData<Int?>

  /** Gets the score of the opponent player. */
  val opponentPlayerScore: MutableLiveData<Int?>

  /** Gets the current status of the game. */
  val gameState: MutableLiveData<GameState?>

  /** Gets the number of opponents in the game. */
  val numberOfOpponents: MutableLiveData<Int?>

  /** Gets the number of opponents in the game. */
  val numberOfOpponentsValue: Int

  /** Gets winner of the most recent round. */
  val roundWinner: RoundWinner?

  /** Gets the number of rounds completed. */
  val roundsCompleted: Int

  /** Gets a serializable object representing the state of the game variables */
  fun getSerializableState(): JSONObject

  /** Loads a serializable object, restoring a specified state of the game. */
  fun loadSerializableState(gameData: JSONObject)
}
