package com.example.gmusicplayer.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.widget.Toast;


public class InternetConnectivityReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (wifi.isWifiEnabled()) {
            Toast toast = Toast.makeText(context,
                    "Wifi is connected",
                    Toast.LENGTH_SHORT);

            toast.show();
        } else {
            Toast toast = Toast.makeText(context,
                    "Wifi is not connected",
                    Toast.LENGTH_SHORT);

            toast.show();
        }
    }
}
