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
/*
 * This file demonstrates,
 * - How to use Nearby Connection features in C++ code with native client,
 * including
 *   - Create nearby connection interface
 *   - Set up connections with other devices
 *   - Handle message sending and receiving
 *   - Layout simple UI
 */

/*
 * Include files
 */
#include "NearbyNativeActivity.h"

const int SCORE_STRING_LEN = 32;

/*
 * PlayGame(): Start up game a new game. put it on UI thread since it is UI
 * activity
 */
void Engine::PlayGame() {
  ndk_helper::JNIHelper::GetInstance()->RunOnUiThread([this]() {
    LOGI("Playing match");
    if (dialog_) delete dialog_;

    // Start game
    InitializeGame();

    //
    // Using jui_helper, a support library, to create and bind gameplay buttons.
    //
    dialog_ = new jui_helper::JUIDialog(app_->activity);

    // Setting up labels
    time_text_ = new jui_helper::JUITextView("Time Left: 0:00");
    time_text_->AddRule(jui_helper::LAYOUT_PARAMETER_ALIGN_PARENT_TOP,
                        jui_helper::LAYOUT_PARAMETER_TRUE);
    time_text_->AddRule(jui_helper::LAYOUT_PARAMETER_CENTER_IN_PARENT,
                        jui_helper::LAYOUT_PARAMETER_TRUE);
    time_text_->SetAttribute("TextSize", jui_helper::ATTRIBUTE_UNIT_SP, 18.f);
    time_text_->SetAttribute("Padding", 10, 10, 10, 10);

    // Adding formula Text
    math_formula_ = new jui_helper::JUITextView("10 + 5 = ?");
    math_formula_->AddRule(jui_helper::LAYOUT_PARAMETER_BELOW, time_text_);
    math_formula_->AddRule(jui_helper::LAYOUT_PARAMETER_CENTER_IN_PARENT,
                           jui_helper::LAYOUT_PARAMETER_TRUE);
    math_formula_->SetAttribute("TextSize", jui_helper::ATTRIBUTE_UNIT_SP,
                                24.f);
    math_formula_->SetAttribute("Padding", 10, 10, 10, 10);

    // Adding Multiple Choice Buttons
    jui_helper::JUIButton *button = CreateChoiceButton("A", NULL);
    if (button) {
      button->AddRule(jui_helper::LAYOUT_PARAMETER_ALIGN_PARENT_LEFT,
                      jui_helper::LAYOUT_PARAMETER_TRUE);
      button->SetMargins(40, 0, 0, 0);  // todo: make it adaptive
    }
    game_buttons_[0] = button;
    for (int i = 1; i < CHOICES_PER_QUESTION; i++) {
      std::string cap(1, 'A' + i);
      button = CreateChoiceButton(cap.c_str(), game_buttons_[i - 1]);
      game_buttons_[i] = button;
    }

    const int32_t labelWidth = 600;
    const int32_t labelHeight = 300;
    scores_text_ = new jui_helper::JUITextView("0:00");
    scores_text_->AddRule(jui_helper::LAYOUT_PARAMETER_BELOW,
                          game_buttons_[CHOICES_PER_QUESTION - 1]);
    scores_text_->AddRule(jui_helper::LAYOUT_PARAMETER_CENTER_IN_PARENT,
                          jui_helper::LAYOUT_PARAMETER_TRUE);
    scores_text_->SetAttribute("TextSize", jui_helper::ATTRIBUTE_UNIT_SP, 18.f);
    scores_text_->SetAttribute("MinimumWidth", labelWidth);
    scores_text_->SetAttribute("MinimumHeight", labelHeight);
    scores_text_->SetAttribute("Padding", 10, 10, 10, 10);

    UpdateScoreBoardUI(false);

    dialog_->AddView(math_formula_);

    std::for_each(
        game_buttons_, game_buttons_ + CHOICES_PER_QUESTION,
        [this](jui_helper::JUIButton *button) { dialog_->AddView(button); });
    PlayOneRound();

    dialog_->AddView(time_text_);
    dialog_->AddView(scores_text_);

    dialog_->SetAttribute("Title", "Select an answer");
    dialog_->SetCallback(
        jui_helper::JUICALLBACK_DIALOG_DISMISSED,
        [this](jui_helper::JUIDialog *dialog, const int32_t message) {
          LOGI("Dialog dismissed");
          LeaveGame();
          dialog_ = nullptr;
        });

    dialog_->Show();

    //
    // Invoke time counter periodically
    //
    std::thread([this]() {
      ndk_helper::JNIHelper &helper = *ndk_helper::JNIHelper::GetInstance();
      helper.AttachCurrentThread();
      while (playing_ && game_time_ <= GAME_DURATION) {
        // Update game UI, UI update needs to be performed in UI thread
        ndk_helper::JNIHelper::GetInstance()->RunOnUiThread([this]() {
          char str[SCORE_STRING_LEN];
          std::lock_guard<std::mutex> lock(mutex_);
          snprintf(str, SCORE_STRING_LEN, "Time Left: %d",
                   GAME_DURATION - this->game_time_);
          time_text_->SetAttribute("Text", const_cast<const char *>(str));
        });

        // maintain our private clock
        std::this_thread::sleep_for(std::chrono::milliseconds(1000));
        game_time_ += 1;
      }

      // finish game
      playing_ = false;
      ndk_helper::JNIHelper::GetInstance()->RunOnUiThread([this]() {
        std::lock_guard<std::mutex> lock(mutex_);
        for (int i = 0; i < CHOICES_PER_QUESTION; i++) {
          game_buttons_[i]->SetAttribute("Enabled", false);
        }
        math_formula_->SetAttribute("Enabled", false);
      });

      BroadcastScore(score_counter_, true);

      UpdateScoreBoardUI(true);
      helper.DetachCurrentThread();
    }).detach();
  });
}

