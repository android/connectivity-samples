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
import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.appcompat.widget.AppCompatButton;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

//
//Java UI SeekBar implementation
//
public class JUIButton extends AppCompatButton {
    private JUIForwardingPopupWindow dummyPopupWindow;

    public JUIForwardingPopupWindow getDummyWindow() {
        return dummyPopupWindow;
    }

    public JUIButton(Context context) {
        this(context, null, android.R.attr.buttonStyle);
    }

    public JUIButton(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.buttonStyle);
    }

    public JUIButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        final Drawable d = getBackground();

        setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.i("test", "clicked, action" + event.getAction());
                switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // setPressed(true);
                    d.setState(PRESSED_ENABLED_STATE_SET);

                    JUIHelper.JUICallbackHandler(getId(),
                            JUIHelper.JUICALLBACK_BUTTON_DOWN, 0, 0);
                    return true;
                    // break;
                case MotionEvent.ACTION_CANCEL:
                    JUIHelper.JUICallbackHandler(getId(),
                            JUIHelper.JUICALLBACK_BUTTON_CANCEL, 0, 0);
                    return true;
                case MotionEvent.ACTION_UP:
                    JUIHelper.JUICallbackHandler(getId(),
                            JUIHelper.JUICALLBACK_BUTTON_UP, 0, 0);
                    d.setState(ENABLED_STATE_SET);

                    // setPressed(false);
                    // break;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    d.setState(PRESSED_ENABLED_STATE_SET);
                    return true;

                }

                return false;
            }
        });

        dummyPopupWindow = new JUIForwardingPopupWindow((Activity)context, this);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right,
            int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        // Put dummy popupWindow over the control
        // so that relativeLayout can pass through touch events to native
        // activity for a background area
        if (changed) {
            dummyPopupWindow.update(this);
        }
    }
}
