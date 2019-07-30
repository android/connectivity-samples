
Android BeamLargeFiles Sample
===================================

This sample demonstrates how to transfer large files via Android Beam. After the initial
handshake over NFC, file transfer will take place over a secondary high-speed
communication channel such as Bluetooth or WiFi Direct.


This feature requires Android 4.1 (Jelly Bean) or above. Unlike traditional Beam,
your application will not receive an Intent on the receiving device. Instead, the system
will save the file to disk and display a notification that the user can select to open
the file using a standard ACTION_VIEW Intent.

Pre-requisites
--------------

- Android SDK 28
- Android Build Tools v28.0.3
- Android Support Repository

Getting Started
---------------

This sample uses the Gradle build system. To build this project, use the
"gradlew build" command or use "Import Project" in Android Studio.

Support
-------

- Stack Overflow: http://stackoverflow.com/questions/tagged/android

If you've found an error in this sample, please file an issue:
https://github.com/android/connectivity

Patches are encouraged, and may be submitted by forking this project and
submitting a pull request through GitHub. Please see CONTRIBUTING.md for more details.
