package com.ism.fourthapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.net.MalformedURLException;
import java.net.URL;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity implements DownloadInfo.iDownloadInfo {

    // Declarations of components
    Button downBTN;
    Button infoBTN;
    EditText addressET;
    TextView kindTV;
    TextView sizeTV;
    TextView downSizeTV;
    ProgressBar progressBar;
    BroadcastReceiver broadcastReceiver;
    String CHANNEL_ID = "1";
    int notificationId = 1;
    NotificationCompat.Builder builder;
    NotificationManagerCompat notificationManager;
    long lastUpdateNotificationTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Bar title
        getSupportActionBar().setTitle("Pobierz plik");
        // Initialize components
        initialize();
        // Actions after click buttons
        onClickActions();
        // Example url for test application
        addressET.setText("https://ocs-pl.oktawave.com/v1/AUTH_2887234e-384a-4873-8bc5-405211db13a2/spidersweb/2019/05/nasa-zdjecie.jpg");
        // Permission for writing
        ActivityCompat.requestPermissions(this, new String[]{WRITE_EXTERNAL_STORAGE}, 1);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Get instance of inflater
        MenuInflater menuInflater = getMenuInflater();
        // Inflate menu
        menuInflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // On click add icon at toolbar menu
            case R.id.actionOpen:
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                // Directory path
                Uri uri = Uri.parse("/mnt/sdcard");
                intent.setDataAndType(uri, "*/*");
                startActivity(Intent.createChooser(intent, "Open folder"));
                // Run activity for add new website
                startActivity(intent);
                // Consume event here
                return true;
            default:
                // Parent constructor
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register receiver for messages from downloading service
        registerReceiver(broadcastReceiver, new IntentFilter(DownloadFile.NOTIFICATION));
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister receiver for messages from downloading service
        unregisterReceiver(broadcastReceiver);
    }

    private void initialize() {
        downBTN = findViewById(R.id.main_downBTN);
        infoBTN = findViewById(R.id.main_infoBTN);
        addressET = findViewById(R.id.main_adressET);
        kindTV = findViewById(R.id.main_kindTV);
        sizeTV = findViewById(R.id.main_sizeTV);
        downSizeTV = findViewById(R.id.main_downTV);
        progressBar = findViewById(R.id.main_progressBar);

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Get downloaded size (bytes)
                int downloadedSize = intent.getExtras().getInt("downloaded");
                // Set size to label
                downSizeTV.setText(downloadedSize + "");
                // Get size of whole file
                int all = 0;
                try {
                    all = Integer.parseInt(sizeTV.getText().toString());
                } catch (Exception e) {
                }
                // Wait for get file size
                if (all != 0) {
                    // Get downloaded size in percent
                    int percent = (int) ((100.0 / all) * downloadedSize);
                    // Set new progress
                    progressBar.setProgress(percent);
                    // For notification bar
                    updateNotificationBar(percent);
                    // After downloaded
                    if (percent >= 98) {
                        sizeTV.setText(all + "");
                        // Hide progress bar
                        progressBar.setVisibility(View.GONE);
                        // Show button
                        downBTN.setVisibility(View.VISIBLE);
                        // Notification bar
                        endNotificationBar();
                    }
                }

            }
        };
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case 9999:
                if (resultCode == RESULT_OK) {
                    // Hide download button
                    downBTN.setVisibility(View.GONE);
                    // Show progress bar
                    progressBar.setVisibility(View.VISIBLE);
                    // Show notification bar
                    startNotificationBar();
                    // Get file url
                    String address = addressET.getText().toString();
                    // Start service for downloading
                    DownloadFile.start(MainActivity.this, address, data.getData());
                    break;
                }

        }
    }

    private void onClickActions() {
        // Action on click button for download file
        downBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Click button for get information about total file size
                infoBTN.callOnClick();
                // Get file url
                String address = addressET.getText().toString();
                //

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                    i.addCategory(Intent.CATEGORY_DEFAULT);
                    startActivityForResult(Intent.createChooser(i, "Choose directory"), 9999);
                }
            }
        });
        // Action on click button for download information about file
        infoBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get file url
                String address = addressET.getText().toString();
                try {
                    // Start fetching information about file
                    new DownloadInfo(MainActivity.this).execute(new URL(address));
                } catch (MalformedURLException e) {
                }
            }
        });
    }

    @Override
    public void updateInformation(String size, String fileKind) {
        // Update labels
        sizeTV.setText(size);
        kindTV.setText(fileKind);
    }

    private void startNotificationBar() {
        createNotificationChannel();
        notificationManager = NotificationManagerCompat.from(this);
        builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        // Configuration
        builder.setContentTitle("File Download")
                .setContentText("Download in progress")
                .setSmallIcon(R.drawable.ic_notification)
                .setVibrate(null)
                .setPriority(NotificationCompat.PRIORITY_LOW);
        // Issue the initial notification with zero progress
        int PROGRESS_MAX = 100;
        int PROGRESS_CURRENT = 0;
        // Set initial progress
        builder.setProgress(PROGRESS_MAX, PROGRESS_CURRENT, false);
        // Show
        notificationManager.notify(notificationId, builder.build());

    }

    // Update progress bar on notification
    private void updateNotificationBar(int percent) {
        if (System.currentTimeMillis() - lastUpdateNotificationTime >= 1000) {
            builder.setProgress(100, percent, false);
            notificationManager.notify(notificationId, builder.build());
            lastUpdateNotificationTime = System.currentTimeMillis();
        }
    }

    // After successfull download
    private void endNotificationBar() {
        builder.setContentText("Download complete");
        builder.setProgress(0, 0, false);
        notificationManager.notify(notificationId, builder.build());
        lastUpdateNotificationTime = System.currentTimeMillis();
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "notification";
            String description = "downloadNotification";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                channel = new NotificationChannel(CHANNEL_ID, name, importance);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                channel.setDescription(description);
                channel.setVibrationPattern(new long[]{0});
                channel.enableVibration(true);
            }
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                notificationManager = getSystemService(NotificationManager.class);
            }
            notificationManager.createNotificationChannel(channel);
        }
    }
}
