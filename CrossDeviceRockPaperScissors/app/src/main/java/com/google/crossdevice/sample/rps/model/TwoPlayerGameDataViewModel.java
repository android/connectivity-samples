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

package com.google.crossdevice.sample.rps.model;

import android.content.Context;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import java.util.Random;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * GameData object used to store information about a Game and to communicate those changes to
 * observers in the UI.
 */
public final class TwoPlayerGameDataViewModel extends ViewModel implements GameData {
  private static final String TAG = "TwoPlayerGameDataViewModel";

  private static final String KEY_LOCAL_PLAYER_NAME = "LOCAL_PLAYER_NAME";
  private static final String KEY_OPPONENT_PLAYER_NAME = "OPPONENT_PLAYER_NAME";
  private static final String KEY_LOCAL_PLAYER_SCORE = "LOCAL_PLAYER_SCORE";
  private static final String KEY_OPPONENT_PLAYER_SCORE = "OPPONENT_PLAYER_SCORE";
  private static final String KEY_ROUNDS_COMPLETED = "ROUNDS_COMPLETED";

  private final MutableLiveData<String> localPlayerName = new MutableLiveData<>();
  private final MutableLiveData<String> opponentPlayerName = new MutableLiveData<>();
  private final MutableLiveData<Integer> localPlayerScore = new MutableLiveData<>();
  private final MutableLiveData<Integer> opponentPlayerScore = new MutableLiveData<>();
  private final MutableLiveData<GameState> gameState =
      new MutableLiveData<>(GameState.DISCONNECTED);
  @Nullable private GameChoice localPlayerChoice;
  @Nullable private GameChoice opponentPlayerChoice;
  private boolean localPlayerChoiceConfirmed;
  private RoundWinner roundWinner = RoundWinner.PENDING;
  private int roundsCompleted;
  private final Random randomGenerator = new Random();

  public static TwoPlayerGameDataViewModel createInstance(Context context) {
    return new ViewModelProvider((ViewModelStoreOwner) context)
        .get(TwoPlayerGameDataViewModel.class);
  }

  /** Resets all variables to default values prior to starting a game. */
  public void resetGameData() {
    localPlayerName.setValue(CodenameGenerator.generate());
    opponentPlayerName.setValue(null);
    localPlayerScore.setValue(0);
    opponentPlayerScore.setValue(0);
    localPlayerChoice = null;
    opponentPlayerChoice = null;
    localPlayerChoiceConfirmed = false;
    gameState.setValue(GameState.DISCONNECTED);
    roundWinner = RoundWinner.PENDING;
    roundsCompleted = 0;
  }

  /** Retrieves Local Player's name */
  @Override
  public MutableLiveData<String> getLocalPlayerName() {
    return localPlayerName;
  }

  /** Retrieves Opponent Player's name */
  @Override
  public MutableLiveData<String> getOpponentPlayerName() {
    return opponentPlayerName;
  }

  /** Retrieves Local Player's game choice. */
  @Override
  public @Nullable GameChoice getLocalPlayerChoice() {
    return localPlayerChoice;
  }

  /** Sets Local Player's game choice. */
  public void setLocalPlayerChoice(@Nullable GameChoice choice) {
    this.localPlayerChoice = choice;
  }

  /** Retrieves Opponent Player's game choice. */
  @Override
  public @Nullable GameChoice getOpponentPlayerChoice() {
    return opponentPlayerChoice;
  }

  /** Sets Opponent Player's game choice. */
  public void setOpponentPlayerChoice(@Nullable GameChoice choice) {
    this.opponentPlayerChoice = choice;
  }

  /** Gets Local Player's game choice confirmation state. */
  public boolean isLocalPlayerChoiceConfirmed() {
    return localPlayerChoiceConfirmed;
  }

  /** Sets Local Player's game choice confirmation state. */
  public void setLocalPlayerChoiceConfirmed(boolean confirmed) {
    this.localPlayerChoiceConfirmed = confirmed;
  }

  /** Gets Local Player's score. */
  @Override
  public MutableLiveData<Integer> getLocalPlayerScore() {
    return localPlayerScore;
  }

  /** Gets Local Player's score, accounting for null case. */
  public int getLocalPlayerScoreValue() {
    Integer value = localPlayerScore.getValue();
    return value == null ? 0 : value;
  }

