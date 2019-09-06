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
 * Callback Handler for Java events
 */
extern "C" {
JNIEXPORT void Java_com_sample_helper_JUIHelper_JUICallbackHandler(
    JNIEnv *env, jobject thiz, int32_t id, int32_t message, int32_t param1,
    int32_t param2) {
  /*
   * id from Java is generated token from native side, which would be mapped back
   * into an UI pointer; so id is opaque to Java -- Java receives and returns back 
   * the same id to native side
   */
  JUIBase *p = JUIBase::id_factory_.getUIBase(id);
  if( p )
      p->DispatchEvent(message, param1, param2);
  else {
      LOGE("Failed to get JUIBase pointer for %d in %s", id, __FILE__);
  }
}
}

/*
 * IdFactory Implementation: our ID is just a simple integer starting from 1,and
 *                           keeps on counting up. There is no re-use -- even
 *                           one ID is removed from cache, we still counting up
 *                           It will take a long while to overflow a 32 bit integer
 *                           for UI ids. Even it does, it wrap back, at that time,
 *                           previous low values will long gone.
 */
int32_t IdFactory::cur_id_ = 0;
int32_t IdFactory::getId(const JUIBase* ui_object) {
    auto it = ids_.find(ui_object);
    if (it == ids_.end()) {
        LOGE("IDs not inside hash for %p, patching done (%d)", ui_object, cur_id_);
        ids_[ui_object] = ++cur_id_;
        return cur_id_;
    }
    return it->second;
}
JUIBase* IdFactory::getUIBase(int32_t ui_id) {
    for (auto id: ids_) {
        if(id.second == ui_id) {
            return const_cast<JUIBase*>(id.first);
        }
    }
    LOGE("Unable to find object point for ID(%d, %x)", ui_id, ui_id);
    return nullptr;
}
bool IdFactory::insert(const JUIBase* ui_object) {
    bool status = true;
    auto it = ids_.find(ui_object);
    if (it != ids_.end()) {
        LOGE("Error: %p is already in = %d", it->first, it->second);
        LOGE("Overwriting %p with %p for it", it->first, ui_object);
        status = false;
        //return status;  ---> let it fall through
    }
    ids_[ui_object] = ++cur_id_;
    return status;
}
bool  IdFactory::remove(const JUIBase* ui_object) {
    auto it = ids_.find(ui_object);
    if (it == ids_.end()) {
        LOGE("IDs not inside hash for %p!!!", ui_object);
        return false;
    }
    ids_.erase(it);
    return true;
}
void IdFactory::debugDumpCurrentHashTable(void) {
    for (auto id: ids_) {
        LOGI("Cached ID (%p = %d)", id.first, id.second);
    }
}

/*
 * Our global instance for id factory
 */
IdFactory JUIBase::id_factory_;

/*
 * JUIView
 */
