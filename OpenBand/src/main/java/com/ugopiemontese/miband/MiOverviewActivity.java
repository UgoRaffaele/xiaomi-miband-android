package com.ugopiemontese.miband;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;

import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;

import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;

import com.ugopiemontese.miband.model.Battery;
import com.ugopiemontese.miband.model.LeParams;
import com.ugopiemontese.miband.model.MiBand;
import com.ugopiemontese.miband.helper.SwipeTouchListener;

public class MiOverviewActivity extends ActionBarActivity implements Observer {

	private static final UUID UUID_MILI_SERVICE = UUID
			.fromString("0000fee0-0000-1000-8000-00805f9b34fb");
	private static final UUID UUID_CHAR_PAIR = UUID
			.fromString("0000ff0f-0000-1000-8000-00805f9b34fb");
    private static final UUID UUID_CHAR_INFO = UUID
            .fromString("0000ff01-0000-1000-8000-00805f9b34fb");
    private static final UUID UUID_CHAR_DEVICE_NAME = UUID
            .fromString("0000ff02-0000-1000-8000-00805f9b34fb");
	private static final UUID UUID_CHAR_CONTROL_POINT = UUID
			.fromString("0000ff05-0000-1000-8000-00805f9b34fb");
	private static final UUID UUID_CHAR_REALTIME_STEPS = UUID
			.fromString("0000ff06-0000-1000-8000-00805f9b34fb");
	private static final UUID UUID_CHAR_ACTIVITY = UUID
			.fromString("0000ff07-0000-1000-8000-00805f9b34fb");
	private static final UUID UUID_CHAR_LE_PARAMS = UUID
			.fromString("0000ff09-0000-1000-8000-00805f9b34fb");
	private static final UUID UUID_CHAR_BATTERY = UUID
			.fromString("0000ff0c-0000-1000-8000-00805f9b34fb");

	// BLUETOOTH
	private String mDeviceAddress;
	private BluetoothManager mBluetoothManager;
	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothDevice mBluetoothMi;
	private BluetoothGatt mGatt;

	private MiBand mMiBand = new MiBand();

	// UI
	private RelativeLayout mLoading;
    private PieChart mChart;
    private TextView mTVBatteryLevel;
    private Menu mMenu;

    private int mSteps;
    private double mDistance;
    private float MIBAND_GOAL = 5000;
    private SharedPreferences sharedPreferences;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mDeviceAddress = getIntent().getStringExtra("address");

		mMiBand.addObserver(this);
		mMiBand.mBTAddress = mDeviceAddress;

		setContentView(R.layout.activity_mi_overview);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        MIBAND_GOAL = Float.parseFloat(sharedPreferences.getString("goal", "5000"));

		mTVBatteryLevel = (TextView) findViewById(R.id.text_battery_level);

		mLoading = (RelativeLayout) findViewById(R.id.loading);

        mChart = (PieChart) findViewById(R.id.piechart);
        mChart.setTouchEnabled(true);
        mChart.setRotationEnabled(false);
        mChart.setDrawLegend(false);
        mChart.setDrawXValues(false);
        mChart.setDrawYValues(false);
        mChart.highlightValues(null);
        mChart.setDescription("");

        mChart.setHoleColor(getResources().getColor(R.color.background_floating_material_light));

