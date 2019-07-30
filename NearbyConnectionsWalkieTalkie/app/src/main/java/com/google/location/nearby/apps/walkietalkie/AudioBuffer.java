package com.google.location.nearby.apps.walkietalkie;

/**
 * A buffer that grabs the smallest supported sample rate for {@link android.media.AudioTrack} and
 * {@link android.media.AudioRecord}.
 */
public abstract class AudioBuffer {
  private static final int[] POSSIBLE_SAMPLE_RATES =
      new int[] {8000, 11025, 16000, 22050, 44100, 48000};

  final int size;
  final int sampleRate;
  final byte[] data;

  protected AudioBuffer() {
    int size = -1;
    int sampleRate = -1;

    // Iterate over all possible sample rates, and try to find the shortest one. The shorter
    // it is, the faster it'll stream.
    for (int rate : POSSIBLE_SAMPLE_RATES) {
      sampleRate = rate;
      size = getMinBufferSize(sampleRate);
      if (validSize(size)) {
        break;
      }
    }

    // If none of them were good, then just pick 1kb
    if (!validSize(size)) {
      size = 1024;
    }

    this.size = size;
    this.sampleRate = sampleRate;
    data = new byte[size];
  }

  protected abstract boolean validSize(int size);

  protected abstract int getMinBufferSize(int sampleRate);
}
