package com.ugopiemontese.openband.model;

import java.util.HashMap;

public class GattAttributes {

    private static HashMap<String, String> attributes = new HashMap();

    public static String MILI_SERVICE = "0000fee0-0000-1000-8000-00805f9b34fb";
    public static String CHAR_INFO = "0000ff01-0000-1000-8000-00805f9b34fb";
    public static String CHAR_DEVICE_NAME = "0000ff02-0000-1000-8000-00805f9b34fb";
    public static String CHAR_USER_INFO = "0000ff04-0000-1000-8000-00805f9b34fb";
    public static String CHAR_CONTROL_POINT = "0000ff05-0000-1000-8000-00805f9b34fb";
    public static String CHAR_REALTIME_STEPS = "0000ff06-0000-1000-8000-00805f9b34fb";
    public static String CHAR_ACTIVITY = "0000ff07-0000-1000-8000-00805f9b34fb";
    public static String CHAR_LE_PARAMS = "0000ff09-0000-1000-8000-00805f9b34fb";
    public static String CHAR_BATTERY = "0000ff0c-0000-1000-8000-00805f9b34fb";
    public static String CHAR_PAIR = "0000ff0f-0000-1000-8000-00805f9b34fb";

    public static String ALERT_SERVICE = "00001802-0000-1000-8000-00805F9B34FB";
    public static String CHAR_ALERT = "00002A06-0000-1000-8000-00805F9B34FB";

    static {

        //Services.
        attributes.put(MILI_SERVICE, "MiLi Service");
        attributes.put(ALERT_SERVICE, "Alert Service");

        // Characteristics.
        attributes.put(CHAR_INFO, "Info String");
        attributes.put(CHAR_DEVICE_NAME, "Device Name String");
        attributes.put(CHAR_USER_INFO, "User Info String");
        attributes.put(CHAR_CONTROL_POINT, "Control Point");
        attributes.put(CHAR_REALTIME_STEPS, "Realtime Steps Couple");
        attributes.put(CHAR_ACTIVITY, "Activity String");
        attributes.put(CHAR_LE_PARAMS, "LE Params");
        attributes.put(CHAR_BATTERY, "Battery Status String");
        attributes.put(CHAR_PAIR, "Pair Status");
        attributes.put(CHAR_ALERT, "Notifications Control Point");

    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }

}