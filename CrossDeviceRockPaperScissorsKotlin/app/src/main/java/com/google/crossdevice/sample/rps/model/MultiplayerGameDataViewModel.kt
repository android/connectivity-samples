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

package com.google.crossdevice.sample.rps.model

import android.content.Context
import android.util.Log
import androidx.annotation.GuardedBy
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.google.ambient.crossdevice.sessions.SessionParticipant
import java.util.EnumMap
import org.json.JSONException
import org.json.JSONObject

/**
 * GameData object used to store information about a Multiplayer Game and to communicate those
 * changes to observers in the UI and the multiplayer GameManager.
 */
class MultiplayerGameDataViewModel : ViewModel(), GameData {

    @GuardedBy("participantRecordMapLock")
    private val participantRecordMap: MutableMap<SessionParticipant, ParticipantRecord> = HashMap()

    /** Stores Local Player's name */
    override val localPlayerName = MutableLiveData<String?>()

    /** Stores Opponent Player's name */
    override val opponentPlayerName = MutableLiveData<String?>()

    /** Stores Local Player's score. */
    override val localPlayerScore = MutableLiveData<Int?>()

    /** Stores Remote Player's score. */
    override val opponentPlayerScore = MutableLiveData<Int?>()

    /** Stores current Game State. */
    override val gameState = MutableLiveData<GameData.GameState?>()

    /** Stores current number of opponents. */
    override val numberOfOpponents = MutableLiveData<Int?>()

    /** Stores the value of number of Opponents. */
    override val numberOfOpponentsValue: Int
        get() = numberOfOpponents.value ?: 0

    /** Stores Local Player's game choice. */
    override var localPlayerChoice: GameChoice? = null

    /** Stores Remote Player's game choice. */
    override var opponentPlayerChoice: GameChoice? = null

    /** Stores the winner for each round or pending if we are still waiting on results. */
    override val roundWinner: GameData.RoundWinner? = null

    /** Stores the number of rounds completed. */
    override var roundsCompleted = 0
        private set

    /** Resets all variables to default values prior to starting a game. */
    fun resetGameData() {
        synchronized(participantRecordMapLock) { participantRecordMap.clear() }
        localPlayerName.value = CodenameGenerator.generate()
        opponentPlayerName.value = null
        localPlayerScore.value = 0
        opponentPlayerScore.value = 0
        numberOfOpponents.value = 0
        localPlayerChoice = null
        opponentPlayerChoice = null
        gameState.value = GameData.GameState.DISCONNECTED
        roundsCompleted = 0
    }

    /** Stores the value of Local Player's score. */
    private val localPlayerScoreValue: Int
        get() = localPlayerScore.value ?: 0

    /** Stores the value of Opponent Player's score. */
    private val opponentPlayerScoreValue: Int
        get() = opponentPlayerScore.value ?: 0

    /** Processes the round, updates each player's score, and returns the states of each player. */
    fun processRoundAndGetParticipantStates(): Map<SessionParticipant, JSONObject> {
        synchronized(participantRecordMapLock) { processRound() }
        return getParticipantStates()
    }

    /** Computes new scores for each player and finishes the round. */
    @GuardedBy("participantRecordMapLock")
    private fun processRound() {
        val scoreChangeForGameChoice = getScoreChangeForGameChoice(participantRecordMap)

        localPlayerChoice?.let {
            localPlayerScore.value = localPlayerScoreValue + scoreChangeForGameChoice.getOrDefault(it, 0)
        }

        var maxScore = localPlayerScoreValue
        var maxParticipantName = DEFAULT_LOCAL_NAME
        for (record in participantRecordMap.values) {
            record.choice?.let { record.score += scoreChangeForGameChoice.getOrDefault(it, 0) }
            if (record.score > maxScore) {
                maxScore = record.score
                maxParticipantName = record.name
            }
        }

        opponentPlayerName.value = maxParticipantName
        opponentPlayerScore.value = maxScore
    }

    /** Builds a map of serializable states for each participant. */
    fun getParticipantStates() =
        HashMap<SessionParticipant, JSONObject>()
            .also {
                synchronized(participantRecordMapLock) {
                    for (participant in participantRecordMap.keys) {
                        it[participant] = getSerializableStateForParticipant(participant)
                    }
                }
            }
            .toMap()

    /** Gets a map that details how much a participant should add or subtract based on choice. */
    private fun getScoreChangeForGameChoice(
        participantRecordMap: Map<SessionParticipant, ParticipantRecord>,
    ): Map<GameChoice, Int> {
        // Initialize map
        val choiceCountMap: MutableMap<GameChoice, Int> = EnumMap(GameChoice::class.java)
        for (choice in GameChoice.values()) {
            choiceCountMap[choice] = 0
        }

        // Count the game choices
        localPlayerChoice?.let { choiceCountMap[it] = 1 }
        for (record in participantRecordMap.values) {
            record.choice?.let { choiceCountMap[it] = choiceCountMap.getOrDefault(it, 0) + 1 }
        }

        // Create a delta map where the delta is the number of inferior choices minus superior choices
        val choiceDeltaMap: MutableMap<GameChoice, Int> = EnumMap(GameChoice::class.java)
        for (choice in GameChoice.values()) {
            choiceDeltaMap[choice] =
                (choiceCountMap.getOrDefault(choice.inferior(), 0) -
                        choiceCountMap.getOrDefault(choice.superior(), 0))
        }
        return choiceDeltaMap
    }

    /** Performs the final steps required to start a new round. */
    fun finishRound() {
        gameState.value = GameData.GameState.ROUND_RESULT
        roundsCompleted++
        resetRound()
    }

