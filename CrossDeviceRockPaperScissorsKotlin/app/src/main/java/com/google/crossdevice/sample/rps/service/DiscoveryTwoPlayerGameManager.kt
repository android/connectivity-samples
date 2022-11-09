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
import com.google.ambient.crossdevice.Participant
import com.google.ambient.crossdevice.connections.ConnectionReceiver
import com.google.ambient.crossdevice.connections.RemoteConnection
import com.google.ambient.crossdevice.discovery.DevicePickerLauncher
import com.google.ambient.crossdevice.discovery.Discovery
import com.google.ambient.crossdevice.wakeup.StartComponentRequest
import com.google.crossdevice.sample.rps.R
import com.google.crossdevice.sample.rps.model.GameChoice
import com.google.crossdevice.sample.rps.model.GameData
import com.google.crossdevice.sample.rps.model.TwoPlayerGameDataViewModel
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Implementation of GameManager using Discovery APIs. */
class DiscoveryTwoPlayerGameManager(
  private val context: Context,
  private val scope: CoroutineScope
) : GameManager {
  override val gameData: TwoPlayerGameDataViewModel =
    TwoPlayerGameDataViewModel.createInstance(context)
  private val discovery = Discovery.create(context)
  private val devicePickerLauncher: DevicePickerLauncher
  private var remotePlayer: RemoteConnection? = null

  init {
    // Register the callback for selected devices. It will provides a list of Participant,
    // available for connections.
    devicePickerLauncher =
      discovery.registerForResult(context as ActivityResultCaller) {
        participants: Collection<Participant> ->
        for (participant in participants) {
          Log.d(TAG, "selected participant=$participant")
          openRemoteConnection(participant)
          break
        }
        if (participants.isEmpty()) {
          resetGame()
        }
      }
    // Ensure data in the View Model is reset
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
    // Launches device picker dialog showing available devices to connect
    scope.launch {
      devicePickerLauncher.launchDevicePicker(
        emptyList(),
        StartComponentRequest.Builder()
          .setAction(ACTION_WAKE_UP)
          .setReason(context.getString(R.string.wakeup_reason))
          .build()
      )
      Log.d(TAG, "Launched device picker")
    }
  }

  /** Sends the local player's game choice to the other remote player. */
  override fun sendGameChoice(choice: GameChoice, callback: GameManager.Callback) {
    gameData.localPlayerChoice = choice
    gameData.gameState.value = GameData.GameState.WAITING_FOR_ROUND_RESULT
    sendPayloadToRemoteConnection(remotePlayer!!, callback)
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

  /** Determines whether the invitation is valid, then opens a connection */
  override fun acceptGameInvitation(intent: Intent) {
    // Extract participant information from the incoming intent.
    val participant = discovery.getParticipantFromIntent(intent)

    participant?.also { acceptRemoteConnection(it) }
      ?: Log.e(TAG, "invalid incoming \$ACTION_WAKE_UP intent")
  }

  /** Returns true if there is a connection, since Discovery based connections are equal peers. */
  override fun isHost(): Boolean {
    return remotePlayer != null
  }

  /** Accept the incoming remote connection explicitly and begins communication. */
  private fun acceptRemoteConnection(participant: Participant) {
    Log.d(TAG, "acceptRemoteConnection() for participant: " + participant.displayName)

    // Registers call back to accept incoming remote connection
    scope.launch {
      val connectionResult = participant.acceptConnection(GAME_CHANNEL_NAME)
      connectionResult.onSuccess { remoteConnection ->
        Log.i(TAG, "onConnectionResult: connection successful")
        updateRemotePlayer(remoteConnection)
        // Ensure data from any previous game is reset
        resetGame()
        gameData.gameState.value = GameData.GameState.WAITING_FOR_PLAYER_INPUT
        gameData.opponentPlayerName.value = participant.displayName.toString()
        receivePayloadFromRemoteConnection(remoteConnection)
      }
      connectionResult.onFailure { throwable ->
        Log.e(TAG, "unable to open receiving connection", throwable)
      }
    }
  }

  /**
   * Asynchronously opens a channel (connection) to a given nearby device. An onSuccess callback
   * provides an instance of [RemoteConnection].
   *
   * @param participant Participant instance to open a channel with.
   */
  private fun openRemoteConnection(participant: Participant) {
    Log.d(TAG, "Opening remote connection with: " + participant.displayName)

    // Opens a remote connection and registers to receive data from participant device.
    scope.launch {
      val connectionResult = participant.openConnection(GAME_CHANNEL_NAME)
      connectionResult.onSuccess { remoteConnection ->
        Log.i(TAG, "onConnectionResult: connection successful")

        // Once there is a successful connection we update remote player information and
        // register to receive payload from them
        gameData.gameState.value = GameData.GameState.WAITING_FOR_PLAYER_INPUT
        gameData.opponentPlayerName.value = participant.displayName.toString()
        remotePlayer = remoteConnection
        receivePayloadFromRemoteConnection(remoteConnection)
      }
      connectionResult.onFailure { throwable ->
        Log.e(TAG, "error opening remote connection", throwable)
        resetGame()
      }
    }
  }

  /** Sends data over a provided [RemoteConnection]. */
  private fun sendPayloadToRemoteConnection(
    remoteConnection: RemoteConnection,
    callback: GameManager.Callback
  ) {
    Log.d(TAG, "sendPayloadToRemoteConnection()")

    // Sends a payload to a remote device
    scope.launch {
      val sendResult =
        remoteConnection.send(gameData.localPlayerChoice!!.name.toByteArray(StandardCharsets.UTF_8))
      sendResult.onSuccess {
        Log.i(TAG, "sendPayloadToRemoteConnection() success")
        gameData.isLocalPlayerChoiceConfirmed = true
        finishRound()
        callback.onSuccess()
      }
      sendResult.onFailure { throwable ->
        Log.e(TAG, "error sending payload", throwable)
        gameData.gameState.value = GameData.GameState.WAITING_FOR_PLAYER_INPUT
        callback.onFailure()
      }
    }
  }

  /** Creates a call back to receive payload data from peer remote connections */
  private fun receivePayloadFromRemoteConnection(remoteConnection: RemoteConnection?) {
    remoteConnection?.also {
      it.registerReceiver(
        object : ConnectionReceiver {
          override fun onMessageReceived(remoteConnection: RemoteConnection, payload: ByteArray) {
            Log.i(TAG, "receivePayloadFromRemoteConnection() success")
            // we set the game choice for player 2
            gameData.opponentPlayerChoice =
              GameChoice.valueOf(String(payload, StandardCharsets.UTF_8))
            finishRound()
          }

          override fun onConnectionClosed(
            remoteConnection: RemoteConnection,
            error: Throwable?,
            reason: String?
          ) {
            Log.i(TAG, "Connection closed. reason=$reason", error)
            remotePlayer = null
            resetGame()
          }
        }
      )
    }
      ?: Log.d(TAG, "receiveRemoteConnectionPayload() called with a null connection")
  }

  private fun updateRemotePlayer(remotePlayer: RemoteConnection) {
    // Close any existing connection
    this.remotePlayer?.let {
      Log.i(TAG, "Disconnecting from previous remote player")
      scope.launch { it.close() }
    }
    this.remotePlayer = remotePlayer
  }

  /** Clears remote connection */
  private fun closeConnections() {
    scope.launch {
      remotePlayer?.close()
      remotePlayer = null
      gameData.gameState.value = GameData.GameState.DISCONNECTED
      gameData.resetGameData()
    }
  }

  companion object {
    private const val TAG = "DiscoveryTPGameManager"
    const val ACTION_WAKE_UP = "com.google.crossdevice.sample.rps.DISCOVERY_TWO_PLAYER_WAKEUP"
    private const val GAME_CHANNEL_NAME = "rock_paper_scissors_channel"
  }
}
