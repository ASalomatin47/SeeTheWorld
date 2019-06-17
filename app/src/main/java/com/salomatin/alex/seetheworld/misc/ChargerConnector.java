package com.salomatin.alex.seetheworld.misc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.salomatin.alex.seetheworld.R;


// Checks connection to charger or USB
public class ChargerConnector extends BroadcastReceiver {

    private static final String TAG = "ChargerConnector";

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();

        if (action.equals(Intent.ACTION_POWER_CONNECTED)) {
            Log.d(TAG, "onReceive: Charger connected.");
            Toast.makeText(context, R.string.connected_toast, Toast.LENGTH_SHORT).show();
        } else if (action.equals(Intent.ACTION_POWER_DISCONNECTED)) {
            Log.d(TAG, "onReceive: Charger disconnected.");
            Toast.makeText(context, R.string.disconnected_toast, Toast.LENGTH_SHORT).show();
        }
    }
}

