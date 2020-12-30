package com.applex.camera;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class App extends Application {
    public static final String DATA_UPLOAD_CHANNEL_ID = "dataUploadServiceChannel";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel("Upload Image Notification", DATA_UPLOAD_CHANNEL_ID);
    }

    private void createNotificationChannel(String name, String channelId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    channelId,
                    name,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            serviceChannel.setSound(null, null);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
}

