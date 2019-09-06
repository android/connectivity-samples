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
 * Include files
 */
#include "NearbyNativeActivity.h"

const int SELECTION_PER_QUESTION = 4;
const int MATH_GRADE_LEVEL = 2;
/*
 * Ctor
 */
Engine::Engine()
    : initialized_resources_(false),
      has_focus_(false),
      app_(nullptr),
      dialog_(nullptr),
      status_text_(nullptr) {
  gl_context_ = ndk_helper::GLContext::GetInstance();
  game_ = new game_helper::GFKSimple(SELECTION_PER_QUESTION, MATH_GRADE_LEVEL);
  if (game_ == nullptr) {
    LOGE("Out of Memory in %s @line %d", __FILE__, __LINE__);
    return;
  }
  memset(ui_buttons_, 0, sizeof(ui_buttons_));
  memset(game_buttons_, 0, sizeof(game_buttons_));
  nbc_state_ = nearby_connection_state::IDLE;
}

/*
 * Dtor
 */
Engine::~Engine() {
  // Clean up nearby connection ( PIGGYBACK on button stop action )
  OnStopButtonClick();
  delete game_;
  jui_helper::JUIWindow::GetInstance()->Close();
  delete dialog_;
}

/**
 * Initialize an EGL context for the current display.
 */
void Engine::InitDisplay(const int32_t cmd) {
  if (!initialized_resources_) {
    gl_context_->Init(app_->window);
    InitUI();
    initialized_resources_ = true;
  } else {
    if (EGL_SUCCESS != gl_context_->Resume(app_->window)) {
      LOGE("Failed To Re-Initialize OpenGL functions");
    }

    jui_helper::JUIWindow::GetInstance()->Resume(app_->activity, cmd);
  }

  // Enable culling OpenGL state
  glEnable(GL_CULL_FACE);

  // Enabled depth test OpenGL state
  glEnable(GL_DEPTH_TEST);
  glDepthFunc(GL_LEQUAL);

  // Note that screen size might have been changed
  glViewport(0, 0, gl_context_->GetScreenWidth(),
             gl_context_->GetScreenHeight());
}

/**
 * Just the current frame in the display.
 */
void Engine::DrawFrame() {
  float bkColor = .5f;
  glClearColor(bkColor, bkColor, bkColor, 1.f);
  glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
  gl_context_->Swap();
}

/**
 * Tear down the EGL context currently associated with the display.
 */
void Engine::TermDisplay(const int32_t cmd) {
  gl_context_->Suspend();
  jui_helper::JUIWindow::GetInstance()->Suspend(cmd);
}

/**
 * HandleInput(): processing any gesture event we are interested in. For this
 * App, all event are handled on Java side
 */
int32_t Engine::HandleInput(android_app *app, AInputEvent *event) {
  return 0;
}

/**
 * Process the next main command.
 */
void Engine::HandleCmd(struct android_app *app, int32_t cmd) {
  Engine *eng = reinterpret_cast<Engine *> (app->userData);
  LOGI("message %d", cmd);
  switch (cmd) {
    case APP_CMD_INIT_WINDOW:
      if (app->window != NULL) {
        eng->InitDisplay(APP_CMD_INIT_WINDOW);
        eng->DrawFrame();
      }
      break;
    case APP_CMD_TERM_WINDOW:
      // Disconnect all connections before going down
      eng->OnStopButtonClick();
      // Note that JUI helper needs to know if a window has been terminated
      eng->TermDisplay(APP_CMD_TERM_WINDOW);
      eng->has_focus_ = false;
      break;
    case APP_CMD_RESUME:
      jui_helper::JUIWindow::GetInstance()->Resume(app->activity,
                                                   APP_CMD_RESUME);
      // Players need re-connect since we teared down the connection when we
      // went suspension
      eng->EnableUI(true);
      break;
    case APP_CMD_GAINED_FOCUS:
      // Start animation
      eng->has_focus_ = true;
      jui_helper::JUIWindow::GetInstance()->Resume(app->activity,
                                                   APP_CMD_GAINED_FOCUS);
      break;
    case APP_CMD_LOST_FOCUS:
      // Also stop animating.
      eng->has_focus_ = false;
      eng->DrawFrame();
      break;
    case APP_CMD_CONFIG_CHANGED:
      // Configuration changes
      eng->TermDisplay(APP_CMD_CONFIG_CHANGED);
      eng->InitDisplay(APP_CMD_CONFIG_CHANGED);
      break;
    case APP_CMD_DESTROY:
      ndk_helper::JNIHelper::GetInstance()->DetachCurrentThread();
      break;
  }
}

/*
 * Misc
 */
void Engine::SetState(android_app *state) { app_ = state; }

/*
 * disable 3D rendering for now
 */
bool Engine::IsReady() {
  if (has_focus_) return true;

  return false;
}

/*
 * Our global instance for Game engine
 */
Engine g_engine;

/**
 * This is the main entry point of a native application that is using
 * android_native_app_glue.  It runs in its own thread, with its own
 * event loop for receiving input events and doing other things.
 */
void android_main(android_app *state) {

  g_engine.SetState(state);

  // Init helper functions
  ndk_helper::JNIHelper::Init(state->activity, HELPER_CLASS_NAME,
                              HELPER_CLASS_SONAME);

  // Init play game services
  g_engine.InitGoogleNearbyConnection();

  state->userData = &g_engine;
  state->onAppCmd = Engine::HandleCmd;
  state->onInputEvent = Engine::HandleInput;

  // loop waiting for stuff to do.
  while (1) {
    // Read all pending events.
    int id;
    int events;
    android_poll_source *source;

    // If not animating, we will block forever waiting for events.
    // If animating, we loop until all events are read, then continue
    // to draw the next frame of animation.
    while ((id = ALooper_pollAll(g_engine.IsReady() ? 0 : -1, NULL, &events,
                                 reinterpret_cast<void **>(&source))) >= 0) {
      // Process this event.
      if (source != NULL) source->process(state, source);

      // Check if we are exiting.
      if (state->destroyRequested != 0) {
        g_engine.TermDisplay(APP_CMD_TERM_WINDOW);
        return;
      }
    }

    if (g_engine.IsReady()) {
      // Drawing is throttled to the screen update rate, so there
      // is no need to do timing here.
      g_engine.DrawFrame();
    }
  }
}

extern "C" {
JNIEXPORT void
Java_com_google_example_games_nearbyconnections_NearbyNativeActivity_OnPauseHandler(
    JNIEnv *env) {
  // This call is to suppress 'E/WindowManager(): android.view.WindowLeaked...'
  // errors.
  // Since orientation change events in NativeActivity comes later than
  // expected, we can not dismiss
  // popupWindow gracefully from NativeActivity.
  // So we are releasing popupWindows explicitly triggered from Java callback
  // through JNI call.
  jui_helper::JUIWindow::GetInstance()->Suspend(APP_CMD_PAUSE);
}
}