// Attribute types for View
std::unordered_map<std::string, int32_t> JUIView::map_attributes_;
const AttributeType JUIView::attributes_[] = {
    {"AccessibilityLiveRegion", ATTRIBUTE_PARAMETER_INT},
    {"Alpha", ATTRIBUTE_PARAMETER_FLOAT},
    {"BackgroundResource", ATTRIBUTE_PARAMETER_INT},
    {"Clickable", ATTRIBUTE_PARAMETER_BOOLEAN},
    {"Enabled", ATTRIBUTE_PARAMETER_BOOLEAN},
    {"DrawingCacheQuality", ATTRIBUTE_PARAMETER_INT},
    {"ScrollbarFadingEnabled", ATTRIBUTE_PARAMETER_BOOLEAN},
    {"FilterTouchesWhenObscured", ATTRIBUTE_PARAMETER_BOOLEAN},
    {"FitsSystemWindows", ATTRIBUTE_PARAMETER_BOOLEAN},
    {"Focusable", ATTRIBUTE_PARAMETER_BOOLEAN},
    {"FocusableATTRIBUTE_PARAMETER_INTouchMode", ATTRIBUTE_PARAMETER_BOOLEAN},
    {"HapticFeedbackEnabled", ATTRIBUTE_PARAMETER_BOOLEAN},
    {"Id", ATTRIBUTE_PARAMETER_INT},
    {"ImportantForAccessibility", ATTRIBUTE_PARAMETER_INT},
    {"ScrollContainer", ATTRIBUTE_PARAMETER_BOOLEAN},
    {"KeepScreenOn", ATTRIBUTE_PARAMETER_BOOLEAN},
    {"LayoutDirection", ATTRIBUTE_PARAMETER_INT},
    {"LongClickable", ATTRIBUTE_PARAMETER_BOOLEAN},
    {"MinimumHeight", ATTRIBUTE_PARAMETER_INT},
    {"MinimumWidth", ATTRIBUTE_PARAMETER_INT},
    {"NextFocusDownId", ATTRIBUTE_PARAMETER_INT},
    {"NextFocusForwardId", ATTRIBUTE_PARAMETER_INT},
    {"NextFocusLeftId", ATTRIBUTE_PARAMETER_INT},
    {"NextFocusRightId", ATTRIBUTE_PARAMETER_INT},
    {"NextFocusUpId", ATTRIBUTE_PARAMETER_INT},
    {"PaddingRelative", ATTRIBUTE_PARAMETER_IIII},
    {"Padding", ATTRIBUTE_PARAMETER_IIII},
    {"VerticalFadingEdgeEnabled", ATTRIBUTE_PARAMETER_BOOLEAN},
    {"Rotation", ATTRIBUTE_PARAMETER_FLOAT},
    {"RotationX", ATTRIBUTE_PARAMETER_FLOAT},
    {"RotationY", ATTRIBUTE_PARAMETER_FLOAT},
    {"SaveEnabled", ATTRIBUTE_PARAMETER_BOOLEAN},
    {"ScaleX", ATTRIBUTE_PARAMETER_FLOAT},
    {"ScaleY", ATTRIBUTE_PARAMETER_FLOAT},
    {"ScrollBarDefaultDelayBeforeFade", ATTRIBUTE_PARAMETER_INT},
    {"ScrollBarFadeDuration", ATTRIBUTE_PARAMETER_INT},
    {"ScrollBarSize", ATTRIBUTE_PARAMETER_INT},
    {"ScrollBarStyle", ATTRIBUTE_PARAMETER_INT},
    {"SoundEffectsEnabled", ATTRIBUTE_PARAMETER_BOOLEAN},
    {"TextAlignment", ATTRIBUTE_PARAMETER_INT},
    {"TextDirection", ATTRIBUTE_PARAMETER_INT},
    {"PivotX", ATTRIBUTE_PARAMETER_FLOAT},
    {"PivotY", ATTRIBUTE_PARAMETER_FLOAT},
    {"TranslationX", ATTRIBUTE_PARAMETER_FLOAT},
    {"TranslationY", ATTRIBUTE_PARAMETER_FLOAT},
    {"Visibility", ATTRIBUTE_PARAMETER_INT},
};

JUIView::JUIView()
    : layoutWidth_(ATTRIBUTE_SIZE_WRAP_CONTENT),
      layoutHeight_(ATTRIBUTE_SIZE_WRAP_CONTENT),
      layoutWeight_(0.f),
      marginLeft_(0),
      marginRight_(0),
      marginTop_(0),
      marginBottom_(0) {
  // setup attribute map (once)
  if (map_attributes_.size() == 0) {
    for (int32_t i = 0; i < sizeof(attributes_) / sizeof(attributes_[0]); ++i) {
      map_attributes_[std::string(attributes_[i].attribute_name)] =
          attributes_[i].attribute_type;
    }
  }

  for (int32_t i = 0; i < LAYOUT_PARAMETER_COUNT; ++i) {
    array_current_rules_[i] = LAYOUT_PARAMETER_UNKNOWN;
  }
}

