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
import com.google.crossdevice.sample.rps.model.MultiplayerGameDataViewModel
import com.google.crossdevice.sample.rps.model.ShareableGameState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.nio.charset.StandardCharsets

/** Implementation of GameManager using Sessions APIs for a multiplayer game. */
class SessionsMultiplayerGameManager(
    private val context: Context,
    private val scope: CoroutineScope
) : GameManager {
    private val primarySessionStateCallback: PrimarySessionStateCallback =
        object : PrimarySessionStateCallback {
            override fun onShareFailureWithParticipant(
                sessionId: SessionId,
                exception: SessionException,
                participant: SessionParticipant,
            ) {
                Log.e(TAG, "Share failure with participant: " + participant.displayName, exception)
            }

            override fun onParticipantDeparted(sessionId: SessionId, participant: SessionParticipant) {
                Log.d(TAG, "SessionParticipant departed: " + participant.displayName)
                gameData.removeParticipant(participant)

                // Number of participants has changed, send new value to all participants
                if (gameData.numberOfOpponentsValue == 0) {
                    Log.d(TAG, "All participants have departed");
                    disconnect()
                    return
                }

                sendUniqueStateToEachParticipant(gameData.getParticipantStates())
                finishRound()
            }

            override fun onParticipantJoined(sessionId: SessionId, participant: SessionParticipant) {
                Log.d(TAG, "New Participant joined: " + participant.displayName)
                primarySession?.let {
                    gameData.addParticipant(participant, participant.displayName.toString())
                    addRemoteConnectionCallback(it, participant)
                    sendUniqueStateToEachParticipant(gameData.getParticipantStates())
                }
            }

            override fun onPrimarySessionCleanup(sessionId: SessionId) {
                Log.d(TAG, "PrimarySession cleanup")
                primarySession = null
                resetGame()
            }

            override fun onShareInitiated(sessionId: SessionId, numPotentialParticipants: Int) {
                Log.d(TAG, "Share initiated with participants: $numPotentialParticipants")

                // Destroy primary session if no one has joined this game yet and no one has joined the
                // most recent share
                if (gameData.numberOfOpponentsValue == 0 && numPotentialParticipants == 0) {
                    primarySession?.let { destroyPrimarySessionAndStopSharing(it) }
                    return
                }
                gameData.gameState.value = GameData.GameState.WAITING_FOR_PLAYER_INPUT
            }

            /** Add a callback to SessionParticipant for handling received messages */
            private fun addRemoteConnectionCallback(
                session: PrimarySession,
                participant: SessionParticipant,
            ) {
                getSecondaryRemoteConnectionForParticipantOrNull(session, participant)?.also {
                    it.registerReceiver(
                        object : SessionConnectionReceiver {
                            override fun onMessageReceived(participant: SessionParticipant, payload: ByteArray) {
                                // Messages received from secondaries are simply their game choices
                                Log.i(TAG, "Parsing message received as Host")
                                gameData.setParticipantChoice(
                                    participant,
                                    GameChoice.valueOf(String(payload, StandardCharsets.UTF_8))
                                )
                                finishRound()
                            }
                        }
                    )
                }
                    ?: Log.w(TAG, "Skipping adding callback for: " + participant.displayName)
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

    override val gameData = MultiplayerGameDataViewModel.createInstance(context)
    private val sessions: Sessions = Sessions.create(context)
    private var sessionId: SessionId? = null
    private var primarySession: PrimarySession? = null
    private var secondarySession: SecondarySession? = null
    private var secondaryConnection: SessionRemoteConnection? = null

    init {
        sessions.registerActivityResultCaller((context as ActivityResultCaller))
        resetGame()
        createSession()
    }

    override fun disconnect() {
        closeConnections()
    }

    override fun findOpponent() {
        sessionId?.also { sessionId ->
            gameData.gameState.value = GameData.GameState.SEARCHING
            scope.launch {
                primarySession =
                    sessions.shareSession(
                        sessionId,
                        StartComponentRequest.Builder()
                            .setAction(ACTION_WAKE_UP)
                            .setReason(context.getString(R.string.wakeup_reason)).build(),
                        deviceFilters = emptyList(),
                        primarySessionStateCallback
                    )
                Log.d(TAG, "Successfully launched opponent picker")
            }
        } ?: Log.d(TAG, "Skipping findOpponent() due to null SessionId")
    }

    override fun sendGameChoice(choice: GameChoice, callback: GameManager.Callback) {
        gameData.localPlayerChoice = choice
        gameData.gameState.value = GameData.GameState.WAITING_FOR_ROUND_RESULT
        if (isHost()) {
            finishRound()
            callback.onSuccess()
        } else if (isParticipant()) {
            broadcastGameChoice(callback)
        }
    }

    override fun resetGame() {
        gameData.resetGameData()
    }

    override fun finishRound() {
        if (!gameData.readyToProcessRound()) {
            return
        }

        if (isHost()) {
            sendUniqueStateToEachParticipant(gameData.processRoundAndGetParticipantStates())
        }

        gameData.finishRound()
    }

    override fun acceptGameInvitation(intent: Intent) {
        getSecondarySessionAndRemoteConnection(intent)
    }

    override fun isHost() = primarySession != null

    private fun isParticipant() = secondaryConnection != null

    private fun createSession() {
        scope.launch {
            sessionId = sessions.createSession()
            Log.d(TAG, "Successfully created: $sessionId")
        }
    }

    /** Gets the SecondarySession and uses it to get the RemoteConnection. */
    private fun getSecondarySessionAndRemoteConnection(intent: Intent) {
        scope.launch {
            secondarySession =
                sessions.getSecondarySession(intent, secondarySessionStateCallback).also {
                    getRemoteConnectionAndRegisterReceiver(it)
                }
            Log.d(TAG, "Got SecondarySession")
        }
    }

    /** Gets the RemoteConnection and registers a message receiver to it. */
    private fun getRemoteConnectionAndRegisterReceiver(sessionHandle: SecondarySession) {
        resetGame()
        secondaryConnection =
            sessionHandle.getDefaultRemoteConnection().apply {
                registerReceiver(
                    object : SessionConnectionReceiver {
                        override fun onMessageReceived(participant: SessionParticipant, payload: ByteArray) {
                            Log.i(TAG, "Parsing message as Participant of size: " + payload.size)
                            // Messages received as a Secondary are GameState updates
                            ShareableGameState().loadBytes(payload).gameData?.let {
                                gameData.loadSerializableState(it)
                            }
                            finishRound()
                            gameData.gameState.value = GameData.GameState.WAITING_FOR_PLAYER_INPUT
                        }
                    }
                )
            }
    }

    /** Sends the game choice to whichever connection is open. */
    private fun broadcastGameChoice(callback: GameManager.Callback) {
        Log.i(TAG, "Sending game choice")
        scope.launch {
            gameData.localPlayerChoice?.let {
                secondaryConnection
                    ?.send(it.name.toByteArray(StandardCharsets.UTF_8))
                    ?.onSuccess {
                        Log.i(TAG, "Successfully sent game choice")
                        callback.onSuccess()
                    }
                    ?.onFailure { throwable ->
                        Log.e(TAG, "Failed to send game choice", throwable)
                        callback.onFailure()
                    }
            }
        }
    }

    /** Sends to each provided participant its state. */
    private fun sendUniqueStateToEachParticipant(
        participantStates: Map<SessionParticipant, JSONObject>,
    ) {
        Log.v(TAG, "Sending state to " + participantStates.size + " participants")
        for ((participant, state) in participantStates) {
            sendParticipantStateToParticipant(state, participant)
        }
    }

    /** Sends the provided state to the specified participant. */
    private fun sendParticipantStateToParticipant(
        participantState: JSONObject,
        participant: SessionParticipant,
    ) {
        scope.launch {
            primarySession?.let { session ->
                getSecondaryRemoteConnectionForParticipantOrNull(session, participant)?.also { connection ->
                    connection
                        .send(
                            ShareableGameState()
                                .setGameData(participantState)
                                .getState()
                                .toString()
                                .toByteArray(StandardCharsets.UTF_8)
                        )
                        .onSuccess { Log.d(TAG, "Succeeded to send game state to participant") }
                        .onFailure { throwable ->
                            Log.e(TAG, "Failed to send game state to participant", throwable)
                        }
                }
                    ?: Log.w(TAG, "Skipping sending state to: " + participant.displayName)
            }
        }
    }

    /** Closes any open PrimarySession or SecondarySession. */
    private fun closeConnections() {
        primarySession?.let { destroyPrimarySessionAndStopSharing(it) }

        secondarySession?.let { destroySecondarySession(it) }
    }

    /** Destroys a PrimarySession. */
    private fun destroyPrimarySessionAndStopSharing(session: PrimarySession) {
        scope.launch {
            session.destroyPrimarySessionAndStopSharing()
            Log.i(TAG, "Destroyed primary session and stopped sharing")
        }
    }

    /** Destroys a SecondarySession. */
    private fun destroySecondarySession(session: SecondarySession) {
        scope.launch {
            session.destroySecondarySession()
            Log.i(TAG, "Destroyed SecondarySession")
        }
    }

    /** Attempts to get the connection for a participant. */
    private fun getSecondaryRemoteConnectionForParticipantOrNull(
        session: PrimarySession,
        participant: SessionParticipant,
    ): SessionRemoteConnection? {
        return try {
            session.getSecondaryRemoteConnectionForParticipant(participant)
        } catch (e: SessionException) {
            Log.e(TAG, "Failed to get connection for participant: " + participant.displayName, e)
            null
        }
    }

    companion object {
        private const val TAG = "SessionsMPGameManager"
        const val ACTION_WAKE_UP =
            "com.google.crossdevice.sample.rps.SESSIONS_MULTIPLAYER_WAKEUP"
    }
}