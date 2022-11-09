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
import android.util.Log
import androidx.activity.result.ActivityResultCaller
import com.google.ambient.crossdevice.sessions.*
import com.google.ambient.crossdevice.wakeup.StartComponentRequest
import com.google.crossdevice.sample.rps.R
import com.google.crossdevice.sample.rps.model.GameChoice
import com.google.crossdevice.sample.rps.model.GameData
import com.google.crossdevice.sample.rps.model.TwoPlayerGameDataViewModel.Companion.createInstance
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Implementation of GameManager using Sessions APIs. */
class SessionsTwoPlayerGameManager(
  private val context: Context,
  private val scope: CoroutineScope
) : GameManager {
  private val primarySessionStateCallback: PrimarySessionStateCallback =
    object : PrimarySessionStateCallback {
      override fun onShareFailureWithParticipant(
        sessionId: SessionId,
        exception: SessionException,
        participant: SessionParticipant
      ) {
        Log.e(TAG, "Share failure with participant: " + participant.displayName, exception)
      }

      override fun onParticipantDeparted(sessionId: SessionId, participant: SessionParticipant) {
        Log.d(TAG, "SessionParticipant departed: " + participant.displayName)
        /* The PrimarySession will only be destroyed if done explicitly. Since the only
         * participant has departed, the PrimarySession should now be destroyed. */
        destroyPrimarySession()
      }

      override fun onParticipantJoined(sessionId: SessionId, participant: SessionParticipant) {
        Log.d(TAG, "New Participant joined: " + participant.displayName)
        gameData.gameState.value = GameData.GameState.WAITING_FOR_PLAYER_INPUT
        gameData.opponentPlayerName.value = participant.displayName.toString()
        primarySession?.also { addRemoteConnectionCallback(it, participant) }
          ?: Log.d(TAG, "Cannot add callback to joined participant since PrimarySession is null")
      }

      override fun onPrimarySessionCleanup(sessionId: SessionId) {
        Log.d(TAG, "PrimarySession cleanup")
        primarySession = null
        resetGame()
      }

      override fun onShareInitiated(sessionId: SessionId, numPotentialParticipants: Int) {
        if (numPotentialParticipants == 0) {
          Log.d(TAG, "No participants joining Session, destroying PrimarySession")
          destroyPrimarySession()
        }
      }

      /** Add a callback to SessionParticipant for handling received messages */
      private fun addRemoteConnectionCallback(
        session: PrimarySession,
        participant: SessionParticipant
      ) {
        session
          .getSecondaryRemoteConnectionForParticipant(participant)
          .registerReceiver(
            object : SessionConnectionReceiver {
              override fun onMessageReceived(participant: SessionParticipant, payload: ByteArray) {
                handleMessageReceived(payload)
              }
            }
          )
      }
    }
  private val secondarySessionStateCallback: SecondarySessionStateCallback =
    object : SecondarySessionStateCallback {
      override fun onSecondarySessionCleanup(sessionId: SessionId) {
        secondarySession = null
        secondaryConnection = null
        resetGame()
      }
    }

  override val gameData = createInstance(context)
  private val sessions: Sessions = Sessions.create(context)
  private var primarySession: PrimarySession? = null
  private var secondarySession: SecondarySession? = null
  private var secondaryConnection: SessionRemoteConnection? = null

  init {
    sessions.registerActivityResultCaller(context as ActivityResultCaller)
    resetGame()
  }

  /** Disconnects from a connected endpoint. */
  override fun disconnect() {
    closeConnections()
  }

  /**
   * Starts an asynchronous device discovery and launches a dialog chooser for available devices.
   */
  override fun findOpponent() {
    gameData.gameState.value = GameData.GameState.SEARCHING
    scope.launch {
      val sessionId = sessions.createSession()
      Log.d(TAG, "Successfully created: $sessionId")

      primarySession =
        sessions.shareSession(
          sessionId,
          StartComponentRequest.Builder()
            .setAction(ACTION_WAKE_UP)
            .setReason(context.getString(R.string.wakeup_reason))
            .build(),
          emptyList(),
          primarySessionStateCallback
        )
      Log.d(TAG, "Successfully launched opponent picker")
    }
  }

  /** Sends the local player's game choice to the other remote player. */
  override fun sendGameChoice(choice: GameChoice, callback: GameManager.Callback) {
    gameData.localPlayerChoice = choice
    gameData.gameState.value = GameData.GameState.WAITING_FOR_ROUND_RESULT
    broadcastGameChoice(callback)
  }

  /** Resets game data to default values. */
  override fun resetGame() {
    gameData.resetGameData()
  }

  /** Finishes and processes the round after all players have entered their choices. */
  override fun finishRound() {
    // process the round and receive the next payload if both players have entered their choices
    if (gameData.opponentPlayerChoice != null && gameData.isLocalPlayerChoiceConfirmed) {
      Log.d(TAG, "Processing round...")
      gameData.processRound()
    }
  }

  /** Starts a flow of events that lead to accepting the game invitation. */
  override fun acceptGameInvitation(intent: Intent) {
    acceptShareSession(intent)
  }

  /** Returns true if a PrimarySession is owned. */
  override fun isHost(): Boolean {
    return primarySession != null
  }

  /** Completes the flow required to accept a shared Session. */
  private fun acceptShareSession(intent: Intent) {
    scope.launch {
      val secondarySession = sessions.getSecondarySession(intent, secondarySessionStateCallback)
      Log.d(TAG, "Got SecondarySession")
      updateSecondarySession(secondarySession)

      secondaryConnection =
        secondarySession.getDefaultRemoteConnection().apply {
          registerReceiver(
            object : SessionConnectionReceiver {
              override fun onMessageReceived(participant: SessionParticipant, payload: ByteArray) {
                handleMessageReceived(payload)
              }
            }
          )
        }

      resetGame()
      gameData.gameState.value = GameData.GameState.WAITING_FOR_PLAYER_INPUT
      gameData.opponentPlayerName.value = secondaryConnection?.participant?.displayName.toString()
    }
  }

  /** Sends the game choice to whichever connection is open. */
  private fun broadcastGameChoice(callback: GameManager.Callback) {
    Log.i(TAG, "Sending game choice")
    val message: ByteArray = gameData.localPlayerChoice!!.name.toByteArray(StandardCharsets.UTF_8)

    scope.launch {
      primarySession?.also {
        it.broadcastToSecondaries(message)
        Log.i(TAG, "Successfully sent game choice")
        gameData.isLocalPlayerChoiceConfirmed = true
        finishRound()
        callback.onSuccess()
      }
        ?: secondaryConnection?.also {
          it
            .send(message)
            .onSuccess {
              Log.i(TAG, "Successfully sent game choice")
              gameData.isLocalPlayerChoiceConfirmed = true
              finishRound()
              callback.onSuccess()
            }
            .onFailure { throwable ->
              Log.e(TAG, "Failed to send game choice", throwable)
              callback.onFailure()
            }
        }
          ?: Log.w(TAG, "There are no open connections to send a message to")
    }
  }

  /** Destroys a SecondarySession. */
  private fun destroySecondarySession(secondarySession: SecondarySession) {
    scope.launch { secondarySession.destroySecondarySession() }
  }

  /** Destroys a PrimarySession. */
  private fun destroyPrimarySession() {
    scope.launch {
      primarySession?.destroyPrimarySessionAndStopSharing()
      Log.i(TAG, "Destroyed primary session handle")
    }
  }

  /**
   * Disconnects from any previous SecondarySession and sets the current to the provided
   * SecondarySession.
   */
  private fun updateSecondarySession(secondarySession: SecondarySession) {
    // Close any existing connection
    this.secondarySession?.let {
      destroySecondarySession(it)
      Log.i(TAG, "Successfully disconnected from the previous SecondarySession")
    }
    this.secondarySession = secondarySession
  }

  /** Closes any open PrimarySession or SecondarySession. */
  private fun closeConnections() {
    primarySession?.let { destroyPrimarySession() }
    secondarySession?.let { destroySecondarySession(it) }
  }

  /** Sets the opponent's choice and attempts to finish the round */
  private fun handleMessageReceived(message: ByteArray) {
    gameData.opponentPlayerChoice = GameChoice.valueOf(String(message, StandardCharsets.UTF_8))
    finishRound()
  }

  companion object {
    private const val TAG = "SessionsTPGameManager"
    const val ACTION_WAKE_UP = "com.google.crossdevice.sample.rps.SESSIONS_TWO_PLAYER_WAKEUP"
  }
}
