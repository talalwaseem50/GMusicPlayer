package com.example.gmusicplayer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.drive_btn).setOnClickListener(view -> {
            Intent intent = new Intent(this, DriveActivity.class);
            startActivity(intent);
        });

    }



}

