/*
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef _USR_LOCAL_GOOGLE_SAMPLES_ANDROID_NEARBYCONNECTIONS_JNI_JUI_HELPER_JAVAUI_VIEW_H_
#define _USR_LOCAL_GOOGLE_SAMPLES_ANDROID_NEARBYCONNECTIONS_JNI_JUI_HELPER_JAVAUI_VIEW_H_

namespace jui_helper {

/*
 * Layout parameters for AddRule() call.
 * Similar behavior to Java RelativeLayout definitions
 */
enum LayoutParameterType {
  LAYOUT_PARAMETER_UNKNOWN = -2,
  LAYOUT_PARAMETER_TRUE = -1,
  LAYOUT_PARAMETER_LEFT_OF = 0,
  LAYOUT_PARAMETER_RIGHT_OF = 1,
  LAYOUT_PARAMETER_ABOVE = 2,
  LAYOUT_PARAMETER_BELOW = 3,
  LAYOUT_PARAMETER_ALIGN_BASELINE = 4,
  LAYOUT_PARAMETER_ALIGN_LEFT = 5,
  LAYOUT_PARAMETER_ALIGN_TOP = 6,
  LAYOUT_PARAMETER_ALIGN_RIGHT = 7,
  LAYOUT_PARAMETER_ALIGN_BOTTOM = 8,
  LAYOUT_PARAMETER_ALIGN_PARENT_LEFT = 9,
  LAYOUT_PARAMETER_ALIGN_PARENT_TOP = 10,
  LAYOUT_PARAMETER_ALIGN_PARENT_RIGHT = 11,
  LAYOUT_PARAMETER_ALIGN_PARENT_BOTTOM = 12,
  LAYOUT_PARAMETER_CENTER_IN_PARENT = 13,
  LAYOUT_PARAMETER_CENTER_HORIZONTAL = 14,
  LAYOUT_PARAMETER_CENTER_VERTICAL = 15,
  LAYOUT_PARAMETER_START_OF = 16,
  LAYOUT_PARAMETER_END_OF = 17,
  LAYOUT_PARAMETER_ALIGN_START = 18,
  LAYOUT_PARAMETER_ALIGN_END = 19,
  LAYOUT_PARAMETER_ALIGN_PARENT_START = 20,
  LAYOUT_PARAMETER_ALIGN_PARENT_END = 21,
  LAYOUT_PARAMETER_COUNT = 22,
};

/*
 * Callback type for event callbacks in widgets.
 * Parameter for SetCallback() call.
 */
enum JUICallbackType {
  JUICALLBACK_SEEKBAR_STOP_TRACKING_TOUCH = 1,
  JUICALLBACK_SEEKBAR_START_TRACKING_TOUCH = 2,
  JUICALLBACK_SEEKBAR_PROGRESSCHANGED = 3,
  JUICALLBACK_COMPOUNDBUTTON_CHECKED = 4,
  JUICALLBACK_BUTTON_DOWN = 5,
  JUICALLBACK_BUTTON_UP = 6,
  JUICALLBACK_BUTTON_CANCELED = 7,

  JUICALLBACK_DIALOG_DISMISSED = 108,
  JUICALLBACK_DIALOG_CANCELLED = 109,
};

/*
 * Gravity attribute settings
 */
enum AttributeGravityType {
  ATTRIBUTE_GRAVITY_TOP = 0x30,
  ATTRIBUTE_GRAVITY_BOTTOM = 0x50,
  ATTRIBUTE_GRAVITY_LEFT = 0x03,
  ATTRIBUTE_GRAVITY_RIGHT = 0x05,
  ATTRIBUTE_GRAVITY_CENTER_VERTICAL = 0x10,
  ATTRIBUTE_GRAVITY_FILL_VERTICAL = 0x70,
  ATTRIBUTE_GRAVITY_CENTER_HORIZONTAL = 0x01,
  ATTRIBUTE_GRAVITY_FILL_HORIZONTAL = 0x07,
  ATTRIBUTE_GRAVITY_CENTER = 0x11,
  ATTRIBUTE_GRAVITY_FILL = 0x77,
  ATTRIBUTE_GRAVITY_CLIP_VERTICAL = 0x80,
  ATTRIBUTE_GRAVITY_CLIP_HORIZONTAL = 0x08,
  ATTRIBUTE_GRAVITY_START = 0x00800003,
  ATTRIBUTE_GRAVITY_END = 0x00800005,
};

