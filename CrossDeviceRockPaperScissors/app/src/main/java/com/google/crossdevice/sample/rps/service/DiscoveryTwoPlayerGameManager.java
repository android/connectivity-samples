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

import static java.nio.charset.StandardCharsets.UTF_8;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.activity.result.ActivityResultCaller;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.google.ambient.crossdevice.Participant;
import com.google.ambient.crossdevice.connections.ConnectionReceiver;
import com.google.ambient.crossdevice.connections.RemoteConnection;
import com.google.ambient.crossdevice.discovery.DevicePickerLauncher;
import com.google.ambient.crossdevice.discovery.Discovery;
import com.google.ambient.crossdevice.wakeup.StartComponentRequest;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.crossdevice.sample.rps.R;
import com.google.crossdevice.sample.rps.model.GameChoice;
import com.google.crossdevice.sample.rps.model.GameData;
import com.google.crossdevice.sample.rps.model.TwoPlayerGameDataViewModel;
import java.util.concurrent.Executor;

/** Implementation of GameManager using Discovery APIs. */
public final class DiscoveryTwoPlayerGameManager implements GameManager {
  private static final String TAG = "DiscoveryTPGameManager";
  public static final String ACTION_WAKE_UP =
      "com.google.crossdevice.sample.rps.DISCOVERY_TWO_PLAYER_WAKEUP";
  private static final String GAME_CHANNEL_NAME = "rock_paper_scissors_channel";

  private final Context context;
  private final Executor mainExecutor;
  private final TwoPlayerGameDataViewModel gameData;
  private final Discovery discovery;
  private final DevicePickerLauncher devicePickerLauncher;

  @Nullable private RemoteConnection remotePlayer;

  public DiscoveryTwoPlayerGameManager(Context context) {
    this.context = context;
    gameData = TwoPlayerGameDataViewModel.createInstance(context);
    discovery = Discovery.create(context);
    mainExecutor = ContextCompat.getMainExecutor(context);

    // Register the callback for selected devices. It will provides a list of Participant,
    // available for connections.
    devicePickerLauncher =
        discovery.registerForResult(
            (ActivityResultCaller) context,
            participants -> {
              for (Participant participant : participants) {
                Log.d(TAG, "selected participant=" + participant);
                openRemoteConnection(participant);
                break;
              }
              if (participants.isEmpty()) {
                resetGame();
              }
            });
    // Ensure data in the View Model is reset
    resetGame();
  }

  /** Getter for the managed GameDataViewModel object. */
  @Override
  public TwoPlayerGameDataViewModel getGameData() {
    return this.gameData;
  }

  /** Disconnects from a connected endpoint. */
  @Override
  public void disconnect() {
    closeConnections();
  }

  /**
   * Starts an asynchronous device discovery and launches a dialog chooser for available devices.
   */
  @Override
  public void findOpponent() {
    gameData.getGameState().setValue(GameData.GameState.SEARCHING);
    // Launches device picker dialog showing available devices to connect
    Futures.addCallback(
        devicePickerLauncher.launchDevicePickerFuture(
            ImmutableList.of(),
            new StartComponentRequest.Builder()
                .setAction(ACTION_WAKE_UP)
                .setReason(context.getString(R.string.wakeup_reason))
                .build()),
        new FutureCallback<Void>() {
          @Override
          public void onSuccess(Void result) {
            Log.d(TAG, "onSuccess launchFuture");
          }

          @Override
          public void onFailure(@NonNull Throwable t) {
            Log.e(TAG, "Discovery failed: error getting devices ", t);
            resetGame();
          }
        },
        mainExecutor);
  }

  /** Sends the local player's game choice to the other remote player. */
  @Override
  public void sendGameChoice(GameChoice choice, @Nullable Callback callback) {
    gameData.setLocalPlayerChoice(choice);
    gameData.getGameState().setValue(GameData.GameState.WAITING_FOR_ROUND_RESULT);
    sendPayloadToRemoteConnection(remotePlayer, callback);
  }

  /** Resets game data to default values. */
  @Override
  public void resetGame() {
    gameData.resetGameData();
  }

  /** Finishes and processes the round after all players have entered their choices. */
  @Override
  public void finishRound() {
    // process the round and receive the next payload if both players have entered their choices
    if (gameData.getOpponentPlayerChoice() != null && gameData.isLocalPlayerChoiceConfirmed()) {
      Log.d(TAG, "Processing round...");
      gameData.processRound();
    }
  }

  /** Determines whether the invitation is valid, then opens a connection */
  @Override
  public void acceptGameInvitation(Intent intent) {
    // Extract participant information from the incoming intent.
    Participant participant = discovery.getParticipantFromIntent(intent);
    if (participant == null) {
      Log.e(TAG, "invalid incoming $ACTION_WAKE_UP intent");
      return;
    }

    acceptRemoteConnection(participant);
  }

  /** Returns true if there is a connection, since Discovery based connections are equal peers. */
  @Override
  public boolean isHost() {
    return remotePlayer != null;
  }

