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
import com.google.crossdevice.sample.rps.R;
import com.google.crossdevice.sample.rps.model.GameChoice;
import com.google.crossdevice.sample.rps.model.GameData;
import com.google.crossdevice.sample.rps.model.MultiplayerGameDataViewModel;
import com.google.crossdevice.sample.rps.model.ShareableGameState;

import org.json.JSONObject;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Executor;

/** Implementation of GameManager using Sessions APIs for a multiplayer game. */
public final class SessionsMultiplayerGameManager implements GameManager {

    private static final String TAG = "SessionsMPGameManager";
    public static final String ACTION_WAKE_UP =
            "com.google.crossdevice.samples.rockpaperscissors.SESSIONS_MULTIPLAYER_WAKEUP";

    private final PrimarySessionStateCallback primarySessionStateCallback =
            new PrimarySessionStateCallback() {
                @Override
                public void onShareFailureWithParticipant(
                        SessionId sessionId, SessionException exception, SessionParticipant participant) {
                    Log.e(TAG, "Share failure with participant: " + participant.getDisplayName(), exception);
                }

                @Override
                public void onParticipantDeparted(SessionId sessionId, SessionParticipant participant) {
                    Log.d(TAG, "SessionParticipant departed: " + participant.getDisplayName());
                    gameData.removeParticipant(participant);

                    if (gameData.getNumberOfOpponentsValue() == 0) {
                        Log.d(TAG, "All participants have departed");
                        disconnect();
                        return;
                    }

                    sendUniqueStateToEachParticipant(gameData.getParticipantStates());
                    finishRound();
                }

                @Override
                public void onParticipantJoined(SessionId sessionId, SessionParticipant participant) {
                    Log.d(TAG, "New Participant joined: " + participant.getDisplayName());
                    gameData.addParticipant(participant, participant.getDisplayName().toString());

                    addRemoteConnectionCallback(primarySession, participant);

                    // Number of participants has changed, send new value to all participants
                    sendUniqueStateToEachParticipant(gameData.getParticipantStates());
                }

                @Override
                public void onPrimarySessionCleanup(SessionId sessionId) {
                    Log.d(TAG, "PrimarySession cleanup");
                    primarySession = null;
                    resetGame();
                }

                @Override
                public void onShareInitiated(SessionId sessionId, int numPotentialParticipants) {
                    Log.d(TAG, "Share initiated with participants: " + numPotentialParticipants);

                    // Destroy primary session if no one has joined this game yet and no one has joined the
                    // most recent share
                    if (gameData.getNumberOfOpponentsValue() == 0 && numPotentialParticipants == 0) {
                        destroyPrimarySessionAndStopSharing();
                        return;
                    }

                    gameData.getGameState().setValue(GameData.GameState.WAITING_FOR_PLAYER_INPUT);
                }

                /** Add a callback to SessionParticipant for handling received messages */
                private void addRemoteConnectionCallback(
                        PrimarySession session, SessionParticipant participant) {
                    SessionRemoteConnection connection =
                            getSecondaryRemoteConnectionForParticipantOrNull(session, participant);

                    if (connection == null) {
                        Log.w(TAG, "Skipping adding callback for: " + participant.getDisplayName());
                        return;
                    }

                    connection.registerReceiver(
                            (participant1, message) -> {
                                // Messages received from secondaries are simply their game choices
                                Log.i(TAG, "Parsing message received as Host");
                                gameData.setParticipantChoice(
                                        participant, GameChoice.valueOf(new String(message, UTF_8)));
                                finishRound();
                            });
                }
            };

    private final SecondarySessionStateCallback secondarySessionStateCallback =
            new SecondarySessionStateCallback() {
                @Override
                public void onSecondarySessionCleanup(SessionId sessionId) {
                    secondarySession = null;
                    secondaryConnection = null;
                    resetGame();
                }
            };

    private final Context context;
    private final Executor mainExecutor;
    private final MultiplayerGameDataViewModel gameData;
    private final Sessions sessions;

    private SessionId sessionId;
    private PrimarySession primarySession;
    private SecondarySession secondarySession;
    private SessionRemoteConnection secondaryConnection;

    public SessionsMultiplayerGameManager(Context context) {
        this.context = context;
        gameData = MultiplayerGameDataViewModel.createInstance(context);
        sessions = Sessions.create(context);
        mainExecutor = ContextCompat.getMainExecutor(context);

        sessions.registerActivityResultCaller((ActivityResultCaller) context);

        resetGame();

        sessionId = sessions.createSession(/* applicationSessionTag */ null);
    }

    @Override
    public GameData getGameData() {
        return gameData;
    }

    @Override
    public void disconnect() {
        closeConnections();
    }

    @Override
    public void findOpponent() {
        gameData.getGameState().setValue(GameData.GameState.SEARCHING);

        if (sessionId != null) {
            inviteOpponent(sessionId);
        }
    }

    @Override
    public void sendGameChoice(GameChoice choice, Callback callback) {
        gameData.setLocalPlayerChoice(choice);
        gameData.getGameState().setValue(GameData.GameState.WAITING_FOR_ROUND_RESULT);

        if (isHost()) {
            finishRound();
            callback.onSuccess();
        } else if (isParticipant()) {
            broadcastGameChoice(callback);
        }
    }

    @Override
    public void resetGame() {
        gameData.resetGameData();
    }

    @Override
    public void finishRound() {
        if (!gameData.readyToProcessRound()) {
            return;
        }

        if (isHost()) {
            sendUniqueStateToEachParticipant(gameData.processRoundAndGetParticipantStates());
        }

        gameData.finishRound();
    }

