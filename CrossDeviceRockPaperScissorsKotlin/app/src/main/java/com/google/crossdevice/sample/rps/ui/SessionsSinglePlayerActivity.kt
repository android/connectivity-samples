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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.google.ambient.crossdevice.sessions.*
import com.google.ambient.crossdevice.wakeup.StartComponentRequest
import com.google.crossdevice.sample.rps.R
import com.google.crossdevice.sample.rps.model.GameChoice
import com.google.crossdevice.sample.rps.model.GameData
import com.google.crossdevice.sample.rps.model.TransferableGameState
import com.google.crossdevice.sample.rps.service.GameManager
import com.google.crossdevice.sample.rps.service.SinglePlayerGameManager
import kotlinx.coroutines.launch

/**
 * Activity for playing a single-player Rock Paper Scissors game using the Sessions API for
 * transferring the game.
 */
class SessionsSinglePlayerActivity : AppCompatActivity(R.layout.activity_single_player) {
  private var originatingSession: OriginatingSession? = null

  private val originatingSessionStateCallback: OriginatingSessionStateCallback =
    object : OriginatingSessionStateCallback {
      override fun onConnected(sessionId: SessionId) {
        Log.d(TAG, "onConnected() called within OriginatingSessionStateCallback")
        lifecycleScope.launch {
          originatingSession
            ?.getStartupRemoteConnection()
            ?.send(transferableState.getState().toString().toByteArray())
            ?.onSuccess { Log.d(TAG, "Successfully sent initialization message") }
            ?.onFailure { throwable ->
              Log.d(TAG, "Failed to send initialization message", throwable)
            }
        }
      }

      override fun onSessionTransferred(sessionId: SessionId) {
        Log.d(TAG, "Successfully transferred: $sessionId")
        this@SessionsSinglePlayerActivity.sessionId.value = null
        resetGame()
        createSession()
        showTransferSuccess()
      }

      override fun onTransferFailure(sessionId: SessionId, exception: SessionException) {
        Log.d(TAG, "Failed to transfer: $sessionId", exception)
        this@SessionsSinglePlayerActivity.sessionId.value = sessionId
        loadState(transferableState)
        showTransferFailure()
      }
    }

  private val receivingSessionStateCallback: ReceivingSessionStateCallback =
    object : ReceivingSessionStateCallback {
      override fun onTransferFailure(sessionId: SessionId, exception: SessionException) {
        Log.d(TAG, "Failed to receive transfer: $sessionId", exception)
        showTransferFailure()
        createSession()
      }
    }

  private lateinit var sessions: Sessions
  private lateinit var transferGameButton: Button
  private lateinit var rockButton: Button
  private lateinit var paperButton: Button
  private lateinit var scissorsButton: Button
  private lateinit var nameText: TextView
  private lateinit var opponentText: TextView
  private lateinit var statusText: TextView
  private lateinit var scoreText: TextView

  private val sessionId: MutableLiveData<SessionId> = MutableLiveData<SessionId>()
  private val transferableState: TransferableGameState = TransferableGameState()
  private lateinit var gameManager: GameManager

  override fun onCreate(bundle: Bundle?) {
    super.onCreate(bundle)

    transferGameButton = findViewById(R.id.transfer_game)
    rockButton = findViewById(R.id.rock)
    paperButton = findViewById(R.id.paper)
    scissorsButton = findViewById(R.id.scissors)
    nameText = findViewById(R.id.name)
    opponentText = findViewById(R.id.opponent_info)
    statusText = findViewById(R.id.status)
    scoreText = findViewById(R.id.score)

    // Buttons should be disabled until a Session is created
    enableButtons(false)

    // Creates a GameManager object for a Single Player game
    gameManager = SinglePlayerGameManager(this)
    addObservers()
    setupSessions(this)
    handleIntent(intent)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    handleIntent(intent)
  }

  override fun onDestroy() {
    super.onDestroy()
    disconnect()
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
        GameData.GameState.WAITING_FOR_PLAYER_INPUT ->
          if (gameManager.gameData.roundsCompleted == 0) {
            setStatusText(getString(R.string.status_session_created))
          }
        GameData.GameState.WAITING_FOR_ROUND_RESULT -> {}
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

    // Observes changes to the Session
    val sessionIdObserver: Observer<SessionId> =
      Observer<SessionId> { newSessionId: SessionId? -> enableButtons(newSessionId != null) }
    sessionId.observe(this, sessionIdObserver)
  }

  /** Sets up Dtdi components required to run Sessions */
  private fun setupSessions(activity: AppCompatActivity) {
    sessions = Sessions.create(activity)
    sessions.registerActivityResultCaller(activity)
  }

  /**
   * Handles incoming requests to this activity. If the intent contains an incoming transfer, accept
   * it. Otherwise, create a Session if not already created.
   */
  private fun handleIntent(intent: Intent) {
    // Note that we are using launchMode="singleTop" for this activity, as registered in the
    // AndroidManifest.
    Log.d(TAG, "onNewIntent() called with action: " + intent.action)
    if (ACTION_SESSIONS_TRANSFER == intent.action) {
      // This will be the case when the intent that starts this Activity is initiated via
      // Session transfer. Instead of creating a new Session, accept the transfer.
      completeAcceptTransferFlow(intent)
    } else {
      // This means that the Activity was started by some means other than a Session transfer.
      // Thus, this Activity needs to create its own Session.
      createSession()
    }
  }

