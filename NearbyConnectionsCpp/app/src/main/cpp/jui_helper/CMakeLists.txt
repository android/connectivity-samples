# Copyright (C) 2018 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
##

# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)
set(CMAKE_VERBOSE_MAKEFILE on)

project(juihelper)

##include the native part of NDKHelper
#add_subdirectory (${NDK_HELPER_PATH} "${CMAKE_CURRENT_SOURCE_DIR}/libs/${ANDROID_ABI}/libndkhelper")

add_library(juihelper STATIC
            JavaUI.cpp
            JavaUI_Dialog.cpp
            JavaUI_Window.cpp
            )

target_include_directories(juihelper PRIVATE
                 ${ANDROID_NDK}/sources/android/native_app_glue
                      ${ndkhelper_SOURCE_DIR}/src/main/cpp
                      ../ndk_helper
                 )
set_target_properties(juihelper
                      PROPERTIES
                      ARCHIVE_OUTPUT_DIRECTORY
                      "${CMAKE_CURRENT_SOURCE_DIR}/libs/${ANDROID_ABI}")
