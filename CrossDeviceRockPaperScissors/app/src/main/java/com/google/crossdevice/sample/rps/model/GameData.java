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

import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import org.json.JSONObject;

/** Interface for defining game data APIs */
public interface GameData {
  /** Enum class used to maintain the state of the game/round */
  enum GameState {
    DISCONNECTED,
    SEARCHING,
    WAITING_FOR_PLAYER_INPUT,
    WAITING_FOR_ROUND_RESULT,
    ROUND_RESULT
  }

  /** Enum class used to indicate winner of a round */
  enum RoundWinner {
    PENDING,
    LOCAL_PLAYER,
    OPPONENT,
    TIE
  }

  /** Gets the name of the local player. */
  MutableLiveData<String> getLocalPlayerName();

  /** Gets the name of the opponent player. */
  MutableLiveData<String> getOpponentPlayerName();

  /** Gets the GameChoice of the local player. */
  @Nullable
  GameChoice getLocalPlayerChoice();

  /** Gets the GameChoice of the opponent player. */
  @Nullable
  GameChoice getOpponentPlayerChoice();

  /** Gets the score of the local player. */
  MutableLiveData<Integer> getLocalPlayerScore();

  /** Gets the score of the opponent player. */
  MutableLiveData<Integer> getOpponentPlayerScore();

  /** Gets the current status of the game. */
  MutableLiveData<GameState> getGameState();

  /** Gets the number of opponents in the game. */
  MutableLiveData<Integer> getNumberOfOpponents();

  /** Gets winner of the most recent round. */
  RoundWinner getRoundWinner();

  /** Gets the number of rounds completed. */
  int getRoundsCompleted();

  /** Gets a serializable object representing the state of the game variables */
  JSONObject getSerializableState();

  /** Loads a serializable object, restoring a specified state of the game. */
  void loadSerializableState(JSONObject gameData);
}
