/*
 * Copyright 2022 Google LLC
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

package com.google.crossdevice.sample.rps.model

/** Class to enumerate game choices and rules */
enum class GameChoice {
  ROCK,
  PAPER,
  SCISSORS;

  fun beats(other: GameChoice): Boolean {
    return other == inferior()
  }

  fun superior(): GameChoice {
    return when (this) {
      ROCK -> PAPER
      PAPER -> SCISSORS
      SCISSORS -> ROCK
    }
  }

  fun inferior(): GameChoice {
    return when (this) {
      ROCK -> SCISSORS
      PAPER -> ROCK
      SCISSORS -> PAPER
    }
  }
}
