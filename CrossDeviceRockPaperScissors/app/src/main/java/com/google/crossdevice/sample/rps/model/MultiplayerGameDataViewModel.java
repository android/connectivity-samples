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

import androidx.annotation.GuardedBy;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.google.ambient.crossdevice.sessions.SessionParticipant;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * GameData object used to store information about a Multiplayer Game and to communicate those
 * changes to observers in the UI and the multiplayer GameManager.
 */
public final class MultiplayerGameDataViewModel extends ViewModel implements GameData {

    private static final String TAG = "MultiplayerGameDataViewModel";
    private static final String DEFAULT_LOCAL_NAME = "Host";

    private static final String KEY_TOP_PLAYER_NAME = "TOP_PLAYER_NAME";
    private static final String KEY_TOP_PLAYER_SCORE = "TOP_PLAYER_SCORE";
    private static final String KEY_LOCAL_PLAYER_SCORE = "LOCAL_PLAYER_SCORE";
    private static final String KEY_NUMBER_OF_OPPONENTS = "NUMBER_OF_OPPONENTS";

    private final Object participantRecordMapLock = new Object();

    @GuardedBy("participantRecordMapLock")
    private final Map<SessionParticipant, ParticipantRecord> participantRecordMap = new HashMap<>();

    private final MutableLiveData<String> localPlayerName = new MutableLiveData<>();
    private final MutableLiveData<String> topPlayerName = new MutableLiveData<>();
    private final MutableLiveData<Integer> localPlayerScore = new MutableLiveData<>();
    private final MutableLiveData<Integer> topPlayerScore = new MutableLiveData<>();
    private final MutableLiveData<GameState> gameState = new MutableLiveData<>();
    private final MutableLiveData<Integer> numberOfOpponents = new MutableLiveData<>();
    private GameChoice localPlayerChoice;
    private GameChoice topPlayerChoice;
    private int roundsCompleted;

    public static MultiplayerGameDataViewModel createInstance(Context context) {
        return new ViewModelProvider((ViewModelStoreOwner) context)
                .get(MultiplayerGameDataViewModel.class);
    }

    /** Resets all variables to default values prior to starting a game. */
    public void resetGameData() {
        synchronized (participantRecordMapLock) {
            participantRecordMap.clear();
        }
        localPlayerName.setValue(CodenameGenerator.generate());
        topPlayerName.setValue(null);
        localPlayerScore.setValue(0);
        topPlayerScore.setValue(0);
        numberOfOpponents.setValue(0);
        localPlayerChoice = null;
        topPlayerChoice = null;
        gameState.setValue(GameData.GameState.DISCONNECTED);
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
        return topPlayerName;
    }

    /** Retrieves Local Player's game choice. */
    @Override
    public GameChoice getLocalPlayerChoice() {
        return localPlayerChoice;
    }

    /** Sets Local Player's game choice. */
    public void setLocalPlayerChoice(GameChoice choice) {
        this.localPlayerChoice = choice;
    }

    /** Retrieves Opponent Player's game choice. */
    @Override
    public GameChoice getOpponentPlayerChoice() {
        return topPlayerChoice;
    }

    /** Sets Opponent Player's game choice. */
    public void setOpponentPlayerChoice(GameChoice choice) {
        this.topPlayerChoice = choice;
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
        return topPlayerScore;
    }

    /** Gets Opponent Player's score, accounting for null case. */
    public int getOpponentPlayerScoreValue() {
        Integer value = topPlayerScore.getValue();
        return value == null ? 0 : value;
    }

    /** Gets current Game State. */
    @Override
    public MutableLiveData<GameState> getGameState() {
        return gameState;
    }

    /** Gets current number of opponents. */
    @Override
    public MutableLiveData<Integer> getNumberOfOpponents() {
        return numberOfOpponents;
    }

    public int getNumberOfOpponentsValue() {
        Integer opponents = numberOfOpponents.getValue();
        return opponents == null ? 0 : opponents;
    }

    /** Returns the winner for each round or pending if we are still waiting on results. */
    @Override
    public RoundWinner getRoundWinner() {
        return null;
    }

