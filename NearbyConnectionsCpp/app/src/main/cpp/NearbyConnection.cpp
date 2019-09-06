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
/**
 * File: NearbyConnection.cpp
 *  Demonstrates features of Nearby Connections such as:
 *      1) Create nearby_connection interface gpg::NearbyConnections::Builder
 *      2) Connect to other peers via StartAdvertising(), StartDiscovery()
 *      3) Send messages with SendReliableMessage() and SendUnreliableMessage()
 *      4) Clean up Google Nearby Connection with Stop()
 *      5) Other in-game communication like relaying connections/messages
 *    Test Instructions:
 *      1) Build and install on phones
 *      2) Set ONE and ONLY ONE device to be advertising first
 *      3) All other devices to be discovering after advertising is ready
 * [indicator is "stop" button is enabled]
 *      4) Monitor bottom of the UI for connected Clients, once anyone
 * connected, "start game" is enabled; play it at any time
 *      5) while playing, your own score and other player's score should be
 * visible to you at the bottom of the screen
 *      note: play time is 30 second by default, set in GAME_DURATION in header
 * file
 */
#include <ctime>
#include <cstdlib>
#include "NearbyNativeActivity.h"

/*
 * InitGoogleNearbyConnection():
 *    Create an NearbyConnection interface, update our internal indicators
 *    according to connection creation result
 */
void Engine::InitGoogleNearbyConnection() {
  // Need only one connection
  if (nearby_connection_ != nullptr) {
    return;
  }
  nbc_state_ = nearby_connection_state::IDLE;

  gpg::AndroidInitialization::android_main(app_);
  gpg::AndroidPlatformConfiguration platform_configuration;
  platform_configuration.SetActivity(app_->activity->clazz);

  // Two devices can connect using Nearby Connections if they share the
  // same service ID.  This value is set in AndroidManifest.xml, it can be any
  // string that is unique for your application
  service_id_ = ndk_helper::JNIHelper::GetInstance()->GetNearbyConnectionServiceID();
  // Prepare to create a nearby connection interface
  gpg::NearbyConnections::Builder nbBuilder;
  nearby_connection_ = nbBuilder.SetDefaultOnLog(gpg::LogLevel::VERBOSE)
      // register callback so we get notified the nearby connect is ready
      .SetOnInitializationFinished([this](gpg::InitializationStatus status) {
        switch (status) {
          case gpg::InitializationStatus::VALID:
            // Our interface is ready to use [ could advertise or discover ]
            LOGI("InitializationFinished() returned VALID");
            nbc_state_ = nearby_connection_state::IDLE;
             // Configure our listener to use for listening
            msg_listener_.SetOnMessageReceivedCallback(
                              [this](int64_t receiver_id,
                                     std::string const &remote_endpoint,
                                     std::vector<uint8_t> const &payload,
                                     bool is_reliable) {
                                OnMessageReceivedCallback(receiver_id,
                                                          remote_endpoint,
                                                          payload, is_reliable);
                              })
                .SetOnDisconnectedCallback([this](
                    int64_t receiver_id, std::string const &remote_endpoint) {
                  OnMessageDisconnectedCallback(receiver_id, remote_endpoint);
                });
            LOGI("Nearby Connection Interface is ready to use!");
            break;
          case gpg::InitializationStatus::ERROR_VERSION_UPDATE_REQUIRED:
            /*
             * interface need update, we should prompt user to do so, or we do
             * it automatically here [not done]
             */
            LOGE(
                "InitializationFinished(): ERROR_VERSION_UPDATE_REQUIRED "
                "returned,"
                "please restart your device to update Google play service");
            nbc_state_ = nearby_connection_state::FAILED;
            break;
          case gpg::InitializationStatus::ERROR_INTERNAL:
            /*
             * Error happened, interface is not ready to use
             */
            LOGE(
                "InitializationFinished() failed, unable to start nearby connection");
            nbc_state_ = nearby_connection_state::FAILED;
            EnableUI(false);
            break;
          default:
            LOGE("InitializationFinished(): Unrecognized error code: %d",
                 (int)status);
            nbc_state_ = nearby_connection_state::FAILED;
            EnableUI(false);
            break;
        }
      })
      .Create(platform_configuration);

  // At this point, connection is still not useful; wait till
  // InitializationFinished()
  // telling us that VALID initialization has been completed
  LOGV("InitGoogleNearbyConnection() created interface: %p",
       nearby_connection_.get());
}

