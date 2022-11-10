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
import com.google.ambient.crossdevice.sessions.PrimarySession;
import com.google.ambient.crossdevice.sessions.PrimarySessionStateCallback;
import com.google.ambient.crossdevice.sessions.SecondarySession;
import com.google.ambient.crossdevice.sessions.SecondarySessionStateCallback;
import com.google.ambient.crossdevice.sessions.SessionException;
import com.google.ambient.crossdevice.sessions.SessionId;
import com.google.ambient.crossdevice.sessions.SessionParticipant;
import com.google.ambient.crossdevice.sessions.SessionRemoteConnection;
import com.google.ambient.crossdevice.sessions.Sessions;
import com.google.ambient.crossdevice.wakeup.StartComponentRequest;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.crossdevice.sample.rps.R;
import com.google.crossdevice.sample.rps.model.GameChoice;
import com.google.crossdevice.sample.rps.model.GameData;
import com.google.crossdevice.sample.rps.model.TwoPlayerGameDataViewModel;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.Executor;

/** Implementation of GameManager using Sessions APIs. */
public final class SessionsTwoPlayerGameManager implements GameManager {
  private static final String TAG = "SessionsTPGameManager";
  public static final String ACTION_WAKE_UP =
      "com.google.crossdevice.sample.rps.SESSIONS_TWO_PLAYER_WAKEUP";

  private final PrimarySessionStateCallback primarySessionStateCallback =
      new PrimarySessionStateCallback() {
        @Override
        public void onShareFailureWithParticipant(
            @NonNull SessionId sessionId,
            @NonNull SessionException exception,
            SessionParticipant participant) {
          Log.e(TAG, "Share failure with participant: " + participant.getDisplayName(), exception);
        }

        @Override
        public void onParticipantDeparted(
            @NonNull SessionId sessionId, SessionParticipant participant) {
          Log.d(TAG, "SessionParticipant departed: " + participant.getDisplayName());
          /* The PrimarySession will only be destroyed if done explicitly. Since the only
           * participant has departed, the PrimarySession should now be destroyed. */
          destroyPrimarySession();
        }

        @Override
        public void onParticipantJoined(
            @NonNull SessionId sessionId, SessionParticipant participant) {
          Log.d(TAG, "New Participant joined: " + participant.getDisplayName());
          gameData.getGameState().setValue(GameData.GameState.WAITING_FOR_PLAYER_INPUT);
          gameData.getOpponentPlayerName().setValue(participant.getDisplayName().toString());
          if (primarySession == null) {
            Log.d(TAG, "Cannot add callback to joined participant since PrimarySession is null");
            return;
          }
          addRemoteConnectionCallback(primarySession, participant);
        }

        @Override
        public void onPrimarySessionCleanup(@NonNull SessionId sessionId) {
          Log.d(TAG, "PrimarySession cleanup");
          primarySession = null;
          resetGame();
        }

        @Override
        public void onShareInitiated(@NonNull SessionId sessionId, int numPotentialParticipants) {
          if (numPotentialParticipants == 0) {
            Log.d(TAG, "No participants joining Session, destroying PrimarySession");
            destroyPrimarySession();
          }
        }

        /** Add a callback to SessionParticipant for handling received messages */
        private void addRemoteConnectionCallback(
            PrimarySession session, SessionParticipant participant) {
          session
              .getSecondaryRemoteConnectionForParticipant(participant)
              .registerReceiver((participant1, payload) -> handleMessageReceived(payload));
        }
      };

  private final SecondarySessionStateCallback secondarySessionStateCallback =
      new SecondarySessionStateCallback() {
        @Override
        public void onSecondarySessionCleanup(@NonNull SessionId sessionId) {
          secondarySession = null;
          secondaryConnection = null;
          resetGame();
        }
      };

  private final Context context;
  private final Executor mainExecutor;
  private final TwoPlayerGameDataViewModel gameData;
  private final Sessions sessions;

  @Nullable private PrimarySession primarySession;
  @Nullable private SecondarySession secondarySession;
  @Nullable private SessionRemoteConnection secondaryConnection;

