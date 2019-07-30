/**
 * Copyright 2016 Google Inc. All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.nearby.messages.samples.nearbybackgroundbeacons;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;

import java.util.List;


/**
 * While subscribed in the background, this service shows a persistent notification with the
 * current set of messages from nearby beacons. Nearby launches this service when a message is
 * found or lost, and this service updates the notification, then stops itself.
 */
public class BackgroundSubscribeIntentService extends IntentService {
    private static final int MESSAGES_NOTIFICATION_ID = 1;
    private static final int NUM_MESSAGES_IN_NOTIFICATION = 5;

    public BackgroundSubscribeIntentService() {
        super("BackgroundSubscribeIntentService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        updateNotification();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            Nearby.Messages.handleIntent(intent, new MessageListener() {
                @Override
                public void onFound(Message message) {
                    Utils.saveFoundMessage(getApplicationContext(), message);
                    updateNotification();
                }

                @Override
                public void onLost(Message message) {
                    Utils.removeLostMessage(getApplicationContext(), message);
                    updateNotification();
                }
            });
        }
    }

    private void updateNotification() {
        List<String> messages = Utils.getCachedMessages(getApplicationContext());
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Intent launchIntent = new Intent(getApplicationContext(), MainActivity.class);
        launchIntent.setAction(Intent.ACTION_MAIN);
        launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
                launchIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        String contentTitle = getContentTitle(messages);
        String contentText = getContentText(messages);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(android.R.drawable.star_on)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText))
                .setOngoing(true)
                .setContentIntent(pi);
        notificationManager.notify(MESSAGES_NOTIFICATION_ID, notificationBuilder.build());
    }

    private String getContentTitle(List<String> messages) {
        switch (messages.size()) {
            case 0:
                return getResources().getString(R.string.scanning);
            case 1:
                return getResources().getString(R.string.one_message);
            default:
                return getResources().getString(R.string.many_messages, messages.size());
        }
    }

    private String getContentText(List<String> messages) {
        String newline = System.getProperty("line.separator");
        if (messages.size() < NUM_MESSAGES_IN_NOTIFICATION) {
            return TextUtils.join(newline, messages);
        }
        return TextUtils.join(newline, messages.subList(0, NUM_MESSAGES_IN_NOTIFICATION)) +
                newline + "&#8230;";
    }
}