/*
 * OnMessageReceived(received_id, remote_endpoint, payload, is_reliable)
 *    for our case, we only looking for a score and update our cached value
 *    otherwise we ignore
 */
void Engine::OnMessageReceivedCallback(int64_t receiver_id,
                                       std::string const &remote_endpoint,
                                       std::vector<uint8_t> const &payload,
                                       bool is_reliable) {
  std::string msg;
  for (auto ch : payload) msg += ch;

  switch (msg[0]) {
    case PAYLOAD_HEADER_NEW_CONNECTION: {
      LOGV("Adding relayed endpoint (%s, %d) to me",
           remote_endpoint.c_str(), static_cast<int>(remote_endpoint.length()));
      std::string other_endpoint = msg.substr(1);
      AddConnectionEndpoint(other_endpoint, false, false);
      EnableUI(true);
      return;
    }
    case PAYLOAD_HEADER_GUEST_READY: {
      // The link I am hosting is up, and the other end ( guest end )
      // is ready to accept all of my connected nodes
      LOGI("Received ready message from =%s", remote_endpoint.c_str());
      SendAllConnections(remote_endpoint);
      return;
    }
    case PAYLOAD_HEADER_INTERMEDIATE_SCORE: {
      int score;
      if (!DecodeScorePayload(payload, &score, remote_endpoint)) {
        LOGE("DecodeScorePayload return failed\n");
      } else {
        UpdatePlayerScore(remote_endpoint, score, false);
        UpdateScoreBoardUI(true);
      }
      return;
    }
    case PAYLOAD_HEADER_FINAL_SCORE: {
      int score;
      if (!DecodeScorePayload(payload, &score,remote_endpoint)) {
        LOGE("DecodeScorePayload return failed\n");
      } else {
        UpdatePlayerScore(remote_endpoint, score, true);
        UpdateScoreBoardUI(true);
      }
      return;
    }
    case PAYLOAD_HEADER_DISCONNECTED: {
      LOGV("Received Disconnect Notification for Endpoint ID %s",
           remote_endpoint.c_str());
      RemoveConnectionEndpoint(remote_endpoint, false);
      EnableUI(true);
      return;
    }
    default: {
      // Drop the message
      LOGE("Unknown payload type: from(%s) with payload(%s) in %s @ line %d",
           remote_endpoint.c_str(), msg.c_str(), __FILE__, __LINE__);
      return;
    }
  }
}

/*
 * OnMessageDisconnectedCallback(): update our player database
 */
void Engine::OnMessageDisconnectedCallback(int64_t receiver_id,
                                           std::string const &remote_endpoint) {
  RemoveConnectionEndpoint(remote_endpoint, true);
  if (CountConnections() == 0) {
    nbc_state_ &= ~nearby_connection_state::CONNECTED;
    if (nbc_state_ == 0) {
      nbc_state_ |= nearby_connection_state::IDLE;
    }
  }
  EnableUI(true);
}

/**
 * IEndpointDiscoverListener::OnEndpointFound()
 *   this get called when discover found someone on the same service_id; we
 *   request connection to the new device
 */
void Engine::OnEndpointFound(int64_t client_id,
                             gpg::EndpointDetails const &endpoint_details) {
  LOGV("EndpointFound(%lld)", static_cast<long long>(client_id));
  LOGV("endpoint details: endpoint_id=%s, name=%s, service_id=%s",
       endpoint_details.endpoint_id.c_str(),
       endpoint_details.name.c_str(), endpoint_details.service_id.c_str());


  std::vector<uint8_t> payload;
  std::string name;
  nearby_connection_->SendConnectionRequest(
      name, endpoint_details.endpoint_id, payload,
      [this](int64_t client_id, gpg::ConnectionResponse const &response) {
        OnConnectionResponse(response);
      },
      msg_listener_);
}
/*
 * IEndpointDiscoverListener::OnEndpointLost():
 *     Get called when lost connection to remote endpoint
 *     We silently accept the fact -- remove it from our cache
 */
void Engine::OnEndpointLost(int64_t client_id,
                            std::string const &remote_endpoint_id) {
  RemoveConnectionEndpoint(remote_endpoint_id, true);
  if (CountConnections() == 0) {
    nbc_state_ &= ~nearby_connection_state::CONNECTED;
    if (nbc_state_ == 0) {
      nbc_state_ = nearby_connection_state::IDLE;
    }
    EnableUI(true);
  }
}

/*
 * ProcessEndPointNotconnected(): The other end is not reachable, clean cache
 */
