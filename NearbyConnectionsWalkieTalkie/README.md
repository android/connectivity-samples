# Google APIs for Mobile: Quickstarts

Demonstrates streaming audio with the
[Nearby.Connections API](https://developers.google.com/nearby/connections/overview) to other
nearby devices.

Getting Started
---------------

This sample uses the Gradle build system. To build this project, import the
project into Android Studio before running it on (at least) 2 devices. This
project comes with 2 build variants, Manual and Automatic, that demonstrate
different ways of connecting the two devices. They can be toggled between
in the Build Variants tab within Android Studio.

WalkieTalkie Manual
-------------------

In the manual build variant, shake one device to begin advertising. As long as
the app is open, other nearby devices should connect to it shortly. To speak,
hold down the volume keys and your voice will be transmitted to the other
devices you have connected to.

This demonstrates a star network, where one device advertises to a group of
devices nearby.

WalkieTalkie Automatic
----------------------

In the automatic build variant, devices simultaneously advertise and discover.
Devices will begin to pair up as they connect to the first device they see.
To speak, hold down the volume keys and your voice will be transmitted to the
paired device.

This demonstrates a point to point network, where one device transmits a high
bandwidth Payload to one other device.

Support
-------

- Stack Overflow: http://stackoverflow.com/questions/tagged/google-play-services

If you've found an error in this sample, please file an issue: https://github.com/android/connectivity

Patches are encouraged, and may be submitted according to the instructions in
CONTRIBUTING.md.


## How to make contributions?
Please read and follow the steps in the [CONTRIBUTING.md](https://github.com/android/connectivity-samples/blob/main/CONTRIBUTING.md)
