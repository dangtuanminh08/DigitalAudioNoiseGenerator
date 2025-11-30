package com.example.audioplayer;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BluetoothReceiver extends BroadcastReceiver {

    // Woah Minh added a new class to his app 3 months after he decided he was finished?!?!?!
    // Anyways.
    // Checks for Bluetooth service states--in this case, checks if Bluetooth disconnects
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
            PlayerManager.pauseCheck();
        }
    }
}