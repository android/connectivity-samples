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
 *  GFKSimpleGame.cpp
 *  A Simple Game to demo Google Nearby Connections Native Interface
 */
#include <cstddef>
#include <cstdlib>
#include <cstdio>
#include <ctime>
#include <cstring>
#include <cmath>
#include <memory>
#include "GFKSimpleGame.h"
#include "JNIHelper.h"

namespace game_helper {
/*
 * supported grade level: currently only supports first grade math level
 */
const int32_t LEVEL_CAP = 2;
const char OPS_CODE[] = {'+', '-',   // 1st and 2nd grader
                         '*', '/',   // 3rd and above
                         '^', 's'};  // 5th

GFKSimple::GFKSimple() : choice_count_(0), level_(0) {
  Init();
}

GFKSimple::GFKSimple(int32_t choiceCount, int32_t gradeLevel)
    :  choice_count_(choiceCount), level_(gradeLevel - 1) {
  Init();
}

const char *GFKSimple::GetQuestion(void) {
  int32_t op1 = GetOperand();
  int32_t op2 = GetOperand();
  char op = GetOperator();

  snprintf(question_, MAX_QUESTION_LEN, "%d %c %d = ?", op1, op, op2);
  int32_t ans;
  switch (op) {
    case '+':
      ans = op1 + op2;
      break;
    case '-':
      ans = op1 - op2;
      break;
    case '*':
      ans = op1 * op2;
      break;
    case '/':
      ans = op1 / op2;
      break;
    case '^':
      op2 %= 4;
      snprintf(question_, MAX_QUESTION_LEN, "%d %c %d = ?", op1, op, op2);
      ans = pow(op1, op2);
      break;
    case 's':
      ans = sqrt(op1);
      snprintf(question_, MAX_QUESTION_LEN, "square_root(%d) = ?", op1);
      break;
    default:
      // Not Recognized, default to '+'
      ans = op1 + op2;
      LOGE("Wrong operation generated: %c, taking default", op);
      break;
  }
  choices_[choice_count_] = ans;

  int32_t ans_idx = rand() % choice_count_;
  choices_[ans_idx] = ans;
  for (int32_t i = 0; i < ans_idx; i++) choices_[i] = ans + i - ans_idx;
  for (int32_t i = ans_idx + 1; i < choice_count_; i++)
    choices_[i] = ans + i - ans_idx;

  return (const char *)&question_[0];
}
const int32_t *GFKSimple::GetAllChoices(void) { return (const int32_t *)&choices_[0]; }

void GetSupportedGrade(int32_t *minGrade, int32_t *maxGrade) {
  if (!minGrade || !maxGrade) {
    LOGE("NULL pointer to GetSupportedGrade()");
    return;
  }
  if (LEVEL_CAP <= 0) {
    *minGrade = *maxGrade = 0;
  }
  *maxGrade = LEVEL_CAP;
  *minGrade = 1;  // Grades start with 1
}

void GFKSimple::Init(void) {
  srand((unsigned)time(NULL));
  switch (level_) {
    case 0:
    default:
      max_operand_ = 10;
      operation_count_ = 2;
      break;
    case 1:
      max_operand_ = 100;
      operation_count_ = 2;
      break;
    case 3:
      max_operand_ = 100;
      operation_count_ = 4;
      break;
    case 4:
    case 5:
      max_operand_ = 100;
      operation_count_ = 6;
      break;
  }
  choices_ = nullptr;
  if (choice_count_) {
    choices_ = std::unique_ptr<int32_t[]>(new int32_t[choice_count_ + 1]);
    if (choices_) {
      memset(choices_.get(), 0, sizeof(int32_t) * (choice_count_ + 1));
    } else {
      LOGE("Out of the memory in %s at %d", __FILE__, __LINE__);
    }
  }
  if (operation_count_ > (sizeof(OPS_CODE) / sizeof(OPS_CODE[0]))) {
    LOGW("Operation (%d) is bigger than simple game capability(%d)",
         operation_count_, static_cast<int>(sizeof(OPS_CODE)/sizeof(OPS_CODE[0])));
    operation_count_ = (sizeof(OPS_CODE) / sizeof(OPS_CODE[0]));
  }
}

int32_t GFKSimple::GetOperand(void) {
  return rand() % max_operand_;
}

char GFKSimple::GetOperator(void) {
  return OPS_CODE[rand() % operation_count_];
}

bool GFKSimple::SetChoicesPerQuestion(int32_t choiceCount) {
  if (choiceCount == choice_count_) {
    return true;
  }

  std::unique_ptr<int32_t[]> newChoices = std::unique_ptr<int32_t []>(new int32_t[choiceCount + 1]);
  if (!newChoices) {
    LOGE("Out of memory from GFKSimple::GerOperand");
    return false;
  }
  choices_ = std::move(newChoices);
  choice_count_ = choiceCount;
  return true;
}

int32_t GFKSimple::GetChoicesPerQuestion(void) {
  return choice_count_;
}

/*
 * GetCorrectChoice()
 *  The correct answer index is the extra one returned in the answers --
 *  0 -- choice_count_ - 1 :  game choices to be displayed on UI
 *  choice_count_          :  The correct_answer
 */
int32_t GFKSimple::GetCorrectChoice(void) {
  if (nullptr != choices_) {
    return choice_count_;
  }
  return 0;
}

GFKSimple::~GFKSimple() {}
}  // namespace game_helper