JUIView::~JUIView() {
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
 * Add relative layout rule to the view
 */
void JUIView::AddRule(const int32_t layoutParameterIndex,
                      const int32_t parameter) {
  if (layoutParameterIndex < 0 ||
      layoutParameterIndex >= LAYOUT_PARAMETER_COUNT) {
    LOGE("Invalid rule index");
    return;
  }
  ndk_helper::JNIHelper::GetInstance()->CallVoidMethod(
      JUIWindow::GetHelperClassInstance(), "addRule",
      "(Landroid/view/View;II)V", GetJobject(), layoutParameterIndex,
      parameter);
  array_current_rules_[layoutParameterIndex] = parameter;
}

void JUIView::AddRule(const int32_t layoutParameterIndex,
                      const JUIView *parameter) {

  /*
   * perform pointer to id xlation
   */
    AddRule(layoutParameterIndex, id_factory_.getId(parameter));
}

void JUIView::SetLayoutParams(const int32_t width, const int32_t height) {
  ndk_helper::JNIHelper::GetInstance()->CallVoidMethod(
      JUIWindow::GetHelperClassInstance(), "setLayoutParams",
      "(Landroid/view/View;II)V", GetJobject(), width, height);
  layoutWidth_ = width;
  layoutHeight_ = height;
  layoutWeight_ = 0.f;
}

void JUIView::SetLayoutParams(const int32_t width, const int32_t height,
                              const float weight) {
  ndk_helper::JNIHelper::GetInstance()->CallVoidMethod(
      JUIWindow::GetHelperClassInstance(), "setLayoutParams",
      "(Landroid/view/View;IIF)V", GetJobject(), width, height, weight);
  layoutWidth_ = width;
  layoutHeight_ = height;
  layoutWeight_ = weight;
}

void JUIView::SetMargins(const int32_t left, const int32_t top,
                         const int32_t right, const int32_t bottom) {
  ndk_helper::JNIHelper::GetInstance()->CallVoidMethod(
      JUIWindow::GetHelperClassInstance(), "setMargins",
      "(Landroid/view/View;IIII)V", GetJobject(), left, top, right, bottom);
  marginLeft_ = left;
  marginRight_ = right;
  marginTop_ = top;
  marginBottom_ = bottom;
}

void JUIView::RestoreParameters(std::unordered_map<std::string, int32_t> &map) {
  // Restore Layout Rule
  for (int32_t i = 0; i < LAYOUT_PARAMETER_COUNT; ++i) {
    if (array_current_rules_[i] != LAYOUT_PARAMETER_UNKNOWN) {
      AddRule(i, array_current_rules_[i]);
    }
  }

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

  if (layoutWidth_ != ATTRIBUTE_SIZE_WRAP_CONTENT ||
      layoutHeight_ != ATTRIBUTE_SIZE_WRAP_CONTENT || layoutWeight_ != 0.f) {
    if (layoutWeight_ != 0.f)
      SetLayoutParams(layoutWidth_, layoutHeight_, layoutWeight_);
    else
      SetLayoutParams(layoutWidth_, layoutHeight_);
  }

  if (marginLeft_ || marginRight_ || marginTop_ || marginBottom_) {
    SetMargins(marginLeft_, marginTop_, marginRight_, marginBottom_);
  }
}

std::unordered_map<std::string, int32_t> JUITextView::map_attributes_;
const AttributeType JUITextView::attributes_[] = {
    {"AutoLinkMask", ATTRIBUTE_PARAMETER_INT},
    {"Text", ATTRIBUTE_PARAMETER_STRING},
    {"CursorVisible", ATTRIBUTE_PARAMETER_BOOLEAN},
    {"CompoundDrawablesWithIntrinsicBounds", ATTRIBUTE_PARAMETER_IIII},
    {"CompoundDrawablesRelativeWithIntrinsicBounds", ATTRIBUTE_PARAMETER_IIII},
    {"CompoundDrawablePadding", ATTRIBUTE_PARAMETER_INT},
    {"InputExtras", ATTRIBUTE_PARAMETER_INT},
    {"Ellipsize", ATTRIBUTE_PARAMETER_INT /*TextUtils.TruncateAt*/},
    {"Ems", ATTRIBUTE_PARAMETER_INT},
    {"Typeface", ATTRIBUTE_PARAMETER_INT /*Typeface*/},
    {"FreezesText", ATTRIBUTE_PARAMETER_BOOLEAN},
    {"Gravity", ATTRIBUTE_PARAMETER_INT},
    {"Height", ATTRIBUTE_PARAMETER_INT},
    {"Hint", ATTRIBUTE_PARAMETER_INT},
    {"ImeOptions", ATTRIBUTE_PARAMETER_INT},
    {"IncludeFontPadding", ATTRIBUTE_PARAMETER_BOOLEAN},
    {"RawInputType", ATTRIBUTE_PARAMETER_INT},
    {"LineSpacing", ATTRIBUTE_PARAMETER_FF},
    {"Lines", ATTRIBUTE_PARAMETER_INT},
    {"LinksClickable", ATTRIBUTE_PARAMETER_BOOLEAN},
    {"MarqueeRepeatLimit", ATTRIBUTE_PARAMETER_INT},
    {"MaxEms", ATTRIBUTE_PARAMETER_INT},
    {"MaxHeight", ATTRIBUTE_PARAMETER_INT},
    {"MaxLines", ATTRIBUTE_PARAMETER_INT},
    {"MaxWidth", ATTRIBUTE_PARAMETER_INT},
    {"MinEms", ATTRIBUTE_PARAMETER_INT},
    {"MinHeight", ATTRIBUTE_PARAMETER_INT},
    {"MinLines", ATTRIBUTE_PARAMETER_INT},
    {"MinWidth", ATTRIBUTE_PARAMETER_INT},
    {"PrivateImeOptions", ATTRIBUTE_PARAMETER_STRING},
    {"HorizontallyScrolling", ATTRIBUTE_PARAMETER_BOOLEAN},
    {"SelectAllOnFocus", ATTRIBUTE_PARAMETER_BOOLEAN},
    {"ShadowLayer", ATTRIBUTE_PARAMETER_FFFI},
    {"AllCaps", ATTRIBUTE_PARAMETER_BOOLEAN},
    {"TextColor", ATTRIBUTE_PARAMETER_INT},
    {"HighlightColor", ATTRIBUTE_PARAMETER_INT},
    {"HintTextColor", ATTRIBUTE_PARAMETER_INT},
    {"LinkTextColor", ATTRIBUTE_PARAMETER_INT},
    {"TextScaleX", ATTRIBUTE_PARAMETER_FLOAT},
    {"TextSize", ATTRIBUTE_PARAMETER_IF},
    {"Width", ATTRIBUTE_PARAMETER_INT}};

JUITextView::JUITextView() : JUIView() {
  obj_ = JUIWindow::GetInstance()->CreateWidget("JUITextView", this);
  if (obj_ == NULL) LOGI("Class initialization failure");

  Init();
}

JUITextView::JUITextView(const char *str) : JUIView() {
  obj_ = JUIWindow::GetInstance()->CreateWidget("JUITextView", this);
  if (obj_ == NULL) LOGI("Class initialization failure");

  Init();
  SetAttribute("Text", str);
}

JUITextView::JUITextView(const bool b) : JUIView() {
  if (b == true)
    JUITextView();
  else
    Init();
}

void JUITextView::Init() {
  // setup attribute map (once)
  if (map_attributes_.size() == 0) {
    // Add base class's map
    map_attributes_.insert(JUIView::map_attributes_.begin(),
                           JUIView::map_attributes_.end());

    for (int32_t i = 0; i < sizeof(attributes_) / sizeof(attributes_[0]); ++i) {
      map_attributes_[std::string(attributes_[i].attribute_name)] =
          attributes_[i].attribute_type;
    }
  }
}

JUITextView::~JUITextView() {
  if (obj_ != NULL) {
    jui_helper::JUIWindow::GetInstance()->CloseWidget(obj_);
    obj_ = NULL;
  }
}

void JUITextView::Restore() {
  // Recreate Java Widget when the activity has been disposed
  obj_ = JUIWindow::GetInstance()->CreateWidget("JUITextView", this);
  if (obj_ == NULL) LOGI("Class initialization failure");

  RestoreParameters(map_attributes_);
}

/*
 * Button
 */
JUIButton::JUIButton() : JUITextView(false), onclick_callback_(NULL) { Init(); }

JUIButton::JUIButton(const char *str) : JUITextView(false) {
  Init();
  SetAttribute("Text", str);
}

JUIButton::JUIButton(const bool b) : JUITextView(false) {
  if (b == true) JUIButton();
}

void JUIButton::Init() {
  obj_ = JUIWindow::GetInstance()->CreateWidget("JUIButton", this);
  if (obj_ == NULL) LOGI("Class initialization failure");
}

JUIButton::~JUIButton() {
  if (obj_ != NULL) {
    jui_helper::JUIWindow::GetInstance()->CloseWidget(obj_);
    obj_ = NULL;
  }
}

void JUIButton::Restore() {
  // Recreate Java Widget when the activity has been disposed
  obj_ = JUIWindow::GetInstance()->CreateWidget("JUIButton", this);
  if (obj_ == NULL) LOGI("Class initialization failure");

  RestoreParameters(map_attributes_);
}

void JUIButton::DispatchEvent(const int32_t message, const int32_t param1,
                              const int32_t param2) {
  if (onclick_callback_ != NULL) onclick_callback_(this, message);
}

bool JUIButton::SetCallback(
    std::function<void(jui_helper::JUIView *, const int32_t)> callback) {
  bool b = true;
  onclick_callback_ = callback;
  return b;
}

}  // namespace jui_helper

