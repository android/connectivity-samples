package com.google.location.nearby.apps.walkietalkie;

import static android.os.Process.THREAD_PRIORITY_AUDIO;
import static android.os.Process.setThreadPriority;
import static com.google.location.nearby.apps.walkietalkie.Constants.TAG;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;

/**
 * A fire-once class. When created, you must pass a {@link InputStream}. Once {@link #start()} is
 * called, the input stream will be read from until either {@link #stop()} is called or the stream
 * ends.
 */
public class AudioPlayer {
  /** The audio stream we're reading from. */
  private final InputStream mInputStream;

  /**
   * If true, the background thread will continue to loop and play audio. Once false, the thread
   * will shut down.
   */
  private volatile boolean mAlive;

  /** The background thread recording audio for us. */
  private Thread mThread;

  /**
   * A simple audio player.
   *
   * @param inputStream The input stream of the recording.
   */
  public AudioPlayer(InputStream inputStream) {
    mInputStream = inputStream;
  }

  /** @return True if currently playing. */
  public boolean isPlaying() {
    return mAlive;
  }

  /** Starts playing the stream. */
  public void start() {
    mAlive = true;
    mThread =
        new Thread() {
          @Override
          public void run() {
            setThreadPriority(THREAD_PRIORITY_AUDIO);

            Buffer buffer = new Buffer();
            AudioTrack audioTrack =
                new AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    buffer.sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    buffer.size,
                    AudioTrack.MODE_STREAM);
            audioTrack.play();

            int len;
            try {
              while (isPlaying() && (len = mInputStream.read(buffer.data)) > 0) {
                audioTrack.write(buffer.data, 0, len);
              }
            } catch (IOException e) {
              Log.e(TAG, "Exception with playing stream", e);
            } finally {
              stopInternal();
              audioTrack.release();
              onFinish();
            }
          }
        };
    mThread.start();
  }

  private void stopInternal() {
    mAlive = false;
    try {
      mInputStream.close();
    } catch (IOException e) {
      Log.e(TAG, "Failed to close input stream", e);
    }
  }

  /** Stops playing the stream. */
  public void stop() {
    stopInternal();
    try {
      mThread.join();
    } catch (InterruptedException e) {
      Log.e(TAG, "Interrupted while joining AudioRecorder thread", e);
      Thread.currentThread().interrupt();
    }
  }

  /** The stream has now ended. */
  protected void onFinish() {}

  private static class Buffer extends AudioBuffer {
    @Override
    protected boolean validSize(int size) {
      return size != AudioTrack.ERROR && size != AudioTrack.ERROR_BAD_VALUE;
    }

    @Override
    protected int getMinBufferSize(int sampleRate) {
      return AudioTrack.getMinBufferSize(
          sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
    }
  }
}
