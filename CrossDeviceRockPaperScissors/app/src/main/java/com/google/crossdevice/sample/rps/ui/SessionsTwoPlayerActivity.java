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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import com.google.crossdevice.sample.rps.R;
import com.google.crossdevice.sample.rps.model.GameChoice;
import com.google.crossdevice.sample.rps.model.GameData;
import com.google.crossdevice.sample.rps.service.GameManager;
import com.google.crossdevice.sample.rps.service.SessionsTwoPlayerGameManager;
import java.util.Arrays;

/**
 * Activity for playing a two-player Rock Paper Scissors game with opponent using a "Sessions
 * API"-based GameManager.
 */
public class SessionsTwoPlayerActivity extends AppCompatActivity {
  private static final String TAG = "SessionsTwoPlayerActivity";

  private GameManager gameManager;

  private Button findOpponentButton;
  private Button disconnectButton;
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
    setContentView(R.layout.activity_two_player);

    findOpponentButton = findViewById(R.id.find_opponent);
    disconnectButton = findViewById(R.id.disconnect);
    rockButton = findViewById(R.id.rock);
    paperButton = findViewById(R.id.paper);
    scissorsButton = findViewById(R.id.scissors);

    nameText = findViewById(R.id.name);
    opponentText = findViewById(R.id.opponent_info);
    statusText = findViewById(R.id.status);
    scoreText = findViewById(R.id.score);

    gameManager = new SessionsTwoPlayerGameManager(this);

    addObservers();
    handleIntent(getIntent());
  }

  @Override
  protected void onDestroy() {
    // we clean-up and stop all connections
    gameManager.disconnect();
    super.onDestroy();
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    handleIntent(intent);
  }

  /** Handles incoming requests to this activity. */
  private void handleIntent(Intent intent) {
    // Handle incoming requests to this application.
    // Note that we are using launchMode="singleTop" for this activity, as registered in the
    // AndroidManifest.
    Log.d(TAG, "onNewIntent() called with action:" + intent.getAction());
    if (SessionsTwoPlayerGameManager.ACTION_WAKE_UP.equals(intent.getAction())) {
      // Attempt to open up a connection with the participant
      gameManager.acceptGameInvitation(intent);
    }
  }

  /** Initializes discovery of other devices. */
  public void findOpponent(View view) {
    gameManager.findOpponent();
    setStatusText(getString(R.string.status_searching));
    findOpponentButton.setEnabled(false);
  }

  /** Disconnects from the opponent and reset the UI. */
  public void disconnect(View view) {
    gameManager.disconnect();
  }

  /** Sends a {@link GameChoice} to the other player. */
  public void makeMove(View view) {
    if (view.getId() == R.id.rock) {
      sendGameChoice(GameChoice.ROCK);
    } else if (view.getId() == R.id.paper) {
      sendGameChoice(GameChoice.PAPER);
    } else if (view.getId() == R.id.scissors) {
      sendGameChoice(GameChoice.SCISSORS);
    }
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
            case DISCONNECTED:
              setButtonState(false);
              statusText.setText(getString(R.string.status_disconnected));
              updateScore(
                  gameManager.getGameData().getLocalPlayerScore().getValue(),
                  gameManager.getGameData().getOpponentPlayerScore().getValue());
              break;
            case SEARCHING:
              statusText.setText(getString(R.string.status_searching));
              break;
            case WAITING_FOR_PLAYER_INPUT:
              setButtonState(true);
              // Only set show status connected if no rounds have been completed
              if (gameManager.getGameData().getRoundsCompleted() == 0) {
                setStatusText(getString(R.string.status_connected));
              }
              break;
            case WAITING_FOR_ROUND_RESULT:
              setStatusText(
                  getString(
                      R.string.game_choice, gameManager.getGameData().getLocalPlayerChoice()));
              setGameChoicesEnabled(false);
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
  }

  /** Sends the user's selection of rock, paper, or scissors to the opponent. */
  private void sendGameChoice(GameChoice choice) {
    gameManager.sendGameChoice(
        choice,
        new GameManager.Callback() {
          @Override
          public void onFailure() {
            Toast.makeText(
                    SessionsTwoPlayerActivity.this, R.string.send_failure, Toast.LENGTH_SHORT)
                .show();
          }
        });
  }

  /** Enables/disables buttons depending on the connection status. */
  private void setButtonState(boolean connected) {
    findOpponentButton.setEnabled(!connected);
    findOpponentButton.setVisibility(connected ? View.GONE : View.VISIBLE);
    disconnectButton.setVisibility(connected ? View.VISIBLE : View.GONE);
    setGameChoicesEnabled(connected);
  }

  /** Enables/disables the rock, paper, and scissors buttons. */
  private void setGameChoicesEnabled(boolean enabled) {
    rockButton.setEnabled(enabled);
    paperButton.setEnabled(enabled);
    scissorsButton.setEnabled(enabled);
  }

  /** Shows a status message to the user. */
  private void setStatusText(String text) {
    statusText.setText(text);
    statusText.setContentDescription(text);
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