  /** Gets Opponent Player's score. */
  @Override
  public MutableLiveData<Integer> getOpponentPlayerScore() {
    return opponentPlayerScore;
  }

  /** Gets Opponent Player's score, accounting for null case. */
  public int getOpponentPlayerScoreValue() {
    Integer value = opponentPlayerScore.getValue();
    return value == null ? 0 : value;
  }

  /** Gets current Game State */
  @Override
  public MutableLiveData<GameState> getGameState() {
    return gameState;
  }

  @Override
  public MutableLiveData<Integer> getNumberOfOpponents() {
    return null;
  }

  /** Returns the winner for each round or pending if we are still waiting on results */
  @Override
  public RoundWinner getRoundWinner() {
    return roundWinner;
  }

  /** Gets current number of rounds completed. */
  @Override
  public int getRoundsCompleted() {
    return roundsCompleted;
  }

  /** Processes the round to determine who wins and auto-increment the winner's score. */
  public void processRound() {
    assert localPlayerChoice != null;
    assert opponentPlayerChoice != null;

    if (localPlayerChoice.beats(opponentPlayerChoice)) {
      incrementLocalPlayerScore();
      roundWinner = RoundWinner.LOCAL_PLAYER;
    } else if (opponentPlayerChoice.beats(localPlayerChoice)) {
      incrementOpponentPlayerScore();
      roundWinner = RoundWinner.OPPONENT;
    } else {
      roundWinner = RoundWinner.TIE;
    }
    gameState.setValue(GameState.ROUND_RESULT);
    roundsCompleted++;
    resetRound();
  }

  /** Reset's player choices and game state after each round. */
  private void resetRound() {
    localPlayerChoice = null;
    opponentPlayerChoice = null;
    localPlayerChoiceConfirmed = false;
    roundWinner = RoundWinner.PENDING;
    gameState.setValue(GameState.WAITING_FOR_PLAYER_INPUT);
  }

  public void setRandomOpponentChoice() {
    opponentPlayerChoice = getRandomGameChoice();
  }

  private GameChoice getRandomGameChoice() {
    return GameChoice.values()[randomGenerator.nextInt(GameChoice.values().length)];
  }

  private void incrementLocalPlayerScore() {
    localPlayerScore.setValue(getLocalPlayerScoreValue() + 1);
  }

  private void incrementOpponentPlayerScore() {
    opponentPlayerScore.setValue(getOpponentPlayerScoreValue() + 1);
  }

  @Override
  public JSONObject getSerializableState() {
    JSONObject jsonGameData = new JSONObject();
    try {
      jsonGameData
          .put(KEY_LOCAL_PLAYER_NAME, localPlayerName.getValue())
          .put(KEY_OPPONENT_PLAYER_NAME, opponentPlayerName.getValue())
          .put(KEY_LOCAL_PLAYER_SCORE, localPlayerScore.getValue())
          .put(KEY_OPPONENT_PLAYER_SCORE, opponentPlayerScore.getValue())
          .put(KEY_ROUNDS_COMPLETED, roundsCompleted);
    } catch (JSONException e) {
      Log.d(TAG, "Failed to get state", e);
    }

    return jsonGameData;
  }

  @Override
  public void loadSerializableState(JSONObject gameData) {
    try {
      String localPlayerName = gameData.getString(KEY_LOCAL_PLAYER_NAME);
      String opponentPlayerName = gameData.getString(KEY_OPPONENT_PLAYER_NAME);
      int localPlayerScore = gameData.getInt(KEY_LOCAL_PLAYER_SCORE);
      int opponentPlayerScore = gameData.getInt(KEY_OPPONENT_PLAYER_SCORE);
      int roundsCompleted = gameData.getInt(KEY_ROUNDS_COMPLETED);

      // Assign values to instance
      this.localPlayerName.setValue(localPlayerName);
      this.opponentPlayerName.setValue(opponentPlayerName);
      this.localPlayerScore.setValue(localPlayerScore);
      this.opponentPlayerScore.setValue(opponentPlayerScore);
      this.roundsCompleted = roundsCompleted;
    } catch (JSONException e) {
      Log.d(TAG, "Failed to load game data", e);
    }
  }
}
