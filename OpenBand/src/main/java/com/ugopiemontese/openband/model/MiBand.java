package com.ugopiemontese.openband.model;


public class MiBand {

    public String mBTAddress;
    public int mSteps;
    public String mName;
    public Battery mBattery;
    public LeParams mLeParams;
    public String mFirmware;
    public byte[] mUserInfo;

    public void setName(String name) {
        mName = name;
    }

    public void setSteps(int steps) {
        mSteps = steps;
    }

    public void setBattery(Battery battery) {
        mBattery = battery;
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

    public void setUserInfo(String alias, int gender, int age, int height, int weight, int type) {
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

        for (int u = 9; u < 19; u++)
            sequence[u] = alias.getBytes()[u-9];

        byte[] crcSequence = new byte[19];
        for (int u = 0; u < crcSequence.length; u++)
            crcSequence[u] = sequence[u];

        sequence[19] = (byte) ((getCRC8(crcSequence) ^ Integer.parseInt(mBTAddress.substring(mBTAddress.length()-2), 16)) & 0xff);

        mUserInfo = sequence;
    }

    protected int getCRC8(byte[] seq) {
        int len = seq.length;
        int i = 0;
        byte crc = 0x00;

        while (len-- > 0) {
            byte extract = seq[i++];
            for (byte tempI = 8; tempI != 0; tempI--) {
                byte sum = (byte) ((crc & 0xff) ^ (extract & 0xff));
                sum = (byte) ((sum & 0xff) & 0x01);
                crc = (byte) ((crc & 0xff) >>> 1);
                if (sum != 0) {
                    crc = (byte)((crc & 0xff) ^ 0x8c);
                }
                extract = (byte) ((extract & 0xff) >>> 1);
            }
        }
        return (crc & 0xff);
    }

}