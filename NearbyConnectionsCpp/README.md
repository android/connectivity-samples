Google Nearby Connections C++ Samples
============================================================================
Copyright (C) 2015 Google LLC

# Contents

NearbyConnectionsNative: The sample demonstrates real time
multiplayer game on [C++ Nearby Connection interface SDK](https://developers.google.com/games/services/cpp/nearby).

* Create nearby_connection interface gpg::NearbyConnections::Builder
* Connect to other peers via StartAdvertising(), StartDiscovery()
* Send messages with SendReliableMessage() and SendUnreliableMessage()
* Clean up Google Nearby Connection with Stop()
* Other in-game communication like relaying connections/messages

# How to run a sample

For generic play game related instruction, please follow steps described in [Getting Started for C++](https://developers.google.com/games/services/cpp/GettingStartedNativeClient)

The build is gradle based.  To build from the command line run:
```
gradlew assembleDebug
```

In Android Studio, sometimes the first sync of the project cannot find the 
Android.mk for gpg-sdk.  To fix this open the terminal window in Android Studio
and run
```
gradlew gpg-sdk:download_and_stage_gpg_sdk
```

# Specific Steps for This App
1. Build App
2. Install on phone
3. Set ONE and ONLY ONE phone/device to be advertising
4. All other devices to be discovering after advertising is ready (indicator is "stop" button is enabled)
5. Monitor bottom of the UI for "Connected Clients"; once anyone connected, it should be bigger than 0, "Play Game" should be enabled; from here, you could play it at any time
6. While playing, your own score and other player's scores should be visible to you at the bottom of the screen
       note: play time is 30 second by default, and was hard coded as GAME_DURATION in header file

# Support

First of all, take a look at our [troubleshooting guide](https://developers.google.com/games/services/android/troubleshooting). Most setup issues can be solved by following this guide.

If your question is not answered by the troubleshooting guide, we encourage
you to post your question to [stackoverflow.com](stackoverflow.com). Our
team answers questions there reguarly.
