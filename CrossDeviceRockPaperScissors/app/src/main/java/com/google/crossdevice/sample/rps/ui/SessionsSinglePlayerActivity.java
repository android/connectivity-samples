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

package com.google.crossdevice.sample.rps.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import com.google.ambient.crossdevice.sessions.OriginatingSession;
import com.google.ambient.crossdevice.sessions.OriginatingSessionStateCallback;
import com.google.ambient.crossdevice.sessions.ReceivingSession;
import com.google.ambient.crossdevice.sessions.ReceivingSessionStateCallback;
import com.google.ambient.crossdevice.sessions.SessionException;
import com.google.ambient.crossdevice.sessions.SessionId;
import com.google.ambient.crossdevice.sessions.SessionRemoteConnection;
import com.google.ambient.crossdevice.sessions.Sessions;
import com.google.ambient.crossdevice.wakeup.StartComponentRequest;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.crossdevice.sample.rps.R;
import com.google.crossdevice.sample.rps.model.GameChoice;
import com.google.crossdevice.sample.rps.model.GameData;
import com.google.crossdevice.sample.rps.model.TransferableGameState;
import com.google.crossdevice.sample.rps.service.GameManager;
import com.google.crossdevice.sample.rps.service.SinglePlayerGameManager;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.Executor;

/**
 * Activity for playing a single-player Rock Paper Scissors game using the Sessions API for
 * transferring the game.
 */
public class SessionsSinglePlayerActivity extends AppCompatActivity {
  private static final String TAG = "SessionsSinglePlayerActivity";

  public static final String ACTION_SESSIONS_TRANSFER =
      "com.google.crossdevice.sample.rps.SESSIONS_TRANSFER";

  @Nullable private OriginatingSession originatingSession;