    /** Gets current number of rounds completed. */
    @Override
    public int getRoundsCompleted() {
        return roundsCompleted;
    }

    /** Processes the round, updates each player's score, and returns the states of each player. */
    public Map<SessionParticipant, JSONObject> processRoundAndGetParticipantStates() {
        synchronized (participantRecordMapLock) {
            processRound();
        }
        return getParticipantStates();
    }

    /** Computes new scores for each player and finishes the round. */
    @GuardedBy("participantRecordMapLock")
    private void processRound() {
        Map<GameChoice, Integer> scoreChangeForGameChoice = getScoreChangeForGameChoice(participantRecordMap);

        localPlayerScore.setValue(
                getLocalPlayerScoreValue() + safeUnbox(scoreChangeForGameChoice.get(localPlayerChoice)));

        int maxScore = getLocalPlayerScoreValue();
        String maxParticipantName = DEFAULT_LOCAL_NAME;

        for (ParticipantRecord record : participantRecordMap.values()) {
            record.score += safeUnbox(scoreChangeForGameChoice.get(record.choice));

            if (record.score > maxScore) {
                maxScore = record.score;
                maxParticipantName = record.name;
            }
        }

        topPlayerName.setValue(maxParticipantName);
        topPlayerScore.setValue(maxScore);
    }

    /** Builds a map of serializable states for each participant. */
    public Map<SessionParticipant, JSONObject> getParticipantStates() {
        Map<SessionParticipant, JSONObject> participantStateMap = new HashMap<>();
        synchronized (participantRecordMapLock) {
            for (SessionParticipant participant : participantRecordMap.keySet()) {
                participantStateMap.put(participant, getSerializableStateForParticipant(participant));
            }
        }
        return participantStateMap;
    }

    /** Gets a map that details how much a participant should add or subtract based on choice. */
    private Map<GameChoice, Integer> getScoreChangeForGameChoice(
            Map<SessionParticipant, ParticipantRecord> participantRecordMap) {
        // Initialize map
        Map<GameChoice, Integer> choiceCountMap = new HashMap<>();
        for (GameChoice choice : GameChoice.values()) {
            choiceCountMap.put(choice, 0);
        }

        // Count the game choices
        choiceCountMap.put(localPlayerChoice, 1);
        for (ParticipantRecord record : participantRecordMap.values()) {
            choiceCountMap.put(record.choice, safeUnbox(choiceCountMap.get(record.choice)) + 1);
        }

        // Create a delta map where the delta is the number of inferior choices minus superior choices
        Map<GameChoice, Integer> choiceDeltaMap = new HashMap<>();
        for (GameChoice choice : GameChoice.values()) {
            choiceDeltaMap.put(
                    choice,
                    safeUnbox(choiceCountMap.get(choice.inferior()))
                            - safeUnbox(choiceCountMap.get(choice.superior())));
        }
        return choiceDeltaMap;
    }

    /** Performs the final steps required to start a new round. */
    public void finishRound() {
        gameState.setValue(GameData.GameState.ROUND_RESULT);
        roundsCompleted++;
        resetRound();
    }

