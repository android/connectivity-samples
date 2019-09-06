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

import android.view.View;
import android.widget.RelativeLayout;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.NativeActivity;
import android.content.DialogInterface;

/*
 * Java UI Dialog implementation
 */
public class JUIDialog extends Dialog {

    NativeActivity activity_;
    RelativeLayout dialogRelativeLayout_;
    int id_ = 0;

    public JUIDialog(final NativeActivity act) {
        super(act);
        activity_ = act;

        // Setup relative layout
        dialogRelativeLayout_ = new RelativeLayout(activity_);
        setContentView(dialogRelativeLayout_);

        setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                NDKHelper.checkSOLoaded();
                if (id_ != 0)
                    JUIHelper.JUICallbackHandler(id_,
                            JUIHelper.JUICALLBACK_DIALOG_DISMISSED, 0, 0);
            }
        });

        setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                NDKHelper.checkSOLoaded();
                if (id_ != 0)
                    JUIHelper.JUICallbackHandler(id_,
                            JUIHelper.JUICALLBACK_DIALOG_CANCELLED, 0, 0);
            }
        });
    }

    public void setID(int id) {
        id_ = id;
    }

    @SuppressLint("NewApi")
    public void addView(final View view) {
        activity_.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (dialogRelativeLayout_ != null)
                    dialogRelativeLayout_.addView(view);
            }
        });
        return;
    }

}
