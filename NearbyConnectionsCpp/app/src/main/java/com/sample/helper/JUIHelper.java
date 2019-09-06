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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.LinkedList;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.app.NativeActivity;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;

@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class JUIHelper {

    public JUIHelper(NativeActivity act) {
        activity_ = act;
    }

    private NativeActivity activity_;

    //
    // JUI related helpers
    //
    LinkedList<PopupWindow> dummyWindows_ = new LinkedList<PopupWindow>();
    RelativeLayout JUIRelativeLayout_;
    public static final int JUICALLBACK_SEEKBAR_STOP_TRACKING_TOUCH = 1;
    public static final int JUICALLBACK_SEEKBAR_START_TRACKING_TOUCH = 2;
    public static final int JUICALLBACK_SEEKBAR_PROGRESSCHANGED = 3;
    public static final int JUICALLBACK_COMPOUNDBUTTON_CHECKEDCHANGED = 4;
    public static final int JUICALLBACK_BUTTON_DOWN = 5;
    public static final int JUICALLBACK_BUTTON_UP = 6;
    public static final int JUICALLBACK_BUTTON_CANCEL = 7;
    public static final int JUICALLBACK_DIALOG_DISMISSED = 108;
    public static final int JUICALLBACK_DIALOG_CANCELLED = 109;

    native static public void JUICallbackHandler(int id, int message,
            int param1, int pram2);

    private void initializeWidget(final View view) {
        activity_.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Check if the control has dummy popupwindow to forward user
                // inputs
                try {
                    Method method = view.getClass().getMethod("getDummyWindow");
                    JUIForwardingPopupWindow window = (JUIForwardingPopupWindow) method
                            .invoke(view);
                    if (window != null)
                        dummyWindows_.add(window);
                } catch (NoSuchMethodException e) {
                    return;
                } catch (Exception e) {
                    return;
                }

                // Set layoutParameter for Widget
                RelativeLayout.LayoutParams relativeParams = new RelativeLayout.LayoutParams(
                        LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                view.setLayoutParams(relativeParams);
            }
        });
    }

    public View createWidget(String className, int id) {
        View view;
        try {
            String baseName = getClass().getName().substring(0,
                    getClass().getName().lastIndexOf(".") + 1);
            @SuppressWarnings("rawtypes")
            Class cls = Class.forName(baseName + className);
            @SuppressWarnings("unchecked")
            Constructor<View> ctor = cls.getConstructor(NativeActivity.class);
            view = ctor.newInstance(activity_);
            view.setId(id);
        } catch (Exception e) {
            Log.e("NDKHelper", "Could not find the name");
            return null;
        }

        initializeWidget(view);
        return view;
    }

    public View createWidget(String className, int id, int param) {
        View view;
        try {
            String baseName = getClass().getName().substring(0,
                    getClass().getName().lastIndexOf(".") + 1);
            @SuppressWarnings("rawtypes")
            Class cls = Class.forName(baseName + className);
            @SuppressWarnings("unchecked")
            Constructor<View> ctor = cls.getConstructor(NativeActivity.class,
                    int.class);
            view = ctor.newInstance(activity_, param);
            view.setId(id);
        } catch (Exception e) {
            return null;
        }

        initializeWidget(view);
        return view;
    }

    public void closeWidget(final View view) {
        JUIForwardingPopupWindow window = null;
        // Check if the control has dummy popupwindow to forward user inputs
        try {
            Method method = view.getClass().getMethod("getDummyWindow");
            window = (JUIForwardingPopupWindow) method.invoke(view);
        } catch (Exception e) {
        }

        final PopupWindow closingWindow = window;
        activity_.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (closingWindow != null) {
                    dummyWindows_.remove(closingWindow);
                    closingWindow.dismiss();
                }
                JUIRelativeLayout_.removeView(view);
            }
        });
        return;
    }

    public void addRule(final View view, final int param1, final int param2) {
        activity_.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                RelativeLayout.LayoutParams relativeParams = (RelativeLayout.LayoutParams) view
                        .getLayoutParams();
                relativeParams.addRule(param1, param2);
            }
        });
    }

    public void setLayoutParams(final View view, final int width,
            final int height) {
        activity_.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ViewGroup.LayoutParams params = view.getLayoutParams();
                params.width = width;
                params.height = height;
            }
        });
    }

    public void setMargins(final View view, final int left, final int top,
            final int right, final int bottom) {
        activity_.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view
                        .getLayoutParams();
                params.setMargins(left, top, right, bottom);
            }
        });
    }

    public void setLayoutParams(final View view, final int width,
            final int height, final float weight) {
        activity_.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ViewGroup.LayoutParams params = view.getLayoutParams();

                if (params instanceof LinearLayout.LayoutParams) {
                    LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) params;
                    layoutParams.width = width;
                    layoutParams.height = height;
                    layoutParams.weight = weight;
                } else {
                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                            width, height, weight);
                    view.setLayoutParams(layoutParams);
                }
            }
        });
    }

    public void addView(final View view) {
        activity_.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (JUIRelativeLayout_ != null) {
                    JUIRelativeLayout_.addView(view);
                    ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view
                            .getLayoutParams();

                    if (params instanceof RelativeLayout.LayoutParams == false) {
                        // Switching to relative layout param
                        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                                params.width, params.height);
                        layoutParams.leftMargin = params.leftMargin;
                        layoutParams.bottomMargin = params.bottomMargin;
                        layoutParams.rightMargin = params.rightMargin;
                        layoutParams.topMargin = params.topMargin;
                        view.setLayoutParams(layoutParams);
                    }
                }
            }
        });
        return;
    }

    public void addView(final ViewGroup layout, final View view) {
        activity_.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                layout.addView(view);

                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view
                        .getLayoutParams();
                if (layout instanceof RadioGroup) {
                    if (params instanceof RadioGroup.LayoutParams == false) {
                        // Switching to linear layout param
                        RadioGroup.LayoutParams layoutParams = new RadioGroup.LayoutParams(
                                params.width, params.height);
                        layoutParams.leftMargin = params.leftMargin;
                        layoutParams.bottomMargin = params.bottomMargin;
                        layoutParams.rightMargin = params.rightMargin;
                        layoutParams.topMargin = params.topMargin;
                        view.setLayoutParams(layoutParams);
                    }
                } else if (layout instanceof LinearLayout) {
                    if (params instanceof LinearLayout.LayoutParams == false) {
                        // Switching to linear layout param
                        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                                params.width, params.height);
                        layoutParams.leftMargin = params.leftMargin;
                        layoutParams.bottomMargin = params.bottomMargin;
                        layoutParams.rightMargin = params.rightMargin;
                        layoutParams.topMargin = params.topMargin;
                        view.setLayoutParams(layoutParams);
                    }
                } else if (layout instanceof RelativeLayout) {
                    if (params instanceof RelativeLayout.LayoutParams == false) {
                        // Switching to linear layout param
                        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                                params.width, params.height);
                        layoutParams.leftMargin = params.leftMargin;
                        layoutParams.bottomMargin = params.bottomMargin;
                        layoutParams.rightMargin = params.rightMargin;
                        layoutParams.topMargin = params.topMargin;
                        view.setLayoutParams(layoutParams);
                    }
                }
            }
        });
        return;
    }

    //
    // Create PopupWindow over nativeActivity with RelativeLayout
    //
    public PopupWindow createPopupWindow(final NativeActivity act) {
        // Check manifest settings if the activity wouldn't be destroyed when
        // the device orientation changes
        try {
            ActivityInfo info = act.getPackageManager().getActivityInfo(
                    act.getComponentName(), 0);
            if ((info.configChanges & ActivityInfo.CONFIG_ORIENTATION) == 0
                    || (info.configChanges & ActivityInfo.CONFIG_SCREEN_SIZE) == 0) {
                Log.i("NDKHelper",
                        "Activity does not have android:configChanges='orientation|screenSize' attributes in AndroidManifest.xml.");
            }
        } catch (NameNotFoundException e) {
          Log.e("NDKHelper", "Failed to find ActivityName");
        }

        activity_ = act;
        // activity.setTheme(android.R.style.Theme_DeviceDefault);

        final PopupWindow popupWindow = new PopupWindow(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

        activity_.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Window window = activity_.getWindow();
                if( window != null )
                {
                    View decorView = window.getDecorView();
                    if( decorView == null )
                    {
                        // Put dummy layout to NativeActivity
                        LinearLayout mainLayout = new LinearLayout(activity_);
                        MarginLayoutParams params = new MarginLayoutParams(
                                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
                        params.setMargins(0, 0, 0, 0);
                        activity_.setContentView(mainLayout, params);
                        decorView = mainLayout;
                    }

                    // Setup relative layout
                    JUIRelativeLayout_ = new RelativeLayout(activity_);
                    popupWindow.setContentView(JUIRelativeLayout_);

                    // Show our UI over NativeActivity window
                    popupWindow.showAtLocation(decorView, Gravity.TOP
                            | Gravity.START, 0, 0);
                    popupWindow.setTouchable(false);
                    popupWindow.update();
                }
            }
        });
        return popupWindow;
    }

    public void suspendPopupWindow(final PopupWindow window) {
        activity_.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Dismiss dummy windows
                for (PopupWindow w : dummyWindows_) {
                    w.dismiss();
                }
                dummyWindows_.clear();
                window.dismiss();
                if (dialog_ != null)
                    dialog_.dismiss();

                JUIRelativeLayout_ = null;
            }
        });
        return;
    }

    public void resumePopupWindow(NativeActivity act, final PopupWindow p) {
        activity_ = act;
        if(p.isShowing()) {
            Log.i("JUIHelper::", "ResumePopupWindow is to about to show");
        }
        return;
    }

    public void closePopupWindow(final PopupWindow window) {
        activity_.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Dismiss dummy windows
                for (PopupWindow w : dummyWindows_) {
                    w.dismiss();
                }
                dummyWindows_.clear();
                window.dismiss();

                if (dialog_ != null)
                    dialog_.dismiss();
            }
        });
        return;
    }

    /*
     * Dialog helpers
     */
    Dialog dialog_;

    public Object createDialog(final NativeActivity act) {
        JUIDialog dlg = new JUIDialog(act);
        dialog_ = dlg;

        return dlg;
    }
}
