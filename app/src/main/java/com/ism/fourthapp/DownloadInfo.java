package com.ism.fourthapp;

import android.os.AsyncTask;

import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadInfo extends AsyncTask<URL, Void, Void> {

    // Size of file
    int size;
    // Kind of file
    String kind;

    // Interface used to communication with main activity
    public interface iDownloadInfo {
        void updateInformation(String size, String fileKind);
    }

    // Listener got from main activity
    iDownloadInfo listener;

    // Constructor
    public DownloadInfo(MainActivity context) {
        this.listener = (iDownloadInfo) context;
    }

    // Action invoked in background
    @Override
    protected Void doInBackground(URL... url) {
        // Connection instance
        HttpURLConnection connection = null;
        try {
            // Initialize connection
            connection = (HttpURLConnection) url[0].openConnection();
//            connection.setRequestMethod("GET");
//            connection.setDoOutput(true);
//            connection.setRequestProperty("Accept-Encoding", "identity");
            // Get information about file
            size = connection.getContentLength();
            kind = connection.getContentType();
        } catch (Exception e) {
        } finally {
            // Close connection
            if (connection != null)
                connection.disconnect();
        }
        return null;
    }

    // After end of action
    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        // Update UI
        listener.updateInformation(size + "", kind);
    }

}
