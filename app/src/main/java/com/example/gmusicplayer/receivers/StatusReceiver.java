package com.example.gmusicplayer.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class StatusReceiver extends BroadcastReceiver {

    private static final String TAG = "StatusReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        int status = intent.getIntExtra("STATUS", 0);
        if (status == 1) {
            Log.d(TAG, "Upload Started Status");
            Toast.makeText(context, "Upload started", Toast.LENGTH_SHORT).show();

        }
        else if (status == 2) {
            Log.d(TAG, "Upload Complete Status");
            Toast.makeText(context, "Upload Completed", Toast.LENGTH_SHORT).show();
        }

    }
}
