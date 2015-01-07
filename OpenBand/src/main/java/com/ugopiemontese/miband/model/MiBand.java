package com.ugopiemontese.miband.model;

import java.util.Observable;

public class MiBand extends Observable {

	public String mBTAddress;
	public int mSteps;
	public String mName;
	public Battery mBattery;
	public LeParams mLeParams;
    public String mFirmware;

	public void setName(String name) {
		mName = name;
		setChanged();
		notifyObservers();
	}
	
	public void setSteps(int steps) {
		mSteps = steps;
		setChanged();
		notifyObservers();
	}
	
	public void setBattery(Battery battery) {
		mBattery = battery;
		setChanged();
		notifyObservers();
	}

	public void setLeParams(LeParams params) {
		mLeParams = params;
	}
	
	public void setFirmware(byte[] firmware) {
        if (firmware.length == 4)
            mFirmware = firmware[3] + "." + firmware[2] + "." + firmware[1] + "." + firmware[0];
        else
            mFirmware = null;
    }
	
}
