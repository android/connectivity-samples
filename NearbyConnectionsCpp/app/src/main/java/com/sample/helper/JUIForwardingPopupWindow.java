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

import android.app.Activity;
import android.app.NativeActivity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

/*
 * Popup window that forward user inputs to underlying widgets
 */
class JUIForwardingLayout extends LinearLayout {
    View target;

    public JUIForwardingLayout(Context context) {
        super(context);
    }

    public JUIForwardingLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setTarget(View view) {
        target = view;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (target != null)
            target.dispatchTouchEvent(ev);
        return true;
    }
}

public class JUIForwardingPopupWindow extends PopupWindow {
    JUIForwardingPopupWindow(final Activity activity, final View view) {
        super(view.getWidth(), view.getHeight());

        JUIForwardingLayout dummyRelativeLayout = new JUIForwardingLayout(
                activity);
        dummyRelativeLayout.setTarget(view);
        setContentView(dummyRelativeLayout);

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Window window = activity.getWindow();
                if( window != null )
                {
                    View decor = window.getDecorView();
                    if( decor != null )
                    {
                        showAtLocation(decor, Gravity.TOP
                                | Gravity.START, view.getLeft(), view.getTop());
                    }
                }

            }
        });
    }

    void update(View view) {
        int[] location = new int[2];
        view.getLocationInWindow(location);
        super.update(location[0], location[1], view.getWidth(),
                view.getHeight());
    }
}