void Engine::ProcessEndPointNotconnected(std::string const &remote_endpoint_id) {
  RemoveConnectionEndpoint(remote_endpoint_id, true);

  if (CountConnections() == 0) {
    nbc_state_ &= ~nearby_connection_state::CONNECTED;
    if (nbc_state_ == 0) {
      nbc_state_ = nearby_connection_state::IDLE;
    }
    EnableUI(true);
  }
}
/*
 * OnConnectionResponse():  process response from others after we request to
 * connect
 */
void Engine::OnConnectionResponse(gpg::ConnectionResponse const &response) {
  LOGV("status=%s, remote_endpoint_id=%s",
       (response.status == gpg::ConnectionResponse::StatusCode::ACCEPTED
            ? "ACCEPTED"
            : "REJECTED"),
       response.remote_endpoint_id.c_str());
  if (response.status == gpg::ConnectionResponse::StatusCode::ACCEPTED) {

    AddConnectionEndpoint(response.remote_endpoint_id, true, false);
    nbc_state_ |= nearby_connection_state::CONNECTED;
    nbc_state_ &= ~nearby_connection_state::IDLE;

    // Notification to the other end that I am ready to accept
    // all other connections that connected to the sender
    std::vector<uint8_t> payload;
    payload.resize(1);
    payload[0] = PAYLOAD_HEADER_GUEST_READY;
    nearby_connection_->SendReliableMessage(response.remote_endpoint_id,
                                            payload);

    EnableUI(true);
  }

  if (response.status ==
      gpg::ConnectionResponse::StatusCode::ERROR_ENDPOINT_NOT_CONNECTED) {
      LOGE("Connection to %s is DOWN due to network error",
         response.remote_endpoint_id.c_str());
      ProcessEndPointNotconnected(response.remote_endpoint_id);
  }

  if (response.status == gpg::ConnectionResponse::StatusCode::REJECTED) {
    LOGV("Connection Rejected by %s", response.remote_endpoint_id.c_str());
    auto it = players_score_.find(response.remote_endpoint_id);
    if (it != players_score_.end() && it->second.connected_ != true) {
      RemoveConnectionEndpoint(response.remote_endpoint_id);
    }
  }
}

/*
 * BroadcastNewConnection()
 *    Relay the new connection to existing connections
 */
void Engine::BroadcastNewConnection(std::string const &endpoint_id) {
  std::vector<uint8_t> payload;
  payload.resize(endpoint_id.length() + 1);  // I do not send the '\0'
  payload[0] = PAYLOAD_HEADER_NEW_CONNECTION;
  strncpy(reinterpret_cast<char *>(&payload[1]), endpoint_id.c_str(),
          endpoint_id.length());
  /*
   * Only the host(advertising) end could relay connection to others
   * So when a new connection is up, host(advertising) end will need
   * broadcast this new connection to ALL REAL(not virtual)connections
   * that it IS hosting, and the link is still VALID ( I do not clear cache
   * when a node is disconnected, only stop button causing clearing;
   * this is because I still want to see everyone's score in the end)
   */
  for (auto it = players_score_.begin(); it != players_score_.end(); ++it) {
    if (it->second.endpoint_id_ != endpoint_id &&  // do not send to himself
        it->second.connected_ &&    // still connected
        it->second.is_direct_connection_ &&    // Not relayed[or call it virtual ]connection
        it->second.is_host_) {      // I(my_endpoint_id) am host for this link
      LOGV("Broadcast new connection message to: %s for new id of %s",
           it->second.endpoint_id_.c_str(), endpoint_id.c_str());
      nearby_connection_->SendReliableMessage(it->first, payload);
    }
  }
}
/*
 * SendAllConnectios():
 *    Send all of my directly connected endpoints to the given endpoint
 *    because it just established a connection to this device and has no
 *    knowledge of other endpoints [directly connecting to me ]
 */
void Engine::SendAllConnections(const std::string& accepting_endpoint_id) {
  std::vector<uint8_t> payload;
  for (auto it = players_score_.begin(); it != players_score_.end(); ++it) {
    if (it->second.endpoint_id_ == accepting_endpoint_id ||
        !it->second.is_direct_connection_)
      continue;
    payload.resize(it->second.endpoint_id_.length() + 1);
    payload[0] = static_cast<uint8_t>(PAYLOAD_HEADER_NEW_CONNECTION);
    strncpy(reinterpret_cast<char *>(&payload[1]),
            it->second.endpoint_id_.c_str(),
            it->second.endpoint_id_.length());
    LOGV("Sending connection to %s for %s", accepting_endpoint_id.c_str(),
         it->second.endpoint_id_.c_str());
    nearby_connection_->SendReliableMessage(accepting_endpoint_id, payload);
  }
}

