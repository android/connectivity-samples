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

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.android.gms.nearby.messages.Message;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class Utils {
    static final String KEY_CACHED_MESSAGES = "cached-messages";

    /**
     * Fetches message strings stored in {@link SharedPreferences}.
     *
     * @param context The context.
     * @return  A list (possibly empty) containing message strings.
     */
    static List<String> getCachedMessages(Context context) {
        SharedPreferences sharedPrefs = getSharedPreferences(context);
        String cachedMessagesJson = sharedPrefs.getString(KEY_CACHED_MESSAGES, "");
        if (TextUtils.isEmpty(cachedMessagesJson)) {
            return Collections.emptyList();
        } else {
            Type type = new TypeToken<List<String>>() {}.getType();
            return new Gson().fromJson(cachedMessagesJson, type);
        }
    }

    /**
     * Saves a message string to {@link SharedPreferences}.
     *
     * @param context The context.
     * @param message The Message whose payload (as string) is saved to SharedPreferences.
     */
    static void saveFoundMessage(Context context, Message message) {
        ArrayList<String> cachedMessages = new ArrayList<>(getCachedMessages(context));
        Set<String> cachedMessagesSet = new HashSet<>(cachedMessages);
        String messageString = new String(message.getContent());
        if (!cachedMessagesSet.contains(messageString)) {
            cachedMessages.add(0, new String(message.getContent()));
            getSharedPreferences(context)
                    .edit()
                    .putString(KEY_CACHED_MESSAGES, new Gson().toJson(cachedMessages))
                    .apply();
        }
    }

    /**
     * Removes a message string from {@link SharedPreferences}.
     * @param context The context.
     * @param message The Message whose payload (as string) is removed from SharedPreferences.
     */
    static void removeLostMessage(Context context, Message message) {
        ArrayList<String> cachedMessages = new ArrayList<>(getCachedMessages(context));
        cachedMessages.remove(new String(message.getContent()));
        getSharedPreferences(context)
                .edit()
                .putString(KEY_CACHED_MESSAGES, new Gson().toJson(cachedMessages))
                .apply();
    }

    /**
     * Gets the SharedPReferences object that is used for persisting data in this application.
     *
     * @param context The context.
     * @return The single {@link SharedPreferences} instance that can be used to retrieve and modify
     *         values.
     */
    static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(
                context.getApplicationContext().getPackageName(),
                Context.MODE_PRIVATE);
    }
}