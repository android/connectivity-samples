/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.google.android.gms.nearby.messages.samples.nearbydevices;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;
import com.google.android.gms.nearby.messages.MessagesClient;
import com.google.android.gms.nearby.messages.PublishCallback;
import com.google.android.gms.nearby.messages.PublishOptions;
import com.google.android.gms.nearby.messages.Strategy;
import com.google.android.gms.nearby.messages.SubscribeCallback;
import com.google.android.gms.nearby.messages.SubscribeOptions;
import com.google.android.gms.nearby.messages.samples.nearbydevices.databinding.ActivityMainBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.material.snackbar.Snackbar;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * An activity that allows a user to publish device information, and receive information about
 * nearby devices.
 * <p/>
 * The UI exposes a button to subscribe to broadcasts from nearby devices, and another button to
 * publish messages that can be read by nearby subscribing devices. Both buttons toggle state,
 * allowing the user to cancel a subscription or stop publishing.
 * <p/>
 * This activity demonstrates the use of the
 * {@link Nearby#getMessagesClient(Activity)}
 * {@link MessagesClient#publish(Message, PublishOptions)}
 * {@link MessagesClient#subscribe(MessageListener, SubscribeOptions)}
 * {@link Strategy}
 * <p>
 * <p/>a
 * We check the app's permissions and present an opt-in dialog to the user, who can then grant the
 * required location permission.
 * <p/>
 * Using Nearby in the foreground is battery intensive, and pub-sub is best done for short
 * durations. In this sample, we set the TTL for publishing and subscribing to three minutes
 * using a {@link Strategy}. When the TTL is reached, a publication or subscription expires.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int TTL_IN_SECONDS = 3 * 60; // Three minutes.
    /**
     * Sets the time in seconds for a published message or a subscription to live. Set to three
     * minutes in this sample.
     */
    private static final Strategy PUB_SUB_STRATEGY = new Strategy.Builder()
            .setTtlSeconds(TTL_IN_SECONDS).build();
    private static final String MISSING_API_KEY = "It's possible that you haven't added your" +
            " API-KEY. See  " +
            "https://developers.google.com/nearby/messages/android/get-started#step_4_configure_your_project";

    /**
     * The {@link Message} object used to broadcast information about the device to nearby devices.
     */
    private Message mMessage;

    /**
     * A {@link MessageListener} for processing messages from nearby devices.
     */
    private MessageListener mMessageListener;

    /**
     * Adapter for working with messages from nearby publishers.
     */
    private ArrayAdapter<String> mNearbyDevicesArrayAdapter;

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        // The message being published is simply the Build.MODEL of the device. But since the
        // Messages API is expecting a byte array, you must convert the data to a byte array.
        mMessage = new Message(Build.MODEL.getBytes(Charset.forName("UTF-8")));

        mMessageListener = new MessageListener() {
            @Override
            public void onFound(final Message message) {
                // Called when a new message is found.
                String msgBody = new String(message.getContent());
                mNearbyDevicesArrayAdapter.add(msgBody);
            }

            @Override
            public void onLost(final Message message) {
                // Called when a message is no longer detectable nearby.
                String msgBody = new String(message.getContent());
                mNearbyDevicesArrayAdapter.remove(msgBody);
            }
        };

        binding.subscribeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                subscribe();
            } else {
                unsubscribe();
            }
        });

        binding.publishSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                publish();
            } else {
                unpublish();
            }
        });

        final List<String> nearbyDevicesArrayList = new ArrayList<>();
        mNearbyDevicesArrayAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                nearbyDevicesArrayList);
        final ListView nearbyDevicesListView = (ListView) findViewById(
                R.id.nearby_devices_list_view);
        if (nearbyDevicesListView != null) {
            nearbyDevicesListView.setAdapter(mNearbyDevicesArrayAdapter);
        }
    }

    /**
     * Subscribes to messages from nearby devices and updates the UI if the subscription either
     * fails or TTLs.
     */
    private void subscribe() {
        Log.i(TAG, "Subscribing");
        mNearbyDevicesArrayAdapter.clear();
        SubscribeOptions options = new SubscribeOptions.Builder()
                .setStrategy(PUB_SUB_STRATEGY)
                .setCallback(new SubscribeCallback() {
                    @Override
                    public void onExpired() {
                        super.onExpired();
                        Log.i(TAG, "No longer subscribing");
                        runOnUiThread(() -> binding.subscribeSwitch.setChecked(false));
                    }
                }).build();

        Nearby.getMessagesClient(this).subscribe(mMessageListener, options);
    }

    /**
     * Publishes a message to nearby devices and updates the UI if the publication either fails or
     * TTLs.
     */
    private void publish() {
        Log.i(TAG, "Publishing");
        PublishOptions options = new PublishOptions.Builder()
                .setStrategy(PUB_SUB_STRATEGY)
                .setCallback(new PublishCallback() {
                    @Override
                    public void onExpired() {
                        super.onExpired();
                        Log.i(TAG, "No longer publishing");
                        runOnUiThread(() -> binding.publishSwitch.setChecked(false));
                    }
                }).build();

        Nearby.getMessagesClient(this).publish(mMessage, options)
                .addOnFailureListener(e -> {
                    logAndShowSnackbar(MISSING_API_KEY);
                });
    }

    /**
     * Stops subscribing to messages from nearby devices.
     */
    private void unsubscribe() {
        Log.i(TAG, "Unsubscribing.");
        Nearby.getMessagesClient(this).unsubscribe(mMessageListener);
    }

    /**
     * Stops publishing message to nearby devices.
     */
    private void unpublish() {
        Log.i(TAG, "Unpublishing.");
        Nearby.getMessagesClient(this).unpublish(mMessage);
    }

    private void logAndShowSnackbar(final String text) {
        Log.w(TAG, text);
        if (binding.activityMainContainer != null) {
            Snackbar.make(binding.activityMainContainer, text, Snackbar.LENGTH_LONG).show();
        }
    }
}