  private final OriginatingSessionStateCallback originatingSessionStateCallback =
      new OriginatingSessionStateCallback() {
        @Override
        public void onConnected(@NonNull SessionId sessionId) {
          Log.d(TAG, "onConnected() called within OriginatingSessionStateCallback");
          SessionRemoteConnection connection = originatingSession.getStartupRemoteConnection();
          Futures.addCallback(
              connection.sendFuture(transferableState.getState().toString().getBytes()),
              new FutureCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                  Log.d(TAG, "Successfully sent initialization message");
                }

                @Override
                public void onFailure(@NonNull Throwable t) {
                  Log.d(TAG, "Failed to send initialization message", t);
                }
              },
              mainExecutor);
        }

        @Override
        public void onSessionTransferred(@NonNull SessionId sessionId) {
          Log.d(TAG, "Successfully transferred: " + sessionId);
          resetGame();
          createSession();
          showTransferSuccess();
        }

        @Override
        public void onTransferFailure(@NonNull SessionId sessionId, SessionException exception) {
          Log.d(TAG, "Failed to transfer: " + sessionId, exception);
          SessionsSinglePlayerActivity.this.sessionId.setValue(sessionId);
          loadState(transferableState);
          showTransferFailure();
        }
      };

  private final ReceivingSessionStateCallback receivingSessionStateCallback =
      (sessionId, exception) -> {
        Log.d(TAG, "Failed to transfer: " + sessionId, exception);
        showTransferFailure();
      };

  private final MutableLiveData<SessionId> sessionId = new MutableLiveData<>();
  private final TransferableGameState transferableState = new TransferableGameState();

  private Executor mainExecutor;
  private GameManager gameManager;
  private Sessions sessions;

  private Button transferGameButton;
  private Button rockButton;
  private Button paperButton;
  private Button scissorsButton;

  private TextView nameText;
  private TextView opponentText;
  private TextView statusText;
  private TextView scoreText;

  @Override
  protected void onCreate(@Nullable Bundle bundle) {
    super.onCreate(bundle);
    setContentView(R.layout.activity_single_player);

    transferGameButton = findViewById(R.id.transfer_game);

    rockButton = findViewById(R.id.rock);
    paperButton = findViewById(R.id.paper);
    scissorsButton = findViewById(R.id.scissors);

    nameText = findViewById(R.id.name);
    opponentText = findViewById(R.id.opponent_info);
    statusText = findViewById(R.id.status);
    scoreText = findViewById(R.id.score);

    // Buttons should be disabled until a Session is created
    enableButtons(false);

    // Creates a GameManager object for a Single Player game
    gameManager = new SinglePlayerGameManager(this);

    addObservers();
    setupSessions(this);
    handleIntent(getIntent());
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    handleIntent(intent);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    disconnect();
  }

  /**
   * Creates observers listening to changes to GameData's LiveData fields and updates UI
   * accordingly.
   */
  private void addObservers() {
    // Observes changes to Local Player's name
    final Observer<String> localPlayerNameObserver =
        newName -> nameText.setText(getString(R.string.codename, newName));
    gameManager.getGameData().getLocalPlayerName().observe(this, localPlayerNameObserver);

    // Observes changes to Remote Player's name
    final Observer<String> opponentPlayerNameObserver =
        newName -> {
          opponentText.setText(
              getString(
                  R.string.opponent_name,
                  TextUtils.isEmpty(newName) ? getString(R.string.no_opponent) : newName));
        };
    gameManager.getGameData().getOpponentPlayerName().observe(this, opponentPlayerNameObserver);

    // Observes changes to the Local Player's score
    final Observer<Integer> localPlayerScoreObserver =
        newLocalPlayerScore -> {
          updateScore(
              newLocalPlayerScore, gameManager.getGameData().getOpponentPlayerScore().getValue());
        };
    gameManager.getGameData().getLocalPlayerScore().observe(this, localPlayerScoreObserver);

    // Observes changes to the Opponent Player's score
    final Observer<Integer> opponentPlayerScoreObserver =
        newOpponentPlayerScore -> {
          updateScore(
              gameManager.getGameData().getLocalPlayerScore().getValue(), newOpponentPlayerScore);
        };
    gameManager.getGameData().getOpponentPlayerScore().observe(this, opponentPlayerScoreObserver);

    // Observes game state changes and updates UI accordingly
    final Observer<GameData.GameState> gameStateObserver =
        gameState -> {
          if (!Arrays.asList(GameData.GameState.values()).contains(gameState)) {
            throw new RuntimeException("Invalid GameState passed to Observer");
          }
          switch (gameState) {
            case WAITING_FOR_PLAYER_INPUT:
              if (gameManager.getGameData().getRoundsCompleted() == 0) {
                setStatusText(getString(R.string.status_session_created));
              }
              break;
            case ROUND_RESULT:
              if (!Arrays.asList(GameData.RoundWinner.values())
                  .contains(gameManager.getGameData().getRoundWinner())) {
                throw new RuntimeException("Invalid RoundWinner in RoundResult");
              }
              switch (gameManager.getGameData().getRoundWinner()) {
                case LOCAL_PLAYER:
                  setStatusText(
                      getString(
                          R.string.win_message,
                          gameManager.getGameData().getLocalPlayerChoice(),
                          gameManager.getGameData().getOpponentPlayerChoice()));
                  break;
                case OPPONENT:
                  setStatusText(
                      getString(
                          R.string.loss_message,
                          gameManager.getGameData().getLocalPlayerChoice(),
                          gameManager.getGameData().getOpponentPlayerChoice()));
                  break;
                case TIE:
                  setStatusText(
                      getString(
                          R.string.tie_message, gameManager.getGameData().getLocalPlayerChoice()));
                  break;
                default:
                  Log.d(TAG, "Ignoring RoundWinner: " + gameState);
              }
              break;
            default:
              Log.d(TAG, "Ignoring GameState: " + gameState);
          }
        };
    gameManager.getGameData().getGameState().observe(this, gameStateObserver);

    // Observes changes to the Session
    final Observer<SessionId> sessionIdObserver =
        newSessionId -> {
          enableButtons(newSessionId != null);
        };
    sessionId.observe(this, sessionIdObserver);
  }

  /** Sets up Dtdi components required to run Sessions */
  private void setupSessions(AppCompatActivity activity) {
    sessions = Sessions.create(activity);
    sessions.registerActivityResultCaller(activity);
    mainExecutor = ContextCompat.getMainExecutor(activity);
  }

  /**
   * Handles incoming requests to this activity. If the intent contains an incoming transfer, accept
   * it. Otherwise, create a Session if not already created.
   */
  private void handleIntent(Intent intent) {
    // Note that we are using launchMode="singleTop" for this activity, as registered in the
    // AndroidManifest.
    Log.d(TAG, "onNewIntent() called with action: " + intent.getAction());
    if (ACTION_SESSIONS_TRANSFER.equals(intent.getAction())) {
      // This will be the case when the intent that starts this Activity is initiated via Session
      // transfer. Instead of creating a new Session, accept the transfer.
      startAcceptTransferFlow(intent);
    } else {
      // This means that the Activity was started by some means other than a Session transfer. Thus,
      // this Activity needs to create its own Session.
      createSession();
    }
  }

  /** Sends a {@link GameChoice} to the GameManager. */
  public void makeMove(View view) {
    if (view.getId() == R.id.rock) {
      setGameChoice(GameChoice.ROCK);
    } else if (view.getId() == R.id.paper) {
      setGameChoice(GameChoice.PAPER);
    } else if (view.getId() == R.id.scissors) {
      setGameChoice(GameChoice.SCISSORS);
    }
  }

  /** Initiates a transfer of the Session to another device. */
  public void transferGame(View view) {
    saveState();
    setStatusText(getString(R.string.status_transferring));
    transfer();
  }

  /** Shows a status message to the user. */
  private void setStatusText(String text) {
    statusText.setText(text);
    statusText.setContentDescription(text);
  }

  /** Sends the user's selection of rock, paper, or scissors to the opponent. */
  private void setGameChoice(GameChoice choice) {
    gameManager.sendGameChoice(choice, null);
  }

  /** Wipes all game state and updates the UI accordingly. */
  private void resetGame() {
    gameManager.resetGame();
  }

  private void enableButtons(boolean enabled) {
    rockButton.setEnabled(enabled);
    paperButton.setEnabled(enabled);
    scissorsButton.setEnabled(enabled);
    transferGameButton.setEnabled(enabled);
  }

  /**
   * Saves the UI state to something which can be restored later. If errors occur when building the
   * state, nothing is stored.
   */
  private void saveState() {
    transferableState.gameData = gameManager.getGameData().getSerializableState();
    transferableState.statusText = statusText.getText().toString();
  }

  /** Parses the UI state for items that it can load. */
  private void loadState(TransferableGameState state) {
    gameManager.getGameData().loadSerializableState(state.gameData);
    setStatusText(state.statusText);
  }

  /** Parses the initialization message, then loads state */
  private void loadStateFromInitMessage(byte[] initMessage) {
    transferableState.loadBytes(initMessage);
    loadState(transferableState);
  }

  /** Creates a Session if it does not already own a SessionId. */
  private void createSession() {
    if (sessionId.getValue() != null) {
      Log.d(TAG, "Session already exists, not creating new Session");
      return;
    }

    Log.d(TAG, "Creating a new Session");
    SessionId newSessionId = sessions.createSession(/* applicationSessionTag */ null);
    sessionId.setValue(newSessionId);
    gameManager.findOpponent();
  }

  /**
   * Transfers a Session to another device. Results of this method are handled within {@link
   * OriginatingSessionStateCallback}.
   */
  private void transfer() {
    if (sessionId.getValue() == null) {
      Log.d(TAG, "Skipping transfer since sessionId is null");
      return;
    }

    Futures.addCallback(
        sessions.transferSessionFuture(
            sessionId.getValue(),
            new StartComponentRequest.Builder()
                .setAction(ACTION_SESSIONS_TRANSFER)
                .setReason(getString(R.string.transfer_reason))
                .build(),
            Collections.emptyList(),
            originatingSessionStateCallback),
        new FutureCallback<OriginatingSession>() {
          @Override
          public void onSuccess(OriginatingSession result) {
            // Save the originating session.
            Log.d(TAG, "Successfully received originating session");
            originatingSession = result;
          }

          @Override
          public void onFailure(@NonNull Throwable t) {
            Log.d(TAG, "onFailure called for transferSessionFuture", t);
          }
        },
        mainExecutor);
    sessionId.setValue(null);
  }

  /** Starts a chain of events which eventually lead to a complete transfer. */
  private void startAcceptTransferFlow(Intent intent) {
    getReceivingSessionAndRemoteConnection(intent);
  }

  /** Gets the Receiving Session in order to get the RemoteConnection. */
  private void getReceivingSessionAndRemoteConnection(Intent intent) {
    Futures.addCallback(
        sessions.getReceivingSessionFuture(intent, receivingSessionStateCallback),
        new FutureCallback<ReceivingSession>() {
          @Override
          public void onSuccess(ReceivingSession result) {
            Log.d(TAG, "Succeeded to get TransferrableSessionHandle");
            receiveAndProcessInitializationMessage(result);
          }

          @Override
          public void onFailure(@NonNull Throwable t) {
            Log.d(TAG, "Failed to get TransferrableSessionHandle", t);
          }
        },
        mainExecutor);
  }

  /** Receives the initialization message in order to initialize the application. */
  private void receiveAndProcessInitializationMessage(ReceivingSession receivingSession) {
    receivingSession
        .getStartupRemoteConnection()
        .registerReceiver(
            (participant, payload) -> {
              Log.d(TAG, "Success to receive initialization message of size: " + payload.length);
              applicationInitialization(receivingSession, payload);
            });
  }

  /** Initializes the application. */
  private void applicationInitialization(ReceivingSession sessionHandle, byte[] initMessage) {
    Futures.addCallback(
        sessionHandle.onCompleteFuture(),
        new FutureCallback<SessionId>() {
          @Override
          public void onSuccess(SessionId result) {
            Log.d(TAG, "Succeeded to complete receive transfer for: " + result);
            // Disconnect from existing Session (if applicable) before accepting transfer
            disconnectFromSession(
                SessionsSinglePlayerActivity.this.sessionId.getValue(),
                new FutureCallback<Void>() {
                  @Override
                  public void onSuccess(Void result) {
                    Log.d(TAG, "Succeeded to remove the old session");
                  }

                  @Override
                  public void onFailure(@NonNull Throwable t) {
                    Log.d(TAG, "Failed to remove the old session, which is now orphaned", t);
                  }
                });
            // Update SessionId
            SessionsSinglePlayerActivity.this.sessionId.setValue(result);
            loadStateFromInitMessage(initMessage);
            showTransferReceiveSuccess();
          }

          @Override
          public void onFailure(Throwable t) {
            Log.d(TAG, "Failed to complete receive transfer", t);
          }
        },
        mainExecutor);
  }

  private void showTransferReceiveSuccess() {
    Toast.makeText(this, getString(R.string.transfer_receive_success), Toast.LENGTH_SHORT).show();
  }

  private void showTransferSuccess() {
    Toast.makeText(this, getString(R.string.transfer_success), Toast.LENGTH_SHORT).show();
  }

  private void showTransferFailure() {
    Toast.makeText(this, getString(R.string.transfer_failure), Toast.LENGTH_SHORT).show();
  }

  /** Disconnects from the SessionId held by this Activity. */
  private void disconnect() {
    disconnectFromSession(
        sessionId.getValue(),
        new FutureCallback<Void>() {
          @Override
          public void onSuccess(Void result) {
            Log.d(TAG, "Successfully disconnected from: " + sessionId);
            sessionId.setValue(null);
            gameManager.disconnect();
          }

          @Override
          public void onFailure(@NonNull Throwable t) {
            Log.d(TAG, "Failed to disconnect from: " + sessionId.getValue(), t);
          }
        });
  }

  /** Disconnects from the provided session. */
  private void disconnectFromSession(@Nullable SessionId id, FutureCallback<Void> callback) {
    if (id == null) {
      Log.d(TAG, "Skipping disconnect, sessionId is null");
      return;
    }

    Log.d(TAG, "Disconnecting from: " + id);
    Futures.addCallback(sessions.removeSessionFuture(id), callback, mainExecutor);
  }

  /**
   * Updates the current score based on the latest score data.
   *
   * @param newSelfScore The value for new score of the local player.
   * @param newOpponentPlayerScore The value for new score of the opponent.
   */
  private void updateScore(int newSelfScore, int newOpponentPlayerScore) {
    scoreText.setText(getString(R.string.game_score, newSelfScore, newOpponentPlayerScore));
    scoreText.setContentDescription(
        getString(R.string.game_score_talk_back, newSelfScore, newOpponentPlayerScore));
  }
}
