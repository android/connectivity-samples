package com.example.android.networkconnect;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class NetworkHandler extends Handler {
    public static int DOWNLOAD_URL = 0;
    DownloadCallback mDcb;
    public NetworkHandler(Looper looper, DownloadCallback dc) {
        super(looper);
        mDcb = dc;
    }
    @Override
    public void handleMessage(Message msg) {
        if (msg.what == 0 && msg.obj instanceof String) {
            handleDownloadTask((String) msg.obj);
        }
    }

    public Message createDownload(String inUrl) {
        return this.obtainMessage(DOWNLOAD_URL, inUrl);
    }

    private void handleDownloadTask(String inUrl) {
        if (mDcb != null) {
            NetworkInfo networkInfo = mDcb.getActiveNetworkInfo();
            if (networkInfo == null || !networkInfo.isConnected() ||
                    (networkInfo.getType() != ConnectivityManager.TYPE_WIFI
                            && networkInfo.getType() != ConnectivityManager.TYPE_MOBILE)) {
                // If no connectivity, cancel task and update Callback with null data.
                mDcb.updateFromDownload(null);
                return;
            }
        }
        Result result = null;
        if (inUrl != null && inUrl.length() > 0) {
            try {
                URL url = new URL(inUrl);
                String resultString = downloadUrl(url);
                if (resultString != null) {
                    result = new Result(resultString);
                } else {
                    throw new IOException("No response received.");
                }
            } catch(Exception e) {
                result = new Result(e);
            }
            if (result.mException != null) {
                mDcb.updateFromDownload(result.mException.getMessage());
            } else if (result.mResultValue != null) {
                mDcb.updateFromDownload(result.mResultValue);
            }
            mDcb.finishDownloading();
        }

    }

    /**
     * Wrapper class that serves as a union of a result value and an exception. When the
     * download task has completed, either the result value or exception can be a non-null
     * value. This allows you to pass exceptions to the UI thread that were thrown during
     * doInBackground().
     */
    private static class Result {
        public String mResultValue;
        public Exception mException;
        public Result(String resultValue) {
            mResultValue = resultValue;
        }
        public Result(Exception exception) {
            mException = exception;
        }
    }

    /**
     * Given a URL, sets up a connection and gets the HTTP response body from the server.
     * If the network request is successful, it returns the response body in String form. Otherwise,
     * it will throw an IOException.
     */
    private String downloadUrl(URL url) throws IOException {
        InputStream stream = null;
        HttpsURLConnection connection = null;
        String result = null;
        try {
            connection = (HttpsURLConnection) url.openConnection();
            // Timeout for reading InputStream arbitrarily set to 3000ms.
            connection.setReadTimeout(3000);
            // Timeout for connection.connect() arbitrarily set to 3000ms.
            connection.setConnectTimeout(3000);
            // For this use case, set HTTP method to GET.
            connection.setRequestMethod("GET");
            // Already true by default but setting just in case; needs to be true since this request
            // is carrying an input (response) body.
            connection.setDoInput(true);
            // Open communications link (network traffic occurs here).
            connection.connect();
            mDcb.onProgressUpdate(DownloadCallback.Progress.CONNECT_SUCCESS, 0);
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpsURLConnection.HTTP_OK) {
                throw new IOException("HTTP error code: " + responseCode);
            }
            // Retrieve the response body as an InputStream.
            stream = connection.getInputStream();
            mDcb.onProgressUpdate(DownloadCallback.Progress.GET_INPUT_STREAM_SUCCESS, 0);
            if (stream != null) {
                // Converts Stream to String with max length of 500.
                result = readStream(stream, 500);
                mDcb.onProgressUpdate(DownloadCallback.Progress.PROCESS_INPUT_STREAM_SUCCESS, 0);
            }
        } finally {
            // Close Stream and disconnect HTTPS connection.
            if (stream != null) {
                stream.close();
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
        return result;
    }

    /**
     * Converts the contents of an InputStream to a String.
     */
    private String readStream(InputStream stream, int maxLength) throws IOException {
        String result = null;
        // Read InputStream using the UTF-8 charset.
        InputStreamReader reader = new InputStreamReader(stream, "UTF-8");
        // Create temporary buffer to hold Stream data with specified max length.
        char[] buffer = new char[maxLength];
        // Populate temporary buffer with Stream data.
        int numChars = 0;
        int readSize = 0;
        while (numChars < maxLength && readSize != -1) {
            numChars += readSize;
            int pct = (100 * numChars) / maxLength;
            mDcb.onProgressUpdate(DownloadCallback.Progress.PROCESS_INPUT_STREAM_IN_PROGRESS, pct);
            readSize = reader.read(buffer, numChars, buffer.length - numChars);
        }
        if (numChars != -1) {
            // The stream was not empty.
            // Create String that is actual length of response body if actual length was less than
            // max length.
            numChars = Math.min(numChars, maxLength);
            result = new String(buffer, 0, numChars);
        }
        return result;
    }

}
