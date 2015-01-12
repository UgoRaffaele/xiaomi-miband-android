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
    	
    	public byte[] setUserInfo(String alias, int gender, int age, int height, int weight, int type) {
    		int i = 0;
        	byte[] sequence = new byte[20];
        	
        	int uid = Integer.parseInt(alias);
        	
        	sequence[0] = (byte) uid;
        	sequence[1] = (byte) (uid >>> 8);
        	sequence[2] = (byte) (uid >>> 16);
        	sequence[3] = (byte) (uid >>> 24);
        	
        	sequence[4] = (byte) (gender & 0xff);
        	sequence[5] = (byte) (age & 0xff);
        	sequence[6] = (byte) (height & 0xff);
        	sequence[7] = (byte) (weight & 0xff);
        	sequence[8] = (byte) (type & 0xff);
        	
        	for (i = 9; i < 19; i++)
        		sequence[i] = alias.getBytes()[i-9];
        		
        	byte[] crcSequence = new byte[19];
        	for (i = 0; i < crcSequence.length; i++)
        		crcSequence[i] = sequence[i];
        	
        	sequence[19] = (byte) ((getCRC8(crcSequence) ^ Integer.parseInt(mBTAddress.substring(mBTAddress.length()-2), 16)) & 0xff);
        	
        	return sequence;
	}
	
	protected int getCRC8(byte[] seq) {
		int len = seq.length;
		int y = 0;
		byte crc = 0x00;
		
		while (len-- > 0) {
			byte extract = (byte) seq[y++];
			
			for (byte tempI = 8; tempI != 0; tempI--) {
				byte sum = (byte) ((crc & 0xff) ^ (extract & 0xff));
				sum = (byte) ((sum & 0xff) & 0x01);
				crc = (byte) ((crc & 0xff) >>> 1);
				if (sum != 0)
					crc = (byte)((crc & 0xff) ^ 0x8c);
				extract = (byte) ((extract & 0xff) >>> 1);
			}
		}
		
		return (int) (crc & 0xff);
	}
	
}