/*
 * Attribute unit for SetAttribute() parameter
 */
enum AttributeUnitType {
  ATTRIBUTE_UNIT_PX = 0x0,
  ATTRIBUTE_UNIT_DIP = 0x1,
  ATTRIBUTE_UNIT_SP = 0x2,
  ATTRIBUTE_UNIT_PT = 0x3,
  ATTRIBUTE_UNIT_IN = 0x4,
  ATTRIBUTE_UNIT_MM = 0x5,
};

/*
 * Size attribute for AddRule()
 */
enum AttributeSizeType {
  ATTRIBUTE_SIZE_MATCH_PARENT = -1,
  ATTRIBUTE_SIZE_WRAP_CONTENT = -2,
};

/*
 * Linear layout orientation
 */
enum LayoutOrientationType {
  LAYOUT_ORIENTATION_HORIZONTAL = 0,
  LAYOUT_ORIENTATION_VERTICAL = 1,
};

/*
 * Enum for alert dialog button
 */
enum AlertDialogButtonType {
  ALERTDIALOG_BUTTON_NEGATIVE = -2,
  ALERTDIALOG_BUTTON_NEUTRAL = -3,
  ALERTDIALOG_BUTTON_POSITIVE = -1,
};

/*
 * ProgressBar style
 */
enum ProgressBarStyleType {
  PROGRESS_BAR_STYLE_DEFAULT = 0x01010077,
  PROGRESS_BAR_STYLE_HIROZONTAL = 0x01010078,
  PROGRESS_BAR_STYLE_SMALL = 0x01010079,
  PROGRESS_BAR_STYLE_LARGE = 0x101007A,
  PROGRESS_BAR_STYLE_INVERSE = 0x01010287,
  PROGRESS_BAR_STYLE_SMALL_INVERSE = 0x01010288,
  PROGRESS_BAR_STYLE_LARGE_INVARSE = 0x01010289,
  PROGRESS_BAR_STYLE_SMALL_TITLE = 0x0101020f,
};

enum ViewVisibility {
  VIEW_VISIVILITY_VISIBLE = 0,
  VIEW_VISIVILITY_INVISIBLE = 4,
  VIEW_VISIVILITY_GONE = 8,
};

/*
 * Internal enums for attribute parameter type
 */
enum AttributeParapeterType {
  ATTRIBUTE_PARAMETER_INT,
  ATTRIBUTE_PARAMETER_FLOAT,
  ATTRIBUTE_PARAMETER_BOOLEAN,
  ATTRIBUTE_PARAMETER_STRING,
  ATTRIBUTE_PARAMETER_IF,  // parameters of int32_t, float
  ATTRIBUTE_PARAMETER_FF,  // parameters of 2 floats
  ATTRIBUTE_PARAMETER_III,  // parameters of int32_t, int32_t, int32_t
  ATTRIBUTE_PARAMETER_IIII,  // parameters of int32_t, int32_t, int32_t, int32_t
  ATTRIBUTE_PARAMETER_FFFI,  // parameters of float, float, float, int32_t
};

/*
 * Internal structure to store attribute parameters
 */
struct AttributeParameterStore {
  AttributeParapeterType type;
  union {
    int32_t i;
    float f;
    bool b;
    std::string *str;
    struct {
      int32_t i1;
      float f2;
    } param_if;
    struct {
      float f1;
      float f2;
    } param_ff;
    struct {
      int32_t i1;
      int32_t i2;
      int32_t i3;
    } param_iii;
    struct {
      float f1;
      float f2;
      float f3;
      int32_t i;
    } param_fffi;
    struct {
      int32_t i1;
      int32_t i2;
      int32_t i3;
      int32_t i4;
    } param_iiii;
  };
};

