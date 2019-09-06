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
#include "JavaUI.h"

namespace jui_helper {
/*
 * JUI Dialog
 */
std::unordered_map<std::string, int32_t> JUIDialog::map_attributes_;
const AttributeType JUIDialog::attributes_[] = {
    {"Title", ATTRIBUTE_PARAMETER_STRING},
};

JUIDialog::JUIDialog()
    : activity_(NULL),
      suspended_(false),
      dismiss_callback_(NULL),
      cancel_callback_(NULL) {
  map_attribute_parameters.clear();
}

JUIDialog::JUIDialog(ANativeActivity *activity)
    : suspended_(false), dismiss_callback_(NULL), cancel_callback_(NULL) {
  Init(activity);
}

JUIDialog::~JUIDialog() {
  Close();

  auto it = map_attribute_parameters.begin();
  auto itEnd = map_attribute_parameters.end();
  while (it != itEnd) {
    AttributeParameterStore &p = map_attribute_parameters[it->first];
    switch (p.type) {
      case ATTRIBUTE_PARAMETER_STRING:
        if (it->second.str != NULL) delete it->second.str;
        break;
      default:
        break;
    }
    it++;
  }
}

/*
 * Init
 */
void JUIDialog::Init(ANativeActivity *activity) {
  // setup attribute map (once)
  if (map_attributes_.size() == 0) {
    for (int32_t i = 0; i < sizeof(attributes_) / sizeof(attributes_[0]); ++i) {
      map_attributes_[std::string(attributes_[i].attribute_name)] =
          attributes_[i].attribute_type;
    }
  }

  activity_ = activity;
  CreateDialog();
}

void JUIDialog::CreateDialog() {
  ndk_helper::JNIHelper &helper = *ndk_helper::JNIHelper::GetInstance();
  JNIEnv *env = helper.AttachCurrentThread();

  // Create dialog
  jmethodID mid = env->GetMethodID(
      JUIWindow::GetInstance()->GetHelperClass(), "createDialog",
      "(Landroid/app/NativeActivity;)Ljava/lang/Object;");
  jobject obj =
      env->CallObjectMethod(JUIWindow::GetInstance()->GetHelperClassInstance(),
                            mid, activity_->clazz);
  if (obj == NULL) {
    LOGI("Failed creating Dialog object");
  }
  obj_ = env->NewGlobalRef(obj);

  // Notify 'id' to JNI side
  ndk_helper::JNIHelper::GetInstance()->CallVoidMethod(obj_, "setID", "(I)V",
                                                       id_factory_.getId(this));
  env->DeleteLocalRef(obj);
}

void JUIDialog::DeleteObject() {
  if (obj_) {
    ndk_helper::JNIHelper *helper = ndk_helper::JNIHelper::GetInstance();
    JNIEnv *env = helper->AttachCurrentThread();
    env->DeleteGlobalRef(obj_);
    obj_ = NULL;
  }
}

/*
 * Close
 */
void JUIDialog::Close() {
  if (obj_) {
    LOGI("Closing Dialog");

    ndk_helper::JNIHelper::GetInstance()->CallVoidMethod(obj_, "dismiss",
                                                         "()V");

    // Delete child views
    auto itBegin = views_.begin();
    auto itEnd = views_.end();
    while (itBegin != itEnd) {
      delete *itBegin;
      itBegin++;
    }

    DeleteObject();
    views_.clear();
    activity_ = NULL;
    obj_ = NULL;
    jui_helper::JUIWindow::GetInstance()->SetDialog(NULL);
  }
}

/*
 * Add JUIView to popup window
 */
void JUIDialog::AddView(JUIView *view) {
  ndk_helper::JNIHelper::GetInstance()->CallVoidMethod(
      obj_, "addView", "(Landroid/view/View;)V", view->GetJobject());
  views_.push_back(view);
}

void JUIDialog::Show() {
  ndk_helper::JNIHelper::GetInstance()->CallVoidMethod(obj_, "show", "()V");
  jui_helper::JUIWindow::GetInstance()->SetDialog(this);
}

void JUIDialog::Suspend() {
  // Close existing dialog
  DeleteObject();
  LOGI("Suspending Dialog");
  suspended_ = true;
}

void JUIDialog::Resume(ANativeActivity *activity) {
  LOGI("Resuming Dialog");
  activity_ = activity;
  DeleteObject();

  ndk_helper::JNIHelper::GetInstance()->RunOnUiThread([this]() {
    suspended_ = false;
    // Creating dialog asynchronous to avoid a crash inside a framework
    CreateDialog();
    RestoreParameters(map_attributes_);

    // Restore widgets
    auto itBegin = views_.begin();
    auto itEnd = views_.end();
    while (itBegin != itEnd) {
      // Restore
      (*itBegin)->Restore();
      ndk_helper::JNIHelper::GetInstance()->CallVoidMethod(
          obj_, "addView", "(Landroid/view/View;)V", (*itBegin)->GetJobject());

      itBegin++;
    }

    Show();  // Show the dialog again
  });
}

void JUIDialog::DispatchEvent(const int32_t message, const int32_t param1,
                              const int32_t param2) {
  switch (message) {
    case JUICALLBACK_DIALOG_DISMISSED:
      if (suspended_ == false) {
        jui_helper::JUIWindow::GetInstance()->SetDialog(NULL);
        if (dismiss_callback_) dismiss_callback_(this, message);
      }
      break;
    case JUICALLBACK_DIALOG_CANCELLED:
      if (suspended_ == false) {
        jui_helper::JUIWindow::GetInstance()->SetDialog(NULL);
        if (cancel_callback_) cancel_callback_(this, message);
      }
      break;
    default:
      break;
  }
}

bool JUIDialog::SetCallback(
    const int32_t message,
    std::function<void(jui_helper::JUIDialog *dialog, const int32_t message)>
        callback) {
  switch (message) {
    case JUICALLBACK_DIALOG_DISMISSED:
      dismiss_callback_ = callback;
      break;
    case JUICALLBACK_DIALOG_CANCELLED:
      cancel_callback_ = callback;
      break;
    default:
      break;
  }
  return true;
}

void JUIDialog::RestoreParameters(
    std::unordered_map<std::string, int32_t> &map) {
  auto it = map_attribute_parameters.begin();
  auto itEnd = map_attribute_parameters.end();
  while (it != itEnd) {
    AttributeParameterStore &p = map_attribute_parameters[it->first];
    switch (p.type) {
      case ATTRIBUTE_PARAMETER_INT:
        JUIBase::SetAttribute(map, it->first.c_str(), (int32_t)p.i);
        break;
      case ATTRIBUTE_PARAMETER_FLOAT:
        JUIBase::SetAttribute(map, it->first.c_str(), p.f);
        break;
      case ATTRIBUTE_PARAMETER_BOOLEAN:
        JUIBase::SetAttribute(map, it->first.c_str(), p.f);
        break;
      case ATTRIBUTE_PARAMETER_STRING:
        JUIBase::SetAttribute(map, it->first.c_str(), p.str->c_str());
        break;
      case ATTRIBUTE_PARAMETER_IF:
        JUIBase::SetAttribute(map, it->first.c_str(), p.param_if.i1,
                              p.param_if.f2);
        break;
      case ATTRIBUTE_PARAMETER_FF:
        JUIBase::SetAttribute(map, it->first.c_str(), p.param_ff.f1,
                              p.param_ff.f2);
        break;
      case ATTRIBUTE_PARAMETER_IIII:
        JUIBase::SetAttribute(map, it->first.c_str(), p.param_iiii.i1,
                              p.param_iiii.i2, p.param_iiii.i3,
                              p.param_iiii.i4);
        break;
      case ATTRIBUTE_PARAMETER_FFFI:
        JUIBase::SetAttribute(map, it->first.c_str(), p.param_fffi.f1,
                              p.param_fffi.f2, p.param_fffi.f3, p.param_fffi.i);
        break;
      default:
        break;
    }
    it++;
  }
}
}  // namespace jui_helper
