# Google APIs for Mobile: Quickstarts

Demonstrates use of the
[Nearby.Messages API](https://developers.google.com/nearby/)
for communicating between
devices within close proximity of each other.

Introduction
------------

This sample allows a user to publish a message to nearby devices, and subscribe
to messages published by those devices.

To run this sample, use two or more devices to publish and subscribe messages.


Getting Started
---------------

This sample uses the Gradle build system. To build this project, use the
"gradlew build" command. Or, use "Import Project" in Android Studio.

To use this sample, follow the following steps:

1. Create a project on
[Google Developer Console](https://console.developers.google.com/). Or, use an
existing project.

1. Click on `APIs & auth -> APIs`, and enable `Nearby Messages API`.

1. Click on `Credentials`, then click on `Create new key`, and pick
`Android key`. Then register your Android app's SHA1 certificate
fingerprint and package name for your app. Use
`com.google.android.gms.nearby.messages.samples.nearbydevices`
for the package name.

1. Copy the API key generated, and paste it in `AndroidManifest.xml`.


Support
-------

- Stack Overflow: http://stackoverflow.com/questions/tagged/google-play-services

If you've found an error in these samples, please file an issue in this repo.

Patches are encouraged, and may be submitted according to the instructions in
CONTRIBUTING.md.


## How to make contributions?
Please read and follow the steps in the [CONTRIBUTING.md](CONTRIBUTING.md)
