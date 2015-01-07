package com.ugopiemontese.miband;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.preference.Preference;
import android.content.SharedPreferences;
import android.preference.PreferenceFragment;

import android.support.v7.widget.Toolbar;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;

import com.ugopiemontese.miband.model.LeParams;

public class PreferencesFragment extends ActionBarActivity {

    private LeParams params;
    private String mac;
    private String firmware;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        params = (LeParams) getIntent().getParcelableExtra("params");
        mac = (String) getIntent().getStringExtra("mac_address");
        firmware = getIntent().getStringExtra("firmware");

        setContentView(R.layout.activity_mi_preferences);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getFragmentManager().beginTransaction().replace(R.id.content, new PrefsFragment(params, mac, firmware)).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("ValidFragment")
    public static class PrefsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

        private LeParams mParams;
        private String mMacAddress;
        private String mFirmware;

        public PrefsFragment(LeParams params, String mac, String firmware) {
            mParams = params;
            mMacAddress = mac;
            mFirmware = firmware;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);

            SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
            SharedPreferences.Editor editor = sharedPreferences.edit();

            Preference name = findPreference("name");
            Preference height = findPreference("height");
            Preference goal = findPreference("goal");
            Preference sex = findPreference("sex");

            Preference connection_interval = findPreference("connection_interval");
            editor.putString("connection_interval", String.format("%s ms", mParams.connInt));

            Preference advertising_interval = findPreference("advertising_interval");
            editor.putString("advertising_interval", String.format("%s ms", mParams.advInt));

            Preference latency = findPreference("latency");
            editor.putString("latency", String.format("%s ms", mParams.latency));

            Preference timeout = findPreference("timeout");
            editor.putString("timeout", String.format("%s ms", mParams.timeout));

            Preference mac_address = findPreference("mac_address");
            editor.putString("mac_address", (String) mMacAddress);

            Preference firmware = findPreference("firmware");
            editor.putString("firmware", (String) mFirmware);

            editor.commit();

            name.setSummary(sharedPreferences.getString("name", getResources().getString(R.string.summary_name_preference)));
            height.setSummary(sharedPreferences.getString("height", getResources().getString(R.string.summary_height_preference)));
            goal.setSummary(sharedPreferences.getString("goal", getResources().getString(R.string.summary_goal_preference)));
            sex.setSummary(sharedPreferences.getString("sex", getResources().getString(R.string.summary_sex_preference)));

            connection_interval.setSummary(sharedPreferences.getString("connection_interval", ""));
            advertising_interval.setSummary(sharedPreferences.getString("advertising_interval", ""));
            latency.setSummary(sharedPreferences.getString("latency", ""));
            timeout.setSummary(sharedPreferences.getString("timeout", ""));
            mac_address.setSummary(sharedPreferences.getString("mac_address", ""));
			firmware.setSummary(sharedPreferences.getString("firmware", ""));

        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        }

        @Override
        public void onPause() {
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
            super.onPause();
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Preference pref = findPreference(key);
            pref.setSummary(sharedPreferences.getString(key, ""));
        }

    }

}