struct AttributeType {
  const char *attribute_name;
  const int32_t attribute_type;
};

/*
 * class IdFactory: cache UI pointers and generate an ID, send ID to Java side
 *                  when get id back from Java, retrieve the UI pointer for that
 *                  ID, and continue. Mainly purpose is for things to be called
 *                  back on UI thread
 */
class JUIBase;
class IdFactory {
   public:
      int32_t   getId(const JUIBase* ui_object);
      JUIBase*  getUIBase(int32_t ui_id);
      void      debugDumpCurrentHashTable(void);
      bool      insert(const JUIBase* ui_object);
      bool      remove(const JUIBase* ui_object);

   private:
      std::unordered_map<const JUIBase*, int32_t> ids_;
      static int32_t cur_id_;
};

/*
 * Base class of JUIView
 */
class JUIBase {
public:
  JUIBase() : obj_(NULL) {  id_factory_.insert(this); }
  virtual ~JUIBase() {  id_factory_.remove(this); }

  /*
   * Dispatch Widget events. This one is called from Java code through
   * Java_com_sample_helper_JUIHelper_JUICallbackHandler()
   */
  virtual void DispatchEvent(const int32_t message, const int32_t param1,
                             const int32_t param2) {}

  /*
   * Template for 1 parameter version of SetAttribute
   */
  template <typename T>
  bool SetAttribute(std::unordered_map<std::string, int32_t> &map,
                    const char *strAttribute, const T t) {
    LOGI("Attribute '%s' updating", strAttribute);
    auto it = map.find(strAttribute);
    if (it != map.end()) {
      std::string s = std::string("set");
      s += it->first;

      AttributeParameterStore &p = map_attribute_parameters[it->first];
      switch (it->second) {
        case ATTRIBUTE_PARAMETER_INT:
          ndk_helper::JNIHelper::GetInstance()->CallVoidMethod(
              obj_, s.c_str(), "(I)V", t);
          p.type = ATTRIBUTE_PARAMETER_INT;
          p.i = t;
          break;
        case ATTRIBUTE_PARAMETER_FLOAT:
          ndk_helper::JNIHelper::GetInstance()->CallVoidMethod(
              obj_, s.c_str(), "(F)V", t);
          p.type = ATTRIBUTE_PARAMETER_FLOAT;
          p.f = t;
          break;
        case ATTRIBUTE_PARAMETER_BOOLEAN:
          ndk_helper::JNIHelper::GetInstance()->CallVoidMethod(
              obj_, s.c_str(), "(Z)V", t);
          p.type = ATTRIBUTE_PARAMETER_BOOLEAN;
          p.b = t;
          break;
        default:
          LOGI("Attribute parameter does not match : %s", strAttribute);
          break;
          }
    } else {
      LOGI("Attribute '%s' not found", strAttribute);
      return false;
    }
    return true;
  }

  /*
   * Specialized Template for string version of SetAttribute
   */
  bool SetAttribute(std::unordered_map<std::string, int32_t> &map,
                    const char *strAttribute, const char *str) {
    auto it = map.find(strAttribute);
    if (it != map.end()) {
      std::string s = std::string("set");
      s += it->first;

      AttributeParameterStore &p = map_attribute_parameters[it->first];
      switch (it->second) {
        case ATTRIBUTE_PARAMETER_STRING: {
          JNIEnv *env =
              ndk_helper::JNIHelper::GetInstance()->AttachCurrentThread();
          jstring string = env->NewStringUTF(str);
          jstring stringGlobal = (jstring)env->NewGlobalRef(string);
          ndk_helper::JNIHelper::GetInstance()->CallVoidMethod(
              obj_, s.c_str(), "(Ljava/lang/CharSequence;)V", stringGlobal);
          env->DeleteGlobalRef(stringGlobal);
          env->DeleteLocalRef(string);

          p.type = ATTRIBUTE_PARAMETER_STRING;
          if (p.str != NULL) {
            if (p.str->compare(str) != 0) {
              delete p.str;
              p.str = new std::string(str);
            }
          } else {
            p.str = new std::string(str);
          }
        } break;
        default:
          LOGI("Attribute parameter does not match : %s", strAttribute);
          break;
      }
    } else {
      LOGI("Attribute '%s' not found", strAttribute);
      return false;
    }
    return true;
  }

