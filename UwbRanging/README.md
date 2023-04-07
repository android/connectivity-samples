# Android Ultra-wideband sample

## Overview
This project showcases the current features of the 
<b>[Android UWB Jetpack library](https://developer.android.com/jetpack/androidx/releases/core-uwb)</b>.

Includes code examples for:

* <b>Device compatibility</b> - How to check if an Android device supports UWB.
* <b>Device Discovery</b> - Ultra-wideband currently does not support a native way to discover devices, so an out of band (OOB) mechanism must be provided.  This project uses the
[NearBy Connections API](https://developers.google.com/nearby/connections/overview), but other radio protocols like 
Bluetooth, BLE, or Wi-Fi could also be used. 
* <b>Simple Ranging</b> - The <b>Ranging</b> screen displays the controllee's distance from the the controller.
* <b>Device Control</b> - The <b>Control</b> screen simulates a use case where a door lock could be 
opened when a UWB-capable device is near by.
* <b>Share Media</b> - The <b>Share file</b> screen demonstrates how to transfer a media file using the
selected OOB mechanism when devices are in close proximity.
* <b>Settings</b> - In this screen you can select which Android device will play each role (Controller or Controlee).


## Pre-requisites
* Two UWB-capable Android phones with Android 12 or higher
* Latest version of the [Core Ultra Wideband (UWB) library](https://developer.android.com/jetpack/androidx/releases/core-uwb)


## What is it not?
  
* An end to end example of Ultra-wideband technology.
 The main goal is to demonstrate basic ranging capabilities between two Android devices and 
how a selected OOB mechanism could be used to facilitate real use cases.  For the latest information on the library status check [this article](https://developer.android.com/guide/topics/connectivity/uwb)
* A reference for a real production app with proper security, network access, app permissions, user authentication, etc. Check out the [Now in Android app](https://github.com/android/nowinandroid) instead.
* A UI/Material Design sample. The interface of the app is deliberately kept simple to focus on the UWB use cases. Check out the [Compose Samples](https://github.com/android/compose-samples) instead.
* A complete Jetpack sample covering all libraries. Check out [Android Sunflower](https://github.com/googlesamples/android-sunflower) or the advanced [GitHub Browser Sample](https://github.com/googlesamples/android-architecture-components/tree/master/GithubBrowserSample) instead.


## Who is it for?

*   Intermediate developers looking for a simple way to understand how the UWB Jetpack library can be used.
*   Advanced developers looking for a quick reference.

## Opening in Android Studio

To open this app in Android Studio, begin by checking out the entire ```connectivity-samples``` project: 

1. Clone the repository, this step checks out the master branch.:

```
git clone git@github.com:android/connectivity-samples.git

```
 
2. Open the ```UwbRanging``` folder in the IDE.


### License

```
Copyright 2023 Google, Inc.

Licensed to the Apache Software Foundation (ASF) under one or more contributor
license agreements. See the NOTICE file distributed with this work for
additional information regarding copyright ownership. The ASF licenses this
file to you under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations under
the License.
```
