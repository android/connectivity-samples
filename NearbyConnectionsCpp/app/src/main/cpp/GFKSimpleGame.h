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
/*
 *  GFKSimple.h
 *      Simple Game Object to show case the nearby connection native interface
 */
#ifndef _USR_LOCAL_GOOGLE_ANDROID_NEARBYCONNECTIONS_JNI_GFKSIMPLEGAME_H_
#define _USR_LOCAL_GOOGLE_ANDROID_NEARBYCONNECTIONS_JNI_GFKSIMPLEGAME_H_


namespace game_helper {
const int MAX_QUESTION_LEN = 32;

class GFKSimple {
 public:
  GFKSimple();
  GFKSimple(int choiceCount, int gradeLevel);
  ~GFKSimple();

  bool          SetChoicesPerQuestion(int choiceCount);
  int32_t       GetChoicesPerQuestion(void);
  const char   *GetQuestion(void);
  const int    *GetAllChoices(void);
  int32_t       GetCorrectChoice(void);
  bool          GetSupportedGrade(int *minGrade, int *maxGrade);

 private:
  std::unique_ptr<int32_t []>      choices_;
  int32_t       choice_count_;
  char          question_[MAX_QUESTION_LEN];
  int32_t       level_;
  int32_t       max_operand_;
  int32_t       operation_count_;
  void          Init(void);
  int32_t       GetOperand(void);
  char          GetOperator(void);
};
}  // namespace game_helper
#endif  // _USR_LOCAL_GOOGLE_ANDROID_NEARBYCONNECTIONS_JNI_GFKSIMPLEGAME_H_
