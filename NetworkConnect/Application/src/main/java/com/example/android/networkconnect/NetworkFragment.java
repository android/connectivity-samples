/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.networkconnect;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.HandlerThread;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

/**
 * Implementation of headless Fragment that runs an AsyncTask to fetch data from the network.
 */
public class NetworkFragment extends Fragment {
    public static final String TAG = "NetworkFragment";

    private static final String URL_KEY = "UrlKey";

    private DownloadCallback mCallback;
    private String mUrlString;
    private HandlerThread mNetworkHandlerThread;
    private NetworkHandler mNetworkHandler;

    /**
     * Static initializer for NetworkFragment that sets the URL of the host it will be downloading
     * from.
     */
    public static NetworkFragment getInstance(FragmentManager fragmentManager, String url) {
        // Recover NetworkFragment in case we are re-creating the Activity due to a config change.
        // This is necessary because NetworkFragment might have a task that began running before
        // the config change and has not finished yet.
        // The NetworkFragment is recoverable via this method because it calls
        // setRetainInstance(true) upon creation.
        NetworkFragment networkFragment = (NetworkFragment) fragmentManager
                .findFragmentByTag(NetworkFragment.TAG);
        if (networkFragment == null) {
            networkFragment = new NetworkFragment();
            Bundle args = new Bundle();
            args.putString(URL_KEY, url);
            networkFragment.setArguments(args);
            fragmentManager.beginTransaction().add(networkFragment, TAG).commit();
        }
        return networkFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retain this Fragment across configuration changes in the host Activity.
        setRetainInstance(true);
        mUrlString = getArguments().getString(URL_KEY);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // Host Activity will handle callbacks from task.
        mCallback = (DownloadCallback)context;
        mNetworkHandlerThread = new HandlerThread("mNetworkHandlerThread");
        mNetworkHandlerThread.start();
        mNetworkHandler = new NetworkHandler(mNetworkHandlerThread.getLooper(), mCallback);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mNetworkHandler.removeMessages(NetworkHandler.DOWNLOAD_URL);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mNetworkHandlerThread.quitSafely();
        } else {
            mNetworkHandlerThread.quit();
        }
        mNetworkHandler = null;
        mNetworkHandlerThread = null;
        // Clear reference to host Activity.
        mCallback = null;
    }

    @Override
    public void onDestroy() {
        // Cancel task when Fragment is destroyed.
        cancelDownload();
        super.onDestroy();
    }

    /**
     * Start non-blocking execution of DownloadTask.
     */
    public void startDownload() {
        cancelDownload();
        mNetworkHandler.createDownload(mUrlString).sendToTarget();
    }

    /**
     * Cancel all pending download requests
     */
    public void cancelDownload() {
        mNetworkHandler.removeMessages(NetworkHandler.DOWNLOAD_URL);
    }
}
