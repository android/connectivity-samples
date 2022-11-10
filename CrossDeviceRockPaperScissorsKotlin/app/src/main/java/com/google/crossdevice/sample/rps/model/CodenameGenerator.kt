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

import java.util.Random

/** Utility class to generate random Android names */
object CodenameGenerator {
  private val COLORS =
    arrayOf(
      "Red",
      "Orange",
      "Yellow",
      "Green",
      "Blue",
      "Indigo",
      "Violet",
      "Purple",
      "Lavender",
      "Fuchsia",
      "Plum",
      "Orchid",
      "Magenta"
    )

  private val TREATS =
    arrayOf(
      "Alpha",
      "Beta",
      "Cupcake",
      "Donut",
      "Eclair",
      "Froyo",
      "Gingerbread",
      "Honeycomb",
      "Ice Cream Sandwich",
      "Jellybean",
      "Kit Kat",
      "Lollipop",
      "Marshmallow",
      "Nougat",
      "Oreo",
      "Pie"
    )

  private val generator = Random()

  /** Generate a random Android agent codename */
  fun generate(): String {
    val color = COLORS[generator.nextInt(COLORS.size)]
    val treat = TREATS[generator.nextInt(TREATS.size)]
    return "$color $treat"
  }
}
