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

package com.google.crossdevice.sample.rps.ui

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.google.crossdevice.sample.rps.R
import com.google.crossdevice.sample.rps.model.GameChoice
import com.google.crossdevice.sample.rps.model.GameData
import com.google.crossdevice.sample.rps.service.GameManager
import com.google.crossdevice.sample.rps.service.SessionsTwoPlayerGameManager

/**
 * Activity for playing a two-player Rock Paper Scissors game with opponent using a "Sessions
 * API"-based GameManager.
 */
class SessionsTwoPlayerActivity : AppCompatActivity(R.layout.activity_two_player) {
  private lateinit var findOpponentButton: Button
  private lateinit var disconnectButton: Button
  private lateinit var rockButton: Button
  private lateinit var paperButton: Button
  private lateinit var scissorsButton: Button
  private lateinit var nameText: TextView
  private lateinit var opponentText: TextView
  private lateinit var statusText: TextView
  private lateinit var scoreText: TextView

  private lateinit var gameManager: GameManager

  override fun onCreate(bundle: Bundle?) {
    super.onCreate(bundle)

    findOpponentButton = findViewById(R.id.find_opponent)
    disconnectButton = findViewById(R.id.disconnect)
    rockButton = findViewById(R.id.rock)
    paperButton = findViewById(R.id.paper)
    scissorsButton = findViewById(R.id.scissors)
    nameText = findViewById(R.id.name)
    opponentText = findViewById(R.id.opponent_info)
    statusText = findViewById(R.id.status)
    scoreText = findViewById(R.id.score)

    gameManager = SessionsTwoPlayerGameManager(this, lifecycleScope)

    addObservers()
    handleIntent(intent)
  }

  override fun onDestroy() {
    // we clean-up and stop all connections
    gameManager.disconnect()
    super.onDestroy()
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    handleIntent(intent)
  }

  /** Handles incoming requests to this activity. */
  private fun handleIntent(intent: Intent) {
    // Handle incoming requests to this application.
    // Note that we are using launchMode="singleTop" for this activity, as registered in the
    // AndroidManifest.
    Log.d(TAG, "onNewIntent() called with action:" + intent.action)
    if (SessionsTwoPlayerGameManager.ACTION_WAKE_UP == intent.action) {
      // Attempt to open up a connection with the participant
      gameManager.acceptGameInvitation(intent)
    }
  }

  /** Initializes discovery of other devices. */
  fun findOpponent(view: View?) {
    gameManager.findOpponent()
    setStatusText(getString(R.string.status_searching))
    findOpponentButton.isEnabled = false
  }

  /** Disconnects from the opponent and reset the UI. */
  fun disconnect(view: View?) {
    gameManager.disconnect()
  }

  /** Sends a [GameChoice] to the other player. */
  fun makeMove(view: View) {
    when (view.id) {
      R.id.rock -> sendGameChoice(GameChoice.ROCK)
      R.id.paper -> sendGameChoice(GameChoice.PAPER)
      R.id.scissors -> sendGameChoice(GameChoice.SCISSORS)
    }
  }

