package com.ism.fourthapp;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v4.provider.DocumentFile;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadFile extends IntentService {

    public final static String NOTIFICATION = "DownloadFile_SERVICE";
    public static Uri uri = null;

    // Constructor
    public DownloadFile() {
        super("DownloadFile");
    }

    @Override
    // Download file
    protected void onHandleIntent(@Nullable Intent intent) {
        // Get url from intent
        String address = intent.getStringExtra("URL");
        // Conecction with url
        HttpURLConnection connection = null;
        // Stream for save content
        OutputStream streamToFile = null;
        try {
            // Get filename from URL
            URL url = new URL(address);
            File file = new File(url.getFile());
            String fileName = file.getName();
            // Create new file
            DocumentFile directory = DocumentFile.fromTreeUri(this, uri);
            DocumentFile dFile = directory.createFile("", fileName);
            // Make connection
            connection = (HttpURLConnection) url.openConnection();
            // Stream for fetching data
            DataInputStream dataInputStream = new DataInputStream(connection.getInputStream());
            // Initialize stream for writing
            streamToFile = getContentResolver().openOutputStream(dFile.getUri());

            //streamToFile = new FileOutputStream(f);
            // Buffor for data
            byte buffor[] = new byte[100];
            // Amount of readed data
            int gotBytes = dataInputStream.read(buffor, 0, 100);
            // Total readed data
            int totalBytes = 0;
            // Read untill end of file
            while (gotBytes != -1) {
                // Write read data to buffer
                streamToFile.write(buffor, 0, gotBytes);
                // How many bytes read
                gotBytes = dataInputStream.read(buffor, 0, 100);
                // Add readed bytes to total counter
                totalBytes += gotBytes;
                // Send message with readed bytes
                sendBroadcast(totalBytes);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Close connetions
            if (connection != null)
                connection.disconnect();

            if (streamToFile != null) {
                try {
                    streamToFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // Static method for running service
    public static void start(Context context, String address, Uri uri) {
        DownloadFile.uri = uri;
        Intent intent = new Intent(context, DownloadFile.class);
        intent.putExtra("URL", address);
        context.startService(intent);

    }

    // method for sending messages
    private void sendBroadcast(int value) {
        Intent intent = new Intent(NOTIFICATION);
        intent.putExtra("downloaded", value);
        sendBroadcast(intent);
    }

}
