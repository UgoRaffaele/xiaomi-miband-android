package com.ugopiemontese.miband;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;

import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.listeners.ActionClickListener;

public class MiActivity extends ActionBarActivity implements LeScanCallback {

	private BluetoothAdapter mBluetoothAdapter;
	private boolean mScanning;
	private TextView mTextView;

	private Handler mHandler = new Handler();
	private static final long SCAN_PERIOD = 10000; // 10 seconds.

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_mi);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

		mTextView = (TextView) findViewById(R.id.text_search);
		mBluetoothAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();

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

	@Override
	public void onResume() {
		super.onResume();
        if (mBluetoothAdapter.isEnabled())
		    scanLeDevice(true);
	}

	@SuppressWarnings("deprecation")
	private void scanLeDevice(final boolean enable) {
        if (enable) {
            mTextView.setText(R.string.looking_for_miband);
            mHandler.postDelayed(new Runnable() {

                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(MiActivity.this);
                    mTextView.setText(R.string.not_found);
                    showAlert();
                }
            }, SCAN_PERIOD);
            mScanning = true;
            mBluetoothAdapter.startLeScan(this);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(this);
        }
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
                            scanLeDevice(true);
                        }
                    }), this);
    }

	@Override
	public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        if (device != null && device.getName().equals("MI")) {
			System.out.println(device.getAddress());
			scanLeDevice(false); // we only care about one miband so that's enough
			Intent intent = new Intent(getApplicationContext(), MiOverviewActivity.class);
			intent.putExtra("address", device.getAddress());
			startActivity(intent);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
        if (mBluetoothAdapter.isEnabled())
		    scanLeDevice(false);
	}
}