/*
 * UpdateScoreBoardUI()
 *  Update game score UI when some player's score changed [ mine or other
 * players
 *  from remote ends]. Scores are already saved in our own cache, just pull out
 * to display
 */
void Engine::UpdateScoreBoardUI(bool UIThreadRequired) {
  // Lock mutex since this one can be called from multiple thread,
  // gpg callback tread and UI callback thread
  std::lock_guard<std::mutex> lock(mutex_);
  if (dialog_ == nullptr) return;

  const int32_t SCORE_SIZE = 64;
  char str[SCORE_SIZE];
  snprintf(str, SCORE_SIZE, "%03d", score_counter_);
  std::string str_myscore(str);

  snprintf(str, SCORE_SIZE, "My score: %03d %s\n", score_counter_,
           playing_ ? "" : "*");
  std::string allstr(str);

  RetrieveScores(allstr);
  if (!UIThreadRequired) {
    scores_text_->SetAttribute("Text",
                               const_cast<const char *>(allstr.c_str()));
    return;
  }

  ndk_helper::JNIHelper::GetInstance()->RunOnUiThread(
      [this, str_myscore, allstr]() {
        scores_text_->SetAttribute("Text",
                                   const_cast<const char *>(allstr.c_str()));
      });
}

/*
 * Initialize game state
 */
void Engine::InitializeGame() {
  std::lock_guard<std::mutex> lock(mutex_);
  playing_ = true;
  game_time_ = 0;
  score_counter_ = 0;
}

/*
 * Leave game
 */
void Engine::LeaveGame() {
  std::lock_guard<std::mutex> lock(mutex_);
  LOGI("Game is over");
  playing_ = false;
}

/*
 * Initialize game management UI,
 * invoking jui_helper functions to create java UIs
 */
