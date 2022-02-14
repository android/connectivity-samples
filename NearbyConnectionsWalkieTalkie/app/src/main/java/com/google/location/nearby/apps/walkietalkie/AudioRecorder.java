package com.google.location.nearby.apps.walkietalkie;

import static android.os.Process.THREAD_PRIORITY_AUDIO;
import static android.os.Process.setThreadPriority;
import static com.google.location.nearby.apps.walkietalkie.Constants.TAG;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import java.io.IOException;
import java.io.OutputStream;

/**
 * When created, you must pass a {@link ParcelFileDescriptor}. Once {@link #start()} is called, the
 * file descriptor will be written to until {@link #stop()} is called.
 */
public class AudioRecorder {
  /** The stream to write to. */
  private final OutputStream mOutputStream;

  /**
   * If true, the background thread will continue to loop and record audio. Once false, the thread
   * will shut down.
   */
  private volatile boolean mAlive;

  /** The background thread recording audio for us. */
  private Thread mThread;

  /**
   * A simple audio recorder.
   *
   * @param file The output stream of the recording.
   */
  public AudioRecorder(ParcelFileDescriptor file) {
    mOutputStream = new ParcelFileDescriptor.AutoCloseOutputStream(file);
  }

  /** @return True if actively recording. False otherwise. */
  public boolean isRecording() {
    return mAlive;
  }

  /** Starts recording audio. */
  public void start() {
    if (isRecording()) {
      Log.w(TAG, "Already running");
      return;
    }

    mAlive = true;
    mThread =
        new Thread() {
          @Override
          public void run() {
            setThreadPriority(THREAD_PRIORITY_AUDIO);

            Buffer buffer = new Buffer();
            @SuppressLint("MissingPermission") AudioRecord record =
                new AudioRecord(
                    MediaRecorder.AudioSource.DEFAULT,
                    buffer.sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    buffer.size);

            if (record.getState() != AudioRecord.STATE_INITIALIZED) {
              Log.w(TAG, "Failed to start recording");
              mAlive = false;
              return;
            }

            record.startRecording();

            // While we're running, we'll read the bytes from the AudioRecord and write them
            // to our output stream.
            try {
              while (isRecording()) {
                int len = record.read(buffer.data, 0, buffer.size);
                if (len >= 0 && len <= buffer.size) {
                  mOutputStream.write(buffer.data, 0, len);
                  mOutputStream.flush();
                } else {
                  Log.w(TAG, "Unexpected length returned: " + len);
                }
              }
            } catch (IOException e) {
              Log.e(TAG, "Exception with recording stream", e);
            } finally {
              stopInternal();
              try {
                record.stop();
              } catch (IllegalStateException e) {
                Log.e(TAG, "Failed to stop AudioRecord", e);
              }
              record.release();
            }
          }
        };
    mThread.start();
  }

  private void stopInternal() {
    mAlive = false;
    try {
      mOutputStream.close();
    } catch (IOException e) {
      Log.e(TAG, "Failed to close output stream", e);
    }
  }

  /** Stops recording audio. */
  public void stop() {
    stopInternal();
    try {
      mThread.join();
    } catch (InterruptedException e) {
      Log.e(TAG, "Interrupted while joining AudioRecorder thread", e);
      Thread.currentThread().interrupt();
    }
  }

  private static class Buffer extends AudioBuffer {
    @Override
    protected boolean validSize(int size) {
      return size != AudioRecord.ERROR && size != AudioRecord.ERROR_BAD_VALUE;
    }

    @Override
    protected int getMinBufferSize(int sampleRate) {
      return AudioRecord.getMinBufferSize(
          sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
    }
  }
}
