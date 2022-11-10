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

package com.google.crossdevice.sample.rps.service

import android.content.Context
import android.content.Intent
import com.google.crossdevice.sample.rps.model.CodenameGenerator.generate
import com.google.crossdevice.sample.rps.model.GameChoice
import com.google.crossdevice.sample.rps.model.GameData
import com.google.crossdevice.sample.rps.model.TwoPlayerGameDataViewModel.Companion.createInstance

/** Implementation of GameManager using DTDI APIs. */
class SinglePlayerGameManager(context: Context?) : GameManager {
  override val gameData = createInstance(context)

  init {
    resetGame()
  }

  /** Generates the opponent's name and starts waiting for player input. */
  override fun findOpponent() {
    generateOpponentName()
    gameData.gameState.value = GameData.GameState.WAITING_FOR_PLAYER_INPUT
  }

  /** Sets the player's choice and finishes the round */
  override fun sendGameChoice(choice: GameChoice, callback: GameManager.Callback) {
    gameData.localPlayerChoice = choice
    finishRound()
  }

  override fun disconnect() {
    gameData.gameState.value = GameData.GameState.DISCONNECTED
  }

  override fun acceptGameInvitation(intent: Intent) {
    // do nothing
  }

  /** Always returns true, since Single Player mode is always its own host. */
  override fun isHost(): Boolean {
    return true
  }

  /** Resets game data to default values. */
  override fun resetGame() {
    gameData.resetGameData()
  }

  /** Sets the opponent's choice and processes the round. */
  override fun finishRound() {
    generateOpponentChoice()
    gameData.processRound()
  }

  /** Generates a choice for the opponent. */
  private fun generateOpponentChoice() {
    gameData.setRandomOpponentChoice()
  }

  /** Generates a name player and opponent. */
  private fun generateOpponentName() {
    gameData.opponentPlayerName.value = generate()
  }

  companion object {
    private const val TAG = "SessionsSPGameManager"
  }
}