  /*
   * Template for 2 parameters version of SetAttribute
   */
  template <typename T, typename T2>
  bool SetAttribute(std::unordered_map<std::string, int32_t> &map,
                    const char *strAttribute, T t, T2 t2) {
    auto it = map.find(strAttribute);
    if (it != map.end()) {
      std::string s = std::string("set");
      s += it->first;

      AttributeParameterStore &p = map_attribute_parameters[it->first];
      switch (it->second) {
        case ATTRIBUTE_PARAMETER_IF:
          ndk_helper::JNIHelper::GetInstance()->CallVoidMethod(
              obj_, s.c_str(), "(IF)V", static_cast<int32_t>(t),
              static_cast<float>(t2));
          p.type = ATTRIBUTE_PARAMETER_IF;
          p.param_if.i1 = static_cast<int32_t>(t);
          p.param_if.f2 = static_cast<float>(t2);
          break;
        case ATTRIBUTE_PARAMETER_FF:
          ndk_helper::JNIHelper::GetInstance()->CallVoidMethod(
              obj_, s.c_str(), "(FF)V", static_cast<float>(t),
              static_cast<float>(t2));
          p.type = ATTRIBUTE_PARAMETER_FF;
          p.param_ff.f1 = static_cast<float>(t);
          p.param_ff.f2 = static_cast<float>(t2);
          break;
        default:
          LOGI("Attribute parameter does not match : %s", strAttribute);
          break;
      }
    } else {
      LOGI("Attribute '%s' not found", strAttribute);
      return false;
    }
    return true;
  }

  /*
   * Template for 4 parameters version of SetAttribute
   */
  template <typename T, typename T2, typename T3, typename T4>
  bool SetAttribute(std::unordered_map<std::string, int32_t> &map,
                    const char *strAttribute, T p1, T2 p2, T3 p3, T4 p4) {
    auto it = map.find(strAttribute);
    if (it != map.end()) {
      std::string s = std::string("set");
      s += it->first;

      AttributeParameterStore &p = map_attribute_parameters[it->first];
      switch (it->second) {
        case ATTRIBUTE_PARAMETER_IIII:
          ndk_helper::JNIHelper::GetInstance()->CallVoidMethod(
              obj_, s.c_str(), "(IIII)V", static_cast<int32_t>(p1),
              static_cast<int32_t>(p2), static_cast<int32_t>(p3),
              static_cast<int32_t>(p4));
          p.type = ATTRIBUTE_PARAMETER_IIII;
          p.param_iiii.i1 = static_cast<int32_t>(p1);
          p.param_iiii.i2 = static_cast<int32_t>(p2);
          p.param_iiii.i3 = static_cast<int32_t>(p3);
          p.param_iiii.i4 = static_cast<int32_t>(p4);
          break;
        case ATTRIBUTE_PARAMETER_FFFI:
          ndk_helper::JNIHelper::GetInstance()->CallVoidMethod(
              obj_, s.c_str(), "(FFFI)V", static_cast<float>(p1),
              static_cast<float>(p2), static_cast<float>(p3),
              static_cast<int32_t>(p4));
          p.type = ATTRIBUTE_PARAMETER_FFFI;
          p.param_fffi.f1 = static_cast<float>(p1);
          p.param_fffi.f2 = static_cast<float>(p2);
          p.param_fffi.f3 = static_cast<float>(p3);
          p.param_fffi.i = static_cast<int32_t>(p4);
          break;
        default:
          LOGI("Attribute parameter does not match : %s", strAttribute);
          break;
      }
    } else {
      LOGI("Attribute '%s' not found", strAttribute);
      return false;
    }
    return true;
  }

