# Google APIs for Mobile: Quickstarts

Demonstrates sending payloads with the
[Nearby.Connections API](https://developers.google.com/nearby/connections/overview) to a
nearby device.

Getting Started
---------------

This sample uses the Gradle build system. To build this project, import the
project into Android Studio before running it on (at least) 2 devices.

Usage
-------------------

Tap the "Find Opponent" button on both devices. The devices will both simultaneously
advertise and discover. They will automatically attempt to connect to the first device they see.
Once paired, each device can choose rock, paper, or scissors. A running score is showed in the
middle, with the first number being your wins and the second being your opponent's wins.

This demonstrates a point to point network, where two devices transmit and receive
payloads with each other.

Support
-------

- Stack Overflow: http://stackoverflow.com/questions/tagged/google-play-services

If you've found an error in this sample, please file an issue: https://github.com/android/connectivity

Patches are encouraged, and may be submitted according to the instructions in
CONTRIBUTING.md.


## How to make contributions?
Please read and follow the steps in the [CONTRIBUTING.md](https://github.com/android/connectivity-samples/blob/main/CONTRIBUTING.md)
