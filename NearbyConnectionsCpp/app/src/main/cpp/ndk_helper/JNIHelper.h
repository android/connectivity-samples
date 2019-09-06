/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#pragma once

#include <jni.h>
#include <vector>
#include <string>
#include <functional>
#include <mutex>
#include <pthread.h>

#include <android/log.h>
#include <android_native_app_glue.h>

#ifndef NDEBUG
#define LOGV(...)                                                              \
  ((void)__android_log_print(                                                  \
      ANDROID_LOG_VERBOSE, ndk_helper::JNIHelper::GetInstance()->GetAppName(), \
      __VA_ARGS__))

#define LOGI(...)                                                           \
  ((void)__android_log_print(                                               \
      ANDROID_LOG_INFO, ndk_helper::JNIHelper::GetInstance()->GetAppName(), \
      __VA_ARGS__))
#define LOGW(...)                                                           \
  ((void)__android_log_print(                                               \
      ANDROID_LOG_WARN, ndk_helper::JNIHelper::GetInstance()->GetAppName(), \
      __VA_ARGS__))
#define LOGE(...)                                                            \
  ((void)__android_log_print(                                                \
      ANDROID_LOG_ERROR, ndk_helper::JNIHelper::GetInstance()->GetAppName(), \
      __VA_ARGS__))
#else
// Have this option is to confirm there is no timing issues: after development
// enable these set of NULL printf to make sure app behaves normally
#define LOGI(...) (0)
#define LOGV(...) (0)
#define LOGW(...) (0)
#define LOGE(...) (0)

#endif
namespace ndk_helper {

class JUIView;

/******************************************************************
 * Helper functions for JNI calls
 * This class wraps JNI calls and provides handy interface calling commonly used
 * features
 * in Java SDK.
 * Such as
 * - loading graphics files (e.g. PNG, JPG)
 * - character code conversion
 * - retrieving system properties which only supported in Java SDK
 *
 * NOTE: To use this class, add NDKHelper.java as a corresponding helpers in
 * Java code
 */
class JNIHelper {
 public:
  /*
   * To load your own Java classes, JNIHelper requires to be initialized with a
   * ANativeActivity handle.
   * This methods need to be called before any call to the helper class.
   * Static member of the class
   *
   * arguments:
   * in: activity, pointer to ANativeActivity. Used internally to set up JNI
   * environment
   * in: helper_class_name, pointer to Java side helper class name. (e.g.
   * "com/sample/helper/NDKHelper" in samples )
   */
  static void Init(ANativeActivity *activity, const char *helper_class_name);

  /*
   * Init() that accept so name.
   * When using a JUI helper class, Java side requires SO name to initialize JNI
   * calls to invoke native callbacks.
   * Use this version when using JUI helper.
   *
   * arguments:
   * in: activity, pointer to ANativeActivity. Used internally to set up JNI
   * environment
   * in: helper_class_name, pointer to Java side helper class name. (e.g.
   * "com/sample/helper/NDKHelper" in samples )
   * in: native_soname, pointer to soname of native library. (e.g.
   * "NativeActivity" for "libNativeActivity.so" )
   */
  static void Init(ANativeActivity *activity, const char *helper_class_name,
                   const char *native_soname);

  /*
   * Retrieve the singleton object of the helper.
   * Static member of the class

   * Methods in the class are designed as thread safe.
   */
  static JNIHelper *GetInstance();

  /*
   * Convert string from character code other than UTF-8
   *
   * arguments:
   *  in: str, pointer to a string which is encoded other than UTF-8
   *  in: encoding, pointer to a character encoding string.
   *  The encoding string can be any valid java.nio.charset.Charset name
   *  e.g. "UTF-16", "Shift_JIS"
   * return: converted input string as an UTF-8 std::string
   */
  std::string ConvertString(const char *str, const char *encode);

  /*
   * Retrieve string used as nearby connection service ID from
   * AndroidManifest.xml file. Entry is:
   * <meta-data
   *        android:name="com.google.android.gms.nearby.connection.SERVICE_ID"
   *        android:value="@string/service_id"/>
   */
  std::string GetNearbyConnectionServiceID();

  /*
   * Retrieves application bundle name
   *
   * return: pointer to an app name string
   *
   */
  const char *GetAppName() { return app_bunlde_name_.c_str(); }

  /*
   * Retrieves application label
   *
   * return: pointer to an app label string
   *
   */
  const char *GetAppLabel() { return app_label_.c_str(); }

  /*
   * Execute given function in Java UIThread.
   *
   * arguments:
   *  in: pFunction, a pointer to a function to be executed in Java UI Thread.
   *  Note that the helper function returns immediately without synchronizing a
   * function completion.
   */
  void RunOnUiThread(std::function<void()> callback);

  /*
   * Attach current thread
   * In Android, the thread doesn't have to be 'Detach' current thread
   * as application process is only killed and VM does not shut down
   */
  JNIEnv *AttachCurrentThread() {
    JNIEnv *env;
    if (activity_->vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_4) == JNI_OK)
      return env;
    activity_->vm->AttachCurrentThread(&env, NULL);
    pthread_key_create(reinterpret_cast<int32_t*>(activity_), DetachCurrentThreadDtor);
    return env;
  }

  void DetachCurrentThread() {
    activity_->vm->DetachCurrentThread();
    return;
  }

  /*
   * Decrement a global reference to the object
   * arguments:
   *  in: obj, obj to decrement a global reference
   */
  void DeleteObject(jobject obj);

  /*
   * Helper methods to call a method in given object
   */
  jobject CreateObject(const char *class_name);
  jobject CallObjectMethod(jobject object, const char *strMethodName,
                           const char *strSignature, ...);
  void CallVoidMethod(jobject object, const char *strMethodName,
                      const char *strSignature, ...);
  float CallFloatMethod(jobject object, const char *strMethodName,
                        const char *strSignature, ...);
  int32_t CallIntMethod(jobject object, const char *strMethodName,
                        const char *strSignature, ...);
  bool CallBooleanMethod(jobject object, const char *strMethodName,
                         const char *strSignature, ...);
  jclass RetrieveClass(JNIEnv *jni, const char *class_name);

 private:
  std::string app_bunlde_name_;
  std::string app_label_;

  ANativeActivity *activity_;
  jobject jni_helper_java_ref_;
  jclass jni_helper_java_class_;

  // mutex for synchronization
  // This class uses singleton pattern and can be invoked from multiple threads,
  // each methods locks the mutex for a thread safety
  mutable std::mutex mutex_;

  JNIHelper();
  ~JNIHelper();
  JNIHelper(const JNIHelper &rhs);
  JNIHelper &operator=(const JNIHelper &rhs);

  /*
   * Call method in JNIHelper class
   */
  jobject CallObjectMethod(const char *strMethodName, const char *strSignature,
                           ...);
  void CallVoidMethod(const char *strMethodName, const char *strSignature, ...);

  /*
   * Unregister this thread from the VM
   */
  static void DetachCurrentThreadDtor(void *p) {
    LOGI("detached current thread");
    ANativeActivity *activity = reinterpret_cast<ANativeActivity *> (p);
    activity->vm->DetachCurrentThread();
  }
};

extern "C" {
JNIEXPORT void Java_com_sample_helper_NDKHelper_RunOnUiThreadHandler(
    JNIEnv *env, jobject thiz, int64_t pointer);
}

}  // namespace ndk_helper
