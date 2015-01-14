package com.ugopiemontese.openband;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
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

import com.ugopiemontese.openband.model.Battery;
import com.ugopiemontese.openband.model.LeParams;
import com.ugopiemontese.openband.model.MiBand;
import com.ugopiemontese.openband.helper.SwipeTouchListener;

public class MiOverviewActivity extends ActionBarActivity {

    // BLUETOOTH
    private String mDeviceAddress;
    private MiBand mMiBand = new MiBand();

    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private BluetoothLeService mBluetoothLeService;

    private Map<UUID, BluetoothGattCharacteristic> map = new HashMap<UUID, BluetoothGattCharacteristic>();

    private int count = 0;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
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
                getGattService(mBluetoothLeService.getMiliService());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                BluetoothGattCharacteristic characteristic;
                byte[] val = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                if (val != null && val.length > 0) {
                    switch (count) {
                        case 0:
                            byte[] version = Arrays.copyOfRange(val, val.length - 4, val.length);
                            mMiBand.setFirmware(version);
                            count++;
                            characteristic = map.get(BluetoothLeService.UUID_DEVICE_NAME);
                            mBluetoothLeService.readCharacteristic(characteristic);
                            break;
                        case 1:
                            mMiBand.setName(new String(val));
                            count++;
                            characteristic = map.get(BluetoothLeService.UUID_REALTIME_STEPS);
                            mBluetoothLeService.readCharacteristic(characteristic);
                            break;
                        case 2:
                            mMiBand.setSteps(0xff & val[0] | (0xff & val[1]) << 8);
                            count++;
                            characteristic = map.get(BluetoothLeService.UUID_LE_PARAMS);
                            mBluetoothLeService.readCharacteristic(characteristic);
                            break;
                        case 3:
                            LeParams params = LeParams.fromByte(val);
                            mMiBand.setLeParams(params);
                            count++;
                            characteristic = map.get(BluetoothLeService.UUID_BATTERY);
                            mBluetoothLeService.readCharacteristic(characteristic);
                            break;
                        case 4:
                            Battery battery = Battery.fromByte(val);
                            mMiBand.setBattery(battery);
                            mBluetoothLeService.disconnect();
                            count = 0;
                            redrawChart();
                            break;
                    }
                }
            }
        }
    };

    // UI
    private RelativeLayout mLoading;
    private PieChart mChart;
    private TextView mTVBatteryLevel;
    private Menu mMenu;

    private int mSteps;
    private double mDistance;
    private float MIBAND_GOAL;
    private SharedPreferences sharedPreferences;

    private final static String TAG = MiOverviewActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mi_overview);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final Intent intent = getIntent();
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        mMiBand.mBTAddress = mDeviceAddress;

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

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
                refreshData();
            }

        });

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

    }

    @Override
    public void onRestart() {
        super.onRestart();
        redrawChart();
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBluetoothLeService.disconnect();
        mBluetoothLeService.close();
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

    private void getGattService(BluetoothGattService service) {
        if (service == null) return;

        BluetoothGattCharacteristic characteristic;

        characteristic = service.getCharacteristic(BluetoothLeService.UUID_DEVICE_NAME);
        map.put(characteristic.getUuid(), characteristic);

        characteristic = service.getCharacteristic(BluetoothLeService.UUID_USER_INFO);
        map.put(characteristic.getUuid(), characteristic);

        characteristic = service.getCharacteristic(BluetoothLeService.UUID_CONTROL_POINT);
        map.put(characteristic.getUuid(), characteristic);

        characteristic = service.getCharacteristic(BluetoothLeService.UUID_REALTIME_STEPS);
        map.put(characteristic.getUuid(), characteristic);

        characteristic = service.getCharacteristic(BluetoothLeService.UUID_ACTIVITY);
        map.put(characteristic.getUuid(), characteristic);

        characteristic = service.getCharacteristic(BluetoothLeService.UUID_LE_PARAMS);
        map.put(characteristic.getUuid(), characteristic);

        characteristic = service.getCharacteristic(BluetoothLeService.UUID_BATTERY);
        map.put(characteristic.getUuid(), characteristic);

        characteristic = service.getCharacteristic(BluetoothLeService.UUID_PAIR);
        map.put(characteristic.getUuid(), characteristic);

        characteristic = service.getCharacteristic(BluetoothLeService.UUID_INFO);
        map.put(characteristic.getUuid(), characteristic);
        mBluetoothLeService.readCharacteristic(characteristic);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    public void refreshData() {

        if (mBluetoothLeService == null) {
            Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
            bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
            registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        }

        if (!mBluetoothLeService.isConnected())
            mBluetoothLeService.connect(sharedPreferences.getString("mac_address", "88:0F:10:00:00:00"));

        getGattService(mBluetoothLeService.getMiliService());

    }

    public void redrawChart() {
        if (mSteps != mMiBand.mSteps)
            mSteps = mMiBand.mSteps;

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

        mLoading.setVisibility(View.GONE);
        findViewById(R.id.textHolder).setVisibility(View.VISIBLE);
        if (mMiBand.mBattery != null) {
            mTVBatteryLevel.setText(mMiBand.mBattery.mBatteryLevel + "%");
            mMenu.getItem(0).setVisible(true);
        }
    }

}