  /*
   * Retrieve attribute
   */
  template <typename T>
  bool GetAttribute(std::unordered_map<std::string, int32_t> &map,
                    const char *strAttribute, T *p_value) {
    T ret;
    auto it = map.find(strAttribute);
    if (it != map.end()) {
      std::string s = std::string("get");
      s += it->first;

      switch (it->second) {
        case ATTRIBUTE_PARAMETER_INT:
          ret = (T)ndk_helper::JNIHelper::GetInstance()->CallIntMethod(
              obj_, s.c_str(), "()I");
          break;
        case ATTRIBUTE_PARAMETER_FLOAT:
          ret = (T)ndk_helper::JNIHelper::GetInstance()->CallFloatMethod(
              obj_, s.c_str(), "()F");
          break;
        case ATTRIBUTE_PARAMETER_BOOLEAN:
          ret = (T)ndk_helper::JNIHelper::GetInstance()->CallBooleanMethod(
              obj_, s.c_str(), "()Z");
          break;
        default:
          ret = 0;
          break;
      }
    } else {
      LOGI("Attribute '%s' not found", strAttribute);
      return false;
    }
    *p_value = ret;
    return true;
  }

  static IdFactory id_factory_;

protected:
  std::unordered_map<std::string, AttributeParameterStore>
      map_attribute_parameters;
  jobject obj_;
  jobject GetJobject() { return obj_; }
};

/*
 * JUIView class
 */
class JUIView : public JUIBase {
  friend class JUIWindow;
  friend class JUIDialog;

 public:
  JUIView();
  virtual ~JUIView();

  /*
   * Add layout rule to the widget
   */
  void AddRule(const int32_t layoutParameterIndex, const int32_t parameter);
  void AddRule(const int32_t layoutParameterIndex, const JUIView *parameter);
  /*
   * Set LayoutParams for RelativeLayout
   */
  void SetLayoutParams(const int32_t width, const int32_t height);
  /*
   * Set LayoutParams for LinearLayout
   */
  void SetLayoutParams(const int32_t width, const int32_t height,
                       const float f);

  /*
   * Set Margins
   */
  void SetMargins(const int32_t left, const int32_t top, const int32_t right,
                  const int32_t bottom);

  /*
   * Set attribute of the widget
   * See attributes_ for available attribute names
   */
  template <typename T>
  bool SetAttribute(const char *strAttribute, const T t) {
    return SetAttribute(map_attributes_, strAttribute, t);
  }

  bool SetAttribute(const char *strAttribute, const char *str) {
    return JUIBase::SetAttribute(map_attributes_, strAttribute, str);
  }

  template <typename T, typename T2>
  bool SetAttribute(const char *strAttribute, T t, T2 t2) {
    return JUIBase::SetAttribute(map_attributes_, strAttribute, t, t2);
  }

  template <typename T, typename T2, typename T3, typename T4>
  bool SetAttribute(const char *strAttribute, T p1, T2 p2, T3 p3, T4 p4) {
    return JUIBase::SetAttribute(map_attributes_, strAttribute, p1, p2, p3, p4);
  }

  template <typename T>
  bool GetAttributeA(const char *strAttribute, T *value) {
    return JUIBase::GetAttribute(map_attributes_, strAttribute, value);
  }

 private:
  const static AttributeType attributes_[];
  int32_t array_current_rules_[LAYOUT_PARAMETER_COUNT];

  int32_t layoutWidth_;
  int32_t layoutHeight_;
  float layoutWeight_;

  int32_t marginLeft_;
  int32_t marginRight_;
  int32_t marginTop_;
  int32_t marginBottom_;

 protected:
  static std::unordered_map<std::string, int32_t> map_attributes_;

  void RestoreParameters(std::unordered_map<std::string, int32_t> &map);
  virtual void Restore() = 0;
};

}  // namespace jui_helper

#endif  // _USR_LOCAL_GOOGLE_SAMPLES_ANDROID_NEARBYCONNECTIONS_JNI_JUI_HELPER_JAVAUI_VIEW_H_

