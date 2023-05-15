package com.shayne.epsilon;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class MediaProjectionService extends Service {
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "my_channel_id";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForegroundService();
        return START_NOT_STICKY;
    }

    private void startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("channel_id", "Channel name", NotificationManager.IMPORTANCE_LOW);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        // Create the notification for the foreground service
        Notification notification = new NotificationCompat.Builder(this, "channel_id")
                .setContentTitle("Shayne ScreeShot Service")
                .setContentText("Service To Take ScreenShot")
                .setSmallIcon(R.drawable.icon)
                .build();
        startForeground(1, notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
