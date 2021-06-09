/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.example.games.nearbyconnections;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NativeActivity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import android.view.View;
import android.widget.Toast;

public class NearbyNativeActivity extends NativeActivity {
    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 5444;

    // Load SO
    static {
        System.loadLibrary("NearbyNativeActivity");
    }

    /**
     * These permissions are required before connecting to Nearby Connections. Only {@link
     * Manifest.permission#ACCESS_COARSE_LOCATION} is considered dangerous, so the others should be
     * granted just by having them in our AndroidManfiest.xml
     */
    private static final String[] REQUIRED_PERMISSIONS =
            new String[] {
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Hide toolbar
        int SDK_INT = android.os.Build.VERSION.SDK_INT;
        if (SDK_INT >= 19) {
            setImmersiveSticky();

            View decorView = getWindow().getDecorView();
            decorView
                    .setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
                        @Override
                        public void onSystemUiVisibilityChange(int visibility) {
                            setImmersiveSticky();
                        }
                    });
        }

        nativeOnActivityCreated(this, savedInstanceState);
    }

    @Override
    @SuppressLint("InlinedApi")
    @SuppressWarnings("deprecation")
    protected void onResume() {
        super.onResume();

        // Hide toolbar
        int SDK_INT = android.os.Build.VERSION.SDK_INT;
        if (SDK_INT >= 14 && SDK_INT < 19) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LOW_PROFILE);
        } else if (SDK_INT >= 19) {
            setImmersiveSticky();
        }

        nativeOnActivityResumed(this);
    }

    @SuppressLint("InlinedApi")
    void setImmersiveSticky() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // This call is to suppress 'E/WindowManager():
        // android.view.WindowLeaked...' errors.
        // Since orientation change events in NativeActivity comes later than
        // expected, we can not dismiss
        // popupWindow gracefully from NativeActivity.
        // So we are releasing popupWindows explicitly triggered from Java
        // callback through JNI call.
        OnPauseHandler();

        nativeOnActivityPaused(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        nativeOnActivityDestroyed(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!hasPermissions(this, REQUIRED_PERMISSIONS)) {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_REQUIRED_PERMISSIONS);
        }
            nativeOnActivityStarted(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        nativeOnActivityStopped(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        nativeOnActivitySaveInstanceState(this, outState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        nativeOnActivityResult(this, requestCode,resultCode, data);
    }

    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_REQUIRED_PERMISSIONS) {
            for (int grantResult : grantResults) {
                if (grantResult == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(this, R.string.error_missing_permissions, Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
            }
            recreate();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
    // Implemented in C++.
    public static native void nativeOnActivityResult(Activity activity,
                                                     int requestCode, int resultCode, Intent data);

    public static native void nativeOnActivityCreated(Activity activity,
                                                      Bundle savedInstanceState);

    private static native void nativeOnActivityDestroyed(Activity activity);

    private static native void nativeOnActivityPaused(Activity activity);

    private static native void nativeOnActivityResumed(Activity activity);

    private static native void nativeOnActivitySaveInstanceState(
            Activity activity, Bundle outState);

    private static native void nativeOnActivityStarted(Activity activity);

    private static native void nativeOnActivityStopped(Activity activity);

    native public void OnPauseHandler();

    /** @return True if the app was granted all the permissions. False otherwise. */
    public static boolean hasPermissions(Context context, String... permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}
