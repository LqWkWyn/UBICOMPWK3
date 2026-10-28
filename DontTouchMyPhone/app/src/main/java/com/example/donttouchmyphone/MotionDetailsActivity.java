package com.example.donttouchmyphone;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MotionDetailsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_motion_details);

        TextView motionTime = findViewById(R.id.motion_time);
        String notificationTime = getIntent()
                .getStringExtra(MotionDetectionService.MOTION_TIME);
        long savedTime = getSharedPreferences("motion", MODE_PRIVATE)
                .getLong("last_motion_time", 0);

        if (notificationTime != null) {
            motionTime.setText("Motion detected at:\n" + notificationTime);
        } else if (savedTime == 0) {
            motionTime.setText("No motion has been detected yet.");
        } else {
            String time = new SimpleDateFormat("MMM d, yyyy HH:mm:ss",
                    Locale.getDefault()).format(new Date(savedTime));
            motionTime.setText("Motion detected at:\n" + time);
        }
    }
}
