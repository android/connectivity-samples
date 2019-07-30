# Google APIs for Mobile: Quickstarts

Demonstrates use of long-lived background subscription with the
[Nearby.Messages API](https://developers.google.com/nearby/) to scan for
beacons using Bluetooth Low Energy (BLE).

Getting Started
---------------

This sample uses the Gradle build system. To build this project, use the
"gradlew build" command. Or, use "Import Project" in Android Studio.

To use this sample, follow the following steps:

1. Create a project on [Google Developer
   Console](https://console.developers.google.com/). Or, use an existing
project.

1. Click on `APIs & auth -> APIs`, and enable `Nearby Messages API`.

1. Click on `Credentials`, then click on `Create new key`, and pick `Android
   key`. Then register your Android app's SHA1 certificate fingerprint and
package name for your app. Use
`com.google.android.gms.nearby.messages.samples.nearbybackgroundbeacons` for the
package name.

1. Copy the API key generated, and paste it in `AndroidManifest.xml`.

Discovering Beacons
-------------------

By default, background subscription discovers all messages published by this
application and other applications in the same Google Developers Console
project. When attaching messages to a beacon, you must be authenticated as a
certain Developer Console project; if you then use the API key associated with
that project in this app, you should find all the messages you attached.


Support
-------

- Stack Overflow: http://stackoverflow.com/questions/tagged/google-play-services

If you've found an error in these samples, please file an issue in this repo.

For providing feedback about Nearby.Messages, use the `SEND FEEDBACK` link on
the [Nearby Messages API for Android
page](https://developers.google.com/android/connectivity).

Patches are encouraged, and may be submitted according to the instructions in
CONTRIBUTING.md.


## How to make contributions?
Please read and follow the steps in the [CONTRIBUTING.md](CONTRIBUTING.md)
