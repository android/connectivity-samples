# Cross-Device SDK samples: Rock Paper Scissors

This sample app demonstrates various APIs that are part of the [Cross device SDK](https://github.com/google/cross-device-sdk) Developer Preview. This includes discovering nearby devices, establishing connections, and using Sessions APIs for transferring and sharing user experiences between devices.

Getting Started
---------------

This sample uses the Gradle build system. To build this project, import the
project into Android Studio before running it on (at least) 2 devices.

The Cross device SDK Developer Preview requires that [Google Play Services is installed](https://developers.google.com/android/guides/setup#check-whether-installed) on all participating devices and they are enrolled in the [Beta Program](https://developers.google.com/android/guides/beta-program) 

Game Play
-------------------

Basic game rules are pretty simple - each device is a player who can select rock, paper, or scissors during their turn. At the end of the turn (once every player makes their choice), the game will display a score giving a point to every "beat" (and in Multiplayer substracting a point for every opponent a player "loses" to).

To play Rock Paper Scissors, users should first select a game mode. 

## Game Modes

To demonstrate the different APIs and features of the Cross device SDK, we introduced the following modes:

* Two Player - Uses the Discovery & Secure Connection APIs (see [this section of developer docs](https://developer.android.com/guide/topics/connectivity/cross-device-sdk/device-discovery) for more information).
* Two Player Sessions - Using Sessions API to achieve the same as above (see [Sessions](guide/topics/connectivity/cross-device-sdk/sessions)).
* Single Player - Uses the Session Transfer API to demonstrate seamless game transfer to a different device. In this mode a player is matched with an AI oponent.
* Multiplayer - Uses the Session Share API to create a multiplayer game where other devices can be invited to join and participate. 

Support
-------

For Cross device SDK Developer Preview feedback, issues, and feature requests, use [this link](https://issuetracker.google.com/issues/new?component=1205991&template=1706309).

If you've found an error in this sample, please file an issue: https://github.com/android/connectivity

Patches are encouraged, and may be submitted according to the instructions in
CONTRIBUTING.md.


## How to make contributions?
Please read and follow the steps in [CONTRIBUTING.md](https://github.com/android/connectivity-samples/blob/main/CONTRIBUTING.md)
