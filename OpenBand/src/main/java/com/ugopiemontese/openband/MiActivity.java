package com.ugopiemontese.openband;

import android.os.Bundle;
import android.os.Handler;
import android.content.Intent;
import android.content.Context;
import android.widget.TextView;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.preference.PreferenceManager;

import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.listeners.ActionClickListener;

import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;

public class MiActivity extends ActionBarActivity {

    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;

    private TextView mTextView;

    private static final long SCAN_PERIOD = 10000; // 10 seconds.

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_mi);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mHandler = new Handler();

        mTextView = (TextView) findViewById(R.id.text_search);

        mBluetoothAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            SnackbarManager.show(
                Snackbar.with(getApplicationContext())
                    .text(getResources().getString(R.string.ble_not_supported))
                    .duration(Snackbar.SnackbarDuration.LENGTH_INDEFINITE)
                    .animation(true), this);
        } else {

            if (!mBluetoothAdapter.isEnabled()) {
                SnackbarManager.show(
                    Snackbar.with(getApplicationContext())
                        .text(getResources().getString(R.string.enable_ble))
                        .duration(Snackbar.SnackbarDuration.LENGTH_INDEFINITE)
                        .animation(true)
                        .actionLabel(getResources().getString(R.string.action_enable_ble))
                        .actionColor(getResources().getColor(R.color.graph_color_primary))
                        .actionListener(new ActionClickListener() {
                            @Override
                            public void onActionClicked(Snackbar snackbar) {
                                Intent intentBluetooth = new Intent();
                                intentBluetooth.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                                startActivity(intentBluetooth);
                            }
                        }), this);
            }

        }

    }

    @Override
    public void onResume() {
        super.onResume();

        if (!mBluetoothAdapter.isEnabled()) {
            SnackbarManager.show(
                Snackbar.with(getApplicationContext())
                    .text(getResources().getString(R.string.enable_ble))
                    .duration(Snackbar.SnackbarDuration.LENGTH_INDEFINITE)
                    .animation(true)
                    .actionLabel(getResources().getString(R.string.action_enable_ble))
                    .actionColor(getResources().getColor(R.color.graph_color_primary))
                    .actionListener(new ActionClickListener() {
                        @Override
                        public void onActionClicked(Snackbar snackbar) {
                            Intent intentBluetooth = new Intent();
                            intentBluetooth.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                            startActivity(intentBluetooth);
                        }
                    }), this);
        }

        scanLeDevice(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mBluetoothAdapter.isEnabled())
            scanLeDevice(false);
    }

    @SuppressWarnings("deprecation")
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                mScanning = false;
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                mTextView.setText(R.string.not_found);
                showAlert();
                }
            }, SCAN_PERIOD);
            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    public void showAlert() {
        if (mBluetoothAdapter.isEnabled())
            SnackbarManager.show(
                Snackbar.with(getApplicationContext())
                    .text(getResources().getString(R.string.scan_again))
                    .duration(Snackbar.SnackbarDuration.LENGTH_INDEFINITE)
                    .animation(true)
                    .actionLabel(getResources().getString(R.string.action_scan_again))
                    .actionColor(getResources().getColor(R.color.graph_color_primary))
                    .actionListener(new ActionClickListener() {
                        @Override
                        public void onActionClicked(Snackbar snackbar) {
                            mTextView.setText(R.string.looking_for_miband);
                            scanLeDevice(true);
                        }
                    }), this);
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                if (device != null && device.getName().equals("MI")) {
                    scanLeDevice(false); // we only care about one miband so that's enough

                    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("mac_address", (String) device.getAddress());
                    editor.commit();

                    Intent overview = new Intent(getApplicationContext(), MiOverviewActivity.class);
                    overview.putExtra(MiOverviewActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
                    startActivity(overview);
                }
                }
            });
        }
    };

}