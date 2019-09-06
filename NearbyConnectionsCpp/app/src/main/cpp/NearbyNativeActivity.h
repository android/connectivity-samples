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
#pragma once

/*
 * Include files
 */
#include <jni.h>
#include <errno.h>

#include <android/log.h>
#include <android_native_app_glue.h>
#include <android/native_window_jni.h>
#include <algorithm>
#include <thread>

// For GPGS
#include "gpg/gpg.h"
#include "GFKSimpleGame.h"
#include "NDKHelper.h"
#include "JavaUI.h"

/*
 * Preprocessors
 */

// Class name of helper function
#define HELPER_CLASS_NAME "com.sample.helper.NDKHelper"
// Class name of JUIhelper function
#define JUIHELPER_CLASS_NAME "com.sample.helper.JUIHelper"
// Share object name of helper function library
#define HELPER_CLASS_SONAME "NearbyNativeActivity"

#define PAYLOAD_HEADER_LENGTH 1
#define PAYLOAD_HEADER_NEW_CONNECTION 'c'
#define PAYLOAD_HEADER_DISCONNECTED   'd'
#define PAYLOAD_HEADER_RENEW_CONNECTION 'r'
#define PAYLOAD_HEADER_GUEST_READY    'n'
#define PAYLOAD_HEADER_INTERMEDIATE_SCORE 'i'
#define PAYLOAD_HEADER_FINAL_SCORE    'f'

const int32_t GAME_DURATION = 30;
const int32_t CHOICES_PER_QUESTION = 4;

struct PLAYER_STATUS {
  std::string device_id_;
  std::string endpoint_id_;
  int32_t score_;
  bool connected_; // this node [endpoint_id_] is still online
  bool finished_;  // this node finished the game
  bool is_host_;   // I am hosting this link[I was advertising and this node was
                   // discovering
  bool is_direct_connection_;  // this node directly connects to me,
                               // not relayed to me by another device
};

class LogFunc {
 public:
  explicit LogFunc(const char *func_name) {
    func_name_ = std::string(func_name);
    LOGI("===>%s", func_name_.c_str());
  }
  ~LogFunc() { LOGI("<==%s", func_name_.c_str()); }

 private:
  std::string func_name_;
};

/*
 * Engine class of the sample: my class should be IEndpointDiscoverListener()
 */
struct android_app;
class Engine : public gpg::IEndpointDiscoveryListener {
 public:
  /*
   * nearby_connection_state are bit flags, they could co-exist.
   */
  enum nearby_connection_state {
    IDLE = 1,
    ADVERTISING = 2,
    DISCOVERING = 4,
    CONNECTED = 8,
    PLAYING = 16,
    FAILED = 32
  };

  // GPG-related methods
  void InitGoogleNearbyConnection();

  void InitializeGame();
  void PlayGame();
  void LeaveGame();

  // Event handling
  static void HandleCmd(struct android_app *app, int32_t cmd);
  static int32_t HandleInput(android_app *app, AInputEvent *event);

  // Engine life cycles
  Engine();
  ~Engine();
  void SetState(android_app *state);
  void InitDisplay(const int32_t cmd);
  void DrawFrame();
  void TermDisplay(const int32_t cmd);
  bool IsReady();

  // IEndpointDiscoverListener members
  virtual void OnEndpointFound(int64_t client_id,
                               gpg::EndpointDetails const &endpoint_details);
  virtual void OnEndpointLost(int64_t client_id,
                              std::string const &remote_endpoint_id);

  // MessageListnerHelper to handle the messages
  void OnMessageDisconnectedCallback(int64_t receiver_id,
                                     std::string const &remote_endpoint);
  void OnMessageReceivedCallback(int64_t receiver_id,
                                 std::string const &remote_endpoint,
                                 std::vector<uint8_t> const &payload,
                                 bool is_reliable);
  void OnStopButtonClick(void);

 private:
  void OnAdvertiseButtonClick(void);
  void OnDiscoverButtonClick(void);
  void OnPlayButtonClick(void);

  void BroadcastNewConnection(std::string const &endpoint);
  void SendAllConnections(const std::string& accepting_endpoint_id);
  void OnConnectionResponse(gpg::ConnectionResponse const &response);
  void ProcessEndPointNotconnected(std::string const &remote_endpoint_id);
  void AddConnectionEndpoint(std::string const &remote_endpoint_id,
                             bool is_native, bool is_host);
  void RemoveConnectionEndpoint(std::string const &remote_endpoint_id,
                                bool need_broadcast = false);
  int32_t CountConnections(void);
  void UpdatePlayerScore(std::string const & endpoint_id,
                         int score, bool final);
  void BroadcastScore(int32_t score, bool final);
  void RetrieveScores(std::string &score_str);
  bool BuildScorePayload(std::vector<uint8_t> &payload, int score,
                         std::string const & endpoint, bool final);
  bool DecodeScorePayload(std::vector<uint8_t> const &payload, int *p_score,
                          const std::string &endpoint);

  void DebugDumpConnections(void);

  void InitUI();
  void EnableUI(bool enable);
  void UpdateScoreBoardUI(bool UIThreadRequired);

  jui_helper::JUIButton *CreateChoiceButton(const char *cap,
                                            jui_helper::JUIButton *left,
                                            float fontSize = 17.f);
  void CheckChoice(jui_helper::JUIButton *selection);
  bool PlayOneRound(void);

  std::unique_ptr<gpg::NearbyConnections> nearby_connection_;

  // hashmap to keep tracking of player scores
  std::unordered_map<std::string, PLAYER_STATUS> players_score_;
  int32_t score_counter_;  // Score counter of local player
  bool playing_;           // Am I playing a game?
  int game_time_;          // Game start time
  game_helper::GFKSimple *game_;

  mutable std::mutex mutex_;

  // GLContext instance
  ndk_helper::GLContext *gl_context_;

  bool initialized_resources_;
  bool has_focus_;

  // Native activity app instance
  android_app *app_;

  // JUI dialog-related UI stuff here
  jui_helper::JUIDialog *dialog_;

  enum BUTTON_INDEX {
    BUTTON_ADVERTISE = 0,
    BUTTON_DISCOVER,
    BUTTON_PLAY_GAME,
    BUTTON_STOP,
    UI_BUTTON_COUNT
  };
  jui_helper::JUIButton *ui_buttons_[UI_BUTTON_COUNT];
  jui_helper::JUITextView *status_text_;

  jui_helper::JUITextView *time_text_;
  jui_helper::JUITextView *math_formula_;
  jui_helper::JUITextView *scores_text_;
  jui_helper::JUIButton *game_buttons_[CHOICES_PER_QUESTION];

  std::string service_id_;
  uint32_t nbc_state_;
  gpg::MessageListenerHelper msg_listener_;
  gpg::EndpointDiscoveryListenerHelper *discovery_helper_;
};
