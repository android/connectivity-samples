# Android Ultra-wide band Samples
<p align="center">

[comment]: <TODO> (Change this image later)
<img src="https://github.com/googlesamples/android-architecture/wiki/images/aab-logov2.png" alt="Illustration by Virginia Poltrack"/>
</p>

'Android Ultra-wide band Samples' is a project to showcase the <b>Jetpack UWB library</b>.

This project includes 3 basic use cases (you will need two UWB-capable Android phones): 
* Home - Ranging demo, shows controllee device distance and position on the controller's screen
* Control - Allows controller device to turn a virtual switch on and off on controllee device
* Send - Controller device can share a file with controllee

Additionally the code shows you how to check if the phone is UWB capable and select which device will play the role of controller and controlee
via the Settings screen.

* A single-activity architecture, using **[Navigation Compose](https://developer.android.com/jetpack/compose/navigation)**.
* A presentation layer that contains a Compose screen (View) and a **ViewModel** per screen (or feature).
* Reactive UIs using **[Flow](https://developer.android.com/kotlin/flow)** and **[coroutines](https://kotlinlang.org/docs/coroutines-overview.html)** for asynchronous operations.
* A **data layer** with a repository and two data sources (local using Room and a fake remote).


## What is it not?
  
* A UI/Material Design sample. The interface of the app is deliberately kept simple to focus on architecture. Check out the [Compose Samples](https://github.com/android/compose-samples) instead.
* A complete Jetpack sample covering all libraries. Check out [Android Sunflower](https://github.com/googlesamples/android-sunflower) or the advanced [GitHub Browser Sample](https://github.com/googlesamples/android-architecture-components/tree/master/GithubBrowserSample) instead.
* A real production app with network access, user authentication, etc. Check out the [Now in Android app](https://github.com/android/nowinandroid) instead.

## Who is it for?

*   Intermediate developers and beginners looking for a way to structure their app in a testable and maintainable way.
*   Advanced developers looking for quick reference.

## Opening a sample in Android Studio

To open one of the samples in Android Studio, begin by checking out one of this folder, and then open it in Android Studio.

Clone the repository:

```
git clone git@github.com:android/architecture-samples.git
```
This step checks out the master branch. If you want to change to a different sample:

```
git checkout origin/main
```


### License

```
Copyright 2022 Google, Inc.

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