  /** Sends a [GameChoice] to the GameManager. */
  fun makeMove(view: View) {
    when (view.id) {
      R.id.rock -> setGameChoice(GameChoice.ROCK)
      R.id.paper -> setGameChoice(GameChoice.PAPER)
      R.id.scissors -> setGameChoice(GameChoice.SCISSORS)
    }
  }

  /** Initiates a transfer of the Session to another device. */
  fun transferGame(view: View?) {
    saveState()
    setStatusText(getString(R.string.status_transferring))
    transfer()
  }

  /** Shows a status message to the user. */
  private fun setStatusText(text: String) {
    statusText.text = text
    statusText.contentDescription = text
  }

  /** Sends the user's selection of rock, paper, or scissors to the opponent. */
  private fun setGameChoice(choice: GameChoice) {
    gameManager.sendGameChoice(choice, object : GameManager.Callback() {})
  }

  /** Wipes all game state and updates the UI accordingly. */
  private fun resetGame() {
    gameManager.resetGame()
  }

  private fun enableButtons(enabled: Boolean) {
    rockButton.isEnabled = enabled
    paperButton.isEnabled = enabled
    scissorsButton.isEnabled = enabled
    transferGameButton.isEnabled = enabled
  }

  /**
   * Saves the UI state to something which can be restored later. If errors occur when building the
   * state, nothing is stored.
   */
  private fun saveState() {
    transferableState.gameData = gameManager.gameData.getSerializableState()
    transferableState.statusText = statusText.text.toString()
  }

  /** Parses the UI state for items that it can load. */
  private fun loadState(state: TransferableGameState) {
    gameManager.gameData.loadSerializableState(state.gameData!!)
    setStatusText(state.statusText!!)
  }

  /** Parses the initialization message, then loads state */
  private fun loadStateFromInitMessage(initMessage: ByteArray) {
    transferableState.loadBytes(initMessage)
    loadState(transferableState)
  }

  /** Creates a Session if it does not already own a SessionId. */
  private fun createSession() {
    if (sessionId.value != null) {
      Log.d(TAG, "Session already exists, not creating new Session")
      return
    }

    Log.d(TAG, "Creating a new Session")
    sessionId.value = sessions.createSession()
    Log.d(TAG, "Successfully created: $sessionId")
    gameManager.findOpponent()
  }

  /**
   * Transfers a Session to another device. Results of this method are handled within {@link
   * OriginatingSessionStateCallback}.
   */
  private fun transfer() {
    sessionId.value?.also {
      lifecycleScope.launch {
        originatingSession =
          sessions.transferSession(
            it,
            StartComponentRequest.Builder()
              .setAction(ACTION_SESSIONS_TRANSFER)
              .setReason(getString(R.string.transfer_reason))
              .build(),
            emptyList(),
            originatingSessionStateCallback
          )
      }
    }
      ?: Log.d(TAG, "Skipping transfer since sessionId is null")
  }

  /** Starts a chain of events which eventually lead to a complete transfer. */
  private fun completeAcceptTransferFlow(intent: Intent) {
    lifecycleScope.launch {
      sessions.getReceivingSession(intent, receivingSessionStateCallback).let { receivingSession ->
        receivingSession
          .getStartupRemoteConnection()
          .registerReceiver(
            object : SessionConnectionReceiver {
              override fun onMessageReceived(participant: SessionParticipant, payload: ByteArray) {
                Log.d(TAG, "Success to receive initialization message of size: " + payload.size)
                applicationInitialization(receivingSession, payload)
              }
            }
          )
      }
    }
  }

  /** Initializes the application. */
  private fun applicationInitialization(
    sessionHandle: ReceivingSession,
    initMessage: ByteArray,
  ) {
    lifecycleScope.launch {
      val id = sessionHandle.onComplete()
      Log.d(TAG, "Succeeded to complete receive transfer for: $id")
      // Disconnect from existing Session (if applicable) before accepting transfer
      disconnectFromSession(sessionId.value)

      sessionId.value = id
      loadStateFromInitMessage(initMessage)
      showTransferReceiveSuccess()
    }
  }

  private fun showTransferReceiveSuccess() {
    Toast.makeText(this, getString(R.string.transfer_receive_success), Toast.LENGTH_SHORT).show()
  }

  private fun showTransferSuccess() {
    Toast.makeText(this, getString(R.string.transfer_success), Toast.LENGTH_SHORT).show()
  }

  private fun showTransferFailure() {
    Toast.makeText(this, getString(R.string.transfer_failure), Toast.LENGTH_SHORT).show()
  }

  /** Disconnects from the SessionId held by this Activity. */
  private fun disconnect() {
    lifecycleScope.launch {
      disconnectFromSession(sessionId.value)
      Log.d(TAG, "Successfully disconnected from: $sessionId")
      sessionId.value = null
      gameManager.disconnect()
    }
  }

  /** Disconnects from the provided session. */
  private suspend fun disconnectFromSession(id: SessionId?) {
    id?.also { sessions.removeSession(it) } ?: Log.d(TAG, "Skipping disconnect, sessionId is null")
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
    private const val TAG = "SessionsSinglePlayerActivity"
    private const val ACTION_SESSIONS_TRANSFER =
      "com.google.crossdevice.sample.rps.SESSIONS_TRANSFER"
  }
}