  /**
   * Creates observers listening to changes to GameData's LiveData fields and updates UI
   * accordingly.
   */
  private fun addObservers() {
    // Observes changes to Local Player's name
    val localPlayerNameObserver = Observer { newName: String? ->
      nameText.text = getString(R.string.codename, newName)
    }
    gameManager.gameData.localPlayerName.observe(this, localPlayerNameObserver)

    // Observes changes to Remote Player's name
    val opponentPlayerNameObserver = Observer { newName: String? ->
      opponentText.text =
        getString(
          R.string.opponent_name,
          if (TextUtils.isEmpty(newName)) getString(R.string.no_opponent) else newName
        )
    }
    gameManager.gameData.opponentPlayerName.observe(this, opponentPlayerNameObserver)

    // Observes changes to the Local Player's score
    val localPlayerScoreObserver = Observer { newLocalPlayerScore: Int? ->
      updateScore(newLocalPlayerScore, gameManager.gameData.opponentPlayerScore.value)
    }
    gameManager.gameData.localPlayerScore.observe(this, localPlayerScoreObserver)

    // Observes changes to the Opponent Player's score
    val opponentPlayerScoreObserver = Observer { newOpponentPlayerScore: Int? ->
      updateScore(gameManager.gameData.localPlayerScore.value, newOpponentPlayerScore)
    }
    gameManager.gameData.opponentPlayerScore.observe(this, opponentPlayerScoreObserver)

    // Observes game state changes and updates UI accordingly
    val gameStateObserver = Observer { gameState: GameData.GameState? ->
      if (!GameData.GameState.values().contains(gameState)) {
        throw RuntimeException("Invalid GameState passed to Observer")
      }
      when (gameState) {
        GameData.GameState.DISCONNECTED -> {
          setButtonState(false)
          setStatusText(getString(R.string.status_disconnected))
          updateScore(
            gameManager.gameData.localPlayerScore.value,
            gameManager.gameData.opponentPlayerScore.value
          )
        }
        GameData.GameState.SEARCHING -> setStatusText(getString(R.string.status_searching))
        GameData.GameState.WAITING_FOR_PLAYER_INPUT -> {
          setButtonState(true)
          // Only set show status connected if no rounds have been completed
          if (gameManager.gameData.roundsCompleted == 0) {
            setStatusText(getString(R.string.status_connected))
          }
        }
        GameData.GameState.WAITING_FOR_ROUND_RESULT -> {
          setStatusText(getString(R.string.game_choice, gameManager.gameData.localPlayerChoice))
          setGameChoicesEnabled(false)
        }
        GameData.GameState.ROUND_RESULT ->
          gameManager.gameData.roundWinner.let { winner ->
            if (!GameData.RoundWinner.values().contains(winner)) {
              throw RuntimeException("Invalid RoundWinner in RoundResult")
            }
            when (winner) {
              GameData.RoundWinner.LOCAL_PLAYER ->
                setStatusText(
                  getString(
                    R.string.win_message,
                    gameManager.gameData.localPlayerChoice,
                    gameManager.gameData.opponentPlayerChoice
                  )
                )
              GameData.RoundWinner.OPPONENT ->
                setStatusText(
                  getString(
                    R.string.loss_message,
                    gameManager.gameData.localPlayerChoice,
                    gameManager.gameData.opponentPlayerChoice
                  )
                )
              GameData.RoundWinner.TIE ->
                setStatusText(
                  getString(R.string.tie_message, gameManager.gameData.localPlayerChoice)
                )
              else -> Log.d(TAG, "Ignoring RoundWinner: $winner")
            }
          }
        else -> Log.d(TAG, "Ignoring GameState: $gameState")
      }
    }
    gameManager.gameData.gameState.observe(this, gameStateObserver)
  }

  /** Sends the user's selection of rock, paper, or scissors to the opponent. */
  private fun sendGameChoice(choice: GameChoice) {
    gameManager.sendGameChoice(
      choice,
      object : GameManager.Callback() {
        override fun onFailure() {
          Toast.makeText(this@SessionsTwoPlayerActivity, R.string.send_failure, Toast.LENGTH_SHORT)
            .show()
        }
      }
    )
  }

  /** Enables/disables buttons depending on the connection status. */
  private fun setButtonState(connected: Boolean) {
    findOpponentButton.isEnabled = !connected
    findOpponentButton.visibility = if (connected) View.GONE else View.VISIBLE
    disconnectButton.visibility = if (connected) View.VISIBLE else View.GONE
    setGameChoicesEnabled(connected)
  }

  /** Enables/disables the rock, paper, and scissors buttons. */
  private fun setGameChoicesEnabled(enabled: Boolean) {
    rockButton.isEnabled = enabled
    paperButton.isEnabled = enabled
    scissorsButton.isEnabled = enabled
  }

  /** Shows a status message to the user. */
  private fun setStatusText(text: String) {
    statusText.text = text
    statusText.contentDescription = text
  }

  /**
   * Updates the current score based on the latest score data.
   *
   * @param newSelfScore The value for new score of the local player.
   * @param newOpponentPlayerScore The value for new score of the opponent.
   */
  private fun updateScore(newSelfScore: Int?, newOpponentPlayerScore: Int?) {
    if (newSelfScore == null || newOpponentPlayerScore === null) {
      return
    }

    scoreText.text = getString(R.string.game_score, newSelfScore, newOpponentPlayerScore)
    scoreText.contentDescription =
      getString(R.string.game_score_talk_back, newSelfScore, newOpponentPlayerScore)
  }

  companion object {
    private const val TAG = "SessionsTwoPlayerActivity"
  }
}