/*
 * BroadcastScore(): Broadcast my score to others
 */
void Engine::BroadcastScore(int32_t score, bool final) {

  std::vector<uint8_t> payload;
    std::string my_placeholder_id = "Broadcast_me";
  if(BuildScorePayload(payload, score_counter_, my_placeholder_id, final) == false) {
    LOGE("BuildScorePayload() failed for BroadcastScore");
    return;
  }
  for (auto it = players_score_.begin(); it != players_score_.end(); ++it) {
    if (it->second.connected_ && it->second.is_direct_connection_) {
      LOGI("Broadcasting my(%s) score to: %s, %s", my_placeholder_id.c_str(),
           it->second.endpoint_id_.c_str(),
           std::string(reinterpret_cast<char*>(&payload[0]), payload.size())
           .c_str());
      nearby_connection_->SendUnreliableMessage(it->second.endpoint_id_,
                                                payload);
    }
  }
}

/*
 * RetrieveScores(): append everyone else's [ not including mine] scores to the
 * given container
 */
void Engine::RetrieveScores(std::string &allstr) {
  for (auto it = players_score_.begin(); it != players_score_.end(); ++it) {
    if (it != players_score_.begin()) {
      allstr += "\n";
    }
    allstr += "Player: " + it->second.endpoint_id_.substr(0, 16);
    allstr += " Score: " + std::to_string(it->second.score_);
  }
}

/*
 * OnAdvertiseButtonClick():
 *    Advertise Button handler --we start advertising our availability so others
 * could DISCOVER us
 *    We could try to call StopAdvertising() before calling StartAdvertising()
 * in case we
 *    have outstanding connection from last session [ not doing that here].
 */
void Engine::OnAdvertiseButtonClick(void) {
  if (nbc_state_ & nearby_connection_state::ADVERTISING) {
    return;
  }
  LOGV("OnAdvertiseButtonClick(): Listening");
  nbc_state_ |= nearby_connection_state::ADVERTISING;
  nbc_state_ &= ~nearby_connection_state::IDLE;

  /*
   * start advertising, and advertise forever until stop is called
   */
  std::vector<gpg::AppIdentifier> app_identifiers;
  gpg::AppIdentifier tmp;
  tmp.identifier = std::string(ndk_helper::JNIHelper::GetInstance()->GetAppName());
  app_identifiers.push_back(tmp);
  nearby_connection_->StartAdvertising(
        "",            // Let SDK generate the name
        app_identifiers,            // Package name is identifier
        gpg::Duration::zero(),      // Advertise forever
        [this](int64_t client_id, gpg::StartAdvertisingResult const &result) {
          LOGV("StartAdvertisingResult(%lld, %s)", static_cast<long long>(client_id),
               result.local_endpoint_name.c_str());
          switch (result.status) {
            case gpg::StartAdvertisingResult::StatusCode::SUCCESS:
              LOGI("advertising success!");
              break;
            case gpg::StartAdvertisingResult::StatusCode::
                ERROR_ALREADY_ADVERTISING:
              LOGI("advertising already in place");
              break;
            case gpg::StartAdvertisingResult::StatusCode::
                ERROR_NETWORK_NOT_CONNECTED:
              nbc_state_ |= nearby_connection_state::FAILED;
              nbc_state_ &= ~nearby_connection_state::ADVERTISING;
              LOGE("advertising failed: no network connection");
              break;
            case gpg::StartAdvertisingResult::StatusCode::ERROR_INTERNAL:
              LOGE("advertising failed as ERROR_INTERNAL");
              nbc_state_ |= nearby_connection_state::FAILED;
              nbc_state_ &= ~nearby_connection_state::ADVERTISING;
              break;
          }
          EnableUI(true);
        },
        [this](int64_t client_id, gpg::ConnectionRequest const &request) {
          LOGI("ConnectionRequest(%lld)", static_cast<long long>(client_id));
          LOGI(
              "remote info: req.endpoint_id=%s,  req_name = "
              "%s",
              request.remote_endpoint_id.c_str(),
              request.remote_endpoint_name.c_str());
          std::vector<uint8_t> payload;   //empty packet for remote

          // Accept all the connection requests as they come in.
          nearby_connection_->AcceptConnectionRequest(
              request.remote_endpoint_id, payload, msg_listener_);
          BroadcastNewConnection(request.remote_endpoint_id);
          // Adding this end point into the connected state:

          AddConnectionEndpoint(request.remote_endpoint_id, true,
                  /* is the host (advertiser of this connection) */true);
          nbc_state_ |= nearby_connection_state::CONNECTED;
          nbc_state_ &= ~nearby_connection_state::IDLE;
          EnableUI(true);
          LOGI("Accepting Request sending out (for %s)",
               request.remote_endpoint_id.c_str());
  });
  EnableUI(true);
}

