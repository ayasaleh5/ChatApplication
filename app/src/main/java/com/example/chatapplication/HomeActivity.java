package com.example.chatapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class HomeActivity extends AppCompatActivity {

    private static int SPLASH_TIME_OUT=3000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent splashIntent = new Intent(HomeActivity.this,StartActivity.class);
                startActivity(splashIntent);
                finish();
            }
        },SPLASH_TIME_OUT);
    }
}