void Engine::InitUI() {

  const int32_t LEFT_MARGIN = 20;

  // The window initialization
  jui_helper::JUIWindow::Init(app_->activity, JUIHELPER_CLASS_NAME);

  // Using jui_helper, a support library, to create and bind game management UIs
  int32_t win_width = ANativeWindow_getWidth(app_->window);
  int32_t win_height = ANativeWindow_getHeight(app_->window);

  if (win_height <= 0 || win_width <= 0) {
    LOGE("Failed to get native window size");
    return;
  }
  if (win_height > win_width) {
    int32_t tmp = win_width;
    win_width = win_height;
    win_height = tmp;
  }

  int32_t button_raw_width = win_width / 4;  // we have 4 buttons
  int32_t button_height = win_height / 4;
  int cur_idx = 0;

  // Create 4 buttons to control nearby sign-in
  // The sequence is dictated by enum BUTTON_INDEX,
  // it MUST match the button titles array defined here
  const char *titles[UI_BUTTON_COUNT] = {"Advertise", "Discover", "Play Game",
                                         "Stop"};
  std::function<void(jui_helper::JUIView *, const int32_t)> button_handlers[] = {
          [this](jui_helper::JUIView *button, const int32_t msg) {
            if (msg == jui_helper::JUICALLBACK_BUTTON_UP) {
              OnAdvertiseButtonClick();
            }
          },
          [this](jui_helper::JUIView *button, const int32_t msg) {
            if (msg == jui_helper::JUICALLBACK_BUTTON_UP) {
              OnDiscoverButtonClick();
            }
          },
          [this](jui_helper::JUIView *button, const int32_t msg) {
            if (msg == jui_helper::JUICALLBACK_BUTTON_UP) {
              OnPlayButtonClick();
            }
          },
          [this](jui_helper::JUIView *button, const int32_t msg) {
            if (msg == jui_helper::JUICALLBACK_BUTTON_UP) {
              OnStopButtonClick();
            }
          },
      };

  for (cur_idx = 0; cur_idx < UI_BUTTON_COUNT; cur_idx++) {
    jui_helper::JUIButton *button = new jui_helper::JUIButton(titles[cur_idx]);
    button->AddRule(jui_helper::LAYOUT_PARAMETER_CENTER_VERTICAL,
                    jui_helper::LAYOUT_PARAMETER_TRUE);
    button->AddRule(jui_helper::LAYOUT_PARAMETER_ALIGN_PARENT_LEFT,
                    jui_helper::LAYOUT_PARAMETER_TRUE);
    button->SetAttribute("MinimumWidth", button_raw_width - LEFT_MARGIN);
    button->SetAttribute("MinimumHeight", button_height);
    button->SetMargins(LEFT_MARGIN + cur_idx * button_raw_width, 0, 0, 0);
    button->SetCallback(button_handlers[cur_idx]);
    jui_helper::JUIWindow::GetInstance()->AddView(button);
    ui_buttons_[cur_idx] = button;
  }

  status_text_ = new jui_helper::JUITextView("Nearby Connection is Idle");
  status_text_->AddRule(jui_helper::LAYOUT_PARAMETER_ALIGN_PARENT_BOTTOM,
                        jui_helper::LAYOUT_PARAMETER_TRUE);
  status_text_->AddRule(jui_helper::LAYOUT_PARAMETER_CENTER_IN_PARENT,
                        jui_helper::LAYOUT_PARAMETER_TRUE);
  status_text_->SetAttribute("TextSize", jui_helper::ATTRIBUTE_UNIT_SP, 17.f);
  jui_helper::JUIWindow::GetInstance()->AddView(status_text_);

  // Init nearby connections...
  std::thread([this]() {
    ndk_helper::JNIHelper &helper = *ndk_helper::JNIHelper::GetInstance();
    helper.AttachCurrentThread();

    InitGoogleNearbyConnection();

    helper.DetachCurrentThread();
  }).detach();

  EnableUI(true);
  return;
}

/*
 * Enable/Disable management UI
 */
void Engine::EnableUI(bool enable) {
  LOGI("Updating UI:%d", enable);
  ndk_helper::JNIHelper::GetInstance()->RunOnUiThread([this, enable]() {
    ui_buttons_[BUTTON_ADVERTISE]->SetAttribute(
        "Enabled",
        enable && !(nbc_state_ & nearby_connection_state::ADVERTISING));
    ui_buttons_[BUTTON_DISCOVER]->SetAttribute(
        "Enabled",
        enable && !(nbc_state_ & nearby_connection_state::DISCOVERING));
    ui_buttons_[BUTTON_PLAY_GAME]->SetAttribute(
        "Enabled", enable && (nbc_state_ & nearby_connection_state::CONNECTED));
    /*
     * For experimental purpose, Stop button is always enabled
     */
    ui_buttons_[BUTTON_STOP]->SetAttribute("Enabled", true);

    std::string str;
    str += "Nearby Connection Status: Connected Clients = ";
    str += std::to_string(CountConnections());
    str += "; ";
    if (nbc_state_ & nearby_connection_state::IDLE) {
      str += "Currently Idle; ";
    }
    if (nbc_state_ & nearby_connection_state::ADVERTISING) {
      str += "In Advertising; ";
    }
    if (nbc_state_ & nearby_connection_state::DISCOVERING) {
      str += "In Discovering; ";
    }
    if (nbc_state_ & nearby_connection_state::FAILED) {
      str += "FAILED; ";
    }
    str = str.substr(0, str.size() - 2);
    status_text_->SetAttribute("Text", const_cast<const char *>(str.c_str()));
  });
}

/*
 * Help function to create(multiple choice buttons)
 */
jui_helper::JUIButton *Engine::CreateChoiceButton(const char *cap,
                                                  jui_helper::JUIButton *left,
                                                  float fontSize) {
  jui_helper::JUIButton *button = new jui_helper::JUIButton(cap);
  if (!button) {
    LOGE("Out of Memory in %s @ line %d", __FILE__, __LINE__);
    return NULL;
  }
  button->SetCallback([this](jui_helper::JUIView *view, const int32_t msg) {
    switch (msg) {
      case jui_helper::JUICALLBACK_BUTTON_UP:
        if (!playing_) return;
        CheckChoice(static_cast<jui_helper::JUIButton *>(view));
        PlayOneRound();
    }
  });
  if (left) button->AddRule(jui_helper::LAYOUT_PARAMETER_RIGHT_OF, left);
  button->AddRule(jui_helper::LAYOUT_PARAMETER_BELOW, math_formula_);
  button->SetAttribute("TextSize", jui_helper::ATTRIBUTE_UNIT_SP, fontSize);
  button->SetAttribute("Padding", 2, 5, 2, 5);
  button->SetMargins(0, 0, 0, 0);
  return button;
}