    /** Returns whether the game data is ready for a round to be processed. */
    public boolean readyToProcessRound() {
        if (localPlayerChoice == null) {
            return false;
        }

        synchronized (participantRecordMapLock) {
            for (ParticipantRecord record : participantRecordMap.values()) {
                if (record.choice == null) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Reset's player choices and game state after each round. */
    private void resetRound() {
        localPlayerChoice = null;
        synchronized (participantRecordMap) {
            for (ParticipantRecord record : participantRecordMap.values()) {
                record.choice = null;
            }
        }

        gameState.setValue(GameData.GameState.WAITING_FOR_PLAYER_INPUT);
    }

    /** Allows tracking choices for a new participant. */
    public void addParticipant(SessionParticipant participant, String name) {
        synchronized (participantRecordMapLock) {
            participantRecordMap.put(participant, new ParticipantRecord.Builder().setName(name).build());
            numberOfOpponents.setValue(getNumberOfOpponentsValue() + 1);
        }
    }

    /** Removes a participant from the game data. */
    public void removeParticipant(SessionParticipant participant) {
        synchronized (participantRecordMapLock) {
            participantRecordMap.remove(participant);
            numberOfOpponents.setValue(getNumberOfOpponentsValue() - 1);
        }
    }

    /** Sets the choice for a participant. */
    public void setParticipantChoice(SessionParticipant participant, GameChoice choice) {
        synchronized (participantRecordMapLock) {
            ParticipantRecord record = participantRecordMap.get(participant);
            if (record == null) {
                Log.w(TAG, "Record for participant does not exist, cannot set game choice");
                return;
            }
            record.choice = choice;
        }
    }

    @GuardedBy("participantRecordMapLock")
    private JSONObject getSerializableStateForParticipant(SessionParticipant participant) {
        Log.i(TAG, "Getting serializable state for participant");
        ParticipantRecord record;
        JSONObject jsonObject = new JSONObject();

        // Return an empty state if the participant can no longer be found or has no entry
        record = participantRecordMap.get(participant);

        if (record == null) {
            Log.w(TAG, "State for participant is empty");
            return jsonObject;
        }

        try {
            jsonObject.put(KEY_LOCAL_PLAYER_SCORE, record.score);
            jsonObject.put(KEY_TOP_PLAYER_NAME, getOpponentPlayerName().getValue());
            jsonObject.put(KEY_TOP_PLAYER_SCORE, getOpponentPlayerScoreValue());
            jsonObject.put(KEY_NUMBER_OF_OPPONENTS, getNumberOfOpponentsValue());
        } catch (JSONException e) {
            Log.e(TAG, "Failed to properly serialize state for participant", e);
        }
        return jsonObject;
    }

    @Override
    public JSONObject getSerializableState() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(KEY_TOP_PLAYER_NAME, topPlayerName.getValue());
            jsonObject.put(KEY_TOP_PLAYER_SCORE, getOpponentPlayerScoreValue());
            jsonObject.put(KEY_LOCAL_PLAYER_SCORE, getLocalPlayerScoreValue());
            jsonObject.put(KEY_NUMBER_OF_OPPONENTS, getNumberOfOpponentsValue());
        } catch (JSONException e) {
            Log.e(TAG, "Failed to get serializable state", e);
        }
        return jsonObject;
    }

    @Override
    public void loadSerializableState(JSONObject gameData) {
        if (gameData == null) {
            Log.w(TAG, "Cannot load a null state");
            return;
        }

        try {
            if (gameData.has(KEY_TOP_PLAYER_NAME)) {
                topPlayerName.setValue(gameData.getString(KEY_TOP_PLAYER_NAME));
            }

            if (gameData.has(KEY_TOP_PLAYER_SCORE)) {
                topPlayerScore.setValue(gameData.getInt(KEY_TOP_PLAYER_SCORE));
            }

            if (gameData.has(KEY_LOCAL_PLAYER_SCORE)) {
                localPlayerScore.setValue(gameData.getInt(KEY_LOCAL_PLAYER_SCORE));
            }

            if (gameData.has(KEY_NUMBER_OF_OPPONENTS)) {
                numberOfOpponents.setValue(gameData.getInt(KEY_NUMBER_OF_OPPONENTS));
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed to load serialized game data", e);
        }
    }

    private int safeUnbox(Integer integer) {
        if (integer == null) {
            Log.w(TAG, "Unboxed a null Integer");
            return 0;
        }
        return integer;
    }

    /** Used to represent data associated with a participant. */
    private static class ParticipantRecord {
        public final String name;
        public GameChoice choice;
        public int score;

        ParticipantRecord(String name, GameChoice choice, int score) {
            this.name = name;
            this.choice = choice;
            this.score = score;
        }

        private static class Builder {
            private String name = "";
            private GameChoice choice = null;
            private int score = 0;

            Builder setName(String name) {
                this.name = name;
                return this;
            }

            Builder setChoice(GameChoice choice) {
                this.choice = choice;
                return this;
            }

            Builder setScore(int score) {
                this.score = score;
                return this;
            }

            ParticipantRecord build() {
                return new ParticipantRecord(name, choice, score);
            }
        }
    }
}