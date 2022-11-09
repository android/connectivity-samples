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

package com.google.crossdevice.sample.rps.service;

import android.content.Intent;
import androidx.annotation.Nullable;
import com.google.crossdevice.sample.rps.model.GameChoice;
import com.google.crossdevice.sample.rps.model.GameData;

/** Interface for defining GameManager APIs */
public interface GameManager {
  /** Gets the managed GameData object. */
  GameData getGameData();

  /** Disconnects from connection endpoint when connected to a session. */
  void disconnect();

  /** Initializes discovery of other devices in order to find an opponent. */
  void findOpponent();

  /**
   * Sends the local player's choice for a given round (i.e. rock, paper or scissor) to a remote
   * participant.
   */
  void sendGameChoice(GameChoice choice, @Nullable Callback callback);

  /** Resets the game to default values. */
  void resetGame();

  /** Processes the round after all players have made their move. */
  void finishRound();

  /** Accepts incoming invitation from a remote participant */
  void acceptGameInvitation(Intent intent);

  /** Returns whether the GameManager is the game host or not */
  boolean isHost();

  /** Can be passed to methods in GameManager when notification of success or failure is desired. */
  abstract class Callback {
    /** Called when the desired operation succeeds. */
    public void onSuccess() {}

    /** Called when the desired operation fails. */
    public void onFailure() {}
  }
}