        Paint paint = mChart.getPaint(Chart.PAINT_CENTER_TEXT);
        paint.setColor(getResources().getColor(R.color.abc_secondary_text_material_light));
        mChart.setCenterTextSize(30f);
        mChart.setCenterTextTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));

		mBluetoothManager = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE));
		mBluetoothAdapter = mBluetoothManager.getAdapter();
		mBluetoothMi = mBluetoothAdapter.getRemoteDevice(mDeviceAddress);

        mChart.setOnTouchListener(new SwipeTouchListener(this) {

            @Override
            public void onSwipeLeft() {
                int HEIGHT = Integer.valueOf(sharedPreferences.getString("height", "170"));
                String SEX = sharedPreferences.getString("sex", getResources().getStringArray(R.array.entries_sex_preference)[0].toString());
                if (SEX.equals(getResources().getStringArray(R.array.entries_sex_preference)[0].toString()))
                    mDistance = mSteps * HEIGHT * 0.415 / 100000;
                else
                    mDistance = mSteps * HEIGHT * 0.413 / 100000;
                mChart.setCenterText(String.format(Locale.getDefault(), "%.2f", mDistance) + "\n" + getResources().getString(R.string.distance));
                mChart.invalidate();
            }

            @Override
            public void onSwipeRight() {
                mChart.setCenterText(mSteps + "\n" + getResources().getString(R.string.steps));
                mChart.invalidate();
            }

            @Override
            public void onSwipeDown() {
                redrawChart();
                mGatt.connect();
            }

        });

	}

	@Override
	public void onResume() {
		super.onResume();
        redrawChart();
		mGatt = mBluetoothMi.connectGatt(this, false, mGattCallback);
		mGatt.connect();
    }

	@Override
	public void onPause() {
		super.onPause();
		mGatt.disconnect();
		mGatt.close();
		mGatt = null;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
        mMenu = menu;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_overview, mMenu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
            case R.id.action_preferences:
                if(mMiBand.mLeParams == null) {
                    SnackbarManager.show(
                        Snackbar.with(getApplicationContext())
                            .text(getResources().getString(R.string.no_le_params))
                            .duration(Snackbar.SnackbarDuration.LENGTH_SHORT)
                            .animation(true), this);
                    return true;
                }
                if(mMiBand.mBTAddress.equals("")) {
                    SnackbarManager.show(
                        Snackbar.with(getApplicationContext())
                            .text(getResources().getString(R.string.no_mac_address))
                            .duration(Snackbar.SnackbarDuration.LENGTH_SHORT)
                            .animation(true), this);
                    return true;
                }
                Intent intent = new Intent(getApplicationContext(), PreferencesFragment.class);
                intent.putExtra("params", mMiBand.mLeParams);
                intent.putExtra("mac_address", mMiBand.mBTAddress);
                intent.putExtra("firmware", mMiBand.mFirmware);
                startActivity(intent);
                break;
		}
		return true;
	}

	private void pair() {
		BluetoothGattCharacteristic chrt = getMiliService().getCharacteristic(UUID_CHAR_PAIR);
		chrt.setValue(new byte[] { 2 });
		mGatt.writeCharacteristic(chrt);
	}

	private void request(UUID what) {
		mGatt.readCharacteristic(getMiliService().getCharacteristic(what));
	}

	private BluetoothGattService getMiliService() {
		return mGatt.getService(UUID_MILI_SERVICE);
	}

	private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

		int state = 0;

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS)
				pair();
		}

		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			if (newState == BluetoothProfile.STATE_CONNECTED)
				gatt.discoverServices();
		}

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			request(UUID_CHAR_REALTIME_STEPS);
		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

            //Log.i(characteristic.getUuid().toString(), "state: " + state + " value:" + Arrays.toString(b));
            byte[] b = characteristic.getValue();

            if ((b.length > 0) && !String.valueOf(b).equals("")) {
                if (characteristic.getUuid().equals(UUID_CHAR_REALTIME_STEPS)) {
                    mMiBand.setSteps(0xff & b[0] | (0xff & b[1]) << 8);
                } else if (characteristic.getUuid().equals(UUID_CHAR_BATTERY)) {
                    Battery battery = Battery.fromByte(b);
                    mMiBand.setBattery(battery);
                } else if (characteristic.getUuid().equals(UUID_CHAR_DEVICE_NAME)) {
                    mMiBand.setName(new String(b));
                } else if (characteristic.getUuid().equals(UUID_CHAR_LE_PARAMS)) {
                    LeParams params = LeParams.fromByte(b);
                    mMiBand.setLeParams(params);
                } else if (characteristic.getUuid().equals(UUID_CHAR_INFO)) {
                    byte [] version = Arrays.copyOfRange(b, b.length - 4, b.length);
                    mMiBand.setFirmware(version);
                }
                state++;
            }
			switch (state) {
                case 0:
                    request(UUID_CHAR_REALTIME_STEPS);
                    break;
                case 1:
                    request(UUID_CHAR_BATTERY);
                    break;
                case 2:
                    request(UUID_CHAR_DEVICE_NAME);
                    break;
                case 3:
                    request(UUID_CHAR_LE_PARAMS);
                    break;
                case 4:
                    request(UUID_CHAR_INFO);
                    break;
			}

		}
	};

    public void redrawChart() {

        ArrayList<Entry> yVals = new ArrayList<Entry>();
        yVals.add(new Entry(mMiBand.mSteps, 0));
        MIBAND_GOAL = Float.parseFloat(sharedPreferences.getString("goal", "5000"));
        yVals.add(new Entry((mMiBand.mSteps > MIBAND_GOAL) ? 0 : MIBAND_GOAL - mMiBand.mSteps, 1));

        PieDataSet set = new PieDataSet(yVals, "Steps");
        set.setSliceSpace(1f);

        ArrayList<Integer> colors = new ArrayList<Integer>();
        colors.add(getResources().getColor(R.color.graph_color_primary));
        colors.add(getResources().getColor(R.color.background_floating_material_light));
        set.setColors(colors);

        ArrayList<String> xVals = new ArrayList<String>();
        xVals.add(0, "steps");
        xVals.add(1, "goal");

        PieData pie = new PieData(xVals, set);

        mChart.setData(pie);

        mChart.setCenterText(mMiBand.mSteps + "\n" + getResources().getString(R.string.steps));
        mChart.animateXY(750, 750);
        mChart.spin(750, 0, 270);
        mChart.invalidate();

    }

	@Override
	public void update(Observable observable, Object data) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {

                if (mSteps != mMiBand.mSteps) {
                    mSteps = mMiBand.mSteps;
                    redrawChart();
                }
                mLoading.setVisibility(View.GONE);

                findViewById(R.id.textHolder).setVisibility(View.VISIBLE);
                if (mMiBand.mBattery != null) {
                    mTVBatteryLevel.setText(mMiBand.mBattery.mBatteryLevel + "%");
                    mMenu.getItem(0).setVisible(true);
                }

			}
		});
	}

}