/*
 * OnDiscoveryButtonClick():
 *    Start to discover other devices on the same service_id
 *    If somehow could not connect, we could try call StopDiscovery() before we
 * call StartDiscovery()
 */
void Engine::OnDiscoverButtonClick(void) {
  nbc_state_ |= nearby_connection_state::DISCOVERING;
  nbc_state_ &= ~nearby_connection_state::IDLE;
  EnableUI(true);

  if (this->discovery_helper_) {
    delete this->discovery_helper_;
  }
  this->discovery_helper_ = new gpg::EndpointDiscoveryListenerHelper();
  discovery_helper_->SetOnEndpointFoundCallback([this](int64_t client_id,
                                                       const gpg::EndpointDetails &endpoint_details)
                                                {
                                                    this->OnEndpointFound(client_id, endpoint_details);
                                                });


  nearby_connection_->StartDiscovery(service_id_, gpg::Duration::zero(),*this->discovery_helper_);
}

/*
 * OnStopButtonClick
 */
void Engine::OnStopButtonClick(void) {
  nearby_connection_->Stop();
  players_score_.clear();
  nbc_state_ = nearby_connection_state::IDLE;
  EnableUI(true);
}

/*
 * OnPlayButtonClick(): play the game
 */
void Engine::OnPlayButtonClick(void) {
  // Run on UI thread and play the game
  PlayGame();
}

/*
 * AddConnectionEndpoint(): add this to the player list if it is not in
 */
void Engine::AddConnectionEndpoint(std::string const &remote_endpoint_id,
                                   bool is_native, bool is_host) {
  // Find if this one is in
  auto it = players_score_.find(remote_endpoint_id);
  if (it == players_score_.end()) {
    PLAYER_STATUS player_info;
    memset(&player_info, 0, sizeof(player_info));
    player_info.connected_ = true;
    player_info.is_direct_connection_ = is_native;
    player_info.is_host_ = is_host;
    player_info.endpoint_id_ = remote_endpoint_id;
    players_score_[remote_endpoint_id] = player_info;
  } else {
    if (it->second.connected_ == true) {
      LOGE("accepting a connection while we are connected in %s @ %d", __FILE__,
           __LINE__);
    }
    it->second.finished_ = false;
    it->second.connected_ = true;
    it->second.is_direct_connection_ = is_native;
    it->second.is_host_ = is_host;
  }
}

/*
 * RemoveConnectionEndpoint(): turn it into NOT connected
 */
void Engine::RemoveConnectionEndpoint(std::string const &remote_endpoint_id,
                                      bool need_broadcast) {
  auto it = players_score_.find(remote_endpoint_id);
  if (it != players_score_.end()) {
    it->second.finished_ = true;
    it->second.connected_ = false;
  }

  if (need_broadcast && it->second.is_host_) {
    // build up array of the connected endpoints to me DIRECTLY
    std::vector<std::string> endpoints;
    for (it = players_score_.begin(); it != players_score_.end(); ++it) {
      if (it->second.connected_ && it->second.is_direct_connection_ &&
          it->second.is_host_ &&
          it->first != remote_endpoint_id) {
        endpoints.push_back(it->first);
      }
    }
    if (endpoints.size()) {
      std::vector<uint8_t> payload;
      payload.resize(PAYLOAD_HEADER_LENGTH + remote_endpoint_id.size());
      payload[0] = (uint8_t)PAYLOAD_HEADER_DISCONNECTED;
      memcpy(&(payload[1]), remote_endpoint_id.c_str(),
             remote_endpoint_id.size());

      nearby_connection_->SendReliableMessage(endpoints, payload);
    }
  }
}

/*
 * CountConnections(void): report current valid connections
 */