/*
 * Play one round of game: generate a questions and config them to UI
 */
bool Engine::PlayOneRound(void) {
  const int32_t CHOICE_LEN = 16;
  math_formula_->SetAttribute("Text", game_->GetQuestion());
  const int *allChoices = game_->GetAllChoices();
  for (int i = 0; i < game_->GetChoicesPerQuestion(); i++) {
    char choice[CHOICE_LEN];
    snprintf(choice, CHOICE_LEN, "%d", allChoices[i]);
    game_buttons_[i]->SetAttribute("Text", (const char*)choice);
    game_buttons_[i]->SetAttribute("Enabled", true);
  }
  return true;
}
/*
 * Check the selection is the correct answer and update our local score
 */
void Engine::CheckChoice(jui_helper::JUIButton *selection) {
  int idx = 0;
  while (idx < CHOICES_PER_QUESTION && game_buttons_[idx] != selection) {
    ++idx;
  }
  const int *allChoices = game_->GetAllChoices();
  if (allChoices[idx] == allChoices[game_->GetCorrectChoice()]) {
    // Update my own UI score
    ++score_counter_;

    // Broadcast this new score to other players
    BroadcastScore(score_counter_, false);
    UpdateScoreBoardUI(true);
  }
}

/*
 * JNI functions those manage activity lifecycle
 */
extern "C" {
/*
 * These callbacks are necessary to work Google Play Game Services UIs properly
 *
 * For apps which target Android 2.3 or 3.x devices (API Version prior to 14),
 * Play Game Services has no way to automatically receive Activity lifecycle
 * callbacks. In these cases, Play Game Services relies on the owning Activity
 * to notify it of lifecycle events. Any Activity which owns a GameServices
 * object should call the AndroidSupport::* functions from within their own
 * lifecycle callback functions. The arguments in these functions match those
 * provided by Android, so no additional processing is necessary.
 */
JNIEXPORT void
Java_com_google_example_games_nearbyconnections_NearbyNativeActivity_nativeOnActivityResult(
    JNIEnv *env, jobject thiz, jobject activity, jint requestCode,
    jint resultCode, jobject data) {
  gpg::AndroidSupport::OnActivityResult(env, activity, requestCode, resultCode,
                                        data);
}

JNIEXPORT void
Java_com_google_example_games_nearbyconnections_NearbyNativeActivity_nativeOnActivityCreated(
    JNIEnv *env, jobject thiz, jobject activity, jobject saved_instance_state) {
  gpg::AndroidSupport::OnActivityCreated(env, activity, saved_instance_state);
}

JNIEXPORT void
Java_com_google_example_games_nearbyconnections_NearbyNativeActivity_nativeOnActivityDestroyed(
    JNIEnv *env, jobject thiz, jobject activity) {
  gpg::AndroidSupport::OnActivityDestroyed(env, activity);
}

JNIEXPORT void
Java_com_google_example_games_nearbyconnections_NearbyNativeActivity_nativeOnActivityPaused(
    JNIEnv *env, jobject thiz, jobject activity) {
  gpg::AndroidSupport::OnActivityPaused(env, activity);
}

JNIEXPORT void
Java_com_google_example_games_nearbyconnections_NearbyNativeActivity_nativeOnActivityResumed(
    JNIEnv *env, jobject thiz, jobject activity) {
  gpg::AndroidSupport::OnActivityResumed(env, activity);
}

JNIEXPORT void
Java_com_google_example_games_nearbyconnections_NearbyNativeActivity_nativeOnActivitySaveInstanceState(
    JNIEnv *env, jobject thiz, jobject activity, jobject out_state) {
  gpg::AndroidSupport::OnActivitySaveInstanceState(env, activity, out_state);
}

JNIEXPORT void
Java_com_google_example_games_nearbyconnections_NearbyNativeActivity_nativeOnActivityStarted(
    JNIEnv *env, jobject thiz, jobject activity) {
  gpg::AndroidSupport::OnActivityStarted(env, activity);
}

JNIEXPORT void
Java_com_google_example_games_nearbyconnections_NearbyNativeActivity_nativeOnActivityStopped(
    JNIEnv *env, jobject thiz, jobject activity) {
  gpg::AndroidSupport::OnActivityStopped(env, activity);
}
}