  public SessionsTwoPlayerGameManager(Context context) {
    this.context = context;
    gameData = TwoPlayerGameDataViewModel.createInstance(context);
    sessions = Sessions.create(context);
    mainExecutor = ContextCompat.getMainExecutor(context);

    sessions.registerActivityResultCaller((ActivityResultCaller) context);

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

    SessionId sessionId = sessions.createSession(/* applicationSessionTag */ null);
    Futures.addCallback(
        sessions.shareSessionFuture(
            sessionId,
            new StartComponentRequest.Builder()
                .setAction(ACTION_WAKE_UP)
                .setReason(context.getString(R.string.wakeup_reason))
                .build(),
            Collections.emptyList(),
            primarySessionStateCallback),
        new FutureCallback<PrimarySession>() {
          @Override
          public void onSuccess(PrimarySession result) {
            Log.d(TAG, "Successfully launched opponent picker");
            primarySession = result;
          }

          @Override
          public void onFailure(@NonNull Throwable t) {
            Log.e(TAG, "Failed to launch opponent picker", t);
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
    broadcastGameChoice(callback);
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

  /** Starts a flow of events that lead to accepting the game invitation. */
  @Override
  public void acceptGameInvitation(Intent intent) {
    getSecondarySessionAndRemoteConnection(intent);
  }

  /** Returns true if a PrimarySession is owned. */
  @Override
  public boolean isHost() {
    return primarySession != null;
  }

  /** Gets the SecondarySession and uses it to get the RemoteConnection. */
  private void getSecondarySessionAndRemoteConnection(Intent intent) {
    Futures.addCallback(
        sessions.getSecondarySessionFuture(intent, secondarySessionStateCallback),
        new FutureCallback<SecondarySession>() {
          @Override
          public void onSuccess(SecondarySession result) {
            Log.d(TAG, "Succeeded to get SecondarySession");
            updateSecondarySession(result);
            getRemoteConnectionAndRegisterReceiver(result);
          }

          @Override
          public void onFailure(@NonNull Throwable t) {
            Log.e(TAG, "Failed to get SessionHandle", t);
          }
        },
        mainExecutor);
  }

  /** Gets the RemoteConnection and registers a message receiver to it. */
  private void getRemoteConnectionAndRegisterReceiver(SecondarySession sessionHandle) {
    secondaryConnection = sessionHandle.getDefaultRemoteConnection();
    secondaryConnection.registerReceiver((participant, payload) -> handleMessageReceived(payload));
    resetGame();
    gameData.getGameState().setValue(GameData.GameState.WAITING_FOR_PLAYER_INPUT);
    gameData
        .getOpponentPlayerName()
        .setValue(secondaryConnection.getParticipant().getDisplayName().toString());
  }

  /** Sends the game choice to whichever connection is open. */
  private void broadcastGameChoice(@Nullable Callback callback) {
    Log.i(TAG, "Sending game choice");

    ListenableFuture<Void> sendMessageFuture = getSendMessageFuture();

    if (sendMessageFuture == null) {
      Log.w(TAG, "There are no open connections to send a message to");
      return;
    }

    Futures.addCallback(
        sendMessageFuture,
        new FutureCallback<Void>() {
          @Override
          public void onSuccess(Void result) {
            Log.i(TAG, "Successfully sent game choice");
            gameData.setLocalPlayerChoiceConfirmed(true);
            finishRound();
            if (callback != null) {
              callback.onSuccess();
            }
          }

          @Override
          public void onFailure(@NonNull Throwable t) {
            Log.e(TAG, "Failed to send game choice", t);
            if (callback != null) {
              callback.onFailure();
            }
          }
        },
        mainExecutor);
  }

  private ListenableFuture<Void> getSendMessageFuture() {
    byte[] message = gameData.getLocalPlayerChoice().name().getBytes(StandardCharsets.UTF_8);

    if (primarySession != null) {
      return primarySession.broadcastToSecondariesFuture(message);
    } else if (secondaryConnection != null) {
      return secondaryConnection.sendFuture(message);
    }
    return null;
  }

  /** Destroys a SecondarySession. */
  private void destroySecondarySession(
      SecondarySession secondarySession, FutureCallback<Void> callback) {
    Futures.addCallback(secondarySession.destroySecondarySessionFuture(), callback, mainExecutor);
  }

  /** Destroys a PrimarySession. */
  private void destroyPrimarySession() {
    if (primarySession == null) {
      // Already destroyed
      return;
    }

    Futures.addCallback(
        primarySession.destroyPrimarySessionAndStopSharingFuture(),
        new FutureCallback<Void>() {
          @Override
          public void onSuccess(Void result) {
            Log.i(TAG, "Destroyed primary session handle");
          }

          @Override
          public void onFailure(@NonNull Throwable t) {
            Log.e(TAG, "Failed to destroy primary session handle", t);
          }
        },
        mainExecutor);
  }

  /**
   * Disconnects from any previous SecondarySession and sets the current to the provided
   * SecondarySession.
   */
  private void updateSecondarySession(SecondarySession secondarySession) {
    // Close any existing connection
    if (this.secondarySession != null) {
      destroySecondarySession(
          this.secondarySession,
          new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
              Log.i(TAG, "Successfully disconnected from the previous SecondarySession");
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
              Log.e(TAG, "Error destroying previous secondary session, which is now orphaned", t);
            }
          });
    }
    this.secondarySession = secondarySession;
  }

  /** Closes any open PrimarySession or SecondarySession. */
  private void closeConnections() {
    destroyPrimarySession();

    if (secondarySession != null) {
      destroySecondarySession(
          secondarySession,
          new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
              Log.i(TAG, "Successfully destroyed SecondarySession");
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
              Log.e(TAG, "Failed to destroy secondary session handle", t);
            }
          });
    }
  }

  /** Sets the opponent's choice and attempts to finish the round */
  private void handleMessageReceived(byte[] message) {
    gameData.setOpponentPlayerChoice(GameChoice.valueOf(new String(message, UTF_8)));
    finishRound();
  }
}
