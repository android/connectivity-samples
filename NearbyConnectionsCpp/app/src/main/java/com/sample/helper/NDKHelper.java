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
package com.sample.helper;

import android.annotation.TargetApi;
import android.app.NativeActivity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class NDKHelper {

    public NDKHelper(NativeActivity act) {
        activity = act;
    }

    public void loadLibrary(String soname) {
        if (soname.isEmpty() == false) {
            System.loadLibrary(soname);
            loadedSO = true;
        }
    }

    public static Boolean checkSOLoaded() {
        if (loadedSO == false) {
            Log.e("NDKHelper",
                "--------------------------------------------\n"
             +  ".so has not been loaded. To use JUI helper, please initialize with \n"
             +  "NDKHelper::Init( ANativeActivity* activity, const char* helper_class_name, const char* native_soname);\n"
             +  "--------------------------------------------\n");
            return false;
        } else
            return true;
    }

    private static boolean loadedSO = false;
    NativeActivity activity;

    public String getApplicationName() {
        final PackageManager pm = activity.getPackageManager();
        ApplicationInfo ai;
        try {
            ai = pm.getApplicationInfo(activity.getPackageName(), 0);
        } catch (final NameNotFoundException e) {
            ai = null;
        }
        String applicationName = (String) (ai != null ? pm
                .getApplicationLabel(ai) : "(unknown)");        
        return applicationName;
    }

    public String getNearbyConnectionServiceID() {
      String nb_id = "unkown_id";
      try {
        ApplicationInfo ai = activity.getPackageManager().getApplicationInfo(activity.getPackageName(), PackageManager.GET_META_DATA);
        Bundle bundle = ai.metaData;
        nb_id = bundle.getString("com.google.android.gms.nearby.connection.SERVICE_ID");
      } catch (NameNotFoundException e) {
          Log.e("NDKHelper", "Failed to load meta-data, NameNotFound: " + e.getMessage());
      } catch (NullPointerException e) {
        Log.e("NDKHelper", "Failed to load meta-data, NullPointer: " + e.getMessage());
      }
      return nb_id;
    }
    /*
     * Helper to execute function in UIThread
     */
    public void runOnUIThread(final long p) {
        if (checkSOLoaded()) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    RunOnUiThreadHandler(p);
                }
            });
        }
        return;
    }

    /*
     * Native code helper for RunOnUiThread
     */
    native public void RunOnUiThreadHandler(long pointer);
}
