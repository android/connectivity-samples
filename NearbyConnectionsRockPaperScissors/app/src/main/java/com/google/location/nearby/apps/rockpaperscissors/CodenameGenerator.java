package com.google.location.nearby.apps.rockpaperscissors;

import java.util.Random;

/** Utility class to generate random Android names */
public final class CodenameGenerator {
  private static final String[] COLORS =
      new String[] {
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
        "Magenta",
      };

  private static final String[] TREATS =
      new String[] {
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
      };

  private static final Random generator = new Random();

  private CodenameGenerator() {}

  /** Generate a random Android agent codename */
  public static String generate() {
    String color = COLORS[generator.nextInt(COLORS.length)];
    String treat = TREATS[generator.nextInt(TREATS.length)];
    return color + " " + treat;
  }
}