  /** Accept the incoming remote connection explicitly and begins communication. */
  private void acceptRemoteConnection(Participant participant) {
    Log.d(TAG, "acceptRemoteConnection() for participant: " + participant.getDisplayName());

    // Registers call back to accept incoming remote connection
    Futures.addCallback(
        participant.acceptConnectionFuture(GAME_CHANNEL_NAME),
        new FutureCallback<RemoteConnection>() {
          @Override
          public void onSuccess(RemoteConnection result) {
            Log.i(TAG, "onConnectionResult: connection successful");
            updateRemotePlayer(result);
            // Ensure data from any previous game is reset
            resetGame();
            gameData.getGameState().setValue(GameData.GameState.WAITING_FOR_PLAYER_INPUT);
            gameData.getOpponentPlayerName().setValue(participant.getDisplayName().toString());
            receivePayloadFromRemoteConnection(result);
          }

          @Override
          public void onFailure(@NonNull Throwable t) {
            Log.e(TAG, "unable to open receiving connection", t);
          }
        },
        mainExecutor);
  }

  /**
   * Asynchronously opens a channel (connection) to a given nearby device. An onSuccess callback
   * provides an instance of {@link RemoteConnection}.
   *
   * @param participant Participant instance to open a channel with.
   */
  private void openRemoteConnection(Participant participant) {
    Log.d(TAG, "Opening remote connection with: " + participant.getDisplayName());

    // Opens a remote connection and registers to receive data from participant device.
    Futures.addCallback(
        participant.openConnectionFuture(GAME_CHANNEL_NAME),
        new FutureCallback<RemoteConnection>() {
          @Override
          public void onSuccess(RemoteConnection remoteConnection) {
            Log.i(TAG, "onConnectionResult: connection successful");

            // Once there is a successful connection update remote player information
            // and register to receive payload from them
            gameData.getGameState().setValue(GameData.GameState.WAITING_FOR_PLAYER_INPUT);
            gameData.getOpponentPlayerName().setValue(participant.getDisplayName().toString());
            remotePlayer = remoteConnection;
            receivePayloadFromRemoteConnection(remoteConnection);
          }

          @Override
          public void onFailure(@NonNull Throwable t) {
            Log.e(TAG, "error opening remote connection", t);
            resetGame();
          }
        },
        mainExecutor);
  }

  /** Sends data over a provided {@link RemoteConnection}. */
  private void sendPayloadToRemoteConnection(
      @Nullable RemoteConnection remoteConnection, @Nullable Callback callback) {
    if (remoteConnection == null) {
      Log.d(TAG, "sendPayloadToRemoteConnection() called with a null connection");
      return;
    }

    Log.d(TAG, "sendPayloadToRemoteConnection()");

    // Sends a payload to a remote device
    Futures.addCallback(
        remoteConnection.sendFuture(gameData.getLocalPlayerChoice().name().getBytes(UTF_8)),
        new FutureCallback<Void>() {
          @Override
          public void onSuccess(Void result) {
            Log.i(TAG, "sendPayloadToRemoteConnection() success");
            gameData.setLocalPlayerChoiceConfirmed(true);
            finishRound();
            if (callback != null) {
              callback.onSuccess();
            }
          }

          @Override
          public void onFailure(@NonNull Throwable t) {
            Log.e(TAG, "error sending payload", t);
            gameData.getGameState().setValue(GameData.GameState.WAITING_FOR_PLAYER_INPUT);
            if (callback != null) {
              callback.onFailure();
            }
          }
        },
        mainExecutor);
  }

  /** Closes the given remote connection. */
  private void closeRemoteConnection(
      @Nullable RemoteConnection remoteConnection, FutureCallback<Void> callback) {
    if (remoteConnection != null) {
      Futures.addCallback(remoteConnection.closeFuture(), callback, mainExecutor);
    }
  }

  /** Creates a call back to receive payload data from peer remote connections */
  private void receivePayloadFromRemoteConnection(@Nullable RemoteConnection remoteConnection) {
    if (remoteConnection == null) {
      Log.d(TAG, "receiveRemoteConnectionPayload() called with a null connection");
      return;
    }

    Log.d(TAG, "receiveRemoteConnectionPayload()");

    // Receives payloads from a remote device
    remoteConnection.registerReceiver(
        new ConnectionReceiver() {
          @Override
          public void onMessageReceived(
              @NonNull RemoteConnection connection, @NonNull byte[] payload) {
            Log.i(TAG, "receivePayloadFromRemoteConnection() success");
            // we set the game choice for player 2
            gameData.setOpponentPlayerChoice(GameChoice.valueOf(new String(payload, UTF_8)));
            finishRound();
          }

          @Override
          public void onConnectionClosed(
              @NonNull RemoteConnection connection,
              @Nullable Throwable error,
              @Nullable String reason) {
            Log.i(TAG, "Connection closed. reason=" + reason, error);
            remotePlayer = null;
            resetGame();
          }
        });
  }

  private void updateRemotePlayer(RemoteConnection remotePlayer) {
    // Close any existing connection
    if (this.remotePlayer != null) {
      Log.i(TAG, "Disconnecting from previous remote player");
      closeRemoteConnection(
          this.remotePlayer,
          new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
              // Do nothing
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
              Log.e(TAG, "Error closing previous remote connection, which is now orphaned", t);
            }
          });
    }
    this.remotePlayer = remotePlayer;
  }

  /** Clears remote connection */
  private void closeConnections() {
    if (remotePlayer == null) {
      // Connection already closed
      return;
    }

    closeRemoteConnection(
        remotePlayer,
        new FutureCallback<Void>() {
          @Override
          public void onSuccess(Void result) {
            remotePlayer = null;
            gameData.getGameState().setValue(GameData.GameState.DISCONNECTED);
            gameData.resetGameData();
          }

          @Override
          public void onFailure(@NonNull Throwable t) {
            Log.e(TAG, "Error closing remote connection", t);
          }
        });
  }
}
