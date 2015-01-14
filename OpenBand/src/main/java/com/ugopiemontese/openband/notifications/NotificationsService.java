package com.ugopiemontese.openband.notifications;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;

import android.os.IBinder;
import android.content.ComponentName;
import android.content.Intent;
import android.content.Context;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import android.service.notification.StatusBarNotification;
import android.service.notification.NotificationListenerService;

import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothGattCharacteristic;

import com.ugopiemontese.openband.BluetoothLeService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import android.util.Log;

public class NotificationsService extends NotificationListenerService {

    private final static String TAG = NotificationsService.class.getSimpleName();

    private BluetoothLeService mBluetoothLeService;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private SharedPreferences sharedPreferences;

    private Map<UUID, BluetoothGattCharacteristic> map = new HashMap<UUID, BluetoothGattCharacteristic>();

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.v(TAG, "Unable to initialize Bluetooth");
            }
            mBluetoothLeService.connect(sharedPreferences.getString("mac_address", "88:0F:10:00:00:00"));
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                getGattService(mBluetoothLeService.getAlertService());
            }
        }
    };

    private void getGattService(BluetoothGattService service) {
        if (service == null) return;
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(BluetoothLeService.UUID_ALERT);
        map.put(characteristic.getUuid(), characteristic);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        return intentFilter;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {

        if (sharedPreferences.getBoolean("notifications", false)) {

            if (mBluetoothManager == null) {
                mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                if (mBluetoothManager == null) {
                    Log.v(TAG, "Unable to initialize BluetoothManager.");
                    return;
                }
            }

            if (mBluetoothAdapter == null) {
                mBluetoothAdapter = mBluetoothManager.getAdapter();
                if (mBluetoothAdapter == null) {
                    Log.v(TAG, "Unable to obtain a BluetoothAdapter.");
                    return;
                }
            }

            if (mBluetoothLeService == null) {
                Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
                bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
                registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
            }

            if (!mBluetoothLeService.isConnected())
                mBluetoothLeService.connect(sharedPreferences.getString("mac_address", "88:0F:10:00:00:00"));

            BluetoothGattCharacteristic alert = map.get(BluetoothLeService.UUID_ALERT);
            byte[] mildAlert = { 0x01 };
            alert.setValue(mildAlert);
            mBluetoothLeService.writeCharacteristic(alert);
        }

    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {}

    @Override
    public void onDestroy() {
        unregisterReceiver(mGattUpdateReceiver);
		mBluetoothLeService.disconnect();
        mBluetoothLeService.close();
        super.onDestroy();
    }

}