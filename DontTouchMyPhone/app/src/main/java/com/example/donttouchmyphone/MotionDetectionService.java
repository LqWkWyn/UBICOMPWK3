package com.example.donttouchmyphone;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MotionDetectionService extends Service implements SensorEventListener {
    private static final String TAG = "MotionService";
    private static final String CHANNEL_ID = "motion_channel";
    private static final int SERVICE_NOTIFICATION_ID = 10;
    private static final int MOTION_NOTIFICATION_ID = 11;
    private static final float MOVEMENT_LIMIT = 1.5f;
    public static final String MOTION_TIME = "motion_time";

    private SensorManager sensorManager;
    private HandlerThread sensorThread;
    private Handler sensorHandler;
    private long lastNotificationTime;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(SERVICE_NOTIFICATION_ID, createMonitoringNotification());

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorThread = new HandlerThread("AccelerometerThread");
        sensorThread.start();
        sensorHandler = new Handler(sensorThread.getLooper());

        if (accelerometer != null) {
            Log.d(TAG, "Accelerometer found. Starting sensor listener");
            sensorManager.registerListener(this, accelerometer,
                    SensorManager.SENSOR_DELAY_GAME, sensorHandler);
        } else {
            Log.e(TAG, "This phone does not have an accelerometer");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        float totalAcceleration = (float) Math.sqrt(x * x + y * y + z * z);
        float movement = Math.abs(totalAcceleration - SensorManager.GRAVITY_EARTH);

        if (movement > MOVEMENT_LIMIT
                && SystemClock.elapsedRealtime() - lastNotificationTime > 3000) {
            lastNotificationTime = SystemClock.elapsedRealtime();
            long motionTime = System.currentTimeMillis();
            String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    .format(new Date(motionTime));

            getSharedPreferences("motion", MODE_PRIVATE).edit()
                    .putLong("last_motion_time", motionTime)
                    .commit();
            Log.d(TAG, "Motion detected at " + time);
            showMotionNotification(time);
        }
    }

    private void showMotionNotification(String time) {
        Intent detailsIntent = new Intent(this, MotionDetailsActivity.class);
        detailsIntent.putExtra(MOTION_TIME, time);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, detailsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Motion detected")
                .setContentText("The phone was moved at " + time)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build();
        getSystemService(NotificationManager.class).notify(MOTION_NOTIFICATION_ID, notification);
    }

    private Notification createMonitoringNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Motion detector is running")
                .setContentText("Monitoring the accelerometer")
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Motion alerts",
                NotificationManager.IMPORTANCE_HIGH);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onDestroy() {
        sensorManager.unregisterListener(this);
        sensorThread.quitSafely();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
