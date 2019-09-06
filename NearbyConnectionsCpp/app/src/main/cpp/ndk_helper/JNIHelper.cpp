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
#include <EGL/egl.h>
#include <GLES2/gl2.h>
#include <fstream>
#include <iostream>

#include "JNIHelper.h"

namespace ndk_helper {

#define NATIVEACTIVITY_CLASS_NAME "android/app/NativeActivity"

/*
 * JNI Helper functions
 */

/*
 * Singleton
 */
JNIHelper *JNIHelper::GetInstance() {
  static JNIHelper helper;
  return &helper;
}

/*
 * Ctor
 */
JNIHelper::JNIHelper() : activity_(NULL) {}

/*
 * Dtor
 */
JNIHelper::~JNIHelper() {
  // Lock mutex
  std::lock_guard<std::mutex> lock(mutex_);

  JNIEnv *env = AttachCurrentThread();
  env->DeleteGlobalRef(jni_helper_java_ref_);
  env->DeleteGlobalRef(jni_helper_java_class_);
}

/*
 * Init
 */
void JNIHelper::Init(ANativeActivity *activity, const char *helper_class_name) {
  JNIHelper &helper = *GetInstance();

  helper.activity_ = activity;

  // Lock mutex
  std::lock_guard<std::mutex> lock(helper.mutex_);

  JNIEnv *env = helper.AttachCurrentThread();

  // Retrieve app bundle id
  jclass android_content_Context = env->GetObjectClass(helper.activity_->clazz);
  jmethodID midGetPackageName = env->GetMethodID(
      android_content_Context, "getPackageName", "()Ljava/lang/String;");

  jstring packageName = (jstring)env->CallObjectMethod(helper.activity_->clazz,
                                                       midGetPackageName);
  const char *appname = env->GetStringUTFChars(packageName, NULL);
  helper.app_bunlde_name_ = std::string(appname);

  // Instantiate JNIHelper class
  jclass cls = helper.RetrieveClass(env, helper_class_name);
  helper.jni_helper_java_class_ = (jclass)env->NewGlobalRef(cls);

  jmethodID constructor =
      env->GetMethodID(helper.jni_helper_java_class_, "<init>",
                       "(Landroid/app/NativeActivity;)V");

  helper.jni_helper_java_ref_ = env->NewObject(helper.jni_helper_java_class_,
                                               constructor, activity->clazz);
  helper.jni_helper_java_ref_ = env->NewGlobalRef(helper.jni_helper_java_ref_);

  // Get app label
  jstring labelName = (jstring)helper.CallObjectMethod("getApplicationName",
                                                       "()Ljava/lang/String;");
  const char *label = env->GetStringUTFChars(labelName, NULL);
  helper.app_label_ = std::string(label);

  env->ReleaseStringUTFChars(packageName, appname);
  env->ReleaseStringUTFChars(labelName, label);
  env->DeleteLocalRef(packageName);
  env->DeleteLocalRef(labelName);
  env->DeleteLocalRef(cls);
}

void JNIHelper::Init(ANativeActivity *activity, const char *helper_class_name,
                     const char *native_soname) {
  Init(activity, helper_class_name);
  if (native_soname) {
    JNIHelper &helper = *GetInstance();
    // Lock mutex
    std::lock_guard<std::mutex> lock(helper.mutex_);

    JNIEnv *env = helper.AttachCurrentThread();

    // Setup soname
    jstring soname = env->NewStringUTF(native_soname);

    jmethodID mid = env->GetMethodID(helper.jni_helper_java_class_,
                                     "loadLibrary", "(Ljava/lang/String;)V");
    env->CallVoidMethod(helper.jni_helper_java_ref_, mid, soname);

    env->DeleteLocalRef(soname);
  }
}

std::string JNIHelper::GetNearbyConnectionServiceID() {
  if (activity_ == NULL) {
    LOGI(
        "JNIHelper has not been initialized. Call init() to initialize the "
        "helper");
    return std::string("");
  }

  JNIEnv *env = AttachCurrentThread();

  std::string service_id("");
  jmethodID mid =
      env->GetMethodID(jni_helper_java_class_, "getNearbyConnectionServiceID",
                       "()Ljava/lang/String;");
  if (NULL == mid) {
    LOGE("JNIHelper could not find function name getNearbyConnectionServiceID");
    return service_id;
  }

  jstring resultJNIStr =
      (jstring)env->CallObjectMethod(jni_helper_java_ref_, mid);
  const char *resultCStr = env->GetStringUTFChars(resultJNIStr, NULL);
  if (NULL == resultCStr) {
    LOGE("Java GetNearbyConnectionServiceID() returned NULL string");
    return service_id;
  }
  service_id = std::string(resultCStr);
  env->ReleaseStringUTFChars(resultJNIStr, resultCStr);
  env->DeleteLocalRef(resultJNIStr);
  return service_id;
}
/*
 * Misc implementations
 */
jclass JNIHelper::RetrieveClass(JNIEnv *jni, const char *class_name) {
  jclass activity_class = jni->FindClass(NATIVEACTIVITY_CLASS_NAME);
  jmethodID get_class_loader = jni->GetMethodID(
      activity_class, "getClassLoader", "()Ljava/lang/ClassLoader;");
  jobject cls = jni->CallObjectMethod(activity_->clazz, get_class_loader);
  jclass class_loader = jni->FindClass("java/lang/ClassLoader");
  jmethodID find_class = jni->GetMethodID(
      class_loader, "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;");

  jstring str_class_name = jni->NewStringUTF(class_name);
  jclass class_retrieved =
      (jclass)jni->CallObjectMethod(cls, find_class, str_class_name);
  jni->DeleteLocalRef(str_class_name);
  jni->DeleteLocalRef(activity_class);
  jni->DeleteLocalRef(class_loader);
  return class_retrieved;
}

void JNIHelper::DeleteObject(jobject obj) {
  if (obj == NULL) {
    LOGI("obj can not be NULL");
    return;
  }

  JNIEnv *env = AttachCurrentThread();
  env->DeleteGlobalRef(obj);
}

jobject JNIHelper::CallObjectMethod(const char *strMethodName,
                                    const char *strSignature, ...) {
  if (activity_ == NULL) {
    LOGI(
        "JNIHelper has not been initialized. Call init() to initialize the "
        "helper");
    return NULL;
  }

  JNIEnv *env = AttachCurrentThread();
  jmethodID mid =
      env->GetMethodID(jni_helper_java_class_, strMethodName, strSignature);
  if (mid == NULL) {
    LOGI("method ID %s, '%s' not found", strMethodName, strSignature);
    return NULL;
  }

  va_list args;
  va_start(args, strSignature);
  jobject obj = env->CallObjectMethodV(jni_helper_java_ref_, mid, args);
  va_end(args);

  return obj;
}

void JNIHelper::CallVoidMethod(const char *strMethodName,
                               const char *strSignature, ...) {
  if (activity_ == NULL) {
    LOGI(
        "JNIHelper has not been initialized. Call init() to initialize the "
        "helper");
    return;
  }

  JNIEnv *env = AttachCurrentThread();
  jmethodID mid =
      env->GetMethodID(jni_helper_java_class_, strMethodName, strSignature);
  if (mid == NULL) {
    LOGI("method ID %s, '%s' not found", strMethodName, strSignature);
    return;
  }
  va_list args;
  va_start(args, strSignature);
  env->CallVoidMethodV(jni_helper_java_ref_, mid, args);
  va_end(args);

  return;
}

jobject JNIHelper::CallObjectMethod(jobject object, const char *strMethodName,
                                    const char *strSignature, ...) {
  if (activity_ == NULL) {
    LOGI(
        "JNIHelper has not been initialized. Call init() to initialize the "
        "helper");
    return NULL;
  }

  JNIEnv *env = AttachCurrentThread();
  jclass cls = env->GetObjectClass(object);
  jmethodID mid = env->GetMethodID(cls, strMethodName, strSignature);
  if (mid == NULL) {
    LOGI("method ID %s, '%s' not found", strMethodName, strSignature);
    return NULL;
  }

  va_list args;
  va_start(args, strSignature);
  jobject obj = env->CallObjectMethodV(object, mid, args);
  va_end(args);

  env->DeleteLocalRef(cls);
  return obj;
}

void JNIHelper::CallVoidMethod(jobject object, const char *strMethodName,
                               const char *strSignature, ...) {
  if (activity_ == NULL) {
    LOGI(
        "JNIHelper has not been initialized. Call init() to initialize the "
        "helper");
    return;
  }

  JNIEnv *env = AttachCurrentThread();
  jclass cls = env->GetObjectClass(object);
  jmethodID mid = env->GetMethodID(cls, strMethodName, strSignature);
  if (mid == NULL) {
    LOGI("method ID %s, '%s' not found", strMethodName, strSignature);
    return;
  }

  va_list args;
  va_start(args, strSignature);
  env->CallVoidMethodV(object, mid, args);
  va_end(args);

  env->DeleteLocalRef(cls);
  return;
}

float JNIHelper::CallFloatMethod(jobject object, const char *strMethodName,
                                 const char *strSignature, ...) {
  float f = 0.f;
  if (activity_ == NULL) {
    LOGI(
        "JNIHelper has not been initialized. Call init() to initialize the "
        "helper");
    return f;
  }

  JNIEnv *env = AttachCurrentThread();
  jclass cls = env->GetObjectClass(object);
  jmethodID mid = env->GetMethodID(cls, strMethodName, strSignature);
  if (mid == NULL) {
    LOGI("method ID %s, '%s' not found", strMethodName, strSignature);
    return f;
  }
  va_list args;
  va_start(args, strSignature);
  f = env->CallFloatMethodV(object, mid, args);
  va_end(args);

  env->DeleteLocalRef(cls);
  return f;
}

int32_t JNIHelper::CallIntMethod(jobject object, const char *strMethodName,
                                 const char *strSignature, ...) {
  int32_t i = 0;
  if (activity_ == NULL) {
    LOGI(
        "JNIHelper has not been initialized. Call init() to initialize the "
        "helper");
    return i;
  }

  JNIEnv *env = AttachCurrentThread();
  jclass cls = env->GetObjectClass(object);
  jmethodID mid = env->GetMethodID(cls, strMethodName, strSignature);
  if (mid == NULL) {
    LOGI("method ID %s, '%s' not found", strMethodName, strSignature);
    return i;
  }
  va_list args;
  va_start(args, strSignature);
  i = env->CallIntMethodV(object, mid, args);
  va_end(args);

  env->DeleteLocalRef(cls);
  return i;
}

bool JNIHelper::CallBooleanMethod(jobject object, const char *strMethodName,
                                  const char *strSignature, ...) {
  bool b;
  if (activity_ == NULL) {
    LOGI(
        "JNIHelper has not been initialized. Call init() to initialize the "
        "helper");
    return false;
  }

  JNIEnv *env = AttachCurrentThread();
  jclass cls = env->GetObjectClass(object);
  jmethodID mid = env->GetMethodID(cls, strMethodName, strSignature);
  if (mid == NULL) {
    LOGI("method ID %s, '%s' not found", strMethodName, strSignature);
    return false;
  }
  va_list args;
  va_start(args, strSignature);
  b = env->CallBooleanMethodV(object, mid, args);
  va_end(args);

  env->DeleteLocalRef(cls);
  return b;
}

jobject JNIHelper::CreateObject(const char *class_name) {
  JNIEnv *env = AttachCurrentThread();

  jclass cls = env->FindClass(class_name);
  jmethodID constructor = env->GetMethodID(cls, "<init>", "()V");

  jobject obj = env->NewObject(cls, constructor);
  jobject objGlobal = env->NewGlobalRef(obj);
  env->DeleteLocalRef(obj);
  env->DeleteLocalRef(cls);
  return objGlobal;
}

void JNIHelper::RunOnUiThread(std::function<void()> callback) {
  // Lock mutex
  std::lock_guard<std::mutex> lock(mutex_);

  JNIEnv *env = AttachCurrentThread();
  static jmethodID mid = NULL;
  if (mid == NULL)
    mid = env->GetMethodID(jni_helper_java_class_, "runOnUIThread", "(J)V");

  // Allocate temporary function object to be passed around
  std::function<void()> *pCallback = new std::function<void()>(callback);
  env->CallVoidMethod(jni_helper_java_ref_, mid, (int64_t)pCallback);
}

// This JNI function is invoked from UIThread asynchronously
extern "C" {
JNIEXPORT void Java_com_sample_helper_NDKHelper_RunOnUiThreadHandler(
    JNIEnv *env, jobject thiz, int64_t pointer) {
  std::function<void()> *pCallback = (std::function<void()> *)pointer;
  (*pCallback)();

  // Deleting temporary object
  delete pCallback;
}
}

}  // namespace ndk_helper
