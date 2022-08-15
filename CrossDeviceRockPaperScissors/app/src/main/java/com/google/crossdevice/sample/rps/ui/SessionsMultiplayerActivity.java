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
import com.google.crossdevice.sample.rps.service.SessionsMultiplayerGameManager;

/**
 * Activity for playing a multiplayer Rock Paper Scissors game with opponents using a "Sessions
 * API"-based GameManager.
 */
public class SessionsMultiplayerActivity extends AppCompatActivity {
    private static final String TAG = "SessionsMultiplayerActivity";

    private Button addOpponentButton;
    private Button disconnectButton;
    private Button rockButton;
    private Button paperButton;
    private Button scissorsButton;

    private TextView nameText;
    private TextView participantsText;
    private TextView statusText;
    private TextView localScoreText;
    private TextView topScoreText;

    private GameManager gameManager;

    @Override
    protected void onCreate(@Nullable Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_multiplayer);

        addOpponentButton = findViewById(R.id.find_opponent);
        disconnectButton = findViewById(R.id.disconnect);
        rockButton = findViewById(R.id.rock);
        paperButton = findViewById(R.id.paper);
        scissorsButton = findViewById(R.id.scissors);

        nameText = findViewById(R.id.name);
        participantsText = findViewById(R.id.opponent_info);
        statusText = findViewById(R.id.status);
        localScoreText = findViewById(R.id.score_local);
        topScoreText = findViewById(R.id.score_top);

        gameManager = new SessionsMultiplayerGameManager(this);

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
        if (SessionsMultiplayerGameManager.ACTION_WAKE_UP.equals(intent.getAction())) {
            // Attempt to open up a connection with the participant
            gameManager.acceptGameInvitation(intent);
        }
    }

    /** Initializes discovery of other devices. */
    public void addOpponent(View view) {
        gameManager.findOpponent();
        setStatusText(getString(R.string.status_searching));
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

        // Observes changes to Opponent Player's name
        final Observer<String> opponentPlayerNameObserver =
                newName -> {
                    topScoreText.setText(
                            getString(
                                    R.string.game_score_labeled,
                                    getString(R.string.top_score_text)
                                            + (TextUtils.isEmpty(newName) ? getString(R.string.no_opponent) : newName),
                                    gameManager.getGameData().getOpponentPlayerScore().getValue()));
                };
        gameManager.getGameData().getOpponentPlayerName().observe(this, opponentPlayerNameObserver);

        // Observes changes to the Local Player's score
        final Observer<Integer> localPlayerScoreObserver =
                newLocalPlayerScore -> {
                    localScoreText.setText(
                            getString(
                                    R.string.game_score_labeled,
                                    getString(R.string.your_score_text),
                                    newLocalPlayerScore));
                };
        gameManager.getGameData().getLocalPlayerScore().observe(this, localPlayerScoreObserver);

        // Observes changes to the Opponent Player's score
        final Observer<Integer> opponentPlayerScoreObserver =
                newOpponentPlayerScore -> {
                    String opponentName = gameManager.getGameData().getOpponentPlayerName().getValue();
                    topScoreText.setText(
                            getString(
                                    R.string.game_score_labeled,
                                    getString(R.string.top_score_text)
                                            + (TextUtils.isEmpty(opponentName)
                                            ? getString(R.string.no_opponent)
                                            : opponentName),
                                    newOpponentPlayerScore));
                };
        gameManager.getGameData().getOpponentPlayerScore().observe(this, opponentPlayerScoreObserver);

        // Observes game state changes and updates UI accordingly
        final Observer<GameData.GameState> gameStateObserver =
                gameState -> {
                    switch (gameState) {
                        case DISCONNECTED:
                            setButtonStateDisconnected();
                            statusText.setText(getString(R.string.status_disconnected));
                            break;
                        case SEARCHING:
                            statusText.setText(getString(R.string.status_searching));
                            break;
                        case WAITING_FOR_PLAYER_INPUT:
                            setButtonStateAsHost(gameManager.isHost());
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
                            setStatusText(getString(R.string.status_round_complete));
                    }
                };
        gameManager.getGameData().getGameState().observe(this, gameStateObserver);

        // Observes changes to the Opponent Player's score
        final Observer<Integer> numberOfOpponentsObserver =
                newNumberOfPlayers -> {
                    participantsText.setText(getString(R.string.num_opponents, newNumberOfPlayers));
                };
        gameManager.getGameData().getNumberOfOpponents().observe(this, numberOfOpponentsObserver);
    }

    /** Sends the user's selection of rock, paper, or scissors to the opponent. */
    private void sendGameChoice(GameChoice choice) {
        gameManager.sendGameChoice(
                choice,
                new GameManager.Callback() {
                    @Override
                    public void onFailure() {
                        Toast.makeText(
                                SessionsMultiplayerActivity.this, R.string.send_failure, Toast.LENGTH_SHORT)
                                .show();
                    }
                });
    }

    /** Enables/disables buttons depending on the connection status. */
    private void setButtonStateDisconnected() {
        addOpponentButton.setEnabled(true);
        addOpponentButton.setVisibility(View.VISIBLE);
        disconnectButton.setVisibility(View.GONE);
        setGameChoicesEnabled(false);
    }

    private void setButtonStateAsHost(boolean isHost) {
        addOpponentButton.setEnabled(isHost);
        addOpponentButton.setVisibility(isHost ? View.VISIBLE : View.GONE);
        disconnectButton.setVisibility(View.VISIBLE);
        disconnectButton.setText(isHost ? "End Game" : "Leave Game");
        setGameChoicesEnabled(true);
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
    }
}