int32_t Engine::CountConnections(void) {
  int32_t count = 0;
  for (auto it = players_score_.begin(); it != players_score_.end(); ++it) {
    if (it->second.connected_) {
      count++;
    }
  }
  return count;
}

/*
 * UpdatePlayerScore()
 *    format:endpoint_it;score
 *    Update the player's score into local cache
 *    Relay the scores to others if I am host of the link;
 *    if I am guest for the link or it is virtual link, just update the local
 * cache
 */
void Engine::UpdatePlayerScore(std::string const & endpoint_id,
                               int score, bool final) {
  auto player = players_score_.find(endpoint_id);
  if (player == players_score_.end()) {
    LOGE("Error: player(%s) is not in my cache", endpoint_id.c_str());
    DebugDumpConnections();
    return;
  }
  if (player->second.score_ > score) {
    LOGI("Score (%d) is lower than saved one(%d), no action", score,
         player->second.score_);
    return;
  }
  player->second.score_ = score;
  player->second.finished_ = final;

  if (!player->second.is_host_) {
    // I am not a host for this link, done
    return;
  }

  //broadcast the score to all others connected to me
  std::vector<uint8_t> payload;
  if (BuildScorePayload(payload, score, endpoint_id, final) == false) {
    LOGE("failed to build payload for player %s:", endpoint_id.c_str());
    return;
  }
  auto *this_player = &player->second;
  for (player = players_score_.begin(); player != players_score_.end();
       ++player) {
    if (player->first == this_player->endpoint_id_ ||
        !player->second.is_direct_connection_ || !player->second.is_host_) {
      continue;  // do not send back for his own score
    }

    LOGV("Sending(%s) for %s score = %d", player->first.c_str(),
         endpoint_id.c_str(), this_player->score_);
    nearby_connection_->SendUnreliableMessage(player->first, payload);
  }
}

/*
 * BuildScorePayload(): payload format -
 *     Token xx EEEEEEEE ddddd
 *     Token: 'f' or 'i'
 *     xx: digit number to indicate how many bytes follows as EndpointId
 *     EEE...EEE: EndpointId bits
 *     dd...dd:   real score bits [left-overs]
 */
bool  Engine::BuildScorePayload(std::vector<uint8_t> &payload, int score,
                                std::string const & endpoint, bool final) {
    if(endpoint.size() <= 0 || endpoint.size() >= 100 ){
        return false;
    }
    std::string  payload_str(1, final?'f' : 'i');
    if(endpoint.size() < 10 ) {
        payload_str += std::string("0");
    }
    payload_str += std::to_string(endpoint.size());
    payload_str += endpoint;
    payload_str += std::to_string(score);

    // Move to payload buffer
    payload.resize(payload_str.size());
    memcpy(&payload[0], payload_str.c_str(), payload_str.size());
    return true;
}

/*
 * DecodeScorePayload()
 */
bool Engine::DecodeScorePayload(std::vector<uint8_t> const &payload, int *p_score,
                        const std::string &endpoint) {
    if(!p_score) {
        LOGE("null pointer for decideScorePayload %p", p_score);
        return false;
    }
    std::string payload_str(reinterpret_cast<const char*>(&payload[0]),payload.size());
    int endpoint_size = (payload_str[1] - '0') * 10 + (payload_str[2] - '0');

    //get our score
    payload_str = payload_str.substr(3+endpoint_size);
    *p_score = 0;
    for (auto ch : payload_str) {
        if(ch < '0' || ch > '9')
            break;
        *p_score = (*p_score) * 10 + (ch - '0');
    }

    return true;
}

/*
 * DebugDumpConnections(): Dump out current connection status
 */
void Engine::DebugDumpConnections(void) {
  LOGI("Play Status Table Contents:");
  auto const_it = players_score_.begin();
  for (; const_it != players_score_.end(); ++const_it) {
    LOGI(
        "Player Info: endpoint_id(%s,%d), score(%d), connected(%s), "
        "native(%s), host(%s), Finished(%s)",
        const_it->first.c_str(), static_cast<int>(const_it->first.size()),
        const_it->second.score_, const_it->second.connected_ ? "true" : "false",
        const_it->second.is_direct_connection_ ? "true" : "false",
        const_it->second.is_host_ ? "true" : "false",
        const_it->second.finished_ ? "true" : "false");
    if (const_it->second.is_direct_connection_) {
      LOGI("Host Endpoint ID = %s",
           const_it->second.is_host_ ? "Debug_placeholder_id"
                                     : const_it->second.endpoint_id_.c_str());
    }
  }
}