    /** Returns whether the game data is ready for a round to be processed. */
    fun readyToProcessRound(): Boolean {
        if (localPlayerChoice == null) {
            return false
        }

        synchronized(participantRecordMapLock) {
            for (record in participantRecordMap.values) {
                if (record.choice == null) {
                    return false
                }
            }
        }
        return true
    }

    /** Reset's player choices and game state after each round. */
    private fun resetRound() {
        localPlayerChoice = null

        synchronized(participantRecordMapLock) {
            for (record in participantRecordMap.values) {
                record.choice = null
            }
        }
        gameState.value = GameData.GameState.WAITING_FOR_PLAYER_INPUT
    }

    /** Allows tracking choices for a new participant. */
    fun addParticipant(participant: SessionParticipant, name: String) {
        synchronized(participantRecordMapLock) {
            participantRecordMap[participant] = ParticipantRecord.Builder().setName(name).build()
            numberOfOpponents.value = numberOfOpponentsValue + 1
        }
    }

    /** Removes a participant from the game data. */
    fun removeParticipant(participant: SessionParticipant) {
        synchronized(participantRecordMapLock) {
            participantRecordMap.remove(participant)
            numberOfOpponents.value = numberOfOpponentsValue - 1
        }
    }

    /** Sets the choice for a participant. */
    fun setParticipantChoice(participant: SessionParticipant, choice: GameChoice) {
        synchronized(participantRecordMapLock) {
            val record = participantRecordMap[participant]
            if (record == null) {
                Log.w(TAG, "Record for participant does not exist, cannot set game choice")
                return
            }
            record.choice = choice
        }
    }

    @GuardedBy("participantRecordMapLock")
    private fun getSerializableStateForParticipant(participant: SessionParticipant): JSONObject {
        Log.i(TAG, "Getting serializable state for participant")
        val jsonObject = JSONObject()
        participantRecordMap[participant]?.also {
            try {
                jsonObject.put(KEY_LOCAL_PLAYER_SCORE, it.score)
                jsonObject.put(KEY_TOP_PLAYER_NAME, opponentPlayerName.value)
                jsonObject.put(KEY_TOP_PLAYER_SCORE, opponentPlayerScoreValue)
                jsonObject.put(KEY_NUMBER_OF_OPPONENTS, opponentPlayerScoreValue)
            } catch (e: JSONException) {
                Log.e(TAG, "Failed to properly serialize state for participant", e)
            }
            return jsonObject
        }
            ?: Log.w(TAG, "State for participant is empty")
        return jsonObject
    }

    override fun getSerializableState(): JSONObject {
        val jsonObject = JSONObject()
        try {
            jsonObject.put(KEY_TOP_PLAYER_NAME, opponentPlayerName.value)
            jsonObject.put(KEY_TOP_PLAYER_SCORE, opponentPlayerScoreValue)
            jsonObject.put(KEY_LOCAL_PLAYER_SCORE, localPlayerScoreValue)
            jsonObject.put(KEY_NUMBER_OF_OPPONENTS, numberOfOpponents.value)
        } catch (e: JSONException) {
            Log.e(TAG, "Failed to get serializable state", e)
        }
        return jsonObject
    }

    override fun loadSerializableState(gameData: JSONObject) {
        try {
            if (gameData.has(KEY_TOP_PLAYER_NAME)) {
                opponentPlayerName.value = gameData.getString(KEY_TOP_PLAYER_NAME)
            }

            if (gameData.has(KEY_TOP_PLAYER_SCORE)) {
                opponentPlayerScore.value = gameData.getInt(KEY_TOP_PLAYER_SCORE)
            }

            if (gameData.has(KEY_LOCAL_PLAYER_SCORE)) {
                localPlayerScore.value = gameData.getInt(KEY_LOCAL_PLAYER_SCORE)
            }

            if (gameData.has(KEY_NUMBER_OF_OPPONENTS)) {
                numberOfOpponents.value = gameData.getInt(KEY_NUMBER_OF_OPPONENTS)
            }
        } catch (e: JSONException) {
            Log.e(TAG, "Failed to load serialized game data", e)
        }
    }

    companion object {
        private const val TAG = "MultiplayerGameDataViewModel"
        private const val DEFAULT_LOCAL_NAME = "Host"

        private const val KEY_TOP_PLAYER_NAME = "TOP_PLAYER_NAME"
        private const val KEY_TOP_PLAYER_SCORE = "TOP_PLAYER_SCORE"
        private const val KEY_LOCAL_PLAYER_SCORE = "LOCAL_PLAYER_SCORE"
        private const val KEY_NUMBER_OF_OPPONENTS = "NUMBER_OF_OPPONENTS"

        private val participantRecordMapLock = Any()

        fun createInstance(context: Context?): MultiplayerGameDataViewModel {
            return ViewModelProvider((context as ViewModelStoreOwner?)!!)[
                    MultiplayerGameDataViewModel::class.java]
        }

        /** Used to represent data associated with a participant. */
        private class ParticipantRecord(val name: String, var choice: GameChoice?, var score: Int) {
            class Builder {
                private var name = ""
                private var choice: GameChoice? = null
                private var score = 0
                fun setName(name: String): Builder {
                    this.name = name
                    return this
                }

                fun setChoice(choice: GameChoice?): Builder {
                    this.choice = choice
                    return this
                }

                fun setScore(score: Int): Builder {
                    this.score = score
                    return this
                }

                fun build(): ParticipantRecord {
                    return ParticipantRecord(name, choice, score)
                }
            }
        }
    }
}