    @Override
    public void acceptGameInvitation(Intent intent) {
        getSecondarySessionAndRemoteConnection(intent);
    }

    @Override
    public boolean isHost() {
        return primarySession != null;
    }

    private boolean isParticipant() {
        return secondaryConnection != null;
    }

    /** Gets the SecondarySession and uses it to get the RemoteConnection. */
    private void getSecondarySessionAndRemoteConnection(Intent intent) {
        Log.d(TAG, "Getting secondary session and remote connection");
        Futures.addCallback(
                sessions.getSecondarySessionFuture(intent, secondarySessionStateCallback),
                new FutureCallback<SecondarySession>() {
                    @Override
                    public void onSuccess(SecondarySession result) {
                        Log.d(TAG, "Succeeded to get SecondarySession");
                        secondarySession = result;
                        getRemoteConnectionAndRegisterReceiver(result);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        Log.e(TAG, "Failed to get SessionHandle", t);
                    }
                },
                mainExecutor);
    }

    /** Gets the RemoteConnection and registers a message receiver to it. */
    private void getRemoteConnectionAndRegisterReceiver(SecondarySession sessionHandle) {
        resetGame();
        secondaryConnection = sessionHandle.getDefaultRemoteConnection();
        secondaryConnection.registerReceiver(
                (participant1, message) -> {
                    Log.i(TAG, "Parsing message as Participant of size: " + message.length);
                    // Messages received as a Secondary are GameState updates
                    gameData.loadSerializableState(new ShareableGameState().loadBytes(message).gameData);
                    finishRound();
                    gameData.getGameState().setValue(GameData.GameState.WAITING_FOR_PLAYER_INPUT);
                });
    }

    /** Invites an opponent to a created Session. */
    private void inviteOpponent(SessionId sessionId) {
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
                    public void onFailure(Throwable t) {
                        Log.e(TAG, "Failed to launch opponent picker", t);
                        resetGame();
                    }
                },
                mainExecutor);
    }

    /** Sends the game choice to whichever connection is open. */
    private void broadcastGameChoice(Callback callback) {
        Log.i(TAG, "Sending game choice");
        sendMessageToRemoteConnection(
                secondaryConnection,
                gameData.getLocalPlayerChoice().name().getBytes(UTF_8),
                new FutureCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Log.i(TAG, "Successfully sent game choice");
                        callback.onSuccess();
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        Log.e(TAG, "Failed to send game choice", t);
                        callback.onFailure();
                    }
                });
    }

    /** Sends to each provided participant its state. */
    private void sendUniqueStateToEachParticipant(
            Map<SessionParticipant, JSONObject> participantStates) {
        Log.v(TAG, "Sending state to " + participantStates.size() + " participants");
        for (Map.Entry<SessionParticipant, JSONObject> stateEntry : participantStates.entrySet()) {
            sendParticipantStateToParticipant(
                    /* state */ stateEntry.getValue(), /* participant */ stateEntry.getKey());
        }
    }

    /** Sends the provided state to the specified participant. */
    private void sendParticipantStateToParticipant(
            JSONObject participantState, SessionParticipant participant) {
        SessionRemoteConnection connection =
                getSecondaryRemoteConnectionForParticipantOrNull(primarySession, participant);

        if (connection == null) {
            Log.w(TAG, "Skipping sending state to: " + participant.getDisplayName());
            return;
        }

        // Adds the game data to a shareable game state, serializes it, and sends to participant
        sendMessageToRemoteConnection(
                connection,
                new ShareableGameState()
                        .setGameData(participantState)
                        .getState()
                        .toString()
                        .getBytes(UTF_8),
                new FutureCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Log.d(TAG, "Succeeded to send game state to participant");
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        Log.e(TAG, "Failed to send game state to participant", t);
                    }
                });
    }

    /** Sends a message to a remote connection. */
    private void sendMessageToRemoteConnection(
            SessionRemoteConnection connection, byte[] message, FutureCallback<Void> callback) {
        Futures.addCallback(connection.sendFuture(message), callback, mainExecutor);
    }

    /** Closes any open PrimarySession or SecondarySession. */
    private void closeConnections() {
        if (primarySession != null) {
            destroyPrimarySessionAndStopSharing();
        }

        if (secondarySession != null) {
            destroySecondarySession();
        }
    }

    /** Destroys a PrimarySession. */
    private void destroyPrimarySessionAndStopSharing() {
        Futures.addCallback(
                primarySession.destroyPrimarySessionAndStopSharingFuture(),
                new FutureCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Log.i(TAG, "Destroyed primary session and stopped sharing");
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        Log.e(TAG, "Failed to destroy primary session", t);
                    }
                },
                mainExecutor);
    }

    /** Destroys a SecondarySession. */
    private void destroySecondarySession() {
        Futures.addCallback(
                secondarySession.destroySecondarySessionFuture(),
                new FutureCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Log.i(TAG, "Successfully destroyed SecondarySession");
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        Log.e(TAG, "Failed to destroy secondary session handle", t);
                    }
                },
                mainExecutor);
    }

    /** Attempts to get the connection for a participant. */
    private SessionRemoteConnection getSecondaryRemoteConnectionForParticipantOrNull(
            PrimarySession session, SessionParticipant participant) {
        try {
            return session.getSecondaryRemoteConnectionForParticipant(participant);
        } catch (SessionException e) {
            Log.e(TAG, "Failed to get connection for participant: " + participant.getDisplayName(), e);
            return null;
        }
    }
}
