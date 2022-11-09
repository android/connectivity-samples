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

import android.content.Context;
import android.content.Intent;
import com.google.crossdevice.sample.rps.model.CodenameGenerator;
import com.google.crossdevice.sample.rps.model.GameChoice;
import com.google.crossdevice.sample.rps.model.GameData;
import com.google.crossdevice.sample.rps.model.TwoPlayerGameDataViewModel;

/** Implementation of GameManager using Cross device APIs. */
public final class SinglePlayerGameManager implements GameManager {
  private static final String TAG = "SessionsSPGameManager";

  private final TwoPlayerGameDataViewModel gameData;

  public SinglePlayerGameManager(Context context) {
    gameData = TwoPlayerGameDataViewModel.createInstance(context);
    resetGame();
  }

  /** Generates the opponent's name and starts waiting for player input. */
  @Override
  public void findOpponent() {
    generateOpponentName();
    gameData.getGameState().setValue(GameData.GameState.WAITING_FOR_PLAYER_INPUT);
  }

  /** Sets the player's choice and finishes the round */
  @Override
  public void sendGameChoice(GameChoice choice, Callback callback) {
    gameData.setLocalPlayerChoice(choice);
    finishRound();
  }

  @Override
  public void disconnect() {
    gameData.getGameState().setValue(GameData.GameState.DISCONNECTED);
  }

  @Override
  public void acceptGameInvitation(Intent intent) {
    // do nothing
  }

  /** Always returns true, since Single Player mode is always its own host. */
  @Override
  public boolean isHost() {
    return true;
  }

  /** Resets game data to default values. */
  @Override
  public void resetGame() {
    gameData.resetGameData();
  }

  /** Sets the opponent's choice and processes the round. */
  @Override
  public void finishRound() {
    generateOpponentChoice();
    gameData.processRound();
  }

  /** Generates a choice for the opponent. */
  private void generateOpponentChoice() {
    gameData.setRandomOpponentChoice();
  }

  /** Generates a name player and opponent. */
  private void generateOpponentName() {
    gameData.getOpponentPlayerName().setValue(CodenameGenerator.generate());
  }

  /** Getter for the managed GameDataViewModel object. */
  public TwoPlayerGameDataViewModel getGameData() {
    return this.gameData;
  }
}
